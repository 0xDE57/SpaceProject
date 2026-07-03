package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Texture;

public class TextureComponent implements Component {
    //texture/image
    public Texture texture = null;
    //scale/size of image
    public float scale = 1.0f;
}
