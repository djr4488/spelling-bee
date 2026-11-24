package com.spellingbee.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.quiz.QuizSession;
import com.spellingbee.screens.ProgressScreen;
import com.spellingbee.screens.QuizScreen;
import com.spellingbee.screens.SummaryScreen;
import com.spellingbee.screens.VoiceCheckScreen;
import com.spellingbee.speech.SpeechService;

import java.util.Arrays;
import java.util.zip.Deflater;

/**
 * Developer tool: visits each screen, saves a PNG of it, then exits.
 * Run with: ./gradlew lwjgl3:run --args="--tour /some/dir"
 * Uses throwaway preference files so real progress is untouched.
 */
public class ScreenshotTour extends SpellingBeeGame {
    private final FileHandle outDir;
    private int frame;
    private int step;

    public ScreenshotTour(SpeechService speech, String outDir) {
        super(speech);
        this.outDir = new FileHandle(outDir);
    }

    @Override
    public void create() {
        System.setProperty("spellingbee.prefsSuffix", "-tour");
        super.create();
        progress.reset();
    }

    @Override
    public void render() {
        super.render();
        if (++frame % 20 != 0) return; // let layouts and actions settle
        switch (step++) {
            case 0: shot("1-menu"); startPractice(); break;
            case 1: shot("2-quiz"); type("nesesary"); break;
            case 2: shot("3-typed"); ((QuizScreen) getScreen()).onEnter(); break;
            case 3: shot("4-reveal"); ((QuizScreen) getScreen()).onEnter(); break;
            case 4: {
                QuizScreen q = (QuizScreen) getScreen();
                type(q.getSession().current().word);
                q.onEnter();
                frame = 6; // capture mid-flash
                break;
            }
            case 5: shot("5-correct"); frame = 0; break;
            case 6: setScreenSession(); break;
            case 7: shot("6-summary"); switchTo(new ProgressScreen(this)); break;
            case 8: shot("7-progress"); showSettings(); break;
            case 9: shot("8-settings"); switchTo(new VoiceCheckScreen(this)); break;
            case 10: shot("9-voicecheck"); progress.reset(); Gdx.app.exit(); break;
            default: break;
        }
    }

    private void type(String s) {
        QuizScreen q = (QuizScreen) getScreen();
        for (char c : s.toCharArray()) q.onChar(c);
    }

    private void setScreenSession() {
        QuizSession s = new QuizSession(QuizSession.Mode.PRACTICE,
                Arrays.asList(words.find("kelp"), words.find("necessary") != null ? words.find("necessary") : words.find("whale"),
                        words.find("a cappella")));
        s.answer("kelp");
        s.advance();
        s.answer("wail");
        s.advance();
        s.answer("acapella");
        s.advance();
        switchTo(new SummaryScreen(this, s));
    }

    private void shot(String name) {
        int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, w, h);
        PixmapIO.writePNG(outDir.child(name + ".png"), pm, Deflater.DEFAULT_COMPRESSION, true);
        pm.dispose();
    }
}
