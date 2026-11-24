package com.spellingbee.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** All practice words, loaded from assets/words.json. */
public class WordBank {
    private final List<WordEntry> entries;
    private final Map<String, WordEntry> byWord = new LinkedHashMap<>();

    public WordBank(List<WordEntry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        for (WordEntry e : entries) {
            byWord.put(key(e.word), e);
        }
    }

    /** Parses the JSON array in words.json. Pure Java; works without a GL context. */
    public static WordBank parse(String json) {
        Json mapper = new Json();
        mapper.setIgnoreUnknownFields(true);
        List<WordEntry> list = new ArrayList<>();
        for (JsonValue v : new JsonReader().parse(json)) {
            WordEntry e = mapper.readValue(WordEntry.class, v);
            if (e.altPronunciations == null) e.altPronunciations = new ArrayList<>();
            list.add(e);
        }
        return new WordBank(list);
    }

    public List<WordEntry> all() {
        return entries;
    }

    public WordEntry find(String word) {
        return byWord.get(key(word));
    }

    public int size() {
        return entries.size();
    }

    private static String key(String word) {
        return word.toLowerCase(Locale.ROOT);
    }
}
