package com.spaceproject.generation;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.ConvexHull;
import com.badlogic.gdx.math.GeometryUtils;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.*;
import com.spaceproject.math.MyMath;
import com.spaceproject.math.Physics;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Sprite3D;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class EntityBuilder {

    private static final EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    private static final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private static final CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);

    //region ships
    public static Array<Entity> createBasicShip(float x, float y, boolean inSpace) {
        return createBasicShip(x, y, null, inSpace);
    }

    public static Array<Entity> createBasicShip(float x, float y, Entity driver, boolean inSpace) {
        return createBasicShip(x, y, MyMath.getSeed(x, y), driver, inSpace);
    }

    public static Array<Entity> createBasicShip(float x, float y, long seed, Entity driver, boolean inSpace) {
        Array<Entity> entityCluster = new Array<>();
        Entity shipEntity = new Entity();

        //seed
        MathUtils.random.setSeed(seed);
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        shipEntity.add(seedComp);

        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.VEHICLES.getHierarchy();
        transform.rotation = (float) Math.PI / 2; //face upwards
        shipEntity.add(transform);

        //generate 3D sprite with random even size
        int shipSize = MathUtils.random(entityCFG.shipSizeMin, entityCFG.shipSizeMax) * 2;
        Texture shipTop = TextureGenerator.generateShip(seed, shipSize);
        Texture shipBottom = TextureGenerator.generateShipUnderSide(shipTop);
        Sprite3DComponent sprite3DComp = new Sprite3DComponent();
        sprite3DComp.renderable = new Sprite3D(shipTop, shipBottom, engineCFG.sprite3DScale);
        shipEntity.add(sprite3DComp);

        //collision detection
        PhysicsComponent physics = new PhysicsComponent();
        float width = shipTop.getWidth() * engineCFG.bodyScale;
        float height = shipTop.getHeight() * engineCFG.bodyScale;
        physics.body = BodyBuilder.createShip(x, y, width, height, shipEntity, inSpace);
        shipEntity.add(physics);

        //engine data and marks entity as drive-able
        VehicleComponent vehicle = new VehicleComponent();
        vehicle.dimensions = new Rectangle(0, 0, width, height);
        vehicle.driver = driver;
        vehicle.thrust = entityCFG.engineThrust;
        shipEntity.add(vehicle);

        //health
        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.shipHealth;
        health.health = health.maxHealth;
        health.lastHitTime = GameScreen.getGameTimeCurrent() - 1000;
        shipEntity.add(health);

        //weapon
        CannonComponent cannon = makeCannon(vehicle.dimensions.width);
        shipEntity.add(cannon);
        vehicle.currentTool = VehicleComponent.Tool.cannon;
        /*
        int index = 0;
        switch(index) {
            case 0:
                CannonComponent cannon = makeCannon(vehicle.dimensions.width);
                shipEntity.add(cannon);
                vehicle.currentTool = VehicleComponent.Tool.cannon;
                break;
            case 1:
                ChargeCannonComponent chargeCannon = makeChargeCannon(vehicle.dimensions.width);
                shipEntity.add(chargeCannon);
                break;
                /*
            case 2:
                LaserComponent laser = new LaserComponent(520, 250, 30, 1);
                shipEntity.add(laser);
                break;
        }*/

        /*
        //vehicle.tools.put(VehicleComponent.Tool.laser.ordinal(), new LaserComponent(520, 250, 30, 1));
        TractorBeamComponent tractorBeam = new TractorBeamComponent();
        tractorBeam.maxDist = 150;
        tractorBeam.magnitude = 70000;
        //vehicle.tools.put(VehicleComponent.Tool.tractor.ordinal(), tractorBeam);

        //hyper drive
        HyperDriveComponent hyperDrive = new HyperDriveComponent();
        hyperDrive.speed = entityCFG.hyperSpeed;
        hyperDrive.coolDownTimer = new SimpleTimer(2000);
        hyperDrive.chargeTimer = new SimpleTimer(2000);
        hyperDrive.graceTimer = new SimpleTimer(1000);
        //shipEntity.add(hyperDrive);

        //shield
        ShieldComponent shield = new ShieldComponent();
        shield.animTimer = new SimpleTimer(100, true);
        shield.defence = 100f;
        float radius = Math.max(vehicle.dimensions.getWidth(), vehicle.dimensions.getHeight());
        shield.maxRadius = radius;
        shield.lastHit = GameScreen.getGameTimeCurrent() - 1000;
        shield.heatResistance = 0f;
        shield.cooldownRate = 0.1f;
        //shipEntity.add(shield);
         */

        //barrel roll
        BarrelRollComponent barrelRoll = new BarrelRollComponent();
        barrelRoll.cooldownTimer = new SimpleTimer(entityCFG.dodgeCooldown);
        barrelRoll.animationTimer = new SimpleTimer(entityCFG.dodgeAnimationTimer, true);
        barrelRoll.revolutions = 1;
        barrelRoll.flipState = BarrelRollComponent.FlipState.off;
        barrelRoll.force = entityCFG.dodgeForce;
        shipEntity.add(barrelRoll);

        //map
        MapComponent map = new MapComponent();
        map.color = new Color(1, 1, 1, 0.9f);
        map.distance = 3000;
        shipEntity.add(map);

        //shield particle effect
        ParticleComponent particle = new ParticleComponent();
        particle.type = ParticleComponent.EffectType.shieldCharge;
        particle.offset = new Vector2();
        particle.angle = 0;
        shipEntity.add(particle);

        //spline
        TrailComponent spline = new TrailComponent();
        spline.zOrder = 100;//should be on top of others
        spline.style = TrailComponent.Style.state;
        shipEntity.add(spline);

        //cargo
        CargoComponent cargo = new CargoComponent();
        shipEntity.add(cargo);

        //sound
        shipEntity.add(new SoundComponent());

        //engine particle effect. todo: kill off cluster, stuff multiple sources in particle component
        Entity mainEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineMain, new Vector2(0, height + 0.2f), 0);
        Entity leftEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineLeft, new Vector2(width/2 - 0.2f, 0), -90);
        Entity rightEngine = createEngine(shipEntity, ParticleComponent.EffectType.shipEngineRight, new Vector2(-(width/2 - 0.2f), 0), 90);

        entityCluster.add(shipEntity);
        entityCluster.add(mainEngine);
        entityCluster.add(leftEngine);
        entityCluster.add(rightEngine);

        return entityCluster;
    }

    public static ChargeCannonComponent makeChargeCannon(float width) {
        //width the anchor point relative to body
        ChargeCannonComponent chargeCannon = new ChargeCannonComponent();
        chargeCannon.anchorVec = new Vector2(width, 0);
        chargeCannon.aimAngle = 0;
        chargeCannon.velocity = entityCFG.cannonVelocity;
        chargeCannon.maxSize = 0.30f;
        chargeCannon.minSize = 0.1f;
        chargeCannon.growRateTimer = new SimpleTimer(1500);
        chargeCannon.baseDamage = 8f;
        return chargeCannon;
    }

    public static CannonComponent makeCannon(float width) {
        //width the anchor point relative to body
        CannonComponent cannon = new CannonComponent();
        cannon.damage = entityCFG.cannonDamage;
        cannon.cooldownRate = 1;
        cannon.heatRate = 0.1f;
        cannon.heatInaccuracy = 0.15f;
        cannon.baseRate = 300;
        cannon.minRate = 40;
        cannon.timerFireRate = new SimpleTimer(entityCFG.cannonFireRate);
        cannon.size = entityCFG.cannonSize;
        cannon.velocity = entityCFG.cannonVelocity;
        cannon.anchorVec = new Vector2(width, 0);
        cannon.aimAngle = 0;
        cannon.timerRechargeRate = new SimpleTimer(entityCFG.cannonRechargeRate);
        return cannon;
    }

    public static Entity createEngine(Entity parent, ParticleComponent.EffectType type, Vector2 offset, float angle) {
        Entity entity = new Entity();

        AttachedToComponent attached = new AttachedToComponent();
        attached.parentEntity = parent;
        entity.add(attached);

        //EngineComponent->thrust?
        //ShipEngineComponent engine = new ShipEngineComponent();
        //engine.engineState = ShipEngineComponent.State.off;
        //engine.thrust
        //entity.add(engine);

        ParticleComponent particle = new ParticleComponent();
        particle.type = type;
        particle.offset = offset;
        particle.angle = angle;
        entity.add(particle);

        //entity.add(new SoundEmitterComponent());
    
        /*
        todo: offset and angle like attached to?
        todo: alternatively, move offset and angle into AttachedTo and allow chaining
        SplineComponent test = new SplineComponent();
        test.zOrder = 200;
        test.style = SplineComponent.Style.solid;
        test.color = Color.GOLD;
        entity.add(test);
        */

        return entity;
    }
    //endregion

    //region characters
    public static Entity createCharacter(float x, float y) {
        Entity entity = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.CHARACTERS.getHierarchy();
        entity.add(transform);

        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.generateCharacter();
        texture.scale = engineCFG.sprite2DScale;
        entity.add(texture);

        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createPlayerBody(x, y, entity);
        entity.add(physics);

        CharacterComponent character = new CharacterComponent();
        character.walkSpeed = entityCFG.characterWalkSpeed;
        entity.add(character);

        HealthComponent health = new HealthComponent();
        health.maxHealth = entityCFG.characterHealth;
        health.health = health.maxHealth;
        entity.add(health);

        DashComponent dash = new DashComponent();
        dash.impulse = 3f;
        dash.dashTimeout = new SimpleTimer(1000);
        entity.add(dash);

        ControllableComponent control = new ControllableComponent();
        control.timerVehicle = new SimpleTimer(entityCFG.controlTimerVehicle);
        entity.add(control);

        return entity;
    }

    public static Entity createPlayer(float x, float y) {
        Entity character = createCharacter(x, y);
        character.add(new CameraFocusComponent());
        character.add(new ControlFocusComponent());
        character.add(new StatsComponent());
        return character;
    }

    public static Entity createCharacterAI(float x, float y) {
        Entity character = createCharacter(x, y);
        character.add(new AIComponent());
        return character;
    }

    public static Array<Entity> createPlayerShip(float x, float y, boolean inSpace) {
        Entity player = createPlayer(x, y);

        PhysicsComponent physicsComponent = player.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;

        Array<Entity> playerShipCluster = createBasicShip(x, y, 0, player, inSpace);
        Entity playerShip = playerShipCluster.first();

        ECSUtil.transferControl(player, playerShip);

        return playerShipCluster;
    }

    public static Array<Entity> createAIShip(float x, float y, boolean inSpace) {
        Entity ai = createCharacterAI(x, y);

        PhysicsComponent physicsComponent = ai.getComponent(PhysicsComponent.class);
        GameScreen.box2dWorld.destroyBody(physicsComponent.body);
        physicsComponent.body = null;

        Array<Entity> aiShipCluster = createBasicShip(x, y, 0, ai, inSpace);
        Entity aiShip = aiShipCluster.first();
        ECSUtil.transferControl(ai, aiShip);

        return aiShipCluster;
    }
    //endregion

    //region Astronomical / Celestial objects and bodies
    public static Entity createStar(World world, long seed, float x, float y, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();

        SeedComponent seedComponent = new SeedComponent();
        seedComponent.seed = seed;
        entity.add(seedComponent);

        //star properties
        int radius = MathUtils.random(celestCFG.minStarSize, celestCFG.maxStarSize);

        StarComponent star = new StarComponent();
        star.radius = radius;
        star.temperature = MathUtils.random(1000, 50000); //typically (2,000K - 40,000K)
        //star.temperature = Physics.Sun.kelvin;//test sun color
        star.peakWavelength = Physics.temperatureToWavelength(star.temperature) * 1000000;
        int[] colorTemp = Physics.wavelengthToRGB(star.peakWavelength);
        star.colorTemp = new Color(colorTemp[0] / 255.0f,
                colorTemp[1] / 255.0f,
                colorTemp[2] / 255.0f, 1);
        /*
        Vector3 spectrum = BlackBodyColorSpectrum.spectrumToXYZ(star.temperature);
        Vector3 color = BlackBodyColorSpectrum.xyzToRGB(BlackBodyColorSpectrum.SMPTEsystem, spectrum.x, spectrum.y, spectrum.z);
        BlackBodyColorSpectrum.constrainRGB(color);
        Vector3 normal = BlackBodyColorSpectrum.normRGB(color.x, color.y, color.z);
        star.colorTemp = new Color(normal.x, normal.y, normal.z, 1);
        */
        entity.add(star);

        // create star texture
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.generateStar(seed, radius, 20);
        texture.scale = 4;
        entity.add(texture);

        //sensor fixture to burn objects
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createCircleCensor(x, y, radius * 4, world, entity);
        entity.add(physics);

        // shader
        ShaderComponent shader = new ShaderComponent();
        shader.shaderType = ShaderComponent.ShaderType.star;
        entity.add(shader);

        // set position
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);

        //orbit for rotation of self (kinda hacky; not really orbiting, just rotating)
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = null;//set to null to negate orbit, but keep rotation
        orbit.rotateClockwise = rotationDir;
        orbit.rotSpeed = MathUtils.random(celestCFG.minStarRot, celestCFG.maxStarRot); //rotation speed of star
        entity.add(orbit);

        //mapState
        MapComponent map = new MapComponent();
        map.color =  new Color(1, 1, 0, 1);//star.colorTemp;
        map.distance = 80000;
        entity.add(map);

        return entity;
    }

    public static Entity createPlanet(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();

        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);

        //create placeholder texture. real texture will be generated by a thread
        TextureComponent texture = new TextureComponent();
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        int planetSize = (int) Math.pow(2, MathUtils.random(7, 10));
        texture.texture = TextureGenerator.generatePlanetPlaceholder(planetSize, chunkSize);
        texture.scale = 16;
        entity.add(texture);

        //transform
        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);

        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        entity.add(orbit);

        //minimap marker
        MapComponent map = new MapComponent();
        map.color = new Color(0.15f, 0.5f, 0.9f, 0.9f);
        map.distance = 10000;
        entity.add(map);

        //planet
        PlanetComponent planet = new PlanetComponent();
        planet.mapSize = planetSize;
        //TODO: randomize features/load from feature profile
        planet.scale = 100;
        planet.octaves = 4;
        planet.persistence = 0.68f;
        planet.lacunarity = 2.6f;
        entity.add(planet);


        return entity;
    }

    public static Entity createMoon(long seed, Entity parent, float radialDistance, boolean rotationDir) {
        MathUtils.random.setSeed(seed);
        Entity entity = new Entity();

        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        entity.add(seedComp);

        //create placeholder texture.
        TextureComponent texture = new TextureComponent();
        int size = (int) Math.pow(2, MathUtils.random(5, 7));
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        texture.texture = TextureGenerator.generatePlanetPlaceholder(size, chunkSize);
        texture.scale = 16;
        entity.add(texture);

        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        entity.add(transform);

        //orbit
        OrbitComponent orbit = new OrbitComponent();
        orbit.parent = parent;
        orbit.radialDistance = radialDistance;
        orbit.tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        orbit.startAngle = MathUtils.random(MathUtils.PI2);
        orbit.rotSpeed = MathUtils.random(celestCFG.minPlanetRot, celestCFG.maxPlanetRot);
        orbit.rotateClockwise = rotationDir;
        entity.add(orbit);

        //map
        MapComponent map = new MapComponent();
        map.color = new Color(0.5f, 0.6f, 0.6f, 0.9f);
        map.distance = 10000;
        entity.add(map);

        return entity;
    }

    public static Entity createAsteroid(float x, float y, float velX, float velY, float angle, float[] vertices, ItemComponent.Resource resource, boolean revealed) {
        Entity entity = new Entity();

        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.rotation = angle;
        entity.add(transform);
    
        /*
        NOTE! Box2D expects Polygons vertices are stored with a counter clockwise winding (CCW).
        We must be careful because the notion of CCW is with respect to a right-handed coordinate
        system with the z-axis pointing out of the plane.
        */
        //GeometryUtils.ensureCCW(vertices); //appears to do nothing...
        Polygon polygon = new Polygon(vertices);
        float area = Math.abs(GeometryUtils.polygonArea(polygon.getVertices(), 0, polygon.getVertices().length));
        AsteroidComponent asteroid = new AsteroidComponent();
        asteroid.polygon = polygon;
        asteroid.area = area;
        asteroid.composition = resource;
        asteroid.color = resource.getColor();
        asteroid.revealed = revealed;
        asteroid.refractiveIndex = resource.getRefractiveIndex();
        entity.add(asteroid);

        PhysicsComponent physics = new PhysicsComponent();
        float density = resource.getDensity();
        physics.body = BodyBuilder.createPoly(transform.pos.x, transform.pos.y,
                polygon.getVertices(), angle, density, BodyDef.BodyType.DynamicBody,
                GameScreen.box2dWorld, entity);
        asteroid.centerOfMass = physics.body.getLocalCenter().cpy();
        physics.body.setLinearVelocity(velX, velY);
        entity.add(physics);

        HealthComponent health = new HealthComponent();
        health.maxHealth = area * 0.1f * resource.getHardness();
        health.health = health.maxHealth;
        entity.add(health);

        return entity;
    }

    enum PolygonClass {
        irregular,
        triangle, rectangle, pentagon, hexagon, heptagon,
        rhombusGold,
        penRhombusThin, penRhombusThicc
    }

    static final ConvexHull convex = new ConvexHull();
    static final Vector2 center = new Vector2();
    public static Entity createAsteroid(long seed, float x, float y, float velX, float velY, float size) {
        int maxPoints = 7;//Box2D poly vert limit is 7: Assertion `3 <= count && count <= 8' failed.
        FloatArray points = new FloatArray();
        PolygonClass poly;

        DebugConfig debugConfig = SpaceProject.configManager.getConfig(DebugConfig.class);
        //debugConfig.spawnRegularBodies = true;
        //debugConfig.spawnPenrose = true;
        if (debugConfig.spawnRegularBodies) {
            if (debugConfig.spawnPenrose) {
                //penrose! rhombic prototiles
                //5: pentagram, 3: triangle
                //phi ø = π/5 = Tau/10 = 36 deg = 0.62 rad
                // The golden rhombus is distinguished from the two rhombi of the Penrose tiling,
                // which are both related in other ways to the golden ratio but have different shapes than the golden rhombus.
                //gold: 60 < 60  > 60
                //thin: 36 < 144 > 36  || ø < 4ø > ø
                //thic: 72 < 108 > 72  || 2ø < 3ø > 2ø
                poly = MathUtils.randomBoolean() ? PolygonClass.penRhombusThin : PolygonClass.penRhombusThicc;
                int gold = 60;
                int thin = 36;
                int thicc = 72;
                float rhombicAngle = poly == PolygonClass.penRhombusThicc ? thicc : thin;
                //rhombicAngle = gold; // ratio of 1: phi
                points.add(0, 0);
                points.add(size, 0);
                Vector2 vector = MyMath.vector(rhombicAngle * MathUtils.degRad, size);
                points.add(vector.x, vector.y);
                vector.add(size,0);
                points.add(vector.x, vector.y);
            } else {
                int segments = MathUtils.random(3, maxPoints);
                //source: ShapeRenderer.circle();
                float angle = 2 * MathUtils.PI / segments;
                float cos = MathUtils.cos(angle);
                float sin = MathUtils.sin(angle);
                float cx = size, cy = 0;
                for (int i = 0; i < segments; i++) {
                    points.add(x + cx, y + cy);
                    float temp = cx;
                    cx = cos * cx - sin * cy;
                    cy = sin * temp + cos * cy;
                }
                switch (points.size/2) {
                    case 3: poly = PolygonClass.triangle; break;
                    case 4: poly = PolygonClass.rectangle; break;
                    case 5: poly = PolygonClass.pentagon; break;
                    case 6: poly = PolygonClass.hexagon; break;
                    case 7: poly = PolygonClass.heptagon; break;
                }
                //Gdx.app.debug("",seed + " : " + segments + " " + poly.name());
            }
        } else {
            float tolerance = 3f;
            for (int i = 0; i < maxPoints * 2; i += 2) {
                //generate unique: Duplicate points will result in undefined behavior.
                float pX, pY;
                do {
                    pX = MathUtils.random(size);
                    pY = MathUtils.random(size);
                } while (isValidPoint(points, pX, pY, tolerance));
                points.add(pX, pY);
            }
        }
        
        //generate hull poly from random points
        float[] hull = convex.computePolygon(points, false).toArray();
        
        //shift vertices to be centered around 0,0 relatively
        GeometryUtils.polygonCentroid(hull, 0, hull.length, center);
        for (int index = 0; index < hull.length; index += 2) {
            hull[index] -= center.x;
            hull[index + 1] -= center.y;
        }

        //todo: scale irregular bodies to desired size because current method is doesnt match
        //GeometryUtils.ensureCCW(hull);
        //Polygon polygon = new Polygon(hull);
        //float area = Math.abs(GeometryUtils.polygonArea(polygon.getVertices(), 0, polygon.getVertices().length));

        ItemComponent.Resource resource = debugConfig.glassOnly ? ItemComponent.Resource.GLASS : ItemComponent.Resource.random();
        return createAsteroid(x, y, velX, velY, MathUtils.random(MathUtils.PI2), hull, resource, false);
    }

    /*todo: should we make custom poly to avoid toArray() causing a system System.arraycopy()
    static public Vector2 polygonCentroid (FloatArray polygon, int offset, int count, Vector2 centroid) {
        if (count < 6) throw new IllegalArgumentException("A polygon must have 3 or more coordinate pairs.");

        float area = 0, x = 0, y = 0;
        int last = offset + count - 2;
        float x1 = polygon[last], y1 = polygon[last + 1];
        for (int i = offset; i <= last; i += 2) {
            float x2 = polygon[i], y2 = polygon[i + 1];
            float a = x1 * y2 - x2 * y1;
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
            centroid.x = x / (6 * area);
            centroid.y = y / (6 * area);
        }
        return centroid;
    }*/
    
    private static boolean isValidPoint(FloatArray points, float pX, float pY, float tolerance) {
        for (int i = 0; i < points.size; i += 2) {
            if (Vector2.dst(points.get(i), points.get(i + 1), pX, pY) <= tolerance) {
                return true;
            }
            /*
            //if 3 or more points, check for colinear
            if (i < 3 * 2) {
                //todo: remove or push out center of colinear points:
                if (GeometryUtils.colinear(i, i+1, i+2, i+3, i+4, i+5)) {
                    return false;
                }
            }*/
        }
        return false;
    }
    //endregion
    
    public static Entity createWall(float x, float y, int width, int height) {
        Entity entity = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.generateTexture(
                width * engineCFG.pixelPerUnit,
                height * engineCFG.pixelPerUnit,
                new Color(0.4f, 0.4f, 0.4f, 1),
                new Color(0, 0, 0, 1));
        entity.add(texture);
    
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createWall(x, y, (int) (width * engineCFG.bodyScale), (int) (height * engineCFG.bodyScale), entity);
        entity.add(physics);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.WORLD_OBJECTS.getHierarchy();
        entity.add(transform);
        
        return entity;
    }
    
    public static Entity createSpaceStation(Entity parentOrbitEntity, float radialDistance, boolean rotationDir) {
        Entity entity = new Entity();
    
        int width = 320;
        int height = 640;
        Vector2 parentBody = Mappers.transform.get(parentOrbitEntity).pos;
        float x = parentBody.x;
        float y = parentBody.y + radialDistance;
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.generateTexture(
                (int) (width * engineCFG.sprite2DScale),
                (int) (height * engineCFG.sprite2DScale),
                new Color(0.4f, 0.4f, 0.4f, 1),
                new Color(1, 1, 1, 1));
        texture.scale = 1f;
        entity.add(texture);
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createSpaceStation(x, y,
                (int) (width * engineCFG.bodyScale),
                (int) (height * engineCFG.bodyScale),
                BodyDef.BodyType.DynamicBody, entity);
        entity.add(physics);
    
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.WORLD_OBJECTS.getHierarchy();
        entity.add(transform);
    
        SpaceStationComponent station = new SpaceStationComponent();
        station.parentOrbitBody = parentOrbitEntity;
        station.velocity = 20f;
        entity.add(station);

        MapComponent map = new MapComponent();
        map.color = new Color(0, 1f, 0, 0.9f);
        map.distance = 10000;
        entity.add(map);

        return entity;
    }
    
    public static Entity dropResource(Vector2 position, Vector2 velocity, ItemComponent.Resource composition, Color color) {
        Entity drop = new Entity();
        
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.createTile(color);
        //todo: rng texture shape between circle, triangle, square
        //texture.texture = TextureFactory.createCircle(asteroid.color);
        texture.scale = 2f;
        
        TransformComponent transform = new TransformComponent();
        Vector2 pos = transform.pos.set(position);
        
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createDrop(pos.x, pos.y, 2, drop);
        physics.body.setLinearVelocity(velocity);
        float angularVelocity = MathUtils.random(1f, 5f);
        physics.body.applyAngularImpulse(MathUtils.randomBoolean() ? angularVelocity : -angularVelocity, true);

        ItemComponent item = new ItemComponent();
        //todo: come up with some sort of composition and chance to drop eg: ruby 0.5%, emerald 0.5%, sapphire 0.5%. diamond 0.1%
        item.resource = composition;

        //expire time (self destruct)
        ExpireComponent expire = new ExpireComponent();
        expire.timer = new SimpleTimer(60 * 1000, true);
        
        drop.add(texture);
        drop.add(transform);
        drop.add(physics);
        drop.add(item);
        drop.add(expire);
        return drop;
    }
    
}
