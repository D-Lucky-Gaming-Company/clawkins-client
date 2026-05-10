package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

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
    private static final int TITLE_FONT_SIZE = 29;
    private static final float MARGIN = 24f;
    private static final float MIN_BOX_HEIGHT = 140f;
    private static final float BOX_HEIGHT_FRACTION = 0.2f;
    private static final float TEXT_PAD_X = 16f;
    private static final float TITLE_BODY_GAP = 26f;
    private static final float MIN_TEXT_SCALE = 0.6f;
    private static final float TEXT_SCALE_STEP = 0.05f;

    private final BitmapFont bodyFont;
    private final BitmapFont titleFont;
    private final ShapeRenderer shapeRenderer;
    private final Matrix4 uiProjection = new Matrix4();
    private final GlyphLayout battleGlyphLayout = new GlyphLayout();
    private final GlyphLayout measureBodyLayout = new GlyphLayout();
    private final GlyphLayout measureTitleLayout = new GlyphLayout();

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
        render(batch, null, speakerName, bodyText, bodyText, position);
    }

    public void render(
            Batch batch,
            Viewport viewport,
            String speakerName,
            String bodyText,
            Interactible.DialoguePosition position) {
        render(batch, viewport, speakerName, bodyText, bodyText, position);
    }

    public void render(
            Batch batch,
            String speakerName,
            String visibleBodyText,
            String fullBodyText,
            Interactible.DialoguePosition position) {
        render(batch, null, speakerName, visibleBodyText, fullBodyText, position);
    }

    public void render(
            Batch batch,
            Viewport viewport,
            String speakerName,
            String visibleBodyText,
            String fullBodyText,
            Interactible.DialoguePosition position) {
        if (viewport != null) {
            viewport.apply();
            uiProjection.set(viewport.getCamera().combined);
        } else {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        }

        float w = viewport != null ? viewport.getWorldWidth() : Gdx.graphics.getWidth();
        float h = viewport != null ? viewport.getWorldHeight() : Gdx.graphics.getHeight();
        float boxH = Math.max(MIN_BOX_HEIGHT, h * BOX_HEIGHT_FRACTION);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.72f));
        shapeRenderer.rect(MARGIN, boxY, boxW, boxH);
        shapeRenderer.end();

        String name = speakerName == null ? "" : speakerName.trim();
        boolean hasName = !name.isEmpty();
        String visibleBody = visibleBodyText == null ? "" : visibleBodyText;
        String fullBody = fullBodyText == null ? visibleBody : fullBodyText;

        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        titleFont.setColor(Color.GOLD);
        bodyFont.setColor(Color.WHITE);

        float textX = MARGIN + TEXT_PAD_X;
        float innerWidth = boxW - TEXT_PAD_X * 2f;
        float topLineY = boxY + boxH - TEXT_PAD_X;
        float textScale = computeBestTextScale(name, fullBody, hasName, innerWidth, boxH);
        bodyFont.getData().setScale(textScale);

        try {
            if (hasName) {
                titleFont.draw(batch, name, textX, topLineY);
                float bodyY = topLineY - TITLE_BODY_GAP;
                bodyFont.draw(batch, visibleBody, textX, bodyY, innerWidth, Align.left, true);
            } else {
                bodyFont.draw(batch, visibleBody, textX, topLineY, innerWidth, Align.left, true);
            }
            batch.end();
        } finally {
            bodyFont.getData().setScale(1f);
        }
    }

    public void renderPromptMarkup(
            Batch batch,
            Viewport viewport,
            String markupText,
            Interactible.DialoguePosition position) {
        if (viewport != null) {
            viewport.apply();
            uiProjection.set(viewport.getCamera().combined);
        } else {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        }

        float w = viewport != null ? viewport.getWorldWidth() : Gdx.graphics.getWidth();
        float h = viewport != null ? viewport.getWorldHeight() : Gdx.graphics.getHeight();
        float boxH = Math.max(MIN_BOX_HEIGHT, h * BOX_HEIGHT_FRACTION);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.72f));
        shapeRenderer.rect(MARGIN, boxY, boxW, boxH);
        shapeRenderer.end();

        String safeMarkup = markupText == null ? "" : markupText;
        String plainText = safeMarkup.replaceAll("\\[[^\\]]*\\]", "");

        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        bodyFont.setColor(Color.WHITE);

        float textX = MARGIN + TEXT_PAD_X;
        float innerWidth = boxW - TEXT_PAD_X * 2f;
        float topLineY = boxY + boxH - TEXT_PAD_X;
        float textScale = computeBestTextScale("", plainText, false, innerWidth, boxH);
        bodyFont.getData().setScale(textScale);

        boolean wasMarkup = bodyFont.getData().markupEnabled;
        bodyFont.getData().markupEnabled = true;
        try {
            bodyFont.draw(batch, safeMarkup, textX, topLineY, innerWidth, Align.left, true);
            batch.end();
        } finally {
            bodyFont.getData().markupEnabled = wasMarkup;
            bodyFont.getData().setScale(1f);
        }
    }

    public void renderPromptMarkupLarge(
            Batch batch,
            Viewport viewport,
            String markupText,
            Interactible.DialoguePosition position) {
        if (viewport != null) {
            viewport.apply();
            uiProjection.set(viewport.getCamera().combined);
        } else {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        }

        float w = viewport != null ? viewport.getWorldWidth() : Gdx.graphics.getWidth();
        float h = viewport != null ? viewport.getWorldHeight() : Gdx.graphics.getHeight();
        float boxH = Math.max(250f, h * 0.42f);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

        shapeRenderer.setProjectionMatrix(uiProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.76f));
        shapeRenderer.rect(MARGIN, boxY, boxW, boxH);
        shapeRenderer.end();

        String safeMarkup = markupText == null ? "" : markupText;
        String plainText = safeMarkup.replaceAll("\\[[^\\]]*\\]", "");

        batch.setProjectionMatrix(uiProjection);
        batch.begin();
        bodyFont.setColor(Color.WHITE);

        float textX = MARGIN + TEXT_PAD_X;
        float innerWidth = boxW - TEXT_PAD_X * 2f;
        float topLineY = boxY + boxH - TEXT_PAD_X;
        float fittedScale = computeBestTextScale("", plainText, false, innerWidth, boxH);
        float textScale = Math.min(1.22f, fittedScale + 0.18f);
        bodyFont.getData().setScale(textScale);

        boolean wasMarkup = bodyFont.getData().markupEnabled;
        bodyFont.getData().markupEnabled = true;
        try {
            bodyFont.draw(batch, safeMarkup, textX, topLineY, innerWidth, Align.left, true);
            batch.end();
        } finally {
            bodyFont.getData().markupEnabled = wasMarkup;
            bodyFont.getData().setScale(1f);
        }
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
        renderBattleLog(batch, null, speakerName, plainFull, spans, visiblePlainChars, position);
    }

    public void renderBattleLog(
            Batch batch,
            Viewport viewport,
            String speakerName,
            String plainFull,
            List<BattleTextSpan> spans,
            int visiblePlainChars,
            Interactible.DialoguePosition position) {
        if (viewport != null) {
            viewport.apply();
            uiProjection.set(viewport.getCamera().combined);
        } else {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        }

        float w = viewport != null ? viewport.getWorldWidth() : Gdx.graphics.getWidth();
        float h = viewport != null ? viewport.getWorldHeight() : Gdx.graphics.getHeight();
        float boxH = Math.max(MIN_BOX_HEIGHT, h * BOX_HEIGHT_FRACTION);
        float boxW = w - MARGIN * 2f;
        float boxY = position == Interactible.DialoguePosition.TOP
                ? h - MARGIN - boxH
                : MARGIN;

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
        float textScale = computeBestTextScale(name, plain, hasName, innerWidth, boxH);
        bodyFont.getData().setScale(textScale);

        try {
            if (hasName) {
                titleFont.draw(batch, name, textX, topLineY);
                float bodyY = topLineY - TITLE_BODY_GAP;
                drawBattleBodyMarkup(batch, markupVisible, textX, bodyY, innerWidth);
            } else {
                drawBattleBodyMarkup(batch, markupVisible, textX, topLineY, innerWidth);
            }
            batch.end();
        } finally {
            bodyFont.getData().setScale(1f);
        }
    }

    private float computeBestTextScale(String speakerName, String bodyText, boolean hasName, float innerWidth, float boxHeight) {
        float availableTextHeight = boxHeight - (TEXT_PAD_X * 2f);
        String measuredBody = (bodyText == null || bodyText.isEmpty()) ? " " : bodyText;
        String measuredTitle = (speakerName == null || speakerName.isEmpty()) ? " " : speakerName;

        for (float scale = 1f; scale >= MIN_TEXT_SCALE; scale -= TEXT_SCALE_STEP) {
            bodyFont.getData().setScale(scale);

            measureBodyLayout.setText(bodyFont, measuredBody, Color.WHITE, innerWidth, Align.left, true);
            float requiredHeight = measureBodyLayout.height;

            if (hasName) {
                measureTitleLayout.setText(titleFont, measuredTitle);
                requiredHeight += measureTitleLayout.height + TITLE_BODY_GAP;
            }

            if (requiredHeight <= availableTextHeight) {
                return scale;
            }
        }

        return MIN_TEXT_SCALE;
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
        parameter.minFilter = TextureFilter.Nearest;
        parameter.magFilter = TextureFilter.Nearest;
        parameter.borderWidth = 3.1f;
        parameter.borderColor = Color.BLACK;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = -2;
        parameter.shadowColor = new Color(0f, 0f, 0f, 0.88f);
    }
}
