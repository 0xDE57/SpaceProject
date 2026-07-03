package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.ScreenTransitionOverlay;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

//todo: consider separating into separate LandOnPlanetSystem and LeavePlanetSystem
public class ScreenTransitionSystem extends IteratingSystem implements IRequireGameContext {
    
    private GameScreen gameScreen;
    private final ScreenTransitionOverlay screenOverlay = new ScreenTransitionOverlay();
    
    public ScreenTransitionSystem() {
        super(Family.all(ScreenTransitionComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void initContext(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        screenOverlay.render();
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(entity);
        
        if (screenTrans.landStage != null) {
            processLanding(entity, screenTrans);
        } else if (screenTrans.takeOffStage != null) {
            processTakeOff(entity, screenTrans);
        }
    }
    
    public static void nextStage(ScreenTransitionComponent screenTrans) {
        if (screenTrans.landStage != null) {
            screenTrans.landStage = screenTrans.landStage.next();
        }
        if (screenTrans.takeOffStage != null) {
            screenTrans.takeOffStage = screenTrans.takeOffStage.next();
        }
    }
    
    private void processLanding(Entity entity, ScreenTransitionComponent screenTrans) {
        if (screenTrans.curLandStage == null || screenTrans.curLandStage != screenTrans.landStage) {
            Gdx.app.log(this.getClass().getSimpleName(), "Animation Stage: " + screenTrans.landStage + " for " + DebugUtil.objString(entity));
            screenTrans.curLandStage = screenTrans.landStage;
        }
        switch (screenTrans.landStage) {
            case shrink:
                shrink(entity, screenTrans);
                break;
            case zoomIn:
                zoomIn(entity, screenTrans);
                break;
            case screenEffectFadeIn:
                fadeIn(screenTrans);
                break;
            case transition:
                landOnPlanet(gameScreen, entity, screenTrans);
                return;
            case screenEffectFadeOut:
                fadeOut(screenTrans);
                break;
            case pause:
                pause(screenTrans);
                break;
            case exit:
                exit(entity, screenTrans);
                break;
            case end:
                entity.remove(ScreenTransitionComponent.class);
                Gdx.app.log(this.getClass().getSimpleName(),
                        "Animation complete. Removed ScreenTransitionComponent for " + DebugUtil.objString(entity));
                break;
            default:
                try {
                    throw new Exception("Unknown Animation Stage: " + screenTrans.landStage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    
    private void processTakeOff(Entity entity, ScreenTransitionComponent screenTrans) {
        if (screenTrans.curTakeOffStage == null || screenTrans.curTakeOffStage != screenTrans.takeOffStage) {
            Gdx.app.log(this.getClass().getSimpleName(), "Animation Stage: " + screenTrans.takeOffStage + " for " + DebugUtil.objString(entity));
            screenTrans.curTakeOffStage = screenTrans.takeOffStage;
        }
        switch (screenTrans.takeOffStage) {
            case screenEffectFadeIn:
                fadeIn(screenTrans);
                break;
            case transition:
                takeOff(gameScreen, entity, screenTrans);
                break;
            case sync:
                syncLoadPosition(entity, screenTrans);
                break;
            case screenEffectFadeOut:
                fadeOut(screenTrans);
                break;
            case zoomOut:
                zoomOut(entity, screenTrans);
                break;
            case grow:
                grow(entity, screenTrans);
                break;
            case end:
                entity.remove(screenTrans.getClass());
                Gdx.app.log(this.getClass().getSimpleName(), "Animation complete. Removed ScreenTransitionComponent from " + DebugUtil.objString(entity));
                break;
            default:
                try {
                    throw new Exception("Unknown Animation Stage: " + screenTrans.takeOffStage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    
    private static void shrink(Entity entity, ScreenTransitionComponent screenTrans) {
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            // freeze movement during animation
            OrbitComponent orbit = Mappers.orbit.get(screenTrans.planet);
            if (orbit != null) {
                physics.body.setLinearVelocity(orbit.velocity);
            } else {
                physics.body.setLinearVelocity(0, 0);
            }
            
            //gentle rotate animation
            if (screenTrans.rotation == 0.0f) {
                screenTrans.rotation = MathUtils.random(0.01f, -0.01f);
            }
            physics.body.setTransform(physics.body.getPosition(), physics.body.getAngle() + screenTrans.rotation);
            
            //lock camera onto ship (bypass lerp)
            if (Mappers.camFocus.get(entity) != null) {
                GameScreen.cam.position.set(physics.body.getPosition().x, physics.body.getPosition().y, 0);
            }
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        if (screenTrans.initialScale == 0) {
            screenTrans.initialScale = sprite3D.renderable.scale.x;
        }
        float interp = screenTrans.animInterpolation.apply(screenTrans.initialScale, 0, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);
        
        if (screenTrans.timer.tryEvent()) {
            sprite3D.renderable.scale.set(0, 0, 0);
            if (entity.getComponent(AIComponent.class) != null) {
                screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.transition;
            } else {
                nextStage(screenTrans);
            }
        }
    }
    
    private static void grow(Entity entity, ScreenTransitionComponent screenTrans) {
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            //match planet vel
            OrbitComponent orbit = Mappers.orbit.get(screenTrans.planet);
            if (orbit != null) {
                physics.body.setLinearVelocity(orbit.velocity);
            }
            
            //gentle rotation animation
            if (screenTrans.rotation == 0.0f) {
                screenTrans.rotation = MathUtils.random(0.01f, -0.01f);
            }
            physics.body.setTransform(physics.body.getPosition(), physics.body.getAngle() + screenTrans.rotation);
            
            //lock cam to target
            if (Mappers.camFocus.get(entity) != null) {
                GameScreen.cam.position.set(physics.body.getPosition().x, physics.body.getPosition().y, 0);
            }
        }
        
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        float interp = screenTrans.animInterpolation.apply(0, screenTrans.initialScale, screenTrans.timer.ratio());
        sprite3D.renderable.scale.set(interp, interp, interp);
        
        if (screenTrans.timer.canDoEvent()) {
            sprite3D.renderable.scale.set(screenTrans.initialScale, screenTrans.initialScale, screenTrans.initialScale);
            nextStage(screenTrans);
        }
    }
    
    private void zoomIn(Entity entity, ScreenTransitionComponent screenTrans) {
        CameraSystem cameraSystem = getEngine().getSystem(CameraSystem.class);
        cameraSystem.setZoomZero();
    
        //lock cam onto entity
        TransformComponent transform = Mappers.transform.get(entity);
        GameScreen.cam.position.set(transform.pos.x, transform.pos.y, 0);
        
        //wait till zoomed all the way into or close enough
        if (MyScreenAdapter.cam.zoom <= 0.2f) {
            nextStage(screenTrans);
        }
    }
    
    private void zoomOut(Entity entity, ScreenTransitionComponent screenTrans) {
        //cam was 0 from take off, set zoom to proper default
        float zoomTarget = getEngine().getSystem(CameraSystem.class).setZoomToDefault(entity);
        
        //when camera has reached target,
        if (MyScreenAdapter.cam.zoom >= zoomTarget) {
            screenTrans.timer.reset();
            nextStage(screenTrans);
        }
    }
    
    private void fadeIn(ScreenTransitionComponent screenTrans) {
        switch (screenOverlay.getFadeState()) {
            case off:
                screenOverlay.fadeIn();
                break;
            case on:
                nextStage(screenTrans);
                break;
        }
    }
    
    private void fadeOut(ScreenTransitionComponent screenTrans) {
        switch (screenOverlay.getFadeState()) {
            case off:
                nextStage(screenTrans);
                break;
            case on:
                screenOverlay.fadeOut();
                break;
        }
    }
    
    private void landOnPlanet(GameScreen gameContext, Entity entity, ScreenTransitionComponent screenTrans) {
        //reset sprite scale to previous
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        sprite3D.renderable.scale.set(screenTrans.initialScale, screenTrans.initialScale, screenTrans.initialScale);
        
        //reset camera zoom
        if (Mappers.camFocus.get(entity) != null) {
            getEngine().getSystem(CameraSystem.class).setZoomToDefault(entity);
        }
    
        //stop movement
        Mappers.physics.get(entity).body.setLinearVelocity(0, 0);
        
        //actually perform switch
        gameContext.switchScreen(entity, screenTrans.planet);
    }
    
    private void takeOff(GameScreen gameContext, Entity entity, ScreenTransitionComponent screenTrans) {
        //set size to 0 so texture can grow
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        screenTrans.initialScale = sprite3D.renderable.scale.x;
        sprite3D.renderable.scale.set(0, 0, 0);
    
        if (Mappers.camFocus.get(entity) != null) {
            getEngine().getSystem(CameraSystem.class).setZoomToDefault(entity);
        }
        
        gameContext.switchScreen(entity, null);
    
        //set player position to last known planet position
        Vector2 lastKnownPlanetPosition = Mappers.transform.get(screenTrans.planet).pos;
        TransformComponent playerTransform = Mappers.transform.get(entity);
        Body playerBody = Mappers.physics.get(entity).body;
        playerTransform.pos.set(lastKnownPlanetPosition);
        playerBody.setTransform(lastKnownPlanetPosition, MathUtils.random(MathUtils.PI2));
        MyScreenAdapter.cam.position.set(lastKnownPlanetPosition, MyScreenAdapter.cam.position.z);
        Gdx.app.debug(this.getClass().getSimpleName(), "Set entity to last known planet position: " + lastKnownPlanetPosition);
    }
    
    private void syncLoadPosition(Entity entity, ScreenTransitionComponent screenTrans) {
        long desiredSeed = screenTrans.planet.getComponent(SeedComponent.class).seed;
        Gdx.app.debug(this.getClass().getSimpleName(), DebugUtil.objString(entity) + " is waiting for " + desiredSeed);
    
        //set player position to last known planet position
        Vector2 lastKnownPlanetPosition = Mappers.transform.get(screenTrans.planet).pos;
        TransformComponent playerTransform = Mappers.transform.get(entity);
        Body playerBody = Mappers.physics.get(entity).body;
        playerTransform.pos.set(lastKnownPlanetPosition);
        playerBody.setTransform(lastKnownPlanetPosition, MathUtils.random(MathUtils.PI2));
        MyScreenAdapter.cam.position.set(lastKnownPlanetPosition, MyScreenAdapter.cam.position.z);
        Gdx.app.debug(this.getClass().getSimpleName(), "Set entity to last known planet position: " + lastKnownPlanetPosition);
        
        //wait for planet to load (astronomical bodies are loaded by another system)
        Family astro = Family.all(PlanetComponent.class, SeedComponent.class).get();
        ImmutableArray<Entity> astroObjects = getEngine().getEntitiesFor(astro);
        for (Entity astroEnt : astroObjects) {
            if (Mappers.seed.get(astroEnt).seed == desiredSeed) {
                Gdx.app.log(this.getClass().getSimpleName(), "FOUND SEED " + desiredSeed);
                
                //sync entity position with planet that it is leaving from
                Vector2 syncPos = Mappers.transform.get(astroEnt).pos;
                OrbitComponent orbitComp = Mappers.orbit.get(astroEnt);
                if (orbitComp != null && orbitComp.parent != null) {
                    syncPos = OrbitSystem.getTimeSyncedPos(orbitComp, GameScreen.getGameTimeCurrent());
                }
                
                playerBody.setTransform(syncPos, playerBody.getAngle());
                playerBody.setLinearVelocity(orbitComp.velocity);
                playerTransform.pos.set(syncPos);
                MyScreenAdapter.cam.position.set(syncPos, MyScreenAdapter.cam.position.z);
                
                Gdx.app.log(this.getClass().getSimpleName(), "Sync position " + syncPos);
                
                nextStage(screenTrans);
                break;
            }
        }
    }
    
    private static void pause(ScreenTransitionComponent screenTrans) {
        if (screenTrans.timer.tryEvent()) {
            nextStage(screenTrans);
        }
    }
    
    private static void exit(Entity entity, ScreenTransitionComponent screenTrans) {
        if (Mappers.vehicle.get(entity) != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control != null) {
                control.changeVehicle = true;
                nextStage(screenTrans);
            }
        }
    }
    
}
