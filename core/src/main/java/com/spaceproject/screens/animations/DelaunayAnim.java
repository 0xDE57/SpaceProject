package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;


public class DelaunayAnim extends TitleAnimation {
    
    float velocity;
    int numPoints;
    int pad;
    FloatArray points;
    FloatArray dirs;
    ShortArray triangles;
    DelaunayTriangulator delaunay = new DelaunayTriangulator();
    final Vector2 cacheVector = new Vector2();

    public DelaunayAnim() {
        velocity = 10;
        numPoints = 20;
        pad = 10;
        points = new FloatArray();
        dirs = new FloatArray();
        for (int i = 0; i < numPoints * 2; i += 2) {
            float x = MathUtils.random(pad, Gdx.graphics.getWidth() - pad);
            float y = MathUtils.random(pad, Gdx.graphics.getHeight() - pad);
            points.add(x);
            points.add(y);
            float dir = MathUtils.random(0, MathUtils.PI2);
            float dx = (float) (Math.cos(dir) * velocity);
            float dy = (float) (Math.sin(dir) * velocity);
            dirs.add(dx);
            dirs.add(dy);
        }
    }

    @Override
    public void render(float delta, ShapeRenderer shape) {
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            boolean duplicate = false;
            for (int i = 0; i < points.size && !duplicate; i += 2) {
                if (MathUtils.isEqual(x, points.get(i), 0.1f) && MathUtils.isEqual(y, points.get(i+1), 0.1f)) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                points.add(x);
                points.add(y);
                float dir = MathUtils.random(0, MathUtils.PI2);
                float dx = (float) (Math.cos(dir) * velocity);
                float dy = (float) (Math.sin(dir) * velocity);
                dirs.add(dx);
                dirs.add(dy);
            }
        }
        
        for (int i = 0; i < points.size; i += 2) {
            //bounds check
            if (points.get(i) <= pad || points.get(i) >= Gdx.graphics.getWidth() - pad)
                dirs.set(i, -dirs.get(i));
            
            if (points.get(i + 1) <= pad || points.get(i + 1) >= Gdx.graphics.getHeight() - pad)
                dirs.set(i + 1, -dirs.get(i + 1));
            
            points.set(i, points.get(i) + dirs.get(i) * delta);
            points.set(i + 1, points.get(i + 1) + dirs.get(i + 1) * delta);
        }
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        //todo: remove duplicate points. spam creating points on a single point will cause crash.
        // Duplicate points will result in undefined behavior.
        triangles = delaunay.computeTriangles(points, false);
        for (int i = 0; i < triangles.size; i += 3) {
            //get points
            int p1 = triangles.get(i) * 2;
            int p2 = triangles.get(i + 1) * 2;
            int p3 = triangles.get(i + 2) * 2;
            shape.triangle(
                    points.get(p1), points.get(p1 + 1),
                    points.get(p2), points.get(p2 + 1),
                    points.get(p3), points.get(p3 + 1));
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < triangles.size; i += 3) {
            //get points
            int p1 = triangles.get(i) * 2;
            int p2 = triangles.get(i + 1) * 2;
            int p3 = triangles.get(i + 2) * 2;
            float x1 = points.get(p1);
            float x2 = points.get(p2);
            float x3 = points.get(p3);
            float y1 = points.get(p1 + 1);
            float y2 = points.get(p2 + 1);
            float y3 = points.get(p3 + 1);
            GeometryUtils.triangleCentroid(
                    x1, y1,
                    x2, y2,
                    x3, y3,
                    cacheVector);
            float quality = GeometryUtils.triangleQuality(
                    cacheVector.x - x1, cacheVector.y - y1,
                    cacheVector.x - x2, cacheVector.y - y2,
                    cacheVector.x - x3, cacheVector.y - y3);
            shape.setColor(0.1f, 0.1f, 0.1f, 1 - quality);
            shape.triangle(
                    x1, y1,
                    x2, y2,
                    x3, y3);
        }
        shape.end();
    }
    
    @Override
    public void resize(int width, int height) {
    }
}
