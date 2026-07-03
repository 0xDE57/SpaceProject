package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class WorldLoadingSystem extends EntitySystem {

    @Override
    public void addedToEngine(Engine engine) {
        initMobs(engine);
    }
    
    private void initMobs(Engine engine) {
        WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
        //a placeholder to add dummy objects for now
        
        // test ships
        int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
    
        Array<Entity> basicShipCluster = EntityBuilder.createBasicShip(position + 10, position + 10, GameScreen.inSpace());
        for (Entity e : basicShipCluster) {
            engine.addEntity(e);
        }
        Array<Entity> basicShipCluster2 = EntityBuilder.createBasicShip(position - 10, position + 10, GameScreen.inSpace());
        for (Entity e : basicShipCluster2) {
            engine.addEntity(e);
        }
        
        Entity aiTest = EntityBuilder.createCharacterAI(position, position + 10);
        Mappers.AI.get(aiTest).state = AIComponent.State.wander;
        engine.addEntity(aiTest);

		/*
		Entity aiTest2 = EntityFactory.createCharacterAI(position, position - 500);
		Mappers.AI.get(aiTest2).State = AIComponent.State.takeOffPlanet;
		engine.addEntity(aiTest2);
		*/
        //engine.addEntity(EntityFactory.createWall(position + 5, position + 5, 8, 16));
        //engine.addEntity(EntityFactory.createWall(position + 9, position + 5, 16, 8));
    }
    
}
