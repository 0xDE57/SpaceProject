package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;

public class DropAnim extends TitleAnimation {
    
    int maxDrops;
    int ringChance;
    int maxRings;
    
    private ArrayList<Drop> drops = new ArrayList<Drop>();
    
    public DropAnim() {
        this(6, 30, 5);
    }
    
    public DropAnim(int maxDrops, int ringChance, int maxRings) {
        this.maxDrops = maxDrops;
        this.ringChance = ringChance;
        this.maxRings = maxRings;
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        boolean clicked = Gdx.input.isTouched();
        
        if ((drops.size() < maxDrops && MathUtils.random(ringChance) == 0) || clicked) {
            int numRings = MathUtils.random(1, maxRings);
            float ringRad = MathUtils.random(40, 150);
            float nextRing = MathUtils.random(8f, 20f);
            
            //could improve by not allowing overlapping rings (low priority, just playing)
            int x = MathUtils.random(0, Gdx.graphics.getWidth());
            int y = MathUtils.random(0, Gdx.graphics.getHeight());
            if (clicked) {
                x = Gdx.input.getX();
                y = Gdx.graphics.getHeight() - Gdx.input.getY();
            }
            
            drops.add(new Drop(x, y, numRings, ringRad, nextRing));
        }
        
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (Iterator<Drop> dropIter = drops.iterator(); dropIter.hasNext(); ) {
            Drop d = dropIter.next();
            d.render(delta, shape);
            
            if (d.rings.size() == 0 && d.numRings == d.maxRings)
                dropIter.remove();
        }
        shape.end();
        
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
    
    private class Drop {
        int x, y;
        int numRings, maxRings;
        float ringRad;
        float nextRing;
        ArrayList<Ring> rings;
        
        Drop(int x, int y, int maxRings, float ringRad, float nextRing) {
            this.x = x;
            this.y = y;
            this.maxRings = maxRings;
            this.ringRad = ringRad;
            this.nextRing = nextRing;
            rings = new ArrayList<Ring>();
        }
        
        public void render(float delta, ShapeRenderer shape) {
            if (numRings < maxRings) {
                if (rings.size() == 0) {
                    rings.add(new Ring(ringRad));
                    numRings++;
                } else if (rings.get(rings.size() - 1).radius > nextRing) {
                    rings.add(new Ring(ringRad));
                    numRings++;
                }
            }
            
            for (Iterator<Ring> ringIter = rings.iterator(); ringIter.hasNext(); ) {
                Ring r = ringIter.next();
                shape.setColor(0, 0, 0, 1 - (r.radius / r.maxRad));
                shape.circle(x, y, r.radius);
                
                r.radius += 25f * delta;
                
                if (r.radius > r.maxRad)
                    ringIter.remove();
            }
            
        }
        
    }
    
    private class Ring {
        float radius = 0;
        float maxRad;
        
        Ring(float maxRadius) {
            maxRad = maxRadius;
        }
    }
}
