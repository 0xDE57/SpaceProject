package com.spaceproject.generation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.math.BlackBodyColorSpectrum;
import com.spaceproject.math.MyMath;
import com.spaceproject.noise.NoiseGen;
import com.spaceproject.noise.OpenSimplexNoise;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.Tile;

import java.util.ArrayList;

public class TextureGenerator {
    
    public static Texture generateCharacter() {
        Pixmap pixmap = new Pixmap(4, 4, Format.RGB565);
        
        //fill square
        pixmap.setColor(0, 0, 0 , 1);
        pixmap.fill();
        
        //draw face/eyes (front of character)
        pixmap.setColor(1, 1, 1, 1);
        pixmap.drawPixel(3, 2);
        pixmap.drawPixel(3, 1);
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    //region space background dust n stars
    static OpenSimplexNoise alphaNoise = new OpenSimplexNoise(GameScreen.getGalaxySeed());
    static OpenSimplexNoise redNoise = new OpenSimplexNoise(GameScreen.getGalaxySeed() + 1);
    static OpenSimplexNoise blueNoise = new OpenSimplexNoise(GameScreen.getGalaxySeed() + 2);
    
    public static Texture generateSpaceBackgroundDust(int tX, int tY, int tileSize, Pixmap.Format format) {
        Pixmap pixmap = new Pixmap(tileSize, tileSize, format);
        boolean debugVisual = false;
        boolean debugSave = false;
        
        double featureSize = 100;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                //position
                double nX = (x + (tX * tileSize)) / featureSize;
                double nY = (y + (tY * tileSize)) / featureSize;
                
                //opacity
                double opacity = alphaNoise.eval(nX, nY, 0);
                opacity = (opacity * 0.5) + 0.5; //normalize from range [-1:1] to [0:1]
                
                //red
                double red = redNoise.eval(nX, nY, 0);
                red = (red * 0.5) + 0.5;
                
                //blue
                double blue = blueNoise.eval(nX, nY, 0);
                blue = (blue * 0.5) + 0.5;
                
                //draw
                pixmap.setColor(new Color((float) red, 0, (float) blue, (float) opacity));
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y);
            }
        }

        if (debugVisual) {
            //DEBUG - fill tile to visualize boundaries
            pixmap.setColor(MathUtils.random(), MathUtils.random(), MathUtils.random(), 0.5f);
            pixmap.fill();
            pixmap.setColor(1, 1, 1, 1);
            pixmap.drawPixel(0, 0);
        }
        
        Texture tex = new Texture(pixmap);
        if (debugSave) {
            FileHandle file = Gdx.files.external("test/" + tX + "," + tY + ".png");
            Gdx.app.error("saved pixmap", file.path());
            PixmapIO.writePNG(file, pixmap);
        }
        pixmap.dispose();
        return tex;
    }
 
    public static Texture generateSpaceDust(long seed, int tileSize, float scale) {
        Pixmap pixmap = new Pixmap(tileSize, tileSize, Format.RGBA8888);
        
        float[][] heightMap = NoiseGen.generateWrappingNoise4D(seed, tileSize, scale, 1, 1, 1);
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getHeight(); ++x) {

                int gX = (x + 100) % tileSize;
                int gY = (y + 100) % tileSize;
                float green = heightMap[gX][gY];

                int bX = (x + 25) % tileSize;
                int bY = (y + 25) % tileSize;
                float blue = heightMap[bX][bY];
                
                float height = heightMap[x][y];
                pixmap.setColor(0, green, blue, height);
                pixmap.drawPixel(x, y);
            }
        }

        Texture tex = new Texture(pixmap);
        //linear filtering samples neighboring cells = smoother (individual texels less obvious)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return tex;
    }
    
    public static Texture generateSpaceBackgroundStars(int tileX, int tileY, int tileSize, float depth) {
        MathUtils.random.setSeed((long) (MyMath.getSeed(tileX, tileY) * (depth * 1000)));
        Pixmap pixmap = new Pixmap(tileSize, tileSize, Format.RGBA4444);
        boolean debugVisual = false;
        int numStars = 200;
        for (int i = 0; i < numStars; ++i) {
            int x = MathUtils.random(tileSize);
            int y = MathUtils.random(tileSize);

            //give star a random temperature
            double temperature = MathUtils.random(1000, 40000); //kelvin

            //calculate black body radiation color for temperature
            Vector3 spectrum = BlackBodyColorSpectrum.spectrumToXYZ(temperature);
            Vector3 color = BlackBodyColorSpectrum.xyzToRGB(BlackBodyColorSpectrum.SMPTEsystem, spectrum.x, spectrum.y, spectrum.z);
            BlackBodyColorSpectrum.constrainRGB(color);
            Vector3 normal = BlackBodyColorSpectrum.normRGB(color.x, color.y, color.z);
            pixmap.setColor(normal.x, normal.y, normal.z, 1);

            /*
            double peakWavelength = Physics.temperatureToWavelength(temperature) * 1000000;
            int[] colorTemp = Physics.wavelengthToRGB(peakWavelength);
            if (colorTemp[0] == 0 && colorTemp[1] == 0 && colorTemp[2] == 0) {
                //override bodies outside the visible spectrum and just render white
                //pixmap.setColor(1, 1, 1, MathUtils.random(0.1f, 1f));
            } else {
                pixmap.setColor(
                        colorTemp[0] / 255.0f, // red
                        colorTemp[1] / 255.0f, // green
                        colorTemp[2] / 255.0f, // blue
                        MathUtils.random(0.1f, 1f));
            }*/

            pixmap.drawPixel(x, y);
        }

        if (debugVisual) {
            //DEBUG - fill tile to visualize boundaries
            pixmap.setColor(MathUtils.random(), MathUtils.random(), MathUtils.random(), 0.5f);
            pixmap.fill();
        }
        
        //create texture and dispose pixmap to prevent memory leak
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    //endregion
    
    //region planet and star game objects
    public static Texture generatePlanet(int[][] tileMap, ArrayList<Tile> tiles) {
        int size = tileMap.length;
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        
        // draw circle for planet
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        
        //draw noise
        for (int y = 0; y < size; ++y) {
            for (int x = 0; x < size; ++x) {
                //only draw on circle
                if (pixmap.getPixel(x, y) != 0) {
                    pixmap.setColor(tiles.get(tileMap[x][y]).getColor());
                    pixmap.drawPixel(x, y);
                }
            }
        }
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    public static Texture generatePlanetPlaceholder(int mapSize, int chunkSize) {
        int size = mapSize / chunkSize;//SIZE = chunks = tileMap.length/chunkSize
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        
        // draw circle for planet
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    /** generate circular grayscale heightmap to represent star and features */
    public static Texture generateStar(long seed, int radius, double scale) {
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        //Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA4444);
        Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA8888);
        
        // draw circle
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
        pixmap.fillCircle(radius, radius, radius - 1);
        
        //add layer of noise
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getWidth(); ++x) {
                //only draw on circle
                if (pixmap.getPixel(x, y) != 0) {
                    double nx = x / scale, ny = y / scale;
                    float i = (float)noise.eval(nx, ny, 0);
                    i = (i * 0.5f) + 0.5f; //normalize from range [-1:1] to [0:1]
                    pixmap.setColor(i, i, i, 1);
                    pixmap.drawPixel(x, y);
                }
            }
        }
        
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
    
    /** old style hard coded color */
    public static Texture generateStaticStar(long seed, int radius) {
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        Pixmap pixmap = new Pixmap(radius * 2, radius * 2, Format.RGBA4444);
        
        double scale = 20;//zoom
        
        // draw circle
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1);
        pixmap.fillCircle(radius, radius, radius - 1);
        
        //add layer of noise
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getWidth(); ++x) {
                //only draw on circle
                if (pixmap.getPixel(x, y) != 0) {
                    double nx = x / scale, ny = y / scale;
                    float i = (float)noise.eval(nx, ny, 0);
                    i = (i * 0.5f) + 0.5f; //normalized from range [-1:1] to [0:1]
                    
                    if (i > 0.5f) {
                        pixmap.setColor(1, 1, 0, i);
                    } else {
                        pixmap.setColor(1, 0, 0, (1 - i));
                    }
                    pixmap.setColor(i, i, i, 1);
                    pixmap.drawPixel(x, y);
                }
            }
        }
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    //endregion
    
    //region ship
    public static Texture generateShip(long seed, int size) {
        MathUtils.random.setSeed(seed);
        
        boolean debugImage = false;
        
        // generate pixmap texture
        Pixmap pixmap = new Pixmap(size, size / 2, Format.RGBA4444);
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        
        // smallest height a ship can be (4 because player is 4 pixels)
        int minEdge = 4;
        // smallest starting point for an edge
        float initialMinimumEdge = height * 0.8f;
        // edge to create shape of ship. initialize to random starting size
        int edge = MathUtils.random((int) initialMinimumEdge, height - 1);
        
        for (int yY = 0; yY <= width; yY++) {
            // draw body
            if (yY == 0) {
                // if first or last position of texture, "cap" it to complete the edging
                pixmap.setColor(Color.RED);
            } else if (yY == width) {
                // if first or last position of texture, "cap" it to complete the edging
                pixmap.setColor(Color.CYAN);
            } else {
                pixmap.setColor(Color.BLACK);
            }
            
            if (!debugImage) {
                pixmap.drawLine(yY, edge, yY, height - edge);
            }
            
            // draw edging
            pixmap.setColor(Color.WHITE);
            pixmap.drawPixel(yY, edge);// bottom edge
            pixmap.drawPixel(yY, height - edge);// top edge
            
            // generate next edge
            // beginning and end of ship have special rule to not be greater
            // than the consecutive or previous edge
            // so that the "caps" look right
            if (yY == 0) { // beginning
                ++edge;
            } else if (yY == width - 1) { // end
                --edge;
            } else { // body
                // random decide to move edge.
                // if so, move edge either up or down 1 pixel
                edge = MathUtils.randomBoolean() ? (MathUtils.randomBoolean() ? --edge : ++edge) : edge;
            }
            
            // keep edges within height and minEdge
            if (edge > height) {
                edge = height;
            }
            if (edge - (height - edge) < minEdge) {
                edge = (height + minEdge) / 2;
            }
        }
        
        if (debugImage) {
            // 0-----width
            // |
            // |
            // |
            // height
            
            // fill to see image size/visual aid---------
            //pixmap.setColor(1, 1, 1, 1);
            //pixmap.fill();
            
            // mini pins for visual aid----------------
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        // create texture and dispose pixmap to prevent memory leak
        Texture t = new Texture(pixmap);
        return t;
    }
    
    public static Texture generateShipWingLeft(long seed, int size) {
        MathUtils.random.setSeed(seed);
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        
        pixmap.setColor(Color.BLACK);
        //pixmap.setColor(0, 0.4f, 0, 1);
        pixmap.fillTriangle(
                0, 0,  //top-right
                0, height, //bottom-left
                width, height);//bottom-right
        //pixmap.setColor(0.6f, 0.6f, 0.6f, 1);
        pixmap.setColor(Color.WHITE);
        pixmap.drawLine(0, 0, width, height);
        pixmap.drawLine(0, 0, 0, height);
        
        boolean debug = false;
        if (debug) {
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        return new Texture(pixmap);
    }
    
    public static Texture FlipTexture(Texture originalTex, boolean flipX, boolean flipY) {
        Pixmap pixmap = new Pixmap(originalTex.getWidth(), originalTex.getHeight(), Format.RGBA4444);
        Pixmap orig = originalTex.getTextureData().consumePixmap();
        for (int y = 0; y < originalTex.getHeight(); y++) {
            for (int x = 0; x < originalTex.getWidth(); x++) {
                pixmap.drawPixel(
                        flipX ? originalTex.getWidth() - 1 - x : x,
                        flipY ? originalTex.getHeight() - 1 - y : y,
                        orig.getPixel(x, y));
            }
        }
        return new Texture(pixmap);
    }
    
    public static Texture combineShip(Texture body, Texture leftWing) {
        int wingOffsetX = 1;
        int wingOffsetY = 4;
        int totalHeight = body.getHeight() + ((leftWing.getHeight() - wingOffsetY) * 2);
        Pixmap pixmap = new Pixmap(body.getWidth(), totalHeight, Format.RGBA4444);
        Texture rightWing = FlipTexture(leftWing, false, true);
        pixmap.drawPixmap(leftWing.getTextureData().consumePixmap(), wingOffsetX, 0);
        pixmap.drawPixmap(rightWing.getTextureData().consumePixmap(), wingOffsetX, totalHeight - rightWing.getHeight());
        pixmap.drawPixmap(body.getTextureData().consumePixmap(), 0, (totalHeight / 2) - (body.getHeight() / 2));
        
        int width = pixmap.getWidth() - 1;
        int height = pixmap.getHeight() - 1;
        boolean debug = false;
        if (debug) {
            pixmap.setColor(1, 0, 0, 1);// red: top-right
            pixmap.drawPixel(0, 0);
            
            pixmap.setColor(0, 1, 0, 1);// green: top-left
            pixmap.drawPixel(width, 0);
            
            pixmap.setColor(0, 0, 1, 1);// blue: bottom-left
            pixmap.drawPixel(0, height);
            
            pixmap.setColor(1, 1, 0, 1);// yellow: bottom-right
            pixmap.drawPixel(width, height);
        }
        
        return new Texture(pixmap);
    }
    
    public static Texture generateShipUnderSide(Texture shipTop) {
        Pixmap pixmap = new Pixmap(shipTop.getWidth(), shipTop.getHeight(), Format.RGBA4444);
        Pixmap orig = shipTop.getTextureData().consumePixmap();
        for (int y = 0; y < shipTop.getHeight(); y++) {
            for (int x = 0; x < shipTop.getWidth(); x++) {
                int color = orig.getPixel(x, y);
                if (color != 0) {
                    Color c = new Color(color);
                    c.set(1-c.r, 1-c.g,1-c.b, 1); //invert
                    pixmap.drawPixel(x, y, Color.rgba8888(c));
                }
            }
        }
        
        return new Texture(pixmap);
    }
    //endregion
    
    //region projectile
    public static Texture generateProjectile() {
        return generateProjectile(3, 2);
    }
    
    public static Texture generateProjectile(int length, int width) {
        Pixmap pixmap = new Pixmap(length, width, Format.RGB565);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    //endregion
    
    public static Texture generateTriangle(int size, Color color) {
        Pixmap pixmap = new Pixmap(size, size, Format.RGB565);
    
        //fill square
        pixmap.setColor(color);
        pixmap.fillTriangle(
                size, (int) (size * 0.5f),
                0, 0,
                0, size);
    
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    
    //region planet game objects
    public static Texture generateTexture(int width, int height, Color innerColor, Color outerColor) {
        Pixmap pixmap = new Pixmap(width, height, Format.RGB565);
        
        //fill square
        pixmap.setColor(innerColor);
        pixmap.fill();
        pixmap.setColor(outerColor);
        pixmap.drawRectangle(0, 0, width, height);
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    //endregion
    
    //region test
    public static Texture createTile(Color c) {
        Pixmap pixmap;
        pixmap = new Pixmap(1, 1, Format.RGB888);
        pixmap.setColor(c);
        pixmap.fill();
        
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }
    
    public static Texture createTestTile() {
        Pixmap pixmap;
        pixmap = new Pixmap(4, 4, Format.RGB888);
        pixmap.drawPixel(0, 0, Color.RED.toIntBits());
        pixmap.drawPixel(0, 1, Color.YELLOW.toIntBits());
        pixmap.drawPixel(1, 0, Color.BLUE.toIntBits());
        pixmap.drawPixel(1, 1, Color.GREEN.toIntBits());
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }
    
    public static Texture generateNoise(long seed, int size, double featureSize) {
        OpenSimplexNoise noise = new OpenSimplexNoise(seed);
        Pixmap pixmap = new Pixmap(size, size, Format.RGBA4444);
        
        //add layer of noise
        for (int y = 0; y < pixmap.getHeight(); ++y) {
            for (int x = 0; x < pixmap.getWidth(); ++x) {
                
                double nx = x / featureSize, ny = y / featureSize;
                double i = noise.eval(nx, ny, 0);
                i = (i * 0.5) + 0.5; // convert from range [-1:1] to [0:1]
                
                pixmap.setColor(new Color((float) i, (float) i, (float) i, 1));
                pixmap.drawPixel(x, y);
            }
        }
        
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }
    //endregion
    
}
