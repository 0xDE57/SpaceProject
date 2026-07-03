package com.spaceproject.components;


import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class AISpawnComponent implements Component {
    
    public AIComponent.State state;
    
    public SimpleTimer spawnTimer;
    
    public long maxSpawn;
    
    public int spawnCount;
    
}
