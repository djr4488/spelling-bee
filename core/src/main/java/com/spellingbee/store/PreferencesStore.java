package com.spellingbee.store;

import com.badlogic.gdx.Preferences;

/** {@link KeyValueStore} backed by libGDX Preferences (a file on desktop, SharedPreferences on Android). */
public class PreferencesStore implements KeyValueStore {
    private final Preferences prefs;

    public PreferencesStore(Preferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public String getString(String key, String fallback) {
        return prefs.getString(key, fallback);
    }

    @Override
    public void putString(String key, String value) {
        prefs.putString(key, value);
    }

    @Override
    public void flush() {
        prefs.flush();
    }
}
