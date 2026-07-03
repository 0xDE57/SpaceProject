package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.*;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class HyperDriveSystem extends IteratingSystem {
    
    public HyperDriveSystem() {
        super(Family.all(TransformComponent.class, HyperDriveComponent.class).exclude(DockedComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
    
        switch (hyperDrive.state) {
            case off:
                // charge up
                if (hyperDrive.activate) {
                    hyperDrive.state = HyperDriveComponent.State.charging;
                    hyperDrive.chargeTimer.reset();
                }
                break;
            case on:
                // override box2d physics with our velocity
                TransformComponent transform = Mappers.transform.get(entity);
                transform.pos.add(hyperDrive.velocity.cpy().scl(deltaTime));
                break;
            case charging:
                //cancel charge
                if (!hyperDrive.activate) {
                    hyperDrive.state = HyperDriveComponent.State.off;
                }
                
                //engage
                if (hyperDrive.chargeTimer.tryEvent()) {
                    engageHyperDrive(entity, hyperDrive);
                }
                break;
            case cooldown:
                //reset
                if (hyperDrive.coolDownTimer.canDoEvent()) {
                    hyperDrive.state = HyperDriveComponent.State.off;
                }
                break;
        }
    }
    
    private void engageHyperDrive(Entity entity, HyperDriveComponent hyperDrive) {
        if (Mappers.controlFocus.get(entity) != null) {
            GameScreen.setHyper(true);
        }
        
        // disable physics body to override velocity
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        physicsComp.body.setActive(false);
        
        // enable hyper
        hyperDrive.state = HyperDriveComponent.State.on;
        hyperDrive.velocity.set(MyMath.vector(physicsComp.body.getAngle(), hyperDrive.speed));
        hyperDrive.graceTimer.reset();
    
        // if player, make sound
        ControlFocusComponent control = Mappers.controlFocus.get(entity);
        if (control != null) {
            getEngine().getSystem(SoundSystem.class).hyperdriveEngage();
        }
    }
    
    public static void disengageHyperDrive(Entity entity, HyperDriveComponent hyperDrive) {
        if (Mappers.controlFocus.get(entity) != null) {
            GameScreen.setHyper(false);
        }
        
        // disable hyper
        hyperDrive.state = HyperDriveComponent.State.cooldown;
        hyperDrive.coolDownTimer.reset();
        
        // re-enable physics body
        PhysicsComponent physicsComp = Mappers.physics.get(entity);
        float bodyAngle = physicsComp.body.getAngle();
        physicsComp.body.setTransform(entity.getComponent(TransformComponent.class).pos, bodyAngle);
        physicsComp.body.setActive(true);
        //spin when exit hyper, temporarily lose control
        float spin = 50.0f * MathUtils.randomSign();
        physicsComp.body.setAngularVelocity(spin);
        //add tiny bit of movement
        physicsComp.body.setLinearVelocity(MyMath.vector(bodyAngle, 999999));
        //physicsComp.body.setLinearVelocity(0, 0); //freeze
    }
    
}
