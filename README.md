# Smart Pantry Manager

An Android app (Java) that helps reduce food waste. It tracks the ingredients you actually have at home, suggests recipes you can cook **right now** using only what is in your pantry, plans your week of meals, and shows how much food and money you save or waste.

## Features

**Pantry**
- Add, edit and delete ingredients: name, quantity, unit, optional expiry date and optional price paid
- Expiry highlighting: expired items in bold red, items expiring soon in bold amber (warning window of 1, 3 or 7 days, chosen in Settings)
- "Use these first" card listing expired and soon-to-expire items, most urgent first
- Removing an item asks why (used it up, threw it away, added by mistake) so the dashboard stays accurate

**Recipes**
- 75 built-in recipes with ingredients, steps and a photo each
- **Strict matching:** a recipe is suggested only if every ingredient is in the pantry in at least the required quantity (with weight and volume unit conversion)
- "Almost There" list for recipes missing exactly one ingredient
- **I cooked this:** takes the used amounts out of the pantry and logs them, in one database transaction

**Weekly meal planner**
- Plan breakfast, lunch and dinner for seven days
- The recipe picker puts recipes that use soon-to-expire ingredients first, then recipes with the fewest missing ingredients
- Plan is saved in the database

**Waste and savings dashboard** (Settings, then "Waste and savings dashboard")
- Items used before expiry, estimated money saved, money wasted, grocery spending
- Monthly bar charts for the last six months (drawn with a custom `BarChartView`, no chart library)
- List of recently expired or discarded items

## Technology

| | |
|---|---|
| Language | Java |
| Min / target SDK | 23 / 37 |
| Persistence | SQLite via `SQLiteOpenHelper` (schema version 3) |
| UI | Material Components, RecyclerView, ConstraintLayout |
| Third-party libraries | None beyond AndroidX and Material |
| Tests | JUnit 4 local unit tests |

**Why SQLite:** it works fully offline with no account or service, persistence is easy to demonstrate (close and reopen the app), and it fits relational data (recipes have many ingredients, a plan slot points at a recipe).

## Database

| Table | Purpose | Main columns |
|---|---|---|
| `pantry_items` | What the user has | `id`, `name`, `display_name`, `quantity`, `unit`, `expiry_date`, `price`, `expiry_logged` |
| `recipes` | Built-in recipes | `id`, `name`, `steps` |
| `recipe_ingredients` | Ingredients per recipe | `id`, `recipe_id` (FK), `ingredient_name`, `required_quantity`, `unit` |
| `meal_plan` | Weekly planner slots | `id`, `day_index` (0 = Monday), `meal_type` (0 breakfast, 1 lunch, 2 dinner), `recipe_id`; unique per day and meal |
| `food_events` | Log behind the dashboard | `id`, `event_type` (purchased, used, expired, discarded), `item_name`, `quantity`, `unit`, `value`, `event_date`, `before_expiry` |

Upgrades are non-destructive: moving from an older schema adds the new tables and columns and keeps existing pantry data.

## Project structure

```
com.example.smartpantry
├── activities/   Pantry list, add/edit, suggestions, recipe detail, planner, settings, dashboard
├── adapters/     RecyclerView adapters
├── database/     DatabaseHelper (all SQL), RecipeSeeder (75 recipes)
├── models/       PantryItem, Recipe, RecipeIngredient, MealPlanEntry, FoodEvent
├── utils/        Logic with no UI code (see below)
└── views/        BarChartView
```

Core logic lives in `utils/` and has no Android dependencies where possible, so it is unit tested:

| Class | Responsibility |
|---|---|
| `IngredientMatcher` | Strict matching, name normalisation, unit conversion (g/kg, ml/l/tsp/tbsp) |
| `ExpiryUtils` | Days until expiry from calendar dates, so daylight saving can never shift a result; warning window |
| `PantryCooker` | Works out the pantry changes when a recipe is cooked |
| `MealPlanner` | Ranks recipes by expiring ingredients; totals what a plan needs minus the pantry |
| `WasteStats` | Builds food-log events and totals them per month |

## Setup and run

1. Clone the repository:
   ```
   git clone <this-repo-url>
   ```
   ```
   [https://github.com/Blackout24/SmartPantry](https://github.com/Blackout24/SmartPantry.git)
   ```
2. Open the folder in Android Studio (File, then Open) and let Gradle sync finish.
3. Create or select an emulator (any device with API 23 or higher), or connect a phone with USB debugging on.
4. Click **Run**. The 75 recipes are seeded automatically on first launch.

## Run the tests

In Android Studio: right-click `app/src/test/java` and choose **Run Tests**. From a terminal:

```
./gradlew testDebugUnitTest
```

67 tests cover expiry dates (leap years, month ends, daylight-saving changes in several time zones), unit conversion, cooking every one of the 75 recipes, meal-plan ranking, shopping-list maths and the dashboard totals.

## Known limitations

- Expiry reminders are shown inside the app only; there are no Android push notifications yet
- The meal planner has the shopping-list calculation (`MealPlanner.shoppingList`) but no screen for it yet
- The Settings units preference is saved but display conversion is not applied
- The dashboard charts show the last six months
- Money values are estimates based on the prices the user enters

## Image credits

Recipe photos: *add the source and license here (for example Unsplash or Pexels)*.

## Author

*Your name, student number, module and institution.*
