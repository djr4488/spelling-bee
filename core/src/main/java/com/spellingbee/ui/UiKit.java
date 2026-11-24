package com.spellingbee.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

/**
 * Builds the Scene2D skin in code: FreeType fonts rendered at the screen's real
 * pixel density (so text stays crisp on a tablet) and rounded-rectangle
 * drawables tinted from {@link Palette}.
 */
public class UiKit implements Disposable {
    /** Virtual screen size; the FitViewport scales this to the window. */
    public static final float WIDTH = 1280;
    public static final float HEIGHT = 800;

    public final Skin skin = new Skin();
    private Texture roundTexture;
    private Texture pixelTexture;

    public UiKit() {
        float density = Math.max(1f, Math.min(Gdx.graphics.getWidth() / WIDTH, Gdx.graphics.getHeight() / HEIGHT));
        FreeTypeFontGenerator regular = new FreeTypeFontGenerator(Gdx.files.internal("fonts/DejaVuSans.ttf"));
        FreeTypeFontGenerator bold = new FreeTypeFontGenerator(Gdx.files.internal("fonts/DejaVuSans-Bold.ttf"));
        skin.add("small", font(regular, 22, density, false));
        skin.add("default", font(regular, 28, density, false));
        skin.add("bold", font(bold, 30, density, false));
        skin.add("big", font(bold, 44, density, true));
        skin.add("title", font(bold, 64, density, false));
        skin.add("answer", font(bold, 56, density, true));
        regular.dispose();
        bold.dispose();

        buildDrawables();
        buildStyles();
    }

    private static BitmapFont font(FreeTypeFontGenerator gen, int size, float density, boolean markup) {
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = Math.round(size * density);
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "ñéáíóú•✓✗←";
        BitmapFont f = gen.generateFont(p);
        f.getData().setScale(1f / density);
        f.setUseIntegerPositions(false);
        f.getData().markupEnabled = markup;
        return f;
    }

    private void buildDrawables() {
        int size = 48, radius = 18;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fillRectangle(radius, 0, size - 2 * radius, size);
        pm.fillRectangle(0, radius, size, size - 2 * radius);
        pm.fillCircle(radius, radius, radius);
        pm.fillCircle(size - radius - 1, radius, radius);
        pm.fillCircle(radius, size - radius - 1, radius);
        pm.fillCircle(size - radius - 1, size - radius - 1, radius);
        roundTexture = new Texture(pm);
        roundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();

        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixelTexture = new Texture(px);
        px.dispose();

        NinePatch round = new NinePatch(roundTexture, radius, radius, radius, radius);
        skin.add("round", new NinePatchDrawable(round), Drawable.class);
        skin.add("pixel", new TextureRegionDrawable(pixelTexture), Drawable.class);
    }

    /** A rounded rectangle in the given color with comfortable padding. */
    public Drawable round(Color color) {
        NinePatchDrawable d = (NinePatchDrawable) skin.newDrawable("round", color);
        d.setLeftWidth(20);
        d.setRightWidth(20);
        d.setTopHeight(12);
        d.setBottomHeight(12);
        return d;
    }

    public Drawable solid(Color color) {
        return skin.newDrawable("pixel", color);
    }

    private void buildStyles() {
        skin.add("default", new Label.LabelStyle(skin.getFont("default"), Palette.TEXT));
        skin.add("small", new Label.LabelStyle(skin.getFont("small"), Palette.MUTED));
        skin.add("bold", new Label.LabelStyle(skin.getFont("bold"), Palette.TEXT));
        skin.add("big", new Label.LabelStyle(skin.getFont("big"), Palette.TEXT));
        skin.add("title", new Label.LabelStyle(skin.getFont("title"), Palette.TEXT));
        // White base color: text is colored with markup or Label.setColor (both multiply with this).
        skin.add("answer", new Label.LabelStyle(skin.getFont("answer"), Color.WHITE));
        skin.add("tint-default", new Label.LabelStyle(skin.getFont("default"), Color.WHITE));
        skin.add("tint-bold", new Label.LabelStyle(skin.getFont("bold"), Color.WHITE));
        skin.add("tint-big", new Label.LabelStyle(skin.getFont("big"), Color.WHITE));

        skin.add("default", buttonStyle("bold", Palette.BUTTON, Palette.BUTTON_DOWN, Color.WHITE));
        skin.add("primary", buttonStyle("big", Palette.HONEY, Palette.HONEY_DARK, Palette.TEXT));
        skin.add("key", buttonStyle("bold", Palette.KEY, Palette.KEY_DOWN, Palette.TEXT));
        skin.add("hint", buttonStyle("default", Palette.BUTTON, Palette.BUTTON_DOWN, Color.WHITE));
        skin.add("correct", buttonStyle("bold", Palette.CORRECT, Palette.CORRECT.cpy().mul(0.8f, 0.8f, 0.8f, 1f), Color.WHITE));

        TextButton.TextButtonStyle toggle = buttonStyle("bold", Palette.KEY, Palette.KEY_DOWN, Palette.TEXT);
        toggle.checked = round(Palette.HONEY);
        skin.add("toggle", toggle);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScrollKnob = solid(Palette.DISABLED);
        skin.add("default", scroll);
    }

    private TextButton.TextButtonStyle buttonStyle(String font, Color up, Color down, Color text) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = skin.getFont(font);
        s.up = round(up);
        s.down = round(down);
        s.disabled = round(Palette.DISABLED);
        s.fontColor = text;
        s.disabledFontColor = Color.WHITE;
        return s;
    }

    @Override
    public void dispose() {
        skin.dispose();
        roundTexture.dispose();
        pixelTexture.dispose();
    }
}
