package com.fqhll.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.graphics.Canvas;

import java.util.List;

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

    // @SuppressLint("UseCompatLoadingForDrawables")
    // @Override
    // public void onDraw(Canvas canvas) {

    //     List<Keyboard.Key> keys = getKeyboard().getKeys();
    //     for (Keyboard.Key key : keys) {
    //         int code = key.codes != null && key.codes.length > 0 ? key.codes[0] : 0;

    //         Drawable background;
    //         if (code == 113) { // q, 32 for space, try that too
    //             background = getContext().getResources().getDrawable(R.drawable.key_custom_background);
    //         }
    //         else {
    //             background = getContext().getResources().getDrawable(R.drawable.key_unpressed_background);
    //         }

    //         background.setBounds(key.x, key.y, key.x + key.width, key.y + key.height);
    //         background.draw(canvas);
    //     }
    //     super.onDraw(canvas); // try putting this before and after to see the layers
    // }
}
