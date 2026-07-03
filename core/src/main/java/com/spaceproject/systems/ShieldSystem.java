package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.spaceproject.components.*;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.utility.Mappers;

public class ShieldSystem extends IteratingSystem {
    
    public ShieldSystem() {
        super(Family.all(ShieldComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ShieldComponent shield = Mappers.shield.get(entity);
        
        //don't allow shield activation while hyperdrive active
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        if (hyperDrive != null && hyperDrive.state == HyperDriveComponent.State.on) {
            if (shield.state == ShieldComponent.State.on) {
                disengage(entity, shield);
            }
            shield.radius = 0;
            shield.state = ShieldComponent.State.off;
            shield.activate = false;
        }
        SoundSystem soundSys = getEngine().getSystem(SoundSystem.class);
        SoundComponent sound = Mappers.sound.get(entity);
        ControlFocusComponent controlFocus = Mappers.controlFocus.get(entity);

        updateCooldown(shield, deltaTime);
        
        switch (shield.state) {
            case off:
                if (shield.activate) {
                    engage(shield);
                }
    
                //stop loop if entity is controlled player
                if (controlFocus != null) {
                    soundSys.shieldAmbient(sound, false);
                }
                break;
            case on:
                if (!shield.activate) {
                    disengage(entity, shield);
                }
    
                //start loop if entity is controlled player
                if (controlFocus != null) {
                    soundSys.shieldAmbient(sound, true);
                }
                break;
            case charge:
                if (!shield.activate) {
                    shield.state = ShieldComponent.State.discharge;
                    break;
                }
                
                //charge: gain energy
                shield.radius = shield.maxRadius * shield.animTimer.ratio();
                if (shield.radius == shield.maxRadius || shield.animTimer.canDoEvent()) {
                    shield.state = ShieldComponent.State.on;
                    
                    //add shield fixture to body for protection
                    Body body = entity.getComponent(PhysicsComponent.class).body;
                    BodyBuilder.addShieldFixtureToBody(body, shield.maxRadius);
    
                    //if entity is controlled player
                    if (controlFocus != null) {
                        soundSys.shieldOn();
                    }
                }
                break;
            case discharge:
                if (shield.activate) {
                    engage(shield);
                    break;
                }
                
                //discharge: lose energy
                shield.radius = shield.maxRadius * (1 - shield.animTimer.ratio());
                if (shield.radius <= 0  || shield.animTimer.canDoEvent()) {
                    shield.state = ShieldComponent.State.off;
                }
                break;
        }
    }

    private void updateCooldown(ShieldComponent shield, float deltaTime) {
        if (shield.heat == 0) return;

        shield.heat -= shield.cooldownRate * deltaTime;
        if (shield.heat < 0) shield.heat = 0;
    }

    private void engage(ShieldComponent shield) {
        float shieldReactivateThreshold = 0.3f;
        
        //reactivate if still enough charge
        if (shield.animTimer.ratio() >= shieldReactivateThreshold) {
            shield.animTimer.flipRatio();
        } else {
            shield.animTimer.reset();
        }
        
        shield.state = ShieldComponent.State.charge;
    }
    
    private void disengage(Entity entity, ShieldComponent shield) {
        shield.state = ShieldComponent.State.discharge;
        
        //destroy shield fixture
        Body body = entity.getComponent(PhysicsComponent.class).body;
        Fixture circleFixture = body.getFixtureList().get(body.getFixtureList().size - 1);
        body.destroyFixture(circleFixture);
        
        shield.animTimer.flipRatio();
        
        //if entity is controlled player
        ControlFocusComponent controlFocus = Mappers.controlFocus.get(entity);
        if (controlFocus != null) {
            SoundSystem sound = getEngine().getSystem(SoundSystem.class);
            sound.shieldOff();
        }
    }
    
}
