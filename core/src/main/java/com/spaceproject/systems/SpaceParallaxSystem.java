package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.SpaceBackgroundTile;
import com.spaceproject.ui.SpaceBackgroundTile.TileType;

import java.nio.IntBuffer;
import java.util.ArrayList;

public class SpaceParallaxSystem extends EntitySystem implements Disposable {

    private final SpriteBatch spriteBatch;
    private final Matrix4 projectionMatrix = new Matrix4();
    
    // background layer of tiles
    private final ArrayList<SpaceBackgroundTile> tiles = new ArrayList<>();
    
    // multiplier for parallax position of tile
    private static final float dustTileDepth = 0.01f;
    private static final float star1TileDepth = 0.02f;
    private static final float star2TileDepth = 0.018f;
    private static final float star3TileDepth = 0.2f;

    private final Vector2 tempVec = new Vector2();
    // center tile to check for tile change
    private final Vector2 dustCenterTile = new Vector2();
    private final Vector2 star0CenterTile = new Vector2();
    private final Vector2 star1CenterTile = new Vector2();
    private final Vector2 star2CenterTile = new Vector2();
    private final Vector2 star3CenterTile = new Vector2();

    private static final int tileSize = 512;//1024;//
    private int surroundX, surroundY;// how many tiles to load around center tile
    private final ShaderProgram spaceShader;

    
    public SpaceParallaxSystem() {
        spriteBatch = new SpriteBatch();

        String vertex = "shaders/spaceParallax.vert";
        String fragment = "shaders/spaceParallax.frag";
        spaceShader = new ShaderProgram(Gdx.files.internal(vertex), Gdx.files.internal(fragment));
        if (spaceShader.isCompiled()) {
            spriteBatch.setShader(spaceShader);
            Gdx.app.log(getClass().getSimpleName(), "shader compiled successfully! [" + vertex + ", " + fragment + "]");
        } else {
            Gdx.app.error(getClass().getSimpleName(), "shader failed to compile:\n" + spaceShader.getLog());
        }

        //todo: according to async-profiler, this is one of the heavier systems. turns out we are drawing way too many tiles!
        // SpriteBatch.flush()
        // SpriteBatch.switchTexture()
        //todo: another potential optimization would be to write tiles to a framebuffer or pixmap (or as many as you can stuff in a maximum texture size optimally)
        // and draw the texture region instead. could manually keep track of which tile is in which texture and mark tiles as free
        // when unloading. this will allow new tiles to write into the free tiles
        int textureSize = 8192; //generally speaking: 16k = newer, 8k = most devices, 4k = older gpus (how old? dunno...)
        //512 tiles into 8k  = 32 tiles.
        //512 tiles into 16k = 64 tiles.
        IntBuffer maxTextureSize = BufferUtils.newIntBuffer(1);
        Gdx.graphics.getGL20().glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, maxTextureSize);
        int maxSupported = maxTextureSize.get();
        //Gdx.app.log(getClass().getSimpleName(), "Maximum texture size: " + maxSupported);
        //FrameBuffer frameBuffer = new FrameBuffer(Pixmap.Format.RGBA4444, maxSupported, maxSupported,false);
        //Pixmap masterPixmap = new Pixmap(maxSupported, maxSupported, Pixmap.Format.RGBA4444);
    }

    @Override
    public void update(float delta) {
        // load and unload tiles
        updateTiles();

        //update shader
        float invert = GameScreen.isHyper() ? 1 : 0;
        CameraSystem cam = getEngine().getSystem(CameraSystem.class);
        float min = 0.30f;
        float blend = MathUtils.map(1, CameraSystem.getZoomForLevel(cam.getMaxZoomLevel()),min, 1, Math.max(GameScreen.cam.zoom, 1));
        spaceShader.bind();
        spaceShader.setUniformf("u_blend", blend);
        spaceShader.setUniformf("u_invert", invert);

        //draw
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        spriteBatch.setProjectionMatrix(projectionMatrix);
        spriteBatch.begin();
        drawParallaxTiles(cam);
        spriteBatch.end();
    }
    
    private void drawParallaxTiles(CameraSystem camSystem) {
        //TextureRegion
        //TextureRegion region = new TextureRegion()
        //Sprite s = new Sprite()
        //frameBuffer.
        //masterPixmap.
        //spriteBatch.draw();
        float centerScreenX = Gdx.graphics.getWidth() * 0.5f;
        float centerScreenY = Gdx.graphics.getHeight() * 0.5f;
        for (SpaceBackgroundTile tile : tiles) {
            float centerTile = tileSize * 0.5f;
            float drawX = ((-GameScreen.cam.position.x * tile.depth) + tile.x - centerTile) + centerScreenX;
            float drawY = ((-GameScreen.cam.position.y * tile.depth) + tile.y - centerTile) + centerScreenY;

            float width = tile.tex.getWidth();
            float height = tile.tex.getHeight();
            switch (tile.layerID) {
                case -1:
                    spriteBatch.setColor(1, 1, 1, 0.825f);
                    break;
                case 0:
                    spriteBatch.setColor(1, 1, 1, 0.95f);
                    break;
                case 1:
                    float fade1 = 1 - (GameScreen.cam.zoom / CameraSystem.getZoomForLevel(camSystem.getMaxZoomLevel()));
                    spriteBatch.setColor(1, 1, 1, fade1);
                    break;
                case 2:
                    float fade2 = 1 - (GameScreen.cam.zoom / CameraSystem.getZoomForLevel(camSystem.getMaxZoomLevel()));
                    spriteBatch.setColor(1, 1, 1, fade2);
                    break;
                case 3:
                    float fade3 = 1 - (GameScreen.cam.zoom / CameraSystem.getZoomForLevel((byte) 10));
                    spriteBatch.setColor(1, 1, 1, fade3);
                    break;
                default:
                    spriteBatch.setColor(1, 1, 1, 1);
            }
            spriteBatch.draw(tile.tex, drawX, drawY,
                    0, 0,
                    width, height,
                    tile.scale, tile.scale,
                    0, 0, 0, (int) width, (int) height, false, false);
        }
    }
    
    private void updateTiles() {
        int prevX = surroundX;
        int prevY = surroundY;
        surroundX = (Gdx.graphics.getWidth() / tileSize) + 1;
        surroundY = (Gdx.graphics.getHeight() / tileSize) + 1;
        //todo: too many tiles are made = overdraw offscreen
        // must calculate surround relative to tile pos
        // @1280x720 we only need 3-4 x and 2-3 y  ->  6 - 12 tiles max. currently there are 35 per layer for a total of 175?! bad!
        // @1920x1080 currently there are 63 per layer for a total of 315?! terrible!
        // @2560x1440 currently there are 91 per layer for a total of 455?! absurd!
        // @4k must be an absolute disaster...
        //surroundX = (int) (Gdx.graphics.getWidth() * 0.5f / tileSize);// + 1;
        //surroundY = (int) (Gdx.graphics.getHeight() * 0.5f / tileSize);//+ 1;

        boolean resize = (prevX != surroundX || prevY != surroundY);
        dustCenterTile.set(updateLayer(resize, dustCenterTile, -1, dustTileDepth, SpaceBackgroundTile.TileType.Dust));

/*
        //debug
        //this could be in tostring...
        String out = "";
        int count = 0;
        for (SpaceBackgroundTile tile : tiles) {
            String message = "(" + tile.tileX + ", " + tile.tileY + ", " + tile.layerID + ")";
            out += message;
            if (count >= 3) {
                out += "\n";
                count = 0;
            }
            count++;
        }
        Gdx.app.log(getClass().getSimpleName(), "center: " + MyMath.formatVector2(dustCenterTile, 0)
                + " - surround:" + surroundX + "," + surroundY + ", tiles:" + tiles.size()
                + "\n" + out);
*/
        star0CenterTile.set(updateLayer(resize, star0CenterTile, 0, 0, SpaceBackgroundTile.TileType.Stars));
        star1CenterTile.set(updateLayer(resize, star1CenterTile, 1, star1TileDepth, SpaceBackgroundTile.TileType.Stars));
        star2CenterTile.set(updateLayer(resize, star2CenterTile, 2, star2TileDepth, SpaceBackgroundTile.TileType.Stars));
        star3CenterTile.set(updateLayer(resize, star3CenterTile, 3, star3TileDepth, SpaceBackgroundTile.TileType.Stars));
    }
    
    /**
     * Checks if correct tiles are loaded around camera position.
     * Old tiles too far away from the camera are unloaded.
     * New tiles are loaded to surround camera.
     *
     * @param lastTile previous tile that was loaded
     * @param depth    layer of tile
     * @param type     of tile
     * @return position of current tile
     */
    private Vector2 updateLayer(boolean resize, Vector2 lastTile, int layerID, float depth, SpaceBackgroundTile.TileType type) {
        //calculate tile camera is within
        Vector2 currentTile = getTilePos(GameScreen.cam.position.x, GameScreen.cam.position.y, depth);

        //load initial tiles
        if (lastTile == null) {
            lastTile = new Vector2(currentTile);
            loadTiles(currentTile, layerID, depth, type);
        }
        
        //check if moved tile
        if (currentTile.x != lastTile.x || currentTile.y != lastTile.y || resize) {
            // unload old tiles
            unloadTiles(currentTile, layerID);
            
            // load new tiles
            loadTiles(currentTile, layerID, depth, type);
        }
        
        return currentTile;
    }
    
    /**
     * Convert world position to tile position.
     *
     * @param posX
     * @param posY
     * @return tile that an object is in.
     */
    private Vector2 getTilePos(float posX, float posY, float depth) {
        float x = (posX * depth) + (tileSize * 0.5f);
        float y = (posY * depth) + (tileSize * 0.5f);
        
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
        return tempVec.set(tX, tY);
    }
    
    /**
     * Load tiles surrounding centerTile of specified depth.
     * @param centerTile
     * @param depth
     * @param type
     */
    private void loadTiles(Vector2 centerTile, int layerID, float depth, TileType type) {
        for (int tX = (int) centerTile.x - surroundX; tX <= centerTile.x + surroundX; tX++) {
            for (int tY = (int) centerTile.y - surroundY; tY <= centerTile.y + surroundY; tY++) {
                // check if tile already exists
                boolean isTileLoaded = false;
                for (int index = 0; index < tiles.size(); ++index) {
                    SpaceBackgroundTile t = tiles.get(index);
                    if (t.tileX == tX && t.tileY == tY && t.layerID == layerID && t.type == type) {
                        isTileLoaded = true;
                        break;
                    }
                }
                
                // create and add tile if doesn't exist
                if (!isTileLoaded) {
                    //Gdx.app.debug(this.getClass().getSimpleName(), "Load " + type + " tile: [" + depth + "]: " + tX + ", " + tY
                    //    + " surround: " + surroundX + ", " + surroundY);
                    SpaceBackgroundTile newTile = new SpaceBackgroundTile(tX, tY, layerID, depth, tileSize, type);
                    //todo: instead of storing individual textures
                    //masterPixmap.drawPixmap(newTile.tex, tX * tileSize?, tY, tileSize, tileSize);
                    tiles.add(newTile);
                }
            }
        }
    }
    
    /**
     * Remove any tiles not surrounding centerTile of same layerID.
     */
    private void unloadTiles(Vector2 centerTile, int layerID) {
        for (int index = 0; index < tiles.size(); ++index) {
            SpaceBackgroundTile tile = tiles.get(index);
            if (tile.layerID == layerID && !tileIsNear(centerTile, tile)) {
                //Gdx.app.debug(this.getClass().getSimpleName(), "Unload " + tile.type + " tile: [" + depth + "]: " + tile.tileX + ", " + tile.tileY);
                
                // dispose the texture so it doesn't eat up memory
                tile.tex.dispose();
                
                // remove tile
                tiles.remove(index);
                
                // reset search index because removing elements changes position of elements
                index = -1;
            }
        }
    }
    
    /** Check if a tile is within range of center tile. */
    private boolean tileIsNear(Vector2 centerTile, SpaceBackgroundTile tileToCheck) {
        return Math.abs(tileToCheck.tileX - centerTile.x) <= surroundX &&
               Math.abs(tileToCheck.tileY - centerTile.y) <= surroundY;
    }
    
    @Override
    public void dispose() {
        //dispose of textures
        for (SpaceBackgroundTile t : tiles) {
            t.tex.dispose();
        }
        tiles.clear();
        
        spriteBatch.dispose();
        spaceShader.dispose();
    }
    
}
