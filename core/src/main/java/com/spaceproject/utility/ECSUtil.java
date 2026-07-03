package com.spaceproject.utility;


import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.spaceproject.components.*;

import java.lang.reflect.Field;

public class ECSUtil {

    static String className = ECSUtil.class.getSimpleName();
    static StringBuilder infoString = new StringBuilder();

    public static Component transferComponent(Entity fromEntity, Entity toEntity, Class<? extends Component> componentClass) {
        Component transferredComponent = fromEntity.remove(componentClass);
        if (transferredComponent == null) {
            /*
            Gdx.app.debug("ECSUtil", "Warning: " + DebugUtil.objString(fromEntity)
                    + " has no " + componentClass.getSimpleName()
                    + " to give to " + DebugUtil.objString(toEntity));*/
            return null;
        }
    
        toEntity.add(transferredComponent);
        //Gdx.app.debug("ECSUtil", "transferComponent: " + DebugUtil.objString(transferredComponent)
        //        + ": " + DebugUtil.objString(fromEntity) + " -> " + DebugUtil.objString(toEntity));
        return transferredComponent;
    }
    
    public static void transferControl(Entity fromEntity, Entity toEntity) {
        transferComponent(fromEntity, toEntity, CameraFocusComponent.class);
        transferComponent(fromEntity, toEntity, ControlFocusComponent.class);
        transferComponent(fromEntity, toEntity, AIComponent.class);
        transferComponent(fromEntity, toEntity, ControllableComponent.class);
        transferComponent(fromEntity, toEntity, CamTargetComponent.class);
        transferComponent(fromEntity, toEntity, StatsComponent.class);
    }
    
    public static Entity copyEntity(Entity entity) {
        Entity newEntity = new Entity();
        for (Component c : entity.getComponents()) {
            transferComponent(entity, newEntity, c.getClass());
        }
        return newEntity;
    }
    
    public static Entity closestEntity(Vector2 position, ImmutableArray<Entity> entities) {
        if (entities == null || entities.size() == 0)
            return null;
        
        Entity targetEntity = entities.first();
        float targetDist = position.dst(Mappers.transform.get(targetEntity).pos);
        for (Entity searchEnt : entities) {
            float dist = position.dst(Mappers.transform.get(searchEnt).pos);
            if (dist < targetDist) {
                targetDist = dist;
                targetEntity = searchEnt;
            }
        }
        
        return targetEntity;
    }


    /** avoid this mess
     * todo: fix engine particle effect to not require multiple entities with AttachedToComponent */
    @Deprecated
    public static Array<Entity> getAttachedEntities(Engine engine, Entity parentEntity) {
        Array<Entity> cluster = new Array<>();
        ImmutableArray<Entity> attachedEntities = engine.getEntitiesFor(Family.all(AttachedToComponent.class).get());
        for (Entity attachedEntity : attachedEntities) {
            AttachedToComponent attachedTo = Mappers.attachedTo.get(attachedEntity);
            if (attachedTo.parentEntity == parentEntity) {
                cluster.add(attachedEntity);
            }
        }
        return cluster;
    }
    
    /*
    public static Array<Entity> getEntityClusterForNode(Engine engine, Entity entity) {
        AttachedToComponent attached = Mappers.attachedTo.get(entity);
        if (attached != null) {
            //todo: if given entity is attached and not the parent itself,
            // then travel recursively up the parent chain and return the entire cluster it belongs to
            // with no duplicate entries
            //cluster.addAll(getAttachedEntities(engine, attached.parentEntity));?
            //?
            //return getAttachedEntities(engine, attached.parentEntity);?
        }
    }*/

    static int peakE, peakC, peakS;
    public static String getECSString(Engine engine, boolean includePeak) {
        int entities = engine.getEntities().size();
        int components = 0;
        for (Entity ent : engine.getEntities()) {
            components += ent.getComponents().size();
        }
        int systems = engine.getSystems().size();
        peakE = Math.max(peakE, entities);
        peakC = Math.max(peakC, components);
        peakS = Math.max(peakS, systems);
        infoString.setLength(0);
        infoString.append("E: ").append(entities)
                .append(" C: ").append(components)
                .append(" S: ").append(systems);
        if (includePeak) {
            infoString.append(" - peak[").append(peakE)
                    .append("/").append(peakC)
                    .append("/").append(peakS)
                    .append("]");
        }
        return infoString.toString();
    }
    
    public static void printSystems(Engine eng) {
        for (EntitySystem sys : eng.getSystems()) {
            Gdx.app.debug(className, sys + " (" + sys.priority + ")");
        }
    }
    
    public static void printEntities(Engine eng) {
        for (Entity entity : eng.getEntities()) {
            printEntity(entity);
        }
    }

    public static void printEntity(Entity entity) {
        infoString.setLength(0);
        infoString.append(entity.toString());
        for (Component c : entity.getComponents()) {
            infoString.append("\n\t").append(c.toString());
            for (Field f : c.getClass().getFields()) {
                try {
                    infoString.append(String.format("\n\t\t%-14s %s", f.getName(), f.get(c)));
                } catch (IllegalArgumentException e) {
                    Gdx.app.error(className, "failed to read fields", e);
                    //infoString.append("failed to read fields").append(e);
                } catch (IllegalAccessException e) {
                    infoString.append("failed to read fields").append(e);
                    //Gdx.app.error(className, "fail to read fields", e);
                }
            }
        }
        Gdx.app.error(className, infoString.toString());
    }
    
}
