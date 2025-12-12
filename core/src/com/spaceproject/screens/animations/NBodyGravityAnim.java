package com.spaceproject.screens.animations;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.ui.VisLabeledSlider;

public class NBodyGravityAnim extends TitleAnimation implements Disposable {

    class Stats {
        float totalMass;
        float totalRadius;
        Vector2 tVeloticy = new Vector2();
        Vector2 tAccel = new Vector2();
        float totalVelocity;
        float totalAcceleration;

        public void reset() {
            totalMass = 0;
            totalRadius = 0;
            totalVelocity = 0;
            totalAcceleration = 0;
        }
    }
    
    class SimpleBody {
        
        Vector2 pos;
        Vector2 vel;
        Vector2 accel;
        float radius;
        float mass;
        
        Vector2[] tail;
        int tailIndex;
        
        public SimpleBody(float x, float y, float velX, float velY, float radius) {
            pos = new Vector2(x, y);
            vel = new Vector2(velX, velY);
            accel = new Vector2();
            this.radius = radius;
            
            //basic model all bodies assume same density
            //mass = (float) (radius * radius * Math.PI);
            //float density = random 0 to 1?
            calculateMass();
            
            //init tail: pre-allocate vectors
            tail = new Vector2[tailSize];
            for (int index = 0; index < tail.length; index++) {
                tail[index] = new Vector2();
            }
            tailIndex = 0;//start at beginning
        }
        
        public SimpleBody(float x, float y, float radius) {
            this(x, y, 0, 0, radius);
        }
        
        public void calculateMass() {
            mass = (float) (radius * radius * Math.PI);
        }
        
        public void updateTail() {
            tail[tailIndex].set(pos.x, pos.y);
            
            tailIndex++;
            if (tailIndex >= tail.length) {
                tailIndex = 0;
            }
        }
    
        public void merge(SimpleBody bodyB) {
            radius += bodyB.radius;//absorb mass
            calculateMass();
            //vel.crs(bodyB.vel);//merge velocity
            //pos.add(pos.cpy().sub(bodyB.pos));//split the difference
        }
    }

    Stats stats = new Stats();

    float timeAccumulator;
    float simulationSpeed;
    float gravity;
    int tailSize = 200;
    
    Array<SimpleBody> bodies = new Array<>();
    SimpleBody ghostBody = null;
    
    boolean isDrag = false;
    SimpleBody selectedBody = null;
    int releaseMS = 200;
    long dragTime;

    boolean renderBodies = true;
    boolean renderTails = true;
    boolean renderDelaunay = false;
    FloatArray points = new FloatArray();
    ShortArray triangles;
    DelaunayTriangulator delaunay = new DelaunayTriangulator();

    Stage stage;
    VisWindow window;
    VisLabeledSlider simulationSpeedSlider;
    VisLabeledSlider gravitySlider;
    
    public NBodyGravityAnim(Stage stage) {
        this.stage = stage;
        buildWindow();

        resetInitialBodies();
    }
    
    private void buildWindow() {
        window = new VisWindow("n-body");
        window.addCloseButton();

        float maxGravity = 5;
        gravitySlider = new VisLabeledSlider(-maxGravity, maxGravity, 0.1f, "gravity");
        gravitySlider.setValue(gravity);
        gravitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gravity = gravitySlider.getValue();
            }
        });
        gravitySlider.pack();

        int maxSim = 100;
        simulationSpeedSlider = new VisLabeledSlider(-maxSim, maxSim, 0.1f, "sim");
        simulationSpeedSlider.setValue(simulationSpeed);
        simulationSpeedSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                simulationSpeed = simulationSpeedSlider.getValue();
            }
        });
        
        final VisTextButton resetButton = new VisTextButton("[R]eset");
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                resetInitialBodies();
            }
        });

        final VisCheckBox toggleDelaunay = new VisCheckBox("delaunay", renderDelaunay);
        toggleDelaunay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                renderDelaunay = toggleDelaunay.isChecked();
            }
        });

        final VisCheckBox toggleBodies = new VisCheckBox("bodies", renderBodies);
        toggleBodies.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                renderBodies = toggleBodies.isChecked();
            }
        });

        final VisCheckBox toggleTails = new VisCheckBox("tails", renderTails);
        toggleTails.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                renderTails = toggleTails.isChecked();
            }
        });

        int maxTail = 5000;
        VisLabeledSlider tailSlider = new VisLabeledSlider(0, maxTail, 1, "tailsize");
        tailSlider.setValue(tailSize);
        tailSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tailSize = (int) tailSlider.getValue();
            }
        });
        tailSlider.pack();

        //todo: alignment. text length of label affects slider alignment
        //todo: add color pickers for bodies, tails, triangle
        window.add(simulationSpeedSlider);
        window.row();
        window.add(gravitySlider);
        window.row();
        window.add(toggleDelaunay);
        window.row();
        window.add(toggleBodies);
        window.row();
        window.add(toggleTails);
        window.row();
        window.add(tailSlider);
        window.row();

        //radio button: no-collision, bounce, absorb
        //bounce type, add or remove velocity, does the bounce absorb energy or give energy?
        //bouns off bodies, bounce off wall
        //---
        //stats:
        //num bodies, total mass

        //render options:
        //color tails based off speed;
        //color bodies based of relative size,
        //color bodies based off relative speed
        //---

        window.add(resetButton);
        window.pack();
    }
    
    private void resetInitialBodies() {
        bodies.clear();
        //classic stable figure 8
        //initial condition computed by Carles Sim ́o
        //x1=−x2=0.97000436−0.24308753i,x3=0; ~V =  ̇x3=−2  ̇x1=−2  ̇x2=−0.93240737−0.86473146i
        Vector2 x1 = new Vector2(-0.97000436f, 0.24308753f);
        x1.scl(300);//make bigger to fill screen
        Vector2 x2 = new Vector2(-x1.x, -x1.y);
        Vector2 x3 = new Vector2(0, 0); //center body
        
        Vector2 vx3 = new Vector2(0.93240737f, 0.86473146f);
        vx3.scl(4.1f);//todo: not accurate scaling: not stable
        Vector2 vx2 = new Vector2(-vx3.x / 2, -vx3.y / 2);
        Vector2 vx1 = new Vector2(vx2.x, vx2.y);
    
        //center bodies on screen
        x1.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        x2.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        x3.add(Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f);
        
        float radius = 40;//somewhat magic number
        bodies.add(new SimpleBody(x1.x, x1.y, vx1.x, vx1.y, radius));
        bodies.add(new SimpleBody(x2.x, x2.y, vx2.x, vx2.y, radius));
        bodies.add(new SimpleBody(x3.x, x3.y, vx3.x, vx3.y, radius));
        
        //reset simulation parameters
        gravity = 1f;
        simulationSpeed = 15.0f;

        window.getTitleLabel().setText("n-body (" + bodies.size + ")");
        gravitySlider.setValue(gravity);
        simulationSpeedSlider.setValue(simulationSpeed);
        Gdx.app.log(getClass().getSimpleName(), "reset initial bodies. initial settings: " + gravity );
    }

    @Override
    public void render(float delta, ShapeRenderer shape) {
        //update simulation
        physicsStep(delta * simulationSpeed);
        timeAccumulator += delta;

        //reset to initial conditions
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {//n for n-body
            if (!window.hasParent()) {
                stage.addActor(window);
                window.setPosition(stage.getWidth(), 0);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {//R for reset
            resetInitialBodies();
        }
        
        //mouse position
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean mouseInWindw = false;
        if (window.hasParent()) {
            Rectangle.tmp.setPosition(window.getX(), window.getY()).setSize(window.getWidth(), window.getHeight());
            mouseInWindw = Rectangle.tmp.contains(x, y);
            //Gdx.app.log(this.getClass().getName(), "x:" + winX + ", y: " + winY);
        }

        //add new body
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !mouseInWindw) {
            if (ghostBody == null) {
                ghostBody = new SimpleBody(0, 0, 5);
                timeAccumulator = 0;
            }
        } else {
            if (ghostBody != null && !mouseInWindw) {
                ghostBody.calculateMass();//recalc mass due to radius change
                bodies.add(ghostBody);
                Gdx.app.log(getClass().getSimpleName(), "added body.(" + ghostBody.mass + ") total: " + bodies.size);
                window.getTitleLabel().setText("n-body (" + bodies.size + ")");
                ghostBody = null;
            }
        }
        
        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            //hold-drag
            if (!isDrag) {
                for (SimpleBody body : bodies) {
                    Circle circle = new Circle(body.pos, body.radius);
                    if (circle.contains(x, y)) {
                        isDrag = true;
                        selectedBody = body;
                        dragTime = System.currentTimeMillis();
                        Gdx.app.log(getClass().getSimpleName(), "selected body: (" + body.mass + ")  " + body.pos);
                        break;
                    }
                }
            } else {
                Vector2 deltaMouse = selectedBody.pos.cpy().sub(x, y);
                selectedBody.vel.sub(deltaMouse.scl(deltaMouse.len() * 0.0001f * delta));
            }
        } else {
            //release: fling
            if (isDrag) {
                isDrag = false;
                //remove body if release was quick, and still in body
                if (System.currentTimeMillis() - dragTime <= releaseMS) {
                    Circle circle = new Circle(selectedBody.pos, selectedBody.radius);
                    if (circle.contains(x, y)) {
                        bodies.removeValue(selectedBody, true);
                        Gdx.app.log(getClass().getSimpleName(),
                                "removed body. (" + selectedBody.mass + ")  " + selectedBody.pos + " total: " + bodies.size);
                        window.getTitleLabel().setText("n-body (" + bodies.size + ")");
                    }
                }
                selectedBody = null;
            }
        }

        if (renderDelaunay) {
            points.clear();
            for (SimpleBody body : bodies) {
                points.add(body.pos.x, body.pos.y);
            }
            triangles = delaunay.computeTriangles(points, false);
            shape.begin(ShapeRenderer.ShapeType.Line);
            for (int i = 0; i < triangles.size; i += 3) {
                //get points
                int p1 = triangles.get(i) * 2;
                int p2 = triangles.get(i + 1) * 2;
                int p3 = triangles.get(i + 2) * 2;
                shape.triangle(
                        points.get(p1), points.get(p1 + 1),
                        points.get(p2), points.get(p2 + 1),
                        points.get(p3), points.get(p3 + 1));
            }
            shape.end();
        }

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0, 0, 0, 1);
        
        //draw bodies
        if (renderBodies) {
            for (SimpleBody body : bodies) {
                shape.setColor(0, 0, 0, 1);
                shape.circle(body.pos.x, body.pos.y, body.radius);
            }
        }
        
        //draw tails
        if (renderTails && tailSize > 0) {
            shape.setColor(1, 1, 1, 1);
            for (SimpleBody body : bodies) {
                body.updateTail();
                for (int index = 0; index < body.tail.length; index++) {
                    Vector2 tail = body.tail[index];
                    shape.point(tail.x, tail.y, 0);
                }
            }
        }
        
        if (isDrag) {
            shape.line(x, y, selectedBody.pos.x, selectedBody.pos.y);
        }
        
        //render and update new ghost body
        if (ghostBody != null) {
            ghostBody.pos.x = x;
            ghostBody.pos.y = y;
            ghostBody.radius += Math.sin(timeAccumulator) * 0.5f;
            
            shape.setColor(1, 1, 1, 1);
            shape.circle(ghostBody.pos.x, ghostBody.pos.y, ghostBody.radius);
        }
        
        shape.end();
    }
    
    /** integration */
    private void physicsStep(float delta) {
        float step = 0.5f * delta;

        stats.reset();

        // add velocity from momentum to position (first pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * step, body.vel.y * step));
        }
        
        // calculate force bodies act upon each other
        calculateForces();
    
        // add acceleration to velocity
        for (SimpleBody body : bodies) {
            body.vel.set(body.vel.cpy().add(body.accel.x * delta, body.accel.y * delta));
            stats.totalVelocity += body.vel.len();
            //only need positive delta?
            //stats.tVeloticy.x += Math.abs(body.pos.x);
            //stats.tVeloticy.y += Math.abs(body.pos.y);

            stats.tVeloticy.add(body.vel);
        }
    
        // add velocity from gravitational force to position (second pass)
        for (SimpleBody body : bodies) {
            body.pos.set(body.pos.cpy().add(body.vel.x * step, body.vel.y * step));
        }
        
        /*
        //collision, merge if touching and velocity difference is low enough
        boolean absorb;
        do {
            absorb = false;
            for (int index = 0; index < bodies.size && absorb == false; index++) {
                SimpleBody bodyA = bodies.get(index);
                for (int indexB = 0; indexB < index; indexB++) {
                    SimpleBody bodyB = bodies.get(indexB);
                    float length = bodyA.pos.cpy().sub(bodyB.pos).len();
                    float rad = bodyA.radius + bodyB.radius;
                    if (rad >= length) {
                        float dotVelocity = bodyA.vel.cpy().dot(bodyB.vel);
                        float deltaAccel = bodyA.accel.cpy().sub(bodyB.accel).len();
                        float deltaVel = bodyA.vel.cpy().sub(bodyB.vel).len();
                        float dotAccel = bodyA.accel.cpy().dot(bodyB.accel);
                        if (MathUtils.isEqual(dotVelocity, 0f, 0.1f)) {
                            Gdx.app.log("", "merge!");
                            if (bodyA.radius - bodyB.radius > 0) {
                                bodyA.merge(bodyB);
                                bodies.removeValue(bodyB, true);
                            } else {
                                bodyB.merge(bodyA);
                                bodies.removeValue(bodyA, true);
                            }
                            absorb = true;
                            break;
                        }
                    }
                }
            }
        } while (absorb);
        */
    }
    
    private void calculateForces() {
        for (int index = 0; index < bodies.size; index++) {
            SimpleBody bodyA = bodies.get(index);
            bodyA.accel.set(0, 0);
            
            for (int indexB = 0; indexB < index; indexB++) {
                SimpleBody bodyB = bodies.get(indexB);
                
                //calculate force of attraction between bodies
                Vector2 delta = bodyA.pos.cpy().sub(bodyB.pos);
                float norm = (float) Math.sqrt(100.0 + delta.len2());
                float mag = gravity / (norm * norm * norm);
                
                //apply force
                bodyA.accel.set(bodyA.accel.cpy().sub(delta.x * (mag * bodyB.mass), delta.y * (mag * bodyB.mass)));
                bodyB.accel.set(bodyB.accel.cpy().add(delta.x * (mag * bodyA.mass), delta.y * (mag * bodyA.mass)));
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
    
    }
    
    @Override
    public void dispose() {
        window.remove();
    }
    
}
