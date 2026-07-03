package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.spaceproject.utility.Steering;

public class AIComponent implements Component {
    
    public enum State {
        idle,
        wander,
        landOnPlanet,
        takeOffPlanet,
        attack,
        follow
    }
    
    public State state;
    
    public Steering steering;
    
    public Entity attackTarget; //todo: should be in attack state data?
    
    public Entity followTarget; //todo: should be in follow state data?
    
    public Entity planetTarget; //todo: should be in land state data?
    
    
}
