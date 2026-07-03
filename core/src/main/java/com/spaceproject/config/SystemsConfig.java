package com.spaceproject.config;


import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.spaceproject.systems.*;

import java.util.ArrayList;

public class SystemsConfig extends Config {
    
    ArrayList<SysCFG> systems;
    
    @Override
    public void loadDefault() {
        //SysCFG(entitySystem, priority, haltOnGamePause, loadInSpace, loadInWorld, loadOnDesktop, loadOnMobile)
        // sorted by priority. engine executes systems in order of priority from low value to high.
        // gaps between values for easy insertion and of new systems and easy tuning/adjusting.
        // flags (handled by @SystemLoader and @GameScreen):
        // - priority: order of execution. order is important for logic and rendering.
        // - haltOnGamePause: if true, system stops processing when game is paused.
        // - loadInSpace: if true, system will be loaded when player is in space.
        // - loadInWorld: if true, system will be loaded when player is on a planet.
        
        systems = new ArrayList<>();
        systems.add(new SysCFG(ClearScreenSystem.class, 0, false, true, true));
        
        //---user input---
        systems.add(new SysCFG(DesktopInputSystem.class, 10, true, true, true));
        systems.add(new SysCFG(ControllerInputSystem.class, 15, true, true, true));
        //systems.add(new SysCFG(MobileInputSystem.class, 20, true, true, true));
        
        
        //---logic---
        systems.add(new SysCFG(AISystem.class, 30, true, true, true));
        
        systems.add(new SysCFG(CharacterControlSystem.class, 40, true, true, true));
        systems.add(new SysCFG(DashSystem.class, 41, true, false, true));
        
        systems.add(new SysCFG(ShipControlSystem.class, 50, true, true, true));
        systems.add(new SysCFG(BarrelRollSystem.class, 52, true, true, true));
        systems.add(new SysCFG(ShieldSystem.class, 53, true, true, true));
        systems.add(new SysCFG(CannonSystem.class, 55, true, true, true));
        systems.add(new SysCFG(HyperDriveSystem.class, 59, true, true, true));
        
        systems.add(new SysCFG(OrbitSystem.class, 60, true, true, false));
        systems.add(new SysCFG(WorldWrapSystem.class, 65, true, false, true));
        
        systems.add(new SysCFG(Box2DPhysicsSystem.class, 70, true, true, true));
        //NOTE: chargecannon ghost charge rendering works better after physics system has updated the transforms. otherwise jitter while movement
        systems.add(new SysCFG(ChargeCannonSystem.class, 71, true, true, true));
        
        //---loading---
        systems.add(new SysCFG(WorldLoadingSystem.class, 90, true, false, true));
        systems.add(new SysCFG(SpaceLoadingSystem.class, 91, false, true, false));
        systems.add(new SysCFG(AsteroidBeltSystem.class, 92, true, true, false));
        systems.add(new SysCFG(PlayerSpawnSystem.class, 99, true, true, true));
        //systems.add(new SysCFG(PlanetAISpawnerSystem.class, 93, true, true, false, true, true));
        
    
        //----render----
        // Rendering Pipeline: render order (priority) is important for which layer draws on top of which layer.
        // - clear screen -> clear color and depth buffer, then repaint solid color. clears display between frames
        // - cam update -> move, zoom, and update camera
        // - parallax grid render -> "under-hud" / frame of reference / debug tool
        // - sprite 2D renderer -> render regular textures no shader
        // - sprite 2D shader renderer -> render textures with shader applied
        // - trail render -> render movement paths, mostly for debug and frame of reference - todo: trail should render on top of planets but below other sprites, if trail is changed to spritebatch it could use z-order ?
        // - asteroid renderer -> custom shaperenderer to draw filled polygons
        // - sprite 3D renderer -> render 3d meshes with no shader
        // - todo: sprite 3D shader renderer -> render 3d meshes with shader applied
        // - shield renderer -> shape render post sprite (possible shader for glow? render to fbo: bloom)
        // - particle renderer -> render particles on top of sprites
        // - hud system -> render player info, minimap, healthbars, and in game Menu when paused
        // - todo: touchcontrolrendersystem: should decouple from hud
        // - screen transition system -> renders full screen white out animation between game states and handles transition logic
        // - debug system -> render debug and diagnostic info: should always be last so we can see debug info at all times, hence high-value priority to make execute near end of frame
        //  [?] could have multilayer rendering if needed:
        //      eg: particle layer pre sprite (under sprites), particle layer post sprite (over sprites)
        systems.add(new SysCFG(CameraSystem.class, 100, true, true, true));
        systems.add(new SysCFG(GridRenderSystem.class, 101, false, true, true));
        systems.add(new SysCFG(SpaceStationSystem.class, 102, false, true, false));
        systems.add(new SysCFG(SpaceParallaxSystem.class, 105, false, true, false));
        //systems.add(new SysCFG(SpaceDustSystem.class, 106, false, true, false, true, true));
        systems.add(new SysCFG(WorldRenderingSystem.class, 110, false, false, true));
        systems.add(new SysCFG(TileGridSystem.class, 111, false, false, true));
        systems.add(new SysCFG(TrailRenderSystem.class, 112, false, true, true));
        systems.add(new SysCFG(Sprite2DRenderSystem.class, 120, false, true, true));
        systems.add(new SysCFG(Sprite2DShaderRenderSystem.class, 121, false, true, true));
        systems.add(new SysCFG(AsteroidRenderSystem.class, 122, false, true, false));
        systems.add(new SysCFG(LaserSystem.class, 123, true, true, true));
        systems.add(new SysCFG(TractorBeamSystem.class, 124, true, true, true));
        systems.add(new SysCFG(ProjectileHitRenderSystem.class, 125, false, true, true));
        systems.add(new SysCFG(Sprite3DRenderSystem.class, 126, false, true, true));
        systems.add(new SysCFG(ShieldRenderSystem.class, 127, false, true, true));
        systems.add(new SysCFG(ParticleSystem.class, 130, true, true, true));
        systems.add(new SysCFG(HUDSystem.class, 200, false, true, true));
        systems.add(new SysCFG(ScreenTransitionSystem.class, 300, true, true, true));
    
        systems.add(new SysCFG(SoundSystem.class, 500, false, true, true));
    
        systems.add(new SysCFG(ExpireSystem.class, 800, true, true, true));
        
        systems.add(new SysCFG(DebugSystem.class, 900, false, true, true));
        
        //Should always be last system fired. This is where entities flagged with the removal component are removed from engine
        //and resources are auto-disposed. Any systems processed after this should be careful if they rely
        //on disposable data (eg: Textures) once an entity has been removed from the engine.
        systems.add(new SysCFG(RemovalSystem.class, 1000, false, true, true));
    }
    
    public ArrayList<SysCFG> getSystems() {
        return systems;
    }
    
    public SysCFG getConfig(EntitySystem system) {
        return getConfig(system.getClass().getName());
    }
    
    public SysCFG getConfig(String className) {
        for (SysCFG sys : systems) {
            if (sys.getClassName().equals(className))
                return sys;
        }
        
        Gdx.app.error(this.getClass().getSimpleName(), "Could not find config for: " + className);
        return null;
    }
    
}
