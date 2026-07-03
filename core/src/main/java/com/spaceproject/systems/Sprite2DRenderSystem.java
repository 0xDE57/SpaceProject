package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.ShaderComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import java.util.Comparator;

public class Sprite2DRenderSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    
    private final Array<Entity> renderQueue = new Array<>();
    private final Comparator<Entity> comparator = new Comparator<Entity>() {
        @Override
        public int compare(Entity entityA, Entity entityB) {
            return (int) Math.signum(Mappers.transform.get(entityB).zOrder
                    - Mappers.transform.get(entityA).zOrder);
        }
    };
    
    
    public Sprite2DRenderSystem() {
        super(Family.all(TextureComponent.class, TransformComponent.class).exclude(ShaderComponent.class).get());
    
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        renderQueue.add(entity);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime); //adds entities to render queue
        
        //sort render order
        renderQueue.sort(comparator);
        
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        for (Entity entity : renderQueue) {
            render(entity);
        }
        spriteBatch.end();
        
        renderQueue.clear();
    }
    
    private void render(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        TransformComponent t = Mappers.transform.get(entity);
        
        float width = tex.texture.getWidth();
        float height = tex.texture.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        
        //draw texture
        spriteBatch.draw(tex.texture, (t.pos.x - originX), (t.pos.y - originY),
                originX, originY,
                width, height,
                tex.scale, tex.scale,
                MathUtils.radiansToDegrees * t.rotation,
                0, 0, (int) width, (int) height, false, false);
    }
    
}
