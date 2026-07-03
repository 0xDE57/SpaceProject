package com.spaceproject.screens.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.noise.NoiseGen;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;
import com.spaceproject.ui.Tile;
import com.spaceproject.ui.custom.Button;
import com.spaceproject.ui.custom.ColorProfile;
import com.spaceproject.ui.custom.Slider;
import com.spaceproject.math.MyMath;

/* TODO:
 * -mapsize
 * -zoom into mouse position
 * -text boxes for names and seed
 * -saving and loading color/feature profiles to json
 * -fix screen skew on resize
 */
public class TestNoiseScreen extends MyScreenAdapter implements InputProcessor {
    
    SpriteBatch batch = new SpriteBatch();
    ShapeRenderer shape = new ShapeRenderer();
    private BitmapFont font;
    
    long seed; //make textbox
    
    int mapRenderWindowSize = 400;
    int mapSize = 120;//make slider
    int pixelSize = 3;//zoom
    
    float[][] heightMap;
    //int[][] tileMap;
    //int[][] pixelatedTileMap;
    
    //feature sliders
    Slider scale, octave, persistence, lacunarity;
    
    Button visitWorld;
    
    //color picking tool
    ColorProfile colorProfile;
    
    boolean mouseDown = false;
    int offsetX = 0;
    int offsetY = 0;
    int prevX = 0;
    int prevY = 0;
    
    int mapX, mapY;
    
    public TestNoiseScreen() {
        getInputMultiplexer().addProcessor(this);
        
        font = FontLoader.createFont(FontLoader.fontBitstreamVMBold, 15);
        
        seed = MathUtils.random(Long.MAX_VALUE);
        
        int width = 200;
        int height = 30;
        int buttonWidth = 20;
        scale = new Slider("scale", 1, 100, 40, 180, buttonWidth, width, height);//1 - x
        octave = new Slider("octave", 1, 6, 40, 140, buttonWidth, width, height);//1 - x
        persistence = new Slider("persistence", 0, 1, 40, 100, buttonWidth, width, height);//0 - 1
        lacunarity = new Slider("lacunarity", 0, 5, 40, 60, buttonWidth, width, height);//0 - x
        
        visitWorld = new Button("Visit World", Gdx.graphics.getWidth()-100, 0, 100, 40);
        
        colorProfile = new ColorProfile(500, 50, 50, 200);
        loadTestProfile();
        updateMap();
        
        mapX = 20;
        mapY = Gdx.graphics.getHeight() - pixelSize - 20;
        //mapY = Gdx.graphics.getHeight() - heightMap.length*pixelSize - 20;
        
    }
    
    private void loadTestProfile() {
        colorProfile.getTiles().addAll(Tile.defaultTiles);
        
        scale.setValue(100);
        octave.setValue(4);
        persistence.setValue(0.68f);
        lacunarity.setValue(2.6f);
    }
    
    public void render(float delta) {
        super.render(delta);
        
        Gdx.gl20.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        
        shape.begin(ShapeRenderer.ShapeType.Filled);
        
        //draw noise map
        drawMap();
        
        //drawMapLerped();
        
        drawPixelatedMap();
        
        //draw UI tool
        colorProfile.draw(shape);
        
        shape.end();
        
        
        batch.begin();
        
        //draw UI tool
        colorProfile.draw(batch, font);
        
        //draw feature sliders
        scale.draw(batch, font);
        octave.draw(batch, font);
        persistence.draw(batch, font);
        lacunarity.draw(batch, font);
        
        visitWorld.draw(batch, font);
        
        font.draw(batch, "Seed: " + seed, 15, Gdx.graphics.getHeight() - 15);
        font.draw(batch, "Zoom: " + pixelSize, 15, Gdx.graphics.getHeight() - 30);
        batch.end();
        
        
        updateClickDragMapOffset();
        
        
        boolean change = false;
        //update
        colorProfile.update();
        
        
        if (scale.update() || octave.update()
                || persistence.update() || lacunarity.update()) {
            change = true;
        }
        
        if (visitWorld.isClicked()) {
            //todo:
            game.setScreen(new GameScreen());
        }
        
        //TODO: make UI sliders for these values
        if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            seed = MathUtils.random(Long.MAX_VALUE);
            change = true;
        }
        
        if (Gdx.input.isKeyPressed(Keys.EQUALS)) {
            pixelSize += 0.5;
            change = true;
        }
        if (Gdx.input.isKeyPressed(Keys.MINUS)) {
            pixelSize -= 0.5;
            change = true;
        }
        
        if (change) {
            updateMap();
        }
        
        
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            MyScreenAdapter.game.setScreen(new TitleScreen(MyScreenAdapter.game));
        }
        
    }
    
    private void updateClickDragMapOffset() {
        // click and drag move map around
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (Gdx.input.justTouched() && mouseX > mapX && mouseX < mapX + mapRenderWindowSize
                && mouseY < mapY && mouseY > mapY - mapRenderWindowSize) {
            mouseDown = true;
            
            prevX = offsetX + mouseX / pixelSize;
            prevY = offsetY - mouseY / pixelSize;
        }
        
        if (Gdx.input.isTouched() && mouseDown) {
            offsetX = prevX - mouseX / pixelSize;
            offsetY = prevY + mouseY / pixelSize;
        } else {
            mouseDown = false;
        }
    }
    
    private void updateMap() {
        double s = scale.getValue();
        int o = (int) octave.getValue();
        float p = persistence.getValue();
        float l = lacunarity.getValue();
        heightMap = NoiseGen.generateWrappingNoise4D(seed, mapSize, s, o, p, l);
        //heightMap = new float[mapSize][mapSize];
        //tileMap = NoiseGen.createTileMap(heightMap, colorProfile.getTiles());
        //pixelatedTileMap = NoiseGen.createPixelatedTileMap(tileMap, colorProfile.getTiles());
        shadowMap = NoiseGen.createShadowMap(heightMap,
                new Vector3((Gdx.input.getX() / pixelSize) - (mapX / pixelSize), (Gdx.input.getY() / pixelSize) - (mapX / pixelSize), 1));
    }
    
    float[][] shadowMap;// = new float[0][0];
    
    
    private void drawMap() {
        for (int y = 0; y * pixelSize <= mapRenderWindowSize; y++) {
            for (int x = 0; x * pixelSize <= mapRenderWindowSize; x++) {
                
                //wrap tiles
                int tX = (x + offsetX) % heightMap.length;
                int tY = (y + offsetY) % heightMap.length;
                if (tX < 0) tX += heightMap.length;
                if (tY < 0) tY += heightMap.length;
                
                float height = heightMap[tX][tY];
                
                
                //pick color
                Color tileColor = new Color();
                for (int k = colorProfile.getTiles().size() - 1; k >= 0; k--) {
                    Tile tile = colorProfile.getTiles().get(k);
                    if (height <= tile.getHeight() || k == 0) {
                        tileColor = tile.getColor().cpy();
                        break;
                    }
                }
                
                if (colorProfile.getTiles().isEmpty()) {
                    tileColor = new Color(height, height, height, height).cpy();
                }
                
                //tileColor = Color.WHITE.cpy();
                
                //draw grid to visualize edge/wrap
                if (tX == heightMap.length - 1 || tY == heightMap.length - 1) {
                    tileColor = Color.BLACK.cpy();
                }
				
				
				/*
				float shadow = shadowMap[tX][tY];
				tileColor.sub(shadow, shadow, shadow, 1);
				*/
                
                //draw
                shape.setColor(tileColor);
                shape.rect(mapX + x * pixelSize, mapY - y * pixelSize, pixelSize, pixelSize);
                
                //grayscale debug
                shape.setColor(height, height, height, height);
                shape.rect(mapX + x * pixelSize + mapRenderWindowSize, mapY - y * pixelSize, pixelSize, pixelSize);
            }
        }
    }
    
    static int round(float n) {
        if (n - ((int) n) >= 0.5)
            return (int) n + 1;
        
        return (int) n;
    }
    
    
    private void drawMapLerped() {
        for (int y = 0; y * pixelSize <= mapRenderWindowSize; y++) {
            for (int x = 0; x * pixelSize <= mapRenderWindowSize; x++) {
                
                //wrap tiles
                int tX = (x + offsetX) % heightMap.length;
                int tY = (y + offsetY) % heightMap.length;
                if (tX < 0) tX += heightMap.length;
                if (tY < 0) tY += heightMap.length;
                
                //pick color
                float i = heightMap[tX][tY];
                for (int k = colorProfile.getTiles().size() - 1; k >= 0; k--) {
                    Tile tile = colorProfile.getTiles().get(k);
                    
                    if (i <= tile.getHeight() || k == 0) {
                        if (k == colorProfile.getTiles().size() - 1) {
                            shape.setColor(tile.getColor());
                            break;
                        }
                        Tile next = colorProfile.getTiles().get(k + 1);
                        float gradient = MyMath.inverseLerp(next.getHeight(), tile.getHeight(), i);
                        shape.setColor(next.getColor().cpy().lerp(tile.getColor(), gradient));
                        
                        break;
                    }
                }
                
                //draw grid to visualize wrap
                if (tX == heightMap.length - 1 || tY == heightMap.length - 1) {
                    shape.setColor(Color.BLACK);
                }
                
                //draw
                shape.rect(mapX + x * pixelSize, mapY - y * pixelSize, pixelSize, pixelSize);
                
                //grayscale debug
                shape.setColor(i, i, i, i);
                shape.rect(mapX + x * pixelSize + mapRenderWindowSize, mapY - y * pixelSize, pixelSize, pixelSize);
            }
        }
    }
    
    private void drawPixelatedMap() {
        if (colorProfile.getTiles().isEmpty()) {
            return;
        }
		/*
		int renderSize = 8;
		int mapX = Gdx.graphics.getWidth() - pixelatedTileMap.length*renderSize - 20;
		int mapY = Gdx.graphics.getHeight() - 20;
		
		for (int y = 0; y < pixelatedTileMap.length; y++) {
			for (int x = 0; x < pixelatedTileMap.length; x++) {
				
				int tX = (x + offsetX) % pixelatedTileMap.length;
				int tY = (y + offsetY) % pixelatedTileMap.length;
				if (tX < 0) tX += pixelatedTileMap.length;
				if (tY < 0) tY += pixelatedTileMap.length;
				
				shape.setColor(colorProfile.getTiles().get(pixelatedTileMap[tX][tY]).getColor());
				shape.rect(mapX + x*renderSize, mapY - y*renderSize, renderSize, renderSize);
			}
		}*/
        
        //int renderSize = 8;
        int chunkSize = 8; //chunkSize must evenly divide into mapsize
        int chunks = heightMap.length / chunkSize;
        
        int mapX = Gdx.graphics.getWidth() - chunks * chunkSize - 20;
        int mapY = Gdx.graphics.getHeight() - 20;
        //for each chunk
        for (int cY = 0; cY < chunks; cY++) {
            for (int cX = 0; cX < chunks; cX++) {
                
                int chunkX = cX * chunkSize;
                int chunkY = cY * chunkSize;
                
                int[] count = new int[colorProfile.getTiles().size()];
                
                //for each tile in chunk, count occurrence of tiles within a chunk
                for (int y = chunkY; y < chunkY + chunkSize; y++) {
                    for (int x = chunkX; x < chunkX + chunkSize; x++) {
                        
                        //wrap tiles
                        int tX = (x + offsetX) % heightMap.length;
                        int tY = (y + offsetY) % heightMap.length;
                        if (tX < 0) tX += heightMap.length;
                        if (tY < 0) tY += heightMap.length;
                        
                        //count colors
                        float i = heightMap[tX][tY];
                        for (int k = colorProfile.getTiles().size() - 1; k >= 0; k--) {
                            Tile tile = colorProfile.getTiles().get(k);
                            if (i <= tile.getHeight() || k == 0) {
                                count[k]++;
                                break;
                            }
                        }
                    }
                }
                
                //set color to highest tile count
                int index = 0;
                for (int i = 0; i < count.length; i++) {
                    if (count[i] > count[index]) {
                        index = i;
                    }
                }
                shape.setColor(colorProfile.getTiles().get(index).getColor());
                shape.rect(mapX + chunkX, mapY - chunkY, chunkSize, chunkSize);
                
            }
        }
        
        
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        pixelSize = (int) MathUtils.clamp(pixelSize - amountY, 1, 32);
		
		/*
		int mouseX = Gdx.input.getX();
		int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
		//prevX = offsetX + mouseX / pixelSize;
		//prevY = offsetY - mouseY / pixelSize;
		offsetX = mouseX / pixelSize;
		offsetY = mouseY / pixelSize;*/
        
        //mapY = Gdx.graphics.getHeight() - heightMap.length*pixelSize - 20;
        mapY = Gdx.graphics.getHeight() - pixelSize - 20;
        return false;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        return false;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    //@Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }
    
}
