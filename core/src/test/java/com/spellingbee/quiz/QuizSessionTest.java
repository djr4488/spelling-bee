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
}
