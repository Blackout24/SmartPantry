package com.example.smartpantry.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.database.DatabaseHelper;
import com.example.smartpantry.models.FoodEvent;
import com.example.smartpantry.utils.WasteStats;
import com.example.smartpantry.views.BarChartView;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

/** Waste and savings dashboard: this month's numbers, monthly charts and recent waste. */
public class DashboardActivity extends AppCompatActivity {

    private static final int MONTHS = 6;
    private static final int COLOR_SAVED = Color.parseColor("#2E7D32");
    private static final int COLOR_WASTED = Color.parseColor("#C62828");
    private static final int COLOR_SPENT = Color.parseColor("#1565C0");

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        Calendar now = Calendar.getInstance();
        int y = now.get(Calendar.YEAR);
        int m = now.get(Calendar.MONTH) + 1;
        int d = now.get(Calendar.DAY_OF_MONTH);

        dbHelper.sweepExpired(y, m, d); // make sure anything that just expired is counted
        List<FoodEvent> events = dbHelper.getAllFoodEvents();
        List<WasteStats.MonthStats> months = WasteStats.monthly(events, y, m, MONTHS);
        WasteStats.MonthStats current = months.get(MONTHS - 1);
        WasteStats.MonthStats all = WasteStats.total(events);
        NumberFormat money = NumberFormat.getCurrencyInstance();

        // This month
        LinearLayout row1 = findViewById(R.id.rowStats1);
        LinearLayout row2 = findViewById(R.id.rowStats2);
        row1.removeAllViews();
        row2.removeAllViews();
        addStat(row1, String.valueOf(current.usedBeforeExpiry), getString(R.string.stat_used_before_expiry));
        addStat(row1, money.format(current.saved), getString(R.string.stat_saved));
        addStat(row2, money.format(current.wasted), getString(R.string.stat_wasted, current.wastedCount()));
        addStat(row2, money.format(current.spent), getString(R.string.stat_spent));

        // All time
        TextView allTime = findViewById(R.id.textAllTime);
        if (all.usedTotal + all.wastedCount() == 0) {
            allTime.setText(R.string.all_time_empty);
        } else {
            allTime.setText(getString(R.string.all_time_summary, all.usedBeforeExpiry, all.usedTotal,
                    (int) Math.round(all.wasteRate() * 100)));
        }

        // Charts
        String[] shortMonths = new DateFormatSymbols().getShortMonths();
        String[] labels = new String[MONTHS];
        double[] saved = new double[MONTHS];
        double[] wasted = new double[MONTHS];
        double[] spent = new double[MONTHS];
        for (int i = 0; i < MONTHS; i++) {
            WasteStats.MonthStats s = months.get(i);
            labels[i] = shortMonths[s.month - 1];
            saved[i] = s.saved;
            wasted[i] = s.wasted;
            spent[i] = s.spent;
        }
        ((BarChartView) findViewById(R.id.chartSavedWasted)).setData(labels,
                new String[]{getString(R.string.legend_saved), getString(R.string.legend_wasted)},
                new int[]{COLOR_SAVED, COLOR_WASTED}, new double[][]{saved, wasted});
        ((BarChartView) findViewById(R.id.chartSpent)).setData(labels,
                new String[]{getString(R.string.legend_spent)},
                new int[]{COLOR_SPENT}, new double[][]{spent});

        // Recent waste
        TextView recent = findViewById(R.id.textRecentWaste);
        List<FoodEvent> waste = WasteStats.recentWaste(events, 8);
        if (waste.isEmpty()) {
            recent.setText(R.string.recent_waste_empty);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < waste.size(); i++) {
                FoodEvent e = waste.get(i);
                if (i > 0) sb.append('\n');
                sb.append("\u2022 ").append(e.getItemName()).append(" \u2013 ")
                        .append(getString(e.getType() == FoodEvent.EXPIRED
                                ? R.string.event_expired : R.string.event_discarded))
                        .append(" \u00B7 ").append(e.getDate());
                if (e.getValue() > 0) sb.append(" \u00B7 ").append(money.format(e.getValue()));
            }
            recent.setText(sb.toString());
        }

        boolean noMoney = all.spent == 0 && all.saved == 0 && all.wasted == 0;
        boolean hasActivity = all.usedTotal + all.wastedCount() > 0;
        findViewById(R.id.textPricesHint).setVisibility(noMoney && hasActivity ? View.VISIBLE : View.GONE);
    }

    private void addStat(LinearLayout row, String value, String label) {
        View cell = LayoutInflater.from(this).inflate(R.layout.item_dashboard_stat, row, false);
        ((TextView) cell.findViewById(R.id.textStatValue)).setText(value);
        ((TextView) cell.findViewById(R.id.textStatLabel)).setText(label);
        row.addView(cell);
    }
}
