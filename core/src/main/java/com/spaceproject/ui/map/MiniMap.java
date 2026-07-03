package com.spaceproject.ui.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.MapComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.MiniMapConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class MiniMap {
    
    private MiniMapConfig miniMapCFG;
    private CelestialConfig celestCFG;
    private MapState mapState = MapState.off;
    private Rectangle mapContainer = new Rectangle();
    private BitmapFont font;
    private float mapScale;
    private SimpleTimer drawScaleTimer;
    private static final Circle tmpCircle = new Circle();
    
    public MiniMap() {
        this(SpaceProject.configManager.getConfig(MiniMapConfig.class), SpaceProject.configManager.getConfig(CelestialConfig.class));
    }
    
    public MiniMap(MiniMapConfig miniMapConfig, CelestialConfig celestialConfig) {
        miniMapCFG = miniMapConfig;
        celestCFG = celestialConfig;
        
        drawScaleTimer = new SimpleTimer(miniMapCFG.drawScaleTimer);
        
        updateMapPosition();
        resetMapScale();
        
        font = FontLoader.createFont(FontLoader.fontPressStart, miniMapCFG.fontSize);
    }
    
    public void drawMiniMap(ShapeRenderer shape, SpriteBatch batch, Entity player, ImmutableArray<Entity> entities) {
        if (!miniMapCFG.debugDisableClipping) {
            ScissorStack.pushScissors(mapContainer);
        }
        
        float centerMapX = mapContainer.x + mapContainer.width / 2;
        float centerMapY = mapContainer.y + mapContainer.height / 2;
        
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw backing
            shape.setColor(miniMapCFG.backingColor);
            shape.rect(mapContainer.x, mapContainer.y, mapContainer.width, mapContainer.height);
            
            
            //draw mouse pos
            if (mapState == MapState.full) {
                drawMouseGrid(shape, miniMapCFG.mouseColor);
            }
            
            
            //draw grid
            drawGrid(shape, centerMapX, centerMapY,  miniMapCFG.gridSize, mapContainer.width, miniMapCFG.gridColor);
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        {
            if (GameScreen.inSpace()) {
                boolean drawOrbit = mapScale <= miniMapCFG.lodRenderOrbitPathScale;
                if (drawOrbit) {
                    drawOrbitPaths(shape, entities, centerMapX, centerMapY, miniMapCFG.orbitPathColor);
                }
    
                //debug
                if (miniMapCFG.debugDrawLoadDist) {
                    drawDebugLoadDist(shape, centerMapX, centerMapY, celestCFG.loadSystemDistance, miniMapCFG.debugLoadDistColor);
                }
            }
            
            drawViewport(shape, centerMapX, centerMapY, miniMapCFG.viewportColor);
        }
        shape.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        {
            //draw all celestial bodies
            if (GameScreen.inSpace()) {
                drawUniversePoints(shape, centerMapX, centerMapY,
                        celestCFG.loadSystemDistance,
                        miniMapCFG.celestialMarkerSize,
                        miniMapCFG.universeMarkerColor);
            }
            
            //draw loaded celestial bodies
            if (entities != null) {
                drawMapableEntities(shape, entities, centerMapX, centerMapY);
            }
            
            drawPlayerMarker(shape, player, centerMapX, centerMapY,
                    miniMapCFG.playerMarkerSize,
                    miniMapCFG.playerMarkerColor,
                    miniMapCFG.velocityVecColor,
                    miniMapCFG.orientationColor);
            
            drawBorder(shape);
        }
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        
        if (!miniMapCFG.debugDisableClipping) {
            ScissorStack.popScissors();
        }
        
        
        batch.begin();
        drawMapText(batch);
        batch.end();
    }
    
    private void drawMapText(SpriteBatch batch) {
        float textPosX = mapContainer.x + 10;
        float textPosY = mapContainer.y + mapContainer.height;
        float lineHeight = font.getLineHeight() + 2;

        if (mapState == MapState.full) {
            //draw game time
            font.draw(batch, MyMath.formatDuration(GameScreen.getGameTimeCurrent()), textPosX, textPosY - lineHeight * 2);
            //draw seed
            long seed = GameScreen.getGalaxySeed();
            if (!GameScreen.inSpace()) {
                seed = GameScreen.getPlanetSeed();
            }
            font.draw(batch, "seed: " + seed, textPosX, textPosY - lineHeight);
        }
        if (mapState == MapState.full && !drawScaleTimer.canDoEvent()) {
            font.draw(batch, "scale: " + mapScale, textPosX, textPosY - (lineHeight * 3));
        }
    }
    
    private void drawPlayerMarker(ShapeRenderer shape, Entity player, float centerMapX, float centerMapY, int playerMarkerSize, Color playerMarkerColor, Color velocityVecColor, Color orientationColor) {
        if (player != null) {
            float scale = 5;
            int vecWidth = 2;
            
            //draw movement direction for navigation assistance, line up vector with target destination
            Body body = Mappers.physics.get(player).body;
            Vector2 velocity = body.getLinearVelocity();
            
            HyperDriveComponent hyperComp = Mappers.hyper.get(player);
            if (hyperComp != null && hyperComp.state == HyperDriveComponent.State.on) {
                velocity = hyperComp.velocity;
            }
            
            if (velocity.len() > 0) {
                Vector2 direction = MyMath.vector(velocity.angleRad(), 50000).add(centerMapX, centerMapY);
                shape.rectLine(centerMapX, centerMapY, direction.x, direction.y, 1, orientationColor, orientationColor);
            }
            Vector2 velocityScaled = MyMath.logVec(velocity, scale).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, velocityScaled.x, velocityScaled.y, vecWidth, velocityVecColor, velocityVecColor);
            
            Vector2 facing = MyMath.vector(body.getAngle(), 50000).add(centerMapX, centerMapY);
            shape.rectLine(centerMapX, centerMapY, facing.x, facing.y, 1, orientationColor, orientationColor);
        }
        
        shape.setColor(playerMarkerColor);
        shape.circle(centerMapX, centerMapY, playerMarkerSize);
    }
    
    private void drawMapableEntities(ShapeRenderer shape, ImmutableArray<Entity> entities, float centerMapX, float centerMapY) {
        for (Entity mapEntity : entities) {
            MapComponent map = Mappers.map.get(mapEntity);
            Vector2 screenPos = Mappers.transform.get(mapEntity).pos;
            
            // n = relative pos / scale + mapPos
            float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;

            float size = 2;
            TextureComponent tex = Mappers.texture.get(mapEntity);
            if (tex != null) {
                size = Math.max((tex.texture.getWidth() * 0.5f * tex.scale) / mapScale, 1f);
            }
            
            tmpCircle.set(x, y, -size);// negative because want to include edges
            if (mapContainer.contains(tmpCircle)) {
                shape.setColor(map.color);
                shape.circle(x, y, size);
            }
        }
    }
    
    private void drawUniversePoints(ShapeRenderer shape, float centerMapX, float centerMapY, float loadDist, int celestialMarkerSize, Color color) {
        loadDist *= loadDist;
        for (Vector2 p : GameScreen.galaxy.points) {
            if (p.dst2(MyScreenAdapter.cam.position.x, MyScreenAdapter.cam.position.y) < loadDist) {
                continue;
            }
            
            // n = relative pos / scale + mapPos
            float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            
            if (mapContainer.contains(x, y)) {
                shape.setColor(color);//TODO: dynamic color based on celestial body type
                shape.circle(x, y, celestialMarkerSize);
            }
        }
    }
    
    private void drawOrbitPaths(ShapeRenderer shape, ImmutableArray<Entity> entities, float centerMapX, float centerMapY, Color color) {
        if (entities == null) {
            return;
        }
        
        shape.setColor(color);
        for (Entity mapEntity : entities) {
            Vector2 screenPos = Mappers.transform.get(mapEntity).pos;
        
            // n = relative pos / scale + mapPos
            float x = ((screenPos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            float y = ((screenPos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
        
            OrbitComponent orbit = Mappers.orbit.get(mapEntity);
            if (orbit != null && orbit.parent != null) {
                TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                float xx = ((parentPos.pos.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float yy = ((parentPos.pos.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            
                shape.circle(xx, yy, orbit.radialDistance / mapScale);
                shape.line(xx, yy, x, y);
            }
        }
    }
    
    private void drawMouseGrid(ShapeRenderer shape, Color color) {
        shape.setColor(color);
        int mX = Gdx.input.getX();
        int mY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (mapContainer.contains(mX, mY)) {
            shape.line(mapContainer.x, mY, mapContainer.x + mapContainer.width, mY);//horizontal
            shape.line(mX, mapContainer.y, mX, mapContainer.y + mapContainer.height);//vertical
        }
    }
    
    private void drawBorder(ShapeRenderer shape) {
        shape.setColor(miniMapCFG.borderColor);
        shape.rect(mapContainer.x, mapContainer.height + mapContainer.y - miniMapCFG.borderWidth, mapContainer.width, miniMapCFG.borderWidth);//top
        shape.rect(mapContainer.x, mapContainer.y, mapContainer.width, miniMapCFG.borderWidth);//bottom
        shape.rect(mapContainer.x, mapContainer.y, miniMapCFG.borderWidth, mapContainer.height);//left
        shape.rect(mapContainer.width + mapContainer.x - miniMapCFG.borderWidth, mapContainer.y, miniMapCFG.borderWidth, mapContainer.height);//right
    }
    
    private void drawGrid(ShapeRenderer shape, float centerMapX, float centerMapY, int gridSize, float width, Color gridColor) {
        shape.setColor(gridColor);
        int halfWidth = (int) (((width / 2)));
        int startX = (int) ((-halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / gridSize;
        int endX = (int) ((halfWidth * mapScale) + MyScreenAdapter.cam.position.x) / gridSize;
        for (int i = startX; i < endX + 1; i++) {
            float finalX = (((i * gridSize) - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
            shape.rect(finalX, mapContainer.y, 1, mapContainer.height);
        }
        
        // draw grid Y
        int halfHeight = (int) (((mapContainer.height / 2)));
        int startY = (int) ((-halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / gridSize;
        int endY = (int) ((halfHeight * mapScale) + MyScreenAdapter.cam.position.y) / gridSize;
        for (int i = startY; i < endY + 1; i++) {
            float finalY = (((i * gridSize) - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
            shape.rect(mapContainer.x, finalY, width, 1);
        }
    }
    
    private void drawViewport(ShapeRenderer shape, float centerMapX, float centerMapY, Color color) {
        shape.setColor(color);
    
        Vector3 topLeft = MyScreenAdapter.cam.unproject(new Vector3(0, 0, 0));
        float x1 = ((topLeft.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
        float y1 = ((topLeft.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
    
        Vector3 bottomRight = MyScreenAdapter.cam.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0));
        float x2 = ((bottomRight.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
        float y2 = ((bottomRight.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
        
        shape.line(x1, y1, x2, y1);//top
        shape.line(x1, y2, x2, y2);//bottom
        shape.line(x1, y1, x1, y2);//left
        shape.line(x2, y1, x2, y2);//right
    }
    
    private void drawDebugLoadDist(ShapeRenderer shape, float centerMapX, float centerMapY, float loadSystemDistance, Color color) {
        shape.setColor(color);
        if (GameScreen.inSpace()) {
            for (Vector2 p : GameScreen.galaxy.points) {
                // n = relative pos / scale + mapPos
                float x = ((p.x - MyScreenAdapter.cam.position.x) / mapScale) + centerMapX;
                float y = ((p.y - MyScreenAdapter.cam.position.y) / mapScale) + centerMapY;
                shape.circle(x, y, loadSystemDistance / mapScale);
            }
        }
    }
    
    public void cycleMiniMapPosition() {
        miniMapCFG.miniMapPosition = miniMapCFG.miniMapPosition.next();
        updateMapPosition();
    }
    
    public void cycleMapState() {
        mapState = mapState.next();
        updateMapPosition();
    }
    
    public void updateMapPosition() {
        createNewMapRectangle(mapContainer);
        drawScaleTimer.reset();
    }
    
    public MapState getState() {
        return mapState;
    }
    
    private Rectangle createNewMapRectangle(Rectangle rectangle) {
        switch (miniMapCFG.miniMapPosition) {
            case topLeft:
                rectangle.set(miniMapCFG.miniEdgePad, Gdx.graphics.getHeight() - miniMapCFG.miniHeight - miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                break;
            case topRight:
                rectangle.set(Gdx.graphics.getWidth() - miniMapCFG.miniWidth - miniMapCFG.miniEdgePad, Gdx.graphics.getHeight() - miniMapCFG.miniHeight - miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                break;
            case bottomLeft:
                rectangle.set(miniMapCFG.miniEdgePad, miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                break;
            case bottomRight:
                rectangle.set(Gdx.graphics.getWidth() - miniMapCFG.miniWidth - miniMapCFG.miniEdgePad, miniMapCFG.miniEdgePad, miniMapCFG.miniWidth, miniMapCFG.miniHeight);
                break;
        }
    
        if (mapState == MapState.full) {
            //padded
            rectangle.set(miniMapCFG.edgePad, miniMapCFG.edgePad,
                    Gdx.graphics.getWidth() - miniMapCFG.edgePad * 2,
                    Gdx.graphics.getHeight() - miniMapCFG.edgePad * 2);
            //truly fullscreen
            rectangle.set(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        
        return rectangle;
    }
    
    public Rectangle getMapContainer() {
        return mapContainer;
    }
    
    private void scrollMiniMap(float amount) {
        float scrollAmount = amount * mapScale / miniMapCFG.zoomMultiplier;
        mapScale += scrollAmount;
        mapScale = MathUtils.clamp(mapScale, miniMapCFG.minScale, miniMapCFG.maxSale);
        
        drawScaleTimer.reset();
    }
    
    public boolean scrolled(float amountX, float amountY) {
        switch (mapState) {
            case full:
                scrollMiniMap(amountY);
                return true;
            case mini:
                if (mapContainer.contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
                    scrollMiniMap(amountY);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    public void resetMapScale() {
        mapScale = miniMapCFG.defaultMapScale;
        drawScaleTimer.reset();
    }
    
}

