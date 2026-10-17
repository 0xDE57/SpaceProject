package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.ItemComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.InsetPolygonCache;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class AsteroidBatchRenderSystem extends IteratingSystem {
    
    final ShapeDrawer shapeDrawer;
    final Batch batch;
    final Texture texture;
    final InsetPolygonCache polygonCache;
    final Color tempColor;
    final Color defaultOutline;
    
    float lineWidth = 0.15f;
    
    JoinType joinType = JoinType.POINTY;
    boolean debugRevealAll = true;
    
    public enum PathMode {
        CENTERED, //current default
        INSET,
        OUTSET
    }
    
    PathMode mode = PathMode.INSET;
    
    public AsteroidBatchRenderSystem() {
        super(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(0, 0);
        texture = new Texture(pixmap); //remember to dispose of later
        pixmap.dispose();
        TextureRegion region = new TextureRegion(texture, 0, 0, 1, 1);
        batch = new SpriteBatch();
        shapeDrawer = new ShapeDrawer(batch, region);
        polygonCache = new InsetPolygonCache(7);
        
        tempColor = new Color();
        defaultOutline = new Color(0.5f, 0.5f, 0.5f, 1);
    }
    
    @Override
    public void update(float deltaTime) {
        batch.setProjectionMatrix(GameScreen.cam.combined);
        
        //todo: not all asteroids need transparency, only glass. could potentially sort enable blending for only glassteroids? profiler.
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        batch.begin();
        super.update(deltaTime);
        batch.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            switch (joinType) {
                case NONE:
                    joinType = JoinType.POINTY;
                    break;
                case POINTY:
                    joinType = JoinType.SMOOTH;
                    break;
                case SMOOTH:
                    joinType = JoinType.NONE;
                    break;
            }
            Gdx.app.debug(getClass().getSimpleName(), "JoinType: " + joinType);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            switch (mode) {
                case CENTERED:
                    mode = PathMode.INSET;
                    break;
                case INSET:
                    mode = PathMode.OUTSET;
                    break;
                case OUTSET:
                    mode = PathMode.CENTERED;
                    break;
            }
            Gdx.app.debug(getClass().getSimpleName(), "Outline Mode: " + mode);
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_ADD)) {
            lineWidth += 2f * deltaTime;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            lineWidth -= 2f * deltaTime;
        }
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        AsteroidComponent asteroid = Mappers.asteroid.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        //set polygon translation and rotation
        Polygon polygon = asteroid.polygon;
        polygon.setRotation(transform.rotation * MathUtils.radiansToDegrees);
        polygon.setPosition(transform.pos.x, transform.pos.y);
        
        //set color based on type and health
        HealthComponent health = Mappers.health.get(entity);
        float ratio = health.health / health.maxHealth;
        long timeSinceDmg = GameScreen.getGameTimeCurrent() - health.lastHitTime;
        long hitTime = 10;
        if (timeSinceDmg < hitTime) {
            float fade = (float) timeSinceDmg / hitTime;
            ratio -= fade * 0.125f;
        }
        if (asteroid.composition == ItemComponent.Resource.GLASS) {
            tempColor.set(asteroid.color);
        } else {
            tempColor.set(Color.BLACK).lerp(asteroid.revealed ? asteroid.color : defaultOutline, 1 - ratio);
        }
        hitTime = 500;
        long timeSinceHit = GameScreen.getGameTimeCurrent() - asteroid.lastShieldHit;
        if (timeSinceHit < hitTime) {
            float fade = (float) timeSinceHit / hitTime;
            tempColor.lerp(asteroid.color, 1 - fade);
        }
        /* determine orbit lock
        if (asteroid.parentOrbitBody == null) {
            color.set(1-ratio, 0 ,0, 1);//black to red
        } else {
            color.set(1, ratio, ratio, 1);//white to red
        }*/
        shapeDrawer.setColor(tempColor);
        shapeDrawer.filledPolygon(polygon);
        
        //mesh outline
        if (asteroid.revealed || debugRevealAll) {
            tempColor.set(asteroid.color);
        } else {
            tempColor.set(defaultOutline);
        }
        tempColor.a = 1;
        shapeDrawer.setColor(tempColor);
        //shapeDrawer.polygon(polygon, lineWidth, joinType);
        //shapeDrawer.setColor(Color.PINK);
        
        // detect winding; GeometryUtils.isCCW expects float[] x,y pairs
        boolean ccw = polygon.getVertices().length > 6;//GeometryUtils.isCCW(vertices, 0 , vertices.length);
        
        switch (mode) {
            case CENTERED:
                shapeDrawer.polygon(polygon, lineWidth, joinType);
                break;
            case INSET:
                float[] innerPoly = polygonCache.insetPolygon(polygon.getTransformedVertices(), lineWidth * 0.5f, ccw);
                shapeDrawer.polygon(innerPoly, 0, polygon.getVertices().length, lineWidth, joinType);
                break;
            case OUTSET:
                float[] outerPoly = polygonCache.insetPolygon(polygon.getTransformedVertices(), lineWidth * 0.5f, !ccw);
                shapeDrawer.polygon(outerPoly, lineWidth, joinType);
                break;
        }
        
        /*
        shapeDrawer.setColor(tempColor);
        //shapeDrawer.setColor(1, 1, 1, 1);
        shapeDrawer.polygon(insetPolygon(polygon.getTransformedVertices(), lineWidth, ccw), lineWidth*2f, joinType);
        //shapeDrawer.setColor(1, 1, 1, 1);
        shapeDrawer.polygon(insetPolygon(polygon.getTransformedVertices(), lineWidth, !ccw), lineWidth*2f, joinType);
        shapeDrawer.setColor(1, 1, 1, 1);
        //shapeDrawer.setColor(tempColor);
        shapeDrawer.polygon(polygon, lineWidth, joinType);
        */
        
        //shapeDrawer.path(polygon.getTransformedVertices(), lineWidth, JoinType.NONE, false);
    }
    
    private static final float EPS = 1e-8f;
    static final Vector2 pPrev = new Vector2();
    static final Vector2 pCur = new Vector2();
    static final Vector2 pNext = new Vector2();
    static final Vector2 ePrev = new Vector2();
    static final Vector2 eNext = new Vector2();
    static final Vector2 nPrev = new Vector2();
    static final Vector2 nNext = new Vector2();
    
    /**
     * Inset a polygon (vertices as [x0,y0, x1,y1, ...]) by distance 'inset'.
     * Positive 'inset' moves edges inward for CCW polygons. Uses GeometryUtils.isCCW(...)
     * to detect winding and flips sign for CW polygons automatically.
     *
     * Note: works best for convex polygons. For concave or complex polygons consider a robust
     * polygon offset library (Clipper).
     *
     * @param vertices ordered pairs x,y — must have length >= 6
     * @param inset distance to inset (use lineWidth / 2f)
     * @return new float[] with inset vertices (same length)
     */
    public static float[] insetPolygon(float[] vertices, float inset, boolean ccw) {
        if (vertices == null) throw new IllegalArgumentException("vertices null");
        if (vertices.length < 6) return null;
        
        if (!ccw) inset = -inset;
        
        int n = vertices.length / 2;
        Vector2[] out = new Vector2[n];
        for (int i = 0; i < n; i++) {
            int im1 = (i - 1 + n) % n;
            int ip1 = (i + 1) % n;
            
            pPrev.set(vertices[im1 * 2], vertices[im1 * 2 + 1]);
            pCur.set(vertices[i * 2], vertices[i * 2 + 1]);
            pNext.set(vertices[ip1 * 2], vertices[ip1 * 2 + 1]);
            
            // directions of edges (prev: pPrev -> pCur, next: pCur -> pNext)
            ePrev.set(pCur).sub(pPrev);
            eNext.set(pNext).sub(pCur);
            
            float lenPrev2 = ePrev.len2();
            float lenNext2 = eNext.len2();
            
            if (lenPrev2 < EPS || lenNext2 < EPS) {
                // degenerate edges: just move current point along zero-avoidance
                out[i] = new Vector2(pCur).add(0, 0);
                continue;
            }
            
            ePrev.nor();
            eNext.nor();
            
            // left normal of an edge (assuming CCW = inward)
            nPrev.set(-ePrev.y, ePrev.x);
            nNext.set(-eNext.y, eNext.x);
            
            // points on shifted edge lines
            Vector2 q1 = new Vector2(pPrev).add(nPrev.x * inset, nPrev.y * inset); // point on shifted previous edge
            Vector2 d1 = ePrev; // direction
            Vector2 q2 = new Vector2(pCur).add(nNext.x * inset, nNext.y * inset); // point on shifted next edge
            Vector2 d2 = eNext;
            
            Vector2 inter = intersectLines(q1, d1, q2, d2);
            //Intersector.intersectLines(q1, q2, d1, d2, inter);//broken, but interesting
            //if (Intersector.intersectLines(q1.x, q1.y, d1.x, d1.y, q2.x, q2.y, d2.x, d2.y, inter)) {
            //if (Intersector.intersectLines(q1.x, q1.y, q2.x, q2.y, d1.x, d1.y, d2.x, d2.y, inter)) {
            if (inter == null) {
                // fallback: move along average normal
                Vector2 avg = new Vector2(nPrev).add(nNext);
                if (avg.len2() < EPS) {
                    // nearly straight; just move along one normal
                    out[i] = new Vector2(pCur).add(nPrev.x * inset, nPrev.y * inset);
                } else {
                    avg.nor();
                    out[i] = new Vector2(pCur).add(avg.x * inset, avg.y * inset);
                }
            } else {
                out[i] = inter;
            }
        }
        
        float[] result = new float[vertices.length];
        for (int i = 0; i < n; i++) {
            result[i * 2] = out[i].x;
            result[i * 2 + 1] = out[i].y;
        }
        return result;
    }
    
    public static float[] insetPolygon2(float[] vertices, float inset, boolean ccw) {
        if (vertices == null) throw new IllegalArgumentException("vertices null");
        if (vertices.length < 6) return null;
        
        if (!ccw) inset = -inset;
        
        int n = vertices.length / 2;
        Vector2[] out = new Vector2[n];
        Vector2 pPrev = new Vector2();
        Vector2 pCur = new Vector2();
        Vector2 pNext = new Vector2();
        Vector2 ePrev = new Vector2();
        Vector2 eNext = new Vector2();
        Vector2 nPrev = new Vector2();
        Vector2 nNext = new Vector2();
        
        for (int i = 0; i < n; i++) {
            int im1 = (i - 1 + n) % n;
            int ip1 = (i + 1) % n;
            
            pPrev.set(vertices[im1 * 2], vertices[im1 * 2 + 1]);
            pCur.set(vertices[i * 2], vertices[i * 2 + 1]);
            pNext.set(vertices[ip1 * 2], vertices[ip1 * 2 + 1]);
            
            // directions of edges (prev: pPrev -> pCur, next: pCur -> pNext)
            ePrev.set(pCur).sub(pPrev);
            eNext.set(pNext).sub(pCur);
            
            float lenPrev2 = ePrev.len2();
            float lenNext2 = eNext.len2();
            
            if (lenPrev2 < EPS || lenNext2 < EPS) {
                // degenerate edges: just move current point along zero-avoidance
                out[i] = new Vector2(pCur).add(0, 0);
                continue;
            }
            
            ePrev.nor();
            eNext.nor();
            
            // left normal of an edge (assuming CCW = inward)
            nPrev.set(-ePrev.y, ePrev.x);
            nNext.set(-eNext.y, eNext.x);
            
            // points on shifted edge lines
            Vector2 q1 = new Vector2(pPrev).add(nPrev.x * inset, nPrev.y * inset); // point on shifted previous edge
            Vector2 d1 = ePrev; // direction
            Vector2 q2 = new Vector2(pCur).add(nNext.x * inset, nNext.y * inset); // point on shifted next edge
            Vector2 d2 = eNext;
            
            Vector2 inter = intersectLines(q1, d1, q2, d2);
            //Intersector.intersectLines()
            if (inter == null) {
                // fallback: move along average normal
                Vector2 avg = new Vector2(nPrev).add(nNext);
                if (avg.len2() < EPS) {
                    // nearly straight; just move along one normal
                    out[i] = new Vector2(pCur).add(nPrev.x * inset, nPrev.y * inset);
                } else {
                    avg.nor();
                    out[i] = new Vector2(pCur).add(avg.x * inset, avg.y * inset);
                }
            } else {
                out[i] = inter;
            }
        }
        
        float[] result = new float[vertices.length];
        for (int i = 0; i < n; i++) {
            result[i * 2] = out[i].x;
            result[i * 2 + 1] = out[i].y;
        }
        return result;
    }
    
    // intersection of two parametric lines: p1 + t*d1  and  p2 + s*d2
    // returns null if nearly parallel
    private static Vector2 intersectLines(Vector2 p1, Vector2 d1, Vector2 p2, Vector2 d2) {
        float a1 = d1.x, b1 = -d2.x, c1 = p2.x - p1.x;
        float a2 = d1.y, b2 = -d2.y, c2 = p2.y - p1.y;
        float det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < 1e-6f) return null;
        float t = (c1 * b2 - c2 * b1) / det;
        return new Vector2(p1.x + d1.x * t, p1.y + d1.y * t);
    }
    
    // intersection of two parametric lines: p1 + t*d1  and  p2 + s*d2
    // returns null if nearly parallel
    private static boolean intersectLines(Vector2 p1, Vector2 d1, Vector2 p2, Vector2 d2, Vector2 out) {
        float a1 = d1.x, b1 = -d2.x, c1 = p2.x - p1.x;
        float a2 = d1.y, b2 = -d2.y, c2 = p2.y - p1.y;
        float det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < 1e-6f) return false;
        float t = (c1 * b2 - c2 * b1) / det;
        out.set(p1.x + d1.x * t, p1.y + d1.y * t);
        return true;
    }
    
}
