package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

import github.dluckycompany.clawkins.battle.BattleLogMarkup;
import github.dluckycompany.clawkins.battle.BattleTextSpan;
import github.dluckycompany.clawkins.component.Interactible;

/**
 * Shared dialogue panel drawing: {@code assets/font/earthbound-dialogue-gold.otf}, semi-transparent
 * box, optional speaker line (gold), wrapped body (white). Use everywhere in-game dialogue should
 * look the same (world interactions, battle log, etc.).
 */
public class DialogueBoxRenderer implements Disposable {

    /** Path under internal assets (typically {@code assets/font/...}). */
    public static final String DIALOGUE_FONT_PATH = "font/earthbound-dialogue-gold.otf";

    private static final int BODY_FONT_SIZE = 24;
    private static final int TITLE_FONT_SIZE = 26;
    private static final float MARGIN = 24f;
    private static final float MIN_BOX_HEIGHT = 140f;
    private static final float BOX_HEIGHT_FRACTION = 0.2f;
    private static final float TEXT_PAD_X = 18f;
    private static final float TITLE_BODY_GAP = 26f;

    private final BitmapFont bodyFont;
    private final BitmapFont titleFont;
    private final ShapeRenderer shapeRenderer;
    private final Matrix4 uiProjection = new Matrix4();
    private final GlyphLayout battleGlyphLayout = new GlyphLayout();

    public DialogueBoxRenderer() {
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal(DIALOGUE_FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter bodyParameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        applyEarthboundStyle(bodyParameter, BODY_FONT_SIZE);
        this.bodyFont = generator.generateFont(bodyParameter);
        FreeTypeFontGenerator.FreeTypeFontParameter titleParameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        applyEarthboundStyle(titleParameter, TITLE_FONT_SIZE);
        this.titleFont = generator.generateFont(titleParameter);
        generator.dispose();
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * @param speakerName optional; when null or blank after trim, the title line is omitted and body
     *                    uses the full vertical padding.
     * @param bodyText    visible text (may be empty while typewriter is at the start).
     */
    public void render(
            Batch batch,
            String speakerName,
            String bodyText,
            Interactible.DialoguePosition position) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float boxH = Math.max(MIN_BOX_HEIGHT, h * BOX_HEIGHT_FRACTION);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

        uiProjection.setToOrtho2D(0f, 0f, w, h);

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.72f));
        shapeRenderer.rect(MARGIN, boxY, boxW, boxH);
        shapeRenderer.end();

        String name = speakerName == null ? "" : speakerName.trim();
        boolean hasName = !name.isEmpty();
        String body = bodyText == null ? "" : bodyText;

        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        titleFont.setColor(Color.GOLD);
        bodyFont.setColor(Color.WHITE);

        float textX = MARGIN + TEXT_PAD_X;
        float innerWidth = boxW - TEXT_PAD_X * 2f;
        float topLineY = boxY + boxH - TEXT_PAD_X;

        if (hasName) {
            titleFont.draw(batch, name, textX, topLineY);
            float bodyY = topLineY - TITLE_BODY_GAP;
            bodyFont.draw(batch, body, textX, bodyY, innerWidth, Align.left, true);
        } else {
            bodyFont.draw(batch, body, textX, topLineY, innerWidth, Align.left, true);
        }
        batch.end();
    }

    /**
     * Battle log: same panel as {@link #render}, but body uses LibGDX markup (colors) and {@code visiblePlainChars}
     * counts only real characters in {@code plainFull}, so typewriter speed matches the string from {@link
     * github.dluckycompany.clawkins.battle.BattleStateMachine#getLastLog()}.
     */
    public void renderBattleLog(
            Batch batch,
            String speakerName,
            String plainFull,
            List<BattleTextSpan> spans,
            int visiblePlainChars,
            Interactible.DialoguePosition position) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float boxH = Math.max(MIN_BOX_HEIGHT, h * BOX_HEIGHT_FRACTION);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

        uiProjection.setToOrtho2D(0f, 0f, w, h);

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.72f));
        shapeRenderer.rect(MARGIN, boxY, boxW, boxH);
        shapeRenderer.end();

        String name = speakerName == null ? "" : speakerName.trim();
        boolean hasName = !name.isEmpty();
        String plain = plainFull == null ? "" : plainFull;

        String markupFull = BattleLogMarkup.toMarkup(plain, spans == null ? List.of() : spans);
        String markupVisible = BattleLogMarkup.truncateMarkupToPlainLength(markupFull, visiblePlainChars);

        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        titleFont.setColor(Color.GOLD);
        bodyFont.setColor(Color.WHITE);

        float textX = MARGIN + TEXT_PAD_X;
        float innerWidth = boxW - TEXT_PAD_X * 2f;
        float topLineY = boxY + boxH - TEXT_PAD_X;

        if (hasName) {
            titleFont.draw(batch, name, textX, topLineY);
            float bodyY = topLineY - TITLE_BODY_GAP;
            drawBattleBodyMarkup(batch, markupVisible, textX, bodyY, innerWidth);
        } else {
            drawBattleBodyMarkup(batch, markupVisible, textX, topLineY, innerWidth);
        }
        batch.end();
    }

    private void drawBattleBodyMarkup(Batch batch, String markupVisible, float textX, float bodyBaselineY, float innerWidth) {
        if (markupVisible == null || markupVisible.isEmpty()) {
            return;
        }
        boolean wasMarkup = bodyFont.getData().markupEnabled;
        bodyFont.getData().markupEnabled = true;
        try {
            battleGlyphLayout.setText(bodyFont, markupVisible, Color.WHITE, innerWidth, Align.left, true);
            bodyFont.draw(batch, battleGlyphLayout, textX, bodyBaselineY);
        } finally {
            bodyFont.getData().markupEnabled = wasMarkup;
        }
    }

    @Override
    public void dispose() {
        bodyFont.dispose();
        titleFont.dispose();
        shapeRenderer.dispose();
    }

    public static void applyEarthboundStyle(FreeTypeFontGenerator.FreeTypeFontParameter parameter, int size) {
        parameter.size = size;
        parameter.kerning = true;
        parameter.spaceX = 1;
        parameter.borderWidth = 1.8f;
        parameter.borderColor = Color.BLACK;
        parameter.shadowOffsetX = 1;
        parameter.shadowOffsetY = -1;
        parameter.shadowColor = new Color(0f, 0f, 0f, 0.7f);
    }
}
