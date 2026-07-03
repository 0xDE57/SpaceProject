package com.spaceproject.config;

import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.SpaceProject;

public class EngineConfig extends Config {
    
    public boolean vsync;
    public boolean msaa;
    
    public float renderScale;
    public float viewportWidth;
    public float viewportHeight;
    
    public float sprite2DScale;
    public float sprite3DScale;
    public float bodyScale;
    public float meterPerUnit;
    public int pixelPerUnit;
    
    public int physicsVelocityIterations;
    public int physicsPositionIterations;
    public int physicsStepPerFrame;
    
    public int maxNoiseGenThreads;
    
    
    @Override
    public void loadDefault() {
        vsync = true;
        msaa = true;
        
        renderScale = 30f;
        viewportWidth = 1280;
        viewportHeight = 800;
        
        sprite2DScale = 0.1f;
        sprite3DScale = 0.1f;
        bodyScale = 0.1f;
        meterPerUnit = 0.1f;
        pixelPerUnit = 2;
        
        physicsVelocityIterations = 6;
        physicsPositionIterations = 2;
        physicsStepPerFrame = 120;


        // dynamically choose number of available cores
        // but leave some threads for other applications (don't be greedy)
        // assume desktop can use at least 4 threads
        int availableCPU = Runtime.getRuntime().availableProcessors();
        maxNoiseGenThreads = MathUtils.clamp(availableCPU - 2, 4, 10);
    }
}
