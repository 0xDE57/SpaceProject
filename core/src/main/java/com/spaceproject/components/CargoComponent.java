package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

import java.util.HashMap;

public class CargoComponent implements Component {

    public HashMap<Integer, Integer> inventory = new HashMap<>();

    public float maxCapacity = -1;
    
    public int credits;
    
    public long lastCollectTime;
    
}
