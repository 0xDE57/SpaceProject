package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.SnapshotArray;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.spaceproject.config.Config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class KeyConfigTab extends ConfigTab {
    
    private static ObjectIntMap<String> keyNames;
    private Map<String, String> changes;
    
    public KeyConfigTab(Stage stage, Config config, String title) {
        super(stage, config, title);
        changes = new HashMap<>();
    }
    
    @Override
    public void buildTab(Object config) {
        buildKeyMap();
        Array<String> keys = keyNames.keys().toArray();
        
        try {
            for (Field f : config.getClass().getFields()) {
                final String key = Input.Keys.toString((Integer) f.get(config));
                Label keyLabel = new Label(f.getName(), VisUI.getSkin());
                keyLabel.setAlignment(Align.right);
                
                final VisSelectBox<String> testKeys = new VisSelectBox<>();
                testKeys.setName(f.getName());
                    /*//TODO: set selected index/highlight key that was pressed when drop down activated
                    testKeys.addListener(new InputListener() {
                        @Override
                        public boolean keyDown(InputEvent event, int keycode) {
                            String keyPressed = Input.Keys.toString(keycode);
                            if (testKeys.getItems().contains(keyPressed, false)) {
                                testKeys.setSelected(keyPressed);
                            }
                            return super.keyDown(event, keycode);
                        }
                    });*/
                testKeys.getScrollPane().addListener(autoFocusScroll(testKeys));
                
                
                testKeys.setItems(keys);
                testKeys.setSelected(key);
                
                testKeys.addListener(keyChanged(testKeys));
                
                scrollContainer.add(keyLabel).expand().pad(padSize).fill();
                scrollContainer.add(testKeys).expand().pad(padSize).fill();
                scrollContainer.row();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            scrollContainer.add(new Label("Failed to reflect field: " + e.getMessage(), VisUI.getSkin()));
        }
    }
    
    private void buildKeyMap() {
        Array<Integer> allowed = new Array<>();
        //allowed.add(Input.Buttons.LEFT);
        //allowed.add(Input.Buttons.RIGHT);
        //allowed.add(Input.Buttons.MIDDLE);
        
        allowed.add(Input.Keys.A);
        allowed.add(Input.Keys.B);
        allowed.add(Input.Keys.C);
        allowed.add(Input.Keys.D);
        allowed.add(Input.Keys.E);
        allowed.add(Input.Keys.F);
        allowed.add(Input.Keys.G);
        allowed.add(Input.Keys.H);
        allowed.add(Input.Keys.I);
        allowed.add(Input.Keys.J);
        allowed.add(Input.Keys.K);
        allowed.add(Input.Keys.L);
        allowed.add(Input.Keys.M);
        allowed.add(Input.Keys.N);
        allowed.add(Input.Keys.O);
        allowed.add(Input.Keys.P);
        allowed.add(Input.Keys.Q);
        allowed.add(Input.Keys.R);
        allowed.add(Input.Keys.S);
        allowed.add(Input.Keys.T);
        allowed.add(Input.Keys.U);
        allowed.add(Input.Keys.V);
        allowed.add(Input.Keys.W);
        allowed.add(Input.Keys.X);
        allowed.add(Input.Keys.Y);
        allowed.add(Input.Keys.Z);
        
        allowed.add(Input.Keys.UP);
        allowed.add(Input.Keys.DOWN);
        allowed.add(Input.Keys.LEFT);
        allowed.add(Input.Keys.RIGHT);
        
        allowed.add(Input.Keys.ALT_LEFT);
        allowed.add(Input.Keys.ALT_RIGHT);
        allowed.add(Input.Keys.CONTROL_LEFT);
        allowed.add(Input.Keys.CONTROL_RIGHT);
        allowed.add(Input.Keys.SHIFT_LEFT);
        allowed.add(Input.Keys.SHIFT_RIGHT);
        allowed.add(Input.Keys.SPACE);
        
        allowed.add(Input.Keys.PLUS);
        allowed.add(Input.Keys.MINUS);
        allowed.add(Input.Keys.BACKSPACE);
        allowed.add(Input.Keys.LEFT_BRACKET);
        allowed.add(Input.Keys.RIGHT_BRACKET);
        allowed.add(Input.Keys.BACKSLASH);
        allowed.add(Input.Keys.SEMICOLON);
        allowed.add(Input.Keys.COMMA);
        allowed.add(Input.Keys.PERIOD);
        allowed.add(Input.Keys.SLASH);
        
        allowed.add(Input.Keys.NUM_0);
        allowed.add(Input.Keys.NUM_1);
        allowed.add(Input.Keys.NUM_2);
        allowed.add(Input.Keys.NUM_3);
        allowed.add(Input.Keys.NUM_4);
        allowed.add(Input.Keys.NUM_5);
        allowed.add(Input.Keys.NUM_6);
        allowed.add(Input.Keys.NUM_7);
        allowed.add(Input.Keys.NUM_8);
        allowed.add(Input.Keys.NUM_9);
        
        allowed.add(Input.Keys.NUMPAD_0);
        allowed.add(Input.Keys.NUMPAD_1);
        allowed.add(Input.Keys.NUMPAD_2);
        allowed.add(Input.Keys.NUMPAD_3);
        allowed.add(Input.Keys.NUMPAD_4);
        allowed.add(Input.Keys.NUMPAD_5);
        allowed.add(Input.Keys.NUMPAD_6);
        allowed.add(Input.Keys.NUMPAD_7);
        allowed.add(Input.Keys.NUMPAD_8);
        allowed.add(Input.Keys.NUMPAD_9);
        
        allowed.add(Input.Keys.F1);
        allowed.add(Input.Keys.F2);
        allowed.add(Input.Keys.F3);
        allowed.add(Input.Keys.F4);
        allowed.add(Input.Keys.F5);
        allowed.add(Input.Keys.F6);
        allowed.add(Input.Keys.F7);
        allowed.add(Input.Keys.F8);
        allowed.add(Input.Keys.F9);
        allowed.add(Input.Keys.F10);
        allowed.add(Input.Keys.F11);
        allowed.add(Input.Keys.F12);
        
        
        if (keyNames == null) {
            keyNames = new ObjectIntMap<>();
            //for (int i : allowed) {
            for (int i = 0; i < 256; i++) {
                String keyName = Input.Keys.toString(i);
                if (keyName != null) keyNames.put(keyName, i);
            }
        }
    }
    
    @Override
    public void saveChanges() {
        Gdx.app.log(this.getClass().getSimpleName(), "keyconfig save");
        
        for (Field f : localConfig.getClass().getFields()) {
            String fieldName = f.getName();
            for (Map.Entry<String, String> kvp : changes.entrySet()) {
                if (fieldName.equals(kvp.getKey())) {
                    try {
                        f.set(localConfig, Input.Keys.valueOf(kvp.getValue()));
                        Gdx.app.log(this.getClass().getSimpleName(), "set " + f.getName() + " to " + kvp.getValue());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    
                }
            }
        }
        changes.clear();
        setDirty(false);
    }
    
    @Override
    public void discardChanges() {
        Gdx.app.log(this.getClass().getSimpleName(), "keyconfig discard");
        SnapshotArray<Actor> children = scrollContainer.getChildren();
        for (Map.Entry<String, String> kvp : changes.entrySet()) {
            for (Actor child : children) {
                if (child instanceof VisSelectBox) {
                    if (child.getName().equals(kvp.getKey())) {
                        try {
                            Field field = localConfig.getClass().getField(kvp.getKey());
                            String key = Input.Keys.toString((Integer) field.get(localConfig));
                            ((VisSelectBox) child).setSelected(key);
                            Gdx.app.log(this.getClass().getSimpleName(), "reset " + child.getName() + " with " + kvp.getValue());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
        
        changes.clear();
        setDirty(false);
    }
    
    private EventListener autoFocusScroll(final VisSelectBox<String> testKeys) {
        return new EventListener() {
            //auto focus scroll on mouse enter
            //credit: Ecumene
            //java-gaming.org/index.php?topic=38333.0
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent)
                    if (((InputEvent) event).getType() == InputEvent.Type.enter)
                        event.getStage().setScrollFocus(testKeys.getScrollPane());
                return false;
            }
        };
    }
    
    private ChangeListener keyChanged(final VisSelectBox<String> testKeys) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //check if key already assigned to another action
                boolean keyInUse = false;
                String usedBy = "null";
                String selected = (String) ((VisSelectBox) actor).getSelected();
                for (Actor other : scrollContainer.getChildren()) {
                    if (other instanceof VisSelectBox) {
                        if (!other.equals(testKeys)) {
                            if ((((VisSelectBox) other).getSelected()).equals(selected)) {
                                usedBy = other.getName();
                                keyInUse = true;
                                break;
                            }
                        }
                    }
                }
                if (keyInUse) {
                    Gdx.app.log(this.getClass().getSimpleName(), "Key already in use by: " + usedBy);
                    //TODO: highlight control / notify used key X used by setting Y
                    event.cancel();
                } else {
                    Gdx.app.log(this.getClass().getSimpleName(), "Key is free to use");
                    changes.put(actor.getName(), selected);
                    dirty();
                }
            }
        };
    }
}