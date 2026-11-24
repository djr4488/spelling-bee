package com.spellingbee.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.quiz.Progress;

import java.util.List;

/** Lifetime totals and the list of missed words. */
public class ProgressScreen extends BaseScreen {
    private boolean confirmReset;

    public ProgressScreen(SpellingBeeGame game) {
        super(game);
        Progress p = game.progress;
        root.defaults().pad(8);
        root.add(label("My progress", "title")).colspan(2).row();

        int pct = p.getAttempted() == 0 ? 0 : Math.round(100f * p.getCorrect() / p.getAttempted());
        Table stats = new Table();
        stats.defaults().pad(6, 20, 6, 20);
        stats.add(stat("Words tried", p.getAttempted()));
        stats.add(stat("Correct", p.getCorrect()));
        stats.add(stat("Missed", p.getIncorrect()));
        stats.add(stat("Score", pct + "%"));
        root.add(stats).colspan(2).padBottom(10).row();

        List<String> missed = p.getMissed();
        root.add(label(missed.isEmpty() ? "No missed words right now. Nice!"
                : "Words to practice (" + missed.size() + "):", "bold")).colspan(2).row();
        if (!missed.isEmpty()) {
            Table list = new Table();
            list.top().defaults().pad(4, 16, 4, 16).left();
            int col = 0;
            for (String w : missed) {
                list.add(label(w, "default"));
                if (++col % 3 == 0) list.row();
            }
            ScrollPane scroll = new ScrollPane(list, skin);
            scroll.setFadeScrollBars(false);
            root.add(scroll).colspan(2).width(1000).height(300).row();
        }

        Table buttons = new Table();
        buttons.defaults().pad(10).height(86).width(380);
        TextButton reset = new TextButton("Reset progress", skin, "default");
        onClick(reset, () -> {
            if (!confirmReset) {
                confirmReset = true;
                reset.setText("Tap again to reset");
            } else {
                game.progress.reset();
                game.switchTo(new ProgressScreen(game));
            }
        });
        buttons.add(reset);
        buttons.add(button("Back", "primary", this::onBack));
        root.add(buttons).colspan(2).padTop(10).row();
    }

    private Table stat(String name, Object value) {
        Table t = new Table();
        t.add(new Label(String.valueOf(value), skin, "title")).row();
        t.add(label(name, "small"));
        return t;
    }
}
