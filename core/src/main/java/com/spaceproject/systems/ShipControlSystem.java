package com.spaceproject.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;


public class ShipControlSystem extends IteratingSystem {
    
    private final float offsetDist = 1.5f;//TODO: dynamic based on ship size
    private final float faceRotSpeed = 8f;
    private static final float boostMultiplier = 3.0f;
    
    private final EntityConfig entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
    private ImmutableArray<Entity> planets;
    
    public ShipControlSystem() {
        super(Family.all(ControllableComponent.class, TransformComponent.class, VehicleComponent.class)
                .exclude(ScreenTransitionComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        controlShip(entity, delta);
    }
    
    private void controlShip(Entity entity, float deltaTime) {
        SoundSystem soundSys = getEngine().getSystem(SoundSystem.class);
        ControllableComponent control = Mappers.controllable.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physics = Mappers.physics.get(entity);
        
        if (GameScreen.isDebugMode) {
            applyDebugControls(entity, transform, physics);
        }
        
        //rotate ship
        if (!control.moveBack)
            faceTarget(control, physics, deltaTime);

        if (!canControlShip(entity)) {
            SoundComponent sound = Mappers.sound.get(entity);
            soundSys.shipEngineAmbient(sound, false, 0,  0,0);
            return;
        }
        
        float angle = physics.body.getAngle();
        if (control.moveBack) {
            float velocityAngle = physics.body.getLinearVelocity().angleRad();
            control.angleTargetFace = velocityAngle;
            faceTarget(control, physics, deltaTime);
            angle += 180 * MathUtils.degRad;
        } else {
            if (control.moveLeft && !control.moveRight && !control.moveForward) angle += 90 * MathUtils.degRad;
            if (control.moveRight && !control.moveLeft && !control.moveForward) angle -= 90 * MathUtils.degRad;
            if (control.moveLeft && !control.moveRight && control.moveForward)  angle += 45 * MathUtils.degRad;
            if (control.moveRight && !control.moveLeft && control.moveForward)  angle -= 45 * MathUtils.degRad;
        }
        boolean engineActive = false;
        if (control.activelyControlled
                && (control.moveForward || control.moveLeft || control.moveRight || control.moveBack || control.boost)
                && !(control.moveLeft && control.moveRight && !control.moveForward)) {
            VehicleComponent vehicle = Mappers.vehicle.get(entity); //todo: switch to per attached engine thrust
            float thrust = vehicle.thrust * control.movementMultiplier * deltaTime;
            if (control.boost) {
                thrust *= boostMultiplier;
            }
            Vector2 force = MyMath.vector(angle, thrust);
            physics.body.applyForceToCenter(force, true);
            engineActive = true;
        }
        
        if (soundSys != null) {
            float velocity = physics.body.getLinearVelocity().len();
            //float pitch = physics.body.getLinearVelocity().len2() / Box2DPhysicsSystem.getVelocityLimit2();
            //pitch = MathUtils.map(0f, 1f, 0.5f, 2.0f, pitch);
            //todo: separate active sound per jet: forward,left,right,reverse(left+right)
            //soundSys.shipEngineActive(engineActive, 1); //active noise from jets.
            SoundComponent sound = Mappers.sound.get(entity);
            float deltaAngle = physics.body.getLinearVelocity().angleRad() - angle;
            soundSys.shipEngineAmbient(sound, engineActive, velocity, deltaAngle, deltaTime);
            //soundSys.shipEngineAmbient(sound, engineActive, velocity + 0.2f);
            // 1. pitch modulation to indicate velocity
            // 2. second modulation for change of dir, g-force?
            // 3. active low noise
        }
        
        //exit vehicle
        if (control.changeVehicle) {
            exitVehicle(entity, control);
        }
        
        if (control.swapWeapon) {
            cycleTool(entity);
        }
        
        //transition or take off from planet
        if (GameScreen.inSpace()) {
            Entity planet = getPlanetNearPosition(transform.pos);
            control.canTransition = planet != null;
            if (control.interact && control.canTransition) {
                beginLandOnPlanet(entity, planet);
            }
        } else {
            control.canTransition = true;
            if (control.interact) {
                takeOffPlanet(entity);
            }
        }
    }
    
    private void faceTarget(ControllableComponent control, PhysicsComponent physicsComp, float delta) {
        float angle = MathUtils.lerpAngle(physicsComp.body.getAngle(), control.angleTargetFace, faceRotSpeed * delta);
        float impulse = MyMath.getAngularImpulse(physicsComp.body, angle, delta);
        physicsComp.body.applyAngularImpulse(impulse, true);
    }
    
    private void exitVehicle(Entity vehicleEntity, ControllableComponent control) {
        if (GameScreen.inSpace())
            return;
      
        if (!control.timerVehicle.tryEvent())
            return;
        
        control.changeVehicle = false;
        
        Entity characterEntity = Mappers.vehicle.get(vehicleEntity).driver;
        Gdx.app.log(getClass().getSimpleName(), DebugUtil.objString(characterEntity)
                + " exiting vehicle " + DebugUtil.objString(vehicleEntity));
        
        // re-create box2D body and set position near vehicle
        Vector2 vehiclePosition = Mappers.transform.get(vehicleEntity).pos;
        Vector2 playerPosition = vehiclePosition.add(MyMath.vector(MathUtils.random(360) * MathUtils.degRad, offsetDist));
        Body body = BodyBuilder.createPlayerBody(0, 0, characterEntity);
        body.setTransform(playerPosition, body.getAngle());
        body.setLinearVelocity(0, 0);
        Mappers.physics.get(characterEntity).body = body;
        
        //transfer focus and controls to character
        ECSUtil.transferControl(vehicleEntity, characterEntity);
        
        //set camera
        if (Mappers.camFocus.get(characterEntity) != null) {
            CameraSystem cameraSystem = getEngine().getSystem(CameraSystem.class);
            cameraSystem.setZoomToDefault(characterEntity);
        }
        
        // remove driver reference from vehicle
        Mappers.vehicle.get(vehicleEntity).driver = null;
        
        // add player back into world
        getEngine().addEntity(characterEntity);
    }
    
    private boolean canControlShip(Entity entity) {
        //don't allow engine activation while shield is active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            return false;
        }
        //don't allow engine activation while hypderdrive is active
        HyperDriveComponent hyperdrive = Mappers.hyper.get(entity);
        if (hyperdrive != null && hyperdrive.state == HyperDriveComponent.State.on) {
            return false;
        }
        ScreenTransitionComponent trans = Mappers.screenTrans.get(entity);
        if (trans != null) {
            return false;
        }
        return true;
    }
    
    private void applyDebugControls(Entity entity, TransformComponent transformComp, PhysicsComponent physicsComp) {
        //debug force insta-stop
        if (Gdx.input.isKeyJustPressed(Keys.X)) {
            physicsComp.body.setLinearVelocity(0, 0);
            physicsComp.body.setActive(true);
            physicsComp.body.setTransform(transformComp.pos, transformComp.rotation);
            
            HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
            if (hyperDrive != null) {
                hyperDrive.state = HyperDriveComponent.State.off;
            }
        }
        if (Gdx.input.isKeyPressed(Keys.Z)) {
            //physicsComp.body.setLinearVelocity(physicsComp.body.getLinearVelocity().add(physicsComp.body.getLinearVelocity()));
            physicsComp.body.applyLinearImpulse(MyMath.vector(physicsComp.body.getAngle(), 1000), physicsComp.body.getPosition(), true);
        }
    }
    
    private void cycleTool(Entity player) {
        VehicleComponent vehicle = Mappers.vehicle.get(player);
        if (vehicle == null || vehicle.tools.isEmpty()) return;

        if (vehicle.toolSwapTimer == null) {
            vehicle.toolSwapTimer = new SimpleTimer(300);
        }
        if (!vehicle.toolSwapTimer.tryEvent()) return;

        VehicleComponent.Tool nextTool = vehicle.currentTool;
        do {
            nextTool = nextTool.next();
            //Gdx.app.log(getClass().getSimpleName(), "searching for:" + nextTool);
        } while (!vehicle.tools.containsKey(nextTool.ordinal()));

        //unload current tool
        Component removedTool = null;
        switch (vehicle.currentTool) {
            case cannon: removedTool = player.remove(CannonComponent.class); break;
            case laser: removedTool = player.remove(LaserComponent.class); break;
            case tractor: removedTool = player.remove(TractorBeamComponent.class); break;
        }
        //store unloaded tool in inventory
        vehicle.tools.put(vehicle.currentTool.ordinal(), removedTool);

        //load new tool from inventory
        Component newTool = vehicle.tools.remove(nextTool.ordinal());
        player.add(newTool);
        vehicle.currentTool = nextTool;
        Gdx.app.log(getClass().getSimpleName(), "equipped:" + vehicle.currentTool);
    }
    
    //region transition
    private void takeOffPlanet(Entity entity) {
        if (Mappers.screenTrans.get(entity) != null)
            return;
        
        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.takeOffStage = ScreenTransitionComponent.TakeOffAnimStage.screenEffectFadeIn;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.pow2;
        entity.add(screenTrans);
        Gdx.app.log(getClass().getSimpleName(), "takeOffPlanet: " + DebugUtil.objString(entity));
    }
    
    private void beginLandOnPlanet(Entity entity, Entity planet) {
        if (Mappers.screenTrans.get(entity) != null) {
            Gdx.app.error(getClass().getSimpleName(), "transition already in progress, aborting.");
            return;
        }
        if (planet == null) {
            Gdx.app.error(getClass().getSimpleName(), "can not land on null planet.");
            return;
        }

        ScreenTransitionComponent screenTrans = new ScreenTransitionComponent();
        screenTrans.landStage = ScreenTransitionComponent.LandAnimStage.shrink;//begin animation
        screenTrans.planet = planet;
        screenTrans.timer = new SimpleTimer(entityCFG.shrinkGrowAnimTime, true);
        screenTrans.animInterpolation = Interpolation.sineIn;
        entity.add(screenTrans);
        Gdx.app.log(getClass().getSimpleName(), "beginLandOnPlanet: " + DebugUtil.objString(entity));
    }
    
    private Entity getPlanetNearPosition(Vector2 pos) {
        for (Entity planet : planets) {
            Vector2 planetPos = Mappers.transform.get(planet).pos;
            TextureComponent planetTex = Mappers.texture.get(planet);
            if (pos.dst(planetPos) <= planetTex.texture.getWidth() * 0.5 * planetTex.scale) {
                return planet;
            }
        }
        return null;
    }
    //endregion
    
}
