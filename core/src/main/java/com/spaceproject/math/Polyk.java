package com.spaceproject.math;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PolyK - Slice() converted to use native libGDX math utilities.
 * Credit: Ivan Kuckir
 * Ported from: https://github.com/martywallace/polyk/blob/master/dist/polyk.js
 */
public final class Polyk {
    
    // just a Vector2 with a flag
    private static class VPoint extends Vector2 {
        public boolean flag;
        
        public VPoint() { super(0f, 0f); this.flag = false; }
        
        public VPoint(float x, float y) { super(x, y); this.flag = false; }
    }
    static final VPoint a = new VPoint();
    static final VPoint b = new VPoint();
    static final Polygon original = new Polygon(); 
    
    /** 
     * Slices the polygon with line segment A-B defined by [startX,startY] and [endX,endY].
     * A and B must not lie inside polygon. Returns a list of polygons (each polygon as float[]).
     * Note: vertices is expected as [x1,y1,x2,y2,...]
     */
    public static List<float[]> slice(float[] vertices, float aX, float aY, float bX, float bY) {
        original.setVertices(vertices);
        if (original.contains(aX, aY) || original.contains(bX, bY)) {
            return null; //the original returns a copy of the original, but we don't need duplicates
            /* return copy of original polygon
            float[] copy = new float[p.length];
            System.arraycopy(p, 0, copy, 0, p.length);
            List<float[]> single = new ArrayList<float[]>();
            single.add(copy);
            return single;*/
        }
        a.set(aX, aY);
        b.set(bX, bY);
        
        List<VPoint> intersections = new ArrayList<>();
        List<VPoint> points = new ArrayList<>();
        
        for (int i = 0; i < vertices.length; i += 2) {
            points.add(new VPoint(vertices[i], vertices[i + 1]));
        }
        
        // find intersections between segment a-b and each polygon edge
        for (int i = 0; i < points.size(); i++) {
            VPoint p1 = points.get(i);
            VPoint p2 = points.get((i + 1) % points.size());
            VPoint intersection = new VPoint();
            boolean hit = Intersector.intersectSegments(a, b, p1, p2, intersection);
            VPoint firstIntersection = !intersections.isEmpty() ? intersections.get(0) : null;
            VPoint lastIntersection = !intersections.isEmpty() ? intersections.get(intersections.size() - 1) : null;
            
            // Avoid duplicates: check distance from first/last saved intersection
            if (hit && (firstIntersection == null || firstIntersection.dst(intersection) > 1e-10f) && (lastIntersection == null || lastIntersection.dst(intersection) > 1e-10f)) {
                intersection.flag = true;
                intersections.add(intersection);
                
                points.add(i + 1, intersection);
                i++; // skip the newly inserted point
            }
        }
        
        if (intersections.size() < 2) {
            return null;
            /* the original returns a copy of the original, but we don't need duplicates
            float[] copy = new float[vertices.length];
            System.arraycopy(vertices, 0, copy, 0, vertices.length);
            List<float[]> single = new ArrayList<>();
            single.add(copy);
            return single;*/
        }
        
        // sort intersections by distance from a
        final VPoint aa = a;
        intersections.sort((u, v) -> {
            float du = aa.dst(u);
            float dv = aa.dst(v);
            return Float.compare(du, dv);
        });
        
        List<List<VPoint>> polygons = new ArrayList<>();
        int dir = 0;
        
        while (intersections.size() > 1) {
            VPoint point0 = intersections.get(0);
            VPoint point1 = intersections.get(1);
            int index0 = points.indexOf(point0);
            int index1 = points.indexOf(point1);
            boolean solved = false;
            
            if (firstWithFlag(points, index0) == index1) {
                solved = true;
            } else {
                // swap
                point0 = intersections.get(1);
                point1 = intersections.get(0);
                index0 = points.indexOf(point0);
                index1 = points.indexOf(point1);
                if (firstWithFlag(points, index0) == index1) solved = true;
            }
            
            if (solved) {
                dir--;
                List<VPoint> polygon = getPoints(points, index0, index1);
                polygons.add(polygon);
                points = getPoints(points, index1, index0);
                point0.flag = false;
                point1.flag = false;
                // remove first two intersections
                intersections.remove(0);
                intersections.remove(0);
                if (intersections.isEmpty()) {
                    polygons.add(points);
                }
            } else {
                dir++;
                // reverse intersections list
                Collections.reverse(intersections);
            }
            if (dir > 1) break;
        }
        
        List<float[]> result = new ArrayList<>();
        for (List<VPoint> polygon : polygons) {
            float[] newPolygon = new float[polygon.size() * 2];
            for (int j = 0; j < polygon.size(); j++) {
                newPolygon[2 * j] = polygon.get(j).x;
                newPolygon[2 * j + 1] = polygon.get(j).y;
            }
            result.add(newPolygon);
        }
        
        return result;
    }
    
    /** 
     * returns sub-list of points from index0 to index1 (wrap-around) inclusive
     */
    private static List<VPoint> getPoints(List<VPoint> points, int index0, int index1) {
        int n = points.size();
        List<VPoint> result = new ArrayList<>();
        if (index1 < index0) index1 += n;
        for (int i = index0; i <= index1; i++) {
            result.add(points.get(i % n));
        }
        return result;
    }
    
    /**
     * find next index after 'index' whose flag is true
     */
    private static int firstWithFlag(List<VPoint> points, int index) {
        int n = points.size();
        while (true) {
            index = (index + 1) % n;
            if (points.get(index).flag) {
                return index;
            }
        }
    }
    
    private static final float VERT_EPS = 1e-6f;
    
    private static boolean almostEqual(Vector2 a, Vector2 b, float eps) {
        return Math.abs(a.x - b.x) <= eps && Math.abs(a.y - b.y) <= eps;
    }
    
    // Remove consecutive duplicate vertices and ensure wrap-around duplicates removed
    public static float[] removeDuplicateVertices(float[] verts) {
        if (verts == null || verts.length < 6) return verts;
        ArrayList<Vector2> out = new ArrayList<>(verts.length / 2);
        for (int i = 0; i < verts.length; i += 2) {
            Vector2 v = new Vector2(verts[i], verts[i + 1]);
            if (out.isEmpty() || !almostEqual(out.get(out.size() - 1), v, VERT_EPS)) {
                out.add(v);
            }
        }
        // remove last if same as first
        if (out.size() > 1 && almostEqual(out.get(0), out.get(out.size() - 1), VERT_EPS)) {
            out.remove(out.size() - 1);
        }
        if (out.size() < 3) return new float[0];
        float[] result = new float[out.size() * 2];
        for (int i = 0; i < out.size(); i++) {
            result[2 * i] = out.get(i).x;
            result[2 * i + 1] = out.get(i).y;
        }
        return result;
    }
    
    // Remove points that are (nearly) collinear with their neighbors
    private static float[] removeCollinearVertices(float[] verts) {
        if (verts == null || verts.length < 6) return verts;
        ArrayList<Vector2> pts = new ArrayList<>();
        for (int i = 0; i < verts.length; i += 2) pts.add(new Vector2(verts[i], verts[i + 1]));
        int n = pts.size();
        if (n < 3) return new float[0];
        ArrayList<Vector2> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Vector2 prev = pts.get((i - 1 + n) % n);
            Vector2 cur = pts.get(i);
            Vector2 next = pts.get((i + 1) % n);
            // compute cross product of (cur - prev) x (next - cur)
            float ux = cur.x - prev.x;
            float uy = cur.y - prev.y;
            float vx = next.x - cur.x;
            float vy = next.y - cur.y;
            float cross = ux * vy - uy * vx;
            if (Math.abs(cross) > VERT_EPS) {
                out.add(cur);
            } else {
                // if cross is tiny, also guard against near-duplicates: keep if distinct from prev/next
                if (!almostEqual(cur, prev, VERT_EPS) && !almostEqual(cur, next, VERT_EPS)) {
                    // if point is not duplicate but collinear, we still can drop it
                    // so do nothing (drop)
                }
            }
        }
        if (out.size() < 3) return new float[0];
        float[] result = new float[out.size() * 2];
        for (int i = 0; i < out.size(); i++) {
            result[2 * i] = out.get(i).x;
            result[2 * i + 1] = out.get(i).y;
        }
        return result;
    }
    
    /**
     * Sanitize polygon:
     * - remove duplicates
     * - remove collinear points
     * Returns sanitized float[] or null if invalid.
     */
    private static float[] sanitizePolygon(float[] poly) {
        if (poly == null || poly.length < 6) return null;
        float[] p = removeDuplicateVertices(poly);
        if (p.length < 6) return null;
        p = removeCollinearVertices(p);
        if (p.length < 6) return null; //could be less than 3 after removing duplicates or collinear
        return p;
    }
    
}
