package com.spaceproject.math;

import com.badlogic.gdx.math.Vector2;

public interface DoubleShape2D {

    /** Returns whether the given point is contained within the shape. */
    boolean contains (Vector2 point);

    /** Returns whether a point with the given coordinates is contained within the shape. */
    boolean contains (double x, double y);

}