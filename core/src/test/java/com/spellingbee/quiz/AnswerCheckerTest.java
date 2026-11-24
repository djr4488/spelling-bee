package com.spellingbee.quiz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerCheckerTest {
    @Test
    void ignoresCaseAndSurroundingSpace() {
        assertTrue(AnswerChecker.isCorrect("  NeCeSsArY ", "necessary"));
        assertTrue(AnswerChecker.isCorrect("halloween", "Halloween"));
    }

    @Test
    void multiWordAndHyphenatedWords() {
        assertTrue(AnswerChecker.isCorrect("a  cappella", "a cappella"));
        assertTrue(AnswerChecker.isCorrect("Cul-de-sac", "cul-de-sac"));
        assertFalse(AnswerChecker.isCorrect("culdesac", "cul-de-sac"));
        assertFalse(AnswerChecker.isCorrect("acappella", "a cappella"));
    }

    @Test
    void accentsAreIgnored() {
        assertTrue(AnswerChecker.isCorrect("piñata", "pinata"));
        assertTrue(AnswerChecker.isCorrect("eclair", "éclair"));
    }

    @Test
    void wrongOrEmptyIsNotCorrect() {
        assertFalse(AnswerChecker.isCorrect("neccessary", "necessary"));
        assertFalse(AnswerChecker.isCorrect("", "a"));
        assertFalse(AnswerChecker.isCorrect(null, "a"));
        assertFalse(AnswerChecker.isCorrect("   ", "a"));
    }
}
