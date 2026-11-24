package com.spellingbee.quiz;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordDeckTest {
    private static final List<String> WORDS = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l");

    @Test
    void noRepeatsUntilDeckIsExhausted() {
        WordDeck deck = new WordDeck(WORDS, "", new Random(1));
        Set<String> seen = new HashSet<>();
        seen.addAll(deck.draw(5));
        seen.addAll(deck.draw(5));
        assertEquals(10, seen.size());
        List<String> last = deck.draw(2);
        seen.addAll(last);
        assertEquals(12, seen.size(), "every word drawn exactly once");
    }

    @Test
    void drawAcrossReshuffleHasNoDuplicatesWithinADraw() {
        WordDeck deck = new WordDeck(WORDS, "", new Random(2));
        deck.draw(10);
        List<String> next = deck.draw(10); // 2 left, then reshuffle
        assertEquals(10, new HashSet<>(next).size());
    }

    @Test
    void zeroOrTooManyMeansAllWords() {
        WordDeck deck = new WordDeck(WORDS, "", new Random(3));
        assertEquals(12, new HashSet<>(deck.draw(0)).size());
        assertEquals(12, deck.draw(99).size());
    }

    @Test
    void shuffles() {
        WordDeck deck = new WordDeck(WORDS, "", new Random(4));
        assertNotEquals(WORDS, deck.draw(0));
    }

    @Test
    void savedStateContinuesWhereItLeftOff() {
        WordDeck first = new WordDeck(WORDS, "", new Random(5));
        List<String> drawn = new ArrayList<>(first.draw(7));
        WordDeck second = new WordDeck(WORDS, first.saveState(), new Random(6));
        assertEquals(5, second.remainingCount());
        List<String> rest = second.draw(5);
        for (String w : rest) assertTrue(!drawn.contains(w), "repeated " + w);
    }

    @Test
    void savedStateDropsWordsNoLongerInTheList() {
        WordDeck deck = new WordDeck(Arrays.asList("a", "b"), "a\nzzz\nb", new Random(7));
        assertEquals(2, deck.remainingCount());
    }
}
