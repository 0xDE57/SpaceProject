package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.spaceproject.components.PassiveShieldComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class PassiveShieldSystem extends IteratingSystem {
    
    public PassiveShieldSystem() {
        super(Family.all(PassiveShieldComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PassiveShieldComponent shield = Mappers.passiveShield.get(entity);
        
        long now = GameScreen.getGameTimeCurrent();
        
        if (now - shield.lastBroken < shield.cooldownBroken)
            return;
        
        //auto restore
        if (shield.shield < shield.maxShield && now - shield.lastHit < shield.cooldownDamaged)
            return;
        
        float restoreRate = shield.restoreRate;
        //docked ships repair at 2x speed
        if (Mappers.docked.get(entity) != null) {
            restoreRate *= 2;
        }
        
        shield.shield += restoreRate * deltaTime;
        
        if (shield.shield >= shield.maxShield) {
            shield.shield = shield.maxShield;
            //todo: shield fully restored sfx
        }
    }
}
