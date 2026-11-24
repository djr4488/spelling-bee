package com.spellingbee.data;

import java.util.ArrayList;
import java.util.List;

/** One practice word with its generated definition, sentence and pronunciation. */
public class WordEntry {
    public String word;
    public String partOfSpeech;
    public String definition;
    public String sentence;
    /** Human-readable respelling, e.g. "NESS-uh-sair-ee". Shown after a miss. */
    public String pronunciation;
    /** Optional respelling fed to text-to-speech instead of {@link #word}. */
    public String speakAs;
    public List<String> altPronunciations = new ArrayList<>();
    public boolean homophone;
    public boolean needsReview;
    public String note;

    /** What to hand the speech engine to say this word. */
    public String spokenWord() {
        return speakAs != null && !speakAs.isEmpty() ? speakAs : word;
    }

    /** The sentence, with the word swapped for its TTS respelling if it has one. */
    public String spokenSentence() {
        if (speakAs == null || speakAs.isEmpty()) return sentence;
        return sentence.replaceAll("(?i)(?<![\\w-])" + java.util.regex.Pattern.quote(word) + "(?![\\w-])",
                java.util.regex.Matcher.quoteReplacement(speakAs));
    }

    public boolean hasAltPronunciation() {
        return altPronunciations != null && !altPronunciations.isEmpty();
    }

    @Override
    public String toString() {
        return word;
    }
}
