package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class ShieldComponent implements Component {

    public enum State { off, on, charge, discharge, overheat }
    
    public State state = State.off;
    
    public boolean activate;
    
    public long lastHit;
    
    public float defence;
    
    public float radius;
    
    public float maxRadius;
    
    public SimpleTimer animTimer;

    public float heat;

    public float overHeat;

    public float heatResistance;

    public float cooldownRate;

}
