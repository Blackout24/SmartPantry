package com.example.smartpantry.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.models.RecipeIngredient;

public class RecipeDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        long recipeId = getIntent().getLongExtra(SuggestedRecipesActivity.EXTRA_RECIPE_ID, -1);
        Recipe recipe = dbHelper.getRecipe(recipeId);

        TextView nameView = findViewById(R.id.textRecipeDetailName);
        TextView ingredientsView = findViewById(R.id.textRecipeDetailIngredients);
        TextView stepsView = findViewById(R.id.textRecipeDetailSteps);

        if (recipe == null) {
            nameView.setText(R.string.recipe_not_found);
            return;
        }

        setTitle(recipe.getName());
        nameView.setText(recipe.getName());

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
