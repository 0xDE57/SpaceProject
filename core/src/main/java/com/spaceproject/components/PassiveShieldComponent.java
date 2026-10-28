package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class PassiveShieldComponent implements Component {
    
    public float shield;
    public float maxShield;
    public float restoreRate;
    public long cooldownDamaged;
    public long cooldownBroken;
    public long lastHit;
    public long lastBroken;
    
}
