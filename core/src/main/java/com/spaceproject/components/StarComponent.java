package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;

public class StarComponent implements Component {
    
    public float radius;
    
    //public double mass;
    
    //public double luminosity;
    
    //temperature of star typically (2,000K - 40,000K)
    public double temperature; //K
    
    public double peakWavelength; //nanometers
    
    public Color colorTemp; // Black Body Radiation!
    
    //public long age; //years

    enum StellarClass {
        redDwarf,
        yellowDwarf, //G-star
        whiteDwarf,
        brownDwarf,
        blackDwarf,
        lowMass,
        massive,
        redGiant,
        redSuperGiant,
        hyperGiant,
        neutron,
        pulsar
    }

    enum SpectralClass {
        O, // ≥ 30,000 K
        B, //10,000–30,000 K
        A, //7,500–10,000 K
        F, //6,000–7,500 K
        G, //5,200–6,000 K
        K, //3,700–5,200 K
        M, //2,400–3,700 K
        L,
        T
    }
    
}
