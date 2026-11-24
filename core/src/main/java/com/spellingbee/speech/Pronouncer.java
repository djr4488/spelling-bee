package com.spellingbee.speech;

import com.spellingbee.data.WordEntry;
import com.spellingbee.store.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Decides what the app says aloud for a word, at the user's chosen speed. */
public class Pronouncer {
    /** The word itself is said a little slower than definitions and sentences. */
    private static final float WORD_RATE_FACTOR = 0.85f;

    private final SpeechService speech;
    private final Settings settings;

    public Pronouncer(SpeechService speech, Settings settings) {
        this.speech = speech;
        this.settings = settings;
    }

    private float rate() {
        return settings.getSpeechRate();
    }

    public void word(WordEntry e) {
        speech.speak(e.spokenWord(), rate() * WORD_RATE_FACTOR);
    }

    public void definition(WordEntry e) {
        speech.speak(e.definition, rate());
    }

    /** Speaks the first (0) or second (1) example sentence; the first if there is no second. */
    public void sentence(WordEntry e, int which) {
        speech.speak(which == 1 && e.hasSentence2() ? e.spokenSentence2() : e.spokenSentence(), rate());
    }

    /** Speaks the alternate pronunciation(s), or says there are none. */
    public void alternate(WordEntry e) {
        if (!e.hasAltPronunciation()) {
            speech.speakSequence(Arrays.asList("There is no other way to say this word.", e.spokenWord()), rate());
            return;
        }
        List<String> parts = new ArrayList<>();
        parts.add("Also said as");
        parts.addAll(e.altPronunciations);
        speech.speakSequence(parts, rate() * WORD_RATE_FACTOR);
    }

    /** After a miss: the word, how it is spelled, its meaning and the sentence. */
    public void reveal(WordEntry e) {
        speech.speakSequence(Arrays.asList(
                e.spokenWord(),
                "is spelled",
                spellOut(e.word),
                e.definition,
                e.spokenSentence()), rate());
    }

    /** The word, then its letters one by one, then the word again. */
    public void spell(WordEntry e) {
        speech.speakSequence(Arrays.asList(e.spokenWord(), spellOut(e.word), e.spokenWord()), rate() * WORD_RATE_FACTOR);
    }

    public void say(String text) {
        speech.speak(text, rate());
    }

    /** "cat" becomes "C, A, T" with letter names a speech engine reads reliably. */
    public static String spellOut(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            String name;
            if (c == ' ') name = "space";
            else if (c == '-') name = "hyphen";
            else if (c == '\'') name = "apostrophe";
            else if (Character.isLetter(c)) name = letterName(Character.toLowerCase(c));
            else continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }
        return sb.toString();
    }

    // Checked against Piper/espeak pronunciation rules: "ay" and "aye" are read as
    // "eye", and "eff" as the abbreviation "E F F", so A and F use other spellings.
    private static final String[] LETTERS = {
            "A", "bee", "see", "dee", "ee", "ef", "gee", "aitch", "eye", "jay", "kay", "el", "em",
            "en", "oh", "pee", "cue", "are", "ess", "tee", "you", "vee", "double you", "ex", "why", "zee"};

    private static String letterName(char c) {
        return c >= 'a' && c <= 'z' ? LETTERS[c - 'a'] : String.valueOf(c);
    }
}
