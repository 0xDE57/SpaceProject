package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.Vector2;

public class ParticleComponent implements Component {
    
    public enum EffectType {
        shipEngineMain,
        shipEngineLeft,
        shipEngineRight,
        bulletCharge,
        bulletExplode,
        shieldCharge,
        projectileTrail
    }
    
    public EffectType type;
    
    public ParticleEffectPool.PooledEffect pooledEffect;
    
    public Vector2 offset;
    
    public float angle;

}
