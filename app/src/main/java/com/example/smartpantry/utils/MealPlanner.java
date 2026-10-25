package com.example.smartpantry.utils;

import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Weekly meal planner logic (no Android dependencies):
 * 1. rankRecipes - orders recipes so those using soon-to-expire pantry items come first.
 * 2. shoppingList - totals what the planned meals need and subtracts what the pantry has.
 */
public class MealPlanner {

    public static class Ranked {
        public final Recipe recipe;
        public final List<String> expiringUsed; // normalised names of expiring pantry items this recipe uses
        public final int missing;               // ingredients the pantry cannot cover for ONE serving of this recipe

        Ranked(Recipe recipe, List<String> expiringUsed, int missing) {
            this.recipe = recipe;
            this.expiringUsed = expiringUsed;
            this.missing = missing;
        }
    }

    public static class ShoppingItem {
        public final String name;     // normalised ingredient name
        public final double quantity; // amount still to buy
        public final String unit;     // unit of the first recipe that needed it (may be null)

        ShoppingItem(String name, double quantity, String unit) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    private static final double EPSILON = 1e-9;

    // ---------- Prioritise by expiry ----------

    /**
     * Most expiring ingredients used first, then fewest missing ingredients, then name.
     * Expired items are ignored: the planner never encourages cooking with them.
     */
    public static List<Ranked> rankRecipes(List<Recipe> recipes, List<PantryItem> pantry,
                                           int y, int m, int d, int soonDays) {
        Set<String> expiring = new HashSet<>();
        Map<String, PantryItem> byName = new HashMap<>();
        for (PantryItem item : pantry) {
            String key = PantryItem.normalize(item.getDisplayName());
            byName.put(key, item);
            Integer days = ExpiryUtils.daysUntil(item.getExpiryDate(), y, m, d);
            if (ExpiryUtils.statusFor(days, soonDays) == ExpiryUtils.Status.SOON) expiring.add(key);
        }

        List<Ranked> out = new ArrayList<>();
        for (Recipe r : recipes) {
            List<String> used = new ArrayList<>();
            int missing = 0;
            for (RecipeIngredient ing : r.getIngredients()) {
                if (expiring.contains(ing.getIngredientName()) && !used.contains(ing.getIngredientName())) {
                    used.add(ing.getIngredientName());
                }
                if (!IngredientMatcher.isSatisfied(ing, byName)) missing++;
            }
            out.add(new Ranked(r, used, missing));
        }

        Collections.sort(out, (a, b) -> {
            if (a.expiringUsed.size() != b.expiringUsed.size()) {
                return Integer.compare(b.expiringUsed.size(), a.expiringUsed.size());
            }
            if (a.missing != b.missing) return Integer.compare(a.missing, b.missing);
            return a.recipe.getName().compareToIgnoreCase(b.recipe.getName());
        });
        return out;
    }

    // ---------- Shopping list ----------

    /**
     * plannedRecipes holds one entry per planned meal, so a recipe planned twice appears twice
     * and its ingredients are counted twice. Units follow IngredientMatcher: g/kg, ml/l/tsp/tbsp
     * convert within their base unit; anything else (pcs...) is compared as raw numbers.
     */
    public static List<ShoppingItem> shoppingList(List<Recipe> plannedRecipes, List<PantryItem> pantry) {
        Map<String, PantryItem> byName = new HashMap<>();
        for (PantryItem item : pantry) {
            byName.put(PantryItem.normalize(item.getDisplayName()), item);
        }

        // Total needed per (ingredient, unit family), in base units where convertible.
        Map<String, double[]> needed = new LinkedHashMap<>();      // key -> {total}
        Map<String, String> firstUnit = new HashMap<>();           // key -> unit of first requirement
        Map<String, String> nameOf = new HashMap<>();

        for (Recipe r : plannedRecipes) {
            for (RecipeIngredient ing : r.getIngredients()) {
                Double base = IngredientMatcher.toBaseUnit(ing.getRequiredQuantity(), ing.getUnit());
                String family = base != null ? "base" : "raw:" + normUnit(ing.getUnit());
                String key = ing.getIngredientName() + "|" + family;
                double amount = base != null ? base : ing.getRequiredQuantity();
                double[] total = needed.get(key);
                if (total == null) {
                    needed.put(key, new double[]{amount});
                    firstUnit.put(key, ing.getUnit());
                    nameOf.put(key, ing.getIngredientName());
                } else {
                    total[0] += amount;
                }
            }
        }

        List<ShoppingItem> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : needed.entrySet()) {
            String key = e.getKey();
            PantryItem have = byName.get(nameOf.get(key));
            boolean baseGroup = key.endsWith("|base");

            double haveAmount = 0;
            if (have != null) {
                Double haveBase = IngredientMatcher.toBaseUnit(have.getQuantity(), have.getUnit());
                haveAmount = (baseGroup && haveBase != null) ? haveBase : have.getQuantity();
            }

            double deficit = e.getValue()[0] - haveAmount;
            if (deficit <= EPSILON) continue;

            String unit = firstUnit.get(key);
            double quantity = deficit;
            if (baseGroup) {
                Double factor = IngredientMatcher.toBaseUnit(1, unit);
                quantity = deficit / factor; // back into the unit the recipe used
            }
            out.add(new ShoppingItem(nameOf.get(key), quantity, unit));
        }
        Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    private static String normUnit(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase();
    }
}