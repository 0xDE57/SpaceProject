package com.spaceproject.utility;

public class TestScaleThing {
    /*
    public static final float meterPerUnit = 0.1f;
    public static final int pixelPerUnit = 2;
    
    public static float PixelToMeter(int pixels) {
      return 0;
    }
    
    public static float PixelToUnit(int pixels) {
        return pixels / pixelPerUnit;
    }
    
    public static int UnitToPixel(int units) {
        return units * pixelPerUnit;
    }
    
    public static float MeterToUnit(float meters) {
        return meters / meterPerUnit;
    }
    
    public static float UnitToMeter(int unit) {
        return unit * meterPerUnit;
    }
    */
    
    //render size = define objects in units, don't care about pixel count. set image to take up units,
    //bullet = 1 unit = 0.1m
    //wall = 2 unit
    /*
    consider max steps eg 60*2 = 120m/s or 432km/h units.
    body size works best between 0.1 - 10m
    10px/1m, 1px = 0.1m,  min image size = 1px, max = (
    16px/1m, 1px = 0.0625m, min image size = 2px, max = , speed
    1 unit = 0.1m,
    2px/unit, 1 unit = (0.0625*2) = 0.125m
        ship = 16*26 unit, 32*52px,
        player = 4*4 unit, 8*8px,
        bullet = 1*1 unit, 2*2px,
        
        
    
    reference diameter = 5,000km - 150,000km
    //reference distance to star = 50,000,000km - 10,000,000,000km
    //playscale diameter (multiplier) = 0.01
    //playscale distance = 0.0001
    //physicsScale = reference * playscale * [whatever makes sense]
    
    scale = 4
    player = 0.5m x 0.5m? 4x4px * 4 = 16px
    default seed 0 = 14x7 * 4 = 56x28
    ~13 ships across @ 1080
    ~21 chars across @ 1080 (ZOOMED 0.5 cuz left ship), so * 2 = ~42 @ 1x zoom
    ship = 1m x 3m
    roughly 13 ships across so 40m height
    
    
     */
}
