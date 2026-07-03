package com.spaceproject.noise;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.ui.Tile;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class NoiseManager implements INoiseGenListener, Disposable {
    
    private final NoiseThreadPoolExecutor noiseThreadPool;
    private final LinkedBlockingQueue<NoiseBuffer> noiseBufferQueue;
    private final HashMap<Long, NoiseBuffer> loadedNoise;
    
    public NoiseManager(int maxThreads) {
        noiseThreadPool = new NoiseThreadPoolExecutor(maxThreads);
        noiseThreadPool.addListener(this);
        noiseBufferQueue = new LinkedBlockingQueue<>();
        loadedNoise = new HashMap<>();
    }
    
    public void generate(long seed, PlanetComponent planet) {
        if (loadedNoise.containsKey(seed)) {
            Gdx.app.debug(this.getClass().getSimpleName(), "noise for seed [" + seed + "] already exists. Ignoring.");
            return;
        }
        
        int chunkSize = SpaceProject.configManager.getConfig(WorldConfig.class).chunkSize;
        NoiseThread noiseThread = new NoiseThread(planet.scale, planet.octaves, planet.persistence, planet.lacunarity, seed, planet.mapSize, chunkSize, Tile.defaultTiles);
        noiseThreadPool.execute(noiseThread);
    }
    
    public NoiseBuffer getNoiseForSeed(long seed) {
        return loadedNoise.get(seed);
    }
    
    public void loadOrCreateNoiseFor(long seed, PlanetComponent planet) {
        NoiseBuffer noiseBuffer = getNoiseForSeed(seed);
        
        if (noiseBuffer == null) {
            Gdx.app.debug(this.getClass().getSimpleName(), "no noise found, generating: " + seed);
            generate(seed, planet);
        } else {
            //push to queue for pickup by SpaceLoadingSystem
            Gdx.app.debug(this.getClass().getSimpleName(), "noise found, loading: " + seed);
            noiseBufferQueue.add(noiseBuffer);
        }
    }
    
    @Override
    public void threadFinished(NoiseThread noiseThread) {
        //pass in noise buffer queue from executor?
        //take() here?
        //add to loadednoise
        //let otheres poll loaded noise, or fire another event from here?
        NoiseBuffer noise = noiseThread.getNoise();
        if (noise != null) {
            loadedNoise.put(noise.seed, noise);
            noiseBufferQueue.add(noise);
        }
    }
    
    
    public NoiseThreadPoolExecutor getNoiseThreadPool() {
        return noiseThreadPool;
    }
    
    
    public LinkedBlockingQueue<NoiseBuffer> getNoiseBufferQueue() {
        return noiseBufferQueue;
    }
    
    
    public HashMap<Long, NoiseBuffer> getLoadedNoise() {
        return loadedNoise;
    }
    
    public boolean isNoiseAvailable() {
        return !noiseBufferQueue.isEmpty();
    }
    
    public NoiseBuffer getNoiseFromQueue() {
        try {
            return noiseBufferQueue.take();
        } catch (InterruptedException e) {
            Gdx.app.error(this.getClass().getSimpleName(), "error retrieving noise from queue", e);
        }
        
        return null;
    }
    
    @Override
    public void dispose() {
        Gdx.app.log(this.getClass().getSimpleName(), "Dispose: " + noiseThreadPool.getActiveCount());
        noiseThreadPool.purge();
        for (Runnable thread : noiseThreadPool.getQueue()) {
            ((NoiseThread) thread).stop();//kindly stop
        }
        noiseThreadPool.shutdown();
        
        loadedNoise.clear();
        noiseBufferQueue.clear();
        
        /*
        try {
            if (noiseThreadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                Gdx.app.error(this.getClass().getSimpleName(), "Thread shutdown timed out. Forcing!");
                noiseThreadPool.shutdownNow();//yeah yeah, shut down is bad
            } else {
                Gdx.app.log(this.getClass().getSimpleName(), "Thread shutdown complete");
            }
        } catch (InterruptedException e) {
            Gdx.app.error(this.getClass().getSimpleName(), "Error shutting down threads", e);
        }*/
        
    }
}
