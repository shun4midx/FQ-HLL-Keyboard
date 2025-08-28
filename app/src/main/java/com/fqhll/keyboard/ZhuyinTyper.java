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

//    public String[] suggest(String[] zhuyinInput) {
//        String croppedZhuyinInput = zhuyinInput[0];
//        if (zhuyinInput.length != 0 && !zhuyinInput[0].isEmpty() && zhuyinInput[0].charAt(zhuyinInput[0].length() - 1) == ' ') {
//            croppedZhuyinInput = zhuyinInput[0].substring(0, zhuyinInput[0].length() - 1);
//        }
//        if (dict.containsKey(croppedZhuyinInput)) {
//            List<String> words = dict.get(croppedZhuyinInput);
//            return words.toArray(new String[0]);
//        }
//        return new String[0];
//    }

    public String[] suggest(String[] zhuyinInput) {
        if (zhuyinInput == null || zhuyinInput.length == 0) {
            return new String[0];
        }

        StringBuilder sb = new StringBuilder();
        for (String part : zhuyinInput) {
            if (part != null) sb.append(part.replace(" ", ""));
        }
        String joined = sb.toString();

        List<String> results = new ArrayList<>();
        for (int len = joined.length(); len > 0; len--) {
            String candidate = joined.substring(0, len);
            if (dict.containsKey(candidate)) {
                results.addAll(dict.get(candidate));
            }
        }

        return results.toArray(new String[0]);
    }

}