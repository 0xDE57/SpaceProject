package com.spaceproject.config;

import com.badlogic.gdx.graphics.Color;

public class UIConfig extends Config {

    public Color uiBaseColor;

    public int playerHPBarY;
    public int playerHPBarWidth;
    public int playerHPBarHeight;
    public Color playerAmmoBarColor;
    public Color playerAmmoBarRechargeColor;

    public Color engineFire;
    public Color engineBoost;

    public boolean renderFullHealth;
    public int entityHPbarLength;
    public int entityHPbarWidth;
    public int entityHPbarYOffset;
    public Color entityHPbarBackground;
    public float entityHPbarOpacity;
    
    public Color orbitObjectColor;
    public Color orbitSyncPosColor;
    public float lodShowOrbitPath;
    public int orbitFadeFactor;
    
    @Override
    public void loadDefault() {
        uiBaseColor = new Color(0.1f, 0.63f, 0.88f, 1f);

        playerHPBarY = 55;
        playerHPBarWidth = 300;
        playerHPBarHeight = 20;
        playerAmmoBarColor = Color.MAGENTA;
        playerAmmoBarRechargeColor = Color.SLATE;

        engineFire = Color.GOLD;
        engineBoost = Color.CYAN;
        
        renderFullHealth = false;
        entityHPbarLength = 40;
        entityHPbarWidth = 8;
        entityHPbarYOffset = -20;
        entityHPbarBackground = new Color(1, 1, 1, 0.5f);
        entityHPbarOpacity = 0.7f;
        
        orbitObjectColor = new Color(1, 1, 1, 1);
        orbitSyncPosColor = new Color(1, 0, 0, 1);
        lodShowOrbitPath = 150;
        orbitFadeFactor = 2;
    }
}
