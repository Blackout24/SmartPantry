package com.example.smartpantry.models;

public class PantryItem {
    private long id;
    private String name;         // normalized (lowercase, singular) - used for matching
    private String displayName;  // original text shown in UI
    private double quantity;
    private String unit;
    private String expiryDate;   // ISO 8601 "yyyy-MM-dd", nullable

    public PantryItem() { }

    public PantryItem(long id, String name, String displayName, double quantity, String unit, String expiryDate) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    /**
     * Normalizes raw user input into the matching key: lowercase, trimmed, singular.
     * Handles common English plurals: tomatoes -> tomato, berries -> berry,
     * peaches -> peach, apples -> apple, zucchinis -> zucchini, onions -> onion.
     * Words ending in "ss" or "us" (e.g. hummus, asparagus) are left alone.
     */
    public static String normalize(String rawName) {
        if (rawName == null) return "";
        String s = rawName.trim().toLowerCase().replaceAll("\\s+", " ");
        if (s.length() <= 3) return s;

        if (s.endsWith("ies")) {
            return s.substring(0, s.length() - 3) + "y";
        }
        if (s.endsWith("oes") || s.endsWith("ches") || s.endsWith("shes")
                || s.endsWith("sses") || s.endsWith("xes") || s.endsWith("zes")) {
            return s.substring(0, s.length() - 2);
        }
        if (s.endsWith("s") && !s.endsWith("ss") && !s.endsWith("us")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
