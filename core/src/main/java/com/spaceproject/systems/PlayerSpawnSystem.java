package com.spaceproject.systems;

import com.badlogic.ashley.core.*;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.EntityBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;

public class PlayerSpawnSystem extends EntitySystem {
    
    //  new game:
    //  > start at space station
    //      > which one? closest to 0, 0? set starter spawn
    //      > what if no space station found?
    //      > spawn space station at nearest star?
    //      > what if no star found? nearest planet or any body?
    //      > failsafe: force spawn 0, 0
    //  > set last spawn station to selected stater station
    //  > auto-spawn at starter station
    //  > anytime land, set current spawn to station
    //
    //  die:
    //  > [DESTROYED]
    //  > press [any key] to respawn
    //  > respawn at last used space station: ei: "checkpoint"
    //  ExplodeEntity
    //      transfer cam focus player -> explode
    //      explode particle fx
    //      outward ring effect
    //      ReSpawnComponent, given to explosion entity on death
    //          .reason = stars are hot, asteroids are solid, killed by [entity]
    //          waits for keypress to restart
    //  on respawn keypress, camera pans from explode position to spawn location
    //      explode entity removed.
    //      then respawn starter ship docked at space station. lose all cargo, credits and upgrades.
    //      perhaps all or a % of cargo can be retrieved from explosion location?
    //
    //  take off planet
    //  > spawn at planet
    //      > which planet? load universe. find saved planet
    //
    //  land on planet -> spawn somewhere on planet
    //      > where on planet? don't want to land in water or lava (unless ship floats in water?)
    //

    private boolean initialSpawn = false;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> respawn;
    private ImmutableArray<Entity> spaceStations;

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class).get());
        respawn = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
        spaceStations = engine.getEntitiesFor(Family.all(SpaceStationComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (players.size() != 0) {
            return;
        }
        if (players.size() > 1) {
            Gdx.app.error(getClass().getSimpleName(), "warning! multiple players");
        }

        if (!initialSpawn) {
            initialSpawn = true;
            spawnPlayerSpaceStation();
        }

        if (respawn.size() > 0) {
            Entity respawnEntity = respawn.first();
            RespawnComponent respawn = Mappers.respawn.get(respawnEntity);

            if (GameScreen.inSpace()) {
                switch (respawn.spawn) {
                    case pan:
                        Entity closestSpaceStation = ECSUtil.closestEntity(Mappers.transform.get(respawnEntity).pos, spaceStations);
                        Vector2 target = Mappers.transform.get(closestSpaceStation).pos;
                        Vector2 cam = Mappers.transform.get(respawnEntity).pos.cpy().lerp(target, respawn.timeout.ratio());
                        Mappers.transform.get(respawnEntity).pos.set(cam);
                        //todo: lerp to spawn location
                        if (respawn.timeout.tryEvent()) {
                            respawn.spawn = RespawnComponent.AnimState.spawn;
                            getEngine().getSystem(CameraSystem.class).setZoomTarget((byte) 3);
                        }
                        break;
                    case spawn:
                        respawn.spawn = RespawnComponent.AnimState.end;
                        respawnEntity.add(new RemoveComponent());
                        //todo: but not just nearest. last station docked at. space station is check point
                        Entity player = spawnPlayerSpaceStation();
                        ECSUtil.transferComponent(respawnEntity, player, StatsComponent.class);
                        player.getComponent(CargoComponent.class).credits = respawn.saveCredits;//hack!
                        break;
                    //default: Gdx.app.debug(getClass().getSimpleName(), respawn.spawn.name());
                }
            } else {
                spawnPlayerWorld();
            }
        }
    }

    private Entity spawnPlayerWorld() {
        WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
        int mapSize = GameScreen.getCurrentPlanet().getComponent(PlanetComponent.class).mapSize;
        int position = mapSize * worldCFG.tileSize / 2;//set  position to middle of planet
        Entity newPlayer = EntityBuilder.createPlayer(position, position);
        getEngine().addEntity(newPlayer);
        return newPlayer;
    }

    public Entity spawnPlayerSpaceStation() {
        //if no space station found: force spawn 0, 0
        Vector2 spawnPosition = new Vector2(0, 0);

        //try to start at last docked space station: checkpoint
        //if (checkpointStation != null) {
        //    spawnPosition.set();
        //} else {

        //start at nearest space station to 0, 0
        Entity spaceStation = ECSUtil.closestEntity(spawnPosition, spaceStations);
        if (spaceStation != null) {
            spawnPosition.set(Mappers.physics.get(spaceStation).body.getPosition());
        } else {
            // no station found. ideally, this should never happen...
            Gdx.app.error(getClass().getSimpleName(), "no space station found to respawn!!!");
        }

        //spawn player
        Array<Entity> playerShip = EntityBuilder.createPlayerShip(spawnPosition.x, spawnPosition.y, true);
        Entity player = playerShip.first();
        for (Entity entity : playerShip) {
            getEngine().addEntity(entity);
        }

        //auto dock
        if (spaceStation != null) {
            Mappers.spaceStation.get(spaceStation).dockPortC = player;
            DockedComponent docked = new DockedComponent();
            docked.parent = spaceStation;
            docked.dockID = "03";
            player.add(docked);
            Mappers.controllable.get(player).activelyControlled = false;
            //todo: could force update position to mitigate camera jump when docked?
            //getEngine().getSystem(SpaceStationSystem.class).updateShipInDock();
        }

        Gdx.app.log(getClass().getSimpleName(), "player [" + DebugUtil.objString(player) + "] spawned: " + MyMath.formatVector2(spawnPosition, 1) +
                (spaceStation == null ? " NO STATION FOUND!!!" : " at station: [" + DebugUtil.objString(spaceStation) + "]"));
        return player;
    }

    public boolean pan(Entity entity) {
        RespawnComponent respawn = Mappers.respawn.get(entity);
        if ((respawn.spawn == RespawnComponent.AnimState.pause) && respawn.timeout.tryEvent()) {
            respawn.spawn = RespawnComponent.AnimState.pan;
            respawn.timeout.setInterval(500, true);
            Gdx.app.debug(getClass().getSimpleName(), "respawn pan");
            return true;
        }
        return false;
    }

}
