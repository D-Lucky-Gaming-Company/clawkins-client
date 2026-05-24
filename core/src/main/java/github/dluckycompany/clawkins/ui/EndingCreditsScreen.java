package github.dluckycompany.clawkins.ui;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.MusicTrack;
import github.dluckycompany.clawkins.ui.CreditsParser.CreditsLine;
import github.dluckycompany.clawkins.ui.CreditsParser.LineType;

/**
 * Ending credits screen with top-down scrolling animation.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Fade in from black</li>
 *   <li>Credits scroll upward from below the screen</li>
 *   <li>"THE END" card fades in after all credits have passed</li>
 *   <li>Leaderboard screen, then Main Menu</li>
 * </ol>
 *
 * <h3>Controls</h3>
 * <ul>
 *   <li>ESC / ENTER / SPACE → skip to end sequence</li>
 * </ul>
 */
public class EndingCreditsScreen extends ScreenAdapter {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final float VIRTUAL_W = 800f;
    private static final float VIRTUAL_H = 600f;

    /** Pixels per second the credits scroll upward (virtual units). */
    private static final float SCROLL_SPEED_NORMAL = 55f;
    /** Faster scroll speed when player holds skip. */
    @SuppressWarnings("unused")
    private static final float SCROLL_SPEED_FAST   = 200f;

    private static final float FADE_IN_DURATION    = 1.2f;
    private static final float FADE_OUT_DURATION   = 1.5f;
    private static final float THE_END_HOLD        = 3.0f;
    private static final float THE_END_FADE_IN     = 1.5f;

    /** Extra blank space added before and after the credits table. */
    private static final float CREDITS_PADDING_TOP    = 40f;
    private static final float CREDITS_PADDING_BOTTOM = VIRTUAL_H * 0.3f;

    private static final String CREDITS_FILE = "docs/CREDITS.md";
    private static final String FONT_PATH    = "font/earthbound-dialogue-gold.otf";

    // -----------------------------------------------------------------------
    // State machine
    // -----------------------------------------------------------------------

    private enum Phase {
        FADE_IN,
        SCROLLING,
        THE_END,
        FADE_OUT,
        DONE
    }

    private Phase phase = Phase.FADE_IN;
    private float phaseTimer = 0f;
    private float overlayAlpha = 1f;   // 1 = black, 0 = transparent
    private float theEndAlpha  = 0f;
    private boolean skipRequested = false;

    // -----------------------------------------------------------------------
    // Scene2D
    // -----------------------------------------------------------------------

    private final Main game;
    private final Batch batch;
    private final AudioService audioService;
    private final Stage stage;

    /** Current Y position of the credits table (starts below screen, moves up). */
    private float creditsY;
    /** Total height of the credits table (computed after building). */
    private float creditsTotalHeight;

    // -----------------------------------------------------------------------
    // Rendering resources
    // -----------------------------------------------------------------------

    private BitmapFont fontH1;
    private BitmapFont fontH2;
    private BitmapFont fontH3;
    private BitmapFont fontBody;
    private BitmapFont fontTheEnd;

    private Texture blackPixel;
    private Texture bgTexture;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public EndingCreditsScreen(Main game) {
        this.game       = game;
        this.batch      = game.getBatch();
        this.audioService = game.getAudioService();
        this.stage      = new Stage(new FitViewport(VIRTUAL_W, VIRTUAL_H));
    }

    // -----------------------------------------------------------------------
    // Screen lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void show() {
        loadFonts();
        buildBlackPixel();
        buildBackground();
        buildCreditsTable();

        // Start fully black; fade in
        overlayAlpha = 1f;
        phase        = Phase.FADE_IN;
        phaseTimer   = 0f;
        skipRequested = false;

        // Play credits music if available
        audioService.playMusic(MusicTrack.CREDITS, false);

        Gdx.input.setInputProcessor(null); // no Scene2D input needed
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 20f); // cap to avoid spiral-of-death

        handleInput();
        update(delta);
        draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        // nothing — resources kept until dispose()
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (fontH1   != null) fontH1.dispose();
        if (fontH2   != null) fontH2.dispose();
        if (fontH3   != null) fontH3.dispose();
        if (fontBody != null) fontBody.dispose();
        if (fontTheEnd != null) fontTheEnd.dispose();
        if (blackPixel != null) blackPixel.dispose();
        if (bgTexture  != null) bgTexture.dispose();
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    private void handleInput() {
        if (phase == Phase.DONE) return;

        boolean skipKey = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                       || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                       || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        if (skipKey && !skipRequested) {
            skipRequested = true;
            jumpToTheEnd();
        }
    }

    // -----------------------------------------------------------------------
    // Update
    // -----------------------------------------------------------------------

    private void update(float delta) {
        switch (phase) {
            case FADE_IN -> {
                phaseTimer += delta;
                float t = Math.min(phaseTimer / FADE_IN_DURATION, 1f);
                overlayAlpha = Interpolation.fade.apply(1f - t);
                if (phaseTimer >= FADE_IN_DURATION) {
                    overlayAlpha = 0f;
                    phase = Phase.SCROLLING;
                    phaseTimer = 0f;
                }
            }

            case SCROLLING -> {
                float speed = SCROLL_SPEED_NORMAL;
                creditsY += speed * delta;

                // Credits are done when the bottom of the block has scrolled past the top of the screen
                if (creditsY > VIRTUAL_H) {
                    phase = Phase.THE_END;
                    phaseTimer = 0f;
                    theEndAlpha = 0f;
                }
            }

            case THE_END -> {
                phaseTimer += delta;
                if (phaseTimer <= THE_END_FADE_IN) {
                    theEndAlpha = Interpolation.fade.apply(phaseTimer / THE_END_FADE_IN);
                } else {
                    theEndAlpha = 1f;
                }
                if (phaseTimer >= THE_END_FADE_IN + THE_END_HOLD) {
                    phase = Phase.FADE_OUT;
                    phaseTimer = 0f;
                }
            }

            case FADE_OUT -> {
                phaseTimer += delta;
                float t = Math.min(phaseTimer / FADE_OUT_DURATION, 1f);
                overlayAlpha = Interpolation.fade.apply(t);
                if (phaseTimer >= FADE_OUT_DURATION) {
                    overlayAlpha = 1f;
                    phase = Phase.DONE;
                    showLeaderboard();
                }
            }

            case DONE -> { /* waiting for returnToMainMenu to fire */ }
        }
    }

    // -----------------------------------------------------------------------
    // Draw
    // -----------------------------------------------------------------------

    private void draw() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);

        batch.begin();

        // Background
        if (bgTexture != null) {
            batch.setColor(1f, 1f, 1f, 0.18f);
            batch.draw(bgTexture, 0, 0, VIRTUAL_W, VIRTUAL_H);
            batch.setColor(Color.WHITE);
        }

        // Draw scrolling credits
        if (phase == Phase.SCROLLING || phase == Phase.FADE_IN) {
            drawCredits();
        }

        // "THE END" overlay
        if (phase == Phase.THE_END || phase == Phase.FADE_OUT) {
            drawTheEnd();
        }

        // Black fade overlay (top layer)
        if (overlayAlpha > 0f) {
            batch.setColor(0f, 0f, 0f, overlayAlpha);
            batch.draw(blackPixel, 0, 0, VIRTUAL_W, VIRTUAL_H);
            batch.setColor(Color.WHITE);
        }

        batch.end();
    }

    /**
     * Draw all credit lines at their current scrolled position.
     * creditsY is the Y of the bottom of the credits table in virtual coords.
     */
    private void drawCredits() {
        // We iterate the table's children (Labels) and draw them manually
        // because we're animating the Y position outside Scene2D.
        // The table was built with absolute positions baked in; we offset by creditsY.
        stage.act(0f);

        // Apply scissor to clip credits to the screen area
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Draw each line
        for (CreditsLineEntry entry : lineEntries) {
            float drawY = creditsY + entry.y;
            // Skip lines that are off-screen
            if (drawY + entry.height < 0 || drawY > VIRTUAL_H) continue;

            switch (entry.type) {
                case HEADING_1 -> {
                    fontH1.setColor(Color.valueOf("#F4D03F"));
                    fontH1.draw(batch, sanitizeForFont(entry.text), entry.x, drawY + entry.height);
                }
                case HEADING_2 -> {
                    fontH2.setColor(Color.valueOf("#E8C97A"));
                    fontH2.draw(batch, sanitizeForFont(entry.text), entry.x, drawY + entry.height);
                }
                case HEADING_3 -> {
                    fontH3.setColor(Color.valueOf("#D6CBB8"));
                    fontH3.draw(batch, sanitizeForFont(entry.text), entry.x, drawY + entry.height);
                }
                case BULLET -> {
                    fontBody.setColor(Color.valueOf("#C8BFA8"));
                    fontBody.draw(batch, "* " + sanitizeForFont(entry.text), entry.x, drawY + entry.height);
                }
                case BODY -> {
                    fontBody.setColor(Color.valueOf("#B8AF9A"));
                    fontBody.draw(batch, sanitizeForFont(entry.text), entry.x, drawY + entry.height);
                }
                case SPACER -> { /* nothing */ }
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void drawTheEnd() {
        if (theEndAlpha <= 0f) return;

        // Semi-transparent dark background
        batch.setColor(0f, 0f, 0f, theEndAlpha * 0.85f);
        batch.draw(blackPixel, 0, 0, VIRTUAL_W, VIRTUAL_H);
        batch.setColor(Color.WHITE);

        // "THE END"
        fontTheEnd.setColor(1f, 1f, 1f, theEndAlpha);
        String theEnd = "THE END";
        com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontTheEnd, theEnd);
        float x = (VIRTUAL_W - layout.width) / 2f;
        float y = VIRTUAL_H / 2f + layout.height / 2f + 30f;
        fontTheEnd.draw(batch, theEnd, x, y);

        // Subtitle
        fontH3.setColor(0.85f, 0.78f, 0.65f, theEndAlpha);
        String sub = "Thank you for playing Clawkins - Dawn of the Primal";
        com.badlogic.gdx.graphics.g2d.GlyphLayout subLayout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(fontH3, sub);
        float subX = (VIRTUAL_W - subLayout.width) / 2f;
        float subY = VIRTUAL_H / 2f - 20f;
        fontH3.draw(batch, sub, subX, subY);
    }

    // -----------------------------------------------------------------------
    // Credits table building
    // -----------------------------------------------------------------------

    /** Lightweight record for a single rendered line. */
    private static final class CreditsLineEntry {
        final LineType type;
        final String   text;
        final float    x;
        final float    y;      // bottom-left Y within the credits block (0 = bottom of block)
        final float    height; // line height

        CreditsLineEntry(LineType type, String text, float x, float y, float height) {
            this.type   = type;
            this.text   = text;
            this.x      = x;
            this.y      = y;
            this.height = height;
        }
    }

    private List<CreditsLineEntry> lineEntries;

    private void buildCreditsTable() {
        List<CreditsLine> parsed = CreditsParser.parse(CREDITS_FILE);
        lineEntries = new java.util.ArrayList<>();

        // Layout constants
        final float marginX   = 80f;

        // Spacing per line type (pixels below the line) — generous for readability
        final float spacingH1     = 22f;
        final float spacingH2     = 16f;
        final float spacingH3     = 12f;
        final float spacingBody   = 8f;
        final float spacingBullet = 8f;
        final float spacerH       = 30f;  // section gap between major blocks

        // Build from bottom up: accumulate Y as we go
        float cursorY = CREDITS_PADDING_BOTTOM;

        // We build the list in reverse so we can lay out bottom-to-top,
        // then reverse at the end so index 0 is the topmost line.
        java.util.List<CreditsLineEntry> reversed = new java.util.ArrayList<>();

        for (int i = parsed.size() - 1; i >= 0; i--) {
            CreditsLine line = parsed.get(i);

            switch (line.type) {
                case HEADING_1 -> {
                    float h = fontH1.getLineHeight();
                    reversed.add(new CreditsLineEntry(LineType.HEADING_1, line.text,
                            (VIRTUAL_W - measureWidth(fontH1, line.text)) / 2f,
                            cursorY, h));
                    cursorY += h + spacingH1;
                }
                case HEADING_2 -> {
                    float h = fontH2.getLineHeight();
                    reversed.add(new CreditsLineEntry(LineType.HEADING_2, line.text,
                            (VIRTUAL_W - measureWidth(fontH2, line.text)) / 2f,
                            cursorY, h));
                    cursorY += h + spacingH2;
                }
                case HEADING_3 -> {
                    float h = fontH3.getLineHeight();
                    reversed.add(new CreditsLineEntry(LineType.HEADING_3, line.text,
                            (VIRTUAL_W - measureWidth(fontH3, line.text)) / 2f,
                            cursorY, h));
                    cursorY += h + spacingH3;
                }
                case BULLET -> {
                    float h = fontBody.getLineHeight();
                    reversed.add(new CreditsLineEntry(LineType.BULLET, line.text,
                            marginX + 10f, cursorY, h));
                    cursorY += h + spacingBullet;
                }
                case BODY -> {
                    float h = fontBody.getLineHeight();
                    float textW = measureWidth(fontBody, line.text);
                    // Center short lines; left-align long ones to prevent overflow
                    float x = textW < (VIRTUAL_W - marginX * 2)
                            ? (VIRTUAL_W - textW) / 2f
                            : marginX;
                    reversed.add(new CreditsLineEntry(LineType.BODY, line.text, x, cursorY, h));
                    cursorY += h + spacingBody;
                }
                case SPACER -> {
                    reversed.add(new CreditsLineEntry(LineType.SPACER, "", 0, cursorY, spacerH));
                    cursorY += spacerH;
                }
            }
        }

        cursorY += CREDITS_PADDING_TOP;
        creditsTotalHeight = cursorY;

        // Reverse so index 0 is the topmost entry
        for (int i = reversed.size() - 1; i >= 0; i--) {
            lineEntries.add(reversed.get(i));
        }

        // Start position: top of credits block just below the screen bottom
        creditsY = -creditsTotalHeight;
    }

    private float measureWidth(BitmapFont font, String text) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout gl = new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, sanitizeForFont(text));
        return gl.width;
    }

    // -----------------------------------------------------------------------
    // Skip / end
    // -----------------------------------------------------------------------

    private void jumpToTheEnd() {
        if (phase == Phase.SCROLLING || phase == Phase.FADE_IN) {
            phase = Phase.THE_END;
            phaseTimer = 0f;
            theEndAlpha = 0f;
            overlayAlpha = 0f;
        }
    }

    private void showLeaderboard() {
        audioService.stopAll();
        Gdx.input.setInputProcessor(null);
        game.setScreen(LeaderboardScreen.class);
    }

    private void returnToMainMenu() {
        audioService.stopAll();
        Gdx.input.setInputProcessor(null);
        game.setScreen(MainMenuScreen.class);
    }

    // -----------------------------------------------------------------------
    // Resource loading
    // -----------------------------------------------------------------------

    private void loadFonts() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal(FONT_PATH));

        // Explicit character set — only include what the font actually supports.
        // This prevents .notdef boxes for missing glyphs.
        String chars = FreeTypeFontGenerator.DEFAULT_CHARS
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                + "0123456789 .,!?-_:;'\"()[]{}@#$%^&*+=/<>\\|~`"
                + "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz";

        FreeTypeFontGenerator.FreeTypeFontParameter p;

        // H1 — large title
        p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 52;
        p.borderWidth = 2f;
        p.borderColor = Color.BLACK;
        p.characters = chars;
        fontH1 = gen.generateFont(p);

        // H2 — section heading
        p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 36;
        p.borderWidth = 1.5f;
        p.borderColor = Color.BLACK;
        p.characters = chars;
        fontH2 = gen.generateFont(p);

        // H3 — sub-heading
        p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 26;
        p.borderWidth = 1f;
        p.borderColor = Color.BLACK;
        p.characters = chars;
        fontH3 = gen.generateFont(p);

        // Body
        p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 20;
        p.borderWidth = 1f;
        p.borderColor = Color.BLACK;
        p.characters = chars;
        fontBody = gen.generateFont(p);

        // "THE END" — very large
        p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 80;
        p.borderWidth = 3f;
        p.borderColor = Color.BLACK;
        p.characters = chars;
        fontTheEnd = gen.generateFont(p);

        gen.dispose();
    }

    /**
     * Replace characters that the pixel font doesn't support with safe ASCII equivalents.
     * Prevents the "box with X" .notdef glyph from appearing.
     */
    private static String sanitizeForFont(String text) {
        if (text == null) return "";
        return text
            .replace('\u2014', '-')   // em-dash → hyphen
            .replace('\u2013', '-')   // en-dash → hyphen
            .replace('\u2022', '*')   // bullet • → *
            .replace('\u2018', '\'')  // left single quote → '
            .replace('\u2019', '\'')  // right single quote → '
            .replace('\u201C', '"')   // left double quote → "
            .replace('\u201D', '"')   // right double quote → "
            .replace('\u2026', '.')   // ellipsis → .
            .replace('\u00A0', ' ')   // non-breaking space → space
            .replace('\u00E9', 'e')   // é → e
            .replace('\u00E8', 'e')   // è → e
            .replace('\u00EA', 'e')   // ê → e
            .replace('\u00EB', 'e')   // ë → e
            .replace('\u00E0', 'a')   // à → a
            .replace('\u00E1', 'a')   // á → a
            .replace('\u00E2', 'a')   // â → a
            .replace('\u00F3', 'o')   // ó → o
            .replace('\u00F4', 'o')   // ô → o
            .replace('\u00FA', 'u')   // ú → u
            .replace('\u00FC', 'u')   // ü → u
            .replace('\u00F1', 'n')   // ñ → n
            .replaceAll("[^\\x20-\\x7E]", ""); // strip anything else outside printable ASCII
    }

    private void buildBlackPixel() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        blackPixel = new Texture(pm);
        pm.dispose();
    }

    private void buildBackground() {
        // Try to reuse the menu background for a warm, consistent feel
        try {
            bgTexture = new Texture(Gdx.files.internal("ui/menu_ui/MenuUI_Background.png"));
        } catch (Exception e) {
            bgTexture = null; // graceful fallback — plain black background
        }
    }
}
