package com.example.smartpantry.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.adapters.PantryAdapter;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.utils.BottomNavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
                item -> new AlertDialog.Builder(PantryListActivity.this)
                        .setTitle(R.string.delete_ingredient)
                        .setMessage(R.string.delete_confirmation_message)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            dbHelper.deletePantryItem(item.getId());
                            List<PantryItem> refreshed = dbHelper.getAllPantryItems();
                            adapter.updateItems(refreshed);
                            toggleEmptyState(refreshed.isEmpty());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());
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
        // Refresh in case an item was added/edited/deleted on the previous screen
        List<PantryItem> items = dbHelper.getAllPantryItems();
        adapter.updateItems(items);
        toggleEmptyState(items.isEmpty());
    }

    private void toggleEmptyState(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
    }
}
