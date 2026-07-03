package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.spaceproject.utility.SimpleTimer;

import java.util.HashMap;

public class VehicleComponent implements Component {
    
    public Entity driver;
    
    public float thrust; //move to engine component as sub entity for ship?
    
    public Rectangle dimensions;
    
    public SimpleTimer toolSwapTimer;

    public enum Tool {
        cannon, laser, tractor;
        //, growcannon

        private static final Tool[] VALUES = values();

        public Tool next() {
            int index = (ordinal() + 1) % VALUES.length;
            return VALUES[index];
        }

        public Tool previous() {
            int index = (ordinal() + VALUES.length - 1) % VALUES.length;
            return VALUES[index];
        }
    }

    public Tool currentTool;

    public HashMap<Integer, Component> tools = new HashMap<>();
    
}
