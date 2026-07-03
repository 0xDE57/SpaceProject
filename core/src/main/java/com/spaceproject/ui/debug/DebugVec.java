package com.spaceproject.ui.debug;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public class DebugVec {
    public Vector2 vecA;
    public Vector2 vecB;
    public Color colorA;
    public Color colorB;
    
    public DebugVec(Vector2 vecA, Vector2 vecB, Color color) {
        this(vecA, vecB, color, color);
    }
    
    public DebugVec(Vector2 vecA, Vector2 vecB, Color colorA, Color colorB) {
        this.vecA = vecA;
        this.vecB = vecB;
        this.colorA = colorA;
        this.colorB = colorB;
    }
    
}
