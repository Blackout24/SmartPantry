package com.example.smartpantry.utils;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.FoodEvent;
import com.example.smartpantry.models.PantryItem;

import java.util.Calendar;

/** Asks why an item is being removed, so the dashboard can tell "used" from "wasted". */
public class PantryRemoval {

    public static void confirm(Context context, DatabaseHelper dbHelper, PantryItem item, Runnable onRemoved) {
        String[] options = {
                context.getString(R.string.remove_used),
                context.getString(R.string.remove_thrown),
                context.getString(R.string.remove_mistake)
        };
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.remove_title, item.getDisplayName()))
                .setItems(options, (dialog, which) -> {
                    Calendar now = Calendar.getInstance();
                    int y = now.get(Calendar.YEAR);
                    int m = now.get(Calendar.MONTH) + 1;
                    int d = now.get(Calendar.DAY_OF_MONTH);
                    FoodEvent event = null;
                    if (which == 0) event = WasteStats.usedUpEvent(item, y, m, d);
                    else if (which == 1) event = WasteStats.removalEvent(item, y, m, d);
                    // which == 2: entered by mistake, nothing is recorded
                    dbHelper.removePantryItem(item.getId(), event);
                    onRemoved.run();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
