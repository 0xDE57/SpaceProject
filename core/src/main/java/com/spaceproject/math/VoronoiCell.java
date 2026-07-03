package com.spaceproject.math;

import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;

class VoronoiCell {
    
    Polygon poly;
    FloatArray verticies = new FloatArray();
    
    public void addVertex(Vector2 point) {
        verticies.add(point.x);
        verticies.add(point.y);
        
    }
    
    public void setVerticies() {
		/*
		FloatArray verts = verticies;
		float[] floatArray = new float[verts.length];
		
		int i = 0;
		for (Float f : verticies) {
		    floatArray[i++] = (f != null ? f : Float.NaN);
		}*/
        
        ConvexHull convex = new ConvexHull();
        FloatArray hull = convex.computePolygon(verticies, false);
        poly.setVertices(hull.toArray());
    }
    
}
