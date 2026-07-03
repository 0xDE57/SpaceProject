package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.spaceproject.ui.menu.GameMenu;

public class ConfigManagerTab extends HotKeyTab {
    
    private int itemPadding = 2;
    
    public ConfigManagerTab(final GameMenu gameMenu) {
        super("Config Manager", Input.Keys.UNKNOWN);
        
        
        TextButton editButton = new TextButton("view/edit", VisUI.getSkin());
        editButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //Dialogs.showOKDialog(gameMenu.getStage(), "test", "not implemented yet");
            }
        });
        
        
        TextButton btnLoad = new TextButton("load all", VisUI.getSkin());
        btnLoad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(gameMenu.getStage(), "load", "not implemented yet");
            }
        });
        
        
        TextButton btnSave = new TextButton("save", VisUI.getSkin());
        btnSave.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(gameMenu.getStage(), "save", "not implemented yet");
            }
        });
        
        
        
        TextButton btnExit = new TextButton("exit", VisUI.getSkin());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(gameMenu.getStage(), "test", "not implemented yet");
            }
        });
        
        
        getContentTable().add(editButton).growX().pad(itemPadding).row();
        getContentTable().add(btnSave).growX().pad(itemPadding).row();
        getContentTable().add(btnLoad).growX().pad(itemPadding).row();
        getContentTable().add(btnExit).growX().pad(itemPadding).row();
    }
    
}
