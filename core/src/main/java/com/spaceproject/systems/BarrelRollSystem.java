package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;

public class BarrelRollSystem extends IteratingSystem {
    
    private final Interpolation animInterpolation = Interpolation.pow2;
    private final float strafeMaxRollAngle = 40 * MathUtils.degRad;
    private final float strafeRollSpeed = 4f;
    private final float hyperRollSpeed = 20;
    
    public BarrelRollSystem() {
        super(Family.all(Sprite3DComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Sprite3DComponent sprite3D = Mappers.sprite3D.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
    
        float rollAmount = strafeRollSpeed * deltaTime;
    
        //clamp within
        //DebugSystem.addDebugText(sprite3D.renderable.angle + "", 10, 10);
        float s = 360 * MathUtils.degRad;
        if (sprite3D.renderable.angle > s) {
            sprite3D.renderable.angle -= s;
        }
        if (sprite3D.renderable.angle < -s) {
            sprite3D.renderable.angle += s;
        }
        
        //don't allow dodging while shield is active
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            //continue to stabilize while shield active
            stabilizeRoll(sprite3D, rollAmount);
            return;
        }
        
        //force roll while hyperdrive active
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            sprite3D.renderable.angle += hyperRollSpeed * deltaTime;
            //and don't allow dodge while hyper so exit here
            return;
        }
        
        //barrel roll
        BarrelRollComponent rollComp = Mappers.barrelRoll.get(entity);
        if (rollComp != null) {
            if (rollComp.flipState != BarrelRollComponent.FlipState.off) {
                barrelRoll(sprite3D, rollComp);
                return;
            }
        }
        
        //strafe roll
        if (control != null) {
            if (control.moveLeft) {
                rollLeft(sprite3D, rollAmount);
            }
            if (control.moveRight) {
                rollRight(sprite3D, rollAmount);
            }
            if (!control.moveLeft && !control.moveRight) {
                stabilizeRoll(sprite3D, rollAmount);
            }
        } else  {
            stabilizeRoll(sprite3D, rollAmount);
        }
    }
    
    private void rollLeft(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle += roll;
        //if (sprite3D.renderable.angle)
        //sprite3D.renderable.angle = MathUtils.clamp(sprite3D.renderable.angle, -strafeMaxRollAngle, strafeMaxRollAngle);
    }
    
    private void rollRight(Sprite3DComponent sprite3D, float roll) {
        sprite3D.renderable.angle -= roll;
        //sprite3D.renderable.angle = MathUtils.clamp(sprite3D.renderable.angle, -strafeMaxRollAngle, strafeMaxRollAngle);
    }
    
    private void stabilizeRoll(Sprite3DComponent sprite3D, float roll) {
        //if (sprite3D.renderable.angle == 0) return;
        
        if (sprite3D.renderable.angle < 0) {
            sprite3D.renderable.angle += roll;
        }
        if (sprite3D.renderable.angle > 0) {
            sprite3D.renderable.angle -= roll;
        }
    }
    
    public static void dodgeLeft(Entity entity, BarrelRollComponent roll) {
        if (roll.cooldownTimer.tryEvent()) {
            applyDodgeImpulse(entity, roll, BarrelRollComponent.FlipState.left);
        }
    }
    
    public static void dodgeRight(Entity entity, BarrelRollComponent roll) {
        if (roll.cooldownTimer.tryEvent()) {
            applyDodgeImpulse(entity, roll, BarrelRollComponent.FlipState.right);
        }
    }
    
    private static void applyDodgeImpulse(Entity entity, BarrelRollComponent roll, BarrelRollComponent.FlipState flipState) {
        //don't allow roll or dodge while hyperdrive active
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            Gdx.app.debug(BarrelRollSystem.class.getSimpleName(), "barrel roll cancelled due to hyperdrive");
            return;
        }
        
        //snap to angle to bypass rotation lerp to make dodge feel better/more responsive
        TransformComponent transform = Mappers.transform.get(entity);
        ControllableComponent control = Mappers.controllable.get(entity);
        transform.rotation = control.angleTargetFace;
        Body body = Mappers.physics.get(entity).body;
        body.setAngularVelocity(0);
        body.setTransform(body.getPosition(), transform.rotation);
        
        //apply left or right impulse
        float direction = transform.rotation;//forward
        switch (flipState) {
            case left:
                direction += MathUtils.PI / 2;
                break;
            case right:
                direction -= MathUtils.PI / 2;
                break;
        }
        body.applyLinearImpulse(MyMath.vector(direction, roll.force), body.getPosition(), true);
    
        //set roll animation
        roll.flipState = flipState;
        roll.animationTimer.reset();
    }
    
    private void barrelRoll(Sprite3DComponent sprite3D, BarrelRollComponent rollComp) {
        //reset
        if (rollComp.animationTimer.canDoEvent()) {
            rollComp.flipState = BarrelRollComponent.FlipState.off;
            sprite3D.renderable.angle = 0;
            return;
        }
    
        float rotation = MathUtils.PI2 * rollComp.revolutions;
        switch (rollComp.flipState) {
            case left:
                sprite3D.renderable.angle = animInterpolation.apply(rotation, 0, rollComp.animationTimer.ratio());
                break;
            case right:
                sprite3D.renderable.angle = animInterpolation.apply(0, rotation, rollComp.animationTimer.ratio());
                break;
        }
    }
    
}
