package com.klu.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class AiUtils {

    // ── Booking type keywords ──────────────────────────────────────────────
    public static final Map<String, List<String>> BOOKING_TYPES = new LinkedHashMap<>();
    static {
        BOOKING_TYPES.put("flight",    Arrays.asList("flight","plane","air ticket","airfare"));
        BOOKING_TYPES.put("train",     Arrays.asList("train","rail","railway"));
        BOOKING_TYPES.put("bus",       Arrays.asList("bus","coach"));
        BOOKING_TYPES.put("cab",       Arrays.asList("cab","taxi","ride"));
        BOOKING_TYPES.put("hotel",     Arrays.asList("hotel","stay","room","resort"));
        BOOKING_TYPES.put("cruise",    Arrays.asList("cruise","ship"));
        BOOKING_TYPES.put("tour",      Arrays.asList("tour","package","holiday","trip"));
        BOOKING_TYPES.put("insurance", Arrays.asList("insurance","cover"));
    }

    public static final Map<String, Double> BOOKING_PRICES = new HashMap<>();
    static {
        BOOKING_PRICES.put("flight",    5499.0);
        BOOKING_PRICES.put("train",     1299.0);
        BOOKING_PRICES.put("bus",        899.0);
        BOOKING_PRICES.put("cab",       1499.0);
        BOOKING_PRICES.put("hotel",     3499.0);
        BOOKING_PRICES.put("cruise",    7999.0);
        BOOKING_PRICES.put("tour",      6499.0);
        BOOKING_PRICES.put("insurance",  499.0);
    }

    private static final Set<String> GREETINGS = new HashSet<>(Arrays.asList(
        "hi","hello","hey","good morning","good evening","good afternoon"
    ));

    private static final Set<String> STOPS = new HashSet<>(Arrays.asList(
        "stop","bye","goodbye","exit","quit","close chat","end chat",
        "thanks","thank you","thankyou"
    ));

    private static final Set<String> COMMON_CHAT_WORDS = new HashSet<>(Arrays.asList(
        "book","booking","flight","train","bus","cab","hotel","tour","cruise",
        "insurance","from","to","on","tomorrow","today","date","time","morning",
        "evening","night","noon","hyderabad","mumbai","goa","chennai","bangalore",
        "kolkata","delhi","please","want","need","change","modify","price",
        "better","discount","travel","traveler","travelers","people","person",
        "ticket","tickets","yes","ok","okay","hi","hello","hey","thanks"
    ));

    // ── normalize ──────────────────────────────────────────────────────────
    public static String normalize(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // ── greeting / stop ───────────────────────────────────────────────────
    public static boolean isGreeting(String text) {
        return GREETINGS.contains(normalize(text));
    }

    public static boolean isStop(String text) {
        return STOPS.contains(normalize(text));
    }

    // ── unreadable input ──────────────────────────────────────────────────
    public static boolean isUnreadable(String text) {
        String n = normalize(text);
        if (n.isEmpty() || n.length() <= 2) return false;
        String[] tokens = n.split("[^a-zA-Z]+");
        List<String> words = new ArrayList<>();
        for (String t : tokens) if (!t.isEmpty()) words.add(t);
        if (words.isEmpty()) return false;
        int readable = 0;
        for (String w : words) {
            if (COMMON_CHAT_WORDS.contains(w)) { readable++; continue; }
            if (w.length() >= 3 && w.matches(".*[aeiou].*")) readable++;
        }
        double ratio = (double) readable / words.size();
        boolean noise = words.stream().anyMatch(w -> w.matches("[bcdfghjklmnpqrstvwxyz]{5,}"));
        return ratio < 0.34 || noise;
    }

    // ── booking type detection ────────────────────────────────────────────
    public static String detectBookingType(String text) {
        String n = normalize(text);
        for (Map.Entry<String, List<String>> e : BOOKING_TYPES.entrySet())
            for (String kw : e.getValue())
                if (n.contains(kw)) return e.getKey();
        return null;
    }

    // ── date parsing ──────────────────────────────────────────────────────
    public static String parseDateFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        String n = normalize(text)
            .replace("tomarrow","tomorrow").replace("tmrw","tomorrow")
            .replace("day after tomarrow","day after tomorrow");

        LocalDate today = LocalDate.now();

        if (n.contains("today"))              return today.toString();
        if (n.contains("day after tomorrow")) return today.plusDays(2).toString();
        if (n.contains("tomorrow"))           return today.plusDays(1).toString();

        // fix ordinals
        n = n.replaceAll("\\b(\\d{1,2})(st|nd|rd|th)\\b","$1").replace(","," ");

        // month spelling fixes
        Map<String,String> monthFix = new LinkedHashMap<>();
        monthFix.put("janurary","january"); monthFix.put("feburary","february");
        monthFix.put("marchh","march");     monthFix.put("aprill","april");
        monthFix.put("\\bjun\\b","june");   monthFix.put("jly","july");
        monthFix.put("augst","august");     monthFix.put("\\bsept\\b","september");
        monthFix.put("octobar","october");  monthFix.put("novembar","november");
        monthFix.put("decemeber","december");monthFix.put("decembar","december");
        monthFix.put("decemebr","december");
        for (Map.Entry<String,String> e : monthFix.entrySet())
            n = n.replaceAll(e.getKey(), e.getValue());

        // numeric patterns
        String[][] numPatterns = {
            {"(\\d{4}-\\d{2}-\\d{2})","yyyy-MM-dd"},
            {"(\\d{2}/\\d{2}/\\d{4})","dd/MM/yyyy"},
            {"(\\d{2}-\\d{2}-\\d{4})","dd-MM-yyyy"}
        };
        for (String[] p : numPatterns) {
            Matcher m = Pattern.compile(p[0]).matcher(text);
            if (m.find()) {
                try {
                    LocalDate d = LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern(p[1]));
                    if (!d.isBefore(today)) return d.toString();
                } catch (Exception ignored) {}
            }
        }

        // named month patterns
        String[] monthNames = {"january","february","march","april","may","june",
                               "july","august","september","october","november","december"};
        String monthGroup = String.join("|", monthNames);
        String[] namedPatterns = {
            "\\b(\\d{1,2})\\s+(" + monthGroup + ")\\b",
            "\\b(" + monthGroup + ")\\s+(\\d{1,2})\\b"
        };
        int year = today.getYear();
        for (String pat : namedPatterns) {
            Matcher m = Pattern.compile(pat, Pattern.CASE_INSENSITIVE).matcher(n);
            if (m.find()) {
                int day; String monthName;
                if (m.group(1).matches("\\d+")) { day = Integer.parseInt(m.group(1)); monthName = m.group(2); }
                else { monthName = m.group(1); day = Integer.parseInt(m.group(2)); }
                try {
                    LocalDate d = LocalDate.parse(day + " " + monthName + " " + year,
                        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
                    if (d.isBefore(today)) d = d.withYear(year + 1);
                    return d.toString();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    // ── time parsing ──────────────────────────────────────────────────────
    public static String parseTimeFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        String n = normalize(text).replace("o'clock","oclock").replace("o clock","oclock");

        Map<String,String> named = new LinkedHashMap<>();
        named.put("morning","09:00 AM"); named.put("afternoon","02:00 PM");
        named.put("evening","06:00 PM"); named.put("night","09:00 PM");
        named.put("noon","12:00 PM");    named.put("midnight","12:00 AM");
        for (Map.Entry<String,String> e : named.entrySet())
            if (n.contains(e.getKey())) return e.getValue();

        Matcher m = Pattern.compile("\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
            Pattern.CASE_INSENSITIVE).matcher(n);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            String min = m.group(2) != null ? m.group(2) : "00";
            return String.format("%02d:%s %s", h, min, m.group(3).toUpperCase());
        }

        Matcher oc = Pattern.compile("\\b(\\d{1,2})\\s*oclock\\b").matcher(n);
        if (oc.find()) {
            int h = Integer.parseInt(oc.group(1));
            if (h >= 1 && h <= 11) return String.format("%02d:00 %s", h, h <= 6 ? "PM" : "AM");
            if (h == 12) return "12:00 PM";
        }

        Matcher t24 = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").matcher(n);
        if (t24.find()) {
            int h = Integer.parseInt(t24.group(1));
            String min = t24.group(2);
            String mer = h < 12 ? "AM" : "PM";
            int dh = h % 12; if (dh == 0) dh = 12;
            return String.format("%02d:%s %s", dh, min, mer);
        }
        return null;
    }

    // ── location extraction ───────────────────────────────────────────────
    public static String[] extractLocations(String text, String bookingType) {
        if (text == null) return new String[]{null, null};

        Pattern full = Pattern.compile(
            "\\bfrom\\s+([A-Za-z][A-Za-z\\s]+?)\\s+to\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+for\\b|\\s+on\\b|$|,|\\.)",
            Pattern.CASE_INSENSITIVE);
        Matcher m = full.matcher(text);
        if (m.find()) return new String[]{cleanLocation(m.group(1)), cleanLocation(m.group(2))};

        Pattern simple = Pattern.compile(
            "\\b([A-Za-z][A-Za-z\\s]+?)\\s+to\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+\\d+\\s+(?:people|persons|passengers|travellers|travelers|tickets|seats|rooms)\\b|\\s+for\\b|\\s+on\\b|$|,|\\.)",
            Pattern.CASE_INSENSITIVE);
        m = simple.matcher(text);
        if (m.find()) return new String[]{cleanLocation(m.group(1)), cleanLocation(m.group(2))};

        Pattern dest = Pattern.compile(
            "\\bto\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+for\\b|\\s+on\\b|$|,|\\.)",
            Pattern.CASE_INSENSITIVE);
        m = dest.matcher(text);
        if (m.find()) return new String[]{null, cleanLocation(m.group(1))};

        Pattern city = Pattern.compile(
            "\\bin\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+for\\b|\\s+on\\b|$|,|\\.)",
            Pattern.CASE_INSENSITIVE);
        m = city.matcher(text);
        if (m.find()) return new String[]{null, cleanLocation(m.group(1))};

        return new String[]{null, null};
    }

    private static String cleanLocation(String raw) {
        if (raw == null) return null;
        String c = raw.replaceAll("(?i)\\b(for|with|on|at)\\b.*$","").trim().replaceAll("[,. ]+$","");
        if (c.isEmpty()) return null;
        return Character.toUpperCase(c.charAt(0)) + c.substring(1).toLowerCase();
    }

    // ── passenger extraction ──────────────────────────────────────────────
    public static Integer extractPassengers(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile(
            "\\b(\\d+)\\s+(?:people|persons|passengers|travellers|travelers|travels|traveler|traveller|tickets|seats|rooms)\\b",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return Math.max(1, Integer.parseInt(m.group(1)));

        if (text.trim().matches("\\d+")) return Math.max(1, Integer.parseInt(text.trim()));

        Map<String,Integer> words = new LinkedHashMap<>();
        words.put("one",1); words.put("two",2); words.put("three",3);
        words.put("four",4); words.put("five",5); words.put("six",6);
        for (Map.Entry<String,Integer> e : words.entrySet()) {
            if (Pattern.compile("\\b" + e.getKey() +
                "\\s+(?:people|persons|passengers|travellers|travelers|travels|traveler|traveller|tickets|seats|rooms)?\\b",
                Pattern.CASE_INSENSITIVE).matcher(text).find())
                return e.getValue();
        }
        return null;
    }

    // ── confirmation / discount / modify ─────────────────────────────────
    public static boolean isConfirmation(String text) {
        return Set.of("ok","okay","yes","confirm","proceed","continue").contains(normalize(text));
    }

    public static boolean wantsBetterPrice(String text) {
        String n = normalize(text);
        return n.contains("better price") || n.contains("best price") || n.contains("lower price")
            || n.contains("less price") || n.contains("discount") || n.contains("cheaper")
            || n.contains("reduce price") || n.contains("show better price")
            || n.contains("can you reduce");
    }

    public static boolean wantsModify(String text) {
        String n = normalize(text);
        return n.contains("modify") || n.contains("change") || n.contains("update date")
            || n.contains("update destination") || n.contains("update place");
    }

    // ── user display name ─────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public static String userDisplayName(Object user) {
        if (user == null) return "Traveler";
        if (user instanceof Map) {
            Map<String,Object> u = (Map<String,Object>) user;
            if (u.get("firstname") != null && !u.get("firstname").toString().isBlank())
                return u.get("firstname").toString();
            if (u.get("username") != null && !u.get("username").toString().isBlank())
                return u.get("username").toString();
            if (u.get("fullname") != null && !u.get("fullname").toString().isBlank())
                return u.get("fullname").toString();
        }
        return "Traveler";
    }

    // ── required fields ───────────────────────────────────────────────────
    public static List<String> requiredFields(String bookingType) {
        if (bookingType == null) return List.of("booking_type");
        if (Set.of("hotel","tour","insurance").contains(bookingType))
            return Arrays.asList("booking_type","to_location","travel_date","passengers");
        return Arrays.asList("booking_type","from_location","to_location","travel_date","travel_time","passengers");
    }

    public static String friendlyFieldName(String field, String bookingType) {
        if ("to_location".equals(field) && Set.of("hotel","tour","insurance").contains(bookingType))
            return "city or destination";
        return switch (field) {
            case "booking_type"  -> "what you want to book";
            case "from_location" -> "departure city";
            case "to_location"   -> "destination";
            case "travel_date"   -> "travel date";
            case "travel_time"   -> "travel time";
            case "passengers"    -> "number of travelers";
            default              -> field;
        };
    }

    // ── booking ref generator ─────────────────────────────────────────────
    public static String genRef() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder("TM");
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
