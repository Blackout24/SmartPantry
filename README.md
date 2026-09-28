# Smart Pantry Manager

An Android app (Java) that helps reduce food waste by tracking the ingredients you actually have at home and suggesting recipes you can cook **right now** — using strictly the ingredients already in your pantry, no shopping trip required.

## Features
- Add, edit, and delete pantry ingredients (name, quantity, unit, optional expiry date)
- 18 pre-seeded recipes, each with required ingredients and preparation steps
- **Strict-matching suggestions**: a recipe only appears if every required ingredient is present in sufficient quantity — no partial matches
- "Almost There" list for recipes missing exactly one ingredient
- Recipe detail screen with full ingredient list and method
- Settings screen (expiring-soon alerts toggle, metric/imperial, units preference)
- Bottom navigation between Pantry, Suggested Recipes, and Settings

## Database: SQLite
This app uses **SQLite** via `SQLiteOpenHelper`, chosen over Firebase/PostgreSQL because:
- It works fully offline with no external service or account setup required
- Data persistence is simple to demonstrate and verify (close and reopen the app)
- It matches the module's coverage of `SQLiteOpenHelper` and local persistent storage directly

Three tables: `pantry_items`, `recipes`, and `recipe_ingredients` (see the project's ER diagram in the written report for the full schema).

## Setup / Run Instructions
1. Clone this repository:
   ```
   git clone <this-repo-url>
   ```
2. Open the project folder in Android Studio (File → Open).
3. Let Gradle sync finish (this may take a few minutes on first open).
4. Create or select an emulator via Device Manager (any API 24+ device works), or connect a physical Android device with USB debugging enabled.
5. Click **Run ▶** to build and launch the app.
6. On first launch, the app automatically seeds 18 starter recipes into the local database — no manual setup needed.

## Core Logic: Strict-Matching Rule
See `utils/IngredientMatcher.java`. A recipe qualifies as "suggested" only if every one of its required ingredients exists in the pantry, normalized for simple plural/casing differences, in at least the required quantity (with basic unit conversion for weight/volume where applicable).
