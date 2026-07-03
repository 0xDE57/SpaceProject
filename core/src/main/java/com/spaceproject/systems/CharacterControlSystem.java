package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;

public class CharacterControlSystem extends IteratingSystem {
    
    private EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private ImmutableArray<Entity> vehicles;
    
    private float offsetDist = 2f;//TODO: replace with box2d sensor
    private float faceRotSpeed = 8f;//move to CharacterControlSystemConfig
    
    public CharacterControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class, CharacterComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        controlCharacter(entity, deltaTime);
    }
    
    private void controlCharacter(Entity entity,  float delta) {
        ControllableComponent control = Mappers.controllable.get(entity);
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        
        //make character face mouse/joystick
        faceTarget(control, physicsComp, delta);
        
        if (control.moveForward) {
            CharacterComponent character = Mappers.character.get(entity);
            float walkSpeed = character.walkSpeed * control.movementMultiplier * delta;
            physicsComp.body.applyForceToCenter(MyMath.vector(physicsComp.body.getAngle(), walkSpeed), true);
        }
        
        if (control.changeVehicle) {
            tryEnterVehicle(entity, control);
        }
    }
    
    private void faceTarget(ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleTargetFace, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
    }
    
    private void tryEnterVehicle(Entity characterEntity, ControllableComponent control) {
        if (!control.timerVehicle.canDoEvent())
            return;
        
        control.changeVehicle = false;
        
        //get all vehicles and check if player is close to one
        PhysicsComponent playerPhysics = Mappers.physics.get(characterEntity);
        for (Entity vehicleEntity : vehicles) {
            
            //skip vehicle is occupied
            if (Mappers.vehicle.get(vehicleEntity).driver != null) {
                Gdx.app.log(this.getClass().getSimpleName(),
                        DebugUtil.objString(characterEntity) + " cannot enter " + DebugUtil.objString(vehicleEntity)
                        + " because " + DebugUtil.objString(Mappers.vehicle.get(vehicleEntity).driver) + " is already driving.");
                continue;
            }
            
            //if character is near a vehicle, enter
            PhysicsComponent vehiclePhysics = Mappers.physics.get(vehicleEntity);
            if (playerPhysics.body.getPosition().dst(vehiclePhysics.body.getPosition()) < offsetDist) {
                enterVehicle(characterEntity, vehicleEntity);
                control.timerVehicle.reset();
                return;
            }
        }
    }
    
    private void enterVehicle(Entity characterEntity, Entity vehicleEntity) {
        Gdx.app.log(this.getClass().getSimpleName(), DebugUtil.objString(characterEntity)
                + " entering vehicle " + DebugUtil.objString(vehicleEntity));
        
        //set reference
        Mappers.vehicle.get(vehicleEntity).driver = characterEntity;
        
        // transfer focus & controls to vehicleEntity
        ECSUtil.transferControl(characterEntity, vehicleEntity);
        
        //set cam zoom
        if (Mappers.camFocus.get(vehicleEntity) != null) {
            CameraSystem cameraSystem = getEngine().getSystem(CameraSystem.class);
            cameraSystem.setZoomToDefault(vehicleEntity);
        }
        
        // remove character
        getEngine().removeEntity(characterEntity);
        PhysicsComponent physicsComponent = characterEntity.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;
    }
    
}
