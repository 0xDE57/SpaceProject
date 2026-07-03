package com.spaceproject.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.IndependentTimer;

/**
 * Modified version of: https://github.com/MrStahlfelge/gdx-controllerutils
 * More compatible with VisUI and some other custom components and behaviors.
 */
public class ControllerMenuStage extends Stage implements ControllerListener {

    private static final float INITIAL_DIRECTION_EMPH_FACTOR = 3.1f;
    private final Vector2 controllerTempCoords = new Vector2();
    private final Array<Actor> focusableActors = new Array<>();
    private boolean isPressed;
    private boolean focusOnTouchdown = true;
    private boolean sendMouseOverEvents = true;
    private Actor focusedActor;
    private Actor escapeActor;
    private float directionEmphFactor = INITIAL_DIRECTION_EMPH_FACTOR;

    int mapButtonA = Input.Keys.ENTER;
    int mapButtonB = Input.Keys.BACKSPACE;
    private float leftStickVertAxis, leftStickHorAxis;
    private final IndependentTimer lastFocusTimer = new IndependentTimer(200, true);


    public ControllerMenuStage(Viewport viewport) {
        super(viewport);
    }

    public boolean isFocusOnTouchdown() {
        return focusOnTouchdown;
    }

    /**
     * @param focusOnTouchdown activate if a click or tap on a focusable actor should set the controller focus to this
     *                         actor. Default is true
     */
    public ControllerMenuStage setFocusOnTouchdown(boolean focusOnTouchdown) {
        this.focusOnTouchdown = focusOnTouchdown;
        return this;
    }

    public boolean isSendMouseOverEvents() {
        return sendMouseOverEvents;
    }

    /**
     * @param sendMouseOverEvents activate if the Actor gaining or losing focus should receive events as if the mouse
     *                            is over the Actor. Default is true. This will highlight a focused Button with the
     *                            Skin's over-drawable. For libGDX 1.9.9, you should disable this in order to use the
     *                            new focused drawables in Actor styles.
     */
    public ControllerMenuStage setSendMouseOverEvents(boolean sendMouseOverEvents) {
        this.sendMouseOverEvents = sendMouseOverEvents;
        return this;
    }

    public Actor getEscapeActor() {
        return escapeActor;
    }

    /**
     * @param escapeActor the actor that should receive a touch event when escape key is pressed
     */
    public void setEscapeActor(Actor escapeActor) {
        //String old = this.escapeActor == null ? "" : " old escape: " + ((TextButton) this.escapeActor).getText().toString();
        //Gdx.app.error(getClass().getSimpleName(), "escape set gained: " + ((TextButton) escapeActor).getText().toString() + " old: " + old);
        this.escapeActor = escapeActor;
    }

    public void addFocusableActor(Actor actor) {
        focusableActors.add(actor);
    }

    public void addFocusableActors(Array<Actor> actors) {
        for (int i = 0; i < actors.size; i++)
            addFocusableActor(actors.get(i));
    }

    public void clearFocusableActors() {
        setFocusedActor(null);
        focusableActors.clear();
    }

    /**
     * garbage collects all actors from focusable list that are not present on the stage
     */
    public void removeFocusableActorsNotOnStage() {
        for (int i = focusableActors.size - 1; i >= 0; i--) {
            if (focusableActors.get(i).getStage() != this)
                focusableActors.removeIndex(i);
        }
    }

    public void removeFocusableActor(Actor actor) {
        focusableActors.removeValue(actor, true);
    }

    public void removeFocusableActors(Array<Actor> actors) {
        for (int i = 0; i < actors.size; i++)
            removeFocusableActor(actors.get(i));
    }

    public Array<Actor> getFocusableActors() {
        return focusableActors;
    }

    /**
     * Sets the currently focused actor. Use this to set the first focused actor after a screen
     * change.
     *
     * Note that this method checks with {@link #isActorFocusable(Actor)}
     * if the given actor can get the focus. If you use a Table layout, it is often needed to
     * call {@link Table#validate()} on the root table for all Actors to layout before calling
     * this method
     *
     * @param actor
     * @return true if the actor is focused now
     */
    public boolean setFocusedActor(Actor actor) {
        if (focusedActor == actor)
            return true;

        if (actor != null && !isActorFocusable(actor))
            return false;

        Actor oldFocused = focusedActor;
        if (oldFocused != null) {
            focusedActor = null;
            onFocusLost(oldFocused, actor);
        }

        // focusedActor may be changed by onFocusLost->touchCancel, in that case don't reset
        if (focusedActor == null) {
            focusedActor = actor;
            if (focusedActor != null) {
                onFocusGained(focusedActor, oldFocused);
            }
            return true;
        }

        return false;
    }

    public Actor getFocusedActor() {
        return focusedActor;
    }

    /**
     * fired when focus was set, override for your own special actions
     * @param focusedActor the actor that gained focus
     */
    protected void onFocusGained(Actor focusedActor, Actor oldFocused) {
        //String old = oldFocused == null ? "" : " old focus: " + ((TextButton) oldFocused).getText().toString();
        //Gdx.app.error(getClass().getSimpleName(), "focused gained: " + ((TextButton) focusedActor).getText().toString() + " pressed: " + isPressed + old);
        if (sendMouseOverEvents) {
            fireEventOnActor(focusedActor, InputEvent.Type.enter, -1, oldFocused);
        }
        setKeyboardFocus(focusedActor);
        setScrollFocus(focusedActor);
    }

    /**
     * fired when focus was lost, override for your own special actions
     * @param focusedActor the actor that lost focus
     */
    protected void onFocusLost(Actor focusedActor, Actor newFocused) {
        //String newFocus = newFocused == null ? "" : " new focus: " + ((TextButton) newFocused).getText().toString();
        //Gdx.app.error(getClass().getSimpleName(), "focused lost: " + ((TextButton) focusedActor).getText().toString() + " pressed: "  + isPressed + newFocus);
        if (isPressed) {
            cancelTouchFocus();
            isPressed = false;
        }

        if (sendMouseOverEvents) {
            fireEventOnActor(focusedActor, InputEvent.Type.exit, -1, newFocused);
        }
    }

    /**
     * checks if the given actor is focusable: in the list of focusable actors, visible, touchable, and on the stage
     * @param actor
     * @return true if focusable
     */
    protected boolean isActorFocusable(Actor actor) {
        if (!focusableActors.contains(actor, true))
            return false;

        if (!actor.isVisible())
            return false;

        if (!actor.isTouchable() && !(actor instanceof IControllerActable))
            return false;

        if (actor.getStage() != this)
            return false;

        return true;
    }

    protected boolean isActorHittable(Actor actor) {
        Vector2 center = actor.localToStageCoordinates(new Vector2(actor.getWidth() / 2, actor.getHeight() / 2));
        Actor hitActor = hit(center.x, center.y, true);
        return hitActor != null && (hitActor.isDescendantOf(actor));
    }

    protected boolean isActorInViewportArea(Actor actor) {
        Vector2 leftBottom = actor.localToStageCoordinates(new Vector2(0, 0));
        Vector2 rightTop = actor.localToStageCoordinates(new Vector2(actor.getWidth(), actor.getHeight()));
        return !(leftBottom.x > getWidth() || leftBottom.y > getHeight() || rightTop.x < 0 || rightTop.y < 0);
    }

    /**
     * called on focusedActor when default action key is pressed or on escapeActor when escape key is pressed
     * @param actor focusedActor or escapeActor
     * @return true if the event was handled
     */
    protected boolean triggerActionOnActor(boolean keyDown, Actor actor) {
        if (actor instanceof IControllerActable) {
            if (keyDown) {
                return ((IControllerActable) actor).onControllerDefaultKeyDown();
            } else {
                return ((IControllerActable) actor).onControllerDefaultKeyUp();
            }
        }
        return fireEventOnActor(actor, keyDown ? InputEvent.Type.touchDown : InputEvent.Type.touchUp, 0, null);
    }

    @Override
    public boolean keyDown(int keyCode) {
        //no actors on stage
        if (!getRoot().hasChildren()) {
            return false;
        }

        boolean handled = false;

        switch (keyCode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                handled = moveFocusByDirection(MoveFocusDirection.north);
                break;
            case Input.Keys.S:
            case Input.Keys.DOWN:
                handled = moveFocusByDirection(MoveFocusDirection.south);
                break;
            case Input.Keys.A:
            case Input.Keys.LEFT:
                handled = moveFocusByDirection(MoveFocusDirection.west);
                break;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                handled = moveFocusByDirection(MoveFocusDirection.east);
                break;
            case Input.Keys.SPACE:
            case Input.Keys.ENTER:
                handled = triggerActionOnActor(true, focusedActor);
                break;
            case Input.Keys.BACK:
            case Input.Keys.BACKSPACE:
            case Input.Keys.ESCAPE:
                if (escapeActor != null) {
                    handled = triggerActionOnActor(true, escapeActor);
                    isPressed = handled;
                } else {
                    //close window
                    if (!GameScreen.isPaused()) {
                        Actor p = getRoot().getChildren().first();
                        if (p instanceof VisWindow) {
                            //not paused + window is present = a window is open. ignore debug tool
                            if (!((VisWindow) p).getTitleLabel().getText().contains("ECS Explorer")) {
                                ((VisWindow) p).fadeOut();
                                handled = true;
                            }
                        }
                    }
                }
                break;
        }

        //Gdx.app.error(getClass().getSimpleName(), "keydown: [" + keyCode + "]" +  Input.Keys.toString(keyCode) + " handled: " + handled + " pressed: " + isPressed);

        if (!handled)
            handled = super.keyDown(keyCode);

        return handled;
    }

    @Override
    public boolean keyUp(int keyCode) {
        //no actors on stage
        if (!getRoot().hasChildren()) {
            return false;
        }

        boolean handled;
        if (isDefaultActionKeyCode(keyCode)) {
            isPressed = false;
            handled = triggerActionOnActor(false, focusedActor);
        } else if (isEscapeActionKeyCode(keyCode) && escapeActor != null) {
            handled = triggerActionOnActor(false, escapeActor);
            isPressed = handled;
            //Gdx.app.error(getClass().getSimpleName(), "keyUp escape action. pressed: " + isPressed);
        } else {
            handled = false;
        }

        if (!handled)
            handled = super.keyUp(keyCode);

        return handled;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        //no actors on stage
        if (!getRoot().hasChildren()) {
            return false;
        }

        if (isFocusOnTouchdown()) {
            screenToStageCoordinates(controllerTempCoords.set(screenX, screenY));
            Actor target = hit(controllerTempCoords.x, controllerTempCoords.y, true);
            if (target != null) {
                if (isActorFocusable(target)) {
                    setFocusedActor(target);
                } else {
                    for (Actor actor : getFocusableActors()) {
                        if (target.isDescendantOf(actor)) {
                            setFocusedActor(actor);
                        }
                    }
                }
            }
        }
        return super.touchDown(screenX, screenY, pointer, button);
    }

    /**
     * returns true if the given keyCode is a DefaultAction key/button. You can override this.
     */
    public boolean isDefaultActionKeyCode(int keyCode) {
        switch (keyCode) {
            case Input.Keys.ENTER:
            case Input.Keys.SPACE:
                return true;
            default:
                return false;
        }
    }

    /**
     * returns true if the given keyCode is a EscapeAction key/button. You can override this.
     */
    public boolean isEscapeActionKeyCode(int keyCode) {
        switch (keyCode) {
            case Input.Keys.BACK:
            case Input.Keys.BACKSPACE:
            case Input.Keys.ESCAPE:
                return true;
            default:
                return false;
        }
    }

    protected boolean fireEventOnActor(Actor actor, InputEvent.Type type, int pointer, Actor related) {
        if (actor == null || !isActorFocusable(actor) || !isActorHittable(actor))
            return false;

        InputEvent event = Pools.obtain(InputEvent.class);
        event.setType(type);
        event.setStage(this);
        event.setRelatedActor(related);
        event.setPointer(pointer);
        event.setButton(pointer);
        event.setStageX(0);
        event.setStageY(0);

        actor.fire(event);

        boolean handled = event.isHandled();
        Pools.free(event);
        return handled;
    }

    /**
     * moves the focus to next or previous focusable actor in list
     * @param next true if this should go to next, or false to go back
     * @return if an action was performed
     */
    protected boolean moveFocusByList(boolean next) {
        if (focusedActor == null)
            return false;

        Actor nextActor = null;

        // check if currently focused actor wants to determine the next Actor
        if (focusedActor instanceof IControllerManageFocus) {
            nextActor = ((IControllerManageFocus) focusedActor).getNextFocusableActor(next);
        }

        // otherwise, we use the next or previous Actor in our list
        if (nextActor == null) {
            int index = focusableActors.indexOf(focusedActor, true);

            while (!next && index > 0 && nextActor == null) {
                if (isActorFocusable(focusableActors.get(index - 1))) {
                    nextActor = focusableActors.get(index - 1);
                }
                index--;
            }

            while (next && index < focusableActors.size - 1 && nextActor == null) {
                if (isActorFocusable(focusableActors.get(index + 1))) {
                    nextActor = focusableActors.get(index + 1);
                }
                index++;
            }
        }

        if (nextActor != null) {
            return setFocusedActor(nextActor);
        }

        return false;
    }

    /**
     * moves the focus in the given direction, if applicable
     *
     * @param direction
     * @return true if an action was perforemd
     */
    protected boolean moveFocusByDirection(MoveFocusDirection direction) {
        if (focusedActor == null) {
            //if no actor was initialized as focused, autofocus on first focusable.
            focusFirstAvailableActor();
            return false;
        }
        Actor nextFocusedActor = null;

        // check if currently focused actor wants to determine the next Actor
        if (focusedActor instanceof IControllerManageFocus) {
            nextFocusedActor = ((IControllerManageFocus) focusedActor).getNextFocusableActor(direction);
        }

        if (nextFocusedActor == null) {
            nextFocusedActor = findNearestFocusableNeighbour(direction);
        }

        // check for scrollable parents
        boolean hasScrolled = checkForScrollable(direction, nextFocusedActor);
        if (!hasScrolled && nextFocusedActor != null) {
            return setFocusedActor(nextFocusedActor);
        }
        return hasScrolled;
    }

    private boolean focusFirstAvailableActor() {
        if (focusableActors.size == 0) return false;

        for (Actor actor : focusableActors) {
            //skip disabled items
            if ((actor instanceof TextButton) && ((TextButton)actor).isDisabled()) continue;
            //may have to skip more types here in future as add controller support to other actors
            //eg: VisTextField
            if (setFocusedActor(actor)) {
                return true;
            }
        }
        return false;
    }

    private Actor findNearestFocusableNeighbour(MoveFocusDirection direction) {
        Vector2 focusedPosition = focusedActor.localToStageCoordinates(new Vector2(
                direction == MoveFocusDirection.east ? focusedActor.getWidth() : direction == MoveFocusDirection.west ? 0 : focusedActor.getWidth() / 2,
                direction == MoveFocusDirection.north ? focusedActor.getHeight() : direction == MoveFocusDirection.south ? 0 : focusedActor.getHeight() / 2));

        // check distance of every focusable actor in the direction
        Actor nearestInDirection = null;
        float distance = Float.MAX_VALUE;

        for (int i = 0; i < focusableActors.size; i++) {
            Actor currentActor = focusableActors.get(i);
            if (currentActor == focusedActor) continue;
            if (!isActorFocusable(currentActor) || !isActorHittable(currentActor) ||!isActorInViewportArea(currentActor)) continue;
            if ((currentActor instanceof TextButton) && ((TextButton)currentActor).isDisabled()) continue;

            Vector2 currentActorPos = currentActor.localToStageCoordinates(new Vector2(
                    direction == MoveFocusDirection.west ? currentActor.getWidth() : direction == MoveFocusDirection.east ? 0 : currentActor.getWidth() / 2,
                    direction == MoveFocusDirection.south ? currentActor.getHeight() : direction == MoveFocusDirection.south ? 0 : currentActor.getHeight() / 2));

            boolean isInDirection = (direction == MoveFocusDirection.south && currentActorPos.y <= focusedPosition.y)
                    || (direction == MoveFocusDirection.north && currentActorPos.y >= focusedPosition.y)
                    || (direction == MoveFocusDirection.west && currentActorPos.x <= focusedPosition.x)
                    || (direction == MoveFocusDirection.east && currentActorPos.x >= focusedPosition.x);

            if (isInDirection) {
                float currentDist = calcNeighbourDistance(direction, focusedPosition, currentActorPos);
                if (currentDist < distance) {
                    nearestInDirection = currentActor;
                    distance = currentDist;
                    //Gdx.app.error(getClass().getSimpleName(), ((TextButton)currentActor).getText() + ": " + direction + " " + distance + " pressed:" + isPressed);
                }
            }
        }
        return nearestInDirection;
    }

    /**
     * @param direction
     * @param focusedPosition
     * @param currentActorPos
     * @return
     */
    protected float calcNeighbourDistance(MoveFocusDirection direction, Vector2 focusedPosition, Vector2 currentActorPos) {
        float horizontalDist = currentActorPos.x - focusedPosition.x;
        float verticalDist = currentActorPos.y - focusedPosition.y;

        // emphasize the direct direction
        if (direction == MoveFocusDirection.south || direction == MoveFocusDirection.north) {
            horizontalDist = horizontalDist * directionEmphFactor;
        } else {
            verticalDist = verticalDist * directionEmphFactor;
        }
        return horizontalDist * horizontalDist + verticalDist * verticalDist;
    }

    public float getDirectionEmphFactor() {
        return directionEmphFactor;
    }

    /**
     * use this to emphasize a direction when navigating by arrows
     *
     * @param directionEmphFactor - the more you give here, the more actors in direct direction are favored
     */
    public void setDirectionEmphFactor(float directionEmphFactor) {
        this.directionEmphFactor = directionEmphFactor;
    }

    /**
     * checks if a IControllerScrollable actor should scroll instead of focusing nearest neighbour
     * @param direction
     * @param nearestInDirection may be null
     * @return true if a scroll was performed
     */
    protected boolean checkForScrollable(MoveFocusDirection direction, Actor nearestInDirection) {
        Actor findScrollable = focusedActor;
        boolean didScroll = false;

        while (!didScroll) {
            if (findScrollable == null)
                return false;

            if (findScrollable instanceof IControllerScrollable) {
                // we found a scrollable... but if the nearest actor in direction is also child of this one, it
                // shouldn't scroll. In this case, we leave the while loop because every parent will also be a parent
                // of the nearest
                if (nearestInDirection != null) {
                    Actor nearestNeighboursParent = nearestInDirection;
                    while (nearestNeighboursParent != null && nearestNeighboursParent != findScrollable)
                        nearestNeighboursParent = nearestNeighboursParent.getParent();

                    if (nearestNeighboursParent == findScrollable)
                        return false;
                }

                // ok - now we try to scroll!
                didScroll = ((IControllerScrollable) findScrollable).onControllerScroll(direction);
            }
            findScrollable = findScrollable.getParent();
        }
        return didScroll;
    }

    @Override
    public void unfocusAll() {
        super.unfocusAll();
        setFocusedActor(null);
    }

    @Override
    public void unfocus(Actor actor) {
        super.unfocus(actor);
        if (actor == focusedActor)
            setFocusedActor(null);
    }

    //region controller
    @Override
    public void connected(Controller controller) {
        logControllerStatus(controller, "Connected", true);
    }

    @Override
    public void disconnected(Controller controller) {
        logControllerStatus(controller, "Disconnected", false);
    }

    private void logControllerStatus(Controller controller, String status, boolean connected) {
        boolean canVibrate = false;
        if (connected) {
            canVibrate = controller.canVibrate();
        }
        String info = status + ": '" + controller.getName() + "' id:[" + controller.getUniqueId() + "] index:" + controller.getPlayerIndex()
                + " power:" + controller.getPowerLevel() + " vibrate:" + canVibrate;
        Gdx.app.log(getClass().getSimpleName(), info);
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        //Gdx.app.error(getClass().getSimpleName(), controller.getName() + ": " + buttonCode);
        if (buttonCode == controller.getMapping().buttonDpadUp) {
            //return moveFocusByDirection(MoveFocusDirection.north);
            return keyDown(Input.Keys.UP);
        }
        if (buttonCode == controller.getMapping().buttonDpadDown) {
            //return moveFocusByDirection(MoveFocusDirection.south);
            return keyDown(Input.Keys.DOWN);
        }
        if (buttonCode == controller.getMapping().buttonDpadLeft) {
            //return moveFocusByDirection(MoveFocusDirection.west);
            return keyDown(Input.Keys.LEFT);
        }
        if (buttonCode == controller.getMapping().buttonDpadRight) {
            //return moveFocusByDirection(MoveFocusDirection.east);
            return keyDown(Input.Keys.RIGHT);
        }
        if (buttonCode == controller.getMapping().buttonA) {
            return keyDown(mapButtonA);
        }
        if (buttonCode == controller.getMapping().buttonB) {
            return keyDown(mapButtonB);
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (buttonCode == controller.getMapping().buttonA) {
            return keyUp(mapButtonA);
        }
        if (buttonCode == controller.getMapping().buttonB) {
            return keyUp(mapButtonB);
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
        }
        if (axisCode == controller.getMapping().axisLeftX) {
            leftStickHorAxis = value;
        }
        float threshold = 0.6f;
        if (Math.abs(leftStickVertAxis) > threshold && lastFocusTimer.tryEvent()) {
            if (leftStickVertAxis > 0) {
                //return moveFocusByDirection(MoveFocusDirection.south);
                return keyDown(Input.Keys.DOWN);
            } else {
                //return moveFocusByDirection(MoveFocusDirection.north);
                return keyDown(Input.Keys.UP);
            }
        }
        if (Math.abs(leftStickHorAxis) > threshold && lastFocusTimer.tryEvent()) {
            if (leftStickHorAxis > 0) {
                //return moveFocusByDirection(MoveFocusDirection.east);
                return keyDown(Input.Keys.RIGHT);
            } else {
                //return moveFocusByDirection(MoveFocusDirection.west);
                return keyDown(Input.Keys.LEFT);
            }
        }
        return false;
    }
    //endregion

    public enum MoveFocusDirection { west, north, east, south }

    /**
     * Groups that should be able to scroll via Controller buttons or keyboard keys can implement this interface.
     * {@link ControllerMenuStage} will then send callbacks to this Actor before setting the focus to an Actor outside.
     * <p>
     * See {link ControllerScrollPane} for an example implementation. You can also use this for Lists or other custom
     * Actors.
     * <p>
     * Created by Benjamin Schulte on 05.02.2018.
     */
    public interface IControllerScrollable {
        /**
         * Called when a key event was done and scroll should be performed
         * @param direction the direction to onControllerScroll to
         * @return if event was handled
         */
        boolean onControllerScroll(MoveFocusDirection direction);
    }

    /**
     * You can implement this interface on an Actor if you have to adjust the way
     * {@link ControllerMenuStage} determines the next focused Actor.
     */
    public interface IControllerManageFocus {
        /**
         * called when the next focusable actor in a direction needs to be determined
         * @param direction
         * @return Actor that should be focused, or null for automatic determination
         */
        Actor getNextFocusableActor(MoveFocusDirection direction);

        /**
         * called when the next focusable actor in a direction needs to be determined
         * @param next true if next Actor in list should be focused, or false if previos Actor should be focused
         * @return Actor that should be focused, or null for automatic determination
         */
        Actor getNextFocusableActor(boolean next);
    }

    /**
     * This interface can be used for Actors where the controller default button should trigger an other action than firing
     * a touch down or touch up event. See {@link ControllerMenuStage#fireEventOnActor(Actor, InputEvent.Type, int, Actor)}
     * and {link ControllerSlider} for an example
     * Created by Benjamin Schulte on 08.02.2018.
     */
    public interface IControllerActable {
        /**
         * called when the controller/keyboard default key was pressed down
         * @return if event was handled
         */
        boolean onControllerDefaultKeyDown();

        /**
         * called when the controller/keyboard default key went up
         * @return if event was handled
         */
        boolean onControllerDefaultKeyUp();
    }

}
