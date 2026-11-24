package com.spellingbee.store;

/** Minimal persistent string storage, so logic can be tested without libGDX. */
public interface KeyValueStore {
    String getString(String key, String fallback);

    void putString(String key, String value);

    /** Writes pending changes to disk. */
    void flush();
}
