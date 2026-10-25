package com.example.smartpantry.utils;

import static org.junit.Assert.*;

import com.example.smartpantry.models.FoodEvent;
import com.example.smartpantry.models.PantryItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WasteStatsTest {

    private static PantryItem item(String name, double qty, String unit, String expiry, Double price) {
        PantryItem p = new PantryItem(1, PantryItem.normalize(name), name, qty, unit, expiry);
        p.setPrice(price);
        return p;
    }

    private static FoodEvent ev(int type, double value, String date, boolean before) {
        return new FoodEvent(0, type, "x", 1, "pcs", value, date, before);
    }

    // ---------- prices and values ----------

    @Test
    public void unitPriceAndValue() {
        PantryItem milk = item("Milk", 2, "l", null, 30.0);
        assertEquals(15.0, WasteStats.unitPrice(milk), 1e-9);
        assertEquals(7.5, WasteStats.valueOf(milk, 0.5), 1e-9);
    }

    @Test
    public void noPriceOrZeroQuantityMeansNoValue() {
        assertNull(WasteStats.unitPrice(item("Milk", 2, "l", null, null)));
        assertEquals(0, WasteStats.valueOf(item("Milk", 2, "l", null, null), 1), 0);
        assertNull(WasteStats.unitPrice(item("Milk", 0, "l", null, 30.0)));
    }

    @Test
    public void purchaseOnlyLoggedWithPrice() {
        assertNull(WasteStats.purchaseEvent(item("Milk", 2, "l", null, null), 2026, 5, 10));
        assertNull(WasteStats.purchaseEvent(item("Milk", 2, "l", null, 0.0), 2026, 5, 10));
        FoodEvent e = WasteStats.purchaseEvent(item("Milk", 2, "l", null, 30.0), 2026, 5, 10);
        assertEquals(FoodEvent.PURCHASED, e.getType());
        assertEquals(30.0, e.getValue(), 1e-9);
        assertEquals("2026-05-10", e.getDate());
    }

    @Test
    public void dateStringIsZeroPadded() {
        assertEquals("2026-01-05", WasteStats.dateString(2026, 1, 5));
    }

    // ---------- cooking ----------

    private static PantryCooker.Change change(PantryItem item, double newQty) {
        return new PantryCooker.Change(item, newQty, newQty <= 0);
    }

    @Test
    public void cookingPartOfAnItemValuesOnlyWhatWasUsed() {
        PantryItem flour = item("Flour", 1000, "g", "2026-05-20", 20.0); // R0.02 per g
        List<FoodEvent> out = WasteStats.cookEvents(Arrays.asList(change(flour, 750)), 2026, 5, 10);
        assertEquals(1, out.size());
        assertEquals(FoodEvent.USED, out.get(0).getType());
        assertEquals(250, out.get(0).getQuantity(), 1e-9);
        assertEquals(5.0, out.get(0).getValue(), 1e-9);
        assertTrue(out.get(0).isBeforeExpiry());
    }

    @Test
    public void cookingEverythingValuesTheWholeItem() {
        PantryItem eggs = item("Eggs", 6, "pcs", "2026-05-10", 48.0);
        List<FoodEvent> out = WasteStats.cookEvents(Arrays.asList(change(eggs, 0)), 2026, 5, 10);
        assertEquals(48.0, out.get(0).getValue(), 1e-9);
        assertTrue("expiring today still counts as in date", out.get(0).isBeforeExpiry());
    }

    @Test
    public void usingExpiredOrUndatedFoodIsNotBeforeExpiry() {
        PantryItem old = item("Milk", 1, "l", "2026-05-01", 20.0);
        PantryItem undated = item("Rice", 1, "kg", null, 40.0);
        List<FoodEvent> out = WasteStats.cookEvents(
                Arrays.asList(change(old, 0), change(undated, 0)), 2026, 5, 10);
        assertEquals(2, out.size());
        assertFalse(out.get(0).isBeforeExpiry());
        assertFalse(out.get(1).isBeforeExpiry());
    }

    @Test
    public void cookingWithoutPriceStillLogsUseWithZeroValue() {
        List<FoodEvent> out = WasteStats.cookEvents(
                Arrays.asList(change(item("Eggs", 6, "pcs", "2026-05-12", null), 4)), 2026, 5, 10);
        assertEquals(1, out.size());
        assertEquals(0, out.get(0).getValue(), 0);
        assertEquals(2, out.get(0).getQuantity(), 1e-9);
    }

    // ---------- expiry sweep and removal ----------

    @Test
    public void sweepLogsOnlyExpiredAndNotYetLoggedItems() {
        PantryItem expired = item("Milk", 1, "l", "2026-05-09", 20.0);
        PantryItem today = item("Eggs", 6, "pcs", "2026-05-10", 48.0);
        PantryItem fresh = item("Rice", 1, "kg", "2027-01-01", 40.0);
        PantryItem undated = item("Salt", 1, "kg", null, 10.0);
        PantryItem alreadyLogged = item("Bread", 1, "pcs", "2026-05-01", 15.0);
        alreadyLogged.setExpiryLogged(true);
        List<FoodEvent> out = WasteStats.expiredSweep(
                Arrays.asList(expired, today, fresh, undated, alreadyLogged), 2026, 5, 10);
        assertEquals(1, out.size());
        assertEquals(FoodEvent.EXPIRED, out.get(0).getType());
        assertEquals(20.0, out.get(0).getValue(), 1e-9);
    }

    @Test
    public void removalOfInDateItemIsDiscardedAndExpiredItemIsExpired() {
        FoodEvent discarded = WasteStats.removalEvent(item("Milk", 1, "l", "2026-05-20", 20.0), 2026, 5, 10);
        assertEquals(FoodEvent.DISCARDED, discarded.getType());
        assertEquals(20.0, discarded.getValue(), 1e-9);

        FoodEvent expired = WasteStats.removalEvent(item("Milk", 1, "l", "2026-05-01", 20.0), 2026, 5, 10);
        assertEquals(FoodEvent.EXPIRED, expired.getType());

        FoodEvent undated = WasteStats.removalEvent(item("Salt", 1, "kg", null, 10.0), 2026, 5, 10);
        assertEquals(FoodEvent.DISCARDED, undated.getType());
    }

    @Test
    public void removingAnAlreadyLoggedExpiredItemAddsNothing() {
        PantryItem p = item("Milk", 1, "l", "2026-05-01", 20.0);
        p.setExpiryLogged(true);
        assertNull(WasteStats.removalEvent(p, 2026, 5, 10));
    }

    @Test
    public void sweepThenRemoveCountsAnExpiredItemExactlyOnce() {
        PantryItem p = item("Milk", 1, "l", "2026-05-01", 20.0);
        List<FoodEvent> log = new ArrayList<>(WasteStats.expiredSweep(Arrays.asList(p), 2026, 5, 10));
        p.setExpiryLogged(true); // what DatabaseHelper.markExpiryLogged does
        FoodEvent removal = WasteStats.removalEvent(p, 2026, 5, 11);
        if (removal != null) log.add(removal);
        assertEquals(1, WasteStats.total(log).expiredCount);
        assertEquals(20.0, WasteStats.total(log).wasted, 1e-9);
    }

    // ---------- monthly totals ----------

    @Test
    public void monthlyGivesRequestedMonthsOldestFirstIncludingEmptyOnes() {
        List<WasteStats.MonthStats> m = WasteStats.monthly(new ArrayList<FoodEvent>(), 2026, 2, 4);
        assertEquals(4, m.size());
        assertEquals(2025, m.get(0).year); assertEquals(11, m.get(0).month);
        assertEquals(2025, m.get(1).year); assertEquals(12, m.get(1).month);
        assertEquals(2026, m.get(2).year); assertEquals(1, m.get(2).month);
        assertEquals(2026, m.get(3).year); assertEquals(2, m.get(3).month);
        assertEquals(0, m.get(3).spent, 0);
    }

    @Test
    public void eventsLandInTheRightMonthAndSumCorrectly() {
        List<FoodEvent> events = Arrays.asList(
                ev(FoodEvent.PURCHASED, 100, "2026-05-01", false),
                ev(FoodEvent.PURCHASED, 50, "2026-05-31", false),
                ev(FoodEvent.PURCHASED, 70, "2026-04-30", false),
                ev(FoodEvent.USED, 30, "2026-05-10", true),
                ev(FoodEvent.USED, 10, "2026-05-11", true),
                ev(FoodEvent.USED, 99, "2026-05-12", false),  // used after expiry: not saved
                ev(FoodEvent.EXPIRED, 20, "2026-05-15", false),
                ev(FoodEvent.DISCARDED, 5, "2026-05-16", false),
                ev(FoodEvent.EXPIRED, 8, "2026-04-02", false),
                ev(FoodEvent.USED, 1000, "2025-01-01", true)); // outside the 3-month window
        List<WasteStats.MonthStats> m = WasteStats.monthly(events, 2026, 5, 3);
        WasteStats.MonthStats may = m.get(2);
        WasteStats.MonthStats apr = m.get(1);
        WasteStats.MonthStats mar = m.get(0);

        assertEquals(150, may.spent, 1e-9);
        assertEquals(40, may.saved, 1e-9);
        assertEquals(25, may.wasted, 1e-9);
        assertEquals(3, may.usedTotal);
        assertEquals(2, may.usedBeforeExpiry);
        assertEquals(1, may.expiredCount);
        assertEquals(1, may.discardedCount);

        assertEquals(70, apr.spent, 1e-9);
        assertEquals(8, apr.wasted, 1e-9);
        assertEquals(0, mar.spent, 0);
        assertEquals(0, mar.usedTotal);
    }

    @Test
    public void malformedDatesAreIgnored() {
        List<FoodEvent> events = Arrays.asList(
                ev(FoodEvent.PURCHASED, 10, "garbage", false),
                ev(FoodEvent.PURCHASED, 10, "2026-13-01", false),
                ev(FoodEvent.PURCHASED, 10, null, false));
        assertEquals(0, WasteStats.total(events).spent, 0);
        assertEquals(0, WasteStats.monthly(events, 2026, 5, 12).get(11).spent, 0);
    }

    @Test
    public void allTimeTotalAndWasteRate() {
        List<FoodEvent> events = Arrays.asList(
                ev(FoodEvent.USED, 10, "2026-01-10", true),
                ev(FoodEvent.USED, 10, "2026-02-10", true),
                ev(FoodEvent.USED, 10, "2026-03-10", true),
                ev(FoodEvent.EXPIRED, 5, "2026-03-11", false));
        WasteStats.MonthStats t = WasteStats.total(events);
        assertEquals(3, t.usedTotal);
        assertEquals(1, t.wastedCount());
        assertEquals(0.25, t.wasteRate(), 1e-9);
        assertEquals(0, WasteStats.total(new ArrayList<FoodEvent>()).wasteRate(), 0);
    }

    // ---------- end to end with the cooker ----------

    @Test
    public void cookPlanFeedsStraightIntoEvents() {
        PantryItem flour = item("Flour", 1, "kg", "2026-05-20", 30.0);
        com.example.smartpantry.models.Recipe r = new com.example.smartpantry.models.Recipe(1, "Bread", "s");
        r.setIngredients(new ArrayList<>(Arrays.asList(
                new com.example.smartpantry.models.RecipeIngredient(0, 0, "flour", 250, "g"))));
        List<PantryCooker.Change> plan = PantryCooker.plan(r, Arrays.asList(flour));
        List<FoodEvent> out = WasteStats.cookEvents(plan, 2026, 5, 10);
        assertEquals(1, out.size());
        assertEquals(0.25, out.get(0).getQuantity(), 1e-9); // in kg, the pantry item's unit
        assertEquals(7.5, out.get(0).getValue(), 1e-9);
    }

    @Test
    public void priceShrinksWithPartialUseSoUnitPriceStaysTheSame() {
        PantryItem flour = item("Flour", 1000, "g", null, 20.0);
        double unitBefore = WasteStats.unitPrice(flour);
        Double left = WasteStats.priceAfterUse(flour, 750);
        assertEquals(15.0, left, 1e-9);
        PantryItem after = item("Flour", 750, "g", null, left);
        assertEquals(unitBefore, WasteStats.unitPrice(after), 1e-12);
    }

    @Test
    public void priceAfterUseHandlesNoPriceAndFullUse() {
        assertNull(WasteStats.priceAfterUse(item("Flour", 1000, "g", null, null), 500));
        assertEquals(0, WasteStats.priceAfterUse(item("Flour", 1000, "g", null, 20.0), 0), 1e-9);
    }

    @Test
    public void expiredUnloggedMatchesTheSweep() {
        PantryItem expired = item("Milk", 1, "l", "2026-05-09", 20.0);
        PantryItem logged = item("Bread", 1, "pcs", "2026-05-01", 15.0);
        logged.setExpiryLogged(true);
        PantryItem fresh = item("Rice", 1, "kg", "2027-01-01", 40.0);
        List<PantryItem> pantry = Arrays.asList(expired, logged, fresh);
        List<PantryItem> unlogged = WasteStats.expiredUnlogged(pantry, 2026, 5, 10);
        assertEquals(1, unlogged.size());
        assertSame(expired, unlogged.get(0));
        assertEquals(unlogged.size(), WasteStats.expiredSweep(pantry, 2026, 5, 10).size());
        // Running the sweep again after flagging finds nothing.
        expired.setExpiryLogged(true);
        assertTrue(WasteStats.expiredUnlogged(pantry, 2026, 5, 11).isEmpty());
    }

    @Test
    public void usedUpEventCountsWholeRemainingItem() {
        FoodEvent inDate = WasteStats.usedUpEvent(item("Eggs", 4, "pcs", "2026-05-12", 32.0), 2026, 5, 10);
        assertEquals(FoodEvent.USED, inDate.getType());
        assertEquals(4, inDate.getQuantity(), 1e-9);
        assertEquals(32.0, inDate.getValue(), 1e-9);
        assertTrue(inDate.isBeforeExpiry());

        assertFalse(WasteStats.usedUpEvent(item("Eggs", 4, "pcs", "2026-05-01", 32.0), 2026, 5, 10).isBeforeExpiry());
        assertFalse(WasteStats.usedUpEvent(item("Salt", 1, "kg", null, null), 2026, 5, 10).isBeforeExpiry());
        assertNull(WasteStats.usedUpEvent(item("Empty", 0, "pcs", null, null), 2026, 5, 10));
    }

    @Test
    public void recentWasteIsNewestFirstWasteOnlyAndLimited() {
        List<FoodEvent> events = Arrays.asList(
                new FoodEvent(1, FoodEvent.EXPIRED, "Milk", 1, "l", 20, "2026-05-01", false),
                new FoodEvent(2, FoodEvent.USED, "Rice", 1, "kg", 40, "2026-05-20", true),
                new FoodEvent(3, FoodEvent.DISCARDED, "Bread", 1, "pcs", 15, "2026-05-10", false),
                new FoodEvent(4, FoodEvent.PURCHASED, "Eggs", 6, "pcs", 48, "2026-05-21", false),
                new FoodEvent(5, FoodEvent.EXPIRED, "Ham", 1, "pcs", 30, "2026-05-10", false));
        List<FoodEvent> recent = WasteStats.recentWaste(events, 10);
        assertEquals(3, recent.size());
        assertEquals("Ham", recent.get(0).getItemName());   // same date as Bread, newer id first
        assertEquals("Bread", recent.get(1).getItemName());
        assertEquals("Milk", recent.get(2).getItemName());
        assertEquals(2, WasteStats.recentWaste(events, 2).size());
        assertTrue(WasteStats.recentWaste(new ArrayList<FoodEvent>(), 5).isEmpty());
    }
}