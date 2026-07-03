package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.math.Vector3;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class DesktopInputSystem extends EntitySystem implements InputProcessor {
    
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);
    private ImmutableArray<Entity> players;
    private final Vector3 tempVec = new Vector3();
    private boolean controllerHasFocus = false;
    
    private final long doubleTapTime = 300;
    private final SimpleTimer doubleTapLeft = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapRight = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapLeftTrigger = new SimpleTimer(200);
    private int tapCounterLeft = 0;
    private int tapCounterRight = 0;
    private int tapCounterLeftTrigger = 0;

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
        GameScreen.getInputMultiplexer().addProcessor(this);
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        GameScreen.getInputMultiplexer().removeProcessor(this);
    }
    
    @Override
    public void update(float delta) {
        if (getControllerHasFocus())
            return;
        
        facePosition(Gdx.input.getX(), Gdx.input.getY());
    }
    
    private boolean playerControls(int keycode, boolean keyDown) {
        setFocusToDesktop();
        if (keycode == Input.Keys.ESCAPE) {
            GameMenu menu = getEngine().getSystem(HUDSystem.class).getGameMenu();
            if (!menu.isVisible()) {
                menu.show();
                return true;
            }
        }

        if (players.size() == 0) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() != 0) {
                PlayerSpawnSystem spawnSystem = getEngine().getSystem(PlayerSpawnSystem.class);
                if (spawnSystem != null) {
                    return spawnSystem.pan(respawnEntities.first());
                }
            }
            return false;
        }
    
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        
        //movement
        control.movementMultiplier = 1; // set multiplier to full power because a key switch is on or off
        if (keycode == keyCFG.forward) {
            control.moveForward = keyDown;
            if (keyDown) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }

        if (keycode == keyCFG.right) {
            control.moveRight = keyDown;
            //check double tap
            if (!keyDown) {
                tapCounterRight++;
                if (tapCounterRight == 1) {
                    //single tap
                    doubleTapRight.reset();
                } else {
                    //double tap
                    tapCounterRight = 0;
                    BarrelRollComponent barrelRoll = Mappers.barrelRoll.get(player);
                    if (barrelRoll != null) {
                        BarrelRollSystem.dodgeRight(player, barrelRoll);
                    }
                }
            }
            //timeout
            if (doubleTapRight.canDoEvent()) {
                tapCounterRight = 0;
            }
            if (keyDown) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }

        if (keycode == keyCFG.left) {
            control.moveLeft = keyDown;
            //check double tap
            if (!keyDown) {
                tapCounterLeft++;
                if (tapCounterLeft == 1) {
                    //single tap
                    doubleTapLeft.reset();
                } else {
                    //double tap
                    tapCounterLeft = 0;
                    BarrelRollComponent barrelRoll = Mappers.barrelRoll.get(player);
                    if (barrelRoll != null) {
                        BarrelRollSystem.dodgeLeft(player, barrelRoll);
                    }
                }
            }
            //timeout
            if (doubleTapLeft.canDoEvent()) {
                tapCounterLeft = 0;
            }
            if (keyDown) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }

        if (keycode == keyCFG.back) {
            control.moveBack = keyDown;
            if (keyDown) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }

        if (keycode == keyCFG.boost) {
            control.boost = keyDown;
            if (keyDown) {
                //cancel hyperdrive if active
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }

        if (keycode == keyCFG.changeVehicle) {//replace with general interact?
            control.changeVehicle = keyDown;
            return true;
        }

        if (keycode == keyCFG.interact) {
            control.interact = keyDown;
            return true;
        }

        if (keycode == keyCFG.switchWeapon) {
            control.swapWeapon = keyDown;
            return true;
        }

        if (keycode == keyCFG.dash) {
            DashComponent dash = Mappers.dash.get(player);
            if (dash != null) {
                dash.activate = keyDown;
                return true;
            }
        }

        if (keycode == keyCFG.activateShield) {
            ShieldComponent shield = Mappers.shield.get(player);
            if (shield != null) {
                shield.activate = keyDown;
                return true;
            }
        }

        if (keycode == keyCFG.activateHyperDrive) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(player);
            if (hyperDrive != null) {
                hyperDrive.activate = keyDown;
                return true;
            }
        }
        return false;
    }
    
    private boolean facePosition(int x, int y) {
        if (players.size() == 0)
            return false;
    
        TransformComponent transform = Mappers.transform.get(players.first());
        ControllableComponent control = Mappers.controllable.get(players.first());
        
        Vector3 playerPos = GameScreen.cam.project(tempVec.set(transform.pos, 0));
        float angle = MyMath.angleTo(playerPos.x, playerPos.y, x, Gdx.graphics.getHeight() - y);
        control.angleTargetFace = angle;
        
        return true;
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        //todo: needs to override middle clicks setZoomToDefault
        //  if I scroll while reseting, set to nearest zoom level
        if (amountY <= 0) {
            getEngine().getSystem(CameraSystem.class).zoomIn();
        } else {
            getEngine().getSystem(CameraSystem.class).zoomOut();
        }
        setFocusToDesktop();
        return false;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        return playerControls(keycode, true);
    }
    
    @Override
    public boolean keyUp(int keycode) {
        return playerControls(keycode, false);
    }
    
    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        setFocusToDesktop();

        if (players.size() == 0) {
            return false;
        }
        Entity player = players.first();
        
        //primary attack
        if (button == Input.Buttons.LEFT) {
            ControllableComponent control = Mappers.controllable.get(player);
            control.attack = true;
    
            CannonComponent cannon = Mappers.cannon.get(player);
            if (cannon != null) {
                cannon.multiplier = 1;
            }

            LaserComponent laser = Mappers.laser.get(player);
            if (laser != null) {
                laser.state = LaserComponent.State.on;
            }


            TractorBeamComponent tractorBeam = Mappers.tractor.get(player);
            if (tractorBeam != null) {
                //check double tap
                tapCounterLeftTrigger++;
                if (tapCounterLeftTrigger == 1) {
                    //single tap
                    doubleTapLeftTrigger.reset();
                    tractorBeam.state = tractorBeam.mode;
                } else {
                    //double tap
                    tapCounterLeftTrigger = 0;
                    //doubleTapLeftTrigger.reset();
                    //toggle between push and pull
                    tractorBeam.state = tractorBeam.mode == TractorBeamComponent.State.push ? TractorBeamComponent.State.pull : TractorBeamComponent.State.push;
                }
                tractorBeam.mode = tractorBeam.state; //always update state
            }
            //timeout
            if (doubleTapLeftTrigger.canDoEvent()) {
                tapCounterLeftTrigger = 0;
            }

            return true;
        }
        
        //reset cam
        if (button == Input.Buttons.MIDDLE) {
            GameScreen.resetRotation();
            getEngine().getSystem(CameraSystem.class).autoZoom(player);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (players.size() == 0) return false;

        if (button == Input.Buttons.LEFT) {
            Entity player = players.first();
            ControllableComponent control = Mappers.controllable.get(player);
            control.attack = false;

            LaserComponent laser = Mappers.laser.get(player);
            if (laser != null) {
                laser.state = LaserComponent.State.off;
            }

            TractorBeamComponent tractorBeam = Mappers.tractor.get(player);
            if (tractorBeam != null) {
                tractorBeam.state = TractorBeamComponent.State.off;
            }

            //timeout
            if (doubleTapLeftTrigger.canDoEvent()) {
                tapCounterLeftTrigger = 0;
            }

            return true;
        }
        return false;
    }

    //@Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return facePosition(screenX, screenY);
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        setFocusToDesktop();
        return false;
    }

    public boolean getControllerHasFocus() {
        return controllerHasFocus;
    }

    public void setFocusToController() {
        if (!controllerHasFocus) {
            //instead of removing cursor, perhaps could set cursor to a set distance away from player in stick direction?
            Gdx.app.debug(getClass().getSimpleName(), "input focus set to controller");
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
        }
        controllerHasFocus = true;
    }

    public void setFocusToDesktop() {
        if (controllerHasFocus) {
            Gdx.app.debug(getClass().getSimpleName(), "input focus set to desktop");
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            Gdx.graphics.setCursor(GameScreen.cursor);
        }
        controllerHasFocus = false;
    }

}
