package com.spaceproject.ui.debug;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Selection;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.debug.nodes.EntityNode;
import com.spaceproject.ui.debug.nodes.GhostNode;
import com.spaceproject.ui.debug.nodes.VisTreeNode;
import com.spaceproject.ui.debug.nodes.ReflectionNode;
import com.spaceproject.ui.debug.nodes.UpdateNode;
import com.spaceproject.utility.IndependentTimer;

import java.util.Arrays;
import java.util.List;

import static com.spaceproject.generation.FontLoader.skinSmallFont;

/**
 * A tree based debug/dev tool for exploring and inspecting the contents of the engine.
 *      view Systems in the Engine, and their Priorities
 *      view Entities
 *      view Components in Entities
 *      view Fields and their values in Components via reflection
 *      enable and disable systems processing
 *      remove entities (soft: add @RemoveComponent() and cleaned up by @RemovalSystem) (hard: directly remove(), no auto-dispose)
 *      remove components from entities
 *      todo: allow expanding non basic types
 *      todo: allow edit values
 *          int float string -> text
 *          bool -> checkbox
 *          color -> color picker
 *          texture -> render?
 *      todo: view by component, eg:
 *          AIComponent
 *              Entity A
 *              Entity B
 *              Entity C
 *      todo: search bar.
 *          - search entities or components by hashcode
 *          - search by component class (filter)
 *      todo: add Entity button -> open new entity designer window, create empty entity. add Components
 *      todo: add Component button -> add Component to entity
 */
public class ECSExplorerWindow extends VisWindow implements EntityListener {
    
    private Engine engine;
    private VisTree tree;
    private VisTree.Node systemNodes, entityNodes;

    private static int refreshRate = 1000;
    public static int newTime = 2000;
    public static int removeTime = 2000;
    public static boolean showHistory = true;
    public static boolean includeChildren = false;
    private IndependentTimer refreshTimer;
    
    private final int keyUP = Input.Keys.UP;
    private final int keyDown = Input.Keys.DOWN;
    private final int keyLeft = Input.Keys.LEFT;
    private final int keyRight = Input.Keys.RIGHT;

    public ECSExplorerWindow(Engine engine) {
        super("ECS Explorer [F9]");
        
        this.engine = engine;
        refreshTimer = new IndependentTimer(refreshRate, true);

        setResizable(true);
        setMovable(true);
        setSize(500, Gdx.graphics.getHeight() - 20);
        setPosition(10, 10);
        addCloseButton();
        systemNodes = new VisTreeNode(new VisLabel("Systems", skinSmallFont, Color.WHITE));
        entityNodes = new VisTreeNode(new VisLabel("Entities", skinSmallFont, Color.WHITE));
        tree = new VisTree();
        tree.add(systemNodes);
        tree.add(entityNodes);
        
        final VisTable contents = new VisTable();
        final VisTable options = new VisTable();
        final VisCheckBox showHistoryCheck = new VisCheckBox("show history");
        showHistoryCheck.setChecked(showHistory);
        showHistoryCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showHistory = showHistoryCheck.isChecked();
            }
        });
        options.add(showHistoryCheck);
        contents.add(options);
        contents.row();

        final VisScrollPane scrollPane = new VisScrollPane(tree);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.setFadeScrollBars(false);//always show scrollbar
        contents.add(scrollPane).expand().fill();
        add(contents).expand().fill();
    }
    
    public void update() {
        if (!isVisible()) {
            return;
        }
        if (refreshTimer.tryEvent()) {
            refreshNodes();
        }
    }
    
    private void refreshNodes() {
        updateSystems();
        if (GameScreen.isPaused())
            return;

        if (showHistory) {
            clearGhosts(entityNodes.getChildren());
        }
        updateEntities();
    }
    
    private void updateEntities() {
        //update entities
        ((VisLabel)entityNodes.getActor()).setText("Entities (" + engine.getEntities().size() + ")");
        for (Entity entity : engine.getEntities()) {
            VisTree.Node entNode = entityNodes.findNode(entity);
            if (entNode == null) {
                entityNodes.add(new EntityNode(entity));
            } else {
                ((UpdateNode) entNode).update();
            }
        }
    }
    
    private void updateSystems() {
        //add nodes
        ((VisLabel)systemNodes.getActor()).setText("Systems (" + engine.getSystems().size() + ")");
        for (EntitySystem system : engine.getSystems()) {
            VisTree.Node sysNode = systemNodes.findNode(system);
            if (sysNode == null) {
                systemNodes.add(new ReflectionNode(system));
            }
        }
        
        //update nodes, clean up dead nodes
        for (Object o : systemNodes.getChildren()) {
            UpdateNode node = (UpdateNode)o;
            if (!engine.getSystems().contains((EntitySystem) node.getValue(), false)) {
                node.remove();
            } else {
                node.update();
            }
        }
    }
    
    private void clearGhosts(Array<VisTree.Node> nodes) {
        for (VisTree.Node child : nodes) {
            if (child.getChildren().size > 0) {
                clearGhosts(child.getChildren());
            }
            
            if (child instanceof GhostNode) {
                ((GhostNode) child).tryRemove();
            }
        }
    }
    
    //region menu controls
    public boolean isVisible() {
        return getStage() != null;
    }
    
    private void show() {
        show(GameScreen.getStage());
    }

    private void show(Stage stage) {
        stage.addActor(this);
        fadeIn();
        
        engine.addEntityListener(this);
        refreshNodes();
    }
    
    private void hide() {
        fadeOut();
        
        engine.removeEntityListener(this);
        systemNodes.clearChildren();
        entityNodes.clearChildren();
    }
    
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }
    
    public void keyDown(InputEvent event, int keycode) {
        Selection<VisTree.Node> nodeSelection = tree.getSelection();
        VisTree.Node selectedNode = nodeSelection.first();
        if (selectedNode == null) {
            List<Integer> keys = Arrays.asList(keyUP, keyDown, keyLeft, keyRight);
            if (keys.contains(keycode)) {
                nodeSelection.choose((VisTree.Node) tree.getRootNodes().first());
            }
            return;
        }
        
        final Array<VisTree.Node> siblingNodes;
        VisTree.Node parentNode = selectedNode.getParent();
        if (parentNode == null) {
            siblingNodes = tree.getRootNodes();
        } else {
            siblingNodes = parentNode.getChildren();
        }
        int index = siblingNodes.indexOf(selectedNode, false);

        VisTree.Node chosen = null;
        switch (keycode) {
            case keyUP:
                if (index > 0) {
                    chosen = getNextNodeAbove(siblingNodes.get(index - 1));
                } else {
                    if (parentNode != null) {
                        chosen = parentNode;
                    }
                }
                break;
            case keyDown:
                if (selectedNode.isExpanded() && (selectedNode.getChildren().size > 0)) {
                    chosen = (VisTree.Node) selectedNode.getChildren().first();
                } else {
                    chosen = getNextNodeBelow(siblingNodes, parentNode, index);
                }
                break;
            case keyLeft:
                if (selectedNode.isExpanded()) {
                    selectedNode.collapseAll();
                } else {
                    if (parentNode != null) {
                        chosen = parentNode;
                    }
                }
                break;
            case keyRight:
                if (!selectedNode.isExpanded()) {
                    selectedNode.setExpanded(true);
                }
                break;
        }
        
        if (chosen != null) {
            nodeSelection.choose(chosen);
        }
    }
    
    private VisTree.Node getNextNodeAbove(VisTree.Node current) {
        if (current.isExpanded()) {
            if (current.getChildren().size > 0) {
                VisTree.Node nextNode = (VisTree.Node) current.getChildren().get(current.getChildren().size - 1);
                return getNextNodeAbove(nextNode);
            }
        }
        
        return current;
    }
    
    private VisTree.Node getNextNodeBelow(Array<VisTree.Node> siblingNodes, VisTree.Node parentNode, int index) {
        if (index < siblingNodes.size - 1) {
            return siblingNodes.get(index + 1);
        } else if (index == siblingNodes.size - 1) {
            if (parentNode != null) {
                VisTree.Node parentsParent = parentNode.getParent(); //(grandparents?)
                final Array<VisTree.Node> parentsSiblings; //(aunts/uncles?)
                if (parentsParent == null) {
                    parentsSiblings = tree.getRootNodes();
                } else {
                    parentsSiblings = parentsParent.getChildren();
                }
                index = parentsSiblings.indexOf(parentNode, false);
                return getNextNodeBelow(parentsSiblings, parentsParent, index);
            }
        }
        return null;
    }
    //endregion
    
    @Override
    public void entityAdded(Entity entity) {
        entityNodes.add(new EntityNode(entity, showHistory));//todo: pool
    }
    
    @Override
    public void entityRemoved(Entity entity) {
        VisTree.Node node = entityNodes.findNode(entity);
        if (node == null) {
            //Gdx.app.error(getClass().getSimpleName(), "node for entity null!");
            return;
        }
        if (showHistory) {
            ((UpdateNode) node).removeAndCreateGhost(includeChildren);
        } else {
            node.remove();
        }
    }
    
}
