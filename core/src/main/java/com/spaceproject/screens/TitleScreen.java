package com.spaceproject.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.spaceproject.SpaceProject;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.screens.animations.DelaunayAnim;
import com.spaceproject.screens.animations.NBodyGravityAnim;
import com.spaceproject.screens.animations.NoiseAnim;
import com.spaceproject.screens.animations.OrbitAnim;
import com.spaceproject.screens.animations.TitleAnimation;
import com.spaceproject.screens.animations.TreeAnim;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.ui.menu.TitleScreenMenu;

public class TitleScreen extends MyScreenAdapter {
    
    private SpaceProject game;
    private ControllerMenuStage stage;
    private TitleScreenMenu menu;
    private VisTable versionTable;
    private VisLabel titleLabel;
    private int edgePad;
    private Matrix4 projectionMatrix = new Matrix4();
    private boolean debugMenuEnabled = false;

    private TitleAnimation foregroundAnimation, backgroundAnimation;
    private ForegroundAnimation previousAnim;

    enum ForegroundAnimation {
        tree, delaunay, orbit, crossNoise, nbody;/*, asteroid*/;

        static ForegroundAnimation[] VALUES = ForegroundAnimation.values();
        
        public static ForegroundAnimation random() {
            return VALUES[MathUtils.random(VALUES.length - 1)];
        }
        
        public ForegroundAnimation next() {
            int index = (this.ordinal() + 1) % VALUES.length;
            return VALUES[index];
        }

        public ForegroundAnimation previous() {
            int index = (this.ordinal() + VALUES.length - 1) % VALUES.length;
            return VALUES[index];
        }
    }
    
    public TitleScreen(SpaceProject spaceProject) {
        this.game = spaceProject;
        
        //init scene2d and VisUI
        if (VisUI.isLoaded())
            VisUI.dispose(true);
        VisUI.load(VisUI.SkinScale.X2);
        TextButton.TextButtonStyle textButtonStyle = VisUI.getSkin().get(TextButton.TextButtonStyle.class);
        textButtonStyle.focused = textButtonStyle.over; //set focused style to over for keyboard navigation because VisUI default focused style is null!
        VisUI.setDefaultTitleAlign(Align.center);

        stage = new ControllerMenuStage(new ScreenViewport());
        getInputMultiplexer().addProcessor(stage); // instead of Gdx.input.setInputProcessor(stage);
        Controllers.addListener(stage);

        //init fonts
        String titleFont = "titleFontLarge";
        initTitleFont(titleFont);
        String menuFont = "menuFont";
        initMenuFont(menuFont);
        
        titleLabel = new VisLabel(SpaceProject.TITLE, titleFont, Color.WHITE);
        titleLabel.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight(), Align.top);
        stage.addActor(titleLabel);
        
        //menu
        menu = new TitleScreenMenu(stage, game, false);
        stage.addActor(menu.table);
        menu.table.pack();
        edgePad = 10;
        menu.table.setPosition(edgePad, edgePad);
        menu.table.validate();
        //must set one focused actor first, otherwise controller stage won't focus
        //stage.setFocusedActor(menu.table.getChildren().first());

        //version note
        versionTable = new VisTable();
        versionTable.add(new VisLabel(SpaceProject.VERSION, menuFont, Color.WHITE));
        stage.addActor(versionTable);
        versionTable.pack();

        //init animations
        backgroundAnimation = new NoiseAnim();
        initForegroundAnim();
        
        Gdx.graphics.setVSync(true);
    }
    
    public void render(float delta) {
        super.render(delta);
        
        Gdx.gl20.glClearColor(1, 1, 1, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

        cam.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);

        backgroundAnimation.render(delta, shape);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        foregroundAnimation.render(delta, shape);
        
        Gdx.gl.glDisable(GL20.GL_BLEND);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            initForegroundAnim();
        }
        
        if (!debugMenuEnabled && Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            menu.addDebugItems(game);
            menu.table.pack();
            debugMenuEnabled = true;
        }
        /*
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (exitPromptUp) return;
            exitPromptUp = true;
            Dialogs.showOptionDialog(stage, "exit", "goodbye?", Dialogs.OptionDialogType.YES_NO, new OptionDialogAdapter() {
                @Override
                public void yes() {
                    Gdx.app.exit();
                }
                @Override
                public void no () {
                    exitPromptUp = false;
                }
                @Override
                public void cancel () {
                    exitPromptUp = false;
                }
            });
        }*/
    }
    boolean exitPromptUp = false;

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
        versionTable.setPosition(Gdx.graphics.getWidth() - versionTable.getWidth() - edgePad, edgePad);
        titleLabel.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight(), Align.top);

        foregroundAnimation.resize(width, height);
        backgroundAnimation.resize(width, height);
    }

    private void initMenuFont(String menuFont) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 30;
        parameter.borderColor = Color.DARK_GRAY;
        parameter.borderWidth = 1;
        BitmapFont fontComfortaaBold = FontLoader.createFont(FontLoader.fontComfortaaBold, parameter);
        VisUI.getSkin().add(menuFont, fontComfortaaBold);
    }

    private void initTitleFont(String titleFont) {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 90;
        parameter.borderColor = Color.DARK_GRAY;
        parameter.borderWidth = 1;
        BitmapFont fontComfortaaBold = FontLoader.createFont(FontLoader.fontComfortaaBold, parameter);
        VisUI.getSkin().add(titleFont, fontComfortaaBold);
    }
    
    private void initForegroundAnim() {
        ForegroundAnimation anim = ForegroundAnimation.random();
        
        //ensure new anim on refresh
        if (previousAnim != null && anim == previousAnim) {
            anim = anim.next();
        }
    
        //cleanup previous
        if (foregroundAnimation instanceof Disposable)
            ((Disposable)foregroundAnimation).dispose();
        anim = ForegroundAnimation.nbody;
        switch (anim) {
            case delaunay:
                this.foregroundAnimation = new DelaunayAnim();
                break;
            case tree:
                this.foregroundAnimation = new TreeAnim();
                break;
            case orbit:
                this.foregroundAnimation = new OrbitAnim();
                break;
            case crossNoise:
                this.foregroundAnimation = new NoiseAnim(0, 0.01f, 3, 0.013f, true);
                break;
            case nbody:
                this.foregroundAnimation = new NBodyGravityAnim(stage);
                break;
                /*
			case asteroid: /
				this.foregroundAnimation = new AsteroidAnim();
				break;*/
        
        }
        previousAnim = anim;
        
        Gdx.app.debug(getClass().getSimpleName(), "Animation: " + anim);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (foregroundAnimation instanceof Disposable)
            ((Disposable)foregroundAnimation).dispose();

        Controllers.removeListener(stage);

        stage.dispose();
    }

}
