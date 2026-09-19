package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class PassiveShieldComponent implements Component {
    
    public float shield;
    public float maxShield;
    public float restoreRate;
    public SimpleTimer cooldownDamaged = new SimpleTimer(5000);
    public SimpleTimer cooldownBroken = new SimpleTimer(10000);
    
}
