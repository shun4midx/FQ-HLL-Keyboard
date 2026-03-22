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
            StringBuilder target = new StringBuilder();
            for (int i = 0; i < k; ++i) target.append(zhuyinInput[i].replace(" ", ""));
            String fuzzyTarget = target.toString();
            if (fuzzyTarget.isEmpty()) continue;
            char firstInput = fuzzyTarget.charAt(0);
            for (Map.Entry<Character, List<String>> entry : index.entrySet()) {
                if (keyDistance(firstInput, entry.getKey(), useEten) > 1) continue;
                for (String key : entry.getValue()) {
                    int dist = fuzzyKeyboardDistance(fuzzyTarget, key, useEten, THRESHOLD);
                    if (dist > 0 && dist <= THRESHOLD) {
                        if (allHits.merge(key, dist, Math::min) == dist) {
                            int rawLen = 0;
                            for (int i = 0; i < k; ++i) rawLen += zhuyinInput[i].length();
                            hitInputLength.put(key, rawLen); // user typed this many chars
                        }
                    }
                }
            }
        }

        // After the fuzzy loop, before sorting:
        if (allHits.size() < 10) {
            int THRESHOLD2 = 7; // higher threshold
            for (int k = 1; k <= 4 && k <= zhuyinInput.length; ++k) {
                StringBuilder target = new StringBuilder();
                for (int i = 0; i < k; ++i) target.append(zhuyinInput[i].replace(" ", ""));
                String fuzzyTarget = target.toString();
                if (fuzzyTarget.isEmpty()) continue;
                char firstInput = fuzzyTarget.charAt(0);
                for (Map.Entry<Character, List<String>> entry : index.entrySet()) {
                    if (keyDistance(firstInput, entry.getKey(), useEten) > 2) continue; // slightly wider
                    for (String key : entry.getValue()) {
                        int dist = fuzzyKeyboardDistance(fuzzyTarget, key, useEten, THRESHOLD2);
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
        sorted.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue)
                .thenComparingInt(a -> -a.getKey().length()));

        List<String[]> results = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String,Integer> e : sorted) {
            int inputLen = hitInputLength.getOrDefault(e.getKey(), e.getKey().length());
            for (String word : dict.getOrDefault(e.getKey(), Collections.emptyList())) {
                results.add(new String[]{word, String.valueOf(inputLen)});
            }
            if (++count >= 12) break;
        }
        return results.toArray(new String[0][]);
    }
}