package com.spaceproject.config;


import com.badlogic.gdx.graphics.Color;
import com.spaceproject.SpaceProject;
import com.spaceproject.ui.map.MiniMapPosition;

public class MiniMapConfig extends Config {
    
    public MiniMapPosition miniMapPosition;
    public float defaultMapScale;
    public int gridSize;
    public int borderWidth;
    public int playerMarkerSize;
    public int celestialMarkerSize;
    public int lodRenderOrbitPathScale;
    public int edgePad;
    public int miniEdgePad;
    public int miniWidth;
    public int miniHeight;
    public int zoomMultiplier;
    public float minScale;
    public float maxSale;
    public long drawScaleTimer;
    public int fontSize;
    public boolean debugDisableClipping;
    public boolean debugDrawLoadDist;
    
    public Color backingColor;
    public Color borderColor;
    public Color gridColor;
    public Color mouseColor;
    public Color viewportColor;
    public Color universeMarkerColor;
    public Color orbitPathColor;
    public Color playerMarkerColor;
    public Color velocityVecColor;
    public Color orientationColor;
    public Color debugLoadDistColor;
    
    
    public void loadDefault() {
        miniMapPosition = MiniMapPosition.topRight;

        defaultMapScale = 200;
        gridSize = (int) Math.pow(2, 17 - 1);
        borderWidth = 1;
        playerMarkerSize = 3;
        celestialMarkerSize = 6;
        lodRenderOrbitPathScale = 500;
        edgePad = 50;
        miniEdgePad = 10;
        miniWidth = 320;
        miniHeight = 240;
        zoomMultiplier = 4;
        minScale = 0.5f;
        maxSale = 10000;
        drawScaleTimer = 5000;
        fontSize = 12;
        debugDisableClipping = false;
        debugDrawLoadDist = false;
        
        backingColor = new Color(0, 0, 0, 0.9f);
        borderColor = new Color(0.1f, 0.63f, 0.88f, 1f);
        gridColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
        mouseColor = new Color(1f, 0.2f, 0.2f, 1f);
        viewportColor = new Color(0.07f, 0.45f, 0.64f, 1f);
        universeMarkerColor = new Color(1, 1, 1, 1);
        orbitPathColor = new Color(0.5f, 0.5f, 0.5f, 0.5f);
        playerMarkerColor = new Color(1, 1, 1, 1);
        velocityVecColor = new Color(1, 0, 0, 1);
        orientationColor = new Color(0.63f, 0.63f, 0.63f, 0.4f);
        debugLoadDistColor = new Color(1, 0, 0, 1);
    }
}
