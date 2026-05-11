package github.dluckycompany.clawkins.encounter;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Rectangle;
import github.dluckycompany.clawkins.component.FieldTrainerWalkSprite;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;

/**
 * Detects overlap between the player and encounter-trigger entities.
 */
public class EncounterDetectionSystem extends EntitySystem {
    private static final float PLAYER_FEET_PROBE_Y_FACTOR = 0.08f;
    private static final float PLAYER_HITBOX_WIDTH_FACTOR = 0.26f;
    private static final float PLAYER_HITBOX_HEIGHT_FACTOR = 0.22f;

    private final EncounterEventBus eventBus;
    private final Rectangle triggerRect;
    private final Rectangle playerRect;
    private ImmutableArray<Entity> players;
    private ImmutableArray<Entity> triggers;
    private Entity overlappingTrigger;

    public EncounterDetectionSystem(EncounterEventBus eventBus) {
        this.eventBus = eventBus;
        this.triggerRect = new Rectangle();
        this.playerRect = new Rectangle();
    }

    @Override
    public void addedToEngine(Engine engine) {
        players = engine.getEntitiesFor(Family.all(Player.class, Transform.class).get());
        triggers = engine.getEntitiesFor(Family.all(EncounterTrigger.class, EncounterZone.class, Transform.class).get());
    }

    @Override
    public void update(float deltaTime) {
        if (players == null || players.size() == 0 || triggers == null || triggers.size() == 0) {
            overlappingTrigger = null;
            return;
        }

        Transform playerTransform = Transform.MAPPER.get(players.first());
        float probeX = playerTransform.getPosition().x + playerTransform.getSize().x * 0.5f;
        float probeY = playerTransform.getPosition().y + playerTransform.getSize().y * PLAYER_FEET_PROBE_Y_FACTOR;
        Rectangle playerHitbox = toPlayerHitbox(playerTransform, playerRect);

        Entity currentOverlap = null;
        for (Entity triggerEntity : triggers) {
            Transform triggerTransform = Transform.MAPPER.get(triggerEntity);
            Rectangle tRect = triggerOverlapRect(triggerEntity, triggerTransform, triggerRect);
            boolean feetInside = tRect.contains(probeX, probeY);
            boolean bodyOverlap = tRect.overlaps(playerHitbox);
            if (!feetInside && !bodyOverlap) {
                continue;
            }
            currentOverlap = triggerEntity;
            break;
        }

        if (currentOverlap == null) {
            overlappingTrigger = null;
            return;
        }

        if (currentOverlap == overlappingTrigger) {
            return;
        }

        EncounterZone zone = EncounterZone.MAPPER.get(currentOverlap);
        eventBus.publish(new EncounterEvent(
                EncounterEventType.START_ENCOUNTER,
                zone.getEncounterId(),
                zone.getEncounterTableId(),
                zone.getEnemyLevel(),
                zone.getEnemyHp(),
                zone.getEnemyAttack(),
                zone.getEnemyDefense(),
                zone.getEnemySpeed(),
            zone.getEnemySkills(),
                zone.getEnemyName(),
                zone.getEnemyImagePath()));
        overlappingTrigger = currentOverlap;

        if (zone.isOneShot() && getEngine() != null) {
            getEngine().removeEntity(currentOverlap);
            overlappingTrigger = null;
        }
    }

    private static Rectangle toRect(Transform transform, Rectangle out) {
        return out.set(
                transform.getPosition().x,
                transform.getPosition().y,
                transform.getSize().x,
                transform.getSize().y
        );
    }

    /**
     * Trainer field enemies use a tall sprite rect; overlap uses the same compact bottom-centered
     * hitbox proportions as the player so encounters do not fire from the sprite's empty margins.
     */
    private static Rectangle triggerOverlapRect(Entity triggerEntity, Transform transform, Rectangle out) {
        if (FieldTrainerWalkSprite.MAPPER.get(triggerEntity) != null) {
            return toPlayerHitbox(transform, out);
        }
        return toRect(transform, out);
    }

    private static Rectangle toPlayerHitbox(Transform transform, Rectangle out) {
        float w = transform.getSize().x * PLAYER_HITBOX_WIDTH_FACTOR;
        float h = transform.getSize().y * PLAYER_HITBOX_HEIGHT_FACTOR;
        float x = transform.getPosition().x + (transform.getSize().x - w) * 0.5f;
        float y = transform.getPosition().y;
        return out.set(x, y, w, h);
    }
}
