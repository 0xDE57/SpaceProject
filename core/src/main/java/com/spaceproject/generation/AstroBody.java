package com.spaceproject.generation;


import com.badlogic.gdx.math.Vector2;
import com.spaceproject.math.MyMath;

public class AstroBody {
    public int x, y;
    public long seed;

//BarycenterComponent.AstronomicalBodyType type;
    
    public AstroBody(Vector2 location) {
        x = (int) location.x;
        y = (int) location.y;
        seed = MyMath.getSeed(x, y);
/*
        switch (MathUtils.random(2)) {
			case 0: type = uniStellar; break;
			case 1: type = multiStellar; break;
			case 2: type = roguePlanet; break;
		}
*/
    }
}
