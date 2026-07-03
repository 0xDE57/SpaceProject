package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.components.OrbitComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.math.MyMath;

public class OrbitSystem extends IteratingSystem {
    
    private final int syncPosThreshold = 10;//todo, move to config
    private static final Vector2 TEMP_VEC = new Vector2();
    
    public OrbitSystem() {
        super(Family.all(OrbitComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        resetProcessedFlag();
    }
    
    @Override
    protected void processEntity(Entity entity, float delta) {
        updateBody(entity, delta);
    }
    
    private void updateBody(Entity entity, float delta) {
        OrbitComponent orbit = Mappers.orbit.get(entity);
        if (orbit == null || orbit.isProcessed) {
            return;
        }
        
        TransformComponent position = Mappers.transform.get(entity);
        //TODO: time sync planet rotation/spin just like orbit
        position.rotation += orbit.rotateClockwise ? orbit.rotSpeed * delta : -orbit.rotSpeed * delta;
    
        //apply tangential velocity
        orbit.angle = getTimeSyncedAngle(orbit, GameScreen.getGameTimeCurrent());
        orbit.velocity.set(orbit.tangentialSpeed, 0).rotateRad(orbit.angle).rotate90(orbit.rotateClockwise ? 1 : -1);
        
        if (orbit.parent != null) {
            updateBody(orbit.parent, delta);
            
            syncOrbit(orbit, position);
    
            OrbitComponent parentOrbit = Mappers.orbit.get(orbit.parent);
            if (parentOrbit != null) {
                orbit.velocity.add(parentOrbit.velocity);
            }
        }
        
        //add velocity to position
        TEMP_VEC.set(orbit.velocity).scl(delta);
        position.pos.add(TEMP_VEC.x, TEMP_VEC.y);
    
        orbit.isProcessed = true;
    }
    
    private void syncOrbit(OrbitComponent orbitComp, TransformComponent position) {
        //calculate exact orbit position, ensure object is not too far from synced location
        Vector2 orbitPos = getTimeSyncedPos(orbitComp, GameScreen.getGameTimeCurrent());
        if (!position.pos.epsilonEquals(orbitPos, syncPosThreshold)) {
            position.pos.set(orbitPos);
        }
    }
    
    public static Vector2 getTimeSyncedPos(OrbitComponent orbitComp, long time) {
        TransformComponent parentPosition = Mappers.transform.get(orbitComp.parent);
        return MyMath.vector(getTimeSyncedAngle(orbitComp, time), orbitComp.radialDistance).add(parentPosition.pos);
    }
    
    public static float getTimeSyncedAngle(OrbitComponent orbit, long gameTime) {
        //calculate time-synced angle, dictate position as a function of time based on tangential velocity
        float angularSpeed = orbit.tangentialSpeed / orbit.radialDistance;
        long msPerRevolution = (long) (1000 * MathUtils.PI2 / angularSpeed);
        float timeSyncAngle = 0;
        if (msPerRevolution != 0) {
            timeSyncAngle = MathUtils.PI2 * ((float) (gameTime % msPerRevolution) / (float) msPerRevolution);
        }
        
        timeSyncAngle = orbit.rotateClockwise ? timeSyncAngle : -timeSyncAngle;
        
        //keep angle relative to starting position
        timeSyncAngle += orbit.startAngle;
        
        //keep angle within 0 to 2PI radians
        if (timeSyncAngle > MathUtils.PI2) {
            timeSyncAngle -= MathUtils.PI2;
        } else if (timeSyncAngle < 0) {
            timeSyncAngle += MathUtils.PI2;
        }
        
        return timeSyncAngle;
    }
    
    private void resetProcessedFlag() {
        for (int i = 0; i < getEntities().size(); ++i) {
            OrbitComponent orbit = Mappers.orbit.get(getEntities().get(i));
            orbit.isProcessed = false;
        }
    }
    
}
