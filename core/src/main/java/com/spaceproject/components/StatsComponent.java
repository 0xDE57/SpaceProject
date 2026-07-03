package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class StatsComponent implements Component {

    //stats
    //public long timeAlive;
    //todo: kills, deaths, resourcesCollected, creditsEarned, creditsSpent, etc...
    // should it collect per life and total on death?
    public long distanceTraveled;

    public int kills; //this life;
    //public int killsTotal; //across all lives?
    public int deaths;

    public long shotsFired;//total?

    public long shotsHit;

    public float damageDealt;

    public float damageTaken;

    public long resourcesCollected;

    //public long resourcesSpent;

    public long resourcesLost;

    public long profit;

}
