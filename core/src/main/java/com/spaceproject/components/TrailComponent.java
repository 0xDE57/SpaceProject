package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.utility.SimpleTimer;


public class TrailComponent implements Component {
    
    //xy = position, z = velocity
    public Vector3[] path;
    
    // states: shield protect,shield on, damage, off, on, boost, hyper -> -3, -2, -1, 0, 1, 2, 3
    public byte[] state;
    
    //the current head of the path
    public int indexHead;
    
    public int stepCount;
    
    public int zOrder;
    
    public Style style;

    public enum Style {
        norender, solid, velocity, state, rainbow
    }
    
    public Color color;

    public SimpleTimer updateTimer;

    public long time;
    
}
