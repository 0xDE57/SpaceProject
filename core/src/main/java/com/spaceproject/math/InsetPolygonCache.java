package com.spaceproject.math;

import com.badlogic.gdx.math.Vector2;

/**
 * Reusable, allocation-free inset polygon resize helper. Construct with maxVertices (7 for box2D).
 *
 * Usage:
 *   InsetPolygonCache cache = new InsetPolygonCache(7);
 *   // Option A: you already know winding
 *   float[] insetVerts = cache.insetPolygon(originalVerts, lineWidth/2f, ccw);
 *   // Option B: let the helper detect winding from Polygon
 *   float[] insetVerts2 = cache.insetPolygon(polygon, lineWidth/2f);
 *
 * Note: the returned float[] points to an internal buffer that will be overwritten
 * on the next call. Copy it if you need it to survive.
 */
public final class InsetPolygonCache {
    
    private static final float EPS = 1e-8f;
    
    private final int maxVertices;
    private final Vector2[] outVerts;
    private final Vector2 pPrev = new Vector2();
    private final Vector2 pCur  = new Vector2();
    private final Vector2 pNext = new Vector2();
    private final Vector2 ePrev = new Vector2();
    private final Vector2 eNext = new Vector2();
    private final Vector2 nPrev = new Vector2();
    private final Vector2 nNext = new Vector2();
    private final Vector2 q1    = new Vector2(); // shifted point on previous edge
    private final Vector2 q2    = new Vector2(); // shifted point on next edge
    private final Vector2 avg   = new Vector2(); // average normal fallback
    private final float[] result; // preallocated float[] returned to caller
    
    public InsetPolygonCache(int maxVertices) {
        if (maxVertices < 3) throw new IllegalArgumentException("maxVertices must be >= 3");
        this.maxVertices = maxVertices;
        outVerts = new Vector2[maxVertices];
        for (int i = 0; i < maxVertices; i++) outVerts[i] = new Vector2();
        result = new float[maxVertices * 2];
    }
    
    /**
     * Inset polygon vertices by 'inset'. The vertices array is x,y pairs length >= 6.
     * 'ccw' should be true if vertices are counter-clockwise; if false the inset direction is flipped.
     * Returns a reused float[] with the inset vertices (only first n*2 entries are valid).
     */
    public float[] insetPolygon(float[] vertices, float inset, boolean ccw) {
        if (vertices == null) throw new IllegalArgumentException("vertices null");
        int n = vertices.length / 2;
        if (n < 3) return null;
        if (n > maxVertices) throw new IllegalArgumentException("vertex count " + n + " exceeds maxVertices " + maxVertices);
        
        if (!ccw) inset = -inset;
        
        for (int i = 0; i < n; i++) {
            int im1 = (i - 1 + n) % n;
            int ip1 = (i + 1) % n;
            
            // load points
            pPrev.set(vertices[im1 * 2], vertices[im1 * 2 + 1]);
            pCur.set(vertices[i * 2], vertices[i * 2 + 1]);
            pNext.set(vertices[ip1 * 2], vertices[ip1 * 2 + 1]);
            
            // edge vectors
            ePrev.set(pCur).sub(pPrev);
            eNext.set(pNext).sub(pCur);
            
            float lenPrev2 = ePrev.len2();
            float lenNext2 = eNext.len2();
            
            if (lenPrev2 < EPS || lenNext2 < EPS) {
                // degenerate: leave vertex in place (or move slightly along nPrev if available)
                outVerts[i].set(pCur);
                result[i * 2]     = outVerts[i].x;
                result[i * 2 + 1] = outVerts[i].y;
                continue;
            }
            
            ePrev.nor();
            eNext.nor();
            
            // inward normals (left-of-edge for CCW)
            nPrev.set(-ePrev.y, ePrev.x);
            nNext.set(-eNext.y, eNext.x);
            
            // Build shifted-line points q1 and q2 (no allocations)
            q1.set(pPrev.x + nPrev.x * inset, pPrev.y + nPrev.y * inset); // point on shifted prev edge
            q2.set(pCur.x  + nNext.x * inset, pCur.y  + nNext.y * inset); // point on shifted next edge
            
            // Attempt intersection of lines (q1 + t*ePrev) and (q2 + s*eNext)
            if (intersectLines(q1, ePrev, q2, eNext, outVerts[i])) {
                // outVerts[i] was set by intersectLines
            } else {
                // fallback: move along average normal
                avg.set(nPrev).add(nNext);
                if (avg.len2() < EPS) {
                    // nearly straight - just shift along one normal
                    outVerts[i].set(pCur.x + nPrev.x * inset, pCur.y + nPrev.y * inset);
                } else {
                    avg.nor();
                    outVerts[i].set(pCur.x + avg.x * inset, pCur.y + avg.y * inset);
                }
            }
            
            result[i * 2]     = outVerts[i].x;
            result[i * 2 + 1] = outVerts[i].y;
        }
        
        // Note: only the first n*2 entries in result are valid for this polygon
        return result;
    }
    
    /**
     * Intersection of two parametric lines: p1 + t*d1 and p2 + s*d2.
     * Writes intersection into 'out' and returns true; returns false if nearly parallel.
     *
     * No allocations.
     */
    private boolean intersectLines(Vector2 p1, Vector2 d1, Vector2 p2, Vector2 d2, Vector2 out) {
        // solve p1 + t*d1 = p2 + s*d2  =>  t*d1 - s*d2 = p2 - p1
        float a1 = d1.x, b1 = -d2.x, c1 = p2.x - p1.x;
        float a2 = d1.y, b2 = -d2.y, c2 = p2.y - p1.y;
        float det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < 1e-6f) return false;
        float t = (c1 * b2 - c2 * b1) / det;
        out.set(p1.x + d1.x * t, p1.y + d1.y * t);
        return true;
    }
}
