package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

/**
 * The barycenter is the center of mass of two or more bodies that orbit each other and is the point about which the bodies orbit.
 * in our use case, this will be the "celestial anchor" in where an entity holding this component will
 * act as the anchor for other entities to orbit around
 */
public class BarycenterComponent implements Component {
    
    public AstronomicalBodyType bodyType;
    
    public enum AstronomicalBodyType {
        uniStellar,
        multiStellar,
        roguePlanet,
        loneStar
    }
}
