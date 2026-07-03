package com.spaceproject.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.spaceproject.generation.TextureGenerator;

public class SpaceBackgroundTile {
    
    public static boolean smoothRender = false;
    
    public final float x;
    public final float y;
    public final int tileX;
    public final int tileY;
    public final int layerID;
    public final float depth;
    public final int size;
    public final Texture tex;
    public final float scale;
    
    public TileType type;
    
    public enum TileType {
        Stars, Dust
    }
    
    public SpaceBackgroundTile(int tileX, int tileY, int layerID, float renderDepth, int tileSize, TileType type) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.type = type;
        this.layerID = layerID;
        depth = renderDepth;
        
        //generate texture
        switch (type) {
            case Stars:
                tex = TextureGenerator.generateSpaceBackgroundStars(tileX, tileY, tileSize, renderDepth);
                scale = 1;
                break;
            case Dust:
                scale = 4;
                tileSize /= scale;
                
                Pixmap.Format format = Pixmap.Format.RGBA4444;
                if (smoothRender) {
                    format = Pixmap.Format.RGBA8888;
                }
                tex = TextureGenerator.generateSpaceBackgroundDust(tileX, tileY, tileSize, format);
                break;
            default:
                tex = null;
                scale = 1;
                break;
        }
        
        size = (int) (tileSize * scale);
        x = tileX * size;
        y = tileY * size;
        
    }
    
}
