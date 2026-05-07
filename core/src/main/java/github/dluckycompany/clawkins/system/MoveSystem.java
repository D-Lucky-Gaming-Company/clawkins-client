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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Transform;

import java.util.ArrayList;
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
    private static final float HITBOX_WIDTH_FACTOR = 0.26f;
    private static final float HITBOX_HEIGHT_FACTOR = 0.22f;
    private static final float ENEMY_HITBOX_WIDTH_FACTOR = 0.28f;
    private static final float ENEMY_HITBOX_HEIGHT_FACTOR = 0.24f;
    private static final float TILE_HITBOX_WIDTH_FACTOR = 0.80f;
    private static final float TILE_HITBOX_HEIGHT_FACTOR = 0.45f;
    private static final float SOLID_HITBOX_WIDTH_FACTOR = 0.80f;
    private static final float SOLID_HITBOX_HEIGHT_FACTOR = 0.45f;

    private float mapWidth;
    private float mapHeight;
    private final List<TiledMapTileLayer> collisionLayers;
    private final List<MapObject> barrierObjects;
    private final Rectangle tmpEntityHitbox;
    private final Rectangle tmpTileRect;
    private final Rectangle tmpSolidRect;
    private ImmutableArray<Entity> solidInteractibles;

    public MoveSystem() {
        super(Family.all(Move.class, Transform.class).get());
        this.collisionLayers = new ArrayList<>();
        this.barrierObjects = new ArrayList<>();
        this.tmpEntityHitbox = new Rectangle();
        this.tmpTileRect = new Rectangle();
        this.tmpSolidRect = new Rectangle();
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.solidInteractibles = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
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
        if (!isBlocked(targetX, position.y, size.x, size.y, entity)) {
            position.x = targetX;
        }

        float targetY = clampY(position.y + moveY, size.y, entity);
        if (!isBlocked(position.x, targetY, size.x, size.y, entity)) {
            position.y = targetY;
        }
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
        this.barrierObjects.clear();
        for (MapLayer layer : tiledMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer tileLayer) {
                // Tile collision objects should block regardless of visual layer placement.
                this.collisionLayers.add(tileLayer);
            }
            if (layer.getObjects() != null) {
                for (MapObject mapObject : layer.getObjects()) {
                    if ("barrier".equalsIgnoreCase(layer.getName()) || isBarrierObject(mapObject)) {
                        barrierObjects.add(mapObject);
                    }
                }
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
        Rectangle entityHitbox = buildHitbox(x, y, width, height, mover, tmpEntityHitbox);

        if (isBlockedByMap(entityHitbox)) {
            return true;
        }
        if (isBlockedByBarrierShape(entityHitbox)) {
            return true;
        }
        return isBlockedBySolidEntity(entityHitbox, mover);
    }

    private boolean isBlockedByMap(Rectangle entityHitbox) {
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
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlockedBySolidEntity(Rectangle entityHitbox, Entity mover) {
        return isBlockedBySolidInteractible(entityHitbox, mover);
    }

    private boolean isBlockedBySolidInteractible(Rectangle entityHitbox, Entity mover) {
        if (solidInteractibles == null || solidInteractibles.size() == 0) {
            return false;
        }
        for (Entity entity : solidInteractibles) {
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
                return true;
            }
        }
        return false;
    }

    private boolean isBlockedByBarrierShape(Rectangle entityHitbox) {
        if (barrierObjects.isEmpty()) {
            return false;
        }

        float probeCenterX = (entityHitbox.x + entityHitbox.width * 0.5f) / Main.UNIT_SCALE;
        float probeBottomY = entityHitbox.y / Main.UNIT_SCALE;
        float probeLeftX = (entityHitbox.x + entityHitbox.width * 0.2f) / Main.UNIT_SCALE;
        float probeRightX = (entityHitbox.x + entityHitbox.width * 0.8f) / Main.UNIT_SCALE;
        float probeMidY = (entityHitbox.y + entityHitbox.height * 0.5f) / Main.UNIT_SCALE;

        for (MapObject barrierObject : barrierObjects) {
            if (containsPoint(barrierObject, probeCenterX, probeBottomY)
                    || containsPoint(barrierObject, probeLeftX, probeBottomY)
                    || containsPoint(barrierObject, probeRightX, probeBottomY)
                    || containsPoint(barrierObject, probeCenterX, probeMidY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPoint(MapObject mapObject, float x, float y) {
        if (mapObject instanceof PolygonMapObject polygonMapObject) {
            return polygonMapObject.getPolygon().contains(x, y);
        }
        if (mapObject instanceof RectangleMapObject rectangleMapObject) {
            return rectangleMapObject.getRectangle().contains(x, y);
        }
        if (mapObject instanceof CircleMapObject circleMapObject) {
            return circleMapObject.getCircle().contains(x, y);
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            return ellipseMapObject.getEllipse().contains(x, y);
        }
        return false;
    }

    private static boolean isBarrierObject(MapObject mapObject) {
        String objectType = readObjectType(mapObject);
        if (objectType == null || objectType.isBlank()) {
            return false;
        }
        return "BARRIER".equals(objectType.trim().toUpperCase(Locale.ROOT));
    }

    private static String readObjectType(MapObject mapObject) {
        if (mapObject == null || mapObject.getProperties() == null) {
            return "";
        }
        Object objectType = mapObject.getProperties().get("ObjectType");
        if (objectType != null) {
            return String.valueOf(objectType);
        }
        Object tiledType = mapObject.getProperties().get("type");
        if (tiledType != null) {
            return String.valueOf(tiledType);
        }
        Object tiledTypePascal = mapObject.getProperties().get("Type");
        if (tiledTypePascal != null) {
            return String.valueOf(tiledTypePascal);
        }
        Object tiledClass = mapObject.getProperties().get("class");
        if (tiledClass != null) {
            return String.valueOf(tiledClass);
        }
        Object tiledClassPascal = mapObject.getProperties().get("Class");
        if (tiledClassPascal != null) {
            return String.valueOf(tiledClassPascal);
        }
        return "";
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
        float minY = 0f;
        float maxY = mapHeight - hitboxHeight;
        if (maxY < minY) {
            return minY;
        }
        return Math.max(minY, Math.min(maxY, y));
    }

    private Rectangle buildHitbox(float x, float y, float width, float height, Entity mover, Rectangle outRect) {
        float hitboxWidth = width * hitboxWidthFactorFor(mover);
        float hitboxHeight = height * hitboxHeightFactorFor(mover);
        float hitboxX = x + (width - hitboxWidth) * 0.5f;
        float hitboxY = y;
        return outRect.set(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    private static float hitboxWidthFactorFor(Entity mover) {
        return Enemy.MAPPER.get(mover) != null ? ENEMY_HITBOX_WIDTH_FACTOR : HITBOX_WIDTH_FACTOR;
    }

    private static float hitboxHeightFactorFor(Entity mover) {
        return Enemy.MAPPER.get(mover) != null ? ENEMY_HITBOX_HEIGHT_FACTOR : HITBOX_HEIGHT_FACTOR;
    }
}
