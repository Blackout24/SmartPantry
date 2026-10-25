package com.example.smartpantry.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapters.PantryAdapter;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.utils.BottomNavHelper;
import com.example.smartpantry.utils.ExpiryUtils;
import com.example.smartpantry.utils.PantryRemoval;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;

public class PantryListActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id";

    private DatabaseHelper dbHelper;
    private PantryAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantry_list);

        dbHelper = new DatabaseHelper(this);
        emptyView = findViewById(R.id.textEmptyPantry);

        RecyclerView recyclerView = findViewById(R.id.recyclerPantry);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<PantryItem> items = dbHelper.getAllPantryItems();
        adapter = new PantryAdapter(items,
                item -> {
                    Intent intent = new Intent(PantryListActivity.this, AddEditIngredientActivity.class);
                    intent.putExtra(EXTRA_ITEM_ID, item.getId());
                    startActivity(intent);
                },
                item -> PantryRemoval.confirm(PantryListActivity.this, dbHelper, item, this::reload));
        recyclerView.setAdapter(adapter);
        toggleEmptyState(items.isEmpty());

        FloatingActionButton fab = findViewById(R.id.fabAddIngredient);
        fab.setOnClickListener(v ->
                startActivity(new Intent(PantryListActivity.this, AddEditIngredientActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.setup(this, bottomNav, BottomNavHelper.Tab.PANTRY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Record anything that expired since the last visit, then refresh the list.
        Calendar now = Calendar.getInstance();
        dbHelper.sweepExpired(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        reload();
    }

    private void reload() {
        List<PantryItem> items = dbHelper.getAllPantryItems();
        adapter.updateItems(items);
        toggleEmptyState(items.isEmpty());
        refreshUseFirst(items);
    }

    /** "Use these first": expired or soon-to-expire items, most urgent first (max 5 shown). */
    private void refreshUseFirst(List<PantryItem> items) {
        android.view.View card = findViewById(R.id.cardUseFirst);
        android.content.SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsActivity.KEY_EXPIRY_ALERTS, true)) {
            card.setVisibility(android.view.View.GONE);
            return;
        }
        int soonDays = prefs.getInt(SettingsActivity.KEY_EXPIRY_DAYS, 3);
        Calendar today = Calendar.getInstance();
        List<ExpiryUtils.Urgent> urgent = ExpiryUtils.urgentItems(items, today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH), soonDays);
        if (urgent.isEmpty()) {
            card.setVisibility(android.view.View.GONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        int shown = Math.min(5, urgent.size());
        for (int i = 0; i < shown; i++) {
            ExpiryUtils.Urgent u = urgent.get(i);
            String when;
            if (u.daysUntil < 0) when = getString(R.string.expired_on, u.item.getExpiryDate());
            else if (u.daysUntil == 0) when = getString(R.string.expires_today);
            else if (u.daysUntil == 1) when = getString(R.string.expires_tomorrow);
            else when = getString(R.string.expires_in_days, u.daysUntil);
            if (i > 0) sb.append('\n');
            sb.append("\u2022 ").append(u.item.getDisplayName()).append(" \u2013 ").append(when);
        }
        if (urgent.size() > shown) sb.append("\n+").append(urgent.size() - shown).append(" more");
        ((TextView) findViewById(R.id.textUseFirst)).setText(sb.toString());
        card.setVisibility(android.view.View.VISIBLE);
    }

    private void toggleEmptyState(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
