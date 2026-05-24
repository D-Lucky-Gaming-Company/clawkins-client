package github.dluckycompany.clawkins.ui;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;

import github.dluckycompany.clawkins.GameScreen;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;

/**
 * InventoryScreen - Full-Screen Inventory UI with Game Pause
 * 
 * Displays a full-screen inventory with orange/white/black color scheme.
 * Pauses the game while inventory is open.
 */
public class InventoryScreen implements Screen {

    private final Main game;
    private final Inventory playerInventory;
    private final GameScreen previousGameScreen;  // Reference to pause the game

    // Core rendering components
    private final Stage stage;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final BitmapFont font;

    // Input handling
    private InputProcessor previousInputProcessor;
    private InventoryUI inventoryUI;

    /**
     * Constructor
     */
    public InventoryScreen(Main game, Inventory playerInventory, GameScreen gameScreen) {
        this.game = game;
        this.playerInventory = playerInventory;
        this.previousGameScreen = gameScreen;

        // Create viewport and stage
        this.viewport = new FitViewport(800, 600);
        this.stage = new Stage(viewport);
        this.stage.clear();

        this.batch = new SpriteBatch();
        this.font = new BitmapFont();

        Gdx.app.log("InventoryScreen", "Constructor: Stage cleared");
    }

    @Override
    public void show() {
        // Pause the previous game screen
        if (previousGameScreen != null) {
            previousGameScreen.setPaused(true);
        }

        // Clear any previous stage content
        stage.clear();

        // Initialize assets
        if (!UIAssets.isInitialized()) {
            UIAssets.initialize();
        }

        // Get party, skin, and wallet from the previous game screen
        List<Clawkin> party = previousGameScreen.getPlayerBattleState().getParty();
        Skin skin = previousGameScreen.getBattleOverlay().getSkin();
        github.dluckycompany.clawkins.item.Wallet wallet = previousGameScreen.getPlayerBattleState().getWallet();
        boolean openedFromBattle = previousGameScreen.getBattleOverlay() != null
                && previousGameScreen.getBattleOverlay().isInBattle();

        // ============================================================
        // CRITICAL: Initialize Skin with required fonts before creating dialogs
        // ============================================================
        // This MUST happen before InventoryUI is created, since it may create
        // dialogs that depend on registered fonts
        UISkinManager.initializeSkin(skin, font);
        Gdx.app.log("InventoryScreen", "Skin initialized with fonts");

        // Create InventoryUI with full-screen layout
        inventoryUI = new InventoryUI(stage, font, playerInventory, party, skin, wallet, game.getAudioService(), openedFromBattle);
        inventoryUI.buildLayout();
        
        // Wire up back button to return to game screen
        inventoryUI.setOnBackPressed(() -> returnToGameScreen());

        // Wire up level-boost callback so Macaramboni updates shared XP in PlayerProgress
        inventoryUI.setOnLevelBoosted(levelsGained -> {
            if (previousGameScreen != null) {
                previousGameScreen.applySharedLevelBoost(levelsGained);
            }
        });

        if (openedFromBattle && previousGameScreen.getBattleOverlay() != null) {
            inventoryUI.setOnBattleItemUsed((item, target) ->
                    previousGameScreen.getBattleOverlay().notifyBattleItemUsed(item, target));
        }

        // Store previous input processor for restoration in hide()
        previousInputProcessor = Gdx.input.getInputProcessor();
        
        // Add a Stage-level InputListener for ESC key to exit inventory
        // Also consume the E key to prevent double-toggle with GameScreen
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                // Inventory list keyboard navigation (WASD + arrow keys)
                if (inventoryUI != null && inventoryUI.handleNavigationKey(keycode)) {
                    return true;
                }

                // Unconfirm/back keys exit inventory when not consumed by InventoryUI first.
                if (keycode == Input.Keys.ESCAPE
                        || keycode == Input.Keys.X
                        || keycode == Input.Keys.BACKSPACE
                        || keycode == Input.Keys.BUTTON_B) {
                    returnToGameScreen();
                    return true;
                }
                // E key is consumed/ignored while inventory is open
                // This prevents GameScreen from also processing it
                return keycode == Input.Keys.E;
            }
        });

        // Set the Stage as the input processor
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // Clear screen to black
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply viewport
        viewport.apply();

        // Update batch projection
        batch.setProjectionMatrix(viewport.getCamera().combined);

        // Update and draw stage
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        // Resume the previous game screen
        if (previousGameScreen != null) {
            previousGameScreen.setPaused(false);
        }

        // DON'T restore input processor here if we're in battle
        // The battle HUD will handle its own input restoration via resumeFromInventory()
        // Only restore if we're NOT in a battle (i.e., opened from exploration)
        boolean inBattle = previousGameScreen != null 
                && previousGameScreen.getBattleOverlay() != null 
                && previousGameScreen.getBattleOverlay().isInBattle();
        
        if (!inBattle) {
            // Restore the previous input processor (exploration mode)
            if (previousInputProcessor != null) {
                Gdx.input.setInputProcessor(previousInputProcessor);
            } else {
                Gdx.input.setInputProcessor(null);
            }
        }
        // If in battle, the input processor is already set by resumeFromInventory()

        // Clear the stage
        if (stage != null) {
            stage.clear();
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }

        if (batch != null) {
            batch.dispose();
        }

        // NOTE: DO NOT dispose font here!
        // The font is registered in the Skin via UISkinManager.initializeSkin()
        // The Skin is owned by BattleOverlay, which will dispose it (and the font) in its own dispose() method
        // Disposing the font here would cause a double-disposal crash
        // The font's lifetime is now managed by the Skin object
    }

    private void returnToGameScreen() {
        // Resume the game
        if (previousGameScreen != null) {
            previousGameScreen.setPaused(false);
            
            // If we're in a battle, restore the battle HUD input
            if (previousGameScreen.getBattleOverlay() != null) {
                previousGameScreen.getBattleOverlay().resumeFromInventory();
            }
        }
        
        // Return to the cached GameScreen
        game.setScreen(GameScreen.class);
    }

    public Stage getStage() {
        return stage;
    }

    public FitViewport getViewport() {
        return viewport;
    }
}
