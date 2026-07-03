package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.utility.Mappers;

public class ExpireSystem extends IteratingSystem {
    
    public ExpireSystem() {
        super(Family.all(ExpireComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ExpireComponent expire = Mappers.expire.get(entity);
        if (expire.timer.canDoEvent()) {
            entity.add(new RemoveComponent());
        }
    }
    
}
