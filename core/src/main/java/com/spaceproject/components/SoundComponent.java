package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.ObjectMap;
import de.pottgames.tuningfork.BufferedSoundSource;

public class SoundComponent implements Component {

    //active, looping sources
    public ObjectMap<String, BufferedSoundSource> sources = new ObjectMap<>();

}
