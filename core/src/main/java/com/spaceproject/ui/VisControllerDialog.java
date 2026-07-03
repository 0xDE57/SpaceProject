package com.spaceproject.ui;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisDialog;

/**
 * Modified version of ControllerMenuDialog as a custom VisUI actor.
 * https://github.com/MrStahlfelge/gdx-controllerutils
 */
public class VisControllerDialog extends VisDialog {

    protected Array<Actor> buttonsToAdd = new Array<>();
    protected Actor previousFocusedActor;
    protected Actor previousEscapeActor;

    public VisControllerDialog(String title) {
        super(title);
    }

    @Override
    public VisDialog button(Button button, Object object) {
        addFocusableQueue(button);
        return super.button(button, object);
    }

    @Override
    protected void setStage(Stage stage) {
        if (getStage() != null) {
            ((ControllerMenuStage) getStage()).removeFocusableActors(buttonsToAdd);
        }

        super.setStage(stage);
        if (stage != null) {
            ((ControllerMenuStage) stage).addFocusableActors(buttonsToAdd);
        }
    }

    @Override
    public VisDialog show(Stage stage, Action action) {
        previousFocusedActor = null;
        previousEscapeActor = null;

        super.show(stage, action);

        previousFocusedActor = ((ControllerMenuStage) stage).getFocusedActor();
        previousEscapeActor = ((ControllerMenuStage) stage).getEscapeActor();
        ((ControllerMenuStage) stage).setFocusedActor(getConfiguredDefaultActor());
        ((ControllerMenuStage) stage).setEscapeActor(getConfiguredEscapeActor());

        return this;
    }

    /**
     * @return Actor that should get the focus when the dialog is shown
     */
    protected Actor getConfiguredDefaultActor() {
        return buttonsToAdd.size >= 1 ? buttonsToAdd.get(0) : null;
    }

    /**
     * @return Actor that should take Action when the escape button is hit while the dialog is shown
     */
    protected Actor getConfiguredEscapeActor() {
        return buttonsToAdd.size == 1 ? buttonsToAdd.get(0) : null;
    }

    @Override
    public void hide(Action action) {
        if (getStage() != null) {
            Actor currentFocusedActor = ((ControllerMenuStage) getStage()).getFocusedActor();
            if (previousFocusedActor != null && previousFocusedActor.getStage() == getStage()
                    && (currentFocusedActor == null || !currentFocusedActor.hasParent() || currentFocusedActor.isDescendantOf(this))) {
                ((ControllerMenuStage) getStage()).setFocusedActor(previousFocusedActor);
            }
            Actor currentEscapeActor = ((ControllerMenuStage) getStage()).getEscapeActor();
            if (previousEscapeActor != null && previousEscapeActor.getStage() == getStage()
                    && (currentEscapeActor == null || !currentEscapeActor.hasParent() || currentEscapeActor.isDescendantOf(this))) {
                ((ControllerMenuStage) getStage()).setEscapeActor(previousEscapeActor);
            }
        }

        super.hide(action);
    }

    public void addFocusableQueue(Actor actor) {
        buttonsToAdd.add(actor);
    }

}
