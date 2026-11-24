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
import com.spellingbee.quiz.AnswerChecker;
import com.spellingbee.quiz.LetterDiff;
import com.spellingbee.ui.OnScreenKeyboard;
import com.spellingbee.ui.Palette;

/**
 * Study one word ("look, say, cover, write, check"): see it while hearing it said
 * and spelled, then cover it and type it from memory. Nothing here is scored or
 * changes progress.
 */
public class StudyScreen extends BaseScreen implements OnScreenKeyboard.Listener {
    private static final int MAX_LENGTH = 40;

    private final WordEntry word;
    private final Container<Table> body = new Container<>();
    private final StringBuilder typed = new StringBuilder();
    private final Label answerLabel = new Label("", skin, "answer");
    private final Table answerBox = new Table();
    private TextButton sentenceButton;
    private int nextSentence;
    /** True while the player is typing (the word is covered). */
    private boolean writing;

    public StudyScreen(SpellingBeeGame game, WordEntry word) {
        super(game);
        this.word = word;
        root.top().pad(16);

        Table header = new Table();
        header.add(label("Study", "bold")).left();
        header.add(label("Practice only, not scored", "small")).expandX();
        header.add(button("Back", "default", this::onBack)).width(140).height(64);
        root.add(header).growX().padBottom(10).row();

        body.fill();
        root.add(body).grow().row();
        showLook(true);
    }

    @Override
    public void show() {
        super.show();
        InputMultiplexer mux = (InputMultiplexer) Gdx.input.getInputProcessor();
        mux.addProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char c) {
                if (!writing) return false;
                if (c == '\b') onBackspace();
                else if (c == '\r' || c == '\n') onEnter();
                else if (Character.isLetter(c) || c == ' ' || c == '-' || c == '\'') onChar(c);
                else return false;
                return true;
            }
        });
    }

    // ---- look: the word on screen --------------------------------------------------------

    private void showLook(boolean speak) {
        writing = false;
        game.speech.stop();

        Table t = new Table();
        t.setBackground(game.ui.round(Palette.PANEL));
        t.pad(20, 28, 20, 28).defaults().left().pad(4);
        Label big = new Label(DARK + word.word + "[]", skin, "answer");
        t.add(big).colspan(2).center().padBottom(6).row();
        t.add(label("Say it", "default")).padRight(24);
        t.add(label(word.pronunciation + "   (" + word.partOfSpeech + ")", "bold")).row();
        t.add(label("Meaning", "default")).top().padRight(24);
        t.add(wrapped(word.definition)).width(900).row();
        t.add(label("Sentences", "default")).top().padRight(24);
        t.add(wrapped(word.sentence)).width(900).row();
        if (word.hasSentence2()) {
            t.add(label("", "default"));
            t.add(wrapped(word.sentence2)).width(900).row();
        }

        Table hints = new Table();
        hints.defaults().pad(6).height(78).width(292);
        hints.add(button("Hear word", "hint", () -> game.pronouncer.word(word)));
        hints.add(button("Spell it out", "hint", () -> game.pronouncer.spell(word)));
        hints.add(sentenceButton());
        hints.add(altButton());

        Table area = new Table();
        area.add(t).growX().row();
        area.add(hints).padTop(10).row();
        area.add(button("Cover it and spell", "primary", this::showWrite)).width(560).height(96).padTop(8).row();
        body.setActor(area);

        if (speak) game.pronouncer.spell(word);
    }

    // ---- write: the word covered, typed from memory --------------------------------------

    /** Covers the word and lets the player type it. */
    public void showWrite() {
        writing = true;
        game.speech.stop();
        typed.setLength(0);

        Table hints = new Table();
        hints.defaults().pad(6).height(78).width(292);
        hints.add(button("Hear word", "hint", () -> game.pronouncer.word(word)));
        hints.add(button("Definition", "hint", () -> game.pronouncer.definition(word)));
        hints.add(sentenceButton());
        hints.add(altButton());

        answerLabel.setAlignment(Align.center);
        answerBox.clearChildren();
        answerBox.add(answerLabel).grow();
        refreshAnswer();

        Table area = new Table();
        area.add(hints).padBottom(8).row();
        area.add(answerBox).width(1000).height(110).padBottom(6).row();
        area.add(new OnScreenKeyboard(skin, this)).row();
        body.setActor(area);

        game.pronouncer.word(word);
    }

    private void refreshAnswer() {
        answerBox.setBackground(game.ui.round(Palette.PANEL));
        answerLabel.setText(typed.length() == 0 ? "[#9A927F]Type the word from memory[]"
                : DARK + typed + "[][#C98F00]|[]");
    }

    @Override
    public void onChar(char c) {
        if (!writing || typed.length() >= MAX_LENGTH) return;
        if (c == ' ' && (typed.length() == 0 || typed.charAt(typed.length() - 1) == ' ')) return;
        typed.append(Character.toLowerCase(c));
        refreshAnswer();
    }

    @Override
    public void onBackspace() {
        if (!writing || typed.length() == 0) return;
        typed.setLength(typed.length() - 1);
        refreshAnswer();
    }

    @Override
    public void onEnter() {
        if (!writing) return;
        if (typed.toString().trim().isEmpty()) {
            game.pronouncer.say("Type your spelling first.");
            return;
        }
        showCheck(typed.toString());
    }

    // ---- check ---------------------------------------------------------------------------

    private void showCheck(String answer) {
        writing = false;
        game.speech.stop();
        boolean ok = AnswerChecker.isCorrect(answer, word.word);

        Table t = new Table();
        t.setBackground(game.ui.round(ok ? Palette.CORRECT : Palette.PANEL));
        t.pad(24).defaults().left().pad(6);
        if (ok) {
            t.add(new Label("[#FFFFFF]Correct!  You spelled " + word.word + ".[]", skin, "answer")).center();
            game.sounds.correct();
        } else {
            LetterDiff diff = LetterDiff.of(answer, word.word);
            Label title = label("Not quite. Compare them:", "tint-bold");
            title.setColor(Palette.WRONG);
            t.add(title).colspan(2).row();
            t.add(label("You typed", "default")).padRight(24);
            t.add(new Label(markup(diff.typed, Palette.MARK_BAD), skin, "answer")).row();
            t.add(label("Correct", "default")).padRight(24);
            t.add(new Label(markup(diff.correct, Palette.MARK_OK), skin, "answer")).row();
            game.sounds.wrong();
            game.pronouncer.spell(word);
        }

        Table buttons = new Table();
        buttons.defaults().pad(10).height(90).width(340);
        buttons.add(button("Look again", "default", () -> showLook(true)));
        buttons.add(button(ok ? "Spell it again" : "Try again", "primary", this::showWrite));
        buttons.add(button("Back to list", "default", this::onBack));

        Table area = new Table();
        area.add(t).growX().row();
        area.add(buttons).padTop(16).row();
        body.setActor(area);
        if (ok) t.addAction(Actions.sequence(Actions.alpha(0.6f), Actions.fadeIn(0.2f)));
    }

    // ---- shared buttons ------------------------------------------------------------------

    /** Alternates between the two example sentences. */
    private TextButton sentenceButton() {
        nextSentence = 0;
        sentenceButton = button("Sentence", "hint", () -> {
            game.pronouncer.sentence(word, nextSentence);
            if (word.hasSentence2()) {
                nextSentence = 1 - nextSentence;
                sentenceButton.setText(nextSentence == 1 ? "Another sentence" : "First sentence");
            }
        });
        return sentenceButton;
    }

    private TextButton altButton() {
        TextButton b = button("Say it another way", "hint", () -> game.pronouncer.alternate(word));
        b.setDisabled(!word.hasAltPronunciation());
        return b;
    }

    @Override
    protected void onBack() {
        game.speech.stop();
        game.switchTo(new ProgressScreen(game));
    }
}
