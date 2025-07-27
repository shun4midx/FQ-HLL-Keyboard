package com.fqhll.keyboard;

import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

public class CustomKeyboardApp extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView kv;
    private Keyboard keyboard;

    private PopupWindow keyPreviewPopup;
    private TextView previewText;

    private int caps_state = 1; // 0 = off, 1 = single shift, 2 = double shift
    private long last_caps_time = 0;
    private boolean defaultCaps = true;
    private static final int DOUBLE_TAP_TIMEOUT = 300; // Smth like Gboard capping

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.custom_keyboard_layout, null);
        keyboard = new Keyboard(this, R.xml.custom_keypad);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);

        // Disable default pop-up
        kv.setPreviewEnabled(false);

        // Get default caps
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        defaultCaps = prefs.getBoolean("default_caps_enabled", false);
        caps_state = defaultCaps ? 1 : 0;
        keyboard.setShifted(caps_state > 0);
        updateCapsLabel();

        // Initialize manual pop-up
        previewText = new TextView(this);
        previewText.setBackgroundColor(getResources().getColor(android.R.color.white));
        previewText.setTextColor(getResources().getColor(android.R.color.black));
        previewText.setTextSize(26f);
        previewText.setGravity(Gravity.CENTER);

        keyPreviewPopup = new PopupWindow(previewText,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setAnimationStyle(0);  // No animation

        kv.setPreviewEnabled(false); // Remove default setup

        return kv;
    }

    @Override
    public void onPress(int primaryCode) {
        // Don't show pop-up for SPACE, CAPS (-1), DELETE (-5), or ENTER (-4)
        if (primaryCode == 32 || primaryCode == -1 || primaryCode == -5 || primaryCode == -4) {
            return;
        }

        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == primaryCode) {
                // Set text
                char curr_char = (char) primaryCode;
                previewText.setText(String.valueOf(caps_state > 0 ? Character.toUpperCase(curr_char) : curr_char));
                previewText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

                // Match popup size to key size
                keyPreviewPopup.setWidth(key.width);
                keyPreviewPopup.setHeight(key.height);

                // Calculate position: center horizontally, one key height above
                int popupX = key.x;
                int popupY = -((kv.getHeight() - key.y) + key.height);

                // Small adjustment if needed (like Gboard's preview offset)
                int adjustmentY = -4; // fine-tune upward offset (dp to px)
                popupY += (int) (adjustmentY * getResources().getDisplayMetrics().density);

                keyPreviewPopup.showAsDropDown(kv, popupX, popupY);
                break;
            }
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
            keyPreviewPopup.dismiss();
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        // If it's SHIFT, handle it *before* anything else:
        if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleCapsPress();
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Always read latest defaultCaps but don't force caps_state here
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        defaultCaps = prefs.getBoolean("default_caps_enabled", false);

        if (defaultCaps && isAtLineStart() && caps_state == 1) {
            applyCapsState();
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                adjustCapsAfterDeletion();
                break;
            case -1: // CAPS key
                handleCapsPress();
                break;
            case -2: // symbols
                keyboard = new Keyboard(this, R.xml.symbols);
                kv.setKeyboard(keyboard);
                kv.invalidateAllKeys();
                break;
            case -10: // back to main
                keyboard = new Keyboard(this, R.xml.custom_keypad);
                kv.setKeyboard(keyboard);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                EditorInfo editorInfo = getCurrentInputEditorInfo();
                if (editorInfo != null && (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
                    ic.commitText("\n", 1);  // Insert newline
                } else {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                }
                resetCaps();
                break;
            default:
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    if (caps_state > 0) {
                        code = Character.toUpperCase(code);
                    }
                }
                ic.commitText(String.valueOf(code), 1);

                // If single-shift was used, reset to 0
                if (caps_state == 1) {
                    resetCaps();
                }

                // Auto-cap if punctuation (e.g., after ". ")
                if (shouldAutoCap() && defaultCaps) {
                    caps_state = 1;
                    applyCapsState();
                }
                break;
        }
    }

    private boolean shouldAutoCap() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;

        CharSequence beforeText = ic.getTextBeforeCursor(4, 0); // Check last 4 chars
        if (beforeText == null || beforeText.length() < 2) return false;

        String lastText = beforeText.toString();
        // Check for ". " but not "..."
        return lastText.endsWith(". ") && !lastText.endsWith("... ");
    }

    private boolean isAtLineStart() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;

        CharSequence beforeText = ic.getTextBeforeCursor(1, 0);
        // Beginning of text or right after a newline
        return (beforeText == null || beforeText.length() == 0 || beforeText.charAt(0) == '\n');
    }

    private void adjustCapsAfterDeletion() {
        if (defaultCaps && isAtLineStart()) {
            caps_state = 1;
        } else if (shouldAutoCap() && defaultCaps) {
            caps_state = 1;
        } else {
            caps_state = 0;
        }
        applyCapsState();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        resetCaps(); // Reset caps or perform actions
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        CharSequence text = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
        if (text == null) return;

        // Case 1: Empty field (new message)
        if (text.length() == 0) {
            if (defaultCaps) {
                caps_state = 1; // Auto-cap new message
            } else {
                caps_state = 0;
            }
            applyCapsState();
            return;
        }

        // Case 2: Cursor at start of line or after newline
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before != null && (before.length() == 0 || before.charAt(0) == '\n')) {
            if (defaultCaps) {
                caps_state = 1; // Auto-cap after newline
            } else if (caps_state != 2) {
                caps_state = 0;
            }
        }

        // Case 3: Check if deletion leads to beginning of line
        CharSequence prevChar = ic.getTextBeforeCursor(2, 0);
        if (prevChar != null && prevChar.length() >= 1 && prevChar.charAt(prevChar.length() - 1) == '\n') {
            if (defaultCaps) {
                caps_state = 1; // Capitalize again after deleting to new line start
            }
        }

        applyCapsState();
    }

    private void resetCaps() {
        if (caps_state == 2) return;  // Never reset caps lock

        if (caps_state == 1 && !isAtLineStart()) {
            caps_state = 0;
        } else if (defaultCaps && isAtLineStart()) {
            caps_state = 1;
        }
        applyCapsState();
    }

    private void handleCapsPress() {
        long curr_time = System.currentTimeMillis();
        if (curr_time - last_caps_time < DOUBLE_TAP_TIMEOUT) {
            // Double tap detected -> toggle caps lock
            caps_state = (caps_state == 2) ? 0 : 2;
        } else {
            // Single tap: toggle between off and single shift
            if (caps_state == 0) {
                caps_state = 1; // single shift
            } else if (caps_state == 1) {
                caps_state = 0; // back to off
            } else if (caps_state == 2) {
                caps_state = 0; // exit caps lock
            }
        }
        last_caps_time = curr_time;
        applyCapsState();
    }

    private void updateCapsLabel() {
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == -1) { // CAPS key
                if (caps_state == 0) {
                    key.label = "caps";
                } else if (caps_state == 1) {
                    key.label = "Caps";
                } else {
                    key.label = "CAPS";
                }
                break;
            }
        }
    }

    private void applyCapsState() {
        keyboard.setShifted(caps_state > 0);
        updateCapsLabel();
        kv.invalidateAllKeys();
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        caps_state = defaultCaps ? 1 : 0;
        applyCapsState();  // Just updates the shift key appearance
    }

    @Override
    public void onText(CharSequence charSequence) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
}
