package com.spaceproject.config;

public enum RenderOrder {
    //note: mix of 2D & 3D sprites will render in separate batches,
    //so 3D sprites currently run on top of 2D regardless of this order
    ASTRO((byte)-200), //stars, planets, moons, should always be in very back / behind
    WORLD_OBJECTS((byte)-100),
    VEHICLES((byte)50),
    CHARACTERS((byte)100),
    PROJECTILES((byte)200);
    
    private final byte hierarchy;
    
    RenderOrder(final byte hierarchy) {
        this.hierarchy = hierarchy;
    }
    
    public byte getHierarchy() {
        return hierarchy;
    }
}
