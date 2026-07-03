package com.spaceproject.math;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Disclaimer: I am not a physicist. There may be errata but I will do my best.
 * Sources:
 *      https://en.wikipedia.org/wiki/Black-body_radiation
 *      https://en.wikipedia.org/wiki/Planck%27s_law
 *      https://en.wikipedia.org/wiki/Wien%27s_displacement_law
 *      https://en.wikipedia.org/wiki/Rayleigh%E2%80%93Jeans_law
 *      https://en.wikipedia.org/wiki/Stefan%E2%80%93Boltzmann_law
 *      https://en.wikipedia.org/wiki/Stellar_classification
 *
 *      https://www.fourmilab.ch/documents/specrend/
 *      https://en.wikipedia.org/wiki/CIE_1931_color_space#Color_matching_functions
 *
 *      Wolfram Alpha for testing and confirming formulas and values.
 *      https://www.wolframalpha.com/widgets/view.jsp?id=5072e9b72faacd73c9a4e4cb36ad08d
 *
 *      Also found a tool that simulates:
 *      https://phet.colorado.edu/sims/html/blackbody-spectrum/latest/blackbody-spectrum_en.html
 */
public class Physics {
    
    // ------ Universal Constants ------
    // c: Speed of light: 299,792,458 (meters per second)
    public static final long speedOfLight = 299792458; //m/s
    
    // h: Planck's constant: 6.626 × 10^-34 (Joule seconds)
    public static final double planckConstant = 6.626 * Math.pow(10, -34); //Js
    public static final BigDecimal planckBig = new BigDecimal("6.62607015").movePointLeft(34);
    
    // h*c: precalculated planckConstant * speedOfLight = 1.98644586...× 10^−25 J⋅m
    public static final double hc = planckConstant * speedOfLight;
    public static final BigDecimal hcBig = new BigDecimal("1.98644586").movePointLeft(25);
    public static final BigDecimal hcCalculated = planckBig.multiply(new BigDecimal(speedOfLight));
    
    // k: Boltzmann constant: 1.380649 × 10^-23 J⋅K (Joules per Kelvin)
    public static final double boltzmannConstant = 1.380649 * Math.pow(10, -23); //JK
    public static final BigDecimal boltzmannBig = new BigDecimal("1.380649").movePointLeft(23); //JK
    
    // b: Wien's displacement constant: 2.897771955 × 10−3 m⋅K,[1] or b ≈ 2898 μm⋅K
    public static final double wiensConstant = 2.8977719; //mK
    
    // G: Gravitational constant: 6.674×10−11 Nm^2 / kg^2 (newton square meters per kilogram squared)
    public static final double gravitationalConstant = 6.674 * Math.pow(10, -11);
    
    // ? : not sure what to call this, but it doesn't change so we can precalculate it
    public static final double unnamedConstant = (2 * planckConstant * Math.pow(speedOfLight, 2));
    
    /** Wien's displacement law: λₘT = b
     * Hotter things - peak at shorter wavelengths - bluer
     * Cooler things - peak at longer wavelengths - redder
     * λₘ = The maximum wavelength in nanometers corresponding to peak intensity
     * T  = The absolute temperature in kelvin
     * b  = Wein’s Constant: 2.88 x 10-3 m-K or 0.288 cm-K
     */
    public static double temperatureToWavelength(double kelvin) {
        return wiensConstant / kelvin;
    }
    
    /** T = b / λₘ */
    public static double wavelengthToTemperature(double wavelength) {
        return wiensConstant / wavelength;
    }
    
    /** ν = c / λ
     * ν = frequency (hertz)
     * λ = wavelength (nanometers)
     * c = speed of light
     */
    public static double wavelengthToFrequency(double wavelength) {
        return speedOfLight / wavelength;
    }
    
    /** E = (h * c) / λ
     * E = energy
     * λ = wavelength (nanometers)
     * h = planck constant
     * c = speed of light
     */
    public static double wavelengthToPhotonEnergy(double wavelength) {
        //energy = (planckConstant * speedOfLight) / wavelength;
        return hc / wavelength;
    }
    
    /** E = hv
     * E = energy
     * v = frequency (hertz)
     * h = planck constant
     */
    public static double frequencyToPhotonEnergy(double frequency) {
        //energy = planckConstant * frequency;
        return  planckConstant * frequency;
    }
    
    
    /** Rayleigh–Jeans law: uν = (8 * π * (ν^2) * k * T) / (c^2)
     * Note: formula fits low frequencies but fails increasingly for higher frequencies.
     * see: "ultraviolet catastrophe"
     * uν =
     * v = frequency (hertz)
     * k = Boltzmann's constant
     * T = is the absolute temperature of the radiating bod
     * c = speed of light
     */
    public static double RayleighJeansLaw(int wavelength) {
        double frequency = wavelengthToFrequency(wavelength);
        double temperature = wavelengthToTemperature(wavelength);
        return (8 * Math.PI * Math.pow(frequency, 2) * boltzmannConstant * temperature) / Math.pow(speedOfLight, 2);
    }
    
    /** Planck's law of black-body radiation: L(λ) = (2 h c^2) / (λ^5 (e^((h c)/(λ k T)) - 1))
     * L(λ) = spectral radiance as function of wavelength
     * λ = wavelength
     * T = temperature of the body Kelvin
     * h = Planck constant (≈ 6.626×10^-34 J s)
     * c = speed of light (≈ 2.998×10^8 m/s)
     * k  Boltzmann constant (≈ 1.381×10^-23 J/K)
     */
    public static double calcSpectralRadiance(int wavelength, double temperature) {
        //L(λ) = (2 h c^2) / (λ^5 (e^((h c)/(λ k T)) - 1))
        //L = (2 * planckConstant * (speedOfLight ^ 2)) /
        //((wavelength ^ 5) * (Math.E ^ ( ((planckConstant * speedOfLight) / (wavelength * boltzmannConstant * temperature)) - 1)));
        
        //break down
        //double unnamedConstant = (2.0 * planckConstant * Math.pow(speedOfLight, 2));//(2 h c^2)
        //double hc = planckConstant * speedOfLight;
        double a = wavelength * boltzmannConstant * temperature;
        double b = Math.exp(hc / a) - 1; //(e^((h c)/(λ k T)) - 1)
        return unnamedConstant / (Math.pow(wavelength, 5) * b);
    }
    
    
    public static void calculateBlackBody(int wavelengthStart, int wavelengthEnd, double temperature) {
        //double temperature = 5772;// wavelengthToTemperature(502);
        
        for (int wavelength = wavelengthStart; wavelength <= wavelengthEnd; wavelength++) {
            //we can kinda ignore rayleigh jean as we know it will produce incorrect values, just testing
            //double r = RayleighJeansLaw(wavelength);
            //Gdx.app.debug("Raleigh Jean    ", String.format("%s - %g", wavelength, r));
            
            double spectralRadiance = calcSpectralRadiance(wavelength, temperature);
            Gdx.app.debug("spectralRadiance", String.format("%s - %g", wavelength, spectralRadiance));
            
            //just a test: i don't think we have a precision issue...
            //BigDecimal spectralBigDecimal = calcSpectralRadianceBig(wavelength);
            //Gdx.app.debug("spectral precise", wavelength + " - " + spectralBigDecimal.toPlainString());
        }
        //expected output: 2.19308090702e+13
        // 5772k, 502nm
        // Radiant emittance: 	    6.29403e+07 W/m2
        // Radiance: 	            2.00345e+07 W/m2/sr
        // Peak spectral radiance:  2.19308090702e+13 (W*sr-1*m-3)
        //                          26239737.802334465 (W/m2-sr-um)
        // Spectral Radiance: 	4207.38 W/m2/sr/µm  (5.03412e+19 photons/J)
        //
        
        //current broken outputs:
        //  [Raleigh Jean    ] 502 - 7.94834e-30
        //  [spectralRadiance] 502 - 1.01051e-29
        //  [spectral precise] 502 - 0.000000000000000000000000000010105
        //502 - 1.17864e+13
        //1.01051e-29
        //7.50587e-28
        //7.52394e-22
    }
    
    /** approximate RGB [0-255] values for wavelengths between 380 nm and 780 nm
     * Ported from: RGB VALUES FOR VISIBLE WAVELENGTHS by Dan Bruton (astro@tamu.edu)
     * http://www.physics.sfasu.edu/astro/color/spectra.html
     */
    public static int[] wavelengthToRGB(double wavelength, double gamma) {
        double factor;
        double red, green, blue;
        
        if ((wavelength >= 380) && (wavelength < 440)) {
            red = -(wavelength - 440) / (440 - 380);
            green = 0.0;
            blue = 1.0;
        } else if ((wavelength >= 440) && (wavelength < 490)) {
            red = 0.0;
            green = (wavelength - 440) / (490 - 440);
            blue = 1.0;
        } else if ((wavelength >= 490) && (wavelength < 510)) {
            red = 0.0;
            green = 1.0;
            blue = -(wavelength - 510) / (510 - 490);
        } else if ((wavelength >= 510) && (wavelength < 580)) {
            red = (wavelength - 510) / (580 - 510);
            green = 1.0;
            blue = 0.0;
        } else if ((wavelength >= 580) && (wavelength < 645)) {
            red = 1.0;
            green = -(wavelength - 645) / (645 - 580);
            blue = 0.0;
        } else if ((wavelength >= 645) && (wavelength < 781)) {
            red = 1.0;
            green = 0.0;
            blue = 0.0;
        } else {
            red = 0.0;
            green = 0.0;
            blue = 0.0;
        }
        
        // Let the intensity fall off near the vision limits
        if ((wavelength >= 380) && (wavelength < 420)) {
            factor = 0.3 + 0.7 * (wavelength - 380) / (420 - 380);
        } else if ((wavelength >= 420) && (wavelength < 701)) {
            factor = 1.0;
        } else if ((wavelength >= 701) && (wavelength < 781)) {
            factor = 0.3 + 0.7 * (780 - wavelength) / (780 - 700);
        } else {
            factor = 0.0;
        }
        
        // Don't want 0^x = 1 for x <> 0
        final double intensityMax = 255;
        int[] rgb = new int[3];
        rgb[0] = red   == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(red * factor, gamma));
        rgb[1] = green == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(green * factor, gamma));
        rgb[2] = blue  == 0.0 ? 0 : (int)Math.round(intensityMax * Math.pow(blue * factor, gamma));
        return rgb;
    }
    
    /** approximate RGB [0-255] values for wavelengths between 380 nm and 780 nm with a default gamma of 0.8 */
    public static int[] wavelengthToRGB(double wavelength) {
        return wavelengthToRGB(wavelength, 0.8);
    }
    
    public static void test() {
        /* Black Body Radiation!
         * Common color temperatures (Kelvin):
         *      1900	Candle flame
         *      2000	Sunlight at sunset
         *      2800	Tungsten bulb—60 watt
         *      2900	Tungsten bulb—200 watt
         *      3300	Tungsten/halogen lamp
         *      3780	Carbon arc lamp
         *      5500	Sunlight plus skylight
         *      5772    Sun "effective temperature"
         *      6000	Xenon strobe light
         *      6500	Overcast sky
         *      7500	North sky light
         *
         *  Harvard spectral classification
         *      O	≥ 33,000 K 	        blue
         *      B 	10,000–33,000 K 	blue white
         *      A 	7,500–10,000 K 	    white
         *      F 	6,000–7,500 K 	    yellow white
         *      G 	5,200–6,000 K 	    yellow
         *      K 	3,700–5,200 K 	    orange
         *      M 	2,000–3,700 K 	    red
         *      R 	1,300–2,000 K 	    red
         *      N 	1,300–2,000 K 	    red
         *      S 	1,300–2,000 K 	    red
         */
    
        //Known sun values: 5772K | 502nm | 597.2 terahertz | 2.47 eV
        double kelvin = Sun.kelvin; //5772
        double expectedWavelength = 502;
        double expectedFrequency = 597.2;
        double expectedEnergy = 2.47;
        double calculatedWavelength = temperatureToWavelength(kelvin);
        double calculatedTemperature = wavelengthToTemperature(expectedWavelength);
        double calculatedFrequency = wavelengthToFrequency(expectedWavelength);
        Gdx.app.debug("PhysicsDebug", kelvin + " K = " + MyMath.round(calculatedWavelength * 1000000, 1) + " nm");
        Gdx.app.debug("PhysicsDebug", expectedWavelength + " nm = " + MyMath.round(calculatedTemperature * 1000000, 1) + " K");
        Gdx.app.debug("PhysicsDebug", "temp(wave(" + kelvin + ")) = " + wavelengthToTemperature(calculatedWavelength));
        Gdx.app.debug("PhysicsDebug", "wave(temp(" + expectedWavelength +")) = " + temperatureToWavelength(calculatedTemperature));
        Gdx.app.debug("PhysicsDebug", expectedWavelength + " nm = " + MyMath.round(calculatedFrequency / 1000, 1) + " THz");
        
        Gdx.app.debug("PhysicsDebug", "wavelength expected: " + MathUtils.isEqual((float)calculatedWavelength * 1000000, (float) expectedWavelength, 0.1f));
        Gdx.app.debug("PhysicsDebug", "temperature expected: " + MathUtils.isEqual((float)calculatedTemperature * 1000000, (float) kelvin, 0.5f));
        //Gdx.app.debug("PhysicsDebug", "frequency expected: " + MathUtils.isEqual((float)calculatedFrequency , (float) expectedFrequency, 0.1f));
        
        
        //todo: photon energy calculations are returning 3.95706346613546E-28, expecting 2.47 eV
        // bug: planck is coming out as -291.54400000000004. expected: 6.626 * (10 ^ -34)
        // update, turns out i need to use math.pow, not ^ [^ = Bitwise exclusive OR] ....i'm a little rusty
        // have we hit the Double.MIN_EXPONENT...is there a precision bug or is my math wrong?
        // frequencyToPhotonEnergy:  502.0 nm = 3.9570634604560330026360810734331607818603515625E-28 eV
        // wavelengthToPhotonEnergy: 502.0 nm = 3.95706346613546E-28 eV
        // expected: 2.47 eV
        // photonEnergy 0.000000000000000000000000198644586 precision: 47  - scale: 74
        Gdx.app.debug("PhysicsDebug", "planck double: " + planckConstant);
        Gdx.app.debug("PhysicsDebug", "size of double: [" + Double.MIN_VALUE + " to " + Double.MAX_VALUE
                + "] exp: [" + Double.MIN_EXPONENT + " to " + Double.MAX_EXPONENT + "]");
        //high precision big decimals
        Gdx.app.debug("PhysicsDebug","planck bigdecimal: " + planckBig.toString() + " -> " + planckBig.toPlainString()
                + " | precision: " + planckBig.precision() + "  - scale: " + planckBig.scale());
        Gdx.app.debug("PhysicsDebug","h * c def:  " + hcBig.toPlainString()
                        + " | precision: " + hcBig.precision() + "  - scale: " + hcBig.scale());
        Gdx.app.debug("PhysicsDebug","h * c calc: " + hcCalculated.toString() + " -> " + hcCalculated.toPlainString()
                + " | precision: " + hcCalculated.precision() + "  - scale: " + hcCalculated.scale());
        
        
        //BigDecimal photonEnergy = frequencyToPhotonEnergy(calculatedFrequency);
        //Gdx.app.debug("PhysicsDebug", expectedWavelength + " nm = " + photonEnergy.toString() + " eV -> " + hcBig.toPlainString() +  " | precision: " + photonEnergy.precision() + "  - scale: " + photonEnergy.scale());
        double photonEnergy = frequencyToPhotonEnergy(calculatedFrequency);
        Gdx.app.debug("PhysicsDebug", expectedWavelength + " nm = " + photonEnergy + " eV ");
        Gdx.app.debug("PhysicsDebug", expectedWavelength + " nm = " + wavelengthToPhotonEnergy(expectedWavelength) + " eV");
        
    
        /* A typical human eye will respond to wavelengths from about 380 to about 750 nanometers.
         * Tristimulus values: The human eye with normal vision has three kinds of cone cells that sense light, having peaks of spectral sensitivity in
         *      short   420 nm – 440 nm
         *      middle  530 nm – 540 nm
         *      long    560 nm – 580 nm
         *
         * Typical color ranges:
         *      Color   Wavelength(nm) Frequency(THz)
         *      Red     620-750        484-400
         *      Orange  590-620        508-484
         *      Yellow  570-590        526-508
         *      Green   495-570        606-526
         *      Blue    450-495        668-606
         *      Violet  380-450        789-668
         */
        double gamma = 0.8;
        int red = 650;
        int green = 540;
        int blue = 470;
        Gdx.app.debug("PhysicsDebug",  expectedWavelength + " -> " + Arrays.toString(wavelengthToRGB(expectedWavelength, gamma)));
        Gdx.app.debug("PhysicsDebug",  red + " -> " + Arrays.toString(wavelengthToRGB(red, gamma)));//red-ish
        Gdx.app.debug("PhysicsDebug",  green + " -> " + Arrays.toString(wavelengthToRGB(green, gamma)));//green-ish
        Gdx.app.debug("PhysicsDebug",  blue + "  -> " + Arrays.toString(wavelengthToRGB(blue, gamma)));//blue-ish
        
        //wavelengthToRGB() approximates 380 nm and 780 nm
        int rgbMinWavelength = 380;
        int rgbMaxWavelength = 780;
        double lowestVisibleTemperature = wavelengthToTemperature(rgbMinWavelength);
        double highestVisibleTemperature = wavelengthToTemperature(rgbMaxWavelength);
        Gdx.app.debug("PhysicsDebug", "380nm to 780nm = " + MyMath.round(lowestVisibleTemperature * 1000000, 1)
                + "K" + " to " + MyMath.round(highestVisibleTemperature * 1000000, 1) + "K");
        Gdx.app.debug("PhysicsDebug",  rgbMinWavelength + "nm " + MyMath.round(lowestVisibleTemperature, 1) + "K -> " + Arrays.toString(wavelengthToRGB(rgbMinWavelength, gamma)));
        Gdx.app.debug("PhysicsDebug",  rgbMaxWavelength + "nm " + MyMath.round(highestVisibleTemperature, 1) + "K -> " + Arrays.toString(wavelengthToRGB(rgbMaxWavelength, gamma)));
    
        
        //calculateBlackBody(rgbMinWavelength, rgbMaxWavelength, kelvin);
        //calculateBlackBody(380, 400);
        
        BlackBodyColorSpectrum.test();
    
        // 5772 K   -> xyz 0.3266 0.3359 0.3376     -> rgb 1.000 0.867 0.813
        Vector3 spectrum = BlackBodyColorSpectrum.spectrumToXYZ(kelvin);
        Vector3 color = BlackBodyColorSpectrum.xyzToRGB(BlackBodyColorSpectrum.SMPTEsystem, spectrum.x, spectrum.y, spectrum.z);
        String xyzTemp = String.format("  %5.0f K      %.4f %.4f %.4f   ", kelvin, spectrum.x, spectrum.y, spectrum.z);
        if (BlackBodyColorSpectrum.constrainRGB(color)) {
            Vector3 normal = BlackBodyColorSpectrum.normRGB(color.x, color.y, color.z);
            Gdx.app.log("PhysicsDebug", xyzTemp + String.format("%.3f %.3f %.3f (Approximation)", normal.x, normal.y, normal.z));
            //Gdx.app.log(this.getClass().getSimpleName(), xyzTemp + String.format("%.3f %.3f %.3f (Approximation)", color.z, color.y, color.z));
        } else {
            Vector3 normal = BlackBodyColorSpectrum.normRGB(color.x, color.y, color.z);
            //Gdx.app.log(this.getClass().getSimpleName(), xyzTemp + String.format("%.3f %.3f %.3f", color.x, color.y, color.z));
            Gdx.app.log("PhysicsDebug", xyzTemp + String.format("%.3f %.3f %.3f", normal.x, normal.y, normal.z));
        }
        
    }
    
    public static class Sun {
        public static final String spectralClass = "GV2 (main sequence)";
        
        //mass: nominal solar mass parameter: GM⊙ = 1.3271244 × 10^20 m3 s−2 or 1.9885 × 10^30 kg.
        public static final double mass = 1.9885 * Math.pow(10, 30);//kg
        
        //radius: nominal solar radius 	R⊙ = 6.957 × 10^8 m
        public static final double radius = 6.957 * Math.pow(10, 8);//m
        
        //effective temperature
        public static final double kelvin = 5772; //K
        
        //5772K = 502nm = 597 THz = green light
        public static final double peakWavelength = temperatureToWavelength(kelvin) * 1000000;
        
        //luminosity: 1 sol -> L⊙ = nominal solar luminosity: 	L⊙ = 3.828 × 10^26 W
        public static final double luminosity = 3.828 * Math.pow(10, 26); //Watts
        
        //public static final age = 4.78 billion years
        
        //AU Astronomical unit: roughly the distance from Earth to the Sun ~1.495978707 × 10^11 m
        public static final long astronomicalUnit = 149597870700L;
    }
    
}
