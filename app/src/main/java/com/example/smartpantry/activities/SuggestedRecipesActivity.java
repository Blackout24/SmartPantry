package com.example.smartpantry.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapters.RecipeAdapter;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.database.RecipeSeeder;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.models.Recipe;
import com.example.smartpantry.utils.BottomNavHelper;
import com.example.smartpantry.utils.IngredientMatcher;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class SuggestedRecipesActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private DatabaseHelper dbHelper;
    private RecipeAdapter suggestedAdapter;
    private RecipeAdapter almostThereAdapter;
    private TextView emptyView, almostThereHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);
        RecipeSeeder.seedMissingRecipes(dbHelper);

        emptyView = findViewById(R.id.textEmptySuggestions);
        almostThereHeader = findViewById(R.id.textAlmostThereHeader);

        RecyclerView suggestedRecycler = findViewById(R.id.recyclerSuggested);
        suggestedRecycler.setLayoutManager(new LinearLayoutManager(this));
        suggestedAdapter = new RecipeAdapter(new ArrayList<>(), false, this::openRecipeDetail);
        suggestedRecycler.setAdapter(suggestedAdapter);

        RecyclerView almostThereRecycler = findViewById(R.id.recyclerAlmostThere);
        almostThereRecycler.setLayoutManager(new LinearLayoutManager(this));
        almostThereAdapter = new RecipeAdapter(new ArrayList<>(), true, this::openRecipeDetail);
        almostThereRecycler.setAdapter(almostThereAdapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.setup(this, bottomNav, BottomNavHelper.Tab.SUGGESTIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSuggestions();
    }

    private void refreshSuggestions() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes();
        List<PantryItem> pantry = dbHelper.getAllPantryItems();

        IngredientMatcher.MatchResult result = IngredientMatcher.match(allRecipes, pantry);

        suggestedAdapter.updateRecipes(result.suggested);
        almostThereAdapter.updateRecipes(result.almostThere);

        emptyView.setVisibility(result.suggested.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        almostThereHeader.setVisibility(result.almostThere.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void openRecipeDetail(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
