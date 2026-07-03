package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.*;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class TractorBeamSystem extends IteratingSystem implements Disposable {

    final Vector2 incidentRay = new Vector2();
    final Vector2 rayPoint = new Vector2();
    final Vector2 rayNormal = new Vector2();
    final Vector2 incidentVector = new Vector2();
    Fixture rayFixture = null;
    final ShapeRenderer shape;

    Color outerColor = Color.SKY.cpy();
    Color innerColor = Color.BLUE.cpy();

    int rayCount;
    int callbackCount;
    StringBuilder infoString = new StringBuilder();

    public TractorBeamSystem() {
        super(Family.all(TractorBeamComponent.class, PhysicsComponent.class).get());
        shape = new ShapeRenderer();
    }

    @Override
    public void update(float deltaTime) {
        //reset stats
        rayCount = 0;
        callbackCount = 0;

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(GameScreen.cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        super.update(deltaTime);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TractorBeamComponent tractorBeam = Mappers.tractor.get(entity);
        if (tractorBeam.state == TractorBeamComponent.State.off) return;

        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state != ShieldComponent.State.off) {
            return;
        }

        Body body = Mappers.physics.get(entity).body;
        incidentRay.set(MyMath.vector(body.getAngle(), tractorBeam.range).add(body.getPosition()));
        castTractorBeam(entity, tractorBeam, body.getPosition(), incidentRay, deltaTime);
    }

    private void castTractorBeam(Entity entity, TractorBeamComponent tractorBeam, Vector2 p1, Vector2 p2, float deltaTime) {
        rayPoint.set(p2);
        rayFixture = null;
        GameScreen.box2dWorld.rayCast((fixture, point, normal, fraction) -> {
            callbackCount++;
            if (fixture.isSensor()) return -1; //ignore sensor fixtures
            rayPoint.set(point);
            rayNormal.set(normal);
            rayFixture = fixture;
            return fraction;
        }, p1, p2);
        rayCount++;

        p2.set(rayPoint);
        incidentVector.set(p2).sub(p1);
        
        boolean hit = rayFixture != null;

        //draw beam
        float outerWidth = hit ? MathUtils.random(0.1f, 0.15f) : 0.1f;
        float innerWidth = hit ? MathUtils.random(0.01f, 0.1f) : 0.1f;
        outerColor.a = hit ? 1 : 0.3f;
        innerColor.a = hit ? 1 : 0.3f;
        shape.setColor(outerColor);
        shape.circle(p2.x, p2.y, outerWidth, 7);
        shape.rectLine(p1.x, p1.y, p2.x, p2.y, outerWidth, outerColor, outerColor);
        shape.setColor(innerColor);
        shape.circle(p2.x, p2.y, innerWidth, 7);
        shape.rectLine(p1.x, p1.y, p2.x, p2.y, innerWidth, innerColor, Color.CLEAR);
        
        if (!hit) return;

        //we hit something!
        Object userData = rayFixture.getBody().getUserData();
        if (userData != null) {
            Entity hitEntity = (Entity) userData;
            if (entity == hitEntity) return; //ignore self (shouldn't happen with no reflection?)

            AsteroidComponent asteroid = Mappers.asteroid.get(hitEntity);
            if (asteroid != null) {
                asteroid.parentOrbitBody = null;
            }
            //tractor beam! tractor beam! tractor beam!
            float magnitude = tractorBeam.magnitude * deltaTime;
            switch (tractorBeam.state) {
                case push:
                    rayFixture.getBody().applyForce(MyMath.vector(incidentVector.angleRad(), magnitude), p2, true);
                    break;
                case pull:
                    Vector2 force = MyMath.vector(incidentVector.angleRad() - MathUtils.PI, magnitude);
                    rayFixture.getBody().applyForce(force, p2, true);
                    //apply same force to player?
                    Mappers.physics.get(entity).body.applyForceToCenter(force.rotateDeg(180).scl(0.2f), true);
                    break;
            }
        }
    }

    @Override
    public void dispose() {
        shape.dispose();
    }

    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("[Raycasts]:         ").append(rayCount);
        infoString.append("\n[Ray callbacks]:    ").append(callbackCount);
        return infoString.toString();
    }

}
