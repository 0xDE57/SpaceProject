package com.spaceproject.config;

import com.badlogic.gdx.Input.Keys;

public class KeyConfig extends Config {
    //Todo: controls shouldn't be limited to keys. what about mapping to mouse and keyboard or controller
    
    //---player controls---
    public int forward;
    public int right;
    public int left;
    public int back;
    public int boost;
    public int dash;
    public int switchWeapon;
    public int activateShield;
    public int changeVehicle;
    public int interact;
    public int activateHyperDrive;
    
    //---UI controls---
    public int toggleHUD;
    public int toggleEdgeMap;
    public int toggleSpaceMap;
    
    //---screen controls---
    public int fullscreen;
    public int vsync;
    public int msaa;
    public int zoomOut;
    public int zoomIn;
    public int resetZoom;
    public int rotateLeft;
    public int rotateRight;
    
    //---debug menu controls---
    public int toggleDebug;
    public int toggleEngineViewer;
    public int togglePos;
    public int toggleComponents;
    public int toggleBounds;
    public int toggleFPS;
    public int toggleOrbit;
    public int toggleVector;
    public int toggleMenu;
    
    
    
    public void loadDefault() {
        //player
        forward = Keys.W;
        right = Keys.D;
        left = Keys.A;
        back = Keys.S;
        boost = Keys.SPACE;
        dash = Keys.SPACE;
        switchWeapon = Keys.Q;
        activateShield = Keys.SHIFT_LEFT;
        changeVehicle = Keys.G;//E ?
        interact = Keys.E;
        activateHyperDrive = Keys.NUM_1;
        
        //ui
        toggleHUD = Keys.H;
        toggleEdgeMap = Keys.N;
        toggleSpaceMap = Keys.M;
        
        //screen
        fullscreen = Keys.F11;
        vsync = Keys.F8;
        msaa = Keys.F7;
        zoomOut = Keys.MINUS;
        zoomIn = Keys.EQUALS;
        resetZoom = Keys.PERIOD;
        rotateLeft = Keys.LEFT_BRACKET;
        rotateRight = Keys.RIGHT_BRACKET;
        
        //debug menu
        toggleDebug = Keys.F3;
        toggleEngineViewer = Keys.F9;
        togglePos = Keys.NUMPAD_0;
        toggleComponents = Keys.NUMPAD_1;
        toggleBounds = Keys.NUMPAD_2;
        toggleFPS = Keys.NUMPAD_3;
        toggleOrbit = Keys.NUMPAD_4;
        toggleVector = Keys.NUMPAD_5;
        toggleMenu = Keys.NUMPAD_9;
    }
    
}
