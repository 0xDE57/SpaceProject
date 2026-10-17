package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ItemComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class AsteroidBatchRenderSystem extends IteratingSystem {
    
    final ShapeDrawer shapeDrawer;
    final Batch batch;
    final Texture texture;
    final Color tempColor;
    final Color defaultOutline;
    
    final float lineWidth = 0.25f;
    
    boolean debugRevealAll = false;
    
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
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        //set polygon translation and rotation
        Polygon polygon = asteroid.polygon;
        polygon.setRotation(transform.rotation * MathUtils.radiansToDegrees);
        polygon.setPosition(transform.pos.x, transform.pos.y);
        
        //set color based on fill type and health
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
        if (asteroid.revealed || debugRevealAll) {
            tempColor.set(asteroid.color);
        } else {
            tempColor.set(defaultOutline);
            if (timeSinceDmg < hitTime) {
                float fade = (float) timeSinceDmg / hitTime;
                tempColor.lerp(asteroid.color, 1 - fade);
            }
        }
        tempColor.a = 1;
        shapeDrawer.setColor(tempColor);
        shapeDrawer.polygon(polygon, lineWidth, JoinType.NONE);
        //shapeDrawer.path(polygon.getTransformedVertices(), lineWidth, JoinType.NONE, false);
    }
    
}
