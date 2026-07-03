package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class CannonSystem extends IteratingSystem {

    public CannonSystem() {
        super(Family.all(CannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CannonComponent cannon = Mappers.cannon.get(entity);

        updateCoolDown(cannon, deltaTime);

        ControllableComponent control = Mappers.controllable.get(entity);//todo: move control.attack into cannon property to decouple
        if (control.attack && canShoot(entity)) {
            fireCannon(cannon, entity);
        }
    }
    
    private boolean canShoot(Entity entity) {
        BarrelRollComponent roll = Mappers.barrelRoll.get(entity);
        ShieldComponent shield = Mappers.shield.get(entity);
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        boolean shieldActive = shield != null && shield.state == ShieldComponent.State.on;
        boolean hyperActive = hyper != null && hyper.state == HyperDriveComponent.State.on;
        boolean isRolling = roll != null && roll.flipState != BarrelRollComponent.FlipState.off;
        return !shieldActive && !hyperActive && !isRolling;
    }


    private void fireCannon(CannonComponent cannon, Entity parentEntity) {
        //set dynamic rate of fire based on multiplier (max on MnK, analog on controller)
        int rateOfFire = (int) (cannon.baseRate * (1 - cannon.multiplier));
        if (rateOfFire < cannon.minRate) {
            rateOfFire = cannon.minRate;
        }
        cannon.timerFireRate.setInterval(rateOfFire, false);

        //check if can fire before shooting
        float overheatThreshold = 0.90f;
        if (!cannon.timerFireRate.canDoEvent() || (cannon.heat > overheatThreshold)) {
            if (SpaceProject.configManager.getConfig(DebugConfig.class).infiniteFire) {
                cannon.velocity = 999999;
                cannon.damage = 999999;
            } else {
                return;
            }
        }
        cannon.timerFireRate.reset();
        cannon.heat += cannon.heatRate;
        if (cannon.heat >= 1f) {
            cannon.heat = 1;
        }
        StatsComponent stats = Mappers.stat.get(parentEntity);
        if (stats != null) {
            stats.shotsFired++;
        }

        //create missile
        Entity missile = createMissile(cannon, parentEntity);
        getEngine().addEntity(missile);
        getEngine().getSystem(SoundSystem.class).cannonShoot(0.4f + (1-cannon.heat * 0.5f));
    }

    private static void  updateCoolDown(CannonComponent cannon, float deltaTime) {
        cannon.heat -= cannon.cooldownRate * deltaTime;
        if (cannon.heat <= 0) {
            cannon.aimOffset = 0;
            cannon.heat = 0;
            return;
        }
        float offset = (float) Math.sin(cannon.heat) * cannon.heatInaccuracy;
        cannon.aimOffset = MathUtils.random(offset, -offset);
    }

    static EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    public static Entity createMissile(CannonComponent cannon, Entity parentEntity) {
        Entity entity = new Entity();

        //physics
        TransformComponent parentTransform = Mappers.transform.get(parentEntity);
        Vector2 spawnPos = cannon.anchorVec.cpy().rotateRad(parentTransform.rotation).add(parentTransform.pos);
        float rot = parentTransform.rotation + cannon.aimAngle + cannon.aimOffset;
        Vector2 sourceVel = Mappers.physics.get(parentEntity).body.getLinearVelocity();
        Vector2 projectileVel = MyMath.vector(rot, cannon.velocity).add(sourceVel);
        float bodyWidth = 3 * engineCFG.bodyScale;
        float bodyHeight = 2 * engineCFG.bodyScale;
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createRect(spawnPos.x, spawnPos.y, bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody, entity);
        physics.body.setTransform(spawnPos, rot);
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        
        //transform
        TransformComponent transform = new TransformComponent();
        transform.pos.set(physics.body.getPosition());
        transform.rotation = physics.body.getAngle();
        transform.zOrder = RenderOrder.PROJECTILES.getHierarchy();
        
        //expire time (self destruct)
        ExpireComponent expire = new ExpireComponent();
        expire.timer = new SimpleTimer(20000, true);
        
        //missile damage
        DamageComponent missile = new DamageComponent();
        missile.damage = cannon.damage;
        missile.source = parentEntity;
    
        //particle fx -> trailing effect
        ParticleComponent newParticle = new ParticleComponent();
        newParticle.type = ParticleComponent.EffectType.projectileTrail;
        newParticle.offset = new Vector2(0, bodyWidth);
        entity.add(newParticle);
        
        //entity.add(new TrailComponent());
        
        entity.add(missile);
        entity.add(expire);
        entity.add(physics);
        entity.add(transform);
        
        return entity;
    }
    
}
