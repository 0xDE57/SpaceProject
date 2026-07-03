package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class ShaderComponent implements Component {
    
    public enum ShaderType {
        star, grayscale;
    }
    
    public ShaderType shaderType;
    
}
