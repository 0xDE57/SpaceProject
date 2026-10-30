package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.utility.SimpleTimer;


public class TrailComponent implements Component {
    
    //xy = position, z = velocity
    public Vector3[] path;

    //endpoint for each path point, eg: rotated endOffset for stateRotate
    public Vector2[] pathEnd;

    public Vector2 offset;

    //offset from each path point to its endpoint, rotated by current rotation (stateRotate)
    public Vector2 endOffset;
    
    // states: shield protect,shield on, damage, off, on, boost, hyper -> -3, -2, -1, 0, 1, 2, 3
    public byte[] state;
    
    //the current head of the path
    public int indexHead;
    
    public int zOrder;
    
    public Style style;

    public enum Style {
        norender, solid, velocity, state, stateRotate, rainbow
    }
    
    public Color color;

    public SimpleTimer updateTimer;

    public long time;
    
}
