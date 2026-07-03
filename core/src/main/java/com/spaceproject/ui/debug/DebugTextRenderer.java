package com.spaceproject.ui.debug;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.generation.FontLoader;

public class DebugTextRenderer {
    
    private BitmapFont fontSmall;
    private Array<DebugText> debugTexts; //todo: profile, could probably be pooled rather than new and clear each loop
    
    public DebugTextRenderer() {
        debugTexts = new Array<>();
        fontSmall = FontLoader.createFont(FontLoader.fontBitstreamVM, 10);
    }
    
    public void draw(SpriteBatch batch) {
        batch.begin();
        for (DebugText t : debugTexts) {
            if (t.font == null) {
                //default font
                fontSmall.setColor(t.color);
                fontSmall.draw(batch, t.text, t.x, t.y);
            } else {
                //custom font
                t.font.setColor(t.color);
                t.font.draw(batch, t.text, t.x, t.y);
            }
        }
        batch.end();
        
        debugTexts.clear();
    }
    
    public void add(String text, float x, float y) {
        debugTexts.add(new DebugText(text, x, y));
    }
    
    public float getFontHeight() {
        return fontSmall.getLineHeight();
    }
    
}
