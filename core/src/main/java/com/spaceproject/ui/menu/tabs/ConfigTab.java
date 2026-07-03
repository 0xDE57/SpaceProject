package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.dialog.Dialogs;
import com.kotcrab.vis.ui.widget.Separator;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.spinner.IntSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.SimpleFloatSpinnerModel;
import com.kotcrab.vis.ui.widget.spinner.Spinner;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.spaceproject.config.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class ConfigTab extends Tab {
    Config localConfig;
    private String title;
    private Table content;
    VisTable scrollContainer;
    int padSize = 2;
    
    public ConfigTab(Stage stage, Config config) {
        this(stage, config, config.getClass().getSimpleName());
    }
    
    public ConfigTab(final Stage stage, Config config, String title) {
        super(true, true);
        this.title = title;
        this.localConfig = config;
        
        content = new VisTable();
        content.setFillParent(true);
        
        scrollContainer = new VisTable();
        scrollContainer.left().top();
        
        buildTab(localConfig);
        
        final VisScrollPane scrollPane = new VisScrollPane(scrollContainer);
        scrollPane.addListener(autoFocusScroll(scrollPane));
        scrollPane.setFlickScroll(false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, false);
        
        getContentTable().add(scrollPane).left().top().expand().fill();
        getContentTable().row();
        
        getContentTable().add(new Separator()).fillX().row();
    
        TextButton btnSave = new TextButton("save", VisUI.getSkin());
        btnSave.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(stage, "save", "not implemented yet");
            }
        });
        
        TextButton btnUndo = new TextButton("undo", VisUI.getSkin());
        btnUndo.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(stage, "undo", "not implemented yet");
            }
        });
        
        TextButton btnReset = new TextButton("reset", VisUI.getSkin());
        btnReset.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Dialogs.showOKDialog(stage, "reset", "not implemented yet");
            }
        });
        
        
        HorizontalGroup hg = new HorizontalGroup();
        hg.addActor(btnSave);
        hg.addActor(btnUndo);
        hg.addActor(btnReset);
        getContentTable().add(hg);
    }
    
    
    private EventListener autoFocusScroll(final VisScrollPane scrollPane) {
        return new EventListener() {
            //auto focus scroll on mouse enter
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent)
                    if (((InputEvent) event).getType() == InputEvent.Type.enter)
                        event.getStage().setScrollFocus(scrollPane);
                return false;
            }
        };
    }
    
    public void buildTab(Object config) {
        try {
            for (Field f : config.getClass().getFields()) {
                String fieldName = f.getName();
                Type t = f.getType();
                Actor a;
                

                if (t == Integer.TYPE) {
                    a = new Spinner(fieldName, new IntSpinnerModel((Integer) f.get(config), Integer.MIN_VALUE, Integer.MAX_VALUE));
                } else if (t == Float.TYPE) {
                    a = new Spinner(fieldName, new SimpleFloatSpinnerModel((Float) f.get(config), -Float.MAX_VALUE, Float.MAX_VALUE, 0.1f, 4));
                } else if (t == Boolean.TYPE) {
                    a = new CheckBox(fieldName, VisUI.getSkin());
                    ((CheckBox) a).setChecked((Boolean) f.get(config));
                } else {
                    a = new Label(String.format("%s %s", fieldName, f.get(config)), VisUI.getSkin());
                }
            
                if (a != null) {
                    scrollContainer.add(a).expand().fill().pad(padSize).row();
                } else {
                    scrollContainer.add(new Label("Failed to reflect field: " + fieldName, VisUI.getSkin()));
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        }
    }
    
    @Override
    public void onHide() {
        //Gdx.app.log(this.getClass().getSimpleName(), "configtab hide");
        if (isDirty()) {
            TabbedPane pane = getPane();
            if (pane != null) {
                pane.switchTab(this);//keep focus on this
                pane.remove(this, false);//trigger save changes dialog
            } else {
                discardChanges();
            }
        } else {
            super.onHide();
        }
    }
    
    @Override
    public String getTabTitle() {
        return title;
    }
    
    @Override
    public Table getContentTable() {
        return content;
    }
    
    @Override
    public boolean save() {
        saveChanges();
        return super.save();
    }
    
    public void saveChanges() {
    
    }
    
    public void discardChanges() {
    
    }
    
}