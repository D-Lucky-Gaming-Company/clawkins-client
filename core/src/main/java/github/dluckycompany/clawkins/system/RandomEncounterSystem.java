package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.asset.MapAsset;
import github.dluckycompany.clawkins.battle.BattleService;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterDifficultyTier;
import github.dluckycompany.clawkins.encounter.EncounterEventBus;
import github.dluckycompany.clawkins.encounter.MapEncounterDifficulty;
import github.dluckycompany.clawkins.encounter.RandomEncounterGenerator;
import github.dluckycompany.clawkins.tiled.TiledService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks overworld travel and rolls for random encounters every two map tiles walked.
 */
public class RandomEncounterSystem extends EntitySystem {
    private static final String TAG = "RandomEncounterSystem";
    private static final float TILES_PER_ROLL = 2f;
    /** Seconds before another random encounter probability roll is allowed after the last attempt. */
    private static final float ROLL_COOLDOWN_SEC = 3f;

    private final TiledService tiledService;
    private final EncounterEventBus encounterEventBus;
    private final RandomEncounterGenerator randomEncounterGenerator;
    private final BattleService battleService;
    private final Vector2 lastPosition = new Vector2();
    private final Vector2 scratch = new Vector2();

    private ImmutableArray<Entity> players;
    private boolean haveLastPosition;
    private float distanceAccumulator;
    private float rollCooldownRemaining;

    public RandomEncounterSystem(
            TiledService tiledService,
            EncounterEventBus encounterEventBus,
            RandomEncounterGenerator randomEncounterGenerator,
            BattleService battleService) {
        this.tiledService = tiledService;
        this.encounterEventBus = encounterEventBus;
        this.randomEncounterGenerator = randomEncounterGenerator;
        this.battleService = battleService;
    }

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(Player.class, Transform.class).get());
    }

    @Override
    public void removedFromEngine(Engine engine) {
        players = null;
    }

    /** Call when the active map changes so travel streaks do not span teleports. */
    public void resetTravelLedger() {
        haveLastPosition = false;
        distanceAccumulator = 0f;
        rollCooldownRemaining = 0f;
    }

    @Override
    public void update(float deltaTime) {
        if (checkProcessing()) {
            rollCooldownRemaining = Math.max(0f, rollCooldownRemaining - deltaTime);
        }
        if (!checkProcessing()) {
            haveLastPosition = false;
            return;
        }
        if (players == null || players.size() == 0) {
            return;
        }
        if (battleService.hasBattleSession() || battleService.isBattleActive()) {
            haveLastPosition = false;
            return;
        }

        TiledMap map = tiledService.getCurrentMap();
        if (map == null) {
            return;
        }

        float tileWorld = tileWorldSize(map);
        if (tileWorld <= 0f) {
            return;
        }

        Entity player = players.first();
        Transform transform = Transform.MAPPER.get(player);
        if (transform == null) {
            return;
        }

        Vector2 pos = transform.getPosition();
        if (!haveLastPosition) {
            lastPosition.set(pos);
            haveLastPosition = true;
            return;
        }

        float moved = scratch.set(pos).sub(lastPosition).len();
        lastPosition.set(pos);
        if (moved <= 0f) {
            return;
        }

        MapAsset mapAsset = resolveMapAsset(map);
        EncounterDifficultyTier tier = MapEncounterDifficulty.tierFor(mapAsset);
        if (tier == EncounterDifficultyTier.NONE) {
            distanceAccumulator = 0f;
            return;
        }

        distanceAccumulator += moved;
        float rollDistance = TILES_PER_ROLL * tileWorld;
        while (distanceAccumulator >= rollDistance) {
            distanceAccumulator -= rollDistance;
            tryRollRandomEncounter(tier);
        }
    }

    private void tryRollRandomEncounter(EncounterDifficultyTier tier) {
        if (battleService.hasBattleSession() || battleService.isBattleActive()) {
            return;
        }
        if (rollCooldownRemaining > 0f) {
            return;
        }
        float chance = tier.encounterChance();
        if (chance <= 0f) {
            return;
        }
        rollCooldownRemaining = ROLL_COOLDOWN_SEC;
        float roll = ThreadLocalRandom.current().nextFloat();
        boolean triggered = roll < chance;
        Gdx.app.log(
                TAG,
                "Random encounter roll -> tier=" + tier
                        + ", roll=" + roll
                        + ", chance=" + chance
                        + ", triggered=" + triggered);
        if (triggered) {
            randomEncounterGenerator.randomEncounter(tier, encounterEventBus);
        }
    }

    private static MapAsset resolveMapAsset(TiledMap map) {
        Object raw = map.getProperties().get("mapAsset");
        if (raw instanceof MapAsset asset) {
            return asset;
        }
        return null;
    }

    private static float tileWorldSize(TiledMap map) {
        int tileW = map.getProperties().get("tilewidth", 16, Integer.class);
        return tileW * Main.UNIT_SCALE;
    }
}
