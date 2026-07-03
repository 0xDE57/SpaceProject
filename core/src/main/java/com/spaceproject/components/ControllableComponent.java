package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class ControllableComponent implements Component {
    
    //movement
    public boolean moveForward;
    public boolean moveLeft;
    public boolean moveRight;
    public boolean moveBack;
    
    public float movementMultiplier;//analog control [0-1]
    public float angleTargetFace;
    
    //actions
    public boolean attack;
    public boolean changeVehicle;
    public boolean interact;
    public boolean canTransition;
    public boolean boost;
    
    //timers
    public SimpleTimer timerVehicle;
    
    public boolean swapWeapon;

    public boolean activelyControlled = true;//flag

}