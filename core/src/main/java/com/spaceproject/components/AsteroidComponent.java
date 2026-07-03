package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

import java.util.HashMap;

public class AsteroidComponent implements Component {

    //public enum Composition { rock, ice } todo: different asteroid types?
    /*todo: research C-type, S-Type, M-Type
    1. Composition-based Classification: Asteroids can be broadly classified into three main compositional groups:

    C-type Asteroids (Carbonaceous):
        These asteroids are rich in carbon compounds, such as organic molecules, and water ice.
        They often contain silicate minerals and may have a dark, reddish appearance.
        C-type asteroids are potential sources of water, organic materials, and volatile elements, which can be valuable for space exploration and colonization.

    S-type Asteroids (Silicaceous):
        S-type asteroids are primarily composed of silicate minerals, including pyroxenes and olivines.
        They tend to be brighter and more reflective than C-type asteroids.
        Some S-type asteroids may contain metals like nickel and iron, which could be of interest for mining.

    M-type Asteroids (Metallic):
        M-type asteroids are mainly composed of metallic elements, with iron and nickel being predominant.
        These asteroids are considered excellent candidates for asteroid mining due to their high metal content.
        Some M-type asteroids may also contain valuable metals like platinum, gold, and rare earth elements.



    HED meteorites are broadly divided into:
        Howardites
        Eucrites
        Diogenites

    Albedo: The albedo, or the reflectivity, of an asteroid's surface can provide clues about its composition. Bright asteroids (high albedo) are often S-types, while dark asteroids (low albedo) are typically C-types.

    Spectroscopy to analyze reflected light from asteroids. Helps determine composition based on absorption and emission lines in their spectra.

    SMASS, Tholen

    A-
    B-
    C: Carbonaceous
    D, D-
    E
    F
    J
    K
    L
    M: Metallic
    O
    P
    Q, Q-
    R
    S, Sa, Sk, Sl, Sq, Sr: Silicaceous
    T
    V: Vestoid / Vesta?
    X, Xe, Xc,Xk

    */
    public enum Classification {
        C, S, M
    }

    public Classification classification;

    /*
    Composition[][] {
        "Fe", 36%,
        "Co", 14%,
        "H2O", 2%,
    }*/
    //public HashMap<Integer, Float> composition = new HashMap<>();//resource ID, percent [0-1]
    public ItemComponent.Resource composition;
    
    public Entity parentOrbitBody;
    
    public Polygon polygon;
    
    public Vector2 centerOfMass;
    
    public float area;
    
    public Color color;

    public long lastShieldHit;

    public boolean revealed;

    public float albedo;

    public float refractiveIndex; //eg: vacuum = 1, Diamond = 2.4

}
