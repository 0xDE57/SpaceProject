package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ItemComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.InsetPolygonCache;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class AsteroidBatchRenderSystem extends IteratingSystem {
    
    boolean debugRevealAll = false;
    
    final ShapeDrawer shapeDrawer;
    final Batch batch;
    final Texture texture;
    final InsetPolygonCache polygonCache;
    final Color tempColor;
    final Color defaultOutline;
    
    float lineWidth = 0.18f;
    float borderGrowMultiplier = 0.75f;
    JoinType joinType = JoinType.POINTY;
    
    boolean asteroidAutoLOD = true;
    boolean fpsLimitedLOD = true;
    
    public enum PathMode {
        CENTERED, //current default
        INSET,
        OUTSET
    }
    
    PathMode mode = PathMode.INSET;
    
    public AsteroidBatchRenderSystem() {
        super(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(0, 0);
        texture = new Texture(pixmap); //remember to dispose of later
        pixmap.dispose();
        TextureRegion region = new TextureRegion(texture, 0, 0, 1, 1);
        batch = new SpriteBatch();
        shapeDrawer = new ShapeDrawer(batch, region);
        polygonCache = new InsetPolygonCache(8);
        
        tempColor = new Color();
        defaultOutline = new Color(0.5f, 0.5f, 0.5f, 1);
    }
    
    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(GameScreen.cam.combined);
        
        //todo: not all asteroids need transparency, only glass. could potentially sort enable blending for only glassteroids? profiler.
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        batch.begin();
        super.update(deltaTime);
        batch.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        if (GameScreen.isDebugMode) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                switch (joinType) {
                    case NONE:
                        joinType = JoinType.POINTY;
                        break;
                    case POINTY:
                        joinType = JoinType.SMOOTH;
                        break;
                    case SMOOTH:
                        joinType = JoinType.NONE;
                        break;
                }
                Gdx.app.debug(getClass().getSimpleName(), "JoinType: " + joinType);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
                switch (mode) {
                    case CENTERED:
                        mode = PathMode.INSET;
                        break;
                    case INSET:
                        mode = PathMode.OUTSET;
                        break;
                    case OUTSET:
                        mode = PathMode.CENTERED;
                        break;
                }
                Gdx.app.debug(getClass().getSimpleName(), "Outline Mode: " + mode);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ADD)) {
                lineWidth += 2f * deltaTime;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_SUBTRACT)) {
                lineWidth -= 2f * deltaTime;
            }
        }
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        //set polygon translation and rotation
        Polygon polygon = asteroid.polygon;
        polygon.setRotation(transform.rotation * MathUtils.radiansToDegrees);
        polygon.setPosition(transform.pos.x, transform.pos.y);
        
        //set color based on type and health
        HealthComponent health = Mappers.health.get(entity);
        float ratio = health.health / health.maxHealth;
        long timeSinceDmg = GameScreen.getGameTimeCurrent() - health.lastHitTime;
        long hitTime = 10;
        if (timeSinceDmg < hitTime) {
            float fade = (float) timeSinceDmg / hitTime;
            ratio -= fade * 0.125f;
        }
        if (asteroid.composition == ItemComponent.Resource.GLASS) {
            tempColor.set(asteroid.color);
        } else {
            //tempColor.set(Color.BLACK).lerp(asteroid.revealed ? asteroid.color : defaultOutline, 1 - ratio);
            tempColor.set(Color.BLACK).lerp(asteroid.color, 1 - ratio);
        }
        hitTime = 500;
        long timeSinceHit = GameScreen.getGameTimeCurrent() - asteroid.lastShieldHit;
        if (timeSinceHit < hitTime) {
            float fade = (float) timeSinceHit / hitTime;
            tempColor.lerp(asteroid.color, 1 - fade);
        }
        /* determine orbit lock
        if (asteroid.parentOrbitBody == null) {
            color.set(1-ratio, 0 ,0, 1);//black to red
        } else {
            color.set(1, ratio, ratio, 1);//white to red
        }*/
        shapeDrawer.setColor(tempColor);
        shapeDrawer.filledPolygon(polygon);
        
        //mesh outline
        tempColor.set(defaultOutline);
        float tempWidth = lineWidth;
        if (timeSinceDmg < hitTime) {
            float fade = (float) timeSinceDmg / hitTime;
            tempColor.lerp(asteroid.color, 1 - fade);
            //float rnd = MathUtils.random(-1f, 1f) * 0.5f * borderGrowMultiplier;
            tempWidth += (tempWidth * borderGrowMultiplier) * (1 - fade);// + rnd;
        }
        if (asteroid.revealed || debugRevealAll) {
            tempColor.set(asteroid.color);
        }
        tempColor.a = 1;
        shapeDrawer.setColor(tempColor);
        
        PathMode tempMode = mode;
        JoinType tempjoin = joinType;
        if (asteroidAutoLOD) {
            //LOD - auto adjust rendering quality
            //  JoinType: POINTY and SMOOTH are more expensive than NONE
            //  PathMode: INSET and OUTSET are more expensive than CENTERED
            //  1. reduce asteroid render cost when zoomed out far enough
            //  2. reduce render cost when fps is detected to be below threshold
            if (GameScreen.cam.zoom > 15f || fpsLimitedLOD) {
                tempMode = PathMode.CENTERED;
                tempjoin = JoinType.NONE;
            }
            int frames = Gdx.graphics.getFramesPerSecond();
            if (fpsLimitedLOD) {
                if (frames > 45) {
                    fpsLimitedLOD = false;
                    Gdx.app.debug(getClass().getSimpleName(), "Asteroid LOD restored due to frames: " + frames);
                }
            } else {
                if (frames < 30) {
                    fpsLimitedLOD = true;
                    Gdx.app.debug(getClass().getSimpleName(), "Asteroid LOD dropped asteroid quality due to low frames: " + frames);
                }
            }
        }
        
        
        //NOTE: ConvexHull by default produces CCW.
        // Does DelaunayTriangulator produces CW? Child triangles seem flipped?
        // attempt to detect winding; isCCW now seem to work even when ensure... bug?
        boolean ccw = polygon.getVertices().length > 6;//GeometryUtils.isCCW(vertices, 0 , vertices.length);
        switch (tempMode) {
            case CENTERED:
                shapeDrawer.polygon(polygon, lineWidth, tempjoin);
                break;
            case INSET:
                float[] innerPoly = polygonCache.insetPolygon(polygon.getTransformedVertices(), tempWidth * 0.5f, ccw);
                shapeDrawer.polygon(innerPoly, 0, polygon.getVertices().length, tempWidth, tempjoin);
                break;
            case OUTSET:
                float[] outerPoly = polygonCache.insetPolygon(polygon.getTransformedVertices(), tempWidth * 0.5f, !ccw);
                shapeDrawer.polygon(outerPoly, 0, polygon.getVertices().length, tempWidth, tempjoin);
                break;
        }
        
        /*
        //hit ring animation test...
        if (asteroidAutoLOD && !fpsLimitedLOD) {
            if (GameScreen.cam.zoom < 10f && tempMode == PathMode.INSET) {
                if (timeSinceDmg < hitTime) {
                    float fade = (float) timeSinceDmg / hitTime;
                    float outlineWidth = lineWidth * fade;
                    float[] outerPoly = polygonCache.insetPolygon(polygon.getTransformedVertices(), outlineWidth * 0.5f, !ccw);
                    
                    tempColor.a = fade;
                    shapeDrawer.setColor(tempColor);
                    shapeDrawer.polygon(outerPoly, 0, polygon.getVertices().length, outlineWidth, tempjoin);
               }
            }
        }*/
        
    }
    
}
