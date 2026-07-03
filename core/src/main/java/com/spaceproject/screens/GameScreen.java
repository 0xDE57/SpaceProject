package com.spaceproject.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldDef;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.PhysicsComponent;
import com.spaceproject.components.PlanetComponent;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.components.ScreenTransitionComponent;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.config.SysCFG;
import com.spaceproject.config.SystemsConfig;
import com.spaceproject.config.WorldConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.generation.Galaxy;
import com.spaceproject.math.MyMath;
import com.spaceproject.noise.NoiseManager;
import com.spaceproject.systems.*;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.ECSUtil;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.ResourceDisposer;
import com.spaceproject.utility.SystemLoader;

public class GameScreen extends MyScreenAdapter {
    
    //core
    private static Engine engine;
    public static World box2dWorld;
    public static NoiseManager noiseManager;
    
    private static long gameTimeCurrent, gameTimeStart, timePaused;
    private static boolean isPaused = false;
    private int engineTicks;
    
    private static boolean inSpace;
    private static boolean isHyper;
    private static Entity currentPlanet;
    private static Entity currentStation;
    
    private static long galaxySeed;
    public static Galaxy galaxy;
    
    private static ControllerMenuStage stage;
    public static Cursor cursor;
    
    public static boolean isDebugMode = true;
    private static final StringBuilder profilerStringBuilder = new StringBuilder();
    
    public GameScreen() {
        //LOG_NONE:  mutes all logging.
        //LOG_DEBUG: logs all messages.
        //LOG_ERROR: logs only error messages.
        //LOG_INFO:  logs error and normal messages.
        Gdx.app.setLogLevel(isDebugMode ? Application.LOG_DEBUG : Application.LOG_INFO);
        
        if (isDebugMode) {
            //test
            //Physics.test();
        }
        
        initUI();
        
        initCore();
    
        initGame(true);
    }
    
    private void initUI() {
        //init scene2d/VisUI
        if (VisUI.isLoaded())
            VisUI.dispose(true);
        VisUI.load();
        VisUI.setDefaultTitleAlign(Align.center);
        BitmapFont font = FontLoader.createFont(FontLoader.fontBitstreamVM, 12);
        VisUI.getSkin().add(FontLoader.skinSmallFont, font);

        //i think this is not true... lets disable it for now then kill it later
        //TextButton.TextButtonStyle textButtonStyle = VisUI.getSkin().get(TextButton.TextButtonStyle.class);
        //textButtonStyle.focused = textButtonStyle.over; //set focused style to over for keyboard navigation because VisUI default focused style is null!

        stage = new ControllerMenuStage(new ScreenViewport());
        getInputMultiplexer().addProcessor(0, stage);
        Controllers.addListener(stage);

        //cursor
        Pixmap cursorImage = new Pixmap(Gdx.files.internal("cursor/simple-01-hit.png"));
        Pixmap scaled = new Pixmap(64, 64, cursorImage.getFormat());
        scaled.drawPixmap(cursorImage,
                0, 0, cursorImage.getWidth(), cursorImage.getHeight(),
                0, 0, scaled.getWidth(), scaled.getHeight());
        cursor = Gdx.graphics.newCursor(scaled, (int) (scaled.getWidth() * 0.5f)+1, (int) (scaled.getHeight() * 0.5f)+1);
        Gdx.graphics.setCursor(cursor);
        cursorImage.dispose();
        scaled.dispose();
    }
    
    private void initCore() {
        //ECS
        engine = new Engine();
        
        //physics
        final WorldDef worldDef = new WorldDef(new Vector2(0f, 0f), true);
        worldDef.bodyPoolMax       = 4096;
        worldDef.contactPoolMax    = 4096;
        worldDef.fixturePoolMax    = 4096;
        worldDef.initialCapacities = 4096;
        box2dWorld = new World(worldDef);
        
        //noise threadpool
        if (noiseManager == null) {
            EngineConfig engineCFG = SpaceProject.configManager.getConfig(EngineConfig.class);
            noiseManager = new NoiseManager(engineCFG.maxNoiseGenThreads);
        }
    }
    
    private void initGame(boolean space) {
        inSpace = space;
        gameTimeStart = System.currentTimeMillis();
        gameTimeCurrent = 0;
        timePaused = 0;
        isPaused = false;
        
        //init content and entities
        galaxySeed = MyMath.getNewGalaxySeed();
        galaxy = new Galaxy();
        
        //init systems
        initSpace(null);
    }
    
    //region system loading
    private void initSpace(Array<Entity> transitioningEntityCluster) {
        inSpace = true;
        
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        //load entity
        if (currentPlanet != null) {
            Vector2 position = Mappers.transform.get(currentPlanet).pos;
            Entity transitioningEntity = transitioningEntityCluster.first();
            Body body = transitioningEntity.getComponent(PhysicsComponent.class).body;
            body.setTransform(position.x, position.y, body.getAngle());
        }
        if (transitioningEntityCluster != null) {
            for (Entity entity : transitioningEntityCluster) {
                engine.addEntity(entity);
            }
            adjustPhysics(transitioningEntityCluster);
        }
        currentPlanet = null;
    }
    
    private void initWorld(Array<Entity> transitioningEntityCluster, Entity planet) {
        inSpace = false;
        currentPlanet = planet;
        
        //load/unload relevant systems
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        SystemLoader.loadSystems(this, engine, inSpace, systemsCFG);
        
        // add player
        if (transitioningEntityCluster != null) {
            WorldConfig worldCFG = SpaceProject.configManager.getConfig(WorldConfig.class);
            int mapSize = planet.getComponent(PlanetComponent.class).mapSize;
            int position = mapSize * worldCFG.tileSize / 2;//set position to middle of planet
            Entity transitioningEntity = transitioningEntityCluster.first();
            Body body = transitioningEntity.getComponent(PhysicsComponent.class).body;
            body.setTransform(position, position, body.getAngle());
            for (Entity entity : transitioningEntityCluster) {
                engine.addEntity(entity);
            }
            adjustPhysics(transitioningEntityCluster);
        }
    }
    
    public void switchScreen(Entity transEntity, Entity planet) {
        Array<Entity> transEntityCluster = ECSUtil.getAttachedEntities(engine, transEntity);
        transEntityCluster.insert(0, transEntity);
        
        //if AI, remove it. todo: should happen in screen transition system. AI shouldn't make it to here
        if (Mappers.AI.get(transEntity) != null) {
            Gdx.app.log(getClass().getSimpleName(), "REMOVING AI: " + DebugUtil.objString(transEntity));
            for (Entity e : transEntityCluster) {
                e.add(new RemoveComponent());
            }
            return;
        }
        
        //clean up resources
        ResourceDisposer.disposeAllExcept(engine.getEntities(), transEntityCluster);
        engine.removeAllEntities();//to fix family references when entities added to engine
        
        ScreenTransitionComponent screenTrans = Mappers.screenTrans.get(transEntity);
        
        //load/unload relevant systems
        if (inSpace) {
            initWorld(transEntityCluster, planet);
        } else {
            screenTrans.planet = currentPlanet;
            initSpace(transEntityCluster);
        }
        
        ScreenTransitionSystem.nextStage(screenTrans);
        
        resetCamera();
    }
    
    private void adjustPhysics(Array<Entity> entities) {
        for (Entity entity : entities) {
            PhysicsComponent physicsComponent = Mappers.physics.get(entity);
            if (physicsComponent != null) {
                Body body = physicsComponent.body;
                if (inSpace) {
                    //no friction in space
                    body.setLinearDamping(0);
                    body.setAngularDamping(0);
                } else {
                    //add friction on planet
                    body.setAngularDamping(30);
                    body.setLinearDamping(45);
                }
            }
        }
    }
    //endregion
    
    @Override
    public void render(float delta) {
        super.render(delta);
        
        // update engine
        if (!isPaused) {
            gameTimeCurrent = System.currentTimeMillis() - gameTimeStart;
        }
        engine.update(delta);
        engineTicks++;

        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();

        pollGLProfiler();
    }
    
    private void pollGLProfiler() {
        profilerStringBuilder.setLength(0);
        profilerStringBuilder.append(Gdx.graphics.getGLVersion().getDebugVersionString());
        String javaVersion = System.getProperty("java.version");
        profilerStringBuilder.append("\nJava: v").append(javaVersion);
        profilerStringBuilder.append("\n[Frame ID]:         ").append(Gdx.graphics.getFrameId());
        profilerStringBuilder.append("\n[Engine Steps]:     ").append(engineTicks);
        Box2DPhysicsSystem box2DSystem = engine.getSystem(Box2DPhysicsSystem.class);
        if (box2DSystem != null) profilerStringBuilder.append("\n").append(box2DSystem);
        LaserSystem laserSystem = engine.getSystem(LaserSystem.class);
        if (laserSystem != null) profilerStringBuilder.append("\n").append(laserSystem);
        profilerStringBuilder.append("\n[GL calls]:         ").append(glProfiler.getCalls());
        profilerStringBuilder.append("\n[Draw calls]:       ").append(glProfiler.getDrawCalls());
        profilerStringBuilder.append("\n[Shader switches]:  ").append(glProfiler.getShaderSwitches());
        profilerStringBuilder.append("\n[Texture bindings]: ").append(glProfiler.getTextureBindings());
        profilerStringBuilder.append("\n[Vertices]:         ").append(glProfiler.getVertexCount().total);
        ParticleSystem particleSystem = engine.getSystem(ParticleSystem.class);
        if (particleSystem != null) profilerStringBuilder.append("\n[Particles]:        ").append(particleSystem.getParticleCount());
        HUDSystem hudSystem = engine.getSystem(HUDSystem.class);
        if (hudSystem != null) profilerStringBuilder.append("\n").append(hudSystem);
        profilerStringBuilder.append("\n------ DISPOSED ------ ").append(ResourceDisposer.getTotalDisposeCount());
        glProfiler.reset();
    }
    
    public static CharSequence getProfilerString() {
        return profilerStringBuilder;
    }
    
    //region states
    public static long getGalaxySeed() {
        return galaxySeed;
    }
    
    public static long getPlanetSeed() {
        return Mappers.seed.get(currentPlanet).seed;
    }
    
    public static Entity getCurrentPlanet() {
        return currentPlanet;
    }
    
    public static boolean inSpace() {
        return inSpace;
    }
    
    public static boolean isHyper() { return isHyper; }
    
    public static void  setHyper(boolean active) {
        isHyper = active;
    }
    
    public static long getGameTimeCurrent() {
        return gameTimeCurrent;
    }
    
    public static void adjustGameTime(long deltaMillis) {
        gameTimeStart += deltaMillis * 1000000;
    }
    
    public static ControllerMenuStage getStage() {
        return stage;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static Engine getEngine() {
        return engine;
    }
    //endregion
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        
        stage.getViewport().update(width, height, true);
        
        //notify systems of resize
        for (EntitySystem system : engine.getSystems()) {
            if (system instanceof IScreenResizeListener) {
                ((IScreenResizeListener) system).resize(width, height);
            }
        }
    }
    
    //region pause/resume
    @Override
    public void pause() {
        setSystemProcessing(false);
    }
    
    @Override
    public void resume() {
        /*
        HUDSystem hud = engine.getSystem(HUDSystem.class);
        if (hud != null) {
            if (hud.getGameMenu().isVisible()) {
                return;
            }
        }*/
        setSystemProcessing(true);
    }
    
    private void setSystemProcessing(boolean process) {
        if (isPaused != process) {
            return;
        }

        isPaused = !process;
        Gdx.app.log(getClass().getSimpleName(), "paused [" + isPaused + "]");
        
        //adjust time
        if (isPaused) {
            timePaused = System.currentTimeMillis();
        } else {
            long delta = System.currentTimeMillis() - timePaused;
            gameTimeStart += delta;
            timePaused = 0;
        }

        //enable/disable systems
        SystemsConfig systemsCFG = SpaceProject.configManager.getConfig(SystemsConfig.class);
        for (EntitySystem system : engine.getSystems()) {
            SysCFG sysCFG = systemsCFG.getConfig(system);
            if (sysCFG.isHaltOnGamePause()) {
                system.setProcessing(!isPaused);
                Gdx.app.log(getClass().getSimpleName(), "processing " + (isPaused ? "disabled" : "enabled") + " for " + system.getClass().getSimpleName());
            }
            //todo: change to interface? IPauseListener { pause(), resume() }
            if (system instanceof SoundSystem) {
                if (isPaused) {
                    ((SoundSystem) system).pauseAll();
                } else {
                    ((SoundSystem) system).resumeAll();
                }
            }
        }
    }
    //endregion
    
    @Override
    public void hide() {
        dispose();
    }
    
    @Override
    public void dispose() {
        Gdx.app.log(getClass().getSimpleName(), "Disposing...");
        
        // clean up after self
        SystemLoader.unloadAll(engine);
        
        ResourceDisposer.disposeAll(engine.getEntities());
        
        engine.removeAllEntities();
        engine = null;
        
        box2dWorld.dispose();
        
        //getInputMultiplexer().clear();
        galaxy = null;
        noiseManager.dispose();
        noiseManager = null;

        Controllers.removeListener(stage);
        stage.dispose();
        cursor.dispose();
    }
    
}
