package github.dluckycompany.clawkins.system;

import java.util.function.Supplier;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;

import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;

/**
 * After {@link PlayerInputSystem}, caps the player move speed on the Cerberus bridge
 * (0.5× base walk after atmosphere 0, 0.25× after atmosphere 1) while still allowing WASD control.
 */
public class CerberusBridgeWalkSlowSystem extends IteratingSystem {

    private static final float ATMOS0_SPEED_FACTOR = 0.5f;
    private static final float ATMOS1_SPEED_FACTOR = 0.25f;

    private final Supplier<Boolean> bridgeTensionActive;
    private final Supplier<Boolean> atmos1DeepTension;

    public CerberusBridgeWalkSlowSystem(Supplier<Boolean> bridgeTensionActive, Supplier<Boolean> atmos1DeepTension) {
        super(Family.all(Player.class, Move.class).get(), 5);
        this.bridgeTensionActive = bridgeTensionActive;
        this.atmos1DeepTension = atmos1DeepTension;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (!Boolean.TRUE.equals(bridgeTensionActive.get())) {
            return;
        }
        Move move = Move.MAPPER.get(entity);
        if (move == null) {
            return;
        }
        float factor = Boolean.TRUE.equals(atmos1DeepTension.get()) ? ATMOS1_SPEED_FACTOR : ATMOS0_SPEED_FACTOR;
        float cap = move.getBaseSpeed() * factor;
        move.setMaxSpeed(Math.min(move.getMaxSpeed(), cap));
    }
}
