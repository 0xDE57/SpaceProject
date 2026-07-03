package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.math.MyMath;

public class OrbitAnim extends TitleAnimation {

    Vector2 centerScreen = new Vector2();
    Array<Body> bodies;
    float angleSyncEpsilon = 0.1f;

    //todo: calculate center of mass of all bodies, then set all bodies relavtive (or move camera) to center of mass
    //todo: render sine, and later explore x / y lissajous
    //todo: test render resonance ghost, leave marker when planets align
    //todo: overlay delaunay, and later voronoi when fixed
    Array<Body> ghosts = new Array<>();
    
    public OrbitAnim() {
        bodies = new Array<>();
    
        bodies.addAll(orbitalResonance());
        
        //addRandomSystem();
        
        //bodies.addAll(solarSystem());
    }
    
    private void addRandomSystem() {
        //star with random planets
        Body star = new Body(null, 0);
        bodies.add(star);
        
        for (int i = 0; i < 5; i++) bodies.add(new Body(star, 100 + (i * 75)));
    }
    
    private Array<Body> orbitalResonance() {
        Array<Body> resonating = new Array<>();
        
        //star
        Body parent = new Body(null, 0);
        resonating.add(parent);
        
        int distance = 100;
        float angle = 0;
        float rotSpeed = 0.5f;
        
        int num = 5;
        for (int i = 0; i < num; i++) {
            Body planet = new Body(parent, distance * (i+1), angle, rotSpeed*(i+1));
            resonating.add(planet);
        }
        
        return resonating;
    }
    
    private Array<Body> solarSystem() {
        Array<Body> bodies = new Array<>();
        
        //Sun
        Body sun = new Body(null, 0);
        
        float scale = 10;// 1/10th or ten times smaller
        float scaledDistance = 1 / scale;
        //Mercury
        //0.39 AU, 36 million miles/57.9 million km
        Body mercury = new Body(sun, 10);
        bodies.add(mercury);
        
        //todo:
        //Venus
        //Earth
        //Mars
        //Jupiter
        //Saturn
        //Uranus
        //Neptune
        //Pluto
        
        return bodies;
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        for (Body orbit : bodies) {
            orbit.update(delta);
        }
        
        //interaction: clicking aligns angle
        if (Gdx.input.isTouched()) {
            centerScreen.set(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
            float angleToTouch = MyMath.angleTo(centerScreen.x, centerScreen.y, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
            for (Body orbit : bodies) {
                //orbit.angleRadians = angleToTouch;
                orbit.angleRadians += 2 * delta;
                //todo: drag to shift back and forth through phase
                //save start position, phase shift = delta between start x and current x
            }
        }
        
        //render orbit rings lines
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < bodies.size; i++) {
            Body body = bodies.get(i);
            //skip render star orbit (until nested orbit, then this is a bad check)
            if (body.parent == null) continue;
    
            shape.setColor(Color.BLACK);
        
            //check if orbits synchronize
            //highlight both, and connect
            for (int j = 0; j < bodies.size; j++) {
                //skip check self
                if (i == j) continue;
    
                Body other = bodies.get(j);
                
                //don't process star angle, skip
                if (other.parent == null) continue;
                
                //angle is equal to other angle with given epsilon
                if (MathUtils.isEqual(body.angleRadians, other.angleRadians, angleSyncEpsilon)) {
                    shape.setColor(Color.WHITE);
                    //shape.line(body.pos, other.pos);
                    
                    /*add ghost marker where phase aligned
                    boolean contain = false;
                    for (Body b: ghosts) {
                        if (b.distance == body.distance) {
                            System.out.println("already contains body");
                            contain = true;
                        }
                    }
                    if (!contain) {
                        System.out.println("added body");
                        Body copy = new Body(body.parent, 0);
                        copy.pos = body.pos.cpy();
                        copy.distance = body.distance;
                        copy.size = body.size;
                        copy.angleRadians = body.angleRadians;
                        copy.update(1);
                        ghosts.add(copy);
                    }*/
                    
                    break;//exit for loop no point continue check
                }
            }
            
            shape.circle(body.parent.pos.x, body.parent.pos.y, body.distance);
        }
        shape.end();
    
        //render sine
        
            //Vector2 center = body.parent.pos;
            /*
            shape.setColor(Color.RED);
            shape.line(body.pos, center);
            shape.setColor(Color.BLUE);
            shape.line(body.pos.x, body.pos.y, body.pos.x+100, body.pos.y);
            
            //for x till end of screen, draw pixel
            //calculate sine wave
            shape.setColor(Color.GREEN);
            Rectangle rectangle = new Rectangle(body.pos.x, body.pos.y, (Gdx.graphics.getWidth() * 0.5f) - 1, 100);
            shape.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            shape.line(body.pos.x, body.pos.y, 0, 0);
            */
        
        /*
        * for note in self.active_notes:
            sin_wave = []
            for x in range(int(canvas_width)):
                sin_wave.append(x)  # x
                y = int(math.sin(x * (note.frequency/self.scale) + phase_offset) * amplitude) + center
                sin_wave.append(y)  # y

                y += center*2

                if pos > 1:
                    sin_wave_sum[x*2+1] += y/len(self.active_notes)
                else:
                    sin_wave_sum.append(x)
                    sin_wave_sum.append(y / len(self.active_notes))

            #print(sin_wave)
            pos += 1
            color = util.get_color_for_octave(pos)#misleading use, not actualy octave, just using key index
            self.canvas.create_line(0, center, canvas_width, center, fill='black')
            self.canvas.create_line(sin_wave, fill=color)

        * */
        
        /*
        shape.begin(ShapeRenderer.ShapeType.Point);
        shape.setColor(Color.GOLD);
        int max = Gdx.graphics.getWidth()/2;
        for (int i = 0; i < bodies.size; i++) {
            Body body = bodies.get(i);
    
            //skip star
            if (body.parent == null)
                continue;
            
            float scale = 1;
            float frequency = 10000;
            float amplitude = body.distance;
            for (int x = 0; x <= max; x++) {
                //y = int(math.sin(x * (note.frequency/self.scale) + phase_offset) * amplitude) + center
                float y = (float) (Math.sin(x * frequency / scale) * amplitude);
                
                shape.point(body.pos.x + x, Gdx.graphics.getHeight()/2 + y, -1);
                
                
                shape.point(body.pos.x + x, body.pos.y, -1);
            }
        }
        shape.end();
        */
        
        //render solid bodies
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Body orbit : bodies) {
            orbit.render(shape);
        }
    
        shape.setColor(Color.GRAY);
        for (Body g : ghosts) {
            shape.circle(g.pos.x, g.pos.y, g.size);
        }
        shape.end();
    }
    
    private void badPointRenderInLineShape(ShapeRenderer shape, Body body) {
        //renders bad because in shapetype line instead of point: flickering
        shape.begin(ShapeRenderer.ShapeType.Line);
        
        shape.setColor(Color.GOLD);
        int max = 200;
        
        //shape.line(body.pos.x, body.pos.y, Gdx.graphics.getWidth(), body.pos.y);
        for (int x = 0; x <= max; x++) {
            shape.point(body.pos.x + x, body.pos.y, -1);
        }
        
        shape.end();
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
    private class Body {
        private Body parent;
        private Vector2 pos;
        private float angleRadians, rotSpeed;
        private float distance;
        private int size;
    
        public Body(Body parent, float distance) {
            this(parent, distance, MathUtils.random(MathUtils.PI2), MathUtils.random(0.15f, 0.8f));
        }
        
        public Body(Body parent, float distance, float angle, float rotSpeed) {
            this.parent = parent;
            this.distance = distance;
            this.angleRadians = angle;// MathUtils.random(MathUtils.PI2);
            this.rotSpeed = rotSpeed;// MathUtils.random(0.15f, 0.8f);
            
            //randomize rotation direction
            //if (MathUtils.randomBoolean())
            //this.rotSpeed = -rotSpeed;
            
            size = MathUtils.random(5, 10);
            if (parent == null) {
                size *= 3;
            }
            
            if (parent != null)
                pos = new Vector2();//MyMath.vector(angle, distance).cpy().add(parent.pos);
            else
                pos = new Vector2(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        }
    
        public void update(float delta) {
            angleRadians += rotSpeed * delta;
            
            //clamp between -180 and 180 degrees
            //why? to avoid wrapping values eg: radians == 360 != 0
            //eg: degrees = 720 (spin 360 2 times) we are left with 720 != 0 even tho its the same angle!
            //  if (degrees == 0) returns false!
            while (angleRadians < -180 * MathUtils.degRad) angleRadians += 360 * MathUtils.degRad;
            while (angleRadians >  180 * MathUtils.degRad) angleRadians -= 360 * MathUtils.degRad;
            //modulus?
            
            if (parent != null) {
                pos = MyMath.vector(angleRadians, distance).cpy().add(parent.pos);
            } else {
                pos.set(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
            }
        }
        
        public void render(ShapeRenderer shape) {
            if (parent != null) {
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
            } else {
                shape.setColor(Color.BLACK);
                shape.circle(pos.x, pos.y, size);
                shape.setColor(Color.WHITE);
                shape.circle(pos.x, pos.y, size-1);
            }
        }
        
    }
    
}
