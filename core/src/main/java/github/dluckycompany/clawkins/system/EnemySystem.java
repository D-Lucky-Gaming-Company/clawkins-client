package github.dluckycompany.clawkins.system;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.Enemy;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;

public class EnemySystem extends IteratingSystem {
    private static final float RAYCAST_STEP = 0.5f;
    private static final float MIN_IDLE_DURATION = 0.6f;
    private static final float MAX_IDLE_DURATION = 1.6f;
    private static final float CHASE_MEMORY_DURATION = 1.2f;
    private static final int ROAM_DIRECTION_ATTEMPTS = 12;
    private static final float[] CHASE_STEER_ANGLES = { 0f, 25f, -25f, 45f, -45f, 70f, -70f };
    private static final float ENEMY_HITBOX_WIDTH_FACTOR = 0.32f;
    private static final float ENEMY_HITBOX_HEIGHT_FACTOR = 0.33f;
    private static final float PLAYER_FEET_PROBE_Y_FACTOR = 0.08f;

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> solidEntities;

    private final List<TiledMapTileLayer> collisionLayers;
    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle tmpTileRect = new Rectangle();
    private final Rectangle tmpSolidRect = new Rectangle();
    private final Vector2 tmpVec = new Vector2();
    private final Vector2 tmpDir = new Vector2();

    private float mapWidth;
    private float mapHeight;

    // Using simple hitboxes for raycasting validation mirroring the MoveSystem
    private static final float TILE_HITBOX_WIDTH_FACTOR = 0.80f;
    private static final float TILE_HITBOX_HEIGHT_FACTOR = 0.45f;
    private static final float SOLID_HITBOX_WIDTH_FACTOR = 0.80f;
    private static final float SOLID_HITBOX_HEIGHT_FACTOR = 0.45f;
    private static final float PLAYER_HITBOX_WIDTH_FACTOR = 0.26f;
    private static final float PLAYER_HITBOX_HEIGHT_FACTOR = 0.22f;

    public EnemySystem() {
        super(Family.all(Enemy.class, Move.class, Transform.class).get());
        this.collisionLayers = new ArrayList<>();
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.players = engine.getEntitiesFor(Family.all(Player.class, Transform.class).get());
        this.solidEntities = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
    }

    public void setMap(TiledMap tiledMap) {
        this.collisionLayers.clear();
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        this.mapWidth = width * tileW * Main.UNIT_SCALE;
        this.mapHeight = height * tileH * Main.UNIT_SCALE;

        boolean reachedObjectsLayer = false;
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                reachedObjectsLayer = true;
                continue;
            }
            if (reachedObjectsLayer) {
                // Keep layer selection aligned with MoveSystem collision behavior.
                if ("elements".equalsIgnoreCase(layer.getName()) && layer instanceof TiledMapTileLayer tileLayer) {
                    this.collisionLayers.add(tileLayer);
                }
                continue;
            }

            if (layer instanceof TiledMapTileLayer tileLayer) {
                this.collisionLayers.add(tileLayer);
            }
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void processEntity(Entity entity, float deltaTime) {
        Enemy enemy = entity.getComponent(Enemy.class);
        Move move = entity.getComponent(Move.class);
        Transform transform = entity.getComponent(Transform.class);

        if (enemy.getHomePosition().isZero()) {
            enemy.getHomePosition().set(transform.getPosition());
        }

        boolean seesPlayer = false;
        Entity targetPlayer = null;

        // 1. Check line of sight to player
        if (players.size() > 0) {
            targetPlayer = players.first();
            Transform playerTransform = targetPlayer.getComponent(Transform.class);
            seesPlayer = hasLineOfSight(transform, enemy, playerTransform, entity);
        }

        if (seesPlayer) {
            enemy.setChaseMemoryTimer(CHASE_MEMORY_DURATION);
        } else {
            enemy.setChaseMemoryTimer(Math.max(0f, enemy.getChaseMemoryTimer() - deltaTime));
        }

        boolean shouldChase = enemy.canChase()
                && targetPlayer != null
                && (seesPlayer || enemy.getChaseMemoryTimer() > 0f);
        if (shouldChase) {
            enemy.setState(Enemy.State.CHASING);
            enemy.setIdlingBetweenRoams(false);
            enemy.setIdleTimer(0f);
            move.setMaxSpeed(enemy.getChasingSpeed());
            // Null-safe: targetPlayer is checked to be non-null in the condition, but
            // getComponent can return null
            Transform targetTransform = targetPlayer != null ? targetPlayer.getComponent(Transform.class) : null;
            if (targetTransform != null) {
                updateChaseDirection(enemy, move, transform, targetTransform, entity);
                updateFacingDirectionFromMove(enemy, move.getDirection());
            }
            return;
        }

        if (targetPlayer != null && seesPlayer) {
            enemy.setState(Enemy.State.ALERTED);
            move.getDirection().setZero();
            move.setMaxSpeed(0f);
            updateFacingDirection(enemy, centerOf(targetPlayer.getComponent(Transform.class)), centerOf(transform));
            return;
        }

        if (enemy.canRoam()) {
            handleRoamIdleCycle(enemy, move, transform, deltaTime, entity);
            return;
        }

        enemy.setState(Enemy.State.IDLE);
        move.getDirection().setZero();
        move.setMaxSpeed(0f);
    }

    private void handleRoamIdleCycle(Enemy enemy, Move move, Transform transform, float deltaTime, Entity self) {
        if (enemy.isIdlingBetweenRoams()) {
            enemy.setState(Enemy.State.IDLE);
            move.getDirection().setZero();
            move.setMaxSpeed(0f);
            enemy.setIdleTimer(enemy.getIdleTimer() - deltaTime);
            if (enemy.getIdleTimer() <= 0f) {
                enemy.setIdlingBetweenRoams(false);
                enemy.setRoamTimer(enemy.getRoamInterval());
                chooseRoamDirection(enemy, move, transform, self);
            }
            return;
        }

        enemy.setState(Enemy.State.ROAMING);
        move.setMaxSpeed(enemy.getRoamingSpeed());

        if (enemy.getRoamTimer() <= 0f && move.getDirection().isZero()) {
            enemy.setRoamTimer(enemy.getRoamInterval());
            chooseRoamDirection(enemy, move, transform, self);
        }

        enemy.setRoamTimer(enemy.getRoamTimer() - deltaTime);
        if (enemy.getRoamTimer() <= 0f) {
            enemy.setIdlingBetweenRoams(true);
            enemy.setIdleTimer(MathUtils.random(MIN_IDLE_DURATION, MAX_IDLE_DURATION));
            move.getDirection().setZero();
            move.setMaxSpeed(0f);
            return;
        }

        if (!move.getDirection().isZero()
                && !canMoveAlong(transform, move.getDirection(), enemy.getRoamDecisionDistance(), self)) {
            chooseRoamDirection(enemy, move, transform, self);
        }
    }

    private void chooseRoamDirection(Enemy enemy, Move move, Transform transform, Entity self) {
        for (int i = 0; i < ROAM_DIRECTION_ATTEMPTS; i++) {
            float angle = MathUtils.random(0f, 360f);
            tmpDir.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).nor();
            if (!canMoveAlong(transform, tmpDir, enemy.getRoamDecisionDistance(), self)) {
                continue;
            }
            move.getDirection().set(tmpDir);
            updateFacingDirectionFromMove(enemy, move.getDirection());
            return;
        }
        move.getDirection().setZero();
    }

    private void updateFacingDirectionFromMove(Enemy enemy, Vector2 moveDir) {
        if (!moveDir.isZero()) {
            enemy.getFacingDirection().set(moveDir).nor();
        }
    }

    private void updateFacingDirection(Enemy enemy, Vector2 target, Vector2 origin) {
        tmpDir.set(target).sub(origin).nor();
        enemy.getFacingDirection().set(tmpDir);
    }

    private void updateChaseDirection(Enemy enemy, Move move, Transform enemyTransform, Transform playerTransform,
            Entity self) {
        Vector2 enemyCenter = centerOf(enemyTransform);
        Vector2 encounterTarget = playerEncounterTarget(enemyCenter, playerTransform);
        tmpDir.set(encounterTarget).sub(enemyCenter);
        if (tmpDir.isZero()) {
            move.getDirection().setZero();
            return;
        }

        Vector2 desired = tmpDir.nor();
        Vector2 chosen = findBestChaseDirection(desired, enemyTransform, enemy, self);
        move.getDirection().set(chosen);
    }

    private Vector2 playerEncounterTarget(Vector2 enemyCenter, Transform playerTransform) {
        float playerX = playerTransform.getPosition().x;
        float playerY = playerTransform.getPosition().y;
        float playerW = playerTransform.getSize().x;
        float playerH = playerTransform.getSize().y;

        float hitboxW = playerW * PLAYER_HITBOX_WIDTH_FACTOR;
        float hitboxH = playerH * PLAYER_HITBOX_HEIGHT_FACTOR;
        float hitboxX = playerX + (playerW - hitboxW) * 0.5f;
        float hitboxY = playerY;
        Rectangle playerEncounterRect = tmpTileRect.set(hitboxX, hitboxY, hitboxW, hitboxH);

        float feetX = playerX + playerW * 0.5f;
        float feetY = playerY + playerH * PLAYER_FEET_PROBE_Y_FACTOR;

        float regionX = Math.max(playerEncounterRect.x,
                Math.min(enemyCenter.x, playerEncounterRect.x + playerEncounterRect.width));
        float regionY = Math.max(playerEncounterRect.y,
                Math.min(enemyCenter.y, playerEncounterRect.y + playerEncounterRect.height));

        // Bias toward the same low-body/feet zone used by encounter triggering.
        return new Vector2((regionX + feetX) * 0.5f, (regionY + feetY) * 0.5f);
    }

    private Vector2 findBestChaseDirection(Vector2 desired, Transform enemyTransform, Enemy enemy, Entity self) {
        Vector2 best = null;
        float bestScore = -Float.MAX_VALUE;
        for (float angle : CHASE_STEER_ANGLES) {
            Vector2 candidate = rotatedDirection(desired, angle);
            if (!canMoveAlong(enemyTransform, candidate, enemy.getChaseProbeDistance(), self)) {
                continue;
            }
            float score = desired.dot(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = new Vector2(candidate);
            }
        }
        return best == null ? new Vector2() : best;
    }

    private Vector2 rotatedDirection(Vector2 direction, float angleDeg) {
        if (angleDeg == 0f) {
            return new Vector2(direction);
        }
        float cos = MathUtils.cosDeg(angleDeg);
        float sin = MathUtils.sinDeg(angleDeg);
        float x = direction.x * cos - direction.y * sin;
        float y = direction.x * sin + direction.y * cos;
        return new Vector2(x, y).nor();
    }

    private boolean canMoveAlong(Transform transform, Vector2 direction, float distance, Entity self) {
        if (direction == null || direction.isZero()) {
            return false;
        }
        float testX = transform.getPosition().x + direction.x * distance;
        float testY = transform.getPosition().y + direction.y * distance;
        Rectangle hitbox = buildEnemyHitbox(testX, testY, transform.getSize().x, transform.getSize().y, tmpRect);
        if (isOutOfMapBounds(hitbox)) {
            return false;
        }
        return !isPointObstructed(hitbox, self);
    }

    private Rectangle buildEnemyHitbox(float x, float y, float width, float height, Rectangle outRect) {
        float hitboxWidth = width * ENEMY_HITBOX_WIDTH_FACTOR;
        float hitboxHeight = height * ENEMY_HITBOX_HEIGHT_FACTOR;
        float hitboxX = x + (width - hitboxWidth) * 0.5f;
        float hitboxY = y;
        return outRect.set(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }

    private boolean hasLineOfSight(Transform enemyTransform, Enemy enemy, Transform playerTransform, Entity self) {
        Vector2 enemyPos = centerOf(enemyTransform);
        Vector2 playerPos = centerOf(playerTransform);

        // 1. Check distance
        float distance = enemyPos.dst(playerPos);
        if (distance > enemy.getSightRange()) {
            return false;
        }

        // 2. Check sight cone (facing direction vs direction to player)
        tmpDir.set(playerPos).sub(enemyPos).nor();
        float dot = enemy.getFacingDirection().dot(tmpDir);
        if (dot < enemy.getSightConeDotThreshold()) {
            return false; // Player is not in front of enemy's cone of vision
        }

        // 3. Raycast for collisions
        // Step along the vector from enemy to player
        int steps = Math.max(1, (int) Math.ceil(distance / RAYCAST_STEP));
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            tmpVec.set(enemyPos).lerp(playerPos, t);

            // Create a small bounding box at the test point
            tmpRect.set(tmpVec.x - 0.2f, tmpVec.y - 0.2f, 0.4f, 0.4f);

            if (isPointObstructed(tmpRect, self)) {
                return false;
            }
        }

        return true;
    }

    private Vector2 centerOf(Transform transform) {
        return new Vector2(
                transform.getPosition().x + transform.getSize().x * 0.5f,
                transform.getPosition().y + transform.getSize().y * 0.5f);
    }

    private boolean isPointObstructed(Rectangle rayRect, Entity self) {
        if (isOutOfMapBounds(rayRect)) {
            return true;
        }

        // 1. Check Map layers
        for (TiledMapTileLayer layer : collisionLayers) {
            float tileWorldW = layer.getTileWidth() * Main.UNIT_SCALE;
            float tileWorldH = layer.getTileHeight() * Main.UNIT_SCALE;
            if (tileWorldW <= 0f || tileWorldH <= 0f)
                continue;

            float layerOffsetX = layer.getOffsetX() * Main.UNIT_SCALE;
            float layerOffsetY = layer.getOffsetY() * Main.UNIT_SCALE;

            int minCol = (int) Math.floor((rayRect.x - layerOffsetX) / tileWorldW) - 1;
            int maxCol = (int) Math.floor((rayRect.x + rayRect.width - layerOffsetX - 0.0001f) / tileWorldW) + 1;
            int minRow = (int) Math.floor((rayRect.y - layerOffsetY) / tileWorldH) - 1;
            int maxRow = (int) Math.floor((rayRect.y + rayRect.height - layerOffsetY - 0.0001f) / tileWorldH) + 1;

            for (int row = minRow; row <= maxRow; row++) {
                for (int col = minCol; col <= maxCol; col++) {
                    TiledMapTileLayer.Cell cell = layer.getCell(col, row);
                    if (cell == null || cell.getTile() == null)
                        continue;

                    TiledMapTile tile = cell.getTile();
                    MapObjects objects = tile.getObjects();
                    if (objects == null || objects.getCount() == 0)
                        continue;

                    for (MapObject mapObject : objects) {
                        Rectangle rect = null;
                        if (mapObject instanceof RectangleMapObject rectMapObject) {
                            rect = rectMapObject.getRectangle();
                        } else if (mapObject instanceof PolygonMapObject polygonMapObject) {
                            rect = polygonMapObject.getPolygon().getBoundingRectangle();
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

                        if (rayRect.overlaps(tmpTileRect)) {
                            return true;
                        }
                    }
                }
            }
        }

        // 2. Check Solid entities
        if (solidEntities == null || solidEntities.size() == 0) {
            return false;
        }

        for (Entity solidEntity : solidEntities) {
            if (solidEntity == self)
                continue;

            Interactible interactible = solidEntity.getComponent(Interactible.class);
            if (interactible != null && interactible.hasCollision()) {
                Transform solidTransform = solidEntity.getComponent(Transform.class);
                if (solidTransform != null) {
                    float solidW = solidTransform.getSize().x * SOLID_HITBOX_WIDTH_FACTOR;
                    float solidH = solidTransform.getSize().y * SOLID_HITBOX_HEIGHT_FACTOR;
                    float solidX = solidTransform.getPosition().x + (solidTransform.getSize().x - solidW) * 0.5f;
                    float solidY = solidTransform.getPosition().y;

                    tmpSolidRect.set(solidX, solidY, solidW, solidH);

                    if (rayRect.overlaps(tmpSolidRect)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOutOfMapBounds(Rectangle rect) {
        if (mapWidth <= 0f || mapHeight <= 0f) {
            return false;
        }
        return rect.x < 0f
                || rect.y < 0f
                || rect.x + rect.width > mapWidth
                || rect.y + rect.height > mapHeight;
    }
}
