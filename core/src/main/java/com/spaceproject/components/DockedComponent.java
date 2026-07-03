package com.spaceproject.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;

//flag component
public class DockedComponent implements Component {

    public Entity parent;

    public String dockID;

}
