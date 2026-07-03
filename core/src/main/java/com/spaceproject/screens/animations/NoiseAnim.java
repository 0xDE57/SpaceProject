package com.spaceproject.screens.animations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.PerformanceCounter;
import com.spaceproject.math.MyMath;
import com.spaceproject.noise.OpenSimplexNoise;
import com.spaceproject.utility.IndependentTimer;

public class NoiseAnim extends TitleAnimation {
    float z;
    float zDelta;
    float size;
    float scale;
    boolean crossSection;
    OpenSimplexNoise noise;
    
    PerformanceCounter performance;
    IndependentTimer lagDetector;
    
    boolean isDrag = false;
    Vector2 clickStart = new Vector2();
    
    public NoiseAnim(float z, float zDelta, float size, float scale, boolean crossSection) {
        this.z = z;
        this.zDelta = zDelta;
        this.size = size * Gdx.graphics.getDensity();
        this.scale = scale;
        this.crossSection = crossSection;
        noise = new OpenSimplexNoise(MathUtils.random(Long.MAX_VALUE));
        
        //one DIP is one pixel on an approximately 160 dpi screen. Thus on a 160dpi screen this density value will be 1; on a 120 dpi screen it would be .75; etc.
        Gdx.app.log(getClass().getSimpleName(), "density: " + Gdx.graphics.getDensity());
        lagDetector = new IndependentTimer(2000, true);
        performance = new PerformanceCounter("anim");
    }
    
    public NoiseAnim() {
        this(0, 0.2f, 16.6f, 0.110f, false);
    }
    
    @Override
    public void render(float delta, ShapeRenderer shape) {
        performance.start();
        
        z += zDelta * delta;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x <= Gdx.graphics.getWidth() / size; x++) {
            for (int y = 0; y <= Gdx.graphics.getHeight() / size; y++) {
                float e = MyMath.inverseLerp(-1, 1, (float) noise.eval(x * scale, y * scale, z));
                if (crossSection) {
                    if (e > 0.45f && e < 0.55f) {
                        shape.setColor(Color.BLACK);
                        shape.rect(x * size, y * size, size, size);
                    }
                } else {
                    shape.setColor(e, e, e, 1);
                    shape.rect(x * size, y * size, size, size);
                }
            }
        }
        shape.end();
        
        performance.stop();
        
        if (crossSection) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                if (!isDrag) {
                    if (!clickStart.epsilonEquals(Gdx.input.getX(), Gdx.input.getY(), 2)) {
                        clickStart.set(Gdx.input.getX(), Gdx.input.getY());
                        isDrag = true;
                    }
                }
            } else {
                isDrag = false;
            }
            if (isDrag)
                z = z + ((Gdx.input.getX() - clickStart.x) * 0.0002f);
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            scale += 0.001f;
            Gdx.app.log(getClass().getSimpleName(), scale + "");
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            scale -= 0.001f;
            Gdx.app.log(getClass().getSimpleName(), scale + "");
        }
        
        if (Gdx.graphics.getFramesPerSecond() < 30) {
            if (lagDetector.canDoEvent()) {
                size += 0.01f;
                Gdx.app.log(getClass().getSimpleName(), "lag detected. adjusting: " +  performance.toString());
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
    }
    
}
