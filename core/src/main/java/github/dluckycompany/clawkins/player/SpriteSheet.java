package github.dluckycompany.clawkins.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Wraps a {@link Texture} spritesheet and provides frame access by either
 * (row, col) coordinates or a flat zero-based index.
 *
 * <h3>Layout assumed for player.png (288 × 480 px)</h3>
 * <pre>
 *   Frame size : {@value #FRAME_W} × {@value #FRAME_H} px
 *   Columns    : {@value #COLS}   (288 / 48)
 *   Rows       : {@value #ROWS}   (480 / 48)
 *   Total      : 60 sprites
 *
 *   Flat index calculation:
 *     index = row * COLS + col
 *     col   = index % COLS
 *     row   = index / COLS
 * </pre>
 *
 * <p>This class does <em>not</em> own or dispose the underlying texture.
 */
public class SpriteSheet {

    // -----------------------------------------------------------------------
    // Sheet constants — derived from the measured pixel dimensions
    // -----------------------------------------------------------------------

    /** Pixel width of every individual sprite frame. */
    public static final int FRAME_W = 48;
    // 104 
    
    /** Pixel height of every individual sprite frame. */
    public static final int FRAME_H = 48;
    // 864

    /** Number of columns in player.png  (288 / 48). */
    public static final int COLS = 6;

    /** Number of rows in player.png  (480 / 48). */
    public static final int ROWS = 6;

    /** Total number of sprites on the sheet. */
    public static final int TOTAL = COLS * ROWS; // 60

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    /** The sliced grid; access as {@code grid[row][col]}. */
    private final TextureRegion[][] grid;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Slices {@code texture} into a {@link #ROWS} × {@link #COLS} grid of
     * {@link #FRAME_W}×{@link #FRAME_H} regions.
     *
     * @param texture the spritesheet texture (not disposed by this class)
     */
    public SpriteSheet(Texture texture) {
        this.grid = TextureRegion.split(texture, FRAME_W, FRAME_H);
    }

    // -----------------------------------------------------------------------
    // Public API — by (row, col)
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link TextureRegion} at {@code (row, col)}.
     *
     * @param row  0-based row index  (0 … {@value #ROWS}-1)
     * @param col  0-based column index  (0 … {@value #COLS}-1)
     * @throws IndexOutOfBoundsException if either coordinate is out of range
     */
    public TextureRegion getRegion(int row, int col) {
        validateRow(row);
        validateCol(col);
        return grid[row][col];
    }

    /**
     * Returns all columns in {@code row} as a contiguous array — convenient
     * for building an {@link com.badlogic.gdx.graphics.g2d.Animation}.
     *
     * @param row 0-based row index
     * @throws IndexOutOfBoundsException if {@code row} is out of range
     */
    public TextureRegion[] getRow(int row) {
        validateRow(row);
        return grid[row];
    }

    /**
     * Returns a sub-range of columns within {@code row}.
     * Useful when only the first <em>n</em> frames of a row are valid.
     *
     * @param row      0-based row index
     * @param fromCol  first column to include (inclusive)
     * @param toCol    last column to include (inclusive)
     * @throws IndexOutOfBoundsException if any coordinate is out of range
     */
    public TextureRegion[] getRow(int row, int fromCol, int toCol) {
        validateRow(row);
        validateCol(fromCol);
        validateCol(toCol);
        int length = toCol - fromCol + 1;
        TextureRegion[] slice = new TextureRegion[length];
        System.arraycopy(grid[row], fromCol, slice, 0, length);
        return slice;
    }

    // -----------------------------------------------------------------------
    // Public API — by flat index
    // -----------------------------------------------------------------------

    /**
     * Returns the sprite at the given flat {@code index} where:
     * <pre>
     *   index = row * {@value #COLS} + col
     * </pre>
     *
     * @param index 0-based flat index (0 … {@value #TOTAL}-1)
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public TextureRegion getRegion(int index) {
        if (index < 0 || index >= TOTAL) {
            throw new IndexOutOfBoundsException(
                "Sprite index " + index + " is out of range [0, " + (TOTAL - 1) + "]");
        }
        return grid[index / COLS][index % COLS];
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Returns the full sliced grid for direct access when needed. */
    public TextureRegion[][] getGrid() {
        return grid;
    }

    // -----------------------------------------------------------------------
    // Validation helpers
    // -----------------------------------------------------------------------

    private static void validateRow(int row) {
        if (row < 0 || row >= ROWS) {
            throw new IndexOutOfBoundsException(
                "Row " + row + " is out of range [0, " + (ROWS - 1) + "]");
        }
    }

    private static void validateCol(int col) {
        if (col < 0 || col >= COLS) {
            throw new IndexOutOfBoundsException(
                "Column " + col + " is out of range [0, " + (COLS - 1) + "]");
        }
    }
}
