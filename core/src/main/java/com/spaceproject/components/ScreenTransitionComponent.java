package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Interpolation;
import com.spaceproject.utility.SimpleTimer;

public class ScreenTransitionComponent implements Component {
    
    public enum TakeOffAnimStage {
        screenEffectFadeIn,
        transition,
        sync,//load
        screenEffectFadeOut,
        zoomOut,
        grow,
        end;
        
        private static final TakeOffAnimStage[] VALUES = values();
        
        public TakeOffAnimStage next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }
    
    public enum LandAnimStage {
        shrink,
        zoomIn,
        screenEffectFadeIn,
        transition,
        //load,
        screenEffectFadeOut,
        pause,
        exit,
        end;
        
        private static final LandAnimStage[] VALUES = values();
        
        public LandAnimStage next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }
    
    public LandAnimStage landStage;
    public LandAnimStage curLandStage;
    public TakeOffAnimStage takeOffStage;
    public TakeOffAnimStage curTakeOffStage;
    
    public Entity planet;
    
    public SimpleTimer timer;
    public Interpolation animInterpolation;
    
    public float rotation;
    
    public float initialScale;
}
