package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.ResourceDisposer;


public class RemovalSystem extends IteratingSystem {
    
    public RemovalSystem() {
        super(Family.one(RemoveComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ResourceDisposer.dispose(entity);
        getEngine().removeEntity(entity);
        
        //attempt to respawn player
        //respawnPlayer(entity); //todo: move to respawn system, subscribe entity removed
    }
    
    private void respawnPlayer(Entity entity) {
        //todo: should not be responsibility of removal system. player spawn system?
        //change to, no player detected -> spawning? or fire an event?
        ControlFocusComponent controlFocusComp = Mappers.controlFocus.get(entity);
        if (controlFocusComp != null) {
            Gdx.app.log(this.getClass().getSimpleName(), "Controlled entity assumed to be player; respawning...");
            if (GameScreen.inSpace()) {
                Array<Entity> newPlayer = EntityBuilder.createPlayerShip(0, 0, true);
                for (Entity e : newPlayer) {
                    getEngine().addEntity(e);
                }
            } else {
                WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
                int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
                int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
                Entity newPlayer = EntityBuilder.createPlayer(position, position);
                getEngine().addEntity(newPlayer);
            }
        }
    }
    
}
