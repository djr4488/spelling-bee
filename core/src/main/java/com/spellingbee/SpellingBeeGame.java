package com.spellingbee;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.spellingbee.data.WordBank;
import com.spellingbee.data.WordEntry;
import com.spellingbee.quiz.Progress;
import com.spellingbee.quiz.QuizSession;
import com.spellingbee.quiz.WordDeck;
import com.spellingbee.screens.MenuScreen;
import com.spellingbee.screens.QuizScreen;
import com.spellingbee.screens.SettingsScreen;
import com.spellingbee.speech.Pronouncer;
import com.spellingbee.speech.SpeechService;
import com.spellingbee.store.PreferencesStore;
import com.spellingbee.store.Settings;
import com.spellingbee.ui.GameSounds;
import com.spellingbee.ui.UiKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Entry point shared by all platforms. Owns the long-lived services. */
public class SpellingBeeGame extends Game {
    public final SpeechService speech;
    public UiKit ui;
    public WordBank words;
    public Settings settings;
    public Progress progress;
    public Pronouncer pronouncer;
    public GameSounds sounds;
    private final Random random = new Random();

    public SpellingBeeGame(SpeechService speech) {
        this.speech = speech;
    }

    @Override
    public void create() {
        ui = new UiKit();
        words = WordBank.parse(Gdx.files.internal("words.json").readString("UTF-8"));
        settings = new Settings(new PreferencesStore(Gdx.app.getPreferences("spelling-bee-settings" + System.getProperty("spellingbee.prefsSuffix", ""))));
        progress = new Progress(new PreferencesStore(Gdx.app.getPreferences("spelling-bee-progress" + System.getProperty("spellingbee.prefsSuffix", ""))));
        speech.setVoice(settings.getVoice());
        pronouncer = new Pronouncer(speech, settings);
        sounds = new GameSounds(settings);
        showMenu();
    }

    /** New practice session of the chosen length, without repeats until the list is used up. */
    public void startPractice() {
        List<String> all = new ArrayList<>();
        for (WordEntry e : words.all()) all.add(e.word);
        int length = settings.getSessionLength();
        List<String> picked;
        if (length <= 0) {
            picked = WordDeck.shuffled(all, random);
        } else {
            WordDeck deck = new WordDeck(all, progress.getDeckState(), random);
            picked = deck.draw(length);
            progress.setDeckState(deck.saveState());
        }
        start(QuizSession.Mode.PRACTICE, picked);
    }

    /** Quizzes only words that are on the missed list. */
    public void startReview() {
        start(QuizSession.Mode.REVIEW, WordDeck.shuffled(progress.getMissed(), random));
    }

    private void start(QuizSession.Mode mode, List<String> picked) {
        List<WordEntry> entries = new ArrayList<>();
        for (String w : picked) {
            WordEntry e = words.find(w);
            if (e != null) entries.add(e);
        }
        if (entries.isEmpty()) {
            showMenu();
            return;
        }
        switchTo(new QuizScreen(this, new QuizSession(mode, entries)));
    }

    public void showMenu() {
        switchTo(new MenuScreen(this));
    }

    public void showSettings() {
        switchTo(new SettingsScreen(this));
    }

    /** Sets a new screen and disposes the old one. */
    public void switchTo(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null) old.dispose();
    }

    /** App sent to the background (e.g. tablet home button): stop talking. */
    @Override
    public void pause() {
        super.pause();
        speech.stop();
    }

    @Override
    public void dispose() {
        Screen s = getScreen();
        if (s != null) s.dispose();
        speech.dispose();
        sounds.dispose();
        ui.dispose();
    }
}
