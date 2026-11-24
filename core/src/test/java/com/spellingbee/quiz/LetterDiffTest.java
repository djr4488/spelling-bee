package com.spellingbee.quiz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterDiffTest {
    private static String typed(String a, String b) {
        return LetterDiff.describe(LetterDiff.of(a, b).typed);
    }

    private static String correct(String a, String b) {
        return LetterDiff.describe(LetterDiff.of(a, b).correct);
    }

    @Test
    void identicalHasNoHighlights() {
        assertEquals("whale", typed("Whale", "whale").toLowerCase());
        assertEquals("whale", correct("Whale", "whale"));
    }

    @Test
    void wrongLetterIsMarkedOnBothSides() {
        assertEquals("ne[s]essary", typed("nesessary", "necessary"));
        assertEquals("ne[c]essary", correct("nesessary", "necessary"));
    }

    @Test
    void missingLetterIsMarkedInCorrectSpelling() {
        assertEquals("necesary", typed("necesary", "necessary"));
        assertEquals(1, LetterDiff.of("necesary", "necessary").correct.stream().filter(m -> !m.ok).count());
    }

    @Test
    void extraLetterIsMarkedInTypedSpelling() {
        assertEquals(1, LetterDiff.of("neccessary", "necessary").typed.stream().filter(m -> !m.ok).count());
        assertEquals("necessary", correct("neccessary", "necessary"));
    }

    @Test
    void emptyTypedMarksEverythingMissing() {
        assertEquals("", typed("", "cat"));
        assertEquals("[c][a][t]", correct("", "cat"));
    }

    @Test
    void keepsOriginalCase() {
        assertEquals("Japa[n]ese", correct("japaese", "Japanese"));
    }
}
