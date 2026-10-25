package com.example.smartpantry.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantry.R;
import com.example.smartpantry.activities.SettingsActivity;
import com.example.smartpantry.models.PantryItem;
import com.example.smartpantry.utils.ExpiryUtils;

import java.util.Calendar;
import java.util.List;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PantryItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(PantryItem item);
    }

    private final List<PantryItem> items;
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longClickListener;

    public PantryAdapter(List<PantryItem> items, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.items = items;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);
        holder.name.setText(item.getDisplayName());
        String qty = trimTrailingZero(item.getQuantity()) + (item.getUnit() != null ? " " + item.getUnit() : "");
        holder.quantity.setText(qty);
        bindExpiry(holder, item);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onItemLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Refreshes the list contents without recreating the adapter. */
    public void updateItems(List<PantryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    private void bindExpiry(PantryViewHolder holder, PantryItem item) {
        Context ctx = holder.itemView.getContext();
        String date = item.getExpiryDate();
        if (date == null || date.isEmpty()) {
            holder.expiry.setVisibility(View.GONE);
            return;
        }
        holder.expiry.setVisibility(View.VISIBLE);

        boolean alertsOn = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(SettingsActivity.KEY_EXPIRY_ALERTS, true);
        Integer days = ExpiryUtils.daysUntil(date, Calendar.getInstance());
        int soonDays = ctx.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(SettingsActivity.KEY_EXPIRY_DAYS, 3);
        ExpiryUtils.Status status = alertsOn ? ExpiryUtils.statusFor(days, soonDays) : ExpiryUtils.Status.NONE;

        switch (status) {
            case EXPIRED:
                holder.expiry.setText(ctx.getString(R.string.expired_on, date));
                holder.expiry.setTextColor(Color.parseColor("#C62828")); // red
                holder.expiry.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case SOON:
                holder.expiry.setText(days == 0 ? ctx.getString(R.string.expires_today)
                        : days == 1 ? ctx.getString(R.string.expires_tomorrow)
                          : ctx.getString(R.string.expires_in_days, days));
                holder.expiry.setTextColor(Color.parseColor("#B26A00")); // amber
                holder.expiry.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            default:
                holder.expiry.setText(ctx.getString(R.string.expires_on, date));
                holder.expiry.setTextColor(Color.parseColor("#888888")); // reset for recycled views
                holder.expiry.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private String trimTrailingZero(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    static class PantryViewHolder extends RecyclerView.ViewHolder {
        TextView name, quantity, expiry;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textIngredientName);
            quantity = itemView.findViewById(R.id.textIngredientQuantity);
            expiry = itemView.findViewById(R.id.textIngredientExpiry);
        }
    }
}