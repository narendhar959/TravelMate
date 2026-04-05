package com.klu.service;

import com.klu.entity.Booking;
import com.klu.entity.User;
import com.klu.models.BookingState;
import com.klu.repository.BookingRepository;
import com.klu.repository.UserRepository;
import com.klu.util.AiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiService {

    @Autowired
    private BookingRepository bookingRepo;
    
    @Autowired
    private UserRepository    userRepo;

    // ── entry point ────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleChat(Map<String, Object> data) {
        try {
            String  message       = str(data, "message", "").trim();
            Object  userIdRaw     = data.get("user_id");
            boolean paymentAction = bool(data, "payment_action");
            Object  userProfile   = data.get("user_profile");
            BookingState bookingState = BookingState.fromMap(data.get("booking_state"));

            if (message.isEmpty())
                return err("Message is required");

            // resolve DB user if user_id provided
            User dbUser = null;
            if (userIdRaw != null) {
                try { dbUser = userRepo.findById(Integer.parseInt(userIdRaw.toString())).orElse(null); }
                catch (Exception ignored) {}
            }
            Object userCtx = dbUser != null ? dbUser : userProfile;

            // ── STOP ──────────────────────────────────────────────────────
            if (AiUtils.isStop(message)) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "chat");
                r.put("booking_state", BookingState.blank().toMap());
                r.put("reply",         buildStopReply(userCtx));
                r.put("chat_closed",   true);
                return r;
            }

            // ── GREETING ─────────────────────────────────────────────────
            if (AiUtils.isGreeting(message)) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "chat");
                r.put("booking_state", bookingState.toMap());
                r.put("reply",         buildGreeting(userCtx));
                return r;
            }

            // ── UNREADABLE ────────────────────────────────────────────────
            if (AiUtils.isUnreadable(message)) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "chat");
                r.put("booking_state", bookingState.toMap());
                r.put("reply",         "I think you may have typed that by mistake, "
                    + AiUtils.userDisplayName(userCtx) + ". Please send it again and I will help you.");
                return r;
            }

            // ── USER CONTEXT QUESTIONS ────────────────────────────────────
            String ctxReply = buildUserContextReply(message, userCtx);
            if (ctxReply != null) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "chat");
                r.put("booking_state", bookingState.toMap());
                r.put("reply",         ctxReply);
                return r;
            }

            // ── MERGE BOOKING STATE ───────────────────────────────────────
            BookingState previous = BookingState.copyOf(bookingState);
            BookingState updated  = mergeBookingState(bookingState, message);
            boolean bookingChanged = hasBookingModification(message, previous, updated);

            if (!"booking".equals(updated.intent)) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "chat");
                r.put("booking_state", BookingState.blank().toMap());
                r.put("reply",         fallbackChatReply(message, userCtx));
                return r;
            }

            // ── MISSING FIELDS ────────────────────────────────────────────
            List<String> missing = missingFields(updated);
            if (!missing.isEmpty()) {
                Map<String, Object> r = new HashMap<>();
                r.put("intent",         "booking");
                r.put("booking_ready",  false);
                r.put("missing_fields", missing);
                r.put("booking_state",  updated.toMap());
                r.put("reply",          buildMissingDetailsReply(updated, userCtx));
                return r;
            }

            // ── BETTER PRICE ──────────────────────────────────────────────
            if (AiUtils.wantsBetterPrice(message)) {
                double discounted = applyBetterPrice(updated);
                updated.stage = "awaiting_confirmation";
                Map<String, Object> r = new HashMap<>();
                r.put("intent",           "booking");
                r.put("booking_ready",    true);
                r.put("quote_ready",      true);
                r.put("booking_state",    updated.toMap());
                r.put("quote_amount",     discounted);
                r.put("booking_summary",  buildBookingSummary(updated));
                r.put("reply",            buildDiscountReply(userCtx, updated));
                return r;
            }

            // ── BOOKING CHANGED (before awaiting_payment) ─────────────────
            if (bookingChanged && !"awaiting_payment".equals(updated.stage)) {
                updated.stage       = "collecting_details";
                updated.quoteAmount = estimateAmount(updated);
                Map<String, Object> r = new HashMap<>();
                r.put("intent",          "booking");
                r.put("booking_ready",   true);
                r.put("quote_ready",     true);
                r.put("booking_state",   updated.toMap());
                r.put("quote_amount",    updated.quoteAmount);
                r.put("booking_summary", buildBookingSummary(updated));
                r.put("reply",           buildModifiedReply(userCtx, updated));
                return r;
            }

            // ── AWAITING CONFIRMATION ─────────────────────────────────────
            if ("awaiting_confirmation".equals(updated.stage)) {
                if (AiUtils.isConfirmation(message)) {
                    updated.stage = "awaiting_payment";
                    Map<String, Object> r = new HashMap<>();
                    r.put("intent",          "booking");
                    r.put("booking_ready",   true);
                    r.put("payment_required",true);
                    r.put("booking_state",   updated.toMap());
                    r.put("quote_amount",    updated.quoteAmount != null ? updated.quoteAmount : estimateAmount(updated));
                    r.put("reply",           "Perfect " + AiUtils.userDisplayName(userCtx)
                        + ". Please pay with UPI below or scan the QR, then tap Pay.");
                    return r;
                }
                if (bookingChanged) {
                    updated.quoteAmount = estimateAmount(updated);
                    updated.stage       = "awaiting_confirmation";
                    Map<String, Object> r = new HashMap<>();
                    r.put("intent",          "booking");
                    r.put("booking_ready",   true);
                    r.put("quote_ready",     true);
                    r.put("booking_state",   updated.toMap());
                    r.put("quote_amount",    updated.quoteAmount);
                    r.put("booking_summary", buildBookingSummary(updated));
                    r.put("reply",           buildModifiedReply(userCtx, updated));
                    return r;
                }
                Map<String, Object> r = new HashMap<>();
                r.put("intent",        "booking");
                r.put("booking_ready", true);
                r.put("booking_state", updated.toMap());
                r.put("quote_amount",  updated.quoteAmount != null ? updated.quoteAmount : estimateAmount(updated));
                r.put("reply",         "If you want to continue, reply OK and I will open the payment option for you.");
                return r;
            }

            // ── AWAITING PAYMENT ──────────────────────────────────────────
            if ("awaiting_payment".equals(updated.stage) && !paymentAction) {
                if (bookingChanged) {
                    updated.stage       = "awaiting_confirmation";
                    updated.quoteAmount = estimateAmount(updated);
                    Map<String, Object> r = new HashMap<>();
                    r.put("intent",          "booking");
                    r.put("booking_ready",   true);
                    r.put("quote_ready",     true);
                    r.put("booking_state",   updated.toMap());
                    r.put("quote_amount",    updated.quoteAmount);
                    r.put("booking_summary", buildBookingSummary(updated));
                    r.put("reply",           buildModifiedReply(userCtx, updated));
                    return r;
                }
                Map<String, Object> r = new HashMap<>();
                r.put("intent",          "booking");
                r.put("booking_ready",   true);
                r.put("payment_required",true);
                r.put("booking_state",   updated.toMap());
                r.put("quote_amount",    updated.quoteAmount != null ? updated.quoteAmount : estimateAmount(updated));
                r.put("reply",           "Your payment option is ready. Tap Pay after choosing UPI or scanning the QR.");
                return r;
            }

            // ── FIRST QUOTE ───────────────────────────────────────────────
            if (!"awaiting_confirmation".equals(updated.stage) && !"awaiting_payment".equals(updated.stage)) {
                updated.quoteAmount = estimateAmount(updated);
                updated.stage       = "awaiting_confirmation";
                Map<String, Object> r = new HashMap<>();
                r.put("intent",          "booking");
                r.put("booking_ready",   true);
                r.put("quote_ready",     true);
                r.put("booking_state",   updated.toMap());
                r.put("quote_amount",    updated.quoteAmount);
                r.put("booking_summary", buildBookingSummary(updated));
                r.put("reply",           buildQuoteReply(userCtx, updated));
                return r;
            }

            // ── SAVE BOOKING ──────────────────────────────────────────────
            if (dbUser == null) {
                if (userProfile != null) {
                    Map<String, Object> local = createLocalBooking(userProfile, updated);
                    Map<String, Object> r = new HashMap<>();
                    r.put("intent",          "booking");
                    r.put("booking_ready",   true);
                    r.put("booking_created", true);
                    r.put("booking_state",   BookingState.blank().toMap());
                    r.put("booking_summary", buildBookingSummary(updated));
                    r.put("booking",         local);
                    r.put("saved_locally",   true);
                    r.put("reply",           "Done " + AiUtils.userDisplayName(userProfile)
                        + ". Your " + updated.bookingType + " booking is confirmed for "
                        + updated.travelDate + ". I also saved it to your upcoming bookings on this device.");
                    return r;
                }
                Map<String, Object> r = new HashMap<>();
                r.put("intent",         "booking");
                r.put("booking_ready",  false);
                r.put("requires_login", true);
                r.put("booking_state",  updated.toMap());
                r.put("reply",          "I have all the booking details now. Please log in to TravelMate and send the message again so I can save it.");
                return r;
            }

            Booking saved = saveBooking(dbUser, updated);
            Map<String, Object> serialized = serializeBooking(saved, updated.travelTime);
            Map<String, Object> r = new HashMap<>();
            r.put("intent",          "booking");
            r.put("booking_ready",   true);
            r.put("booking_created", true);
            r.put("booking_state",   BookingState.blank().toMap());
            r.put("booking_summary", buildBookingSummary(updated));
            r.put("booking",         serialized);
            r.put("reply",           bookingConfirmationReply(dbUser, serialized));
            return r;

        } catch (Exception ex) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", ex.getMessage());
            err.put("reply", "Something went wrong while handling that request. Please try again.");
            return err;
        }
    }

    // ── merge booking state ────────────────────────────────────────────────
    private BookingState mergeBookingState(BookingState prev, String message) {
        BookingState s = BookingState.copyOf(prev);
        String n = AiUtils.normalize(message);

        if (n.contains("book") || n.contains("booking") || n.contains("reserve") || n.contains("ticket"))
            s.intent = "booking";

        String bt = AiUtils.detectBookingType(message);
        if (bt != null) { s.intent = "booking"; s.bookingType = bt; }

        String date = AiUtils.parseDateFromText(message);
        if (date != null) s.travelDate = date;

        String time = AiUtils.parseTimeFromText(message);
        if (time != null) s.travelTime = time;

        String[] locs = AiUtils.extractLocations(message, s.bookingType);
        if (locs[0] != null) s.fromLocation = locs[0];
        if (locs[1] != null) s.toLocation   = locs[1];

        Integer pax = AiUtils.extractPassengers(message);
        if (pax != null) s.passengers = pax;

        if (!"booking".equals(s.intent)) return BookingState.blank();
        return s;
    }

    // ── missing fields ─────────────────────────────────────────────────────
    private List<String> missingFields(BookingState s) {
        List<String> required = AiUtils.requiredFields(s.bookingType);
        List<String> missing  = new ArrayList<>();
        for (String f : required) {
            switch (f) {
                case "booking_type"  -> { if (s.bookingType  == null) missing.add(f); }
                case "from_location" -> { if (s.fromLocation == null) missing.add(f); }
                case "to_location"   -> { if (s.toLocation   == null) missing.add(f); }
                case "travel_date"   -> { if (s.travelDate   == null) missing.add(f); }
                case "travel_time"   -> { if (s.travelTime   == null) missing.add(f); }
                case "passengers"    -> { if (s.passengers   == null) missing.add(f); }
            }
        }
        return missing;
    }

    // ── has modification ───────────────────────────────────────────────────
    private boolean hasBookingModification(String message, BookingState prev, BookingState updated) {
        if (AiUtils.wantsModify(message)) return true;
        if (prev == null || !"booking".equals(prev.intent)) return false;
        return !Objects.equals(prev.bookingType,  updated.bookingType)
            || !Objects.equals(prev.fromLocation, updated.fromLocation)
            || !Objects.equals(prev.toLocation,   updated.toLocation)
            || !Objects.equals(prev.travelDate,   updated.travelDate)
            || !Objects.equals(prev.travelTime,   updated.travelTime)
            || !Objects.equals(prev.passengers,   updated.passengers);
    }

    // ── price helpers ──────────────────────────────────────────────────────
    private double estimateAmount(BookingState s) {
        double base = AiUtils.BOOKING_PRICES.getOrDefault(s.bookingType, 999.0);
        int pax = s.passengers != null ? s.passengers : 1;
        return Math.round(base * pax * 100.0) / 100.0;
    }

    private double applyBetterPrice(BookingState s) {
        double current = s.quoteAmount != null ? s.quoteAmount : estimateAmount(s);
        double disc = Math.max(Math.round(current * 0.9 * 100.0) / 100.0, 199.0);
        s.quoteAmount = disc;
        return disc;
    }

    // ── reply builders ─────────────────────────────────────────────────────
    private String buildGreeting(Object user) {
        return "Hi " + AiUtils.userDisplayName(user) + "! I'm TravelMate AI. "
            + "I can help you plan trips and book flights, trains, buses, cabs, hotels, tours, cruises, and insurance.";
    }

    private String buildStopReply(Object user) {
        return "Sure " + AiUtils.userDisplayName(user) + ", I'll stop here. Come back anytime you want help with travel.";
    }

    private String buildMissingDetailsReply(BookingState s, Object user) {
        List<String> missing = missingFields(s);
        if (missing.isEmpty()) return null;
        List<String> readable = new ArrayList<>();
        for (String f : missing) readable.add(AiUtils.friendlyFieldName(f, s.bookingType));
        if (readable.size() == 1)
            return "I can help with that. Please tell me the " + readable.get(0) + ".";
        String last = readable.remove(readable.size() - 1);
        return "I can book that for you. Please share the " + String.join(", ", readable) + " and " + last + ".";
    }

    private String buildBookingSummary(BookingState s) {
        List<String> parts = new ArrayList<>();
        if (s.bookingType != null) parts.add(capitalize(s.bookingType));
        if (s.fromLocation != null) parts.add("from " + s.fromLocation);
        if (s.toLocation   != null) parts.add("to "   + s.toLocation);
        if (s.travelDate   != null) parts.add("on "   + s.travelDate);
        if (s.travelTime   != null) parts.add("at "   + s.travelTime);
        if (s.passengers   != null) parts.add("for "  + s.passengers + " traveler" + (s.passengers > 1 ? "s" : ""));
        return String.join(" ", parts);
    }

    private String buildQuoteReply(Object user, BookingState s) {
        double amount = s.quoteAmount != null ? s.quoteAmount : estimateAmount(s);
        return "Hi " + AiUtils.userDisplayName(user) + ", your total bill is Rs. "
            + String.format("%.2f", amount) + " for " + buildBookingSummary(s)
            + ". If this looks good, reply OK to continue to payment.";
    }

    private String buildModifiedReply(Object user, BookingState s) {
        double amount = s.quoteAmount != null ? s.quoteAmount : estimateAmount(s);
        return "Okay " + AiUtils.userDisplayName(user) + ", I changed it. The updated total is Rs. "
            + String.format("%.2f", amount) + " for " + buildBookingSummary(s)
            + ". Reply OK if you want to continue to payment.";
    }

    private String buildDiscountReply(Object user, BookingState s) {
        double amount = s.quoteAmount != null ? s.quoteAmount : estimateAmount(s);
        return "I found you a better price, " + AiUtils.userDisplayName(user) + ". The new total is Rs. "
            + String.format("%.2f", amount) + ". If you like this fare, reply OK to continue to payment.";
    }

    private String fallbackChatReply(String message, Object user) {
        if (AiUtils.isGreeting(message)) return buildGreeting(user);
        return "Hi " + AiUtils.userDisplayName(user) + "! I can help with bookings, travel suggestions, budgets, and itineraries.";
    }

    private String bookingConfirmationReply(User user, Map<String, Object> booking) {
        String name = user.getFirstname() != null ? user.getFirstname() : user.getUsername();
        return "Done " + name + ". Your " + booking.get("type") + " booking is confirmed for "
            + booking.get("travel_date") + ". Booking reference: " + booking.get("booking_ref")
            + ". Would you like to book more plans?";
    }

    // ── user context questions ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private String buildUserContextReply(String message, Object user) {
        if (user == null) return null;
        String n    = AiUtils.normalize(message);
        String name = AiUtils.userDisplayName(user);

        String username  = null, firstname = null, city = null;
        if (user instanceof Map) {
            Map<String,Object> u = (Map<String,Object>) user;
            username  = u.get("username")  != null ? u.get("username").toString()  : null;
            firstname = u.get("firstname") != null ? u.get("firstname").toString() : null;
            city      = u.get("city")      != null ? u.get("city").toString()      : null;
        } else if (user instanceof User) {
            User u = (User) user;
            username  = u.getUsername();
            firstname = u.getFirstname();
            city      = u.getCity();
        }

        boolean isPersonalQuery = n.contains("who am i") || n.contains("do you know me")
            || n.contains("what do you know about me") || n.contains("what is my name")
            || n.contains("tell me my name") || n.contains("my username")
            || n.contains("my city") || n.contains("which account")
            || (name != null && n.contains(name.toLowerCase()));

        if (!isPersonalQuery) return null;

        if (n.contains("how do you know") || n.contains("how you know"))
            return "I know " + name + " from the TravelMate login details available in this session. "
                + "I only use that profile information to personalize booking help inside the website.";

        if (n.contains("who am i") || n.contains("do you know me"))
            return "You are signed in as " + name + " on TravelMate, and that is how I recognize you in this chat.";

        if (n.contains("what do you know about me")) {
            List<String> details = new ArrayList<>();
            details.add("your name is " + name);
            if (username  != null) details.add("your username is " + username);
            if (city      != null) details.add("your city is " + city);
            return "From your TravelMate login, I know that " + String.join(", ", details)
                + ". I only use this information to help with your travel bookings.";
        }

        if (n.contains("what is my name") || n.contains("tell me my name"))
            return "Your name on this TravelMate session is " + name + ".";

        if (n.contains("my username") || n.contains("what is my username"))
            return username != null ? "Your TravelMate username is " + username + "."
                : "I do not have your username available in this session.";

        if (n.contains("my city") || n.contains("what is my city"))
            return city != null ? "Your city in this TravelMate session is " + city + "."
                : "I do not have your city details in this session.";

        if (n.contains("which account"))
            return username != null
                ? "You are currently using the TravelMate account for " + name + " with username " + username + "."
                : "You are currently using the TravelMate account for " + name + ".";

        if (name != null && n.contains(name.toLowerCase()))
            return name + " is the logged-in TravelMate user for this session, based on the account details saved after login.";

        return null;
    }

    // ── DB booking ─────────────────────────────────────────────────────────
    private Booking saveBooking(User user, BookingState s) {
        Booking b = new Booking();
        b.setBookingRef(AiUtils.genRef());
        b.setUserId(user.getId());
        b.setBookingType(s.bookingType);
        b.setFromLocation(s.fromLocation);
        b.setToLocation(s.toLocation);
        if (s.travelDate != null) {
            try { b.setTravelDate(LocalDate.parse(s.travelDate)); } catch (Exception ignored) {}
        }
        b.setPassengers(s.passengers != null ? s.passengers : 1);
        b.setAmount(s.quoteAmount != null ? s.quoteAmount : estimateAmount(s));
        b.setStatus("confirmed");
        b.setCreatedAt(LocalDateTime.now());
        return bookingRepo.save(b);
    }

    private Map<String, Object> serializeBooking(Booking b, String travelTime) {
        Map<String, Object> m = new HashMap<>();
        m.put("id",          b.getId());
        m.put("booking_ref", b.getBookingRef());
        m.put("user_id",     b.getUserId());
        m.put("type",        b.getBookingType());
        m.put("from",        b.getFromLocation());
        m.put("to",          b.getToLocation());
        m.put("travel_date", b.getTravelDate() != null ? b.getTravelDate().toString() : null);
        m.put("time",        travelTime);
        m.put("passengers",  b.getPassengers());
        m.put("amount",      b.getAmount());
        m.put("status",      b.getStatus());
        m.put("created_at",  b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        if (b.getUser() != null) {
            m.put("username", b.getUser().getUsername());
            m.put("fullname", trim(b.getUser().getFirstname()) + " " + trim(b.getUser().getLastname()));
        }
        return m;
    }

    // ── local (no DB) booking payload ──────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> createLocalBooking(Object userProfile, BookingState s) {
        Map<String, Object> m = new HashMap<>();
        String ref = AiUtils.genRef();
        m.put("id",          ref);
        m.put("booking_ref", ref);
        m.put("type",        s.bookingType);
        m.put("from",        s.fromLocation);
        m.put("to",          s.toLocation);
        m.put("travel_date", s.travelDate);
        m.put("time",        s.travelTime);
        m.put("passengers",  s.passengers != null ? s.passengers : 1);
        m.put("amount",      s.quoteAmount != null ? s.quoteAmount
                             : AiUtils.BOOKING_PRICES.getOrDefault(s.bookingType, 999.0));
        m.put("status",      "confirmed");
        m.put("created_at",  LocalDateTime.now().toString());
        if (userProfile instanceof Map) {
            Map<String,Object> u = (Map<String,Object>) userProfile;
            m.put("user_id",  u.get("id") != null ? u.get("id") : u.get("user_id"));
            m.put("username", u.get("username"));
            String fn = u.get("fullname") != null ? u.get("fullname").toString()
                : trim(u.get("firstname")) + " " + trim(u.get("lastname"));
            m.put("fullname", fn.trim());
        }
        return m;
    }

    // ── small helpers ──────────────────────────────────────────────────────
    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private static boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String trim(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static Map<String, Object> err(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("error", msg);
        return r;
    }
}
