package com.spaceproject.config;


public class EntityConfig extends Config {
    //TODO: find better way to do entity config + generation

    //character
    public float characterHealth;
    public float characterWalkSpeed;
    
    //control
    public long dodgeCooldown;
    public long controlTimerVehicle;
    
    //ship
    public int shipSizeMin;
    public int shipSizeMax;
    public float shipHealth;
    public float engineThrust;
    public float hyperSpeed;
    public float dodgeForce;
    public long dodgeAnimationTimer;
    public int cannonSize;
    public int cannonAmmo;
    public int cannonDamage;
    public long cannonFireRate;
    public long cannonRechargeRate;
    public float cannonVelocity;
    public long shrinkGrowAnimTime;
    
    
    
    @Override
    public void loadDefault() {
        characterHealth = 100;
        characterWalkSpeed = 200f;
        
        controlTimerVehicle = 1000;
        
        shipSizeMin = 10/2;
        shipSizeMax = 36/2;
        shipHealth = 200;
        engineThrust = 1000;
        hyperSpeed = 2000;
        dodgeCooldown = 500;
        dodgeAnimationTimer = 475;
        dodgeForce = 8f;
        cannonSize = 1;
        cannonAmmo = 5;
        cannonDamage = 5;
        cannonFireRate = 200;
        cannonRechargeRate = 1000;
        cannonVelocity = 80;
        shrinkGrowAnimTime = 2500;
    }
    
}
