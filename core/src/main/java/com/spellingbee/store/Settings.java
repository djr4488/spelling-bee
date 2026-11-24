package com.spellingbee.store;

/** User settings: speech rate, voice, sound effects and session length. */
public class Settings {
    public static final float MIN_RATE = 0.5f;
    public static final float MAX_RATE = 1.5f;
    public static final float DEFAULT_RATE = 0.85f;
    /** Session lengths offered; 0 means every word, {@link #TIMED} a timed session. */
    public static final int[] SESSION_LENGTHS = {10, 20, 0, -1};
    public static final int TIMED = -1;
    /** Timed practice lengths offered, in minutes. */
    public static final int[] TIMED_MINUTES = {5, 10, 15, 20, 30, 45, 60};

    private final KeyValueStore store;
    private float speechRate;
    private String voice;
    private boolean soundOn;
    private int sessionLength;
    private int timedMinutes;

    public Settings(KeyValueStore store) {
        this.store = store;
        speechRate = clamp(parseFloat(store.getString("speechRate", ""), DEFAULT_RATE));
        String v = store.getString("voice", "");
        voice = v.isEmpty() ? null : v;
        soundOn = !"false".equals(store.getString("soundOn", "true"));
        sessionLength = (int) parseFloat(store.getString("sessionLength", "10"), 10);
        if (sessionLength < TIMED) sessionLength = 10;
        timedMinutes = nearestTimed((int) parseFloat(store.getString("timedMinutes", "30"), 30));
    }

    public float getSpeechRate() { return speechRate; }

    public void setSpeechRate(float rate) {
        speechRate = clamp(Math.round(rate * 20f) / 20f);
        save();
    }

    /** Null means the platform's default voice. */
    public String getVoice() { return voice; }

    public void setVoice(String voice) {
        this.voice = voice;
        save();
    }

    public boolean isSoundOn() { return soundOn; }

    public void setSoundOn(boolean on) {
        soundOn = on;
        save();
    }

    public int getSessionLength() { return sessionLength; }

    public void setSessionLength(int length) {
        sessionLength = Math.max(TIMED, length);
        save();
    }

    public int getTimedMinutes() { return timedMinutes; }

    /** Moves to the next ({@code step} = 1) or previous (-1) offered length; stops at the ends. */
    public void stepTimedMinutes(int step) {
        int i = 0;
        while (TIMED_MINUTES[i] != timedMinutes) i++;
        i = Math.max(0, Math.min(TIMED_MINUTES.length - 1, i + step));
        timedMinutes = TIMED_MINUTES[i];
        save();
    }

    private static int nearestTimed(int minutes) {
        int best = TIMED_MINUTES[0];
        for (int m : TIMED_MINUTES) if (Math.abs(m - minutes) < Math.abs(best - minutes)) best = m;
        return best;
    }

    private void save() {
        store.putString("speechRate", Float.toString(speechRate));
        store.putString("voice", voice == null ? "" : voice);
        store.putString("soundOn", Boolean.toString(soundOn));
        store.putString("sessionLength", Integer.toString(sessionLength));
        store.putString("timedMinutes", Integer.toString(timedMinutes));
        store.flush();
    }

    private static float clamp(float r) {
        return Math.max(MIN_RATE, Math.min(MAX_RATE, r));
    }

    private static float parseFloat(String s, float fallback) {
        try {
            return s.isEmpty() ? fallback : Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
