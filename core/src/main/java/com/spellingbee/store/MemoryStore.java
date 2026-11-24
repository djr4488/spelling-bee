package com.spellingbee.store;

import java.util.HashMap;
import java.util.Map;

/** In-memory {@link KeyValueStore}, for tests. */
public class MemoryStore implements KeyValueStore {
    public final Map<String, String> values = new HashMap<>();
    public int flushes;

    @Override
    public String getString(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    @Override
    public void putString(String key, String value) {
        values.put(key, value);
    }

    @Override
    public void flush() {
        flushes++;
    }
}
