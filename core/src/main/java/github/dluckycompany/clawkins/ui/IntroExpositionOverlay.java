package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import github.dluckycompany.clawkins.input.InputConventions;

import java.util.List;

/**
 * Fullscreen cinematic exposition overlay for the game intro.
 * Displays text centered on a black background, covering the ENTIRE physical screen.
 * This overlay renders to screen coordinates, not viewport coordinates, ensuring
 * it covers all visible areas including pillarboxing/letterboxing.
 */
public class IntroExpositionOverlay implements Disposable {

    private static final float TYPEWRITER_CHARS_PER_SECOND = 45f;
    private static final int EXPOSITION_FONT_SIZE = 28;
    private static final float TEXT_PADDING_PERCENT = 0.1f; // 10% padding on each side

    private final BitmapFont expositionFont;
    private final ShapeRenderer shapeRenderer;
    private final Matrix4 screenProjection = new Matrix4();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private List<String> expositionLines = List.of();
    private int currentLineIndex = 0;
    private String currentFullText = "";
    private String currentVisibleText = "";
    private int visibleChars = 0;
    private float typewriterCarry = 0f;
    private boolean active = false;
    private boolean fadeOutActive = false;
    private float fadeAlpha = 1f;
    private float fadeTimer = 0f;
    private static final float FADE_OUT_DURATION = 1.0f;
    private static final float FADE_HOLD_DURATION = 0.3f;
    private static final float FADE_IN_DURATION = 0.8f;

    private Runnable onComplete;

    public IntroExpositionOverlay() {
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal(DialogueBoxRenderer.DIALOGUE_FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        DialogueBoxRenderer.applyEarthboundStyle(parameter, EXPOSITION_FONT_SIZE);
        this.expositionFont = generator.generateFont(parameter);
        generator.dispose();
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * Start the exposition sequence with the given lines.
     *
     * @param lines      List of exposition text lines to display one at a time
     * @param onComplete Callback to run when the entire sequence finishes
     */
    public void start(List<String> lines, Runnable onComplete) {
        if (lines == null || lines.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        this.expositionLines = lines;
        this.currentLineIndex = 0;
        this.onComplete = onComplete;
        this.active = true;
        this.fadeOutActive = false;
        this.fadeAlpha = 1f;
        this.fadeTimer = 0f;
        setActiveLine(0);
    }

    public void update(float delta) {
        if (!active) {
            return;
        }

        if (fadeOutActive) {
            updateFadeTransition(delta);
            return;
        }

        tickTypewriter(delta);

        if (!InputConventions.isInteractJustPressed()) {
            return;
        }

        Gdx.app.log("IntroExpositionOverlay", "Interact pressed! Fully revealed: " + isLineFullyRevealed());

        if (!isLineFullyRevealed()) {
            revealLineImmediately();
            Gdx.app.log("IntroExpositionOverlay", "Revealed line immediately");
            return;
        }

        if (advanceToNextLine()) {
            Gdx.app.log("IntroExpositionOverlay", "Advanced to next line: " + currentLineIndex);
            return;
        }

        // All lines shown, start fade transition
        Gdx.app.log("IntroExpositionOverlay", "Starting fade transition");
        startFadeTransition();
    }

    /**
     * Render the exposition overlay to the ENTIRE physical screen.
     * This uses screen coordinates (pixels) rather than viewport coordinates
     * to ensure complete coverage including black bars.
     */
    public void render(Batch batch) {
        if (!active) {
            return;
        }

        // Get actual physical screen dimensions
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // Set up orthographic projection for full screen rendering
        screenProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);

        // Ensure batch is not drawing
        if (batch.isDrawing()) {
            batch.end();
        }

        // Enable blending for alpha
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Draw fullscreen black background covering ENTIRE screen
        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, fadeAlpha));
        shapeRenderer.rect(0f, 0f, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw centered text
        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        float textAlpha = fadeOutActive ? Math.max(0f, (fadeAlpha - 0.3f) / 0.7f) : 1f;
        expositionFont.setColor(new Color(1f, 1f, 1f, textAlpha));

        // Calculate text area with padding
        float textPaddingX = screenWidth * TEXT_PADDING_PERCENT;
        float innerWidth = screenWidth - (textPaddingX * 2f);

        // Layout text
        glyphLayout.setText(expositionFont, currentVisibleText, Color.WHITE, innerWidth, 
                com.badlogic.gdx.utils.Align.center, true);

        // Center text on screen
        float textX = textPaddingX;
        float textY = (screenHeight / 2f) + (glyphLayout.height / 2f);

        expositionFont.draw(batch, glyphLayout, textX, textY);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public boolean isActive() {
        return active;
    }

    private void setActiveLine(int index) {
        if (index < 0 || index >= expositionLines.size()) {
            return;
        }

        this.currentLineIndex = index;
        this.currentFullText = expositionLines.get(index);
        this.currentVisibleText = "";
        this.visibleChars = 0;
        this.typewriterCarry = 0f;
    }

    private void tickTypewriter(float delta) {
        if (isLineFullyRevealed()) {
            return;
        }

        float charProgress = TYPEWRITER_CHARS_PER_SECOND * Math.max(0f, delta) + typewriterCarry;
        int charsToReveal = (int) charProgress;
        typewriterCarry = charProgress - charsToReveal;

        if (charsToReveal <= 0) {
            return;
        }

        visibleChars = Math.min(currentFullText.length(), visibleChars + charsToReveal);
        currentVisibleText = currentFullText.substring(0, visibleChars);
    }

    private boolean isLineFullyRevealed() {
        return visibleChars >= currentFullText.length();
    }

    private void revealLineImmediately() {
        visibleChars = currentFullText.length();
        currentVisibleText = currentFullText;
    }

    private boolean advanceToNextLine() {
        if (currentLineIndex + 1 >= expositionLines.size()) {
            return false;
        }

        setActiveLine(currentLineIndex + 1);
        return true;
    }

    private void startFadeTransition() {
        fadeOutActive = true;
        fadeTimer = 0f;
        fadeAlpha = 1f;
    }

    private void updateFadeTransition(float delta) {
        fadeTimer += delta;

        if (fadeTimer < FADE_OUT_DURATION) {
            // Fade to black
            fadeAlpha = 1f;
        } else if (fadeTimer < FADE_OUT_DURATION + FADE_HOLD_DURATION) {
            // Hold black
            fadeAlpha = 1f;
        } else if (fadeTimer < FADE_OUT_DURATION + FADE_HOLD_DURATION + FADE_IN_DURATION) {
            // Fade back to gameplay
            float fadeInProgress = (fadeTimer - FADE_OUT_DURATION - FADE_HOLD_DURATION) / FADE_IN_DURATION;
            fadeAlpha = 1f - fadeInProgress;
        } else {
            // Transition complete
            active = false;
            fadeOutActive = false;
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    @Override
    public void dispose() {
        expositionFont.dispose();
        shapeRenderer.dispose();
    }
}
