package com.spellingbee.speech;

import java.util.List;

/** Used when no speech engine exists. Keeps the app usable without audio. */
public class SilentSpeechService implements SpeechService {
    private final String reason;

    public SilentSpeechService(String reason) {
        this.reason = reason;
    }

    @Override public void speak(String text) { }
    @Override public void speak(String text, float rate) { }
    @Override public void speakSequence(List<String> parts, float rate) { }
    @Override public void stop() { }
    @Override public boolean isSpeaking() { return false; }
    @Override public boolean isAvailable() { return false; }
    @Override public String getStatus() { return reason; }
    @Override public void dispose() { }
}
