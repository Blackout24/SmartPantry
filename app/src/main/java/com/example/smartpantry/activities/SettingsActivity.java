package com.example.smartpantry.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartpantry.R;
import com.example.smartpantry.utils.BottomNavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "smart_pantry_prefs";
    public static final String KEY_EXPIRY_ALERTS = "expiry_alerts_enabled";
    public static final String KEY_EXPIRY_DAYS = "expiry_window_days"; // 1, 3 or 7
    public static final String KEY_UNITS = "units_preference"; // "metric" or "imperial"

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Switch switchExpiryAlerts = findViewById(R.id.switchExpiryAlerts);
        RadioGroup radioUnits = findViewById(R.id.radioGroupUnits);

        switchExpiryAlerts.setChecked(prefs.getBoolean(KEY_EXPIRY_ALERTS, true));
        boolean isMetric = prefs.getString(KEY_UNITS, "metric").equals("metric");
        radioUnits.check(isMetric ? R.id.radioMetric : R.id.radioImperial);

        RadioGroup radioDays = findViewById(R.id.radioGroupExpiryDays);
        int days = prefs.getInt(KEY_EXPIRY_DAYS, 3);
        radioDays.check(days == 1 ? R.id.radioDays1 : days == 7 ? R.id.radioDays7 : R.id.radioDays3);
        radioDays.setOnCheckedChangeListener((group, checkedId) -> {
            int value = checkedId == R.id.radioDays1 ? 1 : checkedId == R.id.radioDays7 ? 7 : 3;
            prefs.edit().putInt(KEY_EXPIRY_DAYS, value).apply();
        });

        switchExpiryAlerts.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_EXPIRY_ALERTS, isChecked).apply());

        radioUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String value = (checkedId == R.id.radioMetric) ? "metric" : "imperial";
            prefs.edit().putString(KEY_UNITS, value).apply();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        BottomNavHelper.setup(this, bottomNav, BottomNavHelper.Tab.SETTINGS);
    }
}