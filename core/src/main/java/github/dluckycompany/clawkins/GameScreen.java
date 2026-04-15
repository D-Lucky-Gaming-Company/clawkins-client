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
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import github.dluckycompany.clawkins.audio.AudioEventType;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.battle.BattleOverlay;
import github.dluckycompany.clawkins.battle.BattlePhase;
import github.dluckycompany.clawkins.battle.BattleService;
import github.dluckycompany.clawkins.battle.BattleTransition;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.component.MapTransitionZone;
import github.dluckycompany.clawkins.component.Player;
import github.dluckycompany.clawkins.component.Transform;
import github.dluckycompany.clawkins.encounter.EncounterDetectionSystem;
import github.dluckycompany.clawkins.encounter.EncounterEventBus;
import github.dluckycompany.clawkins.inventory.InventoryController;
import github.dluckycompany.clawkins.item.ItemFactory;
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
import github.dluckycompany.clawkins.ui.InventoryOverlay;
import github.dluckycompany.clawkins.ui.InventoryScreen;
import github.dluckycompany.clawkins.ui.MerchantShopUI;
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
    private InventoryOverlay inventoryOverlay;
    private boolean inventoryVisible = false;
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
        this.battleOverlay = new BattleOverlay(dialogueBoxRenderer);
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
            Gdx.input.setInputProcessor(null);  // Restore normal world input
            isPaused = false;  // Make sure we're not paused
            teamViewerVisible = false;  // Clear team viewer state
            inventoryStage.clear();  // Clear any lingering UI
            return;
        }
        hasBeenInitialized = true;

        // Initialize inventory with default items
        if (playerBattleState.getInventory().getAllItems().isEmpty()) {
            playerBattleState.getInventory().addItem(ItemFactory.BASIC_POTION, 3);
            playerBattleState.getInventory().addItem(ItemFactory.FULL_HEAL, 1);
            playerBattleState.getInventory().addItem(ItemFactory.REVIVE, 2);
            playerBattleState.getInventory().addItem(ItemFactory.ATTACK_BOOST, 1);
            playerBattleState.getInventory().addItem(ItemFactory.DEFENSE_BOOST, 1);
        }
        
        // Initialize wallet with starting money
        if (playerBattleState.getWallet().getMoney() == 0) {
            playerBattleState.getWallet().addMoney(500);
        }
        
        // Create the inventory controller (Command Pattern) for decoupled item actions
        InventoryController inventoryController = new InventoryController(playerBattleState.getInventory());
        
        // Create the inventory UI with the party, inventory, and controller
        this.inventoryOverlay = new InventoryOverlay(
            playerBattleState.getInventory(),
            inventoryController,
            playerBattleState.getParty(),
            battleOverlay.getSkin(),
            new BitmapFont()
        );
        inventoryOverlay.setOnCloseCallback(() -> toggleInventory());
        inventoryOverlay.setOnItemUsedCallback(action -> {
            hudWallet.updateDisplay();
        });
        
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
        
        // Create the full-screen inventory screen and cache it
        this.inventoryScreen = new InventoryScreen(game, playerBattleState.getInventory(), this);
        game.addScreen(inventoryScreen);
        
        // Load the map and hand it to TiledService.
        // This triggers: object parsing → entity spawning → map change notification to systems.
        TiledMap startMap = this.tiledService.loadMap(MapAsset.COTTAGE);
        this.tiledService.setMap(startMap);
        // Prevent frame-1 transition triggers from moving the player off the authored spawn.
        mapTransitionSystem.setCooldown(0f);

        // Center camera immediately on the spawned player so first frame matches Tiled placement.
        Entity startupPlayer = findPlayerEntity();
        if (startupPlayer != null) {
            Transform t = Transform.MAPPER.get(startupPlayer);
            if (t != null) {
                OrthographicCamera camera = game.getCamera();
                Vector2 pos = t.getPosition();
                Vector2 size = t.getSize();
                camera.position.set(pos.x + size.x / 2f, pos.y + size.y / 2f + 1f, camera.position.z);
                camera.update();
            }
        }
        audioService.setMap(startMap);
        audioService.onEvent(AudioEventType.MAP_CHANGED);
    }

    @Override
    public void render(float delta) {
        // Don't update game state when paused (inventory or other overlays using isPaused flag)
        if (isPaused) {
            delta = 0f; // Skip time advancement
        }
        
        // Cap delta to avoid spiral-of-death on lag spikes
        delta = Math.min(1 / 30f, delta);

        // Handle inventory toggle (E key) - but NOT while inventory is open (paused)
        if (!battleService.hasBattleSession() && !interactionSystem.isDialogueVisible() && !isPaused
                && !mapTransitionFade.isTransitioning()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                toggleInventory();
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                toggleTeamViewer();
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
            if (inventoryVisible) {
                System.out.println("[GameScreen] Battle started - closing inventory UI");
                inventoryVisible = false;
                if (inventoryOverlay != null) {
                    inventoryOverlay.getDialogStage().clear();
                }
            }
            if (teamViewerVisible) {
                System.out.println("[GameScreen] Battle started - closing team viewer");
                teamViewerVisible = false;
                if (summaryVisible) {
                    summaryVisible = false;
                }
                if (summaryScreen != null) {
                    summaryScreen.dispose();
                    summaryScreen = null;
                }
            }
            if (inventoryVisible || teamViewerVisible) {
                inventoryStage.clear();
                Gdx.input.setInputProcessor(null);
                isPaused = false;
            }
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
        } else if (mapTransitionSwapDone) {
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
        
        // CRITICAL: Block ALL inventory UI rendering when a battle is active
        // This prevents any UI lockup or visual conflicts with battle overlay
        boolean isBattleActive = battleService.hasBattleSession();
        
        // Render target selection overlay if active (takes priority over inventory)
        if (!isBattleActive && inventoryVisible && inventoryOverlay != null && inventoryOverlay.isSelectingTarget()) {
            Stage targetStage = inventoryOverlay.getTargetSelectionStage();
            if (targetStage != null) {
                renderUIWithViewport(targetStage, delta);
            }
        }
        // Render team viewer if visible (takes priority)
        else if (teamViewerVisible && teamViewerScreen != null && !isBattleActive) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, delta);
        }
        // Render merchant shop if visible
        else if (merchantShopVisible && merchantShopUI != null && !isBattleActive) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, delta);
        } 
        // Render inventory and HUD if visible (only when NOT in battle)
        else if (inventoryVisible && inventoryOverlay != null && !isBattleActive) {
            renderDimmingOverlay();
            renderUIWithViewport(inventoryStage, delta);
        }
        
        // CRITICAL: Render drop quantity dialog LAST so it appears on top of everything
        // This ensures the dialog is visible and not covered by inventory or any other UI
        // BUT: Only render if NOT in a battle (battles have their own overlay)
        if (!isBattleActive && inventoryVisible && inventoryOverlay != null) {
            Stage dialogStage = inventoryOverlay.getDialogStage();
            if (dialogStage != null && dialogStage.getActors().size > 0) {
                System.out.println("[GameScreen] Rendering dialogStage with " + dialogStage.getActors().size + " actors");
                renderUIWithViewport(dialogStage, delta);
            }
        }

        if (mapTransitionFade.isTransitioning()) {
            mapTransitionFade.render(batch);
        }
    }

    /**
     * Render a black dimming overlay covering the entire screen.
     * This creates visual separation between the game world and UI overlays.
     * Used when inventory, team viewer, or merchant shop UIs are open.
     */
    private void renderDimmingOverlay() {
        // Configure ShapeRenderer to use filled rectangle mode
        shapeRenderer.setProjectionMatrix(inventoryStage.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.BLACK);
        
        // Draw black rectangle covering entire virtual UI space (800x600)
        shapeRenderer.rect(0, 0, VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT);
        
        // End rendering
        shapeRenderer.end();
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
        
        // Target selection stage: maps physical screen to virtual 800x600 coordinate space
        if (inventoryOverlay != null) {
            Stage targetStage = inventoryOverlay.getTargetSelectionStage();
            if (targetStage != null) {
                targetStage.getViewport().update(width, height, true);
                targetStage.getCamera().update();
            }
            
            // Dialog stage: maps physical screen to virtual 800x600 coordinate space
            Stage dialogStage = inventoryOverlay.getDialogStage();
            if (dialogStage != null) {
                dialogStage.getViewport().update(width, height, true);
                dialogStage.getCamera().update();
            }
        }
        
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
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (inventoryOverlay != null) {
            Stage targetStage = inventoryOverlay.getTargetSelectionStage();
            if (targetStage != null) {
                targetStage.dispose();
            }
            Stage dialogStage = inventoryOverlay.getDialogStage();
            if (dialogStage != null) {
                dialogStage.dispose();
            }
        }
    }

    private void syncSystemStates() {
        boolean battleLocked = battleService.hasBattleSession();
        boolean dialogueLocked = interactionSystem.isDialogueVisible();
        boolean merchantLocked = merchantShopVisible;
        boolean mapTransitionLocked = mapTransitionFade.isTransitioning();
        boolean shouldEnableExploration = !battleLocked && !dialogueLocked && !merchantLocked && !mapTransitionLocked;
        if (shouldEnableExploration == explorationSystemsEnabled) {
            interactionSystem.setProcessing(!battleLocked && !merchantLocked);
            return;
        }

        explorationSystemsEnabled = shouldEnableExploration;
        engine.getSystem(PlayerInputSystem.class).setProcessing(shouldEnableExploration);
        interactionSystem.setProcessing(!battleLocked && !merchantLocked);
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
            inventoryVisible = false;
            if (inventoryOverlay != null) {
                inventoryOverlay.getDialogStage().clear();
            }
            inventoryStage.clear();
            Gdx.input.setInputProcessor(null);
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

        OrthographicCamera camera = game.getCamera();
        camera.position.set(spawnX + size.x / 2f, spawnY + size.y / 2f + 1f, camera.position.z);
        camera.update();

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

    private void toggleInventory() {
        // CRITICAL: Prevent inventory from opening during battle
        if (battleService.hasBattleSession()) {
            System.out.println("[GameScreen] Inventory toggle blocked - battle active");
            return;
        }
        
        // Switch to the full-screen inventory screen
        if (inventoryScreen != null) {
            System.out.println("[GameScreen] Opening full-screen inventory");
            game.setScreen(InventoryScreen.class);
        }
    }

    private void toggleTeamViewer() {
        // CRITICAL: Prevent team viewer from opening during battle
        if (battleService.hasBattleSession()) {
            System.out.println("[GameScreen] Team Viewer toggle blocked - battle active");
            return;
        }
        
        // Toggle team viewer visibility
        if (!teamViewerVisible) {
            // Show team viewer
            System.out.println("[GameScreen] Opening Team Viewer");
            List<Clawkin> party = playerBattleState.getParty();
            teamViewerScreen = new TeamViewerScreen(inventoryStage, party, uiFont);
            teamViewerScreen.setOnBackPressed(this::toggleTeamViewer);
            // Show which clawkin is currently the active fighter (gold highlight)
            teamViewerScreen.setActiveFighterIndex(playerBattleState.getActiveClawkinIndex());
            // Persist any active-fighter changes the player makes back to battle state
            teamViewerScreen.setOnActiveFighterSet(idx -> playerBattleState.setActiveClawkinIndex(idx));
            // Open standalone summary when SUMMARY action is confirmed.
            teamViewerScreen.setOnSummaryRequested(this::openSummaryFromTeamViewer);
            inventoryStage.clear();
            inventoryStage.addActor(teamViewerScreen.getRootTable());
            
            // CRITICAL FIX: Use the InputMultiplexer created by TeamViewerScreen
            // Do NOT override with just inventoryStage - this breaks keyboard input
            Gdx.input.setInputProcessor(teamViewerScreen.getInputMultiplexer());
            System.out.println("[GameScreen] Set input processor to TeamViewer's InputMultiplexer");
            
            teamViewerVisible = true;
            summaryVisible = false;
            isPaused = true;
        } else {
            // Close team viewer
            System.out.println("[GameScreen] Closing Team Viewer");
            if (summaryScreen != null) {
                summaryScreen.dispose();
                summaryScreen = null;
            }
            summaryVisible = false;
            inventoryStage.clear();
            Gdx.input.setInputProcessor(null);
            teamViewerVisible = false;
            isPaused = false;
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

