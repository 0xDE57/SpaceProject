package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.utility.SimpleTimer;

public class HyperDriveComponent implements Component {
    
    public enum State { off, on, charging, cooldown }
    
    public State state = State.off;
    
    public boolean activate;
    
    public float speed;
    
    public final Vector2 velocity = new Vector2();
    
    //time it takes to activate
    public SimpleTimer chargeTimer;
    
    //time it takes before can de-activate from active state
    public SimpleTimer graceTimer;
    
    //time it takes to cooldown before can activate again
    public SimpleTimer coolDownTimer;
    
}
