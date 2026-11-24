package com.spellingbee.ui;

import com.badlogic.gdx.graphics.Color;

/** App colors: warm honey theme with high-contrast text. */
public final class Palette {
    public static final Color BACKGROUND = Color.valueOf("FFF8E1");
    public static final Color PANEL = Color.valueOf("FFFFFF");
    public static final Color TEXT = Color.valueOf("2B2B2B");
    public static final Color MUTED = Color.valueOf("6B6458");
    public static final Color HONEY = Color.valueOf("F5B800");
    public static final Color HONEY_DARK = Color.valueOf("C98F00");
    public static final Color BUTTON = Color.valueOf("3D5A80");
    public static final Color BUTTON_DOWN = Color.valueOf("2C4260");
    public static final Color KEY = Color.valueOf("FFFFFF");
    public static final Color KEY_DOWN = Color.valueOf("E0D7C3");
    public static final Color CORRECT = Color.valueOf("2E9E4F");
    public static final Color WRONG = Color.valueOf("D64545");
    public static final Color DISABLED = Color.valueOf("B8B2A6");

    /** Markup color strings for labels with markup enabled. */
    public static final String MARK_OK = "[#2E9E4F]";
    public static final String MARK_BAD = "[#D64545]";

    private Palette() {
    }
}
