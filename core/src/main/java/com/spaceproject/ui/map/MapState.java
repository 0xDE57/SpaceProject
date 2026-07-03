package com.spaceproject.ui.map;

public enum MapState {
    off,
    mini,
    full;
    
    private static final MapState[] VALUES = values();
    
    public MapState next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }
}
