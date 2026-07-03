package com.spaceproject.utility;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.screens.GameScreen;

import java.lang.reflect.InvocationTargetException;


public abstract class SystemLoader {
    
    private static String logSource = "SystemLoader";
    
    public static void loadSystems(GameScreen game, Engine engine, boolean inSpace, SystemsConfig cfg) {
        Gdx.app.log(logSource, inSpace ? "==========SPACE==========" : "==========WORLD==========");
        
        long time = System.currentTimeMillis();
        
        for (SysCFG sysCFG : cfg.getSystems()) {
            loadUnloadSystems(game, engine, inSpace, sysCFG);
        }
        
        long now = System.currentTimeMillis();
        Gdx.app.log(logSource,  "Systems: [" + engine.getSystems().size() + "] load time: " + (now - time) + " ms");
    }
    
    @SuppressWarnings("unchecked")
    private static void loadUnloadSystems(GameScreen game, Engine engine, boolean inSpace, SysCFG sysCFG) {
        boolean shouldBeLoaded = (inSpace && sysCFG.isLoadInSpace()) || (!inSpace && sysCFG.isLoadInWorld());
        try {
            Class<? extends EntitySystem> systemClass = (Class<? extends EntitySystem>) Class.forName(sysCFG.getClassName());
            EntitySystem systemInEngine = engine.getSystem(systemClass);
            
            boolean isLoaded = systemInEngine != null;
            if (shouldBeLoaded) {
                if (!isLoaded) {
                    load(game, engine, sysCFG.getPriority(), systemClass);
                }
            } else {
                if (isLoaded) {
                    unload(engine, systemInEngine);
                }
            }
        } catch (Exception e) {
            Gdx.app.error(logSource, "Could not load system " + sysCFG.getClassName(), e);
        }
    }
    
    private static void load(GameScreen game, Engine engine, int priority, Class<? extends EntitySystem> systemClass)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        
        EntitySystem systemToLoad = systemClass.getDeclaredConstructor().newInstance();
        systemToLoad.priority = priority;
        
        //auto-hookup interfaces
        if (systemToLoad instanceof IRequireGameContext) {
            ((IRequireGameContext) systemToLoad).initContext(game);
        }
        
        //NOTE: don't hook up entity listener here, this overrides the family in case of SortedIteratingSystem when addedToEngine()
        //if (systemToLoad instanceof EntityListener) { engine.addEntityListener((EntityListener) systemToLoad); }
        
        engine.addSystem(systemToLoad);
        Gdx.app.log(logSource, "Loaded: " + String.format("%-4d ", systemToLoad.priority) + systemToLoad.getClass().getName());
    }
    
    private static void unload(Engine engine, EntitySystem systemInEngine) {
        // auto unhook listeners and cleanup. we could require that systems:
        //  - remove themselves as listeners on removedFromEngine()
        //  - dispose themselves on removedFromEngine()
        // as a failsafe we clean up here. also note other listeners:
        //  - PhysicsContactListener box2d physics system
        
        if (systemInEngine instanceof EntityListener) {
            //listener must be removed, otherwise a reference is kept in engine (i think?)
            //when system is re-added / re-removed down the line, the families/listeners are broken
            engine.removeEntityListener((EntityListener) systemInEngine);
        }
        if (systemInEngine instanceof ControllerListener) {
            Controllers.removeListener((ControllerListener) systemInEngine);
        }
        if (systemInEngine instanceof InputProcessor) {
            GameScreen.getInputMultiplexer().removeProcessor((InputProcessor) systemInEngine);
        }

        engine.removeSystem(systemInEngine);

        //dispose AFTER remove in case a system has to access some data when removedFromEngine()
        if (systemInEngine instanceof Disposable) {
            ((Disposable)systemInEngine).dispose();
        }
        
        Gdx.app.log(logSource, "Unload: " + String.format("%-4d ", systemInEngine.priority) + systemInEngine.getClass().getName());
    }
    
    public static void unloadAll(Engine engine) {
        EntitySystem[] systems = engine.getSystems().toArray(EntitySystem.class);
        for (int i = 0; i < systems.length; i++) {
            unload(engine, systems[i]);
        }
    }
    
    private void loadMods(){
        //todo: allow custom loading from file, eg: http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
        //load from directory
        //eg: users can simply drop mod in location
        //  assets/mods/
        //      mymodA: a mod pack. basically same structure as assets folder with user replaceable values
        //          config: custom config settings, including SystemsConfig which will define priorities and
        //          systems: replace and add systems logic
        //          sound: replace sounds packs
        
        //Mod manager UI
        //a file browser UI to select mod, mod will be copied to mods folder
        //mod browser will list mods
        //users will be able to select mods they wish to activate
        //mod application will be granular
        //eg: a user may only want the shader from one pack, or the systems from another
        //
        
        //todo: consider security ramifications of loading arbitrary code. users need to be cautions of plugins found online
        //eg: malicious plugin, starts uploading all your files to a remote server
        //https://blog.jayway.com/2014/06/13/sandboxing-plugins-in-java/
        //https://docstore.mik.ua/orelly/java-ent/security/ch06_03.htm
    }

}
