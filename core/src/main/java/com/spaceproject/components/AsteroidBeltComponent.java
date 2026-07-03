package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class AsteroidBeltComponent implements Component {

    /* todo: belt composition. what % of asteroid types to spawn?
    pockets?
    distribution?

    Composition[][] {
        "Fe", 36%,
        "Co", 14%,
        "H2O", 2%,
    }*/


    //distance from parent body
    public float radius;
    
    //width of band centered on radius
    public float bandWidth;
    
    public boolean clockwise;
    
    public float velocity;
    
    public int spawned;
    
    public int maxSpawn;
    
}
