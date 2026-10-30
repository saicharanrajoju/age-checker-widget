package com.agechecker.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import org.junit.Test;

public class DobParserTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    // ---- manual
    @Test public void eightDigits() {
        assertEquals(LocalDate.of(2000, 12, 7), DobParser.parseManual("12072000", TODAY).date);
    }

    @Test public void slashed() {
        assertEquals(LocalDate.of(2000, 12, 7), DobParser.parseManual("12/07/2000", TODAY).date);
        assertEquals(LocalDate.of(2000, 1, 2), DobParser.parseManual("1/2/2000", TODAY).date);
    }

    @Test public void invalidCalendarDates_rejected() {
        assertNotNull(DobParser.parseManual("02302001", TODAY).error);
        assertNotNull(DobParser.parseManual("02/29/2001", TODAY).error);
        assertNotNull(DobParser.parseManual("13/01/2000", TODAY).error);
        assertNotNull(DobParser.parseManual("00/10/2000", TODAY).error);
        assertNotNull(DobParser.parseManual("04/31/2000", TODAY).error);
    }

    @Test public void leapDay_accepted() {
        assertEquals(LocalDate.of(2004, 2, 29), DobParser.parseManual("02292004", TODAY).date);
    }

    @Test public void wrongLength_rejected() {
        assertNotNull(DobParser.parseManual("1207200", TODAY).error);
        assertNotNull(DobParser.parseManual("", TODAY).error);
        assertNotNull(DobParser.parseManual("abc", TODAY).error);
    }

    @Test public void future_rejected() {
        assertNotNull(DobParser.parseManual("10/31/2026", TODAY).error);
        assertNull(DobParser.parseManual("10/30/2026", TODAY).error);
    }

    @Test public void tooOld_rejected() {
        assertNotNull(DobParser.parseManual("01/01/1899", TODAY).error);
    }

    // ---- typing format
    @Test public void formatTyping() {
        assertEquals("", DobParser.formatTyping(""));
        assertEquals("12", DobParser.formatTyping("12"));
        assertEquals("12/0", DobParser.formatTyping("120"));
        assertEquals("12/07", DobParser.formatTyping("1207"));
        assertEquals("12/07/2", DobParser.formatTyping("12072"));
        assertEquals("12/07/2000", DobParser.formatTyping("12072000"));
        assertEquals("12/07/2000", DobParser.formatTyping("120720001"));
        assertEquals("12/07/2000", DobParser.formatTyping("12/07/2000"));
        assertEquals("01/02/2000", DobParser.formatTyping("1/2/2000"));
    }

    // ---- spoken
    private static void spoken(String said, int y, int m, int d) {
        assertEquals(said, LocalDate.of(y, m, d), DobParser.parseSpoken(said));
    }

    @Test public void spokenExamplesFromSpec() {
        spoken("December seven two thousand", 2000, 12, 7);
        spoken("December seventh two thousand", 2000, 12, 7);
        spoken("December 7 2000", 2000, 12, 7);
        spoken("November eighth 2006", 2006, 11, 8);
        spoken("January first nineteen ninety-nine", 1999, 1, 1);
    }

    @Test public void spokenVariants() {
        spoken("December 7th, 2000", 2000, 12, 7);
        spoken("march third twenty oh six", 2006, 3, 3);
        spoken("June twenty-first nineteen eighty eight", 1988, 6, 21);
        spoken("the 21st of June 1988", 1988, 6, 21);
        spoken("the seventh of december two thousand", 2000, 12, 7);
        spoken("July 4 two thousand and five", 2005, 7, 4);
        spoken("Sept 30 twenty twenty", 2020, 9, 30);
        spoken("12/7/2000", 2000, 12, 7);
    }

    @Test public void spokenNonsense_returnsNull() {
        assertNull(DobParser.parseSpoken("hello there"));
        assertNull(DobParser.parseSpoken("February thirtieth two thousand"));
        assertNull(DobParser.parseSpoken("December"));
        assertNull(DobParser.parseSpoken(null));
    }
}
