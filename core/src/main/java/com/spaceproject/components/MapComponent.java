package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

public class MapComponent implements Component {
    /* color of marker */
    public Color color;
    
    /* distance entity must be from player to show up on map */
    public int distance;
}
