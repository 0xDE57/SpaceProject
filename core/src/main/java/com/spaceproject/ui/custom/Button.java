package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spaceproject.generation.TextureGenerator;

public class Button {
    static Texture buttonTex = TextureGenerator.createTile(new Color(1f, 1f, 1f, 1f));
    
    private String text;
    private float x, y;
    private int width, height;
    
    public Button(String text, float x, float y, int width, int height) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public boolean isClicked() {
        return Gdx.input.justTouched() && isMouseOver();
    }
    
    public boolean isMouseOver() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }
    
    public void draw(SpriteBatch batch, BitmapFont font) {
        batch.setColor(Color.BLACK);
        batch.draw(buttonTex, x, y, width, height);
        
        if (isMouseOver()) {
            batch.setColor(0.2f, 0.5f, 0.9f, 1f);
        } else {
            batch.setColor(0.6f, 0.6f, 0.6f, 1f);
        }
        batch.draw(buttonTex, x + 1, y + 1, width - 2, height - 2);
        font.draw(batch, text, x, y + height);
    }
    
    @Override
    public String toString() {
        return text;
    }
    
}