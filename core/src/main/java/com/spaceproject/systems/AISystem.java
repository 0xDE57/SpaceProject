package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.components.AIComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.CharacterComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.VehicleComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.Steering;

public class AISystem extends IteratingSystem {
    
    private ImmutableArray<Entity> vehicles;
    private ImmutableArray<Entity> planets;
    
    public AISystem() {
        super(Family.all(AIComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        vehicles = engine.getEntitiesFor(Family.all(VehicleComponent.class).get());
        planets = engine.getEntitiesFor(Family.all(PlanetComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        /* simple DUMB test "AI" for now just to create structure/skeleton and test some basic behaviors */
        
        AIComponent ai = Mappers.AI.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        Vector2 aiPos = Mappers.transform.get(entity).pos;
        
        if (ai.state == null) {
            ai.state = AIComponent.State.wander;
            Gdx.app.debug(getClass().getSimpleName(), "WARNING: null state. adjusting to default state: " + ai.state.name());
        }
        
        switch (ai.state) {
            case attack:
                if (ai.attackTarget != null || (getEngine().getEntities().contains(ai.attackTarget, false))) {
                    VehicleComponent vehicle = Mappers.vehicle.get(entity);
                    if (vehicle == null) {
                        //find a vehicle if on foot
                        seekAndEnterVehicle(entity, control, aiPos);
                    } else {
                        //seek and attack
                        Vector2 targetPos = Mappers.transform.get(ai.attackTarget).pos;
                        dumbSeek(control, aiPos, targetPos, 0.3f);//todo: distance based. threshold radius
                        control.attack = !control.attack;
                    }
                } else {
                    ai.state = AIComponent.State.wander;
                    Gdx.app.debug(getClass().getSimpleName(), "attack target null or entity not found. setting state: " + ai.state.name());
                }
                break;
            case wander:
                control.attack = false;
                
                //dumb wander
                control.angleTargetFace += 1 * delta;
                control.moveForward = true;
                control.movementMultiplier = 0.1f;
                
                break;
            case landOnPlanet: {
                if (Mappers.screenTrans.get(entity) != null) {
                    ai.state = AIComponent.State.idle;
                    control.interact = false;
                    Gdx.app.debug(getClass().getSimpleName(), DebugUtil.objString(ai) + "AI landing. setting state: " + ai.state.name());
                    return;
                }
                
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle == null) {
                    //todo: debug test behavior. normally character would not be without a ship in space
                    seekAndEnterVehicle(entity, control, aiPos);
                } else {
                    if (ai.planetTarget == null) {
                        ai.planetTarget = ECSUtil.closestEntity(aiPos, planets);
                        Gdx.app.debug(getClass().getSimpleName(),
                                "WARNING: no landing target planet for AI, defaulting nearest planet: " + DebugUtil.objString(ai.planetTarget));
                    }
                    
                    //arrive behavior
                    Vector2 targetPos = Mappers.transform.get(ai.planetTarget).pos;
                    Body body = Mappers.physics.get(entity).body;
                    arrive(body, ai.steering, targetPos);
                    applySteering(body, ai.steering, delta);
                    //control.angleTargetFace = MyMath.angleTo(targetPos, aiPos);
                }
                break;
            }
            case idle:
                //do nothing
				/*
				control.moveForward = false;
				control.moveBack = false;
				control.transition = false;
				control.changeVehicle = false;
				control.shoot = false;
				*/
                break;
            case follow: {
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle != null) {
                    // follow player
                    Entity player = getEngine().getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get()).first();
                    Vector2 pPos = Mappers.transform.get(player).pos;
                    dumbSeek(control, aiPos, pPos, 0.5f);
                }
                
                CharacterComponent character = Mappers.character.get(entity);
                if (character != null) {
                    seekAndEnterVehicle(entity, control, aiPos);
                }
                break;
            }
            case takeOffPlanet: {
                VehicleComponent vehicle = Mappers.vehicle.get(entity);
                if (vehicle == null) {
                    seekAndEnterVehicle(entity, control, aiPos);
                } else {
                    control.interact = true;
                    ai.state = AIComponent.State.idle;
                }
                break;
            }
        }
    }
    
    protected void applySteering (Body body, Steering steering, float deltaTime) {
        boolean anyAccelerations = false;
        
        // Update position and linear velocity.
        if (!steering.linearVelocity.isZero()) {
            // this method internally scales the force by deltaTime
            body.applyForceToCenter(steering.linearVelocity, true);
            anyAccelerations = true;
        }
   
        // If we haven't got any velocity, then we can do nothing.
        Vector2 linVel = body.getLinearVelocity();
        float zeroLinearSpeedThreshold = 0.001f;
        if (!linVel.isZero(zeroLinearSpeedThreshold)) {
            float newOrientation = linVel.angleRad();
            body.setAngularVelocity((newOrientation - body.getAngularVelocity()) * deltaTime); // this is superfluous if independentFacing is always true
            body.setTransform(body.getPosition(), newOrientation);
        }
        
        if (anyAccelerations) {
            // body.activate();
            
            // TODO:
            // Looks like truncating speeds here after applying forces doesn't work as expected.
            // We should likely cap speeds form inside an InternalTickCallback, see
            // http://www.bulletphysics.org/mediawiki-1.5.8/index.php/Simulation_Tick_Callbacks
            
            // Cap the linear speed
            Vector2 velocity = body.getLinearVelocity();
            float currentSpeedSquare = velocity.len2();
            float maxLinearSpeed = Box2DPhysicsSystem.getVelocityLimit2();
            if (currentSpeedSquare > maxLinearSpeed) {
                body.setLinearVelocity(velocity.scl(maxLinearSpeed / (float)Math.sqrt(currentSpeedSquare)));
            }
            
        }
    }
    
    private void arrive(Body body, Steering steering, Vector2 targetPos) {
        float arrivalRadius = 50f; //radius for arriving at the target
        float slowRadius = 500f; //slow down when within this radius of target
        float timeToTarget = 0.1f; //time over which to achieve target speed
        float maxAcceleration = 200f;//aiVehicle.thrust?
        
        Vector2 aiPos = body.getPosition();
        Vector2 direction = steering.linearVelocity.set(targetPos).sub(aiPos);
        float distanceToTarget = direction.len();
        float targetSpeed = Box2DPhysicsSystem.getVelocityLimit();
        
        if (distanceToTarget <= arrivalRadius) {
            //we have arrived at target
            steering.linearVelocity.setZero();
            steering.angularVelocity = 0;
            //control.moveForward = false;
            //control.moveBack = false;
            return;
        }
        
        if (distanceToTarget <= slowRadius) {
            //scale velocity based on distance
            targetSpeed *= distanceToTarget / slowRadius;
        }
        
        steering.linearVelocity = direction.scl(targetSpeed / distanceToTarget);
        steering.linearVelocity.sub(body.getLinearVelocity()).scl(1f / timeToTarget).limit(maxAcceleration);
        
        steering.angularVelocity = 0;
        
        /*
        if (currentVelocity > velocityTarget) {
            control.moveForward = false;
            control.moveBack = true;
            control.boost = true;
        } else {
            //full speed ahead
            control.movementMultiplier = 1f;
            control.moveForward = true;
            control.boost = true;
        }*/
        
    }
    
    private void dumbSeek(ControllableComponent control, Vector2 aiPos, Vector2 targetPos, float multiplier) {
        control.angleTargetFace = MyMath.angleTo(aiPos,targetPos);
        control.moveForward = true;
        control.movementMultiplier = multiplier;
    }
    
    private void seekAndEnterVehicle(Entity entity,  ControllableComponent control, Vector2 aiPos) {
        Entity closestVehicle = ECSUtil.closestEntity(aiPos, vehicles);
        if (closestVehicle != null) {
            //seek
            dumbSeek(control, aiPos, Mappers.transform.get(closestVehicle).pos, 1f);
            
            PhysicsComponent aiPhysics = Mappers.physics.get(entity);
            for (Entity v : vehicles) {
                //skip vehicle is occupied
                if (Mappers.vehicle.get(v).driver != null) continue;
                
                //check if character is near a vehicle
                PhysicsComponent vehiclePhysics = Mappers.physics.get(v);
                if (aiPhysics.body.getPosition().dst(vehiclePhysics.body.getPosition()) < 5) {
                    control.changeVehicle = true;
                }
            }
        }
    }
    
}
