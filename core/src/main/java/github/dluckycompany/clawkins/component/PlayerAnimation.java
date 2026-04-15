package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Holds all walking animations for an entity and tracks playback state.
 *
 * <p>Four directional walk cycles are stored (south / west / east / north).
 * The current animation is selected by {@link #setDirection(Direction)}.
 * A separate {@code stateTime} accumulator drives the frame selection inside
 * {@link com.badlogic.gdx.graphics.g2d.Animation#getKeyFrame(float, boolean)}.
 *
 * <p>The {@link github.dluckycompany.clawkins.system.AnimationSystem} writes the
 * current key-frame back into the entity's {@link Graphic} component each tick.
 */
public class PlayerAnimation implements Component {

    public static final ComponentMapper<PlayerAnimation> MAPPER =
            ComponentMapper.getFor(PlayerAnimation.class);

    // -----------------------------------------------------------------------
    // Inner types
    // -----------------------------------------------------------------------

    /** The four cardinal movement directions. */
    public enum Direction { SOUTH, WEST, EAST, NORTH }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final Animation<TextureRegion> walkSouth;
    private final Animation<TextureRegion> walkWest;
    private final Animation<TextureRegion> walkEast;
    private final Animation<TextureRegion> walkNorth;

    /** Accumulated seconds since this animation started playing. */
    private float stateTime = 0f;

    /** Which directional animation is currently active. */
    private Direction direction = Direction.SOUTH;

    /** Whether the entity is actually moving (pauses animation when idle). */
    private boolean moving = false;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public PlayerAnimation(
            Animation<TextureRegion> walkSouth,
            Animation<TextureRegion> walkWest,
            Animation<TextureRegion> walkEast,
            Animation<TextureRegion> walkNorth) {
        this.walkSouth = walkSouth;
        this.walkWest  = walkWest;
        this.walkEast  = walkEast;
        this.walkNorth = walkNorth;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Advances the state timer and returns the correct key-frame for the
     * current direction.  Call this once per frame from the AnimationSystem.
     *
     * @param delta seconds since last frame
     * @return the {@link TextureRegion} to display
     */
    public TextureRegion update(float delta) {
        if (moving) {
            stateTime += delta;
        }
        return currentAnimation().getKeyFrame(stateTime, true);
    }

    /** Returns the first (idle) frame of the current direction without advancing time. */
    public TextureRegion idleFrame() {
        return currentAnimation().getKeyFrame(0f, false);
    }

    // -----------------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------------

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.direction = direction;
            this.stateTime = 0f; // reset timer on direction change
        }
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        if (this.moving && !moving) {
            stateTime = 0f; // snap back to idle frame when stopping
        }
        this.moving = moving;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Animation<TextureRegion> currentAnimation() {
        return switch (direction) {
            case SOUTH -> walkSouth;
            case WEST  -> walkWest;
            case EAST  -> walkEast;
            case NORTH -> walkNorth;
        };
    }
}
