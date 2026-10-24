package com.example.smartpantry.utils;

import android.content.Context;
import android.widget.ImageView;

import com.example.smartpantry.R;

import java.util.Locale;

/**
 * Shows a photo for a recipe. Photos live in res/drawable and are named after the recipe:
 * "Tomato Egg Scramble" -> recipe_tomato_egg_scramble.jpg. If a photo is missing,
 * a plate/cutlery placeholder is shown instead, so the app never crashes on a missing image.
 */
public class RecipeImages {

    public static String drawableName(String recipeName) {
        String slug = recipeName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return "recipe_" + slug;
    }

    public static void apply(ImageView view, String recipeName) {
        Context ctx = view.getContext();
        int resId = ctx.getResources().getIdentifier(
                drawableName(recipeName), "drawable", ctx.getPackageName());
        if (resId != 0) {
            view.setPadding(0, 0, 0, 0);
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
            view.setImageResource(resId);
        } else {
            int pad = (int) (48 * ctx.getResources().getDisplayMetrics().density);
            view.setPadding(pad, pad, pad, pad);
            view.setScaleType(ImageView.ScaleType.FIT_CENTER);
            view.setImageResource(R.drawable.ic_recipe_placeholder);
        }
    }
}
