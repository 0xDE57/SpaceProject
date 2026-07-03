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
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.DebugConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.math.Physics;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class LaserSystem extends IteratingSystem implements Disposable {

    final float refractiveIndexVacuum = 1; //vacuum = 1, water = 1.33, glass = 1.52, diamond = 2.4
    final int maxReflections = 10;
    final Vector2 incidentRay = new Vector2();
    final Vector2 rayPoint = new Vector2();
    final Vector2 rayNormal = new Vector2();
    final Vector2 incidentVector = new Vector2();
    final Vector2 refract = new Vector2();
    Fixture rayFixture = null;
    final ShapeRenderer shape;

    int rayCount;
    int callbackCount;
    StringBuilder infoString = new StringBuilder();

    boolean drawNormal = false;
    DebugConfig debug = SpaceProject.configManager.getConfig(DebugConfig.class);

    public LaserSystem() {
        super(Family.all(LaserComponent.class, PhysicsComponent.class).get());
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
        LaserComponent laser = Mappers.laser.get(entity);
        if (laser.state == LaserComponent.State.off) return;

        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state != ShieldComponent.State.off) {
            return;
        }

        Body body = Mappers.physics.get(entity).body;
        incidentRay.set(MyMath.vector(body.getAngle(), laser.range).add(body.getPosition()));
        if (debug.discoLaser) {
            int referenceWavelength = 589;//"yellow doublet" sodium D line
            float wavelength = MathUtils.random(380, 780);
            laser.wavelength = wavelength;
            laser.frequency = (float) Physics.wavelengthToFrequency(wavelength);
            int[] rgb = Physics.wavelengthToRGB(wavelength, 1);
            laser.color.set(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1);
        }

        castLaser(entity, laser, body.getPosition(), incidentRay, laser.color.cpy(), laser.range, maxReflections, deltaTime);
    }

    private void castLaser(Entity entity, LaserComponent laser, Vector2 p1, Vector2 p2, Color color, float length, int reflections, float deltaTime) {
        if (reflections <= 0) return;
        if (incidentVector.set(p2).sub(p1).isZero()) return;

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
        float distanceToHit = incidentVector.len();
        float range = distanceToHit / length;
        float remainingDistance = length - distanceToHit;

        Color endColor = Color.CLEAR.cpy().lerp(color, MathUtils.random()*0.25f);
        Color ratio = color.cpy().lerp(endColor, range - MathUtils.random()*0.15f);
        shape.rectLine(p1.x, p1.y, p2.x, p2.y, 0.2f, color, ratio);

        if (rayFixture == null) return;
        //we hit something!
        float damage = laser.damage * (1 - range) * deltaTime;

        boolean isGlass = false;
        Object userData = rayFixture.getBody().getUserData();
        if (userData != null) {
            Entity hitEntity = (Entity) userData;
            AsteroidComponent asteroid = Mappers.asteroid.get(hitEntity);
            if (asteroid != null) {
                //damage *= asteroid.albedo;// 0 = no damage, 1 = full damage
                //remainingDistance *= asteroid.albedo;

                //if fixture hit is a glassteroid, calculate refraction
                switch (asteroid.composition) {
                    case GLASS:
                        isGlass = true;
                        //float waveLengthInN1 = laser.wavelength / refractiveIndexVacuum;
                        refract(refract, incidentVector.nor(), rayNormal.nor(), refractiveIndexVacuum, asteroid.refractiveIndex);
                        if (refract.len2() > 0) {
                            if (drawNormal) {
                                shape.setColor(Color.CYAN);
                                shape.rectLine(p2, MyMath.vector((float) (rayNormal.angleRad() - Math.PI), 10).add(p2), 0.2f);
                            }

                            Vector2 refractedEndPoint = MyMath.vector(refract.angleRad(), remainingDistance).add(p2);
                            Vector2 innerNormal = new Vector2();
                            GameScreen.box2dWorld.rayCast((fixture, point, normal, fraction) -> {
                                callbackCount++;
                                //ignore sensors and other fixtures than the one we already hit
                                if (fixture.isSensor() || fixture != rayFixture) return -1;
                                refractedEndPoint.set(point);
                                innerNormal.set(normal);
                                return fraction;
                            }, refractedEndPoint, p2);
                            rayCount++;
                            shape.setColor(color.r, color.g, color.b, asteroid.color.a);
                            shape.rectLine(p2, refractedEndPoint, 0.1f);
                            //todo: continue to cast internal reflections
                            //calculate reflection: reflectedVector = incidentVector - 2 * (incidentVector dot normal) * normal
                            //Vector2 internalReflection = new Vector2(refractedEndPoint).sub(innerNormal.scl(2 * refractedEndPoint.dot(innerNormal))).setLength(remainingDistance).add(refractedEndPoint);
                            //shape.rectLine(refractedEndPoint,internalReflection, 0.2f);
                        }
                        break;
                        /*
                    case BLACK:
                        damage *= 2;//might need to rethink this entire model.
                        //normalize all regular albedo / damage at 0.5f?
                        //1 means full reflect, 0 means full absorb
                        //we want all regular to reflect
                        //black should fully absorb and do extra damage
                        //gold/silver is shiny so should reflect lots?
                        //blue is darker so takes more damage?

                         */
                    default:
                        if (debug.reflectAsteroidColor) {
                            color.set(asteroid.color.cpy());
                        }
                        break;
                }
            }

            //laser doesn't hurt glass!
            if (!isGlass) {
                if (entity != hitEntity) {
                    Box2DContactListener.damage(getEngine(), hitEntity, entity, damage, p2, rayFixture.getBody());
                }
                if (Mappers.damage.get(hitEntity) != null) {
                    hitEntity.add(new RemoveComponent()); //destroy bullets
                }
            }
        }
        if (drawNormal) {
            shape.setColor(Color.SKY);
            shape.rectLine(p2, MyMath.vector(rayNormal.angleRad(), 10).add(p2), 0.2f);
        }

        //calculate reflection: reflectedVector = incidentVector - 2 * (incidentVector dot normal) * normal
        Vector2 reflectedVector = incidentVector.cpy().sub(rayNormal.scl(2 * incidentVector.dot(rayNormal))).setLength(remainingDistance).add(p2);
        castLaser(entity, laser, p2, reflectedVector, color, remainingDistance, --reflections, deltaTime);
    }

    /** Calculate angle of refraction using Snell's Law
     * θ₁ = angle of incidence
     * θ₂ = angle of refraction
     * n₁sin(θ₁) = n₂sin(θ₂)
     * https://asawicki.info/news_1301_reflect_and_refract_functions.html
     * @param out
     * @param incidentVec
     * @param normal
     * @param n1 refractive index
     * @param n2 refractive index
     */
    private void refract(Vector2 out, Vector2 incidentVec, Vector2 normal, float n1, float n2) {
        float eta = n2/n1;
        float dot = incidentVec.dot(normal);
        float k = 1.0f - eta * eta * (1.0f - dot * dot);
        if (k < 0.f) {
            //past critical angle -> total internal reflection
            out.set(0.f, 0.f);
        } else {
            out.set(eta * incidentVec.x - (eta * dot + (float)Math.sqrt(k)) * normal.x,
                    eta * incidentVec.y - (eta * dot + (float)Math.sqrt(k)) * normal.y);
        }
    }

    /**
     * http://en.wikipedia.org/wiki/Sellmeier_equation
     * Approximation using hardcoded properties from known materials
     * @param wavelength in meters
     * @return n2 refractive index
     */
    private float getSellmeierValue(float wavelength)  {
        float l2 = wavelength * wavelength;
        //coefficients for common borosilicate crown glass known as BK7
        float b1 = 1.03961212f;
        float b2 = 0.231792344f;
        float b3 = 1.01046945f;
        // convert to metric
        float c1 = 6.00069867E-3f * 1E-12f;
        float c2 = 2.00179144E-2f * 1E-12f;
        float c3 = 1.03560653E2f * 1E-12f;
        /*
        //coefficients for sapphire (ordinary)
        float b1 = 1.43134930f;
        float b2 = 0.65054713f;
        float b3 = 5.3414021f;
        // convert to metric
        float c1 = 5.2799261E-3f * 1E-12f;
        float c2 = 1.42382647E-2f * 1E-12f;
        float c3 = 3.25017834E2f * 1E-12f;
        */
        return (float) Math.sqrt( 1 + b1 * l2 / (l2 - c1) + b2 * l2 / (l2 - c2) + b3 * l2 / (l2 - c3));
    }


    public float getIndexOfRefraction(float wavelength, float referenceWavelength, float referenceIndexOfRefraction) {
        // get the reference values
        float nAirReference = getAirIndex(referenceWavelength);
        float nGlassReference = getSellmeierValue(referenceWavelength);

        // determine the mapping and make sure it is in a good range
        float delta = nGlassReference - nAirReference;

        // 0 to 1 (air to glass)
        float x = (referenceIndexOfRefraction - nAirReference) / delta;
        x = MathUtils.clamp(x, 0, Float.POSITIVE_INFINITY);

        // take a linear combination of glass and air equations
        return x * this.getSellmeierValue(wavelength) + (1 - x) * this.getAirIndex(wavelength);
    }

    /**
     * See http://refractiveindex.info/?group=GASES&material=Air
     * @param wavelength - wavelength in meters
     */
    private float getAirIndex(float wavelength) {
        return (float) (1 +
                        5792105E-8f / ( 238.0185f - Math.pow(wavelength * 1E6f, -2)) +
                        167917E-8f / ( 57.362f - Math.pow(wavelength * 1E6f, -2)));
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
