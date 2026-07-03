package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class TouchButtonRectangle {
    Color color;
    Color colorTouched;
    int x, y;
    int width, height;
    public boolean hidden = false;
    public boolean enabled = true;
    
    public TouchButtonRectangle(int x, int y, int width, int height, Color color, Color pressed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.colorTouched = pressed;
    }
    
    public void render(ShapeRenderer shape) {
        if (hidden) {
            return;
        }
        shape.setColor(isTouched() ? colorTouched : color);
        shape.rect(x, y, width, height);
    }
    
    public boolean isTouched() {
        if (!enabled) {
            return false;
        }
        
        float pad = 40f;//let finger be slightly outside of button
        
        // finger 0
        int finger0X = Gdx.input.getX(0);
        int finger0Y = Gdx.graphics.getHeight() - Gdx.input.getY(0);
        boolean finger0 = finger0X > x - pad && finger0X < x + width + pad && finger0Y > y - pad && finger0Y < y + height + pad;
        
        // finger 1
        int finger1X = Gdx.input.getX(1);
        int finger1Y = Gdx.graphics.getHeight() - Gdx.input.getY(1);
        boolean finger1 = finger1X > x - pad && finger1X < x + width + pad && finger1Y > y - pad && finger1Y < y + height + pad;
        
        return (Gdx.input.isTouched(0) && finger0 || Gdx.input.isTouched(1) && finger1);
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
