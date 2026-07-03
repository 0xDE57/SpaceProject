package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;


public class ChargeCannonComponent implements Component {
    
    public Entity projectileEntity;
    
    public Vector2 anchorVec; //offset relative to ship
    
    public float aimAngle; //relative to anchor
    
    public float baseDamage;
    
    public float velocity;
    
    public float size;
    
    public float minSize;
    
    public float maxSize;
    
    public SimpleTimer growRateTimer;
    
    //public SimpleTimer cooldownTimer;//between shots?
    
    public boolean isCharging;
    
    public int shotsFired;
    
}
