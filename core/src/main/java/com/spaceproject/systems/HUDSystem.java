package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.*;
import com.kotcrab.vis.ui.widget.VisWindow;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.KeyConfig;
import com.spaceproject.config.UIConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.screens.MyScreenAdapter;
import com.spaceproject.ui.ControllerMenuStage;
import com.spaceproject.ui.map.MapState;
import com.spaceproject.ui.map.MiniMap;
import com.spaceproject.ui.menu.GameMenu;
import com.spaceproject.ui.menu.SpaceStationMenu;
import com.spaceproject.utility.IRequireGameContext;
import com.spaceproject.utility.IScreenResizeListener;
import com.spaceproject.utility.Mappers;
import com.spaceproject.utility.SimpleTimer;

import java.lang.StringBuilder;
import java.util.Iterator;
import java.util.Map;


public class HUDSystem extends EntitySystem implements IRequireGameContext, IScreenResizeListener, Disposable {

    public static class DamageText implements Pool.Poolable {
        public float damage;
        public float x, y;
        public long timestamp;
        public boolean project;

        public void init(float x, float y, float damage, long lastHitTime, boolean project) {
            this.x = x;
            this.y = y;
            this.damage = damage;
            this.timestamp = lastHitTime;
            this.project = project;
        }

        @Override
        public void reset() {
        }

    }

    private static final int poolSize = 400;
    private static final Array<DamageText> activeNumbers = new Array<>(false, poolSize); //don't care about order so we can avoid a System.arraycopy()
    private static final Pool<DamageText> numbersPool = Pools.get(DamageText.class, poolSize);
    public static boolean showDamageNumbers = true;
    public static int damageTime = 750;
    public static int activePeak;
    private final StringBuilder infoString = new StringBuilder();

    class CreditsMarker {//can this fit into the same damage pool? it really doesn't need to...
        final int value;
        Vector2 location;
        SimpleTimer animTimer;
        final Color color;

        CreditsMarker(int value, Vector2 pos, Color color) {
            this.value = value;
            Vector3 screenPos = cam.project(new Vector3(pos.cpy(), 0));//copy
            location = new Vector2(screenPos.x, screenPos.y);//new
            animTimer = new SimpleTimer(1000, true);//new -> float timestamp
            this.color = color;
        }
    }

    private final Array<CreditsMarker> markers;

    private final UIConfig uiCFG = SpaceProject.configManager.getConfig(UIConfig.class);
    private final KeyConfig keyCFG = SpaceProject.configManager.getConfig(KeyConfig.class);

    private GameMenu gameMenu;
    private VisWindow stationMenu;
    private boolean interactHold;

    //rendering
    private final OrthographicCamera cam;
    private final Matrix4 projectionMatrix;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font, subFont, inventoryFont;
    private final GlyphLayout layout;
    private final Vector3 tempProj = new Vector3();
    private final Color cacheColor = new Color();

    //entity storage
    private ImmutableArray<Entity> mapableEntities;
    private ImmutableArray<Entity> bodies;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> killableEntities;

    private final Vector3 topLeft = new Vector3();
    private final Vector3 bottomRight = new Vector3();
    private final Vector3 screenPos = new Vector3();
    private final Vector2 focusedPos = new Vector2();
    private float focusedDist;

    private MiniMap miniMap;

    private static boolean drawHud = true;
    private boolean drawEdgeMap = true;

    enum SpecialState {
        off, docked, landing, launching, destroyed;
    }

    private SpecialState messageState = SpecialState.off;

    private float statusAnim = 0;
    private float damageAnim = 0;

    private final int radius = 32;

    public HUDSystem() {
        cam = MyScreenAdapter.cam;
        shape = MyScreenAdapter.shape;
        batch = MyScreenAdapter.batch;
        projectionMatrix = new Matrix4();
        layout = new GlyphLayout();

        miniMap = new MiniMap();
        markers = new Array<>(false, 16);

        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 26;
        parameter.borderColor = Color.BLACK;
        parameter.borderWidth = 3;
        font = FontLoader.createFont(FontLoader.fontPressStart, parameter);

        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 20;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontLoader.createFont(FontLoader.fontPressStart, parameter2);

        FreeTypeFontGenerator.FreeTypeFontParameter inventoryParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        inventoryParam.size = 12;
        inventoryParam.borderColor = Color.BLACK;
        inventoryParam.borderWidth = 1;
        inventoryFont = FontLoader.createFont(FontLoader.fontPressStart, inventoryParam);

        GameScreen.getStage().addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                //todo alternatively?
                //if (E ) and at station
                //    stationmenu.show
                if (gameMenu.switchTabForKey(keycode)) {
                    if (!gameMenu.isVisible()) {
                        gameMenu.show();
                    }
                    return true;
                }
                return super.keyDown(event, keycode);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.MIDDLE) {
                    if (miniMap.getMapContainer().contains(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY())) {
                        if (miniMap.getState() != MapState.off) {
                            miniMap.resetMapScale();
                            return true;
                        }
                    }
                }
                super.touchDown(event, x, y, pointer, button);
                return gameMenu.isVisible();
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (getMiniMap().scrolled(amountX, amountY)) {
                    return true;
                }
                return super.scrolled(event, x, y, amountX, amountY);
            }
        });
    }

    @Override
    public void initContext(GameScreen game) {
        gameMenu = new GameMenu(game, false);
    }

    @Override
    public void addedToEngine(Engine engine) {
        mapableEntities = engine.getEntitiesFor(Family.all(MapComponent.class, TransformComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).get());
        killableEntities = engine.getEntitiesFor(Family.all(HealthComponent.class, TransformComponent.class)
                .exclude(ControlFocusComponent.class, AsteroidComponent.class).get());
        bodies = engine.getEntitiesFor(Family.one(PlanetComponent.class, StarComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        checkInput();

        if (drawHud) {
            statusAnim += 4f * deltaTime;
            drawHUD(deltaTime);

            if (miniMap.getState() != MapState.off) {
                Entity p = players.size() > 0 ? players.first() : null;
                miniMap.drawMiniMap(shape, batch, p, mapableEntities);
            }
        }

        //TODO: temporary fix. engine system priority....
        //always draw even if hud off
        MobileInputSystem mobileUI = getEngine().getSystem(MobileInputSystem.class);
        if (mobileUI != null)
            mobileUI.drawControls();
    }

    public boolean isDraw() {
        return drawHud;
    }

    private void drawHUD(float deltaTime) {
        //set projection matrix so things render using correct coordinates
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shape.setProjectionMatrix(projectionMatrix);
        batch.setProjectionMatrix(projectionMatrix);

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeType.Filled);
        Entity player = null;
        if (players.size() > 0) {
            player = players.first();
        }

        //draw background for state message
        if (messageState != SpecialState.off) {
            drawMessageBacking(player);
        }
        if (messageState != SpecialState.docked) {
            //auto close shop menu
            if (stationMenu != null) {
                stationMenu.fadeOut();
                stationMenu = null;
            }
        }
        //skip drawing some UI elements during land and take off
        CameraSystem camSys = getEngine().getSystem(CameraSystem.class);
        if (messageState != SpecialState.launching && messageState != SpecialState.landing) {
            drawZoomLevelIndicator(camSys);
            drawPlayerStatus(player, camSys, deltaTime);

            if (drawEdgeMap) {
                drawEdgeMap();
            }

            drawHealthBars();
        }
        shape.end();

        //draw cargo UI ring
        if ((messageState != SpecialState.launching) && (messageState != SpecialState.landing) && (messageState != SpecialState.destroyed)) {
            shape.begin(ShapeType.Line);
            int barX = Gdx.graphics.getWidth() / 2 - uiCFG.playerHPBarWidth / 2;
            shape.circle(barX - radius - 10, uiCFG.playerHPBarY, animRadius);
            int barXRight = Gdx.graphics.getWidth() / 2 + uiCFG.playerHPBarWidth / 2;
            shape.circle(barXRight + radius + 10, uiCFG.playerHPBarY, radius);
            shape.end();
        }

        batch.begin();
        //skip drawing some UI elements during land and take off
        if (messageState != SpecialState.launching && messageState != SpecialState.landing) {
            if (camSys.isMaxZoomLevel()) {
                for (Entity body : bodies) {
                    drawBodyInfoOnFocus(body);
                }
            } else {
                Entity focused = getEngine().getSystem(GridRenderSystem.class).closestFacing;
                drawBodyInfoOnFocus(focused);
            }

            if (focusedDist > 0) {
                inventoryFont.setColor(1, 1, 1, 1);
                layout.setText(inventoryFont, " " + (int) focusedDist);
                inventoryFont.draw(batch, layout, focusedPos.x, focusedPos.y + 1);
            }

            drawStatusInfo(player, uiCFG.playerHPBarWidth, uiCFG.playerHPBarHeight, uiCFG.playerHPBarY + 5);
            drawInventory(player, 20, 30);

            drawCreditMarkers(deltaTime);
        }

        drawDamageText();

        //draw special state: hyper or landing / launching
        drawSpecialStateMessage(player);


        //drawHint("stars are hot");
        //drawHint("an object in motion remains in motion");
        //todo: #5
        float warningDist = 200000;
        warningDist = warningDist * warningDist;
        if (cam.position.dst2(0, 0, 0) > warningDist) {
            drawHint("warning: broken physics ahead " + MyMath.formatVector3as2(cam.position, 1));
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBodyInfoOnFocus(Entity entity) {
        if (entity == null) return;

        PlanetComponent planet = Mappers.planet.get(entity);
        if (planet != null) {
            TransformComponent transform = Mappers.transform.get(entity);
            tempProj.set(transform.pos.x, transform.pos.y, 0);
            MyScreenAdapter.cam.project(tempProj);
            float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
            inventoryFont.setColor(1, 1, 1, alpha);
            layout.setText(inventoryFont, "<planet name>");
            float offset = layout.height * 1.5f;
            inventoryFont.draw(batch, layout, tempProj.x - layout.width * 0.5f, tempProj.y + offset);
            layout.setText(inventoryFont, "atmosphere: <unknown>");
            inventoryFont.draw(batch, layout, tempProj.x - layout.width * 0.5f, tempProj.y);
            layout.setText(inventoryFont, "size: " + planet.mapSize);
            inventoryFont.draw(batch, layout, tempProj.x - layout.width * 0.5f, tempProj.y - offset);//replace with mass?
            return;
        }
        StarComponent star = Mappers.star.get(entity);
        if (star != null) {
            TransformComponent transform = Mappers.transform.get(entity);
            tempProj.set(transform.pos.x, transform.pos.y, 0);
            MyScreenAdapter.cam.project(tempProj);
            float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
            inventoryFont.setColor(1, 1, 1, alpha);
            layout.setText(inventoryFont, "<star type>" /*+ star.StellarClass*/);
            float offset = layout.height * 1.5f;
            inventoryFont.draw(batch, layout, tempProj.x - layout.width * 0.5f, tempProj.y + offset);
            layout.setText(inventoryFont, "temperature: " + (int) star.temperature + "k");
            inventoryFont.draw(batch, layout, tempProj.x - layout.width * 0.5f, tempProj.y);
        }
    }

    private void drawZoomLevelIndicator(CameraSystem camSys) {
        if (MathUtils.isEqual(cam.zoom, CameraSystem.getZoomForLevel(camSys.getZoomLevel()), 0.1f)
                && camSys.zoomChangeTimer.canDoEvent()) return;

        int width = uiCFG.playerHPBarWidth * 2;
        int x = Gdx.graphics.getWidth() / 2 - width / 2;
        int y = Gdx.graphics.getHeight() - 40;
        int thickness = 2;

        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rectLine(x, y, x + width, y, thickness); //base line

        //levels
        int sliderHeight = 30;
        float maxZoom = CameraSystem.getZoomForLevel(camSys.getMaxZoomLevel());
        for (byte l = 0; l <= camSys.getMaxZoomLevel(); l++) {
            float r = CameraSystem.getZoomForLevel(l) / maxZoom;
            shape.rect(x + (r * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);
        }

        //target
        shape.setColor(Color.WHITE);
        float rr = CameraSystem.getZoomForLevel(camSys.getZoomLevel()) / maxZoom;
        shape.rect(x + (rr * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);

        //current
        shape.setColor(uiCFG.uiBaseColor);
        float ratio = cam.zoom / maxZoom;
        shape.rect(x + (ratio * width), y - (sliderHeight * 0.5f), thickness, sliderHeight);
    }

    private void drawMessageBacking(Entity player) {
        int offset = 50;
        int messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3)) - offset;
        float height = 40 + offset;
        shape.rect(0, messageHeight, Gdx.graphics.getWidth(), height, Color.CLEAR, Color.CLEAR, Color.DARK_GRAY, Color.DARK_GRAY);
        Color color = messageState == SpecialState.destroyed ? Color.RED : uiCFG.uiBaseColor;
        shape.setColor(color);
        shape.rectLine(0, messageHeight + height, Gdx.graphics.getWidth(), messageHeight + height, 3);
        shape.rectLine(0, messageHeight, Gdx.graphics.getWidth(), messageHeight, 1);

        int timerHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3) - 6);
        float halfWidth = Gdx.graphics.getWidth() * 0.5f;

        //draw respawn line
        if (messageState == SpecialState.destroyed) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() > 0) {
                RespawnComponent respawn = Mappers.respawn.get(respawnEntities.first());
                if (respawn.spawn == RespawnComponent.AnimState.pause || respawn.spawn == RespawnComponent.AnimState.pan) {
                    float ratio = 1 - respawn.timeout.ratio();
                    shape.setColor(Color.WHITE);
                    shape.rectLine(halfWidth - (halfWidth * ratio), timerHeight, halfWidth + (ratio * halfWidth), timerHeight, 2);
                }
            }
        }

        if (messageState == SpecialState.docked) {
            if (player != null) {
                ControllableComponent control = Mappers.controllable.get(player);
                if (control.interact) {
                    if (!interactHold) {
                        if (stationMenu == null) {
                            ControllerMenuStage controllerMenuStage = GameScreen.getStage();
                            stationMenu = (VisWindow) SpaceStationMenu.SpaceStationMenu(controllerMenuStage, player, getEngine());
                        } else {
                            stationMenu.remove();
                        }
                    }
                    interactHold = true;
                } else {
                    interactHold = false;
                }
            }
        }

        if (stationMenu != null && stationMenu.getParent() == null) {
            //System.out.println(stationMenu.isVisible() + ", " + stationMenu.getParent());
            stationMenu = null;
        }
    }

    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleHUD)) {//todo: kill key toggle move to debug
            drawHud = !drawHud;
            Gdx.app.log(getClass().getSimpleName(), "HUD: " + drawHud);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleEdgeMap)) {//kill or move to UI option
            drawEdgeMap = !drawEdgeMap;
            Gdx.app.log(getClass().getSimpleName(), "Edge mapState: " + drawEdgeMap);
        }
        if (Gdx.input.isKeyJustPressed(keyCFG.toggleSpaceMap)) {
            miniMap.cycleMapState();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) { //kill or move to UI option
            miniMap.cycleMiniMapPosition();
        }
        if (Gdx.app.getInput().isKeyJustPressed(Input.Keys.Y)) {//kill or move to debug menu
            if (players.size() > 0) {
                CargoComponent cargo = Mappers.cargo.get(players.first());
                if (cargo != null) {
                    int amount = 1000;
                    for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
                        int id = resource.getId();
                        int currentQuantity = 0;
                        if (cargo.inventory.containsKey(id)) {
                            currentQuantity = cargo.inventory.get(id);
                        }
                        cargo.inventory.put(id, currentQuantity + amount);
                    }
                    cargo.lastCollectTime = GameScreen.getGameTimeCurrent();
                    Gdx.app.debug(getClass().getSimpleName(), "cheat! add items");
                }
            }
        }
    }

    private void drawHealthBars() {
        //bar dimensions
        int barLength = uiCFG.entityHPbarLength;
        int barWidth = uiCFG.entityHPbarWidth;
        int yOffset = uiCFG.entityHPbarYOffset;

        for (Entity entity : killableEntities) {
            HealthComponent health = Mappers.health.get(entity);
            //ignore full health
            if (!uiCFG.renderFullHealth && health.health == health.maxHealth) {
                continue;
            }

            tempProj.set(Mappers.transform.get(entity).pos, 0);
            cam.project(tempProj);

            //background
            shape.setColor(uiCFG.entityHPbarBackground);
            shape.rect(tempProj.x - barLength * 0.5f, tempProj.y + yOffset, barLength, barWidth);

            //health
            float ratio = health.health / health.maxHealth;
            shape.setColor(1 - ratio, ratio, 0, uiCFG.entityHPbarOpacity); //creates color between red and green
            shape.rect(tempProj.x - barLength * 0.5f, tempProj.y + yOffset, barLength * ratio, barWidth);
        }
    }

    //region player status
    private void drawSpecialStateMessage(Entity player) {
        messageState = SpecialState.off;
        if (player == null) {
            messageState = SpecialState.destroyed;
        } else {
            ScreenTransitionComponent trans = Mappers.screenTrans.get(player);
            if (trans != null) {
                if (trans.landStage != null) {
                    messageState = SpecialState.landing;
                }
                if (trans.takeOffStage != null) {
                    messageState = SpecialState.launching;
                }
            }
            ControllableComponent control = Mappers.controllable.get(player);
            if (control != null && !control.activelyControlled) {
                messageState = SpecialState.docked;
            }
        }

        switch (messageState) {
            case docked:
                String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).interact);
                boolean controllerHasFocus = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus();
                if (stationMenu == null) {
                    String input = (controllerHasFocus ? "D-Pad Down" : key).toUpperCase();
                    drawHint("[" + input + "] open shop");
                } else {
                    drawHint("[" + (controllerHasFocus ? "B" : key).toUpperCase() + "] close shop");
                }
                String port = Mappers.docked.get(player).dockID;
                layout.setText(font, "[ DOCKED: port " + port + " ]");
                break;
            case landing:
                drawHint("<planet name goes here>");
                layout.setText(font, "[ LANDING ]");
                break;
            case launching:
                drawHint("entering space");
                layout.setText(font, "[ LAUNCHING ]");
                break;
            case destroyed:
                layout.setText(font, "[ DESTROYED ]");
                break;
            case off:
                return;
        }

        float ratio = 1 + (float) Math.sin(statusAnim);
        if (messageState == SpecialState.destroyed) {
            cacheColor.set(0, 0, 0, 1).lerp(Color.RED, ratio);
        } else {
            cacheColor.set(Color.GOLD).lerp(Color.CYAN, ratio);
        }
        font.setColor(cacheColor);

        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3f) + layout.height);
        font.draw(batch, layout, centerX, messageHeight);

        if (player == null) {
            ImmutableArray<Entity> respawnEntities = getEngine().getEntitiesFor(Family.all(RespawnComponent.class).get());
            if (respawnEntities.size() > 0) {
                RespawnComponent respawn = Mappers.respawn.get(respawnEntities.first());
                drawHint(respawn.reason);
                drawStats(Mappers.stat.get(respawnEntities.first()));
            }
        } else {
            ControllableComponent control = Mappers.controllable.get(player);
            if (control != null && control.canTransition && (Mappers.screenTrans.get(player) == null)) {
                String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).interact);
                String input = (getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? "D-Pad Down" : key).toUpperCase();
                if (GameScreen.inSpace()) {
                    drawHint("press [" + input + "] to land");
                } else {
                    if (Mappers.vehicle.get(player) != null) {
                        drawHint("press [" + input + "] to take off");
                    }
                }
            }
        }
    }

    private void drawHint(String text) {
        float ratio = 1 + (float) Math.sin(statusAnim * 0.1f);
        cacheColor.set(0, 1, 0, 1).lerp(Color.PURPLE, ratio);
        subFont.setColor(cacheColor);
        layout.setText(subFont, text);

        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3f) + layout.height);
        messageHeight -= layout.height * 2;
        subFont.draw(batch, layout, centerX, messageHeight);
    }

    private void drawStats(StatsComponent stats) {
        if (stats == null) return;

        //todo: replace with VisUI table for alignment, because string format spacing does not line up properly (glyph spacing?)
        //todo: add another column for current life vs total
        float centerX = 20;
        int offset = 50;
        float messageHeight = (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 3f)) - offset;
        int h = 1;
        //layout.setText(subFont,  String.format("%-10s %s", MyMath.formatDuration(stats.timeAlive), "time alive"), Color.RED, 0, Align.left, false);
        //subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.kills, "kills"), Color.RED, 0, Align.left, false);
        float height = layout.height * 1.6f;
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.deaths, "deaths"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.shotsFired, "shots fired"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.shotsHit, "shots hit") + " - "
                + MyMath.round((double) stats.shotsHit / stats.shotsFired * 100, 2) + "%", Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.damageDealt, "damage dealt"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.damageTaken, "damage taken"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.resourcesCollected, "resources collected"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.resourcesLost, "resources lost"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);

        layout.setText(subFont, String.format("%-10s %s", stats.profit, "profit"), Color.RED, 0, Align.left, false);
        subFont.draw(batch, layout, centerX, messageHeight - height * h++);
    }

    private void drawPlayerStatus(Entity entity, CameraSystem cam, float deltaTime) {
        if (entity == null) return;

        int barWidth = uiCFG.playerHPBarWidth;
        int barHeight = uiCFG.playerHPBarHeight;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int healthBarY = uiCFG.playerHPBarY;
        int ammoBarY = healthBarY - barHeight - 1;
        int hyperBarY = healthBarY + barHeight + 1;

        drawPlayerVelocity(entity, barX, hyperBarY, barWidth, barHeight);
        drawHyperDriveBar(entity, barX, hyperBarY, barWidth, barHeight);

        if (!GameScreen.isPaused()) {
            if (cam.isMaxZoomLevel()) return;

            if (GameScreen.isHyper()) return;
        }

        drawPlayerHealth(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerShield(entity, barX, healthBarY, barWidth, barHeight);
        drawPlayerAmmoBar(entity, barX, ammoBarY, barWidth, barHeight);

        drawCargoResources(entity, barX, healthBarY, deltaTime);
    }

    float animAngle = 0;
    float animRadius = 0;

    private void drawCargoResources(Entity entity, int barX, int healthBarY, float deltaTime) {
        CargoComponent cargo = Mappers.cargo.get(entity);
        if (cargo == null) return;

        int total = 0;
        for (int value : cargo.inventory.values()) {
            total += value;
        }

        float cargoAnimSpeed = 5;
        animAngle += cargoAnimSpeed * deltaTime;
        if (animAngle > 360) {
            animAngle = 0;
        }
        //todo, just write the total on the cargo? might be nice to see total...
        long animTime = 500;
        long timeSinceCollect = GameScreen.getGameTimeCurrent() - cargo.lastCollectTime;
        float angle = animAngle;//degrees
        animRadius = radius;
        if (timeSinceCollect < animTime) {
            float ra = (float) timeSinceCollect / animTime;
            animRadius += (1 - ra) * 7;
        }
        for (Map.Entry<Integer, Integer> entry : cargo.inventory.entrySet()) {
            int key = entry.getKey();
            ItemComponent.Resource resource = ItemComponent.Resource.getById(key);
            int count = entry.getValue();
            float ratio = (float) count / total;
            float newAngle = ratio * 360;
            shape.setColor(resource.getColor());
            shape.arc(barX - radius - 10, healthBarY, animRadius, angle, newAngle);
            angle += newAngle;
        }
    }

    private void drawPlayerHealth(Entity entity, float x, float y, float width, float height) {
        HealthComponent health = Mappers.health.get(entity);
        if (health == null) return;

        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);

        float ratioHP = health.health / health.maxHealth;
        boolean warning = ratioHP < 0.25f;
        boolean hit = GameScreen.getGameTimeCurrent() - health.lastHitTime < 1000;
        if (hit || warning) {
            if (warning) {
                damageAnim += 10 * Gdx.graphics.getDeltaTime();
                float alpha = (float) Math.abs(Math.sin(damageAnim));
                shape.setColor(1, 0, 0, alpha);
            } else {
                shape.setColor(1, 0, 0, 1);
            }
            //border
            int thickness = 2;
            shape.rectLine(x, y + height, x + width, y + height, thickness);//top
            shape.rectLine(x, y, x + width, y, thickness);//bottom
            shape.rectLine(x, y + height, x, y, thickness);//left
            shape.rectLine(x + width, y + height, x + width, y, thickness);//right
        }
        if (!hit) {
            shape.setColor(1 - ratioHP, ratioHP, 0, uiCFG.entityHPbarOpacity);
        }

        shape.rect(x, y, width * ratioHP, height);
    }

    private void drawPlayerShield(Entity entity, float x, float y, float width, float height) {
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield == null || shield.state == ShieldComponent.State.off) {
            return;
        }

        //if shield not engaged, render bar half height
        float halfHeight = height * 0.5f;
        if (shield.state != ShieldComponent.State.on) {
            height = halfHeight;
            y += halfHeight * 0.5f;
        }

        float ratioShield = shield.radius / shield.maxRadius;
        if (shield.state == ShieldComponent.State.on) {
            long hitTime = 500;
            long timeSinceHit = GameScreen.getGameTimeCurrent() - shield.lastHit;
            if (timeSinceHit < hitTime) {
                float green = (float) timeSinceHit / hitTime;
                shape.setColor(0, 1 - green, green, green);
            } else {
                shape.setColor(shield.heat, 0, 1, 1);
            }
        } else {
            shape.setColor(shield.heat, 0, 1, 0.25f + shield.heat);
        }
        if (shield.heat >= 0.95f) {
            shape.setColor(1, 0, 0, 1);
        }
        shape.rect(x, y, width * ratioShield, height);

        if (shield.state == ShieldComponent.State.on) {
            shape.setColor(Color.WHITE);
            if (shield.heat >= 0.95f) {
                shape.setColor(1, 0, 0, 1);
            }
            int thickness = 2;
            shape.rectLine(x, y + height, x + width, y + height, thickness);//top
            shape.rectLine(x, y, x + width, y, thickness);//bottom
            shape.rectLine(x, y + height, x, y, thickness);//left
            shape.rectLine(x + width, y + height, x + width, y, thickness);//right
        }
    }

    private void drawPlayerAmmoBar(Entity entity, int x, int y, int width, int height) {
        drawCannonHeatBar(entity, x, y, width, height);
        drawGrowCannonAmmoBar(entity, x, y, width, height);
    }

    private void drawGrowCannonAmmoBar(Entity entity, int x, int y, int width, int height) {
        ChargeCannonComponent chargeCannon = Mappers.chargeCannon.get(entity);
        if (chargeCannon == null) return;

        float ratioAmmo = chargeCannon.size / chargeCannon.maxSize;
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
        if (chargeCannon.isCharging) {
            shape.setColor(uiCFG.playerAmmoBarRechargeColor);
            if (chargeCannon.size == chargeCannon.maxSize) {
                shape.setColor(uiCFG.playerAmmoBarColor);
            }
            shape.rect(x, y, width * ratioAmmo, height);
        }
    }

    private void drawCannonHeatBar(Entity entity, int x, int y, int width, int height) {
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon == null) return;

        float heatRatio = cannon.heat;
        shape.setColor(uiCFG.entityHPbarBackground);
        shape.rect(x, y, width, height);
        shape.setColor(uiCFG.playerAmmoBarColor);
        shape.rect(x, y, width * heatRatio, height);
    }

    private void drawPlayerVelocity(Entity entity, int x, int y, int width, int height) {
        shape.setColor(Color.BLACK);

        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
            shape.setColor(control.boost ? uiCFG.engineBoost : uiCFG.engineFire);
        }
        shape.rect(x, y + (height * 0.5f), width, 1);

        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics == null) {
            return;
        }

        float velocity = physics.body.getLinearVelocity().len() / Box2DPhysicsSystem.getVelocityLimit();
        float barRatio = MathUtils.clamp(velocity * width, 0, width);
        float center = (width * 0.5f) - (barRatio * 0.5f);
        shape.rect(x + center, y, barRatio, height);

        int barXRight = Gdx.graphics.getWidth() / 2 + uiCFG.playerHPBarWidth / 2;
        int x1 = barXRight + radius + 10;
        int y1 = uiCFG.playerHPBarY;
        Vector2 vel = MyMath.vector(physics.body.getLinearVelocity().angleRad(), radius).add(x1, y1);
        shape.rectLine(x1, y1, vel.x,  vel.y, 2);
        Vector2 facing = MyMath.vector(physics.body.getAngle(), radius).add(x1, y1);
        shape.setColor(Color.BLACK);
        shape.rectLine(x1, y1, facing.x,  facing.y, 2);
    }

    private void drawHyperDriveBar(Entity entity, int x, int y, int width, int height) {
        HyperDriveComponent hyperDrive = Mappers.hyper.get(entity);
        if (hyperDrive == null) return;

        //if hyper drive not engaged, render bar half width
        if (hyperDrive.state != HyperDriveComponent.State.on) {
            int halfHeight = height / 2;
            height = halfHeight;
            y += halfHeight / 2;
        }

        shape.setColor(Color.WHITE);
        switch (hyperDrive.state) {
            case on:
                shape.rect(x, y, width, height);
                break;
            case charging:
                float charge = MathUtils.clamp(0, hyperDrive.chargeTimer.ratio() * width, width);
                float center = (width * 0.5f) - (charge * 0.5f);
                shape.rect(x + center, y, charge, height);
                break;
            case cooldown:
                float cooldown = MathUtils.clamp(0, (1 - hyperDrive.coolDownTimer.ratio()) * width, width);
                float centerD = (width * 0.5f) - (cooldown * 0.5f);
                shape.rect(x + centerD, y, cooldown, height);
                break;
        }
    }

    Color tempColor;
    private void drawStatusInfo(Entity entity, int barWidth, int barHeight, int healthBarY) {
        if (entity == null) return;

        int controlsX = Gdx.graphics.getWidth() - 10;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            cacheColor.set(Color.GOLD).lerp(Color.CYAN, 1 + (float) Math.sin(statusAnim));
            layout.setText(inventoryFont, "[ HYPER-DRIVE ]", cacheColor, 0, Align.center, false);
            inventoryFont.draw(batch, layout, barX + layout.width - 37, healthBarY + barHeight + layout.height - 1);
            return;
        }
        CameraSystem camSystem = getEngine().getSystem(CameraSystem.class);
        if (camSystem.isMaxZoomLevel()) {
            return;
        }

        //draw velocity
        PhysicsComponent physics = Mappers.physics.get(entity);
        if (physics != null) {
            ControllableComponent control = Mappers.controllable.get(entity);
            if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight || control.boost) {
                tempColor = control.boost ? uiCFG.engineBoost : uiCFG.engineFire;
            } else {
                tempColor = Color.WHITE;
            }
            layout.setText(inventoryFont, "" + MyMath.round(physics.body.getLinearVelocity().len(), 1), tempColor, 0, Align.center, false);
            inventoryFont.draw(batch, layout, barX + barWidth + radius + 10, healthBarY - 2);
        }
        //draw HP
        HealthComponent health = Mappers.health.get(entity);
        if (health != null) {
            float ratio = health.health / health.maxHealth;
            Color color = ratio > 0.66 ? Color.GREEN : ratio > 0.33 ? Color.WHITE : Color.RED;
            if (GameScreen.getGameTimeCurrent() - health.lastHitTime < 1000) {
                color = Color.RED;
            }
            ShieldComponent shield = Mappers.shield.get(entity);
            if (shield != null) {
                //draw shield key
                String input = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).activateShield);
                if (getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus()) {
                    input = "LT";
                }
                layout.setText(inventoryFont, "[" + input + "]", Color.WHITE, 0, Align.right, false);
                inventoryFont.draw(batch, layout, controlsX, healthBarY + layout.height);
                if (shield.state == ShieldComponent.State.on) {
                    color = Color.BLUE;
                }
            }
            //inventoryFont.setColor(color);
            layout.setText(inventoryFont, " " + MyMath.round(health.health, 1), color, 0, Align.center, false);
            inventoryFont.draw(batch, layout, barX + barWidth * 0.5f, healthBarY + layout.height - 2);
        }

        VehicleComponent vehicleComponent = Mappers.vehicle.get(entity);
        if (vehicleComponent != null && !vehicleComponent.tools.isEmpty()) {
            inventoryFont.setColor(Color.WHITE);
            String key = Input.Keys.toString(SpaceProject.configManager.getConfig(KeyConfig.class).switchWeapon);
            String input = getEngine().getSystem(DesktopInputSystem.class).getControllerHasFocus() ? "[D-Pad Right]" : "[" + key + "]";
            String toolText = vehicleComponent.currentTool.toString();
            if (vehicleComponent.currentTool == VehicleComponent.Tool.tractor) {
                toolText += ":" + Mappers.tractor.get(entity).mode.toString();
            }
            toolText += " | " + input + " cycle";
            layout.setText(inventoryFont, toolText, Color.WHITE, 0, Align.right, false);
            inventoryFont.draw(batch, layout, controlsX, healthBarY - barHeight + layout.height - 3);
        }
    }

    private void drawInventory(Entity entity, float inventoryX, float inventoryY) {
        if (entity == null) return;
        CargoComponent cargo = Mappers.cargo.get(entity);
        if (cargo == null) return;

        long colorTime = 6000;
        long timeSinceCollect = GameScreen.getGameTimeCurrent() - cargo.lastCollectTime;
        float ratio = (float) timeSinceCollect / (float) colorTime;
        if (GameScreen.isPaused() || messageState == SpecialState.docked) {
            ratio = 0f; //force render if pause or docked
        }
        inventoryFont.setColor(1, 1, 1, 1 - ratio);
        inventoryFont.draw(batch, cargo.credits + " credits", inventoryX, inventoryY);
        int offsetY = 1;
        int total = 0;
        for (ItemComponent.Resource resource : ItemComponent.Resource.values()) {
            int id = resource.getId();
            if (cargo.inventory.containsKey(id)) {
                int quantity = cargo.inventory.get(id);
                total += quantity;
                Color color = resource.getColor();
                inventoryFont.setColor(color.r, color.g, color.b, 1 - ratio);
                layout.setText(inventoryFont, quantity + " " + resource.name().toLowerCase());
                inventoryFont.draw(batch, layout, inventoryX, inventoryY + (layout.height * 1.5f * offsetY++));
            }
        }
        //if (total == 0) return; hide if empty. maybe fade away instead?

        //draw total
        int barWidth = uiCFG.playerHPBarWidth;
        int barX = Gdx.graphics.getWidth() / 2 - barWidth / 2;
        int healthBarY = uiCFG.playerHPBarY;
        layout.setText(inventoryFont, total + "", Color.WHITE, 0, Align.center, false);
        inventoryFont.draw(batch, layout, barX - radius - 10, healthBarY + 3);
    }
    //endregion

    private void drawDamageText() {
        for (Iterator<DamageText> iterator = activeNumbers.iterator(); iterator.hasNext(); ) {
            DamageText text = iterator.next();
            float x = text.x;
            float y = text.y;
            if (text.project) {
                tempProj.set(x, y, 0);
                MyScreenAdapter.cam.project(tempProj);
                x = tempProj.x;
                y = tempProj.y;
            }
            cacheColor.set(1, 0, 0, 1).lerp(Color.CLEAR, (float) (GameScreen.getGameTimeCurrent() - text.timestamp) / damageTime);
            inventoryFont.setColor(cacheColor);
            inventoryFont.draw(batch, "" + (int) text.damage, x, y);

            if (text.timestamp < GameScreen.getGameTimeCurrent() - damageTime) {
                iterator.remove();
                numbersPool.free(text);
            }
        }
    }

    public static void damageMarker(Vector2 pos, float damage) {
        if (!showDamageNumbers || !drawHud) return;
        //https://libgdx.com/wiki/articles/memory-management#object-pooling
        DamageText test = numbersPool.obtain();
        test.init(pos.x, pos.y, damage, GameScreen.getGameTimeCurrent(), true);
        activeNumbers.add(test);
        activePeak = Math.max(activePeak, activeNumbers.size);
    }

    private void drawCreditMarkers(float deltaTime) {
        float velocityY = 35f;
        for (Iterator<CreditsMarker> iterator = markers.iterator(); iterator.hasNext(); ) {
            CreditsMarker marker = iterator.next();
            inventoryFont.setColor(marker.color.r, marker.color.g, marker.color.b, 1 - marker.animTimer.ratio());
            inventoryFont.draw(batch, "+" + marker.value, marker.location.x, marker.location.y += velocityY * deltaTime);
            if (marker.animTimer.tryEvent()) {
                iterator.remove();
            }
        }
    }

    public void addCredits(int totalCredits, Vector2 pos, Color color) {
        markers.add(new CreditsMarker(totalCredits, pos, color));//todo: replace with pool
    }

    /**
     * Mark off-screen objects on edge of screen for navigation.
     * TODO: load star mapState markers based on point list instead of star entity for stars that aren't loaded yet
     * TODO: move these values into MapComponent or a config file
     */
    private void drawEdgeMap() {
        Entity closestApproaching = getEngine().getSystem(GridRenderSystem.class).closestVelocity;

        float markerSmall = 3.5f; //min marker size
        float markerLarge = 8; //max marker size
        float distSmall = 600; //distance when marker is small
        float distLarge = 200; //distance when marker is large

        int padding = (int) (markerLarge + 4); //how close to draw from edge of screen (in pixels)
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int verticalEdge = (height - padding * 2) / 2;
        int horizontalEdge = (width - padding * 2) / 2;

        //todo: add navigation point for current direction facing
        //todo: add navigation point for current velocity vector
        focusedDist = -1;
        topLeft.set(0, 0, 0);
        cam.unproject(topLeft);
        bottomRight.set(width, height, 0);
        cam.unproject(bottomRight);
        for (Entity mapable : mapableEntities) {
            MapComponent map = Mappers.map.get(mapable);
            Vector2 pos = Mappers.transform.get(mapable).pos;
            screenPos.set(pos, 0);

            if (screenPos.dst2(MyScreenAdapter.cam.position) > map.distance * map.distance) {
                continue;
            }

            if (pos.x > topLeft.x && pos.x < bottomRight.x
                    && pos.y < topLeft.y && pos.y > bottomRight.y) {
                continue;
            }

            //set entity co'ords relative to center of screen
            screenPos.x -= MyScreenAdapter.cam.position.x;
            screenPos.y -= MyScreenAdapter.cam.position.y;

            //position to draw marker
            float markerX, markerY;

            //calculate slope of line (y = mx+b)
            float slope = screenPos.y / screenPos.x;

            //calculate where to position the marker
            if (screenPos.y < 0) {
                //top
                markerX = -verticalEdge / slope;
                markerY = -verticalEdge;
            } else {
                //bottom
                markerX = verticalEdge / slope;
                markerY = verticalEdge;
            }

            if (markerX < -horizontalEdge) {
                //left
                markerX = -horizontalEdge;
                markerY = slope * -horizontalEdge;
            } else if (markerX > horizontalEdge) {
                //right
                markerX = horizontalEdge;
                markerY = slope * horizontalEdge;
            }

            //set co'ords relative to center screen
            markerX += centerX;
            markerY += centerY;

            //calculate size of marker based on distance
            float dist = pos.dst(cam.position.x, cam.position.y);
            if (closestApproaching != null && closestApproaching.equals(mapable)) {
                focusedDist = dist;
                focusedPos.set(markerX, markerY);
            }
            TextureComponent tex = Mappers.texture.get(mapable);
            if (tex != null) {
                dist -= Math.max(tex.texture.getWidth(), tex.texture.getHeight()) * 0.5f * tex.scale;
            }
            float distClamp = MathUtils.clamp((dist - distLarge) / (distSmall - distLarge), 0, 1);
            float sizeInterp = Interpolation.pow3In.apply(1 - distClamp);
            float size = MathUtils.clamp((sizeInterp * (markerLarge - markerSmall)) + markerSmall, markerSmall, markerLarge);

            //draw marker
            shape.setColor(map.color);
            shape.circle(markerX, markerY, size);
			
			/*
			switch(mapState.shape) {
				case circle: shape.circle(markerX, markerY, (float)size);
				case square: shape.rect
				case triangle: shape.poly
			}
			 */
        }

    }

    public MiniMap getMiniMap() {
        return miniMap;
    }

    public GameMenu getGameMenu() {
        return gameMenu;
    }

    @Override
    public void resize(int width, int height) {
        gameMenu.resetPosition();
        if (stationMenu != null) {
            stationMenu.setPosition(Gdx.graphics.getWidth() / 3f, SpaceStationMenu.getHeight(stationMenu), Align.right);
        }
        miniMap.updateMapPosition();
    }

    @Override
    public void dispose() {
        font.dispose();
        //these probably shouldn't be static, that's why we have to clear
        numbersPool.clear();
        activeNumbers.clear();
    }

    @Override
    public String toString() {
        infoString.setLength(0);
        infoString.append("[HUD Pool]:         active: ").append(activeNumbers.size)
                .append(", active peak: ").append(activePeak)
                .append(", free: ").append(numbersPool.getFree())
                .append(", peak: ").append(numbersPool.peak)
                .append(", max: ").append(numbersPool.max);
        return infoString.toString();
    }

}
