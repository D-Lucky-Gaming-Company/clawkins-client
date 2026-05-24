package github.dluckycompany.clawkins.ui;

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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.MusicTrack;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.input.InputConventions;
import github.dluckycompany.clawkins.leaderboard.LeaderboardEntry;
import github.dluckycompany.clawkins.leaderboard.LeaderboardHud;
import github.dluckycompany.clawkins.leaderboard.LeaderboardManager;
import github.dluckycompany.clawkins.progress.GameMetaState;

import java.util.List;

/**
 * Full-screen leaderboard shown after the ending credits "THE END" card.
 */
public class LeaderboardScreen extends ScreenAdapter {

    private static final float VIRTUAL_W = 800f;
    private static final float VIRTUAL_H = 600f;
    private static final float FADE_IN_DURATION = 1.2f;
    private static final String FONT_PATH = "font/earthbound-dialogue-gold.otf";

    private final Main game;
    private final Batch batch;
    private final AudioService audioService;
    private final Stage stage;

    private LeaderboardManager leaderboardManager;
    private BitmapFont titleFont;
    private BitmapFont entryFont;
    private BitmapFont hintFont;
    private Texture blackPixel;
    private Texture bgTexture;
    private float overlayAlpha = 1f;
    private float fadeTimer = 0f;
    private boolean continuing = false;

    public LeaderboardScreen(Main game) {
        this.game = game;
        this.batch = game.getBatch();
        this.audioService = game.getAudioService();
        this.stage = new Stage(new FitViewport(VIRTUAL_W, VIRTUAL_H));
    }

    @Override
    public void show() {
        continuing = false;
        overlayAlpha = 1f;
        fadeTimer = 0f;

        if (leaderboardManager == null) {
            leaderboardManager = new LeaderboardManager();
        } else {
            leaderboardManager.load();
        }

        GameMetaState metaState = new GameMetaState();
        metaState.load();

        loadFonts();
        buildBlackPixel();
        buildBackground();
        buildLayout(metaState);

        audioService.playMusic(MusicTrack.POST_CREDITS, false);
        if (metaState.isLastRunPersonalBest()) {
            audioService.playSound(SoundEffect.MILESTONE);
        }
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 20f);

        if (fadeTimer < FADE_IN_DURATION) {
            fadeTimer += delta;
            float t = Math.min(fadeTimer / FADE_IN_DURATION, 1f);
            overlayAlpha = Interpolation.fade.apply(1f - t);
        } else {
            overlayAlpha = 0f;
        }

        if (!continuing && (InputConventions.isInteractJustPressed()
                || InputConventions.isCancelJustPressed()
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {
            continueToMainMenu();
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();

        if (bgTexture != null) {
            batch.setColor(1f, 1f, 1f, 0.22f);
            batch.draw(bgTexture, 0, 0, VIRTUAL_W, VIRTUAL_H);
            batch.setColor(Color.WHITE);
        }

        if (overlayAlpha > 0f) {
            batch.setColor(0f, 0f, 0f, overlayAlpha);
            batch.draw(blackPixel, 0, 0, VIRTUAL_W, VIRTUAL_H);
            batch.setColor(Color.WHITE);
        }

        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        disposeFont(titleFont);
        disposeFont(entryFont);
        disposeFont(hintFont);
        if (blackPixel != null) {
            blackPixel.dispose();
            blackPixel = null;
        }
        if (bgTexture != null) {
            bgTexture.dispose();
            bgTexture = null;
        }
    }

    private void buildLayout(GameMetaState metaState) {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Label title = new Label("Leaderboard", new Label.LabelStyle(titleFont, Color.valueOf("#F4D03F")));
        title.setAlignment(Align.center);
        root.add(title).padBottom(8f).row();

        Label subtitle = new Label("Top Completion Times", new Label.LabelStyle(entryFont, Color.valueOf("#D6CBB8")));
        subtitle.setAlignment(Align.center);
        root.add(subtitle).padBottom(24f).row();

        if (metaState != null && metaState.isLastRunPersonalBest()) {
            Label newRecord = new Label("NEW RECORD!", new Label.LabelStyle(titleFont, Color.valueOf("#9FE870")));
            newRecord.setAlignment(Align.center);
            root.add(newRecord).padBottom(20f).row();
        }

        Table entriesTable = new Table();
        entriesTable.defaults().padBottom(10f).left();

        List<LeaderboardEntry> entries = leaderboardManager.getEntries();
        if (entries.isEmpty()) {
            Label empty = new Label("No records yet", new Label.LabelStyle(entryFont, Color.WHITE));
            entriesTable.add(empty).row();
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                String rank = String.format("%d.", i + 1);
                Label rankLabel = new Label(rank, new Label.LabelStyle(entryFont, Color.GOLD));
                boolean isPlayer = LeaderboardHud.isHighlightedPlayer(
                        entry.getName(),
                        metaState != null ? metaState.getLastCompletionName() : null);
                Color nameColor = isPlayer ? LeaderboardHud.PLAYER_NAME_COLOR : Color.WHITE;
                Label nameLabel = new Label(entry.getName(), new Label.LabelStyle(entryFont, nameColor));
                Label timeLabel = new Label(entry.getFormattedTime(), new Label.LabelStyle(entryFont, Color.valueOf("#E8C97A")));

                Table row = new Table();
                row.add(rankLabel).width(36f).padRight(12f);
                row.add(nameLabel).width(220f).padRight(16f);
                row.add(timeLabel).width(100f);
                entriesTable.add(row).row();
            }
        }

        root.add(entriesTable).padBottom(16f).row();

        if (metaState != null && metaState.getLastCompletionMillis() >= 0L) {
            String yourTimeText = "Your Time: "
                    + LeaderboardManager.formatMillis(metaState.getLastCompletionMillis());
            Label yourTime = new Label(yourTimeText, new Label.LabelStyle(entryFont, Color.valueOf("#9FE870")));
            yourTime.setAlignment(Align.center);
            root.add(yourTime).padBottom(16f).row();
        }

        root.padBottom(16f);

        TextButton continueButton = new TextButton("Continue", createButtonStyle());
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                audioService.playSound(SoundEffect.UI_SELECT);
                continueToMainMenu();
            }
        });
        root.add(continueButton).width(220f).height(52f).padBottom(12f).row();

        Label hint = new Label("Press Z / Enter to continue", new Label.LabelStyle(hintFont, Color.valueOf("#B8AF9A")));
        hint.setAlignment(Align.center);
        root.add(hint).row();

        stage.addActor(root);
    }

    private TextButton.TextButtonStyle createButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = entryFont;
        style.fontColor = Color.valueOf("#F4D03F");
        style.overFontColor = Color.WHITE;
        style.downFontColor = Color.valueOf("#C8A030");
        return style;
    }

    private void continueToMainMenu() {
        if (continuing) {
            return;
        }
        continuing = true;
        Gdx.input.setInputProcessor(null);
        game.setScreen(MainMenuScreen.class);
    }

    private void loadFonts() {
        disposeFont(titleFont);
        disposeFont(entryFont);
        disposeFont(hintFont);

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 48;
        titleParam.borderWidth = 2f;
        titleParam.borderColor = Color.BLACK;
        titleFont = gen.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter entryParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        entryParam.size = 24;
        entryParam.borderWidth = 1f;
        entryParam.borderColor = Color.BLACK;
        entryFont = gen.generateFont(entryParam);

        FreeTypeFontGenerator.FreeTypeFontParameter hintParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        hintParam.size = 18;
        hintParam.borderWidth = 1f;
        hintParam.borderColor = Color.BLACK;
        hintFont = gen.generateFont(hintParam);

        gen.dispose();
    }

    private void buildBlackPixel() {
        if (blackPixel != null) {
            return;
        }
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        blackPixel = new Texture(pm);
        pm.dispose();
    }

    private void buildBackground() {
        if (bgTexture != null) {
            return;
        }
        try {
            bgTexture = new Texture(Gdx.files.internal("ui/menu_ui/MenuUI_Background.png"));
        } catch (Exception ignored) {
            bgTexture = null;
        }
    }

    private static void disposeFont(BitmapFont font) {
        if (font != null) {
            font.dispose();
        }
    }
}
