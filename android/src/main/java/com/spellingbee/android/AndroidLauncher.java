package com.spellingbee.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.spellingbee.SpellingBeeGame;


/** Android / Fire OS entry point. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.useWakelock = true; // keep the screen on during a session
        initialize(new SpellingBeeGame(new AndroidSpeechService(this)), config);
    }
}
