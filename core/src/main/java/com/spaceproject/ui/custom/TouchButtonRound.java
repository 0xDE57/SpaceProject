package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spaceproject.math.MyMath;

public class TouchButtonRound {
    Color color;
    Color colorTouched;
    int x, y;
    int radius;
    boolean hidden = false;
    boolean enabled = true;
    
    public TouchButtonRound(int x, int y, int radius, Color color, Color pressed) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.colorTouched = pressed;
    }
    
    public void render(ShapeRenderer shape) {
        
        
        if (hidden) {
            return;
        }
        shape.setColor(isTouched() ? colorTouched : color);
        shape.circle(x - radius, y + radius, radius, 6);
    }
    
    public boolean isTouched() {
        if (!enabled) {
            return false;
        }
        
        // finger 0
        float distFinger0 = MyMath.distance(Gdx.input.getX(0), Gdx.graphics.getHeight() - Gdx.input.getY(0),
                x - radius, y + radius);
        
        // finger 1
        float distFinger1 = MyMath.distance(Gdx.input.getX(1), Gdx.graphics.getHeight() - Gdx.input.getY(1),
                x - radius, y + radius);
        
        float padding = 40f;//let finger be slightly outside of button
        return ((Gdx.input.isTouched(0) && distFinger0 <= radius + padding)
                || (Gdx.input.isTouched(1) && distFinger1 <= radius + padding));
    }
    
    
    boolean touched = false;
    
    public boolean isJustTouched() {
        if (isTouched()) {
            touched = true;
            return false;
        }
        
        if (touched) {
            touched = false;
            return true;
        }
        
        return false;
    }
}