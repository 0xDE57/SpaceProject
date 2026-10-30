package com.spaceproject.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

/**
 * Helper to draw a laser "beam": a thick line that is even from point A to point B
 * but fades across its width (perpendicular to the line).
 *
 * This works around the fact that
 * {@link ShapeRenderer#rectLine(float, float, float, float, float, Color, Color)}
 * interpolates the color along the line (A to B) instead of across the width.
 *
 * Implementation: the outer edge points of each side of the beam are computed up front
 * in world space, and each side is submitted as two triangles with per-vertex colors
 * (via {@link ShapeRenderer#triangle(float, float, float, float, float, float, Color, Color, Color)}).
 * No rotated-rect origin/rotation bookkeeping is involved, so the beam's centerline
 * lies exactly on the line between the two endpoints at any angle.
 *
 * Usage:
 *   sr.begin(ShapeRenderer.ShapeType.Filled);
 *   BeamShapeRender.rectLineBeam(sr, x1, y1, x2, y2, thickness, centerColor, edgeColor);
 *   sr.end();
 *
 * Brightest in the center, fading outwards:
 *   BeamShapeRender.rectLineBeam(sr, p1, p2, 0.3f, laserColor, Color.CLEAR);
 */
public final class BeamShapeRender {

    private BeamShapeRender() {
    }

    /**
     * Draws a beam between two points: even along its length, fading across its width.
     * {@code centerColor} is applied on the centerline (the line between the two points)
     * and {@code edgeColor} on both outer edges; the GPU interpolates linearly in between.
     *
     * @param thickness total beam width, perpendicular to the line (a value &lt;= 0 draws nothing)
     */
    public static void rectLineBeam(ShapeRenderer sr, float x1, float y1, float x2, float y2,
                                    float thickness, Color centerColor, Color edgeColor) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);

        // degenerate case: a zero-length beam has no direction, so no width to fade across.
        // draw a small square in the center color (invisible if that color is clear).
        if (length <= 1e-8f) {
            float half = thickness * 0.5f;
            sr.rect(x1 - half, y1 - half, thickness, thickness,
                centerColor, centerColor, centerColor, centerColor);
            return;
        }

        // unit perpendicular (one of the two sides of the line)
        float inv = 1f / length;
        float px = dy * inv;
        float py = -dx * inv;

        float hw = thickness * 0.5f;

        // outer edge points, computed up front in world space (one per side of the centerline)
        float ox1p = x1 + px * hw, oy1p = y1 + py * hw;
        float ox2p = x2 + px * hw, oy2p = y2 + py * hw;
        float ox1n = x1 - px * hw, oy1n = y1 - py * hw;
        float ox2n = x2 - px * hw, oy2n = y2 - py * hw;

        // + side: quad (x1,y1) (x2,y2) (ox2p,oy2p) (ox1p,oy1p) as two triangles.
        // centerline vertices get centerColor, outer edge vertices get edgeColor.
        // the color only varies with the perpendicular offset, so the interpolated
        // gradient is linear across the width regardless of the triangle split.
        sr.triangle(x1, y1, x2, y2, ox2p, oy2p, centerColor, centerColor, edgeColor);
        sr.triangle(x1, y1, ox2p, oy2p, ox1p, oy1p, centerColor, edgeColor, edgeColor);

        // - side (mirror)
        sr.triangle(x1, y1, x2, y2, ox2n, oy2n, centerColor, centerColor, edgeColor);
        sr.triangle(x1, y1, ox2n, oy2n, ox1n, oy1n, centerColor, edgeColor, edgeColor);
    }

    /** Convenience overload using Vector2. */
    public static void rectLineBeam(ShapeRenderer sr, Vector2 a, Vector2 b, float thickness, Color centerColor, Color edgeColor) {
        rectLineBeam(sr, a.x, a.y, b.x, b.y, thickness, centerColor, edgeColor);
    }
}
