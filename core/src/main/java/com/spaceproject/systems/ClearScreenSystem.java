package com.spaceproject.systems;


import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.screens.GameScreen;

public class ClearScreenSystem extends EntitySystem {
    
    private final Color tmpColor = new Color();
    
    @Override
    public void update(float deltaTime) {
        //clear screen with color based on camera position
        Color color = backgroundColor(GameScreen.cam);
        Gdx.gl20.glClearColor(color.r, color.g, color.b, 1);
        Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    private Color backgroundColor(OrthographicCamera cam) {
        //still playing with these values to get the right feel/intensity of color...
        float camZoomBlackScale = 500.0f;
        float maxColor = 0.25f;
        float ratio = 0.0001f;
        float green = Math.abs(cam.position.x * ratio);
        float blue = Math.abs(cam.position.y * ratio);
        //green based on x position. range amount of green between 0 and maxColor
        if ((int) (green / maxColor) % 2 == 0) {
            green %= maxColor;
        } else {
            green = maxColor - green % maxColor;
        }
        //blue based on y position. range amount of blue between 0 and maxColor
        if ((int) (blue / maxColor) % 2 == 0) {
            blue %= maxColor;
        } else {
            blue = maxColor - blue % maxColor;
        }
        float red = blue + green;
        tmpColor.set(red, green + (maxColor - red) + 0.2f, blue + (maxColor - red) + 0.1f, 1);
        
        tmpColor.lerp(Color.BLACK, MathUtils.clamp(cam.zoom / camZoomBlackScale, 0, 1)); //fade to black on zoom out
        return tmpColor;
    }
    
}
