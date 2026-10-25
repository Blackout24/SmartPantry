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

public class MealPlannerTest {

    private static PantryItem item(String name, double qty, String unit, String expiry) {
        return new PantryItem(0, PantryItem.normalize(name), name, qty, unit, expiry);
    }

    private static RecipeIngredient req(String name, double qty, String unit) {
        return new RecipeIngredient(0, 0, PantryItem.normalize(name), qty, unit);
    }

    private static Recipe recipe(String name, RecipeIngredient... ings) {
        Recipe r = new Recipe(0, name, "steps");
        r.setIngredients(new ArrayList<>(Arrays.asList(ings)));
        return r;
    }

    private static List<MealPlanner.ShoppingItem> shop(List<Recipe> planned, List<PantryItem> pantry) {
        return MealPlanner.shoppingList(planned, pantry);
    }

    private static MealPlanner.ShoppingItem find(List<MealPlanner.ShoppingItem> list, String name) {
        for (MealPlanner.ShoppingItem s : list) if (s.name.equals(name)) return s;
        return null;
    }

    // ---------- ranking ----------

    @Test
    public void recipesUsingExpiringItemsComeFirst() {
        Recipe a = recipe("Alpha", req("rice", 100, "g"));
        Recipe b = recipe("Beta", req("milk", 200, "ml"));
        Recipe c = recipe("Gamma", req("milk", 100, "ml"), req("egg", 1, "pcs"));
        List<PantryItem> pantry = Arrays.asList(
                item("Rice", 500, "g", "2030-01-01"),
                item("Milk", 1, "l", "2026-05-11"),   // expires tomorrow
                item("Eggs", 6, "pcs", "2026-05-12")); // in 2 days
        List<MealPlanner.Ranked> ranked = MealPlanner.rankRecipes(Arrays.asList(a, b, c), pantry, 2026, 5, 10, 3);
        assertEquals("Gamma", ranked.get(0).recipe.getName()); // uses milk + egg
        assertEquals(2, ranked.get(0).expiringUsed.size());
        assertEquals("Beta", ranked.get(1).recipe.getName());  // uses milk
        assertEquals("Alpha", ranked.get(2).recipe.getName()); // nothing expiring
    }

    @Test
    public void expiredItemsDoNotBoostRanking() {
        Recipe a = recipe("Alpha", req("rice", 100, "g"));
        Recipe b = recipe("Beta", req("milk", 100, "ml"));
        List<PantryItem> pantry = Arrays.asList(
                item("Rice", 500, "g", "2030-01-01"),
                item("Milk", 1, "l", "2026-05-01")); // already expired
        List<MealPlanner.Ranked> ranked = MealPlanner.rankRecipes(Arrays.asList(b, a), pantry, 2026, 5, 10, 7);
        assertEquals(0, ranked.get(0).expiringUsed.size());
        assertEquals("Alpha", ranked.get(0).recipe.getName()); // tie on 0 -> alphabetical
    }

    @Test
    public void tieBrokenByFewerMissingThenName() {
        Recipe full = recipe("Zed", req("rice", 100, "g"));
        Recipe short1 = recipe("Alpha", req("rice", 100, "g"), req("saffron", 1, "g"));
        List<PantryItem> pantry = Arrays.asList(item("Rice", 500, "g", null));
        List<MealPlanner.Ranked> ranked = MealPlanner.rankRecipes(Arrays.asList(short1, full), pantry, 2026, 5, 10, 3);
        assertEquals("Zed", ranked.get(0).recipe.getName());
        assertEquals(0, ranked.get(0).missing);
        assertEquals(1, ranked.get(1).missing);
    }

    @Test
    public void windowSizeChangesWhatIsExpiring() {
        Recipe a = recipe("Alpha", req("milk", 100, "ml"));
        List<PantryItem> pantry = Arrays.asList(item("Milk", 1, "l", "2026-05-15")); // +5 days
        assertEquals(0, MealPlanner.rankRecipes(Arrays.asList(a), pantry, 2026, 5, 10, 3).get(0).expiringUsed.size());
        assertEquals(1, MealPlanner.rankRecipes(Arrays.asList(a), pantry, 2026, 5, 10, 7).get(0).expiringUsed.size());
    }

    // ---------- shopping list ----------

    @Test
    public void nothingToBuyWhenPantryCoversEverything() {
        Recipe r = recipe("R", req("flour", 250, "g"), req("egg", 2, "pcs"));
        List<PantryItem> pantry = Arrays.asList(item("Flour", 1, "kg", null), item("Eggs", 6, "pcs", null));
        assertTrue(shop(Arrays.asList(r), pantry).isEmpty());
    }

    @Test
    public void missingIngredientListedInFull() {
        Recipe r = recipe("R", req("butter", 50, "g"));
        List<MealPlanner.ShoppingItem> list = shop(Arrays.asList(r), new ArrayList<PantryItem>());
        assertEquals(1, list.size());
        assertEquals(50, list.get(0).quantity, 1e-9);
        assertEquals("g", list.get(0).unit);
    }

    @Test
    public void partialPantryOnlyBuysTheShortfall() {
        Recipe r = recipe("R", req("flour", 500, "g"));
        List<MealPlanner.ShoppingItem> list = shop(Arrays.asList(r), Arrays.asList(item("Flour", 0.2, "kg", null)));
        assertEquals(300, find(list, "flour").quantity, 1e-9);
        assertEquals("g", find(list, "flour").unit);
    }

    @Test
    public void sameRecipePlannedTwiceDoublesTheNeed() {
        Recipe r = recipe("R", req("milk", 300, "ml"));
        List<PantryItem> pantry = Arrays.asList(item("Milk", 500, "ml", null));
        assertTrue(shop(Arrays.asList(r), pantry).isEmpty());
        assertEquals(100, find(shop(Arrays.asList(r, r), pantry), "milk").quantity, 1e-9);
    }

    @Test
    public void differentRecipesShareOnePantryStock() {
        Recipe a = recipe("A", req("egg", 3, "pcs"));
        Recipe b = recipe("B", req("egg", 4, "pcs"));
        List<PantryItem> pantry = Arrays.asList(item("Eggs", 5, "pcs", null));
        // Each recipe alone is fine, together they need 7 and only 5 exist.
        assertTrue(shop(Arrays.asList(a), pantry).isEmpty());
        assertTrue(shop(Arrays.asList(b), pantry).isEmpty());
        assertEquals(2, find(shop(Arrays.asList(a, b), pantry), "egg").quantity, 1e-9);
    }

    @Test
    public void mixedUnitsOfTheSameFamilyAreSummed() {
        Recipe a = recipe("A", req("milk", 500, "ml"));
        Recipe b = recipe("B", req("milk", 1, "l"));
        List<MealPlanner.ShoppingItem> list = shop(Arrays.asList(a, b), new ArrayList<PantryItem>());
        assertEquals(1, list.size());
        assertEquals(1500, list.get(0).quantity, 1e-9); // in ml, the first recipe's unit
        assertEquals("ml", list.get(0).unit);
    }

    @Test
    public void spoonUnitsConvertBothWays() {
        Recipe a = recipe("A", req("oil", 3, "tsp"));
        Recipe b = recipe("B", req("oil", 1, "tbsp"));
        List<MealPlanner.ShoppingItem> list = shop(Arrays.asList(a, b), new ArrayList<PantryItem>());
        assertEquals(6, list.get(0).quantity, 1e-9); // 3 tsp + 1 tbsp = 6 tsp
        assertEquals("tsp", list.get(0).unit);
    }

    @Test
    public void noFloatingPointResidueListedAsMissing() {
        Recipe r = recipe("R", req("sugar", 300, "g"));
        assertTrue(shop(Arrays.asList(r), Arrays.asList(item("Sugar", 0.3, "kg", null))).isEmpty());
    }

    @Test
    public void shoppingListIsSortedAndEmptyPlanGivesEmptyList() {
        Recipe r = recipe("R", req("zucchini", 1, "pcs"), req("apple", 2, "pcs"));
        List<MealPlanner.ShoppingItem> list = shop(Arrays.asList(r), new ArrayList<PantryItem>());
        assertEquals("apple", list.get(0).name);
        assertEquals("zucchini", list.get(1).name);
        assertTrue(shop(new ArrayList<Recipe>(), new ArrayList<PantryItem>()).isEmpty());
    }

    // ---------- whole seed set ----------

    @Test
    public void fullWeekOfSeedRecipesEmptyPantryNeedsEverything() {
        List<Recipe> all = RecipeSeeder.getSeedRecipes();
        List<Recipe> week = new ArrayList<>();
        for (int i = 0; i < 21; i++) week.add(all.get(i % all.size())); // 7 days x 3 meals
        List<MealPlanner.ShoppingItem> list = shop(week, new ArrayList<PantryItem>());
        assertFalse(list.isEmpty());
        for (MealPlanner.ShoppingItem s : list) assertTrue(s.name, s.quantity > 0);
    }

    @Test
    public void stockingThePantryFromTheShoppingListEmptiesIt() {
        List<Recipe> all = RecipeSeeder.getSeedRecipes();
        List<Recipe> week = new ArrayList<>();
        for (int i = 0; i < 21; i++) week.add(all.get((i * 3) % all.size()));
        List<PantryItem> pantry = new ArrayList<>();
        for (MealPlanner.ShoppingItem s : shop(week, pantry)) {
            pantry.add(item(s.name, s.quantity, s.unit, null)); // buy exactly what the list says
        }
        assertTrue("buying the list should cover the plan", shop(week, pantry).isEmpty());
    }

    @Test
    public void everySeedRecipeRankedExactlyOnce() {
        List<Recipe> all = RecipeSeeder.getSeedRecipes();
        assertEquals(all.size(), MealPlanner.rankRecipes(all, new ArrayList<PantryItem>(), 2026, 5, 10, 3).size());
    }
}