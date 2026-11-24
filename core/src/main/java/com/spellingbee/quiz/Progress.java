package com.spellingbee.quiz;

import com.spellingbee.store.KeyValueStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lifetime practice record: words attempted, right and wrong, and which words
 * are still missed. Spelling a missed word correctly later takes it off the list.
 * Saved after every answer.
 */
public class Progress {
    private final KeyValueStore store;
    private int attempted;
    private int correct;
    private final Set<String> missed = new LinkedHashSet<>();

    public Progress(KeyValueStore store) {
        this.store = store;
        attempted = parseInt(store.getString("attempted", "0"));
        correct = parseInt(store.getString("correct", "0"));
        for (String w : store.getString("missed", "").split("\n")) {
            if (!w.isEmpty()) missed.add(w);
        }
    }

    public void record(String word, boolean wasCorrect) {
        attempted++;
        if (wasCorrect) {
            correct++;
            missed.remove(word);
        } else {
            missed.remove(word); // move to the end: most recent miss last
            missed.add(word);
        }
        save();
    }

    public int getAttempted() { return attempted; }

    public int getCorrect() { return correct; }

    public int getIncorrect() { return attempted - correct; }

    public List<String> getMissed() { return new ArrayList<>(missed); }

    /** Words left in the saved practice deck (see {@link WordDeck}). */
    public String getDeckState() { return store.getString("deck", ""); }

    public void setDeckState(String state) {
        store.putString("deck", state);
        store.flush();
    }

    public void reset() {
        attempted = 0;
        correct = 0;
        missed.clear();
        store.putString("deck", "");
        save();
    }

    private void save() {
        store.putString("attempted", Integer.toString(attempted));
        store.putString("correct", Integer.toString(correct));
        store.putString("missed", String.join("\n", missed));
        store.flush();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
