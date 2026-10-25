package com.example.smartpantry.models;

/** One slot in the weekly plan: a recipe assigned to a day and a meal. */
public class MealPlanEntry {
    public static final int BREAKFAST = 0;
    public static final int LUNCH = 1;
    public static final int DINNER = 2;
    public static final int DAYS_IN_WEEK = 7;   // dayIndex 0 = Monday ... 6 = Sunday
    public static final int MEALS_PER_DAY = 3;

    private final long id;
    private final int dayIndex;
    private final int mealType;
    private final long recipeId;

    public MealPlanEntry(long id, int dayIndex, int mealType, long recipeId) {
        this.id = id;
        this.dayIndex = dayIndex;
        this.mealType = mealType;
        this.recipeId = recipeId;
    }

    public long getId() { return id; }
    public int getDayIndex() { return dayIndex; }
    public int getMealType() { return mealType; }
    public long getRecipeId() { return recipeId; }
}