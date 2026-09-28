package com.example.smartpantry.utils;

import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "I cooked this": works out how the pantry changes after cooking a recipe.
 * Pure logic - the caller applies the result to the database.
 * Uses the same name lookup and unit rules as IngredientMatcher, so anything the
 * matcher suggests can always be cooked.
 */
public class PantryCooker {

    private static final double EPSILON = 1e-9;

    public static class Change {
        public final PantryItem item;     // item to update or delete
        public final double newQuantity;  // in the pantry item's own unit
        public final boolean delete;      // true when nothing is left

        Change(PantryItem item, double newQuantity, boolean delete) {
            this.item = item;
            this.newQuantity = newQuantity;
            this.delete = delete;
        }
    }

    /** Returns the pantry changes, or null if the recipe cannot be cooked with this pantry. */
    public static List<Change> plan(Recipe recipe, List<PantryItem> pantry) {
        Map<String, PantryItem> byName = new HashMap<>();
        for (PantryItem item : pantry) {
            byName.put(PantryItem.normalize(item.getDisplayName()), item);
        }

        List<Change> changes = new ArrayList<>();
        for (RecipeIngredient req : recipe.getIngredients()) {
            if (!IngredientMatcher.isSatisfied(req, byName)) return null;

            PantryItem have = byName.get(req.getIngredientName());
            double used = amountInPantryUnit(req, have);
            double left = have.getQuantity() - used;
            boolean delete = left <= EPSILON;
            changes.add(new Change(have, delete ? 0 : left, delete));
        }
        return changes;
    }

    /** Converts the recipe amount into the pantry item's unit when both are convertible; else raw. */
    private static double amountInPantryUnit(RecipeIngredient req, PantryItem have) {
        Double reqBase = IngredientMatcher.toBaseUnit(req.getRequiredQuantity(), req.getUnit());
        Double haveFactor = IngredientMatcher.toBaseUnit(1, have.getUnit());
        if (reqBase != null && haveFactor != null) return reqBase / haveFactor;
        return req.getRequiredQuantity();
    }
}
