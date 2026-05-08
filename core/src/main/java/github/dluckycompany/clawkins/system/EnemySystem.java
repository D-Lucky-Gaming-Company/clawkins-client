package github.dluckycompany.clawkins.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.AudioEventType;
import github.dluckycompany.clawkins.audio.AudioService;
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
    private static final float[] CHASE_STEER_ANGLES = { 15f, -15f, 30f, -30f, 45f, -45f, 65f, -65f, 90f, -90f };
    private static final float ENEMY_HITBOX_WIDTH_FACTOR = 0.28f;
    private static final float ENEMY_HITBOX_HEIGHT_FACTOR = 0.24f;
    private static final float PLAYER_FEET_PROBE_Y_FACTOR = 0.08f;
    private static final int PATH_NODE_LIMIT = 800;
    private static final int[][] CARDINAL_DIRS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
    private static final int PATH_SWEEP_SAMPLES_PER_EDGE = 6;
    private static final float HOME_REACHED_DISTANCE = 0.12f;

    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> solidEntities;
    private final AudioService audioService;

    private final List<TiledMapTileLayer> collisionLayers;
    private final List<BarrierShape> barrierHitboxes;
    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle tmpTileRect = new Rectangle();
    private final Rectangle tmpSolidRect = new Rectangle();
    private final Vector2 tmpVec = new Vector2();
    private final Vector2 tmpDir = new Vector2();
    private final Polygon tmpProbePolygon = new Polygon();
    private final float[] tmpProbeVertices = new float[8];

    private float mapWidth;
    private float mapHeight;
    private float tileWorldWidth;
    private float tileWorldHeight;

    // Using simple hitboxes for raycasting validation mirroring the MoveSystem
    private static final float TILE_HITBOX_WIDTH_FACTOR = 1f;
    private static final float TILE_HITBOX_HEIGHT_FACTOR = 1f;
    private static final float SOLID_HITBOX_WIDTH_FACTOR = 1f;
    private static final float SOLID_HITBOX_HEIGHT_FACTOR = 1f;
    private static final float PLAYER_HITBOX_WIDTH_FACTOR = 0.25f;
    private static final float PLAYER_HITBOX_HEIGHT_FACTOR = 0.25f;
    private static final float PLAYER_HITBOX_Y_OFFSET_FACTOR = 0.06f;

    private record PathNode(long key, int col, int row, float fScore) implements Comparable<PathNode> {
        @Override
        public int compareTo(PathNode other) {
            return Float.compare(this.fScore, other.fScore);
        }
    }

    public EnemySystem(AudioService audioService) {
        super(Family.all(Enemy.class, Move.class, Transform.class).get());
        this.audioService = audioService;
        this.collisionLayers = new ArrayList<>();
        this.barrierHitboxes = new ArrayList<>();
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.players = engine.getEntitiesFor(Family.all(Player.class, Transform.class).get());
        this.solidEntities = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
    }

    public void setMap(TiledMap tiledMap) {
        this.collisionLayers.clear();
        this.barrierHitboxes.clear();
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        this.mapWidth = width * tileW * Main.UNIT_SCALE;
        this.mapHeight = height * tileH * Main.UNIT_SCALE;
        this.tileWorldWidth = tileW * Main.UNIT_SCALE;
        this.tileWorldHeight = tileH * Main.UNIT_SCALE;

        for (MapLayer layer : tiledMap.getLayers()) {
            collectBarrierObjects(layer);
            if (layer instanceof TiledMapTileLayer tileLayer) {
                // Tile collision objects should block regardless of visual layer placement.
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

        if (isTerritorialChaser(enemy)
                && distanceFromHome(transform, enemy) > enemy.getTerritorialChaseDistance()) {
            enemy.setReturningToHome(true);
        }

        if (enemy.isReturningToHome()) {
            if (updateReturnToHome(enemy, move, transform, entity)) {
                enemy.setReturningToHome(false);
            }
            return;
        }

        boolean hasChaseTarget = enemy.canChase()
                && targetPlayer != null
                && (seesPlayer || enemy.getChaseMemoryTimer() > 0f);
        if (hasChaseTarget) {
            if (isTerritorialChaser(enemy)
                    && distanceFromHome(transform, enemy) >= enemy.getTerritorialChaseDistance()) {
                enemy.setReturningToHome(true);
                enemy.setChaseMemoryTimer(0f);
                move.getDirection().setZero();
                move.setMaxSpeed(0f);
                return;
            }
            Transform targetTransform = targetPlayer.getComponent(Transform.class);
            if (enemy.getState() != Enemy.State.CHASING) {
                if (enemy.getState() != Enemy.State.ALERTED) {
                    beginAlertState(enemy, move, transform, targetTransform);
                    return;
                }

                move.getDirection().setZero();
                move.setMaxSpeed(0f);
                if (targetTransform != null) {
                    updateFacingDirection(enemy, centerOf(targetTransform), centerOf(transform));
                }
                enemy.setAlertPauseTimer(enemy.getAlertPauseTimer() - deltaTime);
                if (enemy.getAlertPauseTimer() > 0f) {
                    return;
                }
            }

            enemy.setState(Enemy.State.CHASING);
            enemy.setIdlingBetweenRoams(false);
            enemy.setIdleTimer(0f);
            move.setMaxSpeed(enemy.getChasingSpeed());
            if (targetTransform != null) {
                updateChaseDirection(enemy, move, transform, targetTransform, entity);
                updateFacingDirectionFromMove(enemy, move.getDirection());
            }
            return;
        }

        enemy.setAlertPauseTimer(0f);

        if (enemy.canRoam()) {
            handleRoamIdleCycle(enemy, move, transform, deltaTime, entity);
            return;
        }

        enemy.setState(Enemy.State.IDLE);
        move.getDirection().setZero();
        move.setMaxSpeed(0f);
    }

    private void beginAlertState(Enemy enemy, Move move, Transform enemyTransform, Transform targetTransform) {
        enemy.setState(Enemy.State.ALERTED);
        enemy.setAlertPauseTimer(enemy.getAlertPauseDuration());
        move.getDirection().setZero();
        move.setMaxSpeed(0f);
        if (targetTransform != null) {
            updateFacingDirection(enemy, centerOf(targetTransform), centerOf(enemyTransform));
        }
        if (audioService != null) {
            audioService.onEvent(AudioEventType.ENEMY_ALERT_STARTED);
        }
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
                && !canRoamAlong(enemy, transform, move.getDirection(), enemy.getRoamDecisionDistance(), self)) {
            chooseRoamDirection(enemy, move, transform, self);
        }
    }

    private void chooseRoamDirection(Enemy enemy, Move move, Transform transform, Entity self) {
        for (int i = 0; i < ROAM_DIRECTION_ATTEMPTS; i++) {
            float angle = MathUtils.random(0f, 360f);
            tmpDir.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).nor();
            if (!canRoamAlong(enemy, transform, tmpDir, enemy.getRoamDecisionDistance(), self)) {
                continue;
            }
            move.getDirection().set(tmpDir);
            updateFacingDirectionFromMove(enemy, move.getDirection());
            return;
        }
        move.getDirection().setZero();
    }

    private boolean updateReturnToHome(Enemy enemy, Move move, Transform transform, Entity self) {
        Vector2 homeCenter = homeCenterOf(enemy, transform);
        Vector2 currentCenter = centerOf(transform);
        tmpDir.set(homeCenter).sub(currentCenter);
        if (tmpDir.len2() <= HOME_REACHED_DISTANCE * HOME_REACHED_DISTANCE) {
            transform.getPosition().set(enemy.getHomePosition());
            enemy.setState(Enemy.State.IDLE);
            move.getDirection().setZero();
            move.setMaxSpeed(0f);
            return true;
        }

        Vector2 desired = tmpDir.nor();
        Vector2 chosen = findBestReturnDirection(desired, transform, enemy, self);
        if (chosen.isZero()) {
            move.getDirection().setZero();
            move.setMaxSpeed(0f);
            enemy.setState(Enemy.State.IDLE);
            return false;
        }

        enemy.setState(Enemy.State.CHASING);
        move.setMaxSpeed(returnSpeedFor(enemy));
        move.getDirection().set(chosen);
        updateFacingDirectionFromMove(enemy, chosen);
        return false;
    }

    private Vector2 findBestReturnDirection(Vector2 desired, Transform enemyTransform, Enemy enemy, Entity self) {
        if (canMoveAlong(enemyTransform, desired, enemy.getChaseProbeDistance(), self)) {
            return new Vector2(desired);
        }
        for (float angle : CHASE_STEER_ANGLES) {
            Vector2 candidate = rotatedDirection(desired, angle);
            if (canMoveAlong(enemyTransform, candidate, enemy.getChaseProbeDistance(), self)) {
                return new Vector2(candidate);
            }
        }
        return new Vector2();
    }

    private boolean canRoamAlong(Enemy enemy, Transform transform, Vector2 direction, float distance, Entity self) {
        if (!canMoveAlong(transform, direction, distance, self)) {
            return false;
        }
        if (!enemy.isTerritorial()) {
            return true;
        }
        float nextX = transform.getPosition().x + direction.x * distance;
        float nextY = transform.getPosition().y + direction.y * distance;
        return distanceFromHome(nextX, nextY, transform, enemy) <= enemy.getTerritorialRoamRadius();
    }

    private boolean isTerritorialChaser(Enemy enemy) {
        return enemy.isTerritorial() && enemy.canChase();
    }

    private float returnSpeedFor(Enemy enemy) {
        if (enemy.getRoamingSpeed() > 0f) {
            return enemy.getRoamingSpeed();
        }
        return enemy.getChasingSpeed();
    }

    private float distanceFromHome(Transform transform, Enemy enemy) {
        return distanceFromHome(transform.getPosition().x, transform.getPosition().y, transform, enemy);
    }

    private float distanceFromHome(float x, float y, Transform transform, Enemy enemy) {
        float centerX = x + transform.getSize().x * 0.5f;
        float centerY = y + transform.getSize().y * 0.5f;
        Vector2 homeCenter = homeCenterOf(enemy, transform);
        return Vector2.dst(centerX, centerY, homeCenter.x, homeCenter.y);
    }

    private Vector2 homeCenterOf(Enemy enemy, Transform transform) {
        return new Vector2(
                enemy.getHomePosition().x + transform.getSize().x * 0.5f,
                enemy.getHomePosition().y + transform.getSize().y * 0.5f);
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
        Vector2 chosen = findBestChaseDirection(desired, enemyTransform, playerTransform, enemy, self);
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
        float hitboxY = playerY + playerH * PLAYER_HITBOX_Y_OFFSET_FACTOR;
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

    private Vector2 findBestChaseDirection(
            Vector2 desired,
            Transform enemyTransform,
            Transform playerTransform,
            Enemy enemy,
            Entity self) {
        if (canMoveAlong(enemyTransform, desired, enemy.getChaseProbeDistance(), self)) {
            return new Vector2(desired);
        }

        Vector2 pathDirection = findPathDirection(enemyTransform, playerTransform, self);
        if (!pathDirection.isZero() && canMoveAlong(enemyTransform, pathDirection, enemy.getChaseProbeDistance(), self)) {
            return pathDirection;
        }

        for (float angle : CHASE_STEER_ANGLES) {
            Vector2 candidate = rotatedDirection(desired, angle);
            if (!canMoveAlong(enemyTransform, candidate, enemy.getChaseProbeDistance(), self)) {
                continue;
            }
            return new Vector2(candidate);
        }
        return new Vector2();
    }

    private Vector2 findPathDirection(Transform enemyTransform, Transform playerTransform, Entity self) {
        if (tileWorldWidth <= 0f || tileWorldHeight <= 0f) {
            return new Vector2();
        }

        Vector2 enemyCenter = enemyHitboxCenterOf(enemyTransform);
        Vector2 playerCenter = centerOf(playerTransform);
        int startCol = worldToCol(enemyCenter.x);
        int startRow = worldToRow(enemyCenter.y);
        int goalCol = worldToCol(playerCenter.x);
        int goalRow = worldToRow(playerCenter.y);

        if (!isInGrid(startCol, startRow) || !isInGrid(goalCol, goalRow)) {
            return new Vector2();
        }
        if (startCol == goalCol && startRow == goalRow) {
            return new Vector2(playerCenter).sub(enemyCenter).nor();
        }

        long startKey = keyOf(startCol, startRow);
        long goalKey = keyOf(goalCol, goalRow);

        PriorityQueue<PathNode> open = new PriorityQueue<>();
        Set<Long> closed = new HashSet<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Float> gScore = new HashMap<>();

        gScore.put(startKey, 0f);
        open.add(new PathNode(startKey, startCol, startRow, heuristic(startCol, startRow, goalCol, goalRow)));

        int expanded = 0;
        boolean found = false;
        while (!open.isEmpty() && expanded < PATH_NODE_LIMIT) {
            PathNode node = open.poll();
            if (!closed.add(node.key())) {
                continue;
            }
            expanded++;

            if (node.key() == goalKey) {
                found = true;
                break;
            }

            float currentG = gScore.getOrDefault(node.key(), Float.MAX_VALUE);
            for (int[] dir : CARDINAL_DIRS) {
                int nextCol = node.col() + dir[0];
                int nextRow = node.row() + dir[1];
                if (!isInGrid(nextCol, nextRow)) {
                    continue;
                }
                if (!canTraverseGridEdge(node.col(), node.row(), nextCol, nextRow, enemyTransform, self)
                        && !(nextCol == goalCol && nextRow == goalRow)) {
                    continue;
                }

                long nextKey = keyOf(nextCol, nextRow);
                if (closed.contains(nextKey)) {
                    continue;
                }

                float tentativeG = currentG + 1f;
                float knownG = gScore.getOrDefault(nextKey, Float.MAX_VALUE);
                if (tentativeG >= knownG) {
                    continue;
                }
                cameFrom.put(nextKey, node.key());
                gScore.put(nextKey, tentativeG);
                float fScore = tentativeG + heuristic(nextCol, nextRow, goalCol, goalRow);
                open.add(new PathNode(nextKey, nextCol, nextRow, fScore));
            }
        }

        if (!found || !cameFrom.containsKey(goalKey)) {
            return new Vector2();
        }

        long stepKey = goalKey;
        while (cameFrom.containsKey(stepKey) && cameFrom.get(stepKey) != startKey) {
            stepKey = cameFrom.get(stepKey);
        }

        int stepCol = colOf(stepKey);
        int stepRow = rowOf(stepKey);
        Vector2 stepCenter = gridCenter(stepCol, stepRow);
        return stepCenter.sub(enemyCenter).nor();
    }

    private boolean isGridCellWalkable(int col, int row, Transform enemyTransform, Entity self) {
        Vector2 center = gridCenter(col, row);
        Rectangle hitbox = enemyHitboxAtCenter(center.x, center.y, enemyTransform, tmpRect);
        if (isOutOfMapBounds(hitbox)) {
            return false;
        }
        return !isPointObstructed(hitbox, self);
    }

    private boolean canTraverseGridEdge(
            int fromCol,
            int fromRow,
            int toCol,
            int toRow,
            Transform enemyTransform,
            Entity self) {
        if (!isGridCellWalkable(toCol, toRow, enemyTransform, self)) {
            return false;
        }
        Vector2 from = gridCenter(fromCol, fromRow);
        Vector2 to = gridCenter(toCol, toRow);
        for (int i = 1; i <= PATH_SWEEP_SAMPLES_PER_EDGE; i++) {
            float t = (float) i / PATH_SWEEP_SAMPLES_PER_EDGE;
            float x = MathUtils.lerp(from.x, to.x, t);
            float y = MathUtils.lerp(from.y, to.y, t);
            Rectangle sweepHitbox = enemyHitboxAtCenter(x, y, enemyTransform, tmpRect);
            if (isOutOfMapBounds(sweepHitbox) || isPointObstructed(sweepHitbox, self)) {
                return false;
            }
        }
        return true;
    }

    private int worldToCol(float worldX) {
        return (int) Math.floor(worldX / tileWorldWidth);
    }

    private int worldToRow(float worldY) {
        return (int) Math.floor(worldY / tileWorldHeight);
    }

    private Vector2 gridCenter(int col, int row) {
        return new Vector2((col + 0.5f) * tileWorldWidth, (row + 0.5f) * tileWorldHeight);
    }

    private boolean isInGrid(int col, int row) {
        if (tileWorldWidth <= 0f || tileWorldHeight <= 0f || mapWidth <= 0f || mapHeight <= 0f) {
            return false;
        }
        int cols = Math.max(1, (int) Math.floor(mapWidth / tileWorldWidth));
        int rows = Math.max(1, (int) Math.floor(mapHeight / tileWorldHeight));
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    private static long keyOf(int col, int row) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }

    private static int colOf(long key) {
        return (int) key;
    }

    private static int rowOf(long key) {
        return (int) (key >> 32);
    }

    private static float heuristic(int col, int row, int goalCol, int goalRow) {
        return Math.abs(goalCol - col) + Math.abs(goalRow - row);
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

    private Rectangle enemyHitboxAtCenter(float centerX, float centerY, Transform enemyTransform, Rectangle outRect) {
        float hitboxWidth = enemyTransform.getSize().x * ENEMY_HITBOX_WIDTH_FACTOR;
        float hitboxHeight = enemyTransform.getSize().y * ENEMY_HITBOX_HEIGHT_FACTOR;
        float hitboxX = centerX - hitboxWidth * 0.5f;
        float hitboxY = centerY - hitboxHeight * 0.5f;
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

            // Use a slightly wider probe so thin walls cannot be "seen through".
            tmpRect.set(tmpVec.x - 0.28f, tmpVec.y - 0.28f, 0.56f, 0.56f);

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

    private Vector2 enemyHitboxCenterOf(Transform enemyTransform) {
        float width = enemyTransform.getSize().x;
        float height = enemyTransform.getSize().y;
        float hitboxWidth = width * ENEMY_HITBOX_WIDTH_FACTOR;
        float hitboxHeight = height * ENEMY_HITBOX_HEIGHT_FACTOR;
        float hitboxX = enemyTransform.getPosition().x + (width - hitboxWidth) * 0.5f;
        float hitboxY = enemyTransform.getPosition().y;
        return new Vector2(hitboxX + hitboxWidth * 0.5f, hitboxY + hitboxHeight * 0.5f);
    }

    private boolean isPointObstructed(Rectangle rayRect, Entity self) {
        if (isOutOfMapBounds(rayRect)) {
            return true;
        }

        if (isBlockedByBarrierObjects(rayRect)) {
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

    private boolean isBlockedByBarrierObjects(Rectangle probeRect) {
        if (barrierHitboxes.isEmpty()) {
            return false;
        }
        for (BarrierShape barrierRect : barrierHitboxes) {
            if (barrierRect.overlaps(probeRect, tmpProbePolygon, tmpProbeVertices)) {
                return true;
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
                tmpProbeVertices[0] = probe.x;
                tmpProbeVertices[1] = probe.y;
                tmpProbeVertices[2] = probe.x + probe.width;
                tmpProbeVertices[3] = probe.y;
                tmpProbeVertices[4] = probe.x + probe.width;
                tmpProbeVertices[5] = probe.y + probe.height;
                tmpProbeVertices[6] = probe.x;
                tmpProbeVertices[7] = probe.y + probe.height;
                tmpProbePolygon.setVertices(tmpProbeVertices);
                return Intersector.overlapConvexPolygons(polygon, tmpProbePolygon);
            }
            return false;
        }
    }
}
