package com.spaceproject.ui.menu;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.math.Physics;
import com.spaceproject.systems.DesktopInputSystem;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import java.lang.reflect.Field;
import java.util.Collection;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;


public class SpaceStationMenu {

    //todo: component levels may be better than per attribute?
    // examples: 3 or 5 levels?
    // - Hyperdrive [0] - no levels, only speed!
    //      - cooldown?
    // - Active Shield [0,?] - no levels, unless we add the overheat system, maybe cooling rate could be upgrade
    //     modifier for shield dash? (speaking of sheild dash, (revamp boost to apply impulse? double tap + hold?)
    //     - cooldownRate
    //      -
    //     heat += damage * multiplier;
    //      if (heat > threshold); break shield
    // - Passive Regen Shield [1,2,3,4] - rechargeable shield - some forgiveness
    //      25, 50, 75, 100
    // - Laser [1,2,3,?], per level increase power and distance,
    //      - determine max (sane) distance and power levels, and divide by desired number of levels
    //      - damage
    //      - range
    //      - maxReflections? (handled per ray or leave global max?)
    // - Cannon [1,2,3,4,5,6,7,8] 4 is round! (if starting at 80), or 8 for more sub divisions
    //   -vel = 80 max = 240. 40-80 = 160
    //   160 / 4 = 40
    //   8 = 20;
    //   -damage = 5 max = ?
    //      8 * 5 = 40
    // .
    // also projectiles should be adding on velocity damage, not just base damage....
    // will need to play around with asteroids hardness levels and find sensible balance...
    // doesn't have to be round, it just looks nice...
    //   5 - ?
    //      - MODIFIER component: explode, follow, bounce, bifurcate, vampire(health steal)
    // - Health [1,2,3,4,5,6,7,8]
    // 200 - 1000 = 800
    // +100 each time!
    // .
    // health could be hull with multiple properties?
    //  Hull
    //      - health
    //      - Passive Regen (instead of component?)
    //      - heat resistance (stars / lasers)
    //      - impact resistance (eg: -5% less damage from physical impulse)
    // .
    // . Do asteroids need these properties? no its overkill.
    // . should they be individual components? eg: HeatResistanceComponent
    // .
    // .
    // Health (1/9) or (0/8)?
    // Cannon (1/8)
    // Active Shield
    // could have little stat bars for levels (ASCII for now UI later?)
    //  ▱▱▱▱▱▰▰▰▰▰
    // Progress Bar seems limited. best to make a custom component?
    // shape drawer with rounded caps would probably be nice
    // ship level: once levels are implemented we can give the ship a total level = all upgrades combined


    // todo:
    // Health (base) level 1 = 200
    // 2 = 300
    // 3 = 400
    // 4 = 500
    // 5 = 600
    // 6 = 700
    // 7 = 800
    // 8 = 900
    // 9 = 1000
    // i dont want to store the levels in the component itself
    // so how about an upgrade component?

    //maxLevel;
    //buffPerLevelmerhep
    //increaseByAmount()  eg: +100 //fixed
    //increaseByPercent() eg: += 5% ( value += value * 0.005) //stackable or not?
    //how about decrease
    //decreaseByFixed()
    //decreaseByPercent()

    static final JsonValue config = new JsonReader().parse(Gdx.files.internal("assets/config/UpgradeConfig.json"));

    public static Actor SpaceStationMenu(ControllerMenuStage stage, Entity player, Engine engine) {
        //enable color markup
        VisUI.getSkin().get("default-font", BitmapFont.class).getData().markupEnabled = true;
        String titleColor = "#0090d2";
        String colorItem = "#14B1FFFF";
        String colorCredits = "#77FF14";
        String colorControl = "#cb6701";

        boolean controllerHasFocus = engine.getSystem(DesktopInputSystem.class).getControllerHasFocus();
        KeyConfig keyConfig = SpaceProject.configManager.getConfig(KeyConfig.class);
        String hyperControl = controllerHasFocus ? "B" : Input.Keys.toString(keyConfig.activateHyperDrive);
        String shieldControl = controllerHasFocus ? "LT" : Input.Keys.toString(keyConfig.activateShield);
        String laserControl = controllerHasFocus ? "RT" : "L-Click";

        String shieldUpgrade = "Active Shield".toUpperCase();
        String hyperdriveUpgrade = "Hyper-Drive".toUpperCase();
        String laserUpgrade = "Laser".toUpperCase();
        String tractorUpgrade = "Tractor Beam".toUpperCase();
        String hyperdriveDescription = String.format("[%s]%s[]\nenable deep-space exploration.\n\nHold [%s][%s][] to activate.",
                colorItem, hyperdriveUpgrade, colorControl, hyperControl);
        String shieldDescription = String.format("[%s]%s[]\nprotect your ship from damage.\n\nHold [%s][%s][] to activate.",
                colorItem, shieldUpgrade, colorControl, shieldControl);
        String laserDescription = String.format("[%s]%s[]\nprecision tool.\n\nHold [%s][%s][] to activate.",
                colorItem, laserUpgrade, colorControl, laserControl);
        String tractorBeamDescription = String.format("[%s]%s[]\npush or pull objects.\n\nHold [%s][%s][] to activate.\nDouble Tap to toggle between PUSH & PULL",
                colorItem, tractorUpgrade, colorControl, laserControl);


        ScrollableTextArea text = new ScrollableTextArea("");
        text.removeListener(text.getDefaultInputListener());
        VisTable creditsTable = new VisTable();
        VisLabel creditsText = new VisLabel("CREDITS: ");
        creditsTable.add(creditsText);
        CargoComponent cargo = Mappers.cargo.get(player);
        VisLabel creditsValue = new VisLabel("[" + colorCredits + "]" + cargo.credits);
        //creditsValue.addAction(forever(sequence(color(Color.RED,0.2f), color(Color.BLUE, 0.2f), delay(0.5f))));
        creditsTable.add(creditsValue);


        VisTable buttonTable = new VisTable();

        UpgradeComponent upgradeComp = player.getComponent(UpgradeComponent.class);
        if (upgradeComp == null) {
            upgradeComp = new UpgradeComponent();
            player.add(upgradeComp);
            Gdx.app.log(SpaceStationMenu.class.getSimpleName(), "init upgrade component");
        }

        JsonValue upgrades = config.get("upgrades");
        for (JsonValue upgrade : upgrades) {
            String componentClass = upgrade.getString("component");
            String upgradeName = upgrade.getString("name");
            String upgradeDescriptionTemplate = upgrade.getString("description");
            int upgradeCost = upgrade.getInt("cost");
            JsonValue levels = upgrade.get("levels");

            VisTextButton upgradeButton = new VisTextButton("[" + colorItem + "]" + upgradeName);

            //assumes level 1 is defined for existing components, empty level is solution to avoid duplicate definition. will likely have to refactor out of EntityConfig...
            int maxLevel = levels.size;
            int currentLevel = 0;

            Component existing = findComponent(player.getComponents(), componentClass);
            if (existing == null) {
                existing = findComponent(player.getComponent(VehicleComponent.class).tools.values(), componentClass);
            }
            if (existing != null) {
                if (upgradeComp.map.containsKey(componentClass)) {
                    currentLevel = upgradeComp.map.get(componentClass);
                    if (currentLevel >= maxLevel) {
                        upgradeButton.setDisabled(true);
                    }
                } else {
                    //init to level 1
                    upgradeComp.map.put(componentClass, 1);
                    currentLevel = 1;
                }
            }

            upgradeButton.getLabel().setAlignment(Align.left);
            VisLabel levelLabel = new VisLabel("  " + currentLevel + "/" + maxLevel + " ");
            upgradeButton.add(levelLabel).width(45).align(Align.right).grow();
            VisLabel costLabel = new VisLabel(" [" + colorCredits + "]" + upgradeCost);
            costLabel.setAlignment(Align.right);
            upgradeButton.add(costLabel).width(60).align(Align.right);
            upgradeButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (cargo.credits < upgradeCost) {
                        //todo: error soundfx if not enough moneys
                        Gdx.app.debug(getClass().getSimpleName(), "insufficient credits for: " + upgradeName);
                        creditsValue.addAction(sequence(color(Color.RED), color(Color.WHITE, 0.5f)));
                        return;
                    }

                    VehicleComponent vehicleComponent = player.getComponent(VehicleComponent.class);
                    //get current upgrade level map
                    UpgradeComponent upgradeComponent = player.getComponent(UpgradeComponent.class);
                    boolean isNew = false;
                    int level = -1;
                    if (upgradeComponent.map.containsKey(componentClass)) {
                        level = upgradeComponent.map.get(componentClass);
                        //Gdx.app.log(getClass().getSimpleName(), componentClass + ": " + level);
                    } else {
                        //init new component
                        try {
                            Class<? extends Component> newComponent = (Class<? extends Component>) Class.forName("com.spaceproject.components." + componentClass);
                            player.add(newComponent.newInstance()); //kinda hacky? where if it's a tool, it is removed from player and placed in vehicle tools...
                            level = 0;
                            isNew = true;
                            //Gdx.app.log(getClass().getSimpleName(), "Add new " + componentClass);
                        } catch (Exception e) {
                            Gdx.app.error(getClass().getSimpleName(), "could not create component:" + componentClass, e);
                        }
                    }

                    //find target component to upgrade; first on player, then in tools
                    Component componentToUpgrade = findComponent(player.getComponents(), componentClass);
                    if (componentToUpgrade == null) {
                        componentToUpgrade = findComponent(player.getComponent(VehicleComponent.class).tools.values(), componentClass);
                    }

                    //update component properties from values in json config
                    JsonValue nextLevel = levels.get(level);
                    int lvl = nextLevel.getInt("level");
                    for (JsonValue field : nextLevel.get("properties")) {
                        try {
                            assert componentToUpgrade != null;
                            Field f = componentToUpgrade.getClass().getField(field.name);
                            Class<?> fieldType = f.getType();
                            if (fieldType == int.class) {
                                f.set(componentToUpgrade, field.asInt());
                            } else if (fieldType == float.class) {
                                f.set(componentToUpgrade, field.asFloat());
                            } else if (fieldType == double.class) {
                                f.set(componentToUpgrade, field.asDouble());
                            } else if (fieldType == boolean.class) {
                                f.set(componentToUpgrade, field.asBoolean());
                            } else if (fieldType == SimpleTimer.class) {
                                //Gdx.app.error(getClass().getSimpleName(), "unhandled type: [" + fieldType + "] " + field);
                                SimpleTimer newTimer = new SimpleTimer(field.asLong());
                                f.set(componentToUpgrade, newTimer);
                            } else {
                                Gdx.app.error(getClass().getSimpleName(), "unhandled type: [" + fieldType + "] " + field);
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            Gdx.app.error(getClass().getSimpleName(), "upgrade failed: ", e);
                        }
                    }

                    if (isNew) {
                        //handle special cases where calculations need to be done
                        if (componentToUpgrade instanceof ShieldComponent) {
                            Rectangle dimensions = vehicleComponent.dimensions;
                            ((ShieldComponent) componentToUpgrade).maxRadius = Math.max(dimensions.getWidth(), dimensions.getHeight());
                        } else if (componentToUpgrade instanceof LaserComponent) {
                            LaserComponent laser = (LaserComponent) componentToUpgrade;
                            laser.frequency = (float) Physics.wavelengthToFrequency(laser.wavelength);
                            int[] rgb = Physics.wavelengthToRGB(laser.wavelength, 1);
                            laser.color = new Color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f, 1);
                            //store in tools, remove from player
                            vehicleComponent.tools.put(VehicleComponent.Tool.laser.ordinal(), laser);
                            player.remove(componentToUpgrade.getClass());
                        } else if (componentToUpgrade instanceof TractorBeamComponent) {
                            //store in tools, remove from player
                            vehicleComponent.tools.put(VehicleComponent.Tool.tractor.ordinal(), componentToUpgrade);
                            player.remove(componentToUpgrade.getClass());
                        }
                    }
                    upgradeComponent.map.put(componentClass, lvl);
                    //Gdx.app.log(getClass().getSimpleName(), "Upgraded " + componentClass + " to level: " + lvl + "!");

                    //purchase
                    cargo.credits -= upgradeCost;
                    //update UI
                    creditsValue.setText(cargo.credits);
                    creditsValue.addAction(sequence(color(Color.valueOf(colorItem)), color(Color.valueOf(colorCredits), 1f)));
                    levelLabel.setText(" [" + lvl + "/" + maxLevel + "] ");
                    if (lvl == maxLevel) {
                        upgradeButton.setDisabled(true);
                    }
                }
            });
            upgradeButton.addListener(new FocusListener() {
                @Override
                public boolean handle(Event event) {
                    //String upgradeDescription = String.format(upgradeDescriptionTemplate, colorItem, upgradeName, colorControl, hyperControl);
                    text.setText(upgradeName);
                    return super.handle(event);
                }
            });
            buttonTable.add(upgradeButton).fillX().row();
        }

        //debug
        VisTextButton buttonDebugGiveMoney = new VisTextButton("[#ff0000]DEBUG: []ADD CREDITS");
        buttonDebugGiveMoney.getLabel().setAlignment(Align.left);
        buttonDebugGiveMoney.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                cargo.credits += 999999;
                creditsValue.setText(cargo.credits);
                creditsValue.addAction(sequence(color(Color.valueOf(colorItem)), color(Color.valueOf(colorCredits), 1f)));

                //SaveUtil.saveToJson(0, player, false);
            }
        });
        buttonDebugGiveMoney.addListener(new FocusListener() {
            @Override
            public boolean handle(Event event) {
                text.setText("cheater...");
                return super.handle(event);
            }
        });
        buttonTable.add(buttonDebugGiveMoney).fillX().row();
        //buttonTable.debugAll();

        VisTable descTable = new VisTable();
        text.setReadOnly(true);
        descTable.add(text.createCompatibleScrollPane()).height(190).width(200).row();

        VisWindow window = new VisWindow("[" + titleColor + "]STATION DELTA");
        window.getTitleLabel().setAlignment(Align.center);
        window.addCloseButton();
        window.add(new Separator()).fillX().colspan(2).pad(3).row();
        window.add(buttonTable).pad(10);
        window.add(descTable).pad(10).row();
        window.add(new Separator()).fillX().colspan(2).row();
        window.add(creditsTable).colspan(2).pad(10);

        for (Actor button : buttonTable.getChildren()) {
            stage.addFocusableActor(button);
        }

        window.pack();
        stage.addActor(window);
        window.setPosition(Gdx.graphics.getWidth() / 3f, getHeight(window), Align.right);
        stage.setFocusedActor(buttonTable.getChildren().first());

        return window;
    }

    private static Component findComponent(Collection<Component> components, String componentClass) {
        for (Component component : components) {
            if (component.getClass().getSimpleName().equals(componentClass)) {
                return component;
            }
        }
        return null;
    }

    private static Component findComponent(ImmutableArray<Component> components, String componentClass) {
        for (Component component : components) {
            if (component.getClass().getSimpleName().equals(componentClass)) {
                return component;
            }
        }
        return null;
    }

    public static float getHeight(VisWindow window) {
        return (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3)) - 49 - window.getHeight() / 2;
    }

}
