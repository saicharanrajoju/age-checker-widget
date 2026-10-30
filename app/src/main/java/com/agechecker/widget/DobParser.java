package com.agechecker.widget;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses typed and spoken dates of birth into validated LocalDates. No Android dependencies. */
public final class DobParser {
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.US);
    public static final int MIN_YEAR = 1900;

    public static final class Parse {
        public final LocalDate date;
        public final String error;

        private Parse(LocalDate date, String error) {
            this.date = date;
            this.error = error;
        }

        static Parse ok(LocalDate d) { return new Parse(d, null); }
        static Parse fail(String e) { return new Parse(null, e); }
    }

    private static final Pattern EIGHT = Pattern.compile("\\d{8}");
    private static final Pattern SLASHED = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern SPOKEN_SLASHED = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*/\\s*(\\d{4})");
    private static final Pattern DAY_DIGITS = Pattern.compile("(\\d{1,2})(st|nd|rd|th)?");

    private static final String[] MONTHS = {"january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"};

    private static final Map<String, Integer> CARD_UNITS = new HashMap<>();
    private static final Map<String, Integer> ORD_UNITS = new HashMap<>();
    private static final Map<String, Integer> CARD_TEENS = new HashMap<>();
    private static final Map<String, Integer> ORD_TEENS = new HashMap<>();
    private static final Map<String, Integer> CARD_TENS = new HashMap<>();
    private static final Map<String, Integer> ORD_TENS = new HashMap<>();

    static {
        String[] cu = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        String[] ou = {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"};
        for (int i = 0; i < 9; i++) {
            CARD_UNITS.put(cu[i], i + 1);
            ORD_UNITS.put(ou[i], i + 1);
        }
        String[] ct = {"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
        String[] ot = {"tenth", "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth"};
        for (int i = 0; i < 10; i++) {
            CARD_TEENS.put(ct[i], i + 10);
            ORD_TEENS.put(ot[i], i + 10);
        }
        String[] tens = {"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
        for (int i = 0; i < tens.length; i++) CARD_TENS.put(tens[i], (i + 2) * 10);
        ORD_TENS.put("twentieth", 20);
        ORD_TENS.put("thirtieth", 30);
    }

    private DobParser() {}

    // ---------------------------------------------------------------- manual entry

    /** Accepts MMDDYYYY or MM/DD/YYYY (single-digit month/day allowed with slashes). */
    public static Parse parseManual(String raw, LocalDate today) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return Parse.fail("Enter your date of birth as MM/DD/YYYY.");
        int m, d, y;
        Matcher sl = SLASHED.matcher(s);
        if (EIGHT.matcher(s).matches()) {
            m = Integer.parseInt(s.substring(0, 2));
            d = Integer.parseInt(s.substring(2, 4));
            y = Integer.parseInt(s.substring(4, 8));
        } else if (sl.matches()) {
            m = Integer.parseInt(sl.group(1));
            d = Integer.parseInt(sl.group(2));
            y = Integer.parseInt(sl.group(3));
        } else {
            return Parse.fail("Use 8 digits: MMDDYYYY or MM/DD/YYYY.");
        }
        return validate(m, d, y, today);
    }

    static Parse validate(int m, int d, int y, LocalDate today) {
        if (m < 1 || m > 12) return Parse.fail("Month must be between 01 and 12.");
        LocalDate date;
        try {
            date = LocalDate.of(y, m, d);
        } catch (DateTimeException e) {
            return Parse.fail("That date doesn't exist on the calendar.");
        }
        if (y < MIN_YEAR) return Parse.fail("Year must be " + MIN_YEAR + " or later.");
        if (date.isAfter(today)) return Parse.fail("Date of birth can't be in the future.");
        return Parse.ok(date);
    }

    /** Live formatting while typing: digits only, slashes inserted after MM and DD. */
    public static String formatTyping(String raw) {
        if (raw == null) return "";
        Matcher sl = SLASHED.matcher(raw.trim());
        if (sl.matches()) { // pasted "1/2/2000" -> "01/02/2000"
            return String.format(Locale.US, "%02d/%02d/%s",
                    Integer.parseInt(sl.group(1)), Integer.parseInt(sl.group(2)), sl.group(3));
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length() && digits.length() < 8; i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') digits.append(ch);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 2 || i == 4) out.append('/');
            out.append(digits.charAt(i));
        }
        return out.toString();
    }

    // ---------------------------------------------------------------- spoken entry

    /**
     * Interprets recognizer text such as "December seventh two thousand", "November 8th 2006",
     * "January first nineteen ninety-nine" or "the 21st of June 1988". Returns null if unsure.
     * Only parses the date; validation (future, min year) happens via {@link #validate}.
     */
    public static LocalDate parseSpoken(String raw) {
        if (raw == null) return null;
        String s = raw.toLowerCase(Locale.US).replaceAll("(\\d),(\\d{3})", "$1$2");
        Matcher sl = SPOKEN_SLASHED.matcher(s);
        if (sl.find()) {
            return plausible(Integer.parseInt(sl.group(3)), Integer.parseInt(sl.group(1)), Integer.parseInt(sl.group(2)));
        }
        s = s.replaceAll("[^a-z0-9 ]", " ").trim();
        if (s.isEmpty()) return null;
        String[] tok = s.split("\\s+");

        int mi = -1, month = 0;
        for (int i = 0; i < tok.length; i++) {
            int mm = monthOf(tok[i]);
            if (mm > 0) {
                mi = i;
                month = mm;
                break;
            }
        }
        if (mi < 0) return null;

        int[] day = null;
        List<String> yearTokens;
        // Prefer "the seventh of December ..." when a day appears before the month.
        // Otherwise the first word of "two thousand" can be mistaken for day 2.
        if (mi > 0) {
            int[] before = readDay(tok, 0);
            if (before != null && before[1] <= mi) {
                boolean onlyJoiners = true;
                for (int i = before[1]; i < mi; i++) {
                    if (!tok[i].equals("of") && !tok[i].equals("the")) onlyJoiners = false;
                }
                if (onlyJoiners) day = before;
            }
        }
        if (day != null) {
            yearTokens = slice(tok, mi + 1, tok.length);
        } else {
            day = readDay(tok, mi + 1);
            if (day == null) return null;
            yearTokens = slice(tok, day[1], tok.length);
        }
        Integer year = readYear(yearTokens);
        if (year == null) return null;
        return plausible(year, month, day[0]);
    }

    private static LocalDate plausible(int y, int m, int d) {
        if (y < MIN_YEAR || y > 2100) return null;
        try {
            return LocalDate.of(y, m, d);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static int monthOf(String t) {
        if (t.length() < 3) return 0;
        for (int i = 0; i < MONTHS.length; i++) if (MONTHS[i].startsWith(t)) return i + 1;
        return 0;
    }

    private static List<String> slice(String[] t, int from, int to) {
        List<String> out = new ArrayList<>();
        for (int i = from; i < to; i++) out.add(t[i]);
        return out;
    }

    /** Returns {day, nextIndex} or null. */
    private static int[] readDay(String[] t, int i) {
        while (i < t.length && (t[i].equals("the") || t[i].equals("of"))) i++;
        if (i >= t.length) return null;
        String w = t[i];
        int v, next = i + 1;
        Matcher m = DAY_DIGITS.matcher(w);
        if (m.matches()) {
            v = Integer.parseInt(m.group(1));
        } else if (ORD_TENS.containsKey(w)) {
            v = ORD_TENS.get(w);
        } else if (CARD_TENS.containsKey(w)) {
            v = CARD_TENS.get(w);
            if (next < t.length) {
                Integer u = unit(t[next]);
                if (u != null) {
                    v += u;
                    next++;
                }
            }
        } else if (CARD_TEENS.containsKey(w) || ORD_TEENS.containsKey(w)) {
            v = CARD_TEENS.containsKey(w) ? CARD_TEENS.get(w) : ORD_TEENS.get(w);
        } else if (unit(w) != null) {
            v = unit(w);
        } else {
            return null;
        }
        if (v < 1 || v > 31) return null;
        return new int[]{v, next};
    }

    private static Integer unit(String w) {
        Integer v = CARD_UNITS.get(w);
        return v != null ? v : ORD_UNITS.get(w);
    }

    static Integer readYear(List<String> in) {
        List<String> t = new ArrayList<>();
        for (String s : in) {
            if (!s.equals("and") && !s.equals("the") && !s.equals("of") && !s.equals("in")) t.add(s);
        }
        if (t.isEmpty()) return null;
        if (t.size() == 1 && t.get(0).matches("\\d{4}")) return Integer.valueOf(t.get(0));

        int th = t.indexOf("thousand");
        if (th >= 0) { // "two thousand", "two thousand six"
            Integer before = number(t.subList(0, th));
            Integer after = th + 1 == t.size() ? Integer.valueOf(0) : number(t.subList(th + 1, t.size()));
            if (before == null || after == null || after > 999) return null;
            return before * 1000 + after;
        }
        int[] first = readTwo(t, 0); // "nineteen", "twenty", "19"
        if (first == null) return null;
        int i = first[1];
        if (i < t.size() && t.get(i).equals("hundred")) { // "nineteen hundred five"
            Integer rest = i + 1 == t.size() ? Integer.valueOf(0) : number(t.subList(i + 1, t.size()));
            if (rest == null || rest > 99) return null;
            return first[0] * 100 + rest;
        }
        int[] second = readTwo(t, i); // "ninety nine", "oh six", "99"
        if (second == null || second[1] != t.size()) return null;
        return first[0] * 100 + second[0];
    }

    /** Reads a 0–99 chunk. Returns {value, nextIndex} or null. */
    private static int[] readTwo(List<String> t, int i) {
        if (i >= t.size()) return null;
        String w = t.get(i);
        if (w.matches("\\d{1,2}")) return new int[]{Integer.parseInt(w), i + 1};
        if (w.equals("oh") || w.equals("o") || w.equals("zero")) {
            if (i + 1 < t.size() && CARD_UNITS.containsKey(t.get(i + 1))) {
                return new int[]{CARD_UNITS.get(t.get(i + 1)), i + 2};
            }
            return null;
        }
        if (CARD_TEENS.containsKey(w)) return new int[]{CARD_TEENS.get(w), i + 1};
        if (CARD_TENS.containsKey(w)) {
            int v = CARD_TENS.get(w);
            if (i + 1 < t.size() && CARD_UNITS.containsKey(t.get(i + 1))) {
                return new int[]{v + CARD_UNITS.get(t.get(i + 1)), i + 2};
            }
            return new int[]{v, i + 1};
        }
        if (CARD_UNITS.containsKey(w)) return new int[]{CARD_UNITS.get(w), i + 1};
        return null;
    }

    /** Reads a whole number below 1000 that must consume every token. */
    private static Integer number(List<String> t) {
        if (t.isEmpty()) return null;
        if (t.size() == 1 && t.get(0).matches("\\d{1,3}")) return Integer.valueOf(t.get(0));
        int[] c = readTwo(t, 0);
        if (c == null) return null;
        int total = c[0], i = c[1];
        if (i < t.size() && t.get(i).equals("hundred")) {
            total *= 100;
            i++;
            if (i < t.size()) {
                int[] r = readTwo(t, i);
                if (r == null) return null;
                total += r[0];
                i = r[1];
            }
        }
        return i == t.size() ? total : null;
    }
}
