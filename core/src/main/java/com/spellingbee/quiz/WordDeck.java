package com.spellingbee.quiz;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A shuffled deck of words. Draws never repeat a word until every word has
 * been drawn; then the deck is reshuffled. The remaining order can be saved
 * so the no-repeat promise holds across sessions.
 */
public class WordDeck {
    private final List<String> allWords;
    private final Random random;
    private final Deque<String> remaining = new ArrayDeque<>();

    /** @param savedState from {@link #saveState()}, or empty for a fresh shuffle */
    public WordDeck(List<String> allWords, String savedState, Random random) {
        this.allWords = new ArrayList<>(new LinkedHashSet<>(allWords));
        this.random = random;
        if (savedState != null && !savedState.isEmpty()) {
            Set<String> valid = new HashSet<>(this.allWords);
            for (String w : savedState.split("\n")) {
                if (valid.contains(w)) remaining.add(w); // words removed from the list are dropped
            }
        }
        if (remaining.isEmpty()) refill(Collections.emptySet());
    }

    /**
     * Draws {@code count} words (0 or more than the deck = every word once).
     * When the deck runs out mid-draw it is reshuffled without the words this
     * draw already returned.
     */
    public List<String> draw(int count) {
        int n = count <= 0 || count > allWords.size() ? allWords.size() : count;
        List<String> out = new ArrayList<>(n);
        Set<String> taken = new HashSet<>();
        while (out.size() < n) {
            if (remaining.isEmpty()) refill(taken);
            String w = remaining.poll();
            if (taken.add(w)) out.add(w);
        }
        return out;
    }

    public int remainingCount() {
        return remaining.size();
    }

    public String saveState() {
        return String.join("\n", remaining);
    }

    private void refill(Set<String> exclude) {
        List<String> fresh = new ArrayList<>();
        for (String w : allWords) if (!exclude.contains(w)) fresh.add(w);
        Collections.shuffle(fresh, random);
        remaining.addAll(fresh);
    }

    /** A shuffled copy, for one-off sessions such as reviewing missed words. */
    public static List<String> shuffled(List<String> words, Random random) {
        List<String> copy = new ArrayList<>(words);
        Collections.shuffle(copy, random);
        return copy;
    }
}
