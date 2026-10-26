package com.example.smartpantry.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.database.RecipeSeeder;
import com.example.smartpantry.models.MealPlanEntry;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.utils.BottomNavHelper;
import com.example.smartpantry.utils.MealPlanner;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Weekly planner: 7 days x 3 meals. Tap a slot to pick a recipe (expiring ingredients first). */
public class MealPlannerActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);

        dbHelper = new DatabaseHelper(this);
        RecipeSeeder.seedMissingRecipes(dbHelper);

        container = findViewById(R.id.plannerContainer);
        findViewById(R.id.buttonClearPlan).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.planner_clear_confirm)
                        .setPositiveButton(R.string.planner_clear, (d, w) -> {
                            dbHelper.clearMealPlan();
                            render();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.setup(this, bottomNav, BottomNavHelper.Tab.PLANNER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        Map<Long, Recipe> recipesById = new HashMap<>();
        for (Recipe r : dbHelper.getAllRecipes()) recipesById.put(r.getId(), r);

        Map<Integer, Recipe> plan = new HashMap<>(); // key = day * 10 + meal
        for (MealPlanEntry e : dbHelper.getMealPlan()) {
            Recipe r = recipesById.get(e.getRecipeId());
            if (r != null) plan.put(e.getDayIndex() * 10 + e.getMealType(), r);
        }

        String[] dayNames = getResources().getStringArray(R.array.planner_days);
        String[] mealNames = getResources().getStringArray(R.array.planner_meals);
        int today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday = 0

        LayoutInflater inflater = LayoutInflater.from(this);
        container.removeAllViews();

        for (int day = 0; day < MealPlanEntry.DAYS_IN_WEEK; day++) {
            View card = inflater.inflate(R.layout.item_meal_day, container, false);
            String title = dayNames[day];
            if (day == today) title += " \u00B7 " + getString(R.string.planner_today);
            ((TextView) card.findViewById(R.id.textDayTitle)).setText(title);
            LinearLayout slots = card.findViewById(R.id.slotContainer);

            for (int meal = 0; meal < MealPlanEntry.MEALS_PER_DAY; meal++) {
                View slot = inflater.inflate(R.layout.item_meal_slot, slots, false);
                ((TextView) slot.findViewById(R.id.textSlotMeal)).setText(mealNames[meal]);

                TextView recipeText = slot.findViewById(R.id.textSlotRecipe);
                Recipe planned = plan.get(day * 10 + meal);
                if (planned != null) {
                    recipeText.setText(planned.getName());
                } else {
                    recipeText.setText(R.string.planner_tap_to_choose);
                    recipeText.setTextColor(Color.parseColor("#888888"));
                }

                final int d = day;
                final int m = meal;
                final boolean filled = planned != null;
                slot.setOnClickListener(v -> showPicker(d, m, filled));
                slots.addView(slot);
            }
            container.addView(card);
        }
    }

    private void showPicker(int day, int meal, boolean filled) {
        int soonDays = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(SettingsActivity.KEY_EXPIRY_DAYS, 3);
        Calendar now = Calendar.getInstance();
        List<PantryItem> pantry = dbHelper.getAllPantryItems();
        List<MealPlanner.Ranked> ranked = MealPlanner.rankRecipes(dbHelper.getAllRecipes(), pantry,
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
                soonDays);

        ArrayAdapter<MealPlanner.Ranked> adapter = new ArrayAdapter<MealPlanner.Ranked>(this,
                R.layout.item_recipe_pick, ranked) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = convertView != null ? convertView
                        : LayoutInflater.from(getContext()).inflate(R.layout.item_recipe_pick, parent, false);
                MealPlanner.Ranked r = getItem(position);
                TextView name = row.findViewById(R.id.textPickName);
                TextView note = row.findViewById(R.id.textPickNote);
                name.setText(r.recipe.getName());
                if (!r.expiringUsed.isEmpty()) {
                    note.setText(getString(R.string.planner_uses_expiring, TextUtils.join(", ", r.expiringUsed)));
                    note.setTextColor(Color.parseColor("#B26A00")); // amber, same as the expiry warnings
                } else {
                    note.setText(r.missing == 0 ? getString(R.string.planner_can_cook)
                            : getString(R.string.planner_missing, r.missing));
                    note.setTextColor(Color.parseColor("#888888"));
                }
                return row;
            }
        };

        String[] dayNames = getResources().getStringArray(R.array.planner_days);
        String[] mealNames = getResources().getStringArray(R.array.planner_meals);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(dayNames[day] + " \u00B7 " + mealNames[meal])
                .setAdapter(adapter, (dialog, which) -> {
                    dbHelper.setMeal(day, meal, ranked.get(which).recipe.getId());
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (filled) {
            builder.setNeutralButton(R.string.planner_remove, (dialog, which) -> {
                dbHelper.clearMeal(day, meal);
                render();
            });
        }
        builder.show();
    }
}
