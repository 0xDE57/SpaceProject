package com.spaceproject.ui.menu;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.spaceproject.SpaceProject;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.debug.*;
import com.spaceproject.ui.ControllerMenuStage;

public class TitleScreenMenu {

    private final ControllerMenuStage stage;
    public VisTable table;

    public TitleScreenMenu(final ControllerMenuStage stage, final SpaceProject game, boolean showDebugScreens) {
        this.stage = stage;
        table = new VisTable();

        if (showDebugScreens) {
            addDebugItems(game);
        }
    
        addMenuItems(game);
        resetFocusableActors();
    }

    private void addMenuItems(final SpaceProject game) {
        VisTextButton btnPlay = new VisTextButton("play");
        btnPlay.getLabel().setAlignment(Align.left);
        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen());
            }
        });

        VisTextButton btnLoad = new VisTextButton("load");
        btnLoad.getLabel().setAlignment(Align.left);
        btnLoad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                VisDialog dialog = MyDialogs.showOKDialog(stage, "load", "not implemented yet");
                dialog.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        System.out.println("changed?");
                    }
                });
            }
        });

        VisTextButton btnOption = new VisTextButton("options");
        btnOption.getLabel().setAlignment(Align.left);
        btnOption.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showMultiDialog(stage, "[multibuttondialog]",  "[WASD] or arrow keys or [D-Pad] or Left-Joystick to navigate");
            }
        });

        VisTextButton btnAbout = new VisTextButton("about");
        btnAbout.getLabel().setAlignment(Align.left);
        btnAbout.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showAboutDialog(stage);
            }
        });

        VisTextButton btnExit = new VisTextButton("exit");
        btnExit.getLabel().setAlignment(Align.left);

        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showYesNoDialog(stage, "exit", "goodbye?", new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });
        stage.setEscapeActor(btnExit);

/*
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                final int nothing = 1;
                final int everything = 2;
                final int something = 3;

                //confirmdialog may return result of any type, here we are just using ints
                Dialogs.showConfirmDialog(stage, "confirm dialog", "what do you want?",
                        new String[]{"nothing", "everything", "something"}, new Integer[]{nothing, everything, something},
                        result -> {
                            switch (result) {
                                case nothing: Dialogs.showOKDialog(stage, "result", "pressed: nothing"); break;
                                case everything: Dialogs.showOKDialog(stage, "result", "pressed: everything"); break;
                                case something: Dialogs.showOKDialog(stage, "result", "pressed: something"); break;
                            }
                        });

                MyDialogs.showCustomDialog(stage, "confirm dialog", "what do you want?",
                        new String[]{"nothing", "everything", "something"}, new Integer[]{nothing, everything, something},
                        result -> {
                            switch (result) {
                                case nothing: Dialogs.showOKDialog(stage, "result", "pressed: nothing"); break;
                                case everything: Dialogs.showOKDialog(stage, "result", "pressed: everything"); break;
                                case something: Dialogs.showOKDialog(stage, "result", "pressed: something"); break;
                            }
                        });
                }
        });*/

        //btnLoad.setDisabled(true);
        //btnOption.setDisabled(true);
        table.add(btnPlay).fillX().row();
        //table.add(btnLoad).fillX().row();
        //table.add(btnOption).fillX().row();
        table.add(btnAbout).fillX().row();
        table.add(btnExit).fillX().row();
    }

    public void addDebugItems(final SpaceProject game) {
        VisTextButton btnVoronoi = new VisTextButton("[DEBUG] voronoi");
        btnVoronoi.getLabel().setAlignment(Align.left);
        btnVoronoi.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestVoronoiScreen());
            }
        });

        VisTextButton btnNoise = new VisTextButton("[DEBUG] noise");
        btnNoise.getLabel().setAlignment(Align.left);
        btnNoise.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestNoiseScreen());
            }
        });

        VisTextButton btnShip = new VisTextButton("[DEBUG] ship gen");
        btnShip.getLabel().setAlignment(Align.left);
        btnShip.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TestShipGenerationScreen());
            }
        });

        VisTextButton btn3D = new VisTextButton("[DEBUG] 3D rotate");
        btn3D.getLabel().setAlignment(Align.left);
        btn3D.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new Test3DScreen());
            }
        });

        table.add(btnVoronoi).fillX().row();
        table.add(btnNoise).fillX().row();
        table.add(btn3D).fillX().row();
        table.add(btnShip).fillX().row();

        resetFocusableActors();
    }

    private void resetFocusableActors() {
        //stage.setFocusedActor(null); clear focused?
        stage.clearFocusableActors();
        for (Actor button : table.getChildren()) {
            stage.addFocusableActor(button);
        }
    }

}
