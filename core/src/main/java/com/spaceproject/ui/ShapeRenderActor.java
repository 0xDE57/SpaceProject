package com.spaceproject.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.spaceproject.math.BlackBodyColorSpectrum;
import com.spaceproject.math.Physics;

public class ShapeRenderActor extends Actor {
    
    private ShapeRenderer shape;

    public ShapeRenderActor() {
        shape = new ShapeRenderer();
    }
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        //POC for custom drawing on actors using shaperender. normally you are limited to the batch drawing
        //TODO: bug, this breaks resizing of the menu when the tab is visible
        //TODO: resize window breaks: does not scale well in fullscreen
        //TODO: also coordinates seem shifted left a bit, looks like padding
        //Note: you must take care to respect the parent alpha
        //1. break out of batch by overriding and ignoring super's draw
        //super.draw(batch, parentAlpha); //<--don't call
        //2. end the current batch
        batch.end();
        
        //3. set shapes projection to copy and match batch position
        shape.setProjectionMatrix(batch.getProjectionMatrix());
        shape.setTransformMatrix(batch.getTransformMatrix());
        shape.translate(getX(), getY(), 0);
        
        //4. do our custom shape rendering
        shape.begin(ShapeRenderer.ShapeType.Line);
        //setBounds(getParent().getX(), getParent().getY(), getParent().getWidth(), getParent().getWidth());
        
        //coords.set(getX(), getY());
        //localToStageCoordinates(coords);
        
        /*
        //debug corners
        shape.setColor(Color.SKY);
        shape.circle(0, 0, 10);
        shape.circle(getWidth() / 2, getHeight() / 2, 10);
        shape.setColor(Color.GREEN);
        shape.circle(0, getHeight(), 10);
        shape.circle(getWidth(), 0, 10);
        shape.circle(getWidth(), getHeight(), 10);
       
        shape.setColor(1, 1, 0, 1);
        shape.rect(0, 0, getWidth(), getHeight());
        */
        
        //debug black body radiation, color temperature
        float x = 10;
        float y = 10;
        float width = getWidth() - x * 2 ;
        float height = 100;
        float lowestVisibleTemp  = (float) Physics.wavelengthToTemperature(380) * 1000000;
        float highestVisibleTemp = (float) Physics.wavelengthToTemperature(780) * 1000000;
        int fromKelvin = 1000;
        int toKelvin = 50000;
        int gap = 20;
        
        //render full with black bars outside vision
        renderPeakWavelengthSpectrum(x, y, width, height, lowestVisibleTemp, highestVisibleTemp, fromKelvin, toKelvin);
        //render visible only
        float y2 = y + height + gap;
        renderPeakWavelengthSpectrum(x, y2, width, height, lowestVisibleTemp, highestVisibleTemp, highestVisibleTemp, lowestVisibleTemp);
        //render blackbody specrend
        float y3 = y2 + height + gap;
        renderBlackBodySpectrum(x, y3, width, height, fromKelvin, toKelvin);
        
        
        //TODO: plot power spectrum for wavelength
        /*
        //for temperature k, draw spectrum plot
        double kelvin = Physics.Sun.kelvin;//5772k
        int wavelengthStart = 480; int wavelengthEnd = 780;
        double[] spectrum = new double[(wavelengthEnd - wavelengthStart)];
        int index = 0;
        for (int wavelength = wavelengthStart; wavelength < wavelengthEnd; wavelength++) {
            spectrum[index] = Physics.calcSpectralRadiance(wavelength, kelvin);
            index ++;
        }*/

        
        //5. end our shape
        shape.end();
        //6. we must begin the batch again for actors that are drawn after us
        batch.begin();
    }
    
    private void renderPeakWavelengthSpectrum(float x, float y, float width, float height, float lowestVisibleTemp, float highestVisibleTemp, float fromKelvin, float toKelvin) {
        int border = 4;
        
        for (int i = 0; i < width; i++) {
            float percent = (float) i / width;
            double temperature = MathUtils.lerp(fromKelvin, toKelvin, percent);
            double wavelength = Physics.temperatureToWavelength(temperature) * 1000000;
            
            //border
            if (temperature <= lowestVisibleTemp && temperature >= highestVisibleTemp) {
                //within visible range
                shape.setColor(1, 1, 1,1);
            } else {
                shape.setColor(0,0,0,1);
            }
            shape.line(x + i, y - border, x + i, y + height + border);
            
            //spectrum
            int[] colorTemp = Physics.wavelengthToRGB(wavelength);
            shape.setColor(
                    colorTemp[0] / 255.0f, // red
                    colorTemp[1] / 255.0f, // green
                    colorTemp[2] / 255.0f, // blue
                    1); //alpha
            shape.line(x + i, y, x + i, y + height);
        }
        
        shape.setColor(1, 1, 1, 1);
        shape.rect(x, y, width, height);
    }
    
    private void renderBlackBodySpectrum(float x, float y, float width, float height, float fromKelvin, float toKelvin) {
        int border = 4;
        
        for (int i = 0; i < width; i++) {
            float percent = (float) i / width;
            
            //calculate spectrum
            double temperature = MathUtils.lerp(fromKelvin, toKelvin, percent);
            Vector3 spectrum = BlackBodyColorSpectrum.spectrumToXYZ(temperature);
            Vector3 color = BlackBodyColorSpectrum.xyzToRGB(BlackBodyColorSpectrum.SMPTEsystem, spectrum.x, spectrum.y, spectrum.z);
            boolean constrained = BlackBodyColorSpectrum.constrainRGB(color);
            Vector3 normal = BlackBodyColorSpectrum.normRGB(color.x, color.y, color.z);
    
            //draw border
            if (constrained) {
                shape.setColor(1, 0, 0, 1);
            } else {
                shape.setColor(1, 1, 1, 1);
            }
            shape.line(x + i, y - border, x + i, y + height + border);
            
            //draw spectrum
            shape.setColor(normal.x, normal.y, normal.z, 1);
            shape.line(x + i, y, x + i, y + height);
        }
    
        shape.setColor(1, 1, 1, 1);
        shape.rect(x, y, width, height);
    }
    
}
