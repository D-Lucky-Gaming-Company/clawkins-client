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
 * Text appears with fade-in/fade-out transitions instead of typewriter effect.
 */
public class IntroExpositionOverlay implements Disposable {

    private static final int EXPOSITION_FONT_SIZE = 28;
    private static final float TEXT_PADDING_PERCENT = 0.1f; // 10% padding on each side
    private static final float TEXT_FADE_IN_DURATION = 0.8f;
    private static final float TEXT_HOLD_DURATION = 3.0f;
    private static final float TEXT_FADE_OUT_DURATION = 0.8f;
    private static final float FINAL_FADE_OUT_DURATION = 1.0f;
    private static final float FINAL_FADE_HOLD_DURATION = 0.3f;
    private static final float FINAL_FADE_IN_DURATION = 0.8f;

    private final BitmapFont expositionFont;
    private final ShapeRenderer shapeRenderer;
    private final Matrix4 screenProjection = new Matrix4();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private List<String> expositionLines = List.of();
    private int currentLineIndex = 0;
    private String currentText = "";
    private boolean active = false;
    
    // Text fade state
    private enum TextFadePhase { FADE_IN, HOLD, FADE_OUT }
    private TextFadePhase textFadePhase = TextFadePhase.FADE_IN;
    private float textFadeTimer = 0f;
    private float textAlpha = 0f;
    
    // Final transition state
    private boolean finalTransitionActive = false;
    private float finalTransitionTimer = 0f;
    private float backgroundAlpha = 1f;

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
        this.finalTransitionActive = false;
        this.backgroundAlpha = 1f;
        this.finalTransitionTimer = 0f;
        setActiveLine(0);
    }

    public void update(float delta) {
        if (!active) {
            return;
        }

        if (finalTransitionActive) {
            updateFinalTransition(delta);
            return;
        }

        updateTextFade(delta);

        // Check for input to skip current fade or advance
        if (InputConventions.isInteractJustPressed()) {
            handleInput();
        }
    }

    private void updateTextFade(float delta) {
        textFadeTimer += delta;

        switch (textFadePhase) {
            case FADE_IN:
                textAlpha = Math.min(1f, textFadeTimer / TEXT_FADE_IN_DURATION);
                if (textFadeTimer >= TEXT_FADE_IN_DURATION) {
                    textFadePhase = TextFadePhase.HOLD;
                    textFadeTimer = 0f;
                    textAlpha = 1f;
                }
                break;

            case HOLD:
                textAlpha = 1f;
                if (textFadeTimer >= TEXT_HOLD_DURATION) {
                    textFadePhase = TextFadePhase.FADE_OUT;
                    textFadeTimer = 0f;
                }
                break;

            case FADE_OUT:
                textAlpha = Math.max(0f, 1f - (textFadeTimer / TEXT_FADE_OUT_DURATION));
                if (textFadeTimer >= TEXT_FADE_OUT_DURATION) {
                    // Fade out complete, advance to next line
                    if (!advanceToNextLine()) {
                        // No more lines, start final transition
                        startFinalTransition();
                    }
                }
                break;
        }
    }

    private void handleInput() {
        switch (textFadePhase) {
            case FADE_IN:
                // Skip fade-in, go directly to hold
                textFadePhase = TextFadePhase.HOLD;
                textFadeTimer = 0f;
                textAlpha = 1f;
                Gdx.app.log("IntroExpositionOverlay", "Skipped fade-in");
                break;

            case HOLD:
                // Skip hold, go directly to fade-out
                textFadePhase = TextFadePhase.FADE_OUT;
                textFadeTimer = 0f;
                Gdx.app.log("IntroExpositionOverlay", "Skipped hold, starting fade-out");
                break;

            case FADE_OUT:
                // Skip fade-out, advance immediately
                if (!advanceToNextLine()) {
                    startFinalTransition();
                }
                Gdx.app.log("IntroExpositionOverlay", "Skipped fade-out, advancing");
                break;
        }
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
        shapeRenderer.setColor(new Color(0f, 0f, 0f, backgroundAlpha));
        shapeRenderer.rect(0f, 0f, screenWidth, screenHeight);
        shapeRenderer.end();

        // Draw centered text with fade
        batch.setProjectionMatrix(screenProjection);
        batch.begin();

        expositionFont.setColor(new Color(1f, 1f, 1f, textAlpha));

        // Calculate text area with padding
        float textPaddingX = screenWidth * TEXT_PADDING_PERCENT;
        float innerWidth = screenWidth - (textPaddingX * 2f);

        // Layout text
        glyphLayout.setText(expositionFont, currentText, Color.WHITE, innerWidth, 
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
        this.currentText = expositionLines.get(index);
        this.textFadePhase = TextFadePhase.FADE_IN;
        this.textFadeTimer = 0f;
        this.textAlpha = 0f;
    }

    private boolean advanceToNextLine() {
        if (currentLineIndex + 1 >= expositionLines.size()) {
            return false;
        }

        setActiveLine(currentLineIndex + 1);
        return true;
    }

    private void startFinalTransition() {
        finalTransitionActive = true;
        finalTransitionTimer = 0f;
        backgroundAlpha = 1f;
        textAlpha = 0f; // Hide text during final transition
        Gdx.app.log("IntroExpositionOverlay", "Starting final transition");
    }

    private void updateFinalTransition(float delta) {
        finalTransitionTimer += delta;

        if (finalTransitionTimer < FINAL_FADE_OUT_DURATION) {
            // Keep black screen
            backgroundAlpha = 1f;
        } else if (finalTransitionTimer < FINAL_FADE_OUT_DURATION + FINAL_FADE_HOLD_DURATION) {
            // Hold black
            backgroundAlpha = 1f;
        } else if (finalTransitionTimer < FINAL_FADE_OUT_DURATION + FINAL_FADE_HOLD_DURATION + FINAL_FADE_IN_DURATION) {
            // Fade back to gameplay
            float fadeInProgress = (finalTransitionTimer - FINAL_FADE_OUT_DURATION - FINAL_FADE_HOLD_DURATION) / FINAL_FADE_IN_DURATION;
            backgroundAlpha = 1f - fadeInProgress;
        } else {
            // Transition complete
            active = false;
            finalTransitionActive = false;
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
