package com.example.smartpantry.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.smartpantry.R;
import com.example.smartpantry.activities.PantryListActivity;
import com.example.smartpantry.activities.SettingsActivity;
import com.example.smartpantry.activities.SuggestedRecipesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Wires up the bottom navigation bar on the three top-level screens
 * (Pantry, Suggested Recipes, Settings). Each screen is its own Activity,
 * so switching tabs starts the target Activity and finishes the current one
 * to avoid stacking duplicate screens.
 */
public class BottomNavHelper {

    public enum Tab { PANTRY, SUGGESTIONS, SETTINGS }

    public static void setup(Activity activity, BottomNavigationView navView, Tab currentTab) {
        switch (currentTab) {
            case PANTRY: navView.setSelectedItemId(R.id.navPantry); break;
            case SUGGESTIONS: navView.setSelectedItemId(R.id.navSuggestions); break;
            case SETTINGS: navView.setSelectedItemId(R.id.navSettings); break;
        }

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navPantry && currentTab != Tab.PANTRY) {
                switchTo(activity, PantryListActivity.class);
                return false; // keep this screen's own tab highlighted
            } else if (id == R.id.navSuggestions && currentTab != Tab.SUGGESTIONS) {
                switchTo(activity, SuggestedRecipesActivity.class);
                return false; // keep this screen's own tab highlighted
            } else if (id == R.id.navSettings && currentTab != Tab.SETTINGS) {
                switchTo(activity, SettingsActivity.class);
                return false; // keep this screen's own tab highlighted
            }
            return true; // tapping the already-selected tab does nothing
        });
    }

    private static void switchTo(Activity activity, Class<?> destination) {
        Intent intent = new Intent(activity, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }
}
