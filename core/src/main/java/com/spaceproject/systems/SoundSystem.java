package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ObjectMap;
import com.spaceproject.components.ItemComponent;
import com.spaceproject.components.SoundComponent;
import de.pottgames.tuningfork.*;
import de.pottgames.tuningfork.logger.GdxLogger;

public class SoundSystem extends EntitySystem implements Disposable {

    private boolean log = false;
    private AssetManager assetManager;

    private Audio audio;

    private String f3File = "sound/f3.wav",
            shootFile = "sound/laserShoot.wav",
            explodeFile = "sound/explode.wav",
            shipEngineAmbientFile = "sound/55hz.wav",
            hullImpactFile = "sound/hullImpactLight.mp3",
            hullImpactHeavyFile = "sound/hullImpactHeavy.mp3",
            healthAlarmFile = "sound/lowHealthAlarm.ogg",
            shieldOnFile = "sound/shieldOn.mp3",
            shieldOffFile = "sound/shieldOff.mp3",
            shieldAmbientFile = "sound/shieldAmbient.wav",
            shieldImpactFile = "sound/shieldImpact.mp3",
            pickupFile = "sound/pickup.wav",
            creditsFile = "sound/credits.wav",
            dockStationFile = "sound/dockStation.wav",
            undockStationFile = "sound/undockStation.wav",
            clickFile = "sound/click.wav";

    private SoundBuffer shipEngineAmbientLoop;
    private SoundBuffer shipExplode;
    private SoundBuffer f3;
    private SoundBuffer break0, break1, break2, break3, break4;
    //Sound bounce0, bounce1, bounce2;
    private SoundBuffer click;
    private SoundBuffer laserShoot;//, laserShootCharge;
    private SoundBuffer hullImpact, hullImpactHeavy;
    private SoundBuffer healthAlarm;
    private SoundBuffer shieldImpact, shieldOn, shieldOff, shieldAmbientLoop;
    //SoundBuffer hyperdriveEngage;
    private SoundBuffer pickup, credits;
    //SoundBuffer heal;
    private SoundBuffer dockStation, undockStation;

    private Reverb reverbData;
    private RingModulator ringModData;
    private SoundEffect reverbEffect;
    private SoundEffect ringModEffect;

    @Override
    public void addedToEngine(Engine engine) {
        final AudioConfig config = new AudioConfig();
        if (log) {
            config.setLogger(new GdxLogger());
        } else {
            config.setLogger(null);
        }
        audio = Audio.init(config);

        assetManager = new AssetManager();
        assetManager.setLogger(new Logger("AssetManager", Logger.ERROR));
        assetManager.setLoader(SoundBuffer.class, new SoundBufferLoader(new InternalFileHandleResolver()));

        assetManager.load(f3File, SoundBuffer.class);
        assetManager.load(shootFile, SoundBuffer.class);
        assetManager.load(explodeFile, SoundBuffer.class);
        assetManager.load(shipEngineAmbientFile, SoundBuffer.class);
        assetManager.load(hullImpactFile, SoundBuffer.class);
        assetManager.load(hullImpactHeavyFile, SoundBuffer.class);
        assetManager.load(healthAlarmFile, SoundBuffer.class);
        assetManager.load(shieldOnFile, SoundBuffer.class);
        assetManager.load(shieldOffFile, SoundBuffer.class);
        assetManager.load(shieldAmbientFile, SoundBuffer.class);
        assetManager.load(shieldImpactFile, SoundBuffer.class);
        assetManager.load(pickupFile, SoundBuffer.class);
        assetManager.load(creditsFile, SoundBuffer.class);
        assetManager.load(dockStationFile, SoundBuffer.class);
        assetManager.load(undockStationFile, SoundBuffer.class);
        assetManager.load(clickFile, SoundBuffer.class);
        assetManager.finishLoading();

        f3 = assetManager.get(f3File);
        laserShoot = assetManager.get(shootFile);
        shipExplode = assetManager.get(explodeFile);
        shipEngineAmbientLoop = assetManager.get(shipEngineAmbientFile);
        hullImpact = assetManager.get(hullImpactFile);
        hullImpactHeavy = assetManager.get(hullImpactHeavyFile);
        healthAlarm = assetManager.get(healthAlarmFile);
        shieldOn = assetManager.get(shieldOnFile);
        shieldOff = assetManager.get(shieldOffFile);
        shieldAmbientLoop = assetManager.get(shieldAmbientFile);
        shieldImpact = assetManager.get(shieldImpactFile);
        pickup = assetManager.get(pickupFile);
        credits = assetManager.get(creditsFile);
        dockStation = assetManager.get(dockStationFile);
        undockStation = assetManager.get(undockStationFile);
        click = assetManager.get(clickFile);

        //test effects
        reverbData = new Reverb();
        reverbEffect = new SoundEffect(reverbData);
        ringModData = new RingModulator();
        ringModData.waveform = 1;
        ringModEffect = new SoundEffect(ringModData);
    }

    public void setVolume(float value) {
        audio.setMasterVolume(value);
    }

    public float getVolume() {
        return audio.getMasterVolume();
    }

    public void pauseAll() {
        audio.pauseAll();
    }

    public void resumeAll() {
        audio.resumeAll();
    }

    public static int stopSound(SoundComponent soundComponent) {
        int stopped = 0;
        for (ObjectMap.Entry<String, BufferedSoundSource> sound : soundComponent.sources) {
            if (sound.value.isPlaying()) {
                sound.value.free();
                stopped++;
            }
        }
        return stopped;
    }

    public void shipEngineAmbient(SoundComponent sound, boolean active, float velocity, float angleDelta, float deltaTime) {
        String key = shipEngineAmbientFile;
        BufferedSoundSource currentSource = sound.sources.get(key);
        if (active) {
            if (currentSource == null) {
                currentSource = audio.obtainSource(shipEngineAmbientLoop);
                currentSource.attachEffect(reverbEffect);
                currentSource.play();
                currentSource.setLooping(true);
                sound.sources.put(key, currentSource);
            }
            float relVel = velocity / Box2DPhysicsSystem.getVelocityLimit();
            float pitch = MathUtils.map(0f,  1f, 0.5f, 2.0f, relVel);
            currentSource.setPitch(pitch);
            float interp = 1;// Interpolation.exp10In.apply(relVel);
            //DebugSystem.addDebugText(1-interp+ "", 500, 500);
            currentSource.setFilter(1-interp, 0);
            //reverbData.density = 1-relVel;
            //reverbEffect.updateEffect(reverbData);
        } else {
            if (currentSource != null) {
                currentSource.free();
                sound.sources.remove(key);
            }
        }
    }

    public void shieldAmbient(SoundComponent sound, boolean active) {
        String key = shieldAmbientFile;
        BufferedSoundSource currentSource = sound.sources.get(key);
        if (active) {
            if (currentSource == null) {
                currentSource = audio.obtainSource(shieldAmbientLoop);
                currentSource.attachEffect(reverbEffect);
                currentSource.setLooping(true);
                currentSource.play();
                sound.sources.put(key, currentSource);
            }
        } else {
            if (currentSource != null) {
                currentSource.free();
                sound.sources.remove(key);
            }
        }
    }

    public void asteroidShatter(ItemComponent.Resource resource) {
        float pitch = MathUtils.random(0.5f, 2.0f);
        //pitch based on asteroid size?
        //pitch = MathUtils.map(minAsteroidSize, maxArea, 2f, 0.5f, asteroid.area);
        switch (resource) {
            //case RED: return break0.play(1, pitch, 0);
            //case GREEN: return break1.play(1, pitch, 0);
            //case BLUE: return break2.play(1, pitch, 0);
            //case SILVER: return break3.play(1, pitch, 0);
            //case GOLD: return break4.play(1, pitch, 0);
            //default: f3.play(1.25f, pitch, 0);
        }
    }

    public void asteroidHit(float ratio) {
        float pitch = 1 + 1 - ratio;
        click.play(0.5f, pitch);
    }

    public void cannonShoot(float pitch) {
        float offset = 0.02f;
        cannonShoot(0.1f, pitch + MathUtils.random(-offset, offset));
    }

    public void cannonShoot(float volume, float pitch) {
        laserShoot.play(volume, pitch, 0);
    }

    public void laserCharge(float volume, float pitch) {
        //laserShootCharge.play(volume, pitch, 0);
    }

    public void hullImpactLight(float volume) {
        hullImpact.play(volume, 2, 0);
    }

    public void hullImpactHeavy(float pitch) {
        hullImpactHeavy.play(1, pitch, 0);
    }

    //int maxduration = 2;
    //SimpleTimer timer = new SimpleTimer(-1);
    public void healthAlarm(SoundComponent sound) {
        healthAlarm.play();
        /* todo: loop warning only 2 or 3 times.
        String key = healthAlarmFile;
        BufferedSoundSource currentSource = sound.sources.get(key);
        if (currentSource == null) {
            currentSource = audio.obtainSource(healthAlarm);
            currentSource.setLooping(true);
            currentSource.play();
            sound.sources.put(key, currentSource);
            //timer.setInterval(healthAlarm.getDuration(), true);

        } else {
            float time = healthAlarm.getDuration() * maxduration;
            //if ()
        }*/
    }

    public void shieldImpact(float volume) {
        shieldImpact.play(volume, 1, 0);
    }

    public void shieldOn() {
        shieldOn.play();
    }

    public void shieldOff() {
        shieldOff.play();
    }

    public void hyperdriveEngage() {
        //hyperdriveEngage.play();//disable for now
    }

    public void pickup() {
        float pitch = MathUtils.random(0.5f, 2.0f);
        pickup.play(0.5f, pitch, 0);
    }

    public void shipExplode() {
        shipExplode.play();
    }

    public void dockStation() {
        dockStation.play(0.25f);
    }

    public void undockStation() {
        undockStation.play(0.25f);
    }

    public void addCredits(float pitch) {
        credits.play(0.5f, pitch, 0);
    }

    public void heal() {
        //heal.play();
    }

    @Override
    public void dispose() {
        assetManager.dispose();

        //cleanup sounds
        shipEngineAmbientLoop.dispose();
        shipExplode.dispose();
        f3.dispose();
        laserShoot.dispose();
        //laserShootCharge.dispose();
        hullImpact.dispose();
        hullImpactHeavy.dispose();
        healthAlarm.dispose();
        shieldImpact.dispose();
        shieldOn.dispose();
        shieldOff.dispose();
        shieldAmbientLoop.dispose();
        //hyperdriveEngage.dispose();
        pickup.dispose();
        credits.dispose();
        //heal.dispose();
        dockStation.dispose();
        undockStation.dispose();
        click.dispose();
        //cleanup effects
        reverbEffect.dispose();
        ringModEffect.dispose();

        audio.dispose();
    }

}
