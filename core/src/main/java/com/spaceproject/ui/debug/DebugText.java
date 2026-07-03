package com.spaceproject.ui.debug;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class DebugText {
    
    public String text;
    public int x, y;
    public Color color;
    public BitmapFont font;
    
    public DebugText(String text, float x, float y) {
        this(text, x, y, null);
    }
    
    public DebugText(String text, float x, float y, BitmapFont font) {
        this(text, (int) x, (int) y, Color.WHITE, font);
    }
    
    public DebugText(String text, int x, int y, Color color, BitmapFont font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.font = font;
    }
    
}
