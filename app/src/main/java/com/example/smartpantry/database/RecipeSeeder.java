package com.example.smartpantry.database;

import com.example.smartpantry.models.RecipeIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Seeds the built-in recipe collection. Call RecipeSeeder.seedMissingRecipes(dbHelper).
 * Recipes are matched by name, so only recipes that are not yet in the database are
 * inserted - new recipes added in an app update appear without wiping the user's pantry.
 * Ingredient names are written in singular form (they are normalized again on insert).
 */
public class RecipeSeeder {

    private static class SeedRecipe {
        final String name;
        final String steps;
        final List<RecipeIngredient> ingredients;

        SeedRecipe(String name, String steps, List<RecipeIngredient> ingredients) {
            this.name = name;
            this.steps = steps;
            this.ingredients = ingredients;
        }
    }

    public static void seedMissingRecipes(DatabaseHelper db) {
        Set<String> existing = db.getAllRecipeNames();
        for (SeedRecipe recipe : buildRecipes()) {
            if (!existing.contains(recipe.name)) {
                db.addRecipe(recipe.name, recipe.steps, recipe.ingredients);
            }
        }
    }

    /** All built-in recipes as plain Recipe objects (no database needed). Used by unit tests. */
    public static List<com.example.smartpantry.models.Recipe> getSeedRecipes() {
        List<com.example.smartpantry.models.Recipe> out = new ArrayList<>();
        for (SeedRecipe sr : buildRecipes()) {
            com.example.smartpantry.models.Recipe rec = new com.example.smartpantry.models.Recipe(0, sr.name, sr.steps);
            rec.setIngredients(sr.ingredients);
            out.add(rec);
        }
        return out;
    }

    private static List<SeedRecipe> buildRecipes() {
        List<SeedRecipe> r = new ArrayList<>();

        // ---------- Original starter set ----------
        add(r, "Tomato Egg Scramble", "1. Beat eggs.\n2. Fry tomato until soft.\n3. Add eggs, scramble together.\n4. Season with salt and pepper.",
                ing("egg", 2, "pcs"), ing("tomato", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Garlic Butter Pasta", "1. Boil pasta.\n2. Melt butter, fry garlic.\n3. Toss pasta in garlic butter.\n4. Season and serve.",
                ing("pasta", 200, "g"), ing("butter", 30, "g"), ing("garlic", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Rice and Bean Bowl", "1. Cook rice.\n2. Heat beans with onion.\n3. Combine and serve.",
                ing("rice", 200, "g"), ing("bean", 150, "g"), ing("onion", 1, "pcs"));

        add(r, "Cheese Toastie", "1. Butter bread.\n2. Add cheese between slices.\n3. Grill until golden.",
                ing("bread", 2, "pcs"), ing("cheese", 50, "g"), ing("butter", 10, "g"));

        add(r, "Chicken Stir Fry", "1. Slice chicken and vegetables.\n2. Stir fry chicken until cooked.\n3. Add vegetables and soy sauce, cook through.",
                ing("chicken", 300, "g"), ing("carrot", 1, "pcs"), ing("onion", 1, "pcs"), ing("soy sauce", 2, "tbsp"));

        add(r, "Potato Onion Hash", "1. Dice potato and onion.\n2. Fry together until golden and soft.\n3. Season and serve.",
                ing("potato", 2, "pcs"), ing("onion", 1, "pcs"), ing("oil", 2, "tbsp"));

        add(r, "Simple Tomato Soup", "1. Cook tomato and onion until soft.\n2. Blend smooth.\n3. Simmer with salt to taste.",
                ing("tomato", 4, "pcs"), ing("onion", 1, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Banana Oat Pancakes", "1. Mash banana.\n2. Mix with oats and egg.\n3. Fry spoonfuls until golden on both sides.",
                ing("banana", 1, "pcs"), ing("oat", 100, "g"), ing("egg", 1, "pcs"));

        add(r, "Carrot Ginger Soup", "1. Cook carrot and ginger in water until soft.\n2. Blend smooth.\n3. Season and serve.",
                ing("carrot", 3, "pcs"), ing("ginger", 1, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Spinach Feta Wrap", "1. Wilt spinach.\n2. Fill tortilla with spinach and feta.\n3. Roll and serve.",
                ing("spinach", 100, "g"), ing("feta", 50, "g"), ing("tortilla", 1, "pcs"));

        add(r, "Lentil Curry", "1. Cook lentils until soft.\n2. Fry onion and garlic.\n3. Combine with spices and simmer.",
                ing("lentil", 200, "g"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"), ing("curry powder", 1, "tbsp"));

        add(r, "Egg Fried Rice", "1. Scramble eggs.\n2. Fry rice with egg and vegetables.\n3. Add soy sauce and serve.",
                ing("rice", 200, "g"), ing("egg", 2, "pcs"), ing("carrot", 1, "pcs"), ing("soy sauce", 2, "tbsp"));

        add(r, "Mushroom Toast", "1. Fry mushroom in butter.\n2. Pile onto toasted bread.\n3. Season and serve.",
                ing("mushroom", 150, "g"), ing("bread", 2, "pcs"), ing("butter", 15, "g"));

        add(r, "Cucumber Yogurt Salad", "1. Slice cucumber.\n2. Mix with yogurt and salt.\n3. Chill and serve.",
                ing("cucumber", 1, "pcs"), ing("yogurt", 100, "g"), ing("salt", 1, "tsp"));

        add(r, "Apple Cinnamon Oats", "1. Cook oats with water.\n2. Stir in diced apple and cinnamon.\n3. Serve warm.",
                ing("oat", 100, "g"), ing("apple", 1, "pcs"), ing("cinnamon", 1, "tsp"));

        add(r, "Beef and Onion Stew", "1. Brown beef.\n2. Add onion and water, simmer until tender.\n3. Season and serve.",
                ing("beef", 300, "g"), ing("onion", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Pepper Cheese Omelette", "1. Beat eggs.\n2. Fry pepper until soft.\n3. Add eggs and cheese, fold and cook through.",
                ing("egg", 3, "pcs"), ing("bell pepper", 1, "pcs"), ing("cheese", 40, "g"));

        add(r, "Zucchini Garlic Saute", "1. Slice zucchini.\n2. Fry with garlic in oil until tender.\n3. Season and serve.",
                ing("zucchini", 2, "pcs"), ing("garlic", 2, "pcs"), ing("oil", 1, "tbsp"));

        // ---------- Breakfast & eggs ----------
        add(r, "Fluffy Pancakes", "1. Whisk flour, sugar, egg and milk into a smooth batter.\n2. Melt a little butter in a pan.\n3. Pour in small ladles of batter and cook until bubbles form.\n4. Flip and cook until golden.",
                ing("flour", 200, "g"), ing("milk", 300, "ml"), ing("egg", 1, "pcs"), ing("sugar", 2, "tbsp"), ing("butter", 20, "g"));

        add(r, "French Toast", "1. Whisk egg, milk and cinnamon in a shallow dish.\n2. Soak each bread slice on both sides.\n3. Fry in butter until golden on both sides.",
                ing("bread", 4, "pcs"), ing("egg", 2, "pcs"), ing("milk", 100, "ml"), ing("cinnamon", 1, "tsp"), ing("butter", 10, "g"));

        add(r, "Scrambled Eggs on Toast", "1. Toast the bread.\n2. Beat eggs with salt.\n3. Scramble gently in melted butter.\n4. Spoon over the toast.",
                ing("egg", 3, "pcs"), ing("bread", 2, "pcs"), ing("butter", 10, "g"), ing("salt", 1, "tsp"));

        add(r, "Classic Omelette", "1. Beat eggs with salt.\n2. Melt butter in a pan over medium heat.\n3. Pour in eggs and cook until nearly set.\n4. Fold and serve.",
                ing("egg", 3, "pcs"), ing("butter", 10, "g"), ing("salt", 1, "tsp"));

        add(r, "Cheesy Scrambled Eggs", "1. Beat eggs.\n2. Scramble in melted butter over low heat.\n3. Stir in grated cheese just before the eggs set.",
                ing("egg", 3, "pcs"), ing("cheese", 30, "g"), ing("butter", 10, "g"));

        add(r, "Spinach Omelette", "1. Wilt spinach in a pan.\n2. Pour beaten eggs over the spinach.\n3. Sprinkle with cheese, fold and cook through.",
                ing("egg", 3, "pcs"), ing("spinach", 50, "g"), ing("cheese", 30, "g"));

        add(r, "Fried Egg Sandwich", "1. Fry the eggs in butter.\n2. Toast the bread.\n3. Layer egg and cheese between the slices.",
                ing("egg", 2, "pcs"), ing("bread", 2, "pcs"), ing("butter", 10, "g"), ing("cheese", 20, "g"));

        add(r, "Egg Mayo Sandwich", "1. Hard boil the eggs for 10 minutes, cool and peel.\n2. Mash with mayonnaise and salt.\n3. Spread onto bread and close the sandwich.",
                ing("egg", 3, "pcs"), ing("bread", 2, "pcs"), ing("mayonnaise", 2, "tbsp"), ing("salt", 1, "tsp"));

        add(r, "Porridge with Honey", "1. Simmer oats in milk, stirring, until thick.\n2. Pour into a bowl.\n3. Drizzle with honey.",
                ing("oat", 60, "g"), ing("milk", 250, "ml"), ing("honey", 1, "tbsp"));

        add(r, "Overnight Oats", "1. Mix oats, milk, yogurt and honey in a jar.\n2. Cover and refrigerate overnight.\n3. Stir and eat cold.",
                ing("oat", 60, "g"), ing("milk", 150, "ml"), ing("yogurt", 50, "g"), ing("honey", 1, "tbsp"));

        add(r, "Berry Yogurt Bowl", "1. Spoon yogurt into a bowl.\n2. Top with sliced strawberries and oats.\n3. Drizzle with honey.",
                ing("yogurt", 200, "g"), ing("strawberry", 100, "g"), ing("honey", 1, "tbsp"), ing("oat", 30, "g"));

        add(r, "Banana Smoothie", "1. Peel the bananas.\n2. Blend with milk and honey until smooth.\n3. Serve cold.",
                ing("banana", 2, "pcs"), ing("milk", 300, "ml"), ing("honey", 1, "tbsp"));

        add(r, "Peanut Butter Banana Toast", "1. Toast the bread.\n2. Spread with peanut butter.\n3. Top with sliced banana.",
                ing("bread", 2, "pcs"), ing("peanut butter", 2, "tbsp"), ing("banana", 1, "pcs"));

        add(r, "Avocado Toast", "1. Toast the bread.\n2. Mash avocado with lemon juice and salt.\n3. Spread on the toast.",
                ing("avocado", 1, "pcs"), ing("bread", 2, "pcs"), ing("lemon", 1, "pcs"), ing("salt", 1, "tsp"));

        // ---------- Pasta & rice ----------
        add(r, "Tomato Garlic Pasta", "1. Boil pasta until al dente.\n2. Fry garlic in oil, add chopped tomatoes and simmer 10 minutes.\n3. Toss the pasta through the sauce with salt.",
                ing("pasta", 200, "g"), ing("tomato", 4, "pcs"), ing("garlic", 2, "pcs"), ing("oil", 2, "tbsp"), ing("salt", 1, "tsp"));

        add(r, "Macaroni Cheese", "1. Boil the pasta.\n2. Melt butter, stir in flour, then whisk in milk to make a sauce.\n3. Melt in the cheese.\n4. Mix in the pasta and serve.",
                ing("pasta", 200, "g"), ing("cheese", 150, "g"), ing("milk", 300, "ml"), ing("butter", 30, "g"), ing("flour", 2, "tbsp"));

        add(r, "Spaghetti Aglio e Olio", "1. Boil the pasta.\n2. Gently fry sliced garlic and chili flakes in olive oil.\n3. Toss the pasta in the oil and serve.",
                ing("pasta", 200, "g"), ing("garlic", 4, "pcs"), ing("olive oil", 4, "tbsp"), ing("chili flake", 1, "tsp"));

        add(r, "Tuna Pasta", "1. Boil the pasta.\n2. Warm garlic in olive oil, then add the tuna.\n3. Toss with the pasta and serve.",
                ing("pasta", 200, "g"), ing("tuna", 1, "pcs"), ing("olive oil", 2, "tbsp"), ing("garlic", 1, "pcs"));

        add(r, "Spaghetti Bolognese", "1. Brown the beef with chopped onion and garlic.\n2. Add chopped tomatoes and simmer 20 minutes.\n3. Serve over boiled pasta.",
                ing("pasta", 200, "g"), ing("beef", 300, "g"), ing("tomato", 4, "pcs"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"));

        add(r, "Chicken Fried Rice", "1. Cook and cool the rice.\n2. Stir fry diced chicken and onion until cooked.\n3. Add rice and scrambled egg.\n4. Season with soy sauce.",
                ing("rice", 200, "g"), ing("chicken", 200, "g"), ing("egg", 2, "pcs"), ing("soy sauce", 2, "tbsp"), ing("onion", 1, "pcs"));

        add(r, "Vegetable Fried Rice", "1. Cook and cool the rice.\n2. Stir fry diced carrot and peas.\n3. Add rice and scrambled egg.\n4. Season with soy sauce.",
                ing("rice", 200, "g"), ing("egg", 2, "pcs"), ing("carrot", 1, "pcs"), ing("pea", 100, "g"), ing("soy sauce", 2, "tbsp"));

        add(r, "Chicken and Rice Bake", "1. Place rice, chopped onion and garlic in a dish with water and salt.\n2. Lay chicken on top.\n3. Cover and bake 40 minutes until cooked through.",
                ing("chicken", 400, "g"), ing("rice", 200, "g"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Egg Noodle Stir Fry", "1. Cook the noodles and drain.\n2. Scramble eggs in a hot pan with the onion.\n3. Toss in noodles and soy sauce.",
                ing("noodle", 200, "g"), ing("egg", 2, "pcs"), ing("soy sauce", 2, "tbsp"), ing("onion", 1, "pcs"));

        add(r, "Chicken Noodle Soup", "1. Simmer chicken, sliced carrot and onion in water for 20 minutes.\n2. Add noodles and cook until tender.\n3. Season with salt.",
                ing("chicken", 200, "g"), ing("noodle", 100, "g"), ing("carrot", 2, "pcs"), ing("onion", 1, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Milk Rice Pudding", "1. Simmer rice in milk, stirring often, until thick and creamy.\n2. Stir in sugar and cinnamon.\n3. Serve warm.",
                ing("rice", 100, "g"), ing("milk", 500, "ml"), ing("sugar", 3, "tbsp"), ing("cinnamon", 1, "tsp"));

        add(r, "Stuffed Bell Peppers", "1. Cook the rice and mix with chopped tomato.\n2. Halve and deseed the peppers and fill with the mixture.\n3. Top with cheese and bake 25 minutes.",
                ing("bell pepper", 2, "pcs"), ing("rice", 100, "g"), ing("cheese", 50, "g"), ing("tomato", 2, "pcs"));

        // ---------- Chicken, beef & fish ----------
        add(r, "Lemon Garlic Chicken", "1. Marinate chicken in lemon juice, crushed garlic and olive oil.\n2. Pan fry or bake until golden and cooked through.",
                ing("chicken", 400, "g"), ing("lemon", 1, "pcs"), ing("garlic", 3, "pcs"), ing("olive oil", 2, "tbsp"));

        add(r, "Honey Soy Chicken", "1. Mix honey, soy sauce and crushed garlic.\n2. Coat the chicken and marinate 30 minutes.\n3. Bake or pan fry until sticky and cooked through.",
                ing("chicken", 400, "g"), ing("honey", 2, "tbsp"), ing("soy sauce", 3, "tbsp"), ing("garlic", 2, "pcs"));

        add(r, "Beef Tacos", "1. Brown the beef with chopped onion.\n2. Warm the tortillas.\n3. Fill with beef, diced tomato and cheese.",
                ing("beef", 300, "g"), ing("tortilla", 4, "pcs"), ing("onion", 1, "pcs"), ing("cheese", 50, "g"), ing("tomato", 2, "pcs"));

        add(r, "Beef and Potato Hash", "1. Dice potatoes and fry until golden.\n2. Add minced beef and onion.\n3. Cook until browned and season.",
                ing("beef", 200, "g"), ing("potato", 3, "pcs"), ing("onion", 1, "pcs"), ing("oil", 2, "tbsp"));

        add(r, "Lemon Baked Fish", "1. Place fish in a baking dish.\n2. Top with sliced lemon, garlic and a drizzle of olive oil.\n3. Bake 15-20 minutes until flaky.",
                ing("fish", 300, "g"), ing("lemon", 1, "pcs"), ing("olive oil", 2, "tbsp"), ing("garlic", 2, "pcs"));

        // ---------- Potatoes & vegetables ----------
        add(r, "Roasted Vegetables", "1. Chop potato, carrot and onion into chunks.\n2. Toss with oil and salt.\n3. Roast at 200C for 35 minutes.",
                ing("potato", 3, "pcs"), ing("carrot", 2, "pcs"), ing("onion", 1, "pcs"), ing("oil", 2, "tbsp"), ing("salt", 1, "tsp"));

        add(r, "Baked Potato with Cheese", "1. Prick the potatoes and bake at 200C for an hour.\n2. Split open and mash in butter and salt.\n3. Top with grated cheese.",
                ing("potato", 2, "pcs"), ing("cheese", 60, "g"), ing("butter", 20, "g"), ing("salt", 1, "tsp"));

        add(r, "Creamy Mashed Potatoes", "1. Boil potatoes until soft and drain.\n2. Mash with butter and warm milk.\n3. Season with salt.",
                ing("potato", 4, "pcs"), ing("butter", 30, "g"), ing("milk", 100, "ml"), ing("salt", 1, "tsp"));

        add(r, "Spicy Potato Wedges", "1. Cut potatoes into wedges.\n2. Toss with oil, paprika and salt.\n3. Bake at 220C for 30 minutes, turning once.",
                ing("potato", 3, "pcs"), ing("oil", 2, "tbsp"), ing("salt", 1, "tsp"), ing("paprika", 1, "tsp"));

        add(r, "Cinnamon Sweet Potato Bake", "1. Cut sweet potatoes into chunks.\n2. Toss with oil, cinnamon and salt.\n3. Roast at 200C for 30 minutes.",
                ing("sweet potato", 3, "pcs"), ing("oil", 2, "tbsp"), ing("cinnamon", 1, "tsp"), ing("salt", 1, "tsp"));

        add(r, "Pumpkin Soup", "1. Fry chopped onion and garlic.\n2. Add cubed pumpkin and water and simmer until soft.\n3. Blend smooth and season.",
                ing("pumpkin", 400, "g"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Creamy Mushroom Soup", "1. Fry sliced mushrooms and onion in butter.\n2. Stir in flour, then slowly add milk.\n3. Simmer until thick and season.",
                ing("mushroom", 200, "g"), ing("onion", 1, "pcs"), ing("milk", 300, "ml"), ing("butter", 20, "g"), ing("flour", 2, "tbsp"));

        add(r, "Pea and Potato Soup", "1. Boil diced potato and onion until soft.\n2. Add peas and cook 5 minutes.\n3. Blend and season.",
                ing("pea", 300, "g"), ing("onion", 1, "pcs"), ing("potato", 1, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Hearty Lentil Soup", "1. Fry chopped onion, carrot and garlic.\n2. Add lentils and water and simmer 30 minutes.\n3. Season with salt.",
                ing("lentil", 150, "g"), ing("carrot", 2, "pcs"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Ratatouille", "1. Fry onion and garlic.\n2. Add sliced zucchini, pepper and tomato.\n3. Simmer 25 minutes until soft.",
                ing("zucchini", 2, "pcs"), ing("tomato", 3, "pcs"), ing("bell pepper", 1, "pcs"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"));

        add(r, "Cabbage Stir Fry", "1. Heat oil and fry garlic.\n2. Add shredded cabbage and carrot.\n3. Toss with soy sauce until just tender.",
                ing("cabbage", 300, "g"), ing("carrot", 1, "pcs"), ing("soy sauce", 2, "tbsp"), ing("garlic", 2, "pcs"), ing("oil", 1, "tbsp"));

        // ---------- Beans, chickpeas & wraps ----------
        add(r, "Chickpea Curry", "1. Fry onion and garlic, then stir in curry powder.\n2. Add chopped tomatoes and chickpeas.\n3. Simmer 20 minutes.",
                ing("chickpea", 400, "g"), ing("tomato", 3, "pcs"), ing("onion", 1, "pcs"), ing("garlic", 2, "pcs"), ing("curry powder", 2, "tbsp"));

        add(r, "Black Bean Tacos", "1. Warm the black beans with chopped onion.\n2. Fill warm tortillas with the beans.\n3. Top with cheese.",
                ing("black bean", 200, "g"), ing("tortilla", 4, "pcs"), ing("onion", 1, "pcs"), ing("cheese", 50, "g"));

        add(r, "Bean and Cheese Quesadilla", "1. Spread beans and cheese over a tortilla.\n2. Fold in half.\n3. Toast in a dry pan until crisp and melted.",
                ing("tortilla", 2, "pcs"), ing("bean", 100, "g"), ing("cheese", 60, "g"));

        add(r, "Cheese Quesadilla", "1. Sprinkle cheese over a tortilla and fold.\n2. Fry in a little butter until golden on both sides.",
                ing("tortilla", 2, "pcs"), ing("cheese", 80, "g"), ing("butter", 10, "g"));

        add(r, "Homemade Hummus", "1. Blend chickpeas, tahini, lemon juice and garlic.\n2. Add olive oil until smooth.\n3. Season with salt.",
                ing("chickpea", 240, "g"), ing("tahini", 2, "tbsp"), ing("lemon", 1, "pcs"), ing("garlic", 1, "pcs"), ing("olive oil", 2, "tbsp"));

        // ---------- Salads & sandwiches ----------
        add(r, "Tomato Mozzarella Salad", "1. Slice tomatoes and mozzarella.\n2. Layer on a plate with basil.\n3. Drizzle with olive oil.",
                ing("tomato", 3, "pcs"), ing("mozzarella", 100, "g"), ing("olive oil", 1, "tbsp"), ing("basil", 5, "g"));

        add(r, "Greek Salad", "1. Chop cucumber and tomato.\n2. Add olives and crumbled feta.\n3. Dress with olive oil.",
                ing("cucumber", 1, "pcs"), ing("tomato", 3, "pcs"), ing("feta", 80, "g"), ing("olive", 50, "g"), ing("olive oil", 2, "tbsp"));

        add(r, "Simple Coleslaw", "1. Shred cabbage and carrot.\n2. Mix with mayonnaise and salt.\n3. Chill before serving.",
                ing("cabbage", 300, "g"), ing("carrot", 2, "pcs"), ing("mayonnaise", 3, "tbsp"), ing("salt", 1, "tsp"));

        add(r, "Tuna Salad", "1. Drain the tuna and flake into a bowl.\n2. Mix with mayonnaise, finely chopped onion and salt.",
                ing("tuna", 1, "pcs"), ing("mayonnaise", 2, "tbsp"), ing("onion", 1, "pcs"), ing("salt", 1, "tsp"));

        add(r, "Tuna Melt", "1. Mix tuna with mayonnaise.\n2. Spread on bread and top with cheese.\n3. Grill until the cheese is bubbling.",
                ing("tuna", 1, "pcs"), ing("bread", 2, "pcs"), ing("cheese", 40, "g"), ing("mayonnaise", 1, "tbsp"));

        add(r, "Tomato Bruschetta", "1. Toast the bread and rub with garlic.\n2. Top with diced tomato and olive oil.\n3. Season and serve.",
                ing("bread", 4, "pcs"), ing("tomato", 3, "pcs"), ing("garlic", 1, "pcs"), ing("olive oil", 2, "tbsp"));

        add(r, "Garlic Bread", "1. Mash butter with crushed garlic.\n2. Spread on the bread.\n3. Bake at 200C for 10 minutes until crisp.",
                ing("bread", 4, "pcs"), ing("butter", 40, "g"), ing("garlic", 3, "pcs"));

        add(r, "Cheese and Onion Toastie", "1. Butter the outside of the bread.\n2. Fill with cheese and thinly sliced onion.\n3. Toast in a pan until golden.",
                ing("bread", 2, "pcs"), ing("cheese", 50, "g"), ing("onion", 1, "pcs"), ing("butter", 10, "g"));

        // ---------- Baking & desserts ----------
        add(r, "Banana Bread", "1. Mash bananas and mix with melted butter, sugar and eggs.\n2. Fold in the flour.\n3. Bake at 180C for 50 minutes.",
                ing("banana", 3, "pcs"), ing("flour", 250, "g"), ing("sugar", 100, "g"), ing("egg", 2, "pcs"), ing("butter", 80, "g"));

        add(r, "Apple Crumble", "1. Slice apples into a baking dish.\n2. Rub butter into flour, sugar and oats to make the topping.\n3. Scatter over the apples and bake at 180C for 35 minutes.",
                ing("apple", 3, "pcs"), ing("flour", 100, "g"), ing("butter", 60, "g"), ing("sugar", 50, "g"), ing("oat", 40, "g"));

        return r;
    }

    private static void add(List<SeedRecipe> list, String name, String steps, RecipeIngredient... ingredients) {
        List<RecipeIngredient> ingredientList = new ArrayList<>();
        for (RecipeIngredient ri : ingredients) ingredientList.add(ri);
        list.add(new SeedRecipe(name, steps, ingredientList));
    }

    private static RecipeIngredient ing(String name, double quantity, String unit) {
        return new RecipeIngredient(0, 0, name, quantity, unit);
    }
}
