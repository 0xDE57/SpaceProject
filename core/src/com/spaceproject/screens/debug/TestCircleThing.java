package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.MyScreenAdapter;

public class TestCircleThing extends MyScreenAdapter {

    final Matrix4 projectionMatrix = new Matrix4();
    final BitmapFont font;

    float rateOfChange = 0.25f;
    float c = 0f;
    float radius = 350;
    int divisions = 360;
    float angle = MathUtils.PI2 / divisions;

    final Vector2 start = new Vector2();
    final Vector2 end = new Vector2();

    public TestCircleThing() {
        font = FontLoader.createFont(FontLoader.fontBitstreamVMBold, 15);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        //todo: https://gist.github.com/hellvetica42/fe1153a12db62e867f48d280f784762c
        //https://en.wikipedia.org/wiki/Dynamical_billiards#Bunimovich_stadium

        //clear screen
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        float centerX = Gdx.graphics.getWidth() * 0.5f;
        float centerY = Gdx.graphics.getHeight() * 0.5f;
        radius = Math.min(centerX, centerY) - 50;
        shape.begin(ShapeRenderer.ShapeType.Line);
        boolean renderStyleB = false;
        for(int i = 0; i <= divisions; i++) {
            float xx = i;// * c * 0.99f;
            if (renderStyleB) {
                xx = i * c * 0.99f;
            }
            float yy = i * c;
            offsetAngle(xx, start);
            offsetAngle(yy, end);
            start.add(centerX, centerY);
            end.add(centerX, centerY);
            if (i % 4 == 0) {
                shape.line(start.x, start.y, end.x, end.y, Color.RED, Color.BLUE);
            } else {
                shape.line(start.x, start.y, end.x, end.y, Color.PURPLE, Color.GREEN);
            }
        }
        //shape.ellipse(centerX-radius/2, centerY-radius, radius, radius*2, 45f);
        shape.circle(centerX, centerY, radius);


        //quad test
        //temp.set(0, 0);
        //quadraticFloat(1, 2, 3, temp);


        shape.end();

        batch.begin();
        font.draw(batch, "c: " + MyMath.round(c, 2), 20, 20);
        batch.end();

        c += rateOfChange * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            rateOfChange += 0.5f * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            rateOfChange -= 0.5f * delta;
        }

        //temp debug
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            float fa = 1.0f;
            float fb = -4.0000000f;
            float fc = 3.9999999f;
            double da = 1.0;
            double db = -4.0000000;
            double dc = 3.9999999;
            MyMath.quadraticFloat(fa, fb, fc, temp);
            Gdx.app.log("quad", "\n" + MyMath.formatFloat(temp.x) + "\n" + MyMath.formatFloat(temp.y));
            MyMath.quadraticDouble(da, db, dc, temp);
            Gdx.app.log("quad", "\n" + MyMath.formatDouble(temp.x) + "\n" + MyMath.formatDouble(temp.y));

            Gdx.app.log("", "\n" +
                    MyMath.formatFloat(Float.MIN_VALUE) + "\n" +
                    MyMath.formatFloat(Float.MAX_VALUE) + "\n" +
                    MyMath.formatFloat(-1) + "\n" +
                    MyMath.formatFloat(-0.1f) + "\n" +
                    MyMath.formatFloat(0) + "\n" +
                    MyMath.formatFloat(0.1f) + "\n" +
                    MyMath.formatFloat(0.01f) + "\n" +
                    MyMath.formatFloat(0.001f) + "\n" +
                    MyMath.formatFloat(0.0001f) + "\n" +
                    MyMath.formatFloat(0.00001f) + "\n" +
                    MyMath.formatFloat(0.000001f) + "\n" +
                    MyMath.formatFloat(0.0000001f) + "\n" +
                    MyMath.formatFloat(0.00000001f) + "\n" +
                    MyMath.formatFloat(0.000000001f) + "\n" +
                    MyMath.formatFloat(0.0000000001f) + "\n" +
                    MyMath.formatFloat(0.00000000001f) + "\n" +
                    MyMath.formatFloat(0.000000000001f) + "\n" +
                    MyMath.formatFloat(0.0000000000001f) + "\n" +
                    MyMath.formatFloat(0.00000000000001f) + "\n" +
                    MyMath.formatFloat(1));

            Float.intBitsToFloat(010101010101);
            /*
            float t = 0f;
            for (int i = 0; i < 500; i++) {
                Gdx.app.log("i", formatFloat(t));
                t += 0.1f;
            }*/
        }
    }

    Vector2 temp = new Vector2();

    private void offsetAngle(float angleOffset, Vector2 out) {
        float x = (float) (Math.cos(angle * angleOffset) * radius);
        float y = (float) (Math.sin(angle * angleOffset) * radius);
        out.set(x, y);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);
    }

}
