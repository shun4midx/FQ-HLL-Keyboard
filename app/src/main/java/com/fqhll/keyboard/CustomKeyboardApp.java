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
import android.util.TypedValue;
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
import android.media.SoundPool;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Deque;
import java.util.ArrayDeque;
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
    private boolean useFullStopComment = false;
    private static final int DOUBLE_TAP_TIMEOUT = 300; // Smth like Gboard capping

    // Don't show pop-up for SPACE, CAPS (-1), DELETE (-5), Symbols (-10 from symbols page and -2 from main page), or ENTER (-4)
    private static final Set<Integer> NO_POPUP = new HashSet<>(Arrays.asList(32, -1, -5, -10, -2, -4, -42, -52));
    private static final Set<String> CAPITALIZE_ENDS = new HashSet<>(Arrays.asList(". ", "! ", "? "));

    private static final Set<Character> LETTERS = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'));
    private static final String[] engLetterArray = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private static final String[] zhuyinLetterArray = new String[]{"ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ", "ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ", "ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ", "ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ", "ㄧ", "ㄨ", "ㄩ", "ˉ", "ˊ", "ˇ", "ˋ", "˙"};
    private static String[] longPressSymbols = new String[]{};
    private static final String[] longPressSymbolsMain = new String[]{"\"", "}", "\\", "(", "/", ")", "*", "#", "&", "%", "+", "-", ">", "<", "^", "~", "?", "$", "'", "@", ";", "{", "!", "=", ":", "_"};
    private static final String[] longPressSymbolsAlt = new String[]{"@", ";", "'", "$", "|", "_", "&", "-", ">", "+", "(", ")", "?", "!", "{", "}", "%", "=", "#", "[", "<", ":", "\\", "\"", "]", "*"};
    private float scaleX, scaleY;
    private int lastTouchX, lastTouchY;

    private Keyboard.Key tapped;

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

    public static final String[] math_symbol_list = new String[]{"¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹", "⁰", "∀", "∃", "⇔", "⇒", "Δ", "θ", "π", "ƒ", "α", "β", "±", "≠", "≈", "≡", "Σ", "√", "∩", "∪", "∈", "∋", "⊂", "⊃", "⊆", "⊇", "□", "∅", "∞"};
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

    private SoundPool soundPool;
    private int clickSoundId;
    private boolean isKeySoundEnabled = true;

    private android.widget.PopupWindow candidatesPopup;
    private boolean zhuyinExpanded = false;

    List<String> zhuyinSuggestions = new ArrayList<>();
    private static final Set<Character> ZHUYIN_DELIMITERS = new HashSet<>(Arrays.asList('˙', 'ˊ', 'ˇ', 'ˋ', ' '));
    private StringBuilder zhuyinBuffer = new StringBuilder();
    private LinearLayout zhuyinCompositionBar;
    private TextView zhuyinCompositionText;

    private ZhuyinTyper zhuyinTyper;

    // Map math Unicode symbols to ASCII equivalents
    private static final Map<Character, String> mathNaturalize = Map.ofEntries(
            Map.entry('×', "*"),
            Map.entry('÷', "/")
    );

    // Map superscript Unicode chars to normal digits/operators
    private static final Map<Character, String> superscripts = Map.ofEntries(
            Map.entry('⁰', "0"),
            Map.entry('¹', "1"),
            Map.entry('²', "2"),
            Map.entry('³', "3"),
            Map.entry('⁴', "4"),
            Map.entry('⁵', "5"),
            Map.entry('⁶', "6"),
            Map.entry('⁷', "7"),
            Map.entry('⁸', "8"),
            Map.entry('⁹', "9"),
            Map.entry('⁺', "+"),
            Map.entry('⁻', "-"),
            Map.entry('⁽', "("),
            Map.entry('⁾', ")"),
            Map.entry('ˣ', "*"),
            Map.entry('ᐟ', "/"),
            Map.entry('˙', ".")
    );



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

    private void updateCompositionBarVisibility() {
        if (kv != null && kv.getKeyboard() == zhuyinKeyboard) {
            zhuyinCompositionBar.setVisibility(View.VISIBLE);
        } else {
            zhuyinCompositionBar.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateInputView() {
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        defaultCaps = prefs.getBoolean("capsToggle", true);
        defaultAutocor = prefs.getBoolean("autocorToggle", true);
        caps_state = defaultCaps ? 1 : 0;
        init_emoji_variations();
        initSoundPool();
        editEmojiArray();

        root = buildKeyboardView();
        applyCapsState();
        copyAssetToInternal(getApplicationContext(), "test_files/20k_texting.txt");
        copyAssetToInternal(getApplicationContext(), "tsi_custom.json");
        zhuyinTyper = new ZhuyinTyper(getApplicationContext());

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

    private void commitTextAndShowLabel(String commitText) {
        InputConnection ic = getCurrentInputConnection();
        ic.commitText(commitText, 1);
        setPreviewLabel(commitText);
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
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
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
            case 46: // full stop -> delete last word or enter '//'
                if (kv.getKeyboard() == numpadKeyboard) {
                    commitTextAndShowLabel("˙");
                    updateSuggestion(ic);
                } else {
                    if (useFullStopComment) {
                        commitTextAndShowLabel("//");
                    }
                    else {
                        deleteLastWordWithSpace();
                        showSuggestions("");
                    }
                }
                break;
            case 47: // slash -> backslash
                commitTextAndShowLabel("\\");
                updateSuggestion(ic);
                break;
            case 65292: // chi comma -> chi full stop
                commitTextAndShowLabel("。");
                break;
            case -4: // zhuyin enter -> open settings, eng enter -> skip word
                if (kv.getKeyboard() == zhuyinKeyboard) {
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
            case '[':
                commitTextAndShowLabel("{");
                updateSuggestion(ic);
                break;
            case ']':
                commitTextAndShowLabel("}");
                updateSuggestion(ic);
                break;
            case '_':
                commitTextAndShowLabel("|");
                updateSuggestion(ic);
                break;
            case '!':
                commitTextAndShowLabel("！");
                updateSuggestion(ic);
                break;
            case '?':
                commitTextAndShowLabel("？");
                updateSuggestion(ic);
                break;
            case '~':
                commitTextAndShowLabel("～");
                updateSuggestion(ic);
                break;
            case ':':
                commitTextAndShowLabel("：");
                updateSuggestion(ic);
                break;
            case '(':
                if (kv.getKeyboard() == numpadKeyboard) {
                    commitTextAndShowLabel("⁽");
                } else {
                    commitTextAndShowLabel("（");
                }
                updateSuggestion(ic);
                break;
            case ')':
                if (kv.getKeyboard() == numpadKeyboard) {
                    commitTextAndShowLabel("⁾");
                } else {
                    commitTextAndShowLabel("）");
                }
                updateSuggestion(ic);
                break;
            case '-':
                if (kv.getKeyboard() == numpadKeyboard) {
                    commitTextAndShowLabel("⁻");
                } else {
                    commitTextAndShowLabel("、");
                }
                updateSuggestion(ic);
                break;
            case '<':
                commitTextAndShowLabel("「");
                updateSuggestion(ic);
                break;
            case '>':
                commitTextAndShowLabel("」");
                updateSuggestion(ic);
                break;
            case '\'':
                commitTextAndShowLabel("『");
                updateSuggestion(ic);
                break;
            case '"':
                commitTextAndShowLabel("』");
                updateSuggestion(ic);
                break;
            case ';':
                commitTextAndShowLabel("；");
                updateSuggestion(ic);
                break;
            case '1':
                commitTextAndShowLabel("¹");
                updateSuggestion(ic);
                break;
            case '2':
                commitTextAndShowLabel("²");
                updateSuggestion(ic);
                break;
            case '3':
                commitTextAndShowLabel("³");
                updateSuggestion(ic);
                break;
            case '4':
                commitTextAndShowLabel("⁴");
                updateSuggestion(ic);
                break;
            case '5':
                commitTextAndShowLabel("⁵");
                updateSuggestion(ic);
                break;
            case '6':
                commitTextAndShowLabel("⁶");
                updateSuggestion(ic);
                break;
            case '7':
                commitTextAndShowLabel("⁷");
                updateSuggestion(ic);
                break;
            case '8':
                commitTextAndShowLabel("⁸");
                updateSuggestion(ic);
                break;
            case '9':
                commitTextAndShowLabel("⁹");
                updateSuggestion(ic);
                break;
            case '0':
                commitTextAndShowLabel("⁰");
                updateSuggestion(ic);
                break;
            case -1000: // superscript 1
                commitTextAndShowLabel("₁");
                updateSuggestion(ic);
                break;
            case -1001:
                commitTextAndShowLabel("₂");
                updateSuggestion(ic);
                break;
            case -1002:
                commitTextAndShowLabel("₃");
                updateSuggestion(ic);
                break;
            case -1003:
                commitTextAndShowLabel("₄");
                updateSuggestion(ic);
                break;
            case -1004:
                commitTextAndShowLabel("₅");
                updateSuggestion(ic);
                break;
            case -1005:
                commitTextAndShowLabel("₆");
                updateSuggestion(ic);
                break;
            case -1006:
                commitTextAndShowLabel("₇");
                updateSuggestion(ic);
                break;
            case -1007:
                commitTextAndShowLabel("₈");
                updateSuggestion(ic);
                break;
            case -1008:
                commitTextAndShowLabel("₉");
                updateSuggestion(ic);
                break;
            case -1009:
                commitTextAndShowLabel("₀");
                updateSuggestion(ic);
                break;
            case -1010: // for all
                commitTextAndShowLabel("∵");
                updateSuggestion(ic);
                break;
            case -1011: // there exists
                commitTextAndShowLabel("∴");
                updateSuggestion(ic);
                break;
            case -1030: // subset
                commitTextAndShowLabel("⇌");
                updateSuggestion(ic);
                break;
            case -1031: // supset
                commitTextAndShowLabel("⇋");
                updateSuggestion(ic);
                break;
            case -1028: // in
                commitTextAndShowLabel("→");
                updateSuggestion(ic);
                break;
            case -1029: // ni
                commitTextAndShowLabel("↦");
                updateSuggestion(ic);
                break;
            case -1018: // alpha
                commitTextAndShowLabel("∝");
                updateSuggestion(ic);
                break;
            case '+':
                commitTextAndShowLabel("⁺");
                updateSuggestion(ic);
                break;
            case '@':
                commitTextAndShowLabel("⁻");
                updateSuggestion(ic);
                break;
            case '×':
                commitTextAndShowLabel("ˣ");
                updateSuggestion(ic);
                break;
            case '÷':
                commitTextAndShowLabel("ᐟ");
                updateSuggestion(ic);
                break;
            case '=':
                commitTextAndShowLabel("⁼");
                updateSuggestion(ic);
                break;
            case '&':
                commitTextAndShowLabel("⁽");
                updateSuggestion(ic);
                break;
            case '*':
                commitTextAndShowLabel("⁾");
                updateSuggestion(ic);
                break;
            case '#':
                commitTextAndShowLabel("₊");
                updateSuggestion(ic);
                break;
            case '$':
                commitTextAndShowLabel("₋");
                updateSuggestion(ic);
                break;
            case '%':
                if (kv.getKeyboard() != numpadKeyboard) {
                    commitTextAndShowLabel("ₓ");
                    updateSuggestion(ic);
                }
                break;
            case '^':
                commitTextAndShowLabel("₌");
                updateSuggestion(ic);
                break;
            case -1023: // cong
                commitTextAndShowLabel("≅");
                updateSuggestion(ic);
                break;
            case -1026: // int
                commitTextAndShowLabel("ⁿ");
                updateSuggestion(ic);
                break;
            case -1027: // uni
                commitTextAndShowLabel("ₙ");
                updateSuggestion(ic);
                break;
            case -1015: // theta
                commitTextAndShowLabel("ㄥ");
                updateSuggestion(ic);
                break;
            case -1014: // delta
                commitTextAndShowLabel("△");
                updateSuggestion(ic);
                break;
            case -1016: // pi
                commitTextAndShowLabel("Π");
                updateSuggestion(ic);
                break;
            case -1017: // f
                commitTextAndShowLabel("∫");
                updateSuggestion(ic);
                break;
            case -1032: // subseteq
                commitTextAndShowLabel("≤");
                updateSuggestion(ic);
                break;
            case -1033: // supseteq
                commitTextAndShowLabel("≥");
                updateSuggestion(ic);
                break;
            case -1013: // =>
                commitTextAndShowLabel("⇐");
                updateSuggestion(ic);
                break;
            case -1034: // qed
                commitTextAndShowLabel("⊕");
                updateSuggestion(ic);
                break;
            case -1035: // nullset
                commitTextAndShowLabel("⊗");
                updateSuggestion(ic);
                break;
            case -1036: // infty
                commitTextAndShowLabel("⊙");
                updateSuggestion(ic);
                break;

            default:
                // hold down eng letters for symbols, zhuyin letters to commit the letter
                String symbol = "";
                String[] longPressText = longPressSymbols;
                String[] letterArray = engLetterArray;

                if (kv.getKeyboard() == zhuyinKeyboard) {
                    longPressText = zhuyinLetterArray;
                    letterArray = zhuyinLetterArray;
                }

                for (int i=0; i<letterArray.length; i++) {
                    if (String.valueOf((char) primaryCode).equals(letterArray[i])) {

                        if (i < longPressText.length) { // just in case i make it too short
                            symbol = longPressText[i];
                        }
                        commitTextAndShowLabel(symbol);
                    }
                }
                break;
        }
    }

    private void showCandidatePopup(List<String> items) {
        // Container for the list
        Context ctx = this;
        RecyclerView rv = new RecyclerView(ctx);

        // LayoutManager: Flexbox if available, else 6-col Grid
        RecyclerView.LayoutManager lm;
        try {
            Class.forName("com.google.android.flexbox.FlexboxLayoutManager");
            com.google.android.flexbox.FlexboxLayoutManager flm =
                    new com.google.android.flexbox.FlexboxLayoutManager(ctx);
            flm.setFlexDirection(com.google.android.flexbox.FlexDirection.ROW);
            flm.setFlexWrap(com.google.android.flexbox.FlexWrap.WRAP);
            flm.setJustifyContent(com.google.android.flexbox.JustifyContent.FLEX_START);
            flm.setAlignItems(com.google.android.flexbox.AlignItems.CENTER);
            lm = flm;
        } catch (Throwable noFlex) {
            lm = new androidx.recyclerview.widget.GridLayoutManager(ctx, 6);
        }
        rv.setLayoutManager(lm);

        // Simple adapter for chips
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int vt) {
                View v = LayoutInflater.from(p.getContext())
                        .inflate(R.layout.item_candidate_chip, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int i) {
                TextView tv = h.itemView.findViewById(R.id.txt);
                String s = items.get(i);
                tv.setText(s);
                tv.setOnClickListener(v -> {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        replaceCurrentWord(s);
                    }
                    if (candidatesPopup != null && candidatesPopup.isShowing()) {
                        candidatesPopup.dismiss();
                    }
                });
            }
            @Override public int getItemCount() { return items.size(); }
        });

        int pad = (int)(12 * getResources().getDisplayMetrics().density);
        rv.setPadding(pad, pad, pad, pad);

        // Build the popup
        if (candidatesPopup != null && candidatesPopup.isShowing()) {
            candidatesPopup.dismiss();
        }
        candidatesPopup = new android.widget.PopupWindow(
                rv,
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int)(getResources().getDisplayMetrics().heightPixels * 0.45f), // ~45% screen height
                true /* focusable so back dismisses */
        );
        candidatesPopup.setOutsideTouchable(true);
        candidatesPopup.setClippingEnabled(true);
        // Optional: give it a tiny elevation/background
        candidatesPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFFFFFFF));

        // Anchor at bottom of your IME root
        if (root != null) {
            candidatesPopup.showAtLocation(root, Gravity.BOTTOM, 0, 0);
        }
    }

    @Override
    public void onPress(int primaryCode) {

        if (isKeySoundEnabled) {
            playClick();
        }

        isLongPress = false;

        longPressRunnable = () -> {
            handleLongPress(primaryCode);
            isLongPress = true;
        };
        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_MS);

        if (NO_POPUP.contains(primaryCode)) return;

        if (-99 <= primaryCode && primaryCode <= -90) return; // clipboard
        if (-79 <= primaryCode && primaryCode <= -60) return; // text editor
//        if (-1049 <= primaryCode && primaryCode <= -1000) return; // math symbols

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
        tapped = null;
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

    private void setPreviewLabel(CharSequence label) {
        if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
            previewText.setText(label);
            previewText.invalidate();
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

    private void initSoundPool() {
        soundPool = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);

        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        String soundEffect = prefs.getString("key_sound_effect", "click").toLowerCase();
        int soundEffectId = getResources().getIdentifier(soundEffect, "raw", getPackageName());
        clickSoundId = soundPool.load(this, soundEffectId, 1);
    }

    private void playClick() {
        if (soundPool != null && clickSoundId != 0) {
            soundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

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
                updateSuggestion(ic);
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
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
                break;
            case -10: // back to main
                kv.setKeyboard(keyboard);
                applyCapsState();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
                break;
            case -11: // emojis
                kv.setKeyboard(emojiKeyboard);
                kv.invalidateAllKeys();
                showSuggestions("");
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
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
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
                break;
            case -14: // math symbols
                kv.setKeyboard(mathKeyboard);
                kv.invalidateAllKeys();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
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
//                        showSuggestions("");
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
//                        showSuggestions("");
                    }
                }
                return;
            case -62: // up arrow
                CharSequence before2 = ic.getTextBeforeCursor(1, 0);
                if (before2 != null && before2.length() > 0) {
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                    ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
//                    showSuggestions("");
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
//                    showSuggestions("");
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
            case 61: // equal key for numpad
                String expr = getCurrentExpression();
                if (expr.isEmpty() || expr.charAt(expr.length() - 1) != '=') {
                    ic.commitText("=", 1);
                } else { // User wanted to calculate ==
                    try {
                        // remove the '=' at the end before evaluation
                        expr = expr.substring(0, expr.length() - 1);
                        expr = normalizeSuperscripts(expr);
                        expr = normalizeMathSymbols(expr);
                        double res = evaluateExpression(expr);  // returns primitive double

                        String resultStr;
                        if (Math.abs(res - Math.rint(res)) < 1e-9) {
                            resultStr = String.valueOf((long) Math.round(res));
                        } else {
                            // Otherwise keep decimal form
                            resultStr = String.valueOf(res);
                        }

                        ic.deleteSurroundingText(expr.length() + 1, 0); // Rmb the = deleted in param
                        ic.commitText(resultStr, 1);
                    } catch (Exception e) {
                        // fallback if invalid
                        ic.commitText("=", 1);
                    }
                }
                break;
            default: {
                if (isAlphabet(primaryCode) || (kv.getKeyboard() != engKeyboard && kv.getKeyboard() != clipKeyboard && primaryCode != Keyboard.KEYCODE_DONE)) {
                    commitChar(ic, primaryCode);
                    updateSuggestion(ic);
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

                if (primaryCode == Keyboard.KEYCODE_DONE) {
                    if (kv.getKeyboard() == numpadKeyboard) {
                        expr = getCurrentExpression();
                        try {
                            expr = normalizeSuperscripts(expr);
                            expr = normalizeMathSymbols(expr);
                            double res = evaluateExpression(expr);  // returns primitive double

                            String resultStr;
                            if (Math.abs(res - Math.rint(res)) < 1e-9) {
                                resultStr = String.valueOf((long) Math.round(res));
                            } else {
                                // Otherwise keep decimal form
                                resultStr = String.valueOf(res);
                            }

                            ic.commitText("=" + resultStr, 1);

                            // Clear UI and mark bar inactive
                            showSuggestions("");
                            break;
                        } catch (Exception e) {
                            // fallback if invalid, swallow it
                        }
                    } else if (kv.getKeyboard() == engKeyboard) {
                        // Use EXACTLY what's currently shown in the bar
                        String currentWord = getLastWordOnCurrentLine(ic);

                        if (defaultAutocor && score >= AUTO_REPLACE_THRESHOLD && !top.isEmpty() && !top.equals(lastWordStr) && prevChar != ' ' && !isSkippedAutoreplace) {
                            // Accept the visible center suggestion
                            safeReplaceLastWord(ic, currentWord, top);
                        }

                        if (caps_state != 2) {
                            resetCaps();
                        }
                    }

                    // Now send the Enter/new line per IME options
                    EditorInfo editorInfo = getCurrentInputEditorInfo();
                    if (editorInfo != null && (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
                        ic.commitText("\n", 1);
                    } else {
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                    }

                    // Clear UI and mark bar inactive
                    showSuggestions("");
                    break;
                } else if (defaultAutocor && score >= AUTO_REPLACE_THRESHOLD && !top.isEmpty() && !top.equals(lastWordStr) && prevChar != ' ' && !isSkippedAutoreplace) {
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

                    if (kv.getKeyboard() == zhuyinKeyboard) {
                        commitChar(ic, primaryCode);
                        updateSuggestion(ic);
                    } else {
                        ic.commitText(Character.toString((char) (primaryCode)), 1);
                    }

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

    private void updateModeSwitchLabel() {
        if (kv == null || kv.getKeyboard() == null) return;

        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        String keyboardLayout = prefs.getString("keyboard_layout", "qwerty").toLowerCase();

        for (Keyboard.Key key : kv.getKeyboard().getKeys()) {
            if (key.codes[0] == -10) { // abc <-> symbols key
                if (keyboardLayout.equals("zhuyin")) {
                    key.label = "中文"; // show 中文 when on Zhuyin
                } else {
                    key.label = "abc"; // default
                }
                break;
            }
        }
        kv.invalidateAllKeys(); // refresh display
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

    private String getCurrentExpression() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return "";
        ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
        return et != null ? et.text.toString() : "";
    }

    private static double evaluateExpression(String expr) throws Exception {
        // 1. Tokenize
        List<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (char c : expr.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                num.append(c);
            } else if ("+-*/()^".indexOf(c) >= 0) {
                if (num.length() > 0) {
                    tokens.add(num.toString());
                    num.setLength(0);
                }

                String tok = Character.toString(c);

                // Implicit multiplication: number or ')' followed by '('
                if (tok.equals("(") &&
                        !tokens.isEmpty() &&
                        (tokens.get(tokens.size() - 1).matches("\\d+(\\.\\d+)?") || tokens.get(tokens.size() - 1).equals(")"))) {
                    tokens.add("*");
                }

                if (tok.equals("-")) {
                    // unary minus handling
                    String prev = tokens.isEmpty() ? "" : tokens.get(tokens.size() - 1);
                    if (tokens.isEmpty() || "+-*/%^(".contains(prev)) {
                        tokens.add("0");
                    }
                }

                tokens.add(tok);
            } else if (c == '%') {
                if (num.length() > 0) {
                    // Turn the number into a percentage
                    double val = Double.parseDouble(num.toString()) / 100.0;
                    tokens.add(String.valueOf(val));
                    num.setLength(0);
                } else {
                    throw new Exception("Unexpected %");
                }
            } else if (!Character.isWhitespace(c)) {
                throw new Exception("Invalid char: " + c);
            }
        }
        if (num.length() > 0) {
            tokens.add(num.toString());
        }

        // 2. Infix to Postfix (Shunting-yard)
        Map<String, Integer> prec = Map.of(
                "+", 1, "-", 1,
                "*", 2, "/", 2, "%", 2,
                "^", 3
        );
        List<String> output = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();
        for (String t : tokens) {
            if (t.matches("\\d+(\\.\\d+)?")) { // number
                output.add(t);
            } else if (prec.containsKey(t)) { // operator
                while (!ops.isEmpty() && prec.containsKey(ops.peek())) {
                    // if right-associative operator (^) then use > instead of >=
                    if ((t.equals("^") && prec.get(ops.peek()) > prec.get(t)) ||
                            (!t.equals("^") && prec.get(ops.peek()) >= prec.get(t))) {
                        output.add(ops.pop());
                    } else break;
                }
                ops.push(t);
            } else if (t.equals("(")) {
                ops.push(t);
            } else if (t.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    output.add(ops.pop());
                }
                if (ops.isEmpty() || !ops.peek().equals("("))
                    throw new Exception("Mismatched parentheses");
                ops.pop(); // discard "("
            }
        }
        while (!ops.isEmpty()) {
            String op = ops.pop();
            if (op.equals("(") || op.equals(")"))
                throw new Exception("Mismatched parentheses");
            output.add(op);
        }

        // 3. Evaluate Postfix
        Deque<Double> stack = new ArrayDeque<>();
        for (String t : output) {
            if (t.matches("\\d+(\\.\\d+)?")) {
                stack.push(Double.parseDouble(t));
            } else { // operator
                double b = stack.pop(), a = stack.pop();
                switch (t) {
                    case "+": stack.push(a + b); break;
                    case "-": stack.push(a - b); break;
                    case "*": stack.push(a * b); break;
                    case "/": stack.push(a / b); break;
                    case "%": stack.push(a % b); break;
                    case "^": stack.push(Math.pow(a, b)); break;
                }
            }
        }
        if (stack.size() != 1) {
            throw new Exception("Invalid expression");
        }
        return stack.pop();
    }

    private static String normalizeSuperscripts(String expr) {
        StringBuilder out = new StringBuilder();
        StringBuilder expBuf = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (superscripts.containsKey(c)) {
                // Add this superscript to the buffer
                expBuf.append(superscripts.get(c));
            } else {
                // If we just finished a superscript run, flush it
                if (expBuf.length() > 0) {
                    out.append("^(").append(expBuf).append(")");
                    expBuf.setLength(0);
                }
                out.append(c);
            }
        }

        // Flush trailing exponent at end of string
        if (expBuf.length() > 0) {
            out.append("^(").append(expBuf).append(")");
        }

        return out.toString();
    }

    private static String normalizeMathSymbols(String expr) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (mathNaturalize.containsKey(c)) {
                out.append(mathNaturalize.get(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
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

        if (kv != null && kv.getKeyboard() == zhuyinKeyboard && zhuyinBuffer.length() > 0) {
            zhuyinBuffer.setLength(zhuyinBuffer.length() - 1);
            zhuyinCompositionText.setText(zhuyinBuffer.toString());
            return;
        }

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
        char c = (char) code;

        // Zhuyin mode: keep in buffer instead of committing
        if (kv != null && kv.getKeyboard() == zhuyinKeyboard) {
            if (c == ' ' && zhuyinBuffer.length() == 0) {
                ic.commitText(" ", 1);
                return;
            } else if (c == '，') {
                ic.commitText("，", 1);
                return;
            }

            zhuyinBuffer.append(c);

//            zhuyinCompositionBar.setVisibility(View.VISIBLE);
            zhuyinCompositionText.setText(zhuyinBuffer.toString());
            return;
        }

        // Normal mode
        Map<Integer,String> emojis = getEmojiCodes();
        Map<Integer,String> math_symbols = getMathCodes();
        if (emojis.containsKey(code)) {
            ic.commitText(emojis.get(code), 1);
        } else if (math_symbols.containsKey(code)) {
            ic.commitText(math_symbols.get(code), 1);
            updateSuggestion(ic);
        } else {
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

        if (kv != null && kv.getKeyboard() == zhuyinKeyboard) {
            String prefix = zhuyinBuffer.toString();
            showSuggestions(prefix);
            return;
        } else if (kv.getKeyboard() != engKeyboard) {
            // Take everything after the last space/newline
            int lastSpace = Math.max(before.lastIndexOf(' '), before.lastIndexOf('\n'));
            String lastToken = (lastSpace == -1) ? before : before.substring(lastSpace + 1);

            // Strip trailing '=' signs
            lastToken = lastToken.replaceAll("=+$", "");

            showSuggestions(lastToken);
            return;
        }

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

        View scrollView = root.findViewById(R.id.suggestion_scroll);
        LinearLayout strip = root.findViewById(R.id.suggestion_strip);
        TextView s1 = root.findViewById(R.id.suggestion_1);
        TextView s2 = root.findViewById(R.id.suggestion_2);
        TextView s3 = root.findViewById(R.id.suggestion_3);

        if (kv.getKeyboard() != zhuyinKeyboard) {
            s1.setVisibility(View.VISIBLE);
            s2.setVisibility(View.VISIBLE);
            s3.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);

            for (int i = 0; i < 3; i++) {
                final String word = words[i];
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
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        // if long press on suggestions, remove the suggestion from dictionary
                        else {
                            CustomKeyboardApp.nativeRemoveWord(word, absPath);
                        }

                        showSuggestions("");

                        // TODO: fix show toast
                        showToast(committedText); // works but needs notification permission to show toast in background
                        return true;
                    }
                    return false;
                });
            }
        } else {
            s1.setVisibility(View.GONE);
            s2.setVisibility(View.GONE);
            s3.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);

            strip.removeAllViews();

            regenerateZhuyinSuggestions(prefix);

            for (String cand : zhuyinSuggestions) {
                TextView tv = makeSuggestionChip(cand);
                tv.setPadding(30, 4, 30, 4);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                lp.setMargins(20, 0, 20, 0);
                strip.addView(tv, lp);
            }

            scrollView.setVisibility(View.VISIBLE);
            scrollView.post(() -> {
                scrollView.scrollTo(0, 0);
                strip.requestLayout();
                strip.invalidate();
            });
        }
    }

    private void regenerateZhuyinSuggestions(String prefix) {
        zhuyinSuggestions.clear();
        if (prefix.isEmpty() || prefix.equals(" ")) {
            return;
        }

        String[] suggs = zhuyinSuggest(prefix);
        zhuyinSuggestions.addAll(Arrays.asList(suggs));
    }

    private String[] zhuyinSuggest(String prefix) {

        String[] split = splitPrefix(prefix);
        SharedPreferences prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);

        return zhuyinTyper.suggest(split, prefs.getBoolean("etenToggle", false));
    }

    private String[] splitPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (ZHUYIN_DELIMITERS.contains(c)) {
                result.add(prefix.substring(start, i + 1));
                start = i + 1;
            }
        }

        if (start < prefix.length()) {
            String tail = prefix.substring(start);
            if (!tail.trim().isEmpty()) {  // ignore plain space
                result.add(tail);
            }
        }

        return result.toArray(new String[0]);
    }


    private TextView makeSuggestionChip(String text) {
        Context ctx = root.getContext(); // has your selected theme
        TextView tv = new TextView(ctx);

        tv.setText(text);
        tv.setTextSize(20f);
        tv.setPadding(12, 4, 12, 4);
        tv.setGravity(Gravity.CENTER);

        tv.setTextColor(getThemeColor(ctx, R.attr.suggestionBarTextColor));
        tv.setClickable(true);
        tv.setOnClickListener(v -> {
            replaceCurrentWord(text);
            updateSuggestion(getCurrentInputConnection());

            if (zhuyinExpanded) {
                RecyclerView expanded = root.findViewById(R.id.expanded_candidates);
                View kv = root.findViewById(R.id.keyboard_view);

                expanded.setVisibility(View.GONE);
                kv.setVisibility(View.VISIBLE);
                zhuyinExpanded = false;
            }
        });
        return tv;
    }

    private int getThemeColor(Context ctx, int attr) {
        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, typedValue, true);
        if (typedValue.resourceId != 0) {
            return ContextCompat.getColor(ctx, typedValue.resourceId);
        } else {
            return typedValue.data;
        }
    }
    public void showToast(CharSequence text) {
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

//    private String[] splitZhuyinBuffer() {
//        String buf = zhuyinBuffer.toString();
//        if (buf.isEmpty()) return new String[]{"", ""};
//
//        int i = 0;
//        while (i < buf.length()) {
//            char c = buf.charAt(i);
//            if (ZHUYIN_DELIMITERS.contains(c)) {
//                // include delimiter in the syllable
//                return new String[]{ buf.substring(0, i + 1), buf.substring(i + 1).trim() };
//            }
//            i++;
//        }
//        // no delimiter -> whole buffer is one syllable
//        return new String[]{ buf, "" };
//    }

    private String[] splitAllZhuyinSyllables(String buf) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (ZHUYIN_DELIMITERS.contains(c)) {
                result.add(buf.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < buf.length()) {
            result.add(buf.substring(start));
        }
        return result.toArray(new String[0]);
    }

    private void replaceCurrentWord(String suggestion) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || suggestion.equals(" ") || suggestion.isEmpty()) return;

        if (kv != null && kv.getKeyboard() == zhuyinKeyboard) {
            // Break buffer into syllables
            String[] parts = splitAllZhuyinSyllables(zhuyinBuffer.toString());

            // Decide how many syllables the candidate should consume
            int consumeCount = suggestion.length();

            // Build "first" = concatenation of the consumed syllables
            StringBuilder firstBuilder = new StringBuilder();
            for (String part : parts) {
                firstBuilder.append(part);
            }
//            String first = firstBuilder.toString();

            // "rest" = leftover syllables
            StringBuilder restBuilder = new StringBuilder();
            for (int i = consumeCount; i < parts.length; i++) {
                restBuilder.append(parts[i]);
            }
            String rest = restBuilder.toString();

            ic.beginBatchEdit();
            ic.commitText(suggestion, 1);
            ic.endBatchEdit();

            zhuyinBuffer.setLength(0);
            zhuyinBuffer.append(rest);

            if (zhuyinBuffer.length() == 0) {
                zhuyinCompositionText.setText("");
            } else {
                zhuyinCompositionText.setText(zhuyinBuffer.toString());
            }

            ic = getCurrentInputConnection();
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ic != null && ei != null) {
                try {
                    updateSuggestion(ic);
                } catch (Exception e) {
                    // swallow it, don't let IME crash
                }
            }

            return;
        }

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

    // same as deleteLastWord, but deletes a space if cannot find last word
    private void deleteLastWordWithSpace() {
        InputConnection ic = getCurrentInputConnection();
        CharSequence beforeCs = ic.getTextBeforeCursor(50, 0);
        String before = beforeCs == null ? "" : beforeCs.toString();

        int lastNewline = Math.max(before.lastIndexOf('\n'), before.lastIndexOf('\r'));
        int lastSpace = before.lastIndexOf(' ');
        int wordStart = Math.max(lastNewline, lastSpace) + 1;
        int wordLength = before.length() - wordStart;

        if (wordLength > 0) {
            ic.deleteSurroundingText(wordLength, 0);
        }

        else if (wordLength == 0) {
            ic.deleteSurroundingText(1, 0); // hardcoded limit is better than finding number of spaces or newlines
            deleteLastWord(); // avoid recursion
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
        Keyboard k = (kv != null) ? kv.getKeyboard() : null;
        if (k == null) {
            return;
        }

        if (k == mathKeyboard) {
            caps_state = 0;
            k.setShifted(false);
            updateCapsLabel();
            kv.invalidateAllKeys();
            return;
        }

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

        // Reset Zhuyin buffer + bar text
        zhuyinBuffer.setLength(0);
        if (zhuyinCompositionText != null) {
            zhuyinCompositionText.setText("");
        }

        // Always set keyboard first
        kv.setKeyboard(keyboard);
        updateModeSwitchLabel();

        // Now update bar visibility correctly
        if (zhuyinCompositionBar != null) {
            zhuyinCompositionBar.setVisibility(
                    kv.getKeyboard() == zhuyinKeyboard ? View.VISIBLE : View.GONE
            );
        }

        showSuggestions("");
        caps_state = (defaultCaps && shouldAutoCap()) ? 1 : 0;
        applyCapsState();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Set<String> rebuild_prefs = new HashSet<>(Arrays.asList("key_color", "gridToggle", "keyboard_height", "keyboard_layout", "emoji_variation", "etenToggle", "keySoundToggle", "key_sound_effect", "altSymbolToggle", "fullStopCommentToggle"));
        if (!rebuild_prefs.contains(key) && !key.startsWith("clipboard")) {
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

        zhuyinCompositionBar = root.findViewById(R.id.zhuyin_composition_bar);
        zhuyinCompositionText = root.findViewById(R.id.zhuyin_composition_text);

        String keyboardHeight = prefs.getString("keyboard_height", "Short");
        String keyboardLayout = prefs.getString("keyboard_layout", "qwerty").toLowerCase();
        boolean useEtenLayout = prefs.getBoolean("etenToggle", false);
        boolean useAltSymbolLayout = prefs.getBoolean("altSymbolToggle", false);
        useFullStopComment = prefs.getBoolean("fullStopCommentToggle", false);
        isKeySoundEnabled = prefs.getBoolean("keySoundToggle", true);

        if (useEtenLayout) {
            zhuyinKeyboard = new Keyboard(wrap, R.xml.custom_keypad_zhuyin_eten);
        }

        longPressSymbols = longPressSymbolsMain;
        if (useAltSymbolLayout) {
            longPressSymbols = longPressSymbolsAlt;
        }

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

        RecyclerView expanded = root.findViewById(R.id.expanded_candidates);
        View keyboardView = root.findViewById(R.id.keyboard_view);

        // toggle clipboard and normal keyboard
        clipboard.setOnClickListener(v -> {
            if (kv.getKeyboard() == clipKeyboard) {
                kv.setKeyboard(keyboard);
                kv.invalidateAllKeys();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
            }
            else if (!zhuyinExpanded) {
                kv.setKeyboard(clipKeyboard);
                kv.invalidateAllKeys();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
            } else {
                // Minimize
                expanded.setVisibility(View.GONE);
                keyboardView.setVisibility(View.VISIBLE);
                zhuyinExpanded = false;
            }
        });

        // long click to clear clipboard
        clipboard.setOnLongClickListener(v -> {
            if (kv.getKeyboard() == clipKeyboard && !zhuyinExpanded) {
                for (int i = 1; i < 11; i++) {
                    String clipboardPrefs = "clipboard_text_" + i;
                    prefs.edit().putString(clipboardPrefs, "").apply();
                }
                return true;
            } else if (kv.getKeyboard() == zhuyinKeyboard || zhuyinExpanded) {
                if (zhuyinExpanded) {
                    // Minimize
                    expanded.setVisibility(View.GONE);
                    keyboardView.setVisibility(View.VISIBLE);
                    zhuyinExpanded = false;
                } else {
                    // expand panel
                    expanded.setVisibility(View.VISIBLE);
                    keyboardView.setVisibility(View.GONE);

                    expanded.getLayoutParams().height = keyboardView.getHeight();
                    expanded.requestLayout();
                    zhuyinExpanded = true;

                    FlexboxLayoutManager lm = new FlexboxLayoutManager(this);
                    lm.setFlexDirection(FlexDirection.ROW);
                    lm.setFlexWrap(FlexWrap.WRAP);
                    expanded.setLayoutManager(lm);

                    // In buildKeyboardView(), when expanding:
                    zhuyinSuggestions.clear();

                    // reuse the same prefix logic as updateSuggestion/showSuggestions
                    InputConnection ic = getCurrentInputConnection();
                    String prefix = zhuyinBuffer.toString();

                    // generate Zhuyin candidates the same way as the top bar
                    regenerateZhuyinSuggestions(prefix);

                    expanded.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                        @Override
                        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                            View v = LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.item_candidate_chip, parent, false);
                            return new RecyclerView.ViewHolder(v) {
                            };
                        }

                        @Override
                        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                            TextView tv = holder.itemView.findViewById(R.id.txt);
                            String s = zhuyinSuggestions.get(position);
                            tv.setText(s);
                            tv.setOnClickListener(v -> {
                                InputConnection ic = getCurrentInputConnection();
                                if (ic != null) {
                                    replaceCurrentWord(s);
                                }

                                if (zhuyinBuffer.length() == 0) {
                                    // nothing left -> fully minimize
                                    expanded.setVisibility(View.GONE);
                                    keyboardView.setVisibility(View.VISIBLE);
                                    zhuyinExpanded = false;
                                } else {
                                    // still stuff left -> go back to normal bar
                                    expanded.setVisibility(View.GONE);
                                    regenerateZhuyinSuggestions(zhuyinBuffer.toString());
                                    keyboardView.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public int getItemCount() {
                            return zhuyinSuggestions.size();
                        }
                    });
                }

                return true;
            }

            return false;
        });

        // toggle text editor and normal keyboard
        textEditor.setOnClickListener(v -> {
            if (kv.getKeyboard() == editorKeyboard) {
                kv.setKeyboard(keyboard);
                kv.invalidateAllKeys();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
            }
            else if (kv.getKeyboard() != numpadKeyboard && !zhuyinExpanded) { // prevent long press for numpad on release go back to editor
                kv.setKeyboard(editorKeyboard);
                kv.invalidateAllKeys();
                updateCompositionBarVisibility();
                updateModeSwitchLabel();
            }
        });

        textEditor.setOnLongClickListener(v -> {
            if (kv.getKeyboard() == zhuyinKeyboard && !keyboard.equals(zhuyinKeyboard)) {
                kv.setKeyboard(keyboard);
                caps_state = (defaultCaps ? 1 : 0);
                applyCapsState();
            }
            else if (kv.getKeyboard() == zhuyinKeyboard && keyboard.equals(zhuyinKeyboard)) {
                kv.setKeyboard(engKeyboard);
                caps_state = (defaultCaps ? 1 : 0);
                applyCapsState();
            }
            else {
                kv.setKeyboard(zhuyinKeyboard);
            }
            kv.invalidateAllKeys();
            updateCompositionBarVisibility();
            updateModeSwitchLabel();
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
        updateCompositionBarVisibility();
        updateModeSwitchLabel();
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
        initSoundPool();
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
