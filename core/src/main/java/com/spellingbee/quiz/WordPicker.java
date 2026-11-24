package com.spellingbee.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * Chooses the words for a practice session at random, but weighted: words never
 * tried and words being missed come up more often than words already known.
 * A session never repeats a word.
 */
public final class WordPicker {
    static final double UNSEEN = 4;
    static final double MISSED_LAST_TIME = 6;
    static final double MIN = 0.25;

    private WordPicker() {}

    /**
     * How likely a word is to be picked, relative to a word spelled right once (1).
     * Never tried: 4. Missed last time: 6. Otherwise 1 plus up to 4 more for a high
     * miss rate, divided by the number of correct answers in a row, so a word fades
     * as it is learned but never drops out completely.
     */
    public static double weight(Progress.WordStats s) {
        if (s == null || s.tries == 0) return UNSEEN;
        if (s.streak == 0) return MISSED_LAST_TIME;
        double missRate = (double) s.wrong() / s.tries;
        return Math.max(MIN, (1 + 4 * missRate) / s.streak);
    }

    /** Picks {@code count} different words (0 or more than the list = all of them, in weighted order). */
    public static List<String> pick(List<String> words, Progress progress, int count, Random random) {
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(words));
        int n = count <= 0 || count > unique.size() ? unique.size() : count;
        // Weighted sampling without replacement (Efraimidis-Spirakis): each word gets the key
        // -ln(u) / weight for a random u in (0, 1]; the n smallest keys win.
        List<Object[]> keyed = new ArrayList<>(unique.size());
        for (String w : unique) {
            double u = 1.0 - random.nextDouble();
            keyed.add(new Object[] {w, -Math.log(u) / weight(progress.getStats(w))});
        }
        keyed.sort(Comparator.comparingDouble(k -> (Double) k[1]));
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add((String) keyed.get(i)[0]);
        return out;
    }

    /** A shuffled copy, for sessions such as reviewing missed words. */
    public static List<String> shuffled(List<String> words, Random random) {
        List<String> copy = new ArrayList<>(words);
        Collections.shuffle(copy, random);
        return copy;
    }
}
