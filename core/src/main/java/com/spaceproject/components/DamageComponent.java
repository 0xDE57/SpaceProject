package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

public class DamageComponent implements Component {
    
    public Entity source;
    public float damage;
    
}
