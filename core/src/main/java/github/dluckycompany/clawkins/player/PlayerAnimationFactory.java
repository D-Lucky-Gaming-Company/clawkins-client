package github.dluckycompany.clawkins.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.asset.TextureAsset;
import github.dluckycompany.clawkins.component.PlayerAnimation;

/**
 * Factory that slices {@code player.png} (288 × 480 px, 48×48 frames,
 * 6 columns × 10 rows) and builds the four directional walk
 * {@link Animation}s needed by {@link PlayerAnimation}.
 *
 * <h3>player.png row layout (each row = one direction)</h3>
 * <pre>
 *   Row {@value #ROW_SOUTH}  →  walk SOUTH (facing down)
 *   Row {@value #ROW_WEST}   →  walk WEST  (facing left)
 *   Row {@value #ROW_EAST}   →  walk EAST  (facing right)
 *   Row {@value #ROW_NORTH}  →  walk NORTH (facing up)
 * </pre>
 *
 * Slicing is delegated to {@link SpriteSheet}; use
 * {@link SpriteSheet#getRegion(int)} or {@link SpriteSheet#getRegion(int,int)}
 * to pick any single sprite by flat index or (row, col) coordinates.
 *
 * <p>The texture is owned and disposed by {@link AssetService} — this factory
 * never disposes it.
 */
public class PlayerAnimationFactory {

    // -----------------------------------------------------------------------
    // Walk-animation constants
    // -----------------------------------------------------------------------

    /** Seconds each walk frame is displayed. */
    public static final float FRAME_DURATION = 0.15f;

    /**
     * How many columns to include per walk animation.
     * player.png has 6 columns — all are used for the walk cycle.
     * Reduce this if some trailing columns are empty/unused.
     */
    public static final int WALK_FRAME_COUNT = 4;

    // Row indices inside player.png that correspond to each walk direction
    static final int ROW_SOUTH = 0;
    static final int ROW_WEST  = 1;
    static final int ROW_EAST  = 1;
    static final int ROW_NORTH = 2;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    private PlayerAnimationFactory() { /* static factory only */ }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Loads {@link TextureAsset#PLAYER} via {@code assetService}, slices the
     * sheet with {@link SpriteSheet}, and returns a fully configured
     * {@link PlayerAnimation} component ready to attach to an entity.
     *
     * @param assetService the project's asset manager wrapper
     * @return a new {@link PlayerAnimation} component
     */
    public static PlayerAnimation create(AssetService assetService) {
        Texture sheet = assetService.load(TextureAsset.PLAYER);

        // SpriteSheet slices the 288×480 texture into an 18-col × 30-row grid
        // of 16×16 TextureRegions. Use it to select any frame by index or (row,col).
        SpriteSheet sprites = new SpriteSheet(sheet);

        return new PlayerAnimation(
            buildAnimation(sprites, ROW_SOUTH),
            buildAnimation(sprites, ROW_WEST),
            buildAnimation(sprites, ROW_EAST),
            buildAnimation(sprites, ROW_NORTH)
        );
    }

    /**
     * Convenience method: returns a single sprite from the sheet by its
     * zero-based flat {@code index} (index = row × {@link SpriteSheet#COLS} + col).
     *
     * <p>Example — get the very first frame of the south walk row:
     * <pre>
     *   // row 2, col 0  →  flat index = 2 * 6 + 0 = 12
     *   TextureRegion frame = PlayerAnimationFactory.getSprite(assetService, 12);
     * </pre>
     *
     * @param assetService the project's asset manager wrapper
     * @param index        flat sprite index in [0, {@link SpriteSheet#TOTAL})
     * @return the corresponding {@link TextureRegion}
     */
    public static TextureRegion getSprite(AssetService assetService, int index) {
        Texture sheet = assetService.load(TextureAsset.PLAYER);
        return new SpriteSheet(sheet).getRegion(index);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Takes the first {@link #WALK_FRAME_COUNT} columns of {@code row} from
     * {@code sprites} and wraps them in a looping {@link Animation}.
     */
    private static Animation<TextureRegion> buildAnimation(SpriteSheet sprites, int row) {
        // getRow(row, 0, WALK_FRAME_COUNT - 1) returns only the frames we want,
        // ignoring any padding/unused columns at the end of the row.
        TextureRegion[] frames = sprites.getRow(row, 0, WALK_FRAME_COUNT - 1);
        return new Animation<>(FRAME_DURATION, frames);
    }
}
