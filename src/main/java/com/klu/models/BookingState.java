package com.klu.models;

import java.util.HashMap;
import java.util.Map;

public class BookingState {

    public String  intent       = "chat";
    public String  bookingType;
    public String  fromLocation;
    public String  toLocation;
    public String  travelDate;
    public String  travelTime;
    public Integer passengers;
    public String  stage        = "collecting_details";
    public Double  quoteAmount;

    public static BookingState blank() { return new BookingState(); }

    public static BookingState copyOf(BookingState src) {
        BookingState s = new BookingState();
        if (src == null) return s;
        s.intent       = src.intent;
        s.bookingType  = src.bookingType;
        s.fromLocation = src.fromLocation;
        s.toLocation   = src.toLocation;
        s.travelDate   = src.travelDate;
        s.travelTime   = src.travelTime;
        s.passengers   = src.passengers;
        s.stage        = src.stage;
        s.quoteAmount  = src.quoteAmount;
        return s;
    }

    @SuppressWarnings("unchecked")
    public static BookingState fromMap(Object raw) {
        BookingState s = new BookingState();
        if (!(raw instanceof Map)) return s;
        Map<String, Object> m = (Map<String, Object>) raw;
        s.intent       = str(m, "intent",        "chat");
        s.bookingType  = str(m, "booking_type",  null);
        s.fromLocation = str(m, "from_location", null);
        s.toLocation   = str(m, "to_location",   null);
        s.travelDate   = str(m, "travel_date",   null);
        s.travelTime   = str(m, "travel_time",   null);
        s.stage        = str(m, "stage",         "collecting_details");
        Object p = m.get("passengers");
        if (p != null) { try { s.passengers = Integer.parseInt(p.toString()); } catch (Exception ignored) {} }
        Object q = m.get("quote_amount");
        if (q != null) { try { s.quoteAmount = Double.parseDouble(q.toString()); } catch (Exception ignored) {} }
        return s;
    }

    private static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return (v != null && !"null".equals(v.toString()) && !v.toString().isBlank()) ? v.toString() : def;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("intent",        intent);
        map.put("booking_type",  bookingType);
        map.put("from_location", fromLocation);
        map.put("to_location",   toLocation);
        map.put("travel_date",   travelDate);
        map.put("travel_time",   travelTime);
        map.put("passengers",    passengers);
        map.put("stage",         stage);
        map.put("quote_amount",  quoteAmount);
        return map;
    }
}
