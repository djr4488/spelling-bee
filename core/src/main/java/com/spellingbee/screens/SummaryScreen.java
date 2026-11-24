package com.spellingbee.screens;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.data.WordEntry;
import com.spellingbee.quiz.QuizSession;
import com.spellingbee.ui.Palette;

/** End of a session: score and the words to practice again. */
public class SummaryScreen extends BaseScreen {
    public SummaryScreen(SpellingBeeGame game, QuizSession session) {
        super(game);
        int right = session.correctCount(), total = session.answeredCount();
        String cheer = right == total ? "Perfect score!" : right * 10 >= total * 8 ? "Great work!" : "Good practice!";
        String result = "You spelled " + right + " of " + total + " words correctly.";
        if (session.isTimed()) {
            int minutes = Math.max(1, Math.round(session.elapsedSeconds() / 60f));
            result = "In " + minutes + (minutes == 1 ? " minute" : " minutes") + " you spelled "
                    + right + " of " + total + " words correctly.";
        }

        root.defaults().pad(10);
        root.add(label(cheer, "title")).row();
        root.add(label(result, "big")).padBottom(10).row();

        if (!session.missed().isEmpty()) {
            root.add(label("Words to practice:", "bold")).row();
            Table list = new Table();
            list.top();
            for (WordEntry w : session.missed()) {
                Label l = label(w.word, "tint-big");
                l.setColor(Palette.WRONG);
                list.add(l).pad(4).row();
            }
            ScrollPane scroll = new ScrollPane(list, skin);
            scroll.setFadeScrollBars(false);
            root.add(scroll).height(280).width(700).row();
        }

        Table buttons = new Table();
        buttons.defaults().pad(10).height(90).width(380);
        if (!game.progress.getMissed().isEmpty()) {
            buttons.add(button("Review missed words", "default", game::startReview));
        }
        buttons.add(button("Practice more", "primary", game::startPractice));
        buttons.add(button("Menu", "default", game::showMenu));
        root.add(buttons).padTop(20).row();

        game.pronouncer.say(cheer + " " + result.replace(" of ", " out of "));
    }

    @Override
    protected void onBack() {
        game.speech.stop();
        game.showMenu();
    }
}
