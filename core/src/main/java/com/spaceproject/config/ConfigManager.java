package com.spaceproject.config;

import java.util.ArrayList;

public class ConfigManager {
    
    private boolean debugDevForceLoadDefault = true;
    private ArrayList<Config> configs;
    
    
    public ConfigManager() {
        configs = new ArrayList<>();
        configs.add(new EngineConfig());
        configs.add(new SystemsConfig());
        configs.add(new EntityConfig());
        configs.add(new CelestialConfig());
        configs.add(new WorldConfig());
        configs.add(new UIConfig());
        configs.add(new KeyConfig());
        configs.add(new MiniMapConfig());
        configs.add(new DebugConfig());
    }
    
    
    public void init() {
        if (debugDevForceLoadDefault) {
            loadDefaultAll();
            return;
        }
        
        for (int i = 0; i < configs.size(); i++) {
            //todo: test load bad config, broken names, bad structure, bad int val in bool, missing fields, extra fields
            Config loaded = configs.get(i).loadFromJson();
            if (loaded == null) {
                //save default if file not exit
                configs.get(i).loadDefault();
                configs.get(i).saveToJson();
            } else {
                configs.set(i,loaded);
            }
        }
    }
    
    
    public void loadDefaultAll() {
        for (Config cfg : configs) {
            cfg.loadDefault();
        }
    }
    
    
    public void saveAll() {
        for (Config cgf : configs) {
            cgf.saveToJson();
        }
    }
    
    
    public <T extends Config> T getConfig(Class<T> type) {
        for (Config c : configs) {
            if (type.isInstance(c)) {
                return (T) c;
            }
        }
        
        return null;
    }
    
    
    public ArrayList<Config> getConfigs() {
        return configs;
    }
    
}
