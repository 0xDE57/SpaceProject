package com.spaceproject.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.TableUtils;
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue;
import com.kotcrab.vis.ui.widget.LinkLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import com.spaceproject.ui.menu.tabs.AboutTab;

public class AboutMenu extends VisWindow {
    
    private final TabbedPane tabbedPane;
    private final VisTable container;
    
    boolean vertical = true;
    
    public AboutMenu() {
        super("aboot");//eh?
        getTitleLabel().setAlignment(Align.center);
    
        TableUtils.setSpacingDefaults(this);
    
        setResizable(true);
        setMovable(true);
        addCloseButton();
        closeOnEscape();
        center();
    
        container = new VisTable();
        
        TabbedPane.TabbedPaneStyle style = VisUI.getSkin().get(vertical ? "vertical" : "default", TabbedPane.TabbedPaneStyle.class);
        tabbedPane = new TabbedPane(style);
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab(Tab tab) {
                //Gdx.app.log(this.getClass().getSimpleName(), "TabbedPaneAdapter switched tab: " + tab.getTabTitle());
                container.clearChildren();
                container.add(tab.getContentTable()).expand().fill();
            }
        });
    
        if (style.vertical) {
            top();
            defaults().top();
            add(tabbedPane.getTable()).growY();
            add(container).expand().fill();
        } else {
            //force min height for tabbedPane to fix table layout when tab has a VisScrollPane
            //github.com/kotcrab/vis-editor/issues/206#issuecomment-238012673
            add(tabbedPane.getTable()).minHeight(new PrefHeightIfVisibleValue()).growX();
            row();
            add(container).grow();
        }
    
    
        tabbedPane.add(new AboutTab("test"));
        
        //todo: tabbed scroll lists [about/author, support, dependencies, licence], available from game menu too.
        
        LinkLabel link = new LinkLabel("https://github.com/0xDE57/SpaceProject");
        link.setListener(new LinkLabel.LinkLabelListener() {
            @Override
            public void clicked (String url) {
                Gdx.net.openURI(url);
            }
        });
        //link.pack();
        container.add(link);
        
        //container.pack();
    }
}
