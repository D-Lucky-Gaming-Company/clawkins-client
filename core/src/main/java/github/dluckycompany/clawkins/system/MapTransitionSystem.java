package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.MapTransitionZone;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;

import java.util.function.BiConsumer;

/**
 * Detects when the player overlaps a map transition zone and fires a callback.
 * Uses enter-once semantics: the transition only fires on first overlap,
 * not every frame while standing inside.
 *
 * Also responsible for parsing trigger layers on map change and creating
 * transition zone entities.
 */
public class MapTransitionSystem extends EntitySystem {
    private static final String TAG = MapTransitionSystem.class.getSimpleName();
    private static final float PLAYER_HITBOX_WIDTH_FACTOR = 0.32f;
    private static final float PLAYER_HITBOX_HEIGHT_FACTOR = 0.30f;
    private static final float PLAYER_HITBOX_Y_OFFSET_FACTOR = 0.10f;
    private static final String OBJECT_TYPE_KEY = "ObjectType";
    private static final String MAP_TRANSITION_VALUE = "MAP_TRANSITION";

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> transitionZones;
    private Entity currentZone;
    private float cooldownTimer;
    private boolean requiresZoneExitBeforeTrigger;
    private BiConsumer<String, String> transitionCallback;

    private final Rectangle playerRect = new Rectangle();
    private final Rectangle zoneRect = new Rectangle();

    public void setTransitionCallback(BiConsumer<String, String> callback) {
        this.transitionCallback = callback;
    }

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(Player.class, Transform.class).get());
        transitionZones = engine.getEntitiesFor(Family.all(MapTransitionZone.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (cooldownTimer > 0) {
            cooldownTimer -= deltaTime;
            if (cooldownTimer > 0f) {
                return;
            }
        }

        if (players == null || players.size() == 0
            || transitionZones == null || transitionZones.size() == 0) {
            currentZone = null;
            return;
        }

        Transform playerTransform = Transform.MAPPER.get(players.first());
        Rectangle playerHitbox = buildPlayerHitbox(playerTransform, playerRect);

        Entity overlapping = null;
        for (Entity zoneEntity : transitionZones) {
            MapTransitionZone zone = MapTransitionZone.MAPPER.get(zoneEntity);
            zoneRect.set(zone.getWorldBounds());
            if (zoneRect.overlaps(playerHitbox)) {
                overlapping = zoneEntity;
                break;
            }
        }

        // After a spawn/transition, require the player to exit transition zones once
        // before any new transition can trigger.
        if (requiresZoneExitBeforeTrigger) {
            if (overlapping == null) {
                requiresZoneExitBeforeTrigger = false;
                currentZone = null;
            }
            return;
        }

        if (overlapping == null) {
            currentZone = null;
            return;
        }

        if (overlapping == currentZone) {
            return;
        }

        currentZone = overlapping;
        MapTransitionZone zone = MapTransitionZone.MAPPER.get(overlapping);

        if (zone.getTargetMap() == null || zone.getTargetMap().isBlank()) {
            Gdx.app.error(TAG, "Transition zone '" + zone.getTransitionId() + "' has no targetMap, aborting");
            return;
        }

        Gdx.app.log(TAG, "Transition triggered: " + zone.getTransitionId()
            + " -> " + zone.getTargetMap() + ":" + zone.getTargetTransitionId());

        if (transitionCallback != null) {
            transitionCallback.accept(zone.getTargetMap(), zone.getTargetTransitionId());
        }
    }

    public void setCooldown(float seconds) {
        this.cooldownTimer = seconds;
        this.currentZone = null;
        this.requiresZoneExitBeforeTrigger = true;
    }

    /**
     * Called when the map changes. Parses trigger layers and creates
     * MapTransitionZone entities for each rectangle trigger found.
     */
    public void setMap(TiledMap tiledMap) {
        Engine engine = getEngine();
        if (engine == null) return;
        parseTriggerLayers(tiledMap, engine);
    }

    private void parseTriggerLayers(TiledMap tiledMap, Engine engine) {
        for (MapLayer layer : tiledMap.getLayers()) {
            boolean isTriggerLayer = layer.getName() != null
                && "trigger".equalsIgnoreCase(layer.getName().trim());
            for (MapObject mapObject : layer.getObjects()) {
                if (!(mapObject instanceof RectangleMapObject rectObj)) continue;

                String objectType = getStringProp(mapObject, OBJECT_TYPE_KEY, "");
                boolean isMapTransitionObject = MAP_TRANSITION_VALUE.equalsIgnoreCase(objectType);
                if (!isTriggerLayer && !isMapTransitionObject) continue;

                String transitionId = getStringProp(mapObject, "transitionId", null);
                if (transitionId == null) continue;

                String targetMap = getStringProp(mapObject, "targetMap", null);
                String targetTransitionId = getStringProp(mapObject, "targetTransitionId", null);

                Rectangle pixelRect = rectObj.getRectangle();
                float rotation = getFloatProp(mapObject, "rotation", 0f);

                Rectangle worldBounds = computeWorldBounds(pixelRect, rotation);

                Entity zoneEntity = engine.createEntity();
                zoneEntity.add(new MapTransitionZone(
                    worldBounds, transitionId, targetMap, targetTransitionId));
                engine.addEntity(zoneEntity);

                Gdx.app.log(TAG, "Loaded transition zone: id=" + transitionId
                    + " target=" + targetMap + ":" + targetTransitionId
                    + " worldBounds=" + worldBounds);
            }
        }
    }

    /**
     * Computes the world-space AABB for a potentially rotated rectangle.
     * Handles Tiled's clockwise rotation around the top-left pivot
     * after libGDX's y-flip.
     */
    static Rectangle computeWorldBounds(Rectangle pixelRect, float rotationDeg) {
        float x = pixelRect.x;
        float y = pixelRect.y;
        float w = pixelRect.width;
        float h = pixelRect.height;

        if (Math.abs(rotationDeg) < 0.1f) {
            return new Rectangle(
                x * Main.UNIT_SCALE, y * Main.UNIT_SCALE,
                w * Main.UNIT_SCALE, h * Main.UNIT_SCALE);
        }

        float pivotX = x;
        float pivotY = y + h;

        float rad = (float) Math.toRadians(-rotationDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float[][] corners = {
            {x, y}, {x + w, y}, {x + w, y + h}, {x, y + h}
        };

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (float[] c : corners) {
            float dx = c[0] - pivotX;
            float dy = c[1] - pivotY;
            float rx = cos * dx - sin * dy + pivotX;
            float ry = sin * dx + cos * dy + pivotY;
            minX = Math.min(minX, rx);
            maxX = Math.max(maxX, rx);
            minY = Math.min(minY, ry);
            maxY = Math.max(maxY, ry);
        }

        return new Rectangle(
            minX * Main.UNIT_SCALE, minY * Main.UNIT_SCALE,
            (maxX - minX) * Main.UNIT_SCALE, (maxY - minY) * Main.UNIT_SCALE);
    }

    private static String getStringProp(MapObject obj, String key, String defaultValue) {
        Object val = obj.getProperties().get(key);
        if (val instanceof String s) return s;
        if (val != null) return val.toString();
        return defaultValue;
    }

    private static float getFloatProp(MapObject obj, String key, float defaultValue) {
        Object val = obj.getProperties().get(key);
        if (val instanceof Float f) return f;
        if (val instanceof Double d) return d.floatValue();
        if (val instanceof Integer i) return i;
        if (val instanceof String s) {
            try { return Float.parseFloat(s.trim()); }
            catch (NumberFormatException ignored) { return defaultValue; }
        }
        return defaultValue;
    }

    private static Rectangle buildPlayerHitbox(Transform transform, Rectangle out) {
        float w = transform.getSize().x * PLAYER_HITBOX_WIDTH_FACTOR;
        float h = transform.getSize().y * PLAYER_HITBOX_HEIGHT_FACTOR;
        float x = transform.getPosition().x + (transform.getSize().x - w) * 0.5f;
        float y = transform.getPosition().y + transform.getSize().y * PLAYER_HITBOX_Y_OFFSET_FACTOR;
        return out.set(x, y, w, h);
    }
}
