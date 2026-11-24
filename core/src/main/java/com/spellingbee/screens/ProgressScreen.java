package com.spellingbee.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.data.WordEntry;
import com.spellingbee.quiz.Progress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Lifetime totals, the list of missed words and recent timed sessions. */
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
        stats.add(stat("Words seen", p.getSeenCount() + " of " + game.words.size()));
        stats.add(stat("Answers", p.getAttempted()));
        stats.add(stat("Correct", p.getCorrect()));
        stats.add(stat("Missed", p.getIncorrect()));
        stats.add(stat("Score", pct + "%"));
        root.add(stats).colspan(2).padBottom(10).row();

        List<String> missed = p.getMissed();
        root.add(label(missed.isEmpty() ? "No missed words right now. Nice!"
                : "Words to practice (" + missed.size() + "). Tap one to study it:", "bold")).colspan(2).row();
        if (!missed.isEmpty()) {
            Table list = new Table();
            list.top().defaults().pad(4, 8, 4, 8).width(310).height(58);
            int col = 0;
            for (String w : missed) {
                Progress.WordStats s = p.getStats(w);
                int times = s == null ? 1 : Math.max(1, s.wrong());
                WordEntry entry = game.words.find(w);
                String text = times > 1 ? w + "  (" + times + "x)" : w;
                if (entry == null) {
                    list.add(label(text, "default")); // no longer in the word list
                } else {
                    list.add(button(text, "hint", () -> game.switchTo(new StudyScreen(game, entry))));
                }
                if (++col % 3 == 0) list.row();
            }
            ScrollPane scroll = new ScrollPane(list, skin);
            scroll.setFadeScrollBars(false);
            root.add(scroll).colspan(2).width(1000).height(p.getTimedRuns().isEmpty() ? 300 : 170).row();
        }

        // Most recent timed sessions, newest first
        List<Progress.TimedRun> runs = p.getTimedRuns();
        if (!runs.isEmpty()) {
            root.add(label("Timed practice:", "bold")).colspan(2).padTop(6).row();
            Table timed = new Table();
            timed.defaults().pad(2, 16, 2, 16).left();
            SimpleDateFormat when = new SimpleDateFormat("EEE MMM d, h:mm a", Locale.US);
            for (int i = runs.size() - 1; i >= Math.max(0, runs.size() - 3); i--) {
                Progress.TimedRun r = runs.get(i);
                int minutes = Math.max(1, Math.round(r.seconds / 60f));
                timed.add(label(when.format(new Date(r.endedAt)), "default"));
                timed.add(label(minutes + " min:  " + r.words + " words, " + r.correct + " correct", "default")).row();
            }
            root.add(timed).colspan(2).row();
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
