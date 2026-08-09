package com.spaceproject.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.spaceproject.SpaceProject;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication(args);
    }

    private static Lwjgl3Application createApplication(String[] args) {
        if (Platform.get() == Platform.LINUX) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11);
        }
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
