package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.Mappers;

public class WorldWrapSystem extends EntitySystem implements IRequireGameContext {
    /**
     * TODO: fix world wrapping
     * <p>
     * The world/tiles are simply wrapped using techniques described here:
     * http://ronvalstar.nl/creating-tileable-noise-maps/
     * https://gamedev.stackexchange.com/questions/23625/how-do-you-generate-tileable-perlin-noise
     * <p>
     * But the physics and rendering do not like this and there are some technical difficulties to overcome:
     * -“Physics and collision detection have to deal with fact, that object,
     * that crossing the border can be in two (or even four) places
     * simultaneously, and have to collide with other objects from other side.”
     * -“Ai and pathfinding (Bots should see through the border).”
     * “Sounds should be properly positioned.”
     * -“Effects and trails. Just imagine particle emitter crossing the
     * border, and what you should do to make it properly visible from both
     * sides?”
     * https://simonschreibt.de/gat/1nsane-carpet-2-repetitive-worlds/
     * <p>
     * <p>
     * https://imgur.com/a/fUD7n
     * https://godotdevelopers.org/forum/discussion/18108/how-to-create-a-self-wrapping-2d-world-defender
     * https://hero.handmade.network/forums/code-discussion/t/310-movement_on_spherical_vs_toroidal_tile_map
     * https://en.wikipedia.org/wiki/Toroidal_coordinates
     * http://jsfiddle.net/fg5bLh6e/
     */

    private int wrap;
    private int offsetX, offsetY;
    private ImmutableArray<Entity> entities;
    
    @Override
    public void initContext(GameScreen gameScreen) {
        int mapSize = gameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
        WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        wrap = worldCFG.tileSize * mapSize;
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(Family.all(TransformComponent.class).get());
    }
    
    @Override
    public void update(float delta) {
        boolean shifted = false;
        Vector3 camPos = MyScreenAdapter.cam.position;
        //if cam away from border and crossed border, shift...?
        
        
        for (Entity entity : entities) {
            Vector2 entityPos = Mappers.transform.get(entity).pos;
            
            //simple wrap approach
            if (entityPos.x >= wrap) entityPos.x -= wrap;
            if (entityPos.y >= wrap) entityPos.y -= wrap;
            if (entityPos.x < 0) entityPos.x += wrap;
            if (entityPos.y < 0) entityPos.y += wrap;

			/*
			//simple offset approach
			if (entityPos.x >= wrap) {
				entityPos.x -= wrap;
				offsetX += wrap;
				shifted = true;
			}
			if (entityPos.y >= wrap) {
				entityPos.y -= wrap;
				offsetY += wrap;
				shifted = true;
			}
			if (entityPos.x < 0) {
				entityPos.x += wrap;
				offsetX -= wrap;
				shifted = true;
			}
			if (entityPos.y < 0) {
				entityPos.y += wrap;
				offsetY -= wrap;
				shifted = true;
			}*/
        }
        
        if (shifted) {
            Gdx.app.log(this.getClass().getSimpleName(), "Wrap offset: " + offsetX + ", " + offsetY);
        }
    }
    
}
