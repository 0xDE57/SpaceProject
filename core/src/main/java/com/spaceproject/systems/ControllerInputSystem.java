package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.MathUtils;
import com.spaceproject.components.*;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

public class ControllerInputSystem extends EntitySystem implements ControllerListener {
    
    private final boolean debugInput = false;

    private boolean vibrate = true;
    private float leftStickHorAxis;
    private float leftStickVertAxis;
    private float rightStickHorAxis;
    private float rightStickVertAxis;
    private float l2, r2;
    private final float stickDeadzone = 0.5f;
    private final float triggerDeadZone = 0.1f;
    private final float engageThreshold = 0.70f;
    private final SimpleTimer cameraDelayTimer = new SimpleTimer(400);
    
    private final long doubleTapTime = 300;
    private final SimpleTimer doubleTapLeft = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapRight = new SimpleTimer(doubleTapTime);
    private final SimpleTimer doubleTapLeftTrigger = new SimpleTimer(200);
    private int tapCounterLeft = 0;
    private int tapCounterRight = 0;
    private int tapCounterLeftTrigger = 0;

    private ImmutableArray<Entity> players;
    
    @Override
    public void addedToEngine(Engine engine) {
        Controllers.addListener(this);
        
        for (Controller controller : Controllers.getControllers()) {
            logControllerStatus(controller, "Detected", true);
        }
        
        players = engine.getEntitiesFor(Family.all(ControlFocusComponent.class, ControllableComponent.class).get());
    }
    
    @Override
    public void removedFromEngine(Engine engine) {
        Controllers.removeListener(this);
    }
    
    @Override
    public void connected(Controller controller) {
        logControllerStatus(controller, "Connected", true);
    }
    
    @Override
    public void disconnected(Controller controller) {
        logControllerStatus(controller, "Disconnected", false);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        if (GameScreen.isPaused()) return;

        if (Math.abs(rightStickVertAxis) >= stickDeadzone) {
            if (cameraDelayTimer.tryEvent()) {
                if (rightStickVertAxis >= 0) {
                    getEngine().getSystem(CameraSystem.class).zoomOut();
                } else {
                    getEngine().getSystem(CameraSystem.class).zoomIn();
                }
            }
        }
    }
    
    private boolean playerControls(Controller controller, int buttonCode, boolean buttonDown) {
        if (debugInput) {
            logInput(controller, buttonCode, buttonDown);
        }

        getEngine().getSystem(DesktopInputSystem.class).setFocusToController();

        //toggle menu
        if (buttonCode == controller.getMapping().buttonStart) {
            if (buttonDown) {
                GameMenu menu = getEngine().getSystem(HUDSystem.class).getGameMenu();
                if (!menu.isVisible()) {
                    menu.show();
                } else {
                    menu.close();
                }
            }
            return true;
        }

        if (GameScreen.isPaused()) return false;

        if (players.size() == 0) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() != 0) {
                PlayerSpawnSystem spawnSystem = getEngine().getSystem(PlayerSpawnSystem.class);
                if (spawnSystem != null) {
                    spawnSystem.pan(respawnEntities.first());
                }
            }
            return false;
        }
        
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        
        if (buttonCode == controller.getMapping().buttonA) {
            control.boost = buttonDown;
            
            if (control.boost) {
                control.movementMultiplier = 1;

                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            
            DashComponent dash = Mappers.dash.get(player);
            if (dash != null) {
                dash.activate = buttonDown;
            }
            return true;
        }
        
        if (buttonCode == controller.getMapping().buttonB) {
            HyperDriveComponent hyperDrive = Mappers.hyper.get(player);
            if (hyperDrive != null) {
                hyperDrive.activate = buttonDown;
                return true;
            }
        }
        
        if (buttonCode == controller.getMapping().buttonY) {
            control.changeVehicle = buttonDown;
            return true;
        }
        
        if (buttonCode == controller.getMapping().buttonX) {
            control.moveBack = buttonDown;
            return true;
        }
    
        if (buttonCode == controller.getMapping().buttonDpadRight) {
            control.swapWeapon = buttonDown;
            return true;
        }
        
        if (buttonCode == controller.getMapping().buttonDpadDown) {
            control.interact = buttonDown;
            return true;
        }
        
        if (buttonCode == controller.getMapping().buttonR1) {
            control.movementMultiplier = 1;
            control.moveRight = buttonDown;
            
            //check double tap
            if (!buttonDown) {
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
            if (buttonDown) {
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }
        
        if (buttonCode == controller.getMapping().buttonL1) {
            control.movementMultiplier = 1;
            control.moveLeft = buttonDown;
    
            //check double tap
            if (!buttonDown) {
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
            if (buttonDown) {
                HyperDriveComponent hyper = Mappers.hyper.get(player);
                if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
                    HyperDriveSystem.disengageHyperDrive(player, hyper);
                }
            }
            return true;
        }
        
        //reset cam
        if ((buttonCode == controller.getMapping().buttonRightStick) && buttonDown) {
            GameScreen.resetRotation();
            getEngine().getSystem(CameraSystem.class).autoZoom(player);
            return true;
        }

        return false;
    }
    
    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        return playerControls(controller, buttonCode, true);
    }
    
    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return playerControls(controller, buttonCode, false);
    }

    boolean r2Hold = false;
    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (debugInput) {
            logAxis(controller, axisCode, value);
        }

        if (GameScreen.isPaused()) return false;
        if (players.size() == 0) return false;
        
        //update axis
        if (axisCode == controller.getMapping().axisLeftX) {
            leftStickHorAxis = value;
        }
        if (axisCode == controller.getMapping().axisLeftY) {
            leftStickVertAxis = value;
        }
        if (axisCode == controller.getMapping().axisRightX) {
            rightStickHorAxis = value;
        }
        if (axisCode == controller.getMapping().axisRightY) {
            rightStickVertAxis = value;
        }
        //controller.getMapping().buttonL2 = ?
        if (axisCode == 4 /*controller.getMapping().buttonL2*/) {
            l2 = value;
        }
        if (axisCode == 5 /* controller.getMapping().buttonR2*/) {
            r2 = value;
        }
        
        Entity player = players.first();
        ControllableComponent control = Mappers.controllable.get(player);
        DesktopInputSystem desktopInput = getEngine().getSystem(DesktopInputSystem.class);
        if (r2 > triggerDeadZone) {
            desktopInput.setFocusToController();
            control.attack = true;
            
            CannonComponent cannon = Mappers.cannon.get(player);
            if (cannon != null) {
                cannon.multiplier = r2;
            }

            LaserComponent laser = Mappers.laser.get(player);
            if (laser != null) {
                laser.state = LaserComponent.State.on;
            }

            TractorBeamComponent tractorBeam = Mappers.tractor.get(player);
            if (tractorBeam != null) {
                if (!r2Hold) {
                    r2Hold = true;
                    //check double tap
                    tapCounterLeftTrigger++;
                    if (tapCounterLeftTrigger == 1) {
                        //single tap
                        doubleTapLeftTrigger.reset();
                        tractorBeam.state = tractorBeam.mode;
                    } else {
                        //double tap
                        tapCounterLeftTrigger = 0;
                        //toggle between push and pull
                        tractorBeam.state = tractorBeam.mode == TractorBeamComponent.State.push ? TractorBeamComponent.State.pull : TractorBeamComponent.State.push;
                    }
                    tractorBeam.mode = tractorBeam.state; //always update state
                }
            }
            //timeout
            if (doubleTapLeftTrigger.canDoEvent()) {
                tapCounterLeftTrigger = 0;
            }
        } else {
            //todo: bug, stick drift and minor inputs seem to be interfering with mouse input
            if (desktopInput.getControllerHasFocus()) {
                r2Hold = false;

                control.attack = false;

                LaserComponent laser = Mappers.laser.get(player);
                if (laser != null) {
                    laser.state = LaserComponent.State.off;
                }

                TractorBeamComponent tractorBeam = Mappers.tractor.get(player);
                if (tractorBeam != null) {
                    tractorBeam.state = TractorBeamComponent.State.off;
                }
            }
        }
    
        ShieldComponent shield = Mappers.shield.get(player);
        if (shield != null) {
            if (l2 > triggerDeadZone) {
                shield.activate = true;
                desktopInput.setFocusToController();
            } else {
                shield.activate = false;
            }
        }
        
        float dist = Math.abs(MyMath.distance(0, 0, leftStickHorAxis, leftStickVertAxis));
        if (dist >= stickDeadzone) {
            //face stick direction
            control.angleTargetFace = MyMath.angleTo(0, 0, leftStickHorAxis, -leftStickVertAxis);
            control.movementMultiplier = MathUtils.clamp(dist, 0, 1);
            //control.moveForward = control.movementMultiplier > engageThreshold;
            
            //notify desktop system that controller has current focus
            desktopInput.setFocusToController();
        } else {
            if (!control.boost && desktopInput.getControllerHasFocus()) {
                control.moveForward = false;
            }
        }

        float distRight = Math.abs(MyMath.distance(0, 0, rightStickHorAxis, rightStickVertAxis));
        if (distRight >= stickDeadzone) {
            //notify desktop system that controller has current focus
            desktopInput.setFocusToController();
        }

        return false;
    }
    
    private void logControllerStatus(Controller controller, String status, boolean connected) {
        //todo: bug in jampad?
        // canVibrate() <-- null when disconnecting controller
        /* Exception in thread "main" java.lang.NullPointerException
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.canVibrate(JamepadController.java:173)
	        at com.spaceproject.systems.ControllerInputSystem.logController(ControllerInputSystem.java:73)
	        at com.spaceproject.systems.ControllerInputSystem.disconnected(ControllerInputSystem.java:69)
	        at com.badlogic.gdx.controllers.desktop.support.CompositeControllerListener.disconnected(CompositeControllerListener.java:21)
	        at com.badlogic.gdx.controllers.desktop.support.CompositeControllerListener.disconnected(CompositeControllerListener.java:21)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.setDisconnected(JamepadController.java:90)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.getButton(JamepadController.java:55)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.updateButtonsState(JamepadController.java:137)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadController.update(JamepadController.java:105)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadControllerMonitor.update(JamepadControllerMonitor.java:52)
	        at com.badlogic.gdx.controllers.desktop.support.JamepadControllerMonitor.run(JamepadControllerMonitor.java:26)
	        at com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop(Lwjgl3Application.java:208)
	        at com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>(Lwjgl3Application.java:166)
	        at com.spaceproject.desktop.DesktopLauncher.main(DesktopLauncher.java:16)
        * */
        
        boolean canVibrate = false;
        if (connected) {
            canVibrate = controller.canVibrate();
        }
        
        String info = status + ": '" + controller.getName()
                + "' id:[" + controller.getUniqueId()
                + "] index:" + controller.getPlayerIndex()
                + " power:" + controller.getPowerLevel()
                + " vibrate:" + canVibrate;
        Gdx.app.log(getClass().getSimpleName(), info);
    }
    
    private void logInput(Controller controller, int buttonCode, boolean buttonDown) {
        String info = controller.getName() + ": ["+ buttonCode + "] " + buttonDown;
        Gdx.app.debug(getClass().getSimpleName(), info);
    }
    
    private void logAxis(Controller controller, int axisCode, float value) {
        String info = controller.getName() + ": ["+ axisCode + "] " + value;
        Gdx.app.debug(getClass().getSimpleName(), info);
    }
    
    public void vibrate(int durationMS, float strength) {
        if (!vibrate) return;

        Controller controller = Controllers.getCurrent();
        if (controller == null || !controller.canVibrate()) {
            return;
        }
        //strength range: 0.0 - 1.0
        controller.startVibration(durationMS, strength);
    }

    public void setVibrateEnable(boolean checked) {
        vibrate = checked;
    }

}
