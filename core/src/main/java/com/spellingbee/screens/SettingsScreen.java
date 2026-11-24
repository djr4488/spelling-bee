package com.spellingbee.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.store.Settings;

import java.util.List;

/** Speech rate, voice, timed practice length and sound effects. */
public class SettingsScreen extends BaseScreen {
    private final Label rateLabel = label("", "big");
    private final Label voiceLabel = label("", "default");
    private final List<String> voices;

    public SettingsScreen(SpellingBeeGame game) {
        super(game);
        voices = game.speech.getVoices();
        Settings s = game.settings;

        root.defaults().pad(10);
        root.add(label("Settings", "title")).colspan(2).padBottom(10).row();

        // Speech rate
        Table rate = new Table();
        rate.add(button("Slower", "default", () -> changeRate(-0.05f))).width(200).height(80);
        rateLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        rate.add(rateLabel).width(200);
        rate.add(button("Faster", "default", () -> changeRate(0.05f))).width(200).height(80);
        root.add(label("Speech speed", "bold")).right().padRight(20);
        root.add(rate).left().row();

        // Voice
        Table voice = new Table();
        voice.add(button("<", "default", () -> changeVoice(-1))).width(80).height(80);
        voice.add(voiceLabel).width(460).padLeft(10).padRight(10);
        voice.add(button(">", "default", () -> changeVoice(1))).width(80).height(80);
        root.add(label("Voice", "bold")).right().padRight(20);
        if (voices.isEmpty()) {
            root.add(label("Default (this device has no voice choices)", "default")).left().row();
        } else {
            root.add(voice).left().row();
        }

        // Timed practice length
        Label minutes = label("", "big");
        minutes.setAlignment(com.badlogic.gdx.utils.Align.center);
        Runnable showMinutes = () -> minutes.setText(s.getTimedMinutes() + " min");
        showMinutes.run();
        Table timed = new Table();
        timed.add(button("Less", "default", () -> { s.stepTimedMinutes(-1); showMinutes.run(); })).width(200).height(80);
        timed.add(minutes).width(200);
        timed.add(button("More", "default", () -> { s.stepTimedMinutes(1); showMinutes.run(); })).width(200).height(80);
        root.add(label("Timed practice", "bold")).right().padRight(20);
        root.add(timed).left().row();

        // Sound effects
        TextButton sound = new TextButton("", skin, "toggle");
        sound.setChecked(s.isSoundOn());
        sound.setText(s.isSoundOn() ? "Sound effects: ON" : "Sound effects: OFF");
        onClick(sound, () -> {
            s.setSoundOn(sound.isChecked());
            sound.setText(sound.isChecked() ? "Sound effects: ON" : "Sound effects: OFF");
        });
        root.add(label("Sounds", "bold")).right().padRight(20);
        root.add(sound).left().width(400).height(80).row();

        root.add(label(game.speech.getStatus(), "small")).colspan(2).padTop(10).row();

        Table bottom = new Table();
        bottom.defaults().pad(10);
        bottom.add(button("Voice check", "default", () -> game.switchTo(new VoiceCheckScreen(game)))).width(360).height(90);
        bottom.add(button("Done", "primary", this::onBack)).width(360).height(90);
        root.add(bottom).colspan(2).padTop(20).row();

        refresh();
    }

    private void changeRate(float delta) {
        game.settings.setSpeechRate(game.settings.getSpeechRate() + delta);
        refresh();
        game.pronouncer.say("This is how fast I will talk.");
    }

    private void changeVoice(int step) {
        int index = voices.indexOf(game.settings.getVoice()); // -1 = default
        int count = voices.size() + 1;                        // + "Default"
        int next = Math.floorMod(index + 1 + step, count) - 1;
        String v = next < 0 ? null : voices.get(next);
        game.settings.setVoice(v);
        game.speech.setVoice(v);
        refresh();
        game.pronouncer.say("Hello! Let's practice spelling.");
    }

    private void refresh() {
        rateLabel.setText(Math.round(game.settings.getSpeechRate() * 100) + "%");
        String v = game.settings.getVoice();
        voiceLabel.setText(v == null ? "Default" : v);
        voiceLabel.setEllipsis(true);
    }

    @Override
    protected void onBack() {
        game.speech.stop();
        game.showMenu();
    }
}
