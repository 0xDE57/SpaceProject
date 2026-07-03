package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.EntityConfig;
import com.spaceproject.generation.TextureGenerator;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.ui.Sprite3D;
import com.spaceproject.math.MyMath;


public class Test3DScreen extends ScreenAdapter {
    
    
    OrthographicCamera orthographicCam;
    SpriteBatch batch = new SpriteBatch();
    ModelBatch modelBatch = new ModelBatch();
    Environment environment;
    
    EntityConfig entityCFG;
    EngineConfig engineCFG;
    Sprite3D ship3d;
    
    int playerX, playerY;
    float rotX = 0, rotY = 0, rotZ = 0;
    
    public Test3DScreen() {
        entityCFG = SpaceProject.configManager.getConfig(EntityConfig.class);
        engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
        orthographicCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1.f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f));
        
        generateShip();
    }
    
    private void generateShip() {
        long seed = MathUtils.random(Long.MAX_VALUE);
        Texture body = TextureGenerator.generateShip(seed, MathUtils.random(20, 30));
        Texture wing = TextureGenerator.generateShipWingLeft(seed, (body.getWidth() + 1) / 2);
        Texture shipTop = TextureGenerator.combineShip(body, wing);
        Texture shipBottom = TextureGenerator.generateShipUnderSide(shipTop);
        
        ship3d = new Sprite3D(shipTop, shipBottom, 4);
    }
    
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    
        checkInput(delta);
    
        orthographicCam.position.x = playerX;
        orthographicCam.position.y = playerY;
        
        
        orthographicCam.update();
        batch.setProjectionMatrix(orthographicCam.combined);
        /*
        batch.begin();
        batch.draw(combinedTex, 100,100,50,50);

        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight()-Gdx.input.getY();
        draw(combinedTex, x, y);
        draw(shipTop, x + (int)(combinedTex.getWidth()*SpaceProject.entitycfg.renderScale) + 10, y + 25);
        draw(shipBottom, x + (int)(combinedTex.getWidth()*SpaceProject.entitycfg.renderScale) + 10, y - 25);

        batch.end();
        */
        
        modelBatch.begin(orthographicCam);
        modelBatch.render(ship3d);
        modelBatch.end();
        
        
        //ship3d.worldTransform.rotate(Vector3.X, 90 * delta);
        //ship3d.worldTransform.rotate(Vector3.Y, 60 * delta);
        //ship3d.worldTransform.rotate(Vector3.Z, 90 * delta);
        float faceMouse = MyMath.angleTo(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        ship3d.worldTransform.setToRotation(Vector3.Z, faceMouse * MathUtils.radDeg);
        
        
        ship3d.worldTransform.rotate(Vector3.X, rotX);
        ship3d.worldTransform.setTranslation(playerX, playerY, -50);//bring z closer to camera so it doesn't clip outside the camera's near & far (should be at least sprites width/height)

    }
    
    private void checkInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            generateShip();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            playerY += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            playerY -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            rotX += 400f * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.E)) {
            rotX -= 400f * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            orthographicCam.zoom += 1 * delta;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            orthographicCam.zoom -= 1 * delta;
        }
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
    
    @Override
    public void show() {
        super.show();
    }
    
    @Override
    public void hide() {
        super.hide();
    }
    
    @Override
    public void pause() {
        super.pause();
    }
    
    @Override
    public void resume() {
        super.resume();
    }
    
    @Override
    public void dispose() {
        modelBatch.dispose();
        batch.dispose();
    }
}



