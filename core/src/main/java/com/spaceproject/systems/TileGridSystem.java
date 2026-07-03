package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.screens.GameScreen;

public class TileGridSystem extends EntitySystem implements Disposable {
    
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final Vector3 screenCoords = new Vector3();
    private final Vector3 camWorldPos = new Vector3();
    private final Rectangle boundingBox = new Rectangle();
    private final WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
    
    public TileGridSystem() {
        shape = new ShapeRenderer();
        projectionMatrix = new Matrix4();
    }
    
    float animate = 0;
    @Override
    public void update(float deltaTime) {
        //update matrix and convert screen coords to world cords.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        screenCoords.set(0,0,0);
        GameScreen.viewport.project(screenCoords);
        camWorldPos.set(GameScreen.cam.position.cpy());
        GameScreen.viewport.project(camWorldPos);
        boundingBox.set(1, 1, Gdx.graphics.getWidth()-2, Gdx.graphics.getHeight()-2);
        
        //debug override background
        //debugClearScreen();
        
        //render
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        //todo: apply shader to grid
        drawGrid(Color.GOLD, worldCFG.tileSize, 1.0f);
        //drawGrid(Color.MAGENTA, 100, 0.5f);
        
        drawOrigin(Color.SKY);
        drawCameraPos(Color.RED);
        
        animate += deltaTime;
        shape.end();
    }
    
    private void drawCameraPos(Color color) {
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
        
        //todo: draw ring from center to target
        //Vector2 average = getEngine().getSystem(CameraSystem.class).average;
        //Vector2 offsetFromTarget = getEngine().getSystem(CameraSystem.class).offsetFromTarget;
        //shape.circle(camWorldPos.x, camWorldPos.y, offsetFromTarget.len());
        //shape.circle(average.x, average.y, 10);
        //GameScreen.viewport.getWorldHeight();
        /*
        shape.setColor(Color.GREEN);
        shape.circle(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(), 8);
        shape.line(GameScreen.viewport.getScreenX(), GameScreen.viewport.getScreenY(),
                GameScreen.viewport.getScreenX() + GameScreen.viewport.getWorldWidth(), GameScreen.viewport.getScreenY() + GameScreen.viewport.getWorldHeight());
         */
    }
    
    private void drawOrigin(Color color) {
        shape.setColor(color);
        shape.circle(0, 0, 10);
        shape.circle(screenCoords.x, screenCoords.y, 10);
        shape.line(screenCoords.x, 0, screenCoords.x, Gdx.graphics.getHeight());
        shape.line(0, screenCoords.y, Gdx.graphics.getWidth(), screenCoords.y);
    }
    
    private void drawGrid(Color color, int tileSize, float depth) {
        shape.setColor(color);
        
        //unscaled
        for (int horizontal = 0; horizontal <= 10; horizontal++) {
            float offset = horizontal * tileSize;
            shape.line(screenCoords.x + offset, 0, screenCoords.x + offset, Gdx.graphics.getHeight());
        }
        
        float scale = 1/GameScreen.cam.zoom;
        scale += depth;
        for (int horizontal = 0; horizontal <= 10; horizontal++) {
            float offset = horizontal * tileSize * scale;
            //shape.line(screenCoords.x + offset, 0, screenCoords.x + offset, Gdx.graphics.getHeight());
        }
    }
    
    private void debugClearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
