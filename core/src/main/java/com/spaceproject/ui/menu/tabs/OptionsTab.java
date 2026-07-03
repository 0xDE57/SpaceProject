package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.systems.CameraSystem;
import com.spaceproject.systems.ControllerInputSystem;
import com.spaceproject.systems.HUDSystem;
import com.spaceproject.systems.SoundSystem;

//Controller tab?
public class OptionsTab extends Tab {
    private String title;
    private Table content;

    final VisCheckBox toggleFullscreen;
    final VisCheckBox toggleVsync;
    final VisCheckBox toggleMSAA;
    GameScreen game;

    public OptionsTab(String title, GameScreen game) {
        super(false, false);
        this.game = game;
        this.title = title;
        content = new VisTable();
        content.setFillParent(true);
        EngineConfig engineConfig = SpaceProject.configManager.getConfig(EngineConfig.class);
        KeyConfig keyConfig = SpaceProject.configManager.getConfig(KeyConfig.class);

        toggleFullscreen = new VisCheckBox("fullscreen [" + Input.Keys.toString(keyConfig.fullscreen) + "]", Gdx.graphics.isFullscreen());
        toggleFullscreen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (toggleFullscreen.isChecked()) {
                    game.setFullscreen();
                } else {
                    game.setWindowedMode();
                }
            }
        });

        toggleVsync = new VisCheckBox("vsync ["+ Input.Keys.toString(keyConfig.vsync) + "]", engineConfig.vsync);
        toggleVsync.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setVsync(toggleVsync.isChecked());
            }
        });

        toggleMSAA = new VisCheckBox("MSAA ["+ Input.Keys.toString(keyConfig.msaa) + "]", game.isMSAAEnabled());
        toggleMSAA.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (toggleMSAA.isChecked()) {
                    game.enableMSAA();
                } else {
                    game.disableMSAA();
                }
                Gdx.app.log(getClass().getSimpleName(), "MSAA: " + game.isMSAAEnabled());
            }
        });

        final VisLabel volumeText = new VisLabel("volume: 100");
        final VisSlider volumeSlider = new VisSlider(0, 1, 0.1f, false);
        volumeSlider.setValue(volumeSlider.getMaxValue());
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float volume = volumeSlider.getValue();
                GameScreen.getEngine().getSystem(SoundSystem.class).setVolume(volume);
                volumeText.setText("volume: " + (MathUtils.isEqual(volume, 0) ? "OFF" : (int)(MyMath.round(volume,1)*100)));
            }
        });


        final VisCheckBox toggleDamageNumbers = new VisCheckBox("show damage numbers (experimental)", HUDSystem.showDamageNumbers);
        toggleDamageNumbers.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                HUDSystem.showDamageNumbers = toggleDamageNumbers.isChecked();
            }
        });

        final VisCheckBox toggleCameraLerp = new VisCheckBox("lerp camera", GameScreen.getEngine().getSystem(CameraSystem.class).isLerp());
        toggleCameraLerp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameScreen.getEngine().getSystem(CameraSystem.class).setLerp(toggleCameraLerp.isChecked());
                //debugCFG.lerpCam = toggleCameraLerp.isChecked();
            }
        });

        final VisCheckBox toggleControllerVibrate = new VisCheckBox("controller vibrate", true);
        toggleControllerVibrate.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameScreen.getEngine().getSystem(ControllerInputSystem.class).setVibrateEnable(toggleControllerVibrate.isChecked());
            }
        });


        content.add(volumeText).padTop(6).row();
        content.add(volumeSlider).fillX().padBottom(6).row();
        content.add(toggleFullscreen).left().row();
        content.add(toggleVsync).left().row();
        content.add(toggleMSAA).left().row();
        int pad = 10;
        //getContentTable().add(new Separator()).fillX().padTop(pad).padBottom(pad).row();
        //getContentTable().add(new Separator()).fillX().padTop(pad).padBottom(pad).row();
        content.add(toggleDamageNumbers).left().row();
        content.add(toggleCameraLerp).left().row();
        content.add(toggleControllerVibrate).left().row();
        content.pack();

        /*
        getContentTable().addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                updateSettings();
                return super.touchDown(event, x, y, pointer, button);
            }
        });*/
    }

    public void updateSettings() {
        toggleFullscreen.setChecked(Gdx.graphics.isFullscreen());
        toggleMSAA.setChecked(game.isMSAAEnabled());
        //toggleVsync.setChecked(game.getVsync());
        toggleVsync.setChecked(SpaceProject.configManager.getConfig(EngineConfig.class).vsync);
    }

    @Override
    public void onShow() {
        super.onShow();
        updateSettings();
    }

    @Override
    public String getTabTitle() {
        return title;
    }

    @Override
    public Table getContentTable() {
        return content;
    }

}
