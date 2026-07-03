package com.spaceproject.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.math.MyMath;

import java.util.ArrayList;
import java.util.Collections;

public class Tile implements Comparable<Tile> {
    public static ArrayList<Tile> defaultTiles = getDefault();
    private static int nextID;
    
    private final int id;
    private String name;
    private float height;
    private Color color;
    
    public Tile(String name, float height, Color color) {
        id = nextID++;
        
        if (name == null) {
            name = "color" + id;
        }
        
        this.name = name;
        this.height = height;
        this.color = color.cpy();
    }
    
    public int getID() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public float getHeight() {
        return height;
    }
    
    public void setHeight(float height) {
        this.height = MathUtils.clamp(height, 0, 1);
    }
    
    @Override
    public int compareTo(Tile o) {
        return (int) Math.signum(o.getHeight() - this.getHeight());
    }
    
    @Override
    public String toString() {
        return String.format("%-5s", MyMath.round(getHeight(), 3)).replace(' ', '0') + " " + getName();
    }
    
    //TODO: find better place for this.
    //-also loading and unloading to json.
    //-also support for different profiles.
    private static ArrayList<Tile> getDefault() {
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        tiles.add(new Tile("water", 0.41f, Color.BLUE));
        tiles.add(new Tile("water1", 0.345f, new Color(0, 0, 0.42f, 1)));
        tiles.add(new Tile("water2", 0.240f, new Color(0, 0, 0.23f, 1)));
        tiles.add(new Tile("water3", 0.085f, new Color(0, 0, 0.1f, 1)));
        tiles.add(new Tile("sand", 0.465f, Color.YELLOW));
        tiles.add(new Tile("grass", 0.625f, Color.GREEN));
        tiles.add(new Tile("grass1", 0.725f, new Color(0, 0.63f, 0, 1)));
        tiles.add(new Tile("grass2", 0.815f, new Color(0, 0.48f, 0, 1)));
        tiles.add(new Tile("lava", 1f, Color.RED));
        tiles.add(new Tile("rock", 0.95f, Color.BROWN));
        Collections.sort(tiles);
        return tiles;
    }
    
}