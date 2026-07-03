package com.spaceproject.utility;

import com.badlogic.ashley.core.ComponentMapper;
import com.spaceproject.components.*;

public class Mappers {
    
    public static final ComponentMapper<AIComponent> AI = ComponentMapper.getFor(AIComponent.class);
    public static final ComponentMapper<AISpawnComponent> spawn = ComponentMapper.getFor(AISpawnComponent.class);
    public static final ComponentMapper<AsteroidComponent> asteroid = ComponentMapper.getFor(AsteroidComponent.class);
    public static final ComponentMapper<AsteroidBeltComponent> asteroidBelt = ComponentMapper.getFor(AsteroidBeltComponent.class);
    public static final ComponentMapper<AttachedToComponent> attachedTo = ComponentMapper.getFor(AttachedToComponent.class);
    public static final ComponentMapper<BarrelRollComponent> barrelRoll = ComponentMapper.getFor(BarrelRollComponent.class);
    public static final ComponentMapper<BarycenterComponent> barycenter = ComponentMapper.getFor(BarycenterComponent.class);
    public static final ComponentMapper<CameraFocusComponent> camFocus = ComponentMapper.getFor(CameraFocusComponent.class);
    public static final ComponentMapper<CannonComponent> cannon = ComponentMapper.getFor(CannonComponent.class);
    public static final ComponentMapper<CargoComponent> cargo = ComponentMapper.getFor(CargoComponent.class);
    public static final ComponentMapper<CharacterComponent> character = ComponentMapper.getFor(CharacterComponent.class);
    public static final ComponentMapper<ChargeCannonComponent> chargeCannon = ComponentMapper.getFor(ChargeCannonComponent.class);
    public static final ComponentMapper<ControlFocusComponent> controlFocus = ComponentMapper.getFor(ControlFocusComponent.class);
    public static final ComponentMapper<ControllableComponent> controllable = ComponentMapper.getFor(ControllableComponent.class);
    public static final ComponentMapper<DamageComponent> damage = ComponentMapper.getFor(DamageComponent.class);
    public static final ComponentMapper<DashComponent> dash = ComponentMapper.getFor(DashComponent.class);
    public static final ComponentMapper<DockedComponent> docked = ComponentMapper.getFor(DockedComponent.class);
    public static final ComponentMapper<ExpireComponent> expire = ComponentMapper.getFor(ExpireComponent.class);
    public static final ComponentMapper<HealthComponent> health = ComponentMapper.getFor(HealthComponent.class);
    public static final ComponentMapper<HyperDriveComponent> hyper = ComponentMapper.getFor(HyperDriveComponent.class);
    public static final ComponentMapper<ItemComponent> item = ComponentMapper.getFor(ItemComponent.class);
    public static final ComponentMapper<LaserComponent> laser = ComponentMapper.getFor(LaserComponent.class);
    public static final ComponentMapper<MapComponent> map = ComponentMapper.getFor(MapComponent.class);
    public static final ComponentMapper<OrbitComponent> orbit = ComponentMapper.getFor(OrbitComponent.class);
    public static final ComponentMapper<ParticleComponent> particle = ComponentMapper.getFor(ParticleComponent.class);
    public static final ComponentMapper<PhysicsComponent> physics = ComponentMapper.getFor(PhysicsComponent.class);
    public static final ComponentMapper<PlanetComponent> planet = ComponentMapper.getFor(PlanetComponent.class);
    public static final ComponentMapper<RespawnComponent> respawn = ComponentMapper.getFor(RespawnComponent.class);
    public static final ComponentMapper<ScreenTransitionComponent> screenTrans = ComponentMapper.getFor(ScreenTransitionComponent.class);
    public static final ComponentMapper<SeedComponent> seed = ComponentMapper.getFor(SeedComponent.class);
    public static final ComponentMapper<ShaderComponent> shader = ComponentMapper.getFor(ShaderComponent.class);
    public static final ComponentMapper<ShieldComponent> shield = ComponentMapper.getFor(ShieldComponent.class);
    public static final ComponentMapper<SoundComponent> sound = ComponentMapper.getFor(SoundComponent.class);
    public static final ComponentMapper<SpaceStationComponent> spaceStation = ComponentMapper.getFor(SpaceStationComponent.class);
    public static final ComponentMapper<Sprite3DComponent> sprite3D = ComponentMapper.getFor(Sprite3DComponent.class);
    public static final ComponentMapper<StarComponent> star = ComponentMapper.getFor(StarComponent.class);
    public static final ComponentMapper<StatsComponent> stat = ComponentMapper.getFor(StatsComponent.class);
    public static final ComponentMapper<TextureComponent> texture = ComponentMapper.getFor(TextureComponent.class);
    public static final ComponentMapper<TractorBeamComponent> tractor = ComponentMapper.getFor(TractorBeamComponent.class);
    public static final ComponentMapper<TrailComponent> trail = ComponentMapper.getFor(TrailComponent.class);
    public static final ComponentMapper<TransformComponent> transform = ComponentMapper.getFor(TransformComponent.class);
    public static final ComponentMapper<VehicleComponent> vehicle = ComponentMapper.getFor(VehicleComponent.class);

}
