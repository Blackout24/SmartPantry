package com.example.smartpantry.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.FoodEvent;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;
import com.example.smartpantry.utils.PantryCooker;
import com.example.smartpantry.utils.RecipeImages;
import com.example.smartpantry.utils.WasteStats;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long recipeId = getIntent().getLongExtra(SuggestedRecipesActivity.EXTRA_RECIPE_ID, -1);
        Recipe recipe = dbHelper.getRecipe(recipeId);

        TextView nameView = findViewById(R.id.textRecipeDetailName);
        TextView ingredientsView = findViewById(R.id.textRecipeDetailIngredients);
        TextView stepsView = findViewById(R.id.textRecipeDetailSteps);

        if (recipe == null) {
            nameView.setText(R.string.recipe_not_found);
            findViewById(R.id.buttonCooked).setVisibility(View.GONE);
            return;
        }

        setTitle(recipe.getName());
        nameView.setText(recipe.getName());
        RecipeImages.apply(findViewById(R.id.imageRecipe), recipe.getName());

        StringBuilder ingredientsText = new StringBuilder();
        for (RecipeIngredient ing : recipe.getIngredients()) {
            ingredientsText.append("• ")
                    .append(ing.getIngredientName())
                    .append(" - ")
                    .append(formatQuantity(ing.getRequiredQuantity()))
                    .append(ing.getUnit() != null ? " " + ing.getUnit() : "")
                    .append("\n");
        }
        ingredientsView.setText(ingredientsText.toString().trim());
        stepsView.setText(recipe.getSteps());

        findViewById(R.id.buttonCooked).setOnClickListener(v -> confirmCooked(dbHelper, recipe));
    }

    /** "I cooked this": show what will be used, then update the pantry and the food log together. */
    private void confirmCooked(DatabaseHelper dbHelper, Recipe recipe) {
        List<PantryCooker.Change> plan = PantryCooker.plan(recipe, dbHelper.getAllPantryItems());
        if (plan == null) {
            Toast.makeText(this, R.string.cook_missing, Toast.LENGTH_LONG).show();
            return;
        }

        DecimalFormat format = new DecimalFormat("0.##");
        StringBuilder message = new StringBuilder(getString(R.string.cook_confirm_message)).append("\n");
        for (PantryCooker.Change c : plan) {
            PantryItem item = c.item;
            message.append("\n\u2022 ").append(item.getDisplayName()).append(": ")
                    .append(format.format(item.getQuantity() - c.newQuantity))
                    .append(item.getUnit() != null ? " " + item.getUnit() : "");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.cook_confirm_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.cook_confirm_yes, (dialog, which) -> {
                    Calendar now = Calendar.getInstance();
                    List<FoodEvent> events = WasteStats.cookEvents(plan, now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
                    dbHelper.applyCook(plan, events);
                    Toast.makeText(this, R.string.cook_done, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String formatQuantity(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.valueOf(value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
