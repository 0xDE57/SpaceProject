package com.spaceproject.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.utils.Array;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class ExplosionSystem extends EntitySystem {

    private static class Blast {
        final Vector2 position;
        final float radius;
        final float peakVelocity; // velocity kick at blast center, falling off to 0 at radius

        Blast(Vector2 position, float radius, float peakVelocity) {
            this.position = position;
            this.radius = radius;
            this.peakVelocity = peakVelocity;
        }
    }

    private final float blastRadius = 400f;    //how far from blast center the blast reaches
    private final float blastVelocity = 50f;  //peak outward velocity kick at the center

    private final Array<Blast> blasts = new Array<>(false, 8);

    private Blast currentBlast; //set per query in applyBlast
    private final Vector2 toBody = new Vector2();
    private final Vector2 impulse = new Vector2();
    private final QueryCallback blastQuery = new QueryCallback() {
        @Override
        public boolean reportFixture(Fixture fixture) {
            Body body = fixture.getBody();
            if (body.getType() != BodyDef.BodyType.DynamicBody) return true; //only push dynamic bodies

            Entity entity = (Entity) body.getUserData();
            if (entity != null) {
                if (Mappers.remove.get(entity) != null) return true;       //dying this frame (eg: explosion source)
                if (Mappers.spaceStation.get(entity) != null) return true; //don't shove stations around
            }

            //radial push away from blast center, linear falloff with distance
            toBody.set(body.getPosition()).sub(currentBlast.position);
            float distance = toBody.len();
            if (distance >= currentBlast.radius) return true;
            if (distance < 0.0001f) {
                //degenerate case: body at blast center, push in an arbitrary direction
                toBody.set(1f, 0f);
            } else {
                toBody.nor();
            }

            
            float falloff = 1f - distance / currentBlast.radius;
            impulse.set(toBody).scl(currentBlast.peakVelocity * falloff);
            //impulse.set(toBody).scl(currentBlast.peakVelocity * falloff * body.getMass());//alternative; scale impulse by mass so velocity kick is uniform regardless of body size
            body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
            return true;
        }
    };

    /** queues a blast at the given position with default radius/strength.
     *  safe to call during a physics step, applied on the next system update. */
    public void queueBlast(Vector2 position) {
        queueBlast(position, blastRadius, blastVelocity);
    }

    public void queueBlast(Vector2 position, float radius, float peakVelocity) {
        blasts.add(new Blast(position.cpy(), radius, peakVelocity));
    }

    @Override
    public void update(float deltaTime) {
        if (blasts.size == 0) return;
        for (int i = 0; i < blasts.size; i++) {
            applyBlast(blasts.get(i));
        }
        blasts.clear();
    }

    private void applyBlast(Blast blast) {
        if (GameScreen.box2dWorld == null) return;
        currentBlast = blast;
        GameScreen.box2dWorld.queryAABB(blastQuery,
                blast.position.x - blast.radius, blast.position.y - blast.radius,
                blast.position.x + blast.radius, blast.position.y + blast.radius);
        currentBlast = null;
    }

}
