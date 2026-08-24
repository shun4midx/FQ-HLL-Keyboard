package com.fqhll.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.content.res.TypedArray;
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
        initColorBlocks();
        initKeyTextColor(c, attrs);
    }

    public CustomKeyboardView(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
        initColorBlocks();
        initKeyTextColor(c, attrs);
    }

    private void initKeyTextColor(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.keyTextColor});

        keyTextColor = a.getColor(0, 0xFFFFFFFF);
        a.recycle();
    }

    @Override
    public boolean performClick() {
        super.performClick();  // let the framework know we really clicked
        return true;
    }

    // vowel coloring
    public static final Set<Integer> vowels = new HashSet<>(List.of(97, 101, 105, 111, 117));
    public static final Set<Integer> math_operations = new HashSet<>(List.of(43, 40, 61, 41, 45, 215, 247));
    public static final Set<Integer> zhuyin_vowels = new HashSet<>(List.of(12583, 12584, 12585));
    public static final Set<Integer> exponents = new HashSet<>(List.of(-1000, -1001, -1002, -1003, -1004, -1005, -1006, -1007, -1008, -1009));

    // number coloring
    public static final Set<Integer> zhuyin_tones = new HashSet<>(List.of(729, 714, 711, 715));
    public static final Set<Integer> numbers = new HashSet<>(List.of(48, 49, 50, 51, 52, 53, 54, 55, 56, 57));

    // symbol coloring
    public static final Set<Integer> symbols = new HashSet<>(List.of(44, 46, 65292));

    // modifier coloring
    public static final Set<Integer> modifiers = new HashSet<>(List.of(-1, -5, -2, -11, -12, -4, -42, -52, -10, -62, -64, -14, -13));

    private Drawable defaultBackground;
    private Drawable vowelBackground;
    private Drawable numbersBackground;
    private Drawable symbolsBackground;
    private Drawable modifierBackground;

    private int keyTextColor;

    private int dpGapInPx;

    private boolean colorBlocksTheme;

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initColorBlocks() {
        defaultBackground = getResources().getDrawable(
                R.drawable.key_colorblocks_default_background
        );
        vowelBackground = getResources().getDrawable(
                R.drawable.key_colorblocks_vowel_background
        );
        numbersBackground = getResources().getDrawable(
                R.drawable.key_colorblocks_numbers_background
        );
        symbolsBackground = getResources().getDrawable(
                R.drawable.key_colorblocks_symbols_background
        );
        modifierBackground = getResources().getDrawable(
                R.drawable.key_colorblocks_modifier_background
        );

        dpGapInPx = Math.round(
                8 * getResources().getDisplayMetrics().density
        );

        SharedPreferences prefs = getContext().getSharedPreferences(
                "keyboard_settings",
                Context.MODE_PRIVATE
        );

        colorBlocksTheme = "ColorBlocks".equals(
                prefs.getString("key_color", "Unselected")
        );
    }

    public void setColorBlocks(Canvas canvas) {
        Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }

        for (Keyboard.Key key : keyboard.getKeys()) {
            int code = key.codes != null && key.codes.length > 0 ? key.codes[0] : 0;
            Drawable background = defaultBackground;

            if (vowels.contains(code) || math_operations.contains(code) || zhuyin_vowels.contains(code) || exponents.contains(code)) {
                background = vowelBackground;
            } else if (numbers.contains(code) || zhuyin_tones.contains(code) ) {
                background = numbersBackground;
            } else if (symbols.contains(code)) {
                background = symbolsBackground;
            } else if (modifiers.contains(code)) {
                background = modifierBackground;
            }

            background.setBounds(
                    key.x,
                    key.y + dpGapInPx,
                    key.x + key.width,
                    key.y + key.height + dpGapInPx
            );

            background.draw(canvas);
        }
    }

    public void drawHints(Keyboard keyboard, Canvas canvas) {
        if (keyboard == null) return;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(9f * getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(keyTextColor);

        float density = getResources().getDisplayMetrics().density;

        float rightPad = 7f * density;
        float topPad = 14f * density;

        Paint.FontMetrics fm = paint.getFontMetrics();

        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.popupCharacters == null ||
                    key.popupCharacters.length() == 0) {
                continue;
            }

            String hint = key.popupCharacters.toString();

            float keyTop = key.y;

            float x = key.x + key.width - rightPad;
            float y = keyTop + topPad - fm.ascent;

            canvas.drawText(hint, x, y, paint);
        }
    }

     @SuppressLint("UseCompatLoadingForDrawables")
     @Override
     public void onDraw(Canvas canvas) {
         if (colorBlocksTheme) {
             setColorBlocks(canvas);
         }

         super.onDraw(canvas);

         Keyboard keyboard = getKeyboard();
         drawHints(keyboard, canvas);
     }
}
