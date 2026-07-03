package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.screens.GameScreen;

import java.util.ArrayList;

public class SpaceDustSystem extends EntitySystem implements Disposable {
    
    // apply shader and shift Z to bring dust to life!
    // something along the lines of pixel = (1-value) * time
    // this could be done entirely in a shader, but I can't figure out how to implement properly
    // eg: https://www.shadertoy.com/view/XsX3zB
    // this is CC / MIT so I could "steal" it
    
    private OrthographicCamera cam;
    private SpriteBatch spriteBatch;
    private ShaderProgram shader;
    private Texture dustTexture;
    
    private ArrayList<Vector2> tiles;
    private Vector2 centerTile;
    
    private final int tileSize = 512;
    private int surround = 4;// how many tiles to load around center tile
    private float zoomFade = CameraSystem.getZoomForLevel((byte)11);
    
    private float shift = 0;
    private final float shiftSpeed = 0.05f;
    
    @Override
    public void addedToEngine(Engine engine) {
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
        
        ShaderProgram.pedantic = false;
        shader = new ShaderProgram(Gdx.files.internal("shaders/heightShift.vert"), Gdx.files.internal("shaders/heightShift.frag"));
        if (shader.isCompiled()) {
            spriteBatch.setShader(shader);
            Gdx.app.log(this.getClass().getSimpleName(), "shader compiled successfully!");
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "shader failed to compile:\n" + shader.getLog());
        }
        
        dustTexture = TextureGenerator.generateSpaceDust(MathUtils.random(Long.MAX_VALUE), tileSize, 200);
        //dustTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        //todo: is it possible to use GL_REPEAT and make the draw space take up the screen instead of drawing the same texture repeatedly?
        dustTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    
        tiles = new ArrayList<>();
    }
    
    @Override
    public void update(float delta) {
        //fade out when zoom out
        float alpha = Interpolation.pow3In.apply(0, 0.6f, 1 - (cam.zoom / zoomFade));
        
        //update and render
        if (cam.zoom <= zoomFade) {
            centerTile = updateLayer(centerTile);
            drawTiles(alpha, delta);
        }
    }

    private void drawTiles(float alpha, float delta) {
        //shift shader
        shift += shiftSpeed * delta;
        shader.bind();
        shader.setUniformf("u_shift", shift);
        
        //render
        spriteBatch.setColor(1, 1, 1, alpha);
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        
        for (Vector2 tile : tiles) {
            float drawX = (tile.x * tileSize);
            float drawY = (tile.y * tileSize);
            float width = dustTexture.getWidth();
            float height = dustTexture.getHeight();
            
            spriteBatch.draw(dustTexture, drawX, drawY,
                    0, 0,
                    width, height,
                    1, 1,
                    0, 0, 0, (int) width, (int) height, false, false);
        }
        spriteBatch.end();
    }
    
    private Vector2 updateLayer(Vector2 previousTile) {
        //calculate tile camera is within
        Vector2 currentTile = getTilePos(cam.position.x, cam.position.y, tileSize);

        //load initial tiles
        if (previousTile == null) {
            previousTile = new Vector2(currentTile);
            loadTiles(currentTile);
        }
        
        //check if moved tile
        if (currentTile.x != previousTile.x || currentTile.y != previousTile.y) {
            // unload old tiles
            unloadTiles(currentTile);
            
            // load new tiles
            loadTiles(currentTile);
        }
        
        return currentTile;
    }
    
    private void loadTiles(Vector2 centerTile) {
        for (int tX = (int) centerTile.x - surround; tX <= centerTile.x + surround; tX++) {
            for (int tY = (int) centerTile.y - surround; tY <= centerTile.y + surround; tY++) {
                // check if tile already exists
                boolean isTileLoaded = false;
                for (int index = 0; index < tiles.size(); ++index) {
                    Vector2 t = tiles.get(index);
                    if (t.x == tX && t.y == tY) {
                        isTileLoaded = true;
                        break;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!isTileLoaded) {
                    tiles.add(new Vector2(tX, tY));
                }
            }
        }
    }
    
    private void unloadTiles(Vector2 centerTile) {
        for (int index = 0; index < tiles.size(); ++index) {
            Vector2 tile = tiles.get(index);
            if (!tileIsNear(centerTile, tile)) {
                // remove tile
                tiles.remove(index);
                
                // reset search index because element was removed
                index = -1;
            }
        }
    }
    
    private static Vector2 getTilePos(float x, float y, int tileSize) {
        // calculate tile that position is in
        int tX = (int)(x / tileSize);
        int tY = (int)(y / tileSize);
        
        // subtract 1 from tile position if less than zero
        // to account for -1/x giving 0
        if (x < 0) {
            --tX;
        }
        if (y < 0) {
            --tY;
        }
        
        return new Vector2(tX, tY);
    }
    
    /** Check if a tile is within range of center tile. */
    private boolean tileIsNear(Vector2 centerTile, Vector2 tileToCheck) {
        return Math.abs(tileToCheck.x - centerTile.x) <= surround &&
               Math.abs(tileToCheck.y - centerTile.y) <= surround;
    }
    
    @Override
    public void dispose() {
        dustTexture.dispose();
        spriteBatch.dispose();
    }
    
}
