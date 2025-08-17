package com.fqhll.keyboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Looper;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.text.BreakIterator;

public class CustomKeyboardApp extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private CustomKeyboardView kv;
    private Keyboard keyboard;
    private Keyboard emojiKeyboard;
    private Keyboard symbolKeyboard;
    private Keyboard mathKeyboard;
    private Keyboard clipKeyboard;
    private Keyboard editorKeyboard;
    private Keyboard numpadKeyboard;
    private Keyboard zhuyinKeyboard;
    private Keyboard engKeyboard;

    private PopupWindow keyPreviewPopup;
    private TextView previewText;

    private int caps_state = 1; // 0 = off, 1 = single shift, 2 = double shift
    private long last_caps_time = 0;
    private boolean defaultCaps = true;
    private static final int DOUBLE_TAP_TIMEOUT = 300; // Smth like Gboard capping

    // Don't show pop-up for SPACE, CAPS (-1), DELETE (-5), Symbols (-10 from symbols page and -2 from main page), or ENTER (-4)
    private static final Set<Integer> NO_POPUP = new HashSet<>(Arrays.asList(32, -1, -5, -10, -2, -4, -42, -52));
    private static final Set<String> CAPITALIZE_ENDS = new HashSet<>(Arrays.asList(". ", "! ", "? "));

    private static final Set<Character> LETTERS = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'));

    private float scaleX, scaleY;
    private int lastTouchX, lastTouchY;

    // (jperm voice) hope you can turn on word wrap
    public static final String[] emoji_list = new String[]{"😭", "😂", "💀", "😔", "🫠", "💁‍♂️", "🧍‍♂️", "💩", "💅", "🫂", "🔥", "🍀", "👾", "👽", "🛸", "👀", "✨️", "🐟", "✅️", "❌️", "🐸", "🌸", "🎀", "🤡", "😡", "🙏", "👻", "🥺", "😐", "👍", "😤", "🤓", "😀", "🦆", "🥬", "🐒", "🧠"};
    public static final String[][] emoji_variation_list = new String[][]{new String[]{"🧍‍♂️", "🧍‍♀️", "🧍"}, new String[]{"💁‍♂️", "💁‍♀️", "💁"}};
    public static final Map<String, String[]> emoji_variations = new HashMap<>();

    private void init_emoji_variations() {

        for (String[] emojis : emoji_variation_list) {
            for (int j = 0; j < 3; j++) {

                emoji_variations.put(emojis[j], emojis);

            }
        }

    }

    public static final String[] math_symbol_list = new String[]{"¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹", "⁰", "∀", "∃", "⇔", "⇒", "Δ", "θ", "π", "ƒ", "α", "β", "±", "≠", "≈", "≡", "Σ", "√", "∩", "∪", "∈", "∋", "⊂", "⊃", "⊆", "⊇", "□", "ø", "∞"};
    private LinearLayout suggestionBar;
    private View root;

    private boolean nativeLoaded = false;

    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private static final long LONG_PRESS_MS = 350;
    private boolean isLongPress = false;

    // Coyote‑time window for grouping near‑simultaneous presses
    private static final long COYOTE_WINDOW_MS = 1;
    private final List<Integer> pendingKeys = new ArrayList<>();
    private final Handler coyoteHandler = new Handler(Looper.getMainLooper());
    private final Runnable flushRunnable = this::flushPendingKeys;

    private static final double AUTO_REPLACE_THRESHOLD = 0.6;
    private boolean defaultAutocor = true;

    private static final int LOOKBACK = 64;
    private final BreakIterator graphemeIter = BreakIterator.getCharacterInstance();

    private boolean isSelectToggled = false;

    private boolean isSkippedAutoreplace = false;

    private void ensureNative() {
        if (!nativeLoaded) {
            try {
                System.loadLibrary("keyboard");
                nativeLoaded = true;
            } catch (Throwable t) {
                // swallow it—keyboard still works
            }
        }
    }

    private native void nativeInitAutocorrector(String path);
    private native Suggestion nativeSuggest(String prefix);
    public static native void nativeAddWord(String word, String path);
    public static native void nativeRemoveWord(String word, String path);

    private static native void nativeSetLayout(String layout, String path);

    public boolean inDictionary(String word) throws IOException {
        Path path = Paths.get(getFilesDir().getAbsolutePath() + "/test_files/20k_texting.txt");

        List<String> lines = Files.readAllLines(path);
        Set<String> word_set = new HashSet<>(lines);

        return word_set.contains(word);
    }

    @Override
    public View onCreateInputView() {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        defaultCaps = prefs.getBoolean("capsToggle", true);
        defaultAutocor = prefs.getBoolean("autocorToggle", true);
        caps_state = defaultCaps ? 1 : 0;
        init_emoji_variations();
        editEmojiArray();

        root = buildKeyboardView();
        applyCapsState();
        copyAssetToInternal(getApplicationContext(), "test_files/20k_texting.txt");

        String absPath = getFilesDir().getAbsolutePath() + "/test_files/20k_texting.txt";
        ensureNative();
        nativeInitAutocorrector(absPath);
        return root;
    }

    private void copyAssetToInternal(Context ctx, String assetName) {
        try {
            File outFile = new File(ctx.getFilesDir(), assetName);
            if (outFile.exists()) return;

            InputStream is = ctx.getAssets().open(assetName);
            outFile.getParentFile().mkdirs();
            OutputStream os = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            is.close();
            os.close();
        } catch (IOException e) {

        }
    }

    private void handleLongPress(int primaryCode) {
        if (-99 <= primaryCode && primaryCode <= -90) { // long press to delete clipboard item
            int clipboardCode = primaryCode + 90;
            clipboardCode = -clipboardCode; // get code without -9 in front
            clipboardCode = clipboardCode + 1; // since codes start from 0 but clipboard start from 1

            SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
            String clipboardPref = "clipboard_text_" + clipboardCode;
            prefs.edit().putString(clipboardPref, "").apply();
            return;
        }

        InputConnection ic = getCurrentInputConnection();

        switch (primaryCode) {
            case -2: // symbols -> numpad
                kv.setKeyboard(numpadKeyboard);
                kv.invalidateAllKeys();
                break;
            case 44: // comma -> select all
                if (ic == null) break;

                // 1) Ask the target editor to do Select All (works in most places, old and new)
                boolean handled = ic.performContextMenuAction(android.R.id.selectAll);
                if (handled) break;

                // 2) Fallback: try ExtractedText (requests the whole buffer)
                ExtractedTextRequest req = new ExtractedTextRequest();
                req.hintMaxChars = 0; // no limit
                req.hintMaxLines = 0; // no limit
                ExtractedText et = ic.getExtractedText(req, 0);
                if (et != null && et.text != null) {
                    int len = et.text.length();
                    ic.setSelection(0, len);
                    break;
                }

                // 3) Last resort: stitch before/after with big but finite bounds + null-safety
                final int BIG = 100000; // safer than Integer.MAX_VALUE on older devices
                CharSequence before = ic.getTextBeforeCursor(BIG, 0);
                CharSequence after  = ic.getTextAfterCursor(BIG, 0);
                int beforeLen = (before == null) ? 0 : before.length();
                int afterLen  = (after  == null) ? 0 : after.length();
                ic.setSelection(0, beforeLen + afterLen);
                break;
            case 46: // full stop -> delete last word
                deleteLastWord();
                showSuggestions("");
                break;
            case 47: // slash -> backslash
                ic.commitText("\\", 1);
                break;
            case 65292: // chi comma -> chi full stop
                ic.commitText("。", 1);
                break;
            case -4: // zhuyin enter -> open settings, eng enter -> skip word
                SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
                String keyboardLayout = prefs.getString("keyboard_layout", "qwerty").toLowerCase();
                if (keyboardLayout.equals("zhuyin")) {
                    PackageManager manager = getPackageManager();
                    Intent launchIntent = manager.getLaunchIntentForPackage("com.fqhll.keyboard");
                    launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    startActivity(launchIntent);
                }
                else {
                    if (ic != null) {
                        isSkippedAutoreplace = true;
                        showSuggestions("");
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onPress(int primaryCode) {
        isLongPress = false;

        longPressRunnable = () -> {
            handleLongPress(primaryCode);
            isLongPress = true;
        };
        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_MS);

        if (NO_POPUP.contains(primaryCode)) return;

        if (-99 <= primaryCode && primaryCode <= -90) return; // clipboard
        if (-79 <= primaryCode && primaryCode <= -60) return; // text editor
        if (-1049 <= primaryCode && primaryCode <= -1000) return; // math symbols

        // 1) Un‑scale into keyboard coords (you already have scaleX/scaleY set up)
        int kx = (int)((lastTouchX - kv.getPaddingLeft()) / scaleX);
        int ky = (int)((lastTouchY - kv.getPaddingTop())  / scaleY);

        // 2) Collect all keys with that code whose box contains the touch
        List<Keyboard.Key> matches = new ArrayList<>();
        for (Keyboard.Key key : kv.getKeyboard().getKeys()) {
            if (key.codes[0] == primaryCode) {
                if ( kx >= key.x
                        && kx <  key.x + key.width
                        && ky >= key.y
                        && ky <  key.y + key.height) {
                    matches.add(key);
                }
            }
        }

        // 3) Pick the one whose center is closest to (kx,ky)
        Keyboard.Key tapped = null;
        if (!matches.isEmpty()) {
            double bestDist = Double.MAX_VALUE;
            for (Keyboard.Key key : matches) {
                double centerX = key.x + key.width  * 0.5;
                double centerY = key.y + key.height * 0.5;
                double dx = kx - centerX, dy = ky - centerY;
                double dist = dx*dx + dy*dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    tapped = key;
                }
            }
        }

        // 4) If we found one, show the popup above it
        if (tapped != null && tapped.codes[0] == primaryCode) {
            showKeyPreview(tapped, primaryCode);
        }
    }

    public void showKeyPreview(Keyboard.Key key, int code) {
        // Set text
        char curr_char = (char) (code);
        previewText.setText(String.valueOf(caps_state > 0 && code > 0 ? Character.toUpperCase(curr_char) : key.label));
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
    }

    @Override
    public void onRelease(int primaryCode) {
        longPressHandler.removeCallbacks(longPressRunnable);
        if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
            keyPreviewPopup.dismiss();
        }
    }

    private void playClick(int keyCode){
        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        switch(keyCode){
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;
            case Keyboard.KEYCODE_DONE:
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;
            default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        playClick(primaryCode);

        if (isLongPress) {
            return;
        }

        Map<Integer,String> emojis = getEmojiCodes();
        Map<Integer,String> math_symbols = getMathCodes();

        if (emojis.containsKey(primaryCode)) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(emojis.get(primaryCode), 1);
            }
            return;
        }

        if (math_symbols.containsKey(primaryCode)) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.commitText(math_symbols.get(primaryCode), 1);
            }
            return;
        }

        if (isChordable(primaryCode)) {
            synchronized (pendingKeys) {
                pendingKeys.add(primaryCode);
            }
            coyoteHandler.removeCallbacks(flushRunnable);
            coyoteHandler.postDelayed(flushRunnable, COYOTE_WINDOW_MS);
            return;
        }

        // If it's SHIFT, handle it *before* anything else:
        if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleCapsPress();
            return;
        }

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        flushPendingKeys();

        if (defaultCaps && isAtLineStart() && caps_state == 1) {
            applyCapsState();
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                // First, see if there's any selected text
                CharSequence selected = ic.getSelectedText(0);
                if (selected != null && selected.length() > 0) {
                    // If so, replace it (commit empty string) and return
                    ic.commitText("", 1);
                    adjustCapsAfterDeletion();
                } else {
                    handleDelete();
                    updateSuggestion(getCurrentInputConnection());
                    adjustCapsAfterDeletion();
                }
                updateSuggestion(ic);
                break;
            case -1: // CAPS key
                handleCapsPress();
                break;
            case -2: // symbols
                kv.setKeyboard(symbolKeyboard);
                kv.invalidateAllKeys();
                break;
            case -10: // back to main
                kv.setKeyboard(keyboard);
                applyCapsState();
                break;
            case -11: // emojis
                kv.setKeyboard(emojiKeyboard);
                kv.invalidateAllKeys();
                showSuggestions("");
                break;
            case -12: // settings
                PackageManager manager = getPackageManager();
                Intent launchIntent = manager.getLaunchIntentForPackage("com.fqhll.keyboard");
                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                startActivity(launchIntent);
                break;
            case -13: // numpad
                kv.setKeyboard(numpadKeyboard);
                kv.invalidateAllKeys();
                break;
            case -14: // math symbols
                kv.setKeyboard(mathKeyboard);
                kv.invalidateAllKeys();
                break;
            case -42: // Left arrow
                // Look at the char immediately before the cursor
                CharSequence before = ic.getTextBeforeCursor(1, 0);
                if (before != null && before.length() > 0) {

                    if (isSelectToggled) {
                        int cursorPosition = ic.getExtractedText(new ExtractedTextRequest(), 0).selectionStart;
                        int selectedTextLength = 0;
                        CharSequence selectedText = ic.getSelectedText(0);
                        if (selectedText != null) {
                            selectedTextLength = selectedText.length();
                        }

                        if (cursorPosition > 0) {
                            int newSelectionStart = cursorPosition - 1;
                            int newSelectionEnd = cursorPosition + selectedTextLength;
                            ic.setSelection(newSelectionStart, newSelectionEnd);
                        }
                    }

                    else {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                        showSuggestions("");
                    }
                }
                return;
            case -52: // Right arrow
                // look at the char immediately after the cursor
                CharSequence after = ic.getTextAfterCursor(1, 0);
                if (after != null && after.length() > 0) {

                    if (isSelectToggled) {
                        int cursorPosition = ic.getExtractedText(new ExtractedTextRequest(), 0).selectionStart;
                        int selectedTextLength = 0;
                        CharSequence selectedText = ic.getSelectedText(0);
                        if (selectedText != null) {
                            selectedTextLength = selectedText.length();
                        }

                        int newSelectionStart = cursorPosition + 1;
                        int newSelectionEnd = cursorPosition - selectedTextLength;
                        ic.setSelection(newSelectionStart, newSelectionEnd);

                    }

                    else {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));
                        showSuggestions("");
                    }
                }
                return;
            case -62: // up arrow
                CharSequence before2 = ic.getTextBeforeCursor(1, 0);
                if (before2 != null && before2.length() > 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                    showSuggestions("");
                }
                return;
            case -63: // select button
                isSelectToggled = !isSelectToggled;
                break;
            case -64: // down arrow
                CharSequence after2 = ic.getTextAfterCursor(1, 0);
                if (after2 != null && after2.length() > 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
                    showSuggestions("");
                }
                return;
            case -65: // leftest
                ic.setSelection(0, 0);
                break;
//            case -66: // select all
//                CharSequence selectAllText = ic.getTextBeforeCursor(Integer.MAX_VALUE, 0)
//                        .toString() + ic.getTextAfterCursor(Integer.MAX_VALUE, 0).toString();
//                ic.setSelection(0, selectAllText.length());
//                break;
            case -66: // select all
                ic = getCurrentInputConnection();
                if (ic == null) break;

                // 1) Ask the target editor to do Select All (works in most places, old and new)
                boolean handled = ic.performContextMenuAction(android.R.id.selectAll);
                if (handled) break;

                // 2) Fallback: try ExtractedText (requests the whole buffer)
                ExtractedTextRequest req = new ExtractedTextRequest();
                req.hintMaxChars = 0; // no limit
                req.hintMaxLines = 0; // no limit
                ExtractedText et = ic.getExtractedText(req, 0);
                if (et != null && et.text != null) {
                    int len = et.text.length();
                    ic.setSelection(0, len);
                    break;
                }

                // 3) Last resort: stitch before/after with big but finite bounds + null-safety
                final int BIG = 100000; // safer than Integer.MAX_VALUE on older devices
                before = ic.getTextBeforeCursor(BIG, 0);
                after  = ic.getTextAfterCursor(BIG, 0);
                int beforeLen = (before == null) ? 0 : before.length();
                int afterLen  = (after  == null) ? 0 : after.length();
                ic.setSelection(0, beforeLen + afterLen);
                break;
            case -67: // rightest
                CharSequence rightestText = ic.getTextBeforeCursor(Integer.MAX_VALUE, 0)
                        .toString() + ic.getTextAfterCursor(Integer.MAX_VALUE, 0).toString();
                ic.setSelection(rightestText.length(), rightestText.length());
                break;
            case -68: // cut
                CharSequence cutText = ic.getSelectedText(0);
                if (cutText != null) {
                    copyToClipboard(cutText.toString());
                    ic.commitText("", 1);
                    adjustCapsAfterDeletion();
                }
                break;
            case -69: // copy
                CharSequence copyText = ic.getSelectedText(0);
                if (copyText != null) {
                    copyToClipboard(copyText.toString());
                }
                break;
            case -70: // paste
                SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
                String pasteText = prefs.getString("clipboard_text_1", "");
                ic.commitText(pasteText, 1);
                break;
            case -71: // invis key
                break;
            default: {
                if (isAlphabet(primaryCode)) {
                    commitChar(ic, primaryCode);
                    return;
                }

                // if clipboard button, commit text in clipboard
                else if (-99 <= primaryCode && primaryCode <= -90) {
                    int clipboardCode = primaryCode + 90;
                    clipboardCode = -clipboardCode; // get code without -9 in front
                    clipboardCode = clipboardCode + 1; // since codes start from 0 but clipboard start from 1

                    SharedPreferences prefs2 = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
                    String clipboardPref = "clipboard_text_" + clipboardCode;
                    String clipboardText = prefs2.getString(clipboardPref, "");

                    if (!clipboardText.isEmpty()) {
                        ic.commitText(clipboardText, 1);
                    }
                    break;
                }

                // Figure out the last word before space
                CharSequence beforeCs = ic.getTextBeforeCursor(50, 0);
                String raw = (beforeCs == null ? "" : beforeCs.toString());
                String trimmed = raw.replaceAll("\\s+$", ""); // Trim trailing whitespace
                String[] parts = (trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+"));
                String lastWord = parts.length > 0 ? parts[parts.length - 1] : "";

                // Ask JNI for suggestions on lastWord
                Suggestion s = nativeLoaded && !lastWord.isEmpty()
                        ? nativeSuggest(lastWord)
                        : new Suggestion(new String[]{"", "", ""}, new double[]{0, 0, 0});
                String top = s.suggestions[1];
                double score = s.scores.length > 0 ? s.scores[1] : 0;

                // If we should auto‑replace:
                CharSequence beforeChar = ic.getTextBeforeCursor(1, 0);
                char prevChar = (beforeChar != null && beforeChar.length() > 0) ? beforeChar.charAt(0) : '\0';

                String beforeStr = ic.getTextBeforeCursor(50, 0).toString();
                String trimmedStr = beforeStr.replaceAll("\\s+$", "");
                String[] partsStr = trimmedStr.split("\\s+");
                String lastWordStr = partsStr.length > 0 ? partsStr[partsStr.length - 1] : "";

                if (defaultAutocor && score >= AUTO_REPLACE_THRESHOLD && !top.isEmpty() && !top.equals(lastWordStr) && prevChar != ' ' && !isSkippedAutoreplace) {
                    int toDelete = lastWord.length();
                    String newText = top + (primaryCode == Keyboard.KEYCODE_DONE ? "\n" : (char)(primaryCode));

                    ic.beginBatchEdit();
                    ic.deleteSurroundingText(toDelete, 0);
                    ic.commitText(newText, 1);
                    ic.endBatchEdit();

                    showSuggestions(""); // Clear UI
                    break;
                } else {

                    if (isSkippedAutoreplace) {
                        isSkippedAutoreplace = false;
                    }

                    if (primaryCode == Keyboard.KEYCODE_DONE) {
                        EditorInfo editorInfo = getCurrentInputEditorInfo();
                        if (editorInfo != null && (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
                            ic.commitText("\n", 1);
                        } else {
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                        }

                        if (caps_state != 2) {
                            resetCaps();
                        }

                        showSuggestions(""); // Clear bar
                    }

                    else {
                        ic.commitText(Character.toString((char) (primaryCode)), 1);

                        // Auto-cap if punctuation (e.g., after ". ") ans space
                        if (primaryCode == 32 && shouldAutoCap() && defaultCaps) {
                            caps_state = 1;
                            applyCapsState();
                        }
                        break;
                    }
                }
            }
        }
    }

    private void safeReplaceLastWord(InputConnection ic, String lastWord, String replacement) {
        CharSequence beforeCs = ic.getTextBeforeCursor(50, 0);
        if (beforeCs == null) return;

        String before = beforeCs.toString();
        int lastNewline = Math.max(before.lastIndexOf('\n'), before.lastIndexOf('\r'));
        String currentLine = lastNewline == -1 ? before : before.substring(lastNewline + 1);
        currentLine = currentLine.replaceAll("\\s+$", "");

        if (currentLine.endsWith(lastWord)) {
            int deleteCount = lastWord.codePointCount(0, lastWord.length()); // more accurate for emojis
            ic.beginBatchEdit();
            ic.deleteSurroundingText(deleteCount, 0);
            ic.commitText(replacement, 1);
            ic.endBatchEdit();
        }
    }

    private String getLastWordOnCurrentLine(InputConnection ic) {
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        if (before == null) return "";

        String raw = before.toString();
        int lastNewline = Math.max(raw.lastIndexOf('\n'), raw.lastIndexOf('\r'));
        if (lastNewline != -1) {
            raw = raw.substring(lastNewline + 1);
        }

        raw = raw.replaceAll("\\s+$", "");
        String[] parts = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }


    private void handleDelete() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        // Grab up to LOOKBACK code units before the cursor
        CharSequence beforeCs = ic.getTextBeforeCursor(LOOKBACK, 0);
        if (beforeCs == null || beforeCs.length() == 0) return;
        String before = beforeCs.toString();

        // Compute last grapheme boundary
        graphemeIter.setText(before);
        int cursor = before.length();
        int prev = graphemeIter.preceding(cursor);
        if (prev == BreakIterator.DONE) prev = 0;
        int unitsToDelete = cursor - prev;

        ic.deleteSurroundingText(unitsToDelete, 0);
    }

    private boolean isAlphabet(int primaryCode) {
        return LETTERS.contains((char)(primaryCode));
    }

    private boolean isChordable(int code) {
        // Letters, digits, or space
        return code >= 0 && isLetterOrDigit((char)code);
    }

    private boolean isLetterOrDigit(char code) {
        if (code >= '0' && code <= '9') {
            return true;
        }

        return isAlphabet(code);
    }

    private void flushPendingKeys() {
        List<Integer> batch;
        synchronized (pendingKeys) {
            batch = new ArrayList<>(pendingKeys);
            pendingKeys.clear();
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        for (int code : batch) {
            commitChar(ic, code);
        }
    }
    private void commitChar(InputConnection ic, int code) {
        Map<Integer,String> emojis = getEmojiCodes();
        Map<Integer,String> math_symbols = getMathCodes();
        if (emojis.containsKey(code)) {
            ic.commitText(emojis.get(code), 1);
        } else if (math_symbols.containsKey(code)) {
            ic.commitText(math_symbols.get(code), 1);
        } else {
            char c = (char)code;
            if (Character.isLetter(c) && caps_state > 0) {
                c = Character.toUpperCase(c);
            }
            ic.commitText(String.valueOf(c), 1);
            updateSuggestion(ic);
            if (caps_state == 1) resetCaps();
        }
    }

    private void updateSuggestion(InputConnection ic) {
        CharSequence beforeCs = ic.getTextBeforeCursor(50, 0);
        String before = (beforeCs == null ? "" : beforeCs.toString());
        before = before.trim();

        if (before.isEmpty()) {
            showSuggestions("");
            return;
        }

        String[] parts = before.split("\\s+");
        String last_word = parts[parts.length - 1];

        showSuggestions(last_word);
    }

    private void showSuggestions(String prefix) {
        ensureNative();
        suggestionBar.setVisibility(View.VISIBLE);

        Suggestion s = nativeLoaded && !prefix.isEmpty()
                ? nativeSuggest(prefix)
                : new Suggestion(new String[]{"", "", ""}, new double[]{0,0,0});

        String[] words  = s.suggestions;
        double[] scores = s.scores;

        for (int i = 0; i < 3; i++) {
            final String word  = words[i];
            final double score = (i < scores.length ? scores[i] : 0);

            TextView tv = (TextView) suggestionBar.getChildAt(i + 1);
            tv.setText(word);

            // bold if score >= threshold and middle and also not equal to current word
            tv.setTypeface(null, score >= AUTO_REPLACE_THRESHOLD && defaultAutocor && i == 1 && !prefix.equals(word) ? Typeface.BOLD : Typeface.NORMAL);

            tv.setOnClickListener(v -> {
                replaceCurrentWord(word);
                showSuggestions("");
            });

            int finalI = i; // idk android studio told me to

            tv.setOnLongClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && !words[1].isEmpty()) {

                    CharSequence committedText = "";

                    String absPath = getFilesDir().getAbsolutePath() + "/test_files/20k_texting.txt";

                    // if long press on user typed word (0 if has suggestions, 1 if no suggestions), add the word to dictionary
                    if (finalI == 0 && !words[0].isEmpty() && !words[0].equals(" ") && words[0].equals(prefix)) {

                        try {
                            if (!inDictionary(word)) {
                                CustomKeyboardApp.nativeAddWord(word, absPath);
                                committedText = "\n\n" + word + " is added to dictionary!";
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // if long press on suggestions, remove the suggestion from dictionary
                    else {
                        CustomKeyboardApp.nativeRemoveWord(word, absPath);
                        committedText = "\n\n" + word + " is removed from dictionary!";
                    }

                    showSuggestions("");

                    // TODO: fix show toast
                    showToast(committedText); // works but needs notification permission to show toast in background
                    return true;
                }
                return false;
            });
        }
    }

    public void showToast(CharSequence text) {
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    private void replaceCurrentWord(String suggestion) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || suggestion.equals(" ") || suggestion.isEmpty()) return;

        deleteLastWord();

        // Commit the suggestion in its place
        ic.commitText(suggestion + " ", 1);

        showSuggestions("");
    }

    private void deleteLastWord() {
        InputConnection ic = getCurrentInputConnection();
        // Grab up to 50 chars before cursor
        CharSequence beforeCs = ic.getTextBeforeCursor(50, 0);
        String before = beforeCs == null ? "" : beforeCs.toString();

        // Find the start of the curr word
        int lastNewline = Math.max(before.lastIndexOf('\n'), before.lastIndexOf('\r'));
        int lastSpace = before.lastIndexOf(' ');
        int wordStart = Math.max(lastNewline, lastSpace) + 1; // if no space, this is 0
        int wordLength = before.length() - wordStart;

        // Delete that many chars before the cursor
        if (wordLength > 0) {
            ic.deleteSurroundingText(wordLength, 0);
        }
    }


    private static Map<Integer, String> getEmojiCodes() {
        Map<Integer, String> emoji_codes = new HashMap<>();

        for (int i = 0; i < emoji_list.length; i++) {
            int emoji_keycode = 100 + i;
            emoji_keycode = -emoji_keycode; // just add the negative sign for negative keycode

            emoji_codes.put(emoji_keycode, emoji_list[i]);
        }

        return emoji_codes;
    }

    private void updateEmojiLabel() {
        for (Keyboard.Key key : emojiKeyboard.getKeys()) {

            for (int i = 0; i < emoji_list.length; i++) {
                int emoji_keycode = 100 + i;
                emoji_keycode = -emoji_keycode; // just add the negative sign for negative keycode

                if (key.codes[0] == emoji_keycode) {
                    key.label = emoji_list[i];
                    break;
                }
            }
        }
    }
    private static Map<Integer, String> getMathCodes() {
        Map<Integer, String> math_codes = new HashMap<>();

        for (int i = 0; i < math_symbol_list.length; i++) {
            int math_keycode = 1000 + i;
            math_keycode = -math_keycode; // just add the negative sign for negative keycode

            math_codes.put(math_keycode, math_symbol_list[i]);
        }

        return math_codes;
    }

    private void updateMathLabel() {
        for (Keyboard.Key key : mathKeyboard.getKeys()) {

            for (int i = 0; i < math_symbol_list.length; i++) {
                int math_keycode = 1000 + i;
                math_keycode = -math_keycode; // just add the negative sign for negative keycode

                if (key.codes[0] == math_keycode) {
                    key.label = math_symbol_list[i];
                    break;
                }
            }
        }
    }

    private void updateClipboardLabel() {
        for (Keyboard.Key key : clipKeyboard.getKeys()) {
            SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);

            for (int i = 0; i < 10; i++) {
                int clipboard_keycode = 90 + i;
                clipboard_keycode = -clipboard_keycode; // just add the negative sign for negative keycode

                int clipboard_pref_code = i + 1;

                String clipboard_prefs = "clipboard_text_" + clipboard_pref_code;
                String clipboard_text = prefs.getString(clipboard_prefs, "");

                int truncate_length = 20;
                if (clipboard_text.length() > truncate_length) {
                    clipboard_text = clipboard_text.substring(0, truncate_length) + "...";
                }

                if (key.codes[0] == clipboard_keycode) {
                    key.label = clipboard_text;
                    break;
                }
            }
        }
    }

    private boolean shouldAutoCap() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;

        // If there is no character before the cursor, we're at the very start, auto-cap
        CharSequence oneBefore = ic.getTextBeforeCursor(1, 0);
        if (oneBefore == null || oneBefore.length() == 0) {
            return true;
        }

        // If the character immediately before is a newline, auto-cap
        if (oneBefore.charAt(oneBefore.length() - 1) == '\n') {
            return true;
        }

        CharSequence beforeText = ic.getTextBeforeCursor(4, 0); // Check last 4 chars
        if (beforeText == null) return true;

        if (beforeText.length() < 2) return false;

        String lastText = beforeText.toString();

        if (lastText.endsWith("... ")) return false;

        return CAPITALIZE_ENDS.contains(lastText.substring(lastText.length() - 2));
    }

    private boolean isAtLineStart() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return false;

        CharSequence beforeText = ic.getTextBeforeCursor(1, 0);
        // Beginning of text or right after a newline
        return (beforeText == null || beforeText.length() == 0 || beforeText.charAt(0) == '\n');
    }

    private void adjustCapsAfterDeletion() {
        if (caps_state == 2) {
            return;
        }

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
        coyoteHandler.removeCallbacks(flushRunnable);
        synchronized (pendingKeys) { pendingKeys.clear(); }
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

        if (caps_state == 2) {

        } else if (defaultCaps && shouldAutoCap()) {
            caps_state = 1;
        } else {
            caps_state = 0;
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
        for (Keyboard.Key key : kv.getKeyboard().getKeys()) {
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
        kv.getKeyboard().setShifted(caps_state > 0);
        updateCapsLabel();
        kv.invalidateAllKeys();
    }


    private int updateTheme() {
        // get saved theme
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        String keyColor = prefs.getString("key_color", "Unselected");

        // unselected theme defaults to shun
        if (keyColor.equals("Unselected")) {
            keyColor = "Shun";
        }

        // return theme id
        return getResources().getIdentifier("Theme.FQHLLKeyboard." + keyColor, "style", getPackageName());
    }

    private void editEmojiArray() {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        String emoji_variation = prefs.getString("emoji_variation", "neutral").toLowerCase();

        for (int i=0; i < emoji_list.length; i++) {
            String emoji = emoji_list[i];

            if (emoji_variations.containsKey(emoji)) {

                if (emoji_variation.equals("masculine")) {
                    emoji_list[i] = emoji_variations.get(emoji)[0];
                } else if (emoji_variation.equals("feminine")) {
                    emoji_list[i] = emoji_variations.get(emoji)[1];
                } else {
                    emoji_list[i] = emoji_variations.get(emoji)[2];
                }

            }
        }
    }


    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        defaultCaps = prefs.getBoolean("capsToggle", true);
        defaultAutocor = prefs.getBoolean("autocorToggle", true);

        kv.setKeyboard(keyboard);
        showSuggestions("");
        caps_state = (defaultCaps && shouldAutoCap()) ? 1 : 0;
        applyCapsState();  // Just updates the shift key appearance
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (!"key_color".equals(key) && !"gridToggle".equals(key) && !"keyboard_height".equals(key) && !key.startsWith("clipboard") && !"keyboard_layout".equals(key) && !"emoji_variation".equals(key)) {
            return;
        }

        if ("emoji_variation".equals(key)) {
            editEmojiArray();
        }
        View newRoot = buildKeyboardView();
        setInputView(newRoot);
        root = newRoot;
        applyCapsState();
    }

    private View buildKeyboardView() {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // 1) Figure out the theme, but don't gate inflation on it
        int themeId = updateTheme();

        ContextThemeWrapper wrap;

        if (themeId != 0) {
            wrap = new ContextThemeWrapper(this, themeId);
        } else {
            return root;
        }

        // 2) Always inflate under that context
        LayoutInflater li = LayoutInflater.from(wrap);
        View root = li.cloneInContext(wrap)
                .inflate(R.layout.custom_keyboard_layout, null);

        // 3) (Exactly as before) wire up your KeyboardView + pop‑up machinery
        kv = root.findViewById(R.id.keyboard_view);
        View clipboard = root.findViewById(R.id.btn_clipboard);
        View textEditor = root.findViewById(R.id.btn_editor);

        emojiKeyboard = new Keyboard(wrap, R.xml.emojis);
        symbolKeyboard= new Keyboard(wrap, R.xml.symbols);
        mathKeyboard  = new Keyboard(wrap, R.xml.math_symbols);
        clipKeyboard  = new Keyboard(wrap, R.xml.clipboard);
        numpadKeyboard= new Keyboard(wrap, R.xml.numpad);
        zhuyinKeyboard= new Keyboard(wrap, R.xml.custom_keypad_zhuyin);

        String keyboardHeight = prefs.getString("keyboard_height", "Short");
        String keyboardLayout = prefs.getString("keyboard_layout", "qwerty").toLowerCase();

        if (keyboardLayout.equals("qwerty")) {
            switch (keyboardHeight) {
                case "Short":
                    keyboard = new Keyboard(wrap, R.xml.custom_keypad_short);
                    break;
                case "Medium":
                    keyboard = new Keyboard(wrap, R.xml.custom_keypad_medium);
                    break;
                case "Tall":
                    keyboard = new Keyboard(wrap, R.xml.custom_keypad_tall);
                    break;
                default:
                    keyboard = new Keyboard(wrap, R.xml.custom_keypad_qwerty);
                    break;
            }
            
            engKeyboard = keyboard;
        }

        else if (keyboardLayout.equals("zhuyin")) {
            keyboard = zhuyinKeyboard;
            engKeyboard = new Keyboard(wrap, R.xml.custom_keypad_qwerty);
        }

        else {
            String layoutName = "custom_keypad_" + keyboardLayout;
            int layoutXml = getResources().getIdentifier(layoutName, "xml", getPackageName());
            keyboard = new Keyboard(wrap, layoutXml);
            engKeyboard = keyboard;
        }

        // Update layout
        String absPath = getFilesDir().getAbsolutePath() + "/test_files/20k_texting.txt";
        ensureNative();
        nativeSetLayout(keyboardLayout, absPath);

        if (!prefs.getBoolean("gridToggle", false)) {
            editorKeyboard = new Keyboard(wrap, R.xml.editor_maximize);
        }
        else {
            editorKeyboard = new Keyboard(wrap, R.xml.editor_grid);
        }

        // toggle clipboard and normal keyboard
        clipboard.setOnClickListener(v -> {
            if (kv.getKeyboard() == clipKeyboard) {
                kv.setKeyboard(keyboard);
            }
            else {
                kv.setKeyboard(clipKeyboard);
            }
            kv.invalidateAllKeys();
        });

        // long click to clear clipboard
        clipboard.setOnLongClickListener(v -> {
            if (kv.getKeyboard() == clipKeyboard) {
                for (int i = 1; i < 11; i++) {
                    String clipboardPrefs = "clipboard_text_" + i;
                    prefs.edit().putString(clipboardPrefs, "").apply();
                }
                return true;
            }
            else {
                return false;
            }
        });

        // toggle text editor and normal keyboard
        textEditor.setOnClickListener(v -> {
            if (kv.getKeyboard() == editorKeyboard) {
                kv.setKeyboard(keyboard);
                kv.invalidateAllKeys();
            }
            else if (kv.getKeyboard() != numpadKeyboard) { // prevent long press for numpad on release go back to editor
                kv.setKeyboard(editorKeyboard);
                kv.invalidateAllKeys();
            }
        });

        // Long press text editor to commit a space without autocorrecting
        textEditor.setOnLongClickListener(v -> {
            if (kv.getKeyboard() == zhuyinKeyboard && !keyboard.equals(zhuyinKeyboard)) {
                kv.setKeyboard(keyboard);
            }
            else if (kv.getKeyboard() == zhuyinKeyboard && keyboard.equals(zhuyinKeyboard)) {
                kv.setKeyboard(engKeyboard);
            }
            else {
                kv.setKeyboard(zhuyinKeyboard);
            }
            kv.invalidateAllKeys();
            return true;
        });

//        // long press text editor to open numpad
//        textEditor.setOnLongClickListener(v -> {
//            kv.setKeyboard(numpadKeyboard);
//            kv.invalidateAllKeys();
//            return true;
//        });

//        suggestion1.setOnLongClickListener(v -> {
//            InputConnection ic = getCurrentInputConnection();
//            if (ic != null) {
//                ic.commitText("added", 1);
//                showSuggestions("");
//            }
//            return true;
//        });

        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        kv.setPreviewEnabled(false);
        kv.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                lastTouchX = (int)ev.getX();
                lastTouchY = (int)ev.getY();
            }
            if (ev.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return false;
        });

        // 4) Compute scale *after* layout
        scaleX = scaleY = 1f;
        kv.post(() -> {
            scaleX = (float)kv.getWidth()  / kv.getKeyboard().getMinWidth();
            scaleY = (float)kv.getHeight() / kv.getKeyboard().getHeight();
        });

        // 5) Recreate your manual popup
        previewText = new TextView(this);
        previewText.setBackgroundColor(getResources().getColor(android.R.color.white));
        previewText.setTextColor(getResources().getColor(android.R.color.black));
        previewText.setTextSize(26f);
        previewText.setGravity(Gravity.CENTER);

        keyPreviewPopup = new PopupWindow(previewText,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setAnimationStyle(0);

        updateCapsLabel();
        updateEmojiLabel();
        updateMathLabel();
        updateClipboardLabel();
        ensureNative();
        suggestionBar = root.findViewById(R.id.suggestion_bar_container);

        return root;
    }

    private void moveClipboardContent(int i) {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        for (int j = i-1; j > 0; j--) {
            String prev_pref = "clipboard_text_" + j;
            String curr_pref = "clipboard_text_" + (j+1);
            String prev_text = prefs.getString(prev_pref, "");
            prefs.edit().putString(curr_pref, prev_text).apply();
        }
    }

    private void copyToClipboard(String text) {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);

//        // slot names start from 1, ends at 10
//        for (int i = 1; i < 11; i++) {
//            String clipboard_pref = "clipboard_text_" + i;
//            String clipboard_text = prefs.getString(clipboard_pref, "nothing here in this FQ-HLL clipboard slot"); // not naturally occurring def value
//
//            // if theres an empty slot, move everything below 1 slot then copy to first
//            if (clipboard_text.equals("nothing here in this FQ-HLL clipboard slot")) {
//
//                moveClipboardContent(i);
//                prefs.edit().putString("clipboard_text_1", text).apply();
//                return;
//            }
//        }
//
//        // if no empty slots, copy all slots down, then copy to first

        // copy all slots down then copy to first
        moveClipboardContent(10);
        prefs.edit().putString("clipboard_text_1", text).apply();
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
