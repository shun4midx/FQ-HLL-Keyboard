package com.fqhll.keyboard;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PinyinTyper {
    private final Map<String, List<String>> dictionary = new HashMap<>();
    private final Map<String, List<String>> tonelessDictionary = new HashMap<>();

    public PinyinTyper(Context context) {
        loadDictionary(context);
    }

    private void loadDictionary(Context context) {
        try (InputStream input = context.getAssets().open("tsi_custom.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            JSONObject root = new JSONObject(json.toString());

            Iterator<String> keys = root.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = root.getJSONArray(key);
                List<String> candidates = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    candidates.add(array.getString(i));
                }

                dictionary.put(key, candidates);
            }

            Map<String, Map<Character, List<String>>> grouped = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : dictionary.entrySet()) {
                String toneless = removeTone(entry.getKey());

                char tone = getTone(entry.getKey());

                grouped.computeIfAbsent(toneless, k -> new HashMap<>())
                        .put(tone, entry.getValue());
            }

            for (Map.Entry<String, Map<Character, List<String>>> entry : grouped.entrySet()) {
                List<String> merged = new ArrayList<>();
                HashSet<String> seen = new HashSet<>();

                int depth = 0;

                while (true) {
                    boolean added = false;

                    char[] order = {'1', '2', '3', '4', '0'};

                    for (char tone : order) {
                        List<String> list = entry.getValue().get(tone);

                        if (list != null && depth < list.size()) {
                            String word = list.get(depth);

                            if (seen.add(word)) {
                                merged.add(word);
                            }

                            added = true;
                        }
                    }

                    if (!added) {
                        break;
                    }

                    ++depth;
                }

                tonelessDictionary.put(entry.getKey(), merged);
            }
        } catch (Exception ignored) {

        }
    }

    private String removeTone(String zhuyin) {
        return zhuyin
                .replace("ˊ", "")
                .replace("ˇ", "")
                .replace("ˋ", "")
                .replace("˙", "");
    }

    private char getTone(String zhuyin) {
        if (zhuyin.endsWith("ˊ")) return '2';
        if (zhuyin.endsWith("ˇ")) return '3';
        if (zhuyin.endsWith("ˋ")) return '4';
        if (zhuyin.endsWith("˙")) return '0';
        return '1';
    }

    private static final Map<String, String> PINYIN_INITIALS = Map.ofEntries(
            Map.entry("b", "ㄅ"),
            Map.entry("p", "ㄆ"),
            Map.entry("m", "ㄇ"),
            Map.entry("f", "ㄈ"),
            Map.entry("d", "ㄉ"),
            Map.entry("t", "ㄊ"),
            Map.entry("n", "ㄋ"),
            Map.entry("l", "ㄌ"),
            Map.entry("g", "ㄍ"),
            Map.entry("k", "ㄎ"),
            Map.entry("h", "ㄏ"),
            Map.entry("j", "ㄐ"),
            Map.entry("q", "ㄑ"),
            Map.entry("x", "ㄒ"),
            Map.entry("zh", "ㄓ"),
            Map.entry("ch", "ㄔ"),
            Map.entry("sh", "ㄕ"),
            Map.entry("r", "ㄖ"),
            Map.entry("z", "ㄗ"),
            Map.entry("c", "ㄘ"),
            Map.entry("s", "ㄙ")
    );

    private static final Map<String, String> SPECIAL_PINYIN = Map.ofEntries(
            Map.entry("zhi", "ㄓ"),
            Map.entry("chi", "ㄔ"),
            Map.entry("shi", "ㄕ"),
            Map.entry("ri", "ㄖ"),
            Map.entry("zi", "ㄗ"),
            Map.entry("ci", "ㄘ"),
            Map.entry("si", "ㄙ")
    );

    private static final Map<String, String> PINYIN_FINALS = Map.ofEntries(
            // Ordinary finals after initials
            Map.entry("i", "ㄧ"),
            Map.entry("u", "ㄨ"),
            Map.entry("v", "ㄩ"),

            Map.entry("a", "ㄚ"),
            Map.entry("o", "ㄛ"),
            Map.entry("e", "ㄜ"),
            Map.entry("ai", "ㄞ"),
            Map.entry("ei", "ㄟ"),
            Map.entry("ao", "ㄠ"),
            Map.entry("ou", "ㄡ"),
            Map.entry("an", "ㄢ"),
            Map.entry("en", "ㄣ"),
            Map.entry("ang", "ㄤ"),
            Map.entry("eng", "ㄥ"),
            Map.entry("er", "ㄦ"),

            Map.entry("ong", "ㄨㄥ"),

            Map.entry("ia", "ㄧㄚ"),
            Map.entry("iao", "ㄧㄠ"),
            Map.entry("ie", "ㄧㄝ"),
            Map.entry("iu", "ㄧㄡ"),
            Map.entry("ian", "ㄧㄢ"),
            Map.entry("in", "ㄧㄣ"),
            Map.entry("iang", "ㄧㄤ"),
            Map.entry("ing", "ㄧㄥ"),
            Map.entry("iong", "ㄩㄥ"),

            Map.entry("ua", "ㄨㄚ"),
            Map.entry("uo", "ㄨㄛ"),
            Map.entry("uai", "ㄨㄞ"),
            Map.entry("ui", "ㄨㄟ"),
            Map.entry("uan", "ㄨㄢ"),
            Map.entry("un", "ㄨㄣ"),
            Map.entry("uang", "ㄨㄤ"),

            Map.entry("ue", "ㄩㄝ"),

            // Explicit ü spellings used internally
            Map.entry("ve", "ㄩㄝ"),
            Map.entry("van", "ㄩㄢ"),
            Map.entry("vn", "ㄩㄣ"),

            // Zero-initial spellings
            Map.entry("yi", "ㄧ"),
            Map.entry("wu", "ㄨ"),
            Map.entry("yu", "ㄩ"),

            Map.entry("ya", "ㄧㄚ"),
            Map.entry("yao", "ㄧㄠ"),
            Map.entry("ye", "ㄧㄝ"),
            Map.entry("you", "ㄧㄡ"),
            Map.entry("yan", "ㄧㄢ"),
            Map.entry("yin", "ㄧㄣ"),
            Map.entry("yang", "ㄧㄤ"),
            Map.entry("ying", "ㄧㄥ"),
            Map.entry("yong", "ㄩㄥ"),

            Map.entry("wa", "ㄨㄚ"),
            Map.entry("wo", "ㄨㄛ"),
            Map.entry("wai", "ㄨㄞ"),
            Map.entry("wei", "ㄨㄟ"),
            Map.entry("wan", "ㄨㄢ"),
            Map.entry("wen", "ㄨㄣ"),
            Map.entry("wang", "ㄨㄤ"),
            Map.entry("weng", "ㄨㄥ"),

            Map.entry("yue", "ㄩㄝ"),
            Map.entry("yuan", "ㄩㄢ"),
            Map.entry("yun", "ㄩㄣ")
    );

    private static final Map<Character, String> TONE_SYMBOLS = Map.of(
            '1', "",
            '2', "ˊ",
            '3', "ˇ",
            '4', "ˋ",
            '0', "˙"
    );

    private String pinyinToZhuyin(String syllable) {
        if (syllable == null || syllable.length() < 2) {
            return "";
        }

        String input = syllable.toLowerCase(Locale.ROOT);

        String toneSymbol = "";
        String body = input;

        if (isTone(input.charAt(input.length() - 1))) {
            char tone = input.charAt(input.length() - 1);
            toneSymbol = TONE_SYMBOLS.get(tone);
            body = input.substring(0, input.length() - 1);
        }

        if (body.isEmpty()) {
            return "";
        }

        // zhi, chi, shi, ri, zi, ci, si
        String special = SPECIAL_PINYIN.get(body);

        if (special != null) {
            return special + toneSymbol;
        }

        // Zero-initial or vowel-only syllables: yi, wo, yuan, er, a, etc.
        String wholeSyllable = PINYIN_FINALS.get(body);

        if (wholeSyllable != null) {
            return wholeSyllable + toneSymbol;
        }

        String initialPinyin = "";
        String initialZhuyin = "";

        // Longest initial first so zh/ch/sh beat z/c/s.
        for (int length = Math.min(2, body.length()); length >= 1; --length) {

            String possibleInitial = body.substring(0, length);

            String convertedInitial = PINYIN_INITIALS.get(possibleInitial);

            if (convertedInitial != null) {
                initialPinyin = possibleInitial;
                initialZhuyin = convertedInitial;
                body = body.substring(length);
                break;
            }
        }

        // A consonant initial is required at this point because
        // vowel-only forms were handled above.
        if (initialZhuyin.isEmpty() || body.isEmpty()) {
            return "";
        }

        if (isJqxy(initialPinyin)) {
            if (body.equals("uan")) {
                body = "van";
            } else if (body.equals("ue")) {
                body = "ve";
            } else if (body.equals("un")) {
                body = "vn";
            } else if (body.startsWith("u")) {
                body = "v" + body.substring(1);
            }
        }

        String finalZhuyin = PINYIN_FINALS.get(body);

        if (finalZhuyin == null) {
            return "";
        }

        return initialZhuyin + finalZhuyin + toneSymbol;
    }

    private boolean isJqxy(String initial) {
        return initial.equals("j") || initial.equals("q") || initial.equals("x") || initial.equals("y");
    }

    private List<String> splitCompletedSyllables(String rawInput) {
        String input = rawInput.toLowerCase(Locale.ROOT);

        List<String> syllables = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c >= 'a' && c <= 'z') {
                current.append(c);
                continue;
            }

            if (c == ' ') {
                if (current.length() > 0) {
                    syllables.add(current.toString());
                    current.setLength(0);
                }

                continue;
            }

            if (isTone(c)) {
                if (current.length() == 0) {
                    return new ArrayList<>();
                }

                current.append(c);
                syllables.add(current.toString());
                current.setLength(0);
                continue;
            }

            // Invalid character means the input cannot be exactly parsed.
            return new ArrayList<>();
        }

        // Deliberately ignore an unfinished trailing syllable.
        if (current.length() > 0) {
            syllables.add(current.toString());
        }

        return syllables;
    }
    private boolean isTone(char c) {
        return c == '0' || c == '1' || c == '2' || c == '3' || c == '4';
    }

    public String[][] suggest(String rawInput) {
        List<String> syllables = splitCompletedSyllables(rawInput);

        if (syllables.isEmpty()) {
            return new String[0][];
        }

        List<String[]> results = new ArrayList<>();

        // Try the longest complete prefix first.
        for (int count = syllables.size(); count >= 1; --count) {
            StringBuilder zhuyinKey = new StringBuilder();
            int rawCharsConsumed = 0;
            boolean valid = true;

            for (int i = 0; i < count; ++i) {
                String pinyinSyllable = syllables.get(i);
                String converted = pinyinToZhuyin(pinyinSyllable);

                if (converted == null || converted.isEmpty()) {
                    valid = false;
                    break;
                }

                zhuyinKey.append(converted);
                rawCharsConsumed += pinyinSyllable.length();

                if (i + 1 < count) {
                    ++rawCharsConsumed;
                }
            }

            if (!valid) {
                continue;
            }

            String key = zhuyinKey.toString();

            boolean hasTone = key.contains("ˊ") || key.contains("ˇ") || key.contains("ˋ") || key.contains("˙");
            List<String> candidates = hasTone ? dictionary.get(key) : tonelessDictionary.get(key);

            if (candidates == null) {
                continue;
            }

            for (String candidate : candidates) {
                results.add(new String[]{candidate, String.valueOf(rawCharsConsumed)});
            }
        }

        return results.toArray(new String[0][]);
    }
}