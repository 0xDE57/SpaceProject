package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DashComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;

public class DashSystem extends IteratingSystem {
    
    public DashSystem() {
        super(Family.all(DashComponent.class, PhysicsComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        DashComponent dash = Mappers.dash.get(entity);
        if (dash.activate && dash.dashTimeout.tryEvent()) {
            ControllableComponent control = Mappers.controllable.get(entity);
            Body body = Mappers.physics.get(entity).body;
            
            Vector2 impulse = MyMath.vector(control.angleTargetFace, dash.impulse);
            body.applyLinearImpulse(impulse, body.getPosition(), true);
        }
    }
    
}
