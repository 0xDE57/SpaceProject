package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class TransformComponent implements Component {
    /* position in world x, y */
    public final Vector2 pos = new Vector2();
    
    /* Orientation in radians */
    public float rotation = 0.0f;
    
    /* render order */
    public byte zOrder;
}
