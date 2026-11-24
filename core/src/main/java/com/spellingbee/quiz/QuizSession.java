package com.spellingbee.quiz;

import com.spellingbee.data.WordEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One run through a list of words: tracks position, score and misses. A timed
 * session also ends when its time is up (after the word in progress).
 */
public class QuizSession {
    public enum Mode { PRACTICE, REVIEW, TIMED }

    public final Mode mode;
    private final List<WordEntry> words;
    private final List<WordEntry> missed = new ArrayList<>();
    private int index;
    private int correct;
    private int answered;
    /** Time limit in seconds; 0 = untimed. */
    private final float timeLimit;
    private float elapsed;
    /** Set when the session moves on after its time is up. */
    private boolean stopped;

    public QuizSession(Mode mode, List<WordEntry> words) {
        this(mode, words, 0);
    }

    public QuizSession(Mode mode, List<WordEntry> words, int timeLimitSeconds) {
        this.mode = mode;
        this.words = new ArrayList<>(words);
        this.timeLimit = Math.max(0, timeLimitSeconds);
    }

    public WordEntry current() {
        return index < words.size() ? words.get(index) : null;
    }

    /** Checks the answer for the current word and records the result (does not advance). */
    public boolean answer(String typed) {
        WordEntry w = current();
        boolean ok = AnswerChecker.isCorrect(typed, w.word);
        answered++;
        if (ok) correct++;
        else missed.add(w);
        return ok;
    }

    public void advance() {
        index++;
        if (isTimeUp()) stopped = true; // time ran out during the word just finished
    }

    public boolean isFinished() {
        return index >= words.size() || stopped;
    }

    /** Adds playing time. Only counted while the quiz is on screen. */
    public void tick(float seconds) {
        if (timeLimit > 0) elapsed = Math.min(timeLimit, elapsed + Math.max(0, seconds));
    }

    public boolean isTimed() { return timeLimit > 0; }

    public boolean isTimeUp() { return isTimed() && elapsed >= timeLimit; }

    /** Whole seconds left, rounded up (0 when untimed or time is up). */
    public int secondsLeft() { return isTimed() ? (int) Math.ceil(timeLimit - elapsed) : 0; }

    public float elapsedSeconds() { return elapsed; }

    /** Words answered so far (right or wrong). */
    public int answeredCount() { return answered; }

    /** 1-based number of the current word. */
    public int number() { return Math.min(index + 1, words.size()); }

    public int size() { return words.size(); }

    public int correctCount() { return correct; }

    public List<WordEntry> missed() { return Collections.unmodifiableList(missed); }
}
