package com.spaceproject.math;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class PolygonUtil {
    
    static final Vector2 tempcenter = new Vector2();
    static final Vector2 tempvec1 = new Vector2();
    static final Vector2 tempvec2 = new Vector2();
    
    public static BoundingBox calculateBoundingBox(Body body) {
        BoundingBox boundingBox = null;
        
        for (Fixture fixture : body.getFixtureList()) {
            if (boundingBox == null) {
                boundingBox = calculateBoundingBox(fixture);
            } else {
                boundingBox.ext(calculateBoundingBox(fixture));
            }
        }
        
        return boundingBox;
    }
    
    /**
     * Calculates a {@link BoundingBox} for the given {@link Fixture}. It will
     * be in physics/world coordinates.
     * credit: gist.github.com/nooone/8363982
     */
    public static BoundingBox calculateBoundingBox(Fixture fixture) {
        BoundingBox boundingBox = new BoundingBox();
        switch (fixture.getShape().getType()) {
            case Polygon: {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                
                Vector2 vertex = new Vector2();
                shape.getVertex(0, vertex);
                vertex = fixture.getBody().getWorldPoint(vertex);
                boundingBox = new BoundingBox(new Vector3(vertex, 0), new Vector3(vertex, 0));
                for (int v = 1; v < shape.getVertexCount(); v++) {
                    shape.getVertex(v, vertex);
                    boundingBox.ext(new Vector3(fixture.getBody().getWorldPoint(vertex), 0));
                }
                
                break;
            }
            case Circle:
                // TODO implement
                //fixture.getShape().getRadius()
                break;
            case Chain:
                // TODO implement
                break;
            case Edge:
                // TODO implement
                break;
        }
        
        return boundingBox;
    }
    
    public static Rectangle getBoundingRectangle(float[] vertices) {
        float minX = vertices[0];
        float minY = vertices[1];
        float maxX = vertices[0];
        float maxY = vertices[1];
        
        final int numFloats = vertices.length;
        for (int i = 2; i < numFloats; i += 2) {
            minX = minX > vertices[i] ? vertices[i] : minX;
            minY = minY > vertices[i + 1] ? vertices[i + 1] : minY;
            maxX = maxX < vertices[i] ? vertices[i] : maxX;
            maxY = maxY < vertices[i + 1] ? vertices[i + 1] : maxY;
        }
        
        Rectangle bounds = new Rectangle();
        bounds.x = minX;
        bounds.y = minY;
        bounds.width = maxX - minX;
        bounds.height = maxY - minY;
        
        return bounds;
    }
    
    /***
     * https://stackoverflow.com/a/48542735
     */
    public static boolean overlaps(Polygon polygon, Circle circle) {
        float[] vertices = polygon.getTransformedVertices();
        tempcenter.set(circle.x, circle.y);
        float squareRadius = circle.radius * circle.radius;
        for (int i = 0; i < vertices.length; i += 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(tempvec1.set(vertices[vertices.length - 2], vertices[vertices.length - 1]),
                        tempvec2.set(vertices[i], vertices[i + 1]), tempcenter, squareRadius))
                    return true;
            } else {
                if (Intersector.intersectSegmentCircle(tempvec1.set(vertices[i - 2], vertices[i - 1]), tempvec2.set(vertices[i], vertices[i + 1]), tempcenter, squareRadius))
                    return true;
            }
        }

        return polygon.contains(circle.x, circle.y);
    }

    public final static Vector3 cacheVector3 = new Vector3();
    public final static Vector2 cacheVector2 = new Vector2();

    /**
     * Calculate smallest possible circle that intersects
     * each vertex of a triangle defined by vertex a,b,c.
     * https://gist.github.com/mutoo/5617691
     *
     * effectively merges position and radius properties into a single call:
     * GeometryUtils.triangleCircumcenter();
     * GeometryUtils.triangleCircumradius();
     * without having to calculate (recalculate) both properties separately.
     *
     * @param a
     * @param b
     * @param c
     * @return circumcirle of triangle in a Vector3 with position in x,y and radius in z.
     */
    public static Vector3 circumcircle(Vector2 a, Vector2 b, Vector2 c) {
        float EPSILON = 1.0f / 1048576.0f;
        
        float fabsy1y2 = Math.abs(a.y - b.y),
                fabsy2y3 = Math.abs(b.y - c.y),
                xc, yc, m1, m2, mx1, mx2, my1, my2, dx, dy;
        
        /* Check for coincident points */
        if (fabsy1y2 < EPSILON && fabsy2y3 < EPSILON) {
            Gdx.app.log("circumcircle", "Eek! Coincident points!");
        }
        
        if (fabsy1y2 < EPSILON) {
            m2 = -((c.x - b.x) / (c.y - b.y));
            mx2 = (b.x + c.x) / 2.0f;
            my2 = (b.y + c.y) / 2.0f;
            xc = (b.x + a.x) / 2.0f;
            yc = m2 * (xc - mx2) + my2;
        } else if (fabsy2y3 < EPSILON) {
            m1 = -((b.x - a.x) / (b.y - a.y));
            mx1 = (a.x + b.x) / 2.0f;
            my1 = (a.y + b.y) / 2.0f;
            xc = (c.x + b.x) / 2.0f;
            yc = m1 * (xc - mx1) + my1;
        } else {
            m1 = -((b.x - a.x) / (b.y - a.y));
            m2 = -((c.x - b.x) / (c.y - b.y));
            mx1 = (a.x + b.x) / 2.0f;
            mx2 = (b.x + c.x) / 2.0f;
            my1 = (a.y + b.y) / 2.0f;
            my2 = (b.y + c.y) / 2.0f;
            xc = (m1 * mx1 - m2 * mx2 + my2 - my1) / (m1 - m2);
            yc = (fabsy1y2 > fabsy2y3) ? m1 * (xc - mx1) + my1 : m2 * (xc - mx2) + my2;
        }
        
        dx = b.x - xc;
        dy = b.y - yc;
        float radius = (float) Math.sqrt(dx * dx + dy * dy);
        return cacheVector3.set(xc, yc, radius);
    }

    /** Calculate Incircle: compute inscribed circle of a triangle.
     * NOTE: expects area to be calculated before calling
     * @return incircle of triangle in a Vector3 with incenter in x,y and inradius in z.
     */
    public static Vector3 incircle(Vector2 a, Vector2 b, Vector2 c, float area) {
        //delta a, b and c are the side lengths OPPOSITE vertex A, B and C
        float da = b.dst(c);
        float db = c.dst(a);
        float dc = a.dst(b);
        float dt = da + db + dc; //delta total
        float x = (da * a.x + db * b.x + dc * c.x) / dt;
        float y = (da * a.y + db * b.y + dc * c.y) / dt;

        //calculate inRadius
        float p = dt / 2;// p = semi-perimeter of the circle
        float r = area / p;
        return cacheVector3.set(x, y, r);//todo: out parameter instead of cache?
    }

    /** Calculate Orthocenter
     * coincides with the circumcenter, incenter and centroid for an equilateral triangle,
     * coincides with the right-angled vertex for right triangles,
     * lies inside the triangle for acute triangles,
     * lies outside the triangle in obtuse triangles.
     */
    public static Vector2 orthocenter(Vector2 a, Vector2 b, Vector2 c) {
        // slopes
        float slopeAB = (b.y - a.y) / (b.x - a.x);
        float slopeBC = (c.y - b.y) / (c.x - b.x);
        float slopeCA = (a.y - c.y) / (a.x - c.x);

        // altitudes slopes (negative reciprocals)
        float altSlopeC = -1 / slopeAB;
        float altSlopeA = -1 / slopeBC;
        float altSlopeB = -1 / slopeCA;

        // altitudes y-intercepts
        float interceptC = c.y - altSlopeC * c.x;
        float interceptA = a.y - altSlopeA * a.x;
        float interceptB = b.y - altSlopeB * b.x;

        // intersection (orthocenter)
        // we only need 2 altitudes, as the intersection of any two altitudes will also lie on the third,
        // but if any vertices are co-linear then slope = 0 giving infinite altitude.
        // only non-infinite y-intercepts must be used for intersection.
        float orthocenterX;
        float orthocenterY;
        if (Float.isInfinite(altSlopeC)) {
            orthocenterX = (interceptA - interceptB) / (altSlopeB - altSlopeA);
            orthocenterY = altSlopeB * orthocenterX + interceptB;
        } else if (Float.isInfinite(altSlopeA)) {
            orthocenterX = (interceptC - interceptB) / (altSlopeB - altSlopeC);
            orthocenterY = altSlopeB * orthocenterX + interceptB;
        } else {
            orthocenterX = (interceptA - interceptC) / (altSlopeC - altSlopeA);
            orthocenterY = altSlopeC * orthocenterX + interceptC;
        }

        return cacheVector2.set(orthocenterX, orthocenterY);
    }

    public static void excircle(Vector2 a, Vector2 b, Vector2 c, float area, Vector3 exCircleA, Vector3 exCircleB, Vector3 exCircleC) {
        float da = b.dst(c);
        float db = c.dst(a);
        float dc = a.dst(b);
        float s = (da + db + dc) / 2.0f;

        float exRadiusA = area / (s - da);
        float exRadiusB = area / (s - db);
        float exRadiusC = area / (s - dc);

        exCircleA.set(
                (-da * a.x + db * b.x + dc * c.x) / (-da + db + dc),
                (-da * a.y + db * b.y + dc * c.y) / (-da + db + dc),
                exRadiusA);
        exCircleB.set(
                (da * a.x - db * b.x + dc * c.x) / (da - db + dc),
                (da * a.y - db * b.y + dc * c.y) / (da - db + dc),
                exRadiusB);
        exCircleC.set(
                (da * a.x + db * b.x - dc * c.x) / (da + db - dc),
                (da * a.y + db * b.y - dc * c.y) / (da + db - dc),
                exRadiusC);
    }


    /** Ratio of circumradius to shortest edge as a measure of triangle quality.
     * copy of GeometryUtils.triangleQuality() modified to use provided circumradius instead of recalculating.
     * NOTE: this function expects triangle cords to be relative to the centroid origin (0, 0)!
     */
    public static float triangleQuality(float x1, float y1, float x2, float y2, float x3, float y3, float circumRadius) {
        float sqLength1 = x1 * x1 + y1 * y1;
        float sqLength2 = x2 * x2 + y2 * y2;
        float sqLength3 = x3 * x3 + y3 * y3;
        return (float)Math.sqrt(Math.min(sqLength1, Math.min(sqLength2, sqLength3))) / circumRadius;
    }


    /** Returns the centroid for the specified non-self-intersecting polygon. */
    static public Vector2 doublePolygonCentroid(double[] polygon, int offset, int count, Vector2 centroid) {
        if (count < 6) throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.");

        double area = 0, x = 0, y = 0;
        int last = offset + count - 2;
        double x1 = polygon[last], y1 = polygon[last + 1];
        for (int i = offset; i <= last; i += 2) {
            double x2 = polygon[i], y2 = polygon[i + 1];
            double a = x1 * y2 - x2 * y1;
            area += a;
            x += (x1 + x2) * a;
            y += (y1 + y2) * a;
            x1 = x2;
            y1 = y2;
        }
        if (area == 0) {
            centroid.x = 0;
            centroid.y = 0;
        } else {
            area *= 0.5f;
            centroid.x = (float) (x / (6 * area));
            centroid.y = (float) (y / (6 * area));
        }
        return centroid;
    }

    /** Computes the area for a convex polygon. */
    static public double doublePolygonArea (double[] polygon, int offset, int count) {
        float area = 0;
        int last = offset + count - 2;
        double x1 = polygon[last], y1 = polygon[last + 1];
        for (int i = offset; i <= last; i += 2) {
            double x2 = polygon[i], y2 = polygon[i + 1];
            area += x1 * y2 - x2 * y1;
            x1 = x2;
            y1 = y2;
        }
        return area * 0.5f;
    }
    
}
