package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.spaceproject.utility.SimpleTimer;

public class PassiveShieldComponent implements Component {

    public float protection;

    public float currentShield;

    public float cooldownSeconds;

    public SimpleTimer cooldownTimer;

    public boolean isOverheated;

    public PassiveShieldComponent() {
    }
}
