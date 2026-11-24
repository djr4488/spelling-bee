package com.spellingbee.quiz;

import com.spellingbee.data.WordEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One run through a list of words: tracks position, score and misses. */
public class QuizSession {
    public enum Mode { PRACTICE, REVIEW }

    public final Mode mode;
    private final List<WordEntry> words;
    private final List<WordEntry> missed = new ArrayList<>();
    private int index;
    private int correct;

    public QuizSession(Mode mode, List<WordEntry> words) {
        this.mode = mode;
        this.words = new ArrayList<>(words);
    }

    public WordEntry current() {
        return index < words.size() ? words.get(index) : null;
    }

    /** Checks the answer for the current word and records the result (does not advance). */
    public boolean answer(String typed) {
        WordEntry w = current();
        boolean ok = AnswerChecker.isCorrect(typed, w.word);
        if (ok) correct++;
        else missed.add(w);
        return ok;
    }

    public void advance() {
        index++;
    }

    public boolean isFinished() {
        return index >= words.size();
    }

    /** 1-based number of the current word. */
    public int number() { return Math.min(index + 1, words.size()); }

    public int size() { return words.size(); }

    public int correctCount() { return correct; }

    public List<WordEntry> missed() { return Collections.unmodifiableList(missed); }
}
