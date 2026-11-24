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
        p.setDeckState("x\ny");
        p.reset();
        Progress reloaded = new Progress(store);
        assertEquals(0, reloaded.getAttempted());
        assertTrue(reloaded.getMissed().isEmpty());
        assertEquals("", reloaded.getDeckState());
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
