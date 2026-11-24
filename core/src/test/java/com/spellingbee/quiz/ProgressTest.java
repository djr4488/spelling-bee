package com.spellingbee.quiz;

import com.spellingbee.store.MemoryStore;
import com.spellingbee.store.Settings;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressTest {
    @Test
    void countsAndMissedWordsSurviveReload() {
        MemoryStore store = new MemoryStore();
        Progress p = new Progress(store);
        p.record("whale", true);
        p.record("necessary", false);
        p.record("rhythm", false);

        Progress reloaded = new Progress(store);
        assertEquals(3, reloaded.getAttempted());
        assertEquals(1, reloaded.getCorrect());
        assertEquals(2, reloaded.getIncorrect());
        assertEquals(Arrays.asList("necessary", "rhythm"), reloaded.getMissed());
        assertTrue(store.flushes > 0, "progress is flushed to disk");
    }

    @Test
    void spellingAMissedWordCorrectlyRemovesIt() {
        Progress p = new Progress(new MemoryStore());
        p.record("kelp", false);
        p.record("kelp", true);
        assertEquals(Collections.emptyList(), p.getMissed());
    }

    @Test
    void missingTheSameWordTwiceListsItOnce() {
        Progress p = new Progress(new MemoryStore());
        p.record("a cappella", false);
        p.record("kelp", false);
        p.record("a cappella", false);
        assertEquals(Arrays.asList("kelp", "a cappella"), p.getMissed());
    }

    @Test
    void resetClearsEverything() {
        MemoryStore store = new MemoryStore();
        Progress p = new Progress(store);
        p.record("kelp", false);
        p.reset();
        Progress reloaded = new Progress(store);
        assertEquals(0, reloaded.getAttempted());
        assertTrue(reloaded.getMissed().isEmpty());
        assertEquals(0, reloaded.getSeenCount());
        assertNull(reloaded.getStats("kelp"));
    }

    @Test
    void perWordRecordsSurviveReload() {
        MemoryStore store = new MemoryStore();
        Progress p = new Progress(store);
        p.record("kelp", false);
        p.record("kelp", true);
        p.record("kelp", true);
        p.record("a cappella", false);

        Progress reloaded = new Progress(store);
        Progress.WordStats kelp = reloaded.getStats("kelp");
        assertEquals(3, kelp.tries);
        assertEquals(2, kelp.correct);
        assertEquals(1, kelp.wrong());
        assertEquals(2, kelp.streak);
        assertEquals(0, reloaded.getStats("a cappella").streak);
        assertEquals(2, reloaded.getSeenCount());
        assertNull(reloaded.getStats("whale"));
    }

    @Test
    void olderSavesKeepTheirMissedWords() {
        MemoryStore store = new MemoryStore(); // as saved before per-word records existed
        store.putString("attempted", "5");
        store.putString("correct", "3");
        store.putString("missed", "rhythm\nkelp");
        Progress p = new Progress(store);
        assertEquals(2, p.getSeenCount());
        assertEquals(0, p.getStats("rhythm").streak, "counts as missed last time");
        assertEquals(5, p.getAttempted());
    }

    @Test
    void timedRunsSurviveReloadAndOnlyTheLatestAreKept() {
        MemoryStore store = new MemoryStore();
        Progress p = new Progress(store);
        for (int i = 0; i < Progress.MAX_TIMED_RUNS + 3; i++) {
            p.recordTimedRun(new Progress.TimedRun(1000L * i, 1800, 60 + i, 50));
        }
        Progress reloaded = new Progress(store);
        assertEquals(Progress.MAX_TIMED_RUNS, reloaded.getTimedRuns().size());
        Progress.TimedRun last = reloaded.getTimedRuns().get(Progress.MAX_TIMED_RUNS - 1);
        assertEquals(60 + Progress.MAX_TIMED_RUNS + 2, last.words);
        assertEquals(1800, last.seconds);
        reloaded.reset();
        assertTrue(new Progress(store).getTimedRuns().isEmpty());
    }

    @Test
    void timedPracticeSettings() {
        MemoryStore store = new MemoryStore();
        Settings s = new Settings(store);
        assertEquals(30, s.getTimedMinutes());
        s.stepTimedMinutes(1);
        assertEquals(45, s.getTimedMinutes());
        for (int i = 0; i < 10; i++) s.stepTimedMinutes(1);
        assertEquals(60, s.getTimedMinutes(), "stops at the longest");
        s.setSessionLength(Settings.TIMED);
        Settings reloaded = new Settings(store);
        assertEquals(60, reloaded.getTimedMinutes());
        assertEquals(Settings.TIMED, reloaded.getSessionLength());
        store.putString("timedMinutes", "27");
        assertEquals(30, new Settings(store).getTimedMinutes(), "odd values snap to an offered length");
    }

    @Test
    void corruptValuesFallBackToZero() {
        MemoryStore store = new MemoryStore();
        store.putString("attempted", "lots");
        assertEquals(0, new Progress(store).getAttempted());
    }

    @Test
    void settingsPersistAndClamp() {
        MemoryStore store = new MemoryStore();
        Settings s = new Settings(store);
        assertEquals(Settings.DEFAULT_RATE, s.getSpeechRate(), 0.001);
        assertTrue(s.isSoundOn());
        assertNull(s.getVoice());
        s.setSpeechRate(9f);
        s.setSoundOn(false);
        s.setVoice("English (America)+Andy");
        s.setSessionLength(20);

        Settings reloaded = new Settings(store);
        assertEquals(Settings.MAX_RATE, reloaded.getSpeechRate(), 0.001);
        assertFalse(reloaded.isSoundOn());
        assertEquals("English (America)+Andy", reloaded.getVoice());
        assertEquals(20, reloaded.getSessionLength());
    }
}
