package com.fqhll.keyboard;

import android.content.Context;

import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ZhuyinTyper {
    private final Map<String, List<String>> dict = new HashMap<>();
    private final Map<String, List<String>> fuzzyDict = new HashMap<>();
    private final Map<String, Integer> bestWordLen = new HashMap<>();
    private final Map<Character, Map<Integer, List<String>>> indexByHeadAndLength = new HashMap<>();
    private final Set<String> keysWithSingleHanChar = new HashSet<>();

    private final Map<String, Long> charFreq = new HashMap<>();
    private long maxCharFreq = 1;

    private final Map<String, Integer> wordFreq = new HashMap<>();

    private final Map<Character, List<Character>> near2Standard = new HashMap<>();
    private final Map<Character, List<Character>> near2Eten = new HashMap<>();
    private final Map<Character, List<Character>> near3Standard = new HashMap<>();
    private final Map<Character, List<Character>> near3Eten = new HashMap<>();

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

    private final Map<Character, int[]> posStandard = buildPosMap(STANDARD_LAYOUT);
    private final Map<Character, int[]> posEten = buildPosMap(ETEN_LAYOUT);

    private static class ZhuyinParts {
        String base;
        char tone;
    }

    private static class SoundBucket {
        final String key;
        final int distance;
        final int inputLen;
        final boolean cutOnly;
        int rank;

        SoundBucket(String key, int distance, int inputLen, boolean cutOnly) {
            this.key = key;
            this.distance = distance;
            this.inputLen = inputLen;
            this.cutOnly = cutOnly;
        }
    }

    private static class Candidate {
        final String word;
        final SoundBucket bucket;
        final int localIndex;
        final int mergeRank;

        Candidate(String word, SoundBucket bucket, int localIndex) {
            this.word = word;
            this.bucket = bucket;
            this.localIndex = localIndex;
            this.mergeRank = bucket.rank + localIndex;
        }
    }

    public ZhuyinTyper(Context ctx) {
        near2Standard.putAll(buildNearMap(posStandard, 2));
        near2Eten.putAll(buildNearMap(posEten, 2));
        near3Standard.putAll(buildNearMap(posStandard, 3));
        near3Eten.putAll(buildNearMap(posEten, 3));

        File file = new File(ctx.getFilesDir(), "tsi_custom.json");
        if (file.exists()) loadJson(file);

        loadCharFreq(ctx);
        loadWordFreq(ctx);
        buildFuzzyDict();
    }

    public static int etenToStandardCode(int etenCode) {
        char etenChar = (char) etenCode;

        for (int row = 0; row < ETEN_LAYOUT.length; ++row) {
            int col = ETEN_LAYOUT[row].indexOf(etenChar);
            if (col >= 0 && col < STANDARD_LAYOUT[row].length()) {
                return STANDARD_LAYOUT[row].charAt(col);
            }
        }

        return etenCode;
    }

    private Map<Character, int[]> buildPosMap(String[] rows) {
        Map<Character, int[]> map = new HashMap<>();

        for (int r = 0; r < rows.length; ++r) {
            for (int c = 0; c < rows[r].length(); ++c) {
                char ch = rows[r].charAt(c);
                if (ch != ' ') map.put(ch, new int[]{r, c});
            }
        }

        return map;
    }

    private Map<Character, List<Character>> buildNearMap(Map<Character, int[]> posMap, int maxDist) {
        Map<Character, List<Character>> out = new HashMap<>();
        List<Character> chars = new ArrayList<>(posMap.keySet());

        for (char a : chars) {
            List<Character> near = new ArrayList<>();
            int[] pa = posMap.get(a);

            for (char b : chars) {
                int[] pb = posMap.get(b);
                int d = Math.abs(pa[0] - pb[0]) + Math.abs(pa[1] - pb[1]);
                if (d <= maxDist) near.add(b);
            }

            out.put(a, near);
        }

        return out;
    }

    private int keyDistance(char a, char b, boolean useEten) {
        Map<Character, int[]> posMap = useEten ? posEten : posStandard;
        int[] pa = posMap.get(a);
        int[] pb = posMap.get(b);

        if (pa == null || pb == null) return 999;
        return Math.abs(pa[0] - pb[0]) + Math.abs(pa[1] - pb[1]);
    }

    private List<Character> getNearbyHeads(char firstInput, boolean useEten, int maxDist) {
        if (useEten) {
            return (maxDist <= 2 ? near2Eten : near3Eten)
                    .getOrDefault(firstInput, Collections.singletonList(firstInput));
        }

        return (maxDist <= 2 ? near2Standard : near3Standard)
                .getOrDefault(firstInput, Collections.singletonList(firstInput));
    }

    private boolean isTone(char ch) {
        return ch == '˙' || ch == 'ˊ' || ch == 'ˇ' || ch == 'ˋ';
    }

    private boolean isSingleHanCharacter(String s) {
        if (s == null || s.codePointCount(0, s.length()) != 1) return false;
        return Character.UnicodeScript.of(s.codePointAt(0)) == Character.UnicodeScript.HAN;
    }

    private int realCharLength(String s) {
        return s.codePointCount(0, s.length());
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
        if (isTone(typed)) return 1;

        int d = keyDistance(typed, targetTone, useEten);
        if (d <= 1) return 1;
        if (d <= 2) return 2;
        return 3;
    }

    private boolean isSingleAdjacentSwap(String a, String b) {
        if (a.length() != b.length()) return false;

        int firstDiff = -1;
        for (int i = 0; i < a.length(); ++i) {
            if (a.charAt(i) != b.charAt(i)) {
                firstDiff = i;
                break;
            }
        }

        if (firstDiff < 0 || firstDiff == a.length() - 1) return false;

        int i = firstDiff;
        if (a.charAt(i) != b.charAt(i + 1) || a.charAt(i + 1) != b.charAt(i)) return false;

        for (int j = i + 2; j < a.length(); ++j) {
            if (a.charAt(j) != b.charAt(j)) return false;
        }

        return true;
    }

    private int fuzzyZhuyinDistanceSmart(String input, String key, boolean useEten, int threshold) {
        int best = fuzzyKeyboardDistance(input, key, useEten, threshold);

        if (input.length() == key.length() && isSingleAdjacentSwap(input, key)) {
            best = Math.min(best, 1);
        }

        ZhuyinParts kp = splitCanonicalZhuyin(key);

        if (kp.tone != 0) {
            int baseOnly = fuzzyKeyboardDistance(input, kp.base, useEten, threshold);
            if (baseOnly <= threshold - 1) best = Math.min(best, baseOnly + 1);

            if (!input.isEmpty()) {
                String inputPrefix = input.substring(0, input.length() - 1);
                char inputLast = input.charAt(input.length() - 1);
                int baseCost = fuzzyKeyboardDistance(inputPrefix, kp.base, useEten, threshold);

                if (baseCost <= threshold) {
                    int total = baseCost + toneSlotCost(inputLast, kp.tone, useEten);
                    if (total <= threshold) best = Math.min(best, total);
                }
            }
        }

        return best <= threshold ? best : threshold + 1;
    }

    private int fuzzyKeyboardDistance(String a, String b, boolean useEten, int threshold) {
        int n = a.length();
        int m = b.length();

        if (Math.abs(n - m) > 1) return threshold + 1;

        if (n == m) {
            int cost = 0;

            for (int i = 0; i < n; ++i) {
                char ca = a.charAt(i);
                char cb = b.charAt(i);
                if (ca == cb) continue;

                int d = keyDistance(ca, cb, useEten);
                cost += d <= 1 ? 1 : 2;
                if (cost > threshold) return threshold + 1;
            }

            return cost;
        }

        String longer = n > m ? a : b;
        String shorter = n > m ? b : a;

        int i = 0;
        int j = 0;
        int edits = 0;

        while (i < longer.length() && j < shorter.length()) {
            if (longer.charAt(i) == shorter.charAt(j)) {
                ++i;
                ++j;
                continue;
            }

            edits += 2;
            ++i;
            if (edits > threshold) return threshold + 1;
        }

        edits += longer.length() - i;
        return edits;
    }

    private void loadJson(File file) {
        try {
            String jsonString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonString);
            Iterator<String> keys = root.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = root.getJSONArray(key);

                List<String> values = new ArrayList<>();
                int best = 0;
                boolean hasSingleHan = false;

                for (int i = 0; i < arr.length(); ++i) {
                    String word = arr.getString(i);
                    values.add(word);

                    int len = realCharLength(word);
                    best = Math.max(best, len);
                    if (isSingleHanCharacter(word)) hasSingleHan = true;
                }

                values.sort(Comparator.<String>comparingInt(this::realCharLength).reversed());

                dict.put(key, values);
                bestWordLen.put(key, best);
                if (hasSingleHan) keysWithSingleHanChar.add(key);

                if (!key.isEmpty()) {
                    char first = key.charAt(0);
                    indexByHeadAndLength
                            .computeIfAbsent(first, k -> new HashMap<>())
                            .computeIfAbsent(key.length(), k -> new ArrayList<>())
                            .add(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCharFreq(Context ctx) {
        try (
                InputStream in = ctx.getAssets().open("taiwan_char_freq.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
        ) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject root = new JSONObject(sb.toString());
            Iterator<String> it = root.keys();

            while (it.hasNext()) {
                String ch = it.next();
                long freq = root.getLong(ch);
                charFreq.put(ch, freq);
                maxCharFreq = Math.max(maxCharFreq, freq);
            }
        } catch (Exception ignored) {
        }
    }

    private void loadWordFreq(Context ctx) {
        try (
                InputStream in = ctx.getAssets().open("taiwan_word_freq.json");
                InputStreamReader inputReader = new InputStreamReader(in, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(inputReader)
        ) {
            reader.beginObject();

            while (reader.hasNext()) {
                String word = reader.nextName();
                int freq = reader.nextInt();
                wordFreq.put(word, freq);
            }

            reader.endObject();
        } catch (Exception e) {

        }
    }

    private void buildFuzzyDict() {
        for (Map.Entry<String, List<String>> entry : dict.entrySet()) {
            List<String> ranked = new ArrayList<>(entry.getValue());

            ranked.sort((a, b) -> {
                int lenA = realCharLength(a);
                int lenB = realCharLength(b);

                if (lenA != lenB) return Integer.compare(lenB, lenA);

                int freqA = wordFreq.getOrDefault(a, 0);
                int freqB = wordFreq.getOrDefault(b, 0);
                return Integer.compare(freqB, freqA);
            });

            fuzzyDict.put(entry.getKey(), ranked);
        }
    }

    private int leftWeightedExactMatches(String a, String b) {
        int n = Math.min(a.length(), b.length());
        int score = 0;

        for (int i = 0; i < n; ++i) {
            if (a.charAt(i) == b.charAt(i)) score += n - i;
        }

        return score;
    }

    private int betterConsumeLen(String key, int oldLen, int newLen) {
        int target = key.length();
        int oldDiff = Math.abs(oldLen - target);
        int newDiff = Math.abs(newLen - target);

        if (newDiff != oldDiff) return newDiff < oldDiff ? newLen : oldLen;
        return Math.min(oldLen, newLen);
    }

    private void collectHits(
            String fuzzyTarget,
            int rawLen,
            boolean useEten,
            int threshold,
            int firstKeyLimit,
            Map<String, Integer> allHits,
            Map<String, Integer> hitInputLength
    ) {
        if (fuzzyTarget == null || fuzzyTarget.isEmpty()) return;

        char firstInput = fuzzyTarget.charAt(0);
        List<Character> heads = getNearbyHeads(firstInput, useEten, firstKeyLimit);

        int targetLength = fuzzyTarget.length();
        int minLength = Math.max(1, targetLength - 1);
        int maxLength = targetLength + 1;

        for (char head : heads) {
            Map<Integer, List<String>> lengthBuckets = indexByHeadAndLength.get(head);
            if (lengthBuckets == null) continue;

            for (int candidateLength = minLength; candidateLength <= maxLength; ++candidateLength) {
                List<String> keys = lengthBuckets.get(candidateLength);
                if (keys == null) continue;

                for (String key : keys) {
                    int dist = fuzzyZhuyinDistanceSmart(fuzzyTarget, key, useEten, threshold);
                    if (dist <= 0 || dist > threshold) continue;

                    Integer oldDist = allHits.get(key);

                    if (oldDist == null || dist < oldDist) {
                        allHits.put(key, dist);
                        hitInputLength.put(key, rawLen);
                    } else if (dist == oldDist) {
                        int oldLen = hitInputLength.getOrDefault(key, rawLen);
                        hitInputLength.put(key, betterConsumeLen(key, oldLen, rawLen));
                    }
                }
            }
        }
    }

    private boolean allSubstitutionsAdjacent(String input, String key, boolean useEten) {
        if (input.length() != key.length()) return false;

        for (int i = 0; i < input.length(); ++i) {
            char a = input.charAt(i);
            char b = key.charAt(i);

            if (a == b) continue;
            if (keyDistance(a, b, useEten) > 1) return false;
        }

        return true;
    }

    private void collectCutSingleHanHits(String fuzzyTarget, int rawLen, boolean useEten, int threshold, int firstKeyLimit, Map<String, Integer> cutHits, Map<String, Integer> cutInputLength) {
        if (fuzzyTarget == null || fuzzyTarget.isEmpty()) return;

        char firstInput = fuzzyTarget.charAt(0);
        List<Character> heads = getNearbyHeads(firstInput, useEten, firstKeyLimit);

        int targetLength = fuzzyTarget.length();
        int minLength = Math.max(1, targetLength - 1);
        int maxLength = targetLength + 1;

        for (char head : heads) {
            Map<Integer, List<String>> lengthBuckets = indexByHeadAndLength.get(head);
            if (lengthBuckets == null) continue;

            for (int candidateLength = minLength; candidateLength <= maxLength; ++candidateLength) {
                List<String> keys = lengthBuckets.get(candidateLength);
                if (keys == null) continue;

                for (String key : keys) {
                    if (!keysWithSingleHanChar.contains(key)) continue;

                    int matchScore = leftWeightedExactMatches(fuzzyTarget, key);
                    int dist = fuzzyZhuyinDistanceSmart(fuzzyTarget, key, useEten, threshold);

                    if (dist <= 0 || dist > threshold) continue;

                    boolean hasEnoughExactOverlap = matchScore >= Math.max(1, fuzzyTarget.length());
                    boolean plausibleTwoKeyTypo = fuzzyTarget.length() == 2 && key.length() == 2 && dist == 2 && allSubstitutionsAdjacent(fuzzyTarget, key, useEten);

                    if (!hasEnoughExactOverlap && !plausibleTwoKeyTypo) continue;
                    if (dist <= 0 || dist > threshold) continue;

                    Integer oldDist = cutHits.get(key);

                    if (oldDist == null || dist < oldDist) {
                        cutHits.put(key, dist);
                        cutInputLength.put(key, rawLen);
                    } else if (dist == oldDist) {
                        int oldLen = cutInputLength.getOrDefault(key, rawLen);
                        cutInputLength.put(key, Math.max(oldLen, rawLen));
                    }
                }
            }
        }
    }

    private Comparator<SoundBucket> soundBucketComparator() {
        return (a, b) -> {
            int effectiveA = a.distance + (a.cutOnly ? 1 : 0);
            int effectiveB = b.distance + (b.cutOnly ? 1 : 0);

            if (a.inputLen != b.inputLen)
                return Integer.compare(b.inputLen, a.inputLen);

            if (effectiveA != effectiveB)
                return Integer.compare(effectiveA, effectiveB);

            int lenA = bestWordLen.getOrDefault(a.key, 0);
            int lenB = bestWordLen.getOrDefault(b.key, 0);
            if (lenA != lenB)
                return Integer.compare(lenB, lenA);

            return Integer.compare(b.key.length(), a.key.length());
        };
    }

    private double charScore(Candidate candidate) {
        Long freq = charFreq.get(candidate.word);
        double freqScore = 0.0;

        if (freq != null && maxCharFreq > 1) {
            freqScore = Math.log1p(freq) / Math.log1p(maxCharFreq);
        }

        int effectiveDistance = candidate.bucket.distance + (candidate.bucket.cutOnly ? 1 : 0);
        return freqScore - 0.25 * effectiveDistance;
    }

    private List<Candidate> interleaveCandidates(List<SoundBucket> buckets, Set<String> alreadyAdded) {
        List<Candidate> flattened = new ArrayList<>();

        for (SoundBucket bucket : buckets) {
            List<String> words = fuzzyDict.getOrDefault(bucket.key, Collections.emptyList());

            for (int localIndex = 0; localIndex < words.size(); ++localIndex) {
                String word = words.get(localIndex);
                if (bucket.cutOnly && !isSingleHanCharacter(word)) continue;
                if (alreadyAdded.contains(word)) continue;

                flattened.add(new Candidate(word, bucket, localIndex));
            }
        }

        flattened.sort((a, b) -> {
            if (a.mergeRank != b.mergeRank) return Integer.compare(a.mergeRank, b.mergeRank);
            if (a.bucket.rank != b.bucket.rank) return Integer.compare(a.bucket.rank, b.bucket.rank);
            return Integer.compare(a.localIndex, b.localIndex);
        });

        List<Integer> charSlots = new ArrayList<>();
        List<Candidate> chars = new ArrayList<>();

        for (int i = 0; i < flattened.size(); ++i) {
            Candidate candidate = flattened.get(i);
            if (!isSingleHanCharacter(candidate.word)) continue;

            charSlots.add(i);
            chars.add(candidate);
        }

        chars.sort((a, b) -> {
            int scoreCmp = Double.compare(charScore(b), charScore(a));
            if (scoreCmp != 0) return scoreCmp;

            long freqA = charFreq.getOrDefault(a.word, 0L);
            long freqB = charFreq.getOrDefault(b.word, 0L);
            if (freqA != freqB) return Long.compare(freqB, freqA);

            if (a.bucket.distance != b.bucket.distance) {
                return Integer.compare(a.bucket.distance, b.bucket.distance);
            }

            if (a.mergeRank != b.mergeRank) return Integer.compare(a.mergeRank, b.mergeRank);
            return Integer.compare(a.localIndex, b.localIndex);
        });

        for (int i = 0; i < charSlots.size(); ++i) {
            flattened.set(charSlots.get(i), chars.get(i));
        }

        return flattened;
    }

    public String[][] suggest(String[] zhuyinInput, boolean useEten) {
        if (zhuyinInput == null || zhuyinInput.length == 0) return new String[0][];

        String inputStr = zhuyinInput[0];

        if (inputStr.length() >= 4 && inputStr.length() % 2 == 0) {
            boolean allHa = true;

            for (int i = 0; i < inputStr.length(); i += 2) {
                if (inputStr.charAt(i) != 'ㄏ' || inputStr.charAt(i + 1) != 'ㄚ') {
                    allHa = false;
                    break;
                }
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
            boolean allA = true;
            boolean allU = true;
            boolean allH = true;

            for (int i = 0; i < inputStr.length(); ++i) {
                char c = inputStr.charAt(i);
                if (c != 'ㄚ') allA = false;
                if (c != 'ㄨ') allU = false;
                if (c != 'ㄏ') allH = false;
                if (!allA && !allU && !allH) break;
            }

            if (allA) return new String[][]{{"啊".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
            if (allH) return new String[][]{{"哈".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
            if (allU) return new String[][]{{"嗚".repeat(inputStr.length()), String.valueOf(inputStr.length())}};
        }

        int end = Math.min(7, zhuyinInput.length);
        List<String> candidates = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < end; ++i) {
            builder.append(zhuyinInput[i].replace(" ", ""));
            candidates.add(builder.toString());
        }

        String fullInput = candidates.get(candidates.size() - 1);
        Map<String, Integer> allHits = new HashMap<>();
        Map<String, Integer> hitInputLength = new HashMap<>();

        for (int i = candidates.size() - 1; i >= 0; --i) {
            String candidate = candidates.get(i);
            if (!dict.containsKey(candidate)) continue;

            int rawLen = 0;
            for (int j = 0; j <= i; ++j) rawLen += zhuyinInput[j].length();

            allHits.putIfAbsent(candidate, 0);
            hitInputLength.putIfAbsent(candidate, rawLen);
        }

        int threshold = 2;
        int[] prefixRawLens = new int[end];
        int runningRawLen = 0;

        for (int i = 0; i < end; ++i) {
            runningRawLen += zhuyinInput[i].length();
            prefixRawLens[i] = runningRawLen;
        }

        for (int k = Math.min(4, zhuyinInput.length); k >= 1; --k) {
            collectHits(candidates.get(k - 1), prefixRawLens[k - 1], useEten, threshold, 2, allHits, hitInputLength);
        }

        if (allHits.size() < 4) {
            int fallbackThreshold = 3;

            for (int k = 1; k <= 4 && k <= zhuyinInput.length; ++k) {
                collectHits(
                        candidates.get(k - 1),
                        prefixRawLens[k - 1],
                        useEten,
                        fallbackThreshold,
                        3,
                        allHits,
                        hitInputLength
                );
            }
        }

        Map<String, Integer> cutHits = new HashMap<>();
        Map<String, Integer> cutInputLength = new HashMap<>();

        for (int cut = Math.min(4, fullInput.length()); cut >= 1; --cut) {
            String fuzzyTarget = fullInput.substring(0, cut);

            collectCutSingleHanHits(
                    fuzzyTarget,
                    fuzzyTarget.length(),
                    useEten,
                    threshold,
                    2,
                    cutHits,
                    cutInputLength
            );
        }

        Set<String> alreadyAdded = new HashSet<>();
        List<String[]> results = new ArrayList<>();
        final int maxResults = 500;

        if (dict.containsKey(fullInput)) {
            int inputLen = hitInputLength.getOrDefault(fullInput, fullInput.length());

            for (String word : dict.get(fullInput)) {
                if (!alreadyAdded.add(word)) continue;

                results.add(new String[]{word, String.valueOf(inputLen)});
                if (results.size() >= maxResults) return results.toArray(new String[0][]);
            }
        }

        List<SoundBucket> buckets = new ArrayList<>();

        for (Map.Entry<String, Integer> e : allHits.entrySet()) {
            String key = e.getKey();
            if (key.equals(fullInput)) continue;

            buckets.add(new SoundBucket(
                    key,
                    e.getValue(),
                    hitInputLength.getOrDefault(key, key.length()),
                    false
            ));
        }

        for (Map.Entry<String, Integer> e : cutHits.entrySet()) {
            String key = e.getKey();
            if (allHits.containsKey(key)) continue;

            buckets.add(new SoundBucket(
                    key,
                    e.getValue(),
                    cutInputLength.getOrDefault(key, key.length()),
                    true
            ));
        }

        buckets.sort(soundBucketComparator());

        for (int i = 0; i < buckets.size(); ++i) {
            buckets.get(i).rank = i;
        }

        int fuzzyBucketsUsed = 0;
        final int maxFuzzyBuckets = 40;
        List<SoundBucket> allowedBuckets = new ArrayList<>();

        for (SoundBucket bucket : buckets) {
            boolean fuzzy = bucket.distance >= 2;
            if (fuzzy && fuzzyBucketsUsed >= maxFuzzyBuckets) continue;

            allowedBuckets.add(bucket);
            if (fuzzy) ++fuzzyBucketsUsed;
        }

        for (Candidate candidate : interleaveCandidates(allowedBuckets, alreadyAdded)) {
            if (!alreadyAdded.add(candidate.word)) continue;

            results.add(new String[]{candidate.word, String.valueOf(candidate.bucket.inputLen)});
            if (results.size() >= maxResults) break;
        }

        return results.toArray(new String[0][]);
    }
}