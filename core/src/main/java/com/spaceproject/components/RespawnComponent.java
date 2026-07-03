package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class RespawnComponent implements Component {

    public enum AnimState {
        pause,
        pan,
        //flicker,
        spawn,
        end
    }

    public AnimState spawn;

    public String reason;

    public SimpleTimer timeout;

    public int saveCredits;//todo: revisit save?

}
