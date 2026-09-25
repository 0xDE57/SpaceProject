package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.AsteroidComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.math.DoubleDelaunayTriangulator;
import com.spaceproject.math.MyMath;
import com.spaceproject.math.PolygonUtil;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class AsteroidBeltSystem extends EntitySystem {
    
    private enum ShatterMode {
        CENTROID,
        RANDOM,
        INNER_VERTICES,
        INNER_EDGES;
        //CONTACT_POINT //todo: add point(s) near hit contact area
        
        static final ShatterMode[] VALUES = ShatterMode.values();
        public static ShatterMode random() {
            return VALUES[MathUtils.random(VALUES.length - 1)];
        }
    }
    
    static class AsteroidRemovedQueue implements Pool.Poolable {
        public AsteroidComponent asteroidComponent;
        public Vector2 position;
        public Vector2 velocity;
        public float angle;
        public float angularVelocity;

        public void init(AsteroidComponent asteroidComponent, Vector2 position, Vector2 velocity, float angle, float angularVelocity) {
            this.asteroidComponent = asteroidComponent;
            this.position = position;
            this.velocity = velocity;
            this.angle = angle;
            this.angularVelocity = angularVelocity;
        }

        @Override
        public void reset() {
            // https://libgdx.com/wiki/articles/memory-management#object-pooling
            // Beware of leaking references to Pooled objects. Just because you invoke “free” on the Pool does not invalidate any outstanding references.
            // This can lead to subtle bugs if you’re not careful.
            // You can also create subtle bugs if the state of your objects is not fully reset when the object is put in the pool.
            asteroidComponent = null;
            position = null;
            velocity = null;
        }
    }

    private ImmutableArray<Entity> asteroids;
    private ImmutableArray<Entity> spawnBelt;
    
    private final SimpleTimer lastSpawnedTimer = new SimpleTimer(1000);
    private final Pool<AsteroidRemovedQueue> removePool = Pools.get(AsteroidRemovedQueue.class, 100);
    private final Array<AsteroidRemovedQueue> spawnQ = new Array<>(false, 100);

    private final DoubleDelaunayTriangulator delaunay = new DoubleDelaunayTriangulator();
    private final float minAsteroidSize = 100; //anything smaller than this will not create more
    private final float maxDriftAngle = 0.05f; //angular drift when shatter
    private final float minDriftAngle = 0.01f;
    private final Vector2 center = new Vector2();
    private final Vector2 childCenter = new Vector2();

    private float minArea = Float.MAX_VALUE, maxArea = Float.MIN_VALUE;
    private float totalArea = 0;

    private final StringBuilder infoString = new StringBuilder();
    private int activePeak = 0;
    
    @Override
    public void addedToEngine(Engine engine) {
        asteroids = engine.getEntitiesFor(Family.all(AsteroidComponent.class, TransformComponent.class).get());
        spawnBelt = engine.getEntitiesFor(Family.all(AsteroidBeltComponent.class).get());
        lastSpawnedTimer.setCanDoEvent();
    }
    
    @Override
    public void update(float deltaTime) {
        //create initial belt
        spawnAsteroidBelt();

        //artificial gravity to rotate bodies around belt
        updateBeltOrbit();

        //spawn child asteroids or resource drops when asteroids destroyed
        processAsteroidDestructionQueue();
    
        //debug spawn asteroid at mouse position
        if (GameScreen.isDebugMode && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            DebugConfig debug = SpaceProject.configManager.getConfig(DebugConfig.class);
            if (debug.spawnAsteroid) {
                Vector3 unproject = GameScreen.cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                if (debug.spawnCluster) {
                    spawnAsteroidField(unproject.x, unproject.y, 0, 0, 20, 400);
                } else {
                    spawnAsteroid(unproject.x, unproject.y, 0, 0);
                }
            }
        }
    }

    private void updateBeltOrbit() {
        //keep asteroids orbit around parent body, don't fling everything out into universe...
        for (Entity entity : asteroids) {
            AsteroidComponent asteroid = Mappers.asteroid.get(entity);
            PhysicsComponent physics = Mappers.physics.get(entity);
            if (asteroid.parentOrbitBody != null) {
                TransformComponent parentTransform = Mappers.transform.get(asteroid.parentOrbitBody);
                AsteroidBeltComponent asteroidBelt = Mappers.asteroidBelt.get(asteroid.parentOrbitBody);
                //set velocity perpendicular to parent body, (simplified 2-body model)
                float angle = MyMath.angleTo(physics.body.getPosition(), parentTransform.pos) + (asteroidBelt.clockwise ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
                Vector2 targetVelocity = MyMath.vector(angle, asteroidBelt.velocity);

                physics.body.setLinearVelocity(targetVelocity);
                //todo: set should be avoided.
                //  instead we should add the desired velocity. or rather the difference between the current velocity and the desired velocity. a steering behavior?
            }
            /*else {
                //todo: gravity pull into belt if close enough: re-entry?
                for (Entity parentEntity : spawnBelt) {
                    AsteroidBeltComponent asteroidBelt = Mappers.asteroidBelt.get(parentEntity);
                    TransformComponent parentTransform = Mappers.transform.get(parentEntity);

                    //todo: if close enough: slowly pull into stream of asteroid, match velocity and angle
                    float dist = parentTransform.pos.dst(physics.body.getPosition());
                    if (dist > asteroidBelt.radius - (asteroidBelt.bandWidth/2) && dist < asteroidBelt.radius + (asteroidBelt.bandWidth/2)) {
                        float targetAngle = MyMath.angleTo(parentTransform.pos, physics.body.getPosition()) + (asteroidBelt.clockwise ? -MathUtils.HALF_PI : MathUtils.HALF_PI);
                        double angleDeltaThreshold = Math.PI / 6;
                        boolean meetsAngleThreshold = Math.abs(physics.body.getLinearVelocity().angleRad() - targetAngle) < angleDeltaThreshold;

                        float velDeltaThreshold = 5f;
                        boolean meetsVelThreshold = Math.abs(physics.body.getLinearVelocity().len() - asteroidBelt.velocity) < velDeltaThreshold;

                        //todo: if should merge, begin merge
                        if (meetsAngleThreshold /*&& meetsVelThreshold*) {
                            //asteroid.parentOrbitBody = parentEntity;
                            //Gdx.app.debug(this.getClass().getSimpleName(), "ASTEROID re-entry into orbit");
                            break; // no point looking at other disks once met
                        }
                    }
                }
            }*/
        }
    }

    private void spawnAsteroidBelt() {
        for (Entity parentEntity : spawnBelt) {
            AsteroidBeltComponent disk = Mappers.asteroidBelt.get(parentEntity);
            if (disk.spawned <= disk.maxSpawn) {
                //todo, should bias towards middle and taper off edges
                // alternatively could be a 1D noise from inner to outer with different concentrations?
                float bandwidthOffset = MathUtils.random(-disk.bandWidth /2, disk.bandWidth /2);
                float angle = MathUtils.random(MathUtils.PI2);
                Vector2 pos = Mappers.transform.get(parentEntity).pos.cpy();
                pos.add(MyMath.vector(angle, disk.radius + bandwidthOffset));

                Entity newAsteroid = spawnAsteroid(pos.x, pos.y, 0, 0);
                AsteroidComponent ast = Mappers.asteroid.get(newAsteroid);
                ast.parentOrbitBody = parentEntity;

                disk.spawned++;
                totalArea += ast.area;
            }
        }
    }

    private void spawnAsteroidField(float x, float y, float angle, float velocity, int clusterSize, float range) {
        Vector2 vel = MyMath.vector(angle, velocity);
        for (int i = 0; i < clusterSize; i++) {
            float newX = MathUtils.random(x - range, x + range);
            float newY = MathUtils.random(y - range, y + range);
            spawnAsteroid(newX, newY, vel.x, vel.y);
        }
        Gdx.app.log(getClass().getSimpleName(), "spawn field: " + clusterSize);
    }

    public void destroyAsteroid(AsteroidComponent asteroid, Vector2 pos, Vector2 vel, float angle, float angularVel) {
        //NOTE: cannot CreateBody() during physics step
        //  jni/Box2D/Dynamics/b2World.cpp:109: b2Body* b2World::CreateBody(const b2BodyDef*): Assertion `IsLocked() == false' failed.
        //just like we cannot destroy a body during a physics step (hence removing entities at end of frame)
        //so instead we add to a spawn queue to be processed next system tick
        AsteroidRemovedQueue remove = removePool.obtain();
        remove.init(asteroid, pos, vel, angle, angularVel);
        spawnQ.add(remove);
    }

    private void processAsteroidDestructionQueue() {
        for (AsteroidRemovedQueue asteroid : spawnQ) {
            asteroidDestroyed(asteroid.asteroidComponent, asteroid.position, asteroid.velocity, asteroid.angle, asteroid.angularVelocity);
        }
        //DebugSystem.addDebugText(toString(), 200, 200);
        removePool.freeAll(spawnQ);
        spawnQ.clear();
    }

    private void asteroidDestroyed(AsteroidComponent asteroid, Vector2 parentPos, Vector2 parentVel, float parentAngle, float parentAngularVel) {
        if (asteroid.area >= minAsteroidSize) {
            shatterAsteroid(parentPos, parentVel, parentAngle, parentAngularVel, asteroid);
        }
        //todo: pool drops
        GeometryUtils.polygonCentroid(asteroid.polygon.getVertices(), 0, asteroid.polygon.getVertices().length, center);
        center.rotateRad(parentAngle);
        Entity drop = EntityBuilder.dropResource(parentPos.add(center), parentVel, asteroid.composition, asteroid.color);
        getEngine().addEntity(drop);
    }

    private Entity spawnAsteroid(float x, float y, float velX, float velY) {
        int size = MathUtils.random(14, 120);//NOTE: does not guarantee final area
        long seed = MyMath.getSeed(x, y);
        //todo: pool asteroids. note box2d body is already pooled internally, but we can pool the entity itself to eliminate new
        Entity asteroid = EntityBuilder.createAsteroid(seed, x, y, velX, velY, 0, size);
        Polygon polygon = asteroid.getComponent(AsteroidComponent.class).polygon;
        float area = Math.abs(GeometryUtils.polygonArea(polygon.getVertices(), 0, polygon.getVertices().length));
        if (area > maxArea) {
            maxArea = area;
            Gdx.app.debug(getClass().getSimpleName(), "new max area: " + area + " > " + DebugUtil.objString(asteroid));
        } else if (area < minArea) {
            minArea = area;
            Gdx.app.debug(getClass().getSimpleName(), "new min area: " + area + " > " + DebugUtil.objString(asteroid));
        }
        getEngine().addEntity(asteroid);
        return asteroid;
    }
    
    private float[] addCentroidPoint(float[] vertices) {
        int length = vertices.length;
        float[] newPoly = new float[length + 2];
        
        System.arraycopy(vertices, 0, newPoly, 0, length);
        
        GeometryUtils.polygonCentroid(vertices, 0, length, center);
        
        newPoly[length] = center.x;
        newPoly[length + 1] = center.y;
        
        return newPoly;
    }
    
    private static final float MIN_POINT_DISTANCE = 2f;
    private float[] addRandomPoints(float[] vertices, int newShatterPoints) {
        int length = vertices.length;
        float[] newPoly = new float[length + (newShatterPoints * 2)];
        System.arraycopy(vertices, 0, newPoly, 0, length);
        
        GeometryUtils.polygonCentroid(vertices, 0, length, center);
        Rectangle bounds = PolygonUtil.localBounds(vertices);
        
        int pointsAdded = 0;
        int maxAttempts = newShatterPoints * 100;
        
        for (int attempt = 0;
             attempt < maxAttempts && pointsAdded < newShatterPoints;
             attempt++) {
            
            // width/height are sizes, so add them to x/y.
            float x = MathUtils.random(bounds.x, bounds.x + bounds.width);
            float y = MathUtils.random(bounds.y, bounds.y + bounds.height);
            
            // Test against the raw local-space vertices.
            if (!Intersector.isPointInPolygon(vertices, 0, vertices.length, x, y)) {
                continue;
            }
            
            // Avoid nearly duplicate points, which can produce invalid or zero-area Delaunay triangles.
            if (PolygonUtil.containsNear(newPoly, length + pointsAdded * 2, x, y, MIN_POINT_DISTANCE)) {
                continue;
            }
            
            int index = length + pointsAdded * 2;
            newPoly[index] = x;
            newPoly[index + 1] = y;
            pointsAdded++;
        }
        
        if (pointsAdded == 0) {
            // Rejection sampling can theoretically fail for very thin polygons.
            // Fall back to the centroid rather than adding an invalid point.
            return addCentroidPoint(vertices);
        }
        
        if (pointsAdded < newShatterPoints) {
            // Do not leave unused zero-valued points in the polygon.
            float[] resized = new float[length + pointsAdded * 2];
            System.arraycopy(newPoly, 0, resized, 0, resized.length);
            newPoly = resized;
        }
        
        return newPoly;
    }
    
    private final Vector2 tempVec = new Vector2();
    private float[] addInnerVertexPoints(float[] vertices, float scale) {
        int originalLength = vertices.length;
        float[] newPoly = new float[originalLength * 2];
        System.arraycopy(vertices, 0, newPoly, 0, originalLength);
        
        GeometryUtils.polygonCentroid(vertices, 0, originalLength, center);
        int outputIndex = vertices.length;
        
        for (int index = 0; index < vertices.length; index += 2) {
            tempVec.set(vertices[index], vertices[index + 1]);
            
            tempVec.sub(center).scl(scale).add(center);
            
            newPoly[outputIndex++] = tempVec.x;
            newPoly[outputIndex++] = tempVec.y;
        }
        
        return newPoly;
    }
    
    private float[] addInnerEdgePoints(float[] vertices, float scale) {
        int originalLength = vertices.length;
        
        float[] newPoly = new float[originalLength * 2];
        System.arraycopy(vertices, 0, newPoly, 0, vertices.length);
        
        GeometryUtils.polygonCentroid(vertices, 0, originalLength, center);
        
        int outputIndex = vertices.length;
        
        for (int i = 0; i < vertices.length; i += 2) {
            int next = (i + 2) % vertices.length;
            
            float edgeMidX = (vertices[i] + vertices[next]) * 0.5f;
            float edgeMidY = (vertices[i + 1] + vertices[next + 1]) * 0.5f;
            
            float innerX = center.x + (edgeMidX - center.x) * scale;
            float innerY = center.y + (edgeMidY - center.y) * scale;
            
            newPoly[outputIndex++] = innerX;
            newPoly[outputIndex++] = innerY;
        }
        
        return newPoly;
    }
    
    private void shatterAsteroid(Vector2 parentPos, Vector2 parentVel, float parentAngle, float parentAngularVel, AsteroidComponent asteroid) {
        //create new polygons from vertices + center point to "sub shatter" into smaller polygon shards
        float[] vertices = asteroid.polygon.getVertices();
        
        //todo: could add some variation in parameters
        // could also make shatter mode a property of asteroid composition so different colors break different
        ShatterMode mode = ShatterMode.random();
        float[] newPoly = null;
        switch (mode) {
            case CENTROID:
                newPoly = addCentroidPoint(vertices);
                break;
            case RANDOM:
                newPoly = addRandomPoints(vertices, 3);
                break;
            case INNER_VERTICES:
                newPoly = addInnerVertexPoints(vertices, 0.5f);
                break;
            case INNER_EDGES:
                newPoly = addInnerEdgePoints(vertices, 0.5f);
                break;
            default:
                newPoly = addCentroidPoint(vertices);
                break;
        }
        
        spawnChildAsteroid(parentPos, parentVel, parentAngle, parentAngularVel, asteroid, newPoly);
    }

    private void spawnChildAsteroid(Vector2 parentPos, Vector2 parentVel, float parentAngle, float parentAngularVel, AsteroidComponent asteroidComponent, float[] vertices) {
        /*
        NOTE: Box2D expects Polygons vertices are stored with a counterclockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed
        coordinate system with the z-axis pointing out of the plane.

        NOTE: The body definition gives you the chance to initialize the position of the body on creation.
        This has far better performance than creating the body at the world origin and then moving the body.
        Caution: Do not create a body at the origin and then move it. If you create several bodies at the origin, then performance will suffer.
        A body has two main points of interest. The first point is the body's origin. Fixtures and joints are attached relative to the body's origin.
        The second point of interest is the center of mass.
        The center of mass is determined from mass distribution of the attached shapes or is explicitly set with b2MassData.
        Much of Box2D's internal computations use the center of mass position. For example b2Body stores the linear velocity for the center of mass.
        When you are building the body definition, you may not know where the center of mass is located.
        Therefore you specify the position of the body's origin.
        You may also specify the body's angle in radians, which is not affected by the position of the center of mass.
        If you later change the mass properties of the body, then the center of mass may move on the body,
        but the origin position does not change and the attached shapes and joints do not move.
        */

        //copy float to double for higher precision triangulation
        double[] vertsDouble = new double[vertices.length];
        for (int i = 0; i < vertsDouble.length; i++) {
            vertsDouble[i] = vertices[i];
        }
        IntArray triangleIndices = delaunay.computeTriangles(vertsDouble, false);

        //create cells for each triangle
        for (int index = 0; index < triangleIndices.size; index += 3) {
            int p1 = triangleIndices.get(index) * 2;
            int p2 = triangleIndices.get(index + 1) * 2;
            int p3 = triangleIndices.get(index + 2) * 2;
            float[] hull = new float[]{
                    vertices[p1], vertices[p1 + 1], // xy: 0, 1
                    vertices[p2], vertices[p2 + 1], // xy: 2, 3
                    vertices[p3], vertices[p3 + 1]  // xy: 4, 5
            };

            //if (PolygonUtil.validTriangle(hull)) continue;
            //discard duplicate points
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                    (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                    (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                Gdx.app.error(getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                //duplicate points result in crash:
                //../b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
                continue;
            }
            
            //todo: discard shards / slivers?
            //float quality = GeometryUtils.triangleQuality(hull[0], hull[1], hull[2], hull[3], hull[4], hull[5]);
            float childArea = GeometryUtils.triangleArea(hull[0], hull[1], hull[2], hull[3], hull[4], hull[5]);
            if (childArea < minAsteroidSize * 0.5f /* || (quality < qualityThreshold)*/) {
                //if too small just drop a resource
                GeometryUtils.triangleCentroid(
                    hull[0], hull[1],
                    hull[2], hull[3],
                    hull[4], hull[5],
                    center);
                childCenter.set(center).rotateRad(parentAngle).add(parentPos);
                Entity drop = EntityBuilder.dropResource(childCenter, parentVel, asteroidComponent.composition, asteroidComponent.color);
                getEngine().addEntity(drop);
                continue;
            }
            
            //shift vertices to be centered
            GeometryUtils.triangleCentroid(
                    hull[0], hull[1],
                    hull[2], hull[3],
                    hull[4], hull[5],
                    center);
            
            for (int j = 0; j < hull.length; j += 2) {
                hull[j] -= center.x;
                hull[j + 1] -= center.y;
            }
            
            // adjust local offset relative to parent asteroid
            childCenter.set(center).rotateRad(parentAngle).add(parentPos);
            
            //add some angular drift relative to parent angular momentum
            float angularDrift = MathUtils.random(minDriftAngle, maxDriftAngle);
            if (MathUtils.randomBoolean()) {
                angularDrift = -angularDrift;
            }
            angularDrift += parentAngularVel;
            
            Entity childAsteroid = EntityBuilder.createAsteroid(
                childCenter.x, childCenter.y,
                parentVel.x, parentVel.y,
                parentAngle, angularDrift,
                hull, asteroidComponent.composition, true);
            
            getEngine().addEntity(childAsteroid);
            
            //fracture damage model: should extra damage be passed down to children?
            //eg: parent hp = 100, takes 150 damage = 50 damage
            // for each child asteroid, apply (remaining damage / number of children)
            // eg: 50 damage / 3 children = 16.6
            //float distributedDamage = damage / triangleIndices/3; // * assuming no triangles are discarded!!!
            //childAsteroid.getComponent(HealthComponent.class).health -= damage;
            //we could aslo pre sub shatter further depending on how much damage is done? since we already know the what the child
            //bodies will be under the current simplified delaunay-based shatter.
        }
    }

    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("[AsteroidRemovePool] active: ").append(spawnQ.size)
                .append(", active peak: ").append(activePeak = Math.max(activePeak, spawnQ.size))
                .append(", free: ").append(removePool.getFree())
                .append(", peak: ").append(removePool.peak)
                .append(", max: ").append(removePool.max);
        return infoString.toString();
    }

}
