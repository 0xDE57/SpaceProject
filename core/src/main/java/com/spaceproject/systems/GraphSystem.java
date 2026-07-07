package com.spaceproject.systems;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.GraphSeries;
import com.spaceproject.utility.MultiGraph;

public class GraphSystem extends EntitySystem {
    
    private final ShapeRenderer shape;
    private final Matrix4 projectionMatrix;
    private final MultiGraph graph;
    private final GraphSeries fpsSeries, entities;//, c, s;
    
    public GraphSystem() {
        shape = GameScreen.shape;
        projectionMatrix = new Matrix4();
        
        graph = new MultiGraph(400, 150);
        fpsSeries = graph.addSeries(1000, Color.GREEN);
        entities = graph.addSeries(1000, Color.WHITE);
        //c = graph.addSeries(200, Color.LIGHT_GRAY);
        //s = graph.addSeries(200, Color.DARK_GRAY);
    }
    
    @Override
    public void update(float deltaTime) {
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        fpsSeries.update(Gdx.graphics.getFramesPerSecond());
        entities.update(getEngine().getEntities().size());
        graph.draw(shape, Gdx.graphics.getWidth() - 50 - graph.getWidth(), Gdx.graphics.getHeight() - 50 - graph.getHeight());
        shape.end();
    }
}
