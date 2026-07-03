package com.spaceproject.ui.debug.nodes;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.spaceproject.components.RemoveComponent;
import com.spaceproject.ui.debug.ECSExplorerWindow;
import com.spaceproject.utility.DebugUtil;
import com.spaceproject.utility.SimpleTimer;

import static com.spaceproject.generation.FontLoader.skinSmallFont;


public class EntityNode extends UpdateNode {
    
    boolean isNew = false;
    private SimpleTimer newTimer;
    
    public EntityNode(Entity entity) {
        super(createActor(entity), entity);
    }

    private static Actor createActor(Entity entity) {
        VisTable table = new VisTable();
        table.add(new VisLabel(DebugUtil.objString(entity),  skinSmallFont, Color.WHITE)).width(400);

        //VisTextButton addButton = new VisTextButton("[+] add");
        //table.add(addButton);

        VisTextButton deleteButton = new VisTextButton("delete");
        deleteButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                entity.add(new RemoveComponent());
                Gdx.app.debug(getClass().getSimpleName(), "marked entity for death: " + DebugUtil.objString(entity));
            }
        });
        table.add(deleteButton);
        return table;
    }

    public EntityNode(Entity entity, boolean markNew) {
        this(entity);
        
        isNew = markNew;
        if (isNew) {
            newTimer = new SimpleTimer(ECSExplorerWindow.newTime, true);
            VisLabel label = (VisLabel) ((VisTable)getActor()).getChildren().first();
            label.setColor(Color.GREEN);
        }
    }
    
    public Entity getEntity() {
        return (Entity) getValue();
    }
    
    @Override
    public void update() {
        if (getValue() == null) {
            return;
        }

        VisLabel label = (VisLabel) ((VisTable)getActor()).getChildren().first();
        if (isNew && newTimer.tryEvent()) {
            isNew = false;
            newTimer = null;
            label.setColor(Color.WHITE);
        }

        label.setText(toString());
        
        if (!isExpanded())
            return;
        
        boolean showHistory = ECSExplorerWindow.showHistory;
        
        //add nodes
        ImmutableArray<Component> components = getEntity().getComponents();
        for (Component comp : components) {
            if (findNode(comp) == null) {
                add(new ReflectionNode(comp, showHistory));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Object child : getChildren()) {
            UpdateNode node = (UpdateNode)child;
            if (!components.contains((Component) node.getValue(), false)) {
                if (showHistory) {
                    if (!(node instanceof GhostNode)) {
                        node.removeAndCreateGhost(ECSExplorerWindow.includeChildren);
                    }
                } else {
                    node.remove();
                }
            } else {
                node.update();
            }
        }
    }
    
    @Override
    public String toString() {
        if (getValue() == null)
            return super.toString();
        
        return DebugUtil.objString(getEntity()) + " (" + getEntity().getComponents().size() + ")";
    }

}
