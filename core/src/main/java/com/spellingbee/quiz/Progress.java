package com.spellingbee.quiz;

import com.spellingbee.store.KeyValueStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lifetime practice record: totals, a record for every word tried, and which
 * words are still missed. Spelling a missed word correctly later takes it off
 * the missed list. Saved after every answer.
 */
public class Progress {
    /** How one word has gone so far. */
    public static final class WordStats {
        public int tries;
        public int correct;
        /** Correct answers in a row since the last miss (0 if the last answer was wrong). */
        public int streak;

        public int wrong() { return tries - correct; }
    }

    /** One finished timed practice session. */
    public static final class TimedRun {
        /** When it ended, in milliseconds since 1970. */
        public final long endedAt;
        public final int seconds;
        public final int words;
        public final int correct;

        public TimedRun(long endedAt, int seconds, int words, int correct) {
            this.endedAt = endedAt;
            this.seconds = seconds;
            this.words = words;
            this.correct = correct;
        }
    }

    /** How many timed sessions are kept. */
    static final int MAX_TIMED_RUNS = 20;

    private final KeyValueStore store;
    private int attempted;
    private int correct;
    private final Set<String> missed = new LinkedHashSet<>();
    private final Map<String, WordStats> words = new LinkedHashMap<>();
    private final List<TimedRun> timedRuns = new ArrayList<>();

    public Progress(KeyValueStore store) {
        this.store = store;
        attempted = parseInt(store.getString("attempted", "0"));
        correct = parseInt(store.getString("correct", "0"));
        for (String w : store.getString("missed", "").split("\n")) {
            if (!w.isEmpty()) missed.add(w);
        }
        for (String line : store.getString("timed", "").split("\n")) {
            String[] f = line.split("\t");
            if (f.length != 4) continue;
            try {
                timedRuns.add(new TimedRun(Long.parseLong(f[0]), parseInt(f[1]), parseInt(f[2]), parseInt(f[3])));
            } catch (NumberFormatException ignored) {
                // skip a damaged line
            }
        }
        String saved = store.getString("words", null);
        if (saved != null) {
            for (String line : saved.split("\n")) {
                String[] f = line.split("\t");
                if (f.length != 4 || f[0].isEmpty()) continue;
                WordStats s = new WordStats();
                s.tries = parseInt(f[1]);
                s.correct = Math.min(parseInt(f[2]), s.tries);
                s.streak = Math.min(parseInt(f[3]), s.correct);
                if (s.tries > 0) words.put(f[0], s);
            }
        } else {
            // Saved by a version without per-word records: all we know is what was missed.
            for (String w : missed) {
                WordStats s = new WordStats();
                s.tries = 1;
                words.put(w, s);
            }
        }
    }

    public void record(String word, boolean wasCorrect) {
        attempted++;
        WordStats s = words.computeIfAbsent(word, k -> new WordStats());
        s.tries++;
        if (wasCorrect) {
            correct++;
            s.correct++;
            s.streak++;
            missed.remove(word);
        } else {
            s.streak = 0;
            missed.remove(word); // move to the end: most recent miss last
            missed.add(word);
        }
        save();
    }

    public int getAttempted() { return attempted; }

    public int getCorrect() { return correct; }

    public int getIncorrect() { return attempted - correct; }

    public List<String> getMissed() { return new ArrayList<>(missed); }

    /** The record for {@code word}, or null if it has never been tried. */
    public WordStats getStats(String word) { return words.get(word); }

    /** How many different words have been tried. */
    public int getSeenCount() { return words.size(); }

    /** Saves a timed session's result; only the most recent {@value #MAX_TIMED_RUNS} are kept. */
    public void recordTimedRun(TimedRun run) {
        timedRuns.add(run);
        while (timedRuns.size() > MAX_TIMED_RUNS) timedRuns.remove(0);
        save();
    }

    /** Timed sessions, oldest first. */
    public List<TimedRun> getTimedRuns() { return new ArrayList<>(timedRuns); }

    public void reset() {
        attempted = 0;
        correct = 0;
        missed.clear();
        words.clear();
        timedRuns.clear();
        store.putString("deck", ""); // left over from older versions
        save();
    }

    private void save() {
        store.putString("attempted", Integer.toString(attempted));
        store.putString("correct", Integer.toString(correct));
        store.putString("missed", String.join("\n", missed));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, WordStats> e : words.entrySet()) {
            WordStats s = e.getValue();
            if (sb.length() > 0) sb.append('\n');
            sb.append(e.getKey()).append('\t').append(s.tries).append('\t')
                    .append(s.correct).append('\t').append(s.streak);
        }
        store.putString("words", sb.toString());
        StringBuilder runs = new StringBuilder();
        for (TimedRun r : timedRuns) {
            if (runs.length() > 0) runs.append('\n');
            runs.append(r.endedAt).append('\t').append(r.seconds).append('\t')
                    .append(r.words).append('\t').append(r.correct);
        }
        store.putString("timed", runs.toString());
        store.flush();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
