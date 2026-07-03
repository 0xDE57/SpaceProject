package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Queue;
import com.spaceproject.components.CamTargetComponent;
import com.spaceproject.components.CameraFocusComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class CameraSystem extends IteratingSystem {
    
    private final OrthographicCamera cam;
    
    private byte maxZoomLevel = 17;
    private byte zoomLevel = 2;
    private float zoomTarget = 1;
    private final float zoomSpeed = 2;
    private final float zoomSetThreshold = 0.001f;
    private final float minZoom = 0f;
    private final float maxZoom = 100000;
    private final byte vehicleDefault = 3;

    private boolean lerpCam = false;
    private final float smoothFollowSpeed = 10f;
    private float maxOffsetFromTarget = 5f;
    private final Vector2 offsetFromTarget = new Vector2();
    
    private final int frames = 20; //how many frames to average lerp over
    private final Queue<Vector2> averageTarget = new Queue<>();
    private final Vector3 average = new Vector3();
    
    private ImmutableArray<Entity> focalPoints;
    
    private Mode mode = Mode.lockTarget;

    enum Mode {
        free, lerpTarget, lockTarget, combat
    }

    public SimpleTimer zoomChangeTimer;

    final Vector2 cacheVec = new Vector2();

    public CameraSystem() {
        super(Family.all(CameraFocusComponent.class, TransformComponent.class).get());
        cam = MyScreenAdapter.cam;
        zoomChangeTimer = new SimpleTimer(3000);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        focalPoints = engine.getEntitiesFor(Family.all(CamTargetComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void processEntity(Entity entity, float delta) {
        cacheVec.set(Mappers.transform.get(entity).pos);
        /*
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null && !physics.body.getLinearVelocity().isZero()) {
            cacheVec.add(physics.body.getLinearVelocity().cpy().scl(0.1f));
        }*/

        if (lerpCam) {
            mode = Mode.lerpTarget;
        } else {
            mode = Mode.lockTarget;
        }
        
        switch (mode) {
            case lockTarget: lockToTarget(cacheVec); break;
            case lerpTarget: lerpToTarget(cacheVec, delta); break;
            case combat: {
                //midpoint between mouse and player to pull you away
                Vector3 targetPos = GameScreen.cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                //lockToTarget(targetPos);
                Vector2 midpoint = new Vector2(targetPos.x, targetPos.y).scl(0.5f);
                //lockToTarget(pos.cpy().add(midpoint));
            } break;
            case free: {
                //just chase mouse relative to center screen
                Vector3 targetPos = GameScreen.cam.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                //lockToTarget(targetPos);
            }
        }
        
        //clamp lerp offset so character remains on screen
        clampOffset(cacheVec, maxOffsetFromTarget);
        //if (entity near edge of screen, clamp(). or zoom out) { }

        animateZoom(delta);
        
        debugCameraControls(delta);
        
        cam.update();
    }
    
    private void lockToTarget(Vector2 pos) {
        cam.position.x = pos.x;
        cam.position.y = pos.y;
    }

    private void lerpToTarget(Vector2 pos, float delta) {
        //we want to lerp the camera for some smoothed following
        //but if we lerp directly using built in camera.position.lerp()
        //  x += alpha * (target.x - x)
        //  y += alpha * (target.y - y)
        //we are left from some jitter due to the lerp result being a ratio between self and target values.
        //the cam position would be closer to the lerped position one frame leading to a lower value
        //the next frame which in turns lags the camera and the lerp value will be higher
        //leading to an ugly back and forth jitter making cam / sprites render unstable/jumpy
        
        //so we calculate the rolling average of the lerped value over a few frames to smooth out jitter
        averageTarget.addLast(
                new Vector2(cam.position.x + (pos.x - cam.position.x) * smoothFollowSpeed * delta,
                cam.position.y + (pos.y - cam.position.y) * smoothFollowSpeed * delta));
        if (averageTarget.size > frames) {
            averageTarget.removeFirst();
        }
        average.set(0,0,0);
        for (Vector2 v : averageTarget) {
            average.add(v.x, v.y, 0);
        }
        average.scl(1.0f / averageTarget.size);
        cam.position.set(average);

        //cacheVector.set(pos.x, pos.y, 0);
        //cam.position.lerp(cacheVector, 0.9f);
    }
    
    private void clampOffset(Vector2 pos, float maxOffset) {
        //radial clamp
        offsetFromTarget.set(pos.x - cam.position.x, pos.y - cam.position.y);
        offsetFromTarget.clamp(0, maxOffset);
        cam.position.x = pos.x - offsetFromTarget.x;
        cam.position.y = pos.y - offsetFromTarget.y;
    }
    
    private void clampViewport() {
        /*
        //todo: always keep player within viewport
        int padding = 0;
        Rectangle focalWindow = new Rectangle(padding, padding,
                Gdx.graphics.getWidth()-padding, Gdx.graphics.getHeight()-padding);
        Vector3 playerScreenPos = cam.project(new Vector3(pos, 0));
        int edgeTop = Gdx.graphics.getWidth();
        int edgeBottom = 0;
        int edgeLeft = 0;
        int edgeRight = Gdx.graphics.getHeight();
        float distLeft = playerScreenPos.x - edgeLeft;
        float distRight = playerScreenPos.x - edgeRight;
        float distTop = playerScreenPos.y - edgeTop;
        float distBottom = playerScreenPos.y - edgeBottom;
        
        float closestEdge = distLeft;
        if (Math.abs(closestEdge) > Math.abs(distRight)) {
            closestEdge = distRight;
        }
        if (Math.abs(closestEdge) > Math.abs(distTop)) {
            closestEdge = distTop;
        }
        if (Math.abs(closestEdge) > Math.abs(distBottom)) {
            closestEdge = distBottom;
        }
        
        if (playerScreenPos.x < focalWindow.x) {
            cam.position.x += /*pos.x -* (playerScreenPos.x - focalWindow.x);//pos.x - (playerScreenPos.x - edgeLeft);
        }
        if (playerScreenPos.y < focalWindow.y) {
            //cam.position.x = pos.y - focalWindow.y;
        }
        if (playerScreenPos.x > focalWindow.width) {
            //cam.position.x = pos.x - focalWindow.width;
        }
        if (playerScreenPos.y > focalWindow.height) {
            //cam.position.y = pos.y - focalWindow.height;
        }
        
        
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)) {
            //Gdx.app.debug(this.getClass().getSimpleName(),
            //        "x: " + playerScreenPos.x + " y: " + playerScreenPos.y);// +
                            //" l: " + distLeft + " r: " +
                           // distRight + " t: " + distTop + " b: " + distBottom);
            Gdx.app.debug(this.getClass().getSimpleName(),
                    "x: " + playerScreenPos.x + " y: " + playerScreenPos.y +
                            " entity out of window: " + focalWindow.toString());
            
            //cameraFocus.zoomTarget += 2f * delta;
        }
    */
    }

    public boolean isLerp() {
        return lerpCam;
    }

    public void setLerp(boolean lerp) {
        lerpCam = lerp;
    }
    
    private void focusCombatCamera(Vector2 playerPosition, float delta) {
        Entity targetEntity = focalPoints.first();
        //Gdx.app.log(this.getClass().getSimpleName(), "Combat Focus Cam on: " + Misc.objString(targetEntity));
        //first for now, todo:
        //should be closest not first,
        //actually...should be furthest entity within threshold?
        //eg: if a 3rd or 4rth is within threshold include it in cluster
        //
        
        Vector2 targetPos = Mappers.transform.get(targetEntity).pos;
        Vector2 midpoint = playerPosition.cpy().add(targetPos).scl(0.5f);
        
        //set camera to focal point between targets, lock once acquired.
        lockToTarget(midpoint);
        /*
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)){
            adjust cam position until player is in
        }
        
        lerpToTarget(midpoint, delta);
        if (cam.position.epsilonEquals(midpoint.x, midpoint.y, 0)) {
            lockToTarget(midpoint);
        }
        }*/
        
        
        //zoom in or out to include focused entities
        //zoom in as close as possible at all times, until entities are near edge of camera
        //threshold of how far apart before ignore, don't lock onto midpoint
        //camera edge points?
        //Vector3 topLeft = cam.unproject(new Vector3(0, 0, 0));
        //Vector3 bottomRight = cam.unproject(new Vector3(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0));
        //Rectangle focalWindow = new Rectangle(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
        
        int padding = 32;
        float percent = 0.7f;
        Rectangle focalWindow = new Rectangle(padding, padding,
                Gdx.graphics.getWidth()-padding, Gdx.graphics.getHeight()-padding);
        Vector3 playerScreenPos = cam.project(new Vector3(playerPosition, 0));
        Vector3 targetScreenPos = cam.project(new Vector3(targetPos, 0));
        
        //zoom out if player or target is outside of camera view
        if (!focalWindow.contains(playerScreenPos.x, playerScreenPos.y)
                || !focalWindow.contains(targetScreenPos.x, targetScreenPos.y)) {
            Gdx.app.debug(this.getClass().getSimpleName(), "entity out of window: " + focalWindow.toString());
            
            zoomTarget += 1.5f * delta;
            //zoomOut();
        } else {
            if (cam.zoom > 1) {
                Rectangle threshHoldWindow = new Rectangle(0, 0, focalWindow.width * percent, focalWindow.height * percent);
                Vector2 center = new Vector2();
                focalWindow.getCenter(center);
                threshHoldWindow.setCenter(center);
                
                if (threshHoldWindow.contains(playerScreenPos.x, playerScreenPos.y) || threshHoldWindow.contains(targetScreenPos.x, targetScreenPos.y)) {
                    Gdx.app.debug(this.getClass().getSimpleName(), "inner window: " + threshHoldWindow.toString());
                    
                    //zoom in if target and player are closer together relative to camera
                    zoomTarget -= 1.5f * delta;
                    // zoomIn();
                }
            }
        }
    }
    
    //region zoom
    public void setZoomZero() {
        setZoomTarget((byte) -1);
        //Gdx.app.debug(this.getClass().getSimpleName(), "zoom0: " + zoomTarget + " : " + zoomLevel);
    }

    public void setZoomTarget(byte level) {
        zoomLevel = level;
        zoomTarget = getZoomForLevel(zoomLevel);
        zoomChangeTimer.reset();
    }

    public float setZoomToDefault(Entity entity) {
        if (entity != null && Mappers.vehicle.get(entity) != null) {
            setZoomTarget(vehicleDefault);
        } else {
            setZoomTarget((byte) 1);
        }
        //Gdx.app.debug(this.getClass().getSimpleName(), "default zoom: " + zoomTarget + " : " + zoomLevel);
        return zoomTarget;
    }
    
    public void zoomIn() {
        if (GameScreen.isPaused()) return;
        if (zoomLevel <= 0) return;
        setZoomTarget(--zoomLevel);
        //Gdx.app.debug(this.getClass().getSimpleName(), "zoomIn: " + zoomTarget + " : " + zoomLevel);
    }
    
    public void zoomOut() {
        if (GameScreen.isPaused()) return;
        if (zoomLevel >= maxZoomLevel) return;
        setZoomTarget(++zoomLevel);
        //Gdx.app.debug(this.getClass().getSimpleName(), "zoomOut: " + zoomTarget + " : " + zoomLevel);
    }
    
    public void zoomOutMax() {
        zoomLevel = (byte) (maxZoomLevel-1);
        zoomOut();
    }
    
    public byte getMaxZoomLevel() {
        return maxZoomLevel;
    }

    public boolean isMaxZoomLevel() {
        return getZoomLevel() == getMaxZoomLevel();
    }
    
    /** iter: -1,    0,   1, 2, 3, 4, 5, 6,  7, 8...
     *  fib:    ,    0,   1, 1, 2, 3, 5, 8, 13, 21..
     *  out:   0, 0.25, 0.5, 1, 2, 3, 5, 8, 13, 21.. */
    public static float getZoomForLevel(byte level) {
        switch (level) {
            case -1: return 0;
            case 0: return 0.25f;
            case 1: return 0.5f; //default character zoom
            case 2: return 1.0f; //default vehicle zoom
            default: return MyMath.fibonacci(level);
        }
    }
    
    public byte getZoomLevel() {
        return zoomLevel;
    }
    
    private void animateZoom(float delta) {
        cam.zoom = MathUtils.lerp(cam.zoom, zoomTarget, zoomSpeed * delta);
        
        //if zoom is close enough, just set it to target
        if (Math.abs(cam.zoom - zoomTarget) < zoomSetThreshold) {
            cam.zoom = zoomTarget;
        }
        
        cam.zoom = MathUtils.clamp(cam.zoom, minZoom, maxZoom);
    }
    
    public void impact(Entity entity) {
        if (getZoomLevel() > 10) {
            setZoomToDefault(entity);
        } else if (getZoomLevel() > 5) {
            zoomIn();
        }
    }

    public void autoZoom(Entity player) {
        if (getZoomLevel() == vehicleDefault) {
            zoomOutMax();
        } else {
            setZoomToDefault(player);
        }
    }
    //endregion
    
    private void debugCameraControls(float delta) {
        float angle = 5f * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            cam.rotate(angle);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            cam.rotate(-angle);
        }
    }
    
}
