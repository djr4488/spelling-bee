package com.spellingbee.screens;

import com.spellingbee.SpellingBeeGame;
import com.spellingbee.data.WordEntry;
import com.spellingbee.speech.Pronouncer;

/** Lets you hear the speech engine say a sample word, definition and sentence. */
public class VoiceCheckScreen extends BaseScreen {
    private final WordEntry sample;

    public VoiceCheckScreen(SpellingBeeGame game) {
        super(game);
        WordEntry w = game.words.find("necessary");
        sample = w != null ? w : game.words.all().get(0);

        root.defaults().pad(10);
        root.add(label("Voice check", "title")).row();
        root.add(label(game.speech.getStatus(), "small")).row();
        root.add(label("Sample word: " + sample.word, "big")).padBottom(20).row();

        Pronouncer say = game.pronouncer;
        root.add(button("Say the word", "default", () -> say.word(sample))).width(520).height(80).row();
        root.add(button("Word, definition, sentence", "default", () -> say.reveal(sample))).width(520).height(80).row();
        root.add(button("Stop", "default", game.speech::stop)).width(520).height(80).row();
        root.add(button("Back", "primary", this::onBack)).width(520).height(90).padTop(30).row();
    }

    @Override
    protected void onBack() {
        game.speech.stop();
        game.showSettings();
    }
}
