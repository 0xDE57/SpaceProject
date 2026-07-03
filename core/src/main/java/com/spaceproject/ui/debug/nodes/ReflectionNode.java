package com.spaceproject.ui.debug.nodes;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.debug.ECSExplorerWindow;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;

import static com.spaceproject.generation.FontLoader.skinSmallFont;


public class ReflectionNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public ReflectionNode(Object object) {
        super(createActor(object), object);
        init();
    }

    private static Actor createActor(Object object) {
        VisTable table = new VisTable();
        boolean isComponent = object instanceof Component;
        table.add(new VisLabel(DebugUtil.objString(object), skinSmallFont, Color.WHITE)).width(isComponent ? 392 : 389);

        VisTextButton modifyButton = null;
        //if object is type system -> disable set processing to false
        if (object instanceof EntitySystem) {
            EntitySystem system = (EntitySystem) object;
            modifyButton = new VisTextButton(system.checkProcessing() ? "enabled" : "disabled");
            modifyButton.setColor(system.checkProcessing() ? Color.GREEN : Color.RED);
            VisTextButton finalDisableButton = modifyButton;
            modifyButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    system.setProcessing(!system.checkProcessing());
                    finalDisableButton.setText(system.checkProcessing() ? "enabled" : "disabled");
                    finalDisableButton.setColor(system.checkProcessing() ? Color.GREEN : Color.RED);
                    Gdx.app.debug(getClass().getSimpleName(), system.getClass().getSimpleName() + " processing: " + system.checkProcessing());
                }
            });
        }
        //if object is type component -> remove component from entity
        if (isComponent) {
            Component component = (Component) object;
            modifyButton = new VisTextButton("delete");
            modifyButton.setColor(Color.RED);
            modifyButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    ImmutableArray<Entity> candidateEntities = GameScreen.getEngine().getEntitiesFor(Family.all(component.getClass()).get());
                    for (Entity entity : candidateEntities) {
                        if (entity.getComponent(component.getClass()).equals(component)) {
                            entity.remove(component.getClass());
                            Gdx.app.debug(getClass().getSimpleName(), "manually removed: " + DebugUtil.objString(component));
                            break;
                        }
                    }
                }
            });
        }
        if (modifyButton != null) {
            table.add(modifyButton);
        }
        return table;
    }

    public ReflectionNode(Object object, boolean markNew) {
        this(object);
        VisLabel label = (VisLabel) ((VisTable)getActor()).getChildren().first();
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(ECSExplorerWindow.newTime, true);
            label.setColor(Color.GREEN);
        }
    }
    
    private void init() {
        for (Field f : getValue().getClass().getDeclaredFields()) {
            f.setAccessible(true);
            add(new FieldNode(new VisLabel("init", skinSmallFont, Color.WHITE), getValue(), f));
        }
    }
    
    @Override
    public void update() {
        if (isNew && newTimer.tryEvent()) {
            isNew = false;
            newTimer = null;
            VisLabel label = (VisLabel) ((VisTable)getActor()).getChildren().first();
            label.setColor(Color.WHITE);
        }

        if (getValue() instanceof EntitySystem) {
            EntitySystem system = (EntitySystem) getValue();
            VisTextButton enableButton = (VisTextButton) ((VisTable) getActor()).getChildren().items[1];
            enableButton.setText(system.checkProcessing() ? "enabled" : "disabled");
            enableButton.setColor(system.checkProcessing() ? Color.GREEN : Color.RED);
            //Gdx.app.debug(getClass().getSimpleName(), system.getClass().getSimpleName() + " state: " + system.checkProcessing());
        }

        if (!isExpanded())
            return;
        
        for (Object node : getChildren())
            ((FieldNode) node).update();
    }
    
    @Override
    public String toString() {
        if (getValue() == null)
            return super.toString();
        
        return DebugUtil.objString(getValue());
    }

}
