package com.spellingbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.store.Settings;
import com.spellingbee.ui.Palette;

/** Home screen: start practice (with session length), review missed words, progress, settings. */
public class MenuScreen extends BaseScreen {
    public MenuScreen(SpellingBeeGame game) {
        super(game);
        root.defaults().pad(10);
        root.add(label("Spelling Bee", "title")).row();
        root.add(label(game.words.size() + " words to practice", "small")).padBottom(20).row();

        // Session length
        Table lengths = new Table();
        lengths.defaults().pad(6).width(150).height(76);
        lengths.add(label("Words:", "bold")).width(110);
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        for (int len : Settings.SESSION_LENGTHS) {
            TextButton b = new TextButton(len == 0 ? "All" : String.valueOf(len), skin, "toggle");
            group.add(b);
            if (len == game.settings.getSessionLength()) b.setChecked(true);
            onClick(b, () -> { if (b.isChecked()) game.settings.setSessionLength(len); });
            lengths.add(b);
        }
        root.add(lengths).row();

        root.add(button("Start practice", "primary", game::startPractice)).width(560).height(110).padTop(6).row();

        int missed = game.progress.getMissed().size();
        TextButton review = button("Review missed words (" + missed + ")", "default", game::startReview);
        review.setDisabled(missed == 0);
        review.setTouchable(missed == 0 ? com.badlogic.gdx.scenes.scene2d.Touchable.disabled
                : com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        root.add(review).width(560).height(90).row();

        Table row = new Table();
        row.defaults().pad(10).width(270).height(84);
        row.add(button("My progress", "default", () -> game.switchTo(new ProgressScreen(game))));
        row.add(button("Settings", "default", game::showSettings));
        root.add(row).row();

        if (!game.speech.isAvailable()) {
            Label warn = label(game.speech.getStatus(), "tint-default");
            warn.setColor(Palette.WRONG);
            root.add(warn).padTop(10).row();
        }
    }

    @Override
    protected void onBack() {
        Gdx.app.exit();
    }
}
