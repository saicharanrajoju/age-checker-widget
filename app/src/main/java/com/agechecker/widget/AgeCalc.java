package com.agechecker.widget;

import java.time.LocalDate;
import java.time.Period;

/** Pure age arithmetic. No Android dependencies so it can be unit tested on the JVM. */
public final class AgeCalc {
    public static final int THRESHOLD = 21;

    private AgeCalc() {}

    /**
     * Completed years between {@code dob} and {@code today}.
     * Uses java.time.Period, so a Feb 29 birthday is treated as reached on Mar 1 in non-leap years.
     *
     * @throws IllegalArgumentException if dob is after today
     */
    public static int completedYears(LocalDate dob, LocalDate today) {
        if (dob.isAfter(today)) throw new IllegalArgumentException("Date of birth is in the future");
        return Period.between(dob, today).getYears();
    }

    /** Inclusive: someone who turns 21 today is eligible. */
    public static boolean isAtLeast21(LocalDate dob, LocalDate today) {
        return completedYears(dob, today) >= THRESHOLD;
    }
}
