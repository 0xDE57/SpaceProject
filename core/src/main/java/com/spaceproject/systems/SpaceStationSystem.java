package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Transform;
import com.spaceproject.components.*;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.Mappers;

public class SpaceStationSystem extends IteratingSystem {

    private final ShapeRenderer shape;
    private final Vector2 tempVec = new Vector2();
    private final long inventorySellTimer = 400;
    private float animate = 0;

    private ImmutableArray<Entity> players;
    Body body = null;

    public SpaceStationSystem() {
        super(Family.all(SpaceStationComponent.class, TransformComponent.class).get());
        shape = new ShapeRenderer();
    }


    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).exclude(DockedComponent.class).get());
        super.addedToEngine(engine);
    }

    @Override
    public void update(float deltaTime) {
        if (players != null && players.size() > 0) {
            body = Mappers.physics.get(players.first()).body;
        } else {
            body = null;
        }
        animate += 5 * deltaTime;
        shape.setProjectionMatrix(GameScreen.cam.combined);
        super.update(deltaTime);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        //keep station in orbit around parent body, don't fling out into universe...
        SpaceStationComponent spaceStation = Mappers.spaceStation.get(entity);
        PhysicsComponent stationPhysics = Mappers.physics.get(entity);
        if (spaceStation.parentOrbitBody != null) {
            TransformComponent parentTransform = Mappers.transform.get(spaceStation.parentOrbitBody);
            //set velocity perpendicular to parent body, (simplified 2-body model)
            float angle = MyMath.angleTo(stationPhysics.body.getPosition(), parentTransform.pos);
            stationPhysics.body.setLinearVelocity(MyMath.vector(angle + (spaceStation.velocity > 0 ? -MathUtils.HALF_PI : MathUtils.HALF_PI), spaceStation.velocity));
            Vector2 pos = stationPhysics.body.getPosition();
            stationPhysics.body.setTransform(pos.x, pos.y, angle);
        }

        //update docked ships
        if (spaceStation.dockPortA != null) {
            updateShipInDock(entity, spaceStation, stationPhysics, spaceStation.dockPortA, BodyBuilder.DOCK_A_ID, deltaTime);
        }
        if (spaceStation.dockPortB != null) {
            updateShipInDock(entity, spaceStation, stationPhysics, spaceStation.dockPortB, BodyBuilder.DOCK_B_ID, deltaTime);
        }
        if (spaceStation.dockPortC != null) {
            updateShipInDock(entity, spaceStation, stationPhysics, spaceStation.dockPortC, BodyBuilder.DOCK_C_ID, deltaTime);
        }
        if (spaceStation.dockPortD != null) {
            updateShipInDock(entity, spaceStation, stationPhysics, spaceStation.dockPortD, BodyBuilder.DOCK_D_ID, deltaTime);
        }

        //draw docking pad
        shape.begin(ShapeRenderer.ShapeType.Filled);
        Transform transform = stationPhysics.body.getTransform();
        for (Fixture fixture : stationPhysics.body.getFixtureList()) {
            if (fixture.getUserData() == null) continue;

            shape.setColor(Color.BLACK);
            int dockID = (int) fixture.getUserData();
            if (spaceStation.dockPortA != null && dockID == BodyBuilder.DOCK_A_ID) {
                shape.setColor(Color.GRAY);
            }
            if (spaceStation.dockPortB != null && dockID == BodyBuilder.DOCK_B_ID) {
                shape.setColor(Color.GRAY);
            }
            if (spaceStation.dockPortC != null && dockID == BodyBuilder.DOCK_C_ID) {
                shape.setColor(Color.GRAY);
            }
            if (spaceStation.dockPortD != null && dockID == BodyBuilder.DOCK_D_ID) {
                shape.setColor(Color.GRAY);
            }

            CircleShape dock = (CircleShape) fixture.getShape();
            tempVec.set(dock.getPosition());
            transform.mul(tempVec);
            shape.circle(tempVec.x, tempVec.y, dock.getRadius());
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Fixture fixture : stationPhysics.body.getFixtureList()) {
            if (fixture.getUserData() == null) continue;

            shape.setColor(Color.WHITE);
            int dockID = (int) fixture.getUserData();
            if (spaceStation.dockPortA != null && dockID == BodyBuilder.DOCK_A_ID) {
                shape.setColor(Color.GREEN);
            }
            if (spaceStation.dockPortB != null && dockID == BodyBuilder.DOCK_B_ID) {
                shape.setColor(Color.GREEN);
            }
            if (spaceStation.dockPortC != null && dockID == BodyBuilder.DOCK_C_ID) {
                shape.setColor(Color.GREEN);
            }
            if (spaceStation.dockPortD != null && dockID == BodyBuilder.DOCK_D_ID) {
                shape.setColor(Color.GREEN);
            }

            CircleShape dock = (CircleShape) fixture.getShape();
            tempVec.set(dock.getPosition());
            transform.mul(tempVec);

            //highlight landing pad when player velocity vector intersect with dock
            if (body != null && !body.getLinearVelocity().isZero()) {
                Vector2 facing = MyMath.vector(body.getLinearVelocity().angleRad(), 500000).add(body.getPosition());
                boolean intersects = Intersector.intersectSegmentCircle(body.getPosition(), facing, tempVec, dock.getRadius() * dock.getRadius());
                if (intersects) {
                    float g = (float) Math.abs(Math.sin(animate));
                    shape.setColor(0, g, 0, 1);
                }
            }

            shape.circle(tempVec.x, tempVec.y, dock.getRadius());
        }
        shape.end();
    }

    private void updateShipInDock(Entity stationEntity, SpaceStationComponent spaceStation, PhysicsComponent stationPhysics, Entity dockedShip, int dockId, float deltaTime) {
        PhysicsComponent shipPhysics = Mappers.physics.get(dockedShip);
        if (shipPhysics.body == null) return;
        Transform transform = stationPhysics.body.getTransform();

        //update ship position relative to space station
        for (Fixture fixture : stationPhysics.body.getFixtureList()) {
            if (fixture.getUserData() == null) continue;
            
            if ((int)fixture.getUserData() == dockId) {
                CircleShape dock = (CircleShape) fixture.getShape();
                tempVec.set(dock.getPosition());
                transform.mul(tempVec);
                shipPhysics.body.setTransform(tempVec, shipPhysics.body.getAngle());
                shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());

                heal(Mappers.health.get(dockedShip), deltaTime);
                sellCargo(dockedShip, shipPhysics.body.getPosition());
            }
        }

        //undock
        ControllableComponent control = Mappers.controllable.get(dockedShip);
        if (spaceStation.lastDockedTimer.canDoEvent() && (control.moveForward || control.moveRight || control.moveLeft || control.boost)) {
            undock(stationEntity, spaceStation, stationPhysics, dockedShip, shipPhysics, control);
        }
    }

    public void dock(Fixture shipFixture, Fixture dockFixture, Entity vehicleEntity, Entity stationEntity) {
        if (shipFixture.getUserData() != null && (int)shipFixture.getUserData() != BodyBuilder.SHIP_FIXTURE_ID) {
            return;
        }
        if (dockFixture.getUserData() == null) {
            return;
        }
        ShieldComponent shield = Mappers.shield.get(vehicleEntity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            return; //ignore shielded entities
        }
        HyperDriveComponent hyperdrive = Mappers.hyper.get(vehicleEntity);
        if (hyperdrive != null && hyperdrive.state == HyperDriveComponent.State.charging) {
            //cancel charge
            hyperdrive.chargeTimer.reset();
            hyperdrive.state = HyperDriveComponent.State.off;
        }

        SpaceStationComponent station = Mappers.spaceStation.get(stationEntity);
        String dockedPort = "";
        switch ((int) dockFixture.getUserData()) {
            case BodyBuilder.DOCK_A_ID:
                if (station.dockPortA != null) return;//port is in use
                station.dockPortA = vehicleEntity;
                dockedPort = "01";
                break;
            case BodyBuilder.DOCK_B_ID:
                if (station.dockPortB != null) return;
                station.dockPortB = vehicleEntity;
                dockedPort = "02";
                break;
            case BodyBuilder.DOCK_C_ID:
                if (station.dockPortC != null) return;
                station.dockPortC = vehicleEntity;
                dockedPort = "03";
                break;
            case BodyBuilder.DOCK_D_ID:
                if (station.dockPortD != null) return;
                station.dockPortD = vehicleEntity;
                dockedPort = "04";
                break;
        }
        station.lastDockedTimer.reset();
        DockedComponent docked = new DockedComponent();
        docked.parent = stationEntity;
        docked.dockID = dockedPort;
        vehicleEntity.add(docked);

        Mappers.physics.get(vehicleEntity).body.setLinearVelocity(0, 0);
        Mappers.controllable.get(vehicleEntity).activelyControlled = false;
        Mappers.cargo.get(vehicleEntity).lastCollectTime = GameScreen.getGameTimeCurrent();//show inventory

        getEngine().getSystem(CameraSystem.class).impact(vehicleEntity);
        getEngine().getSystem(SoundSystem.class).dockStation();
        Gdx.app.debug(getClass().getSimpleName(), "dock [" + DebugUtil.objString(vehicleEntity) + "] at station: [" + DebugUtil.objString(stationEntity) + "] port: " + dockedPort);
    }

    private void undock(Entity stationEntity, SpaceStationComponent spaceStation, PhysicsComponent stationPhysics, Entity dockedShip, PhysicsComponent shipPhysics, ControllableComponent control) {
        String port = "";
        if (spaceStation.dockPortA == dockedShip) {
            spaceStation.dockPortA = null;
            port = "01";
        }
        if (spaceStation.dockPortB == dockedShip) {
            spaceStation.dockPortB = null;
            port = "02";
        }
        if (spaceStation.dockPortC == dockedShip) {
            spaceStation.dockPortC = null;
            port = "03";
        }
        if (spaceStation.dockPortD == dockedShip) {
            spaceStation.dockPortD = null;
            port = "04";
        }
        control.activelyControlled = true;
        shipPhysics.body.setLinearVelocity(stationPhysics.body.getLinearVelocity());
        Mappers.cargo.get(dockedShip).lastCollectTime = GameScreen.getGameTimeCurrent();//show inventory
        dockedShip.remove(DockedComponent.class);

        getEngine().getSystem(SoundSystem.class).undockStation();
        Gdx.app.debug(getClass().getSimpleName(), "undock [" + DebugUtil.objString(dockedShip) + "] from station: [" + DebugUtil.objString(stationEntity) + "] port: " + port);
    }

    private void sellCargo(Entity collector, Vector2 pos) {
        CargoComponent cargo = Mappers.cargo.get(collector);
        if (cargo == null) return;

        StatsComponent stats = Mappers.stat.get(collector);
        for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
            int id = resource.getId();
            if (!cargo.inventory.containsKey(id)) continue;

            if (GameScreen.getGameTimeCurrent() - cargo.lastCollectTime < inventorySellTimer) return; //animation

            /*todo: base local value on rarity of resource in that local system.
               eg: a system red is common so value local value will be less,
               or a system where gold is more rare than usual  so value will be more
            float scarcity = 1.0f;
            float localValue = scarcity * resource.getValue();
            int credits = quantity * localValue;
            */
            int quantity = cargo.inventory.get(id);
            int credits = quantity * resource.getValue();
            float pitch = 0.5f + ((((float) id / cargo.inventory.size()) + 1) * 0.12f);
            getEngine().getSystem(SoundSystem.class).addCredits(pitch);
            cargo.credits += credits;
            cargo.inventory.remove(id);
            cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
            if (stats != null) {
                stats.profit += credits;
            }
            getEngine().getSystem(HUDSystem.class).addCredits(credits, pos, resource.getColor());
            Gdx.app.debug(getClass().getSimpleName(), "+" + credits + "c. sold " + quantity + " " + resource.name() + " @"+ resource.getValue() + "c");
        }
    }

    private void heal(HealthComponent health, float deltaTime) {
        if (health == null) return;
        if (health.maxHealth == health.health) return;

        float healRate = 30;
        health.health += healRate * deltaTime;
        /* todo: add repair sound to signal to player that ship is fully repaired
        if (health.maxHealth == health.health) {
            getEngine().getSystem(SoundSystem.class).heal();
        }*/
        health.health = Math.min(health.health, health.maxHealth);
    }

}
