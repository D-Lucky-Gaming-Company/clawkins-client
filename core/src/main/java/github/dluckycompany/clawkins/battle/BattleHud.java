package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.asset.TextureAsset;
import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Scene2D {@link Stage}-based battle HUD.
 *
 * <h3>Layout</h3>
 * <pre>
 *   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
 *   â”‚  [Battle background]               â”‚  â† full-screen backdrop
 *   â”‚  [PLAYER]         [BOSS]           â”‚  â† placeholder combatants
 *   â”‚                                    â”‚
 *   â”‚  [ATTACK]  [DEFEND]  [ITEM]        â”‚  â† ImageButtons from BattleUI_sheet.png
 *   â”‚  [1]       [2]       [3]           â”‚  â† keyboard hint labels
 *   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
 * </pre>
 *
 * <h3>Icon sheet assumptions</h3>
 * {@code BattleUI_sheet.png} is expected to be a single row of 4 icons,
 * each {@value #ICON_SIZE}Ã—{@value #ICON_SIZE} px:
 * <pre>
 *   Col 0 â€“ Attack
 *   Col 1 â€“ Defend
 *   Col 2 â€“ Item
 *   Col 3 â€“ Special
 * </pre>
 *
 * <p>Wired into {@link BattleOverlay}; call {@link #show()} when a battle
 * starts, {@link #hide()} when it ends, and {@link #render()} every frame
 * while the HUD is visible.
 */
public class BattleHud implements Disposable {

    // -----------------------------------------------------------------------
    // Icon sheet constants
    // -----------------------------------------------------------------------

    /** Pixel size of each icon cell in BattleUI_sheet.png. */
    public static final int ICON_SIZE = 64;

    /** Pixel width of the skill icon. */
    public static final int SKILL_ICON_WIDTH = 228;

    /** Pixel height of the skill icon. */
    public static final int SKILL_ICON_HEIGHT = 100;

    /** Column index of the Attack icon. */
    public static final int COL_ATTACK = 0;
    /** Column index of the Defend icon. */
    public static final int COL_DEFEND = 1;
    /** Column index of the Item icon. */
    public static final int COL_ITEM   = 2;
    /** Column index of the Special Skill icon. */
    public static final int COL_SPECIAL = 3;

    // -----------------------------------------------------------------------
    // Placeholder combatant sprites (replace with real ones later)
    // -----------------------------------------------------------------------

    private static final String PLAYER_PLACEHOLDER_PATH = "maps/characters/Clawkin_01.png";
    private static final String BOSS_PLACEHOLDER_PATH   = "maps/characters/Clawkin_02.png";

    private static final float PLAYER_PLACEHOLDER_W = 96f;
    private static final float PLAYER_PLACEHOLDER_H = 96f;
    private static final float BOSS_PLACEHOLDER_W   = 128f;
    private static final float BOSS_PLACEHOLDER_H   = 128f;

    // -----------------------------------------------------------------------
    // Button asset paths (individual PNG files)
    // -----------------------------------------------------------------------

    private static final String BUTTON1_PATH = "ui/button1.png";
    private static final String BUTTON2_PATH = "ui/button2.png";
    private static final String BUTTON3_PATH = "ui/button3.png";
    private static final String BUTTON4_PATH = "ui/button4.png";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Stage stage;
    private final Texture battleBg;
    private final BitmapFont font;

    // Owned placeholder textures and actors (dispose in dispose())
    private Texture playerPlaceholderTex;
    private Texture bossPlaceholderTex;
    private Image playerImage;
    private Image bossImage;

    // Owned button textures (dispose in dispose())
    private Texture button1Tex;
    private Texture button2Tex;
    private Texture button3Tex;
    private Texture button4Tex;

    // â”€â”€â”€ HP Bar UI Elements â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private Label playerNameLabel;
    private ProgressBar playerHpBar;
    private Label playerHpLabel;

    private Label bossNameLabel;
    private ProgressBar bossHpBar;
    private Label bossHpLabel;

    // Reused UI actors/tables (for responsive relayout)
    private Image bg;
    private ImageButton attackBtn;
    private ImageButton defendBtn;
    private ImageButton specialBtn;
    private ImageButton itemBtn;
    private Label attackLbl;
    private Label defendLbl;
    private Label specialLbl;
    private Label itemLbl;
    private Table playerHpTable;
    private Table bossHpTable;
    private Table playerHpCorner;
    private Table bossHpCorner;
    private Table root;

    // HP tracking (updated externally via setters)
    private float playerCurrentHp = 100f;
    private float playerMaxHp = 100f;
    private float bossCurrentHp = 100f;
    private float bossMaxHp = 100f;

    // Active clawkin portrait — loaded dynamically; disposed when clawkin changes
    private Texture activeClawkinTex;
    private String lastDisplayedClawkinKey = null;

    /** Enemy portrait loaded for this encounter (separate from static boss placeholder). */
    private Texture activeEnemyTex;
    private String lastEnemyPortraitKey = null;

    /** True while the HUD should be drawn and receive input. */
    private boolean visible = false;

    /** Callback slots â€” set by GameScreen / BattleOverlay. */
    private Runnable onAttack = () -> {};
    private Runnable onDefend = () -> {};
    private Runnable onSpecial = () -> {};
    private Runnable onItem   = () -> {};

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Builds the Stage and all widgets.
     *
     * @param assetService used to load {@link TextureAsset#BATTLE_UI}
     */
    public BattleHud(AssetService assetService) {
        this.stage    = new Stage(new ScreenViewport());
        this.battleBg = assetService.load(TextureAsset.BATTLE_BACKGROUND);
        this.font     = new BitmapFont();
        this.font.getData().setScale(1.1f);

        buildWidgets();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Makes the HUD visible and routes input through the Stage. */
    public void show() {
        visible = true;
        Gdx.input.setInputProcessor(stage);
    }

    /** Hides the HUD and removes the Stage as input processor. */
    public void hide() {
        visible = false;
        lastDisplayedClawkinKey = null;
        lastEnemyPortraitKey = null;
        Gdx.input.setInputProcessor(null);
    }

    /** Returns true when the HUD is currently active. */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Updates and draws the HUD.  Call once per frame from
     * {@link BattleOverlay#render} when a battle session is active.
     */
    public void render() {
        if (!visible) return;
        stage.getViewport().apply();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    /** Resize the Stage viewport â€” call from {@code Screen.resize()}. */
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        applyResponsiveLayout();
        positionPlaceholders();
    }

    // -----------------------------------------------------------------------
    // Callback setters
    // -----------------------------------------------------------------------

    public void setOnAttack(Runnable onAttack) { this.onAttack = onAttack != null ? onAttack : () -> {}; }
    public void setOnDefend(Runnable onDefend) { this.onDefend = onDefend != null ? onDefend : () -> {}; }
    public void setOnSpecial(Runnable onSpecial) { this.onSpecial = onSpecial != null ? onSpecial : () -> {}; }
    public void setOnItem(Runnable onItem) { this.onItem = onItem != null ? onItem : () -> {}; }

    /** Invoke current Attack action callback. */
    public void triggerAttack() { onAttack.run(); }

    /** Invoke current Defend action callback. */
    public void triggerDefend() { onDefend.run(); }

    /** Invoke current Special action callback. */
    public void triggerSpecial() { onSpecial.run(); }

    /** Invoke current Item action callback. */
    public void triggerItem() { onItem.run(); }

    // -----------------------------------------------------------------------
    // Active Clawkin
    // -----------------------------------------------------------------------

    /**
     * Updates the player-side display to reflect the currently active clawkin.
     * Uses {@code image_clawkin} when set, otherwise the same fallback paths as {@link github.dluckycompany.clawkins.ui.ClawkinCard}.
     * Reloads the portrait only when the active clawkin identity changes.
     */
    public void updateActiveClawkin(Clawkin clawkin) {
        if (playerNameLabel != null) {
            if (clawkin == null) {
                playerNameLabel.setText("—");
            } else {
                playerNameLabel.setText(clawkin.getName() != null ? clawkin.getName() : clawkin.getId());
            }
        }

        if (clawkin == null) {
            lastDisplayedClawkinKey = null;
            restorePlayerPlaceholderPortrait();
            return;
        }

        String key = clawkinVisualKey(clawkin);
        boolean clawkinChanged = !key.equals(lastDisplayedClawkinKey);
        if (!clawkinChanged) {
            return;
        }
        lastDisplayedClawkinKey = key;

        for (String path : resolvePlayerPortraitCandidates(clawkin)) {
            if (path == null || path.isBlank()) {
                continue;
            }
            if (!Gdx.files.internal(path).exists()) {
                continue;
            }
            if (activeClawkinTex != null) {
                activeClawkinTex.dispose();
                activeClawkinTex = null;
            }
            activeClawkinTex = new Texture(Gdx.files.internal(path));
            if (playerImage != null) {
                playerImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeClawkinTex)));
            }
            return;
        }
        restorePlayerPlaceholderPortrait();
    }

    /**
     * Updates the enemy-side portrait and name from encounter data.
     */
    public void updateEnemyCombatant(String encounterId, String displayName, String portraitPath) {
        String label = displayName != null && !displayName.isBlank()
                ? displayName
                : (encounterId != null && !encounterId.isBlank() ? encounterId : "Enemy");
        if (bossNameLabel != null) {
            bossNameLabel.setText(label);
        }

        String pathKey = (encounterId == null ? "" : encounterId) + "\0" + (portraitPath == null ? "" : portraitPath);
        if (pathKey.equals(lastEnemyPortraitKey)) {
            return;
        }
        lastEnemyPortraitKey = pathKey;

        for (String path : resolveEnemyPortraitCandidates(portraitPath, label, encounterId)) {
            if (path == null || path.isBlank()) {
                continue;
            }
            if (!Gdx.files.internal(path).exists()) {
                continue;
            }
            if (activeEnemyTex != null) {
                activeEnemyTex.dispose();
                activeEnemyTex = null;
            }
            activeEnemyTex = new Texture(Gdx.files.internal(path));
            applyLeftFlippedTextureToBossImage(activeEnemyTex);
            return;
        }
        restoreBossPlaceholderPortrait();
    }

    private static String clawkinVisualKey(Clawkin c) {
        String id = c.getId() == null ? "" : c.getId().trim();
        if (!id.isEmpty()) {
            return id;
        }
        return c.getName() == null ? "" : c.getName().trim();
    }

    private void restorePlayerPlaceholderPortrait() {
        if (activeClawkinTex != null) {
            activeClawkinTex.dispose();
            activeClawkinTex = null;
        }
        if (playerImage != null && playerPlaceholderTex != null) {
            playerImage.setDrawable(new TextureRegionDrawable(new TextureRegion(playerPlaceholderTex)));
        }
    }

    private void restoreBossPlaceholderPortrait() {
        if (activeEnemyTex != null) {
            activeEnemyTex.dispose();
            activeEnemyTex = null;
        }
        if (bossImage != null && bossPlaceholderTex != null) {
            TextureRegion region = new TextureRegion(bossPlaceholderTex);
            region.flip(true, false);
            bossImage.setDrawable(new TextureRegionDrawable(region));
        }
    }

    private void applyLeftFlippedTextureToBossImage(Texture tex) {
        if (bossImage == null || tex == null) {
            return;
        }
        TextureRegion region = new TextureRegion(tex);
        region.flip(true, false);
        bossImage.setDrawable(new TextureRegionDrawable(region));
    }

    /** Same resolution order as ClawkinCard portrait loading. */
    private static String[] resolvePlayerPortraitCandidates(Clawkin clawkin) {
        String configured = clawkin.getImagePath();
        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.trim();
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            return new String[] { trimmed };
        }

        String name = clawkin.getName() == null ? "" : clawkin.getName().toLowerCase();
        String id = clawkin.getId() == null ? "" : clawkin.getId().toLowerCase();

        if (name.contains("ginger") || id.contains("ginger")) {
            return new String[] {
                    "ui/Clawkin_01.png",
                    "maps/entities/clawkins/Clawkin_01.png",
                    "maps/characters/Clawkin_01.png"
            };
        }
        if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return new String[] {
                    "ui/Clawkin_02.png",
                    "maps/entities/clawkins/Clawkin_02.png",
                    "maps/characters/Clawkin_02.png"
            };
        }
        if (name.contains("dart") || id.contains("dart")) {
            return new String[] {
                    "ui/Clawkin_03.png",
                    "maps/entities/clawkins/Clawkin_03.png",
                    "maps/characters/Clawkin_03.png"
            };
        }
        return new String[] {
                "ui/Clawkin_01.png",
                "maps/entities/clawkins/Clawkin_01.png",
                "maps/characters/Clawkin_01.png"
        };
    }

    private static String[] resolveEnemyPortraitCandidates(String configuredPath, String enemyName, String encounterId) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            String trimmed = configuredPath.trim();
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            return new String[] { trimmed };
        }
        String name = enemyName == null ? "" : enemyName.toLowerCase();
        String id = encounterId == null ? "" : encounterId.toLowerCase();
        if (name.contains("ginger") || id.contains("ginger")) {
            return new String[] {
                    "ui/Clawkin_01.png",
                    "maps/entities/clawkins/Clawkin_01.png",
                    "maps/characters/Clawkin_01.png"
            };
        }
        if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return new String[] {
                    "ui/Clawkin_02.png",
                    "maps/entities/clawkins/Clawkin_02.png",
                    "maps/characters/Clawkin_02.png"
            };
        }
        if (name.contains("dart") || id.contains("dart")) {
            return new String[] {
                    "ui/Clawkin_03.png",
                    "maps/entities/clawkins/Clawkin_03.png",
                    "maps/characters/Clawkin_03.png"
            };
        }
        return new String[] {
                BOSS_PLACEHOLDER_PATH,
                "ui/Clawkin_02.png",
                "maps/entities/clawkins/Clawkin_02.png",
                "maps/characters/Clawkin_02.png"
        };
    }

    // -----------------------------------------------------------------------
    // HP Management
    // -----------------------------------------------------------------------

    /**
     * Updates player HP and refreshes the HP bar display.
     */
    public void setPlayerHp(float current, float max) {
        float safeMax = Math.max(1f, max);
        this.playerCurrentHp = Math.max(0f, Math.min(current, safeMax));
        this.playerMaxHp = safeMax;
        updateHpBars();
    }

    /**
     * Updates boss HP and refreshes the HP bar display.
     */
    public void setBossHp(float current, float max) {
        float safeMax = Math.max(1f, max);
        this.bossCurrentHp = Math.max(0f, Math.min(current, safeMax));
        this.bossMaxHp = safeMax;
        updateHpBars();
    }

    /** Applies damage to boss HP and refreshes the existing bar/label. */
    public void damageBoss(float damageAmount) {
        if (damageAmount <= 0f) return;
        setBossHp(bossCurrentHp - damageAmount, bossMaxHp);
    }

    /** Applies healing/utility effect to boss HP and refreshes existing bar/label. */
    public void healBoss(float healAmount) {
        if (healAmount <= 0f) return;
        setBossHp(bossCurrentHp + healAmount, bossMaxHp);
    }

    /** Applies healing effect to player HP and refreshes existing bar/label. */
    public void healPlayer(float healAmount) {
        if (healAmount <= 0f) return;
        setPlayerHp(playerCurrentHp + healAmount, playerMaxHp);
    }

    public float getBossCurrentHp() {
        return bossCurrentHp;
    }

    public float getBossMaxHp() {
        return bossMaxHp;
    }

    /**
     * Refreshes HP bar visuals based on current HP values.
     */
    private void updateHpBars() {
        if (playerHpBar != null) {
            playerHpBar.setValue(playerCurrentHp / playerMaxHp);
            if (playerHpLabel != null) {
                playerHpLabel.setText(String.format("%.0f / %.0f", playerCurrentHp, playerMaxHp));
            }
        }
        if (bossHpBar != null) {
            bossHpBar.setValue(bossCurrentHp / bossMaxHp);
            if (bossHpLabel != null) {
                bossHpLabel.setText(String.format("%.0f / %.0f", bossCurrentHp, bossMaxHp));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Disposable
    // -----------------------------------------------------------------------

    @Override
    public void dispose() {
        stage.dispose();
        font.dispose();
        if (playerPlaceholderTex != null) playerPlaceholderTex.dispose();
        if (bossPlaceholderTex != null) bossPlaceholderTex.dispose();
        if (button1Tex != null) button1Tex.dispose();
        if (button2Tex != null) button2Tex.dispose();
        if (button3Tex != null) button3Tex.dispose();
        if (button4Tex != null) button4Tex.dispose();
        if (activeClawkinTex != null) activeClawkinTex.dispose();
        if (activeEnemyTex != null) activeEnemyTex.dispose();
        // battleBg is owned by AssetService â€” do NOT dispose it here
    }

    // -----------------------------------------------------------------------
    // Widget construction
    // -----------------------------------------------------------------------

    /**
     * Loads button textures and assembles the root {@link Table} onto the stage.
     */
    private void buildWidgets() {
        if (button1Tex == null) button1Tex = new Texture(Gdx.files.internal(BUTTON1_PATH));
        if (button2Tex == null) button2Tex = new Texture(Gdx.files.internal(BUTTON2_PATH));
        if (button3Tex == null) button3Tex = new Texture(Gdx.files.internal(BUTTON3_PATH));
        if (button4Tex == null) button4Tex = new Texture(Gdx.files.internal(BUTTON4_PATH));

        bg = new Image(new TextureRegionDrawable(new TextureRegion(battleBg)));
        bg.setFillParent(true);
        bg.setScaling(Scaling.stretch);

        ProgressBar.ProgressBarStyle playerHpStyle = createHpBarStyle(Color.GREEN, Color.DARK_GRAY);
        ProgressBar.ProgressBarStyle bossHpStyle = createHpBarStyle(Color.RED, Color.DARK_GRAY);

        LabelStyle labelStyle = new LabelStyle(font, Color.WHITE);
        playerNameLabel = new Label("Player", labelStyle);
        playerHpBar = new ProgressBar(0f, 1f, 0.01f, false, playerHpStyle);
        playerHpBar.setValue(playerCurrentHp / playerMaxHp);
        playerHpLabel = new Label(String.format("%.0f / %.0f", playerCurrentHp, playerMaxHp), labelStyle);

        bossNameLabel = new Label("Boss", labelStyle);
        bossHpBar = new ProgressBar(0f, 1f, 0.01f, false, bossHpStyle);
        bossHpBar.setValue(bossCurrentHp / bossMaxHp);
        bossHpLabel = new Label(String.format("%.0f / %.0f", bossCurrentHp, bossMaxHp), labelStyle);

        attackBtn = loadButton(button1Tex, () -> this.onAttack.run());
        defendBtn = loadButton(button2Tex, () -> this.onDefend.run());
        specialBtn = loadButton(button3Tex, () -> this.onSpecial.run());
        itemBtn = loadButton(button4Tex, () -> this.onItem.run());
        attackBtn.getImage().setScaling(Scaling.fit);
        defendBtn.getImage().setScaling(Scaling.fit);
        specialBtn.getImage().setScaling(Scaling.fit);
        itemBtn.getImage().setScaling(Scaling.fit);

        attackLbl = new Label("[1] Attack", labelStyle);
        defendLbl = new Label("[2] Defend", labelStyle);
        specialLbl = new Label("[3] Special", labelStyle);
        itemLbl = new Label("[4] Item", labelStyle);

        playerHpTable = new Table();
        bossHpTable = new Table();

        playerHpCorner = new Table();
        playerHpCorner.setFillParent(true);
        playerHpCorner.top().left();

        bossHpCorner = new Table();
        bossHpCorner.setFillParent(true);
        bossHpCorner.top().right();

        root = new Table();
        root.setFillParent(true);
        root.bottom();

        loadPlaceholders();
        applyResponsiveLayout();
        positionPlaceholders();

        stage.addActor(bg);
        stage.addActor(playerHpCorner);
        stage.addActor(bossHpCorner);
        stage.addActor(playerImage);
        stage.addActor(bossImage);
        stage.addActor(root);
    }

    /** Rebuilds cell sizing/padding using viewport-relative values. */
    private void applyResponsiveLayout() {
        if (root == null || playerHpTable == null || bossHpTable == null) return;

        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();

        float buttonSize = MathUtils.clamp(Math.min(worldW * 0.09f, worldH * 0.16f), 48f, 96f);
        float buttonGap = MathUtils.clamp(buttonSize * 0.35f, 10f, 36f);
        float bottomPad = MathUtils.clamp(worldH * 0.04f, 16f, 48f);
        float labelPadTop = MathUtils.clamp(worldH * 0.008f, 4f, 10f);

        float hpBarWidth = MathUtils.clamp(worldW * 0.22f, 140f, 340f);
        float hpBarHeight = MathUtils.clamp(worldH * 0.03f, 14f, 28f);
        float hpTopPad = MathUtils.clamp(worldH * 0.015f, 8f, 20f);
        float hpSidePad = MathUtils.clamp(worldW * 0.012f, 10f, 24f);

        playerHpTable.clearChildren();
        playerHpTable.add(playerNameLabel).left().padBottom(2f);
        playerHpTable.row();
        playerHpTable.add(playerHpBar).width(hpBarWidth).height(hpBarHeight).left();
        playerHpTable.row();
        playerHpTable.add(playerHpLabel).left().padTop(2f);

        bossHpTable.clearChildren();
        bossHpTable.add(bossNameLabel).right().padBottom(2f);
        bossHpTable.row();
        bossHpTable.add(bossHpBar).width(hpBarWidth).height(hpBarHeight).right();
        bossHpTable.row();
        bossHpTable.add(bossHpLabel).right().padTop(2f);

        playerHpCorner.clearChildren();
        playerHpCorner.top().left().pad(hpTopPad, hpSidePad, 0f, 0f);
        playerHpCorner.add(playerHpTable).left();

        bossHpCorner.clearChildren();
        bossHpCorner.top().right().pad(hpTopPad, 0f, 0f, hpSidePad);
        bossHpCorner.add(bossHpTable).right();

        root.clearChildren();
        root.bottom().padBottom(bottomPad);
        root.add(attackBtn).size(buttonSize, buttonSize).padRight(buttonGap);
        root.add(defendBtn).size(buttonSize, buttonSize).padRight(buttonGap);
        root.add(specialBtn).size(buttonSize, buttonSize).padRight(buttonGap);
        root.add(itemBtn).size(buttonSize, buttonSize);
        root.row().padTop(labelPadTop);
        root.add(attackLbl).center().padRight(buttonGap);
        root.add(defendLbl).center().padRight(buttonGap);
        root.add(specialLbl).center().padRight(buttonGap);
        root.add(itemLbl).center();

        root.invalidateHierarchy();
        playerHpCorner.invalidateHierarchy();
        bossHpCorner.invalidateHierarchy();
    }

    /**
     * Creates a ProgressBar style with specified fill and background colors.
     */
    private ProgressBar.ProgressBarStyle createHpBarStyle(Color fillColor, Color backgroundColor) {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();

        // Create colored pixmaps for fill and background
        Pixmap fillPixmap = new Pixmap(4, 20, Pixmap.Format.RGBA8888);
        fillPixmap.setColor(fillColor);
        fillPixmap.fill();
        Texture fillTexture = new Texture(fillPixmap);
        fillPixmap.dispose();

        Pixmap bgPixmap = new Pixmap(4, 20, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(backgroundColor);
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        // Set the drawable styles
        style.knobBefore = new TextureRegionDrawable(new TextureRegion(fillTexture));
        style.background = new TextureRegionDrawable(new TextureRegion(bgTexture));

        return style;
    }

    /**
     * Loads placeholder textures and builds their Image actors (idempotent).
     */
    private void loadPlaceholders() {
        if (playerImage == null) {
            playerPlaceholderTex = new Texture(Gdx.files.internal(PLAYER_PLACEHOLDER_PATH));
            playerImage = new Image(new TextureRegionDrawable(new TextureRegion(playerPlaceholderTex)));
            playerImage.setSize(PLAYER_PLACEHOLDER_W, PLAYER_PLACEHOLDER_H);
            playerImage.setName("player_placeholder");
        }
        if (bossImage == null) {
            bossPlaceholderTex = new Texture(Gdx.files.internal(BOSS_PLACEHOLDER_PATH));

            // Create region and flip it
            TextureRegion bossRegion = new TextureRegion(bossPlaceholderTex);
            
            // flip(flipX, flipY). 
            // Use (true, false) to mirror left/right. 
            // Use (true, true) for a full 180-degree upside-down rotation.
            bossRegion.flip(true, false); 

            bossImage = new Image(new TextureRegionDrawable(bossRegion));
            bossImage.setSize(BOSS_PLACEHOLDER_W, BOSS_PLACEHOLDER_H);
            bossImage.setName("boss_placeholder");
        }
    }

    /**
     * Positions placeholders in the center of the screen (center-left for player,
     * center-right for boss).
     */
    private void positionPlaceholders() {
        if (playerImage == null || bossImage == null) return;

        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();

    float playerW = MathUtils.clamp(Math.min(w, h) * 0.14f, 72f, 140f);
    float playerH = playerW;
    float bossW = MathUtils.clamp(Math.min(w, h) * 0.19f, 96f, 196f);
    float bossH = bossW;

    playerImage.setSize(playerW, playerH);
    bossImage.setSize(bossW, bossH);

        // Player â€” center-left (horizontally at 25%, vertically centered)
        playerImage.setPosition(
        (w * 0.25f) - (playerW / 2f),
        (h / 2f) - (playerH / 2f)
        );

        // Boss â€” center-right (horizontally at 75%, vertically centered)
        bossImage.setPosition(
        (w * 0.75f) - (bossW / 2f),
        (h / 2f) - (bossH / 2f)
        );
    }

    /**
     * Creates an {@link ImageButton} from a PNG texture with visual feedback.
     * Uses the same texture for up/over/down states; tints on press for visual feedback.
     */
    private static ImageButton loadButton(Texture buttonTex, Runnable onClick) {
        TextureRegion region = new TextureRegion(buttonTex);
        TextureRegionDrawable upDrawable = new TextureRegionDrawable(region);

        // Optional: Create a tinted version for the down/pressed state
        TextureRegionDrawable downDrawable = new TextureRegionDrawable(region);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp   = upDrawable;     // Normal state
        style.imageOver = upDrawable;     // Hover state (same as up)
        style.imageDown = downDrawable;   // Pressed state (tinted slightly)

        ImageButton btn = new ImageButton(style);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { onClick.run(); }
        });
        return btn;
    }
}
