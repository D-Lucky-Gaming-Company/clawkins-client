package github.dluckycompany.clawkins;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
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
import github.dluckycompany.clawkins.battle.BattleOverlay;
import github.dluckycompany.clawkins.battle.BattlePhase;
import github.dluckycompany.clawkins.battle.BattleService;
import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.battle.BattleTransition;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.component.MapTransitionZone;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterDetectionSystem;
import github.dluckycompany.clawkins.encounter.EncounterEventBus;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.ItemFactory;
import github.dluckycompany.clawkins.save.SaveState;
import github.dluckycompany.clawkins.system.AnimationSystem;
import github.dluckycompany.clawkins.system.CameraSystem;
import github.dluckycompany.clawkins.system.EnemySystem;
import github.dluckycompany.clawkins.system.InteractionSystem;
import github.dluckycompany.clawkins.system.MapTransitionSystem;
import github.dluckycompany.clawkins.system.MoveSystem;
import github.dluckycompany.clawkins.system.PlayerInputSystem;
import github.dluckycompany.clawkins.system.RenderSystem;
import github.dluckycompany.clawkins.tiled.TiledObjectConfigurator;
import github.dluckycompany.clawkins.tiled.TiledService;
import github.dluckycompany.clawkins.ui.DialogueBoxRenderer;
import github.dluckycompany.clawkins.ui.DialogueOverlay;
import github.dluckycompany.clawkins.ui.HudWallet;
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

    private final Main game;
    private final Engine engine;
    private final Batch batch;
    private final TiledService tiledService;
    private final TiledObjectConfigurator tiledObjectConfigurator;
    private final BattleService battleService;
    private final BattleOverlay battleOverlay;
    private final InteractionSystem interactionSystem;
    private final DialogueOverlay dialogueOverlay;
    private final AudioService audioService;
    private boolean explorationSystemsEnabled;
    private boolean wasBattleSessionPresent;
    private boolean wasBattlePlaying;
    
    // Inventory system - Virtual coordinate system (800x600)
    private final Stage inventoryStage;
    private final Stage hudStage;
    private final HudWallet hudWallet;
    private final BitmapFont uiFont;
    private final BitmapFont areaTitleFont;
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
    
    // Virtual UI resolution (constant, independent of physical screen)
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;

    public GameScreen(Main game) {
        this.game = game;
        AssetService assetService = game.getAssetService();
        Viewport viewport = game.getViewport();
        OrthographicCamera camera = game.getCamera();
        this.batch = game.getBatch();
        this.audioService = game.getAudioService();

        // Create and configure the Ashley ECS Engine
        this.engine = new Engine();

        // Register systems in processing order:
        // 0 – PlayerInputSystem  (reads WASD → sets Move.direction + Animation flags)
        // 1 – AnimationSystem    (advances animation timer → writes frame to Graphic)
        // 2 – MoveSystem         (applies direction * speed * delta to Transform)
        // 3 – CameraSystem       (follows CameraFollow entity)
        // 4 – RenderSystem       (draws map + entities)
        this.engine.addSystem(new PlayerInputSystem());
        this.interactionSystem = new InteractionSystem();
        this.engine.addSystem(interactionSystem);
        this.engine.addSystem(new AnimationSystem());
        this.engine.addSystem(new EnemySystem());
        this.engine.addSystem(new MoveSystem());
        this.engine.addSystem(new CameraSystem(camera));
        this.engine.addSystem(new RenderSystem(batch, viewport, camera));

        this.mapTransitionSystem = new MapTransitionSystem();
        this.mapTransitionSystem.setTransitionCallback((targetMap, targetTransitionId) -> {
            this.pendingTransitionMap = targetMap;
            this.pendingTransitionId = targetTransitionId;
        });
        this.engine.addSystem(mapTransitionSystem);

        EncounterEventBus encounterEventBus = new EncounterEventBus();
        this.playerBattleState = new PlayerBattleState();
        this.interactionSystem.setClawkinPartySupplier(playerBattleState::getParty);
        this.battleService = new BattleService(encounterEventBus, playerBattleState);
        this.engine.addSystem(new EncounterDetectionSystem(encounterEventBus));
        DialogueBoxRenderer dialogueBoxRenderer = new DialogueBoxRenderer();
        this.battleOverlay = new BattleOverlay(game, dialogueBoxRenderer);
        this.battleOverlay.init(assetService, battleService, playerBattleState);
        this.mapTransitionFade = new BattleTransition();
        this.mapTransitionSwapDone = false;
        this.dialogueOverlay = new DialogueOverlay(dialogueBoxRenderer, true);
        this.explorationSystemsEnabled = true;
        this.wasBattleSessionPresent = false;
        this.wasBattlePlaying = false;

        // Inventory system initialization - Fixed virtual UI resolution
        this.inventoryStage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.hudStage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.shapeRenderer = new ShapeRenderer();
        this.uiFont = new BitmapFont();
        this.areaTitleFont = createAreaTitleFont();
        this.areaTitleLayout = new GlyphLayout();
        this.activeAreaTitle = null;
        this.areaTitleTimer = 0f;
        this.pendingAreaTitleAsset = null;
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

        // Tiled map services
        this.tiledService = new TiledService(assetService);
        this.tiledObjectConfigurator = new TiledObjectConfigurator(engine, assetService, playerBattleState);

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
        this.tiledService.setMapChangeConsumer(renderConsumer.andThen(cameraConsumer).andThen(moveConsumer).andThen(enemyConsumer).andThen(transitionConsumer).andThen(audioConsumer));
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
        
        // Initialize wallet with starting money
        if (!loadedFromSave && playerBattleState.getWallet().getMoney() == 0) {
            playerBattleState.getWallet().addMoney(500);
        }
        
        // Create the merchant shop UI (fixed virtual resolution)
        this.merchantShopUI = new MerchantShopUI(
            playerBattleState.getInventory(),
            playerBattleState.getWallet(),
            "Merchant",
            battleOverlay.getSkin(),
            new BitmapFont()
        );
        merchantShopUI.setOnCloseCallback(() -> {
            closeMerchantShop();
            hudWallet.updateDisplay();
        });
        
        // Wire up merchant interaction callback
        interactionSystem.setOnMerchantInteraction(() -> {
            openMerchantShop();
        });
        registerSpecialInteractions();
        
        // Create the full-screen inventory screen and cache it
        this.inventoryScreen = new InventoryScreen(game, playerBattleState.getInventory(), this);
        game.addScreen(inventoryScreen);
        
        if (!loadedFromSave) {
            // Load the map and hand it to TiledService.
            // This triggers: object parsing → entity spawning → map change notification to systems.
            TiledMap startMap = this.tiledService.loadMap(MapAsset.COTTAGE);
            this.tiledService.setMap(startMap);
            this.lastAreaNameForSfx = resolveAreaName(MapAsset.COTTAGE);
            this.lastAreaDisplayKey = buildAreaDisplayKey(MapAsset.COTTAGE);
            // Prevent frame-1 transition triggers from moving the player off the authored spawn.
            mapTransitionSystem.setCooldown(0f);

            // Center camera immediately on the spawned player so first frame matches Tiled placement.
            Entity startupPlayer = findPlayerEntity();
            if (startupPlayer != null) {
                centerCameraOnPlayer(startupPlayer);
            }
            audioService.setMap(startMap);
            audioService.onEvent(AudioEventType.MAP_CHANGED);
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
        //
        // Example:
        // interactionSystem.registerSpecialInteraction("chest_tutorial_01", context -> {
        //     playerBattleState.getWallet().addMoney(100);
        //     hudWallet.updateDisplay();
        // });
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
            currentMap = tiledService.loadMap(MapAsset.COTTAGE);
        }

        tiledService.setMap(currentMap);
        mapTransitionSystem.setCooldown(0.2f);

        Entity restoredPlayer = findPlayerEntity();
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

        // Keep pause state derived from live UI state so movement reliably resumes
        // after exiting CLAWKINS/SETTINGS and closing the sidebar.
        isPaused = shouldPauseForUi();

        // Don't update game state when paused (inventory or other overlays using isPaused flag)
        if (isPaused) {
            delta = 0f; // Skip time advancement
        }
        
        // Cap delta to avoid spiral-of-death on lag spikes
        delta = Math.min(1 / 30f, delta);

        // Handle side-menu and submenu navigation while in exploration.
        if (!battleService.hasBattleSession() && !interactionSystem.isDialogueVisible() && !merchantShopVisible
                && !mapTransitionFade.isTransitioning()) {
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
        syncAudioStates();
        syncSystemStates();

        // This single call updates ALL systems in order:
        // MoveSystem → CameraSystem → RenderSystem
        float worldDelta = mapTransitionFade.isTransitioning() ? 0f : delta;
        engine.update(worldDelta);
        battleOverlay.render(batch, battleService);
        dialogueOverlay.render(batch, interactionSystem);
        
        // ============================================================
        // UI Rendering with Proper Viewport Coordinate Management
        // ============================================================
        
        boolean isBattleActive = battleService.hasBattleSession();

        if (!isBattleActive && teamViewerVisible && teamViewerScreen != null) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, uiDelta);
        } else if (!isBattleActive && (sideMenuOverlay.isSettingsVisible() || sideMenuOverlay.isSidebarVisible())) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, uiDelta);
        } else if (!isBattleActive && merchantShopVisible && merchantShopUI != null) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, uiDelta);
        }

        if (mapTransitionFade.isTransitioning()) {
            mapTransitionFade.render(batch);
        }
        renderAreaTitle(uiDelta);
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
        
        // Force camera update to recalculate projection matrix
        inventoryStage.getCamera().update();
        hudStage.getCamera().update();
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
        if (inventoryStage != null) {
            inventoryStage.dispose();
        }
        if (hudStage != null) {
            hudStage.dispose();
        }
        if (uiFont != null) {
            uiFont.dispose();
        }
        if (areaTitleFont != null) {
            areaTitleFont.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    private void syncSystemStates() {
        boolean battleLocked = battleService.hasBattleSession();
        boolean dialogueLocked = interactionSystem.isDialogueVisible();
        boolean merchantLocked = merchantShopVisible;
        boolean mapTransitionLocked = mapTransitionFade.isTransitioning();
        boolean menuLocked = sideMenuOverlay.isBlockingGameplay() || teamViewerVisible;
        boolean shouldEnableExploration = !battleLocked && !dialogueLocked && !merchantLocked && !mapTransitionLocked && !menuLocked;
        if (shouldEnableExploration == explorationSystemsEnabled) {
            interactionSystem.setProcessing(!battleLocked && !merchantLocked && !menuLocked);
            return;
        }

        explorationSystemsEnabled = shouldEnableExploration;
        engine.getSystem(PlayerInputSystem.class).setProcessing(shouldEnableExploration);
        interactionSystem.setProcessing(!battleLocked && !merchantLocked && !menuLocked);
        engine.getSystem(MoveSystem.class).setProcessing(shouldEnableExploration);
        engine.getSystem(EncounterDetectionSystem.class).setProcessing(shouldEnableExploration);
        mapTransitionSystem.setProcessing(shouldEnableExploration);
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

        this.activeTransitionMap = targetMapKey;
        this.activeTransitionId = targetTransitionId;
        this.mapTransitionSwapDone = false;
        this.mapTransitionFade.start();
    }

    private void syncAudioStates() {
        boolean hasSession = battleService.hasBattleSession();
        boolean isPlaying = battleService.isBattleActive();

        if (hasSession && !wasBattleSessionPresent) {
            audioService.onEvent(AudioEventType.ENCOUNTER_STARTED);
            audioService.onEvent(AudioEventType.BATTLE_STARTED);
        }

        if (wasBattlePlaying && !isPlaying && hasSession) {
            BattlePhase endPhase = battleService.getBattleStateMachine().getPhase();
            if (endPhase == BattlePhase.VICTORY) {
                audioService.onEvent(AudioEventType.BATTLE_VICTORY);
            } else if (endPhase == BattlePhase.DEFEAT) {
                audioService.onEvent(AudioEventType.BATTLE_DEFEAT);
            }
        }

        if (!hasSession && wasBattleSessionPresent) {
            audioService.onEvent(AudioEventType.BATTLE_ENDED);
            
            // CRITICAL: Clean up inventory state after battle ends
            // This ensures no UI lockup or lingering inventory state
            System.out.println("[GameScreen] Battle ended - cleaning up inventory state");
            closeAllMenuUi();
            System.out.println("[GameScreen] Inventory cleanup complete, ready for exploration");
        }

        wasBattleSessionPresent = hasSession;
        wasBattlePlaying = isPlaying;
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

        // If trigger overlaps blocked space, find closest free point inside the trigger area.
        MoveSystem moveSystem = engine.getSystem(MoveSystem.class);
        if (moveSystem != null && moveSystem.isBlockedPosition(spawnX, spawnY, size.x, size.y, playerEntity)) {
            Vector2 safeSpawn = findClosestSafeSpawnInZone(
                spawnBounds, size, worldW, worldH, targetX, targetY, moveSystem, playerEntity);
            if (safeSpawn != null) {
                spawnX = safeSpawn.x;
                spawnY = safeSpawn.y;
            } else {
                Gdx.app.log("GameScreen", "No non-colliding spawn found inside transition zone "
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
            float targetCenterX,
            float targetCenterY,
            MoveSystem moveSystem,
            Entity playerEntity) {
        float minX = Math.max(0f, zone.x);
        float maxX = Math.min(worldW - playerSize.x, zone.x + zone.width - playerSize.x);
        float minY = Math.max(0f, zone.y);
        float maxY = Math.min(worldH - playerSize.y, zone.y + zone.height - playerSize.y);

        if (maxX < minX || maxY < minY) {
            return null;
        }

        float stepX = Math.max(0.05f, Math.min(playerSize.x * 0.25f, Math.max(0.05f, (maxX - minX) / 8f)));
        float stepY = Math.max(0.05f, Math.min(playerSize.y * 0.25f, Math.max(0.05f, (maxY - minY) / 8f)));

        Vector2 best = null;
        float bestDist2 = Float.MAX_VALUE;

        for (float y = minY; y <= maxY + 0.0001f; y += stepY) {
            float cy = Math.min(y, maxY);
            for (float x = minX; x <= maxX + 0.0001f; x += stepX) {
                float cx = Math.min(x, maxX);
                if (moveSystem.isBlockedPosition(cx, cy, playerSize.x, playerSize.y, playerEntity)) {
                    continue;
                }

                float centerX = cx + playerSize.x * 0.5f;
                float centerY = cy + playerSize.y * PLAYER_TRANSITION_ANCHOR_Y_FACTOR;
                float dx = centerX - targetCenterX;
                float dy = centerY - targetCenterY;
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

        return best;
    }

    private void openTeamViewerSubmenu() {
        summaryVisible = false;
        inventoryStage.clear();

        List<Clawkin> party = playerBattleState.getParty();
        teamViewerScreen = new TeamViewerScreen(inventoryStage, party, uiFont);
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
            () -> game.setScreen(GameScreen.class)
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

    private SaveState buildSaveState() {
        SaveState state = new SaveState();
        state.setMapKey(resolveCurrentMapKey());

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
            targetAsset = MapAsset.COTTAGE;
        } else {
            Gdx.app.log("GameScreen", "Resolved map asset: " + targetAsset.name());
        }

        // Remove all existing entities (including player if present)
        // The new map will spawn a fresh player entity
        int entityCountBefore = engine.getEntities().size();
        engine.removeAllEntities();
        Gdx.app.log("GameScreen", "Removed " + entityCountBefore + " entities before loading new map");

        // Load and set the new map (this spawns all entities including player)
        Gdx.app.log("GameScreen", "Loading map: " + targetAsset.name());
        TiledMap loadedMap = tiledService.loadMap(targetAsset);
        
        Gdx.app.log("GameScreen", "Setting map (this will spawn entities)");
        tiledService.setMap(loadedMap);
        
        int entityCountAfter = engine.getEntities().size();
        Gdx.app.log("GameScreen", "After setMap, entity count: " + entityCountAfter);
        
        mapTransitionSystem.setCooldown(0f);

        // Find the newly spawned player entity
        Entity loadedPlayer = findPlayerEntity();
        if (loadedPlayer != null) {
            Gdx.app.log("GameScreen", "✓ Player entity found, applying saved position");
            applySavedPlayerPosition(loadedPlayer, loadedMap, saveState.getPlayerX(), saveState.getPlayerY());
            
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
        playerBattleState.getParty().clear();
        for (SaveState.PartyEntry entry : saveState.getParty()) {
            if (entry == null) {
                continue;
            }

            List<BattleSkill> skills = new java.util.ArrayList<>();
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
            return MapAsset.COTTAGE.name();
        }
        Object asset = map.getProperties().get("mapAsset");
        if (asset instanceof MapAsset mapAsset) {
            return mapAsset.name();
        }
        return MapAsset.COTTAGE.name();
    }

    private static BattleSkill.EffectType parseEffectType(String raw) {
        if (raw == null || raw.isBlank()) {
            return BattleSkill.EffectType.DAMAGE;
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "heal" -> BattleSkill.EffectType.HEAL;
            case "attack" -> BattleSkill.EffectType.ATTACK;
            case "defense" -> BattleSkill.EffectType.DEFENSE;
            default -> BattleSkill.EffectType.DAMAGE;
        };
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
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
            inventoryStage.addActor(merchantShopUI);
            // Set input processor for consistent coordinate unprojection with virtual viewport
            Gdx.input.setInputProcessor(inventoryStage);
        }
    }

    private void closeMerchantShop() {
        merchantShopVisible = false;
        interactionSystem.closeMerchant();
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
        return sideMenuOverlay.isBlockingGameplay() || teamViewerVisible || merchantShopVisible;
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
}

