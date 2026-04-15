package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Battle screen — renders a Stage-based battle layout with combatant sprites.
 *
 * <h3>Combatant positions</h3>
 * <pre>
 *   ┌─────────────────────────────────────────────┐
 *   │                             [  BOSS  ]       │  ← upper-right
 *   │                                              │
 *   │  [ PLAYER ]                                  │  ← lower-left
 *   └─────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>Swapping to real sprites</h3>
 * Replace the {@code Texture} loaded in {@link #loadCombatants()} with your
 * final spritesheet and swap {@code Image} for an {@code AnimatedImage} /
 * custom actor when ready.  Everything else (position, stage wiring,
 * disposal) stays the same.
 */
public class BattleScreen extends ScreenAdapter {

    // -----------------------------------------------------------------------
    // Asset paths — change these to the real sprite paths when ready
    // -----------------------------------------------------------------------

    /** Path to the player placeholder (or final) sprite. */
    private static final String PLAYER_TEXTURE_PATH = "maps/characters/player_placeholder.png";

    /** Path to the boss placeholder (or final) sprite. */
    private static final String BOSS_TEXTURE_PATH = "maps/characters/boss_placeholder.png";

    // -----------------------------------------------------------------------
    // Desired display sizes (pixels at screen resolution)
    // -----------------------------------------------------------------------

    private static final float PLAYER_WIDTH  = 96f;
    private static final float PLAYER_HEIGHT = 96f;
    private static final float BOSS_WIDTH    = 128f;
    private static final float BOSS_HEIGHT   = 128f;

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private Stage   stage;

    /** Textures owned by this screen — disposed in {@link #dispose()}. */
    private Texture playerTexture;
    private Texture bossTexture;

    /**
     * Scene2D Image actors.  To swap in a real sprite later, replace the
     * {@link TextureRegionDrawable} on these actors — no other code changes needed.
     */
    private Image playerImage;
    private Image bossImage;

    // -----------------------------------------------------------------------
    // ScreenAdapter lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        loadCombatants();
        positionCombatants();
        stage.addActor(playerImage);
        stage.addActor(bossImage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        // Re-position combatants whenever the window is resized
        positionCombatants();
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (playerTexture != null) playerTexture.dispose();
        if (bossTexture   != null) bossTexture.dispose();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Loads combatant textures and creates their Scene2D {@link Image} actors.
     *
     * <p><b>Swap point:</b> replace the {@code Texture} paths or swap
     * {@code Image} for a custom animated actor here when final assets arrive.
     */
    private void loadCombatants() {
        playerTexture = new Texture(Gdx.files.internal(PLAYER_TEXTURE_PATH));
        bossTexture   = new Texture(Gdx.files.internal(BOSS_TEXTURE_PATH));

        // TextureRegionDrawable makes it trivial to hot-swap the frame later
        playerImage = new Image(new TextureRegionDrawable(new TextureRegion(playerTexture)));
        playerImage.setSize(PLAYER_WIDTH, PLAYER_HEIGHT);
        playerImage.setName("player");   // useful for stage.findActor("player")

        bossImage = new Image(new TextureRegionDrawable(new TextureRegion(bossTexture)));
        bossImage.setSize(BOSS_WIDTH, BOSS_HEIGHT);
        bossImage.setName("boss");
    }

    /**
     * Sets combatant positions relative to the current viewport dimensions.
     *
     * <p>Called from both {@link #show()} and {@link #resize(int, int)} so
     * positions stay correct on window resize.
     */
    private void positionCombatants() {
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();

        // Player — bottom-left quadrant, 10 % margin from edges
        playerImage.setPosition(
                w * 0.10f,
                h * 0.10f
        );

        // Boss — upper-right quadrant, 10 % margin from edges
        bossImage.setPosition(
                w * 0.90f - BOSS_WIDTH,
                h * 0.90f - BOSS_HEIGHT
        );
    }
}
