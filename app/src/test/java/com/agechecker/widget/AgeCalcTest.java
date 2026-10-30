package com.agechecker.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;

public class AgeCalcTest {
    private static LocalDate d(int y, int m, int day) { return LocalDate.of(y, m, day); }

    @Test public void turns21Today_isEligible() {
        assertEquals(21, AgeCalc.completedYears(d(2004, 10, 30), d(2025, 10, 30)));
        assertTrue(AgeCalc.isAtLeast21(d(2004, 10, 30), d(2025, 10, 30)));
    }

    @Test public void dayBefore21stBirthday_isNotEligible() {
        assertEquals(20, AgeCalc.completedYears(d(2004, 10, 30), d(2025, 10, 29)));
        assertFalse(AgeCalc.isAtLeast21(d(2004, 10, 30), d(2025, 10, 29)));
    }

    @Test public void birthdayLaterThisYear_notYetCounted() {
        assertEquals(29, AgeCalc.completedYears(d(1995, 12, 7), d(2025, 6, 1)));
    }

    @Test public void monthBoundary() {
        assertEquals(20, AgeCalc.completedYears(d(2000, 1, 31), d(2021, 1, 30)));
        assertEquals(21, AgeCalc.completedYears(d(2000, 1, 31), d(2021, 1, 31)));
    }

    @Test public void leapDayBirthday_nonLeapYear_countsFromMarch1() {
        assertEquals(20, AgeCalc.completedYears(d(2004, 2, 29), d(2025, 2, 28)));
        assertEquals(21, AgeCalc.completedYears(d(2004, 2, 29), d(2025, 3, 1)));
    }

    @Test public void leapDayBirthday_leapYear() {
        assertEquals(20, AgeCalc.completedYears(d(2004, 2, 29), d(2024, 2, 29)));
        assertEquals(19, AgeCalc.completedYears(d(2004, 2, 29), d(2024, 2, 28)));
    }

    @Test public void bornToday_isZero() {
        assertEquals(0, AgeCalc.completedYears(d(2025, 5, 5), d(2025, 5, 5)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void futureDate_throws() {
        AgeCalc.completedYears(d(2030, 1, 1), d(2025, 1, 1));
    }
}
