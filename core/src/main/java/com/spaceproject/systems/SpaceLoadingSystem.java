package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.AsteroidBeltComponent;
import com.spaceproject.components.BarycenterComponent;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.SeedComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.AstroBody;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.math.MyMath;
import com.spaceproject.noise.NoiseBuffer;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import static com.spaceproject.screens.MyScreenAdapter.cam;


public class SpaceLoadingSystem extends EntitySystem implements EntityListener {
    
    private final CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    private ImmutableArray<Entity> loadedAstronomicalBodies;
    private ImmutableArray<Entity> orbitingBodies;
    //private ImmutableArray<Entity> spaceStations;
    private SimpleTimer loadTimer;
    
    private boolean hasInit;
    
    @Override
    public void addedToEngine(Engine engine) {
        // currently loaded stars/planets
        loadedAstronomicalBodies = engine.getEntitiesFor(Family.all(BarycenterComponent.class, TransformComponent.class).get());
        orbitingBodies = engine.getEntitiesFor(Family.all(OrbitComponent.class).get());
        
        loadTimer = new SimpleTimer(4000);
        loadTimer.setCanDoEvent();
    
        hasInit = false;
        
        engine.addEntityListener(this);
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        engine.removeEntityListener(this);
    }
    
    @Override
    public void entityAdded(Entity entity) {
        PlanetComponent planet = Mappers.planet.get(entity);
        if (planet != null) {
            long seed = Mappers.seed.get(entity).seed;
            GameScreen.noiseManager.loadOrCreateNoiseFor(seed, planet);
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        for (Entity e : orbitingBodies) {
            OrbitComponent orbit = Mappers.orbit.get(e);
            if (orbit.parent != null && orbit.parent == entity) {
                e.add(new RemoveComponent());
            }
        }
    }

    @Override
    public void update(float delta) {
        //if (!hasInit) { initTestDebugMobs(getEngine()); }
        
        // load and unload stars
        updateLoadedBodies(celestCFG.loadSystemDistance);
        
        // update/replace textures
        updatePlanetTextures();

        //debug
        /*
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            Vector2 camPos = new Vector2(cam.position.x, cam.position.y);
            Entity spaceStation = ECSUtil.closestEntity(camPos, spaceStations);
            if (spaceStation == null) {
                return;
            }
            Vector2 pos = Mappers.physics.get(spaceStation).body.getPosition();
            //todo: pos.add(dockedPortOffset)
            Array<Entity> playerShip = EntityBuilder.createPlayerShip(pos.x, pos.y, true);
            for (Entity entity : playerShip) {
                getEngine().addEntity(entity);
            }
            Mappers.spaceStation.get(spaceStation).dockedPortA = playerShip.first();
            
            /*
            GameScreen.galaxy.objects.clear();
            GameScreen.galaxy.points.clear();

            unloadFarEntities(0);
            GameScreen.noiseManager.clearQueue();

            /*
            AstroBody astroBody = new AstroBody(new Vector2(1337, 420));
            astroBody.seed = MathUtils.random(Long.MIN_VALUE, Long.MAX_VALUE);
            GameScreen.galaxy.points.add(new Vector2(astroBody.x, astroBody.y));
            GameScreen.galaxy.objects.add(astroBody);*

            for (Entity e : createPlanetarySystem(1337, 420, ThreadLocalRandom.current().nextLong())) {
                getEngine().addEntity(e);
            }
            
        }*/
    }
    
    //region load
    private void updateLoadedBodies(float loadDistance) {
        if (!loadTimer.tryEvent()) return;
        
        loadDistance *= loadDistance;//square for dst2
    
        // remove stars from engine that are too far
        unloadFarEntities(loadDistance);
    
        // add planetary systems to engine
        loadCloseEntities(loadDistance);
    }
    
    private void loadCloseEntities(float loadDistance) {
        for (AstroBody astroBodies : GameScreen.galaxy.objects) {
            //check if point is close enough to be loaded
            if (Vector2.dst2(astroBodies.x, astroBodies.y, cam.position.x, cam.position.y) > loadDistance) {
                continue;
            }
            
            // check if astro bodies already in world
            boolean loaded = false;
            for (Entity loadedEntity : loadedAstronomicalBodies) {
                SeedComponent seed = Mappers.seed.get(loadedEntity);
                if (seed.seed == astroBodies.seed) {
                    loaded = true;
                    break;
                }
            }
            if (!loaded) {
                for (Entity newEntity : createAstronomicalObjects(astroBodies.x, astroBodies.y)) {
                    getEngine().addEntity(newEntity);
                }
            }
        }
    }
    
    private void unloadFarEntities(float loadDistance) {
        for (Entity entity : loadedAstronomicalBodies) {
            TransformComponent transform = Mappers.transform.get(entity);
            float distance = Vector2.dst2(transform.pos.x, transform.pos.y, cam.position.x, cam.position.y);
            if (distance > loadDistance) {
                entity.add(new RemoveComponent());
                Gdx.app.log(getClass().getSimpleName(), "Removing Planetary System: " + entity.getComponent(TransformComponent.class).pos.toString());
            }
        }
    }
    
    private void updatePlanetTextures() {
        //todo: this should probably subscribe to a thread finished event instead of polling
        //check queue for tilemaps / pixmaps to load into textures
        if (!GameScreen.noiseManager.isNoiseAvailable()) {
            return;
        }
        
        //grab noise from queue
        NoiseBuffer noise = GameScreen.noiseManager.getNoiseFromQueue();
        if (noise.pixelatedTileMap == null) {
            Gdx.app.log(getClass().getSimpleName(), "ERROR, no map for: [" + noise.seed + "]");
            return;
        }
        
        //find planet that noise belongs to (matching seed)
        for (Entity p : getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get())) {
            if (p.getComponent(SeedComponent.class).seed == noise.seed) {
                // create planet texture from tileMap, replace texture
                Texture newTex = TextureGenerator.generatePlanet(noise.pixelatedTileMap, Tile.defaultTiles);
                p.getComponent(TextureComponent.class).texture = newTex;
                Gdx.app.log(getClass().getSimpleName(), "Texture loaded: [" + noise.seed + "]");
                return;
            }
        }
    }
    //endregion
    
    //region create bodies
    public Array<Entity> createAstronomicalObjects(float x, float y) {
        if (!GameScreen.isDebugMode) {
            long seed = MyMath.getSeed(x, y);
            MathUtils.random.setSeed(seed);
            
            switch (MathUtils.random(2)) {
                case 0:
                    return createPlanetarySystem(x, y);
                case 1:
                    return createBinarySystem(x, y);
                case 2:
                    return createRoguePlanet(x, y);
            }
        }
        
        return createPlanetarySystem(x, y);
    }
    
    public Array<Entity> createPlanetarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createPlanetarySystem(x, y, seed);
    }
    
    public Array<Entity> createPlanetarySystem(float x, float y, long seed) {
        MathUtils.random.setSeed(seed);
        
        //number of planets in a system
        int numPlanets = MathUtils.random(celestCFG.minPlanets, celestCFG.maxPlanets);
        
        //distance between planets
        float distance = celestCFG.minPlanetDist / 3; //add some initial distance between star and first planet
        
        //rotation of system (orbits and spins)
        boolean isRotateClockwise = MathUtils.randomBoolean();
        
        //collection of planets/stars
        Array<Entity> entities = new Array<Entity>();
        
        //add star to center of planetary system
        Entity star = EntityBuilder.createStar(GameScreen.box2dWorld, seed, x, y, isRotateClockwise);
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = numPlanets == 0 ? BarycenterComponent.AstronomicalBodyType.loneStar : BarycenterComponent.AstronomicalBodyType.uniStellar;
        star.add(barycenter);
    
        //todo, allow multiple disks at different radius: make sure layer is distinct (not overlap planet orbits)
        AsteroidBeltComponent circumstellarDisc = new AsteroidBeltComponent();
        circumstellarDisc.radius = 1500;
        circumstellarDisc.bandWidth = 220; //how wide of band centered on radius, todo: concentrate larger bodies at radius, getting smaller outward
        circumstellarDisc.maxSpawn = 180; //todo: calculate density: ratio of asteroids to space in disk
        circumstellarDisc.velocity = 20;
        circumstellarDisc.clockwise = isRotateClockwise;
        star.add(circumstellarDisc);
        entities.add(star);

        Entity spaceStation;
        //if (numPlanets == 0) {
        spaceStation = EntityBuilder.createSpaceStation(star, Mappers.star.get(star).radius * 4 + 200, false);
        entities.add(spaceStation);
        
        /*
        if (!hasInit) {
            hasInit = true;
            Vector2 pos = Mappers.physics.get(spaceStation).body.getPosition();
            Array<Entity> playerShip = EntityBuilder.createPlayerShip(pos.x, pos.y, true);
            for (Entity entity : playerShip) {
                getEngine().addEntity(entity);
            }
            Mappers.spaceStation.get(spaceStation).dockedPortA = playerShip.first();
        }*/
        
        //create planes around star
        for (int i = 0; i < numPlanets; ++i) {
            //add some distance from previous entity
            distance += MathUtils.random(celestCFG.minPlanetDist, celestCFG.maxPlanetDist);
            
            //create planet
            long planetSeed = MyMath.getSeed(x, y + distance);
            Entity planet = EntityBuilder.createPlanet(planetSeed, star, distance, isRotateClockwise);
            
            //add moon
            boolean hasMoon = MathUtils.randomBoolean();
            if (hasMoon) {
                float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
                moonDist *= 0.7f;
                planet.getComponent(OrbitComponent.class).radialDistance += moonDist; //push further out
                distance += moonDist * 2;
                Entity moon = EntityBuilder.createMoon(MyMath.getSeed(x, y + distance), planet, moonDist, isRotateClockwise);
                entities.add(moon);
                
                /*// nested test
                distance += moonDist;
                Entity moonsMoon = EntityFactory.createMoon(MyMath.getSeed(x, y + distance), moon, moonDist, rotDir);
                entities.add(moonsMoon);
    
                distance += moonDist;
                Entity moonsMoonsMoon = EntityFactory.createMoon(MyMath.getSeed(x, y + distance), moonsMoon, moonDist, rotDir);
                entities.add(moonsMoonsMoon);*/
            }
            
            addLifeToPlanet(planet);
            
            entities.add(planet);
        }

        //hack in outer belt
        Entity secondBelt = new Entity();
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        transform.zOrder = RenderOrder.ASTRO.getHierarchy();
        secondBelt.add(transform);
        AsteroidBeltComponent outerBelt = new AsteroidBeltComponent();
        outerBelt.radius = distance + 1500;
        outerBelt.bandWidth = 300;
        outerBelt.maxSpawn = 400;
        outerBelt.velocity = 20;
        outerBelt.clockwise = isRotateClockwise;
        secondBelt.add(outerBelt);
        entities.add(secondBelt);

        Gdx.app.log(getClass().getSimpleName(), "Planetary System: [" + seed + "](" + x + ", " + y + ") Bodies: " + (numPlanets));
        
        return entities;
    }
    
    private static void addLifeToPlanet(Entity planet) {
        //add entity spawner if planet has life
        //dumb coin flip for now, can have rules later like no life when super close to star = lava, or super far = ice
        //simply base it on distance from star. habital zone
        //  eg chance of life = distance from habit zone
        //more complex rules can be applied like considering the planets type. ocean might have life but if entire planet is ocean = no ships = no spawner
        //desert might have different life, so different spawner rules
        
        //boolean hasLife = MathUtils.randomBoolean();
        
        AISpawnComponent spawnComponent = new AISpawnComponent();
        spawnComponent.maxSpawn = 10;
        planet.add(spawnComponent);
    }
    
    public Array<Entity> createRoguePlanet(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createRoguePlanet(x, y, seed);
    }
    
    public Array<Entity> createRoguePlanet(float x, float y, long seed) {
        MathUtils.random.setSeed(seed);
        Array<Entity> entities = new Array<>();
        
        Entity planet = EntityBuilder.createPlanet(seed, null, 0, MathUtils.randomBoolean());
        planet.getComponent(OrbitComponent.class).tangentialSpeed = 0;
        
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.roguePlanet;
        planet.add(barycenter);
        planet.getComponent(TransformComponent.class).pos.set(x, y);
        
        //add moon
        boolean hasMoon = MathUtils.randomBoolean();
        if (hasMoon) {
            float moonDist = planet.getComponent(TextureComponent.class).texture.getWidth() * planet.getComponent(TextureComponent.class).scale * 2;
            boolean rotDir = MathUtils.randomBoolean();
            Entity moon = EntityBuilder.createMoon(MyMath.getSeed(x, y + moonDist), planet, moonDist, rotDir);
            entities.add(moon);
        }
        
        entities.add(planet);
        Gdx.app.log(this.getClass().getSimpleName(), "Rogue Planet: [" + seed + "](" + x + ", " + y + ")");
        return entities;
    }
    
    public Array<Entity> createBinarySystem(float x, float y) {
        long seed = MyMath.getSeed(x, y);
        return createBinarySystem(x, y, seed);
    }
    
    public Array<Entity> createBinarySystem(float x, float y, long seed) {
        Entity anchorEntity = new Entity();
        
        SeedComponent seedComp = new SeedComponent();
        seedComp.seed = seed;
        anchorEntity.add(seedComp);
        
        BarycenterComponent barycenter = new BarycenterComponent();
        barycenter.bodyType = BarycenterComponent.AstronomicalBodyType.multiStellar;
        anchorEntity.add(barycenter);
        
        TransformComponent transform = new TransformComponent();
        transform.pos.set(x, y);
        anchorEntity.add(transform);
    
        
        //add stars
        float distance = celestCFG.maxPlanetSize * 2 + celestCFG.maxPlanetDist * 2;
        boolean rotDir = MathUtils.randomBoolean();
        float startAngle = MathUtils.random(MathUtils.PI2);
        float tangentialSpeed = MathUtils.random(celestCFG.minPlanetTangentialSpeed, celestCFG.maxPlanetTangentialSpeed);
        
        Entity starA = EntityBuilder.createStar(GameScreen.box2dWorld, MyMath.getSeed(x + distance, y), x + distance, y, rotDir);
        OrbitComponent orbitA = starA.getComponent(OrbitComponent.class);
        orbitA.parent = anchorEntity;
        orbitA.radialDistance = distance;
        orbitA.startAngle = startAngle;
        orbitA.tangentialSpeed = tangentialSpeed;
        
        Entity starB = EntityBuilder.createStar(GameScreen.box2dWorld, MyMath.getSeed(x - distance, y), x - distance, y, rotDir);
        OrbitComponent orbitB = starB.getComponent(OrbitComponent.class);
        orbitB.parent = anchorEntity;
        orbitB.radialDistance = distance;
        orbitB.startAngle = startAngle + MathUtils.PI;
        orbitB.tangentialSpeed = tangentialSpeed;
        
        
        Array<Entity> entities = new Array<Entity>();
        entities.add(anchorEntity);
        entities.add(starA);
        entities.add(starB);
        
        Gdx.app.log(this.getClass().getSimpleName(), "Binary System: [" + seed + "](" + x + ", " + y + ")");
        return entities;
    }
    //endregion
    
    public void initTestDebugMobs(Engine engine) {
        hasInit = true;
        
        //a placeholder to add dummy objects for now
        for (Entity e : EntityBuilder.createBasicShip(1300, 1600, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityBuilder.createBasicShip(1330, 1600, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityBuilder.createBasicShip(1340, 1600, GameScreen.inSpace())) {
            engine.addEntity(e);
        }
        for (Entity e : EntityBuilder.createBasicShip(1360, 1600, GameScreen.inSpace())) {
            engine.addEntity(e);
        }

        /*
        Entity aiTest = EntityBuilder.createCharacterAI(0, 40);
        Mappers.AI.get(aiTest).state = AIComponent.State.wander;
        //aiTest.add(new CameraFocusComponent());//test cam focus on AI
        engine.addEntity(aiTest);
        
        Entity aiTest2 = EntityBuilder.createCharacterAI(0, 60);
        Mappers.AI.get(aiTest2).state = AIComponent.State.idle;
        engine.addEntity(aiTest2);
        
        Entity aiTest3 = EntityBuilder.createCharacterAI(0, 80);
        Mappers.AI.get(aiTest3).state = AIComponent.State.landOnPlanet;
        //aiTest3.add(new CameraFocusComponent());
        engine.addEntity(aiTest3);*/
    }
    
}
