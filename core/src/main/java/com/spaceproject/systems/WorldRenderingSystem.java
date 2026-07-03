package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.noise.NoiseBuffer;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;

import java.util.ArrayList;

public class WorldRenderingSystem extends EntitySystem {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;

    private final ArrayList<Tile> tiles = Tile.defaultTiles;
    
    private NoiseBuffer noiseMap = null;
    
    private final Texture tileTex = TextureGenerator.createTile(new Color(1f, 1f, 1f, 1f));
    private final WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
    
    private boolean debugShowEdgeTile = false;
    private boolean debugGrayscaleWorld = false;
    
    public WorldRenderingSystem() {
        this.cam = GameScreen.cam;
        this.spriteBatch = new SpriteBatch();
    }
    
    private void loadMap() {
        //todo: should not time out.
        // 1. check if already exists in memory, then disk
        //      if in loadednoise, grab it.
        //      else, check disk for saved noise
        // 2. check if in progress
        //      check if in threadpool workers or queued.
        //      if so, wait for it to generate, goto 1.
        // 3. if not in buffer, progress, or queued (doesn't exist and not being generated)
        //      not in buffer, threadpool or queue (never asked to generate or doesent exist, then ask pool to generate,
        //   - or -
        // perhaps can we subscribe to the NoiseThreadPoolExecutor (INoiseGenListener)
        //   - or -
        // always check disk? no. nvm. memory loaded noise first, then disk. should always save to disk upon completion
        // add additional state to noise buffer?: finished flag
        // add noise buffer to planetcomponent?
    
        long seed = GameScreen.getCurrentPlanet().getComponent(SeedComponent.class).seed;
        Gdx.app.debug(this.getClass().getSimpleName(), "World loader looking for " + seed);
        
        long time = System.currentTimeMillis();
        long timeout = 10000;
        do {
            noiseMap = GameScreen.noiseManager.getNoiseForSeed(seed);
            
            if ((System.currentTimeMillis() - time) > timeout) {
                Gdx.app.error(this.getClass().getSimpleName(), "TIMED OUT: could not find seed for noise: " + seed);
                //TODO: if not cached and if not in process of being generated, only then generate. but this should probably never happen?
                //GameScreen.noiseManager.loadOrCreateNoiseFor(seed, PlanetComponent);
            }
        } while (noiseMap == null);
        Gdx.app.log(this.getClass().getSimpleName(), "Successfully loaded: " + seed + " in: " + (System.currentTimeMillis() - time));
    }
    
    @Override
    public void update(float delta) {
        if (noiseMap == null) {
            loadMap();
            return;
        }
        
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        
        //render background tiles
        drawTiles(worldCFG.tileSize);
        
        spriteBatch.end();
    }

    private void drawTiles(int tileSize) {
        //TODO: change to be calculated by tileSize and window height/width.
        // how many tiles to draw around the camera
        int surroundX = 60;
        int surroundY = 30;
        
        // calculate tile that the camera is in
        int centerX = (int) (cam.position.x / tileSize);
        int centerY = (int) (cam.position.y / tileSize);
        
        // subtract 1 from tile position if less than zero to account for -1/n = 0
        if (cam.position.x < 0) --centerX;
        if (cam.position.y < 0) --centerY;
        
        for (int tileY = centerY - surroundX; tileY <= centerY + surroundY; tileY++) {
            for (int tileX = centerX - surroundX; tileX <= centerX + surroundX; tileX++) {
                //wrap tiles when position is outside of map
                int tX = tileX % noiseMap.heightMap.length;
                int tY = tileY % noiseMap.heightMap.length;
                if (tX < 0) tX += noiseMap.heightMap.length;
                if (tY < 0) tY += noiseMap.heightMap.length;
                
                //render tile
                Color tileColor = tiles.get(noiseMap.tileMap[tX][tY]).getColor();
                if (debugGrayscaleWorld) {
                    tileColor.r = noiseMap.heightMap[tX][tY];
                    tileColor.g = noiseMap.heightMap[tX][tY];
                    tileColor.b = noiseMap.heightMap[tX][tY];
                }
                if (debugShowEdgeTile) {
                    if (tX == noiseMap.heightMap.length - 1 || tY == noiseMap.heightMap.length - 1)
                        tileColor = Color.BLACK;
                }
                
                spriteBatch.setColor(tileColor);
                spriteBatch.draw(tileTex,
                        tileX * tileSize,
                        tileY * tileSize,
                        tileSize, tileSize);
            }
        }
    }
    
}
