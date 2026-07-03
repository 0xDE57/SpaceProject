package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.DamageComponent;
import com.spaceproject.components.ExpireComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TrailComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.RenderOrder;
import com.spaceproject.generation.BodyBuilder;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.math.MyMath;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class ChargeCannonSystem extends IteratingSystem {
    
    public ChargeCannonSystem() {
        super(Family.all(ChargeCannonComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(entity);
        ShieldComponent shieldComponent = Mappers.shield.get(entity);
        
        if (chargeCannon.isCharging) {
            //update position to be in front of ship
            updateChargePosition(chargeCannon, entity);
        
            //accumulate size
            growCharge(chargeCannon);
            
            //use of shield will kill charge, cancel shot
            if (shieldComponent != null && shieldComponent.state != ShieldComponent.State.off) {
                deactivate(chargeCannon);
                return;
            }
        }
        
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.attack) {
            //charge
            if (!chargeCannon.isCharging && canShoot(entity)) {
                activate(chargeCannon);
            }
        } else {
            //release
            if (chargeCannon.isCharging) {
                releaseProjectile(chargeCannon, entity);
            }
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
    
    private void growCharge(ChargeCannonComponent chargeCannon) {
        chargeCannon.size = chargeCannon.growRateTimer.ratio() * chargeCannon.maxSize;
        updateChargeTexture(chargeCannon);
    }
    
    private void updateChargeTexture(ChargeCannonComponent chargeCannon) {
        TextureComponent textureComp = Mappers.texture.get(chargeCannon.projectileEntity);
        textureComp.scale = chargeCannon.size;
    }
    
    private void updateChargePosition(ChargeCannonComponent chargeCannon, Entity parentEntity) {
        TransformComponent parentTransform = Mappers.transform.get(parentEntity);
        TransformComponent projectileTransform = Mappers.transform.get(chargeCannon.projectileEntity);
        Vector2 spawnPos = chargeCannon.anchorVec.cpy().rotateRad(parentTransform.rotation).add(parentTransform.pos);
        projectileTransform.pos.set(spawnPos);
        projectileTransform.rotation = parentTransform.rotation + chargeCannon.aimAngle;
    }
    
    private void activate(ChargeCannonComponent chargeCannon) {
        chargeCannon.isCharging = true;
        chargeCannon.growRateTimer.reset();
        
        chargeCannon.projectileEntity = createGrowMissileChargeEntity();
        getEngine().addEntity(chargeCannon.projectileEntity);
    }
    
    private void deactivate(ChargeCannonComponent chargeCannon) {
        if (chargeCannon.projectileEntity != null) {
            chargeCannon.projectileEntity.add(new RemoveComponent());
            chargeCannon.projectileEntity = null;
        }
        chargeCannon.isCharging = false;
    }
    
    private void releaseProjectile(ChargeCannonComponent chargeCannon, Entity parentEntity) {
        chargeCannon.isCharging = false;
    
        //clamp size and update texture
        chargeCannon.size = MathUtils.clamp(chargeCannon.size, chargeCannon.minSize, chargeCannon.maxSize);
        updateChargeTexture(chargeCannon);
    
        //damage modifier
        DamageComponent damageComponent = new DamageComponent();
        damageComponent.source = parentEntity;
        damageComponent.damage = chargeCannon.baseDamage + (10 * (chargeCannon.size / chargeCannon.maxSize) * chargeCannon.baseDamage);
        if (chargeCannon.size >= chargeCannon.maxSize) {
            damageComponent.damage *= 1.20;//bonus damage for maxed out
        }
        chargeCannon.projectileEntity.add(damageComponent);
    
        //physics
        TextureComponent textureComponent = Mappers.texture.get(chargeCannon.projectileEntity);
        float bodyWidth = textureComponent.texture.getWidth() * textureComponent.scale;
        float bodyHeight = textureComponent.texture.getHeight() * textureComponent.scale;
        TransformComponent transformComponent = Mappers.transform.get(chargeCannon.projectileEntity);
        PhysicsComponent physics = new PhysicsComponent();
        physics.body = BodyBuilder.createRect(transformComponent.pos.x, transformComponent.pos.y,
                bodyWidth, bodyHeight, BodyDef.BodyType.DynamicBody, chargeCannon.projectileEntity);
        physics.body.setTransform(transformComponent.pos, transformComponent.rotation);
        //add parent velocity to projectile
        Body parentBody = Mappers.physics.get(parentEntity).body;
        Vector2 projectileVel = MyMath.vector(transformComponent.rotation, chargeCannon.velocity).add(parentBody.getLinearVelocity());
        physics.body.setLinearVelocity(projectileVel);
        physics.body.setBullet(true);//turn on CCD
        chargeCannon.projectileEntity.add(physics);
    
        //auto expire
        ExpireComponent expire = new ExpireComponent();
        expire.timer = new SimpleTimer(8000, true);
        chargeCannon.projectileEntity.add(expire);
    
        //particle fx
        ParticleSystem particleSystem = getEngine().getSystem(ParticleSystem.class);
        //destroy old particle -> stop absorbing effect
        ParticleComponent oldParticle = chargeCannon.projectileEntity.remove(ParticleComponent.class);
        if (oldParticle != null && oldParticle.pooledEffect != null) {
            oldParticle.pooledEffect.allowCompletion();
            if (particleSystem != null) {
                particleSystem.freeParticleFromPool(oldParticle);
            }
        }
        //add new particle -> start trailing effect
        ParticleComponent newParticle = new ParticleComponent();
        newParticle.type = ParticleComponent.EffectType.projectileTrail;
        if (particleSystem != null) {
            particleSystem.initializeParticleFromPool(newParticle);
        }
        newParticle.offset = new Vector2(0.75f, 0.0f);
        chargeCannon.projectileEntity.add(newParticle);
        
        //spline trail
        chargeCannon.projectileEntity.add(new TrailComponent());
        
        //release reference to "ghost"
        chargeCannon.projectileEntity = null;
        chargeCannon.shotsFired++;
        
        //normalize: size(min,max) to pitch [0.5-2.0]
        //min -> 2.0f
        //max -> 0.5f
        //m = change in y / change in x
        //m = (y2 - y1) / (x2 - x1)
        //
        //map a-b to x-y
        //out = b1 + (b2 - b1) * ((in-a1)/(a2-a1))
        //map from/to
        float pitch = MathUtils.map(chargeCannon.minSize,chargeCannon.maxSize, 2.0f, 0.5f, chargeCannon.size);
        getEngine().getSystem(SoundSystem.class).laserCharge(1, pitch);
    }
    
    private Entity createGrowMissileChargeEntity() {
        Entity entity = new Entity();
        
        //create texture
        TextureComponent texture = new TextureComponent();
        texture.texture = TextureGenerator.generateProjectile();
        texture.scale = 0;//start at nothing
        entity.add(texture);
        
        TransformComponent transform = new TransformComponent();
        transform.zOrder = RenderOrder.PROJECTILES.getHierarchy();
        entity.add(transform);
        
        ParticleComponent particle = new ParticleComponent();
        particle.type = ParticleComponent.EffectType.bulletCharge;
        particle.offset = new Vector2();
        entity.add(particle);
        
        return entity;
    }
    
}
