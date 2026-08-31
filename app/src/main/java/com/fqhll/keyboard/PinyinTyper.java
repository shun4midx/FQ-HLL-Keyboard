package com.fqhll.keyboard;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PinyinTyper {
    private final Map<String, List<String>> dictionary = new HashMap<>();
    private final HashSet<String> dictionaryPrefixes = new HashSet<>();
    private final Map<String, Long> charFreq = new HashMap<>();

    private static final Map<String, String> PINYIN_INITIALS = Map.ofEntries(
            Map.entry("b", "ㄅ"), Map.entry("p", "ㄆ"), Map.entry("m", "ㄇ"), Map.entry("f", "ㄈ"),
            Map.entry("d", "ㄉ"), Map.entry("t", "ㄊ"), Map.entry("n", "ㄋ"), Map.entry("l", "ㄌ"),
            Map.entry("g", "ㄍ"), Map.entry("k", "ㄎ"), Map.entry("h", "ㄏ"),
            Map.entry("j", "ㄐ"), Map.entry("q", "ㄑ"), Map.entry("x", "ㄒ"),
            Map.entry("zh", "ㄓ"), Map.entry("ch", "ㄔ"), Map.entry("sh", "ㄕ"), Map.entry("r", "ㄖ"),
            Map.entry("z", "ㄗ"), Map.entry("c", "ㄘ"), Map.entry("s", "ㄙ")
    );

    private static final Map<String, String> SPECIAL_PINYIN = Map.ofEntries(
            Map.entry("zhi", "ㄓ"), Map.entry("chi", "ㄔ"), Map.entry("shi", "ㄕ"),
            Map.entry("ri", "ㄖ"), Map.entry("zi", "ㄗ"), Map.entry("ci", "ㄘ"), Map.entry("si", "ㄙ")
    );

    private static final Map<String, String> PINYIN_FINALS = Map.ofEntries(
            Map.entry("i", "ㄧ"), Map.entry("u", "ㄨ"), Map.entry("v", "ㄩ"),
            Map.entry("a", "ㄚ"), Map.entry("o", "ㄛ"), Map.entry("e", "ㄜ"),
            Map.entry("ai", "ㄞ"), Map.entry("ei", "ㄟ"), Map.entry("ao", "ㄠ"), Map.entry("ou", "ㄡ"),
            Map.entry("an", "ㄢ"), Map.entry("en", "ㄣ"), Map.entry("ang", "ㄤ"),
            Map.entry("eng", "ㄥ"), Map.entry("er", "ㄦ"), Map.entry("ong", "ㄨㄥ"),
            Map.entry("ia", "ㄧㄚ"), Map.entry("iao", "ㄧㄠ"), Map.entry("ie", "ㄧㄝ"),
            Map.entry("iu", "ㄧㄡ"), Map.entry("ian", "ㄧㄢ"), Map.entry("in", "ㄧㄣ"),
            Map.entry("iang", "ㄧㄤ"), Map.entry("ing", "ㄧㄥ"), Map.entry("iong", "ㄩㄥ"),
            Map.entry("ua", "ㄨㄚ"), Map.entry("uo", "ㄨㄛ"), Map.entry("uai", "ㄨㄞ"),
            Map.entry("ui", "ㄨㄟ"), Map.entry("uan", "ㄨㄢ"), Map.entry("un", "ㄨㄣ"),
            Map.entry("uang", "ㄨㄤ"), Map.entry("ue", "ㄩㄝ"),
            Map.entry("ve", "ㄩㄝ"), Map.entry("van", "ㄩㄢ"), Map.entry("vn", "ㄩㄣ"),
            Map.entry("yi", "ㄧ"), Map.entry("wu", "ㄨ"), Map.entry("yu", "ㄩ"),
            Map.entry("ya", "ㄧㄚ"), Map.entry("yao", "ㄧㄠ"), Map.entry("ye", "ㄧㄝ"),
            Map.entry("you", "ㄧㄡ"), Map.entry("yan", "ㄧㄢ"), Map.entry("yin", "ㄧㄣ"),
            Map.entry("yang", "ㄧㄤ"), Map.entry("ying", "ㄧㄥ"), Map.entry("yong", "ㄩㄥ"),
            Map.entry("wa", "ㄨㄚ"), Map.entry("wo", "ㄨㄛ"), Map.entry("wai", "ㄨㄞ"),
            Map.entry("wei", "ㄨㄟ"), Map.entry("wan", "ㄨㄢ"), Map.entry("wen", "ㄨㄣ"),
            Map.entry("wang", "ㄨㄤ"), Map.entry("weng", "ㄨㄥ"),
            Map.entry("yue", "ㄩㄝ"), Map.entry("yuan", "ㄩㄢ"), Map.entry("yun", "ㄩㄣ")
    );

    private static final Map<Character, String> TONE_SYMBOLS = Map.of(
            '1', "", '2', "ˊ", '3', "ˇ", '4', "ˋ", '0', "˙"
    );

    private static class Candidate {
        final String word;
        final int baseSlot;

        Candidate(String word, int baseSlot) {
            this.word = word;
            this.baseSlot = baseSlot;
        }
    }

    public PinyinTyper(Context context) {
        loadDictionary(context);
        loadCharFreq(context);
    }

    private void loadDictionary(Context context) {
        try (
                InputStream input = context.getAssets().open("tsi_custom.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
        ) {
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) json.append(line);

            JSONObject root = new JSONObject(json.toString());
            Iterator<String> keys = root.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = root.getJSONArray(key);
                List<String> candidates = new ArrayList<>();

                for (int i = 0; i < array.length(); ++i) {
                    candidates.add(array.getString(i));
                }

                dictionary.put(key, candidates);

                for (int i = 1; i <= key.length(); ++i) {
                    dictionaryPrefixes.add(key.substring(0, i));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void loadCharFreq(Context context) {
        try (
                InputStream input = context.getAssets().open("taiwan_char_freq.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
        ) {
            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) json.append(line);

            JSONObject root = new JSONObject(json.toString());
            Iterator<String> keys = root.keys();

            while (keys.hasNext()) {
                String character = keys.next();
                charFreq.put(character, root.getLong(character));
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isTone(char c) {
        return c == '0' || c == '1' || c == '2' || c == '3' || c == '4';
    }

    private boolean isSingleHanCharacter(String s) {
        if (s == null || s.codePointCount(0, s.length()) != 1) return false;
        return Character.UnicodeScript.of(s.codePointAt(0)) == Character.UnicodeScript.HAN;
    }

    private boolean isJqxy(String initial) {
        return initial.equals("j") || initial.equals("q") || initial.equals("x") || initial.equals("y");
    }

    private String pinyinToZhuyin(String syllable) {
        if (syllable == null || syllable.isEmpty()) return "";

        String input = syllable.toLowerCase(Locale.ROOT);
        String toneSymbol = "";
        String body = input;

        if (isTone(input.charAt(input.length() - 1))) {
            char tone = input.charAt(input.length() - 1);
            toneSymbol = TONE_SYMBOLS.get(tone);
            body = input.substring(0, input.length() - 1);
        }

        if (body.isEmpty()) return "";

        String special = SPECIAL_PINYIN.get(body);
        if (special != null) return special + toneSymbol;

        String wholeSyllable = PINYIN_FINALS.get(body);
        if (wholeSyllable != null) return wholeSyllable + toneSymbol;

        String initialPinyin = "";
        String initialZhuyin = "";

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

        if (initialZhuyin.isEmpty() || body.isEmpty()) return "";

        if (isJqxy(initialPinyin)) {
            if (body.equals("uan")) body = "van";
            else if (body.equals("ue")) body = "ve";
            else if (body.equals("un")) body = "vn";
            else if (body.startsWith("u")) body = "v" + body.substring(1);
        }

        String finalZhuyin = PINYIN_FINALS.get(body);
        if (finalZhuyin == null) return "";

        return initialZhuyin + finalZhuyin + toneSymbol;
    }

    private List<String> pinyinToPossibleZhuyin(String syllable) {
        List<String> result = new ArrayList<>();
        if (syllable == null || syllable.isEmpty()) return result;

        char last = syllable.charAt(syllable.length() - 1);

        if (isTone(last)) {
            String converted = pinyinToZhuyin(syllable);
            if (!converted.isEmpty()) result.add(converted);
            return result;
        }

        String base = pinyinToZhuyin(syllable);
        if (base.isEmpty()) return result;

        result.add(base);
        result.add(base + "ˊ");
        result.add(base + "ˇ");
        result.add(base + "ˋ");
        result.add(base + "˙");

        return result;
    }

    private List<String> generateZhuyinKeys(List<String> syllables, int count) {
        List<String> current = new ArrayList<>();
        current.add("");

        for (int i = 0; i < count; ++i) {
            List<String> variants = pinyinToPossibleZhuyin(syllables.get(i));
            if (variants.isEmpty()) return new ArrayList<>();

            List<String> next = new ArrayList<>();

            for (String prefix : current) {
                for (String variant : variants) {
                    String combined = prefix + variant;
                    if (dictionaryPrefixes.contains(combined)) next.add(combined);
                }
            }

            current = next;
            if (current.isEmpty()) break;
        }

        return current;
    }

    private List<String> splitCompletedSyllables(String rawInput) {
        String input = rawInput.toLowerCase(Locale.ROOT);
        List<String> syllables = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); ++i) {
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
                if (current.length() == 0) return new ArrayList<>();

                current.append(c);
                syllables.add(current.toString());
                current.setLength(0);
                continue;
            }

            return new ArrayList<>();
        }

        if (current.length() > 0) syllables.add(current.toString());
        return syllables;
    }

    private boolean allExplicitTone(List<String> syllables, int count) {
        for (int i = 0; i < count; ++i) {
            String syllable = syllables.get(i);
            if (syllable.isEmpty() || !isTone(syllable.charAt(syllable.length() - 1))) return false;
        }

        return true;
    }

    private int rawCharsConsumed(List<String> syllables, int count) {
        int consumed = 0;

        for (int i = 0; i < count; ++i) {
            consumed += syllables.get(i).length();
            if (i + 1 < count) ++consumed;
        }

        return consumed;
    }

    private List<Candidate> mergeToneBuckets(List<String> keys) {
        List<Candidate> merged = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        int depth = 0;

        while (true) {
            boolean addedAtDepth = false;

            for (String key : keys) {
                List<String> words = dictionary.get(key);
                if (words == null || depth >= words.size()) continue;

                addedAtDepth = true;
                String word = words.get(depth);

                if (seen.add(word)) {
                    merged.add(new Candidate(word, merged.size()));
                }
            }

            if (!addedAtDepth) break;
            ++depth;
        }

        /*
         * Pinyin is NOT fuzzy. All generated tone keys are exact possibilities.
         * We preserve the round-robin slots for words/emoji, then frequency-sort
         * only the single-Han subsequence across all generated tone buckets.
         */
        List<Integer> charSlots = new ArrayList<>();
        List<Candidate> chars = new ArrayList<>();

        for (int i = 0; i < merged.size(); ++i) {
            Candidate candidate = merged.get(i);

            if (isSingleHanCharacter(candidate.word)) {
                charSlots.add(i);
                chars.add(candidate);
            }
        }

        chars.sort((a, b) -> {
            long freqA = charFreq.getOrDefault(a.word, 0L);
            long freqB = charFreq.getOrDefault(b.word, 0L);

            if (freqA != freqB) return Long.compare(freqB, freqA);
            return Integer.compare(a.baseSlot, b.baseSlot);
        });

        for (int i = 0; i < charSlots.size(); ++i) {
            merged.set(charSlots.get(i), chars.get(i));
        }

        return merged;
    }

    public String[][] suggest(String rawInput) {
        List<String> syllables = splitCompletedSyllables(rawInput);
        if (syllables.isEmpty()) return new String[0][];

        List<String[]> results = new ArrayList<>();
        HashSet<String> globallyAdded = new HashSet<>();
        final int maxResults = 150;

        for (int count = syllables.size(); count >= 1; --count) {
            List<String> possibleKeys = generateZhuyinKeys(syllables, count);
            if (possibleKeys.isEmpty()) continue;

            int consumed = rawCharsConsumed(syllables, count);

            /*
             * Fully tone-specified full input is 100% exact: preserve its dictionary
             * bucket untouched. Toneless syllables never enter this branch.
             */
            if (count == syllables.size() && allExplicitTone(syllables, count) && possibleKeys.size() == 1) {
                List<String> exact = dictionary.get(possibleKeys.get(0));

                if (exact != null) {
                    for (String word : exact) {
                        if (!globallyAdded.add(word)) continue;

                        results.add(new String[]{word, String.valueOf(consumed)});
                        if (results.size() >= maxResults) return results.toArray(new String[0][]);
                    }
                }

                continue;
            }

            for (Candidate candidate : mergeToneBuckets(possibleKeys)) {
                if (!globallyAdded.add(candidate.word)) continue;

                results.add(new String[]{candidate.word, String.valueOf(consumed)});
                if (results.size() >= maxResults) return results.toArray(new String[0][]);
            }
        }

        return results.toArray(new String[0][]);
    }
}