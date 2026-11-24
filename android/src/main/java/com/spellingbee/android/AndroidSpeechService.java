package com.spellingbee.android;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.spellingbee.speech.SpeechService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Android / Fire OS text-to-speech. The engine starts asynchronously; anything
 * requested before it is ready is remembered and spoken once it is.
 */
public class AndroidSpeechService implements SpeechService {
    private static final long PAUSE_BETWEEN_PARTS_MS = 600;

    private final TextToSpeech tts;
    private final AtomicInteger pendingUtterances = new AtomicInteger();
    private volatile boolean ready;
    private volatile boolean failed;
    private volatile String status = "Speech: starting...";
    private List<String> pendingParts;
    private float pendingRate = 1f;
    private String pendingVoice;
    private int utteranceCounter;

    public AndroidSpeechService(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this::onInit);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) { pendingUtterances.decrementAndGet(); }
            @Override public void onError(String id) { pendingUtterances.decrementAndGet(); }
            @Override public void onStop(String id, boolean interrupted) { pendingUtterances.set(0); }
        });
    }

    private synchronized void onInit(int result) {
        if (result != TextToSpeech.SUCCESS) {
            failed = true;
            status = "No text-to-speech engine. Check Settings > Accessibility > Text-to-speech.";
            return;
        }
        int lang = tts.setLanguage(Locale.US);
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            lang = tts.setLanguage(Locale.ENGLISH);
        }
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            status = "Speech: English voice data missing; install it in the text-to-speech settings.";
        } else {
            status = "Speech: " + (tts.getDefaultEngine() == null ? "system" : tts.getDefaultEngine());
        }
        ready = true;
        if (pendingVoice != null) applyVoice(pendingVoice);
        if (pendingParts != null) {
            List<String> parts = pendingParts;
            pendingParts = null;
            speakNow(parts, pendingRate);
        }
    }

    @Override
    public void speak(String text) {
        speak(text, 1f);
    }

    @Override
    public void speak(String text, float rate) {
        speakSequence(Collections.singletonList(text), rate);
    }

    @Override
    public synchronized void speakSequence(List<String> parts, float rate) {
        if (failed) return;
        if (!ready) {
            pendingParts = new ArrayList<>(parts);
            pendingRate = rate;
            return;
        }
        speakNow(parts, rate);
    }

    private void speakNow(List<String> parts, float rate) {
        tts.setSpeechRate(rate);
        int mode = TextToSpeech.QUEUE_FLUSH; // the first part cancels whatever is playing
        pendingUtterances.set(0);
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                pendingUtterances.incrementAndGet();
                tts.playSilentUtterance(PAUSE_BETWEEN_PARTS_MS, TextToSpeech.QUEUE_ADD, nextId());
            }
            pendingUtterances.incrementAndGet();
            tts.speak(parts.get(i), mode, new Bundle(), nextId());
            mode = TextToSpeech.QUEUE_ADD;
        }
    }

    private String nextId() {
        return "u" + (utteranceCounter++);
    }

    @Override
    public synchronized void stop() {
        pendingParts = null;
        if (ready) tts.stop();
        pendingUtterances.set(0);
    }

    @Override
    public boolean isSpeaking() {
        return pendingUtterances.get() > 0 || (ready && tts.isSpeaking());
    }

    @Override
    public boolean isAvailable() {
        return !failed;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public synchronized List<String> getVoices() {
        List<String> names = new ArrayList<>();
        if (!ready) return names;
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return names;
            for (Voice v : voices) {
                Locale l = v.getLocale();
                if (l != null && "en".equals(l.getLanguage()) && !v.isNetworkConnectionRequired()) {
                    names.add(v.getName());
                }
            }
        } catch (RuntimeException e) {
            // Some engines (older Fire OS builds) throw instead of returning null.
        }
        Collections.sort(names);
        return names;
    }

    @Override
    public synchronized void setVoice(String voice) {
        if (!ready) {
            pendingVoice = voice;
            return;
        }
        applyVoice(voice);
    }

    private void applyVoice(String name) {
        try {
            if (name == null) {
                tts.setLanguage(Locale.US);
                return;
            }
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return;
            for (Voice v : voices) {
                if (v.getName().equals(name)) {
                    tts.setVoice(v);
                    return;
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void dispose() {
        tts.stop();
        tts.shutdown();
    }
}
