package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.spaceproject.components.ControlFocusComponent;
import com.spaceproject.components.ControllableComponent;
import com.spaceproject.ui.custom.TouchButtonRectangle;
import com.spaceproject.ui.custom.TouchButtonRound;
import com.spaceproject.ui.custom.TouchJoyStick;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;

/*TODO: test multiple screen sizes
 * https://developer.android.com/guide/practices/screens_support.html
 */

@Deprecated
public class MobileInputSystem extends EntitySystem {
    
    //rendering
    private Matrix4 projectionMatrix = new Matrix4();
    private ShapeRenderer shape = new ShapeRenderer();
    
    Color white = new Color(1f, 1f, 1f, 0.5f);
    Color blue = new Color(0.5f, 0.5f, 1f, 0.7f);
    
    TouchButtonRound btnShoot = new TouchButtonRound(Gdx.graphics.getWidth() - 80, 100, 70, white, blue);
    TouchButtonRound btnVehicle = new TouchButtonRound(Gdx.graphics.getWidth() - 80, 300, 50, white, blue);
    TouchButtonRectangle btnLand = new TouchButtonRectangle(Gdx.graphics.getWidth() / 2 - 60, Gdx.graphics.getHeight() - 60 - 20, 120, 60, white, blue);
    TouchButtonRectangle btnMap = new TouchButtonRectangle(20, Gdx.graphics.getHeight() - 60 - 20, 120, 60, white, blue);
    TouchButtonRectangle btnMenu = new TouchButtonRectangle(Gdx.graphics.getWidth() - 120 - 20, Gdx.graphics.getHeight() - 60 - 20, 120, 60, white, blue);
    TouchJoyStick joyMovement = new TouchJoyStick(230, 230, 200, white, blue);
    
    private ImmutableArray<Entity> players;
    
    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    public void update(float delta) {
        HUDSystem hud = getEngine().getSystem(HUDSystem.class);
        
        if (btnMenu.isJustTouched()) {
            if (hud != null) {
                GameMenu gameMenu = hud.getGameMenu();
                if (gameMenu.isVisible()) {
                    gameMenu.close();
                } else {
                    gameMenu.show();
                }
            }
        }
        if (hud.getGameMenu().isVisible())
            return;
        
        if (btnMap.isJustTouched()) {
            if (hud != null)
                hud.getMiniMap().cycleMapState();
        }
        
        
        //player controls
        if (players.size() == 0) return;
        Entity player = players.first();
        
        ControllableComponent control = Mappers.controllable.get(player);
        control.attack = btnShoot.isTouched();
        control.changeVehicle = btnVehicle.isJustTouched();
        control.interact = btnLand.isTouched();
        btnLand.hidden = !control.canTransition;
        
        if (joyMovement.isTouched()) {
            
            // face finger
            control.angleTargetFace = joyMovement.getAngle();
            
            //apply thrust
            control.movementMultiplier = joyMovement.getPowerRatio();
            
            // if finger is close to center of joystick, apply breaks
            if (joyMovement.getPowerRatio() < 0.25f) {
                // breaks
                control.moveForward = false;
                control.moveBack = true;
            } else {
                // move
                control.moveForward = true;
                control.moveBack = false;
            }
        } else {
            control.moveForward = false;
            control.moveBack = false;
        }
        
    }
    
    /**
     * Draw on-screen buttons.
     */
    public void drawControls() {
        //set projection matrix so things render using correct coordinates
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        
        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        joyMovement.render(shape);
        
        shape.begin(ShapeType.Filled);
        
        //draw shoot button
        btnShoot.render(shape);
        
        //draw vehicle button
        //TODO: test if player is in vehicle or can get in a vehicle;
        btnVehicle.render(shape);
        
        btnLand.render(shape);
        btnMap.render(shape);
        btnMenu.render(shape);
        
        shape.end();
        
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    
}
