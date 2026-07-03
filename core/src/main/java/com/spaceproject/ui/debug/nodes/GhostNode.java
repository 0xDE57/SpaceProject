package com.spaceproject.ui.debug.nodes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.spaceproject.ui.debug.ECSExplorerWindow;
import com.spaceproject.utility.SimpleTimer;

import static com.spaceproject.generation.FontLoader.skinSmallFont;

/**
 * Created by Whilow Schock on 25/09/2019.
 */
public class GhostNode extends UpdateNode {
    
    private SimpleTimer removeTimer;
    
    public GhostNode(UpdateNode nodeRemoved, boolean includeChildren) {
        super(new VisLabel(nodeRemoved.getActor().getName(), skinSmallFont, Color.RED), null);
        
        Tree.Node parent = nodeRemoved.getParent();
        final Array<Tree.Node> parentsSiblings;
        if (parent == null) {
            parentsSiblings = nodeRemoved.getTree().getRootNodes();
        } else {
            parentsSiblings = parent.getChildren();
        }
        //insert in same position as removed node
        int index = parentsSiblings.indexOf(nodeRemoved, false);
        parent.insert(index, this);
        
        
        removeTimer = new SimpleTimer(ECSExplorerWindow.removeTime, true);
        
        setExpanded(nodeRemoved.isExpanded());
        if (includeChildren && isExpanded()) {
            addChildren(nodeRemoved.getChildren(), this);
        }
    }
    
    public void addChildren(Array<Tree.Node> children, Tree.Node root) {
        for (Tree.Node child : children) {
            addChildren(child.getChildren(), root);
            
            add(new VisTreeNode(new VisLabel(child.getActor().getName(), skinSmallFont, Color.RED)));
        }
    }
    
    @Override
    public void update() {
        tryRemove();
    }
    
    public void tryRemove() {
        /*
        if (isExpanded()) {
            //this should be if has focus/isSelection (including children) instead
            return;
        }*/
        
        if (removeTimer.canDoEvent())
            remove();
    }
    
}
