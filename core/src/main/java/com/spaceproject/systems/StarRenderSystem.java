package com.spaceproject.systems;


import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

public class StarRenderSystem extends IteratingSystem implements Disposable {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    private final ShaderProgram starShader;
    
    private float shift = 0;
    private final float shiftSpeed = 0.1f;
    
    private final Array<Entity> renderQueue = new Array<>();

    public StarRenderSystem() {
        super(Family.all(TextureComponent.class, TransformComponent.class, StarComponent.class).get());
        
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
      
        //load shaders
        ShaderProgram.pedantic = false;
        starShader = new ShaderProgram(Gdx.files.internal("shaders/starAnimate.vert"), Gdx.files.internal("shaders/starAnimate.frag"));
        if (starShader.isCompiled()) {
            spriteBatch.setShader(starShader);
            Gdx.app.log(this.getClass().getSimpleName(), "shader compiled successfully!");
        } else {
            Gdx.app.error(this.getClass().getSimpleName(), "shader failed to compile:\n" + starShader.getLog());
        }
    }
    
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        renderQueue.add(entity);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime); //adds entities to render queue
        
        //shift shader
        shift += shiftSpeed * deltaTime;
        starShader.bind();
        starShader.setUniformf("u_shift", (float) Math.sin(shift));
        DebugSystem.addDebugText(MyMath.round(shift, 2) + " shifted -> " + MyMath.round(Math.sin(shift), 2), 500, 500);
    
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        
        for (Entity entity : renderQueue) {
            /*
            StarComponent star = Mappers.star.get(entity);
            if (star != null) {
                starShader.setUniformf("u_colorTemp",
                        star.colorTemp[0],  // red
                        star.colorTemp[1],  // green
                        star.colorTemp[2]); // blue
            }
            */
            render(entity);
        }

        spriteBatch.end();
    }
    
    private void render(Entity entity) {
        TextureComponent tex = Mappers.texture.get(entity);
        TransformComponent transform = Mappers.transform.get(entity);
        
        float width = tex.texture.getWidth();
        float height = tex.texture.getHeight();
        float originX = width * 0.5f; //center
        float originY = height * 0.5f; //center
        
        //draw texture
        spriteBatch.draw(tex.texture, (transform.pos.x - originX), (transform.pos.y - originY),
                originX, originY,
                width, height,
                tex.scale, tex.scale,
                MathUtils.radiansToDegrees * transform.rotation,
                0, 0, (int) width, (int) height, false, false);
    }
    
    @Override
    public void dispose() {
        spriteBatch.dispose();
        starShader.dispose();
    }
    
}
