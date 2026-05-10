package github.dluckycompany.clawkins.tiled;

import java.util.List;
import java.util.Locale;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.objects.CircleMapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.component.CameraFollow;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.PlayerProfile;
import github.dluckycompany.clawkins.component.Prop;
import github.dluckycompany.clawkins.component.Tiled;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterTrigger;
import github.dluckycompany.clawkins.encounter.EncounterZone;
import github.dluckycompany.clawkins.player.PlayerAnimationFactory;

/**
 * The "entity factory" — turns Tiled map objects into Ashley ECS entities.
 *
 * Object classification now prefers the custom property `ObjectType` and falls
 * back to legacy object names for backward compatibility.
 */
public class TiledObjectConfigurator {
    private static final String TAG = TiledObjectConfigurator.class.getSimpleName();
    private static final String TYPE_KEY = "ObjectType";
    private static final String NESTED_PROPERTIES_KEY = "properties";

    private final Engine engine;
    private final AssetService assetService;
    private final PlayerBattleState playerBattleState;

    private enum ObjectType {
        PLAYER,
        PROP,
        INTERACTIBLE,
        MERCHANT,
        ENEMY,
        BARRIER,
        UNDEFINED
    }

    public TiledObjectConfigurator(Engine engine, AssetService assetService, PlayerBattleState playerBattleState) {
        this.engine = engine;
        this.assetService = assetService;
        this.playerBattleState = playerBattleState;
    }

    /**
     * Called by TiledService for each object in map object layers.
     * Creates and configures an entity from the map object's properties.
     */
    public void onLoadObject(MapObject mapObject) {
        if (mapObject instanceof TiledMapTileMapObject tileMapObject) {
            onLoadObject(tileMapObject);
            return;
        }
        onLoadShapeObject(mapObject);
    }

    private void onLoadShapeObject(MapObject mapObject) {
        if (mapObject == null) {
            return;
        }

        ObjectType objectType = readObjectType(mapObject);
        if (objectType != ObjectType.INTERACTIBLE && objectType != ObjectType.MERCHANT) {
            return;
        }

        Rectangle bounds = getObjectBounds(mapObject);
        if (bounds == null) {
            return;
        }

        Entity entity = engine.createEntity();
        addTransform(bounds, entity);
        entity.add(new Tiled(mapObject));

        boolean shouldAdd = configureShapeByType(mapObject, objectType, entity);
        if (shouldAdd) {
            engine.addEntity(entity);
            Gdx.app.log(TAG, "✓ Spawned shape entity: name=" + mapObject.getName()
                    + " bounds=" + bounds);
        }
    }

    public void onLoadObject(TiledMapTileMapObject tileMapObject) {
        Gdx.app.log(TAG, "onLoadObject called for: " + tileMapObject.getName() + " (type: " + tileMapObject.getProperties().get("type") + ")");
        
        Entity entity = engine.createEntity();
        TiledMapTile tile = tileMapObject.getTile();
        TextureRegion textureRegion = tile.getTextureRegion();

        addTransform(tileMapObject, textureRegion, entity);
        entity.add(new Graphic(textureRegion, Color.WHITE.cpy()));
        entity.add(new Tiled(tileMapObject));
        boolean shouldAdd = configureByType(tileMapObject, entity);

        if (shouldAdd) {
            engine.addEntity(entity);
            Gdx.app.log(TAG, "✓ Spawned entity: name=" + tileMapObject.getName()
                    + " pos=" + Transform.MAPPER.get(entity).getPosition());
        } else {
            Gdx.app.log(TAG, "✗ Skipped entity: name=" + tileMapObject.getName());
        }
    }

    private void addTransform(TiledMapTileMapObject tileMapObject, TextureRegion region, Entity entity) {
        float x = tileMapObject.getX() * Main.UNIT_SCALE;
        float y = tileMapObject.getY() * Main.UNIT_SCALE;
        float w = region.getRegionWidth() * Main.UNIT_SCALE;
        float h = region.getRegionHeight() * Main.UNIT_SCALE;

        Vector2 position = new Vector2(x, y);
        Vector2 size = new Vector2(w, h);
        Vector2 scaling = new Vector2(tileMapObject.getScaleX(), tileMapObject.getScaleY());
        float rotation = tileMapObject.getRotation();

        entity.add(new Transform(position, 1, size, scaling, rotation));
    }

    private void addTransform(Rectangle objectBounds, Entity entity) {
        float x = objectBounds.x * Main.UNIT_SCALE;
        float y = objectBounds.y * Main.UNIT_SCALE;
        float w = objectBounds.width * Main.UNIT_SCALE;
        float h = objectBounds.height * Main.UNIT_SCALE;
        entity.add(new Transform(new Vector2(x, y), 1, new Vector2(w, h), new Vector2(1f, 1f), 0f));
    }

    private boolean configureByType(TiledMapTileMapObject tileMapObject, Entity entity) {
        ObjectType objectType = readObjectType(tileMapObject);
        switch (objectType) {
            case PLAYER -> {
                int existingPlayerCount = engine.getEntitiesFor(Family.all(Player.class).get()).size();
                Gdx.app.log(TAG, "Configuring PLAYER entity. Existing player count: " + existingPlayerCount);
                
                if (existingPlayerCount > 0) {
                    Gdx.app.log(TAG, "Skipping duplicate PLAYER entity (map transition)");
                    return false;
                }
                
                Gdx.app.log(TAG, "Creating new PLAYER entity");
                float moveSpeed = getFloatProperty(tileMapObject, "moveSpeed", 3f);
                String playerName = getStringProperty(tileMapObject, "Name", "Player");
                int playerHp = getIntProperty(tileMapObject, "playerHp", 100);
                int playerAttack = getIntProperty(tileMapObject, "playerAttack", 12);
                int playerDefense = getIntProperty(tileMapObject, "playerDefense", 8);
                int playerSpeed = getIntProperty(tileMapObject, "playerSpeed", 10);
                String playerSkill1Name = getStringProperty(tileMapObject, "playerSkill1Name", "Slash");
                int playerSkill1Power = getIntProperty(tileMapObject, "playerSkill1Power", 12);
                String playerSkill2Name = getStringProperty(tileMapObject, "playerSkill2Name", "Heavy Strike");
                int playerSkill2Power = getIntProperty(tileMapObject, "playerSkill2Power", 16);
                String playerSkill3Name = getStringProperty(tileMapObject, "playerSkill3Name", "Quick Jab");
                int playerSkill3Power = getIntProperty(tileMapObject, "playerSkill3Power", 9);

                playerBattleState.initializeIfUnset(
                    playerHp,
                    playerAttack,
                    playerDefense,
                    playerSpeed,
                    List.of(
                        new BattleSkill(playerSkill1Name, playerSkill1Power),
                        new BattleSkill(playerSkill2Name, playerSkill2Power),
                        new BattleSkill(playerSkill3Name, playerSkill3Power)
                    )
                );

                if (playerBattleState.getPartySize() == 0) {
                    int loaded = loadConfiguredPlayerClawkins(tileMapObject);
                    Gdx.app.debug(TAG, "Loaded " + loaded + " configured clawkin(s) from PLAYER properties.");
                }

                entity.add(new Player());
                entity.add(new PlayerProfile(playerName));
                entity.add(new CameraFollow());
                entity.add(new Move(moveSpeed));
                entity.add(PlayerAnimationFactory.create(assetService));
                Gdx.app.debug(TAG, "Configured as PLAYER speed=" + moveSpeed + " hp=" + playerBattleState.getCurrentHp());
            }
            case ENEMY -> {
                String encounterId = getStringProperty(tileMapObject, "encounterId", "enemy");
                String enemyName = getStringProperty(
                        tileMapObject,
                        "enemyName",
                        getStringProperty(tileMapObject, "Name", encounterId)
                );
                String encounterTableId = getStringProperty(tileMapObject, "encounterTableId", "default");
                boolean oneShot = getBooleanProperty(tileMapObject, "oneShot", false);
                int enemyHp = getIntProperty(tileMapObject, "enemyHp", 40);
                int enemyAttack = getIntProperty(tileMapObject, "enemyAttack", 8);
                int enemyDefense = getIntProperty(tileMapObject, "enemyDefense", 3);
                int enemySpeed = getIntProperty(tileMapObject, "enemySpeed", 6);
                List<BattleSkill> enemySkills = parseEnemySkills(tileMapObject, enemyName);
                String enemyImagePath = getStringProperty(
                    tileMapObject,
                    "enemyImagePath",
                    getStringProperty(tileMapObject, "image_clawkin", "")
                );
                boolean canRoam = getBooleanProperty(tileMapObject, "canRoam", true);
                boolean canChase = getBooleanProperty(tileMapObject, "canChase", true);
                float roamingSpeed = getFloatProperty(tileMapObject, "roamingSpeed", 1.5f);
                float chasingSpeed = getFloatProperty(tileMapObject, "chasingSpeed", 3.0f);
                float sightRange = getFloatProperty(tileMapObject, "sightRange", 4.0f);
                float sightConeDotThreshold = getFloatProperty(tileMapObject, "sightConeDotThreshold", 0.5f);
                float alertPauseDuration = getFloatProperty(tileMapObject, "alertPauseDuration", 0.8f);
                boolean isTerritorial = getBooleanProperty(tileMapObject, "isTerritorial", false);
                float territorialRoamRadius = getFloatProperty(tileMapObject, "territorialRoamRadius", 3.0f);
                float territorialChaseDistance = getFloatProperty(tileMapObject, "territorialChaseDistance", 6.0f);
                String facingDirection = getStringProperty(tileMapObject, "facingDirection", "SOUTH");

                Enemy enemy = new Enemy(
                        canRoam,
                        canChase,
                        roamingSpeed,
                        chasingSpeed,
                        sightRange,
                        sightConeDotThreshold,
                        alertPauseDuration,
                        isTerritorial,
                        territorialRoamRadius,
                        territorialChaseDistance);
                enemy.setFacingDirection(parseFacingDirection(facingDirection));
                entity.add(enemy);
                entity.add(new Move(0)); // Speed is managed by EnemySystem
                entity.add(new EncounterTrigger());
                entity.add(new EncounterZone(
                        encounterId,
                        enemyName,
                        encounterTableId,
                        oneShot,
                        enemyHp,
                        enemyAttack,
                        enemyDefense,
                        enemySpeed,
                        enemySkills,
                        enemyImagePath
                ));
                Gdx.app.debug(TAG, "Configured as ENEMY: id=" + encounterId + " table=" + encounterTableId);
            }
            case INTERACTIBLE -> {
                String objectName = getStringProperty(tileMapObject, "ObjectName", "Object");
                String objectIdFallback = buildInteractibleObjectIdFallback(tileMapObject, objectName);
                String objectId = getStringProperty(tileMapObject, "ObjectId", objectIdFallback);
                if (objectId == null || objectId.isBlank()) {
                    objectId = objectIdFallback;
                }
                String dialogueDirectory = getStringProperty(tileMapObject, "DialogueDirectory", "...");
                boolean hasCollision = getBooleanProperty(tileMapObject, "hasCollision", true);
                boolean isTrippable = getBooleanProperty(tileMapObject, "isTrippable", false);
                String posRaw = getStringProperty(tileMapObject, "DialoguePosition", "BOTTOM");
                Interactible.DialoguePosition dialoguePosition = parseDialoguePosition(posRaw);

                entity.add(new Interactible(
                        objectName,
                    objectId,
                        dialogueDirectory,
                        hasCollision,
                        dialoguePosition,
                        false,
                        isTrippable
                ));
                Gdx.app.debug(TAG, "Configured as INTERACTIBLE: " + objectName + " collision=" + hasCollision);
            }
            case MERCHANT -> {
                String objectName = getStringProperty(tileMapObject, "ObjectName", "Merchant");
                String objectIdFallback = buildInteractibleObjectIdFallback(tileMapObject, objectName);
                String objectId = getStringProperty(tileMapObject, "ObjectId", objectIdFallback);
                if (objectId == null || objectId.isBlank()) {
                    objectId = objectIdFallback;
                }
                String dialogueDirectory = getStringProperty(tileMapObject, "DialogueDirectory", "Welcome!");
                boolean hasCollision = getBooleanProperty(tileMapObject, "hasCollision", true);
                boolean isTrippable = getBooleanProperty(tileMapObject, "isTrippable", false);
                String posRaw = getStringProperty(tileMapObject, "DialoguePosition", "BOTTOM");
                Interactible.DialoguePosition dialoguePosition = parseDialoguePosition(posRaw);

                entity.add(new Interactible(
                        objectName,
                    objectId,
                        dialogueDirectory,
                        hasCollision,
                        dialoguePosition,
                        true,  // isMerchant = true
                        isTrippable
                ));
                Gdx.app.debug(TAG, "Configured as MERCHANT: " + objectName + " collision=" + hasCollision);
            }
            case BARRIER -> {
                // Barrier collision is handled via map barrier objects, no entity component required.
                Gdx.app.debug(TAG, "Configured as BARRIER (shape-based map collision)");
            }
            case PROP -> {
                String objectIdFallback = buildPropObjectIdFallback(tileMapObject);
                String objectId = getStringProperty(tileMapObject, "ObjectId", objectIdFallback);
                if (objectId == null || objectId.isBlank()) {
                    objectId = objectIdFallback;
                }
                applyPropFlipFromTileObject(tileMapObject, entity);
                entity.add(new Prop(objectId));
                Gdx.app.debug(TAG, "Configured as PROP (non-interactible cosmetic object)");
            }
            default -> Gdx.app.debug(TAG, "Unknown ObjectType for object name: " + tileMapObject.getName());
        }
        return true;
    }

    private static void applyPropFlipFromTileObject(TiledMapTileMapObject tileMapObject, Entity entity) {
        if (tileMapObject == null || entity == null) {
            return;
        }
        Transform transform = Transform.MAPPER.get(entity);
        if (transform == null) {
            return;
        }

        Vector2 scaling = transform.getScaling();
        float scaleX = Math.max(0.0001f, Math.abs(scaling.x));
        float scaleY = Math.max(0.0001f, Math.abs(scaling.y));

        scaling.x = tileMapObject.isFlipHorizontally() ? -scaleX : scaleX;
        scaling.y = tileMapObject.isFlipVertically() ? -scaleY : scaleY;
    }

    private boolean configureShapeByType(MapObject mapObject, ObjectType objectType, Entity entity) {
        switch (objectType) {
            case INTERACTIBLE -> {
                addInteractibleComponent(mapObject, entity, false);
                Gdx.app.debug(TAG, "Configured shape as INTERACTIBLE: " + mapObject.getName());
                return true;
            }
            case MERCHANT -> {
                addInteractibleComponent(mapObject, entity, true);
                Gdx.app.debug(TAG, "Configured shape as MERCHANT: " + mapObject.getName());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void addInteractibleComponent(MapObject mapObject, Entity entity, boolean isMerchant) {
        String objectName = getStringProperty(mapObject, "ObjectName", isMerchant ? "Merchant" : "Object");
        String objectIdFallback = buildInteractibleObjectIdFallback(mapObject, objectName);
        String objectId = getStringProperty(mapObject, "ObjectId", objectIdFallback);
        if (objectId == null || objectId.isBlank()) {
            objectId = objectIdFallback;
        }
        String dialogueDirectory = getStringProperty(mapObject, "DialogueDirectory", isMerchant ? "Welcome!" : "...");
        boolean hasCollision = getBooleanProperty(mapObject, "hasCollision", true);
        boolean isTrippable = getBooleanProperty(mapObject, "isTrippable", false);
        String posRaw = getStringProperty(mapObject, "DialoguePosition", "BOTTOM");
        Interactible.DialoguePosition dialoguePosition = parseDialoguePosition(posRaw);

        entity.add(new Interactible(
                objectName,
                objectId,
                dialogueDirectory,
                hasCollision,
                dialoguePosition,
                isMerchant,
                isTrippable
        ));
    }

    private static Interactible.DialoguePosition parseDialoguePosition(String raw) {
        if (raw == null || raw.isBlank()) {
            return Interactible.DialoguePosition.BOTTOM;
        }
        try {
            return Interactible.DialoguePosition.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Interactible.DialoguePosition.BOTTOM;
        }
    }

    private static Vector2 parseFacingDirection(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Vector2(0f, -1f);
        }

        return switch (raw.trim().toUpperCase()) {
            case "NORTH", "UP" -> new Vector2(0f, 1f);
            case "EAST", "RIGHT" -> new Vector2(1f, 0f);
            case "WEST", "LEFT" -> new Vector2(-1f, 0f);
            case "SOUTH", "DOWN" -> new Vector2(0f, -1f);
            default -> new Vector2(0f, -1f);
        };
    }

    private static String buildInteractibleObjectIdFallback(MapObject object, String objectName) {
        String normalizedName = (objectName == null || objectName.isBlank())
                ? "object"
                : objectName.trim().toLowerCase().replaceAll("\\s+", "_");
        Rectangle bounds = getObjectBounds(object);
        int x = Math.round(bounds != null ? bounds.x : 0f);
        int y = Math.round(bounds != null ? bounds.y : 0f);
        return normalizedName + "_" + x + "_" + y;
    }

    private static String buildPropObjectIdFallback(MapObject object) {
        Rectangle bounds = getObjectBounds(object);
        int x = Math.round(bounds != null ? bounds.x : 0f);
        int y = Math.round(bounds != null ? bounds.y : 0f);
        return "prop_" + x + "_" + y;
    }

    private static ObjectType readObjectType(MapObject object) {
        String rawType = getStringProperty(object, TYPE_KEY, "");
        if (!rawType.isBlank()) {
            try {
                return ObjectType.valueOf(rawType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to legacy object names below
            }
        }

        // Support Tiled's built-in object "type" field (often serialized in properties maps).
        String tiledType = getStringProperty(object, "type", "");
        if (tiledType.isBlank()) {
            tiledType = getStringProperty(object, "Type", "");
        }
        if (tiledType != null && !tiledType.isBlank()) {
            String normalized = tiledType.trim().toUpperCase(Locale.ROOT);
            try {
                return ObjectType.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                if ("ENCOUNTERZONE".equals(normalized)) return ObjectType.ENEMY;
            }
        }

        String legacyName = object.getName();
        if (legacyName == null) {
            return ObjectType.UNDEFINED;
        }
        String normalizedName = legacyName.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedName) {
            case "PLAYER" -> ObjectType.PLAYER;
            case "ENEMY", "ENCOUNTERZONE" -> ObjectType.ENEMY;
            case "INTERACTIBLE" -> ObjectType.INTERACTIBLE;
            case "MERCHANT" -> ObjectType.MERCHANT;
            case "BARRIER" -> ObjectType.BARRIER;
            case "PROP" -> ObjectType.PROP;
            default -> ObjectType.UNDEFINED;
        };
    }

    private static String getStringProperty(MapObject object, String key, String defaultValue) {
        Object value = getRawProperty(object, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }

    private static int getIntProperty(MapObject object, String key, int defaultValue) {
        Object value = getRawProperty(object, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Float f) {
            return Math.round(f);
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static float getFloatProperty(MapObject object, String key, float defaultValue) {
        Object value = getRawProperty(object, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Float f) {
            return f;
        }
        if (value instanceof Double d) {
            return d.floatValue();
        }
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof String s) {
            try {
                return Float.parseFloat(s.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean getBooleanProperty(MapObject object, String key, boolean defaultValue) {
        Object value = getRawProperty(object, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        if (value instanceof String s) {
            if (s.equalsIgnoreCase("true")) return true;
            if (s.equalsIgnoreCase("false")) return false;
            String trimmed = s.trim();
            if (trimmed.equals("1")) return true;
            if (trimmed.equals("0")) return false;
        }
        return defaultValue;
    }

    private static Object getRawProperty(MapObject object, String key) {
        if (object == null || key == null) {
            return null;
        }

        Object direct = object.getProperties().get(key);
        if (direct != null) {
            return direct;
        }

        Object nested = object.getProperties().get(NESTED_PROPERTIES_KEY);
        if (nested == null) {
            return null;
        }

        switch (nested) {
            case MapProperties nestedProps -> {
                Object v = nestedProps.get(key);
                if (v != null) return v;
            }
            case java.util.Map<?, ?> nestedMap -> {
                // Some immutable map implementations throw on null keys.
                try {
                    Object v = nestedMap.get(key);
                    if (v != null) return v;
                } catch (NullPointerException ignored) {
                    return null;
                }
            }
            default -> {
            }
        }
        return null;
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
                    radius * 2f
            );
        }
        if (mapObject instanceof EllipseMapObject ellipseMapObject) {
            return new Rectangle(
                    ellipseMapObject.getEllipse().x,
                    ellipseMapObject.getEllipse().y,
                    ellipseMapObject.getEllipse().width,
                    ellipseMapObject.getEllipse().height
            );
        }
        if (mapObject instanceof TiledMapTileMapObject tileMapObject) {
            TiledMapTile tile = tileMapObject.getTile();
            TextureRegion region = tile == null ? null : tile.getTextureRegion();
            if (region == null) {
                return null;
            }
            return new Rectangle(
                    tileMapObject.getX(),
                    tileMapObject.getY(),
                    region.getRegionWidth(),
                    region.getRegionHeight()
            );
        }
        return null;
    }

    private int loadConfiguredPlayerClawkins(TiledMapTileMapObject tileMapObject) {
        int loaded = 0;
        for (int slot = 1; slot <= 3; slot++) {
            Clawkin clawkin = buildClawkinFromSlot(tileMapObject, slot);
            if (clawkin == null) {
                continue;
            }
            playerBattleState.addClawkinToParty(clawkin);
            loaded++;
        }
        return loaded;
    }

    private Clawkin buildClawkinFromSlot(TiledMapTileMapObject tileMapObject, int slot) {
        MapProperties clawkinProps = getNestedClawkinProperties(tileMapObject, slot);

        String rawName = getStringFromProps(clawkinProps, "name", "").trim();
        String rawId = getStringFromProps(clawkinProps, "id", "").trim();

        if (rawName.isBlank() && rawId.isBlank()) {
            return null;
        }

        String resolvedName = rawName.isBlank() ? ("Clawkin " + slot) : rawName;
        String resolvedId = rawId.isBlank() ? ("clawkin_" + toIdToken(resolvedName)) : rawId;

        String imagePath = getStringFromProps(clawkinProps, "image_clawkin", "").trim();
        String iconImagePath = getStringFromProps(clawkinProps, "image_clawkin_icon", "").trim();
        int level = getIntFromProps(clawkinProps, "level", 1);
        int hp = getIntFromProps(clawkinProps, "hp", 50);
        int attack = getIntFromProps(clawkinProps, "attack", 8);
        int defense = getIntFromProps(clawkinProps, "defense", 4);
        int speed = getIntFromProps(clawkinProps, "speed", 6);
        List<BattleSkill> skills = parseClawkinSkills(clawkinProps, resolvedName);
        Clawkin.SummaryProfile summaryProfile = buildSummaryProfile(clawkinProps, hp, attack, defense, speed);

        Gdx.app.log(
            TAG,
            "Clawkin slot " + slot + " loaded -> id=" + resolvedId
                + ", name=" + resolvedName
                + ", image=" + imagePath
                + ", iconImage=" + iconImagePath
                + ", level=" + level
                + ", hp=" + hp
                + ", atk=" + attack
                + ", def=" + defense
                + ", spd=" + speed
                + ", skills=" + skills.size()
                + ", species=" + summaryProfile.getSpecies()
                + ", role=" + summaryProfile.getRole()
                + ", title=" + summaryProfile.getTitle()
        );

        return new Clawkin(
            resolvedId,
            resolvedName,
            imagePath,
            iconImagePath,
            level,
            hp,
            attack,
            defense,
            speed,
            skills,
            summaryProfile
        );
    }

    private static Clawkin.SummaryProfile buildSummaryProfile(
        MapProperties clawkinProps,
        int hp,
        int attack,
        int defense,
        int speed
    ) {
        String species = getStringFromProps(clawkinProps, "species", "");
        String role = getStringFromProps(clawkinProps, "role", "");
        String title = getStringFromProps(clawkinProps, "title", "");
        String overview = getStringFromProps(clawkinProps, "overview", "");

        int profileHp = getIntFromProps(clawkinProps, "summaryHp", hp);
        int profileAttack = getIntFromProps(clawkinProps, "summaryAttack", attack);
        int profileDefense = getIntFromProps(clawkinProps, "summaryDefense", defense);
        int profileSpeed = getIntFromProps(clawkinProps, "summarySpeed", speed);

        String hpNote = getStringFromProps(clawkinProps, "summaryHpNote", "");
        String attackNote = getStringFromProps(clawkinProps, "summaryAttackNote", "");
        String defenseNote = getStringFromProps(clawkinProps, "summaryDefenseNote", "");
        String speedNote = getStringFromProps(clawkinProps, "summarySpeedNote", "");

        return new Clawkin.SummaryProfile(
            species,
            role,
            title,
            overview,
            profileHp,
            profileAttack,
            profileDefense,
            profileSpeed,
            hpNote,
            attackNote,
            defenseNote,
            speedNote
        );
    }

    /**
     * Retrieves clawkin slot properties from direct object-level class properties:
     * object -> clawkin{slot}(Clawkin class)
     */
    private static MapProperties getNestedClawkinProperties(TiledMapTileMapObject object, int slot) {
        String key = "clawkin" + slot;
        Object direct = object.getProperties().get(key);
        return asMapProperties(direct);
    }

    private static MapProperties asMapProperties(Object value) {
        if (value instanceof MapProperties mp) {
            return mp;
        }
        if (value instanceof java.util.Map<?, ?> map) {
            MapProperties wrapped = new MapProperties();
            for (var entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    wrapped.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return wrapped;
        }
        return null;
    }

    private static String getStringFromProps(MapProperties props, String key, String defaultValue) {
        if (props == null) return defaultValue;
        Object value = props.get(key);
        if (value == null) return defaultValue;
        if (value instanceof String s) return s;
        return String.valueOf(value);
    }

    private static int getIntFromProps(MapProperties props, String key, int defaultValue) {
        if (props == null) return defaultValue;
        Object value = props.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer i) return i;
        if (value instanceof Float f) return Math.round(f);
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private List<BattleSkill> parseClawkinSkills(MapProperties clawkinProps, String clawkinName) {
        List<BattleSkill> skills = new java.util.ArrayList<>();
        for (int slot = 1; slot <= 3; slot++) {
            String skillPrefix = "skill" + slot;
            String skillName = getStringFromProps(clawkinProps, skillPrefix + "Name", "").trim();
            String effectType = getStringFromProps(clawkinProps, skillPrefix + "EffectType", "").trim();
            int effectBaseStat = getIntFromProps(clawkinProps, skillPrefix + "EffectBaseStat", 0);
            String effectStatScale = getStringFromProps(
                clawkinProps,
                skillPrefix + "EffectStatScale",
                getStringFromProps(clawkinProps, skillPrefix + "EffectStat", "")
            ).trim();
            int effectDurationTurns = getIntFromProps(clawkinProps, skillPrefix + "EffectDurationTurns", 0);
            int turnCooldown = getIntFromProps(clawkinProps, skillPrefix + "TurnCooldown", 0);
            String summaryDescription = getStringFromProps(clawkinProps, skillPrefix + "Description", "").trim();
            String summaryEffectText = getStringFromProps(clawkinProps, skillPrefix + "SummaryEffect", "").trim();
            String summaryScalingText = getStringFromProps(clawkinProps, skillPrefix + "SummaryScaling", "").trim();

            boolean hasSkillData = !skillName.isBlank() || !effectType.isBlank() || effectBaseStat > 0
                || !effectStatScale.isBlank() || effectDurationTurns > 0;
            if (!hasSkillData) {
                continue;
            }

            BattleSkill.EffectType parsedType = parseEffectType(effectType);
            String resolvedName = skillName.isBlank() ? ("Skill " + slot) : skillName;
            BattleSkill skill = new BattleSkill(
                resolvedName,
                parsedType,
                effectBaseStat,
                effectStatScale,
                effectDurationTurns,
                turnCooldown,
                summaryDescription,
                summaryEffectText,
                summaryScalingText
            );
            skills.add(skill);

                Gdx.app.log(
                    TAG,
                    "Parsed clawkin skill metadata for " + clawkinName
                            + " slot=" + slot
                        + " name=" + resolvedName
                        + " effectTypeRaw=" + effectType
                        + " effectTypeParsed=" + parsedType
                            + " effectBaseStat=" + skill.getEffectBaseStat()
                            + " effectStatScale=" + skill.getEffectStatScale()
                            + " effectDurationTurns=" + effectDurationTurns
                            + " turnCooldown=" + turnCooldown
                            + " (phase 2 enabled)."
            );
        }
        
        // If no skills were defined in the map, use SkillUnlockSystem
        if (skills.isEmpty()) {
            String clawkinId = getStringFromProps(clawkinProps, "id", "").trim();
            int level = getIntFromProps(clawkinProps, "level", 5);
            
            if (!clawkinId.isBlank()) {
                skills = github.dluckycompany.clawkins.character.SkillUnlockSystem.getAllSkillsUpToLevel(clawkinId, level);
                Gdx.app.log(TAG, "No skills defined in map for " + clawkinName + ", using SkillUnlockSystem -> " + skills.size() + " skills loaded");
            }
        }
        
        return skills;
    }

    private static BattleSkill.EffectType parseEffectType(String raw) {
        if (raw == null || raw.isBlank()) {
            return BattleSkill.EffectType.DAMAGE;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "heal" -> BattleSkill.EffectType.HEAL;
            case "attack" -> BattleSkill.EffectType.ATTACK;
            case "defense" -> BattleSkill.EffectType.DEFENSE;
            default -> BattleSkill.EffectType.DAMAGE;
        };
    }

    private List<BattleSkill> parseEnemySkills(TiledMapTileMapObject tileMapObject, String enemyName) {
        List<BattleSkill> skills = new java.util.ArrayList<>();
        for (int slot = 1; slot <= 3; slot++) {
            String skillPrefix = "skill" + slot;

            String name = getStringProperty(
                    tileMapObject,
                    skillPrefix + "Name",
                    getStringProperty(tileMapObject, "enemySkill" + slot + "Name", "")
            ).trim();
            String effectTypeRaw = getStringProperty(tileMapObject, skillPrefix + "EffectType", "").trim();
                int effectBaseStat = getIntProperty(tileMapObject, skillPrefix + "EffectBaseStat", 0);
                String effectStatScale = getStringProperty(
                    tileMapObject,
                    skillPrefix + "EffectStatScale",
                    getStringProperty(tileMapObject, skillPrefix + "EffectStat", "")
                ).trim();
            int effectDuration = getIntProperty(tileMapObject, skillPrefix + "EffectDurationTurns", 0);
            int turnCooldown = getIntProperty(tileMapObject, skillPrefix + "TurnCooldown", 0);

                boolean hasSkillData = !name.isBlank() || !effectTypeRaw.isBlank() || effectBaseStat > 0 || !effectStatScale.isBlank() || effectDuration > 0;
            if (!hasSkillData) {
                continue;
            }

            BattleSkill.EffectType effectType = parseEffectType(effectTypeRaw);
            String resolvedName = name.isBlank() ? ("Skill " + slot) : name;
                BattleSkill skill = new BattleSkill(resolvedName, effectType, effectBaseStat, effectStatScale, effectDuration, turnCooldown);
            skills.add(skill);

            Gdx.app.log(
                    TAG,
                    "Parsed enemy skill metadata for " + enemyName
                            + " slot=" + slot
                            + " name=" + resolvedName
                            + " effectTypeRaw=" + effectTypeRaw
                            + " effectTypeParsed=" + effectType
                            + " effectBaseStat=" + effectBaseStat
                            + " effectStatScale=" + effectStatScale
                            + " effectDurationTurns=" + effectDuration
                            + " turnCooldown=" + turnCooldown
            );
        }

        if (skills.isEmpty()) {
            skills.add(new BattleSkill("Bite", BattleSkill.EffectType.DAMAGE, 0, "attack[self]", 0, 0));
            skills.add(new BattleSkill("Claw Swipe", BattleSkill.EffectType.DAMAGE, 0, "attack[self]", 0, 0));
            skills.add(new BattleSkill("Rend", BattleSkill.EffectType.DAMAGE, 0, "attack[self]", 0, 0));
        }

        return skills;
    }

    private static String toIdToken(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_", "").replaceAll("_$", "");
        return normalized.isBlank() ? "slot" : normalized;
    }
}
