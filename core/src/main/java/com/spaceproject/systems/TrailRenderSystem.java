package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HealthComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TrailComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import java.util.Comparator;

public class TrailRenderSystem extends SortedIteratingSystem implements EntityListener, Disposable {
    
    private static class ZComparator implements Comparator<Entity> {
        @Override
        public int compare(Entity entityA, Entity entityB) {
            return (int)Math.signum(Mappers.trail.get(entityA).zOrder - Mappers.trail.get(entityB).zOrder);
        }
    }
    
    private final ShapeRenderer shape;
    private final Color tmpColor = new Color();
    private int maxPathSize = 40;
    private float alpha = 1;
    private float animation;
    private float animSpeed = 2f;

    public TrailRenderSystem() {
        super(Family.all(TrailComponent.class, TransformComponent.class).get(), new ZComparator());
        shape = new ShapeRenderer();
    }

    @Override
    public void entityAdded(Entity entity) {
        super.entityAdded(entity);
        TrailComponent trail = Mappers.trail.get(entity);

        //initialize
        if (trail.path == null) {
            //todo: cache. use pooled vectors
            trail.path = new Vector3[maxPathSize];
            for (int v = 0; v < maxPathSize; v++) {
                trail.path[v] = new Vector3();
            }
            trail.state = new byte[maxPathSize];
        }
        if (trail.style == null) {
            trail.style = TrailComponent.Style.norender;
        }
        if (trail.updateTimer == null) {
            trail.updateTimer = new SimpleTimer(10);
            trail.updateTimer.setCanDoEvent();
        }
    }

    @Override
    public void update(float deltaTime) {
        //warning: system coupling -> todo: use signals?
        HUDSystem hudSystem = getEngine().getSystem(HUDSystem.class);
        if (hudSystem != null && !hudSystem.isDraw()) {
            return;
        }
    
        alpha = MathUtils.clamp((GameScreen.cam.zoom / 100), 0, 1);
        if (MathUtils.isEqual(alpha, 0)) return;
        
        animation += animSpeed * deltaTime;
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        shape.setProjectionMatrix(GameScreen.cam.combined);
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        super.update(deltaTime);
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TrailComponent trail = Mappers.trail.get(entity);

        updateTail(trail, entity);

        switch (trail.style) {
            case velocity: renderVelocityPath(trail); break;
            case state: renderStatePath(trail); break;
            case solid: renderDefaultPath(trail); break;
        }
    }
    
    // these are more like paths than splines, but see also:
    // https://libgdx.com/wiki/math-utils/path-interface-and-splines
    private void updateTail(TrailComponent trail, Entity entity) {
        if (!trail.updateTimer.tryEvent()) {
            return;
        }

        TransformComponent transform = Mappers.transform.get(entity);
        PhysicsComponent physics = Mappers.physics.get(entity);

        //todo: eliminate these magic numbers
        //off, on, boost, hyper -> 0, 1, 2, 3
        float velocity = 0;
        byte state = 0;
        if (trail.style == TrailComponent.Style.state) {
            if (physics != null) {
                //set velocity
                velocity = physics.body.getLinearVelocity().len2();
        
                if (trail.style == TrailComponent.Style.state) {
                    //set state
                    ControllableComponent control = Mappers.controllable.get(entity);
                    if (control != null) {
                        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
                            state = 1;
                            if (control.boost) {
                                state = 2;
                            }
                        }
                        if (!control.activelyControlled) {
                            state = -4;
                        }
                    }
            
                    ShieldComponent shield = Mappers.shield.get(entity);
                    if (shield != null) {
                        if (shield.state == ShieldComponent.State.on) {
                            state = -2;
                        } else {
                            //charge discharge
                            //in out
                            //state =- 4;?
                        }
                
                        if ((GameScreen.getGameTimeCurrent() - shield.lastHit) < 500) {
                            state = -3;
                        }
                
                    }
            
                    if (!physics.body.isActive()) {
                        state = 3;
                
                        HyperDriveComponent hyper = Mappers.hyper.get(entity);
                        velocity = hyper.speed * hyper.speed;
                    }
                }
            }

            //show hurt
            HealthComponent health = Mappers.health.get(entity);
            if (health != null) {
                long hurtTime = 1000;
                if ((GameScreen.getGameTimeCurrent() - health.lastHitTime) < hurtTime) {
                    state = -1;
                }
            }
        }
        
        trail.path[trail.indexHead].set(transform.pos.x, transform.pos.y, velocity);
        if (trail.style == TrailComponent.Style.state) {
            trail.state[trail.indexHead] = state;
        }
        
        //roll index
        trail.stepCount++;
        trail.indexHead++;
        if (trail.indexHead >= trail.path.length) {
            trail.indexHead = 0;
        }
    }
    
    public void renderVelocityPath(TrailComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
    
        //set default color
        if (spline.color == null) {
            spline.color = Color.MAGENTA;
        }
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = (i + 1) % spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                //z = linearVelocity [0 - max box2d] then hyperdrive velocity
                float velocity = p.z / Box2DPhysicsSystem.getVelocityLimit2();
                tmpColor.set(Color.BLACK.cpy().lerp(spline.color, velocity));
                tmpColor.a = alpha;
                if (velocity > 1.01f) {
                    //hyperdrive travel
                    tmpColor.set(1, 1, 1, 1);
                }
                shape.line(p.x, p.y, p2.x, p2.y, tmpColor, tmpColor);
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }

    Color color = new Color();
    public void renderStatePath(TrailComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = (i + 1) % spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                switch (spline.state[indexWrap]) {
                    case -4: continue; //don't render
                    case -3: color.set(Color.GREEN); break; //or color of asteroid hit?
                    case -2: color.set(Color.BLUE); break;
                    case -1: color.set(Color.RED); break;
                    case 1: color.set(Color.GOLD); break;
                    case 2: color.set(Color.CYAN); break;
                    case 3: color.set(Color.WHITE); break;
                    default: color.set(Color.GRAY);
                }
                color.a -= (float) (spline.indexHead - indexWrap) / spline.path.length+1;
                shape.line(p.x, p.y, p2.x, p2.y, color, color);
                if (spline.state[indexWrap] == 3) {
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation * 3.14 * 0.5)), Color.RED, Color.RED);
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation)), Color.MAGENTA, Color.CYAN);
                    shape.rectLine(p.x, p.y, p2.x, p2.y, (float) (0.5f * Math.sin(animation * 3.14)), Color.GOLD, Color.GREEN);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
        }
    }
    
    public void renderDefaultPath(TrailComponent spline) {
        //todo: bug with tail index not properly rendered
        boolean debugDrawHeadTail = false;
        
        for (int i = 0; i < spline.path.length-1; i++) {
            Vector3 p = spline.path[i];
            
            int indexWrap = (i + 1) % spline.path.length;
            Vector3 p2 = spline.path[indexWrap];
            
            // don't draw head to tail
            if (indexWrap != spline.indexHead) {
                if (spline.color == null) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.GREEN, Color.BLUE);
                } else {
                    float fadeTime = 500;
                    long diff = GameScreen.getGameTimeCurrent() - spline.time;
                    if (diff >= fadeTime) continue;
                    spline.color.a = 1.0f - ((float) diff / fadeTime);
                    shape.line(p.x, p.y, p2.x, p2.y, spline.color, spline.color);
                }
            } else {
                //debug draw head to tail
                if (debugDrawHeadTail) {
                    shape.line(p.x, p.y, p2.x, p2.y, Color.RED, Color.WHITE);
                }
            }
    
            if (spline.indexHead == 0) {
                shape.line(p.x, p.y, p2.x, p2.y, Color.PINK, Color.PURPLE);
            }
        }
    }
    
    public void renderRainbowPath(TrailComponent spline) {
        //todo: rainbow render
        //xy mode
        //change of angle: same color when going straight, 360 otherwise, doesnt care speed
        //change of speed: 0-max, doesn't care angle
    }
    
    @Override
    public void dispose() {
        shape.dispose();
    }
    
}
