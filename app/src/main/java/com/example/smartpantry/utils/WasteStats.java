package com.example.smartpantry.utils;

import com.example.smartpantry.models.FoodEvent;
import com.example.smartpantry.models.PantryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Dashboard logic with no Android dependencies: builds food-log events from pantry actions
 * and totals them per month.
 * Definitions used by the dashboard:
 *  - Saved  = value of food used while it was still in date.
 *  - Wasted = value of food that expired or was thrown away.
 *  - Spent  = prices entered when items were added to the pantry.
 * Items added without a price count in the item totals but add 0 to any money total.
 */
public class WasteStats {

    public static class MonthStats {
        public final int year;   // 0 for an all-time total
        public final int month;  // 1-12, 0 for an all-time total
        public double spent;
        public double saved;
        public double wasted;
        public int usedTotal;
        public int usedBeforeExpiry;
        public int expiredCount;
        public int discardedCount;

        MonthStats(int year, int month) {
            this.year = year;
            this.month = month;
        }

        public int wastedCount() { return expiredCount + discardedCount; }

        /** Share of finished items that were wasted, 0..1 (0 when nothing has been finished). */
        public double wasteRate() {
            int finished = usedTotal + wastedCount();
            return finished == 0 ? 0 : (double) wastedCount() / finished;
        }
    }

    // ---------- building events ----------

    public static String dateString(int y, int m, int d) {
        return String.format(Locale.ROOT, "%04d-%02d-%02d", y, m, d);
    }

    /** Price per unit of the item, or null when no usable price is stored. */
    public static Double unitPrice(PantryItem item) {
        if (item.getPrice() == null || item.getQuantity() <= 0) return null;
        return item.getPrice() / item.getQuantity();
    }

    public static double valueOf(PantryItem item, double quantity) {
        Double unit = unitPrice(item);
        return unit == null ? 0 : unit * quantity;
    }

    /**
     * Price left on an item after part of it is used. The stored price is the total for the
     * quantity currently in the pantry, so it shrinks in proportion (null if no price).
     */
    public static Double priceAfterUse(PantryItem item, double newQuantity) {
        if (item.getPrice() == null || item.getQuantity() <= 0) return null;
        return item.getPrice() * Math.max(0, newQuantity) / item.getQuantity();
    }

    /** A purchase is only logged when the user entered a price. */
    public static FoodEvent purchaseEvent(PantryItem item, int y, int m, int d) {
        if (item.getPrice() == null || item.getPrice() <= 0) return null;
        return new FoodEvent(0, FoodEvent.PURCHASED, item.getDisplayName(), item.getQuantity(),
                item.getUnit(), item.getPrice(), dateString(y, m, d), false);
    }

    /** One USED event per pantry change from PantryCooker. Plan (quantity used = old - new). */
    public static List<FoodEvent> cookEvents(List<PantryCooker.Change> changes, int y, int m, int d) {
        List<FoodEvent> out = new ArrayList<>();
        for (PantryCooker.Change c : changes) {
            double used = c.item.getQuantity() - c.newQuantity;
            if (used <= 0) continue;
            Integer days = ExpiryUtils.daysUntil(c.item.getExpiryDate(), y, m, d);
            boolean before = days != null && days >= 0;
            out.add(new FoodEvent(0, FoodEvent.USED, c.item.getDisplayName(), used, c.item.getUnit(),
                    valueOf(c.item, used), dateString(y, m, d), before));
        }
        return out;
    }

    /** Items past their expiry date whose expiry has not been logged yet. */
    public static List<PantryItem> expiredUnlogged(List<PantryItem> pantry, int y, int m, int d) {
        List<PantryItem> out = new ArrayList<>();
        for (PantryItem item : pantry) {
            if (item.isExpiryLogged()) continue;
            Integer days = ExpiryUtils.daysUntil(item.getExpiryDate(), y, m, d);
            if (days != null && days < 0) out.add(item);
        }
        return out;
    }

    /** EXPIRED events for items past their date that have not been logged yet. */
    public static List<FoodEvent> expiredSweep(List<PantryItem> pantry, int y, int m, int d) {
        List<FoodEvent> out = new ArrayList<>();
        for (PantryItem item : expiredUnlogged(pantry, y, m, d)) {
            out.add(new FoodEvent(0, FoodEvent.EXPIRED, item.getDisplayName(), item.getQuantity(),
                    item.getUnit(), valueOf(item, item.getQuantity()), dateString(y, m, d), false));
        }
        return out;
    }

    /** The user finished the whole item without going through a recipe. */
    public static FoodEvent usedUpEvent(PantryItem item, int y, int m, int d) {
        if (item.getQuantity() <= 0) return null;
        Integer days = ExpiryUtils.daysUntil(item.getExpiryDate(), y, m, d);
        return new FoodEvent(0, FoodEvent.USED, item.getDisplayName(), item.getQuantity(), item.getUnit(),
                valueOf(item, item.getQuantity()), dateString(y, m, d), days != null && days >= 0);
    }

    /**
     * Event for removing an item without cooking it: DISCARDED if it was still in date,
     * EXPIRED if it had passed its date. Returns null when its expiry was already logged.
     */
    public static FoodEvent removalEvent(PantryItem item, int y, int m, int d) {
        Integer days = ExpiryUtils.daysUntil(item.getExpiryDate(), y, m, d);
        boolean expired = days != null && days < 0;
        if (expired && item.isExpiryLogged()) return null;
        return new FoodEvent(0, expired ? FoodEvent.EXPIRED : FoodEvent.DISCARDED,
                item.getDisplayName(), item.getQuantity(), item.getUnit(),
                valueOf(item, item.getQuantity()), dateString(y, m, d), false);
    }

    // ---------- totals ----------

    /** The last `months` months ending at (endYear, endMonth), oldest first, empty months included. */
    public static List<MonthStats> monthly(List<FoodEvent> events, int endYear, int endMonth, int months) {
        List<MonthStats> out = new ArrayList<>();
        int y = endYear;
        int m = endMonth;
        for (int i = 0; i < months; i++) {
            out.add(0, new MonthStats(y, m));
            m--;
            if (m == 0) { m = 12; y--; }
        }
        for (FoodEvent e : events) {
            int[] ym = yearMonth(e.getDate());
            if (ym == null) continue;
            for (MonthStats s : out) {
                if (s.year == ym[0] && s.month == ym[1]) {
                    add(s, e);
                    break;
                }
            }
        }
        return out;
    }

    public static MonthStats total(List<FoodEvent> events) {
        MonthStats s = new MonthStats(0, 0);
        for (FoodEvent e : events) {
            if (yearMonth(e.getDate()) != null) add(s, e);
        }
        return s;
    }

    private static void add(MonthStats s, FoodEvent e) {
        switch (e.getType()) {
            case FoodEvent.PURCHASED:
                s.spent += e.getValue();
                break;
            case FoodEvent.USED:
                s.usedTotal++;
                if (e.isBeforeExpiry()) {
                    s.usedBeforeExpiry++;
                    s.saved += e.getValue();
                }
                break;
            case FoodEvent.EXPIRED:
                s.expiredCount++;
                s.wasted += e.getValue();
                break;
            case FoodEvent.DISCARDED:
                s.discardedCount++;
                s.wasted += e.getValue();
                break;
            default:
                break;
        }
    }

    /** Latest expired/discarded events first (newest date, then newest entry), at most `limit`. */
    public static List<FoodEvent> recentWaste(List<FoodEvent> events, int limit) {
        List<FoodEvent> waste = new ArrayList<>();
        for (FoodEvent e : events) {
            if (e.getType() == FoodEvent.EXPIRED || e.getType() == FoodEvent.DISCARDED) waste.add(e);
        }
        Collections.sort(waste, (a, b) -> {
            int byDate = String.valueOf(b.getDate()).compareTo(String.valueOf(a.getDate()));
            return byDate != 0 ? byDate : Long.compare(b.getId(), a.getId());
        });
        return waste.size() > limit ? new ArrayList<>(waste.subList(0, limit)) : waste;
    }

    /** {year, month} from "yyyy-MM-dd", or null if malformed. */
    private static int[] yearMonth(String date) {
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) return null;
        int month = Integer.parseInt(date.substring(5, 7));
        if (month < 1 || month > 12) return null;
        return new int[]{Integer.parseInt(date.substring(0, 4)), month};
    }
}
