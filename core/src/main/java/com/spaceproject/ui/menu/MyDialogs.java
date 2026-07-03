package com.spaceproject.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.util.dialog.OptionDialogAdapter;
import com.kotcrab.vis.ui.util.dialog.OptionDialogListener;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.SpaceProject;
import com.spaceproject.ui.VisControllerDialog;

/**
 * Modifications of VisUI default Dialogs
 */
public class MyDialogs {

    public static VisControllerDialog showOKDialog (Stage stage, String title, String text) {
        final VisControllerDialog dialog = new VisControllerDialog(title);
        dialog.text(text).padLeft(12).padRight(12);
        dialog.button("ok", null).padBottom(5);
        dialog.pack();
        addOkDismissListener(dialog);

        dialog.show(stage);
        return dialog;
    }

    public static VisControllerDialog showMultiDialog (Stage stage, String title, String text) {
        final VisControllerDialog dialog = new VisControllerDialog(title);
        dialog.text(text);
        dialog.button("abra", null);
        dialog.button("kabarbra", null);
        dialog.button("alexazam", null);
        dialog.button("...", null);
        ((TextButton)dialog.getButtonsTable().getChild(1)).setDisabled(true);
        dialog.pack();
        addDismissListener(dialog);

        dialog.show(stage);
        return dialog;
    }

    /*
    public static VisControllerDialog showCustomDialog (Stage stage, String title, String text, String[] buttons, T[] returns, ConfirmDialogListener<T> listener) {
        return null;
    }*/

    public static VisControllerDialog showYesNoDialog (Stage stage, String title, String text, OptionDialogListener listener) {
        final VisControllerDialog dialog = new VisControllerDialog(title);
        dialog.text(text);
        dialog.button("no", null);
        dialog.button("yes", null);
        dialog.pack();
        addDismissListener(dialog);

        ChangeListener noBtnListener = new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                listener.no();
                dialog.fadeOut();
            }
        };
        ChangeListener yesBtnListener = new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                listener.yes();
                dialog.fadeOut();
            }
        };
        dialog.getButtonsTable().getChildren().get(0).addListener(noBtnListener);
        dialog.getButtonsTable().getChildren().get(1).addListener(yesBtnListener);

        dialog.show(stage);
        return dialog;
    }

    public static VisDialog showAboutDialog(Stage stage) {
        final VisControllerDialog dialog = new VisControllerDialog("");
        dialog.centerWindow();

        String aboutText = "Shoot some asteroids, they fall apart!";
        aboutText += "\nversion: " + SpaceProject.VERSION
                + "\nlibGDX: " + com.badlogic.gdx.Version.VERSION;
        aboutText += "\nDeveloped with <3";
        dialog.text(aboutText);

        LinkLabel link = new LinkLabel("https://github.com/0xDE57/SpaceProject");
        link.setListener(url -> MyDialogs.showYesNoDialog(stage, "Visit URL? (open browser)", url, new OptionDialogAdapter() {
            @Override
            public void yes() {
                Gdx.net.openURI(url);
            }
        }));
        dialog.getContentTable().row();
        dialog.getContentTable().add(link);

        dialog.button("ok");
        dialog.pad(10.0f);

        addOkDismissListener(dialog);

        dialog.pack();
        stage.addActor(dialog.fadeIn());
        return dialog;
    }

    private static void addDismissListener(VisControllerDialog dialog) {
        dialog.addListener(new InputListener() {
            @Override
            public boolean keyDown (InputEvent event, int keycode) {
                switch (keycode) {
                    case Keys.ESCAPE:
                    case Keys.BACKSPACE:
                        dialog.hide();
                        return true;
                }
                return false;
            }
        });
    }
    private static void addOkDismissListener(VisControllerDialog dialog) {
        dialog.addListener(new InputListener() {
            @Override
            public boolean keyDown (InputEvent event, int keycode) {
                switch (keycode) {
                    case Keys.ESCAPE:
                    case Keys.BACKSPACE:
                    case Keys.SPACE:
                    case Keys.ENTER:
                        dialog.hide();
                        return true;
                }
                return false;
            }
        });
    }

}
