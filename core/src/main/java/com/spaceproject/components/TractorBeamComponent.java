package com.spaceproject.components;

import com.badlogic.ashley.core.Component;

public class TractorBeamComponent implements Component {

    public enum State {
        off, push, pull;
    }

    public State state = State.off;

    public State mode = State.push; //push/pull toggle

    public float range;

    public float magnitude;

}
