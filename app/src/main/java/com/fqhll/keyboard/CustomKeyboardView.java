package com.fqhll.keyboard;

import android.content.Context;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

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
}
