package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;

public class ReproFamilyBugTest {
    
    public ReproFamilyBugTest() {
        /**
         * BUG: if engine is reassigned (engine = new Engine()) and a previously existing entity is re-added,
         * the underlying families will not see that entity unless removed first
         * <p>
         * SOLUTION: workaround is to engine.removeAllEntities() when changing levels
         * in the engine,when an entity is removed it calls updateFamilyMembership()
         * Maybe the problem lies in references to old bits that aren't discarded when a new Engine() is created?
         * <p>
         * not sure if bug or intended library behavior...
         */
        entityNotFound();//broken
        entityFound();//fixed
    }
    
    private void entityNotFound() {
        Engine engine = new Engine();
        engine.addSystem(new TestSystem());
        
        Entity entity = new Entity();
        entity.add(new TestComponent());
        
        engine.addEntity(entity);
        
        System.out.println("Engine: " + engine.getEntities().size());
        for (Entity e : engine.getEntitiesFor(Family.all(TestComponent.class).get())) {
            System.out.println("--> " + e);
        }
        
        engine.update(1);
        
        //engine.removeAllEntities();//<---without this, entity is not picked up by @ReproBugTestDebugSystem
        
        engine = new Engine();
        engine.addEntity(entity);
        
        System.out.println("Engine: " + engine.getEntities().size());
        for (Entity e : engine.getEntitiesFor(Family.all(TestComponent.class).get())) {
            System.out.println("--> " + e);
        }
        
        engine.addSystem(new TestSystem());//complains about not finding entity
    }
    
    private void entityFound() {
        Engine engine = new Engine();
        engine.addSystem(new TestSystem());
        
        Entity entity = new Entity();
        entity.add(new TestComponent());
        
        engine.addEntity(entity);
        
        System.out.println("Engine: " + engine.getEntities().size());
        for (Entity e : engine.getEntitiesFor(Family.all(TestComponent.class).get())) {
            System.out.println("--> " + e);
        }
        
        engine.update(1);
        
        engine.removeAllEntities();//<---fixes!
        
        engine = new Engine();
        engine.addEntity(entity);
        
        System.out.println("Engine: " + engine.getEntities().size());
        for (Entity e : engine.getEntitiesFor(Family.all(TestComponent.class).get())) {
            System.out.println("--> " + e);
        }
        
        engine.addSystem(new TestSystem());//finds entity, does not complain
    }
}

 class TestSystem extends IteratingSystem {
    
    public TestSystem() {
        super(Family.all(TestComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        
        for (Entity engineEntity : engine.getEntities()) {
            if (engineEntity.getComponent(TestComponent.class) != null) {
                ImmutableArray<Entity> familyEntities = engine.getEntitiesFor(Family.all(TestComponent.class).get());
                if (!familyEntities.contains(engineEntity, true)) {
                    Gdx.app.log(this.getClass().getSimpleName(), "FOUND ENTITY IN ENGINE: FAMILY DID NOT PICK UP ENTITY!");
                    //throw new Exception("Family did not pick up entity in engine");
                }
            }
        }
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Gdx.app.log(this.getClass().getSimpleName(), "processed: " + entity);
    }
    
}

class TestComponent implements Component {}