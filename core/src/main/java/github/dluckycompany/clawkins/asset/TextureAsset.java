package github.dluckycompany.clawkins.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.Texture;

/**
 * Enum of every standalone {@link Texture} asset used in the game.
 *
 * Usage:
 * <pre>
 *   Texture tex = assetService.load(TextureAsset.PLAYER);
 * </pre>
 *
 * Add a new entry here whenever you introduce a new sprite sheet or texture.
 */
public enum TextureAsset implements Asset<Texture> {

    /**
     * The player character spritesheet.
     * Expected layout: 4 columns × 4 rows, 16×16 px per frame.
     *   Row 0 – walk south (down)
     *   Row 1 – walk west  (left)
     *   Row 2 – walk east  (right)
     *   Row 3 – walk north (up)
     */
    PLAYER("maps/characters/player.png"),

    /**
     * Battle UI icon spritesheet.
     * Used by {@link github.dluckycompany.clawkins.battle.BattleHud} to render
     * Attack / Defend / Item buttons on the Scene2D Stage.
     * Expected layout: 3 columns × 1 row, one 64×64 icon per cell.
     *   Col 0 – Attack icon
     *   Col 1 – Defend icon
     *   Col 2 – Item icon
     */
    BATTLE_UI("ui/BattleUI_sheet.png"),

    /**
     * Full-screen battle background image (1280 × 720 px, 16:9).
     * Displayed by {@link github.dluckycompany.clawkins.battle.BattleHud}
     * when a skeleton encounter starts.
     */
    BATTLE_BACKGROUND("ui/battle_background.png");

    // -------------------------------------------------------------------------

    private final AssetDescriptor<Texture> descriptor;

    TextureAsset(String path) {
        this.descriptor = new AssetDescriptor<>(path, Texture.class);
    }

    @Override
    public AssetDescriptor<Texture> getDescriptor() {
        return descriptor;
    }
}
