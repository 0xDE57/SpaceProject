package com.spaceproject.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.math.DoubleDelaunayTriangulator;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import java.lang.StringBuilder;


public class AsteroidBeltSystem extends EntitySystem {

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
        Entity asteroid = EntityBuilder.createAsteroid(seed, x, y, velX, velY, size);
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

    private void shatterAsteroid(Vector2 parentPos, Vector2 parentVel, float parentAngle, float parentAngularVel, AsteroidComponent asteroid) {
        //create new polygons from vertices + center point to "sub shatter" into smaller polygon shards
        float[] vertices = asteroid.polygon.getVertices();
        int length = vertices.length;
        float[] newPoly = new float[length + 2];
        System.arraycopy(vertices, 0, newPoly, 0, length);

        //center new point
        GeometryUtils.polygonCentroid(vertices, 0, length, center);
        newPoly[length] = center.x;
        newPoly[length + 1] = center.y;

        spawnChildAsteroid(parentPos, parentVel, parentAngle, parentAngularVel, asteroid, newPoly);
    }

    private void spawnChildAsteroid(Vector2 parentPos, Vector2 parentVel, float parentAngle, float parentAngularVel, AsteroidComponent asteroidComponent, float[] vertices) {
        /* todo: re shatter issues; if we turn on b2d debug we can see the velocity is not the origin of child bodies
        NOTE: Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
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

        asteroid.centerOfMass == B2D::body->GetLocalCenter()

        Vector2 center = new Vector2();
        Polygon poly = new Polygon();
        poly.getCentroid(center); -> transformed vertices -> GeometryUtils.polygonCentroid()

        GeometryUtils.polygonCentroid()

        API Addition: Polygon methods setVertex, getVertex, getVertexCount, getCentroid.
        API Addition: GeometryUtils,polygons isCCW, ensureClockwise, reverseVertices
        https://libgdx.com/news/2022/05/gdx-1-11

        computeTriangles():
            - Duplicate points will result in undefined behavior. sorted – If false, the points will be sorted by the x coordinate,
              which is required by the triangulation algorithm. If sorting is done the input array is not modified,
              the returned indices are for the input array, and count*2 additional working memory is needed.
            - Returns: triples of indices into the points that describe the triangles in clockwise order.
        */
        int child = 0;
        float childArea = 0;

        //copy float to double for higher precision triangulation
        double[] vertsFloat = new double[vertices.length];
        for (int i = 0; i < vertsFloat.length; i++) {
            vertsFloat[i] = vertices[i];
        }
        IntArray triangleIndices = delaunay.computeTriangles(vertsFloat, false);

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

            //discard duplicate points
            if ((hull[0] == hull[2] && hull[1] == hull[3]) || // p1 == p2 or
                    (hull[0] == hull[4] && hull[1] == hull[5]) || // p1 == p3 or
                    (hull[2] == hull[4] && hull[3] == hull[5])) { // p2 == p3
                Gdx.app.error(getClass().getSimpleName(), "Duplicate point! Discarding triangle");
                //duplicate points result in crash:
                //../b2PolygonShape.cpp:158: void b2PolygonShape::Set(const b2Vec2*, int32): Assertion `false' failed.
                continue;
            }
            child++;


            //todo: discard shards / slivers
            // if (ratio between length and width > threshold) reject

            boolean reCenter = false;
            if (reCenter) {
                //shift vertices to be centered
                GeometryUtils.triangleCentroid(
                        hull[0], hull[1],
                        hull[2], hull[3],
                        hull[4], hull[5],
                        center);
                for (int j = 0; j < hull.length; j += 2) {
                    //todo: this is a fix for #30...
                    // but causes #2 to get worse and places bodies incorrect relative to parent
                    hull[j] -= center.x;
                    hull[j + 1] -= center.y;
                }
                //center.rotateRad(parentAngle);
                //parentPos.rotateAroundRad(center, parentAngle);
                //todo: rotate center relative to parent by angle?
                //Vector2 pos = parentBody.getPosition().cpy();//.mulAdd(center.rotateAroundRad(parentBody.getPosition(), parentBody.getAngle()), 0.1f);
                //center.rotateAroundRad(parentBody.getPosition(), parentBody.getAngle());
                //Vector2 pos = parentBody.getPosition().cpy().add(center);
                //Vector2 vel = parentBody.getLinearVelocity().cpy();
            }

            float angularDrift = Math.max(MathUtils.random(-maxDriftAngle, maxDriftAngle), minDriftAngle);
            Entity childAsteroid = EntityBuilder.createAsteroid(parentPos.x, parentPos.y, parentVel.x, parentVel.y, parentAngle, hull, asteroidComponent.composition, true);
            childAsteroid.getComponent(PhysicsComponent.class).body.setAngularVelocity(parentAngularVel + angularDrift);
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
