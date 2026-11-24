package com.spellingbee.quiz;

import com.spellingbee.store.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordPickerTest {
    private static List<String> words(int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add("w" + i);
        return out;
    }

    private static Progress.WordStats stats(int tries, int correct, int streak) {
        Progress.WordStats s = new Progress.WordStats();
        s.tries = tries;
        s.correct = correct;
        s.streak = streak;
        return s;
    }

    @Test
    void weightsFavorNewAndMissedWords() {
        assertEquals(4, WordPicker.weight(null));
        assertEquals(6, WordPicker.weight(stats(2, 1, 0)), "missed last time");
        assertEquals(1, WordPicker.weight(stats(1, 1, 1)), 1e-9, "right once");
        assertEquals(0.5, WordPicker.weight(stats(2, 2, 2)), 1e-9, "right twice in a row");
        assertEquals(3, WordPicker.weight(stats(2, 1, 1)), 1e-9, "missed, then right once");
        assertEquals(0.25, WordPicker.weight(stats(9, 9, 9)), 1e-9, "never drops below the minimum");
    }

    @Test
    void sessionHasNoRepeatsAndTheRightSize() {
        Progress p = new Progress(new MemoryStore());
        List<String> picked = WordPicker.pick(words(30), p, 10, new Random(1));
        assertEquals(10, picked.size());
        assertEquals(10, new HashSet<>(picked).size());
        assertEquals(30, WordPicker.pick(words(30), p, 0, new Random(1)).size(), "0 means all words");
        assertEquals(30, WordPicker.pick(words(30), p, 50, new Random(1)).size());
    }

    @Test
    void newAndMissedWordsComeUpMoreOftenThanKnownOnes() {
        Progress p = new Progress(new MemoryStore());
        List<String> all = words(30);
        for (int i = 0; i < 10; i++) { p.record("w" + i, true); p.record("w" + i, true); } // known, weight 0.5
        for (int i = 10; i < 20; i++) p.record("w" + i, false);                              // missed, weight 6
        // w20..w29 never tried, weight 4
        Map<String, Integer> counts = new HashMap<>();
        Random r = new Random(7);
        for (int run = 0; run < 2000; run++) {
            for (String w : WordPicker.pick(all, p, 5, r)) counts.merge(w, 1, Integer::sum);
        }
        int known = 0, missed = 0, fresh = 0;
        for (int i = 0; i < 10; i++) {
            known += counts.getOrDefault("w" + i, 0);
            missed += counts.getOrDefault("w" + (i + 10), 0);
            fresh += counts.getOrDefault("w" + (i + 20), 0);
        }
        assertTrue(missed > fresh, "missed " + missed + " > new " + fresh);
        assertTrue(fresh > 3 * known, "new " + fresh + " > 3x known " + known);
        assertTrue(known > 0, "known words still come up sometimes");
    }
}
