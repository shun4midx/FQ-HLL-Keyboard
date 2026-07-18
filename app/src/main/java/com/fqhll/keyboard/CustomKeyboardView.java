package com.fqhll.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.graphics.Canvas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomKeyboardView extends KeyboardView {
    public CustomKeyboardView(Context c, AttributeSet attrs) {
        super(c, attrs);
    }
    public CustomKeyboardView(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
    }
    @Override
    public boolean performClick() {
        super.performClick();  // let the framework know we really clicked
        return true;
    }

    // vowel coloring
    Set<Integer> vowels = new HashSet<>(List.of(97, 101, 105, 111, 117));
    Set<Integer> math_operations = new HashSet<>(List.of(43, 40, 61, 41, 45, 215, 247));

    // number coloring
    Set<Integer> numbers = new HashSet<>(List.of(48, 49, 50, 51, 52, 53, 54, 55, 56, 57));

    // symbol coloring
    Set<Integer> symbols = new HashSet<>(List.of(44, 46, 65292));

    // modifier coloring
    Set<Integer> modifiers = new HashSet<>(List.of(-1, -5, -2, -11, -12, -4, -42, -52, -10, -62, -64, -2, -14));

    private boolean isColorBlocksTheme() {
        SharedPreferences prefs = getContext().getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE);
        String keyColor = prefs.getString("key_color", "Unselected");
        return "ColorBlocks".equals(keyColor);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void setColorBlocks(Canvas canvas) {

        float density = getResources().getDisplayMetrics().density;
        int dpGapInPx = Math.round(8 * density);

        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            int code = key.codes != null && key.codes.length > 0 ? key.codes[0] : 0;

            Drawable background;
            int drawableID = R.drawable.key_colorblocks_default_background;

            if (vowels.contains(code) || math_operations.contains(code)) {
                drawableID = R.drawable.key_colorblocks_vowel_background;
            }
            else if (numbers.contains(code)) {
                drawableID = R.drawable.key_colorblocks_numbers_background;
            }
            else if (symbols.contains(code)) {
                drawableID = R.drawable.key_colorblocks_symbols_background;
            }
            else if (modifiers.contains(code)) {
                drawableID = R.drawable.key_colorblocks_modifier_background;
            }

            background = getContext().getResources().getDrawable(drawableID);

            background.setBounds(key.x, key.y+dpGapInPx, key.x + key.width, key.y + key.height + dpGapInPx);
            background.draw(canvas);
        }
    }

     @SuppressLint("UseCompatLoadingForDrawables")
     @Override
     public void onDraw(Canvas canvas) {
        if (isColorBlocksTheme()) {
            setColorBlocks(canvas);
        }
        super.onDraw(canvas); // make sure keycolor and keybordercolor are transparent for custom per-key color
     }
}
