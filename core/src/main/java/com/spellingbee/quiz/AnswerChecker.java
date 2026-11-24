package com.spellingbee.quiz;

import java.text.Normalizer;
import java.util.Locale;

/** Compares a typed spelling with the correct one. */
public final class AnswerChecker {
    private AnswerChecker() {
    }

    /**
     * Case-insensitive; ignores surrounding spaces, repeated inner spaces and
     * accents (so "piñata" matches "pinata"); treats curly apostrophes as straight.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().replaceAll("\\s+", " ").replace('’', '\'').replace('‘', '\'');
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ROOT);
    }

    public static boolean isCorrect(String typed, String word) {
        return !normalize(typed).isEmpty() && normalize(typed).equals(normalize(word));
    }
}
