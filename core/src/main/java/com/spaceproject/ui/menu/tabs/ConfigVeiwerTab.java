package com.spaceproject.ui.menu.tabs;


import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane;
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter;
import com.spaceproject.config.Config;

import java.util.ArrayList;

public class ConfigVeiwerTab extends HotKeyTab {
    
    private final TabbedPane tabbedPane;
    private int itemPadding = 2;
    
    public ConfigVeiwerTab(final Stage stage, ArrayList<Config> configurations) {
        super("Config List", Input.Keys.UNKNOWN);
        
        boolean vertical = true;
        TabbedPane.TabbedPaneStyle style = VisUI.getSkin().get(vertical ? "vertical" : "default", TabbedPane.TabbedPaneStyle.class);
        tabbedPane = new TabbedPane(style);
        final VisTable container = new VisTable();
        
        tabbedPane.addListener(new TabbedPaneAdapter() {
            @Override
            public void switchedTab(Tab tab) {
                container.clearChildren();
                container.add(tab.getContentTable()).expand().fill();
            }
        });
    
    
        if (style.vertical) {
            //top();
            //defaults().top();
            getContentTable().add(tabbedPane.getTable()).growY();
            getContentTable().add(container).expand().fill();
        } else {
            //force min height for tabbedPane to fix table layout when tab has a VisScrollPane
            //github.com/kotcrab/vis-editor/issues/206#issuecomment-238012673
            getContentTable().add(tabbedPane.getTable()).minHeight(new PrefHeightIfVisibleValue()).growX();
            getContentTable().row();
            getContentTable().add(container).grow();
        }
        
        for (Config config : configurations) {
            tabbedPane.add(new ConfigTab(stage, config));
            /*
            TextButton editButton = new TextButton(config.getClass().getSimpleName(), VisUI.getSkin());
            editButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    //gameMenu.getStage()
                    
                }
            });
            
            getContentTable().add(editButton).growX().pad(itemPadding).row();*/
        }
        tabbedPane.switchTab(0);
        
        getContentTable().add(container);
    }
    
}
