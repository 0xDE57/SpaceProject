package com.spaceproject.noise;

import com.badlogic.gdx.Gdx;
import com.spaceproject.ui.Tile;

import java.util.ArrayList;

public class NoiseThread implements Runnable {
    
    private volatile boolean isDone = false;
    private volatile boolean stop = false;

    //generation features and parameters
    private final long seed;
    private final float scale;
    private final int octaves;
    private final float persistence;
    private final float lacunarity;
    private final int mapSize;
    private final int chunkSize;

    //heightmap and tilemap data
    private final NoiseBuffer noise;
    private final ArrayList<Tile> tiles;
    
    public NoiseThread(float s, int o, float p, float l, long seed, int mapSize, int chunkSize, ArrayList<Tile> tiles) {
        this.scale = s;
        this.octaves = o;
        this.persistence = p;
        this.lacunarity = l;
        this.seed = seed;
        this.mapSize = mapSize;
        this.chunkSize = chunkSize;
        this.tiles = tiles;
        noise = new NoiseBuffer();
        noise.seed = seed;
    }
    
    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long heightTime = 0, tileTime = 0, pixelTime = 0;

        Gdx.app.log(getClass().getSimpleName(), "Started: " + toString());

        try {
            //Thread.sleep(10000);//debug delay to see loading effects

            //generate noise
            if (stop) { return; }
            heightTime = System.currentTimeMillis();
            noise.heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, scale, octaves, persistence, lacunarity);
            heightTime = System.currentTimeMillis() - heightTime;

            //map height to tile
            if (stop) { return; }
            tileTime = System.currentTimeMillis();
            noise.tileMap = NoiseGen.createTileMap(noise.heightMap, tiles);
            tileTime = System.currentTimeMillis() - tileTime;

            //create pixelated tilemap based on chunk size
            if (stop) { return; }
            pixelTime = System.currentTimeMillis();
            noise.pixelatedTileMap = NoiseGen.createPixelatedTileMap(noise.tileMap, tiles, chunkSize);
            pixelTime = System.currentTimeMillis() - pixelTime;

            //finish
            isDone = true;
            long endTime = System.currentTimeMillis() - startTime;
            Gdx.app.log(getClass().getSimpleName(),
                    "complete: " + this + " in " + endTime + "ms -> heightmap("
                            + heightTime + ") tilemap(" + tileTime + ") pixelate(" + pixelTime + ")");

        } catch (Exception e) {
            Gdx.app.error(getClass().getSimpleName(), "Interrupt", e);
        }
    }

    public NoiseBuffer getNoise() {
        if (isDone()) return noise;
        
        return null;
    }
    
    public long getSeed() {
        return seed;
    }
    
    public boolean isDone() {
        return isDone && !stop;
    }

    public void stop() {
        stop = true;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof NoiseThread) {
            return ((NoiseThread) o).getSeed() == this.getSeed();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (int) ((seed >> 32) ^ seed); //q/4045063
    }
    
    @Override
    public String toString() {
        return seed + " @ " + mapSize + "x" + mapSize;
    }

}
