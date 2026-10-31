package com.spaceproject.utility;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

/**
 * Helper to draw a laser "beam": a thick line that fades across its width
 * (perpendicular to the line) and from start to end (along the line), like
 * {@link ShapeRenderer#rectLine(float, float, float, float, float, Color, Color)}
 * but with the width fade added on top.
 *
 * Implementation: the outer edge points of each side of the beam are computed up front
 * in world space, and each side is submitted as two triangles with per-vertex colors
 * (via {@link ShapeRenderer#triangle(float, float, float, float, float, float, Color, Color, Color)}).
 * No rotated-rect origin/rotation bookkeeping is involved, so the beam's centerline
 * lies exactly on the line between the two endpoints at any angle.
 *
 * Color model: each beam corner is the 50/50 blend of its length-axis color
 * (start/end) and its width-axis color (inner/outer):
 *
 *   corner (start, inner) = avg(startColor, innerColor)
 *   corner (end,   inner) = avg(endColor,   innerColor)
 *   corner (start, outer) = avg(startColor, outerColor)
 *   corner (end,   outer) = avg(endColor,   outerColor)
 *
 * and the GPU interpolates those corner colors across the beam, so the beam fades
 * from start to end along its length and from inner to outer across its width at the same time.
 *
 * Usage:
 *   sr.begin(ShapeRenderer.ShapeType.Filled);
 *   BeamShapeRender.rectLineBeam(sr, x1, y1, x2, y2, thickness, startColor, endColor, innerColor, outerColor);
 *   sr.end();
 *
 * Bright core at the emitter, fading toward the impact point and outwards to transparent:
 *   BeamShapeRender.rectLineBeam(sr, p1, p2, 0.3f, laserColor, fadedColor, laserColor, Color.CLEAR);
 */
public final class BeamShapeRender {

    private BeamShapeRender() {
    }

    // scratch colors for the per-corner blends (rendering is single-threaded)
    private static final Color cornerStartInner = new Color();
    private static final Color cornerEndInner = new Color();
    private static final Color cornerStartOuter = new Color();
    private static final Color cornerEndOuter = new Color();

    /**
     * Draws a beam between two points: fading from {@code startColor} to {@code endColor}
     * along its length and from {@code innerColor} (centerline, the line between the two
     * points) to {@code outerColor} (both outer edges) across its width.
     *
     * @param thickness total beam width, perpendicular to the line (a value &lt;= 0 draws nothing)
     */
    public static void rectLineBeam(ShapeRenderer sr, float x1, float y1, float x2, float y2,
                                    float thickness, Color startColor, Color endColor, Color innerColor, Color outerColor) {
        // each corner = blend of its length-axis and width-axis color (see class docs)
        cornerStartInner.set(startColor).lerp(innerColor, 0.5f);
        cornerEndInner.set(endColor).lerp(innerColor, 0.5f);
        cornerStartOuter.set(startColor).lerp(outerColor, 0.5f);
        cornerEndOuter.set(endColor).lerp(outerColor, 0.5f);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float)Math.sqrt(dx * dx + dy * dy);

        // degenerate case: a zero-length beam has no direction, so no width to fade across.
        // draw a small square in the start core color (invisible if that color is clear).
        if (length <= 1e-8f) {
            float half = thickness * 0.5f;
            sr.rect(x1 - half, y1 - half, thickness, thickness,
                cornerStartInner, cornerStartInner, cornerStartInner, cornerStartInner);
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
        // (x1,y1) and (x2,y2) sit on the centerline, so they take the inner corner colors.
        sr.triangle(x1, y1, x2, y2, ox2p, oy2p, cornerStartInner, cornerEndInner, cornerEndOuter);
        sr.triangle(x1, y1, ox2p, oy2p, ox1p, oy1p, cornerStartInner, cornerEndOuter, cornerStartOuter);

        // - side (mirror), same corner colors
        sr.triangle(x1, y1, x2, y2, ox2n, oy2n, cornerStartInner, cornerEndInner, cornerEndOuter);
        sr.triangle(x1, y1, ox2n, oy2n, ox1n, oy1n, cornerStartInner, cornerEndOuter, cornerStartOuter);
    }

    /** Convenience overload using Vector2. */
    public static void rectLineBeam(ShapeRenderer sr, Vector2 a, Vector2 b, float thickness, Color startColor, Color endColor, Color innerColor, Color outerColor) {
        rectLineBeam(sr, a.x, a.y, b.x, b.y, thickness, startColor, endColor, innerColor, outerColor);
    }
}
