package com.spellingbee.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** A large-key QWERTY letter keyboard that works the same with touch and mouse. */
public class OnScreenKeyboard extends Table {
    public interface Listener {
        void onChar(char c);

        void onBackspace();

        void onEnter();
    }

    private static final String[] ROWS = {"QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"};
    private static final float KEY_W = 100, KEY_H = 84, GAP = 4;

    private final TextButton enterKey;

    public OnScreenKeyboard(Skin skin, Listener listener) {
        defaults().pad(GAP);
        for (int r = 0; r < ROWS.length; r++) {
            Table row = new Table();
            row.defaults().pad(GAP).width(KEY_W).height(KEY_H);
            for (char c : ROWS[r].toCharArray()) {
                final char lower = Character.toLowerCase(c);
                row.add(key(skin, String.valueOf(c), "key", () -> listener.onChar(lower)));
            }
            if (r == 2) {
                row.add(key(skin, "Delete", "default", listener::onBackspace)).width(KEY_W * 2 + GAP * 2);
            }
            add(row).row();
        }
        Table bottom = new Table();
        bottom.defaults().pad(GAP).height(KEY_H);
        bottom.add(key(skin, "-  hyphen", "key", () -> listener.onChar('-'))).width(KEY_W * 2 + GAP * 2);
        bottom.add(key(skin, "space", "key", () -> listener.onChar(' '))).width(KEY_W * 4 + GAP * 6);
        enterKey = key(skin, "Check", "correct", listener::onEnter);
        bottom.add(enterKey).width(KEY_W * 3 + GAP * 4);
        add(bottom).row();
    }

    private static TextButton key(Skin skin, String text, String style, Runnable action) {
        TextButton b = new TextButton(text, skin, style);
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return b;
    }
}
