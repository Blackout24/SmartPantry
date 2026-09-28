package com.example.smartpantry.utils;

import static org.junit.Assert.*;

import com.example.smartpantry.database.RecipeSeeder;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PantryCookerTest {

    private static PantryItem item(long id, String name, double qty, String unit) {
        return new PantryItem(id, PantryItem.normalize(name), name, qty, unit, null);
    }

    private static Recipe recipe(RecipeIngredient... ings) {
        Recipe r = new Recipe(1, "Test", "steps");
        r.setIngredients(new ArrayList<>(Arrays.asList(ings)));
        return r;
    }

    private static RecipeIngredient req(String name, double qty, String unit) {
        return new RecipeIngredient(0, 0, PantryItem.normalize(name), qty, unit);
    }

    // ---------- unit handling ----------

    @Test
    public void subtractsAcrossUnitsInPantryUnit() {
        List<PantryItem> pantry = Arrays.asList(item(1, "Flour", 1, "kg"), item(2, "Milk", 2, "l"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("flour", 250, "g"), req("milk", 500, "ml")), pantry);
        assertNotNull(ch);
        assertEquals(0.75, ch.get(0).newQuantity, 1e-9);
        assertEquals(1.5, ch.get(1).newQuantity, 1e-9);
        assertFalse(ch.get(0).delete);
    }

    @Test
    public void subtractsGramsFromGramsAndKgFromRecipeInGrams() {
        List<PantryItem> pantry = Arrays.asList(item(1, "Rice", 500, "g"));
        assertEquals(300, PantryCooker.plan(recipe(req("rice", 200, "g")), pantry).get(0).newQuantity, 1e-9);
    }

    @Test
    public void unitFamiliesDoNotMix() {
        // 500 ml of flour is not 200 g of flour: units are not comparable, so raw numbers are compared.
        // Documented behaviour: falls back to raw quantity (same as IngredientMatcher).
        List<PantryItem> pantry = Arrays.asList(item(1, "Flour", 500, "ml"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("flour", 200, "g")), pantry);
        assertNotNull(ch);
        assertEquals(300, ch.get(0).newQuantity, 1e-9);
    }

    @Test
    public void unitMatchIsCaseAndSpaceInsensitive() {
        List<PantryItem> pantry = Arrays.asList(item(1, "Flour", 1, " KG "));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("flour", 250, "g")), pantry);
        assertEquals(0.75, ch.get(0).newQuantity, 1e-9);
    }

    @Test
    public void tablespoonsAndTeaspoonsAreNotConfused() {
        // Pantry has 1 tbsp of oil, recipe wants 3 tsp (= 1 tbsp). Should be cookable and use everything.
        List<PantryItem> pantry = Arrays.asList(item(1, "Oil", 1, "tbsp"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("oil", 3, "tsp")), pantry);
        assertNotNull("1 tbsp should cover 3 tsp", ch);
        assertTrue(ch.get(0).delete);
    }

    @Test
    public void tspRecipeAgainstTbspPantryDoesNotOverSubtract() {
        // Pantry 2 tbsp, recipe 1 tsp: should leave about 1.67 tbsp, not 1.
        List<PantryItem> pantry = Arrays.asList(item(1, "Oil", 2, "tbsp"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("oil", 1, "tsp")), pantry);
        assertNotNull(ch);
        assertEquals(2 - 1.0 / 3.0, ch.get(0).newQuantity, 1e-6);
    }

    // ---------- cooking behaviour ----------

    @Test
    public void exactAmountDeletesItem() {
        List<PantryItem> pantry = Arrays.asList(item(7, "Eggs", 2, "pcs"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("egg", 2, "pcs")), pantry);
        assertTrue(ch.get(0).delete);
        assertEquals(7, ch.get(0).item.getId());
    }

    @Test
    public void floatingPointResidueCountsAsEmpty() {
        // 0.3 kg - 300 g leaves 5.5e-17 style residue in doubles; must be treated as empty.
        List<PantryItem> pantry = Arrays.asList(item(1, "Sugar", 0.3, "kg"));
        assertTrue(PantryCooker.plan(recipe(req("sugar", 300, "g")), pantry).get(0).delete);
        List<PantryItem> p2 = Arrays.asList(item(1, "Milk", 1.1, "l"));
        assertTrue(PantryCooker.plan(recipe(req("milk", 1100, "ml")), p2).get(0).delete);
    }

    @Test
    public void notCookableReturnsNullAndChangesNothing() {
        List<PantryItem> pantry = Arrays.asList(item(1, "Eggs", 1, "pcs"), item(2, "Salt", 5, "tsp"));
        assertNull(PantryCooker.plan(recipe(req("egg", 2, "pcs"), req("salt", 1, "tsp")), pantry));
        assertNull(PantryCooker.plan(recipe(req("egg", 1, "pcs"), req("pepper", 1, "tsp")), pantry));
        assertEquals(1, pantry.get(0).getQuantity(), 0); // plan() never mutates
    }

    @Test
    public void pluralAndCaseInPantryStillMatch() {
        List<PantryItem> pantry = Arrays.asList(item(1, "Tomatoes", 4, "pcs"));
        List<PantryCooker.Change> ch = PantryCooker.plan(recipe(req("tomato", 3, "pcs")), pantry);
        assertNotNull(ch);
        assertEquals(1, ch.get(0).newQuantity, 1e-9);
    }

    @Test
    public void duplicatePantryEntriesForSameIngredient() {
        // Two rows for eggs (2 + 2). Recipe needs 3. Strict matcher only sees one row, so it says no.
        // Cooking must agree with the matcher, otherwise the app would suggest something it cannot cook.
        List<PantryItem> pantry = Arrays.asList(item(1, "Egg", 2, "pcs"), item(2, "Eggs", 2, "pcs"));
        Recipe r = recipe(req("egg", 3, "pcs"));
        boolean suggested = IngredientMatcher.match(Arrays.asList(r), pantry).suggested.contains(r);
        assertEquals(suggested, PantryCooker.plan(r, pantry) != null);
    }

    // ---------- every seeded recipe ----------

    private static List<PantryItem> pantryFor(Recipe r, double factor) {
        List<PantryItem> p = new ArrayList<>();
        long id = 1;
        for (RecipeIngredient ri : r.getIngredients()) {
            p.add(item(id++, ri.getIngredientName(), ri.getRequiredQuantity() * factor, ri.getUnit()));
        }
        return p;
    }

    @Test
    public void seedHasExpectedRecipeCountAndUniqueNames() {
        List<Recipe> all = RecipeSeeder.getSeedRecipes();
        assertTrue("assignment asks for 15-20+ recipes", all.size() >= 15);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Recipe r : all) assertTrue("duplicate recipe name " + r.getName(), names.add(r.getName()));
    }

    @Test
    public void everyRecipeCookableWithExactAmountsAndEmptiesPantry() {
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            List<PantryItem> pantry = pantryFor(r, 1);
            assertTrue(r.getName() + " should be suggested",
                    IngredientMatcher.match(Arrays.asList(r), pantry).suggested.contains(r));
            List<PantryCooker.Change> ch = PantryCooker.plan(r, pantry);
            assertNotNull(r.getName(), ch);
            assertEquals(r.getName(), r.getIngredients().size(), ch.size());
            for (PantryCooker.Change c : ch) assertTrue(r.getName() + ": " + c.item.getDisplayName(), c.delete);
        }
    }

    @Test
    public void everyRecipeWithDoubleAmountsLeavesExactlyOneBatch() {
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            List<PantryItem> pantry = pantryFor(r, 2);
            List<PantryCooker.Change> ch = PantryCooker.plan(r, pantry);
            assertNotNull(r.getName(), ch);
            for (int i = 0; i < ch.size(); i++) {
                assertFalse(r.getName(), ch.get(i).delete);
                assertEquals(r.getName() + ": " + ch.get(i).item.getDisplayName(),
                        r.getIngredients().get(i).getRequiredQuantity(), ch.get(i).newQuantity, 1e-9);
            }
        }
    }

    @Test
    public void everyRecipeCookableWhenPantryUsesKgOrLitres() {
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            List<PantryItem> pantry = new ArrayList<>();
            long id = 1;
            for (RecipeIngredient ri : r.getIngredients()) {
                String u = ri.getUnit();
                double q = ri.getRequiredQuantity() * 2;
                if ("g".equals(u)) { q /= 1000; u = "kg"; }
                else if ("ml".equals(u)) { q /= 1000; u = "l"; }
                pantry.add(item(id++, ri.getIngredientName(), q, u));
            }
            List<PantryCooker.Change> ch = PantryCooker.plan(r, pantry);
            assertNotNull(r.getName(), ch);
            for (int i = 0; i < ch.size(); i++) {
                RecipeIngredient ri = r.getIngredients().get(i);
                String u = ch.get(i).item.getUnit();
                double leftBase = ch.get(i).newQuantity * ("kg".equals(u) || "l".equals(u) ? 1000 : 1);
                assertEquals(r.getName() + ": " + ri.getIngredientName(), ri.getRequiredQuantity(), leftBase, 1e-6);
            }
        }
    }

    @Test
    public void everyRecipeFailsWhenAnyOneIngredientIsMissingOrShort() {
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            for (int skip = 0; skip < r.getIngredients().size(); skip++) {
                List<PantryItem> missing = pantryFor(r, 1);
                missing.remove(skip);
                assertNull(r.getName() + " without ingredient " + skip, PantryCooker.plan(r, missing));

                List<PantryItem> shortOne = pantryFor(r, 1);
                shortOne.get(skip).setQuantity(shortOne.get(skip).getQuantity() * 0.5);
                assertNull(r.getName() + " short on ingredient " + skip, PantryCooker.plan(r, shortOne));
            }
        }
    }

    @Test
    public void cookingThenMatchingAgainDropsRecipeWhenPantryEmpties() {
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            List<PantryItem> pantry = pantryFor(r, 1);
            List<PantryItem> after = new ArrayList<>(pantry);
            for (PantryCooker.Change c : PantryCooker.plan(r, pantry)) {
                if (c.delete) after.remove(c.item);
                else c.item.setQuantity(c.newQuantity);
            }
            assertFalse(r.getName(), IngredientMatcher.match(Arrays.asList(r), after).suggested.contains(r));
        }
    }

    @Test
    public void seedIngredientNamesAreAlreadyNormalised() {
        // The matcher looks up the seed name directly, so it must equal its own normalised form.
        for (Recipe r : RecipeSeeder.getSeedRecipes()) {
            for (RecipeIngredient ri : r.getIngredients()) {
                assertEquals(r.getName(), PantryItem.normalize(ri.getIngredientName()), ri.getIngredientName());
            }
        }
    }
}
