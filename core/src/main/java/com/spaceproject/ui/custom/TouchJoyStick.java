package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.spaceproject.math.MyMath;

public class TouchJoyStick {
    private Color color;
    private Color colorTouched;
    
    private int radius;
    private int x;
    private int y;
    
    //position of stick
    private int stickX = x;
    private int stickY = y;
    
    private float angle;
    //analog movement: how close stick is to edge [0-1]=[center-edge]
    private float ratio;
    
    public TouchJoyStick(int x, int y, int radius, Color color, Color colorTouched) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.colorTouched = colorTouched;
    }
    
    public void render(ShapeRenderer shape) {
        //draw joystick base
        shape.begin(ShapeType.Line);
        shape.setColor(color);
        shape.circle(x, y, radius, 12);
        shape.end();
        
        
        //draw stick on joystick
        shape.begin(ShapeType.Filled);
        shape.setColor(isTouched() ? colorTouched : color);
        shape.circle(stickX, stickY, radius / 5, 6);
        shape.line(x, y, stickX, stickY);
        shape.end();
        
    }
    
    public boolean isTouched() {
        // reset stick to center
        stickX = x;
        stickY = y;
        
        // check finger 0
        float distFinger0 = MyMath.distance(Gdx.input.getX(0), Gdx.graphics.getHeight() - Gdx.input.getY(0), x, y);
        
        // check finger 1
        float distFinger1 = MyMath.distance(Gdx.input.getX(1), Gdx.graphics.getHeight() - Gdx.input.getY(1), x, y);
        
        // finger is touching joystick
        // padding to register touch if finger is a little bit off the joystick
        int padding = 100;
        boolean finger0 = Gdx.input.isTouched(0) && distFinger0 <= radius + padding;
        boolean finger1 = Gdx.input.isTouched(1) && distFinger1 <= radius + padding;
        
        setPowerRatio(0);
        
        if (finger0) {
            stickX = Gdx.input.getX(0);
            stickY = Gdx.graphics.getHeight() - Gdx.input.getY(0);
            setPowerRatio(distFinger0 / radius);
        } else if (finger1) {
            stickX = Gdx.input.getX(1);
            stickY = Gdx.graphics.getHeight() - Gdx.input.getY(1);
            setPowerRatio(distFinger1 / radius);
        }
        
        // cap ratio and set multiplier
        if (getPowerRatio() > 1)
            setPowerRatio(1);
        if (getPowerRatio() < 0)
            setPowerRatio(0);
        
        //angle = MyMath.angleTo(stickX, stickY, stickCenterX, stickCenterY);
        setAngle(MyMath.angleTo(stickX, stickY, x, y));
        
        return finger0 || finger1;
    }
    
    public float getAngle() {
        return angle;
    }
    
    public void setAngle(float angle) {
        this.angle = angle;
    }
    
    public float getPowerRatio() {
        return ratio;
    }
    
    public void setPowerRatio(float powerRatio) {
        this.ratio = powerRatio;
    }
}