package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.ui.CustomShapeRenderer;

import java.util.Iterator;

public class AsteroidAnim extends TitleAnimation {
    
    Array<Asteroid> asteroids = new Array<Asteroid>();
    
    CustomShapeRenderer customShapeRenderer;
    Matrix4 projectionMatrix = new Matrix4();
    
    
    int velocityIterations = 6;
    int positionIterations = 2;
    float timeStep = 1 / 60.0f;
    float accumulator = 0f;
    World world;
    Box2DDebugRenderer box2DDebugRenderer;
    Array<Body> bodies = new Array<>();
    
    float[] vertices = new float[100];
    Vector2 tempVertex = new Vector2();
    
    Body bullet;
    //1. move to b2d
    //2. move ship to polygon -> b2d
    //2. move bullet to circle body
    //3; fix rendering
    //4; fortune test
    //5; local test
    
    public AsteroidAnim() {
        Asteroid asteroid = new Asteroid(new Vector2(Gdx.graphics.getWidth() * MathUtils.random(), Gdx.graphics.getHeight() * MathUtils.random()), 200, 0, 0);
        asteroids.add(asteroid);
        customShapeRenderer = new CustomShapeRenderer();
        
        world = new World(new Vector2(), true);
        box2DDebugRenderer = new Box2DDebugRenderer(true, true, true, true, true, true);
        
        Body body = BodyBuilder.createPoly(200, 200, asteroid.hullPoly.getVertices(), 0, 0.5f, BodyDef.BodyType.DynamicBody, world, null);
        body.applyForceToCenter(10,1,true);
        body.applyAngularImpulse(200, true);
        
        bullet = BodyBuilder.createCircle(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f, 20, world, null);
    }
    
    
    @Override
    public void render(float deltaTime, ShapeRenderer shape) {
        accumulator += deltaTime;
        while (accumulator >= timeStep) {
            world.step(timeStep, velocityIterations, positionIterations);
            accumulator -= timeStep;
        }

        
        Vector2 centerScreen = new Vector2(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        Vector2 mousePos = new Vector2(Gdx.input.getX(),Gdx.graphics.getHeight()-Gdx.input.getY());
        
        float mouseAngle = MyMath.angleTo(centerScreen.x, centerScreen.y, mousePos.x, mousePos.y);
    
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            bullet.setTransform(centerScreen, mouseAngle);
            bullet.setLinearVelocity(MyMath.vector(mouseAngle, 100000));
        }
        
        
        customShapeRenderer.setProjectionMatrix(projectionMatrix);
        customShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        world.getBodies(bodies);
        for (Body body : bodies) {
            Fixture fixture = body.getFixtureList().first();
            if (!(fixture.getShape() instanceof PolygonShape)) continue;
            PolygonShape polyShape = (PolygonShape)fixture.getShape();
            int vertexCount = polyShape.getVertexCount();
            
            Transform transform = body.getTransform();
            for (int i = 0; i < vertexCount; i++) {
                polyShape.getVertex(i, tempVertex);
                transform.mul(tempVertex);
                vertices[i]   = tempVertex.x;
                vertices[i+1] = tempVertex.y;
            }
            //customShapeRenderer.fillPolygon(vertices, 0, vertexCount*2, Color.WHITE);
        }
        
        for (Asteroid a : asteroids) {
            //a.renderBody(customShapeRenderer);
        }
        customShapeRenderer.end();
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Iterator<Asteroid> asteroidIterator = new Array.ArrayIterator<>(asteroids); asteroidIterator.hasNext(); ) {
            Asteroid a = asteroidIterator.next();
            //a.render(shape, deltaTime);
        
        }
    
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            asteroids.add(new Asteroid(mousePos.cpy(), 200, 0, 0));
        }
        
        shape.end();
        
        shape.begin(ShapeRenderer.ShapeType.Filled);

        
        // draw ship
        shape.setColor(Color.WHITE);
        setShape(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2, mouseAngle);
        for(int i = 0, j = shapeX.length - 1; i < shapeY.length; j = i++) {
            shape.line(shapeX[i], shapeY[i], shapeX[j], shapeY[j]);
        }
        shape.end();
    
    
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());//<---scale up for max size 10 units
        box2DDebugRenderer.render(world, projectionMatrix);
        
    }
    
    private float[] shapeX = new float[4];
    private float[] shapeY = new float[4];
    private void setShape(float x, float y, float radians) {
        float scale = 40;
        shapeX[0] = x + MathUtils.cos(radians) * scale;
        shapeY[0] = y + MathUtils.sin(radians) * scale;
        
        shapeX[1] = x + MathUtils.cos(radians - 4 * 3.1415f / 5) * scale;
        shapeY[1] = y + MathUtils.sin(radians - 4 * 3.1415f / 5) * scale;
        
        shapeX[2] = x + MathUtils.cos(radians + 3.1415f) * scale * (5.0f/8);
        shapeY[2] = y + MathUtils.sin(radians + 3.1415f) * scale * (5.0f/8);
        
        shapeX[3] = x + MathUtils.cos(radians + 4 * 3.1415f / 5) * scale;
        shapeY[3] = y + MathUtils.sin(radians + 4 * 3.1415f / 5) * scale;
    }
    
    
    @Override
    public void resize(int width, int height) {
    
    }
    
    private class Asteroid {
        
        Vector2 position;
        float angle, velocity;
        Polygon hullPoly;
        int size;
        
        public Asteroid(Vector2 position, int size, float angle, float velocity) {
            this.size = size;
            this.angle = angle;
            this.velocity = velocity;
            this.position = position.sub(size/2, size/2);
            
            FloatArray points = new FloatArray();
            int numPoints = 7;//Box2D poly vert limit: Assertion `3 <= count && count <= 8' failed.
            for (int i = 0; i < numPoints * 2; i += 2) {
                float x = MathUtils.random(size);
                float y = MathUtils.random(size);
                points.add(x);
                points.add(y);
            }
            
            ConvexHull convex = new ConvexHull();
            float[] hull = convex.computePolygon(points, false).toArray();
            
            hullPoly = new Polygon(hull);
            
            hullPoly.setOrigin(size / 2, size / 2);//should actually be center of mass//TODO: lookup center of mass for arbitrary poly
        }
        
        public void render(ShapeRenderer shape, float delta) {
            //float angle = MyMath.angleTo(new Vector2(Gdx.input.getX(), Gdx.graphics.getHeight()-Gdx.input.getY()), position);
            //position.add(MyMath.vector(angle,10*delta));
            
            Rectangle bounds = hullPoly.getBoundingRectangle();
            if (bounds.y <= 0) {
                position.add(0, 1);
                angle = MathUtils.PI2 - angle;
            } else if (bounds.y + bounds.height >= Gdx.graphics.getHeight()) {
                position.sub(0, 1);
                angle = MathUtils.PI2 - angle;
            }
        //shape.triangle();
            if (bounds.x <= 0) {
                position.add(1, 0);
                angle = MathUtils.PI - angle;
            } else if (bounds.x + bounds.width >= Gdx.graphics.getWidth()) {
                position.sub(1, 0);
                angle = MathUtils.PI - angle;
            }
            
            position.add(MyMath.vector(angle, velocity * delta));
            
            
            hullPoly.rotate(10 * delta);
            hullPoly.setPosition(position.x, position.y);
            shape.setColor(Color.BLACK);
            //shape.polyline(hullPoly.getTransformedVertices());
            
            shape.setColor(Color.RED);
            Rectangle rectangle = bounds;
            //shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }
        
        public void renderBody(CustomShapeRenderer shape) {
            shape.fillPolygon(hullPoly.getTransformedVertices(), 0, hullPoly.getVertices().length, Color.WHITE);
        }
    }
    
}
