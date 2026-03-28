package com.fqhll.keyboard;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ZhuyinTyper {
    private final Map<String, List<String>> dict = new HashMap<>();
    private final Map<Character, List<String>> index = new HashMap<>();

    // Row strings: one string per row of keys
    private static final String[] ETEN_LAYOUT = {
            "˙ˊˇˋㄝㄢㄣㄤㄥㄟ",
            "ㄑㄡㄜㄖㄊㄞㄨㄧㄆ",
            "ㄚㄙㄉㄈㄍㄏㄐㄎㄌㄠ",
            "ㄗㄒㄘㄩㄅㄋㄇㄓㄔㄕ",
            "       ㄦ"
    };

    private static final String[] STANDARD_LAYOUT = {
            "ㄅㄉˇˋㄓˊ˙ㄚㄞㄢ",
            "ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ",
            "ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ",
            "ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ",
            "       ㄦ"
    };

    private Map<Character,int[]> buildPosMap(String[] rows) {
        Map<Character,int[]> map = new HashMap<>();
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == ' ') {
                    continue;
                }
                map.put(ch, new int[]{r, c});
            }
        }
        return map;
    }

    private final Map<Character,int[]> posStandard = buildPosMap(STANDARD_LAYOUT);
    private final Map<Character,int[]> posEten = buildPosMap(ETEN_LAYOUT);

    private int keyDistance(char a, char b, boolean useEten) {
        Map<Character,int[]> posMap = useEten ? posEten : posStandard;
        int[] pa = posMap.getOrDefault(a, new int[]{-99, -99});
        int[] pb = posMap.getOrDefault(b, new int[]{-99, -99});
        return Math.abs(pa[0] - pb[0]) + Math.abs(pa[1] - pb[1]);
    }

    private boolean isTone(char ch) {
        return ch == '˙' || ch == 'ˊ' || ch == 'ˇ' || ch == 'ˋ';
    }

    static class ZhuyinParts {
        String base;
        char tone; // 0 if none
    }

    private ZhuyinParts splitCanonicalZhuyin(String s) {
        ZhuyinParts p = new ZhuyinParts();
        if (s == null || s.isEmpty()) {
            p.base = "";
            p.tone = 0;
            return p;
        }

        char last = s.charAt(s.length() - 1);
        if (isTone(last)) {
            p.base = s.substring(0, s.length() - 1);
            p.tone = last;
        } else {
            p.base = s;
            p.tone = 0;
        }
        return p;
    }

    private int toneSlotCost(char typed, char targetTone, boolean useEten) {
        if (typed == targetTone) return 0;

        // typed another tone
        if (isTone(typed)) return 1;

        // typed a non-tone near the tone key
        int d = keyDistance(typed, targetTone, useEten);
        if (d <= 1) return 1;
        if (d <= 2) return 2;
        return 3;
    }

    private boolean isSingleAdjacentSwap(String a, String b) {
        if (a.length() != b.length()) return false;

        int firstDiff = -1;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff == -1 || firstDiff == a.length() - 1) return false;

        int i = firstDiff;
        if (a.charAt(i) != b.charAt(i + 1)) return false;
        if (a.charAt(i + 1) != b.charAt(i)) return false;

        for (int j = i + 2; j < a.length(); j++) {
            if (a.charAt(j) != b.charAt(j)) return false;
        }

        return true;
    }

    private int fuzzyZhuyinDistanceSmart(String input, String key, boolean useEten, int threshold) {
        int best = fuzzyKeyboardDistance(input, key, useEten, threshold);

        // Cheap adjacent transposition bonus, e.g. ㄧㄅ <-> ㄅㄧ
        if (input.length() == key.length() && isSingleAdjacentSwap(input, key)) {
            best = Math.min(best, 1);
        }

        ZhuyinParts kp = splitCanonicalZhuyin(key);

        // Only do tone-aware handling if canonical dictionary key ends with a tone
        if (kp.tone != 0) {
            // Case A: input omitted the tone entirely
            int baseOnly = fuzzyKeyboardDistance(input, kp.base, useEten, threshold);
            if (baseOnly <= threshold - 1) {
                best = Math.min(best, baseOnly + 1);
            }

            // Case B: input has something in the tone slot
            if (!input.isEmpty()) {
                String inputPrefix = input.substring(0, input.length() - 1);
                char inputLast = input.charAt(input.length() - 1);

                int baseCost = fuzzyKeyboardDistance(inputPrefix, kp.base, useEten, threshold);
                if (baseCost <= threshold) {
                    int total = baseCost + toneSlotCost(inputLast, kp.tone, useEten);
                    if (total <= threshold) {
                        best = Math.min(best, total);
                    }
                }
            }
        }

        return best <= threshold ? best : threshold + 1;
    }

    private int fuzzyKeyboardDistance(String a, String b, boolean useEten, int threshold) {
        int n = a.length(), m = b.length();

        // Bail fast if lengths too different
        if (Math.abs(n - m) > 1) {
            return threshold + 1;
        }

        // Case 1: same length -> substitution check
        if (n == m) {
            int cost = 0;
            for (int i = 0; i < n; ++i) {
                char ca = a.charAt(i), cb = b.charAt(i);
                if (ca == cb) {
                    continue;
                }
                int d = keyDistance(ca, cb, useEten);
                cost += (d <= 1 ? 1 : 2);
                if (cost > threshold) {
                    return threshold + 1;
                }
            }
            return cost;
        }

        // Case 2: length differs by 1 -> insertion/deletion
        String longer = n > m ? a : b;
        String shorter = n > m ? b : a;

        int i = 0, j = 0, edits = 0;
        while (i < longer.length() && j < shorter.length()) {
            if (longer.charAt(i) == shorter.charAt(j)) {
                i++; j++;
            } else {
                edits += 2;
                i++;
                if (edits > threshold) {
                    return threshold + 1;
                }
            }
        }
        edits += (longer.length() - i); // leftovers
        return edits;
    }

    private int bestWordLengthForKey(String key) {
        int best = 0;
        for (String word : dict.getOrDefault(key, Collections.emptyList())) {
            best = Math.max(best, word.length());
        }
        return best;
    }

    private int realCharLength(String s) {
        return s.codePointCount(0, s.length());
    }

    public ZhuyinTyper(Context ctx) {
        // Load the JSON file from internal storage
        File file = new File(ctx.getFilesDir(), "tsi_custom.json");
        if (file.exists()) {
            loadJson(file);
        }
    }

    private void loadJson(File file) {
        try {
            String jsonString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonString);

            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next(); // e.g. "ㄓㄨˋ"
                JSONArray arr = root.getJSONArray(key);

                List<String> values = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    values.add(arr.getString(i)); // e.g. ["住", "注", "柱"]
                }

                dict.put(key, values);

                if (!key.isEmpty()) {
                    char first = key.charAt(0);
                    index.computeIfAbsent(first, k -> new ArrayList<>()).add(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[][] suggest(String[] zhuyinInput, boolean useEten) {
        if (zhuyinInput == null || zhuyinInput.length == 0) return new String[0][];

        // haha special case
        String inputStr = zhuyinInput[0];
        if (inputStr.length() >= 4 && inputStr.length() % 2 == 0) {
            boolean allHa = true;
            for (int i = 0; i < inputStr.length(); i += 2) {
                if (inputStr.charAt(i) != 'ㄏ' || inputStr.charAt(i + 1) != 'ㄚ') { allHa = false; break; }
            }
            if (allHa) {
                int count = inputStr.length() / 2;
                return new String[][]{{"哈".repeat(count), String.valueOf(inputStr.length())}};
            }
        }

        if (inputStr.equals("ㄏㄏ")) {
            return new String[][]{{"哈哈", String.valueOf(inputStr.length())}};
        }

        if (inputStr.length() >= 3) {
            // aaa/uuu/hhh special case
            boolean allAaa = true;
            boolean allUuu = true;
            boolean allHhh = true;

            for (int i = 0; i < inputStr.length(); ++i) {
                if (inputStr.charAt(i) != 'ㄚ') {
                    allAaa = false;
                }

                if (inputStr.charAt(i) != 'ㄨ') {
                    allUuu = false;
                }

                if (inputStr.charAt(i) != 'ㄏ') {
                    allHhh = false;
                }

                if (!allAaa && !allUuu && !allHhh) {
                    break;
                }
            }

            if (allAaa) {
                return new String[][]{{"啊".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
            } else if (allHhh) {
                return new String[][]{{"哈".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
            } else if (allUuu) {
                return new String[][]{{"嗚".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
            }
        }

        Map<String, Integer> allHits = new HashMap<>();
        Map<String, Integer> hitInputLength = new HashMap<>(); // track input chars consumed per key

        // Exact matches
        List<String> candidates = new ArrayList<>();
        StringBuilder candBuilder = new StringBuilder();
        for (String part : zhuyinInput) {
            candBuilder.append(part.replace(" ", ""));
            candidates.add(candBuilder.toString());
        }
        for (int i = candidates.size() - 1; i >= 0; --i) {
            String candidate = candidates.get(i);
            if (dict.containsKey(candidate)) {
                allHits.putIfAbsent(candidate, 0);
                int rawLen = 0;
                for (int j = 0; j <= i; ++j) rawLen += zhuyinInput[j].length();
                hitInputLength.putIfAbsent(candidate, rawLen); // exact: consume candidate.length()
            }
        }

        // Fuzzy matches
        int THRESHOLD = 2;
        for (int k = 1; k <= 4 && k <= zhuyinInput.length; ++k) {
            String fuzzyTarget = candidates.get(k - 1);
            if (fuzzyTarget.isEmpty()) continue;
            char firstInput = fuzzyTarget.charAt(0);
            for (Map.Entry<Character, List<String>> entry : index.entrySet()) {
                if (keyDistance(firstInput, entry.getKey(), useEten) > 2) continue;
                for (String key : entry.getValue()) {
//                    int dist = fuzzyKeyboardDistance(fuzzyTarget, key, useEten, THRESHOLD);
                    int dist = fuzzyZhuyinDistanceSmart(fuzzyTarget, key, useEten, THRESHOLD);
                    if (dist > 0 && dist <= THRESHOLD) {
                        if (allHits.merge(key, dist, Math::min) == dist) {
                            int rawLen = 0;
                            for (int i = 0; i < k; ++i) rawLen += zhuyinInput[i].length();
                            hitInputLength.put(key, rawLen);
                        }
                    }
                }
            }
        }

        String fullInput = candidates.get(candidates.size() - 1);
        if (fullInput.length() > 4) {
            for (int cut = 4; cut >= 2; --cut) {
                String fuzzyTarget = fullInput.substring(0, cut);
                char firstInput = fuzzyTarget.charAt(0);

                for (Map.Entry<Character, List<String>> entry : index.entrySet()) {
                    if (keyDistance(firstInput, entry.getKey(), useEten) > 2) continue;

                    for (String key : entry.getValue()) {
                        int dist = fuzzyZhuyinDistanceSmart(fuzzyTarget, key, useEten, THRESHOLD);
                        if (dist > 0 && dist <= THRESHOLD) {
                            if (allHits.merge(key, dist, Math::min) == dist) {
                                hitInputLength.put(key, Math.max(
                                        hitInputLength.getOrDefault(key, 0),
                                        key.length()
                                ));
                            }
                        }
                    }
                }
            }
        }

        // After the fuzzy loop, before sorting:
        if (allHits.size() < 4) {
            int THRESHOLD2 = 3; // higher threshold
            for (int k = 1; k <= 4 && k <= zhuyinInput.length; ++k) {
                String fuzzyTarget = candidates.get(k - 1);
                if (fuzzyTarget.isEmpty()) continue;
                char firstInput = fuzzyTarget.charAt(0);
                for (Map.Entry<Character, List<String>> entry : index.entrySet()) {
                    if (keyDistance(firstInput, entry.getKey(), useEten) > 3) continue; // slightly wider
                    for (String key : entry.getValue()) {
//                        int dist = fuzzyKeyboardDistance(fuzzyTarget, key, useEten, THRESHOLD2);
                        int dist = fuzzyZhuyinDistanceSmart(fuzzyTarget, key, useEten, THRESHOLD2);
                        if (dist > 0 && dist <= THRESHOLD2) {
                            if (allHits.merge(key, dist, Math::min) == dist) {
                                int rawLen = 0;
                                for (int i = 0; i < k; ++i) rawLen += zhuyinInput[i].length();
                                hitInputLength.put(key, rawLen);
                            }
                        }
                    }
                }
            }
        }

        List<Map.Entry<String,Integer>> sorted = new ArrayList<>(allHits.entrySet());
//        sorted.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue)
//                .thenComparingInt(a -> -a.getKey().length()));

        fullInput = candidates.get(candidates.size() - 1);

        sorted.sort((a, b) -> {
            int distA = a.getValue();
            int distB = b.getValue();

            int inputA = hitInputLength.getOrDefault(a.getKey(), 0);
            int inputB = hitInputLength.getOrDefault(b.getKey(), 0);

            // Special case:
            // if one side is exact but tiny, and the other is a very plausible fuller fuzzy match,
            // prefer the fuller one
            if (distA == 0 && distB > 0) {
                if (inputB >= inputA + 2 && distB <= 3) {
                    return 1;
                }
            }
            if (distB == 0 && distA > 0) {
                if (inputA >= inputB + 2 && distA <= 3) {
                    return -1;
                }
            }

            // Longer-span bias only for fuzzy-vs-fuzzy
            if (distA > 0 && distB > 0 && Math.abs(distA - distB) <= 1 && inputA != inputB) {
                return Integer.compare(inputB, inputA);
            }

            // Then usual typo quality
            if (distA != distB) return Integer.compare(distA, distB);

            int syllA = a.getKey().length();
            int syllB = b.getKey().length();
            if (syllA != syllB) return Integer.compare(syllB, syllA);

            int lenA = bestWordLengthForKey(a.getKey());
            int lenB = bestWordLengthForKey(b.getKey());

            int scoreA = 4 * distA - lenA;
            int scoreB = 4 * distB - lenB;

            if (scoreA != scoreB) return Integer.compare(scoreA, scoreB);

            return Integer.compare(b.getKey().length(), a.getKey().length());
        });

        Set<String> alreadyAdded = new HashSet<>();
        List<String[]> results = new ArrayList<>();

        if (dict.containsKey(fullInput)) {
            int inputLen = hitInputLength.getOrDefault(fullInput, fullInput.length());
            List<String> exactWords = new ArrayList<>(dict.get(fullInput));
            exactWords.sort(Comparator.<String>comparingInt(this::realCharLength).reversed());

            for (String word : exactWords) {
                results.add(new String[]{word, String.valueOf(inputLen)});
                alreadyAdded.add(word);
            }
        }

        int key_count = 0;
        for (Map.Entry<String,Integer> e : sorted) {
            if (e.getKey().equals(fullInput)) {
                key_count++;
                if (key_count >= 12) break;
                continue;
            }

            int inputLen = hitInputLength.getOrDefault(e.getKey(), e.getKey().length());

            List<String> words = new ArrayList<>(dict.getOrDefault(e.getKey(), Collections.emptyList()));
            words.sort(Comparator.<String>comparingInt(this::realCharLength).reversed());

            for (String word : words) {
                if (alreadyAdded.add(word)) {
                    results.add(new String[]{word, String.valueOf(inputLen)});
                }
            }

            if (results.size() >= 70) {
                break;
            }
        }
        return results.toArray(new String[0][]);
    }
}