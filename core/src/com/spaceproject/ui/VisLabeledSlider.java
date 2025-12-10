package com.spaceproject.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;

public class VisLabeledSlider extends VisTable {

    private final VisSlider slider;
    private final VisLabel label;
    private final VisTextField textField;

    public VisLabeledSlider(float min, float max, float stepSize, String field) {
        slider = new VisSlider(min, max, stepSize, false);

        label = new VisLabel(field);

        textField = new VisTextField("0");
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                textField.setText(String.valueOf(slider.getValue()));
            }
        });

        defaults().padRight(10).padLeft(10);
        add(label);
        add(slider);
        add(textField);

        //pack();
        label.setWidth(200);
    }

    public float getValue() {
        return slider.getValue();
    }

    public void setValue(float gravity) {
        slider.setValue(gravity);
    }

}