package com.spaceproject.generation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.CelestialConfig;
import com.spaceproject.screens.GameScreen;


public class Galaxy {
    
    private static CelestialConfig celestCFG = SpaceProject.configManager.getConfig(CelestialConfig.class);
    public Array<Vector2> points;
    public Array<AstroBody> objects = new Array<>();
    
    
    public Galaxy() {
        this(generatePoints(GameScreen.getGalaxySeed(), celestCFG.numPoints, celestCFG.pointGenRange, celestCFG.minPointDistance));
    }
    
    public Galaxy(Array<Vector2> points) {
        for (Vector2 p : points) {
            objects.add(new AstroBody(p));
        }
        this.points = points;
        
        if(!GameScreen.isDebugMode) {
            saveToJson();
        }
    }

    private static Array<Vector2> generatePoints(long seed, int numStars, int genRange, float dist) {
        // Apparently this is called "Blue Noise" or Poisson Disk Sampling.
        // it suffers an issue where as the graph becomes denser with points, the harder it is to insert a new point
        // and also has to check more points as n increases
        //  https://www.youtube.com/watch?v=Ge3aKEmZcqY
        MathUtils.random.setSeed(seed);
        Array<Vector2> points = new Array<>();
        
        dist *= dist;//squared for dst2
        
        for (int i = 0; i < numStars; i++) {
            Vector2 newPoint;
            
            boolean reGen = false; // flag for if the point is needs to be regenerated
            boolean fail = false; // flag for when to give up generating a point
            int fails = 0; // how many times a point has been regenerated
            do {
                // create point at random position
                int x = MathUtils.random(-genRange, genRange);
                int y = MathUtils.random(-genRange, genRange);
                newPoint = new Vector2(x, y);
                
                // check for collisions
                reGen = false;
                for (int j = 0; j < points.size && !reGen; j++) {
                    // if point is too close to other point; regenerate
                    if (newPoint.dst2(points.get(j)) <= dist) {
                        reGen = true;
                        
                        // if too many tries, give up to avoid infinite or
                        // exceptionally long loops
                        fails++;
                        if (fails > 3) {
                            fail = true;
                        }
                    }
                }
            } while (reGen && !fail);
            
            // add point if valid
            if (!fail)
                points.add(newPoint);
        }
        
        if (GameScreen.isDebugMode) {
            //points.clear();
            //points.add(new Vector2(1337, 420));//TODO: system near origin for debug, don't forget about me
        }
        
        return points;
    }
    
    
    public void saveToJson() {
        Json json = new Json();
        json.setUsePrototypes(false);
        
        Gdx.app.log(this.getClass().getSimpleName(), json.toJson(this));
        
        FileHandle keyFile = Gdx.files.local("save/" + this.getClass().getSimpleName() + ".json");
        try {
            keyFile.writeString(json.toJson(this), false);
        } catch (GdxRuntimeException ex) {
            Gdx.app.error(this.getClass().getSimpleName(), "Could not save file", ex);
        }
    }
    
}