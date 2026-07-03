package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class HealthComponent implements Component {
    //health for living things / combat, entity dies upon value reaching 0
    public float health;
    
    //total health
    public float maxHealth;
    
    //timestamp for taking damage
    public long lastHitTime;

    public Entity lastHitSource;
}
