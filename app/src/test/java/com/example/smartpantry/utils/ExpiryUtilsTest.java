package com.example.smartpantry.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

public class ExpiryUtilsTest {

    private static Integer days(String expiry, int y, int m, int d) {
        return ExpiryUtils.daysUntil(expiry, y, m, d);
    }

    @Test
    public void basicDifferences() {
        assertEquals(Integer.valueOf(0), days("2026-05-10", 2026, 5, 10));
        assertEquals(Integer.valueOf(1), days("2026-05-11", 2026, 5, 10));
        assertEquals(Integer.valueOf(-1), days("2026-05-09", 2026, 5, 10));
        assertEquals(Integer.valueOf(365), days("2027-05-10", 2026, 5, 10));
    }

    @Test
    public void monthYearAndLeapBoundaries() {
        assertEquals(Integer.valueOf(1), days("2027-01-01", 2026, 12, 31));
        assertEquals(Integer.valueOf(1), days("2028-02-29", 2028, 2, 28)); // leap year
        assertEquals(Integer.valueOf(1), days("2028-03-01", 2028, 2, 29));
        assertEquals(Integer.valueOf(1), days("2027-03-01", 2027, 2, 28)); // non-leap
        assertEquals(Integer.valueOf(366), days("2029-01-01", 2028, 1, 1)); // 2028 has 366 days
        assertEquals(Integer.valueOf(1), days("2100-03-01", 2100, 2, 28));  // 2100 is not a leap year
        assertEquals(Integer.valueOf(1), days("2000-03-01", 2000, 2, 29));  // 2000 is a leap year
    }

    @Test
    public void invalidOrMissingDatesGiveNull() {
        assertNull(days(null, 2026, 5, 10));
        assertNull(days("", 2026, 5, 10));
        assertNull(days("garbage", 2026, 5, 10));
        assertNull(days("2026-5-1", 2026, 5, 10));
        assertNull(days("2027-02-29", 2026, 5, 10));
        assertNull(days("2026-13-01", 2026, 5, 10));
        assertNull(days("2026-04-31", 2026, 5, 10));
        assertNull(days("2026-00-10", 2026, 5, 10));
        assertNull(days("2026-05-00", 2026, 5, 10));
        assertNull(days("2026-05-10T00:00", 2026, 5, 10));
    }

    @Test
    public void statusThresholds() {
        assertEquals(ExpiryUtils.Status.NONE, ExpiryUtils.statusFor(null));
        assertEquals(ExpiryUtils.Status.EXPIRED, ExpiryUtils.statusFor(-1));
        assertEquals(ExpiryUtils.Status.SOON, ExpiryUtils.statusFor(0));
        assertEquals(ExpiryUtils.Status.SOON, ExpiryUtils.statusFor(3));
        assertEquals(ExpiryUtils.Status.OK, ExpiryUtils.statusFor(4));
    }

    /** Runs the check under a time zone, using a real Calendar late in the evening. */
    private static void withZone(String zoneId, Runnable body) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            body.run();
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static Calendar at(int y, int m, int d, int hour, int min) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, hour, min, 0);
        return c;
    }

    @Test
    public void daylightSavingSpringForwardNewYork() {
        withZone("America/New_York", () -> {
            // 2026-03-08 is only 23 hours long.
            assertEquals(Integer.valueOf(2), ExpiryUtils.daysUntil("2026-03-09", at(2026, 3, 7, 0, 30)));
            assertEquals(Integer.valueOf(1), ExpiryUtils.daysUntil("2026-03-09", at(2026, 3, 8, 23, 59)));
            assertEquals(Integer.valueOf(0), ExpiryUtils.daysUntil("2026-03-08", at(2026, 3, 8, 3, 30)));
            assertEquals(Integer.valueOf(1), ExpiryUtils.daysUntil("2026-03-08", at(2026, 3, 7, 23, 59)));
        });
    }

    @Test
    public void daylightSavingFallBackNewYork() {
        withZone("America/New_York", () -> {
            // 2026-11-01 is 25 hours long.
            assertEquals(Integer.valueOf(1), ExpiryUtils.daysUntil("2026-11-02", at(2026, 11, 1, 0, 30)));
            assertEquals(Integer.valueOf(0), ExpiryUtils.daysUntil("2026-11-01", at(2026, 11, 1, 23, 59)));
            assertEquals(Integer.valueOf(2), ExpiryUtils.daysUntil("2026-11-02", at(2026, 10, 31, 0, 0)));
        });
    }

    @Test
    public void daylightSavingOtherZones() {
        // Southern hemisphere, and a 30-minute shift.
        withZone("Australia/Sydney", () -> {
            assertEquals(Integer.valueOf(2), ExpiryUtils.daysUntil("2026-10-06", at(2026, 10, 4, 0, 10))); // DST starts 4 Oct
            assertEquals(Integer.valueOf(2), ExpiryUtils.daysUntil("2026-04-07", at(2026, 4, 5, 23, 50))); // DST ends 5 Apr
        });
        withZone("Australia/Lord_Howe", () ->
                assertEquals(Integer.valueOf(1), ExpiryUtils.daysUntil("2026-10-05", at(2026, 10, 4, 12, 0))));
        // Durban (no DST).
        withZone("Africa/Johannesburg", () ->
                assertEquals(Integer.valueOf(3), ExpiryUtils.daysUntil("2026-09-30", at(2026, 9, 27, 23, 59))));
    }

    @Test
    public void resultIndependentOfTimeZone() {
        String[] zones = {"UTC", "America/Los_Angeles", "Asia/Kolkata", "Pacific/Kiritimati", "Pacific/Pago_Pago"};
        for (String z : zones) {
            withZone(z, () ->
                    assertEquals(Integer.valueOf(5), ExpiryUtils.daysUntil("2026-06-15", at(2026, 6, 10, 23, 59))));
        }
    }
}
