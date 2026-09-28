package com.example.smartpantry.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.utils.InputValidator;

import java.util.Calendar;
import java.util.Locale;

public class AddEditIngredientActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EditText editName, editQuantity, editUnit, editExpiry;
    private Button buttonSave, buttonDelete;

    private long itemId = -1;
    private PantryItem existingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);

        editName = findViewById(R.id.editIngredientName);
        editQuantity = findViewById(R.id.editIngredientQuantity);
        editUnit = findViewById(R.id.editIngredientUnit);
        editExpiry = findViewById(R.id.editIngredientExpiry);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);
        TextView clearExpiry = findViewById(R.id.buttonClearExpiry);

        itemId = getIntent().getLongExtra(PantryListActivity.EXTRA_ITEM_ID, -1);

        if (itemId != -1) {
            existingItem = dbHelper.getPantryItem(itemId);
            if (existingItem != null) {
                setTitle(R.string.edit_ingredient);
                editName.setText(existingItem.getDisplayName());
                editQuantity.setText(String.valueOf(existingItem.getQuantity()));
                editUnit.setText(existingItem.getUnit());
                editExpiry.setText(existingItem.getExpiryDate());
                buttonDelete.setVisibility(android.view.View.VISIBLE);
            }
        } else {
            setTitle(R.string.add_ingredient);
            buttonDelete.setVisibility(android.view.View.GONE);
        }

        editExpiry.setOnClickListener(v -> showDatePicker());
        clearExpiry.setOnClickListener(v -> editExpiry.setText(""));

        buttonSave.setOnClickListener(v -> saveIngredient());
        buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // If a date is already set, start the picker from that date instead of today.
        String current = editExpiry.getText().toString().trim();
        if (!current.isEmpty()) {
            try {
                String[] parts = current.split("-");
                calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {
                // fall back to today if the stored value is somehow malformed
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    editExpiry.setText(formatted);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveIngredient() {
        String name = editName.getText().toString().trim();
        String quantityText = editQuantity.getText().toString().trim();
        String unit = editUnit.getText().toString().trim();
        String expiry = editExpiry.getText().toString().trim();

        String nameError = InputValidator.validateName(name);
        if (nameError != null) {
            editName.setError(nameError);
            editName.requestFocus();
            return;
        }

        String quantityError = InputValidator.validateQuantity(quantityText);
        if (quantityError != null) {
            editQuantity.setError(quantityError);
            editQuantity.requestFocus();
            return;
        }

        double quantity = Double.parseDouble(quantityText);

        PantryItem item = new PantryItem(
                itemId != -1 ? itemId : 0,
                PantryItem.normalize(name),
                name,
                quantity,
                unit.isEmpty() ? null : unit,
                expiry.isEmpty() ? null : expiry
        );

        if (itemId != -1) {
            dbHelper.updatePantryItem(item);
        } else {
            dbHelper.addPantryItem(item);
        }
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_ingredient)
                .setMessage(R.string.delete_confirmation_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deletePantryItem(itemId);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
