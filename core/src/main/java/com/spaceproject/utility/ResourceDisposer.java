package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.components.ChargeCannonComponent;
import com.spaceproject.components.ParticleComponent;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.SoundComponent;
import com.spaceproject.components.Sprite3DComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.systems.SoundSystem;

public class ResourceDisposer {
    
    private static int disposedTextures, disposedS3D, destroyedBody, disposedParticle;
    private static int totalTextures, totalS3D, totalBody, totalParticle;
    private static int totalTotal;
    private static int soundKilled;
    private static int additionalRemove;
    private static final StringBuilder info = new StringBuilder();
    
    public static void dispose(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        if (tex != null) {
            tex.texture.dispose();
            tex.texture = null;
            disposedTextures++;
        }
        
        Sprite3DComponent s3d = Mappers.sprite3D.get(entity);
        if (s3d != null) {
            s3d.renderable.dispose();
            s3d.renderable = null;
            disposedS3D++;
        }
    
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            physics.body.getWorld().destroyBody(physics.body);
            physics.body = null;
            destroyedBody++;
        }
    
        ParticleComponent particle = Mappers.particle.get(entity);
        if (particle != null && particle.pooledEffect != null) {
            particle.pooledEffect.dispose();
            particle.pooledEffect = null;
            disposedParticle++;
        }
    
        SoundComponent sound = Mappers.sound.get(entity);
        if (sound != null) {
            int stopped = SoundSystem.stopSound(sound);
            soundKilled += stopped;
        }

        //if entity was charging a projectile, make sure the projectile entity is also removed
        ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(entity);
        if (chargeCannon != null && chargeCannon.projectileEntity != null) {
            //destroy or release
            chargeCannon.projectileEntity.add(new RemoveComponent());
            chargeCannon.projectileEntity = null;
            additionalRemove++;
        }
    }
    
    public static void disposeAllExcept(ImmutableArray<Entity> entities, Array<Entity> ignoreEntities) {
        for (Entity entity : entities) {
            if (ignoreEntities != null && ignoreEntities.contains(entity, false)) {
                Gdx.app.debug("ResourceDisposer", "Skip dispose: " + DebugUtil.objString(entity));
                continue;
            }
            dispose(entity);
        }
    }
    
    public static void disposeAll(ImmutableArray<Entity> entities) {
        disposeAllExcept(entities, null);
    }
    
    public static void reset() {
        //total
        totalTextures += disposedTextures;
        totalS3D += disposedS3D;
        totalParticle += disposedParticle;
        totalBody += destroyedBody;
        totalTotal = totalTextures + totalS3D + totalParticle + totalBody;//<-in case it wasn't clear, this is the total
        
        //reset per frame data: should be called at very end of frame
        disposedTextures = 0;
        disposedS3D = 0;
        disposedParticle = 0;
        destroyedBody = 0;
    }
    
    public static String getTotalDisposeCount() {
        reset();
        info.setLength(0);
        info.append("\n     [Texture]:  ").append(totalTextures);
        info.append("\n     [Sprite3D]: ").append(totalS3D);
        info.append("\n     [Particle]: ").append(totalParticle);
        info.append("\n     [B2D Body]: ").append(totalBody);
        info.append("\n     [Total]:    ").append(totalTotal);
        //not disposed so maybe doesn't belong but maybe useful
        info.append("\n     [Sound]:    ").append(soundKilled);
        info.append("\n     [AddRemove]:    ").append(additionalRemove);//?
        return info.toString();
    }
    
}
