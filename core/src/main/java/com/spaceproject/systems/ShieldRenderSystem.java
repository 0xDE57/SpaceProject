package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.ShieldComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ShieldRenderSystem extends IteratingSystem implements Disposable {

    private final ShapeRenderer shape;

    public ShieldRenderSystem() {
        super(Family.all(ShieldComponent.class, TransformComponent.class).get());
        shape = new ShapeRenderer();
    }

    @Override
    public void update(float delta) {
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(GameScreen.cam.combined);

        super.update(delta);

        rotateAnim += 2 * delta;

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    float rotateAnim = 0;

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield.state == ShieldComponent.State.off) {
            return;
        }

        TransformComponent transform = Mappers.transform.get(entity);

        //draw overlay
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(shield.heat, 0, 1, 0.20f + shield.heat);

        if (shield.heat >= 0.95f) {
            shape.setColor(1, 0, 0, 1);
        }

        float hitTime = 500;
        long lastHit = GameScreen.getGameTimeCurrent() - shield.lastHit;
        if (lastHit < hitTime) {
            float green = lastHit / hitTime;
            shape.setColor(0, 1 - green, green, Math.max(1 - green, 0.25f));
        }
        circle(transform.pos.x, transform.pos.y, shield.radius, rotateAnim);
        circle(transform.pos.x, transform.pos.y, shield.radius, -rotateAnim);
        shape.end();//flush inside loop = bad?

        //draw outline
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(1, 1 - shield.heat, 1 - shield.heat, 1f);
        if (lastHit < hitTime) {
            float green = lastHit / hitTime;
            //shape.setColor(1-green, 1-green, 1-green, 1);
            shape.setColor(1, green, 1, 1);
        }
        if (shield.heat >= 0.95f) {
            shape.setColor(1, 1, 0, 1);
        }

        circle(transform.pos.x, transform.pos.y, shield.radius, rotateAnim);
        circle(transform.pos.x, transform.pos.y, shield.radius, -rotateAnim);
        shape.end();//double flush inside same loop?
    }

    public void circle(float x, float y, float radius, float rotAngle) {
        circle(x, y, radius, Math.max(1, (int) (6 * (float) Math.cbrt(radius))), rotAngle);
    }

    public void circle(float x, float y, float radius, int segments, float rotAngle) {
        ImmediateModeRenderer renderer = shape.getRenderer();
        ShapeRenderer.ShapeType shapeType = shape.getCurrentType();
        Color color = shape.getColor();

        if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.");
        float colorBits = color.toFloatBits();
        float angle = 2 * MathUtils.PI / segments;
        float cos = MathUtils.cos(angle);
        float sin = MathUtils.sin(angle);
        Vector2 t = MyMath.vector(rotAngle, radius);
        float cx = t.x, cy = t.y;
        if (shapeType == ShapeRenderer.ShapeType.Line) {
            for (int i = 0; i < segments; i++) {
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
            }
        } else {
            segments--;
            for (int i = 0; i < segments; i++) {
                renderer.color(colorBits);
                renderer.vertex(x, y, 0);
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
                float temp = cx;
                cx = cos * cx - sin * cy;
                cy = sin * temp + cos * cy;
                renderer.color(colorBits);
                renderer.vertex(x + cx, y + cy, 0);
            }
            // Ensure the last segment is identical to the first.
            renderer.color(colorBits);
            renderer.vertex(x, y, 0);
            renderer.color(colorBits);
            renderer.vertex(x + cx, y + cy, 0);
            cx = t.x;
            cy = t.y;
            renderer.color(colorBits);
            renderer.vertex(x + cx, y + cy, 0);
        }
    }

    @Override
    public void dispose() {
        shape.dispose();
    }

}
