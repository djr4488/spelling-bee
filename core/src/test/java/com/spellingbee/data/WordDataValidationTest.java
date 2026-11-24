package com.spellingbee.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks every entry in assets/words.json (the same rules as tools/enrich.py). */
class WordDataValidationTest {
    private static Path assets;
    private static WordBank bank;

    @BeforeAll
    static void load() throws IOException {
        assets = Path.of(System.getProperty("assetsDir", "../assets"));
        bank = WordBank.parse(Files.readString(assets.resolve("words.json"), StandardCharsets.UTF_8));
    }

    @Test
    void requiredFieldsPresent() {
        List<String> problems = new ArrayList<>();
        for (WordEntry e : bank.all()) {
            if (blank(e.word)) problems.add("entry without word");
            if (blank(e.partOfSpeech)) problems.add(e.word + ": partOfSpeech");
            if (blank(e.definition)) problems.add(e.word + ": definition");
            if (blank(e.sentence)) problems.add(e.word + ": sentence");
            if (blank(e.sentence2)) problems.add(e.word + ": sentence2");
            else if (e.sentence2.equals(e.sentence)) problems.add(e.word + ": sentence2 repeats sentence");
            if (blank(e.pronunciation)) problems.add(e.word + ": pronunciation");
        }
        assertTrue(problems.isEmpty(), "Missing fields: " + problems);
    }

    @Test
    void sentenceContainsExactWord() {
        List<String> problems = new ArrayList<>();
        for (WordEntry e : bank.all()) {
            int flags = Character.isUpperCase(e.word.charAt(0)) ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern p = Pattern.compile("(?<![\\w-])" + Pattern.quote(e.word) + "(?![\\w-])", flags);
            if (!p.matcher(e.sentence).find()) problems.add(e.word);
            if (!p.matcher(e.sentence2).find()) problems.add(e.word + " (sentence2)");
        }
        assertTrue(problems.isEmpty(), "Sentence lacks the word: " + problems);
    }

    @Test
    void definitionDoesNotContainWord() {
        List<String> problems = new ArrayList<>();
        for (WordEntry e : bank.all()) {
            String def = e.definition.toLowerCase(Locale.ROOT);
            for (String part : e.word.toLowerCase(Locale.ROOT).split("[\\s-]+")) {
                if (part.length() < 3) continue;
                String stem = stem(part);
                String regex = stem.length() >= 4 ? "\\b" + Pattern.quote(stem) : "\\b" + Pattern.quote(part) + "\\b";
                if (Pattern.compile(regex).matcher(def).find()) problems.add(e.word);
            }
        }
        assertTrue(problems.isEmpty(), "Definition gives away the word: " + problems);
    }

    @Test
    void everyDictionaryWordHasAnEntry() throws IOException {
        String raw = Files.readString(assets.resolve("words.txt"), StandardCharsets.UTF_8);
        Set<String> seen = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (String token : raw.split("[,\\n]")) {
            String w = token.trim().replaceAll("\\s+", " ");
            if (w.isEmpty() || !seen.add(w.toLowerCase(Locale.ROOT))) continue;
            if (bank.find(w) == null) missing.add(w);
        }
        assertTrue(missing.isEmpty(), "Run tools/enrich.py; missing: " + missing);
        assertEquals(seen.size(), bank.size(), "words.json has entries not in words.txt");
    }

    @Test
    void speakAsSwapsWordInSentence() {
        WordEntry quiche = bank.find("quiche");
        assertTrue(quiche.spokenSentence().contains("keesh"));
        assertTrue(!quiche.spokenSentence().contains("quiche"));
    }

    private static final String[] SUFFIXES = {"ness", "ing", "ies", "ied", "ed", "es", "ly", "er", "est", "s", "y", "e"};

    private static String stem(String w) {
        for (String s : SUFFIXES) {
            if (w.endsWith(s) && w.length() - s.length() >= 4) return w.substring(0, w.length() - s.length());
        }
        return w;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
