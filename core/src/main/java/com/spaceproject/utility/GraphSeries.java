package com.spaceproject.utility;

import com.badlogic.gdx.graphics.Color;

public class GraphSeries {
    private final float[] buffer;
    private int head = 0;
    private final int bufferSize;

    private float maxScale = 60f;
    private final float minScale = 1f;
    private final Color color;

    public GraphSeries(int bufferSize, Color color) {
        this.bufferSize = bufferSize;
        this.buffer = new float[bufferSize];
        this.color = color;
    }

    public void update(float value) {
        buffer[head] = value;
        head = (head + 1) % bufferSize;

        // Autoscaling logic
        float currentBufferMax = 0;
        for (float val : buffer) {
            if (val > currentBufferMax) currentBufferMax = val;
        }

        if (currentBufferMax > maxScale) {
            maxScale *= 2;
        } else if (currentBufferMax < maxScale / 2 && maxScale > minScale) {
            maxScale /= 2;
        }
    }

    public float getValueAt(int chronologicalIndex) {
        return buffer[(head + chronologicalIndex) % bufferSize];
    }

    public int getBufferSize() { return bufferSize; }
    public float getMaxScale() { return maxScale; }
    public Color getColor() { return color; }
}
