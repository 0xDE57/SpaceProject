package com.spaceproject.ui.map;

public enum MiniMapPosition {
    topLeft,
    topRight,
    bottomRight,
    bottomLeft;
    //custom;
    
    private static final MiniMapPosition[] VALUES = values();
    
    public MiniMapPosition next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }
}
