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
import com.spaceproject.components.ShaderComponent;
import com.spaceproject.components.StarComponent;
import com.spaceproject.components.TextureComponent;
import com.spaceproject.components.TransformComponent;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import java.util.Comparator;

public class Sprite2DShaderRenderSystem extends IteratingSystem implements Disposable {
    
    private final OrthographicCamera cam;
    private final SpriteBatch spriteBatch;
    private final ShaderProgram starShader;
    private final ShaderProgram grayscaleShader;
    
    private ShaderComponent.ShaderType currentActiveShader;
    private float shift = 0;
    private final float shiftSpeed = 0.1f;
    
    private final Array<Entity> renderQueue = new Array<>();
    
    //sort by enum ordinal (group by shader)
    private final Comparator<Entity> shaderComparator = new Comparator<Entity>() {
        @Override
        public int compare(Entity entityA, Entity entityB) {
            return (int) Math.signum(Mappers.shader.get(entityB).shaderType.ordinal()
                    - Mappers.shader.get(entityA).shaderType.ordinal());
        }
    };
    
    public Sprite2DShaderRenderSystem() {
        super(Family.all(TextureComponent.class, TransformComponent.class, ShaderComponent.class).get());
    
        cam = GameScreen.cam;
        spriteBatch = new SpriteBatch();
    
        //load shaders
        ShaderProgram.pedantic = false;
        String starVert = "shaders/starAnimate.vert";
        String starFrag = "shaders/starAnimate.frag";
        starShader = new ShaderProgram(Gdx.files.internal(starVert), Gdx.files.internal(starFrag));
        if (starShader.isCompiled()) {
            //load by default for now
            spriteBatch.setShader(starShader);
            Gdx.app.log(getClass().getSimpleName(), "shader compiled successfully! [" + starVert + ", " + starFrag + "]");
        } else {
            Gdx.app.error(getClass().getSimpleName(), "shader failed to compile:\n" + starShader.getLog());
        }

        String grayVert = "shaders/grayscale.vert";
        String grayFrag = "shaders/grayscale.frag";
        grayscaleShader = new ShaderProgram(Gdx.files.internal(grayVert), Gdx.files.internal(grayFrag));
        if (starShader.isCompiled()) {
            //spriteBatch.setShader(starShader);
            Gdx.app.log(getClass().getSimpleName(), "shader compiled successfully! [" + grayVert + ", " + grayFrag + "]");
        } else {
            Gdx.app.error(getClass().getSimpleName(), "shader failed to compile:\n" + starShader.getLog());
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        renderQueue.add(entity);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime); //adds entities to render queue
        
        //sort render order
        renderQueue.sort(shaderComparator);
    
        //shift shader
        shift += shiftSpeed * deltaTime;
        starShader.bind();
        starShader.setUniformf("u_shift", (float) Math.sin(shift));
        //DebugSystem.addDebugText(MyMath.round(shift, 2) + " shifted -> " + MyMath.round(Math.sin(shift), 2), 500, 500);
        
        //render
        spriteBatch.setProjectionMatrix(cam.combined);
        spriteBatch.begin();
        for (Entity entity : renderQueue) {
            ShaderComponent shaderComponent = Mappers.shader.get(entity);
            if (currentActiveShader != shaderComponent.shaderType) {
                currentActiveShader = shaderComponent.shaderType;
                // dynamically set appropriate shader
                // warning: setting shader will flush the batch between batch.begin() and batch.end()
                // this is fine for now, but something to potentially consider in the future
                // todo: profile. (don't prematurely optimise)
                switch (shaderComponent.shaderType) {
                    case star:
                        spriteBatch.setShader(starShader);
                        break;
                    case grayscale:
                        spriteBatch.setShader(grayscaleShader);
                        break;
                }
                Gdx.app.debug(this.getClass().getSimpleName(), "shader set to: " + currentActiveShader);
            }
    
            StarComponent star = Mappers.star.get(entity);
            if (star != null) {
                //starShader.bind();
                starShader.setUniformf("u_colorTemp", star.colorTemp.r, star.colorTemp.g, star.colorTemp.b);
            }
            
            render(entity);
        }
        spriteBatch.end();
        
        renderQueue.clear();
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
        grayscaleShader.dispose();
    }
}
