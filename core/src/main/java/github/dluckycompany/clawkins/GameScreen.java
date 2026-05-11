package github.dluckycompany.clawkins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.asset.MapAsset;
import github.dluckycompany.clawkins.asset.MapAssetName;
import github.dluckycompany.clawkins.audio.AudioEventType;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.MusicTrack;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.battle.BattleOverlay;
import github.dluckycompany.clawkins.battle.BattlePhase;
import github.dluckycompany.clawkins.battle.BattleService;
import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.battle.BattleTransition;
import github.dluckycompany.clawkins.battle.BattleUnit;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.component.MapTransitionZone;
import github.dluckycompany.clawkins.component.CameraFollow;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.Move;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.PlayerAnimation;
import github.dluckycompany.clawkins.component.Prop;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterDetectionSystem;
import github.dluckycompany.clawkins.encounter.EncounterEvent;
import github.dluckycompany.clawkins.encounter.EncounterEventBus;
import github.dluckycompany.clawkins.encounter.EncounterEventType;
import github.dluckycompany.clawkins.encounter.RandomEncounterGenerator;
import github.dluckycompany.clawkins.input.InputConventions;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.ItemFactory;
import github.dluckycompany.clawkins.item.MerchantInventory;
import github.dluckycompany.clawkins.model.Gender;
import github.dluckycompany.clawkins.model.PlayerProfile;
import github.dluckycompany.clawkins.progress.PlayerProgress;
import github.dluckycompany.clawkins.save.SaveState;
import github.dluckycompany.clawkins.save.SaveStateManager;
import github.dluckycompany.clawkins.system.AnimationSystem;
import github.dluckycompany.clawkins.system.CameraSystem;
import github.dluckycompany.clawkins.system.CerberusBridgeWalkSlowSystem;
import github.dluckycompany.clawkins.system.EnemySystem;
import github.dluckycompany.clawkins.system.EnemyTrainerSpriteSystem;
import github.dluckycompany.clawkins.system.InteractionSystem;
import github.dluckycompany.clawkins.system.InteractionSystem.SpecialInteractionContext;
import github.dluckycompany.clawkins.system.MapTransitionSystem;
import github.dluckycompany.clawkins.system.MoveSystem;
import github.dluckycompany.clawkins.system.PlayerInputSystem;
import github.dluckycompany.clawkins.system.RandomEncounterSystem;
import github.dluckycompany.clawkins.system.RenderSystem;
import github.dluckycompany.clawkins.tiled.TiledObjectConfigurator;
import github.dluckycompany.clawkins.tiled.TiledService;
import github.dluckycompany.clawkins.ui.DialogueBoxRenderer;
import github.dluckycompany.clawkins.ui.DialogueOverlay;
import github.dluckycompany.clawkins.ui.HudWallet;
import github.dluckycompany.clawkins.ui.IntroExpositionOverlay;
import github.dluckycompany.clawkins.ui.InventoryScreen;
import github.dluckycompany.clawkins.ui.MainMenuScreen;
import github.dluckycompany.clawkins.ui.MainSideMenuOverlay;
import github.dluckycompany.clawkins.ui.MerchantShopUI;
import github.dluckycompany.clawkins.ui.SaveStateScreen;
import github.dluckycompany.clawkins.ui.SummaryScreen;
import github.dluckycompany.clawkins.ui.TeamViewerScreen;

/**
 * Main game screen coordinating world rendering, ECS engine, and UI overlays.
 * 
 * ============================================================================
 * COORDINATE SYSTEM ARCHITECTURE
 * ============================================================================
 * 
 * This game uses THREE coordinate systems that must be carefully managed:
 * 
 * 1. WORLD COORDINATES (Game Space)
 *    - Controlled by: RenderSystem's world viewport (game.getViewport())
 *    - Aspect ratio: Handled by current game viewport (typically FitViewport)
 *    - Purpose: Renderer, ECS entities, tiled maps
 * 
 * 2. UI COORDINATES (Virtual Space)
 *    - Controlled by: inventoryStage, hudStage (FitViewport 800x600)
 *    - Fixed resolution: Always 800x600 virtual units
 *    - Purpose: Inventory, HUD, menus, dialogs
 *    - Key property: CONSTANT across all physical screen resolutions
 * 
 * 3. PHYSICAL SCREEN (Pixel Space)
 *    - Determined by: Gdx.graphics.getWidth/Height()
 *    - Variable: Changes during fullscreen transitions
 *    - Handled by: Viewport.apply() and camera.combined transformation
 * 
 * ============================================================================
 * CRITICAL RENDERING SEQUENCE
 * ============================================================================
 * 
 * For each frame, coordinate transformation must occur in this order:
 * 
 *   Physical Screen Pixels (1920x1080, 3840x2160, etc.)
 *          ↓
 *   Viewport.apply() - applies glViewport() and camera
 *          ↓
 *   Camera.update() - calculates combined transformation matrix
 *          ↓
 *   Batch.setProjectionMatrix(camera.combined) - sets batch to use this space
 *          ↓
 *   Virtual World Coordinates (800x600 for UI, game-specific for world)
 * 
 * This is why Stage.draw() must be wrapped with explicit viewport.apply() calls:
 * Stage.draw() internally handles projection, but the viewport must be explicitly
 * applied BEFORE camera operations to ensure correct coordinate transformation.
 * 
 * ============================================================================
 * FULLSCREEN TRANSITION PROTECTION
 * ============================================================================
 * During fullscreen transitions:
 *   - Physical screen dimensions change suddenly (e.g., 1920x1080 → 3840x2160)
 *   - Gdx.graphics.getWidth/Height() return new physical dimensions
 *   - FitViewport automatically scales to fit new physical screen
 *   - Virtual coordinates remain unchanged (800x600)
 *   - If projection matrix is NOT updated, UI clips to old physical bounds
 * 
 * Solution: Call viewport.update(width, height, true) in resize() to:
 *   1. Update FitViewport's scaling calculations
 *   2. Re-center camera (centerCamera=true) to prevent drift
 *   3. Force camera matrix recalculation in next render()
 *
 * ============================================================================
 */
public class GameScreen extends ScreenAdapter {
    // Transition anchor placed in lower torso/feet region for stable RPG-style placement.
    private static final float PLAYER_TRANSITION_ANCHOR_Y_FACTOR = 0.22f;
    private static final String AREA_TITLE_FONT_PATH = "font/TheWildBreathOfZelda-15Lv.ttf";
    private static final int AREA_TITLE_FONT_SIZE = 75;
    private static final float AREA_TITLE_Y = 485f;
    private static final float AREA_TITLE_DURATION_SECONDS = 3f;
    private static final float AREA_TITLE_FADE_IN_SECONDS = 0f;
    private static final float AREA_TITLE_FADE_OUT_SECONDS = 2f;
    private static final float END_ROAD_FORCE_MOVE_DURATION_SECONDS = 1f;
    private static final float MANSION_PATH_BLOCK_FORCE_MOVE_DURATION_SECONDS = 1.5f;
    private static final float BOSS_DECLINE_FORCE_MOVE_DURATION_SECONDS = 1f;
    private static final float INTERACTION_RETRIGGER_BLOCK_SECONDS = 0.2f;
    private static final float SAVE_TOAST_DURATION_SECONDS = 2f;
    private static final int SAVE_TOAST_FONT_SIZE = 30;
    private static final float SAVE_TOAST_X = 20f;
    private static final float SAVE_TOAST_Y = 575f;
    private static final String SAVE_TOAST_MESSAGE_SAVED = "SAVED";
    private static final String SAVE_TOAST_MESSAGE_CHECKPOINT = "CHECKPOINT";
    public static final int DEFAULT_BATTLE_XP_REWARD = 25;
    private static final String EVENT_BOSS_0 = "boss_0_event";
    private static final String EVENT_BOSS_0_DEFEATED = "boss_0_defeated";
    private static final String EVENT_BOSS_1 = "boss_1_event";
    private static final String EVENT_BOSS_1_DEFEATED = "boss_1_defeated";
    /** After Boss 1, garden props + dialogue + walk-off sequence has finished. */
    private static final String EVENT_BOSS_1_GARDEN_CUTSCENE_DONE = "boss_1_garden_cutscene_done";
    private static final String EVENT_BOSS_2 = "boss_2_event";
    private static final String EVENT_BOSS_2_DEFEATED = "boss_2_defeated";
    private static final String ENCOUNTER_BERT_JR_ID = "boss_0_encounter";
    // Placeholder IDs for upcoming main bosses.
    private static final String ENCOUNTER_SPARTACUS_ID = "boss_1_encounter";
    private static final String ENCOUNTER_CERBERUS_ID = "boss_2_encounter";
    /** Placeholder combat tuning for Cerberus; replace when Boss 1 progression is locked. */
    private static final String ENCOUNTER_TABLE_CERBERUS_ID = "main_boss_cerberus";
    private static final String CERBERUS_PORTRAIT_PATH = "entities/clawkins/Clawkin_13_Cerberus.png";
    private static final int CERBERUS_PLACEHOLDER_LEVEL = 18;
    private static final int CERBERUS_PLACEHOLDER_HP = 420;
    private static final int CERBERUS_PLACEHOLDER_ATTACK = 48;
    private static final int CERBERUS_PLACEHOLDER_DEFENSE = 42;
    private static final int CERBERUS_PLACEHOLDER_SPEED = 32;
    private static final int CERBERUS_VICTORY_XP_PLACEHOLDER = 250;
    private static final String ENCOUNTER_TABLE_SPARTACUS_ID = "main_boss_spartacus";
    private static final String SPARTACUS_PORTRAIT_PATH = "entities/clawkins/Clawkin_08_Spartacus.png";
    private static final int SPARTACUS_PLACEHOLDER_LEVEL = 10;
    private static final int SPARTACUS_PLACEHOLDER_HP = 280;
    private static final int SPARTACUS_PLACEHOLDER_ATTACK = 42;
    private static final int SPARTACUS_PLACEHOLDER_DEFENSE = 38;
    private static final int SPARTACUS_PLACEHOLDER_SPEED = 28;
    /** Marginal XP for L10→L11 ({@link LevelSystem#getExpForNextLevel(int)} at 10). */
    private static final int SPARTACUS_VICTORY_XP_PLACEHOLDER = 140;
    /** Optional per-encounter XP; see {@link BattleOverlay} boss reward notes. */
    private static final Map<String, Integer> BOSS_XP_REWARDS_BY_ENCOUNTER_ID = Map.ofEntries(
            Map.entry(ENCOUNTER_BERT_JR_ID, 50),
            Map.entry(ENCOUNTER_SPARTACUS_ID, SPARTACUS_VICTORY_XP_PLACEHOLDER),
            Map.entry(ENCOUNTER_CERBERUS_ID, CERBERUS_VICTORY_XP_PLACEHOLDER)
    );
    private static final String TRIGGER_BOSS_BERT_JR_ID = "trigger_boss_0";
    /** Map {@code ObjectId} for Spartacus (Boss 1); set {@code DialogueDirectory} on the object (e.g. dialogue/spartacus_boss.json). */
    private static final String TRIGGER_BOSS_1_ID = "trigger_boss1";
    /** Matches {@code ObjectId} on cave_3.tmx Cerberus trigger. */
    private static final String TRIGGER_BOSS_2_ID = "trigger_boss_2";
    private static final String END_ROAD_OBJECT_ID = "end_road";
    /** {@code cave_3.tmx} trippable that rolls credits after Cerberus (see {@link #cerberusPostVictoryEndingWalkActive}). */
    private static final String END_TRIGGER_OBJECT_ID = "end_trigger";
    private static final float CERBERUS_POST_VICTORY_WALK_SPEED_FACTOR = 0.5f;
    /** Match {@link github.dluckycompany.clawkins.system.InteractionSystem} trippable overlap origin. */
    private static final float PLAYER_TRIPPABLE_ORIGIN_Y_FACTOR = 0.22f;
    private static final String MANSION_PATH_BLOCK_OBJECT_ID = "mansion_path_block";
    private static final String BED_COTTAGE_OBJECT_ID = "bed_cottage";
    private static final String PROP_BERT_JR_OBJECT_ID = "bertjr_prop";
    private static final String PROP_MANSION_SPARTACUS = "spartacus_prop";
    private static final String PROP_MANSION_DUKEKHAI = "dukekhai_prop";
    private static final String PROP_MANSION_SPARTACUS_1 = "spartacus_prop_1";
    private static final String PROP_MANSION_DUKEKHAI_1 = "dukekhai_prop_1";
    /** {@code cave_3.tmx} Cerberus tile prop; removed after {@link #EVENT_BOSS_2_DEFEATED}. */
    private static final String PROP_CERBERUS_OBJECT_ID = "cerberus_prop";
    /** Cave bridge atmosphere (trips {@code dialogue/cerberus_bridge.json}). */
    private static final String CERBERUS_ATMOS0_OBJECT_ID = "cerberus_enc_atmos0";
    private static final String CERBERUS_ATMOS1_OBJECT_ID = "cerberus_enc_atmos1";
    private static final float CERBERUS_ATMOS0_CAMERA_ZOOM = 1.34f;
    private static final float CERBERUS_ATMOS1_CAMERA_ZOOM = 1.62f;
    /** Camera follow easing vs default {@link CameraSystem} during Cerberus pre-dialogue (0.25 = quarter speed). */
    private static final float CERBERUS_BOSS_PREDIALOGUE_CAMERA_SMOOTH_MUL = 0.25f;
    private static final float CERBERUS_BOSS_PREDIALOGUE_CAMERA_FAIL_SAFE_SEC = 10f;
    /** Negative shifts framing left while the camera follows the Cerberus prop. */
    private static final float CERBERUS_BOSS_CAMERA_PROP_FRAMING_BIAS_X = -0.65f;
    private static final float CERBERUS_BOSS_POST_DIALOGUE_CAMERA_FAIL_SAFE_SEC = 8f;
    private static final String DIALOGUE_SPARTACUS_AFTER_BOSS = "dialogue/spartacus_Afterboss.json";
    private static final float GARDEN_BOSS1_CUTSCENE_OFF_CAMERA_MARGIN = 0.85f;
    /** After a prop passes the off-screen edge, keep moving this long before removal. */
    private static final float GARDEN_BOSS1_CUTSCENE_EXIT_EXTRA_SECONDS = 1f;
    private static final Set<String> SAVE_POINT_OBJECT_IDS = Set.of(
            BED_COTTAGE_OBJECT_ID
    );
    /** Story checkpoint volumes (see mansion_garden.tmx {@code checkpoint_1}, cave_3 {@code checkpoint_2}). */
    private static final Set<String> CHECKPOINT_OBJECT_IDS = Set.of(
            "checkpoint_1",
            "checkpoint_2"
    );
    /** Heal interactibles on nursery maps (see nurse_interior*.tmx ObjectId). */
    private static final List<String> NURSE_HEAL_OBJECT_IDS = List.of(
            "nurse_01", "nurse_02", "nurse_03", "nurse_04"
    );
    private static final float BERT_JR_PROP_INITIAL_X_OFFSET = 6f;
    private static final float BERT_JR_PROP_WALK_IN_SPEED = 1.2f;

    private final Main game;
    private final Engine engine;
    private final Batch batch;
    private final TiledService tiledService;
    private final TiledObjectConfigurator tiledObjectConfigurator;
    private final EncounterEventBus encounterEventBus;
    private final PlayerProgress playerProgress;
    private final BattleService battleService;
    private final RandomEncounterGenerator randomEncounterGenerator;
    private final BattleOverlay battleOverlay;
    private final InteractionSystem interactionSystem;
    private final DialogueOverlay dialogueOverlay;
    private final IntroExpositionOverlay introExpositionOverlay;
    private final AudioService audioService;
    private boolean explorationSystemsEnabled;
    private boolean wasBattleSessionPresent;
    private boolean wasBattlePlaying;
    private boolean introExpositionComplete = false;
    private boolean tutorialDialogueTriggered = false;
    
    // Inventory system - Virtual coordinate system (800x600)
    private final Stage inventoryStage;
    private final Stage hudStage;
    private final Stage cheatConsoleStage;
    private final HudWallet hudWallet;
    private final BitmapFont uiFont;
    private final BitmapFont areaTitleFont;
    private final BitmapFont saveToastFont;
    private final GlyphLayout areaTitleLayout;
    private final PlayerBattleState playerBattleState;
    
    // Merchant system
    private MerchantShopUI merchantShopUI;
    private boolean merchantShopVisible = false;
    
    // Game pause state - used when inventory or other overlays pause the game
    private boolean isPaused = false;
    
    // Full-screen inventory screen (separate from inventory overlay)
    private InventoryScreen inventoryScreen;
    
    // Team viewer screen for party member status
    private TeamViewerScreen teamViewerScreen;
    private boolean teamViewerVisible = false;
    private SummaryScreen summaryScreen;
    private boolean summaryVisible = false;
    private final MainSideMenuOverlay sideMenuOverlay;
    
    // Shape renderer for UI overlays (black background dimming)
    private final ShapeRenderer shapeRenderer;
    
    // Track if initial setup has been done (to prevent re-initialization on return from overlay)
    private boolean hasBeenInitialized = false;
    
    // Map transition state (deferred to avoid modifying engine during update)
    private final MapTransitionSystem mapTransitionSystem;
    private String pendingTransitionMap;
    private String pendingTransitionId;
    private final BattleTransition mapTransitionFade;
    private String activeTransitionMap;
    private String activeTransitionId;
    private boolean mapTransitionSwapDone;
    private String activeAreaTitle;
    private float areaTitleTimer;
    private MapAsset pendingAreaTitleAsset;
    private String lastAreaNameForSfx;
    private String lastAreaDisplayKey;

    private SaveState pendingSaveState;
    private ForcedMoveState activeForcedMove;
    private BossFightPromptState activeBossFightPrompt;
    private SaveGamePromptState activeSaveGamePrompt;
    private SaveActionPromptState activeSaveActionPrompt;
    private NurseStationPromptState activeNurseStationPrompt;
    private FallenPromptState activeFallenPrompt;
    private boolean queuedFallenScreenAfterBattle;
    private boolean defeatSessionEffectsApplied;
    private float saveToastTimer;
    private String saveToastMessage = SAVE_TOAST_MESSAGE_SAVED;
    private float interactionRetriggerBlockSeconds;
    private Entity bertJrPropEntity;
    private Vector2 bertJrPropTargetPosition;
    private boolean bertJrPropWalkingIn;
    private boolean bertJrPropReachedTarget;
    private boolean bertJrPreDialogueSequenceActive;
    private boolean bertJrFirstEncounterMusicStarted;
    /** After {@code cerberus_enc_atmos0}: music + first zoom tier until Cerberus is defeated or map reloads. */
    private boolean cerberusBridgePresentationActive;
    /** After {@code cerberus_enc_atmos1}: additional zoom until Cerberus is defeated or map reloads. */
    private boolean cerberusBridgeAtmos1Zoom;
    /** Bridge tension (slow walk + dia music) until Cerberus is defeated or map reloads. */
    private boolean cerberusBridgeConveyorUntilBossDefeated;
    /** Set when Cerberus is beaten; consumed when the battle session fully ends. */
    private boolean pendingCerberusPostVictoryEndingWalk;
    /**
     * Automated walk toward {@link #END_TRIGGER_OBJECT_ID} at {@link #CERBERUS_POST_VICTORY_WALK_SPEED_FACTOR}× speed;
     * player input disabled, trippables still run.
     */
    private boolean cerberusPostVictoryEndingWalkActive;
    /**
     * Cerberus boss (Bert-Jr pattern): pre-dialogue eases camera onto the prop and returns {@code false} until
     * centered, then {@link InteractionSystem#triggerInteractionByObjectId} re-enters for map dialogue → post-dialogue
     * special (fight prompt).
     */
    private boolean cerberusBossPredialogueCameraActive;
    private float cerberusBossPredialogueFailSafeRemaining;
    /** When true, pre-dialogue allows the normal {@code DialogueDirectory} flow. Cleared when post-dialogue runs. */
    private boolean cerberusBossCameraLinedUpForDialogue;
    /** After map dialogue: ease camera back to the player, then show the fight prompt (no snap). */
    private boolean cerberusBossPostDialogueCameraReturnPending;
    private float cerberusBossPostDialogueReturnFailSafeRemaining;
    private boolean cerberusBossDeferredPromptDeclineIsFirst;

    private enum MansionGardenBoss1CutscenePhase {
        INACTIVE,
        SPARTACUS_RUN,
        DIALOGUE,
        DUKE_WALK
    }

    private MansionGardenBoss1CutscenePhase mansionGardenBoss1CutscenePhase = MansionGardenBoss1CutscenePhase.INACTIVE;
    private boolean pendingMansionGardenBoss1Cutscene;
    private Entity mansionGardenCutsceneSpartacus1Entity;
    private Entity mansionGardenCutsceneDuke1Entity;
    private float mansionGardenBoss1CutscenePastExitElapsed;
    private final Map<String, BossMusicHooks> bossMusicHooksByEncounterId = new HashMap<>();
    private ActiveBossMusicState activeBossMusicState;
    
    // Player profile from character setup (name and gender)
    private PlayerProfile playerProfile;
    
    // Cheat console system for developer debugging
    private github.dluckycompany.clawkins.debug.CheatCodeManager cheatCodeManager;
    private github.dluckycompany.clawkins.debug.CheatConsoleOverlay cheatConsoleOverlay;
    private boolean debugWorldOverlayEnabled;
    
    // Virtual UI resolution (constant, independent of physical screen)
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;

    public GameScreen(Main game, PlayerProfile playerProfile) {
        this.game = game;
        this.playerProfile = playerProfile;
        AssetService assetService = game.getAssetService();
        Viewport viewport = game.getViewport();
        OrthographicCamera camera = game.getCamera();
        this.batch = game.getBatch();
        this.audioService = game.getAudioService();

        // Create and configure the Ashley ECS Engine
        this.engine = new Engine();

        // Register systems in processing order (Ashley: lower priority runs first):
        // 0 – PlayerInputSystem        (WASD → Move + animation)
        // 5 – CerberusBridgeWalkSlowSystem (caps player maxSpeed on cave bridge)
        // 0 – InteractionSystem, AnimationSystem, EnemySystem (same tier as input)
        // 10 – MoveSystem              (must run after input + Cerberus speed cap)
        // … CameraSystem, RenderSystem
        this.engine.addSystem(new PlayerInputSystem());
        this.engine.addSystem(new CerberusBridgeWalkSlowSystem(this::isCerberusConveyorWalkActive, () -> cerberusBridgeAtmos1Zoom));
        this.interactionSystem = new InteractionSystem();
        this.engine.addSystem(interactionSystem);
        this.engine.addSystem(new AnimationSystem());
        this.engine.addSystem(new EnemySystem(audioService));
        this.engine.addSystem(new MoveSystem());
        this.engine.addSystem(new EnemyTrainerSpriteSystem());
        this.engine.addSystem(new CameraSystem(camera));
        this.engine.addSystem(new RenderSystem(batch, viewport, camera));

        this.mapTransitionSystem = new MapTransitionSystem();
        this.mapTransitionSystem.setTransitionCallback((targetMap, targetTransitionId) -> {
            this.pendingTransitionMap = targetMap;
            this.pendingTransitionId = targetTransitionId;
        });
        this.engine.addSystem(mapTransitionSystem);

        this.encounterEventBus = new EncounterEventBus();
        this.playerProgress = new PlayerProgress();
        this.playerBattleState = new PlayerBattleState();
        this.interactionSystem.setClawkinPartySupplier(playerBattleState::getParty);
        this.battleService = new BattleService(this.encounterEventBus, playerBattleState);
        this.randomEncounterGenerator = new RandomEncounterGenerator();
        this.engine.addSystem(new EncounterDetectionSystem(this.encounterEventBus));
        DialogueBoxRenderer dialogueBoxRenderer = new DialogueBoxRenderer();
        this.battleOverlay = new BattleOverlay(game, dialogueBoxRenderer);
        this.battleOverlay.init(assetService, battleService, playerBattleState);
        this.mapTransitionFade = new BattleTransition();
        this.mapTransitionSwapDone = false;
        this.dialogueOverlay = new DialogueOverlay(dialogueBoxRenderer, true);
        this.introExpositionOverlay = new IntroExpositionOverlay();
        this.explorationSystemsEnabled = true;
        this.wasBattleSessionPresent = false;
        this.wasBattlePlaying = false;

        // Inventory system initialization - Fixed virtual UI resolution
        this.inventoryStage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.hudStage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.cheatConsoleStage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.shapeRenderer = new ShapeRenderer();
        this.uiFont = new BitmapFont();
        this.areaTitleFont = createAreaTitleFont();
        this.saveToastFont = createSaveToastFont();
        this.areaTitleLayout = new GlyphLayout();
        this.activeAreaTitle = null;
        this.areaTitleTimer = 0f;
        this.pendingAreaTitleAsset = null;
        this.saveToastTimer = 0f;
        this.lastAreaNameForSfx = null;
        this.lastAreaDisplayKey = null;
        this.sideMenuOverlay = new MainSideMenuOverlay(inventoryStage, battleOverlay.getSkin(), uiFont, audioService);
        this.hudWallet = new HudWallet(playerBattleState.getWallet(), uiFont);
        this.hudWallet.setPosition(10, 10);
        this.hudWallet.pack(); // Size the label to fit its content
        
        // Create root table for HUD wallet with fixed virtual coordinates
        Table hudRoot = new Table();
        hudRoot.setFillParent(true);
        hudRoot.top().left();
        hudRoot.add(hudWallet);
        this.hudStage.addActor(hudRoot);
        this.bertJrPropTargetPosition = new Vector2();
        this.bertJrPropWalkingIn = false;
        this.bertJrPropReachedTarget = false;
        this.bertJrPreDialogueSequenceActive = false;
        this.bertJrFirstEncounterMusicStarted = false;

        // Tiled map services
        this.tiledService = new TiledService(assetService);
        this.tiledObjectConfigurator = new TiledObjectConfigurator(engine, assetService, playerBattleState);
        this.engine.addSystem(new RandomEncounterSystem(
                this.tiledService,
                this.encounterEventBus,
                this.randomEncounterGenerator,
                this.battleService));
        
        // Set player name from character setup if available
        if (playerProfile != null) {
            tiledObjectConfigurator.setPlayerNameOverride(playerProfile.getName());
        }

        // Wire up callbacks:
        // - When a map object is found → TiledObjectConfigurator creates an entity
        // - When the map changes → hand it to RenderSystem and CameraSystem
        this.tiledService.setLoadObjectConsumer(tiledObjectConfigurator::onLoadObject);

        Consumer<TiledMap> renderConsumer = engine.getSystem(RenderSystem.class)::setMap;
        Consumer<TiledMap> cameraConsumer = engine.getSystem(CameraSystem.class)::setMap;
        Consumer<TiledMap> moveConsumer = engine.getSystem(MoveSystem.class)::setMap;
        Consumer<TiledMap> enemyConsumer = engine.getSystem(EnemySystem.class)::setMap;
        Consumer<TiledMap> transitionConsumer = mapTransitionSystem::setMap;
        Consumer<TiledMap> audioConsumer = map -> {
            audioService.setMap(map);
            audioService.onEvent(AudioEventType.MAP_CHANGED);
        };
        Consumer<TiledMap> randomEncounterReset = m -> engine.getSystem(RandomEncounterSystem.class).resetTravelLedger();
        this.tiledService.setMapChangeConsumer(
                renderConsumer
                        .andThen(cameraConsumer)
                        .andThen(moveConsumer)
                        .andThen(enemyConsumer)
                        .andThen(transitionConsumer)
                        .andThen(audioConsumer)
                        .andThen(randomEncounterReset));
        
        // Initialize cheat console system
        this.cheatCodeManager = new github.dluckycompany.clawkins.debug.CheatCodeManager(playerBattleState);
        this.cheatConsoleOverlay = new github.dluckycompany.clawkins.debug.CheatConsoleOverlay(cheatConsoleStage, cheatCodeManager, uiFont, battleOverlay.getSkin());
        this.debugWorldOverlayEnabled = false;
        
        // Set up callback to update HUD when money changes via cheats
        this.cheatCodeManager.setOnMoneyChanged(() -> {
            hudWallet.updateDisplay();
            Gdx.app.log("GameScreen", "HUD wallet updated after cheat: " + playerBattleState.getWallet().getMoney());
        });
        
        // Set up callback to handle teleport requests from cheats
        this.cheatCodeManager.setOnTeleportRequested(() -> {
            Gdx.app.log("GameScreen", "Teleport requested via cheat");
            // The actual teleport will be processed in the render loop
        });

        // Give CheatCodeManager access to PlayerProgress for level cheats
        this.cheatCodeManager.setPlayerProgress(playerProgress);

        // When a level cheat fires, re-sync everything through the proper path
        this.cheatCodeManager.setOnSharedLevelChanged(() -> {
            applySharedLevelSet(LevelSystem.calculateLevelFromExp(playerProgress.getExperiencePoints()));
        });
        
        // Register the "end" cheat to trigger the ending credits screen
        this.cheatCodeManager.registerCheat("end", () -> {
            Gdx.app.log("CheatCodeManager", "Triggering ending credits via cheat 'end'");
            // Defer to next frame so the cheat console can close cleanly first
            Gdx.app.postRunnable(this::triggerEndingCredits);
            return github.dluckycompany.clawkins.debug.CheatCodeManager.CheatResult.success("Rolling credits...");
        });
    }

    @Override
    public void show() {
        // When returning from screens like inventory, restore the normal input processor
        if (hasBeenInitialized) {
            // Check if we're in a battle - if so, don't reset input processor
            // The battle HUD has already restored its own input processor
            boolean inBattle = battleOverlay != null && battleOverlay.isInBattle();
            
            if (!inBattle) {
                Gdx.input.setInputProcessor(null);  // Restore normal world input
            }
            
            isPaused = false;  // Make sure we're not paused
            teamViewerVisible = false;  // Clear team viewer state
            summaryVisible = false;
            inventoryStage.clear();  // Clear any lingering UI
            sideMenuOverlay.restoreSidebarAfterExternalScreenReturn();
            isPaused = shouldPauseForUi();
            ensurePlayerEntityPresentAfterReturn();
            return;
        }
        hasBeenInitialized = true;

        // Check if CharacterSetupScreen has a profile and set it
        github.dluckycompany.clawkins.ui.CharacterSetupScreen characterSetupScreen = 
            game.getScreen(github.dluckycompany.clawkins.ui.CharacterSetupScreen.class);
        if (characterSetupScreen != null) {
            github.dluckycompany.clawkins.model.PlayerProfile profile = characterSetupScreen.getPlayerProfile();
            if (profile != null) {
                setPlayerProfile(profile);
                Gdx.app.log("GameScreen", "Player profile loaded: " + profile.getName() + " (" + profile.getGender() + ")");
            }
        }

        boolean loadedFromSave = false;
        if (pendingSaveState != null) {
            loadedFromSave = applySaveState(pendingSaveState);
            pendingSaveState = null;
        }

        // Initialize inventory with default items
        if (!loadedFromSave && playerBattleState.getInventory().getAllItems().isEmpty()) {
            playerBattleState.getInventory().addItem(ItemFactory.BASIC_POTION, 3);
            playerBattleState.getInventory().addItem(ItemFactory.FULL_HEAL, 1);
            playerBattleState.getInventory().addItem(ItemFactory.REVIVE, 2);
            playerBattleState.getInventory().addItem(ItemFactory.ATTACK_BOOST, 1);
            playerBattleState.getInventory().addItem(ItemFactory.DEFENSE_BOOST, 1);
        }
        
        // New games start with zero money by default.
        refreshProgressSnapshots();
        
        // Create the merchant shop UI (matches InventoryUI structure)
        MerchantInventory defaultMerchantInventory = MerchantInventory.createDefaultInventory();
        this.merchantShopUI = new MerchantShopUI(
            inventoryStage,
            new BitmapFont(),
            playerBattleState.getInventory(),
            defaultMerchantInventory,
            playerBattleState.getParty(),
            battleOverlay.getSkin(),
            playerBattleState.getWallet(),
            audioService,
            "Merchant",  // Default merchant name
            false  // Not in battle context
        );
        merchantShopUI.setOnBackPressed(() -> {
            closeMerchantShop();
            hudWallet.updateDisplay();
        });
        
        registerBossMusicHooks();
        registerSpecialInteractions();
        
        // Log player profile information
        if (playerProfile != null) {
            Gdx.app.log("GameScreen", "Initialized with player: " + 
                playerProfile.getName() + " (" + playerProfile.getGender().getDisplayName() + ")");
        } else {
            Gdx.app.log("GameScreen", "Initialized without player profile (loading from save or initial screen)");
        }
        
        // Create the full-screen inventory screen and cache it
        this.inventoryScreen = new InventoryScreen(game, playerBattleState.getInventory(), this);
        game.addScreen(inventoryScreen);
        
        if (!loadedFromSave) {
            // Load the map and hand it to TiledService.
            // This triggers: object parsing → entity spawning → map change notification to systems.
            TiledMap startMap = this.tiledService.loadMap(MapAsset.COTTAGE_SAMPLE);
            this.tiledService.setMap(startMap);
            refreshBertJrPropStateForCurrentMap();
            refreshCerberusPropForCurrentMap();
            refreshCerberusAtmos0AfterMapLoad();
            refreshMansionGardenBossPropsState(true);
            this.lastAreaNameForSfx = resolveAreaName(MapAsset.COTTAGE_SAMPLE);
            this.lastAreaDisplayKey = buildAreaDisplayKey(MapAsset.COTTAGE_SAMPLE);
            // Prevent frame-1 transition triggers from moving the player off the authored spawn.
            mapTransitionSystem.setCooldown(0f);

            // Center camera immediately on the spawned player so first frame matches Tiled placement.
            Entity startupPlayer = findPlayerEntity();
            if (startupPlayer != null) {
                centerCameraOnPlayer(startupPlayer);
            }
            audioService.setMap(startMap);
            audioService.onEvent(AudioEventType.MAP_CHANGED);
            
            // Start intro exposition sequence for new games
            startIntroExposition();
        }
    }

    public void queueSaveStateLoad(SaveState saveState) {
        if (hasBeenInitialized) {
            applySaveState(saveState);
            return;
        }
        this.pendingSaveState = saveState;
    }

    private void registerSpecialInteractions() {
        // Register object-id based handlers here.
        // Flow: dialogue (if any) always finishes first, then this handler executes.
        // Pre-dialogue checks run before dialogue/special handlers and may block interaction.
        interactionSystem.registerPreDialogueCheck(TRIGGER_BOSS_BERT_JR_ID, context -> {
            if (context.interactionCount() == 1 && !bertJrPropReachedTarget) {
                startBertJrPreDialogueSequence(context.playerEntity());
                return false;
            }
            if (context.interactionCount() == 1 && !bertJrFirstEncounterMusicStarted) {
                audioService.playMusic(MusicTrack.BOSS_BERTJR_DIA_FIRST_ENCOUNTER, false);
                bertJrFirstEncounterMusicStarted = true;
            }
            return !playerProgress.isEventAccomplished(EVENT_BOSS_0_DEFEATED);
        });
        interactionSystem.registerSpecialInteraction(END_ROAD_OBJECT_ID, context -> {
            startEndRoadForcedMove(context.playerEntity());
        });
        interactionSystem.registerSpecialInteraction(MANSION_PATH_BLOCK_OBJECT_ID, context -> {
            startMansionPathBlockForcedMove(context.playerEntity());
        });
        interactionSystem.registerSkipDialogueForObjectId(END_TRIGGER_OBJECT_ID);
        interactionSystem.registerSpecialInteraction(END_TRIGGER_OBJECT_ID, context -> {
            if (!cerberusPostVictoryEndingWalkActive) {
                return;
            }
            cerberusPostVictoryEndingWalkActive = false;
            Entity playerEntity = context.playerEntity();
            if (playerEntity != null) {
                Move move = Move.MAPPER.get(playerEntity);
                if (move != null) {
                    move.getDirection().setZero();
                    move.setMaxSpeed(move.getBaseSpeed());
                }
                PlayerAnimation anim = PlayerAnimation.MAPPER.get(playerEntity);
                if (anim != null) {
                    anim.setMoving(false);
                }
            }
            triggerEndingCredits();
        });
        registerSavePointInteractions();
        registerCheckpointInteractions();
        for (String nurseHealObjectId : NURSE_HEAL_OBJECT_IDS) {
            interactionSystem.registerSpecialInteraction(nurseHealObjectId, context -> openNurseStationMenu());
        }
        
        // Register merchant shop interactions (shop_01 and shop_02)
        // Both shops use the same dialogue from merchants.json
        // After dialogue completes, the merchant shop UI opens
        interactionSystem.registerSpecialInteraction("shop_01", context -> {
            openMerchantShop();
        });
        interactionSystem.registerSpecialInteraction("shop_02", context -> {
            openMerchantShop();
        });
        
        interactionSystem.registerSpecialInteraction(TRIGGER_BOSS_BERT_JR_ID, context -> {
            if (battleService.hasBattleSession() || playerProgress.isEventAccomplished(EVENT_BOSS_0_DEFEATED)) {
                return;
            }
            playerProgress.incrementAttempts(EVENT_BOSS_0);

            promptBossFightChoice(
                    "Bert Jr., The House Bandit",
                    () -> {
                        playerProgress.incrementAccepted(EVENT_BOSS_0);
                        runBossPreBattleMusicHook(ENCOUNTER_BERT_JR_ID);
                        encounterEventBus.publish(new EncounterEvent(
                                EncounterEventType.START_ENCOUNTER,
                                ENCOUNTER_BERT_JR_ID,
                                "tutorial_boss_bert_jr",
                                5,
                                150,
                                25,
                                35,
                                35,
                                createBertJrTutorialSkills(),
                                "Bert Jr., The House Bandit",
                                "entities/clawkins/Clawkin_04_Bert_Jr.png"
                        ));
                    },
                    () -> {
                        applyBertJrEncounterDeclineOutcome(
                                context.playerEntity(),
                                context.interactionCount() == 1
                        );
                    }
            );
        });

        interactionSystem.registerPreDialogueCheck(TRIGGER_BOSS_1_ID, context ->
                !playerProgress.isEventAccomplished(EVENT_BOSS_1_DEFEATED));
        interactionSystem.registerSpecialInteraction(TRIGGER_BOSS_1_ID, context -> {
            if (battleService.hasBattleSession() || playerProgress.isEventAccomplished(EVENT_BOSS_1_DEFEATED)) {
                return;
            }
            playerProgress.incrementAttempts(EVENT_BOSS_1);
            promptBossFightChoice(
                    "Spartacus",
                    () -> {
                        playerProgress.incrementAccepted(EVENT_BOSS_1);
                        runBossPreBattleMusicHook(ENCOUNTER_SPARTACUS_ID);
                        encounterEventBus.publish(new EncounterEvent(
                                EncounterEventType.START_ENCOUNTER,
                                ENCOUNTER_SPARTACUS_ID,
                                ENCOUNTER_TABLE_SPARTACUS_ID,
                                SPARTACUS_PLACEHOLDER_LEVEL,
                                SPARTACUS_PLACEHOLDER_HP,
                                SPARTACUS_PLACEHOLDER_ATTACK,
                                SPARTACUS_PLACEHOLDER_DEFENSE,
                                SPARTACUS_PLACEHOLDER_SPEED,
                                createSpartacusBossSkills(),
                                "Spartacus",
                                SPARTACUS_PORTRAIT_PATH
                        ));
                    },
                    () -> applySpartacusEncounterDeclineOutcome(
                            context.playerEntity(),
                            context.interactionCount() == 1
                    )
            );
        });

        interactionSystem.registerPreDialogueCheck(TRIGGER_BOSS_2_ID, this::cerberusBossPreDialogueCheck);
        interactionSystem.registerSpecialInteraction(TRIGGER_BOSS_2_ID, context -> {
            cerberusBossCameraLinedUpForDialogue = false;
            if (battleService.hasBattleSession() || playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
                return;
            }
            beginCerberusBossPostDialogueSmoothCameraReturnForFightPrompt(context.interactionCount() == 1);
        });

        interactionSystem.registerPreDialogueCheck(CERBERUS_ATMOS0_OBJECT_ID, context -> {
            beginCerberusEncAtmos0Ambience();
            return true;
        });
        interactionSystem.registerPreDialogueCheck(CERBERUS_ATMOS1_OBJECT_ID, context -> {
            beginCerberusEncAtmos1Presentation();
            return true;
        });
    }

    private void registerBossMusicHooks() {
        // Keep Bert Jr. on default battle music flow.
        bossMusicHooksByEncounterId.remove(ENCOUNTER_BERT_JR_ID);
        bossMusicHooksByEncounterId.put(
                ENCOUNTER_SPARTACUS_ID,
                new BossMusicHooks(
                        ENCOUNTER_SPARTACUS_ID,
                        null,
                        MusicTrack.BOSS_SPARTACUS,
                        List.of(),
                        null,
                        null));
        bossMusicHooksByEncounterId.put(
                ENCOUNTER_CERBERUS_ID,
                new BossMusicHooks(
                        ENCOUNTER_CERBERUS_ID,
                        null,
                        MusicTrack.BOSS_CERBERUS,
                        List.of(),
                        null,
                        null));
    }

    private void promptBossFightChoice(String enemyName, Runnable onYes, Runnable onNo) {
        if (enemyName == null || enemyName.isBlank()) {
            enemyName = "Boss";
        }
        activeBossFightPrompt = new BossFightPromptState(enemyName, onYes, onNo);
    }

    private void promptSaveGameChoice(Runnable onYes, Runnable onNo) {
        activeSaveGamePrompt = new SaveGamePromptState(onYes, onNo);
    }

    private void registerSavePointInteractions() {
        for (String objectId : SAVE_POINT_OBJECT_IDS) {
            interactionSystem.registerSpecialInteraction(objectId, context -> openSavePromptFromInteractible());
        }
    }

    private void registerCheckpointInteractions() {
        for (String objectId : CHECKPOINT_OBJECT_IDS) {
            interactionSystem.registerSkipDialogueForObjectId(objectId);
            interactionSystem.registerSpecialInteraction(objectId, context -> writeStoryCheckpointFromCurrentGameState());
        }
    }

    private void writeStoryCheckpointFromCurrentGameState() {
        SaveStateManager saveStateManager = game.getSaveStateManager();
        if (saveStateManager == null) {
            return;
        }
        SaveState state = buildSaveState();
        if (saveStateManager.writeCheckpointState(state)) {
            showSaveToast(SAVE_TOAST_MESSAGE_CHECKPOINT);
        }
    }

    private void openSavePromptFromInteractible() {
        if (isBossFightPromptVisible() || isSaveGamePromptVisible() || isSaveActionPromptVisible()
                || isFallenPromptVisible() || isNurseStationPromptVisible()) {
            return;
        }
        promptSaveGameChoice(this::openSaveActionPrompt, () -> {});
    }

    /** Opens after nursery greeting dialogue: Save / Heal / Exit. */
    private void openNurseStationMenu() {
        if (isBossFightPromptVisible() || isSaveGamePromptVisible() || isSaveActionPromptVisible()
                || isFallenPromptVisible() || isNurseStationPromptVisible()) {
            return;
        }
        activeNurseStationPrompt = new NurseStationPromptState();
    }

    /**
     * Full heal for all party members from the nursery station menu.
     * Plays the same heal sting as battle / item use when anyone actually gains HP.
     */
    private void applyNurseStationHeal() {
        int healedCount = 0;
        for (Clawkin clawkin : playerBattleState.getParty()) {
            if (clawkin == null) {
                continue;
            }
            int beforeHp = clawkin.getCurrentHp();
            int maxHp = clawkin.getMaxHp();
            if (beforeHp < maxHp) {
                clawkin.setCurrentHp(maxHp);
                healedCount++;
            }
        }
        if (healedCount > 0) {
            audioService.playSound(SoundEffect.BATTLE_HEAL);
            refreshProgressSnapshots();
        }
    }

    private void openSaveActionPrompt() {
        if (isBossFightPromptVisible() || isSaveActionPromptVisible() || isFallenPromptVisible()
                || isNurseStationPromptVisible()) {
            return;
        }
        activeSaveActionPrompt = new SaveActionPromptState(game.getSaveStateManager().hasSaveStates());
    }

    private static List<BattleSkill> createBertJrTutorialSkills() {
        return List.of(
                new BattleSkill(
                        "Telegraphed Chomp",
                        BattleSkill.EffectType.DAMAGE,
                        15,
                        "attack[self]",
                        0,
                        1
                ),
                new BattleSkill(
                        "Tail Whip",
                        BattleSkill.EffectType.DAMAGE,
                        10,
                        "attack[self]",
                        0,
                        0
                ),
                new BattleSkill(
                        "Junk Toss",
                        BattleSkill.EffectType.DAMAGE,
                        8,
                        "attack[self]",
                        0,
                        0
                )
        );
    }

    /** Placeholder kit; replace with authored Spartacus skills. */
    private static List<BattleSkill> createSpartacusBossSkills() {
        return List.of(
                new BattleSkill(
                        "Gladius Rush",
                        BattleSkill.EffectType.DAMAGE,
                        24,
                        "attack[self]",
                        0,
                        1
                ),
                new BattleSkill(
                        "Arena Roar",
                        BattleSkill.EffectType.DAMAGE,
                        16,
                        "attack[self]",
                        0,
                        0
                ),
                new BattleSkill(
                        "Lion's Chain",
                        BattleSkill.EffectType.DAMAGE,
                        18,
                        "attack[self]",
                        0,
                        0
                )
        );
    }

    /** Placeholder kit; replace with authored Cerberus skills. */
    private static List<BattleSkill> createCerberusBossSkills() {
        return List.of(
                new BattleSkill(
                        "Triple Bite",
                        BattleSkill.EffectType.DAMAGE,
                        22,
                        "attack[self]",
                        0,
                        1
                ),
                new BattleSkill(
                        "Infernal Howl",
                        BattleSkill.EffectType.DAMAGE,
                        14,
                        "attack[self]",
                        0,
                        0
                ),
                new BattleSkill(
                        "Chain Lash",
                        BattleSkill.EffectType.DAMAGE,
                        16,
                        "attack[self]",
                        0,
                        0
                )
        );
    }

    private void ensurePlayerEntityPresentAfterReturn() {
        Entity existingPlayer = findPlayerEntity();
        if (existingPlayer != null) {
            centerCameraOnPlayer(existingPlayer);
            return;
        }

        Gdx.app.log("GameScreen", "Player entity missing after returning to GameScreen. Rebuilding map entities.");

        engine.removeAllEntities();

        TiledMap currentMap = tiledService.getCurrentMap();
        if (currentMap == null) {
            currentMap = tiledService.loadMap(MapAsset.COTTAGE_SAMPLE);
        }

        tiledService.setMap(currentMap);
        refreshBertJrPropStateForCurrentMap();
        refreshCerberusPropForCurrentMap();
        refreshCerberusAtmos0AfterMapLoad();
        refreshMansionGardenBossPropsState(true);
        mapTransitionSystem.setCooldown(0.2f);

        Entity restoredPlayer = findPlayerEntity();
        if (restoredPlayer == null) {
            restoredPlayer = tiledObjectConfigurator.spawnPlayerWithoutMapObject(currentMap);
        }
        if (restoredPlayer != null) {
            centerCameraOnPlayer(restoredPlayer);
        } else {
            Gdx.app.error("GameScreen", "Failed to restore player entity after map rebuild.");
        }
    }

    private void centerCameraOnPlayer(Entity playerEntity) {
        Transform t = Transform.MAPPER.get(playerEntity);
        if (t == null) {
            return;
        }

        CameraSystem cameraSystem = engine.getSystem(CameraSystem.class);
        if (cameraSystem != null) {
            cameraSystem.snapTo(t);
            game.getCamera().update();
            return;
        }

        OrthographicCamera camera = game.getCamera();
        Vector2 pos = t.getPosition();
        camera.position.set(pos.x, pos.y + 1f, camera.position.z);
        camera.update();
    }

    @Override
    public void render(float delta) {
        float uiDelta = Math.min(1 / 30f, delta);
        
        // Update intro exposition overlay FIRST with original delta (before pausing)
        if (introExpositionOverlay.isActive()) {
            introExpositionOverlay.update(Math.min(1 / 30f, delta));
        }

        // Keep pause state derived from live UI state so movement reliably resumes
        // after exiting CLAWKINS/SETTINGS and closing the sidebar.
        isPaused = shouldPauseForUi();

        // Handle F12 cheat console toggle (before other input processing)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            cheatConsoleOverlay.toggle();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            debugWorldOverlayEnabled = !debugWorldOverlayEnabled;
            RenderSystem renderSystem = engine.getSystem(RenderSystem.class);
            if (renderSystem != null) {
                renderSystem.setDebugRenderingEnabled(debugWorldOverlayEnabled);
            }
            Gdx.app.log("GameScreen", "World debug overlay: " + (debugWorldOverlayEnabled ? "ON" : "OFF"));
        }
        
        // Handle cheat console input when open
        boolean cheatConsoleHandledInput = cheatConsoleOverlay.handleInput();
        
        // Update cheat console
        cheatConsoleOverlay.update(uiDelta);
        
        // Update player position for whereami cheat
        Entity playerEntity = findPlayerEntity();
        if (playerEntity != null) {
            Transform playerTransform = Transform.MAPPER.get(playerEntity);
            if (playerTransform != null) {
                Vector2 pos = playerTransform.getPosition();
                cheatCodeManager.updatePlayerPosition(resolveCurrentMapKey(), pos.x, pos.y);
            }
        }
        
        // Handle pending teleport from cheat console
        String pendingTeleportMapKey = cheatCodeManager.getPendingTeleportMapKey();
        if (pendingTeleportMapKey != null && !mapTransitionFade.isTransitioning()) {
            processTeleportCheat(pendingTeleportMapKey);
            cheatCodeManager.clearPendingTeleport();
        }

        // Don't update game state when paused (inventory or other overlays using isPaused flag)
        if (isPaused || cheatConsoleOverlay.isVisible() || introExpositionOverlay.isActive()) {
            delta = 0f; // Skip time advancement
        }
        
        // Cap delta to avoid spiral-of-death on lag spikes
        delta = Math.min(1 / 30f, delta);
        updateBossFightPromptInput();
        updateSaveGamePromptInput();
        updateSaveActionPromptInput();
        updateNurseStationPromptInput();
        updateFallenPromptInput();
        saveToastTimer = Math.max(0f, saveToastTimer - delta);
        interactionRetriggerBlockSeconds = Math.max(0f, interactionRetriggerBlockSeconds - delta);

        // Handle side-menu and submenu navigation while in exploration.
        if (!battleService.hasBattleSession() && !interactionSystem.isDialogueVisible() && !merchantShopVisible
                && !mapTransitionFade.isTransitioning() && !isSpecialMovementActive() && !cerberusPostVictoryEndingWalkActive
                && !isBossFightPromptVisible() && !isSaveGamePromptVisible() && !isSaveActionPromptVisible()
                && !isNurseStationPromptVisible()
                && !isFallenPromptVisible()
                && !isMansionGardenBoss1CutsceneActive()
                && !isBertJrPreDialogueSequenceActive()
                && !teamViewerVisible && !summaryVisible
                && !cheatConsoleOverlay.isVisible()
                && !introExpositionOverlay.isActive()) {
            MainSideMenuOverlay.Action menuAction = sideMenuOverlay.handleInput();
            switch (menuAction) {
                case OPEN_CLAWKINS -> openTeamViewerSubmenu();
                case OPEN_INVENTORY -> openInventoryScreen();
                case OPEN_SAVE_STATE -> openSaveStateScreen();
                case OPEN_LOAD_STATE -> openLoadStateScreen();
                case EXIT_GAME -> returnToMainMenu();
                case RETURN_TO_SIDEBAR -> returnToSidebarFromSubmenu();
                case NONE -> {
                }
            }
        }

        // Temporary Summary entry path without modifying TeamViewerScreen.
        // While TeamViewer is open, press Y to open the standalone Summary screen.
        if (teamViewerVisible && !summaryVisible && teamViewerScreen != null && Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            openSummaryFromTeamViewer();
        }

        // CRITICAL: Close inventory and team viewer immediately when a battle starts
        // This ensures the UI is hidden before any battle rendering occurs
        boolean hasBattle = battleService.hasBattleSession();
        if (hasBattle && !wasBattleSessionPresent) {
            closeAllMenuUi();
        }

        if (pendingTransitionMap != null && !mapTransitionFade.isTransitioning()) {
            beginMapTransition(pendingTransitionMap, pendingTransitionId);
            pendingTransitionMap = null;
            pendingTransitionId = null;
        }

        if (mapTransitionFade.isTransitioning()) {
            mapTransitionFade.update(delta);
            if (!mapTransitionSwapDone && mapTransitionFade.isHudReadyToShow()) {
                performMapTransition(activeTransitionMap, activeTransitionId);
                mapTransitionSwapDone = true;
            }
            if (mapTransitionSwapDone) {
                Entity transitionPlayer = findPlayerEntity();
                if (transitionPlayer != null) {
                    centerCameraOnPlayer(transitionPlayer);
                }
            }
        } else if (mapTransitionSwapDone) {
            if (pendingAreaTitleAsset != null) {
                showAreaTitle(pendingAreaTitleAsset);
                pendingAreaTitleAsset = null;
            }
            mapTransitionSwapDone = false;
            activeTransitionMap = null;
            activeTransitionId = null;
        }

        battleOverlay.update(battleService, delta);
        battleService.update(delta);
        updateForcedMove(delta);
        updateBertJrPropEntrance(delta);
        
        syncAudioStates();
        syncSystemStates();
        updateCerberusPostVictoryEndingWalk(delta);
        tryConsumePendingMansionGardenBoss1Cutscene();
        updateMansionGardenBoss1Cutscene(delta);

        // This single call updates ALL systems in order:
        // MoveSystem → CameraSystem → RenderSystem
        float worldDelta = mapTransitionFade.isTransitioning() ? 0f : delta;
        
        // Apply game speed multiplier from cheat (if enabled)
        float gameSpeedMultiplier = cheatCodeManager.getGameSpeedMultiplier();
        worldDelta *= gameSpeedMultiplier;
        
        engine.update(worldDelta);
        updateCerberusBridgePresentationState(worldDelta);
        updateCerberusBossPredialogueCameraApproach(worldDelta);
        updateCerberusBossPostDialogueCameraReturn(worldDelta);
        battleOverlay.render(batch, battleService);
        dialogueOverlay.render(batch, inventoryStage.getViewport(), interactionSystem);
        renderBossFightPrompt();
        renderSaveGamePrompt();
        renderSaveActionPrompt();
        renderNurseStationPrompt();
        
        // ============================================================
        // UI Rendering with Proper Viewport Coordinate Management
        // ============================================================
        
        boolean isBattleActive = battleService.hasBattleSession();

        if (!isBattleActive && teamViewerVisible && teamViewerScreen != null) {
            renderPhysicalBlackout();
            renderUIWithViewport(inventoryStage, uiDelta);
        } else if (!isBattleActive && (sideMenuOverlay.isSettingsVisible() || sideMenuOverlay.isSidebarVisible())) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, uiDelta);
        } else if (!isBattleActive && merchantShopVisible && merchantShopUI != null) {
            renderFullBlackoutOverlay();  // Use full blackout instead of dimming
            renderUIWithViewport(inventoryStage, uiDelta);
        }

        if (mapTransitionFade.isTransitioning()) {
            mapTransitionFade.render(batch);
        }
        renderAreaTitle(uiDelta);
        renderSaveToast();
        renderFallenPrompt();
        
        // Render intro exposition overlay (fullscreen, on top of everything)
        // This renders to physical screen coordinates, not viewport coordinates
        if (introExpositionOverlay.isActive()) {
            introExpositionOverlay.render(batch);
        }
        
        // Render cheat console overlay (always on top, independent stage)
        if (cheatConsoleOverlay.isVisible()) {
            renderUIWithViewport(cheatConsoleStage, uiDelta);
        }
    }

    /**
     * Render a black dimming overlay covering the entire screen.
     * This creates visual separation between the game world and UI overlays.
     * Used when inventory, team viewer, or merchant shop UIs are open.
     */
    private void renderDimmingOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Configure ShapeRenderer to use filled rectangle mode
        shapeRenderer.setProjectionMatrix(inventoryStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.45f);
        
        // Draw black rectangle covering entire virtual UI space (800x600)
        shapeRenderer.rect(0, 0, VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT);
        
        // End rendering
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    /**
     * Render a fully opaque black overlay to completely hide the game background.
     * Used for merchant shop and inventory screens where the game should not be visible.
     */
    private void renderFullBlackoutOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Configure ShapeRenderer to use filled rectangle mode
        shapeRenderer.setProjectionMatrix(inventoryStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 1.0f);  // Fully opaque black
        
        // Draw black rectangle covering entire virtual UI space (800x600)
        shapeRenderer.rect(0, 0, VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT);
        
        // End rendering
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /**
     * Clears the entire physical framebuffer to black, including FitViewport letterbox bars.
     * Use this before rendering full-screen UI overlays (team viewer, summary) so the map
     * does not bleed through the sides on widescreen or non-16:9 displays.
     */
    private void renderPhysicalBlackout() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Render UI stage with proper viewport application and projection matrix management.
     * This ensures UI elements remain anchored to their virtual world coordinates
     * regardless of physical screen resolution during fullscreen transitions.
     *
     * @param stage the UI stage to render
     * @param delta elapsed time since last frame
     */
    private void renderUIWithViewport(Stage stage, float delta) {
        // Update stage logic (animations, listeners, etc.)
        stage.act(delta);
        
        // Apply viewport transformation - maps physical screen coordinates to virtual world coordinates
        // This is the CRITICAL step that keeps UI locked to world space during fullscreen transitions
        stage.getViewport().apply();
        
        // Update and apply camera - stage's camera is locked to viewport's virtual world
        stage.getCamera().update();
        
        // Set batch projection matrix to match the viewport's camera
        // This ensures batch draws use the same coordinate system as the viewport
        batch.setProjectionMatrix(stage.getCamera().combined);
        
        // Now draw the UI within the virtual coordinate system
        stage.draw();
    }

    @Override
    public void hide() {
        // Don't remove entities when switching to overlays (inventory, shop, etc.)
        // Entities are preserved so the game state persists when returning
        // Only remove entities in dispose() when permanently destroying the screen
    }

    @Override
    public void resize(int width, int height) {
        // Update battle overlay first (may have its own viewport)
        battleOverlay.resize(width, height);
        
        // ============================================================
        // Critical Viewport Resize Management
        // ============================================================
        // Update UI stages with proper centering during fullscreen transitions
        // The centerCamera flag (true) is critical: it re-centers the virtual coordinate system
        // after physical screen dimensions change, preventing UI drift and clipping.
        
        // Inventory stage: maps physical screen to virtual 800x600 coordinate space
        inventoryStage.getViewport().update(width, height, true);
        
        // HUD stage: maps physical screen to virtual 800x600 coordinate space  
        hudStage.getViewport().update(width, height, true);
        
        // Cheat console stage: maps physical screen to virtual 800x600 coordinate space
        cheatConsoleStage.getViewport().update(width, height, true);
        
        // Force camera update to recalculate projection matrix
        inventoryStage.getCamera().update();
        hudStage.getCamera().update();
        cheatConsoleStage.getCamera().update();
    }

    @Override
    public void dispose() {
        // Dispose any systems that implement Disposable (e.g. RenderSystem)
        for (EntitySystem system : engine.getSystems()) {
            if (system instanceof Disposable disposable) {
                disposable.dispose();
            }
        }
        engine.removeAllEntities();
        mapTransitionFade.dispose();
        battleOverlay.dispose();
        dialogueOverlay.dispose();
        if (introExpositionOverlay != null) {
            introExpositionOverlay.dispose();
        }
        if (inventoryStage != null) {
            inventoryStage.dispose();
        }
        if (hudStage != null) {
            hudStage.dispose();
        }
        if (cheatConsoleStage != null) {
            cheatConsoleStage.dispose();
        }
        if (uiFont != null) {
            uiFont.dispose();
        }
        if (areaTitleFont != null) {
            areaTitleFont.dispose();
        }
        if (saveToastFont != null) {
            saveToastFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (cheatConsoleOverlay != null) {
            cheatConsoleOverlay.dispose();
        }
    }

    private void syncSystemStates() {
        if (isBertJrPreDialogueSequenceActive()) {
            explorationSystemsEnabled = false;
            engine.getSystem(PlayerInputSystem.class).setProcessing(false);
            engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(false);
            interactionSystem.setProcessing(true);
            engine.getSystem(MoveSystem.class).setProcessing(false);
            engine.getSystem(EncounterDetectionSystem.class).setProcessing(false);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(false);
            mapTransitionSystem.setProcessing(false);
            return;
        }
        if (isMansionGardenBoss1CutsceneMovementLocked()) {
            explorationSystemsEnabled = false;
            engine.getSystem(PlayerInputSystem.class).setProcessing(false);
            engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(false);
            interactionSystem.setProcessing(false);
            engine.getSystem(MoveSystem.class).setProcessing(false);
            engine.getSystem(EncounterDetectionSystem.class).setProcessing(false);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(false);
            mapTransitionSystem.setProcessing(false);
            return;
        }
        if (isSpecialMovementActive()) {
            explorationSystemsEnabled = false;
            engine.getSystem(PlayerInputSystem.class).setProcessing(false);
            engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(false);
            interactionSystem.setProcessing(false);
            engine.getSystem(MoveSystem.class).setProcessing(true);
            engine.getSystem(EncounterDetectionSystem.class).setProcessing(false);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(false);
            mapTransitionSystem.setProcessing(false);
            return;
        }
        if (cerberusPostVictoryEndingWalkActive) {
            explorationSystemsEnabled = false;
            engine.getSystem(PlayerInputSystem.class).setProcessing(false);
            engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(false);
            boolean battleLocked = battleService.hasBattleSession();
            boolean dialogueLocked = interactionSystem.isDialogueVisible();
            boolean bossPromptLocked = isBossFightPromptVisible();
            boolean savePromptLocked = isSaveGamePromptVisible();
            boolean saveActionPromptLocked = isSaveActionPromptVisible();
            boolean nursePromptLocked = isNurseStationPromptVisible();
            boolean fallenPromptLocked = isFallenPromptVisible();
            boolean interactionRetriggerLocked = interactionRetriggerBlockSeconds > 0f;
            boolean merchantLocked = merchantShopVisible;
            boolean menuLocked = sideMenuOverlay.isBlockingGameplay() || teamViewerVisible;
            boolean cheatConsoleLocked = cheatConsoleOverlay.isVisible();
            interactionSystem.setProcessing(!battleLocked
                    && !dialogueLocked
                    && !bossPromptLocked
                    && !savePromptLocked
                    && !saveActionPromptLocked
                    && !nursePromptLocked
                    && !fallenPromptLocked
                    && !interactionRetriggerLocked
                    && !merchantLocked
                    && !menuLocked
                    && !cheatConsoleLocked);
            engine.getSystem(MoveSystem.class).setProcessing(true);
            engine.getSystem(EncounterDetectionSystem.class).setProcessing(false);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(false);
            mapTransitionSystem.setProcessing(false);
            return;
        }
        if (cerberusBossPredialogueCameraActive || cerberusBossPostDialogueCameraReturnPending) {
            explorationSystemsEnabled = false;
            engine.getSystem(PlayerInputSystem.class).setProcessing(false);
            engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(false);
            interactionSystem.setProcessing(false);
            engine.getSystem(MoveSystem.class).setProcessing(false);
            engine.getSystem(EncounterDetectionSystem.class).setProcessing(false);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(false);
            mapTransitionSystem.setProcessing(false);
            return;
        }

        boolean battleLocked = battleService.hasBattleSession();
        boolean dialogueLocked = interactionSystem.isDialogueVisible();
        boolean bossPromptLocked = isBossFightPromptVisible();
        boolean savePromptLocked = isSaveGamePromptVisible();
        boolean saveActionPromptLocked = isSaveActionPromptVisible();
        boolean nursePromptLocked = isNurseStationPromptVisible();
        boolean fallenPromptLocked = isFallenPromptVisible();
        boolean interactionRetriggerLocked = interactionRetriggerBlockSeconds > 0f;
        boolean merchantLocked = merchantShopVisible;
        boolean mapTransitionLocked = mapTransitionFade.isTransitioning();
        boolean menuLocked = sideMenuOverlay.isBlockingGameplay() || teamViewerVisible;
        boolean cheatConsoleLocked = cheatConsoleOverlay.isVisible();
        boolean shouldEnableExploration = !battleLocked
                && !dialogueLocked
                && !bossPromptLocked
                && !savePromptLocked
                && !saveActionPromptLocked
                && !nursePromptLocked
                && !fallenPromptLocked
                && !merchantLocked
                && !mapTransitionLocked
                && !menuLocked
                && !cheatConsoleLocked;
        boolean randomEncountersEnabled = shouldEnableExploration && !isCerberusConveyorWalkActive()
                && !cerberusPostVictoryEndingWalkActive;
        if (shouldEnableExploration == explorationSystemsEnabled) {
            interactionSystem.setProcessing(!battleLocked
                    && !bossPromptLocked
                    && !savePromptLocked
                    && !saveActionPromptLocked
                    && !nursePromptLocked
                    && !fallenPromptLocked
                    && !interactionRetriggerLocked
                    && !merchantLocked
                    && !menuLocked
                    && !cheatConsoleLocked);
            engine.getSystem(RandomEncounterSystem.class).setProcessing(randomEncountersEnabled);
            return;
        }

        explorationSystemsEnabled = shouldEnableExploration;
        engine.getSystem(PlayerInputSystem.class).setProcessing(shouldEnableExploration);
        engine.getSystem(CerberusBridgeWalkSlowSystem.class).setProcessing(shouldEnableExploration);
        interactionSystem.setProcessing(!battleLocked
                && !bossPromptLocked
                && !savePromptLocked
                && !saveActionPromptLocked
                && !nursePromptLocked
                && !fallenPromptLocked
                && !interactionRetriggerLocked
                && !merchantLocked
                && !menuLocked
                && !cheatConsoleLocked);
        engine.getSystem(MoveSystem.class).setProcessing(shouldEnableExploration);
        engine.getSystem(EncounterDetectionSystem.class).setProcessing(shouldEnableExploration);
        engine.getSystem(RandomEncounterSystem.class).setProcessing(randomEncountersEnabled);
        mapTransitionSystem.setProcessing(shouldEnableExploration);
    }

    private boolean isSpecialMovementActive() {
        return activeForcedMove != null;
    }

    private boolean isMansionGardenBoss1CutsceneMovementLocked() {
        return mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.SPARTACUS_RUN
                || mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.DUKE_WALK;
    }

    private boolean isMansionGardenBoss1CutsceneActive() {
        return mansionGardenBoss1CutscenePhase != MansionGardenBoss1CutscenePhase.INACTIVE;
    }

    private boolean isBossFightPromptVisible() {
        return activeBossFightPrompt != null;
    }

    private boolean isSaveGamePromptVisible() {
        return activeSaveGamePrompt != null;
    }

    private boolean isSaveActionPromptVisible() {
        return activeSaveActionPrompt != null;
    }

    private boolean isNurseStationPromptVisible() {
        return activeNurseStationPrompt != null;
    }

    private boolean isBertJrPreDialogueSequenceActive() {
        return bertJrPreDialogueSequenceActive;
    }

    private void startEndRoadForcedMove(Entity playerEntity) {
        if (playerEntity == null || isSpecialMovementActive() || isCerberusConveyorWalkActive()
                || cerberusPostVictoryEndingWalkActive || isMansionGardenBoss1CutsceneMovementLocked()) {
            return;
        }
        Move move = Move.MAPPER.get(playerEntity);
        if (move == null) {
            return;
        }

        startForcedMove(playerEntity, 0f, 1f, PlayerAnimation.Direction.NORTH, END_ROAD_FORCE_MOVE_DURATION_SECONDS);
    }

    private void startMansionPathBlockForcedMove(Entity playerEntity) {
        if (playerEntity == null || isSpecialMovementActive() || isCerberusConveyorWalkActive()
                || cerberusPostVictoryEndingWalkActive || isMansionGardenBoss1CutsceneMovementLocked()) {
            return;
        }
        Move move = Move.MAPPER.get(playerEntity);
        if (move == null) {
            return;
        }

        startForcedMove(
                playerEntity,
                -1f,
                0f,
                PlayerAnimation.Direction.WEST,
                MANSION_PATH_BLOCK_FORCE_MOVE_DURATION_SECONDS
        );
    }

    private void startForcedMove(
            Entity playerEntity,
            float directionX,
            float directionY,
            PlayerAnimation.Direction animationDirection,
            float durationSeconds) {
        if (playerEntity == null || isSpecialMovementActive() || isCerberusConveyorWalkActive()
                || cerberusPostVictoryEndingWalkActive || isMansionGardenBoss1CutsceneMovementLocked()) {
            return;
        }
        Move move = Move.MAPPER.get(playerEntity);
        if (move == null) {
            return;
        }

        activeForcedMove = new ForcedMoveState(
                playerEntity,
                directionX,
                directionY,
                animationDirection,
                durationSeconds
        );
        move.getDirection().set(directionX, directionY);
        move.setMaxSpeed(move.getBaseSpeed());

        PlayerAnimation animation = PlayerAnimation.MAPPER.get(playerEntity);
        if (animation != null && animationDirection != null) {
            animation.setDirection(animationDirection);
            animation.setMoving(true);
        }
    }

    private void updateForcedMove(float delta) {
        ForcedMoveState forcedMove = activeForcedMove;
        if (forcedMove == null) {
            return;
        }

        Move move = Move.MAPPER.get(forcedMove.playerEntity);
        if (move == null) {
            activeForcedMove = null;
            return;
        }

        float dt = Math.max(0f, delta);
        forcedMove.remainingDuration = Math.max(0f, forcedMove.remainingDuration - dt);

        move.getDirection().set(forcedMove.directionX, forcedMove.directionY);
        move.setMaxSpeed(move.getBaseSpeed());

        PlayerAnimation animation = PlayerAnimation.MAPPER.get(forcedMove.playerEntity);
        if (animation != null) {
            if (forcedMove.animationDirection != null) {
                animation.setDirection(forcedMove.animationDirection);
            }
            animation.setMoving(true);
        }

        if (forcedMove.remainingDuration > 0f) {
            return;
        }

        move.getDirection().setZero();
        move.setMaxSpeed(move.getBaseSpeed());
        if (animation != null) {
            animation.setMoving(false);
        }
        activeForcedMove = null;
    }

    private static final class ForcedMoveState {
        private final Entity playerEntity;
        private final float directionX;
        private final float directionY;
        private final PlayerAnimation.Direction animationDirection;
        private float remainingDuration;

        private ForcedMoveState(
                Entity playerEntity,
                float directionX,
                float directionY,
                PlayerAnimation.Direction animationDirection,
                float remainingDuration) {
            this.playerEntity = playerEntity;
            this.directionX = directionX;
            this.directionY = directionY;
            this.animationDirection = animationDirection;
            this.remainingDuration = Math.max(0f, remainingDuration);
        }
    }

    private void updateBossFightPromptInput() {
        BossFightPromptState prompt = activeBossFightPrompt;
        if (prompt == null || isNurseStationPromptVisible()) {
            return;
        }

        if (InputConventions.isMenuLeftJustPressed()) {
            prompt.yesSelected = true;
            return;
        }
        if (InputConventions.isMenuRightJustPressed()) {
            prompt.yesSelected = false;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            handleBossFightPromptChoice(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            handleBossFightPromptChoice(false);
            return;
        }

        if (InputConventions.isInteractJustPressed()) {
            handleBossFightPromptChoice(prompt.yesSelected);
        }
    }

    private void handleBossFightPromptChoice(boolean choseYes) {
        BossFightPromptState prompt = activeBossFightPrompt;
        if (prompt == null) {
            return;
        }
        activeBossFightPrompt = null;
        if (choseYes) {
            prompt.onYes.run();
            return;
        }
        prompt.onNo.run();
    }

    private void renderBossFightPrompt() {
        BossFightPromptState prompt = activeBossFightPrompt;
        if (prompt == null) {
            return;
        }
        String yesOption = promptOptionText("Yes", prompt.yesSelected);
        String noOption = promptOptionText("No", !prompt.yesSelected);
        String text = "Fight " + prompt.enemyName + "?\n\n" + yesOption + "    " + noOption;
        dialogueOverlay.renderPrompt(batch, inventoryStage.getViewport(), text, Interactible.DialoguePosition.BOTTOM);
    }

    private void updateSaveGamePromptInput() {
        SaveGamePromptState prompt = activeSaveGamePrompt;
        if (prompt == null || isBossFightPromptVisible()) {
            return;
        }

        if (InputConventions.isMenuLeftJustPressed()) {
            prompt.yesSelected = true;
            return;
        }
        if (InputConventions.isMenuRightJustPressed()) {
            prompt.yesSelected = false;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            handleSaveGamePromptChoice(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            handleSaveGamePromptChoice(false);
            return;
        }

        if (InputConventions.isInteractJustPressed()) {
            handleSaveGamePromptChoice(prompt.yesSelected);
        }
    }

    private void handleSaveGamePromptChoice(boolean choseYes) {
        SaveGamePromptState prompt = activeSaveGamePrompt;
        if (prompt == null) {
            return;
        }
        activeSaveGamePrompt = null;
        blockInteractionRetrigger();
        if (choseYes) {
            prompt.onYes.run();
            return;
        }
        prompt.onNo.run();
    }

    private void renderSaveGamePrompt() {
        SaveGamePromptState prompt = activeSaveGamePrompt;
        if (prompt == null || isBossFightPromptVisible() || isSaveActionPromptVisible()) {
            return;
        }
        String yesOption = promptOptionText("Yes", prompt.yesSelected);
        String noOption = promptOptionText("No", !prompt.yesSelected);
        String text = "Do you want to save your game?\n\n" + yesOption + "    " + noOption;
        dialogueOverlay.renderPrompt(batch, inventoryStage.getViewport(), text, Interactible.DialoguePosition.BOTTOM);
    }

    private void updateSaveActionPromptInput() {
        SaveActionPromptState prompt = activeSaveActionPrompt;
        if (prompt == null || isBossFightPromptVisible() || isSaveGamePromptVisible()) {
            return;
        }

        if (InputConventions.isMenuLeftJustPressed()) {
            prompt.actionSelected = true;
            return;
        }
        if (InputConventions.isMenuRightJustPressed()) {
            prompt.actionSelected = false;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
            handleSaveActionPromptChoice(true);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            handleSaveActionPromptChoice(false);
            return;
        }

        if (InputConventions.isInteractJustPressed()) {
            handleSaveActionPromptChoice(prompt.actionSelected);
        }
    }

    private void handleSaveActionPromptChoice(boolean choseAction) {
        SaveActionPromptState prompt = activeSaveActionPrompt;
        if (prompt == null) {
            return;
        }
        activeSaveActionPrompt = null;
        blockInteractionRetrigger();
        if (!choseAction) {
            return;
        }
        if (performSavePointSave(prompt.overwriteMode)) {
            showSaveToast();
        }
    }

    private void renderSaveActionPrompt() {
        SaveActionPromptState prompt = activeSaveActionPrompt;
        if (prompt == null || isBossFightPromptVisible() || isSaveGamePromptVisible()) {
            return;
        }
        String actionLabel = prompt.overwriteMode ? "Overwrite" : "Save";
        String actionOption = promptOptionText(actionLabel, prompt.actionSelected);
        String cancelOption = promptOptionText("Cancel", !prompt.actionSelected);
        String text = actionOption + "    " + cancelOption;
        dialogueOverlay.renderPrompt(batch, inventoryStage.getViewport(), text, Interactible.DialoguePosition.BOTTOM);
    }

    private void updateNurseStationPromptInput() {
        NurseStationPromptState prompt = activeNurseStationPrompt;
        if (prompt == null || isBossFightPromptVisible() || isSaveGamePromptVisible() || isSaveActionPromptVisible()) {
            return;
        }
        boolean changed = false;
        if (InputConventions.isMenuLeftJustPressed()) {
            prompt.selectedIndex = (prompt.selectedIndex + 2) % 3;
            changed = true;
        } else if (InputConventions.isMenuRightJustPressed()) {
            prompt.selectedIndex = (prompt.selectedIndex + 1) % 3;
            changed = true;
        } else if (InputConventions.isMenuUpJustPressed()) {
            prompt.selectedIndex = (prompt.selectedIndex + 2) % 3;
            changed = true;
        } else if (InputConventions.isMenuDownJustPressed()) {
            prompt.selectedIndex = (prompt.selectedIndex + 1) % 3;
            changed = true;
        }
        if (changed) {
            audioService.playSound(SoundEffect.UI_HOVER);
        }
        if (InputConventions.isCancelJustPressed()) {
            activeNurseStationPrompt = null;
            blockInteractionRetrigger();
            return;
        }
        if (InputConventions.isInteractJustPressed()) {
            handleNurseStationChoice(prompt.selectedIndex);
        }
    }

    private void handleNurseStationChoice(int optionIndex) {
        activeNurseStationPrompt = null;
        blockInteractionRetrigger();
        audioService.playSound(SoundEffect.UI_SELECT);
        switch (optionIndex) {
            case 0 -> promptSaveGameChoice(this::openSaveActionPrompt, () -> {});
            case 1 -> applyNurseStationHeal();
            default -> {
            }
        }
    }

    private void renderNurseStationPrompt() {
        NurseStationPromptState prompt = activeNurseStationPrompt;
        if (prompt == null || isBossFightPromptVisible() || isSaveGamePromptVisible() || isSaveActionPromptVisible()) {
            return;
        }
        String saveLine = promptOptionText("Save", prompt.selectedIndex == 0);
        String healLine = promptOptionText("Heal", prompt.selectedIndex == 1);
        String exitLine = promptOptionText("Exit", prompt.selectedIndex == 2);
        String text = "What would you like to do?\n\n" + saveLine + "\n" + healLine + "\n" + exitLine;
        dialogueOverlay.renderPrompt(batch, inventoryStage.getViewport(), text, Interactible.DialoguePosition.BOTTOM);
    }

    private String promptOptionText(String text, boolean selected) {
        if (selected) {
            return "[#F2C14E]" + text + "[]";
        }
        return text;
    }

    private boolean performSavePointSave(boolean overwriteMode) {
        SaveStateManager saveStateManager = game.getSaveStateManager();
        SaveState state = buildSaveState();
        if (saveStateManager == null || state == null) {
            return false;
        }

        if (overwriteMode) {
            List<SaveState> existing = saveStateManager.listSaveStates();
            if (!existing.isEmpty()) {
                SaveState selected = existing.get(0);
                state.setDisplayName(selected.getDisplayName());
                state.setCreatedAt(selected.getCreatedAt());
                return saveStateManager.updateSaveState(selected.getFileName(), state);
            }
        }
        return saveStateManager.createSaveState(state) != null;
    }

    private void showSaveToast() {
        showSaveToast(SAVE_TOAST_MESSAGE_SAVED);
    }

    private void showSaveToast(String message) {
        saveToastMessage = (message == null || message.isBlank()) ? SAVE_TOAST_MESSAGE_SAVED : message;
        saveToastTimer = SAVE_TOAST_DURATION_SECONDS;
        audioService.playSound(SoundEffect.CONFIRM);
    }

    private void blockInteractionRetrigger() {
        interactionRetriggerBlockSeconds = INTERACTION_RETRIGGER_BLOCK_SECONDS;
    }

    private void beginMapTransition(String targetMapKey, String targetTransitionId) {
        if (targetMapKey == null || targetMapKey.isBlank()) {
            Gdx.app.error("GameScreen", "Cannot start map transition: missing targetMap");
            return;
        }
        if (targetTransitionId == null || targetTransitionId.isBlank()) {
            Gdx.app.error("GameScreen", "Cannot start map transition: missing targetTransitionId");
            return;
        }

        cancelCerberusBossEncounterCameraIfActive();
        this.activeTransitionMap = targetMapKey;
        this.activeTransitionId = targetTransitionId;
        this.mapTransitionSwapDone = false;
        this.mapTransitionFade.start();
    }

    private void syncAudioStates() {
        boolean hasSession = battleService.hasBattleSession();
        boolean isPlaying = battleService.isBattleActive();
        String encounterId = resolveActiveEncounterId();

        if (hasSession && !wasBattleSessionPresent) {
            defeatSessionEffectsApplied = false;
            triggerRandomEncounterPlayerAlert(encounterId);
            audioService.onEvent(AudioEventType.ENCOUNTER_STARTED);
            audioService.onEvent(AudioEventType.BATTLE_STARTED);
            runBossBattleStartMusicHook(encounterId);
        }

        if (hasSession && isPlaying) {
            runBossMidBattleMusicHooks(encounterId);
        }

        if (wasBattlePlaying && !isPlaying && hasSession) {
            BattlePhase endPhase = battleService.getBattleStateMachine().getPhase();
            if (endPhase == BattlePhase.VICTORY) {
                audioService.onEvent(AudioEventType.BATTLE_VICTORY);
                runBossPostBattleMusicHook(encounterId, true);
                playerProgress.incrementEnemiesDefeated();
                if (ENCOUNTER_BERT_JR_ID.equals(encounterId)) {
                    playerProgress.incrementWins(EVENT_BOSS_0);
                    playerProgress.markEventAccomplished(EVENT_BOSS_0_DEFEATED);
                    hideBertJrProp();
                } else if (ENCOUNTER_SPARTACUS_ID.equals(encounterId)) {
                    playerProgress.incrementWins(EVENT_BOSS_1);
                    playerProgress.markEventAccomplished(EVENT_BOSS_1_DEFEATED);
                    queueMansionGardenBoss1CutsceneIfNeeded();
                    // Apply immediately so pre-battle sprites vanish as soon as victory registers (session may
                    // stay open for reward UI; tryConsume cutscene waits for session to end).
                    refreshMansionGardenBossPropsState(false);
                } else if (ENCOUNTER_CERBERUS_ID.equals(encounterId)) {
                    playerProgress.incrementWins(EVENT_BOSS_2);
                    playerProgress.markEventAccomplished(EVENT_BOSS_2_DEFEATED);
                    removeAllPropEntitiesByObjectId(PROP_CERBERUS_OBJECT_ID);
                    clearCerberusBridgePresentationAfterBossVictory();
                    pendingCerberusPostVictoryEndingWalk = true;
                }
            } else if (endPhase == BattlePhase.ESCAPE) {
                applyEncounterEscapeOutcome(encounterId);
            }
        }

        if (hasSession
                && battleService.getBattleStateMachine().getPhase() == BattlePhase.DEFEAT
                && !defeatSessionEffectsApplied) {
            defeatSessionEffectsApplied = true;
            audioService.onEvent(AudioEventType.BATTLE_DEFEAT);
            runBossPostBattleMusicHook(encounterId, false);
            refreshProgressSnapshots();
            if (ENCOUNTER_BERT_JR_ID.equals(encounterId)) {
                playerProgress.incrementLosses(EVENT_BOSS_0);
            } else if (ENCOUNTER_SPARTACUS_ID.equals(encounterId)) {
                playerProgress.incrementLosses(EVENT_BOSS_1);
            } else if (ENCOUNTER_CERBERUS_ID.equals(encounterId)) {
                playerProgress.incrementLosses(EVENT_BOSS_2);
                pendingCerberusPostVictoryEndingWalk = false;
            }
            queuedFallenScreenAfterBattle = true;
        }

        if (!hasSession && wasBattleSessionPresent) {
            if (!queuedFallenScreenAfterBattle) {
                audioService.onEvent(AudioEventType.BATTLE_ENDED);
            }
            
            // CRITICAL: Clean up inventory state after battle ends
            // This ensures no UI lockup or lingering inventory state
            System.out.println("[GameScreen] Battle ended - cleaning up inventory state");
            closeAllMenuUi();
            System.out.println("[GameScreen] Inventory cleanup complete, ready for exploration");
            activeBossMusicState = null;

            if (queuedFallenScreenAfterBattle) {
                queuedFallenScreenAfterBattle = false;
                activeFallenPrompt = new FallenPromptState(hasPreviousCheckpoint());
                audioService.playSound(SoundEffect.FALLEN);
            }

            refreshMansionGardenBossPropsState(true);
            refreshCerberusPropForCurrentMap();

            if (pendingCerberusPostVictoryEndingWalk) {
                pendingCerberusPostVictoryEndingWalk = false;
                tryBeginCerberusPostVictoryEndingWalk();
            }
        }

        wasBattleSessionPresent = hasSession;
        wasBattlePlaying = isPlaying;
    }

    private void triggerRandomEncounterPlayerAlert(String encounterId) {
        if (encounterId == null || !encounterId.startsWith("random_")) {
            return;
        }
        RenderSystem renderSystem = engine.getSystem(RenderSystem.class);
        if (renderSystem != null) {
            renderSystem.triggerRandomEncounterPlayerAlert();
        }
    }

    private String resolveActiveEncounterId() {
        if (battleService.getBattleStateMachine().getContext() == null
                || battleService.getBattleStateMachine().getContext().getEncounterId() == null) {
            return "";
        }
        return battleService.getBattleStateMachine().getContext().getEncounterId();
    }

    private void applyEncounterEscapeOutcome(String encounterId) {
        if (ENCOUNTER_BERT_JR_ID.equals(encounterId)) {
            applyBertJrEncounterDeclineOutcome(findPlayerEntity(), false);
        } else if (ENCOUNTER_SPARTACUS_ID.equals(encounterId)) {
            applySpartacusEncounterDeclineOutcome(findPlayerEntity(), false);
        } else if (ENCOUNTER_CERBERUS_ID.equals(encounterId)) {
            applyCerberusEncounterDeclineOutcome(findPlayerEntity(), false);
        }
    }

    private void applySpartacusEncounterDeclineOutcome(Entity playerEntity, boolean playCurrentMapMusic) {
        playerProgress.incrementDeclined(EVENT_BOSS_1);
        if (playCurrentMapMusic) {
            audioService.playCurrentMapMusic();
        }
        if (playerEntity == null) {
            return;
        }
        startForcedMove(
                playerEntity,
                -1f,
                0f,
                PlayerAnimation.Direction.WEST,
                BOSS_DECLINE_FORCE_MOVE_DURATION_SECONDS
        );
    }

    private void applyCerberusEncounterDeclineOutcome(Entity playerEntity, boolean playCurrentMapMusic) {
        playerProgress.incrementDeclined(EVENT_BOSS_2);
        if (playCurrentMapMusic) {
            audioService.playCurrentMapMusic();
        }
        if (playerEntity == null) {
            return;
        }
        startForcedMove(
                playerEntity,
                -1f,
                0f,
                PlayerAnimation.Direction.WEST,
                BOSS_DECLINE_FORCE_MOVE_DURATION_SECONDS
        );
    }

    private void applyBertJrEncounterDeclineOutcome(Entity playerEntity, boolean playCurrentMapMusic) {
        playerProgress.incrementDeclined(EVENT_BOSS_0);
        if (playCurrentMapMusic) {
            audioService.playCurrentMapMusic();
        }
        if (playerEntity == null) {
            return;
        }
        startForcedMove(
                playerEntity,
                -1f,
                0f,
                PlayerAnimation.Direction.WEST,
                BOSS_DECLINE_FORCE_MOVE_DURATION_SECONDS
        );
    }

    private void runBossPreBattleMusicHook(String encounterId) {
        BossMusicHooks hooks = bossMusicHooksByEncounterId.get(encounterId);
        if (hooks == null || hooks.preBattleTrack == null) {
            return;
        }
        audioService.playMusic(hooks.preBattleTrack, true);
    }

    private void runBossBattleStartMusicHook(String encounterId) {
        BossMusicHooks hooks = bossMusicHooksByEncounterId.get(encounterId);
        if (hooks == null) {
            activeBossMusicState = null;
            return;
        }
        activeBossMusicState = new ActiveBossMusicState(encounterId, hooks);
        if (hooks.battleStartTrack != null) {
            audioService.playMusic(hooks.battleStartTrack, true);
        }
    }

    private void runBossMidBattleMusicHooks(String encounterId) {
        ActiveBossMusicState state = activeBossMusicState;
        if (state == null || !state.encounterId.equals(encounterId) || state.hooks.midBattleHooks.isEmpty()) {
            return;
        }
        BattleUnit enemy = battleService.getBattleStateMachine().firstEnemy();
        if (enemy == null || enemy.getMaxHp() <= 0) {
            return;
        }
        float hpRatio = (float) enemy.getHp() / (float) enemy.getMaxHp();
        for (MidBattleMusicHook hook : state.hooks.midBattleHooks) {
            String key = Float.toString(hook.hpRatioThreshold);
            if (state.triggeredMidBattleThresholds.contains(key)) {
                continue;
            }
            if (hpRatio <= hook.hpRatioThreshold) {
                if (hook.track != null) {
                    audioService.playMusic(hook.track, true);
                }
                state.triggeredMidBattleThresholds.add(key);
            }
        }
    }

    private void runBossPostBattleMusicHook(String encounterId, boolean victory) {
        BossMusicHooks hooks = bossMusicHooksByEncounterId.get(encounterId);
        if (hooks == null) {
            return;
        }
        MusicTrack track = victory ? hooks.postVictoryTrack : hooks.postDefeatTrack;
        if (track != null) {
            audioService.playMusic(track, false);
        }
    }

    private void performMapTransition(String targetMapKey, String targetTransitionId) {
        MapAsset targetAsset = MapAsset.fromKey(targetMapKey);
        if (targetAsset == null) {
            Gdx.app.error("GameScreen", "Invalid targetMap key: " + targetMapKey + ", aborting transition");
            return;
        }

        Gdx.app.log("GameScreen", "Performing map transition to " + targetMapKey + ":" + targetTransitionId);

        Entity playerEntity = findPlayerEntity();
        if (playerEntity == null) {
            Gdx.app.error("GameScreen", "No player entity found, aborting transition");
            return;
        }

        removeMapScopedEntities(playerEntity);

        TiledMap newMap = tiledService.loadMap(targetAsset);
        tiledService.setMap(newMap);
        refreshBertJrPropStateForCurrentMap();
        refreshCerberusPropForCurrentMap();
        refreshCerberusAtmos0AfterMapLoad();
        refreshMansionGardenBossPropsState(true);
        pendingAreaTitleAsset = targetAsset;

        Rectangle spawnBounds = findTransitionZoneBounds(targetTransitionId);
        if (spawnBounds == null) {
            Gdx.app.error("GameScreen", "Destination zone '" + targetTransitionId
                + "' not found in " + targetMapKey + ", player stays at current position");
        } else {
            repositionPlayer(playerEntity, spawnBounds, newMap);
        }

        mapTransitionSystem.setCooldown(0.4f);
    }

    private Entity findPlayerEntity() {
        ImmutableArray<Entity> players = engine.getEntitiesFor(
            Family.all(Player.class, Transform.class).get());
        return players.size() > 0 ? players.first() : null;
    }

    private void removeMapScopedEntities(Entity playerEntity) {
        java.util.List<Entity> toRemove = new java.util.ArrayList<>();
        ImmutableArray<Entity> allEntities = engine.getEntitiesFor(Family.all().get());
        for (Entity entity : allEntities) {
            if (entity != playerEntity) {
                toRemove.add(entity);
            }
        }
        for (Entity entity : toRemove) {
            engine.removeEntity(entity);
        }
        Gdx.app.log("GameScreen", "Removed " + toRemove.size() + " map-scoped entities");
    }

    private Rectangle findTransitionZoneBounds(String transitionId) {
        ImmutableArray<Entity> zones = engine.getEntitiesFor(
            Family.all(MapTransitionZone.class).get());
        for (Entity zone : zones) {
            MapTransitionZone mtz = MapTransitionZone.MAPPER.get(zone);
            if (transitionId.equals(mtz.getTransitionId())) {
                return mtz.getWorldBounds();
            }
        }
        return null;
    }

    private void repositionPlayer(Entity playerEntity, Rectangle spawnBounds, TiledMap newMap) {
        Transform transform = Transform.MAPPER.get(playerEntity);
        Vector2 pos = transform.getPosition();
        Vector2 size = transform.getSize();

        int mapW = newMap.getProperties().get("width", 0, Integer.class);
        int tileW = newMap.getProperties().get("tilewidth", 0, Integer.class);
        int mapH = newMap.getProperties().get("height", 0, Integer.class);
        int tileH = newMap.getProperties().get("tileheight", 0, Integer.class);
        float worldW = mapW * tileW * Main.UNIT_SCALE;
        float worldH = mapH * tileH * Main.UNIT_SCALE;

        // RPG-style spawn: place player's feet anchor inside destination trigger box.
        float targetX = spawnBounds.x + spawnBounds.width * 0.5f;
        float targetY = spawnBounds.y + spawnBounds.height * 0.5f;

        float spawnX = targetX - size.x * 0.5f;
        float spawnY = targetY - size.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;

        // Final safety clamp to map bounds.
        spawnX = Math.max(0f, Math.min(spawnX, worldW - size.x));
        spawnY = Math.max(0f, Math.min(spawnY, worldH - size.y));

        // Find the closest free point whose player anchor sits inside the trigger area.
        MoveSystem moveSystem = engine.getSystem(MoveSystem.class);
        if (moveSystem != null) {
            Vector2 safeSpawn = findClosestSafeSpawnInZone(
                spawnBounds, size, worldW, worldH, moveSystem, playerEntity);
            if (safeSpawn != null) {
                spawnX = safeSpawn.x;
                spawnY = safeSpawn.y;
            } else if (moveSystem.isBlockedPosition(spawnX, spawnY, size.x, size.y, playerEntity)) {
                Gdx.app.log("GameScreen", "No non-colliding spawn found for transition zone "
                    + spawnBounds + "; using clamped fallback position.");
            }
        }

        pos.set(spawnX, spawnY);

        centerCameraOnPlayer(playerEntity);

        Gdx.app.log("GameScreen", "Player repositioned to (" + spawnX + ", " + spawnY + ") in zone "
            + spawnBounds);
    }

    private static Vector2 findClosestSafeSpawnInZone(
            Rectangle zone,
            Vector2 playerSize,
            float worldW,
            float worldH,
            MoveSystem moveSystem,
            Entity playerEntity) {
        if (zone.width <= 0f || zone.height <= 0f || playerSize.x <= 0f || playerSize.y <= 0f) {
            return null;
        }

        float targetAnchorX = zone.x + zone.width * 0.5f;
        float targetAnchorY = zone.y + zone.height * 0.5f;
        float minAnchorX = zone.x;
        float maxAnchorX = zone.x + zone.width;
        float minAnchorY = zone.y;
        float maxAnchorY = zone.y + zone.height;

        float stepX = Math.max(0.04f, Math.min(playerSize.x * 0.20f, Math.max(0.04f, zone.width / 10f)));
        float stepY = Math.max(0.04f, Math.min(playerSize.y * 0.20f, Math.max(0.04f, zone.height / 10f)));

        Vector2 best = null;
        float bestDist2 = Float.MAX_VALUE;

        for (float anchorY = minAnchorY; anchorY <= maxAnchorY + 0.0001f; anchorY += stepY) {
            float clampedAnchorY = Math.min(anchorY, maxAnchorY);
            for (float anchorX = minAnchorX; anchorX <= maxAnchorX + 0.0001f; anchorX += stepX) {
                float clampedAnchorX = Math.min(anchorX, maxAnchorX);
                float candidateX = clampedAnchorX - playerSize.x * 0.5f;
                float candidateY = clampedAnchorY - playerSize.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;

                float cx = Math.max(0f, Math.min(candidateX, worldW - playerSize.x));
                float cy = Math.max(0f, Math.min(candidateY, worldH - playerSize.y));

                if (moveSystem.isBlockedPosition(cx, cy, playerSize.x, playerSize.y, playerEntity)) {
                    continue;
                }

                float landedAnchorX = cx + playerSize.x * 0.5f;
                float landedAnchorY = cy + playerSize.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;
                float dx = landedAnchorX - targetAnchorX;
                float dy = landedAnchorY - targetAnchorY;
                float dist2 = dx * dx + dy * dy;

                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    if (best == null) {
                        best = new Vector2(cx, cy);
                    } else {
                        best.set(cx, cy);
                    }
                }
            }
        }

        // Fallback: if the trigger box has no safe position, search outward for the
        // closest non-colliding point just outside the trigger bounds.
        if (best == null) {
            float maxSearchRadius = Math.max(
                    Math.max(zone.width, zone.height) * 2f,
                    Math.max(playerSize.x, playerSize.y) * 6f);
            int ringCount = Math.max(1, (int) Math.ceil(maxSearchRadius / Math.min(stepX, stepY)));

            for (int ring = 1; ring <= ringCount; ring++) {
                float padX = ring * stepX;
                float padY = ring * stepY;
                float ringMinX = minAnchorX - padX;
                float ringMaxX = maxAnchorX + padX;
                float ringMinY = minAnchorY - padY;
                float ringMaxY = maxAnchorY + padY;

                for (float anchorY = ringMinY; anchorY <= ringMaxY + 0.0001f; anchorY += stepY) {
                    float clampedRingY = Math.min(anchorY, ringMaxY);
                    for (float anchorX = ringMinX; anchorX <= ringMaxX + 0.0001f; anchorX += stepX) {
                        float clampedRingX = Math.min(anchorX, ringMaxX);

                        boolean outsideZone = clampedRingX < minAnchorX
                                || clampedRingX > maxAnchorX
                                || clampedRingY < minAnchorY
                                || clampedRingY > maxAnchorY;
                        if (!outsideZone) {
                            continue;
                        }

                        float candidateX = clampedRingX - playerSize.x * 0.5f;
                        float candidateY = clampedRingY - playerSize.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;

                        float cx = Math.max(0f, Math.min(candidateX, worldW - playerSize.x));
                        float cy = Math.max(0f, Math.min(candidateY, worldH - playerSize.y));

                        if (moveSystem.isBlockedPosition(cx, cy, playerSize.x, playerSize.y, playerEntity)) {
                            continue;
                        }

                        float landedAnchorX = cx + playerSize.x * 0.5f;
                        float landedAnchorY = cy + playerSize.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;
                        float dx = landedAnchorX - targetAnchorX;
                        float dy = landedAnchorY - targetAnchorY;
                        float dist2 = dx * dx + dy * dy;

                        if (dist2 < bestDist2) {
                            bestDist2 = dist2;
                            if (best == null) {
                                best = new Vector2(cx, cy);
                            } else {
                                best.set(cx, cy);
                            }
                        }
                    }
                }

                // First ring with any valid candidate is already the nearest outside band.
                if (best != null) {
                    break;
                }
            }
        }

        return best;
    }

    private void openTeamViewerSubmenu() {
        summaryVisible = false;
        inventoryStage.clear();
        synchronizePartyLevelsWithSharedXp();

        List<Clawkin> party = playerBattleState.getParty();
        teamViewerScreen = new TeamViewerScreen(inventoryStage, party, uiFont, audioService);
        teamViewerScreen.setSharedExperience(playerProgress.getExperiencePoints());
        teamViewerScreen.setOnBackPressed(this::returnToSidebarFromSubmenu);
        teamViewerScreen.setActiveFighterIndex(playerBattleState.getActiveClawkinIndex());
        teamViewerScreen.setOnActiveFighterSet(idx -> playerBattleState.setActiveClawkinIndex(idx));
        teamViewerScreen.setOnSummaryRequested(this::openSummaryFromTeamViewer);
        inventoryStage.addActor(teamViewerScreen.getRootTable());
        Gdx.input.setInputProcessor(teamViewerScreen.getInputMultiplexer());

        teamViewerVisible = true;
        isPaused = true;
    }

    private void closeTeamViewerSubmenu(boolean returnToSidebar) {
        if (summaryScreen != null) {
            summaryScreen.dispose();
            summaryScreen = null;
        }
        summaryVisible = false;
        teamViewerVisible = false;
        teamViewerScreen = null;
        inventoryStage.clear();
        if (returnToSidebar) {
            sideMenuOverlay.returnToSidebarFromSubmenu();
        }
    }

    private void returnToSidebarFromSubmenu() {
        if (teamViewerVisible) {
            closeTeamViewerSubmenu(true);
            return;
        }

        sideMenuOverlay.returnToSidebarFromSubmenu();
        isPaused = sideMenuOverlay.isBlockingGameplay();
    }

    private void closeAllMenuUi() {
        closeTeamViewerSubmenu(false);

        if (summaryScreen != null) {
            summaryScreen.dispose();
            summaryScreen = null;
        }

        summaryVisible = false;
        sideMenuOverlay.closeAll();
        isPaused = false;
    }

    private void openInventoryScreen() {
        isPaused = false;
        game.setScreen(InventoryScreen.class);
    }

    private void openSaveStateScreen() {
        SaveStateScreen screen = game.getScreen(SaveStateScreen.class);
        screen.configure(
            SaveStateScreen.Mode.SAVE,
            this::buildSaveState,
            null,
            () -> game.setScreen(GameScreen.class)
        );
        closeAllMenuUi();
        game.setScreen(SaveStateScreen.class);
    }

    private void openLoadStateScreen() {
        openLoadStateScreen(true);
    }

    private void openLoadStateScreen(boolean allowBack) {
        SaveStateScreen screen = game.getScreen(SaveStateScreen.class);
        screen.configure(
            SaveStateScreen.Mode.LOAD,
            null,
            saveState -> {
                if (saveState == null) {
                    return;
                }
                queueSaveStateLoad(saveState);
                game.setScreen(GameScreen.class);
            },
            allowBack ? () -> game.setScreen(GameScreen.class) : null
        );
        closeAllMenuUi();
        game.setScreen(SaveStateScreen.class);
    }

    private void returnToMainMenu() {
        // Clean up all menu UI state
        closeAllMenuUi();
        
        // Clear inventory stage to remove any lingering UI elements
        inventoryStage.clear();
        
        // Stop all game audio (music and sounds)
        audioService.stopAll();
        
        // Reset input processor to null (MainMenuScreen will set its own)
        Gdx.input.setInputProcessor(null);
        
        // Properly transition to MainMenuScreen using the screen cache system
        game.setScreen(MainMenuScreen.class);
    }

    /**
     * Trigger the ending credits sequence.
     * Called by the "end" cheat code.
     */
    public void triggerEndingCredits() {
        closeAllMenuUi();
        inventoryStage.clear();
        audioService.stopAll();
        Gdx.input.setInputProcessor(null);
        game.setScreen(github.dluckycompany.clawkins.ui.EndingCreditsScreen.class);
    }

    private SaveState buildSaveState() {
        SaveState state = new SaveState();
        state.setMapKey(resolveCurrentMapKey());
        state.setPlayerName(resolveCurrentPlayerName());
        if (playerProfile != null && playerProfile.getGender() != null) {
            state.setPlayerGender(playerProfile.getGender().name());
        }

        Entity playerEntity = findPlayerEntity();
        if (playerEntity != null) {
            Transform transform = Transform.MAPPER.get(playerEntity);
            if (transform != null) {
                Vector2 pos = transform.getPosition();
                state.setPlayerX(pos.x);
                state.setPlayerY(pos.y);
            }
        }

        state.setMoney(playerBattleState.getWallet().getMoney());
        state.setActiveClawkinIndex(playerBattleState.getActiveClawkinIndex());

        for (Clawkin clawkin : playerBattleState.getParty()) {
            if (clawkin == null) {
                continue;
            }
            SaveState.PartyEntry entry = new SaveState.PartyEntry();
            entry.setId(clawkin.getId());
            entry.setName(clawkin.getName());
            entry.setImagePath(clawkin.getImagePath());
            entry.setIconImagePath(clawkin.getIconImagePath());
            entry.setLevel(clawkin.getLevel());
            entry.setMaxHp(clawkin.getMaxHp());
            entry.setCurrentHp(clawkin.getCurrentHp());
            entry.setAttack(clawkin.getBaseAttack());
            entry.setDefense(clawkin.getBaseDefense());
            entry.setSpeed(clawkin.getBaseSpeed());

            Clawkin.SummaryProfile summary = clawkin.getSummaryProfile();
            if (summary != null) {
                SaveState.SummaryEntry summaryEntry = new SaveState.SummaryEntry();
                summaryEntry.setSpecies(summary.getSpecies());
                summaryEntry.setRole(summary.getRole());
                summaryEntry.setTitle(summary.getTitle());
                summaryEntry.setOverview(summary.getOverview());
                summaryEntry.setProfileHp(summary.getProfileHp());
                summaryEntry.setProfileAttack(summary.getProfileAttack());
                summaryEntry.setProfileDefense(summary.getProfileDefense());
                summaryEntry.setProfileSpeed(summary.getProfileSpeed());
                summaryEntry.setHpNote(summary.getHpNote());
                summaryEntry.setAttackNote(summary.getAttackNote());
                summaryEntry.setDefenseNote(summary.getDefenseNote());
                summaryEntry.setSpeedNote(summary.getSpeedNote());
                entry.setSummary(summaryEntry);
            }

            for (BattleSkill skill : clawkin.getSkills()) {
                if (skill == null) {
                    continue;
                }
                SaveState.SkillEntry skillEntry = new SaveState.SkillEntry();
                skillEntry.setName(skill.getName());
                skillEntry.setEffectType(skill.getEffectType().name());
                skillEntry.setEffectBaseStat(skill.getEffectBaseStat());
                skillEntry.setEffectStatScale(skill.getEffectStatScale());
                skillEntry.setEffectDurationTurns(skill.getEffectDurationTurns());
                skillEntry.setTurnCooldown(skill.getTurnCooldown());
                skillEntry.setSummaryDescription(skill.getSummaryDescription());
                skillEntry.setSummaryEffectText(skill.getSummaryEffectText());
                skillEntry.setSummaryScalingText(skill.getSummaryScalingText());
                entry.getSkills().add(skillEntry);
            }

            state.getParty().add(entry);
        }

        for (Item item : playerBattleState.getInventory().getAllItems()) {
            int qty = playerBattleState.getInventory().getQuantity(item);
            if (item == null || qty <= 0) {
                continue;
            }
            SaveState.InventoryEntry entry = new SaveState.InventoryEntry();
            entry.setItemId(item.getId());
            entry.setQuantity(qty);
            state.getInventory().add(entry);
        }

        refreshProgressSnapshots();
        playerProgress.writeToFlags(state.getFlags());
        state.getFlags().put(PlayerProgress.PROTOCOL_FLAG_KEY, playerProgress.toProtocolPayload());

        return state;
    }

    private boolean applySaveState(SaveState saveState) {
        if (saveState == null) {
            return false;
        }

        Gdx.app.log("GameScreen", "=== APPLYING SAVE STATE ===");
        Gdx.app.log("GameScreen", "Map: " + saveState.getMapKey());
        Gdx.app.log("GameScreen", "Position: (" + saveState.getPlayerX() + ", " + saveState.getPlayerY() + ")");
        Gdx.app.log("GameScreen", "Party size: " + saveState.getParty().size());

        // Apply player data (party, inventory, wallet) first
        applySaveStateToPlayer(saveState);

        MapAsset targetAsset = MapAsset.fromKey(saveState.getMapKey());
        if (targetAsset == null) {
            Gdx.app.error("GameScreen", "Invalid map key: " + saveState.getMapKey() + ", defaulting to COTTAGE");
            targetAsset = MapAsset.COTTAGE_SAMPLE;
        } else {
            Gdx.app.log("GameScreen", "Resolved map asset: " + targetAsset.name());
        }

        // Remove all existing entities (including player if present)
        // The new map normally spawns a player from a PLAYER tile; maps without one get a fallback
        int entityCountBefore = engine.getEntities().size();
        engine.removeAllEntities();
        Gdx.app.log("GameScreen", "Removed " + entityCountBefore + " entities before loading new map");

        // Load and set the new map (this spawns all entities including player)
        Gdx.app.log("GameScreen", "Loading map: " + targetAsset.name());
        TiledMap loadedMap = tiledService.loadMap(targetAsset);
        
        Gdx.app.log("GameScreen", "Setting map (this will spawn entities)");
        tiledService.setMap(loadedMap);
        refreshBertJrPropStateForCurrentMap();
        refreshCerberusPropForCurrentMap();
        refreshCerberusAtmos0AfterMapLoad();
        refreshMansionGardenBossPropsState(true);
        
        int entityCountAfter = engine.getEntities().size();
        Gdx.app.log("GameScreen", "After setMap, entity count: " + entityCountAfter);
        
        mapTransitionSystem.setCooldown(0f);

        // Find the newly spawned player entity (maps without a PLAYER tile need a programmatic spawn)
        Entity loadedPlayer = findPlayerEntity();
        if (loadedPlayer == null) {
            Gdx.app.log("GameScreen", "No PLAYER tile on map; spawning player entity");
            loadedPlayer = tiledObjectConfigurator.spawnPlayerWithoutMapObject(loadedMap);
        }
        if (loadedPlayer != null) {
            Gdx.app.log("GameScreen", "✓ Player entity ready, applying saved position");
            applySavedPlayerPosition(loadedPlayer, loadedMap, saveState.getPlayerX(), saveState.getPlayerY());
            applyPlayerNameToEntity(loadedPlayer, resolveCurrentPlayerName());
            
            // Center camera on player
            centerCameraOnPlayer(loadedPlayer);
            Gdx.app.log("GameScreen", "✓ Camera centered on player");
        } else {
            Gdx.app.error("GameScreen", "✗ CRITICAL: Player entity not found after loading map " + targetAsset);
            Gdx.app.error("GameScreen", "✗ Total entities in engine: " + engine.getEntities().size());
            
            // List all entities for debugging
            for (int i = 0; i < engine.getEntities().size(); i++) {
                Entity e = engine.getEntities().get(i);
                Gdx.app.log("GameScreen", "  Entity " + i + ": " + e.getClass().getSimpleName());
            }
        }

        // Update audio and UI
        audioService.setMap(loadedMap);
        audioService.onEvent(AudioEventType.MAP_CHANGED);
        hudWallet.updateDisplay();
        this.lastAreaNameForSfx = resolveAreaName(targetAsset);
        this.lastAreaDisplayKey = buildAreaDisplayKey(targetAsset);
        
        Gdx.app.log("GameScreen", "=== SAVE STATE APPLIED ===");
        return true;
    }

    private void applySaveStateToPlayer(SaveState saveState) {
        restorePlayerProfile(saveState);

        playerBattleState.getParty().clear();
        for (SaveState.PartyEntry entry : saveState.getParty()) {
            if (entry == null) {
                continue;
            }

            // SKILL MIGRATION: Always refresh skills from SkillUnlockSystem instead of using saved skills
            // This ensures old/incorrect skill names are replaced with correct ones
            List<BattleSkill> skills = github.dluckycompany.clawkins.character.SkillUnlockSystem.getAllSkillsUpToLevel(
                entry.getId(), 
                entry.getLevel()
            );
            
            Gdx.app.log("GameScreen", "SKILL MIGRATION: " + entry.getId() + " level " + entry.getLevel() 
                + " -> loaded " + skills.size() + " skills from SkillUnlockSystem");
            if (!skills.isEmpty()) {
                for (BattleSkill skill : skills) {
                    Gdx.app.log("GameScreen", "  - " + skill.getName());
                }
            }
            
            // Fallback: If SkillUnlockSystem returns no skills, load from save file
            if (skills.isEmpty()) {
                Gdx.app.log("GameScreen", "SKILL MIGRATION FALLBACK: Loading skills from save file");

                skills = new java.util.ArrayList<>();
                for (SaveState.SkillEntry skillEntry : entry.getSkills()) {
                    if (skillEntry == null) {
                        continue;
                    }
                    BattleSkill.EffectType effectType = parseEffectType(skillEntry.getEffectType());
                    BattleSkill skill = new BattleSkill(
                        safeText(skillEntry.getName(), "Skill"),
                        effectType,
                        skillEntry.getEffectBaseStat(),
                        safeText(skillEntry.getEffectStatScale(), "attack[self]"),
                        skillEntry.getEffectDurationTurns(),
                        skillEntry.getTurnCooldown(),
                        safeText(skillEntry.getSummaryDescription(), ""),
                        safeText(skillEntry.getSummaryEffectText(), ""),
                        safeText(skillEntry.getSummaryScalingText(), "")
                    );
                    skills.add(skill);
                }
            }

            SaveState.SummaryEntry summaryEntry = entry.getSummary();
            Clawkin.SummaryProfile summary = summaryEntry == null
                ? Clawkin.SummaryProfile.fromCoreStats(entry.getName(), entry.getMaxHp(), entry.getAttack(), entry.getDefense(), entry.getSpeed())
                : new Clawkin.SummaryProfile(
                    safeText(summaryEntry.getSpecies(), ""),
                    safeText(summaryEntry.getRole(), ""),
                    safeText(summaryEntry.getTitle(), ""),
                    safeText(summaryEntry.getOverview(), ""),
                    summaryEntry.getProfileHp(),
                    summaryEntry.getProfileAttack(),
                    summaryEntry.getProfileDefense(),
                    summaryEntry.getProfileSpeed(),
                    safeText(summaryEntry.getHpNote(), ""),
                    safeText(summaryEntry.getAttackNote(), ""),
                    safeText(summaryEntry.getDefenseNote(), ""),
                    safeText(summaryEntry.getSpeedNote(), "")
                );

            Clawkin clawkin = new Clawkin(
                safeText(entry.getId(), "clawkin"),
                safeText(entry.getName(), "Clawkin"),
                safeText(entry.getImagePath(), ""),
                safeText(entry.getIconImagePath(), ""),
                Math.max(1, entry.getLevel()),
                Math.max(1, entry.getMaxHp()),
                Math.max(1, entry.getAttack()),
                Math.max(0, entry.getDefense()),
                Math.max(1, entry.getSpeed()),
                skills,
                summary
            );
            clawkin.setCurrentHp(entry.getCurrentHp());
            playerBattleState.addClawkinToParty(clawkin);
        }

        playerBattleState.getInventory().clear();
        for (SaveState.InventoryEntry entry : saveState.getInventory()) {
            if (entry == null || entry.getItemId() == null) {
                continue;
            }
            Item item = ItemFactory.getItemById(entry.getItemId());
            if (item == null) {
                continue;
            }
            playerBattleState.getInventory().addItem(item, entry.getQuantity());
        }

        playerBattleState.getWallet().setMoney(saveState.getMoney());
        playerBattleState.setActiveClawkinIndex(saveState.getActiveClawkinIndex());

        String protocolPayload = saveState.getFlags().get(PlayerProgress.PROTOCOL_FLAG_KEY);
        if (protocolPayload != null && !protocolPayload.isBlank()) {
            playerProgress.loadFromProtocolPayload(protocolPayload);
        } else {
            playerProgress.loadFromFlags(saveState.getFlags());
        }
        synchronizePartyLevelsWithSharedXp();
        interactionSystem.loadPersistedInteractionCountsByObjectId(playerProgress.snapshotObjectInteractionCounts());
        refreshProgressSnapshots();
    }

    private String resolveCurrentPlayerName() {
        if (playerProfile != null && playerProfile.getName() != null && !playerProfile.getName().isBlank()) {
            return playerProfile.getName();
        }
        Entity playerEntity = findPlayerEntity();
        if (playerEntity == null) {
            return "Player";
        }
        github.dluckycompany.clawkins.component.PlayerProfile profileComponent =
                github.dluckycompany.clawkins.component.PlayerProfile.MAPPER.get(playerEntity);
        if (profileComponent == null || profileComponent.getPlayerName() == null || profileComponent.getPlayerName().isBlank()) {
            return "Player";
        }
        return profileComponent.getPlayerName();
    }

    private void restorePlayerProfile(SaveState saveState) {
        if (saveState == null) {
            return;
        }
        String name = safeText(saveState.getPlayerName(), "Player");
        Gender gender = parseGender(saveState.getPlayerGender());
        if (gender != null) {
            this.playerProfile = new PlayerProfile(name, gender);
            return;
        }
        if (this.playerProfile == null) {
            this.playerProfile = new PlayerProfile(name, Gender.MALE);
        }
    }

    private void applyPlayerNameToEntity(Entity playerEntity, String playerName) {
        if (playerEntity == null) {
            return;
        }
        String name = safeText(playerName, "Player");
        playerEntity.remove(github.dluckycompany.clawkins.component.PlayerProfile.class);
        playerEntity.add(new github.dluckycompany.clawkins.component.PlayerProfile(name));
    }

    private void refreshProgressSnapshots() {
        synchronizePartyLevelsWithSharedXp();
        playerProgress.captureInventory(playerBattleState.getInventory());
        playerProgress.capturePartyStats(playerBattleState.getParty());
        playerProgress.captureObjectInteractionCounts(
                interactionSystem.snapshotPersistedInteractionCountsByObjectId());
    }

    private void synchronizePartyLevelsWithSharedXp() {
        int sharedLevel = LevelSystem.calculateLevelFromExp(playerProgress.getExperiencePoints());
        List<Clawkin> party = playerBattleState.getParty();
        if (party == null || party.isEmpty()) {
            return;
        }
        for (Clawkin clawkin : party) {
            if (clawkin == null) {
                continue;
            }
            clawkin.syncStatsToSharedExperienceLevel(sharedLevel);
        }
    }

    /**
     * Boosts the shared experience level by the given number of levels.
     * Updates PlayerProgress.experiencePoints to the EXP floor of the new level,
     * then re-syncs all party Clawkins and refreshes all HUD/TeamViewer displays.
     *
     * This is the correct entry point for any level-boost operation (items, cheats)
     * so that the BattleHud XP bar and TeamViewer all stay in sync.
     *
     * @param levelsToAdd number of levels to add (clamped to MAX_LEVEL)
     */
    public void applySharedLevelBoost(int levelsToAdd) {
        if (levelsToAdd <= 0 || playerProgress == null) {
            return;
        }
        int currentLevel = LevelSystem.calculateLevelFromExp(playerProgress.getExperiencePoints());
        int newLevel = Math.min(LevelSystem.MAX_LEVEL, currentLevel + levelsToAdd);
        if (newLevel <= currentLevel) {
            return;
        }
        // Set XP to the floor of the new level so the bar starts at 0% for that level
        int newExp = LevelSystem.getExpRequiredForLevel(newLevel);
        playerProgress.setExperiencePoints(newExp);
        refreshProgressSnapshots();
        Gdx.app.log("GameScreen", "applySharedLevelBoost: level " + currentLevel + " -> " + newLevel + " (XP set to " + newExp + ")");
    }

    /**
     * Boosts the shared experience to the given absolute level.
     * Used by the maxlevel cheat.
     *
     * @param targetLevel the level to set (clamped to MAX_LEVEL)
     */
    public void applySharedLevelSet(int targetLevel) {
        if (playerProgress == null) {
            return;
        }
        int clamped = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, targetLevel));
        int newExp = LevelSystem.getExpRequiredForLevel(clamped);
        playerProgress.setExperiencePoints(newExp);
        refreshProgressSnapshots();
        Gdx.app.log("GameScreen", "applySharedLevelSet: level set to " + clamped + " (XP set to " + newExp + ")");
    }

    /**
     * Awards battle victory XP when the post-battle reward dialogue opens.
     * Boss encounters may use a larger amount via {@link #BOSS_XP_REWARDS_BY_ENCOUNTER_ID}.
     *
     * @param encounterId encounter id from battle context, or {@code null} for wild / generic fights
     * @return XP actually added
     */
    public int applyVictoryExperienceReward(String encounterId) {
        int xp = DEFAULT_BATTLE_XP_REWARD;
        if (encounterId != null) {
            Integer bossXp = BOSS_XP_REWARDS_BY_ENCOUNTER_ID.get(encounterId);
            if (bossXp != null) {
                xp = bossXp;
            }
        }
        playerProgress.addExperiencePoints(xp);
        refreshProgressSnapshots();
        return xp;
    }

    private void applySavedPlayerPosition(Entity playerEntity, TiledMap map, float x, float y) {
        Transform transform = Transform.MAPPER.get(playerEntity);
        if (transform == null || map == null) {
            return;
        }

        Vector2 size = transform.getSize();
        int mapW = map.getProperties().get("width", 0, Integer.class);
        int tileW = map.getProperties().get("tilewidth", 0, Integer.class);
        int mapH = map.getProperties().get("height", 0, Integer.class);
        int tileH = map.getProperties().get("tileheight", 0, Integer.class);
        float worldW = mapW * tileW * Main.UNIT_SCALE;
        float worldH = mapH * tileH * Main.UNIT_SCALE;

        float spawnX = Math.max(0f, Math.min(x, worldW - size.x));
        float spawnY = Math.max(0f, Math.min(y, worldH - size.y));
        transform.getPosition().set(spawnX, spawnY);
        centerCameraOnPlayer(playerEntity);
    }

    private String resolveCurrentMapKey() {
        TiledMap map = tiledService.getCurrentMap();
        if (map == null) {
            return MapAsset.COTTAGE_SAMPLE.name();
        }
        Object asset = map.getProperties().get("mapAsset");
        if (asset instanceof MapAsset mapAssetEnum) {
            return mapAssetEnum.name();
        }
        if (asset instanceof String mapAssetStr && !mapAssetStr.isBlank()) {
            MapAsset resolved = MapAsset.fromKey(mapAssetStr.trim());
            if (resolved != null) {
                return resolved.name();
            }
        }
        return MapAsset.COTTAGE_SAMPLE.name();
    }
    
    /**
     * Process a teleport cheat request.
     * This performs a full map transition using the existing map loading pipeline.
     * 
     * @param mapKey The MapAsset enum name to teleport to
     */
    private void processTeleportCheat(String mapKey) {
        if (mapKey == null || mapKey.isEmpty()) {
            Gdx.app.error("GameScreen", "Teleport cheat: Invalid map key");
            return;
        }
        
        // Resolve the MapAsset from the key
        MapAsset targetAsset = MapAsset.fromKey(mapKey);
        if (targetAsset == null) {
            Gdx.app.error("GameScreen", "Teleport cheat: Unknown map key: " + mapKey);
            return;
        }
        
        Gdx.app.log("Cheat", "Processing teleport to: " + targetAsset.name());
        
        // Find the player entity
        Entity playerEntity = findPlayerEntity();
        if (playerEntity == null) {
            Gdx.app.error("GameScreen", "Teleport cheat: No player entity found");
            return;
        }
        
        // Remove all map-scoped entities except the player
        removeMapScopedEntities(playerEntity);
        
        // Load and set the new map (this spawns all entities including NPCs, enemies, etc.)
        TiledMap loadedMap = tiledService.loadMap(targetAsset);
        tiledService.setMap(loadedMap);
        
        // Refresh Bert Jr. prop state for the new map
        refreshBertJrPropStateForCurrentMap();
        refreshCerberusPropForCurrentMap();
        refreshCerberusAtmos0AfterMapLoad();
        refreshMansionGardenBossPropsState(true);

        // Reset map transition cooldown to prevent immediate re-triggering
        mapTransitionSystem.setCooldown(0.4f);
        
        // Try to find a default spawn point (look for "spawn" transition zone)
        Rectangle spawnBounds = findTransitionZoneBounds("spawn");
        
        if (spawnBounds != null) {
            // Use the spawn zone if it exists
            repositionPlayer(playerEntity, spawnBounds, loadedMap);
            Gdx.app.log("Cheat", "Teleported to spawn zone in " + targetAsset.name());
        } else {
            // No spawn zone found - place player at map center
            int mapW = loadedMap.getProperties().get("width", 0, Integer.class);
            int tileW = loadedMap.getProperties().get("tilewidth", 0, Integer.class);
            int mapH = loadedMap.getProperties().get("height", 0, Integer.class);
            int tileH = loadedMap.getProperties().get("tileheight", 0, Integer.class);
            float worldW = mapW * tileW * Main.UNIT_SCALE;
            float worldH = mapH * tileH * Main.UNIT_SCALE;
            
            Transform transform = Transform.MAPPER.get(playerEntity);
            Vector2 pos = transform.getPosition();
            Vector2 size = transform.getSize();
            
            // Place player at center of map
            float centerX = (worldW - size.x) * 0.5f;
            float centerY = (worldH - size.y) * 0.5f;
            
            // Ensure player is within map bounds
            centerX = Math.max(0f, Math.min(centerX, worldW - size.x));
            centerY = Math.max(0f, Math.min(centerY, worldH - size.y));
            
            pos.set(centerX, centerY);
            
            Gdx.app.log("Cheat", "Teleported to map center in " + targetAsset.name() + " at (" + centerX + ", " + centerY + ")");
        }
        
        // Center camera on player
        centerCameraOnPlayer(playerEntity);
        
        // Update audio and UI
        audioService.setMap(loadedMap);
        audioService.onEvent(AudioEventType.MAP_CHANGED);
        hudWallet.updateDisplay();
        
        // Show area title
        this.lastAreaNameForSfx = resolveAreaName(targetAsset);
        showAreaTitle(targetAsset);
        
        Gdx.app.log("Cheat", "Teleport complete");
    }

    private static BattleSkill.EffectType parseEffectType(String raw) {
        if (raw == null || raw.isBlank()) {
            return BattleSkill.EffectType.DAMAGE;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "heal" -> BattleSkill.EffectType.HEAL;
            case "attack" -> BattleSkill.EffectType.ATTACK;
            case "defense" -> BattleSkill.EffectType.DEFENSE;
            case "bleed" -> BattleSkill.EffectType.BLEED;
            case "parry" -> BattleSkill.EffectType.PARRY;
            default -> BattleSkill.EffectType.DAMAGE;
        };
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static Gender parseGender(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void openSummaryFromTeamViewer() {
        if (teamViewerScreen == null) {
            return;
        }

        Clawkin selected = teamViewerScreen.getSelectedClawkin();
        if (selected == null) {
            System.out.println("[GameScreen] Summary open ignored - selected TeamViewer slot is empty");
            return;
        }

        if (summaryScreen != null) {
            summaryScreen.dispose();
        }

        Clawkin.SummaryProfile profile = selected.getSummaryProfile();
        System.out.println(
            "[GameScreen] Summary data check -> id=" + selected.getId()
                + ", name=" + selected.getName()
                + ", species=" + profile.getSpecies()
                + ", role=" + profile.getRole()
                + ", title=" + profile.getTitle()
                + ", overview=" + profile.getOverview()
                + ", skills=" + selected.getSkills().size()
        );

        summaryScreen = new SummaryScreen(inventoryStage, selected, uiFont, this::closeSummaryToTeamViewer);
        inventoryStage.clear();
        inventoryStage.addActor(summaryScreen.getRootTable());
        Gdx.input.setInputProcessor(summaryScreen.getInputMultiplexer());
        summaryVisible = true;
        System.out.println("[GameScreen] Opened standalone Summary screen for " + selected.getName());
    }

    private void closeSummaryToTeamViewer() {
        if (summaryScreen != null) {
            summaryScreen.dispose();
            summaryScreen = null;
        }

        summaryVisible = false;
        inventoryStage.clear();
        if (teamViewerScreen != null) {
            inventoryStage.addActor(teamViewerScreen.getRootTable());
            Gdx.input.setInputProcessor(teamViewerScreen.getInputMultiplexer());
        }
        System.out.println("[GameScreen] Closed standalone Summary screen and returned to Team Viewer");
    }

    private void openMerchantShop() {
        merchantShopVisible = true;
        if (merchantShopUI != null) {
            inventoryStage.clear();
            merchantShopUI.buildLayout();  // Build the UI layout like InventoryUI
            // Set input processor for consistent coordinate unprojection with virtual viewport
            Gdx.input.setInputProcessor(inventoryStage);
        }
    }

    private void closeMerchantShop() {
        merchantShopVisible = false;
        interactionSystem.closeMerchant();
        if (merchantShopUI != null) {
            merchantShopUI.detachStageKeyboardListener();
        }
        // Restore world input processor when closing merchant shop
        Gdx.input.setInputProcessor(null);
    }

    private BitmapFont createAreaTitleFont() {
        if (!Gdx.files.internal(AREA_TITLE_FONT_PATH).exists()) {
            Gdx.app.log("GameScreen", "Area title font missing: " + AREA_TITLE_FONT_PATH + ", using default font.");
            return new BitmapFont();
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(AREA_TITLE_FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = AREA_TITLE_FONT_SIZE;
        parameter.borderWidth = 2f;
        parameter.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        parameter.color = com.badlogic.gdx.graphics.Color.WHITE;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    private BitmapFont createSaveToastFont() {
        if (!Gdx.files.internal(DialogueBoxRenderer.DIALOGUE_FONT_PATH).exists()) {
            return new BitmapFont();
        }

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(DialogueBoxRenderer.DIALOGUE_FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = SAVE_TOAST_FONT_SIZE;
        parameter.borderWidth = 2f;
        parameter.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        parameter.color = com.badlogic.gdx.graphics.Color.valueOf("#E8C15A");
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    private void renderSaveToast() {
        if (saveToastTimer <= 0f) {
            return;
        }
        float alpha = Math.min(1f, saveToastTimer / SAVE_TOAST_DURATION_SECONDS);

        inventoryStage.getViewport().apply();
        inventoryStage.getCamera().update();
        batch.setProjectionMatrix(inventoryStage.getCamera().combined);

        batch.begin();
        float originalR = saveToastFont.getColor().r;
        float originalG = saveToastFont.getColor().g;
        float originalB = saveToastFont.getColor().b;
        float originalA = saveToastFont.getColor().a;
        saveToastFont.setColor(originalR, originalG, originalB, alpha);
        saveToastFont.draw(batch, saveToastMessage, SAVE_TOAST_X, SAVE_TOAST_Y);
        saveToastFont.setColor(originalR, originalG, originalB, originalA);
        batch.end();
    }

    private void showAreaTitle(MapAsset targetAsset) {
        String areaName = resolveAreaName(targetAsset);
        String variation = resolveAreaVariation(targetAsset);
        String title = variation == null || variation.isBlank()
            ? areaName
            : areaName + " " + variation;
        String displayKey = buildAreaDisplayKey(targetAsset);

        // Skip duplicate transitions within the same named area + variation.
        if (displayKey != null && displayKey.equals(lastAreaDisplayKey)) {
            return;
        }

        boolean shouldPlaySfx = lastAreaNameForSfx != null && !lastAreaNameForSfx.equals(areaName);
        this.activeAreaTitle = title;
        this.areaTitleTimer = AREA_TITLE_DURATION_SECONDS;
        if (shouldPlaySfx) {
            audioService.onEvent(AudioEventType.AREA_NAME_DISPLAY);
        }
        this.lastAreaNameForSfx = areaName;
        this.lastAreaDisplayKey = displayKey;
    }

    private String resolveAreaName(MapAsset targetAsset) {
        MapAssetName mapped = MapAssetName.fromAssetEntry(targetAsset);
        if (mapped != null && mapped.areaName() != null && !mapped.areaName().isBlank()) {
            return mapped.areaName();
        }
        String fallback = MapAssetName.fromAsset(targetAsset);
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return targetAsset.name().replace('_', ' ');
    }

    private String resolveAreaVariation(MapAsset targetAsset) {
        MapAssetName mapped = MapAssetName.fromAssetEntry(targetAsset);
        if (mapped == null) {
            return null;
        }
        String variation = mapped.variationName();
        return variation == null || variation.isBlank() ? null : variation;
    }

    private String buildAreaDisplayKey(MapAsset targetAsset) {
        String areaName = resolveAreaName(targetAsset);
        String variation = resolveAreaVariation(targetAsset);
        if (variation == null) {
            return areaName;
        }
        return areaName + "|" + variation;
    }

    private void renderAreaTitle(float delta) {
        if (activeAreaTitle == null || activeAreaTitle.isBlank()) {
            return;
        }
        if (areaTitleTimer <= 0f) {
            activeAreaTitle = null;
            return;
        }

        areaTitleTimer = Math.max(0f, areaTitleTimer - delta);
        float elapsed = AREA_TITLE_DURATION_SECONDS - areaTitleTimer;
        float fadeInAlpha = AREA_TITLE_FADE_IN_SECONDS <= 0f ? 1f : Math.min(1f, elapsed / AREA_TITLE_FADE_IN_SECONDS);
        float fadeOutAlpha = AREA_TITLE_FADE_OUT_SECONDS <= 0f ? 1f : Math.min(1f, areaTitleTimer / AREA_TITLE_FADE_OUT_SECONDS);
        float alpha = Math.max(0f, Math.min(1f, Math.min(fadeInAlpha, fadeOutAlpha)));
        if (alpha <= 0f) {
            return;
        }

        inventoryStage.getViewport().apply();
        inventoryStage.getCamera().update();
        batch.setProjectionMatrix(inventoryStage.getCamera().combined);

        areaTitleLayout.setText(areaTitleFont, activeAreaTitle);
        float x = (VIRTUAL_UI_WIDTH - areaTitleLayout.width) * 0.5f;
        float y = AREA_TITLE_Y;

        batch.begin();
        areaTitleFont.setColor(0f, 0f, 0f, alpha * 0.8f);
        areaTitleFont.draw(batch, areaTitleLayout, x + 2f, y - 2f);
        areaTitleFont.setColor(1f, 1f, 1f, alpha);
        areaTitleFont.draw(batch, areaTitleLayout, x, y);
        batch.end();
    }

    private boolean shouldPauseForUi() {
        return sideMenuOverlay.isBlockingGameplay()
                || teamViewerVisible
                || merchantShopVisible
                || isBossFightPromptVisible()
                || isSaveGamePromptVisible()
                || isSaveActionPromptVisible()
                || isNurseStationPromptVisible()
                || cheatConsoleOverlay.isVisible()
                || isFallenPromptVisible();
    }

    private boolean isFallenPromptVisible() {
        return activeFallenPrompt != null;
    }

    private void updateFallenPromptInput() {
        FallenPromptState prompt = activeFallenPrompt;
        if (prompt == null || isNurseStationPromptVisible() || isBossFightPromptVisible() || isSaveGamePromptVisible()
                || isSaveActionPromptVisible()) {
            return;
        }

        boolean changed = false;
        if (prompt.checkpointAvailable && InputConventions.isMenuUpJustPressed() && !prompt.checkpointSelected) {
            prompt.checkpointSelected = true;
            changed = true;
        } else if (prompt.checkpointAvailable && InputConventions.isMenuDownJustPressed() && prompt.checkpointSelected) {
            prompt.checkpointSelected = false;
            changed = true;
        }
        if (changed) {
            audioService.playSound(SoundEffect.UI_HOVER);
        }

        if (InputConventions.isInteractJustPressed()) {
            audioService.playSound(SoundEffect.UI_SELECT);
            if (prompt.checkpointAvailable && prompt.checkpointSelected) {
                handleFallenReturnToLastCheckpointChoice();
                return;
            }
            activeFallenPrompt = null;
            openLoadStateScreen(false);
        }
    }

    private void renderFallenPrompt() {
        FallenPromptState prompt = activeFallenPrompt;
        if (prompt == null || isNurseStationPromptVisible() || isBossFightPromptVisible() || isSaveGamePromptVisible()
                || isSaveActionPromptVisible()) {
            return;
        }
        renderFullBlackoutOverlay();

        String line1 = prompt.checkpointAvailable
                ? promptOptionText("Return to Last Checkpoint", prompt.checkpointSelected)
                : "[#8A8A8A]Return to Last Checkpoint";
        String line2 = promptOptionText("Load Save", !prompt.checkpointSelected);
        String text = "You Have Fallen\n\n" + line1 + "\n" + line2;
        dialogueOverlay.renderPrompt(batch, inventoryStage.getViewport(), text, Interactible.DialoguePosition.BOTTOM);
    }

    private boolean hasPreviousCheckpoint() {
        SaveStateManager saveStateManager = game.getSaveStateManager();
        return saveStateManager != null && saveStateManager.hasCheckpointState();
    }

    private void returnToLastStoryCheckpoint() {
        SaveStateManager saveStateManager = game.getSaveStateManager();
        if (saveStateManager == null || !saveStateManager.hasCheckpointState()) {
            return;
        }
        SaveState checkpoint = saveStateManager.loadCheckpointState();
        if (checkpoint == null) {
            return;
        }
        closeAllMenuUi();
        if (battleService.hasBattleSession()) {
            battleService.closeBattleSession();
        }
        applySaveState(checkpoint);
        audioService.playSound(SoundEffect.CONFIRM);
    }

    private void handleFallenReturnToLastCheckpointChoice() {
        FallenPromptState prompt = activeFallenPrompt;
        if (prompt == null) {
            return;
        }
        activeFallenPrompt = null;
        blockInteractionRetrigger();
        returnToLastStoryCheckpoint();
    }

    private static final class BossFightPromptState {
        private final String enemyName;
        private final Runnable onYes;
        private final Runnable onNo;
        private boolean yesSelected = true;

        private BossFightPromptState(String enemyName, Runnable onYes, Runnable onNo) {
            this.enemyName = enemyName;
            this.onYes = onYes == null ? () -> {} : onYes;
            this.onNo = onNo == null ? () -> {} : onNo;
        }
    }

    private static final class SaveGamePromptState {
        private final Runnable onYes;
        private final Runnable onNo;
        private boolean yesSelected = true;

        private SaveGamePromptState(Runnable onYes, Runnable onNo) {
            this.onYes = onYes == null ? () -> {} : onYes;
            this.onNo = onNo == null ? () -> {} : onNo;
        }
    }

    private static final class SaveActionPromptState {
        private final boolean overwriteMode;
        private boolean actionSelected = true;

        private SaveActionPromptState(boolean overwriteMode) {
            this.overwriteMode = overwriteMode;
        }
    }

    /** Nursery station: Save / Heal / Exit (after greeting dialogue). */
    private static final class NurseStationPromptState {
        /** 0 Save, 1 Heal, 2 Exit */
        private int selectedIndex;
    }

    private static final class FallenPromptState {
        private final boolean checkpointAvailable;
        /** true = Return to Last Checkpoint, false = Load Save */
        private boolean checkpointSelected;

        private FallenPromptState(boolean checkpointAvailable) {
            this.checkpointAvailable = checkpointAvailable;
            this.checkpointSelected = checkpointAvailable;
        }
    }

    private static final class BossMusicHooks {
        private final String encounterId;
        private final MusicTrack preBattleTrack;
        private final MusicTrack battleStartTrack;
        private final List<MidBattleMusicHook> midBattleHooks;
        private final MusicTrack postVictoryTrack;
        private final MusicTrack postDefeatTrack;

        private BossMusicHooks(
                String encounterId,
                MusicTrack preBattleTrack,
                MusicTrack battleStartTrack,
                List<MidBattleMusicHook> midBattleHooks,
                MusicTrack postVictoryTrack,
                MusicTrack postDefeatTrack) {
            this.encounterId = encounterId;
            this.preBattleTrack = preBattleTrack;
            this.battleStartTrack = battleStartTrack;
            this.midBattleHooks = midBattleHooks == null ? List.of() : List.copyOf(midBattleHooks);
            this.postVictoryTrack = postVictoryTrack;
            this.postDefeatTrack = postDefeatTrack;
        }
    }

    private static final class MidBattleMusicHook {
        private final float hpRatioThreshold;
        private final MusicTrack track;

        private MidBattleMusicHook(float hpRatioThreshold, MusicTrack track) {
            this.hpRatioThreshold = Math.max(0f, Math.min(1f, hpRatioThreshold));
            this.track = track;
        }
    }

    private static final class ActiveBossMusicState {
        private final String encounterId;
        private final BossMusicHooks hooks;
        private final Set<String> triggeredMidBattleThresholds = new HashSet<>();

        private ActiveBossMusicState(String encounterId, BossMusicHooks hooks) {
            this.encounterId = encounterId == null ? "" : encounterId;
            this.hooks = hooks;
        }
    }

    // ============================================================
    // Viewport Coordinate System Utilities
    // ============================================================
    
    /**
     * Calculate centered X-position for UI element within viewport world coordinates.
     * This anchors UI horizontally to the center of the virtual world, ensuring it
     * remains centered regardless of physical screen resolution.
     *
     * @param stage the UI stage containing the element
     * @param elementWidth width of the UI element in virtual units
     * @return X-coordinate centered in the stage's virtual world
     */
    public float getAnchoredCenterX(Stage stage, float elementWidth) {
        float worldWidth = stage.getViewport().getWorldWidth();
        return (worldWidth / 2f) - (elementWidth / 2f);
    }

    /**
     * Calculate centered Y-position for UI element within viewport world coordinates.
     * This anchors UI vertically to the center of the virtual world, ensuring it
     * remains centered regardless of physical screen resolution.
     *
     * @param stage the UI stage containing the element
     * @param elementHeight height of the UI element in virtual units
     * @return Y-coordinate centered in the stage's virtual world
     */
    public float getAnchoredCenterY(Stage stage, float elementHeight) {
        float worldHeight = stage.getViewport().getWorldHeight();
        return (worldHeight / 2f) - (elementHeight / 2f);
    }

    /**
     * Get the world width of the UI viewport in virtual units.
     * Always returns the same value regardless of physical screen resolution.
     * Used for constraint-based UI layout.
     *
     * @param stage the UI stage
     * @return virtual world width (typically 800 for inventory/HUD)
     */
    public float getViewportWorldWidth(Stage stage) {
        return stage.getViewport().getWorldWidth();
    }

    /**
     * Get the world height of the UI viewport in virtual units.
     * Always returns the same value regardless of physical screen resolution.
     * Used for constraint-based UI layout.
     *
     * @param stage the UI stage
     * @return virtual world height (typically 600 for inventory/HUD)
     */
    public float getViewportWorldHeight(Stage stage) {
        return stage.getViewport().getWorldHeight();
    }

    /**
     * Calculate safe area bounds for UI elements.
     * Returns a bounding box in virtual world coordinates that accounts for
     * screen edges and ensures content remains visible during fullscreen transitions.
     *
     * @param stage the UI stage
     * @param padding edge padding in virtual units
     * @return array [minX, minY, maxX, maxY] defining safe bounds
     */
    public float[] getSafeAreaBounds(Stage stage, float padding) {
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        return new float[] {
            padding,                    // minX
            padding,                    // minY
            worldWidth - padding,       // maxX
            worldHeight - padding       // maxY
        };
    }

    /**
     * Set the pause state of the game.
     * When paused, the game does not advance time in the render() method,
     * effectively freezing all game logic.
     *
     * @param paused true to pause the game, false to resume
     */
    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    /**
     * Get the current pause state of the game.
     *
     * @return true if the game is paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Get the player battle state for accessing party and battle-related data.
     *
     * @return the PlayerBattleState instance
     */
    public github.dluckycompany.clawkins.battle.PlayerBattleState getPlayerBattleState() {
        return playerBattleState;
    }

    /**
     * Get the battle overlay for accessing UI skin and other battle UI components.
     *
     * @return the BattleOverlay instance
     */
    public github.dluckycompany.clawkins.battle.BattleOverlay getBattleOverlay() {
        return battleOverlay;
    }

    public PlayerProgress getPlayerProgress() {
        return playerProgress;
    }

    public void refreshHudWallet() {
        hudWallet.updateDisplay();
    }

    private void refreshBertJrPropStateForCurrentMap() {
        bertJrPropEntity = findPropEntityByObjectId(PROP_BERT_JR_OBJECT_ID);
        bertJrPropWalkingIn = false;
        bertJrPropReachedTarget = false;
        bertJrPreDialogueSequenceActive = false;
        bertJrPropTargetPosition.setZero();
        int bertJrAttempts = playerProgress.getEventStats(EVENT_BOSS_0).getAttemptCount();
        bertJrFirstEncounterMusicStarted = bertJrAttempts > 0;

        if (bertJrPropEntity == null) {
            return;
        }

        if (playerProgress.isEventAccomplished(EVENT_BOSS_0_DEFEATED)) {
            hideBertJrProp();
            return;
        }

        Transform transform = Transform.MAPPER.get(bertJrPropEntity);
        if (transform == null) {
            bertJrPropEntity = null;
            return;
        }

        bertJrPropTargetPosition.set(transform.getPosition());
        if (bertJrAttempts <= 0) {
            transform.getPosition().x = bertJrPropTargetPosition.x + BERT_JR_PROP_INITIAL_X_OFFSET;
            transform.getPosition().y = bertJrPropTargetPosition.y;
            bertJrPropReachedTarget = false;
            return;
        }
        bertJrPropReachedTarget = true;
    }

    /**
     * Resets Cerberus bridge presentation (camera, music, slow walk) after map load or boss defeat.
     */
    private void refreshCerberusAtmos0AfterMapLoad() {
        cerberusBridgePresentationActive = false;
        cerberusBridgeAtmos1Zoom = false;
        cerberusBridgeConveyorUntilBossDefeated = false;
        cerberusBossCameraLinedUpForDialogue = false;
        cerberusBossPostDialogueCameraReturnPending = false;
        cerberusBossPostDialogueReturnFailSafeRemaining = 0f;
        snapWorldCameraZoom(1f);
        audioService.clearMapMusicOverride();
    }

    private void beginCerberusEncAtmos0Ambience() {
        if (!MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())) {
            return;
        }
        if (playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            return;
        }
        if (cerberusBridgeConveyorUntilBossDefeated) {
            return;
        }
        cerberusBridgePresentationActive = true;
        cerberusBridgeConveyorUntilBossDefeated = true;
        audioService.setMapMusicOverride(MusicTrack.BOSS_CERBERUS_DIA);
    }

    private void beginCerberusEncAtmos1Presentation() {
        if (!MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())) {
            return;
        }
        if (playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            return;
        }
        if (!cerberusBridgeConveyorUntilBossDefeated) {
            return;
        }
        cerberusBridgeAtmos1Zoom = true;
    }

    private void clearCerberusBridgePresentationAfterBossVictory() {
        cerberusBridgePresentationActive = false;
        cerberusBridgeAtmos1Zoom = false;
        cerberusBridgeConveyorUntilBossDefeated = false;
        cancelCerberusBossEncounterCameraIfActive();
        snapWorldCameraZoom(1f);
        audioService.clearMapMusicOverride();
        audioService.playCurrentMapMusic();
    }

    private boolean isCerberusConveyorWalkActive() {
        return cerberusBridgeConveyorUntilBossDefeated
                && MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())
                && !playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED);
    }

    private void tryBeginCerberusPostVictoryEndingWalk() {
        if (!playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            return;
        }
        if (!MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())) {
            return;
        }
        if (findPlayerEntity() == null) {
            return;
        }
        if (findInteractibleEntityByObjectId(END_TRIGGER_OBJECT_ID) == null) {
            Gdx.app.error("GameScreen", "end_trigger interactible missing; skipping post-Cerberus ending walk.");
            return;
        }
        cerberusPostVictoryEndingWalkActive = true;
        interactionSystem.rearmTrippableByObjectId(END_TRIGGER_OBJECT_ID);
    }

    private void stopCerberusPostVictoryEndingWalk() {
        cerberusPostVictoryEndingWalkActive = false;
        Entity player = findPlayerEntity();
        if (player == null) {
            return;
        }
        Move move = Move.MAPPER.get(player);
        if (move != null) {
            move.getDirection().setZero();
            move.setMaxSpeed(move.getBaseSpeed());
        }
        PlayerAnimation anim = PlayerAnimation.MAPPER.get(player);
        if (anim != null) {
            anim.setMoving(false);
        }
    }

    private void updateCerberusPostVictoryEndingWalk(float delta) {
        if (!cerberusPostVictoryEndingWalkActive) {
            return;
        }
        if (!MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())
                || !playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            stopCerberusPostVictoryEndingWalk();
            return;
        }
        Entity player = findPlayerEntity();
        Entity trigger = findInteractibleEntityByObjectId(END_TRIGGER_OBJECT_ID);
        if (player == null || trigger == null) {
            stopCerberusPostVictoryEndingWalk();
            return;
        }
        Move move = Move.MAPPER.get(player);
        Transform playerTr = Transform.MAPPER.get(player);
        Transform triggerTr = Transform.MAPPER.get(trigger);
        if (move == null || playerTr == null || triggerTr == null) {
            stopCerberusPostVictoryEndingWalk();
            return;
        }
        float px = playerTr.getPosition().x + playerTr.getSize().x * 0.5f;
        float py = playerTr.getPosition().y + playerTr.getSize().y * PLAYER_TRIPPABLE_ORIGIN_Y_FACTOR;
        float cx = triggerTr.getPosition().x + triggerTr.getSize().x * 0.5f;
        float cy = triggerTr.getPosition().y + triggerTr.getSize().y * 0.5f;
        float dx = cx - px;
        float dy = cy - py;
        float len2 = dx * dx + dy * dy;
        if (len2 < 1e-8f) {
            dx = 0f;
            dy = 1f;
        }
        move.getDirection().set(dx, dy).nor();
        move.setMaxSpeed(move.getBaseSpeed() * CERBERUS_POST_VICTORY_WALK_SPEED_FACTOR);
        PlayerAnimation anim = PlayerAnimation.MAPPER.get(player);
        if (anim != null) {
            anim.setMoving(true);
            if (dy < 0f) {
                anim.setDirection(PlayerAnimation.Direction.SOUTH);
            } else if (dy > 0f) {
                anim.setDirection(PlayerAnimation.Direction.NORTH);
            } else if (dx < 0f) {
                anim.setDirection(PlayerAnimation.Direction.WEST);
            } else {
                anim.setDirection(PlayerAnimation.Direction.EAST);
            }
        }
    }

    private Entity findInteractibleEntityByObjectId(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return null;
        }
        String normalizedTarget = objectId.trim().toLowerCase(Locale.ROOT);
        ImmutableArray<Entity> list = engine.getEntitiesFor(Family.all(Interactible.class, Transform.class).get());
        for (Entity entity : list) {
            Interactible inter = Interactible.MAPPER.get(entity);
            if (inter == null || inter.getObjectId() == null) {
                continue;
            }
            if (normalizedTarget.equals(inter.getObjectId().trim().toLowerCase(Locale.ROOT))) {
                return entity;
            }
        }
        return null;
    }

    private void updateCerberusBridgePresentationState(float delta) {
        float dt = Math.max(0f, delta);
        OrthographicCamera cam = game.getCamera();
        if (cam != null) {
            float targetZ = 1f;
            if (MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())
                    && !playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
                if (cerberusBridgeAtmos1Zoom) {
                    targetZ = CERBERUS_ATMOS1_CAMERA_ZOOM;
                } else if (cerberusBridgePresentationActive) {
                    targetZ = CERBERUS_ATMOS0_CAMERA_ZOOM;
                }
            }
            cam.zoom += (targetZ - cam.zoom) * Math.min(1f, dt * 2.85f);
            cam.zoom = MathUtils.clamp(cam.zoom, 0.4f, 3.5f);
            cam.update();
        }
    }

    private void snapWorldCameraZoom(float zoom) {
        OrthographicCamera cam = game.getCamera();
        if (cam != null) {
            cam.zoom = MathUtils.clamp(zoom, 0.25f, 4f);
            cam.update();
        }
    }

    /**
     * Handoff boss pipeline: pre-dialogue (camera) → map dialogue → post-dialogue special (fight prompt).
     * Mirrors Bert Jr.: return {@code false} until camera is centered, then {@link InteractionSystem#triggerInteractionByObjectId}.
     */
    private boolean cerberusBossPreDialogueCheck(SpecialInteractionContext context) {
        if (playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            return false;
        }
        if (!cerberusBossCameraLinedUpForDialogue) {
            if (tryBeginCerberusBossPredialogueCamera()) {
                return false;
            }
            cerberusBossCameraLinedUpForDialogue = true;
        }
        return true;
    }

    /**
     * @return {@code true} if camera pan started and dialogue must wait; {@code false} if no prop (dialogue may run immediately).
     */
    private boolean tryBeginCerberusBossPredialogueCamera() {
        Entity prop = findPropEntityByObjectId(PROP_CERBERUS_OBJECT_ID);
        Entity player = findPlayerEntity();
        if (prop == null || player == null) {
            return false;
        }
        Transform propTr = Transform.MAPPER.get(prop);
        if (propTr == null) {
            return false;
        }
        if (player.getComponent(CameraFollow.class) != null) {
            player.remove(CameraFollow.class);
        }
        if (prop.getComponent(CameraFollow.class) == null) {
            prop.add(new CameraFollow());
        }
        CameraSystem cam = engine.getSystem(CameraSystem.class);
        if (cam != null) {
            cam.setSmoothingSpeedMultiplier(CERBERUS_BOSS_PREDIALOGUE_CAMERA_SMOOTH_MUL);
            cam.setFollowTargetBiasWorldX(CERBERUS_BOSS_CAMERA_PROP_FRAMING_BIAS_X);
        }
        cerberusBossPredialogueFailSafeRemaining = CERBERUS_BOSS_PREDIALOGUE_CAMERA_FAIL_SAFE_SEC;
        cerberusBossPredialogueCameraActive = true;
        freezePlayerInPlace(player);
        return true;
    }

    private void onCerberusPredialogueCameraCentered() {
        cerberusBossPredialogueCameraActive = false;
        cerberusBossPredialogueFailSafeRemaining = 0f;
        CameraSystem cam = engine.getSystem(CameraSystem.class);
        if (cam != null) {
            cam.setSmoothingSpeedMultiplier(1f);
        }
        cerberusBossCameraLinedUpForDialogue = true;
        interactionSystem.triggerInteractionByObjectId(TRIGGER_BOSS_2_ID);
    }

    private void updateCerberusBossPredialogueCameraApproach(float delta) {
        if (!cerberusBossPredialogueCameraActive) {
            return;
        }
        float dt = Math.max(0f, delta);
        cerberusBossPredialogueFailSafeRemaining -= dt;
        CameraSystem cam = engine.getSystem(CameraSystem.class);
        boolean arrived = cam != null && cam.isCameraNearFollowTarget(0.07f);
        if (arrived || cerberusBossPredialogueFailSafeRemaining <= 0f) {
            if (!arrived && cam != null) {
                Entity prop = findPropEntityByObjectId(PROP_CERBERUS_OBJECT_ID);
                Transform propTr = prop == null ? null : Transform.MAPPER.get(prop);
                if (propTr != null) {
                    cam.snapTo(propTr);
                }
            }
            onCerberusPredialogueCameraCentered();
        }
    }

    private void beginCerberusBossPostDialogueSmoothCameraReturnForFightPrompt(boolean declineIsFirstApproach) {
        cerberusBossPostDialogueCameraReturnPending = true;
        cerberusBossPostDialogueReturnFailSafeRemaining = CERBERUS_BOSS_POST_DIALOGUE_CAMERA_FAIL_SAFE_SEC;
        cerberusBossDeferredPromptDeclineIsFirst = declineIsFirstApproach;
        restoreCerberusBossEncounterCameraToPlayer(false);
    }

    private void updateCerberusBossPostDialogueCameraReturn(float delta) {
        if (!cerberusBossPostDialogueCameraReturnPending) {
            return;
        }
        float dt = Math.max(0f, delta);
        cerberusBossPostDialogueReturnFailSafeRemaining -= dt;
        CameraSystem cam = engine.getSystem(CameraSystem.class);
        boolean arrived = cam != null && cam.isCameraNearFollowTarget(0.08f);
        if (arrived || cerberusBossPostDialogueReturnFailSafeRemaining <= 0f) {
            if (!arrived && cam != null) {
                Entity player = findPlayerEntity();
                Transform tr = player == null ? null : Transform.MAPPER.get(player);
                if (tr != null) {
                    cam.snapTo(tr);
                }
            }
            cerberusBossPostDialogueCameraReturnPending = false;
            cerberusBossPostDialogueReturnFailSafeRemaining = 0f;
            playerProgress.incrementAttempts(EVENT_BOSS_2);
            promptBossFightChoice(
                    "Cerberus",
                    () -> {
                        playerProgress.incrementAccepted(EVENT_BOSS_2);
                        runBossPreBattleMusicHook(ENCOUNTER_CERBERUS_ID);
                        encounterEventBus.publish(new EncounterEvent(
                                EncounterEventType.START_ENCOUNTER,
                                ENCOUNTER_CERBERUS_ID,
                                ENCOUNTER_TABLE_CERBERUS_ID,
                                CERBERUS_PLACEHOLDER_LEVEL,
                                CERBERUS_PLACEHOLDER_HP,
                                CERBERUS_PLACEHOLDER_ATTACK,
                                CERBERUS_PLACEHOLDER_DEFENSE,
                                CERBERUS_PLACEHOLDER_SPEED,
                                createCerberusBossSkills(),
                                "Cerberus",
                                CERBERUS_PORTRAIT_PATH
                        ));
                    },
                    () -> applyCerberusEncounterDeclineOutcome(
                            findPlayerEntity(),
                            cerberusBossDeferredPromptDeclineIsFirst
                    )
            );
        }
    }

    /**
     * @param snapToPlayer if true, camera jumps to the player (cancel / map transition); if false, follow eases back.
     */
    private void restoreCerberusBossEncounterCameraToPlayer(boolean snapToPlayer) {
        Entity player = findPlayerEntity();
        Entity prop = findPropEntityByObjectId(PROP_CERBERUS_OBJECT_ID);
        if (prop != null && prop.getComponent(CameraFollow.class) != null) {
            prop.remove(CameraFollow.class);
        }
        if (player != null && player.getComponent(CameraFollow.class) == null) {
            player.add(new CameraFollow());
        }
        CameraSystem cam = engine.getSystem(CameraSystem.class);
        if (cam != null) {
            cam.setFollowTargetBiasWorldX(0f);
            cam.setSmoothingSpeedMultiplier(1f);
            if (snapToPlayer && player != null) {
                Transform tr = Transform.MAPPER.get(player);
                if (tr != null) {
                    cam.snapTo(tr);
                }
            }
        }
    }

    private void cancelCerberusBossEncounterCameraIfActive() {
        cerberusBossPredialogueCameraActive = false;
        cerberusBossPredialogueFailSafeRemaining = 0f;
        cerberusBossCameraLinedUpForDialogue = false;
        cerberusBossPostDialogueCameraReturnPending = false;
        cerberusBossPostDialogueReturnFailSafeRemaining = 0f;
        restoreCerberusBossEncounterCameraToPlayer(true);
    }

    private void refreshCerberusPropForCurrentMap() {
        if (!MapAsset.CAVE_3.name().equals(resolveCurrentMapKey())) {
            return;
        }
        if (!playerProgress.isEventAccomplished(EVENT_BOSS_2_DEFEATED)) {
            return;
        }
        removeAllPropEntitiesByObjectId(PROP_CERBERUS_OBJECT_ID);
    }

    private void startBertJrPropEntranceIfNeeded(int interactionCount) {
        if (interactionCount != 1 || playerProgress.isEventAccomplished(EVENT_BOSS_0_DEFEATED)) {
            return;
        }
        if (bertJrPropEntity == null) {
            bertJrPropEntity = findPropEntityByObjectId(PROP_BERT_JR_OBJECT_ID);
        }
        if (bertJrPropEntity == null || bertJrPropReachedTarget) {
            return;
        }
        if (bertJrPropTargetPosition.isZero()) {
            Transform transform = Transform.MAPPER.get(bertJrPropEntity);
            if (transform == null) {
                return;
            }
            float currentX = transform.getPosition().x;
            float currentY = transform.getPosition().y;
            bertJrPropTargetPosition.set(currentX - BERT_JR_PROP_INITIAL_X_OFFSET, currentY);
        }
        bertJrPropWalkingIn = true;
    }

    private void startBertJrPreDialogueSequence(Entity playerEntity) {
        if (bertJrPreDialogueSequenceActive || bertJrPropReachedTarget) {
            return;
        }
        bertJrPreDialogueSequenceActive = true;
        freezePlayerInPlace(playerEntity);
        startBertJrPropEntranceIfNeeded(1);
    }

    private void freezePlayerInPlace(Entity playerEntity) {
        if (playerEntity == null) {
            return;
        }
        Move move = Move.MAPPER.get(playerEntity);
        if (move != null) {
            move.getDirection().setZero();
            move.setMaxSpeed(move.getBaseSpeed());
        }
        PlayerAnimation animation = PlayerAnimation.MAPPER.get(playerEntity);
        if (animation != null) {
            animation.setMoving(false);
        }
    }

    private void updateBertJrPropEntrance(float delta) {
        if (!bertJrPropWalkingIn || bertJrPropEntity == null) {
            return;
        }
        Transform transform = Transform.MAPPER.get(bertJrPropEntity);
        if (transform == null) {
            bertJrPropWalkingIn = false;
            bertJrPropEntity = null;
            return;
        }
        float dt = Math.max(0f, delta);
        float targetX = bertJrPropTargetPosition.x;
        float currentX = transform.getPosition().x;
        float nextX = Math.max(targetX, currentX - BERT_JR_PROP_WALK_IN_SPEED * dt);
        transform.getPosition().x = nextX;
        transform.getPosition().y = bertJrPropTargetPosition.y;
        if (nextX <= targetX + 0.001f) {
            transform.getPosition().x = targetX;
            bertJrPropReachedTarget = true;
            bertJrPropWalkingIn = false;
            if (bertJrPreDialogueSequenceActive) {
                bertJrPreDialogueSequenceActive = false;
                interactionSystem.triggerInteractionByObjectId(TRIGGER_BOSS_BERT_JR_ID);
            }
        }
    }

    private void hideBertJrProp() {
        if (bertJrPropEntity != null) {
            engine.removeEntity(bertJrPropEntity);
        }
        bertJrPropEntity = null;
        bertJrPropWalkingIn = false;
        bertJrPropReachedTarget = true;
        bertJrPreDialogueSequenceActive = false;
        bertJrPropTargetPosition.setZero();
    }

    private Entity findPropEntityByObjectId(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return null;
        }
        String normalizedTarget = objectId.trim().toLowerCase();
        ImmutableArray<Entity> props = engine.getEntitiesFor(Family.all(Prop.class, Transform.class).get());
        for (Entity entity : props) {
            Prop prop = Prop.MAPPER.get(entity);
            if (prop == null || prop.getObjectId() == null) {
                continue;
            }
            if (normalizedTarget.equals(prop.getObjectId().trim().toLowerCase())) {
                return entity;
            }
        }
        return null;
    }

    private void queueMansionGardenBoss1CutsceneIfNeeded() {
        if (!MapAsset.MANSION_GARDEN.name().equals(resolveCurrentMapKey())) {
            return;
        }
        if (playerProgress.isEventAccomplished(EVENT_BOSS_1_GARDEN_CUTSCENE_DONE)) {
            return;
        }
        pendingMansionGardenBoss1Cutscene = true;
    }

    private void tryConsumePendingMansionGardenBoss1Cutscene() {
        if (!pendingMansionGardenBoss1Cutscene) {
            return;
        }
        if (battleService.hasBattleSession()) {
            return;
        }
        if (!MapAsset.MANSION_GARDEN.name().equals(resolveCurrentMapKey())) {
            pendingMansionGardenBoss1Cutscene = false;
            return;
        }
        if (!playerProgress.isEventAccomplished(EVENT_BOSS_1_DEFEATED)
                || playerProgress.isEventAccomplished(EVENT_BOSS_1_GARDEN_CUTSCENE_DONE)) {
            pendingMansionGardenBoss1Cutscene = false;
            return;
        }
        if (mansionGardenBoss1CutscenePhase != MansionGardenBoss1CutscenePhase.INACTIVE) {
            return;
        }
        pendingMansionGardenBoss1Cutscene = false;
        beginMansionGardenBoss1Cutscene();
    }

    private void beginMansionGardenBoss1Cutscene() {
        refreshMansionGardenBossPropsState(false);
        freezePlayerInPlace(findPlayerEntity());
        mansionGardenBoss1CutscenePastExitElapsed = 0f;
        mansionGardenCutsceneSpartacus1Entity = findPropEntityByObjectId(PROP_MANSION_SPARTACUS_1);
        mansionGardenCutsceneDuke1Entity = findPropEntityByObjectId(PROP_MANSION_DUKEKHAI_1);
        mansionGardenBoss1CutscenePhase = MansionGardenBoss1CutscenePhase.SPARTACUS_RUN;
    }

    private void updateMansionGardenBoss1Cutscene(float delta) {
        if (mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.INACTIVE
                || mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.DIALOGUE) {
            return;
        }
        OrthographicCamera camera = game.getCamera();
        if (camera == null) {
            return;
        }
        float speed = resolvePlayerBaseMoveSpeed() * 1.5f;
        float dt = Math.max(0f, delta);
        float camHalfW = camera.viewportWidth * 0.5f;
        float camCx = camera.position.x;

        if (mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.SPARTACUS_RUN) {
            Entity e = mansionGardenCutsceneSpartacus1Entity;
            if (e == null) {
                advanceMansionGardenBossCutsceneToAfterbossDialogue();
                return;
            }
            Transform t = Transform.MAPPER.get(e);
            if (t == null) {
                mansionGardenCutsceneSpartacus1Entity = null;
                advanceMansionGardenBossCutsceneToAfterbossDialogue();
                return;
            }
            t.getPosition().x += speed * dt;
            float right = t.getPosition().x + t.getSize().x;
            if (right > camCx + camHalfW + GARDEN_BOSS1_CUTSCENE_OFF_CAMERA_MARGIN) {
                mansionGardenBoss1CutscenePastExitElapsed += dt;
                if (mansionGardenBoss1CutscenePastExitElapsed >= GARDEN_BOSS1_CUTSCENE_EXIT_EXTRA_SECONDS) {
                    engine.removeEntity(e);
                    mansionGardenCutsceneSpartacus1Entity = null;
                    mansionGardenBoss1CutscenePastExitElapsed = 0f;
                    advanceMansionGardenBossCutsceneToAfterbossDialogue();
                }
            } else {
                mansionGardenBoss1CutscenePastExitElapsed = 0f;
            }
            return;
        }

        if (mansionGardenBoss1CutscenePhase == MansionGardenBoss1CutscenePhase.DUKE_WALK) {
            Entity e = mansionGardenCutsceneDuke1Entity;
            if (e == null) {
                completeMansionGardenBoss1Cutscene();
                return;
            }
            Transform t = Transform.MAPPER.get(e);
            if (t == null) {
                mansionGardenCutsceneDuke1Entity = null;
                completeMansionGardenBoss1Cutscene();
                return;
            }
            t.getPosition().x += speed * dt;
            float right = t.getPosition().x + t.getSize().x;
            if (right > camCx + camHalfW + GARDEN_BOSS1_CUTSCENE_OFF_CAMERA_MARGIN) {
                mansionGardenBoss1CutscenePastExitElapsed += dt;
                if (mansionGardenBoss1CutscenePastExitElapsed >= GARDEN_BOSS1_CUTSCENE_EXIT_EXTRA_SECONDS) {
                    engine.removeEntity(e);
                    mansionGardenCutsceneDuke1Entity = null;
                    mansionGardenBoss1CutscenePastExitElapsed = 0f;
                    completeMansionGardenBoss1Cutscene();
                }
            } else {
                mansionGardenBoss1CutscenePastExitElapsed = 0f;
            }
        }
    }

    private void advanceMansionGardenBossCutsceneToAfterbossDialogue() {
        mansionGardenBoss1CutscenePastExitElapsed = 0f;
        mansionGardenBoss1CutscenePhase = MansionGardenBoss1CutscenePhase.DIALOGUE;
        interactionSystem.showScriptedDialogueFromFile(
                DIALOGUE_SPARTACUS_AFTER_BOSS,
                Interactible.DialoguePosition.BOTTOM,
                () -> {
                    mansionGardenBoss1CutscenePastExitElapsed = 0f;
                    mansionGardenBoss1CutscenePhase = MansionGardenBoss1CutscenePhase.DUKE_WALK;
                    mansionGardenCutsceneDuke1Entity = findPropEntityByObjectId(PROP_MANSION_DUKEKHAI_1);
                });
    }

    private void completeMansionGardenBoss1Cutscene() {
        mansionGardenBoss1CutscenePhase = MansionGardenBoss1CutscenePhase.INACTIVE;
        mansionGardenBoss1CutscenePastExitElapsed = 0f;
        mansionGardenCutsceneSpartacus1Entity = null;
        mansionGardenCutsceneDuke1Entity = null;
        playerProgress.markEventAccomplished(EVENT_BOSS_1_GARDEN_CUTSCENE_DONE);
        refreshMansionGardenBossPropsState(true);
        refreshProgressSnapshots();
        audioService.playCurrentMapMusic();
    }

    private float resolvePlayerBaseMoveSpeed() {
        Entity player = findPlayerEntity();
        Move move = player == null ? null : Move.MAPPER.get(player);
        if (move == null) {
            return 3f;
        }
        return Math.max(0.0001f, move.getBaseSpeed());
    }

    /**
     * Visibility for {@code mansion_garden} Spartacus / Duke Khai props before and after the post-boss cutscene.
     *
     * @param queueCutsceneWhenNeeded when true, defeated-but-not-finished maps request {@link #tryConsumePendingMansionGardenBoss1Cutscene()}
     */
    private void refreshMansionGardenBossPropsState(boolean queueCutsceneWhenNeeded) {
        if (!MapAsset.MANSION_GARDEN.name().equals(resolveCurrentMapKey())) {
            return;
        }

        boolean bossDefeated = playerProgress.isEventAccomplished(EVENT_BOSS_1_DEFEATED);
        boolean cutsceneDone = playerProgress.isEventAccomplished(EVENT_BOSS_1_GARDEN_CUTSCENE_DONE);

        if (!bossDefeated) {
            setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_SPARTACUS, 1f);
            setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_DUKEKHAI, 1f);
            setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_SPARTACUS_1, 0f);
            setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_DUKEKHAI_1, 0f);
            return;
        }

        if (cutsceneDone) {
            removeAllPropEntitiesByObjectId(PROP_MANSION_SPARTACUS);
            removeAllPropEntitiesByObjectId(PROP_MANSION_DUKEKHAI);
            removeAllPropEntitiesByObjectId(PROP_MANSION_SPARTACUS_1);
            removeAllPropEntitiesByObjectId(PROP_MANSION_DUKEKHAI_1);
            return;
        }

        removeAllPropEntitiesByObjectId(PROP_MANSION_SPARTACUS);
        removeAllPropEntitiesByObjectId(PROP_MANSION_DUKEKHAI);
        setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_SPARTACUS_1, 1f);
        setAllMansionGardenPropAlphaByObjectId(PROP_MANSION_DUKEKHAI_1, 1f);
        if (queueCutsceneWhenNeeded) {
            pendingMansionGardenBoss1Cutscene = true;
        }
    }

    private void removeAllPropEntitiesByObjectId(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return;
        }
        String target = objectId.trim().toLowerCase(Locale.ROOT);
        ImmutableArray<Entity> props = engine.getEntitiesFor(Family.all(Prop.class, Transform.class).get());
        ArrayList<Entity> toRemove = new ArrayList<>();
        for (int i = 0; i < props.size(); i++) {
            Entity entity = props.get(i);
            Prop prop = Prop.MAPPER.get(entity);
            if (prop == null || prop.getObjectId() == null) {
                continue;
            }
            if (target.equals(prop.getObjectId().trim().toLowerCase(Locale.ROOT))) {
                toRemove.add(entity);
            }
        }
        for (Entity e : toRemove) {
            engine.removeEntity(e);
        }
    }

    private void setAllMansionGardenPropAlphaByObjectId(String objectId, float alpha) {
        if (objectId == null || objectId.isBlank()) {
            return;
        }
        String target = objectId.trim().toLowerCase(Locale.ROOT);
        ImmutableArray<Entity> props = engine.getEntitiesFor(Family.all(Prop.class, Transform.class).get());
        for (int i = 0; i < props.size(); i++) {
            Entity entity = props.get(i);
            Prop prop = Prop.MAPPER.get(entity);
            if (prop == null || prop.getObjectId() == null) {
                continue;
            }
            if (target.equals(prop.getObjectId().trim().toLowerCase(Locale.ROOT))) {
                setMansionGardenPropGraphicAlpha(entity, alpha);
            }
        }
    }

    private static void setMansionGardenPropGraphicAlpha(Entity entity, float alpha) {
        if (entity == null) {
            return;
        }
        Graphic graphic = Graphic.MAPPER.get(entity);
        if (graphic == null) {
            return;
        }
        float a = Math.max(0f, Math.min(1f, alpha));
        Color c = graphic.getColor();
        c.a = a;
    }

    /**
     * Sets the player profile created during character setup.
     *
     * @param profile the player profile to set
     */
    public void setPlayerProfile(PlayerProfile profile) {
        this.playerProfile = profile;
    }

    /**
     * Gets the player profile created during character setup.
     *
     * @return the player profile, or null if not set
     */
    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    /**
     * Starts the intro exposition sequence with cinematic presentation.
     * This is triggered on new game start (not when loading from save).
     */
    private void startIntroExposition() {
        List<String> expositionLines = List.of(
                "The world is full of strange happenings.",
                "One of these is the bond between humans and creatures known as Clawkins.",
                "Animals that possess otherworldly abilities, changing the fundamental rules of nature.",
                "Their numbers great and vast, humans and Clawkins coexist to build a world of balance and order.",
                "Or so one would think.",
                "The greed of humanity knows no bounds, their hands stretched maliciously for a new world order.",
                "One that could break the balance, or fulfil the peace."
        );

        introExpositionOverlay.start(expositionLines, this::onIntroExpositionComplete);
    }

    /**
     * Called when the intro exposition finishes.
     * Triggers the tutorial dialogue using the normal dialogue system.
     */
    private void onIntroExpositionComplete() {
        introExpositionComplete = true;
        // Trigger tutorial dialogue on next frame
        Gdx.app.postRunnable(this::triggerTutorialDialogue);
    }

    /**
     * Triggers the tutorial dialogue using the interaction system.
     * This uses the normal dialogue box (not fullscreen).
     */
    private void triggerTutorialDialogue() {
        if (tutorialDialogueTriggered) {
            return;
        }
        tutorialDialogueTriggered = true;

        // Create tutorial dialogue entries
        List<InteractionSystem.DialogueEntry> tutorialFlow = List.of(
                new InteractionSystem.DialogueEntry("", "It seems like we've run out of food."),
                new InteractionSystem.DialogueEntry("", "We have to head to town to refill.")
        );

        // Manually trigger dialogue through the interaction system
        interactionSystem.showTutorialDialogue(tutorialFlow, Interactible.DialoguePosition.BOTTOM);
    }
}
