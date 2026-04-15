package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Disposable;

/**
 * Renders a two-phase fade transition before the battle HUD appears.
 *
 * <h3>Phase timeline</h3>
 * <pre>
 *   FADE_IN   0.0s ──────────► {@value #FADE_DURATION}s   alpha 0 → 1  (screen goes black)
 *   HOLD      {@value #FADE_DURATION}s ──► {@value #FADE_DURATION}+{@value #HOLD_DURATION}s    alpha 1       (black screen held)
 *   FADE_OUT  after hold ────────────────► end            alpha 1 → 0  (battle HUD revealed)
 * </pre>
 *
 * Call {@link #start()} when a collision is detected.
 * Poll {@link #isHudReadyToShow()} — it returns {@code true} exactly once at
 * the FADE_IN/HOLD boundary, which is when the caller should initialise the
 * battle HUD behind the black overlay.
 * Poll {@link #isFinished()} to know when to stop calling {@link #update} and
 * {@link #render}.
 *
 * <p>This class owns the 1×1 black {@link Texture} it uses and disposes it in
 * {@link #dispose()}.
 */
public class BattleTransition implements Disposable {

    // -----------------------------------------------------------------------
    // Timing constants  (seconds)
    // -----------------------------------------------------------------------

    /** Seconds the black overlay takes to fade in (0 → 1 alpha). */
    public static final float FADE_DURATION = 0.45f;

    /** Seconds the screen is held fully black before fading out. */
    public static final float HOLD_DURATION = 0.25f;

    /** Seconds the black overlay takes to fade out (1 → 0 alpha). */
    public static final float FADE_OUT_DURATION = 0.55f;

    /** Total transition duration in seconds. */
    public static final float TOTAL_DURATION =
            FADE_DURATION + HOLD_DURATION + FADE_OUT_DURATION;

    // -----------------------------------------------------------------------
    // Internal state machine
    // -----------------------------------------------------------------------

    private enum Phase { IDLE, FADE_IN, HOLD, FADE_OUT, DONE }

    private Phase phase = Phase.IDLE;

    /** Elapsed seconds within the current phase. */
    private float phaseTime = 0f;

    /** Current alpha of the black overlay (0 = transparent, 1 = opaque). */
    private float alpha = 0f;

    /**
     * Flipped to {@code true} exactly once — at the moment FADE_IN ends.
     * Consumed by a single {@link #isHudReadyToShow()} call.
     */
    private boolean hudReadySignal = false;

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /** 1×1 black texture stretched to fill the screen. */
    private final Texture overlay;

    /** Reused batch colour to avoid allocations. */
    private final Color batchColor = new Color();

    // -----------------------------------------------------------------------
    // Construction / disposal
    // -----------------------------------------------------------------------

    public BattleTransition() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        this.overlay = new Texture(pm);
        pm.dispose();
    }

    @Override
    public void dispose() {
        overlay.dispose();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Begins the transition. Safe to call even if one is already running —
     * it will restart from the beginning.
     */
    public void start() {
        phase        = Phase.FADE_IN;
        phaseTime    = 0f;
        alpha        = 0f;
        hudReadySignal = false;
    }

    /** Returns {@code true} while the transition is playing. */
    public boolean isTransitioning() {
        return phase != Phase.IDLE && phase != Phase.DONE;
    }

    /** Returns {@code true} once the transition has fully completed. */
    public boolean isFinished() {
        return phase == Phase.DONE;
    }

    /**
     * Returns {@code true} exactly once — at the frame the screen becomes
     * fully black (end of FADE_IN / start of HOLD).
     * The caller should call {@link BattleOverlay#startBattle} at this point,
     * so the battle assets load behind the opaque overlay.
     */
    public boolean isHudReadyToShow() {
        if (hudReadySignal) {
            hudReadySignal = false;
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Per-frame methods
    // -----------------------------------------------------------------------

    /**
     * Advances the transition state machine.
     * Call every frame while {@link #isTransitioning()} is {@code true}.
     *
     * @param delta seconds since last frame
     */
    public void update(float delta) {
        if (phase == Phase.IDLE || phase == Phase.DONE) return;

        phaseTime += delta;

        switch (phase) {
            case FADE_IN -> {
                float t = Math.min(phaseTime / FADE_DURATION, 1f);
                alpha = Interpolation.fade.apply(t);
                if (phaseTime >= FADE_DURATION) {
                    alpha          = 1f;
                    phase          = Phase.HOLD;
                    phaseTime      = 0f;
                    hudReadySignal = true; // signal: HUD can now be shown behind overlay
                }
            }
            case HOLD -> {
                alpha = 1f;
                if (phaseTime >= HOLD_DURATION) {
                    phase     = Phase.FADE_OUT;
                    phaseTime = 0f;
                }
            }
            case FADE_OUT -> {
                float t = Math.min(phaseTime / FADE_OUT_DURATION, 1f);
                alpha = Interpolation.fade.apply(1f - t);
                if (phaseTime >= FADE_OUT_DURATION) {
                    alpha = 0f;
                    phase = Phase.DONE;
                }
            }
            default -> {}
        }
    }

    /**
     * Draws the black overlay at the current alpha.
     * Call every frame while {@link #isTransitioning()} is {@code true},
     * AFTER the battle background has been rendered so the overlay sits on top.
     *
     * @param batch an already-configured SpriteBatch (NOT between begin/end)
     */
    public void render(Batch batch) {
        if (phase == Phase.IDLE || phase == Phase.DONE || alpha <= 0f) return;

        batchColor.set(batch.getColor());          // save caller's colour
        batch.setColor(0f, 0f, 0f, alpha);
        batch.begin();
        batch.draw(overlay,
                0f, 0f,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());
        batch.end();
        batch.setColor(batchColor);                // restore
    }
}
