package com.example.smartpantry.utils;

/**
 * Expiry-date logic with no Android or time-zone dependency.
 * Dates are converted to a day number (days since 1970-01-01) from their year/month/day
 * fields, so daylight-saving changes (23h/25h days) can never shift the result.
 */
public class ExpiryUtils {

    public static final int SOON_THRESHOLD_DAYS = 3;

    public enum Status { NONE, OK, SOON, EXPIRED }

    /** Days from today until expiry (negative = already expired), or null if the date is missing/invalid. */
    public static Integer daysUntil(String isoExpiry, int todayYear, int todayMonth, int todayDay) {
        Long expiry = parseToDayNumber(isoExpiry);
        if (expiry == null || !isValidDate(todayYear, todayMonth, todayDay)) return null;
        return (int) (expiry - toDayNumber(todayYear, todayMonth, todayDay));
    }

    /** Convenience for Android code: pass Calendar.getInstance() (month is 0-based there). */
    public static Integer daysUntil(String isoExpiry, java.util.Calendar today) {
        return daysUntil(isoExpiry, today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH) + 1, today.get(java.util.Calendar.DAY_OF_MONTH));
    }

    public static Status statusFor(Integer daysUntil) {
        if (daysUntil == null) return Status.NONE;
        if (daysUntil < 0) return Status.EXPIRED;
        if (daysUntil <= SOON_THRESHOLD_DAYS) return Status.SOON;
        return Status.OK;
    }

    /** Strict "yyyy-MM-dd" parse; returns null for anything else (including 2027-02-29). */
    static Long parseToDayNumber(String iso) {
        if (iso == null) return null;
        String s = iso.trim();
        if (!s.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        int y = Integer.parseInt(s.substring(0, 4));
        int m = Integer.parseInt(s.substring(5, 7));
        int d = Integer.parseInt(s.substring(8, 10));
        if (!isValidDate(y, m, d)) return null;
        return toDayNumber(y, m, d);
    }

    static boolean isValidDate(int y, int m, int d) {
        if (m < 1 || m > 12 || d < 1) return false;
        return d <= daysInMonth(y, m);
    }

    private static int daysInMonth(int y, int m) {
        switch (m) {
            case 2: return isLeap(y) ? 29 : 28;
            case 4: case 6: case 9: case 11: return 30;
            default: return 31;
        }
    }

    private static boolean isLeap(int y) {
        return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
    }

    /** Civil date to days since 1970-01-01 (proleptic Gregorian). */
    private static long toDayNumber(int y, int m, int d) {
        long yy = m <= 2 ? y - 1 : y;
        long era = Math.floorDiv(yy, 400);
        long yoe = yy - era * 400;
        long doy = (153L * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}
