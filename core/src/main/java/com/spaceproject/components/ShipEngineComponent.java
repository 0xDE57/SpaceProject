package com.spaceproject.components;


import com.badlogic.ashley.core.Component;

public class ShipEngineComponent implements Component {
    
    public enum State {
        off, on, boost, hyper;
    }
    
    public State engineState;
    
    public float thrust;
    
}
