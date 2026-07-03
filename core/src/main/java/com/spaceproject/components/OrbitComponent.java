package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;

public class OrbitComponent implements Component {
    //the entity to orbit
    public Entity parent;
    
    public final Vector2 velocity = new Vector2();
    
    //angle from parent in radians
    public float angle, startAngle;
    
    //orbit parameters
    public float radialDistance; //distance from entity to rotate
    public float tangentialSpeed; //how fast to orbit
    
    //how fast to rotate
    public float rotSpeed;
    
    //rotation direction
    public boolean rotateClockwise;
    
    public boolean isProcessed;
    
}
