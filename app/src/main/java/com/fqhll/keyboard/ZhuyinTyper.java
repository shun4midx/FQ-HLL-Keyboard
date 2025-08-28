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

    private int fuzzyDistance(String a, String b, boolean useEten, int threshold) {
        int n = a.length();
        int m = b.length();

        // quick length check
        if (Math.abs(n - m) > threshold) {
            return threshold + 1;
        }

        int[][] dp = new int[Math.min(16, n + 1)][Math.min(16, m + 1)];
        for (int i = 0; i <= n; ++i) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; ++j) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= n; ++i) {
            int minInRow = Integer.MAX_VALUE;
            for (int j = 1; j <= m; ++j) {
                char ca = a.charAt(i - 1), cb = b.charAt(j - 1);
                int cost;
                if (ca == cb) {
                    cost = 0;
                } else {
                    int d = keyDistance(ca, cb, useEten);
                    cost = (d <= 1 ? 1 : 2); // neighbor = cheaper
                }

                int val = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                dp[i][j] = val;
                if (val < minInRow) {
                    minInRow = val;
                }
            }
            // if even best in this row > threshold, bail out early
            if (minInRow > threshold) {
                return threshold + 1;
            }
        }

        return dp[n][m];
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String[] suggest(String[] zhuyinInput, boolean useEten) {
        if (zhuyinInput == null || zhuyinInput.length == 0) {
            return new String[0];
        }

        StringBuilder sb = new StringBuilder();
        for (String part : zhuyinInput) {
            if (part != null) sb.append(part.replace(" ", ""));
        }
        String joined = sb.toString();

        List<String> results = new ArrayList<>();

        // 1. Try exact matches (longest prefix first)
        for (int len = joined.length(); len > 0; --len) {
            String candidate = joined.substring(0, len);
            if (dict.containsKey(candidate)) {
                results.addAll(dict.get(candidate));
            }
        }

        // 2. Fuzzy match
        if (results.size() < 15) {
            int THRESHOLD = 2;
            Map<String,Integer> fuzzyHits = new HashMap<>();

            String firstSyllable = zhuyinInput[0];

            for (String key : dict.keySet()) {
                if (key.isEmpty()) {
                    continue;
                }

                // skip quickly if starting char too far
                if (keyDistance(firstSyllable.charAt(0), key.charAt(0), useEten) > 1) {
                    continue;
                }

                int dist = fuzzyDistance(firstSyllable, key, useEten, THRESHOLD);
                if (dist <= THRESHOLD) {
                    fuzzyHits.putIfAbsent(key, dist);
                }
            }


            // Sort candidates by distance
            List<Map.Entry<String,Integer>> sorted = new ArrayList<>(fuzzyHits.entrySet());
            sorted.sort(Comparator.comparingInt(Map.Entry::getValue));

            int MAX_KEYS = 10;
            int count = 0;
            Set<String> seenWords = new HashSet<>();

            for (Map.Entry<String,Integer> e : sorted) {
                for (String word : dict.get(e.getKey())) {
                    if (seenWords.add(word)) {
                        results.add(word);
                    }
                }
                if (++count >= MAX_KEYS) {
                    break;
                }
            }
        }

        return results.toArray(new String[0]);
    }

}