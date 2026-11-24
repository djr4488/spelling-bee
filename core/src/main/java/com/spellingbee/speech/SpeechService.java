package com.spellingbee.speech;

import java.util.Collections;
import java.util.List;

/**
 * Text-to-speech, implemented per platform and injected into the game.
 * Every call must return immediately; speaking happens in the background.
 * Starting a new utterance cancels the current one.
 */
public interface SpeechService {

    /** Speaks at the default rate (1.0 = normal). */
    void speak(String text);

    /** Speaks at {@code rate}, where 1.0 is the engine's normal speed. */
    void speak(String text, float rate);

    /**
     * Speaks several utterances in order with a short pause between them,
     * e.g. word, definition, sentence. Cancels anything currently speaking.
     */
    void speakSequence(List<String> parts, float rate);

    void stop();

    boolean isSpeaking();

    /** False when no speech engine could be found or started. */
    boolean isAvailable();

    /** Human-readable status, e.g. which engine is used or what is missing. */
    String getStatus();

    /** Voice names the user can pick from; empty if the platform can't choose. */
    default List<String> getVoices() {
        return Collections.emptyList();
    }

    /** Selects a voice from {@link #getVoices()}; null means the default voice. */
    default void setVoice(String voice) {
    }

    void dispose();
}
