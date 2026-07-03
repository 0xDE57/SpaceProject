package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;


public class DashComponent implements Component {
    
    public boolean activate;
    public float impulse;
    public SimpleTimer dashTimeout;
    
}