package com.spaceproject.ui.debug.nodes;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.kotcrab.vis.ui.widget.VisLabel;

import java.lang.reflect.Field;


public class FieldNode extends UpdateNode {
    
    private Object owner;
    
    public FieldNode(Actor actor, Object owner, Field field) {
        super(actor, field);
        this.owner = owner;
        getActor().setName(toString());
        update();
    }
    
    private Object getOwner() {
        return owner;
    }
    
    @Override
    public void update() {
        ((VisLabel) getActor()).setText(toString());
    }
    
    @Override
    public String toString() {
        if (getValue() == null || getOwner() == null) {
            return super.toString();
        }
        try {
            return String.format("%-14s %s", ((Field) getValue()).getName(), ((Field) getValue()).get(getOwner()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

}
