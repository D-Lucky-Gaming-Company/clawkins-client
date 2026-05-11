package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Marks an enemy whose map graphic comes from a multi-row trainer field sheet
 * ({@code trainer*_field} tilesets). A dedicated {@link TextureRegion} is
 * updated each frame from the sheet so facing matches movement.
 *
 * <p>Tileset animation master IDs (see {@code trainer*_field.tsx}):
 * <ul>
 *   <li>{@code trainer1_field}: south {@value #TRAINER1_SOUTH_MASTER_ID},
 *       side {@value #TRAINER1_SIDE_MASTER_ID}, north {@value #TRAINER1_NORTH_MASTER_ID}</li>
 *   <li>{@code trainer2_field}: south {@value #TRAINER2_SOUTH_MASTER_ID},
 *       side {@value #TRAINER2_SIDE_MASTER_ID}, north {@value #TRAINER2_NORTH_MASTER_ID}</li>
 * </ul>
 * Side sprites face <em>right</em> by default; west uses horizontal mirror.
 */
public final class FieldTrainerWalkSprite implements Component {
    public static final ComponentMapper<FieldTrainerWalkSprite> MAPPER =
            ComponentMapper.getFor(FieldTrainerWalkSprite.class);

    public static final int WALK_FRAMES = 6;
    public static final float FRAME_DURATION_SEC = 0.18f;

    /** Tiled local tile ids for {@code trainer1_field.tsx} walk masters. */
    public static final int TRAINER1_SOUTH_MASTER_ID = 5;
    public static final int TRAINER1_SIDE_MASTER_ID = 6;
    public static final int TRAINER1_NORTH_MASTER_ID = 12;

    /** Tiled local tile ids for {@code trainer2_field.tsx} walk masters. */
    public static final int TRAINER2_SOUTH_MASTER_ID = 0;
    public static final int TRAINER2_SIDE_MASTER_ID = 6;
    public static final int TRAINER2_NORTH_MASTER_ID = 12;

    private final TextureRegion region;
    private final int tilePixelWidth;
    private final int tilePixelHeight;
    private final int sheetColumns;
    private final int southTextureRow;
    private final int sideTextureRow;
    private final int northTextureRow;

    private float animTime;

    public FieldTrainerWalkSprite(
            TextureRegion region,
            int tilePixelWidth,
            int tilePixelHeight,
            int sheetColumns,
            int southMasterTileId,
            int sideMasterTileId,
            int northMasterTileId) {
        this.region = region;
        this.tilePixelWidth = tilePixelWidth;
        this.tilePixelHeight = tilePixelHeight;
        this.sheetColumns = sheetColumns;
        this.southTextureRow = southMasterTileId / sheetColumns;
        this.sideTextureRow = sideMasterTileId / sheetColumns;
        this.northTextureRow = northMasterTileId / sheetColumns;
    }

    public TextureRegion getRegion() {
        return region;
    }

    public int getTilePixelWidth() {
        return tilePixelWidth;
    }

    public int getTilePixelHeight() {
        return tilePixelHeight;
    }

    public int getSheetColumns() {
        return sheetColumns;
    }

    public int getSouthTextureRow() {
        return southTextureRow;
    }

    public int getSideTextureRow() {
        return sideTextureRow;
    }

    public int getNorthTextureRow() {
        return northTextureRow;
    }

    public float getAnimTime() {
        return animTime;
    }

    public void addAnimTime(float delta) {
        animTime += delta;
    }

    public void clearAnimTime() {
        animTime = 0f;
    }
}
