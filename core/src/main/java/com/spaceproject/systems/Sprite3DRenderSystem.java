package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import java.util.Comparator;

public class Sprite3DRenderSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    private final ModelBatch modelBatch;
    
    private final Array<Entity> renderQueue = new Array<>();
    private final Comparator<Entity> comparator = new Comparator<Entity>() {
        @Override
        public int compare(Entity entityA, Entity entityB) {
            return (int) Math.signum(Mappers.transform.get(entityB).zOrder
                    - Mappers.transform.get(entityA).zOrder);
        }
    };
    
    
    public Sprite3DRenderSystem() {
        super(Family.all(Sprite3DComponent.class, TransformComponent.class).get());
        
        cam = GameScreen.cam;
        modelBatch = new ModelBatch();
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
    
        modelBatch.begin(cam);
        for (Entity entity : renderQueue) {
            render(entity);
        }
        modelBatch.end();
        
        renderQueue.clear();
    }
    
    private void render(Entity entity) {
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        TransformComponent t = Mappers.transform.get(entity);

        //TODO: the switch to renderables from textures seems to have a performance impact. currently it's a mesh and texture per entity.
        //see https://xoppa.github.io/blog/a-simple-card-game/#reduce-the-number-of-render-calls
        sprite3D.renderable.worldTransform.setToRotation(Vector3.Z, MathUtils.radDeg * t.rotation);
        sprite3D.renderable.worldTransform.rotate(Vector3.X, MathUtils.radDeg * sprite3D.renderable.angle);
        sprite3D.renderable.worldTransform.setTranslation(t.pos.x, t.pos.y, -50);
        sprite3D.renderable.worldTransform.scale(sprite3D.renderable.scale.x, sprite3D.renderable.scale.y, sprite3D.renderable.scale.z);
        
        modelBatch.render(sprite3D.renderable);
    }
    
    /* Debug manual control
	    if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
	    	sprite3D.renderable.angle += 15*delta;
	    }
	    if (Gdx.input.isKeyPressed(Input.Keys.E)) {
	    	sprite3D.renderable.angle -= 15*delta;
	    }
	    if (Gdx.input.isKeyPressed(Input.Keys.R)) {
	    	sprite3D.renderable.angle += (float)Math.PI;
	    }
	    System.out.println(sprite3D.renderable.angle * MathUtils.radDeg);
	    */
	    /*
	    // Test
	    //		set() seems to overwrite previous rotation only applying last called set
	    //		setEulerAnglesRad() seems to apply pitch and yaw in the opposite order we desire
	    sprite3D.renderable.position.set(t.pos.x, t.pos.y, -50);
	    //sprite3D.renderable.rotation.set(Vector3.X, MathUtils.radDeg * sprite3D.renderable.angle);//"roll"
	    //sprite3D.renderable.rotation.set(Vector3.Z, MathUtils.radDeg * t.rotation);//"orientation facing"
	    //this applies in the wrong order resulting in funny rotation
	    sprite3D.renderable.rotation.setEulerAnglesRad(0, sprite3D.renderable.angle, t.rotation);
    * */
}
