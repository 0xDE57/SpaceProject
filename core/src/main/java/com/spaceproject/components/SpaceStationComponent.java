package com.spaceproject.components;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.spaceproject.utility.SimpleTimer;

public class SpaceStationComponent implements Component {
    
    public float velocity;
    
    public Entity parentOrbitBody;
    
    public Entity dockPortA, dockPortB, dockPortC, dockPortD;

    public SimpleTimer lastDockedTimer = new SimpleTimer(1500);

}
