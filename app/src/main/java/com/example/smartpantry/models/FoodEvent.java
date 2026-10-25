package com.example.smartpantry.models;

/** One entry in the food log that powers the dashboard. */
public class FoodEvent {
    public static final int PURCHASED = 0; // added to the pantry with a price
    public static final int USED = 1;      // cooked / used up
    public static final int EXPIRED = 2;   // went past its expiry date
    public static final int DISCARDED = 3; // thrown away before expiring

    private final long id;
    private final int type;
    private final String itemName;
    private final double quantity;
    private final String unit;
    private final double value;          // money value of this event; 0 when no price is known
    private final String date;           // "yyyy-MM-dd"
    private final boolean beforeExpiry;  // USED only: item had an expiry date and had not passed it

    public FoodEvent(long id, int type, String itemName, double quantity, String unit,
                     double value, String date, boolean beforeExpiry) {
        this.id = id;
        this.type = type;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unit = unit;
        this.value = value;
        this.date = date;
        this.beforeExpiry = beforeExpiry;
    }

    public long getId() { return id; }
    public int getType() { return type; }
    public String getItemName() { return itemName; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getValue() { return value; }
    public String getDate() { return date; }
    public boolean isBeforeExpiry() { return beforeExpiry; }
}