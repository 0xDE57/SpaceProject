package com.spaceproject.systems;


import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.AttachedToComponent;
import com.spaceproject.components.BarrelRollComponent;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.components.HyperDriveComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ParticleSystem extends IteratingSystem implements EntityListener, Disposable {
    
    SpriteBatch spriteBatch;
    ParticleEffect fireEffect;
    ParticleEffectPool fireEffectPool;
    ParticleEffect chargeEffect;
    ParticleEffectPool chargeEffectPool;
    ParticleEffect explodeEffect;
    ParticleEffectPool explodeEffectPool;
    ParticleEffect tailEffect;
    ParticleEffectPool tailEffectPool;
    ParticleEffect shieldEffect;
    ParticleEffectPool shieldEffectPool;
    
    final float[] engineColor;
    final float[] engineColorBoost;
    final float[] engineColorHyper;
    final float[] engineColorDamage;
    
    float particleScale = 0.02f;

    EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
    
    public ParticleSystem() {
        super(Family.all(ParticleComponent.class).get());
        
        spriteBatch = new SpriteBatch();
    
        //engine fire
        fireEffect = new ParticleEffect();
        fireEffect.load(Gdx.files.internal("particles/shipEngineFire.particle"), Gdx.files.internal("particles/"));
        fireEffect.scaleEffect(particleScale);
        fireEffectPool = new ParticleEffectPool(fireEffect, 20, 20);
        engineColor = new float[]{ 1, 0.34901962f, 0.047058824f };
        engineColorBoost = new float[]{ 0.047058824f, 0.34901962f, 1 };
        engineColorHyper = new float[]{ 1, 0.047058824f, 0.34901962f };
        engineColorDamage = new float[]{ 1, 0.047058824f, 0.047058824f };

        //projectile charge
        chargeEffect = new ParticleEffect();
        chargeEffect.load(Gdx.files.internal("particles/absorb2.particle"), Gdx.files.internal("particles/"));
        chargeEffect.scaleEffect(particleScale);
        chargeEffectPool = new ParticleEffectPool(chargeEffect, 20, 20);
    
        //projectile explode
        explodeEffect = new ParticleEffect();
        explodeEffect.load(Gdx.files.internal("particles/absorb2.particle"), Gdx.files.internal("particles/"));
        explodeEffect.scaleEffect(particleScale);
        //todo: give own particle effect instead of reusing absorb
        Array<ParticleEmitter> emitters = explodeEffect.getEmitters();
        for (int i = 0; i < emitters.size; i++) {
            emitters.get(i).getVelocity().setLow(15);
        }
        explodeEffectPool = new ParticleEffectPool(explodeEffect, 20, 40);
    
        //shield charge
        shieldEffect = new ParticleEffect();
        shieldEffect.load(Gdx.files.internal("particles/absorb2.particle"), Gdx.files.internal("particles/"));
        shieldEffect.scaleEffect(particleScale);
        shieldEffectPool = new ParticleEffectPool(shieldEffect, 20, 20);
        
        //projectile tail
        tailEffect = new ParticleEffect();
        tailEffect.load(Gdx.files.internal("particles/tail4.particle"), Gdx.files.internal("particles/"));
        tailEffect.scaleEffect(particleScale);
        tailEffectPool = new ParticleEffectPool(tailEffect, 20, 20);
    }
    
    @Override
    public void update(float deltaTime) {
        spriteBatch.setProjectionMatrix(GameScreen.cam.combined);
        spriteBatch.begin();
        count = 0;
        super.update(deltaTime);
        spriteBatch.end();
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ParticleComponent particle = Mappers.particle.get(entity);
        
        switch (particle.type) {
            case shipEngineMain:
            case shipEngineLeft:
            case shipEngineRight:
                AttachedToComponent attached = Mappers.attachedTo.get(entity);
                if (attached != null) {
                    updateEngineParticle(attached.parentEntity, particle);
                }
                break;
            case bulletCharge:
                updateChargeParticle(entity, particle);
                break;
            case bulletExplode:
                updateExplodeParticle(entity, particle);
                break;
            case projectileTrail:
                updateTailParticle(entity, particle);
                break;
            case shieldCharge:
                updateShieldChargeParticle(entity, particle);
                break;
        }

        particle.pooledEffect.draw(spriteBatch, deltaTime);

        for (ParticleEmitter emitter : particle.pooledEffect.getEmitters()) {
            count += emitter.getActiveCount();
        }
    }
    int count;
    private void updateEngineParticle(Entity entity, ParticleComponent particle) {
        ControllableComponent control = Mappers.controllable.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        BarrelRollComponent roll = Mappers.barrelRoll.get(entity);
        ShieldComponent shield = Mappers.shield.get(entity);
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        
        boolean shieldIsOff = shield == null || shield.state != ShieldComponent.State.on;
        boolean hyperdriveIsActive = hyper != null && hyper.state == HyperDriveComponent.State.on;
        
        if (control != null && shieldIsOff && control.activelyControlled) {
            switch (particle.type) {
                case shipEngineMain:
                    if (((control.moveForward || control.boost) && !control.moveBack) || hyperdriveIsActive) {
                        particle.pooledEffect.start();
                    } else {
                        particle.pooledEffect.allowCompletion();
                    }
                    break;
                case shipEngineLeft:
                    if ((control.moveRight || roll.flipState == BarrelRollComponent.FlipState.right || control.moveBack) && !hyperdriveIsActive) {
                        particle.pooledEffect.start();
                    } else {
                        particle.pooledEffect.allowCompletion();
                    }
                    break;
                case shipEngineRight:
                    if ((control.moveLeft || roll.flipState == BarrelRollComponent.FlipState.left || control.moveBack) && !hyperdriveIsActive) {
                        particle.pooledEffect.start();
                    } else {
                        particle.pooledEffect.allowCompletion();
                    }
                    break;
            }
        } else {
            particle.pooledEffect.allowCompletion();
        }
        
        //update emitters
        Array<ParticleEmitter> emitters = particle.pooledEffect.getEmitters();
        float engineRotation = particle.angle + (transform.rotation * MathUtils.radDeg + 180);
        for (int i = 0; i < emitters.size; i++) {
            //set angle to always point out as engine exhaust
            ParticleEmitter.ScaledNumericValue angle = emitters.get(i).getAngle();
            float relativeAngle = engineRotation;
            if (control != null && control.moveBack) {
                ///point side engines forward
                switch (particle.type) {
                    case shipEngineLeft:  relativeAngle -= 45 * MathUtils.radDeg; break;
                    case shipEngineRight: relativeAngle += 45 * MathUtils.radDeg; break;
                }
            }
            angle.setHigh(relativeAngle);
            angle.setLow(relativeAngle);

            //change color during boost
            if (hyperdriveIsActive) {
                ParticleEmitter.GradientColorValue tint = emitters.get(i).getTint();
                tint.setColors(engineColorHyper);
            } else {
                if (roll != null) {
                    ParticleEmitter.GradientColorValue tint = emitters.get(i).getTint();
                    boolean boostActive = roll.flipState != BarrelRollComponent.FlipState.off ||
                            (control != null && control.boost);
                    if (boostActive) {
                        tint.setColors(engineColorBoost);
                    } else {
                        tint.setColors(engineColor);
                    }
                    if (GameScreen.getGameTimeCurrent() - Mappers.health.get(entity).lastHitTime <1000) {
                        tint.setColors(engineColorDamage);
                    }
                }
            }
        }
        
        particle.offset.setAngleDeg(engineRotation);
        particle.pooledEffect.setPosition(transform.pos.x + particle.offset.x, transform.pos.y + particle.offset.y);
    }
    
    private void updateChargeParticle(Entity entity, ParticleComponent particle) {
        ChargeCannonComponent cannon = Mappers.chargeCannon.get(entity);
        if (cannon != null) {
            if (cannon.isCharging) {
                particle.pooledEffect.start();
            } else {
                particle.pooledEffect.allowCompletion();
            }
        }
        
        TransformComponent transform = Mappers.transform.get(entity);
        particle.pooledEffect.setPosition(transform.pos.x, transform.pos.y);
    }
    
    private void updateExplodeParticle(Entity entity, ParticleComponent particle) {
        TransformComponent transform = Mappers.transform.get(entity);
        particle.pooledEffect.setPosition(transform.pos.x, transform.pos.y);
    }
    
    private void updateShieldChargeParticle(Entity entity, ParticleComponent particle) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null) {
            if (shield.state == ShieldComponent.State.charge) {
                particle.pooledEffect.start();
            } else {
                particle.pooledEffect.allowCompletion();
            }
        }
        
        TransformComponent transform = Mappers.transform.get(entity);
        particle.pooledEffect.setPosition(transform.pos.x, transform.pos.y);
    }
    
    private void updateTailParticle(Entity entity, ParticleComponent particle) {
        TransformComponent transform = Mappers.transform.get(entity);
        float rot = particle.angle + (transform.rotation * MathUtils.radDeg + 180);
        float thicc = engineCFG.bodyScale * 2f;
        
        Array<ParticleEmitter> emitters = particle.pooledEffect.getEmitters();
        for (int i = 0; i < emitters.size; i++) {
            //set angle to always point out at tail of projectile
            ParticleEmitter.ScaledNumericValue angle = emitters.get(i).getAngle();
            angle.setHigh(rot);
            angle.setLow(rot);
            
            //set rotation to always point out at tail of projectile
            ParticleEmitter.ScaledNumericValue rotation = emitters.get(i).getRotation();
            rotation.setHigh(rot);
            rotation.setLow(rot);
            
            //set thickness to match texture
            ParticleEmitter.ScaledNumericValue thickness = emitters.get(i).getYScale();
            thickness.setHigh(thicc);
            thickness.setLow(thicc);
        }
        
        particle.offset.setAngleDeg(rot);
        particle.pooledEffect.setPosition(transform.pos.x + particle.offset.x, transform.pos.y + particle.offset.y);
    }
    
    public void initializeParticleFromPool(ParticleComponent particle) {
        switch (particle.type) {
            case shipEngineMain:
            case shipEngineLeft:
            case shipEngineRight:
                particle.pooledEffect = fireEffectPool.obtain();
                //start with emitter off
                particle.pooledEffect.allowCompletion();
                break;
            case bulletCharge:
                particle.pooledEffect = chargeEffectPool.obtain();
                //start with emitter on
                particle.pooledEffect.start();
                break;
            case bulletExplode:
                particle.pooledEffect = explodeEffectPool.obtain();
                particle.pooledEffect.start();
                break;
            case projectileTrail:
                particle.pooledEffect = tailEffectPool.obtain();
                //start with emitter on
                particle.pooledEffect.start();
                break;
            case shieldCharge:
                particle.pooledEffect = shieldEffectPool.obtain();
                //start with emitter off
                particle.pooledEffect.allowCompletion();
                break;
        }
    }
    
    public void freeParticleFromPool(ParticleComponent particle) {
        switch (particle.type) {
            case shipEngineMain:
            case shipEngineLeft:
            case shipEngineRight:
                fireEffectPool.free(particle.pooledEffect);
                break;
            case bulletCharge:
                chargeEffectPool.free(particle.pooledEffect);
                break;
            case bulletExplode:
                explodeEffectPool.free(particle.pooledEffect);
                break;
            case projectileTrail:
                tailEffectPool.free(particle.pooledEffect);
                break;
            case shieldCharge:
                shieldEffectPool.free(particle.pooledEffect);
                break;
        }
    }

    public int getParticleCount() {
        return count;
    }
    
    @Override
    public void entityAdded(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect == null) {
            initializeParticleFromPool(particle);
        }
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect != null) {
            freeParticleFromPool(particle);
        }
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        engine.addEntityListener(this);
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        super.removedFromEngine(engine);
        engine.removeEntityListener(this);
    }
    
    @Override
    public void dispose() {
        fireEffect.dispose();
        chargeEffect.dispose();
        tailEffect.dispose();
        shieldEffect.dispose();
        explodeEffect.dispose();
        spriteBatch.dispose();
    }
    
}
