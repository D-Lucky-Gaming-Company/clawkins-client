package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Applies movement to entities each frame.
 * For every entity with [Move + Transform], adds direction * maxSpeed *
 * deltaTime to position.
 *
 * This is a simplified move system (no Box2D physics). If you add physics
 * later,
 * you'd replace this with a PhysicMoveSystem that sets body velocity instead.
 */
public class MoveSystem extends IteratingSystem {
    private static final float MAX_SWEEP_STEP_DISTANCE = 0.08f;
    private static final int COLLISION_EDGE_SOLVE_ITERATIONS = 8;
    private static final float COLLISION_CONTACT_NUDGE = 0.0005f;
    private static final float HITBOX_WIDTH_FACTOR = 0.25f;
    private static final float HITBOX_HEIGHT_FACTOR = 0.25f;
    private static final float HITBOX_Y_OFFSET_FACTOR = 0.16f;
    private static final float ENEMY_HITBOX_WIDTH_FACTOR = 0.28f;
    private static final float ENEMY_HITBOX_HEIGHT_FACTOR = 0.24f;
    private static final float TILE_HITBOX_WIDTH_FACTOR = 1f;
    private static final float TILE_HITBOX_HEIGHT_FACTOR = 1f;
    private static final float SOLID_HITBOX_WIDTH_FACTOR = 1f;
    private static final float SOLID_HITBOX_HEIGHT_FACTOR = 1f;

    private float mapWidth;
    private float mapHeight;
    private final List<TiledMapTileLayer> collisionLayers;
    private final List<BarrierShape> barrierHitboxes;
    private final List<Rectangle> debugTileCollisionBoxes;
    private final List<DebugShape> debugBarrierCollisionShapes;
    private final Rectangle tmpEntityHitbox;
    private final Rectangle tmpTileRect;
    private final Rectangle tmpSolidRect;
    private final Polygon tmpProbePolygon;
    private final float[] tmpProbeVertices;
    private final Rectangle debugLastBlockedMoverHitbox;
    private final Rectangle debugLastBlockingRect;
    private final Circle debugLastBlockingCircle;
    private float[] debugLastBlockingPolygonVertices;
    private DebugCollisionKind debugLastCollisionKind;
    private ImmutableArray<Entity> solidEntities;

    public MoveSystem() {
        super(Family.all(Move.class, Transform.class).get(), 10);
        this.collisionLayers = new ArrayList<>();
        this.barrierHitboxes = new ArrayList<>();
        this.debugTileCollisionBoxes = new ArrayList<>();
        this.debugBarrierCollisionShapes = new ArrayList<>();
        this.tmpEntityHitbox = new Rectangle();
        this.tmpTileRect = new Rectangle();
        this.tmpSolidRect = new Rectangle();
        this.tmpProbePolygon = new Polygon();
        this.tmpProbeVertices = new float[8];
        this.debugLastBlockedMoverHitbox = new Rectangle();
        this.debugLastBlockingRect = new Rectangle();
        this.debugLastBlockingCircle = new Circle();
        this.debugLastBlockingPolygonVertices = null;
        this.debugLastCollisionKind = DebugCollisionKind.NONE;
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.solidEntities = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (Player.MAPPER.get(entity) != null) {
            clearLastCollisionDebug();
        }

        Move move = Move.MAPPER.get(entity);
        Transform transform = Transform.MAPPER.get(entity);

        Vector2 direction = move.getDirection();
        if (direction.isZero()) {
            return;
        }

        float speed = move.getMaxSpeed();
        Vector2 position = transform.getPosition();
        Vector2 size = transform.getSize();

        float moveX = direction.x * speed * deltaTime;
        float moveY = direction.y * speed * deltaTime;
        int sweepSteps = sweepStepsFor(moveX, moveY);
        if (sweepSteps <= 1) {
            applySingleStep(entity, position, size, moveX, moveY);
            return;
        }

        float stepX = moveX / sweepSteps;
        float stepY = moveY / sweepSteps;
        for (int i = 0; i < sweepSteps; i++) {
            applySingleStep(entity, position, size, stepX, stepY);
        }
    }

    private void applySingleStep(Entity entity, Vector2 position, Vector2 size, float moveX, float moveY) {
        float targetX = clampX(position.x + moveX, size.x, entity);
        position.x = resolveAxisToCollisionEdge(
                position.x,
                targetX,
                position.y,
                size.x,
                size.y,
                true,
                entity);

        float targetY = clampY(position.y + moveY, size.y, entity);
        position.y = resolveAxisToCollisionEdge(
                position.y,
                targetY,
                position.x,
                size.x,
                size.y,
                false,
                entity);
    }

    private float resolveAxisToCollisionEdge(
            float startAxis,
            float desiredAxis,
            float otherAxis,
            float width,
            float height,
            boolean xAxis,
            Entity mover) {
        if (desiredAxis == startAxis) {
            return startAxis;
        }
        if (!isBlockedAtAxis(desiredAxis, otherAxis, width, height, xAxis, mover)) {
            return desiredAxis;
        }

        float free = startAxis;
        float blocked = desiredAxis;
        for (int i = 0; i < COLLISION_EDGE_SOLVE_ITERATIONS; i++) {
            float mid = (free + blocked) * 0.5f;
            if (isBlockedAtAxis(mid, otherAxis, width, height, xAxis, mover)) {
                blocked = mid;
            } else {
                free = mid;
            }
        }

        float direction = Math.signum(desiredAxis - startAxis);
        if (direction != 0f) {
            float nudged = free + direction * COLLISION_CONTACT_NUDGE;
            if (!isBlockedAtAxis(nudged, otherAxis, width, height, xAxis, mover)) {
                return nudged;
            }
        }
        return free;
    }

    private boolean isBlockedAtAxis(
            float axis,
            float otherAxis,
            float width,
            float height,
            boolean xAxis,
            Entity mover) {
        float x = xAxis ? axis : otherAxis;
        float y = xAxis ? otherAxis : axis;
        return isBlocked(x, y, width, height, mover, true);
    }

    private static int sweepStepsFor(float moveX, float moveY) {
        float longestAxisDistance = Math.max(Math.abs(moveX), Math.abs(moveY));
        if (longestAxisDistance <= MAX_SWEEP_STEP_DISTANCE) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(longestAxisDistance / MAX_SWEEP_STEP_DISTANCE));
    }

    public void setMap(TiledMap tiledMap) {
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        this.mapWidth = width * tileW * Main.UNIT_SCALE;
        this.mapHeight = height * tileH * Main.UNIT_SCALE;

        this.collisionLayers.clear();
        this.barrierHitboxes.clear();
        this.debugTileCollisionBoxes.clear();
        this.debugBarrierCollisionShapes.clear();
        for (MapLayer layer : tiledMap.getLayers()) {
            collectBarrierObjects(layer);
            if (layer instanceof TiledMapTileLayer tileLayer) {
                // Tile collision objects should block regardless of visual layer placement.
                this.collisionLayers.add(tileLayer);
                collectTileCollisionDebugObjects(tileLayer);
            }
        }
    }

    /**
     * Checks whether a mover would be blocked at a given world position.
     * Intended for one-shot placement checks (e.g., map transition spawning).
     */
    public boolean isBlockedPosition(float x, float y, float width, float height, Entity mover) {
        return isBlocked(x, y, width, height, mover);
    }

    private boolean isBlocked(float x, float y, float width, float height, Entity mover) {
        return isBlocked(x, y, width, height, mover, false);
    }

    private boolean isBlocked(float x, float y, float width, float height, Entity mover, boolean captureDebug) {
        Rectangle entityHitbox = buildHitbox(x, y, width, height, mover, tmpEntityHitbox);

        if (isBlockedByMap(entityHitbox, mover, captureDebug)) {
            return true;
        }
        return isBlockedBySolidEntity(entityHitbox, mover, captureDebug);
    }

    private boolean isBlockedByMap(Rectangle entityHitbox, Entity mover, boolean captureDebug) {
        if (isBlockedByBarrierObjects(entityHitbox, mover, captureDebug)) {
            return true;
        }
        if (collisionLayers.isEmpty()) {
            return false;
        }

        for (TiledMapTileLayer layer : collisionLayers) {
            float tileWorldW = layer.getTileWidth() * Main.UNIT_SCALE;
            float tileWorldH = layer.getTileHeight() * Main.UNIT_SCALE;
            if (tileWorldW <= 0f || tileWorldH <= 0f) {
                continue;
            }

            float layerOffsetX = layer.getOffsetX() * Main.UNIT_SCALE;
            float layerOffsetY = layer.getOffsetY() * Main.UNIT_SCALE;

            int minCol = (int) Math.floor((entityHitbox.x - layerOffsetX) / tileWorldW) - 1;
            int maxCol = (int) Math.floor((entityHitbox.x + entityHitbox.width - layerOffsetX - 0.0001f) / tileWorldW) + 1;
            int minRow = (int) Math.floor((entityHitbox.y - layerOffsetY) / tileWorldH) - 1;
            int maxRow = (int) Math.floor((entityHitbox.y + entityHitbox.height - layerOffsetY - 0.0001f) / tileWorldH) + 1;

            for (int row = minRow; row <= maxRow; row++) {
                for (int col = minCol; col <= maxCol; col++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                    if (cell == null) {
                        continue;
                    }
                    TiledMapTile tile = cell.getTile();
                    if (tile == null) {
                        continue;
                    }
                    MapObjects objects = tile.getObjects();
                    if (objects == null || objects.getCount() == 0) {
                        continue;
                    }

                    for (MapObject mapObject : objects) {
                        Rectangle rect = null;
                        if (mapObject instanceof RectangleMapObject) {
                            rect = ((RectangleMapObject) mapObject).getRectangle();
                        } else if (mapObject instanceof PolygonMapObject) {
                            rect = ((PolygonMapObject) mapObject).getPolygon().getBoundingRectangle();
                        }

                        if (rect == null) {
                            continue;
                        }

                        float tileOffsetX = tile.getOffsetX() * Main.UNIT_SCALE;
                        float tileOffsetY = tile.getOffsetY() * Main.UNIT_SCALE;

                        float rectX = col * tileWorldW + layerOffsetX + tileOffsetX + rect.x * Main.UNIT_SCALE;
                        float rectY = row * tileWorldH + layerOffsetY + tileOffsetY + rect.y * Main.UNIT_SCALE;
                        float rectW = rect.width * Main.UNIT_SCALE;
                        float rectH = rect.height * Main.UNIT_SCALE;
                        float tileHitboxW = rectW * TILE_HITBOX_WIDTH_FACTOR;
                        float tileHitboxH = rectH * TILE_HITBOX_HEIGHT_FACTOR;
                        float tileHitboxX = rectX + (rectW - tileHitboxW) * 0.5f;
                        float tileHitboxY = rectY;
                        tmpTileRect.set(tileHitboxX, tileHitboxY, tileHitboxW, tileHitboxH);

                        if (entityHitbox.overlaps(tmpTileRect)) {
                            if (captureDebug && Player.MAPPER.get(mover) != null) {
                                setLastCollisionDebugRect(entityHitbox, tmpTileRect, DebugCollisionKind.TILE);
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlockedByBarrierObjects(Rectangle entityHitbox, Entity mover, boolean captureDebug) {
        if (barrierHitboxes.isEmpty()) {
            return false;
        }
        for (BarrierShape barrierRect : barrierHitboxes) {
            if (barrierRect.overlaps(entityHitbox, tmpProbePolygon, tmpProbeVertices)) {
                if (captureDebug && Player.MAPPER.get(mover) != null) {
                    setLastCollisionDebugBarrier(entityHitbox, barrierRect);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedBySolidEntity(Rectangle entityHitbox, Entity mover, boolean captureDebug) {
        if (solidEntities == null || solidEntities.size() == 0) {
            return false;
        }

        for (Entity entity : solidEntities) {
            if (entity == mover) {
                continue;
            }
            Interactible interactible = Interactible.MAPPER.get(entity);
            if (interactible == null || !interactible.hasCollision()) {
                continue;
            }
            Transform transform = Transform.MAPPER.get(entity);
            if (transform == null) {
                continue;
            }

            float solidW = transform.getSize().x * SOLID_HITBOX_WIDTH_FACTOR;
            float solidH = transform.getSize().y * SOLID_HITBOX_HEIGHT_FACTOR;
            float solidX = transform.getPosition().x + (transform.getSize().x - solidW) * 0.5f;
            float solidY = transform.getPosition().y;
            tmpSolidRect.set(solidX, solidY, solidW, solidH);
            if (entityHitbox.overlaps(tmpSolidRect)) {
                if (captureDebug && Player.MAPPER.get(mover) != null) {
                    setLastCollisionDebugRect(entityHitbox, tmpSolidRect, DebugCollisionKind.SOLID_ENTITY);
                }
                return true;
            }
        }
        return false;
    }

    private float clampX(float x, float width, Entity mover) {
        float hitboxWidth = width * hitboxWidthFactorFor(mover);
        float hitboxOffsetX = (width - hitboxWidth) * 0.5f;
        float minX = -hitboxOffsetX;
        float maxX = mapWidth - (hitboxOffsetX + hitboxWidth);
        if (maxX < minX) {
            return minX;
        }
        return Math.max(minX, Math.min(maxX, x));
    }

    private float clampY(float y, float height, Entity mover) {
        float hitboxHeight = height * hitboxHeightFactorFor(mover);
        float hitboxOffsetY = height * hitboxYOffsetFactorFor(mover);
        float minY = -hitboxOffsetY;
        float maxY = mapHeight - (hitboxOffsetY + hitboxHeight);
        if (maxY < minY) {
            return minY;
        }
        return Math.max(minY, Math.min(maxY, y));
    }

    private Rectangle buildHitbox(float x, float y, float width, float height, Entity mover, Rectangle outRect) {
        float hitboxWidth = width * hitboxWidthFactorFor(mover);
        float hitboxHeight = height * hitboxHeightFactorFor(mover);
        float hitboxX = x + (width - hitboxWidth) * 0.5f;
        float hitboxY = y + height * hitboxYOffsetFactorFor(mover);
        return outRect.set(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    private void collectBarrierObjects(MapLayer layer) {
        if (layer == null || layer.getObjects() == null || layer.getObjects().getCount() == 0) {
            return;
        }
        float layerOffsetX = layer.getOffsetX();
        float layerOffsetY = layer.getOffsetY();
        for (MapObject object : layer.getObjects()) {
            if (!isBarrierObject(layer, object)) {
                continue;
            }
            Rectangle bounds = getObjectBounds(object);
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                continue;
            }
            BarrierShape barrierShape = toBarrierShape(object, layerOffsetX, layerOffsetY);
            if (barrierShape != null) {
                barrierHitboxes.add(barrierShape);
                debugBarrierCollisionShapes.add(DebugShape.fromBarrierShape(barrierShape));
            }
        }
    }

    private void collectTileCollisionDebugObjects(TiledMapTileLayer layer) {
        float tileWorldW = layer.getTileWidth() * Main.UNIT_SCALE;
        float tileWorldH = layer.getTileHeight() * Main.UNIT_SCALE;
        if (tileWorldW <= 0f || tileWorldH <= 0f) {
            return;
        }

        float layerOffsetX = layer.getOffsetX() * Main.UNIT_SCALE;
        float layerOffsetY = layer.getOffsetY() * Main.UNIT_SCALE;
        int rows = layer.getHeight();
        int cols = layer.getWidth();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                if (cell == null) {
                    continue;
                }
                TiledMapTile tile = cell.getTile();
                if (tile == null) {
                    continue;
                }
                MapObjects objects = tile.getObjects();
                if (objects == null || objects.getCount() == 0) {
                    continue;
                }

                for (MapObject mapObject : objects) {
                    Rectangle rect = null;
                    if (mapObject instanceof RectangleMapObject) {
                        rect = ((RectangleMapObject) mapObject).getRectangle();
                    } else if (mapObject instanceof PolygonMapObject) {
                        rect = ((PolygonMapObject) mapObject).getPolygon().getBoundingRectangle();
                    }
                    if (rect == null) {
                        continue;
                    }

                    float tileOffsetX = tile.getOffsetX() * Main.UNIT_SCALE;
                    float tileOffsetY = tile.getOffsetY() * Main.UNIT_SCALE;
                    float rectX = col * tileWorldW + layerOffsetX + tileOffsetX + rect.x * Main.UNIT_SCALE;
                    float rectY = row * tileWorldH + layerOffsetY + tileOffsetY + rect.y * Main.UNIT_SCALE;
                    float rectW = rect.width * Main.UNIT_SCALE;
                    float rectH = rect.height * Main.UNIT_SCALE;
                    float tileHitboxW = rectW * TILE_HITBOX_WIDTH_FACTOR;
                    float tileHitboxH = rectH * TILE_HITBOX_HEIGHT_FACTOR;
                    float tileHitboxX = rectX + (rectW - tileHitboxW) * 0.5f;
                    float tileHitboxY = rectY;
                    debugTileCollisionBoxes.add(new Rectangle(tileHitboxX, tileHitboxY, tileHitboxW, tileHitboxH));
                }
            }
        }
    }

    private static BarrierShape toBarrierShape(MapObject mapObject, float layerOffsetX, float layerOffsetY) {
        if (mapObject instanceof RectangleMapObject rectangleMapObject) {
            Rectangle rect = rectangleMapObject.getRectangle();
            return BarrierShape.fromRectangle(new Rectangle(
                    (rect.x + layerOffsetX) * Main.UNIT_SCALE,
                    (rect.y + layerOffsetY) * Main.UNIT_SCALE,
                    rect.width * Main.UNIT_SCALE,
                    rect.height * Main.UNIT_SCALE));
        }
        if (mapObject instanceof PolygonMapObject polygonMapObject) {
            float[] transformed = polygonMapObject.getPolygon().getTransformedVertices();
            if (transformed == null || transformed.length < 6) {
                return null;
            }
            float[] worldVertices = new float[transformed.length];
            for (int i = 0; i < transformed.length; i += 2) {
                worldVertices[i] = (transformed[i] + layerOffsetX) * Main.UNIT_SCALE;
                worldVertices[i + 1] = (transformed[i + 1] + layerOffsetY) * Main.UNIT_SCALE;
            }
            return BarrierShape.fromPolygon(new Polygon(worldVertices));
        }
        if (mapObject instanceof CircleMapObject circleMapObject) {
            Circle circle = circleMapObject.getCircle();
            return BarrierShape.fromCircle(new Circle(
                    (circle.x + layerOffsetX) * Main.UNIT_SCALE,
                    (circle.y + layerOffsetY) * Main.UNIT_SCALE,
                    circle.radius * Main.UNIT_SCALE));
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            Ellipse ellipse = ellipseMapObject.getEllipse();
            if (ellipse.width <= 0f || ellipse.height <= 0f) {
                return null;
            }
            final int segments = 20;
            float[] worldVertices = new float[segments * 2];
            float centerX = (ellipse.x + ellipse.width * 0.5f + layerOffsetX) * Main.UNIT_SCALE;
            float centerY = (ellipse.y + ellipse.height * 0.5f + layerOffsetY) * Main.UNIT_SCALE;
            float radiusX = ellipse.width * 0.5f * Main.UNIT_SCALE;
            float radiusY = ellipse.height * 0.5f * Main.UNIT_SCALE;
            for (int i = 0; i < segments; i++) {
                float angle = (float) (Math.PI * 2.0 * i / segments);
                worldVertices[i * 2] = centerX + (float) Math.cos(angle) * radiusX;
                worldVertices[i * 2 + 1] = centerY + (float) Math.sin(angle) * radiusY;
            }
            return BarrierShape.fromPolygon(new Polygon(worldVertices));
        }
        return null;
    }

    private static boolean isBarrierObject(MapLayer layer, MapObject object) {
        String layerName = normalize(layer == null ? null : layer.getName());
        if ("barrier".equals(layerName)) {
            return true;
        }
        String objectType = normalize(stringProperty(object, "ObjectType"));
        if ("barrier".equals(objectType)) {
            return true;
        }
        objectType = normalize(stringProperty(object, "type"));
        if ("barrier".equals(objectType)) {
            return true;
        }
        objectType = normalize(stringProperty(object, "Type"));
        if ("barrier".equals(objectType)) {
            return true;
        }
        return "barrier".equals(normalize(object == null ? null : object.getName()));
    }

    private static String stringProperty(MapObject object, String key) {
        if (object == null || key == null) {
            return null;
        }
        Object value = object.getProperties().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Rectangle getObjectBounds(MapObject mapObject) {
        if (mapObject instanceof RectangleMapObject rectangleMapObject) {
            return rectangleMapObject.getRectangle();
        }
        if (mapObject instanceof PolygonMapObject polygonMapObject) {
            return polygonMapObject.getPolygon().getBoundingRectangle();
        }
        if (mapObject instanceof CircleMapObject circleMapObject) {
            float radius = circleMapObject.getCircle().radius;
            return new Rectangle(
                    circleMapObject.getCircle().x - radius,
                    circleMapObject.getCircle().y - radius,
                    radius * 2f,
                    radius * 2f);
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            return new Rectangle(
                    ellipseMapObject.getEllipse().x,
                    ellipseMapObject.getEllipse().y,
                    ellipseMapObject.getEllipse().width,
                    ellipseMapObject.getEllipse().height);
        }
        return null;
    }

    private void setLastCollisionDebugRect(Rectangle moverHitbox, Rectangle blocker, DebugCollisionKind kind) {
        debugLastBlockedMoverHitbox.set(moverHitbox);
        debugLastBlockingRect.set(blocker);
        debugLastBlockingCircle.set(0f, 0f, 0f);
        debugLastBlockingPolygonVertices = null;
        debugLastCollisionKind = kind;
    }

    private void setLastCollisionDebugCircle(Rectangle moverHitbox, Circle blocker, DebugCollisionKind kind) {
        debugLastBlockedMoverHitbox.set(moverHitbox);
        debugLastBlockingCircle.set(blocker.x, blocker.y, blocker.radius);
        debugLastBlockingRect.set(0f, 0f, 0f, 0f);
        debugLastBlockingPolygonVertices = null;
        debugLastCollisionKind = kind;
    }

    private void setLastCollisionDebugPolygon(Rectangle moverHitbox, float[] blockerVertices, DebugCollisionKind kind) {
        debugLastBlockedMoverHitbox.set(moverHitbox);
        debugLastBlockingRect.set(0f, 0f, 0f, 0f);
        debugLastBlockingCircle.set(0f, 0f, 0f);
        debugLastBlockingPolygonVertices = blockerVertices == null ? null : blockerVertices.clone();
        debugLastCollisionKind = kind;
    }

    private void setLastCollisionDebugBarrier(Rectangle moverHitbox, BarrierShape barrier) {
        if (barrier.rect != null) {
            setLastCollisionDebugRect(moverHitbox, barrier.rect, DebugCollisionKind.BARRIER);
            return;
        }
        if (barrier.circle != null) {
            setLastCollisionDebugCircle(moverHitbox, barrier.circle, DebugCollisionKind.BARRIER);
            return;
        }
        if (barrier.polygon != null) {
            setLastCollisionDebugPolygon(
                    moverHitbox,
                    barrier.polygon.getTransformedVertices(),
                    DebugCollisionKind.BARRIER);
        }
    }

    private void clearLastCollisionDebug() {
        debugLastCollisionKind = DebugCollisionKind.NONE;
        debugLastBlockedMoverHitbox.set(0f, 0f, 0f, 0f);
        debugLastBlockingRect.set(0f, 0f, 0f, 0f);
        debugLastBlockingCircle.set(0f, 0f, 0f);
        debugLastBlockingPolygonVertices = null;
    }

    public DebugCollisionKind getDebugLastCollisionKind() {
        return debugLastCollisionKind;
    }

    public Rectangle getDebugLastBlockedMoverHitbox(Rectangle out) {
        if (out == null) {
            return new Rectangle(debugLastBlockedMoverHitbox);
        }
        return out.set(debugLastBlockedMoverHitbox);
    }

    public Rectangle getDebugLastBlockingRect(Rectangle out) {
        if (out == null) {
            return new Rectangle(debugLastBlockingRect);
        }
        return out.set(debugLastBlockingRect);
    }

    public Circle getDebugLastBlockingCircle(Circle out) {
        if (out == null) {
            return new Circle(debugLastBlockingCircle);
        }
        out.x = debugLastBlockingCircle.x;
        out.y = debugLastBlockingCircle.y;
        out.radius = debugLastBlockingCircle.radius;
        return out;
    }

    public float[] getDebugLastBlockingPolygonVertices() {
        return debugLastBlockingPolygonVertices == null ? null : debugLastBlockingPolygonVertices.clone();
    }

    public List<Rectangle> getDebugTileCollisionBoxes() {
        return Collections.unmodifiableList(debugTileCollisionBoxes);
    }

    public List<DebugShape> getDebugBarrierCollisionShapes() {
        return Collections.unmodifiableList(debugBarrierCollisionShapes);
    }

    public enum DebugCollisionKind {
        NONE,
        TILE,
        BARRIER,
        SOLID_ENTITY
    }

    public static final class DebugShape {
        private final Rectangle rect;
        private final Circle circle;
        private final float[] polygonVertices;

        private DebugShape(Rectangle rect, Circle circle, float[] polygonVertices) {
            this.rect = rect;
            this.circle = circle;
            this.polygonVertices = polygonVertices;
        }

        static DebugShape fromBarrierShape(BarrierShape shape) {
            if (shape.rect != null) {
                return new DebugShape(new Rectangle(shape.rect), null, null);
            }
            if (shape.circle != null) {
                return new DebugShape(null, new Circle(shape.circle.x, shape.circle.y, shape.circle.radius), null);
            }
            if (shape.polygon != null) {
                float[] vertices = shape.polygon.getTransformedVertices();
                return new DebugShape(null, null, vertices == null ? null : vertices.clone());
            }
            return new DebugShape(null, null, null);
        }

        public Rectangle getRect() {
            return rect;
        }

        public Circle getCircle() {
            return circle;
        }

        public float[] getPolygonVertices() {
            return polygonVertices;
        }
    }

    private static final class BarrierShape {
        private final Rectangle rect;
        private final Polygon polygon;
        private final Circle circle;

        private BarrierShape(Rectangle rect, Polygon polygon, Circle circle) {
            this.rect = rect;
            this.polygon = polygon;
            this.circle = circle;
        }

        static BarrierShape fromRectangle(Rectangle rect) {
            return new BarrierShape(rect, null, null);
        }

        static BarrierShape fromPolygon(Polygon polygon) {
            return new BarrierShape(null, polygon, null);
        }

        static BarrierShape fromCircle(Circle circle) {
            return new BarrierShape(null, null, circle);
        }

        boolean overlaps(Rectangle probe, Polygon tmpProbePolygon, float[] tmpProbeVertices) {
            if (rect != null) {
                return probe.overlaps(rect);
            }
            if (circle != null) {
                return Intersector.overlaps(circle, probe);
            }
            if (polygon != null) {
                float[] vertices = polygon.getTransformedVertices();
                if (vertices == null || vertices.length < 4) {
                    return false;
                }

                for (int i = 0; i < vertices.length; i += 2) {
                    int next = (i + 2) % vertices.length;
                    float startX = vertices[i];
                    float startY = vertices[i + 1];
                    float endX = vertices[next];
                    float endY = vertices[next + 1];
                    if (segmentIntersectsRectangle(startX, startY, endX, endY, probe)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private static boolean segmentIntersectsRectangle(
                float startX,
                float startY,
                float endX,
                float endY,
                Rectangle rect
        ) {
            if (rect.contains(startX, startY) || rect.contains(endX, endY)) {
                return true;
            }

            float minX = rect.x;
            float minY = rect.y;
            float maxX = rect.x + rect.width;
            float maxY = rect.y + rect.height;

            return Intersector.intersectSegments(startX, startY, endX, endY, minX, minY, maxX, minY, null)
                    || Intersector.intersectSegments(startX, startY, endX, endY, maxX, minY, maxX, maxY, null)
                    || Intersector.intersectSegments(startX, startY, endX, endY, maxX, maxY, minX, maxY, null)
                    || Intersector.intersectSegments(startX, startY, endX, endY, minX, maxY, minX, minY, null);
        }
    }

    private static float hitboxWidthFactorFor(Entity mover) {
        return Enemy.MAPPER.get(mover) != null ? ENEMY_HITBOX_WIDTH_FACTOR : HITBOX_WIDTH_FACTOR;
    }

    private static float hitboxHeightFactorFor(Entity mover) {
        return Enemy.MAPPER.get(mover) != null ? ENEMY_HITBOX_HEIGHT_FACTOR : HITBOX_HEIGHT_FACTOR;
    }

    private static float hitboxYOffsetFactorFor(Entity mover) {
        return Enemy.MAPPER.get(mover) != null ? 0f : HITBOX_Y_OFFSET_FACTOR;
    }
}
