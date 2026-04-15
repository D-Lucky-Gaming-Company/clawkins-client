package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.PlayerAnimation;
import github.dluckycompany.clawkins.component.PlayerAnimation.Direction;

/**
 * Reads WASD keyboard input each frame and writes the result into the player
 * entity's {@link Move} and {@link PlayerAnimation} components.
 *
 * <p><b>Responsibilities</b>
 * <ul>
 *   <li>Compute a normalised movement direction from WASD keys.</li>
 *   <li>Set the correct {@link Direction} and {@code moving} flag on
 *       {@link PlayerAnimation} so the AnimationSystem shows the right
 *       walk cycle.</li>
 * </ul>
 *
 * <p>This system does <em>not</em> move the entity itself — that is done by
 * the existing {@link MoveSystem} which runs after this one.
 */
public class PlayerInputSystem extends IteratingSystem {
    private static final float DEFAULT_SPRINT_MULTIPLIER = 1.6f;
    private static final float SHIFT_SLOW_MULTIPLIER = 0.55f;

    public PlayerInputSystem() {
        // Requires: Player tag + Move (to write direction)
        // PlayerAnimation is optional — checked at runtime with MAPPER
        super(Family.all(Player.class, Move.class).get(),
              /* priority — run before MoveSystem (lower = earlier) */ 0);
    }

    // -----------------------------------------------------------------------
    // IteratingSystem
    // -----------------------------------------------------------------------

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Move move = Move.MAPPER.get(entity);
        boolean slowWalk = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
        float sprintSpeed = move.getBaseSpeed() * DEFAULT_SPRINT_MULTIPLIER;
        move.setMaxSpeed(slowWalk ? sprintSpeed * SHIFT_SLOW_MULTIPLIER : sprintSpeed);

        // ── 1. Read WASD ────────────────────────────────────────────────────
        float dx = 0f, dy = 0f;
        if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP))    dy += 1f;
        if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN))  dy -= 1f;
        if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) dx += 1f;
        if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT))  dx -= 1f;

        boolean isMoving = dx != 0f || dy != 0f;

        // Normalise so diagonal movement is not faster
        Vector2 direction = move.getDirection();
        if (isMoving) {
            direction.set(dx, dy).nor();
        } else {
            direction.setZero();
        }

        // ── 2. Update animation state ────────────────────────────────────────
        PlayerAnimation anim = PlayerAnimation.MAPPER.get(entity);
        if (anim == null) return;

        anim.setMoving(isMoving);

        if (isMoving) {
            // Prefer vertical direction for row selection, matching typical RPG sheets
            if      (dy < 0) anim.setDirection(Direction.SOUTH);
            else if (dy > 0) anim.setDirection(Direction.NORTH);
            else if (dx < 0) anim.setDirection(Direction.WEST);
            else             anim.setDirection(Direction.EAST);
        }
    }
}
