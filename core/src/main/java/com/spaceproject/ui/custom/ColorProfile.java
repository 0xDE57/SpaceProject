package com.spaceproject.ui.custom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.spaceproject.ui.Tile;

import java.util.ArrayList;
import java.util.Collections;

public class ColorProfile {
    //column dimensions
    int columnX;
    int columnY;
    int colWidth;
    int colHeight;
    
    //height modification button
    int buttonSize = 15;
    int buttonPadding = 5;
    
    //tile ID's (-1 = none selected)
    int selectedColorID = -1;
    int lastSelectedID = -1;
    
    //colors in column
    private ArrayList<Tile> tiles = new ArrayList<Tile>();
    
    //color of tile
    Slider red, green, blue;
    //add or remove colors
    Button add, remove;
    
    public ColorProfile(int x, int y, int width, int height) {
        columnX = x;
        columnY = y;
        colWidth = width;
        colHeight = height;
        
        addSliders();
        
        addButtons();
    }
    
    private void addButtons() {
        int buttonWidth = 60;
        int buttonHeight = 22;
        int buttonOffsetX = buttonWidth * 2 - 35;
        int buttonOffsetY = columnY;
        add = new Button("add", columnX - buttonOffsetX - buttonWidth - 6, buttonOffsetY, buttonWidth, buttonHeight);
        remove = new Button("remove", columnX - buttonOffsetX, buttonOffsetY, buttonWidth, buttonHeight);
    }
    
    private void addSliders() {
        int sldWidth = 100;
        int sldHeight = 16;
        int btnWidth = 10;
        int sldPadding = 5;
        int offsetX = sldWidth + 30;
        int offsetY = 40;
        red = new Slider("r", 0, 1, columnX - offsetX, columnY + offsetY + (sldHeight + sldPadding) * 2, btnWidth, sldWidth, sldHeight);
        green = new Slider("g", 0, 1, columnX - offsetX, columnY + offsetY + sldHeight + sldPadding, btnWidth, sldWidth, sldHeight);
        blue = new Slider("b", 0, 1, columnX - offsetX, columnY + offsetY, btnWidth, sldWidth, sldHeight);
    }
    
    public ArrayList<Tile> getTiles() {
        return tiles;
    }
    
    public void setTiles(ArrayList<Tile> tiles) {
        this.tiles = tiles;
    }
    
    public void add(Tile t) {
        getTiles().add(t);
        Collections.sort(getTiles());
    }
    
    public void draw(ShapeRenderer shape) {
        // draw color profile UI tool
        shape.setColor(Color.BLACK);
        shape.rect(columnX - 1, columnY - 1, colWidth + 2, colHeight + 2);
        for (int i = 0; i < getTiles().size(); i++) {
            Tile t = getTiles().get(i);
            shape.setColor(t.getColor());
            
            // draw color column
            if (i == 0) {
                shape.rect(columnX, columnY + colHeight, colWidth, -colHeight);
            } else {
                shape.rect(columnX, columnY + t.getHeight() * colHeight, colWidth, -t.getHeight() * colHeight);
            }
            
            // draw button
            shape.rect(columnX - buttonSize - buttonPadding, columnY + t.getHeight() * colHeight, buttonSize, -buttonSize);
            //shape.setColor(Color.BLACK);
            //shape.rect(colorBoxX + colorWidth, colorBoxY + t.getHeight() * colorHeight, -buttonPadding-buttonSize-colorWidth, -1);
        }
        
        //draw currently selected color
        int size = 50;
        int offsetX = 50;
        int offsetY = colHeight - size - 30;
        shape.setColor(Color.BLACK);
        shape.rect(columnX - size - offsetX - 1, columnY + offsetY - 1, size + 2, size + 2);
        if (lastSelectedID != -1) {
            Tile t = getTileById(lastSelectedID);
            if (t != null) {
                shape.setColor(t.getColor());
            }
            shape.rect(columnX - size - offsetX, columnY + offsetY, size, size);
        }
        
    }
    
    
    public void draw(SpriteBatch batch, BitmapFont font) {
        //draw tile info
        for (int i = 0; i < getTiles().size(); i++) {
            Tile t = getTiles().get(i);
            font.draw(batch, t.toString(), columnX + colWidth + 5, columnY + t.getHeight() * colHeight);
        }
        
        //draw color sliders
        red.draw(batch, font);
        green.draw(batch, font);
        blue.draw(batch, font);
        
        //draw buttons
        add.draw(batch, font);
        remove.draw(batch, font);
    }
    
    public Tile getTileById(int ID) {
        for (int t = 0; t < getTiles().size(); t++) {
            if (getTiles().get(t).getID() == lastSelectedID) {
                return getTiles().get(t);
            }
        }
        return null;
    }
    
    public void update() {
        //check buttons
        if (add.isClicked()) {
            Tile newTile = new Tile(null, 0, Color.BLACK);
            add(newTile);
            lastSelectedID = newTile.getID();
            selectedColorID = newTile.getID();
        }
        if (remove.isClicked()) {
            Tile t = getTileById(lastSelectedID);
            if (t != null) {
                getTiles().remove(t);
                lastSelectedID = -1;
                selectedColorID = -1;
            }
        }
        
        
        //update color sliders
        if (red.update() || green.update() || blue.update()) {
            //set selected color to slider values
            Tile t = getTileById(lastSelectedID);
            if (t != null) {
                t.getColor().r = red.getValue();
                t.getColor().g = green.getValue();
                t.getColor().b = blue.getValue();
            }
        }
        
        
        boolean selectionChanged = false;
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        
        // select color to modify
        if (Gdx.input.justTouched() && selectedColorID == -1) {
            for (int i = 0; i < getTiles().size(); i++) {
                
                Tile t = getTiles().get(i);
                float colorTop = columnY + t.getHeight() * colHeight;
                
                //check if mouse in button
                if (mouseX > columnX - buttonSize - buttonPadding && mouseX < columnX - buttonPadding
                        && mouseY < colorTop && mouseY > colorTop - buttonSize) {
                    
                    selectedColorID = t.getID();
                    lastSelectedID = t.getID();
                    selectionChanged = true;
                    break;
                }
                
                //check if click on color in color column
                if (mouseX > columnX && mouseX < columnX + colWidth && mouseY < columnY + colHeight
                        && mouseY > columnY + getTiles().get(0).getHeight() * colHeight) {
                    //select first tile from top if height is not at the top of the column
                    lastSelectedID = getTiles().get(0).getID();
                    selectionChanged = true;
                    break;
                } else if (mouseX > columnX && mouseX < columnX + colWidth && mouseY < colorTop) {
                    lastSelectedID = t.getID();
                    selectionChanged = true;
                }
            }
        }
        
        if (!Gdx.input.isTouched())
            selectedColorID = -1;
        
        // move selected color
        if (selectedColorID != -1) {
            Tile t = getTileById(selectedColorID);
            if (t != null) {
                t.setHeight((float) (mouseY - columnY + buttonSize / 2) / colHeight);
                Collections.sort(getTiles());
            }
        }
        
        //set RGB sliders to selected color
        if (selectionChanged) {
            Tile t = getTileById(lastSelectedID);
            if (t != null) {
                red.setValue(t.getColor().r);
                green.setValue(t.getColor().g);
                blue.setValue(t.getColor().b);
            }
        }
    }
    
}