package com.spellingbee.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.spellingbee.store.Settings;

/** Feedback sounds, honoring the sound on/off setting. */
public class GameSounds implements Disposable {
    private final Settings settings;
    private final Sound correct;
    private final Sound wrong;

    public GameSounds(Settings settings) {
        this.settings = settings;
        correct = load("sounds/correct.wav");
        wrong = load("sounds/wrong.wav");
    }

    private static Sound load(String path) {
        try {
            return Gdx.audio.newSound(Gdx.files.internal(path));
        } catch (RuntimeException e) {
            Gdx.app.error("sounds", "Could not load " + path, e);
            return null;
        }
    }

    public void correct() {
        play(correct);
    }

    public void wrong() {
        play(wrong);
    }

    private void play(Sound s) {
        if (s != null && settings.isSoundOn()) s.play(0.8f);
    }

    @Override
    public void dispose() {
        if (correct != null) correct.dispose();
        if (wrong != null) wrong.dispose();
    }
}
