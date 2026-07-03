package com.spaceproject.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spaceproject.SpaceProject;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication(args);
    }

    private static Lwjgl3Application createApplication(String[] args) {
        return new Lwjgl3Application(new SpaceProject(args), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 8);
        config.setWindowedMode(1280, 800);
        config.useVsync(true);
        config.setForegroundFPS(0);//disable limit for when vsync off
        config.disableAudio(true); //disable libGDX audio in favor of TuningFork

        return config;
    }
}
