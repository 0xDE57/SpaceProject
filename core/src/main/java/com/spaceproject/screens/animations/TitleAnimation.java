package com.spaceproject.screens.animations;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class TitleAnimation {
    
    public abstract void render(float delta, ShapeRenderer shape);
    
    public abstract void resize(int width, int height);
}
