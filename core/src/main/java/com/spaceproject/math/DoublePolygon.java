/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.spaceproject.math;

import com.badlogic.gdx.math.*;

/** Encapsulates a 2D polygon defined by it's vertices relative to an origin point (default of 0, 0). */
public class DoublePolygon implements DoubleShape2D {
    private double[] localVertices;
    private double[] worldVertices;
    private double x, y;
    private double originX, originY;
    private double rotation;
    private double scaleX = 1, scaleY = 1;
    private boolean dirty = true;
    private Rectangle bounds;

    /** Constructs a new polygon with no vertices. */
    public DoublePolygon () {
        this.localVertices = new double[0];
    }

    /** Constructs a new polygon from a double array of parts of vertex points.
     *
     * @param vertices an array where every even element represents the horizontal part of a point, and the following element
     *           representing the vertical part
     *
     * @throws IllegalArgumentException if less than 6 elements, representing 3 points, are provided */
    public DoublePolygon (double[] vertices) {
        if (vertices.length < 6) throw new IllegalArgumentException("polygons must contain at least 3 points.");
        this.localVertices = vertices;
    }

    /** Returns the polygon's local vertices without scaling or rotation and without being offset by the polygon position. */
    public double[] getVertices () {
        return localVertices;
    }

    /** Calculates and returns the vertices of the polygon after scaling, rotation, and positional translations have been applied,
     * as they are position within the world.
     *
     * @return vertices scaled, rotated, and offset by the polygon position. */
    public double[] getTransformedVertices () {
        if (!dirty) return worldVertices;
        dirty = false;

        final double[] localVertices = this.localVertices;
        if (worldVertices == null || worldVertices.length != localVertices.length) worldVertices = new double[localVertices.length];

        final double[] worldVertices = this.worldVertices;
        final double positionX = x;
        final double positionY = y;
        final double originX = this.originX;
        final double originY = this.originY;
        final double scaleX = this.scaleX;
        final double scaleY = this.scaleY;
        final boolean scale = scaleX != 1 || scaleY != 1;
        final double rotation = this.rotation;
        final double cos = MathUtils.cosDeg((float) rotation);
        final double sin = MathUtils.sinDeg((float) rotation);

        for (int i = 0, n = localVertices.length; i < n; i += 2) {
            double x = localVertices[i] - originX;
            double y = localVertices[i + 1] - originY;

            // scale if needed
            if (scale) {
                x *= scaleX;
                y *= scaleY;
            }

            // rotate if needed
            if (rotation != 0) {
                double oldX = x;
                x = cos * x - sin * y;
                y = sin * oldX + cos * y;
            }

            worldVertices[i] = positionX + x + originX;
            worldVertices[i + 1] = positionY + y + originY;
        }
        return worldVertices;
    }

    /** Sets the origin point to which all of the polygon's local vertices are relative to. */
    public void setOrigin (double originX, double originY) {
        this.originX = originX;
        this.originY = originY;
        dirty = true;
    }

    /** Sets the polygon's position within the world. */
    public void setPosition (double x, double y) {
        this.x = x;
        this.y = y;
        dirty = true;
    }

    /** Sets the polygon's local vertices relative to the origin point, without any scaling, rotating or translations being
     * applied.
     *
     * @param vertices double array where every even element represents the x-coordinate of a vertex, and the proceeding element
     *           representing the y-coordinate.
     * @throws IllegalArgumentException if less than 6 elements, representing 3 points, are provided */
    public void setVertices (double[] vertices) {
        if (vertices.length < 6) throw new IllegalArgumentException("polygons must contain at least 3 points.");
        localVertices = vertices;
        dirty = true;
    }

    /** Set vertex position
     * @param vertexNum min=0, max=vertices.length/2-1
     * @throws IllegalArgumentException if vertex doesnt exist */
    public void setVertex (int vertexNum, double x, double y) {
        if (vertexNum < 0 || vertexNum > localVertices.length / 2 - 1)
            throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist");
        localVertices[2 * vertexNum] = x;
        localVertices[2 * vertexNum + 1] = y;
        dirty = true;
    }

    /** Translates the polygon's position by the specified horizontal and vertical amounts. */
    public void translate (double x, double y) {
        this.x += x;
        this.y += y;
        dirty = true;
    }

    /** Sets the polygon to be rotated by the supplied degrees. */
    public void setRotation (double degrees) {
        this.rotation = degrees;
        dirty = true;
    }

    /** Applies additional rotation to the polygon by the supplied degrees. */
    public void rotate (double degrees) {
        rotation += degrees;
        dirty = true;
    }

    /** Sets the amount of scaling to be applied to the polygon. */
    public void setScale (double scaleX, double scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        dirty = true;
    }

    /** Applies additional scaling to the polygon by the supplied amount. */
    public void scale (double amount) {
        this.scaleX += amount;
        this.scaleY += amount;
        dirty = true;
    }

    /** Sets the polygon's world vertices to be recalculated when calling {@link #getTransformedVertices()
     * getTransformedVertices}. */
    public void dirty () {
        dirty = true;
    }

    /** Returns the area contained within the polygon. */
    public double area () {
        double[] vertices = getTransformedVertices();
        return PolygonUtil.doublePolygonArea(vertices, 0, vertices.length);
    }

    public int getVertexCount () {
        return this.localVertices.length / 2;
    }

    /** @return Position(transformed) of vertex */
    public Vector2 getVertex (int vertexNum, Vector2 pos) {
        if (vertexNum < 0 || vertexNum > getVertexCount())
            throw new IllegalArgumentException("the vertex " + vertexNum + " doesn't exist");
        double[] vertices = this.getTransformedVertices();
        return pos.set((float) vertices[2 * vertexNum], (float) vertices[2 * vertexNum + 1]);
    }

    public Vector2 getCentroid (Vector2 centroid) {
        double[] vertices = getTransformedVertices();
        return PolygonUtil.doublePolygonCentroid(vertices, 0, vertices.length, centroid);
    }

    /** Returns an axis-aligned bounding box of this polygon.
     *
     * Note the returned Rectangle is cached in this polygon, and will be reused if this Polygon is changed.
     *
     * @return this polygon's bounding box {@link Rectangle} */
    public Rectangle getBoundingRectangle () {
        double[] vertices = getTransformedVertices();

        double minX = vertices[0];
        double minY = vertices[1];
        double maxX = vertices[0];
        double maxY = vertices[1];

        final int numFloats = vertices.length;
        for (int i = 2; i < numFloats; i += 2) {
            minX = minX > vertices[i] ? vertices[i] : minX;
            minY = minY > vertices[i + 1] ? vertices[i + 1] : minY;
            maxX = maxX < vertices[i] ? vertices[i] : maxX;
            maxY = maxY < vertices[i + 1] ? vertices[i + 1] : maxY;
        }

        if (bounds == null) bounds = new Rectangle();
        bounds.x = (float) minX;
        bounds.y = (float) minY;
        bounds.width = (float) (maxX - minX);
        bounds.height = (float) (maxY - minY);

        return bounds;
    }

    /** Returns whether an x, y pair is contained within the polygon. */
    @Override
    public boolean contains (double x, double y) {
        final double[] vertices = getTransformedVertices();
        final int numFloats = vertices.length;
        int intersects = 0;

        for (int i = 0; i < numFloats; i += 2) {
            double x1 = vertices[i];
            double y1 = vertices[i + 1];
            double x2 = vertices[(i + 2) % numFloats];
            double y2 = vertices[(i + 3) % numFloats];
            if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) && x < ((x2 - x1) / (y2 - y1) * (y - y1) + x1)) intersects++;
        }
        return (intersects & 1) == 1;
    }

    @Override
    public boolean contains (Vector2 point) {
        return contains(point.x, point.y);
    }

    /** Returns the x-coordinate of the polygon's position within the world. */
    public double getX () {
        return x;
    }

    /** Returns the y-coordinate of the polygon's position within the world. */
    public double getY () {
        return y;
    }

    /** Returns the x-coordinate of the polygon's origin point. */
    public double getOriginX () {
        return originX;
    }

    /** Returns the y-coordinate of the polygon's origin point. */
    public double getOriginY () {
        return originY;
    }

    /** Returns the total rotation applied to the polygon. */
    public double getRotation () {
        return rotation;
    }

    /** Returns the total horizontal scaling applied to the polygon. */
    public double getScaleX () {
        return scaleX;
    }

    /** Returns the total vertical scaling applied to the polygon. */
    public double getScaleY () {
        return scaleY;
    }
}
