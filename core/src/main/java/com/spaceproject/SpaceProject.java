package com.spaceproject;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.spaceproject.config.ConfigManager;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.screens.TitleScreen;

public class SpaceProject extends Game {
    
    public static final String TITLE = "a space project";
    public static final String VERSION = "pre-alpha";//<α
    
    public static ConfigManager configManager;

    boolean skipTitle = false;

    public SpaceProject(String[] arg) {
        if (arg.length > 0 && arg[0].equalsIgnoreCase("skipTitle")) {
            skipTitle = true;
        }
    }

    @Override
    public void create() {
        MyScreenAdapter.game = this;
    
        Gdx.graphics.setTitle(TITLE + "  (" + VERSION + ")");
        
        loadConfigs();

        if (skipTitle) {
            setScreen(new GameScreen());
        } else {
            setScreen(new TitleScreen(this));
        }
    }
    
    private static void loadConfigs() {
        configManager = new ConfigManager();
        configManager.init();
    }
    
}
