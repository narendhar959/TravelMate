package util;

import java.util.*;
import java.util.regex.*;

public class AiUtils {

    static Set<String> GREETING = Set.of("hi","hello","hey");
    static Set<String> STOP = Set.of("bye","exit","quit","stop");

    static Map<String, List<String>> TYPES = Map.of(
            "flight", List.of("flight","plane"),
            "train", List.of("train"),
            "bus", List.of("bus"),
            "cab", List.of("cab","taxi"),
            "hotel", List.of("hotel"),
            "tour", List.of("tour")
    );

    static Map<String, Integer> PRICES = Map.of(
            "flight", 5499,
            "train", 1299,
            "bus", 899,
            "cab", 1499,
            "hotel", 3499,
            "tour", 6499
    );

    // ---------- BASIC ----------
    public static Map<String, Object> blankState() {
        Map<String, Object> m = new HashMap<>();
        m.put("intent", "chat");
        m.put("booking_type", null);
        m.put("from_location", null);
        m.put("to_location", null);
        m.put("travel_date", null);
        m.put("passengers", null);
        return m;
    }

    public static boolean isGreeting(String msg) {
        return GREETING.contains(msg.toLowerCase());
    }

    public static boolean isStop(String msg) {
        return STOP.contains(msg.toLowerCase());
    }

    public static boolean isUnreadable(String msg) {
        return msg.length() > 5 && !msg.matches(".*[aeiou].*");
    }

    // ---------- DETECT ----------
    public static String detectType(String msg) {
        String text = msg.toLowerCase();
        for (var entry : TYPES.entrySet()) {
            for (String k : entry.getValue()) {
                if (text.contains(k)) return entry.getKey();
            }
        }
        return null;
    }

    public static String extractFrom(String msg) {
        Matcher m = Pattern.compile("from (\\w+)").matcher(msg.toLowerCase());
        return m.find() ? m.group(1) : null;
    }

    public static String extractTo(String msg) {
        Matcher m = Pattern.compile("to (\\w+)").matcher(msg.toLowerCase());
        return m.find() ? m.group(1) : null;
    }

    public static Integer extractPassengers(String msg) {
        Matcher m = Pattern.compile("(\\d+)").matcher(msg);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    // ---------- MERGE ----------
    public static Map<String, Object> mergeState(Map<String, Object> state, String msg) {

        String type = detectType(msg);
        if (type != null) {
            state.put("booking_type", type);
            state.put("intent", "booking");
        }

        String from = extractFrom(msg);
        if (from != null) state.put("from_location", from);

        String to = extractTo(msg);
        if (to != null) state.put("to_location", to);

        Integer p = extractPassengers(msg);
        if (p != null) state.put("passengers", p);

        return state;
    }

    // ---------- VALIDATION ----------
    public static boolean hasMissing(Map<String, Object> state) {
        return state.get("booking_type") == null ||
               state.get("to_location") == null;
    }

    public static String askMissing(Map<String, Object> state) {
        if (state.get("booking_type") == null)
            return "What do you want to book?";
        if (state.get("to_location") == null)
            return "Where do you want to go?";
        return "Provide details";
    }

    // ---------- PRICE ----------
    public static double calculateAmount(Map<String, Object> state) {
        String type = (String) state.get("booking_type");
        int base = PRICES.getOrDefault(type, 1000);

        int passengers = (int) state.getOrDefault("passengers", 1);

        return base * passengers;
    }
}