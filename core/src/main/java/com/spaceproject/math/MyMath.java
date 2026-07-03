package com.spaceproject.math;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.spaceproject.screens.GameScreen;

import java.util.HashMap;
import java.util.Random;

public abstract class MyMath {
    
    //region seed
    public static long getSeed(float x, float y) {
        return getSeed((int) x, (int) y);
    }
    
    public static long getSeed(int x, int y) {
        //long is 64 bits. store x in upper bits, y in lower bits
        return ((long) x << 32) + y + GameScreen.getGalaxySeed();
    }
    
    public static int test(int x, int y) {
        int largePrime = 198491317;
        return x + (largePrime * y);
    }
    
    public static long getNewGalaxySeed() {
        long newSeed = new Random().nextLong();
        if (GameScreen.isDebugMode) {
            newSeed = 1; //test seed. todo: select seed on game start
        }
        Gdx.app.log("MyMath", "galaxy seed: " + newSeed);
        return newSeed;
    }
    //endregion
    
    //region vectors and angles
    private static final Vector2 TEMP_VEC = new Vector2();
    public static Vector2 vector(float direction, float magnitude) {
        float dx = MathUtils.cos(direction) * magnitude;
        float dy = MathUtils.sin(direction) * magnitude;
        return TEMP_VEC.set(dx, dy);
    }
    
    public static Vector2 logVec(Vector2 vec, float scale) {
        float length = (float) Math.log(vec.len()) * scale;
        return vector(vec.angleRad(), length);
    }
    
    public static float distance(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x2 - x1, y2 - y1);
    }
   
    public static float angleTo(Vector2 a, Vector2 b) {
        return angleTo(a.x, a.y, b.x, b.y);
    }
    
    public static float angleTo(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }
    
    public static float getAngularImpulse(Body body, float targetAngle, float delta) {
        //https://www.iforce2d.net/b2dtut/rotate-to-angle
        float nextAngle = body.getAngle() + body.getAngularVelocity() * delta;
        float totalRotation = targetAngle - nextAngle;
        while (totalRotation < -180 * MathUtils.degRad) totalRotation += 360 * MathUtils.degRad;
        while (totalRotation >  180 * MathUtils.degRad) totalRotation -= 360 * MathUtils.degRad;
        float desiredAngularVelocity = totalRotation * 60;
        float change = 50 * MathUtils.degRad; //max degrees of rotation per time step
        desiredAngularVelocity = Math.min(change, Math.max(-change, desiredAngularVelocity));
        float impulse = body.getInertia() * desiredAngularVelocity;
        return impulse;
    }
    //endregion
    
    /**
     * Returns the percentage of the range max-min that corresponds to value.
     *
     * @param min
     * @param max
     * @param value
     * @return interpolant value within the range [min, max].
     */
    public static float inverseLerp(float min, float max, float value) {
        return (value - min) / (max - min);
    }
    
    //region formatting
    /** Convert bytes to a human readable format.
     * Eg: 26673720 -> 25.44MB
     * Credit: icza, stackoverflow.com/questions/3758606/#24805871
     *
     * @param bytes
     * @return bytes with SI postfix
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.2f%sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
    
    /** Convert milliseconds to hours, minutes, seconds
     * https://stackoverflow.com/a/21701635
     */
    static StringBuilder builder = new StringBuilder();
    public static String formatDuration(final long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        builder.setLength(0);//clear
        builder.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : hours).append(":");
        builder.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : minutes).append(":");
        builder.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : seconds);
        return builder.toString();
    }

    public static String formatPos(float x, float y, int decimal) {
        return round(x, decimal) + ", " + round(y, decimal);
    }
    
    public static String formatVector2(Vector2 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal);
    }

    public static String formatVector2Full(Vector2 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal) + " (" + round(vec.len(), decimal) + ") -> [" + round(vec.angleRad(), decimal + 1) + " == " + round(vec.angleDeg(), decimal + 1);
    }

    public static String formatVector3(Vector3 vec, int decimal) {
        return round(vec.x, decimal) + ", " + round(vec.y, decimal) + ", " + round(vec.z, decimal);
    }
    
    public static String formatVector3as2(Vector3 vec, int decimal) {
        //ignore Z component in this case
        return round(vec.x, decimal) + ", " + round(vec.y, decimal);
    }

    public static String formatFloat(float f) {
        int bits = Float.floatToIntBits(f);
        builder.setLength(0);
        builder.append("float: ").append(f).append( "f | 0x")
                .append(String.format("%8s", Integer.toHexString(bits)).replace(" ", "0")).append(" | ")
                .append(String.format("%32s", Integer.toBinaryString(bits)).replace(" ", "0")).append("b");
        //float: 2.0f | 0x40000000 | 01000000000000000000000000000000b
        // 40 00 00 00
        // 01000000 00000000 00000000 00000000
        return builder.toString();
    }

    public static String formatDouble(double d) {
        long bits = Double.doubleToLongBits(d);
        builder.setLength(0);
        builder.append("double: ").append(d).append( " | 0x")
                .append(String.format("%16s", Long.toHexString(bits)).replace(" ", "0")).append(" | ")
                .append(String.format("%64s", Long.toBinaryString(bits)).replace(" ", "0")).append("b");
        //todo: add spacing
        // double: 2.0003161430358887 | 0x400000a5c0000000 | 0100000000000000000000001010010111000000000000000000000000000000b
        // 40 00 00 a5 c0 00 00 00
        // 01000000 00000000 00000000 10100101 11000000 00000000 00000000 00000000
        return builder.toString();
    }

    //quadratic float
    public static void quadraticFloat(float a, float b, float c, Vector2 out) {
        float d = b * b - 4.0f * a * c;
        float sd = (float) Math.sqrt(d);
        float r1 = (-b + sd) / (2.0f * a);
        float r2 = (-b - sd) / (2.0f * a);
        out.set(r1, r2);
    }
    //quadratic double
    public static void quadraticDouble(double a, double b, double c, Vector2 out) {
        double d = b * b - 4.0 * a * c;
        double sd = Math.sqrt(d);
        double r1 = (-b + sd) / (2.0 * a);
        double r2 = (-b - sd) / (2.0 * a);
        out.set((float) r1, (float) r2);
        //todo: determine bits lost, store difference in double for compare?
        //formatDouble(r1);
        //formatFloat(out.x);
        //formatDouble(r2);
        //formatFloat(out.y);
    }

    /**
     * Round value with specified precision.
     *
     * @param value
     * @param precision number of decimal points
     * @return rounded value
     */
    public static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
    //endregion

    static public boolean isEqualDouble(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    static HashMap<Integer, Long> memo = new HashMap<Integer, Long>() {{
        put(0, 0L);
        put(1, 1L);
    }};

    public static long fibonacci(int n) {
        if (!memo.containsKey(n)) {
            memo.put(n, fibonacci(n - 1) + fibonacci(n - 2));
        }
        return memo.get(n);
    }

    /** Taxicab or Manhattan distance is the distance between two points is defined as
     * the sum of the absolute differences of their respective Cartesian coordinates.
     * rectilinear distance
     */
    public static float manhattanDistance(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /** Chebyshev "chessboard" distance.
     * The distance between two points is the greatest of their differences along any coordinate dimension.
     */
    public static float chessDistance(float x1, float y1, float x2, float y2) {
        float dX = Math.abs(x1 - x2);
        float dY = Math.abs(y1 - y2);
        return Math.max(dX, dY);
    }

    /** Modification inspired by above for min distance.
     */
    public static float antiChessDistance(float x1, float y1, float x2, float y2) {
        float dX = Math.abs(x1 - x2);
        float dY = Math.abs(y1 - y2);
        return Math.min(dX, dY);
    }

}
