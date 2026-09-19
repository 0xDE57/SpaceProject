package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.PassiveShieldComponent;
import com.spaceproject.utility.Mappers;

public class PassiveShieldSystem extends IteratingSystem {
    
    public PassiveShieldSystem() {
        super(Family.all(PassiveShieldComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PassiveShieldComponent shield = Mappers.passiveShield.get(entity);
        
        if (!shield.cooldownBroken.canDoEvent())
            return;
        
        //auto restore
        if (shield.shield < shield.maxShield && shield.cooldownDamaged.canDoEvent()) {
            shield.shield += shield.restoreRate * deltaTime;
            
            if (shield.shield >= shield.maxShield) {
                shield.shield = shield.maxShield;
                //todo: shield fully restored sfx
            }
        }
    }
}
