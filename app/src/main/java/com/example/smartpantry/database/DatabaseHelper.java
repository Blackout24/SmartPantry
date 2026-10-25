package com.example.smartpantry.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.smartpantry.models.MealPlanEntry;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_pantry.db";
    private static final int DATABASE_VERSION = 2;

    // pantry_items
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_PANTRY_ID = "id";
    public static final String COL_PANTRY_NAME = "name";
    public static final String COL_PANTRY_DISPLAY_NAME = "display_name";
    public static final String COL_PANTRY_QUANTITY = "quantity";
    public static final String COL_PANTRY_UNIT = "unit";
    public static final String COL_PANTRY_EXPIRY = "expiry_date";

    // recipes
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_RECIPE_ID = "id";
    public static final String COL_RECIPE_NAME = "name";
    public static final String COL_RECIPE_STEPS = "steps";

    // recipe_ingredients
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RI_ID = "id";
    public static final String COL_RI_RECIPE_ID = "recipe_id";
    public static final String COL_RI_INGREDIENT_NAME = "ingredient_name";
    public static final String COL_RI_QUANTITY = "required_quantity";
    public static final String COL_RI_UNIT = "unit";

    // meal_plan (added in version 2)
    public static final String TABLE_MEAL_PLAN = "meal_plan";
    public static final String COL_MP_ID = "id";
    public static final String COL_MP_DAY = "day_index";     // 0 = Monday ... 6 = Sunday
    public static final String COL_MP_MEAL = "meal_type";    // 0 breakfast, 1 lunch, 2 dinner
    public static final String COL_MP_RECIPE_ID = "recipe_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " (" +
                COL_PANTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PANTRY_NAME + " TEXT NOT NULL, " +
                COL_PANTRY_DISPLAY_NAME + " TEXT NOT NULL, " +
                COL_PANTRY_QUANTITY + " REAL NOT NULL, " +
                COL_PANTRY_UNIT + " TEXT, " +
                COL_PANTRY_EXPIRY + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                COL_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECIPE_NAME + " TEXT NOT NULL, " +
                COL_RECIPE_STEPS + " TEXT NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                COL_RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RI_RECIPE_ID + " INTEGER NOT NULL, " +
                COL_RI_INGREDIENT_NAME + " TEXT NOT NULL, " +
                COL_RI_QUANTITY + " REAL NOT NULL, " +
                COL_RI_UNIT + " TEXT, " +
                "FOREIGN KEY(" + COL_RI_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COL_RECIPE_ID + "))");

        createMealPlanTable(db);
    }

    private void createMealPlanTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MEAL_PLAN + " (" +
                COL_MP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MP_DAY + " INTEGER NOT NULL, " +
                COL_MP_MEAL + " INTEGER NOT NULL, " +
                COL_MP_RECIPE_ID + " INTEGER NOT NULL, " +
                "UNIQUE(" + COL_MP_DAY + ", " + COL_MP_MEAL + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep the user's pantry: only add what is new.
        if (oldVersion < 2) {
            createMealPlanTable(db);
        }
    }

    // ---------- Pantry CRUD ----------

    public long addPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PANTRY_NAME, PantryItem.normalize(item.getDisplayName()));
        values.put(COL_PANTRY_DISPLAY_NAME, item.getDisplayName());
        values.put(COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(COL_PANTRY_UNIT, item.getUnit());
        values.put(COL_PANTRY_EXPIRY, item.getExpiryDate());
        long id = db.insert(TABLE_PANTRY, null, values);
        db.close();
        return id;
    }

    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANTRY, null, null, null, null, null,
                COL_PANTRY_DISPLAY_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                items.add(cursorToPantryItem(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return items;
    }

    public PantryItem getPantryItem(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANTRY, null, COL_PANTRY_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        PantryItem item = null;
        if (cursor.moveToFirst()) {
            item = cursorToPantryItem(cursor);
        }
        cursor.close();
        db.close();
        return item;
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PANTRY_NAME, PantryItem.normalize(item.getDisplayName()));
        values.put(COL_PANTRY_DISPLAY_NAME, item.getDisplayName());
        values.put(COL_PANTRY_QUANTITY, item.getQuantity());
        values.put(COL_PANTRY_UNIT, item.getUnit());
        values.put(COL_PANTRY_EXPIRY, item.getExpiryDate());
        int rows = db.update(TABLE_PANTRY, values, COL_PANTRY_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    public void deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PANTRY, COL_PANTRY_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ---------- Meal plan CRUD ----------

    /** Assigns a recipe to a slot, replacing whatever was there. */
    public void setMeal(int dayIndex, int mealType, long recipeId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MP_DAY, dayIndex);
        values.put(COL_MP_MEAL, mealType);
        values.put(COL_MP_RECIPE_ID, recipeId);
        db.insertWithOnConflict(TABLE_MEAL_PLAN, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void clearMeal(int dayIndex, int mealType) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MEAL_PLAN, COL_MP_DAY + "=? AND " + COL_MP_MEAL + "=?",
                new String[]{String.valueOf(dayIndex), String.valueOf(mealType)});
        db.close();
    }

    public void clearMealPlan() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MEAL_PLAN, null, null);
        db.close();
    }

    public List<MealPlanEntry> getMealPlan() {
        List<MealPlanEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEAL_PLAN, null, null, null, null, null,
                COL_MP_DAY + " ASC, " + COL_MP_MEAL + " ASC");
        if (cursor.moveToFirst()) {
            do {
                entries.add(new MealPlanEntry(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_MP_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_MP_DAY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_MP_MEAL)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_MP_RECIPE_ID))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return entries;
    }

    // ---------- Recipe read / seed ----------

    /** Inserts a recipe with its ingredients. Used only by the seeder on first run. */
    public long addRecipe(String name, String steps, List<RecipeIngredient> ingredients) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(COL_RECIPE_NAME, name);
        recipeValues.put(COL_RECIPE_STEPS, steps);
        long recipeId = db.insert(TABLE_RECIPES, null, recipeValues);

        for (RecipeIngredient ing : ingredients) {
            ContentValues riValues = new ContentValues();
            riValues.put(COL_RI_RECIPE_ID, recipeId);
            riValues.put(COL_RI_INGREDIENT_NAME, PantryItem.normalize(ing.getIngredientName()));
            riValues.put(COL_RI_QUANTITY, ing.getRequiredQuantity());
            riValues.put(COL_RI_UNIT, ing.getUnit());
            db.insert(TABLE_RECIPE_INGREDIENTS, null, riValues);
        }
        db.close();
        return recipeId;
    }

    /** Names of all recipes already stored; lets the seeder add only the missing ones. */
    public Set<String> getAllRecipeNames() {
        Set<String> names = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, new String[]{COL_RECIPE_NAME}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                names.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return names;
    }

    public boolean hasRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECIPES, null);
        boolean has = false;
        if (cursor.moveToFirst()) {
            has = cursor.getInt(0) > 0;
        }
        cursor.close();
        db.close();
        return has;
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, null, null, null, null, COL_RECIPE_NAME + " ASC");

        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_STEPS))
                );
                recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                recipes.add(recipe);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return recipes;
    }

    public Recipe getRecipe(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, COL_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)}, null, null, null);

        Recipe recipe = null;
        if (cursor.moveToFirst()) {
            recipe = new Recipe(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_RECIPE_STEPS))
            );
            recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
        }
        cursor.close();
        db.close();
        return recipe;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(SQLiteDatabase db, long recipeId) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        Cursor cursor = db.query(TABLE_RECIPE_INGREDIENTS, null, COL_RI_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ingredients.add(new RecipeIngredient(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_RI_ID)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_RI_RECIPE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_RI_INGREDIENT_NAME)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RI_QUANTITY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_RI_UNIT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return ingredients;
    }

    private PantryItem cursorToPantryItem(Cursor cursor) {
        return new PantryItem(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PANTRY_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_DISPLAY_NAME)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PANTRY_QUANTITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_UNIT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PANTRY_EXPIRY))
        );
    }
}