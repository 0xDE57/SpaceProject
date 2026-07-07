package com.spaceproject.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;

public class MultiGraph {
    private final List<GraphSeries> seriesList = new ArrayList<>();
    private final int width;
    private final int height;
    
    public MultiGraph(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    /**
     * Adds a new data line to the graph.
     *
     * @param bufferSize How many points to track.
     * @param color      The color of the line.
     * @return The created series so you can call .update() on it.
     */
    public GraphSeries addSeries(int bufferSize, Color color) {
        GraphSeries series = new GraphSeries(bufferSize, color);
        seriesList.add(series);
        return series;
    }
    
    public void draw(ShapeRenderer shape, float x, float y) {
        shape.setColor(new Color(0.1f, 0.1f, 0.1f, 1f));
        shape.rect(x, y, width, height);
        
        for (GraphSeries series : seriesList) {
            shape.setColor(series.getColor());
            
            float prevX = -1;
            float prevY = -1;
            int size = series.getBufferSize();
            float scale = series.getMaxScale();
            
            for (int i = 0; i < size; i++) {
                float val = series.getValueAt(i);
                
                float posX = x + ((float) i / size) * width;
                float posY = y + (Math.max(0, val) / scale) * height;
                
                if (prevX != -1) {
                    shape.line(prevX, prevY, posX, posY);
                }
                
                prevX = posX;
                prevY = posY;
            }
        }
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getWidth() {
        return width;
    }
    
}
