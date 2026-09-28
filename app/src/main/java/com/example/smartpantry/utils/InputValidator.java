package com.example.smartpantry.utils;

import android.text.TextUtils;

public class InputValidator {

    public static String validateName(String name) {
        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            return "Ingredient name is required";
        }
        return null;
    }

    public static String validateQuantity(String quantityText) {
        if (TextUtils.isEmpty(quantityText)) {
            return "Quantity is required";
        }
        try {
            double value = Double.parseDouble(quantityText);
            if (value <= 0) {
                return "Quantity must be greater than 0";
            }
        } catch (NumberFormatException e) {
            return "Enter a valid number";
        }
        return null;
    }
}
