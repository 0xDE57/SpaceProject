package com.spaceproject.config;

public class WorldConfig extends Config {

	/*
	minMapSize, maxMapSize
	minScale, maxScale
	minOctaves, maxOctave
	minPersistence, maxPersistence
	minLacunarity, maxLacunarity

	//playing with ideas:
	//planetary/stellar features
	public double mass;
	public double volume;
	public double density;
	public float averageTemp;
	//public object[] composition; //eg: hydrogen, helium, oxygen, carbon, etc..
	//public object absoluteMagnitude; //measure of the luminosity of a celestial object, on a logarithmic astronomical magnitude scale
	public float gravity;
	public String[] classifications;


	//properties of sun
	public final String solarSpectralClass = "GV2";
	public final double solarMass = 2e30;//1.988435×10^30 kg
	public final int solarRadii  = 695700;//km
	public final int solarTemp = 5700;//K
	*/
    
    public int tileSize;
    public int chunkSize;
    
    @Override
    public void loadDefault() {
        tileSize = 2;
        chunkSize = 8;
    }
}
