package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.math.MyMath;

public class Slider {
    static Texture tex = TextureGenerator.createTile(new Color(1f, 1f, 1f, 1f));
    
    private int btnWidth;
    private int sldWidth;
    private int sldHeight;
    
    private int btnX;
    private int x, y;
    private float value;
    private float min, max;
    
    public String name;
    
    public Slider(String name, float min, float max, int x, int y, int btnWidth, int sldWidth, int sldHeight) {
        this.name = name;
        this.min = min;
        this.max = max;
        
        this.x = x;
        this.y = y;
        
        this.btnWidth = btnWidth;
        this.sldWidth = sldWidth;
        this.sldHeight = sldHeight;
        
        setValue((max - min) / 2 + min);
    }
    
    public String toString() {
        return name + ": " + MyMath.round(getValue(), 3);
    }
    
    public void setValue(float val) {
        value = MathUtils.clamp(val, min, max);
        updateButtonPos();
    }
    
    public float getValue() {
        return value;
    }
    
    private void updateButtonPos() {
        float pos = (sldWidth * (min - value)) / (min - max) + x;
        btnX = (int) MathUtils.clamp(pos, x, x + sldWidth);
    }
    
    public boolean isMouseOver() {
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        int mouseX = Gdx.input.getX();
        return mouseY >= y && mouseY <= y + sldHeight
                && mouseX >= x - btnWidth / 2 && mouseX <= x + sldWidth + btnWidth;
    }
    
    public void draw(SpriteBatch batch, BitmapFont font) {
        //draw bar
        batch.setColor(Color.BLACK);
        batch.draw(tex, x - btnWidth / 2, y, sldWidth + btnWidth, sldHeight);
        if (isMouseOver()) {
            batch.setColor(0.6f, 0.6f, 0.6f, 1f);
        } else {
            batch.setColor(0.3f, 0.3f, 0.3f, 1f);
        }
        batch.draw(tex, x - btnWidth / 2 + 1, y + 1, sldWidth + btnWidth - 2, sldHeight - 2);
        
        //draw button
        batch.setColor(Color.BLACK);
        batch.draw(tex, btnX - btnWidth / 2, y, btnWidth, sldHeight);
        if (isMouseOver()) {
            batch.setColor(0.2f, 0.5f, 0.9f, 1f);
        } else {
            batch.setColor(0.7f, 0.7f, 0.7f, 1f);
        }
        batch.draw(tex, btnX - btnWidth / 2 + 1, y + 1, btnWidth - 2, sldHeight - 2);
        
        //draw text
        font.draw(batch, toString(), x, y + sldHeight / 1.5f);
    }
    
    public boolean update() {
        if (Gdx.input.isTouched() && isMouseOver()) {
            float temp = value;
            float v = (Gdx.input.getX() - x) / (float) sldWidth * (max - min) + min;
            setValue(v);
            return temp != v;
        }
        return false;
    }
    
}