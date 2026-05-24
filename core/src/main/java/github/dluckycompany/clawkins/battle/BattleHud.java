package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.ArrayList;
import java.util.List;

import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.asset.TextureAsset;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;

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
    // Viewport constants - Fixed 4:3 aspect ratio
    // -----------------------------------------------------------------------
    
    /** Fixed virtual width for 4:3 aspect ratio (retro RPG style) */
    private static final float VIRTUAL_WIDTH = 960f;
    
    /** Fixed virtual height for 4:3 aspect ratio (retro RPG style) */
    private static final float VIRTUAL_HEIGHT = 720f;

    /** Standout color for enemy level text beside the battle HUD name. */
    private static final Color ENEMY_LEVEL_LABEL_COLOR = Color.valueOf("#FFD54F");
    private static final float ENEMY_LEVEL_FONT_SCALE = 1.22f;

    // -----------------------------------------------------------------------
    // Fixed UI element sizes (in virtual pixels)
    // -----------------------------------------------------------------------
    
    /** Fixed size for action buttons (Attack, Defend, Special, Item) */
    private static final float BUTTON_SIZE = 80f;
    
    /** Fixed gap between action buttons */
    private static final float BUTTON_GAP = 24f;
    
    /** Fixed padding from bottom of screen */
    private static final float BOTTOM_PAD = 32f;
    
    /** Fixed padding for labels above buttons */
    private static final float LABEL_PAD_TOP = 6f;
    
    /** Fixed width for HP bars */
    private static final float HP_BAR_WIDTH = 240f;
    
    /** Fixed height for HP bars */
    private static final float HP_BAR_HEIGHT = 20f;
    
    /** Fixed padding from top of screen for HP bars */
    private static final float HP_TOP_PAD = 16f;
    
    /** Fixed padding from sides of screen for HP bars */
    private static final float HP_SIDE_PAD = 16f;
    
    /** Fixed size for corner buttons (Inventory, Flee) */
    private static final float CORNER_BUTTON_SIZE = 64f;
    
    /** Fixed padding for corner buttons */
    private static final float CORNER_PAD = 20f;
    
    /** Fixed width for Clawkin container */
    private static final float CONTAINER_WIDTH = 80f;
    
    /** Fixed height for Clawkin container */
    private static final float CONTAINER_HEIGHT = 240f;
    
    /** Fixed size for player combatant sprite */
    private static final float PLAYER_SIZE = 120f;
    
    /** Fixed size for boss combatant sprite */
    private static final float BOSS_SIZE = 160f;

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

    private static final String PLAYER_PLACEHOLDER_PATH = "entities/clawkins/Clawkin_01_Ginger.png";
    private static final String BOSS_PLACEHOLDER_PATH   = "entities/clawkins/Clawkin_14_Stray.png";

    // -----------------------------------------------------------------------
    // Button asset paths (individual PNG files)
    // -----------------------------------------------------------------------

    private static final String BUTTON1_PATH = "ui/button1.png";
    private static final String BUTTON2_PATH = "ui/button2.png";
    private static final String BUTTON3_PATH = "ui/button3.png";
    private static final String BUTTON4_PATH = "ui/button4.png";

    // Corner button asset paths
    private static final String INVENTORY_BUTTON_PATH = "ui/battle_ui/Sprites/Inventory_Button.png";
    private static final String FLEE_BUTTON_PATH = "ui/battle_ui/Sprites/Flee_Button.png";

    // Shadow sprite asset path
    private static final String SHADOW_PATH = "ui/battle_ui/Sprites/Shadow.png";

    // Clawkin container asset paths
    private static final String CLAWKIN_CONTAINER_PATH = "ui/battle_ui/Sprites/Clawkins_Container.png";
    private static final String ICON_DIR = "ui/battle_ui/Sprites/icons/";
    
    // Icon file name patterns
    private static final String GINGER_ICON = "Ginger_icon.png";
    private static final String GINGER_ICON_DOWN = "Ginger_icon_down.png";
    private static final String DART_ICON = "Dart_icon.png";
    private static final String DART_ICON_DOWN = "Dart_icon_down.png";
    private static final String SWEEPEA_ICON = "Sweepea'_icon.png";
    private static final String SWEEPEA_ICON_DOWN = "Sweepea'_icon_down.png";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Stage stage;
    private final Texture battleBg;
    private final BitmapFont font;

    /** Encounter-specific background texture loaded dynamically; disposed by this class. */
    private Texture encounterBgTex;

    /** Black overlay drawn on top of background for darkening effect. */
    private Image bgOverlay;
    private Texture bgOverlayTex;

    // Owned placeholder textures and actors (dispose in dispose())
    private Texture playerPlaceholderTex;
    private Texture bossPlaceholderTex;
    private Image playerImage;
    private Image bossImage;

    // Shadow textures and actors
    private Texture shadowTex;
    private Image playerShadow;
    private Image bossShadow;

    // Clawkin container UI
    private Texture clawkinContainerTex;
    private Image clawkinContainer;
    private Table clawkinIconTable;
    private Stack clawkinStack;  // Stack to layer container and icons
    private Table clawkinWrapper; // Wrapper table for positioning
    private List<Image> clawkinIcons;
    private List<Clawkin> currentParty;
    private String lastPartyVisualKey = "";
    
    // Selection highlight indicator
    private Image selectionHighlight;
    private Texture highlightTex;

    // Owned button textures (dispose in dispose())
    private Texture button1Tex;
    private Texture button2Tex;
    private Texture button3Tex;
    private Texture button4Tex;
    private Texture inventoryButtonTex;
    private Texture fleeButtonTex;

    // â”€â”€â”€ HP Bar UI Elements â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private Label playerNameLabel;
    private ProgressBar playerHpBar;
    private Label playerHpLabel;

    private Label bossNameTextLabel;
    private Label bossLevelLabel;
    private Table bossNameRow;
    private ProgressBar bossHpBar;
    private Label bossHpLabel;
    
    // ─── EXP/Level UI Elements ─────────────────────────────────────────────────────────────────────
    private Label playerLevelLabel;
    private ProgressBar playerExpBar;
    private Label playerExpLabel;

    // Reused UI actors/tables (for responsive relayout)
    private Image bg;
    private ImageButton attackBtn;
    private ImageButton defendBtn;
    private ImageButton specialBtn;
    private ImageButton itemBtn;
    private ImageButton inventoryBtn;
    private ImageButton fleeBtn;
    private Label attackLbl;
    private Label defendLbl;
    private Label specialLbl;
    private Label itemLbl;
    private Table playerHpTable;
    private Table bossHpTable;
    private Table playerHpCorner;
    private Table bossHpCorner;
    private Table playerExpTable;
    private Table inventoryCorner;
    private Table fleeCorner;
    private Table root;

    // HP tracking (updated externally via setters)
    private float playerCurrentHp = 100f;
    private float playerMaxHp = 100f;
    private float bossCurrentHp = 100f;
    private float bossMaxHp = 100f;
    
    // EXP/Level tracking
    private int playerLevel = 1;
    private float playerExpProgress = 0f;
    private int playerCurrentExp = 0;
    private int playerExpToNextLevel = 100;

    // Active clawkin portrait — loaded dynamically; disposed when clawkin changes
    private Texture activeClawkinTex;
    private String lastDisplayedClawkinKey = null;

    /** Enemy portrait loaded for this encounter (separate from static boss placeholder). */
    private Texture activeEnemyTex;
    private String lastEnemyPortraitKey = null;

    /** True while the HUD should be drawn and receive input. */
    private boolean visible = false;

    /** True if this is a wild battle (allows fleeing). */
    private boolean isWildBattle = false;

    /** Currently active Clawkin index (the one in battle). */
    private int activeClawkinIndex = 0;

    /** Currently highlighted Clawkin index (cursor position for navigation). */
    private int highlightedClawkinIndex = 0;

    /** Callback slots â€” set by GameScreen / BattleOverlay. */
    private Runnable onAttack = () -> {};
    private Runnable onDefend = () -> {};
    private Runnable onSpecial = () -> {};
    private Runnable onItem   = () -> {};
    private Runnable onInventory = () -> {};
    private Runnable onFlee = () -> {};
    private Runnable onClawkinSelected = () -> {}; // Called when a Clawkin icon is clicked
    private Runnable onSkillSelectionChanged = () -> {};

    /** Currently selected skill button (0=Attack, 1=Defend, 2=Special, 3=Item). */
    private int selectedSkillIndex = 0;

    /** True once updateSkillLabels(SkillManager, ...) has been called; disables legacy availability override. */
    private boolean skillManagerActive = false;

    private static final float SELECTED_SKILL_SCALE = 1.08f;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    /**
     * Builds the Stage and all widgets.
     * Uses a fixed 4:3 aspect ratio viewport (960x720) for consistent UI sizing.
     *
     * @param assetService used to load {@link TextureAsset#BATTLE_UI}
     */
    public BattleHud(AssetService assetService) {
        // Use FitViewport with fixed 4:3 dimensions for consistent UI sizing
        // This will add black bars (pillarboxing) on wider screens
        this.stage    = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        this.battleBg = assetService.load(TextureAsset.BATTLE_BACKGROUND);
        this.font     = new BitmapFont();
        this.font.getData().setScale(1.1f);

        buildWidgets();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Makes the HUD visible and enables mouse input for clicking buttons. */
    public void show() {
        visible = true;
        // Highlight will be synced to active Clawkin in setActiveClawkinIndex() on first battle start
        // Enable input processor for mouse clicks
        Gdx.input.setInputProcessor(stage);
    }

    /** Hides the HUD and removes the Stage as input processor. */
    public void hide() {
        visible = false;
        lastDisplayedClawkinKey = null;
        lastEnemyPortraitKey = null;
        lastPartyVisualKey = "";
        skillManagerActive = false;
        Gdx.input.setInputProcessor(null);
    }

    /**
     * Swaps the battle background to the given asset path.
     * If the path is null, blank, or the file doesn't exist, restores the default background.
     *
     * @param backgroundPath internal asset path to the background PNG
     */
    public void setBattleBackground(String backgroundPath) {
        Texture newTex = null;
        if (backgroundPath != null && !backgroundPath.isBlank()) {
            if (Gdx.files.internal(backgroundPath).exists()) {
                newTex = new Texture(Gdx.files.internal(backgroundPath));
            } else {
                Gdx.app.error("BattleHud", "Battle background not found: " + backgroundPath);
            }
        }

        // Dispose previous encounter-specific texture if any
        if (encounterBgTex != null) {
            encounterBgTex.dispose();
            encounterBgTex = null;
        }

        if (newTex != null) {
            encounterBgTex = newTex;
            if (bg != null) {
                bg.setDrawable(new TextureRegionDrawable(new TextureRegion(encounterBgTex)));
            }
        } else {
            // Restore default
            if (bg != null) {
                bg.setDrawable(new TextureRegionDrawable(new TextureRegion(battleBg)));
            }
        }
    }

    /**
     * Resets the battle background to the default asset.
     */
    public void resetBattleBackground() {
        if (encounterBgTex != null) {
            encounterBgTex.dispose();
            encounterBgTex = null;
        }
        if (bg != null) {
            bg.setDrawable(new TextureRegionDrawable(new TextureRegion(battleBg)));
        }
        setBackgroundOverlayOpacity(0f);
    }

    /**
     * Sets the opacity of the black foreground overlay drawn on top of the background.
     *
     * @param opacity 0.0 = fully transparent (no darkening), 1.0 = fully black
     */
    public void setBackgroundOverlayOpacity(float opacity) {
        if (bgOverlay != null) {
            bgOverlay.setColor(0f, 0f, 0f, Math.max(0f, Math.min(1f, opacity)));
        }
    }

    /** Returns true when the HUD is currently active. */
    public boolean isVisible() {
        return visible;
    }
    
    /** Returns the Stage for input hit detection. */
    public Stage getStage() {
        return stage;
    }

    /**
     * Updates and draws the HUD.  Call once per frame from
     * {@link BattleOverlay#render} when a battle session is active.
     * Renders black bars outside the 4:3 viewport area.
     */
    public void render() {
        if (!visible) return;
        
        // Draw black bars to cover areas outside the 4:3 viewport.
        // Must set GL viewport to full screen first so we can draw outside the FitViewport area.
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderBlackBars();
        
        // Apply viewport and render the stage
        stage.getViewport().apply();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }
    
    /**
     * Renders black bars (pillarboxing) outside the 4:3 gameplay area.
     * This covers any background content that might be visible.
     */
    private void renderBlackBars() {
        // Get the viewport's screen coordinates
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        
        // Get the viewport's actual rendering area
        int viewportX = stage.getViewport().getScreenX();
        int viewportY = stage.getViewport().getScreenY();
        int viewportWidth = stage.getViewport().getScreenWidth();
        int viewportHeight = stage.getViewport().getScreenHeight();

        // Only draw bars if there's actually a gap
        boolean hasGap = viewportX > 0
                || viewportY > 0
                || (viewportX + viewportWidth) < screenWidth
                || (viewportY + viewportHeight) < screenHeight;
        if (!hasGap) {
            return;
        }

        // Use a screen-space projection matrix
        com.badlogic.gdx.math.Matrix4 screenProj = new com.badlogic.gdx.math.Matrix4();
        screenProj.setToOrtho2D(0, 0, screenWidth, screenHeight);

        com.badlogic.gdx.graphics.glutils.ShapeRenderer sr = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
        sr.setProjectionMatrix(screenProj);
        sr.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        sr.setColor(0, 0, 0, 1);
        
        // Left bar
        if (viewportX > 0) {
            sr.rect(0, 0, viewportX, screenHeight);
        }
        
        // Right bar
        if (viewportX + viewportWidth < screenWidth) {
            sr.rect(viewportX + viewportWidth, 0, screenWidth - (viewportX + viewportWidth), screenHeight);
        }
        
        // Top bar
        if (viewportY + viewportHeight < screenHeight) {
            sr.rect(0, viewportY + viewportHeight, screenWidth, screenHeight - (viewportY + viewportHeight));
        }
        
        // Bottom bar
        if (viewportY > 0) {
            sr.rect(0, 0, screenWidth, viewportY);
        }
        
        sr.end();
        sr.dispose();
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
    public void setOnInventory(Runnable onInventory) { this.onInventory = onInventory != null ? onInventory : () -> {}; }
    public void setOnFlee(Runnable onFlee) { this.onFlee = onFlee != null ? onFlee : () -> {}; }
    public void setOnClawkinSelected(Runnable onClawkinSelected) { this.onClawkinSelected = onClawkinSelected != null ? onClawkinSelected : () -> {}; }
    public void setOnSkillSelectionChanged(Runnable onSkillSelectionChanged) {
        this.onSkillSelectionChanged = onSkillSelectionChanged != null ? onSkillSelectionChanged : () -> {};
    }

    /** Sets whether this is a wild battle (enables/disables flee button). */
    public void setWildBattle(boolean isWildBattle) { 
        this.isWildBattle = isWildBattle;
        if (fleeBtn != null) {
            fleeBtn.setDisabled(!isWildBattle);
            fleeBtn.setColor(isWildBattle ? Color.WHITE : Color.GRAY);
        }
    }

    /** Returns true if this is a wild battle. */
    public boolean isWildBattle() { return isWildBattle; }

    /** Invoke current Attack action callback. */
    public void triggerAttack() { onAttack.run(); }

    /** Invoke current Defend action callback. */
    public void triggerDefend() { onDefend.run(); }

    /** Invoke current Special action callback. */
    public void triggerSpecial() { onSpecial.run(); }

    /** Invoke current Item action callback. */
    public void triggerItem() { onItem.run(); }

    /** Invoke currently selected skill action callback. */
    public void triggerSelectedSkill() {
        int safeSelectedSkill = resolveNearestEnabledSkillIndex(selectedSkillIndex);
        if (safeSelectedSkill != selectedSkillIndex) {
            selectedSkillIndex = safeSelectedSkill;
            updateSelectedSkillVisuals();
            return;
        }
        switch (safeSelectedSkill) {
            case 0 -> triggerAttack();
            case 1 -> triggerDefend();
            case 2 -> triggerSpecial();
            case 3 -> triggerItem();
            default -> {
            }
        }
    }

    public void setSelectedSkillIndex(int index, boolean notifySelectionChanged) {
        if (index < 0 || index > 3) {
            return;
        }
        if (!isSkillSlotEnabled(index)) {
            return;
        }
        if (selectedSkillIndex == index) {
            return;
        }
        selectedSkillIndex = index;
        updateSelectedSkillVisuals();
        if (notifySelectionChanged) {
            onSkillSelectionChanged.run();
        }
    }

    public int getSelectedSkillIndex() {
        return selectedSkillIndex;
    }

    public void moveSelectedSkill(int direction) {
        int next = selectedSkillIndex;
        for (int i = 0; i < 4; i++) {
            next += direction;
            if (next < 0) {
                next = 3;
            } else if (next > 3) {
                next = 0;
            }
            if (isSkillSlotEnabled(next)) {
                setSelectedSkillIndex(next, true);
                return;
            }
        }
    }

    /** Invoke current Inventory action callback. */
    public void triggerInventory() { onInventory.run(); }

    /** Invoke current Flee action callback (only if wild battle). */
    public void triggerFlee() { 
        if (isWildBattle) {
            onFlee.run();
        }
    }

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
     *
     * @param encounterTableId optional table id from the map; included in the portrait cache key when present
     */
    public void updateEnemyCombatant(
            String encounterId,
            String encounterTableId,
            String displayName,
            String portraitPath,
            int enemyLevel) {
        String label = displayName != null && !displayName.isBlank()
                ? displayName
                : (encounterId != null && !encounterId.isBlank() ? encounterId : "Enemy");
        updateBossNameDisplay(label, enemyLevel);

        String pathKey = (encounterId == null ? "" : encounterId)
                + "\0" + (encounterTableId == null ? "" : encounterTableId)
                + "\0" + (portraitPath == null ? "" : portraitPath);
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
            applyTextureToBossImage(activeEnemyTex, false);  // Never flip boss images
            return;
        }
        restoreBossPlaceholderPortrait();
    }

    private void updateBossNameDisplay(String name, int enemyLevel) {
        if (bossNameTextLabel != null) {
            bossNameTextLabel.setText(name == null || name.isBlank() ? "Enemy" : name);
        }
        if (bossLevelLabel != null) {
            int clamped = Math.max(
                    LevelSystem.MIN_LEVEL,
                    Math.min(enemyLevel, LevelSystem.MAX_LEVEL));
            bossLevelLabel.setText("Lv. " + clamped);
        }
    }

    private static String clawkinVisualKey(Clawkin c) {
        String id = c.getId() == null ? "" : c.getId().trim();
        String name = c.getName() == null ? "" : c.getName().trim();
        String portraitPath = c.getImagePath() == null ? "" : c.getImagePath().trim();
        String iconPath = c.getIconImagePath() == null ? "" : c.getIconImagePath().trim();
        return id + "|" + name + "|" + portraitPath + "|" + iconPath;
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
            // No flip - use original orientation
            bossImage.setDrawable(new TextureRegionDrawable(region));
        }
    }

    private void applyTextureToBossImage(Texture tex, boolean flipHorizontally) {
        if (bossImage == null || tex == null) {
            return;
        }
        TextureRegion region = new TextureRegion(tex);
        if (flipHorizontally) {
            region.flip(true, false);
        }
        bossImage.setDrawable(new TextureRegionDrawable(region));
    }

    /**
     * Horizontal mirror for the boss HUD portrait only on the Bert Jr. fight
     * ({@code boss_0_encounter}, same id as {@link github.dluckycompany.clawkins.GameScreen}).
     */
    private static boolean isBertJrBossEncounter(String encounterId) {
        return encounterId != null && "boss_0_encounter".equalsIgnoreCase(encounterId.trim());
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
                    "entities/clawkins/Clawkin_01.png",
                    "characters/Clawkin_01.png"
            };
        }
        if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return new String[] {
                    "ui/Clawkin_02.png",
                    "entities/clawkins/Clawkin_02.png",
                    "characters/Clawkin_02.png"
            };
        }
        if (name.contains("dart") || id.contains("dart")) {
            return new String[] {
                    "ui/Clawkin_03.png",
                    "entities/clawkins/Clawkin_03.png",
                    "characters/Clawkin_03.png"
            };
        }
        return new String[] {
                "ui/Clawkin_01.png",
                "entities/clawkins/Clawkin_01.png",
                "characters/Clawkin_01.png"
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
                    "entities/clawkins/Clawkin_01.png",
                    "characters/Clawkin_01.png"
            };
        }
        if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return new String[] {
                    "ui/Clawkin_02.png",
                    "entities/clawkins/Clawkin_02.png",
                    "characters/Clawkin_02.png"
            };
        }
        if (name.contains("dart") || id.contains("dart")) {
            return new String[] {
                    "ui/Clawkin_03.png",
                    "entities/clawkins/Clawkin_03.png",
                    "characters/Clawkin_03.png"
            };
        }
        return new String[] {
                BOSS_PLACEHOLDER_PATH,
                "ui/Clawkin_02.png",
                "entities/clawkins/Clawkin_02.png",
                "characters/Clawkin_02.png"
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
    
    /**
     * Refreshes EXP bar visuals based on current EXP values.
     */
    private void updateExpBar() {
        if (playerExpBar != null) {
            playerExpBar.setValue(playerExpProgress);
        }
        if (playerLevelLabel != null) {
            playerLevelLabel.setText("LV " + playerLevel);
        }
        if (playerExpLabel != null) {
            playerExpLabel.setText(String.format("EXP: %d / %d", playerCurrentExp, playerExpToNextLevel));
        }
    }
    
    /**
     * Updates skill button labels with skill names, cooldown, and lock information.
     * Call this method when skills change, cooldowns update, or level changes.
     * 
     * @param skillManager The SkillManager containing skill unlock state
     * @param playerUnit The player's BattleUnit to check cooldowns
     */
    public void updateSkillLabels(SkillManager skillManager, BattleUnit playerUnit) {
        if (skillManager == null) {
            return;
        }
        skillManagerActive = true;
        
        // Update slot 1 (Attack button)
        updateSkillButton(
            attackBtn,
            attackLbl,
            skillManager.getSkillSlot(0),
            skillManager.getCurrentLevel(),
            playerUnit,
            "[1] "
        );
        
        // Update slot 2 (Defend button)
        updateSkillButton(
            defendBtn,
            defendLbl,
            skillManager.getSkillSlot(1),
            skillManager.getCurrentLevel(),
            playerUnit,
            "[2] "
        );
        
        // Update slot 3 (Special button)
        updateSkillButton(
            specialBtn,
            specialLbl,
            skillManager.getSkillSlot(2),
            skillManager.getCurrentLevel(),
            playerUnit,
            "[3] "
        );

        // Update slot 4 (Item button) — dimmed until Skill 4 unlocks at Level 20
        SkillSlot slot4 = skillManager.getSkillSlot(3);
        if (slot4 == null) {
            // No Skill 4 defined for this Clawkin — show dimmed placeholder
            setSkill4Locked("Lv. 20");
        } else if (slot4.isLocked(skillManager.getCurrentLevel())) {
            // Skill 4 exists but level requirement not met — show dimmed with unlock text
            setSkill4Locked(slot4.getLockDisplayText(skillManager.getCurrentLevel()));
        } else {
            // Level 20+ reached — fully enable Skill 4
            setSkill4Unlocked();
            updateSkillButton(
                itemBtn,
                itemLbl,
                slot4,
                skillManager.getCurrentLevel(),
                playerUnit,
                "[4] "
            );
        }
    }

    /**
     * Shows Skill 4 button in a locked/dimmed state (reduced opacity, disabled).
     */
    private void setSkill4Locked(String lockText) {
        if (itemBtn != null) {
            itemBtn.setVisible(true);
            itemBtn.setDisabled(true);
            itemBtn.setTouchable(Touchable.disabled);
            itemBtn.setColor(1f, 1f, 1f, 0.35f);
        }
        if (itemLbl != null) {
            itemLbl.setVisible(true);
            itemLbl.setText("[4] " + (lockText != null ? lockText : "Locked"));
            itemLbl.setColor(1f, 1f, 1f, 0.35f);
        }
    }

    /**
     * Shows Skill 4 button in a fully unlocked/enabled state.
     */
    private void setSkill4Unlocked() {
        if (itemBtn != null) {
            itemBtn.setVisible(true);
            itemBtn.setDisabled(false);
            itemBtn.setTouchable(Touchable.enabled);
            itemBtn.setColor(Color.WHITE);
        }
        if (itemLbl != null) {
            itemLbl.setVisible(true);
            itemLbl.setColor(Color.WHITE);
        }
    }
    
    /**
     * Updates a single skill button with lock/unlock state.
     */
    private void updateSkillButton(
        ImageButton button,
        Label label,
        SkillSlot slot,
        int currentLevel,
        BattleUnit playerUnit,
        String prefix
    ) {
        if (button == null || label == null || slot == null) {
            return;
        }
        
        BattleSkill skill = slot.getSkill();
        if (skill == null) {
            label.setText(prefix + "Locked");
            button.setDisabled(true);
            button.setTouchable(Touchable.disabled);
            button.setColor(0.3f, 0.3f, 0.3f, 1.0f); // Dark gray
            return;
        }
        
        // Check if skill is locked by level
        if (slot.isLocked(currentLevel)) {
            label.setText(prefix + slot.getLockDisplayText(currentLevel));
            button.setDisabled(true);
            button.setTouchable(Touchable.disabled);
            button.setColor(0.5f, 0.5f, 0.5f, 1.0f); // Gray with lock indicator
            return;
        }
        
        // Check if skill is on cooldown
        if (playerUnit != null && playerUnit.isSkillOnCooldown(skill.getName())) {
            int cooldown = playerUnit.getSkillCooldown(skill.getName());
            label.setText(prefix + skill.getName() + " (" + cooldown + ")");
            button.setDisabled(true);
            button.setTouchable(Touchable.disabled);
            button.setColor(0.6f, 0.6f, 0.8f, 1.0f); // Blue-gray for cooldown
            return;
        }
        
        // Skill is available
        label.setText(prefix + skill.getName());
        button.setDisabled(false);
        button.setTouchable(Touchable.enabled);
        button.setColor(Color.WHITE);
    }
    
    /**
     * Updates skill button labels with skill names and cooldown information.
     * Call this method when skills change or cooldowns update.
     * 
     * @param skill1 First skill (slot 1)
     * @param skill2 Second skill (slot 2)
     * @param skill3 Third skill (slot 3)
     * @param playerUnit The player's BattleUnit to check cooldowns
     * @deprecated Use updateSkillLabels(SkillManager, BattleUnit) instead
     */
    @Deprecated
    public void updateSkillLabels(BattleSkill skill1, BattleSkill skill2, BattleSkill skill3, BattleUnit playerUnit) {
        if (attackLbl != null && skill1 != null) {
            int cooldown = playerUnit != null ? playerUnit.getSkillCooldown(skill1.getName()) : 0;
            if (cooldown > 0) {
                attackLbl.setText("[1] " + skill1.getName() + " (" + cooldown + " turns)");
                attackBtn.setDisabled(true);
                attackBtn.setTouchable(Touchable.disabled);
                attackBtn.setColor(Color.GRAY);
            } else {
                attackLbl.setText("[1] " + skill1.getName());
                attackBtn.setDisabled(false);
                attackBtn.setTouchable(Touchable.enabled);
                attackBtn.setColor(Color.WHITE);
            }
        }
        
        if (defendLbl != null && skill2 != null) {
            int cooldown = playerUnit != null ? playerUnit.getSkillCooldown(skill2.getName()) : 0;
            if (cooldown > 0) {
                defendLbl.setText("[2] " + skill2.getName() + " (" + cooldown + " turns)");
                defendBtn.setDisabled(true);
                defendBtn.setTouchable(Touchable.disabled);
                defendBtn.setColor(Color.GRAY);
            } else {
                defendLbl.setText("[2] " + skill2.getName());
                defendBtn.setDisabled(false);
                defendBtn.setTouchable(Touchable.enabled);
                defendBtn.setColor(Color.WHITE);
            }
        }
        
        if (specialLbl != null && skill3 != null) {
            int cooldown = playerUnit != null ? playerUnit.getSkillCooldown(skill3.getName()) : 0;
            if (cooldown > 0) {
                specialLbl.setText("[3] " + skill3.getName() + " (" + cooldown + " turns)");
                specialBtn.setDisabled(true);
                specialBtn.setTouchable(Touchable.disabled);
                specialBtn.setColor(Color.GRAY);
            } else {
                specialLbl.setText("[3] " + skill3.getName());
                specialBtn.setDisabled(false);
                specialBtn.setTouchable(Touchable.enabled);
                specialBtn.setColor(Color.WHITE);
            }
        }
    }
    
    /**
     * Updates EXP/Level display from ClawkinData.
     * 
     * @param clawkinData The ClawkinData to read from
     */
    public void updateExpFromClawkinData(github.dluckycompany.clawkins.character.ClawkinData clawkinData) {
        if (clawkinData == null) {
            return;
        }
        
        this.playerLevel = clawkinData.getLevel();
        this.playerExpProgress = clawkinData.getExpProgressToNextLevel();
        this.playerCurrentExp = clawkinData.getCurrentExp() - github.dluckycompany.clawkins.character.LevelSystem.getExpRequiredForLevel(playerLevel);
        this.playerExpToNextLevel = clawkinData.getExpToNextLevel();
        
        updateExpBar();
    }
    
    /**
     * Updates EXP/Level display from level only (simplified version).
     * Shows level but EXP bar will be at 0% until full ClawkinData integration.
     * 
     * @param level The Clawkin's current level
     */
    public void updateExpFromLevel(int level) {
        this.playerLevel = level;
        this.playerExpProgress = 0f;
        this.playerCurrentExp = 0;
        this.playerExpToNextLevel = github.dluckycompany.clawkins.character.LevelSystem.getExpForNextLevel(level);
        
        updateExpBar();
    }

    /**
     * Updates EXP/Level display from total accumulated EXP.
     *
     * @param totalExp total accumulated EXP
     */
    public void updateExpFromTotalExp(int totalExp) {
        int safeTotalExp = Math.max(0, totalExp);
        int calculatedLevel = LevelSystem.calculateLevelFromExp(safeTotalExp);
        this.playerLevel = calculatedLevel;

        if (calculatedLevel >= LevelSystem.MAX_LEVEL) {
            this.playerExpProgress = 1f;
            this.playerCurrentExp = 0;
            this.playerExpToNextLevel = 0;
            updateExpBar();
            return;
        }

        int currentLevelExpFloor = LevelSystem.getExpRequiredForLevel(calculatedLevel);
        int expNeededForNextLevel = LevelSystem.getExpForNextLevel(calculatedLevel);
        int expIntoCurrentLevel = Math.max(0, safeTotalExp - currentLevelExpFloor);
        int clampedExpIntoCurrentLevel = Math.min(expIntoCurrentLevel, Math.max(0, expNeededForNextLevel));

        this.playerCurrentExp = clampedExpIntoCurrentLevel;
        this.playerExpToNextLevel = Math.max(0, expNeededForNextLevel);
        this.playerExpProgress = this.playerExpToNextLevel > 0
                ? (float) this.playerCurrentExp / (float) this.playerExpToNextLevel
                : 1f;

        updateExpBar();
    }
    
    /**
     * Grants EXP and updates the EXP bar display.
     * Handles level-ups and EXP overflow correctly.
     * 
     * @param expAmount Amount of EXP to grant
     * @param clawkinData The ClawkinData to update (for level-up processing)
     * @return List of level-up results (empty if no level-ups occurred)
     */
    public java.util.List<github.dluckycompany.clawkins.character.LevelUpResult> grantExp(
            int expAmount, 
            github.dluckycompany.clawkins.character.ClawkinData clawkinData) {
        if (clawkinData == null || expAmount <= 0) {
            return java.util.List.of();
        }
        
        // Grant EXP to ClawkinData (handles level-ups)
        java.util.List<github.dluckycompany.clawkins.character.LevelUpResult> levelUpResults = 
                clawkinData.grantExp(expAmount);
        
        // Update HUD display from ClawkinData
        updateExpFromClawkinData(clawkinData);
        
        return levelUpResults;
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
        if (shadowTex != null) shadowTex.dispose();
        if (clawkinContainerTex != null) clawkinContainerTex.dispose();
        if (highlightTex != null) highlightTex.dispose();
        if (button1Tex != null) button1Tex.dispose();
        if (button2Tex != null) button2Tex.dispose();
        if (button3Tex != null) button3Tex.dispose();
        if (button4Tex != null) button4Tex.dispose();
        if (inventoryButtonTex != null) inventoryButtonTex.dispose();
        if (fleeButtonTex != null) fleeButtonTex.dispose();
        if (activeClawkinTex != null) activeClawkinTex.dispose();
        if (activeEnemyTex != null) activeEnemyTex.dispose();
        if (encounterBgTex != null) encounterBgTex.dispose();
        if (bgOverlayTex != null) bgOverlayTex.dispose();
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
        if (inventoryButtonTex == null) inventoryButtonTex = new Texture(Gdx.files.internal(INVENTORY_BUTTON_PATH));
        if (fleeButtonTex == null) fleeButtonTex = new Texture(Gdx.files.internal(FLEE_BUTTON_PATH));

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

        bossNameTextLabel = new Label("Boss", labelStyle);
        LabelStyle enemyLevelStyle = new LabelStyle(font, ENEMY_LEVEL_LABEL_COLOR);
        bossLevelLabel = new Label("Lv. 1", enemyLevelStyle);
        bossLevelLabel.setFontScale(ENEMY_LEVEL_FONT_SCALE);
        bossNameRow = new Table();
        bossNameRow.add(bossNameTextLabel).right();
        bossNameRow.add(bossLevelLabel).right().padLeft(6f);
        bossHpBar = new ProgressBar(0f, 1f, 0.01f, false, bossHpStyle);
        bossHpBar.setValue(bossCurrentHp / bossMaxHp);
        bossHpLabel = new Label(String.format("%.0f / %.0f", bossCurrentHp, bossMaxHp), labelStyle);
        
        // ─── Create EXP/Level UI Elements ──────────────────────────────────────────────────────────
        ProgressBar.ProgressBarStyle playerExpStyle = createExpBarStyle(Color.YELLOW, Color.DARK_GRAY);
        playerLevelLabel = new Label("LV 1", labelStyle);
        playerExpBar = new ProgressBar(0f, 1f, 0.01f, false, playerExpStyle);
        playerExpBar.setValue(playerExpProgress);
        playerExpLabel = new Label("EXP: 0 / 100", labelStyle);

        attackBtn = loadButton(button1Tex, () -> {
            setSelectedSkillIndex(0, true);
            this.onAttack.run();
        });
        defendBtn = loadButton(button2Tex, () -> {
            setSelectedSkillIndex(1, true);
            this.onDefend.run();
        });
        specialBtn = loadButton(button3Tex, () -> {
            setSelectedSkillIndex(2, true);
            this.onSpecial.run();
        });
        itemBtn = loadButton(button4Tex, () -> {
            setSelectedSkillIndex(3, true);
            this.onItem.run();
        });
        registerSkillHoverSelection(attackBtn, 0);
        registerSkillHoverSelection(defendBtn, 1);
        registerSkillHoverSelection(specialBtn, 2);
        registerSkillHoverSelection(itemBtn, 3);
        attackBtn.getImage().setScaling(Scaling.fit);
        defendBtn.getImage().setScaling(Scaling.fit);
        specialBtn.getImage().setScaling(Scaling.fit);
        itemBtn.getImage().setScaling(Scaling.fit);

        // Create corner buttons with mouse interaction enabled
        inventoryBtn = loadButton(inventoryButtonTex, () -> this.onInventory.run());
        fleeBtn = loadButton(fleeButtonTex, () -> this.onFlee.run());
        inventoryBtn.getImage().setScaling(Scaling.fit);
        fleeBtn.getImage().setScaling(Scaling.fit);

        attackLbl = new Label("[1] 0 Turns", labelStyle);
        defendLbl = new Label("[2] 2 Turns", labelStyle);
        specialLbl = new Label("[3] 4 Turns", labelStyle);
        itemLbl = new Label("[4] Special Skill", labelStyle);

        playerHpTable = new Table();        bossHpTable = new Table();
        playerExpTable = new Table();

        playerHpCorner = new Table();
        playerHpCorner.setFillParent(true);
        playerHpCorner.top().left();

        bossHpCorner = new Table();
        bossHpCorner.setFillParent(true);
        bossHpCorner.top().right();

        // Create corner tables for inventory and flee buttons
        inventoryCorner = new Table();
        inventoryCorner.setFillParent(true);
        inventoryCorner.bottom().left();

        fleeCorner = new Table();
        fleeCorner.setFillParent(true);
        fleeCorner.bottom().right();

        root = new Table();
        root.setFillParent(true);
        root.bottom();

        loadPlaceholders();
        buildClawkinContainer();
        applyResponsiveLayout();
        positionPlaceholders();

        // Skill 4 (item button) starts in locked/dimmed state — fully enabled at Level 20 via updateSkillLabels
        setSkill4Locked("Lv. 20");

        stage.addActor(bg);

        // Black foreground overlay (sits on top of background, behind all other actors)
        Pixmap overlayPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        overlayPixmap.setColor(0f, 0f, 0f, 1f);
        overlayPixmap.fill();
        bgOverlayTex = new Texture(overlayPixmap);
        overlayPixmap.dispose();
        bgOverlay = new Image(new TextureRegionDrawable(new TextureRegion(bgOverlayTex)));
        bgOverlay.setFillParent(true);
        bgOverlay.setColor(0f, 0f, 0f, 0f); // Start fully transparent
        bgOverlay.setTouchable(Touchable.disabled);
        stage.addActor(bgOverlay);

        stage.addActor(clawkinWrapper);  // Container + icons
        stage.addActor(selectionHighlight);  // Highlight as separate actor for absolute positioning
        stage.addActor(playerHpCorner);
        stage.addActor(bossHpCorner);
        stage.addActor(playerShadow);  // Shadow behind player
        stage.addActor(bossShadow);    // Shadow behind boss
        stage.addActor(playerImage);
        stage.addActor(bossImage);
        stage.addActor(inventoryCorner);
        stage.addActor(fleeCorner);
        stage.addActor(root);
        updateSkillButtonAvailability();
        updateSelectedSkillVisuals();
    }

    /** 
     * Rebuilds cell sizing/padding using FIXED values for consistent UI sizing.
     * No longer scales based on window size - uses fixed virtual pixel dimensions.
     */
    private void applyResponsiveLayout() {
        if (root == null || playerHpTable == null || bossHpTable == null) return;

        // Use fixed sizes from constants - no more percentage-based scaling
        playerHpTable.clearChildren();
        playerHpTable.add(playerNameLabel).left().padBottom(2f);
        playerHpTable.row();
        playerHpTable.add(playerHpBar).width(HP_BAR_WIDTH).height(HP_BAR_HEIGHT).left();
        playerHpTable.row();
        playerHpTable.add(playerHpLabel).left().padTop(2f);
        
        // ─── Build EXP/Level Table ─────────────────────────────────────────────────────────────────
        playerExpTable.clearChildren();
        playerExpTable.add(playerLevelLabel).left().padTop(8f).padBottom(2f);
        playerExpTable.row();
        playerExpTable.add(playerExpBar).width(HP_BAR_WIDTH * 0.8f).height(12f).left();
        playerExpTable.row();
        playerExpTable.add(playerExpLabel).left().padTop(2f);

        bossHpTable.clearChildren();
        bossHpTable.add(bossNameRow).right().padBottom(2f);
        bossHpTable.row();
        bossHpTable.add(bossHpBar).width(HP_BAR_WIDTH).height(HP_BAR_HEIGHT).right();
        bossHpTable.row();
        bossHpTable.add(bossHpLabel).right().padTop(2f);

        playerHpCorner.clearChildren();
        playerHpCorner.top().left().pad(HP_TOP_PAD, HP_SIDE_PAD, 0f, 0f);
        
        // Stack HP and EXP tables vertically
        Table playerStatsStack = new Table();
        playerStatsStack.add(playerHpTable).left();
        playerStatsStack.row();
        playerStatsStack.add(playerExpTable).left();
        
        playerHpCorner.add(playerStatsStack).left();

        bossHpCorner.clearChildren();
        bossHpCorner.top().right().pad(HP_TOP_PAD, 0f, 0f, HP_SIDE_PAD);
        bossHpCorner.add(bossHpTable).right();

        root.clearChildren();
        root.bottom().padBottom(BOTTOM_PAD);
        root.add(attackBtn).size(BUTTON_SIZE, BUTTON_SIZE).padRight(BUTTON_GAP);
        root.add(defendBtn).size(BUTTON_SIZE, BUTTON_SIZE).padRight(BUTTON_GAP);
        root.add(specialBtn).size(BUTTON_SIZE, BUTTON_SIZE).padRight(BUTTON_GAP);
        root.add(itemBtn).size(BUTTON_SIZE, BUTTON_SIZE);
        root.row().padTop(LABEL_PAD_TOP);
        root.add(attackLbl).center().padRight(BUTTON_GAP);
        root.add(defendLbl).center().padRight(BUTTON_GAP);
        root.add(specialLbl).center().padRight(BUTTON_GAP);
        root.add(itemLbl).center();

        // Corner buttons - fixed sizes
        if (inventoryCorner != null && inventoryBtn != null) {
            inventoryCorner.clearChildren();
            inventoryCorner.bottom().left().pad(0f, CORNER_PAD, CORNER_PAD, 0f);
            inventoryCorner.add(inventoryBtn).size(CORNER_BUTTON_SIZE, CORNER_BUTTON_SIZE);
            inventoryCorner.invalidateHierarchy();
        }

        if (fleeCorner != null && fleeBtn != null) {
            fleeCorner.clearChildren();
            fleeCorner.bottom().right().pad(0f, 0f, CORNER_PAD, CORNER_PAD);
            fleeCorner.add(fleeBtn).size(CORNER_BUTTON_SIZE, CORNER_BUTTON_SIZE);
            fleeCorner.invalidateHierarchy();
        }

        root.invalidateHierarchy();
        playerHpCorner.invalidateHierarchy();
        bossHpCorner.invalidateHierarchy();
        updateSelectedSkillVisuals();

        // Position Clawkin container
        positionClawkinContainer();
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
     * Creates a ProgressBar style for EXP bar with specified fill and background colors.
     * EXP bar is visually smaller/thinner than HP bar.
     */
    private ProgressBar.ProgressBarStyle createExpBarStyle(Color fillColor, Color backgroundColor) {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();

        // Create colored pixmaps for fill and background (thinner than HP bar)
        Pixmap fillPixmap = new Pixmap(4, 12, Pixmap.Format.RGBA8888);
        fillPixmap.setColor(fillColor);
        fillPixmap.fill();
        Texture fillTexture = new Texture(fillPixmap);
        fillPixmap.dispose();

        Pixmap bgPixmap = new Pixmap(4, 12, Pixmap.Format.RGBA8888);
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
     * Initial size will be set by positionPlaceholders().
     */
    private void loadPlaceholders() {
        // Load shadow texture (shared by both player and boss)
        if (shadowTex == null) {
            shadowTex = new Texture(Gdx.files.internal(SHADOW_PATH));
        }

        if (playerImage == null) {
            playerPlaceholderTex = new Texture(Gdx.files.internal(PLAYER_PLACEHOLDER_PATH));
            playerImage = new Image(new TextureRegionDrawable(new TextureRegion(playerPlaceholderTex)));
            playerImage.setSize(PLAYER_SIZE, PLAYER_SIZE);
            playerImage.setName("player_placeholder");
        }

        if (playerShadow == null) {
            playerShadow = new Image(new TextureRegionDrawable(new TextureRegion(shadowTex)));
            playerShadow.setName("player_shadow");
            // Set transparency for natural shadow look
            playerShadow.setColor(1f, 1f, 1f, 0.5f);
        }

        if (bossImage == null) {
            bossPlaceholderTex = new Texture(Gdx.files.internal(BOSS_PLACEHOLDER_PATH));

            // Create region and flip it
            TextureRegion bossRegion = new TextureRegion(bossPlaceholderTex);
            
            // flip(flipX, flipY). 
            // Use (true, false) to mirror left/right. 
            // Use (true, true) for a full 180-degree upside-down rotation.
            // bossRegion.flip(true, false); 

            bossImage = new Image(new TextureRegionDrawable(bossRegion));
            bossImage.setSize(BOSS_SIZE, BOSS_SIZE);
            bossImage.setName("boss_placeholder");
        }

        if (bossShadow == null) {
            bossShadow = new Image(new TextureRegionDrawable(new TextureRegion(shadowTex)));
            bossShadow.setName("boss_shadow");
            // Set transparency for natural shadow look
            bossShadow.setColor(1f, 1f, 1f, 0.5f);
        }
    }

    /**
     * Positions placeholders in the center of the screen (center-left for player,
     * center-right for boss).
     */
    private void positionPlaceholders() {
        if (playerImage == null || bossImage == null) return;

        float w = VIRTUAL_WIDTH;
        float h = VIRTUAL_HEIGHT;

        // Use fixed sizes from constants
        float playerW = PLAYER_SIZE;
        float playerH = PLAYER_SIZE;
        float bossW = BOSS_SIZE;
        float bossH = BOSS_SIZE;

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

        // Position shadows below characters
        if (playerShadow != null) {
            // Shadow is slightly wider than the character (1.2x width)
            float shadowW = playerW * 1.2f;
            // Shadow height is proportional but flatter (0.3x of width for ellipse effect)
            float shadowH = shadowW * 0.3f;
            playerShadow.setSize(shadowW, shadowH);
            
            // Position shadow centered horizontally under the character, at the feet
            float playerX = (w * 0.25f) - (playerW / 2f);
            float playerY = (h / 2f) - (playerH / 2f);
            float shadowX = playerX + (playerW / 2f) - (shadowW / 2f);
            float shadowY = playerY - (shadowH * 0.5f); // Slightly below the character's feet
            playerShadow.setPosition(shadowX, shadowY);
        }

        if (bossShadow != null) {
            // Shadow is slightly wider than the boss (1.2x width)
            float shadowW = bossW * 1.2f;
            // Shadow height is proportional but flatter (0.3x of width for ellipse effect)
            float shadowH = shadowW * 0.3f;
            bossShadow.setSize(shadowW, shadowH);
            
            // Position shadow centered horizontally under the boss, at the feet
            float bossX = (w * 0.75f) - (bossW / 2f);
            float bossY = (h / 2f) - (bossH / 2f);
            float shadowX = bossX + (bossW / 2f) - (shadowW / 2f);
            float shadowY = bossY - (shadowH * 0.5f); // Slightly below the boss's feet
            bossShadow.setPosition(shadowX, shadowY);
        }
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

    private void registerSkillHoverSelection(ImageButton button, int skillIndex) {
        if (button == null) {
            return;
        }
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                setSelectedSkillIndex(skillIndex, true);
            }
        });
    }

    private void updateSelectedSkillVisuals() {
        applySkillButtonScale(attackBtn, selectedSkillIndex == 0);
        applySkillButtonScale(defendBtn, selectedSkillIndex == 1);
        applySkillButtonScale(specialBtn, selectedSkillIndex == 2);
        applySkillButtonScale(itemBtn, selectedSkillIndex == 3);
    }

    private void applySkillButtonScale(ImageButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTransform(true);
        button.setOrigin(button.getWidth() * 0.5f, button.getHeight() * 0.5f);
        float scale = selected && !button.isDisabled() ? SELECTED_SKILL_SCALE : 1f;
        button.setScale(scale);
    }

    private void updateSkillButtonAvailability() {
        // When SkillManager-based updateSkillLabels has been called, it is the authoritative
        // source for button enabled/disabled state. Skip the legacy skill-count-based override.
        if (skillManagerActive) {
            return;
        }
        int skillCount = getActiveSkillCount();
        updateSkillButtonAvailability(attackBtn, 0, skillCount);
        updateSkillButtonAvailability(defendBtn, 1, skillCount);
        updateSkillButtonAvailability(specialBtn, 2, skillCount);
        updateSkillButtonAvailability(itemBtn, 3, skillCount);

        int safeIndex = resolveNearestEnabledSkillIndex(selectedSkillIndex);
        if (safeIndex != selectedSkillIndex) {
            selectedSkillIndex = safeIndex;
        }
        updateSelectedSkillVisuals();
    }

    private void updateSkillButtonAvailability(ImageButton button, int skillIndex, int availableSkillCount) {
        if (button == null) {
            return;
        }
        boolean enabled = skillIndex < availableSkillCount;
        button.setDisabled(!enabled);
        button.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
        button.setColor(enabled ? new Color(1f, 1f, 1f, 1f) : new Color(1f, 1f, 1f, 0.5f));
    }

    private int getActiveSkillCount() {
        Clawkin active = getClawkinAtSlot(activeClawkinIndex);
        if (active == null) {
            return 3;
        }
        List<BattleSkill> skills = active.getSkills();
        if (skills == null || skills.isEmpty()) {
            return 1;
        }
        int count = 0;
        for (BattleSkill skill : skills) {
            if (skill == null) {
                break;
            }
            count++;
            if (count >= 4) {
                break;
            }
        }
        if (count <= 0) {
            return 1;
        }
        return Math.min(4, count);
    }

    private int resolveNearestEnabledSkillIndex(int preferredIndex) {
        if (isSkillSlotEnabled(preferredIndex)) {
            return preferredIndex;
        }
        for (int i = 0; i < 4; i++) {
            if (isSkillSlotEnabled(i)) {
                return i;
            }
        }
        return 0;
    }

    public boolean isSkillSlotEnabled(int index) {
        ImageButton button = switch (index) {
            case 0 -> attackBtn;
            case 1 -> defendBtn;
            case 2 -> specialBtn;
            case 3 -> itemBtn;
            default -> null;
        };
        return button != null && !button.isDisabled();
    }

    /**
     * Creates an {@link ImageButton} for visual display only (no mouse interaction).
     * Used for buttons that are controlled via keyboard input.
     */
    private static ImageButton loadButtonVisualOnly(Texture buttonTex) {
        TextureRegion region = new TextureRegion(buttonTex);
        TextureRegionDrawable upDrawable = new TextureRegionDrawable(region);

        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp   = upDrawable;
        style.imageOver = upDrawable;
        style.imageDown = upDrawable;

        ImageButton btn = new ImageButton(style);
        // Disable touch/click interaction - keyboard only
        btn.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        
        return btn;
    }

    /**
     * Builds the Clawkin container UI with party member icons.
     * Creates a vertical container on the left side showing party status.
     * Uses Stack to layer container background and icons properly.
     */
    private void buildClawkinContainer() {
        // Load container texture
        if (clawkinContainerTex == null) {
            clawkinContainerTex = new Texture(Gdx.files.internal(CLAWKIN_CONTAINER_PATH));
        }

        // Create container background image
        if (clawkinContainer == null) {
            clawkinContainer = new Image(new TextureRegionDrawable(new TextureRegion(clawkinContainerTex)));
            clawkinContainer.setName("clawkin_container");
            clawkinContainer.setScaling(Scaling.stretch);
        }

        // Create selection highlight texture (yellow border)
        if (highlightTex == null) {
            highlightTex = createHighlightTexture();
        }

        // Create selection highlight image
        if (selectionHighlight == null) {
            selectionHighlight = new Image(new TextureRegionDrawable(new TextureRegion(highlightTex)));
            selectionHighlight.setName("selection_highlight");
            selectionHighlight.setColor(1f, 1f, 0f, 0.8f); // Yellow with transparency
        }

        // Create table for icons (will be layered on top of container)
        if (clawkinIconTable == null) {
            clawkinIconTable = new Table();
            clawkinIconTable.center(); // Center icons within the table
        }

        // Create stack to layer container and icons (highlight positioned separately)
        if (clawkinStack == null) {
            clawkinStack = new Stack();
            clawkinStack.add(clawkinContainer); // Background layer
            clawkinStack.add(clawkinIconTable); // Foreground layer (icons)
        }

        // Create wrapper table for positioning on screen
        // DON'T use setFillParent - we'll position it manually
        if (clawkinWrapper == null) {
            clawkinWrapper = new Table();
            clawkinWrapper.add(clawkinStack); // Add the stack to the wrapper
        }

        // Initialize icon list
        if (clawkinIcons == null) {
            clawkinIcons = new ArrayList<>();
        }
    }

    /**
     * Creates a texture for the selection highlight border.
     * @return A texture with a border outline
     */
    private Texture createHighlightTexture() {
        int size = 128;
        int borderWidth = 4;
        
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        
        // Draw border (hollow rectangle)
        pixmap.setColor(1f, 1f, 0f, 1f); // Yellow
        
        // Top border
        pixmap.fillRectangle(0, 0, size, borderWidth);
        // Bottom border
        pixmap.fillRectangle(0, size - borderWidth, size, borderWidth);
        // Left border
        pixmap.fillRectangle(0, 0, borderWidth, size);
        // Right border
        pixmap.fillRectangle(size - borderWidth, 0, borderWidth, size);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return texture;
    }

    /**
     * Updates the Clawkin container with current party data.
     * Refreshes icons based on party member health states.
     */
    public void updateClawkinContainer(List<Clawkin> party) {
        if (party == null) {
            return;
        }

        String nextVisualKey = buildPartyVisualKey(party);
        if (nextVisualKey.equals(lastPartyVisualKey)) {
            return;
        }

        // Store current party reference
        this.currentParty = party;
        this.lastPartyVisualKey = nextVisualKey;

        // Trigger repositioning which will rebuild icons
        positionClawkinContainer();
    }

    /**
     * Creates an icon for a Clawkin based on its current health state.
     */
    private Image createClawkinIcon(Clawkin clawkin) {
        if (clawkin == null) {
            return null;
        }

        String iconPath = getIconPathForClawkin(clawkin);
        if (iconPath == null) {
            return null;
        }

        try {
            Texture iconTex = new Texture(Gdx.files.internal(iconPath));
            Image icon = new Image(new TextureRegionDrawable(new TextureRegion(iconTex)));
            icon.setScaling(Scaling.fit);
            icon.setName("clawkin_icon_" + clawkin.getId());
            return icon;
        } catch (Exception e) {
            Gdx.app.error("BattleHud", "Failed to load icon: " + iconPath, e);
            return null;
        }
    }

    /**
     * Determines the correct icon path based on Clawkin name/ID and health.
     */
    private String getIconPathForClawkin(Clawkin clawkin) {
        boolean isDown = clawkin.getCurrentHp() <= 0;
        String configuredIconPath = normalizeAssetPath(clawkin.getIconImagePath());
        if (!configuredIconPath.isBlank()) {
            if (isDown) {
                String downVariantPath = toDownVariantPath(configuredIconPath);
                if (!downVariantPath.isBlank() && Gdx.files.internal(downVariantPath).exists()) {
                    return downVariantPath;
                }
            }
            if (Gdx.files.internal(configuredIconPath).exists()) {
                return configuredIconPath;
            }
            Gdx.app.error("BattleHud", "Configured clawkin icon path not found: " + configuredIconPath);
        }

        String name = clawkin.getName() != null ? clawkin.getName().toLowerCase() : "";
        String id = clawkin.getId() != null ? clawkin.getId().toLowerCase() : "";

        // Determine which icon to use based on name/ID
        if (name.contains("ginger") || id.contains("ginger")) {
            return ICON_DIR + (isDown ? GINGER_ICON_DOWN : GINGER_ICON);
        } else if (name.contains("dart") || id.contains("dart")) {
            return ICON_DIR + (isDown ? DART_ICON_DOWN : DART_ICON);
        } else if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return ICON_DIR + (isDown ? SWEEPEA_ICON_DOWN : SWEEPEA_ICON);
        }

        // Default to Ginger if unknown
        return ICON_DIR + (isDown ? GINGER_ICON_DOWN : GINGER_ICON);
    }

    private static String normalizeAssetPath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String trimmed = rawPath.trim();
        if (trimmed.startsWith("/")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private static String toDownVariantPath(String path) {
        int extensionDotIndex = path.lastIndexOf('.');
        if (extensionDotIndex <= 0) {
            return "";
        }
        return path.substring(0, extensionDotIndex) + "_down" + path.substring(extensionDotIndex);
    }

    /**
     * Refreshes Clawkin icons based on current party health.
     * Call this when party member health changes during battle.
     */
    public void refreshClawkinIcons() {
        if (currentParty != null) {
            updateClawkinContainer(currentParty);
        }
    }

    /**
     * Sets the currently active Clawkin index (the one in battle).
     * On first initialization, syncs the highlight to match.
     * After that, highlight is independent for navigation.
     * @param index The index of the active Clawkin (0-2)
     */
    public void setActiveClawkinIndex(int index) {
        int maxSlots = currentParty == null ? 3 : Math.min(3, currentParty.size());
        if (index < 0 || index >= maxSlots) {
            return;
        }
        
        boolean isFirstTimeSet = (this.activeClawkinIndex == 0 && this.highlightedClawkinIndex == 0 && visible);
        
        if (this.activeClawkinIndex == index) {
            return;
        }
        this.activeClawkinIndex = index;
        
        // On first battle start, sync highlight to active Clawkin
        // This ensures if player selected Sweepea before battle, highlight starts on Sweepea
        if (isFirstTimeSet) {
            this.highlightedClawkinIndex = index;
        }
        
        // After first initialization, DO NOT sync highlight - let player continue navigating
        // Refresh container to update visual indicator
        positionClawkinContainer();
        updateSkillButtonAvailability();
    }

    /**
     * Gets the currently active Clawkin index.
     * @return The index of the active Clawkin (0-2)
     */
    public int getActiveClawkinIndex() {
        return activeClawkinIndex;
    }

    /**
     * Gets the currently highlighted Clawkin index (cursor position).
     * @return The index of the highlighted Clawkin (0-2)
     */
    public int getHighlightedClawkinIndex() {
        return highlightedClawkinIndex;
    }

    /**
     * Moves the selection highlight up (decreases index).
     */
    public void moveSelectionUp() {
        if (currentParty == null || currentParty.isEmpty()) return;

        int slotCount = Math.min(3, currentParty.size());
        int newIndex = highlightedClawkinIndex - 1;
        if (newIndex < 0) {
            newIndex = slotCount - 1; // Wrap to bottom
        }

        highlightedClawkinIndex = newIndex;
        Gdx.app.log("BattleHud", "Move UP: highlightedIndex = " + highlightedClawkinIndex);
        updateSelectionHighlight();
    }

    /**
     * Moves the selection highlight down (increases index).
     */
    public void moveSelectionDown() {
        if (currentParty == null || currentParty.isEmpty()) return;

        int slotCount = Math.min(3, currentParty.size());
        int newIndex = highlightedClawkinIndex + 1;
        if (newIndex >= slotCount) {
            newIndex = 0; // Wrap to top
        }

        highlightedClawkinIndex = newIndex;
        Gdx.app.log("BattleHud", "Move DOWN: highlightedIndex = " + highlightedClawkinIndex);
        updateSelectionHighlight();
    }

    /**
     * Checks if the currently highlighted Clawkin can be switched to.
     * @return true if the Clawkin is valid and can be switched to
     */
    public boolean canSwitchToHighlighted() {
        if (currentParty == null || highlightedClawkinIndex >= currentParty.size()) {
            return false;
        }
        
        Clawkin highlighted = getClawkinAtSlot(highlightedClawkinIndex);
        if (highlighted == null) {
            return false;
        }
        
        // Cannot switch to a knocked out Clawkin
        if (highlighted.getCurrentHp() <= 0) {
            return false;
        }
        
        // Cannot switch to the already active Clawkin
        if (highlightedClawkinIndex == activeClawkinIndex) {
            return false;
        }
        
        return true;
    }

    /**
     * Gets the Clawkin at a specific slot index.
     * @param slotIndex The slot index (0=Ginger, 1=Swee'pea, 2=Dart)
     * @return The Clawkin at that slot, or null if not found
     */
    public Clawkin getClawkinAtSlot(int slotIndex) {
        if (currentParty == null) {
            return null;
        }

        if (slotIndex < 0 || slotIndex >= currentParty.size() || slotIndex >= 3) {
            return null;
        }
        return currentParty.get(slotIndex);
    }

    /**
     * Gets the name of the currently highlighted Clawkin.
     * @return The Clawkin's name, or null if not found
     */
    public String getHighlightedClawkinName() {
        Clawkin highlighted = getClawkinAtSlot(highlightedClawkinIndex);
        return highlighted != null ? highlighted.getName() : null;
    }

    /**
     * Positions and sizes the Clawkin container on the left side of the screen.
     * Ensures icons are properly centered within container slots.
     * Uses FIXED sizes for consistent appearance.
     */
    private void positionClawkinContainer() {
        if (clawkinWrapper == null || clawkinStack == null || clawkinIconTable == null) {
            return;
        }

        // Use fixed virtual dimensions
        float w = VIRTUAL_WIDTH;
        float h = VIRTUAL_HEIGHT;

        // Use fixed container sizes from constants
        float containerW = CONTAINER_WIDTH;
        float containerH = CONTAINER_HEIGHT;

        // Update wrapper table
        clawkinWrapper.clearChildren();
        clawkinWrapper.add(clawkinStack).size(containerW, containerH);

        // Position wrapper manually at LEFT EDGE, vertically centered
        float wrapperX = 0f; // Flush against left edge
        float wrapperY = (h / 2f) - (containerH / 2f); // Vertically centered
        clawkinWrapper.setPosition(wrapperX, wrapperY);
        clawkinWrapper.setSize(containerW, containerH);

        // Rebuild icon table with SLOT-BASED sizing
        clawkinIconTable.clearChildren();
        clawkinIconTable.top(); // Align from top

        if (currentParty != null && !currentParty.isEmpty()) {
            // SLOT CALCULATION
            // Container has 3 equal vertical slots
            int numSlots = 3; // Always 3 slots (top, middle, bottom)
            float slotHeight = containerH / numSlots; // Each slot is 1/3 of container
            float slotWidth = containerW; // Slot width = container width

            // Icon sizing within slot
            // Use 95% of slot dimensions to leave padding
            float iconWidth = slotWidth * 0.92f;
            float iconHeight = slotHeight * 0.92f;
            
            // Use the smaller dimension to maintain aspect ratio
            float iconSize = Math.min(iconWidth, iconHeight);

            // Padding to center icon within slot
            float verticalPadding = (slotHeight - iconSize) / 2f;
            float horizontalPadding = (slotWidth - iconSize) / 2f;

            // Add icons to table in slot order (top to bottom)
            for (int i = 0; i < numSlots; i++) {
                Clawkin clawkin = getClawkinAtSlot(i);
                Image icon = createClawkinIcon(clawkin);
                
                // Check if this slot is active (in battle)
                boolean isActive = (i == activeClawkinIndex);
                
                if (icon != null) {
                    // Apply visual tint to active icon (subtle indicator)
                    if (isActive) {
                        // Slightly brighter for active Clawkin
                        icon.setColor(1.1f, 1.1f, 1.0f, 1.0f);
                    } else {
                        // Normal appearance
                        icon.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                    
                    // Make icon clickable
                    final int slotIndex = i;
                    icon.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            // Update highlighted index when clicked
                            highlightedClawkinIndex = slotIndex;
                            updateSelectionHighlight();
                            // Trigger the selection callback (shows confirmation dialog)
                            onClawkinSelected.run();
                        }
                    });
                    
                    // Add icon with exact slot dimensions
                    clawkinIconTable.add(icon)
                        .size(iconSize, iconSize)
                        .padTop(i == 0 ? verticalPadding : 0f) // Top padding for first slot
                        .padBottom(verticalPadding)
                        .padLeft(horizontalPadding)
                        .padRight(horizontalPadding)
                        .center()
                        .row();
                } else {
                    // Empty slot - add spacer to maintain layout
                    clawkinIconTable.add().size(slotWidth, slotHeight).row();
                }
            }
        }

        clawkinWrapper.invalidateHierarchy();
        clawkinIconTable.invalidateHierarchy();
        
        // Update selection highlight position
        updateSelectionHighlight();
    }

    /**
     * Updates the position and size of the selection highlight to match the highlighted slot.
     * Uses FIXED sizes for consistent appearance.
     */
    private void updateSelectionHighlight() {
        if (selectionHighlight == null || clawkinWrapper == null) {
            return;
        }

        // Use fixed virtual dimensions
        float w = VIRTUAL_WIDTH;
        float h = VIRTUAL_HEIGHT;

        // Use fixed container sizes (must match positionClawkinContainer)
        float containerW = CONTAINER_WIDTH;
        float containerH = CONTAINER_HEIGHT;

        // Slot dimensions
        int numSlots = 3;
        float slotHeight = containerH / numSlots;
        float slotWidth = containerW;

        // Highlight size (match icon size for proper fit)
        float iconWidth = slotWidth * 0.92f;
        float iconHeight = slotHeight * 0.92f;
        float iconSize = Math.min(iconWidth, iconHeight);
        
        float highlightW = iconSize;
        float highlightH = iconSize;

        // Container position (must match positionClawkinContainer)
        float containerX = 0f; // Flush against left edge
        float containerY = (h / 2f) - (containerH / 2f); // Vertically centered

        // Calculate Y position for the highlighted slot
        // Slots are arranged from top to bottom (index 0 = top, 1 = middle, 2 = bottom)
        // Start from top of container and move down by slot index
        float slotTopY = containerY + containerH - (slotHeight * highlightedClawkinIndex);
        float slotCenterY = slotTopY - (slotHeight / 2f);

        // Center highlight in slot
        float highlightX = containerX + (slotWidth - highlightW) / 2f;
        float highlightY = slotCenterY - (highlightH / 2f);

        selectionHighlight.setSize(highlightW, highlightH);
        selectionHighlight.setPosition(highlightX, highlightY);
        
        // Bring to front to ensure it's visible above container
        selectionHighlight.toFront();

        // Make highlight visible
        selectionHighlight.setVisible(true);
    }

    private static String buildPartyVisualKey(List<Clawkin> party) {
        StringBuilder key = new StringBuilder();
        int slots = Math.min(3, party.size());
        for (int i = 0; i < slots; i++) {
            Clawkin c = party.get(i);
            if (c == null) {
                key.append("null").append('|');
                continue;
            }
            key.append(c.getId() == null ? "" : c.getId().trim()).append('|')
               .append(c.getCurrentHp()).append('|')
               .append(c.getIconImagePath() == null ? "" : c.getIconImagePath().trim()).append('|');
        }
        return key.toString();
    }
}