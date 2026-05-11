package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.FieldTrainerWalkSprite;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.MapTransitionZone;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Prop;
import github.dluckycompany.clawkins.component.Tiled;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterTrigger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Renders the tiled map and all entities that have [Transform + Graphic].
 *
 * Render order per frame:
 * 1. Apply viewport
 * 2. Draw tiled map background layers (everything before the "objects" layer,
 *    excluding the "elements" layer)
 * 3. Draw the "elements" layer interleaved with entities using Y-sorting so
 *    that entities appear in front of or behind elements tiles depending on
 *    their world Y position (RPG top-down convention).
 *    If the map has no "elements" layer, entities are drawn as a plain
 *    Y-sorted pass (original behaviour).
 * 4. Draw tiled map foreground layers (everything after the "objects" layer)
 *
 * This system OWNS the OrthogonalTiledMapRenderer and is Disposable.
 */
public class RenderSystem extends SortedIteratingSystem implements Disposable {
    private static final float PLAYER_HITBOX_WIDTH_FACTOR = 0.25f;
    private static final float PLAYER_HITBOX_HEIGHT_FACTOR = 0.25f;
    private static final float PLAYER_HITBOX_Y_OFFSET_FACTOR = 0.16f;
    private static final float ENEMY_HITBOX_WIDTH_FACTOR = 0.28f;
    private static final float ENEMY_HITBOX_HEIGHT_FACTOR = 0.24f;
    private static final float TILE_HITBOX_WIDTH_FACTOR = 1f;
    private static final float TILE_HITBOX_HEIGHT_FACTOR = 1f;
    private static final float SOLID_HITBOX_WIDTH_FACTOR = 1f;
    private static final float SOLID_HITBOX_HEIGHT_FACTOR = 1f;
    private static final int ALERT_ICON_FRAME_PX = 16;
    private static final float ALERT_ICON_FRAME_DURATION = 0.12f;
    private static final float ALERT_ICON_WORLD_SIZE = 16f * Main.UNIT_SCALE;
    private static final float ALERT_ICON_OFFSET_Y = 2f * Main.UNIT_SCALE;
    /** Player alert sits lower (toward the head) than enemy alerts; one frame in world units. */
    private static final float PLAYER_ALERT_ICON_EXTRA_Y = -16f * Main.UNIT_SCALE;
    /** Trainer-tall sprites: nudge alert icon down by one 16px frame in world units. */
    private static final float TRAINER_ENEMY_ALERT_ICON_EXTRA_Y = -16f * Main.UNIT_SCALE;
    private static final float RANDOM_ENCOUNTER_PLAYER_ALERT_DURATION = 0.45f;

    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final OrthogonalTiledMapRenderer tiledRenderer;
    private final List<MapLayer> bgdLayers;
    private final List<MapLayer> fgdLayers;

    // -- NEW: extracted "elements" layer for Y-sort interleaving --
    private TiledMapTileLayer elementsLayer;
    private final List<ElementTile> elementTiles = new ArrayList<>();
    private final ShapeRenderer debugShapeRenderer;
    private final List<Rectangle> debugTileCollisionBoxes;
    private final List<BarrierShape> debugBarrierBoxes;
    private final Rectangle tmpDebugRect;
    private final Rectangle tmpBlockedMoverHitboxRect;
    private final Rectangle tmpBlockingRect;
    private final Circle tmpBlockingCircle;
    private boolean debugRenderingEnabled;
    private Texture alertTexture;
    private Animation<TextureRegion> alertIconAnimation;
    private float alertIconAnimTime;
    private float playerAlertTimer;

    private static class ElementTile implements Comparable<ElementTile> {
        float x, y;
        float sortY;
        TiledMapTile tile;
        boolean flipX, flipY;
        int rotation;

        @Override
        public int compareTo(ElementTile other) {
            return Float.compare(other.sortY, this.sortY);
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

        void draw(ShapeRenderer renderer) {
            if (rect != null) {
                renderer.rect(rect.x, rect.y, rect.width, rect.height);
                return;
            }
            if (circle != null) {
                renderer.circle(circle.x, circle.y, circle.radius, 20);
                return;
            }
            if (polygon != null) {
                renderer.polygon(polygon.getTransformedVertices());
            }
        }
    }

    public RenderSystem(Batch batch, Viewport viewport, OrthographicCamera camera) {
        super(
                Family.all(Transform.class, Graphic.class).get(),
                Comparator.comparing(Transform.MAPPER::get));

        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
        this.tiledRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, batch);
        this.bgdLayers = new ArrayList<>();
        this.fgdLayers = new ArrayList<>();
        this.debugShapeRenderer = new ShapeRenderer();
        this.debugTileCollisionBoxes = new ArrayList<>();
        this.debugBarrierBoxes = new ArrayList<>();
        this.tmpDebugRect = new Rectangle();
        this.tmpBlockedMoverHitboxRect = new Rectangle();
        this.tmpBlockingRect = new Rectangle();
        this.tmpBlockingCircle = new Circle();
        this.debugRenderingEnabled = false;
        this.playerAlertTimer = 0f;
        loadAlertIconTexture();
    }

    @Override
    public void update(float deltaTime) {
        alertIconAnimTime += deltaTime;
        playerAlertTimer = Math.max(0f, playerAlertTimer - deltaTime);
        AnimatedTiledMapTile.updateAnimationBaseTime();
        viewport.apply();

        batch.begin();
        batch.setColor(Color.WHITE);
        tiledRenderer.setView(camera);

        // Draw background map layers (unchanged)
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        forceSort();

        // -- CHANGED: if an "elements" layer exists, interleave it with entities;
        //    otherwise fall back to the original plain entity pass. --
        if (elementsLayer != null) {
            renderElementsWithEntityYSort(deltaTime);
        } else {
            super.update(deltaTime);
        }

        // Draw foreground map layers (unchanged)
        batch.setColor(Color.WHITE);
        fgdLayers.forEach(tiledRenderer::renderMapLayer);
        renderAlertIcons();
        batch.end();
        if (debugRenderingEnabled) {
            renderDebugOverlay();
        }
    }

    // -------------------------------------------------------------------------
    // Original processEntity — completely unchanged
    // -------------------------------------------------------------------------

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        Graphic graphic = Graphic.MAPPER.get(entity);
        TextureRegion region = graphic.getRegion();
        if (Prop.MAPPER.get(entity) != null) {
            region = resolveAnimatedPropRegion(entity, region);
        }
        if (region == null) {
            return;
        }

        Vector2 position = transform.getPosition();
        Vector2 scaling = transform.getScaling();
        Vector2 size = transform.getSize();

        batch.setColor(graphic.getColor());
        batch.draw(
                region,
                position.x - (1f - scaling.x) * size.x * 0.5f,
                position.y - (1f - scaling.y) * size.y * 0.5f,
                size.x * 0.5f, size.y * 0.5f,
                size.x, size.y,
                scaling.x, scaling.y,
                transform.getRotationDeg());
    }

    private TextureRegion resolveAnimatedPropRegion(Entity entity, TextureRegion fallbackRegion) {
        Tiled tiled = Tiled.MAPPER.get(entity);
        if (tiled == null) {
            return fallbackRegion;
        }
        if (!(tiled.getMapObjectRef() instanceof TiledMapTileMapObject tileMapObject)) {
            return fallbackRegion;
        }
        TiledMapTile tile = tileMapObject.getTile();
        if (tile == null) {
            return fallbackRegion;
        }
        TextureRegion currentRegion = tile.getTextureRegion();
        return currentRegion != null ? currentRegion : fallbackRegion;
    }

    // -------------------------------------------------------------------------
    // NEW: Y-sort interleaved render for the "elements" layer
    // -------------------------------------------------------------------------

    private void renderElementsWithEntityYSort(float deltaTime) {
        float opacity = elementsLayer != null ? elementsLayer.getOpacity() : 1f;

        ImmutableArray<Entity> sorted = getEntities();
        int entityIdx = 0;
        int entityCount = sorted.size();
        int tileIdx = 0;
        int tileCount = elementTiles.size();

        while (entityIdx < entityCount || tileIdx < tileCount) {
            float entityY = entityIdx < entityCount 
                ? Transform.MAPPER.get(sorted.get(entityIdx)).getPosition().y 
                : -Float.MAX_VALUE;
            float tileY = tileIdx < tileCount 
                ? elementTiles.get(tileIdx).sortY 
                : -Float.MAX_VALUE;

            if (tileY >= entityY) { // tile is further away
                ElementTile t = elementTiles.get(tileIdx);
                batch.setColor(1f, 1f, 1f, opacity);
                
                TextureRegion region = t.tile.getTextureRegion();
                if (region != null) {
                    TextureRegion reg = new TextureRegion(region);
                    if (t.flipX) reg.flip(true, false);
                    if (t.flipY) reg.flip(false, true);

                    float regionWidth = region.getRegionWidth() * Main.UNIT_SCALE;
                    float regionHeight = region.getRegionHeight() * Main.UNIT_SCALE;

                    if (t.rotation == TiledMapTileLayer.Cell.ROTATE_90) {
                        reg.flip(false, true);
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 90f);
                    } else if (t.rotation == TiledMapTileLayer.Cell.ROTATE_180) {
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 180f);
                    } else if (t.rotation == TiledMapTileLayer.Cell.ROTATE_270) {
                        reg.flip(false, true);
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 270f);
                    } else {
                        batch.draw(reg, t.x, t.y, regionWidth, regionHeight);
                    }
                }
                tileIdx++;
            } else {
                Entity e = sorted.get(entityIdx);
                batch.setColor(Color.WHITE);
                processEntity(e, deltaTime);
                entityIdx++;
            }
        }
        batch.setColor(Color.WHITE);
    }

    // -------------------------------------------------------------------------
    // setMap — original logic + extraction of the "elements" layer
    // -------------------------------------------------------------------------

    /**
     * Sets the tiled map and sorts its layers into background vs foreground.
     * Everything before the "objects" layer is background; everything after is
     * foreground. The "elements" layer (if present) is extracted separately so
     * it can be Y-sorted with entities — it is NOT added to bgdLayers.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);

        bgdLayers.clear();
        fgdLayers.clear();
        elementsLayer = null;                  // reset each map load
        elementTiles.clear();
        rebuildMapDebugData(tiledMap);

        List<MapLayer> currentLayers = bgdLayers;
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                currentLayers = fgdLayers;
                continue;
            }
            if (layer.getClass().equals(MapLayer.class)) {
                continue;
            }
            // -- NEW: intercept the "elements" layer before it goes into bgdLayers --
            if ("elements".equalsIgnoreCase(layer.getName())
                    && layer instanceof TiledMapTileLayer) {
                elementsLayer = (TiledMapTileLayer) layer;
                continue;
            }
            currentLayers.add(layer);
        }

        if (elementsLayer != null) {
            int mapCols = elementsLayer.getWidth();
            int mapRows = elementsLayer.getHeight();
            float scaledTileW = elementsLayer.getTileWidth() * Main.UNIT_SCALE;
            float scaledTileH = elementsLayer.getTileHeight() * Main.UNIT_SCALE;
            float layerOffsetX = elementsLayer.getOffsetX() * Main.UNIT_SCALE;
            float layerOffsetY = elementsLayer.getOffsetY() * Main.UNIT_SCALE;

            for (int row = 0; row < mapRows; row++) {
                for (int col = 0; col < mapCols; col++) {
                    TiledMapTileLayer.Cell cell = elementsLayer.getCell(col, row);
                    if (cell == null) continue;
                    TiledMapTile tile = cell.getTile();
                    if (tile == null) continue;

                    ElementTile et = new ElementTile();
                    et.tile = tile;
                    et.x = col * scaledTileW + layerOffsetX + tile.getOffsetX() * Main.UNIT_SCALE;
                    et.y = row * scaledTileH + layerOffsetY + tile.getOffsetY() * Main.UNIT_SCALE;
                    et.flipX = cell.getFlipHorizontally();
                    et.flipY = cell.getFlipVertically();
                    et.rotation = cell.getRotation();

                    float anchorY = 0;
                    boolean hasCollision = false;
                    MapObjects objects = tile.getObjects();
                    if (objects != null) {
                        for (MapObject mapObject : objects) {
                            if (mapObject instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) mapObject).getRectangle();
                                anchorY = rect.y * Main.UNIT_SCALE;
                                hasCollision = true;
                                break;
                            } else if (mapObject instanceof PolygonMapObject) {
                                Rectangle rect = ((PolygonMapObject) mapObject).getPolygon().getBoundingRectangle();
                                anchorY = rect.y * Main.UNIT_SCALE;
                                hasCollision = true;
                                break;
                            }
                        }
                    }
                    if (hasCollision) {
                        et.sortY = et.y + anchorY;
                    } else {
                        // Render above the player. Keep relative et.y so overlapping non-collision tiles sort correctly amongst themselves.
                        et.sortY = -1000000f + et.y;
                    }
                    elementTiles.add(et);
                }
            }
            java.util.Collections.sort(elementTiles);
        }
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
        debugShapeRenderer.dispose();
        if (alertTexture != null) {
            alertTexture.dispose();
            alertTexture = null;
            alertIconAnimation = null;
        }
    }

    public void triggerRandomEncounterPlayerAlert() {
        triggerPlayerAlert(RANDOM_ENCOUNTER_PLAYER_ALERT_DURATION);
    }

    public void triggerPlayerAlert(float durationSeconds) {
        playerAlertTimer = Math.max(playerAlertTimer, Math.max(0f, durationSeconds));
    }

    public void setDebugRenderingEnabled(boolean debugRenderingEnabled) {
        this.debugRenderingEnabled = debugRenderingEnabled;
    }

    private void renderDebugOverlay() {
        viewport.apply();
        debugShapeRenderer.setProjectionMatrix(camera.combined);
        debugShapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        drawMapCollisionBoxes();
        drawPlayerDebug();
        drawLastBlockingCollisionDebug();
        drawEnemyDebug();
        drawInteractibleDebug();
        drawEncounterTriggerDebug();
        drawMapTransitionDebug();
        drawInvisibleTiledObjectDebug();

        debugShapeRenderer.end();
    }

    private void drawMapCollisionBoxes() {
        MoveSystem moveSystem = getEngine() == null ? null : getEngine().getSystem(MoveSystem.class);
        if (moveSystem != null) {
            debugShapeRenderer.setColor(1f, 0.95f, 0.2f, 1f);
            for (Rectangle rect : moveSystem.getDebugTileCollisionBoxes()) {
                debugShapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
            }

            debugShapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
            for (MoveSystem.DebugShape shape : moveSystem.getDebugBarrierCollisionShapes()) {
                Rectangle rect = shape.getRect();
                if (rect != null) {
                    debugShapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
                    continue;
                }
                Circle circle = shape.getCircle();
                if (circle != null) {
                    debugShapeRenderer.circle(circle.x, circle.y, circle.radius, 20);
                    continue;
                }
                float[] polygon = shape.getPolygonVertices();
                if (polygon != null && polygon.length >= 6) {
                    debugShapeRenderer.polygon(polygon);
                }
            }
            return;
        }

        debugShapeRenderer.setColor(1f, 0.95f, 0.2f, 1f);
        for (Rectangle rect : debugTileCollisionBoxes) {
            debugShapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        debugShapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
        for (BarrierShape rect : debugBarrierBoxes) {
            rect.draw(debugShapeRenderer);
        }
    }

    private void drawPlayerDebug() {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(Player.class, Transform.class).get());
        for (Entity player : players) {
            Transform transform = Transform.MAPPER.get(player);
            if (transform == null) {
                continue;
            }
            Rectangle bodyRect = tmpDebugRect.set(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getSize().x,
                    transform.getSize().y);
            debugShapeRenderer.setColor(0.30f, 0.9f, 1f, 1f);
            debugShapeRenderer.rect(bodyRect.x, bodyRect.y, bodyRect.width, bodyRect.height);

            Rectangle hitboxRect = computeBottomHitbox(
                    transform,
                    PLAYER_HITBOX_WIDTH_FACTOR,
                    PLAYER_HITBOX_HEIGHT_FACTOR,
                    PLAYER_HITBOX_Y_OFFSET_FACTOR);
            debugShapeRenderer.setColor(0.15f, 1f, 0.35f, 1f);
            debugShapeRenderer.rect(hitboxRect.x, hitboxRect.y, hitboxRect.width, hitboxRect.height);
        }
    }

    private void drawLastBlockingCollisionDebug() {
        if (getEngine() == null) {
            return;
        }
        MoveSystem moveSystem = getEngine().getSystem(MoveSystem.class);
        if (moveSystem == null) {
            return;
        }
        MoveSystem.DebugCollisionKind kind = moveSystem.getDebugLastCollisionKind();
        if (kind == MoveSystem.DebugCollisionKind.NONE) {
            return;
        }

        // Cyan = live player movement hitbox (same position as green box logic).
        Rectangle blockedMover = null;
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(Player.class, Transform.class).get());
        if (players != null && players.size() > 0) {
            Transform playerTransform = Transform.MAPPER.get(players.first());
            if (playerTransform != null) {
                blockedMover = computeBottomHitbox(
                        playerTransform,
                        PLAYER_HITBOX_WIDTH_FACTOR,
                        PLAYER_HITBOX_HEIGHT_FACTOR,
                        PLAYER_HITBOX_Y_OFFSET_FACTOR);
            }
        }
        if (blockedMover == null) {
            blockedMover = moveSystem.getDebugLastBlockedMoverHitbox(tmpBlockedMoverHitboxRect);
        }
        debugShapeRenderer.setColor(0.05f, 1f, 1f, 1f);
        debugShapeRenderer.rect(blockedMover.x, blockedMover.y, blockedMover.width, blockedMover.height);

        // White = exact collider that blocked movement.
        if (kind == MoveSystem.DebugCollisionKind.TILE || kind == MoveSystem.DebugCollisionKind.SOLID_ENTITY) {
            Rectangle blocker = moveSystem.getDebugLastBlockingRect(tmpBlockingRect);
            debugShapeRenderer.setColor(1f, 1f, 1f, 1f);
            debugShapeRenderer.rect(blocker.x, blocker.y, blocker.width, blocker.height);
            return;
        }

        Rectangle blockerRect = moveSystem.getDebugLastBlockingRect(tmpBlockingRect);
        if (blockerRect.width > 0f && blockerRect.height > 0f) {
            debugShapeRenderer.setColor(1f, 1f, 1f, 1f);
            debugShapeRenderer.rect(blockerRect.x, blockerRect.y, blockerRect.width, blockerRect.height);
            return;
        }

        Circle blockerCircle = moveSystem.getDebugLastBlockingCircle(tmpBlockingCircle);
        if (blockerCircle.radius > 0f) {
            debugShapeRenderer.setColor(1f, 1f, 1f, 1f);
            debugShapeRenderer.circle(blockerCircle.x, blockerCircle.y, blockerCircle.radius, 20);
            return;
        }

        float[] blockerPolygon = moveSystem.getDebugLastBlockingPolygonVertices();
        if (blockerPolygon != null && blockerPolygon.length >= 6) {
            debugShapeRenderer.setColor(1f, 1f, 1f, 1f);
            debugShapeRenderer.polygon(blockerPolygon);
        }
    }

    private void drawEnemyDebug() {
        ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(Family.all(Enemy.class, Transform.class).get());
        for (Entity enemy : enemies) {
            Transform transform = Transform.MAPPER.get(enemy);
            if (transform == null) {
                continue;
            }

            debugShapeRenderer.setColor(1f, 0.5f, 0.15f, 1f);
            debugShapeRenderer.rect(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getSize().x,
                    transform.getSize().y);

            Rectangle hitboxRect = computeBottomHitbox(transform, ENEMY_HITBOX_WIDTH_FACTOR, ENEMY_HITBOX_HEIGHT_FACTOR);
            debugShapeRenderer.setColor(1f, 0.15f, 0.15f, 1f);
            debugShapeRenderer.rect(hitboxRect.x, hitboxRect.y, hitboxRect.width, hitboxRect.height);
        }
    }

    private void drawInteractibleDebug() {
        ImmutableArray<Entity> interactibles = getEngine().getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
        for (Entity entity : interactibles) {
            Transform transform = Transform.MAPPER.get(entity);
            Interactible interactible = Interactible.MAPPER.get(entity);
            if (transform == null || interactible == null) {
                continue;
            }

            debugShapeRenderer.setColor(0.95f, 0.35f, 1f, 1f);
            debugShapeRenderer.rect(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getSize().x,
                    transform.getSize().y);

            if (interactible.hasCollision()) {
                Rectangle collisionRect = computeBottomHitbox(transform, SOLID_HITBOX_WIDTH_FACTOR, SOLID_HITBOX_HEIGHT_FACTOR);
                debugShapeRenderer.setColor(0.8f, 0.2f, 0.9f, 1f);
                debugShapeRenderer.rect(collisionRect.x, collisionRect.y, collisionRect.width, collisionRect.height);
            }
        }
    }

    private void drawEncounterTriggerDebug() {
        ImmutableArray<Entity> triggers = getEngine().getEntitiesFor(Family.all(EncounterTrigger.class, Transform.class).get());
        debugShapeRenderer.setColor(0.15f, 0.35f, 1f, 1f);
        for (Entity trigger : triggers) {
            Transform transform = Transform.MAPPER.get(trigger);
            if (transform == null) {
                continue;
            }
            debugShapeRenderer.rect(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getSize().x,
                    transform.getSize().y);
        }
    }

    private void drawMapTransitionDebug() {
        ImmutableArray<Entity> transitions = getEngine().getEntitiesFor(Family.all(MapTransitionZone.class).get());
        debugShapeRenderer.setColor(0.95f, 0.95f, 0.95f, 1f);
        for (Entity transition : transitions) {
            MapTransitionZone zone = MapTransitionZone.MAPPER.get(transition);
            if (zone == null || zone.getWorldBounds() == null) {
                continue;
            }
            Rectangle bounds = zone.getWorldBounds();
            debugShapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void drawInvisibleTiledObjectDebug() {
        ImmutableArray<Entity> invisibleTiledEntities = getEngine().getEntitiesFor(
                Family.all(Tiled.class, Transform.class).exclude(Graphic.class).get());
        debugShapeRenderer.setColor(0.95f, 0.95f, 0.95f, 1f);
        for (Entity entity : invisibleTiledEntities) {
            Transform transform = Transform.MAPPER.get(entity);
            if (transform == null) {
                continue;
            }
            debugShapeRenderer.rect(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getSize().x,
                    transform.getSize().y);
        }
    }

    private Rectangle computeBottomHitbox(Transform transform, float widthFactor, float heightFactor) {
        return computeBottomHitbox(transform, widthFactor, heightFactor, 0f);
    }

    private Rectangle computeBottomHitbox(Transform transform, float widthFactor, float heightFactor, float yOffsetFactor) {
        float width = transform.getSize().x;
        float height = transform.getSize().y;
        float hitboxWidth = width * widthFactor;
        float hitboxHeight = height * heightFactor;
        float hitboxX = transform.getPosition().x + (width - hitboxWidth) * 0.5f;
        float hitboxY = transform.getPosition().y + height * yOffsetFactor;
        return tmpDebugRect.set(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    private void rebuildMapDebugData(TiledMap tiledMap) {
        debugTileCollisionBoxes.clear();
        debugBarrierBoxes.clear();
        if (tiledMap == null) {
            return;
        }

        for (MapLayer layer : tiledMap.getLayers()) {
            collectBarrierObjects(layer);
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }
            collectTileCollisionObjects(tileLayer);
        }
    }

    private void loadAlertIconTexture() {
        try {
            alertTexture = new Texture("ui/alert.png");
            TextureRegion[][] grid = TextureRegion.split(alertTexture, ALERT_ICON_FRAME_PX, ALERT_ICON_FRAME_PX);
            if (grid.length == 0 || grid[0].length == 0) {
                alertTexture.dispose();
                alertTexture = null;
                alertIconAnimation = null;
                return;
            }
            alertIconAnimation = new Animation<>(ALERT_ICON_FRAME_DURATION, grid[0]);
            alertIconAnimation.setPlayMode(Animation.PlayMode.LOOP);
        } catch (Exception ignored) {
            alertTexture = null;
            alertIconAnimation = null;
        }
    }

    private void renderAlertIcons() {
        if (alertIconAnimation == null || getEngine() == null) {
            return;
        }

        batch.setColor(Color.WHITE);
        ImmutableArray<Entity> enemies = getEngine().getEntitiesFor(Family.all(Enemy.class, Transform.class).get());
        for (Entity enemyEntity : enemies) {
            Enemy enemy = Enemy.MAPPER.get(enemyEntity);
            Transform transform = Transform.MAPPER.get(enemyEntity);
            if (enemy == null || transform == null || enemy.getState() != Enemy.State.ALERTED) {
                continue;
            }
            float extraY = FieldTrainerWalkSprite.MAPPER.get(enemyEntity) != null
                    ? TRAINER_ENEMY_ALERT_ICON_EXTRA_Y
                    : 0f;
            drawAlertIconAbove(transform, extraY);
        }

        if (playerAlertTimer > 0f) {
            ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(Player.class, Transform.class).get());
            if (players != null && players.size() > 0) {
                Transform playerTransform = Transform.MAPPER.get(players.first());
                if (playerTransform != null) {
                    drawAlertIconAbove(playerTransform, PLAYER_ALERT_ICON_EXTRA_Y);
                }
            }
        }
    }

    private void drawAlertIconAbove(Transform transform, float extraY) {
        Vector2 pos = transform.getPosition();
        Vector2 size = transform.getSize();
        float drawX = pos.x + (size.x - ALERT_ICON_WORLD_SIZE) * 0.5f;
        float drawY = pos.y + size.y + ALERT_ICON_OFFSET_Y + extraY;
        TextureRegion frame = alertIconAnimation.getKeyFrame(alertIconAnimTime, true);
        batch.draw(frame, drawX, drawY, ALERT_ICON_WORLD_SIZE, ALERT_ICON_WORLD_SIZE);
    }

    private void collectTileCollisionObjects(TiledMapTileLayer layer) {
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
                    Rectangle localRect = getObjectBounds(mapObject);
                    if (localRect == null || localRect.width <= 0f || localRect.height <= 0f) {
                        continue;
                    }
                    float tileOffsetX = tile.getOffsetX() * Main.UNIT_SCALE;
                    float tileOffsetY = tile.getOffsetY() * Main.UNIT_SCALE;

                    float rectX = col * tileWorldW + layerOffsetX + tileOffsetX + localRect.x * Main.UNIT_SCALE;
                    float rectY = row * tileWorldH + layerOffsetY + tileOffsetY + localRect.y * Main.UNIT_SCALE;
                    float rectW = localRect.width * Main.UNIT_SCALE;
                    float rectH = localRect.height * Main.UNIT_SCALE;
                    float hitboxW = rectW * TILE_HITBOX_WIDTH_FACTOR;
                    float hitboxH = rectH * TILE_HITBOX_HEIGHT_FACTOR;
                    float hitboxX = rectX + (rectW - hitboxW) * 0.5f;
                    float hitboxY = rectY;
                    debugTileCollisionBoxes.add(new Rectangle(hitboxX, hitboxY, hitboxW, hitboxH));
                }
            }
        }
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
                debugBarrierBoxes.add(barrierShape);
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
}
