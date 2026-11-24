package com.spellingbee.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.spellingbee.SpellingBeeGame;
import com.spellingbee.ui.Palette;
import com.spellingbee.ui.UiKit;

/** A Stage-based screen with a FitViewport and a root table filling it. */
public abstract class BaseScreen extends ScreenAdapter {
    protected final SpellingBeeGame game;
    protected final Stage stage;
    protected final Skin skin;
    protected final Table root = new Table();

    protected BaseScreen(SpellingBeeGame game) {
        this.game = game;
        this.skin = game.ui.skin;
        this.stage = new Stage(new FitViewport(UiKit.WIDTH, UiKit.HEIGHT));
        root.setFillParent(true);
        root.pad(24);
        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                    onBack();
                    return true;
                }
                return false;
            }
        }));
    }

    /** Back button / Escape. Default: return to the menu. */
    protected void onBack() {
        game.showMenu();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(Palette.BACKGROUND.r, Palette.BACKGROUND.g, Palette.BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() != null) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    // ---- small helpers -------------------------------------------------------------------

    protected Label label(String text, String style) {
        return new Label(text, skin, style);
    }

    protected TextButton button(String text, String style, Runnable onClick) {
        TextButton b = new TextButton(text, skin, style);
        onClick(b, onClick);
        return b;
    }

    protected static void onClick(Actor actor, Runnable action) {
        actor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor a) {
                action.run();
            }
        });
    }
}
