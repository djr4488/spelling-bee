package com.spellingbee.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.speech.SilentSpeechService;
import com.spellingbee.speech.SpeechService;

/** Desktop entry point. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        SpeechService speech = DesktopSpeechService.detect();
        if (speech == null) {
            String msg = DesktopSpeechService.missingEngineMessage();
            System.err.println(msg);
            speech = new SilentSpeechService(msg);
        }
        if (args.length > 0 && args[0].equals("--speech-test")) {
            speechTest(speech);
            return;
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Spelling Bee");
        config.setWindowedMode(1280, 800);
        if (args.length >= 2 && args[0].equals("--tour")) {
            if (args.length >= 4) config.setWindowedMode(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            new Lwjgl3Application(new ScreenshotTour(new SilentSpeechService("Speech: off for screenshot tour"), args[1]), config);
            return;
        }
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new SpellingBeeGame(speech), config);
    }

    /** Command-line check of the speech engine, no window: java -jar SpellingBee.jar --speech-test */
    private static void speechTest(SpeechService speech) {
        System.out.println(speech.getStatus());
        System.out.println("English voices found: " + speech.getVoices().size());
        speech.speakSequence(java.util.Arrays.asList("necessary", "Needed in order to achieve a result."), 0.85f);
        long deadline = System.currentTimeMillis() + 15000;
        try {
            Thread.sleep(300);
            while (speech.isSpeaking() && System.currentTimeMillis() < deadline) Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        System.out.println(speech.isSpeaking() ? "Timed out while speaking." : "Done speaking.");
        speech.dispose();
    }
}
