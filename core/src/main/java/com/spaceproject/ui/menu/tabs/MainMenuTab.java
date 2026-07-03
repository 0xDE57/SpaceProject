package com.spaceproject.ui.menu.tabs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.ui.menu.MyDialogs;

import static com.spaceproject.screens.MyScreenAdapter.game;

public class MainMenuTab extends HotKeyTab {
    
    private int itemPadding = 4;
    
    public MainMenuTab(final GameMenu gameMenu) {
        super("menu", Input.Keys.ESCAPE);

        VisTextButton btnGotoMain = new VisTextButton("main menu");
        btnGotoMain.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showYesNoDialog(GameScreen.getStage(),"exit", "return to main menu?", new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        game.setScreen(new TitleScreen(game));
                    }
                });
            }
        });

        VisTextButton btnLoad = new VisTextButton("load");
        btnLoad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showOKDialog(gameMenu.getStage(), "load", "not implemented yet");
            }
        });

        VisTextButton btnSave = new VisTextButton("save");
        btnSave.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showOKDialog(gameMenu.getStage(), "save", "not implemented yet");
            }
        });

        VisTextButton btnOptions = new VisTextButton("keybinds");
        btnOptions.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                TabbedPane tabbedPane = gameMenu.getTabbedPane();
                Tab keyConfigTab = gameMenu.getKeyConfigTab();
                if (!tabbedPane.getTabs().contains(keyConfigTab, false)) {
                    tabbedPane.add(keyConfigTab);
                }
                tabbedPane.switchTab(keyConfigTab);
            }
        });
    
        VisTextButton btnExit = new VisTextButton("exit");
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                MyDialogs.showYesNoDialog(gameMenu.getStage(), "exit", "good bye?", new OptionDialogAdapter() {
                    @Override
                    public void yes() {
                        Gdx.app.exit();
                    }
                });
            }
        });
    
        btnLoad.setDisabled(true);
        btnSave.setDisabled(true);
        getContentTable().add(btnGotoMain).growX().pad(itemPadding).row();
        getContentTable().add(btnSave).growX().pad(itemPadding).row();
        getContentTable().add(btnLoad).growX().pad(itemPadding).row();
        getContentTable().add(btnOptions).growX().pad(itemPadding).row();
        getContentTable().add(btnExit).growX().pad(itemPadding).row();
    }
    
}
