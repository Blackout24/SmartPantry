package com.example.smartpantry.utils;

import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the assignment's strict-matching rule: a recipe qualifies only if every
 * required ingredient is present in the pantry in at least the required quantity.
 */
public class IngredientMatcher {

    // Basic same-family unit conversions to grams/millilitres, so "1 kg" satisfies "500 g".
    private static final Map<String, Double> TO_BASE_UNIT = new HashMap<>();
    static {
        TO_BASE_UNIT.put("g", 1.0);
        TO_BASE_UNIT.put("kg", 1000.0);
        TO_BASE_UNIT.put("ml", 1.0);
        TO_BASE_UNIT.put("l", 1000.0);
        TO_BASE_UNIT.put("tsp", 5.0);   // 1 tsp = 5 ml
        TO_BASE_UNIT.put("tbsp", 15.0); // 1 tbsp = 15 ml
    }

    public static class MatchResult {
        public final List<Recipe> suggested = new ArrayList<>();
        public final List<Recipe> almostThere = new ArrayList<>(); // missing exactly 1 ingredient
    }

    public static MatchResult match(List<Recipe> allRecipes, List<PantryItem> pantry) {
        Map<String, PantryItem> pantryByName = new HashMap<>();
        for (PantryItem item : pantry) {
            // Re-normalize from the original text so items saved under an older rule still match.
            pantryByName.put(PantryItem.normalize(item.getDisplayName()), item);
        }

        MatchResult result = new MatchResult();

        for (Recipe recipe : allRecipes) {
            int missing = 0;
            for (RecipeIngredient required : recipe.getIngredients()) {
                if (!isSatisfied(required, pantryByName)) {
                    missing++;
                }
            }
            recipe.setMissingCount(missing);
            if (missing == 0) {
                result.suggested.add(recipe);
            } else if (missing == 1) {
                result.almostThere.add(recipe);
            }
        }
        return result;
    }

    static boolean isSatisfied(RecipeIngredient required, Map<String, PantryItem> pantryByName) {
        PantryItem pantryItem = pantryByName.get(required.getIngredientName());
        if (pantryItem == null) return false;

        Double reqQty = toBaseUnit(required.getRequiredQuantity(), required.getUnit());
        Double haveQty = toBaseUnit(pantryItem.getQuantity(), pantryItem.getUnit());

        if (reqQty != null && haveQty != null) {
            return haveQty >= reqQty;
        }
        // Units not comparable (e.g. "pcs") or unspecified: fall back to raw quantity comparison.
        return pantryItem.getQuantity() >= required.getRequiredQuantity();
    }

    /** Converts to a common base unit (grams or millilitres) where possible; null if unit unrecognized. */
    static Double toBaseUnit(double quantity, String unit) {
        if (unit == null) return null;
        Double factor = TO_BASE_UNIT.get(unit.trim().toLowerCase());
        if (factor == null) return null;
        return quantity * factor;
    }
}
