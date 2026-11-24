package com.spellingbee.quiz;

import com.spellingbee.data.WordEntry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizSessionTest {
    private static WordEntry entry(String w) {
        WordEntry e = new WordEntry();
        e.word = w;
        return e;
    }

    @Test
    void walksThroughWordsAndScores() {
        QuizSession s = new QuizSession(QuizSession.Mode.PRACTICE, Arrays.asList(entry("kelp"), entry("whale")));
        assertEquals(1, s.number());
        assertTrue(s.answer(" KELP "));
        s.advance();
        assertFalse(s.answer("wale"));
        s.advance();
        assertTrue(s.isFinished());
        assertEquals(1, s.correctCount());
        assertEquals("whale", s.missed().get(0).word);
    }

    @Test
    void timedSessionLetsTheCurrentWordFinishWhenTimeRunsOut() {
        QuizSession s = new QuizSession(QuizSession.Mode.TIMED,
                Arrays.asList(entry("kelp"), entry("whale"), entry("rod")), 60);
        assertTrue(s.isTimed());
        s.tick(30);
        assertEquals(30, s.secondsLeft());
        s.answer("kelp");
        s.advance();
        s.tick(45); // time runs out while "whale" is being typed
        assertTrue(s.isTimeUp());
        assertEquals(0, s.secondsLeft());
        assertFalse(s.isFinished(), "the word in progress can still be answered");
        s.answer("wale");
        s.advance();
        assertTrue(s.isFinished());
        assertEquals(2, s.answeredCount());
        assertEquals(1, s.correctCount());
        assertEquals(60, s.elapsedSeconds(), 0.001, "time stops at the limit");
    }

    @Test
    void untimedSessionIgnoresTheClock() {
        QuizSession s = new QuizSession(QuizSession.Mode.PRACTICE, Arrays.asList(entry("kelp"), entry("whale")));
        s.tick(10_000);
        assertFalse(s.isTimed());
        assertFalse(s.isTimeUp());
        s.answer("kelp");
        s.advance();
        assertFalse(s.isFinished());
    }
}
