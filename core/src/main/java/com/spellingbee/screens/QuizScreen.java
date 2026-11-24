package com.spellingbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.data.WordEntry;
import com.spellingbee.quiz.LetterDiff;
import com.spellingbee.quiz.QuizSession;
import com.spellingbee.ui.OnScreenKeyboard;
import com.spellingbee.ui.Palette;

import java.util.List;

/**
 * The quiz: the word is spoken, never shown, while the player types. A correct
 * answer gets a chime and moves on; a wrong one reveals the spelling, meaning
 * and sentence with the differences highlighted.
 */
public class QuizScreen extends BaseScreen implements OnScreenKeyboard.Listener {
    private static final int MAX_LENGTH = 40;
    private static final String DARK = "[#2B2B2B]";

    private final QuizSession session;
    private final StringBuilder typed = new StringBuilder();

    private final Label progressLabel = label("", "bold");
    private final Label scoreLabel = label("", "bold");
    private final Label answerLabel = new Label("", skin, "answer");
    private final Table answerBox = new Table();
    private final Container<Table> body = new Container<>();
    private final Table quizArea = new Table();
    private final TextButton altButton;

    /** True while the "Correct!" flash plays or the reveal is showing. */
    private boolean busy;
    private boolean revealing;

    public QuizScreen(SpellingBeeGame game, QuizSession session) {
        super(game);
        this.session = session;
        root.top().pad(16);

        // Header
        Table header = new Table();
        header.add(progressLabel).left().expandX();
        header.add(label(session.mode == QuizSession.Mode.REVIEW ? "Review missed words" : "Practice", "small")).expandX();
        header.add(scoreLabel).right().padRight(20);
        header.add(button("Menu", "default", this::onBack)).width(140).height(64);
        root.add(header).growX().padBottom(10).row();

        // Hint buttons: audio only, nothing revealing on screen
        Table hints = new Table();
        hints.defaults().pad(6).height(78).width(292);
        hints.add(button("Hear word", "hint", () -> { if (!revealing) game.pronouncer.word(current()); }));
        hints.add(button("Definition", "hint", () -> { if (!revealing) game.pronouncer.definition(current()); }));
        hints.add(button("Sentence", "hint", () -> { if (!revealing) game.pronouncer.sentence(current()); }));
        altButton = button("Say it another way", "hint", () -> { if (!revealing) game.pronouncer.alternate(current()); });
        hints.add(altButton);
        quizArea.add(hints).padBottom(8).row();

        // What the player has typed
        answerLabel.setAlignment(Align.center);
        answerBox.add(answerLabel).grow();
        quizArea.add(answerBox).width(1000).height(110).padBottom(6).row();
        quizArea.add(new OnScreenKeyboard(skin, this)).row();

        body.fill();
        root.add(body).grow().row();
        startWord();
    }

    @Override
    public void show() {
        super.show();
        InputMultiplexer mux = (InputMultiplexer) Gdx.input.getInputProcessor();
        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char c) {
                if (c == '\b') onBackspace();
                else if (c == '\r' || c == '\n') onEnter();
                else if (Character.isLetter(c) || c == ' ' || c == '-' || c == '\'') onChar(c);
                else return false;
                return true;
            }
        });
    }

    public QuizSession getSession() {
        return session;
    }

    private WordEntry current() {
        return session.current();
    }

    private void startWord() {
        busy = false;
        revealing = false;
        typed.setLength(0);
        body.setActor(quizArea);
        progressLabel.setText("Word " + session.number() + " of " + session.size());
        scoreLabel.setText("Correct: " + session.correctCount());
        altButton.setDisabled(!current().hasAltPronunciation());
        refreshAnswer();
        stage.addAction(Actions.sequence(Actions.delay(0.4f), Actions.run(() -> game.pronouncer.word(current()))));
    }

    private void refreshAnswer() {
        answerBox.setBackground(game.ui.round(Palette.PANEL));
        if (typed.length() == 0) {
            answerLabel.setText("[#9A927F]Type the word you hear[]");
        } else {
            answerLabel.setText(DARK + typed + "[][#C98F00]|[]");
        }
    }

    // ---- keyboard ------------------------------------------------------------------------

    @Override
    public void onChar(char c) {
        if (busy || typed.length() >= MAX_LENGTH) return;
        if (c == ' ' && (typed.length() == 0 || typed.charAt(typed.length() - 1) == ' ')) return;
        typed.append(Character.toLowerCase(c));
        refreshAnswer();
    }

    @Override
    public void onBackspace() {
        if (busy || typed.length() == 0) return;
        typed.setLength(typed.length() - 1);
        refreshAnswer();
    }

    @Override
    public void onEnter() {
        if (revealing) {
            next();
            return;
        }
        if (busy) return;
        if (typed.toString().trim().isEmpty()) {
            game.pronouncer.say("Type your spelling first.");
            return;
        }
        submit();
    }

    // ---- answering -----------------------------------------------------------------------

    private void submit() {
        busy = true;
        WordEntry w = current();
        String answer = typed.toString();
        boolean ok = session.answer(answer);
        game.progress.record(w.word, ok);
        scoreLabel.setText("Correct: " + session.correctCount());
        if (ok) {
            game.speech.stop();
            game.sounds.correct();
            answerBox.setBackground(game.ui.round(Palette.CORRECT));
            answerLabel.setText("[#FFFFFF]Correct!  Great job![]");
            answerBox.setTransform(true);
            answerBox.setOrigin(Align.center);
            answerBox.addAction(Actions.sequence(
                    Actions.scaleTo(1.06f, 1.06f, 0.12f), Actions.scaleTo(1f, 1f, 0.12f),
                    Actions.delay(1.0f), Actions.run(this::next)));
        } else {
            game.sounds.wrong();
            showReveal(w, answer);
        }
    }

    private void showReveal(WordEntry w, String answer) {
        revealing = true;
        LetterDiff diff = LetterDiff.of(answer, w.word);

        Table t = new Table();
        t.setBackground(game.ui.round(Palette.PANEL));
        t.pad(24).defaults().left().pad(6);

        Label title = label("Not quite. Here is the word:", "tint-bold");
        title.setColor(Palette.WRONG);
        t.add(title).colspan(2).row();

        t.add(label("You typed", "default")).padRight(24);
        t.add(new Label(markup(diff.typed, Palette.MARK_BAD), skin, "answer")).row();
        t.add(label("Correct", "default")).padRight(24);
        t.add(new Label(markup(diff.correct, Palette.MARK_OK), skin, "answer")).row();

        t.add(label("Say it", "default")).padRight(24);
        t.add(label(w.pronunciation + "   (" + w.partOfSpeech + ")", "bold")).row();
        t.add(label("Meaning", "default")).top().padRight(24);
        t.add(wrapped(w.definition)).width(900).row();
        t.add(label("Sentence", "default")).top().padRight(24);
        t.add(wrapped(w.sentence)).width(900).row();
        if (w.note != null && !w.note.isEmpty()) {
            t.add(label("Note", "default")).top().padRight(24);
            t.add(wrapped(w.note)).width(900).row();
        }

        Table buttons = new Table();
        buttons.defaults().pad(10).height(90);
        buttons.add(button("Hear it again", "default", () -> game.pronouncer.reveal(w))).width(340);
        buttons.add(button("Continue", "primary", this::next)).width(340);

        Table area = new Table();
        area.add(t).growX().row();
        area.add(buttons).padTop(10).row();
        body.setActor(area);

        game.pronouncer.reveal(w);
    }

    private Label wrapped(String text) {
        Label l = label(text, "default");
        l.setWrap(true);
        return l;
    }

    /** Letters that don't match are colored; mismatched spaces show as "_". */
    private static String markup(List<LetterDiff.Mark> marks, String badColor) {
        if (marks.isEmpty()) return "[#9A927F](nothing)[]";
        StringBuilder sb = new StringBuilder(DARK);
        for (LetterDiff.Mark m : marks) {
            char c = m.ch == '[' ? '(' : m.ch;
            if (m.ok) {
                sb.append(c);
            } else {
                sb.append(badColor).append(c == ' ' ? '_' : c).append("[]");
            }
        }
        return sb.append("[]").toString();
    }

    private void next() {
        game.speech.stop();
        stage.getRoot().clearActions();
        answerBox.clearActions();
        answerBox.setScale(1f);
        session.advance();
        if (session.isFinished()) {
            game.switchTo(new SummaryScreen(game, session));
        } else {
            startWord();
        }
    }

    @Override
    protected void onBack() {
        game.speech.stop();
        game.showMenu();
    }

}
