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