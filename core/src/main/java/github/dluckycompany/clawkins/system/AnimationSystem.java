package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.PlayerAnimation;
import github.dluckycompany.clawkins.component.PlayerAnimation.Direction;

/**
 * Advances every entity's {@link PlayerAnimation} by {@code deltaTime} and
 * writes the resulting key-frame back into its {@link Graphic} component so
 * the {@link RenderSystem} always draws the correct animation frame.
 *
 * <p>Run order (Ashley priority): after {@link PlayerInputSystem} (which sets
 * the direction / moving flag) but before {@link RenderSystem}.
 */
public class AnimationSystem extends IteratingSystem {

    public AnimationSystem() {
        // Priority 1 — runs after PlayerInputSystem (priority 0), before RenderSystem
        super(Family.all(PlayerAnimation.class, Graphic.class).get(), 1);
    }

    // -----------------------------------------------------------------------
    // IteratingSystem
    // -----------------------------------------------------------------------

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerAnimation anim    = PlayerAnimation.MAPPER.get(entity);
        Graphic         graphic = Graphic.MAPPER.get(entity);

        // Advance the animation timer and fetch the key-frame for this tick
        TextureRegion frame = anim.update(deltaTime);
        // Mirror the frame horizontally when facing WEST
        if (anim.getDirection() == Direction.WEST) {
            // Only flip if not already flipped — avoids double-flipping each frame
            if (!frame.isFlipX()) {
                frame.flip(true, false);
            }
        } else {
            // Restore the frame if it was previously flipped
            if (frame.isFlipX()) {
                frame.flip(true, false);
            }
        }
        graphic.setRegion(frame);
    }
}
