package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.AISpawnComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class PlanetAISpawnerSystem extends IteratingSystem implements EntityListener {
    
    //goal: give life to the universe
    //when in a planetary system (loaded planets)
    //if planet has life
    //  add to planet SpawnerComponent
    //  add spawning rules
    //possible rules:
    //  ChanceSpawner: chance to spawn on interval
    //      eg: 30 seconds interval, 50% spawn chance
    //  IntervalSpawner: spawn x every x seconds
    //  RandomIntervalSpawner: spawn x between interval(min,max)
    //could have a more busy planet with more spawners / lower interval
    //should consider spawn caps. we don't want to spawn too many,
    //so should consider local count for how many spawner has spawned (entity tracking list?)
    //could have spawnCap for spawner
    //should also consider global entity count, we don't want to just endlessly fill
    //eg: within this system, don't spawn more than 30?
    
    private ImmutableArray<Entity> aiEntities;
    private final int maxGlobalSpawn = 1;
    
    public PlanetAISpawnerSystem() {
        super(Family.all(AISpawnComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        aiEntities = engine.getEntitiesFor(Family.all(AIComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        if (aiEntities.size() <= maxGlobalSpawn) {
            spawn(entity);
        }
    }
    
    private void spawn(Entity spawnSourceEntity) {
        AISpawnComponent spawn = Mappers.spawn.get(spawnSourceEntity);
        if (!spawn.spawnTimer.canDoEvent()) {
            return;
        }
        
        //spawn ship at planets location
        TransformComponent transform = Mappers.transform.get(spawnSourceEntity);
        Array<Entity> aiShipCluster = EntityBuilder.createAIShip(transform.pos.x, transform.pos.y, GameScreen.inSpace());
        Entity aiShip = aiShipCluster.first();
        
        //handle spawn state
        AIComponent aiComponent = aiShip.getComponent(AIComponent.class);
        aiComponent.state = spawn.state;
        switch (aiComponent.state) {
            case attack: {
                //todo: agro test
                //find player entity, send angry ships at them
                ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(ControlFocusComponent.class).get());
                if (players.size() == 0) {
                    return;
                }
                aiComponent.attackTarget = players.first();
            }
            break;
            case landOnPlanet: {
                //find a planet that is not the one we spawned on. set it as target. go land.
                ImmutableArray<Entity> planets = getEngine().getEntitiesFor(Family.all(PlanetComponent.class).get());
                Entity destinationPlanet;
                int failSafe = 20;
                do {
                    destinationPlanet = planets.random();
                } while (destinationPlanet == spawnSourceEntity && failSafe-- >= 0);
                
                if (destinationPlanet != null) {
                    aiComponent.planetTarget = destinationPlanet;
                }
    
                //todo: ensure there are other planets available
                //todo: ensure planet is local star?
                // maybe the AI wants to go to another system even
                // eg: % chance destination is another start system outside current
                // and % chance random direction
            }
        }
        
        //spawn
        for (Entity ent : aiShipCluster) {
            getEngine().addEntity(ent);
        }
        spawn.spawnCount++;
        spawn.spawnTimer.setInterval(MathUtils.random(100, 1000), true);
        Gdx.app.debug(getClass().getSimpleName(),
                "spawned: " + DebugUtil.objString(aiShip)
                        + " @(" + MyMath.formatVector2(transform.pos, 2)
                        + ") source:" + DebugUtil.objString(spawnSourceEntity)
                        + ") destination:" + DebugUtil.objString(aiComponent.planetTarget));
    
        Entity player = getEngine().getEntitiesFor(Family.all(ControlFocusComponent.class).get()).first();
        ECSUtil.transferComponent(player, aiShip, CameraFocusComponent.class);
        
    }
    
    @Override
    public void entityAdded(Entity entity) {
        if (Mappers.planet.get(entity) != null) {
            addLifeToPlanet(entity);
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) { }
    
    private static void addLifeToPlanet(Entity planet) {
        //add entity spawner if planet has life
        //dumb coin flip for now, can have rules later like no life when super close to star = lava, or super far = ice
        //simply base it on distance from star. habital zone
        //  eg chance of life = distance from habit zone
        //more complex rules can be applied like considering the planets type. ocean might have life but if entire planet is ocean = no ships = no spawner
        //desert might have different life, so different spawner rules
        //boolean hasLife = true;
        
        //if (hasLife) {
            //int min = 10000;
            //int max = 100000;
            //int lifeDensity =
            
        AISpawnComponent spawnComponent = new AISpawnComponent();
        spawnComponent.state = AIComponent.State.landOnPlanet;
        spawnComponent.spawnTimer = new SimpleTimer(10);
        spawnComponent.maxSpawn = 10;
        
        planet.add(spawnComponent);
    }
    
}
