package com.spaceproject.noise;

public class NoiseBuffer {
    //TODO: use a byte instead of float and int
    //byte  = 1 byte (8 bits): -128 to 127
    //float = 4 bytes (32 bits): ±1.18×10−38 to ±3.4×1038	Approximately 7 decimal digits
    //int   = 4 bytes (-2,147,483,648 to 2,147,483, 647)
    //we don't need that much resolution/precision
    //eg: map size = 1024
    //float: [1024] * [1024] * 4 = 4194304 bytes (~4.2MB)... yikes!
    //byte:  [1024] * [1024] * 1 = 1048576 bytes (~1MB).
    //bitpacking: ?: can we store x in lower 4 bits and y in upper 4 bits?
    //255 values, x = 0-127, y = 0-127. probably not necessary but might be nice for file saves
    
    public long seed;
    public float[][] heightMap;
    public int[][] tileMap;
    public int[][] pixelatedTileMap;
}

