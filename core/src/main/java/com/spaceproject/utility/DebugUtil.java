package com.spaceproject.utility;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.spaceproject.math.MyMath;

import java.lang.reflect.Field;

public class DebugUtil {
    
    public static String getMemory() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        //long javaHeap = Gdx.app.getJavaHeap();
        //long nativeHeap = Gdx.app.getNativeHeap();
        //all 3 values seem to agree
        //memory += ", java heap: " + MyMath.formatBytes(javaHeap) + ", native heap: " + MyMath.formatBytes(nativeHeap);
        return "Mem: " + MyMath.formatBytes(used);
    }
    
    public static String objString(Object o) {
        if (o == null) {
            return "null";
        }
        
        //a shorter version to getSimpleName() and hashcode
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(o.hashCode());
    }
    
    public static void printObjectFields(Object o) {
        if (o == null) {
            System.out.println("OBJECT IS NULL");
            return;
        }
        
        System.out.println(o.getClass());
        for (Field f : o.getClass().getFields()) {
            try {
                System.out.println(String.format("\t%-14s %s", f.getName(), f.get(o)));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void printSystemProperties() {
        System.getProperties().list(System.out);
    }
    
    public static void printDisplayModes() {
        System.out.println(String.format("%s %s", Gdx.graphics.getPpiX(), Gdx.graphics.getPpiY()));
        for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes()) {
            System.out.println(String.format("%s %s %s %s", mode.width, mode.height, mode.bitsPerPixel, mode.refreshRate));
        }
        System.out.println("-------------------------\n");
    }
    
}
