package com.spaceproject.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Disposable;
import com.spaceproject.SpaceProject;
import com.spaceproject.components.*;
import com.spaceproject.config.EngineConfig;
import com.spaceproject.generation.FontLoader;
import com.spaceproject.math.MyMath;
import com.spaceproject.screens.GameScreen;
import com.spaceproject.utility.Mappers;

import static com.spaceproject.screens.MyScreenAdapter.*;


public class GridRenderSystem extends EntitySystem implements Disposable {
    
    //debug options
    public boolean clearScreen = false;
    public boolean drawOrigin = false;
    public boolean drawCameraPos = false;
    public boolean drawCameraPath = false;
    public boolean drawMousePath = false;
    public boolean drawThirdsGrid = false;
    
    //rendering
    private final ShapeRenderer shape = new ShapeRenderer();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Vector3 origin = new Vector3();
    private final Vector3 camWorldPos = new Vector3();
    private final Vector3 mouseProj = new Vector3();
    private final Vector3 playerPos = new Vector3();
    
    private final int gridWidth = 50;
    private Vector3 topLeft = new Vector3();
    private Vector3 bottomRight = new Vector3();
    private final Color gridColor = Color.BLACK.cpy();
    private final Color gridColorB = new Color(.51f, .5f, .5f, 0.3f);
    private final Color lineColor = new Color(0.15f, 0.5f, 0.9f, 0.9f);;
    private final Color highlight = Color.WHITE.cpy();
    private final Color stationColor = Color.GREEN.cpy();
    //private final Color compassColor = Color.WHITE.cpy();
    private final Color hintColorB = Color.PURPLE.cpy();
    private final Color hintColorA = Color.GREEN.cpy();

    private final Color cacheColor = new Color();
    private final Vector2 cacheVec = new Vector2();

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> orbitEntities;
    private ImmutableArray<Entity> stations;
    private Entity camMarker, mouseMarker;
    private float accumulator = 0;

    Entity closestFacing;
    Entity closestVelocity;

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont subFont;
    private final GlyphLayout layout = new GlyphLayout();
    
    public GridRenderSystem() {
        FreeTypeFontGenerator.FreeTypeFontParameter parameter2 = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter2.size = 10;
        parameter2.borderColor = Color.BLACK;
        parameter2.borderWidth = 3;
        subFont = FontLoader.createFont(FontLoader.fontPressStart, parameter2);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        orbitEntities = engine.getEntitiesFor(Family.all(OrbitComponent.class).get());
        players = engine.getEntitiesFor(Family.all(CameraFocusComponent.class, ControllableComponent.class).exclude(DockedComponent.class).get());
        stations = engine.getEntitiesFor(Family.all(SpaceStationComponent.class, TransformComponent.class).get());
    }
    
    @Override
    public void update(float deltaTime) {
        //warning: system coupling -> todo: use signals?
        HUDSystem hudSystem = getEngine().getSystem(HUDSystem.class);
        if (hudSystem != null && !hudSystem.isDraw()) {
            return;
        }
        
        //update matrix and convert screen coords to world cords.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(cam.combined);

        //enable transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        //render
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        drawGrid(GameScreen.isHyper() ? Color.WHITE : gridColor, calculateGridDensity(gridWidth), 2f);
        //drawGrid(GameScreen.isHyper() ? Color.WHITE : gridColor, gridColorB, calculateGridDensity(gridWidth), 2f, 5);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        drawOrbitPath();

        shape.setProjectionMatrix(projectionMatrix);

        //debug override background
        if (clearScreen) debugClearScreen();
        //debug reference points
        if (drawOrigin) drawWorldOrigin(Color.WHITE);
        if (drawCameraPos) drawDebugCameraPos(Color.RED);
        if (drawCameraPath) debugDrawCameraPath(Color.YELLOW);
        if (drawMousePath) debugDrawMousePath();
        if (drawThirdsGrid) drawThirdsGrid(Color.PINK);
        
        //draw helpful navigation information
        if (players.size() > 0) {
            drawCompass(players.first());
        }

        shape.end();

        /*
        batch.begin();
        drawHint("an object in motion, remains in motion");
        batch.end();
        */
    
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawGrid(Color color, int gridSize, float width) {
        shape.setColor(color);
        //unproject to get edges of viewport
        topLeft.set(0, 0, 0);
        viewport.unproject(topLeft);
        bottomRight.set(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0);
        viewport.unproject(bottomRight);
        //draw vertical along X axis
        float thickness = (1/SpaceProject.configManager.getConfig(EngineConfig.class).renderScale) * cam.zoom * width;
        int i = 0;
        int referenceX = (int) (topLeft.x/gridSize);
        int alignedX = referenceX * gridSize;
        for (int x = alignedX; x < bottomRight.x; x += gridSize) {
            float aX = alignedX + (i * gridSize);
            i++;
            shape.rectLine(aX, topLeft.y, aX, bottomRight.y, thickness);
        }
        //draw horizontal along Y axis
        int j = 0;
        int referenceY = (int) (bottomRight.y / gridSize);
        int alignedY = referenceY * gridSize;
        for (int y = alignedY; y < topLeft.y; y += gridSize) {
            float aY = alignedY + (j * gridSize);
            j++;
            shape.rectLine(topLeft.x, aY, bottomRight.x, aY, thickness);
        }
    }

    private void drawGridMultiColor(Color colorA, Color colorB, int gridSize, float width, int emphasisDivisor) {
        //unproject to get edges of viewport
        topLeft.set(0, 0, 0);
        viewport.unproject(topLeft);
        bottomRight.set(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0);
        viewport.unproject(bottomRight);
        //draw vertical along X axis
        float thickness = (1/SpaceProject.configManager.getConfig(EngineConfig.class).renderScale) * cam.zoom * width;
        int i = 0;
        int referenceX = (int) (topLeft.x/gridSize);
        int alignedX = referenceX * gridSize;
        for (int x = alignedX; x < bottomRight.x; x += gridSize) {
            float aX = alignedX + (i * gridSize);
            boolean emphasis = (referenceX + i) % emphasisDivisor == 0;
            i++;
            shape.setColor(emphasis ? colorB : colorA);
            shape.rectLine(aX, topLeft.y, aX, bottomRight.y, emphasis ? thickness * 2 : thickness);
        }
        //draw horizontal along Y axis
        int j = 0;
        int referenceY = (int) (bottomRight.y / gridSize);
        int alignedY = referenceY * gridSize;
        for (int y = alignedY; y < topLeft.y; y += gridSize) {
            float aY = alignedY + (j * gridSize);
            boolean emphasis = (referenceY + j) % emphasisDivisor == 0;
            j++;
            shape.setColor(emphasis ? colorB : colorA);
            shape.rectLine(topLeft.x, aY, bottomRight.x, aY, emphasis ? thickness * 2 : thickness);
        }
    }

    private int calculateGridDensity(int width) {
        // calculate "adaptive grid size"
        CameraSystem camera = getEngine().getSystem(CameraSystem.class);
        byte zoomLevel = camera.getZoomLevel();
        byte zoomLevelThreshold = 10; // at what level to start halving grid cells
        for (byte level = zoomLevelThreshold; level <= camera.getMaxZoomLevel(); level++) {
            if (zoomLevel >= level) {
                width *= 2; // double width as we zoom out
            }
        }
        return width;
    }

    private void drawOrbitPath() {
        float alpha = MathUtils.clamp((cam.zoom / 150 / 2), 0, 1);
        if (MathUtils.isEqual(alpha, 0)) return;

        lineColor.a = alpha;
        highlight.a = alpha;

        Body body = null;
        if (players.size() > 0) {
            body = Mappers.physics.get(players.first()).body;
        }

        closestFacing = null;
        closestVelocity = null;
        float distClosestFacing = Float.MAX_VALUE;
        float distClosestVelocity = Float.MAX_VALUE;
        for (Entity entity : orbitEntities) {
            OrbitComponent orbit = Mappers.orbit.get(entity);
            TransformComponent transform = Mappers.transform.get(entity);
            TextureComponent tex = Mappers.texture.get(entity);

            boolean intersectsFacing = false;
            boolean intersectsVelocity = false;
            if (tex != null) {
                float radius = tex.texture.getWidth() * 0.5f * tex.scale;
                if (body != null && !body.getLinearVelocity().isZero()) {
                    Vector2 facing = MyMath.vector(body.getAngle(), 500000).add(body.getPosition());
                    intersectsFacing = Intersector.intersectSegmentCircle(body.getPosition(), facing, transform.pos, radius * radius);
                    if (intersectsFacing) {
                        float dist2 = body.getPosition().dst2(transform.pos);
                        if (dist2 < distClosestFacing) {
                            distClosestFacing = dist2;
                            closestFacing = entity;
                        }
                    }
                    Vector2 velocity = MyMath.vector(body.getLinearVelocity().angleRad(), 500000).add(body.getPosition());
                    intersectsVelocity = Intersector.intersectSegmentCircle(body.getPosition(),velocity, transform.pos, radius * radius);
                    if (intersectsVelocity) {
                        float dist2 = body.getPosition().dst2(transform.pos);
                        if (dist2 < distClosestVelocity) {
                            distClosestVelocity = dist2;
                            closestVelocity = entity;
                        }
                    }
                }
                shape.setColor(intersectsFacing ? highlight : lineColor);
                shape.circle(transform.pos.x, transform.pos.y, radius);
            }

            if (orbit.parent != null) {
                TransformComponent parentPos = Mappers.transform.get(orbit.parent);
                shape.setColor(intersectsFacing ? highlight : lineColor);
                shape.circle(parentPos.pos.x, parentPos.pos.y, orbit.radialDistance);
                shape.line(parentPos.pos.x, parentPos.pos.y, transform.pos.x, transform.pos.y);
            }

            AsteroidBeltComponent stellarDisk = Mappers.asteroidBelt.get(entity);
            if (stellarDisk != null) {
                Vector2 pos = Mappers.transform.get(entity).pos;
                shape.setColor(gridColorB);
                shape.circle(pos.x, pos.y, stellarDisk.radius - (stellarDisk.bandWidth / 2));
                shape.circle(pos.x, pos.y, stellarDisk.radius + (stellarDisk.bandWidth / 2));
            }
        }

        for (Entity entity : stations) {
            TransformComponent transform = Mappers.transform.get(entity);
            TransformComponent parentTransform = Mappers.transform.get(Mappers.spaceStation.get(entity).parentOrbitBody);
            float dist = transform.pos.dst(parentTransform.pos);
            stationColor.a = alpha;
            shape.setColor(stationColor);
            shape.circle(parentTransform.pos.x, parentTransform.pos.y, dist);
        }

        accumulator += 3 * Gdx.graphics.getDeltaTime();
        if (closestFacing != null && Mappers.star.get(closestFacing) == null) {
            Vector2 pos = Mappers.transform.get(closestFacing).pos;
            TextureComponent tex = Mappers.texture.get(closestFacing);
            float radius = tex.texture.getWidth() * 0.5f * tex.scale;
            lineColor.a = (float) Math.abs(Math.sin(accumulator));
            shape.setColor(lineColor);
            float width = radius * 2;
            shape.rect(pos.x - width * 0.5f, pos.y - width * 0.5f, width, width);
        }
        if (closestVelocity != null && Mappers.star.get(closestVelocity) == null) {
            Vector2 pos = Mappers.transform.get(closestVelocity).pos;
            TextureComponent tex = Mappers.texture.get(closestVelocity);
            float radius = tex.texture.getWidth() * 0.5f * tex.scale;
            lineColor.a = (float) Math.abs(Math.sin(accumulator));
            shape.setColor(lineColor);
            float width = radius * 2;
            shape.rect(pos.x - width * 0.5f, pos.y - width * 0.5f, width, width);
        }
    }

    private void drawCompass(Entity entity) {
        //set alpha
        float min = 0.05f;
        float maxZoom = CameraSystem.getZoomForLevel(getEngine().getSystem(CameraSystem.class).getMaxZoomLevel());
        float alpha = MathUtils.map(1, maxZoom, min, 1, Math.max(Math.min(GameScreen.cam.zoom * 10, maxZoom), 1));
        if (MathUtils.isEqual(alpha, 0)) return;

        HyperDriveComponent hyper = Mappers.hyper.get(entity);
        if (hyper != null && hyper.state == HyperDriveComponent.State.on) {
            return;
        }

        //draw movement direction for navigation assistance, line up vector with target destination
        Body body = Mappers.physics.get(entity).body;
        Vector2 facing = MyMath.vector(body.getAngle(), 500000);
        Vector2 originalPosition = Mappers.transform.get(entity).pos;
        playerPos.set(originalPosition, 0);
        Vector3 pos = cam.project(playerPos);

        float width = 0.5f;
        cacheColor.set(Color.WHITE);
        cacheColor.a = alpha;
        shape.rectLine(pos.x, pos.y, facing.x, facing.y, width, cacheColor, cacheColor);
        CannonComponent cannon = Mappers.cannon.get(entity);
        if (cannon != null) {
            int hitTime = 200;
            long timeSinceHit = GameScreen.getGameTimeCurrent() - cannon.lastHitTime;
            if (timeSinceHit < hitTime) {
                cacheColor.set(Color.WHITE.cpy()).lerp(Color.MAGENTA, 1 - ((float) timeSinceHit / hitTime));
            }
        }
        cacheColor.a = 1;
        shape.setColor(cacheColor);
        Vector2 lead = MyMath.vector(body.getAngle(), 80).add(pos.x, pos.y);
        shape.circle(lead.x, lead.y, 2);

        if (body.getLinearVelocity().isZero()) {
            return;
        }

        //draw movement direction for navigation assistance, line up vector with target destination
        cacheColor.set(Color.WHITE);
        ControllableComponent control = Mappers.controllable.get(entity);
        if (control.moveForward || control.moveBack || control.moveLeft || control.moveRight) {
            width *= 2;
            cacheColor.set(0xffd700ff);
        }
        if (control.boost) {
            cacheColor.set(0, 1, 1, 1);
        }
        ShieldComponent shield = Mappers.shield.get(entity);
        if (shield != null && shield.state == ShieldComponent.State.on) {
            int hitTime = 500;
            long timeSinceHit = GameScreen.getGameTimeCurrent() - shield.lastHit;
            if (timeSinceHit < hitTime) {
                cacheColor.set(Color.GREEN);
            } else {
                cacheColor.set(Color.BLUE);
            }
        }
        HealthComponent health = Mappers.health.get(entity);
        long hurtTime = 1000;
        if (health != null && (GameScreen.getGameTimeCurrent() - health.lastHitTime < hurtTime)) {
            cacheColor.set(Color.RED);
        }
        cacheColor.a = alpha;

        //draw velocity vector
        cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 500000));
        shape.rectLine(pos.x, pos.y, cacheVec.x, cacheVec.y, width, cacheColor, cacheColor);
        
        float velocity = body.getLinearVelocity().len();
        //boolean isMaxVelocity = MathUtils.isEqual(velocity, Box2DPhysicsSystem.getVelocityLimit(), 0.5f);

        cacheColor.a = 1;
        shape.setColor(cacheColor);

        //draw arrows
        int arrowSize = 10;
        float angle = 135 * MathUtils.degreesToRadians;
        cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 80)).add(pos.x, pos.y);
        Vector2 arrowLeft = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
        shape.rectLine(cacheVec, arrowLeft, 1);
        Vector2 arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
        shape.rectLine(cacheVec, arrowRight, 1);

        float percent = velocity / Box2DPhysicsSystem.getVelocityLimit();
        float endPoint = MathUtils.map(0, 1, 80, 120, percent);
        cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), endPoint)).add(pos.x, pos.y);
        Vector2 arrowLeftVel = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
        shape.rectLine(cacheVec, arrowLeftVel, 1);
        Vector2 arrowRightVel = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
        shape.rectLine(cacheVec, arrowRightVel, 1);

        //more!
        if (velocity >= Box2DPhysicsSystem.getVelocityLimit() * 0.25f) {
            cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 90)).add(pos.x, pos.y);
            arrowLeft = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowLeft, 1);
            arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowRight, 1);
        }
        //more++
        if (velocity >= Box2DPhysicsSystem.getVelocityLimit() * 0.5f) {
            cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 100)).add(pos.x, pos.y);
            arrowLeft = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowLeft, 1);
            arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowRight, 1);
        }
        //moar+++
        if (velocity >= Box2DPhysicsSystem.getVelocityLimit() * 0.75f) {
            cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 110)).add(pos.x, pos.y);
            arrowLeft = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowLeft, 1);
            arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowRight, 1);
        }
        /*
        //max velocity!!!
        if (isMaxVelocity) {
            cacheVec.set(MyMath.vector(body.getLinearVelocity().angleRad(), 120)).add(pos.x, pos.y);
            arrowLeft = MyMath.vector(body.getLinearVelocity().angleRad() + angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowLeft, 1);
            arrowRight = MyMath.vector(body.getLinearVelocity().angleRad() - angle, arrowSize).add(cacheVec);
            shape.rectLine(cacheVec, arrowRight, 1);
        }*/

        //draw force applied from engines?
    }

    private void drawHint(String text) {
        float ratio = 1 + (float) Math.sin(accumulator*0.1);
        hintColorA.set(Color.GREEN).lerp(hintColorB, ratio);
        subFont.setColor(hintColorA);
        layout.setText(subFont, text);
        
        float centerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        int messageHeight = (int) (Gdx.graphics.getHeight() - (Gdx.graphics.getHeight()/3) + layout.height);
        messageHeight -= layout.height * 2;
        subFont.draw(batch, layout, 0, 0);
    }
    
    //region debug and testing
    private void debugClearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    }
    
    private void drawDebugCameraPos(Color color) {
        camWorldPos.set(cam.position);
        GameScreen.viewport.project(camWorldPos);
        shape.setColor(color);
        shape.circle(camWorldPos.x, camWorldPos.y, 8);
        shape.line(camWorldPos.x, 0, camWorldPos.x, Gdx.graphics.getHeight());
        shape.line(0, camWorldPos.y, Gdx.graphics.getWidth(), camWorldPos.y);
    }
    
    private void debugDrawCameraPath(Color color) {
        if (camMarker == null) {
            //add debug camera marker
            TrailComponent spline = new TrailComponent();
            spline.color = color;
            camMarker = new Entity().add(spline).add(new TransformComponent());
            getEngine().addEntity(camMarker);
            Gdx.app.log(this.getClass().getSimpleName(), "debug cam marker activated");
        }
        Mappers.transform.get(camMarker).pos.set(cam.position.x, cam.position.y);
    }
    
    private void drawWorldOrigin(Color color) {
        origin.set(0,0,0);
        GameScreen.viewport.project(origin);
        shape.setColor(color);
        shape.circle(origin.x, origin.y, 10);
        shape.line(origin.x, 0, origin.x, Gdx.graphics.getHeight());
        shape.line(0, origin.y, Gdx.graphics.getWidth(), origin.y);
    }
    
    private void debugDrawMousePath(){
        if (mouseMarker == null) {
            mouseMarker = new Entity().add(new TrailComponent()).add(new TransformComponent());
            mouseMarker.getComponent(TrailComponent.class).color = Color.YELLOW;
            getEngine().addEntity(mouseMarker);
        }
        mouseProj.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        GameScreen.viewport.unproject(mouseProj);
        Mappers.transform.get(mouseMarker).pos.set(mouseProj.x, mouseProj.y);
    }
    
    private void drawThirdsGrid(Color color) {
        float widthThirds = Gdx.graphics.getWidth() / 3.0f;
        float heightThirds = Gdx.graphics.getHeight() / 3.0f;
        shape.setColor(color);
        
        //verticle
        shape.line(widthThirds, 0, widthThirds, Gdx.graphics.getHeight());
        shape.line(widthThirds*2, 0, widthThirds*2, Gdx.graphics.getHeight());
        
        //horizontal
        shape.line(0, heightThirds, Gdx.graphics.getWidth(), heightThirds);
        shape.line(0, heightThirds*2, Gdx.graphics.getWidth(), heightThirds*2);
    }
    //endregion
    
    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        subFont.dispose();
    }
    
}
