package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.util.List;

import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.DialogueSoundManager;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.ui.DialogueBoxRenderer;

/**
 * Coordinates the full battle presentation layer:
 *
 * <pre>
 *   checkCollision()           – EncounterDetectionSystem (Rectangle.contains)
 *       ↓ collision detected
 *   startTransition()          – BattleOverlay.update() detects new session,
 *                                freezes exploration, starts BattleTransition
 *       ↓ screen fades to black
 *   playTransitionAnimation()  – BattleTransition.update() / render() each frame
 *       ↓ screen fully black  (isHudReadyToShow() fires once)
 *   startBattle()              – showBattleHud() called behind the black overlay
 *       ↓ overlay fades out
 *   Scene2D battle HUD visible – BattleHud drawn every frame
 * </pre>
 *
 * <h3>Render order per frame (battle active)</h3>
 * <ol>
 *   <li>BattleHud (Stage) — background image + icon buttons</li>
 *   <li>SpriteBatch text — HP / phase / log</li>
 *   <li>BattleTransition — black fade overlay (on top of everything)</li>
 * </ol>
 */
public class BattleOverlay implements Disposable {

    private enum DialogueFlowPhase {
        NONE,
        PLAYER_RESULT,
        ENEMY_RESULT,
        RUN_CONFIRMATION,
        SWITCH_CONFIRMATION
    }

    private static final int SKIN_FONT_SIZE = 12;

    private final Main game;
    private final DialogueBoxRenderer dialogueBoxRenderer;
    private final BitmapFont skinFont;
    private final Matrix4 uiProjection;
    private final BattleTransition transition;

    /** Scene2D HUD — null until {@link #init} is called. */
    private BattleHud battleHud;

    /** Stored from init — used during HP sync to access active clawkin. */
    private PlayerBattleState playerBattleState;

    /** Scene2D Skin for UI widgets */
    private Skin skin;

    /** True once the HUD has been shown (transition finished or skipped). */
    private boolean inBattle = false;

    /**
     * True while we are waiting for a session to start so we can fire the
     * transition exactly once per encounter.
     */
    private boolean transitionPending = false;

    private DialogueFlowPhase dialogueFlowPhase = DialogueFlowPhase.NONE;
    private boolean dialogueVisible = false;
    private String dialogueSpeakerName = "";
    private String dialogueFullText = "";
    private List<BattleTextSpan> dialogueSpans = List.of();
    private float dialogueVisibleChars = 0f;
    private static final float DIALOGUE_TYPEWRITER_CHARS_PER_SECOND = 44f;
    private final DialogueSoundManager dialogueSoundManager = new DialogueSoundManager();
    private BattleActionSfxHandler battleActionSfxHandler;

    /** True when inventory is open from battle. */
    private boolean inventoryOpen = false;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public BattleOverlay(Main game, DialogueBoxRenderer dialogueBoxRenderer) {
        this.game = game;
        this.dialogueBoxRenderer = dialogueBoxRenderer;
        FreeTypeFontGenerator generator =
                new FreeTypeFontGenerator(Gdx.files.internal(DialogueBoxRenderer.DIALOGUE_FONT_PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        DialogueBoxRenderer.applyEarthboundStyle(parameter, SKIN_FONT_SIZE);
        this.skinFont = generator.generateFont(parameter);
        generator.dispose();

        this.uiProjection = new Matrix4();
        this.transition   = new BattleTransition();
    }

    // -----------------------------------------------------------------------
    // Initialisation
    // -----------------------------------------------------------------------

    /**
     * Builds the {@link BattleHud} and wires button callbacks.
     * Call once from {@code GameScreen} after the engine and assets are ready.
     */
    public void init(AssetService assetService, BattleService battleService, PlayerBattleState playerBattleStateArg) {
        this.playerBattleState = playerBattleStateArg;
        this.battleActionSfxHandler = new BattleActionSfxHandler(game != null ? game.getAudioService() : null);
        // Create a minimal Skin with default styles for UI components
        this.skin = new Skin();
        BitmapFont defaultFont = new BitmapFont(); // Use default LibGDX bitmap font
        
        // Add font to skin
        skin.add("default-font", defaultFont);
        
        // Create basic drawable for backgrounds
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
        skin.add("default", drawable);
        
        // Create lighter drawable for knobs/scrollbars
        Pixmap pixmap2 = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap2.setColor(Color.GRAY);
        pixmap2.fill();
        Texture texture2 = new Texture(pixmap2);
        pixmap2.dispose();
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(texture2);
        skin.add("knob", knobDrawable);
        
        // Add Window.WindowStyle
        WindowStyle windowStyle = new WindowStyle(skinFont, Color.WHITE, drawable);
        skin.add("default", windowStyle, Window.WindowStyle.class);
        
        // Add TextButton.TextButtonStyle
        TextButtonStyle buttonStyle = new TextButtonStyle(drawable, drawable, drawable, skinFont);
        buttonStyle.fontColor = Color.WHITE;
        skin.add("default", buttonStyle, TextButton.TextButtonStyle.class);
        
        // Add Button.ButtonStyle
        ButtonStyle bStyle = new ButtonStyle(drawable, drawable, drawable);
        skin.add("default", bStyle, ButtonStyle.class);
        
        // Add Label.LabelStyle
        LabelStyle labelStyle = new LabelStyle(skinFont, Color.WHITE);
        skin.add("default", labelStyle, Label.LabelStyle.class);
        
        // Add Slider.SliderStyle
        SliderStyle sliderStyle = new SliderStyle(drawable, knobDrawable);
        skin.add("default-horizontal", sliderStyle, SliderStyle.class);
        
        // Add List.ListStyle
        ListStyle listStyle = new ListStyle(skinFont, Color.WHITE, Color.GRAY, drawable);
        skin.add("default", listStyle, ListStyle.class);
        
        // Add ScrollPane.ScrollPaneStyle
        ScrollPaneStyle scrollPaneStyle = new ScrollPaneStyle(drawable, knobDrawable, knobDrawable, drawable, drawable);
        skin.add("default", scrollPaneStyle, ScrollPaneStyle.class);
        
        this.battleHud = new BattleHud(assetService);

        // Optional initial value sync (if you have real values available)
        // battleHud.setBossHp(100f, 100f);

        // Button1 -> Basic Attack
        battleHud.setOnAttack(() -> submitPlayerSkillAndOpenDialogue(battleService, 1));

        // Button2 -> Strong Attack
        battleHud.setOnDefend(() -> submitPlayerSkillAndOpenDialogue(battleService, 2));

        // Button3 -> Utility / Heal effect
        battleHud.setOnSpecial(() -> submitPlayerSkillAndOpenDialogue(battleService, 3));

        // Button4 -> Special Attack
        battleHud.setOnItem(() -> submitPlayerSkillAndOpenDialogue(battleService, 3));

        // Inventory button -> Open inventory screen
        battleHud.setOnInventory(() -> openInventoryScreen());

        // Flee button -> Attempt to flee from battle
        battleHud.setOnFlee(() -> {
            if (battleService != null) {
                battleService.submitEscapeAction();
                BattleStateMachine machine = battleService.getBattleStateMachine();
                if (machine != null) {
                    if (battleActionSfxHandler != null) {
                        battleActionSfxHandler.playEscapeAction();
                    }
                    openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.PLAYER_RESULT);
                }
            }
        });
    }

    /** Forward resize events so the Stage viewport stays correct. */
    public void resize(int width, int height) {
        if (battleHud != null) battleHud.resize(width, height);
    }

    /**
     * Get the UI Skin for creating inventory and other UI overlays.
     *
     * @return the Skin
     */
    public Skin getSkin() {
        return skin;
    }

    // -----------------------------------------------------------------------
    // Per-frame update  (call BEFORE BattleService.update)
    // -----------------------------------------------------------------------

    /**
     * Drives the transition state machine and handles keyboard battle input.
     *
     * <p>State flow:
     * <ol>
     *   <li>No session → reset everything.</li>
     *   <li>New session detected → {@link #startTransition()}.</li>
     *   <li>Transition playing → {@link #updateTransition}; when the screen
     *       goes fully black, {@link #startBattle()} is called.</li>
     *   <li>Transition finished + HUD visible → normal battle input.</li>
     * </ol>
     */
    public void update(BattleService battleService, float delta) {
        if (!battleService.hasBattleSession()) {
            // Session ended — tear everything down
            if (inBattle || transitionPending || transition.isTransitioning()) {
                hideBattleHud();
                transitionPending = false;
                resetDialogueFlow();
            }
            return;
        }

        // ── New session just started ────────────────────────────────────────
        if (!inBattle && !transitionPending && !transition.isTransitioning()) {
            startTransition();
            return;
        }

        // ── Transition in progress ─────────────────────────────────────────
        if (transition.isTransitioning()) {
            updateTransition(delta);
            // At the black-screen moment: show the HUD behind the overlay
            if (transition.isHudReadyToShow()) {
                startBattle(battleService);
            }
            return;
        }

        // ── Transition finished — handle battle input ──────────────────────
        if (!inBattle) return;

        BattleStateMachine battle = battleService.getBattleStateMachine();
        syncHudHpFromBattleState(battle);

        if (dialogueVisible) {
            updateTypewriter(delta);
            // Check for interaction (Z/Space/Enter) OR cancel (X/Escape)
            if (isInteractionPressed() || Gdx.input.isKeyJustPressed(Keys.X) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                handleDialogueAdvance(battleService);
            }
            return;
        }

        if (battle.canAcceptPlayerAction()) {
            // Keyboard input for battle actions
            if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
                battleHud.triggerAttack();
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
                battleHud.triggerDefend();
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
                battleHud.triggerSpecial();
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
                battleHud.triggerItem();
            } else if (isInteractionPressed()) {
                battleHud.triggerAttack();
            } else if (Gdx.input.isKeyJustPressed(Keys.E)) {
                // Toggle inventory
                toggleInventory();
            } else if (Gdx.input.isKeyJustPressed(Keys.X)) {
                // Show run confirmation prompt
                if (battleHud.isWildBattle()) {
                    showRunConfirmation();
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.UP)) {
                // Navigate Clawkin selection up
                battleHud.moveSelectionUp();
            } else if (Gdx.input.isKeyJustPressed(Keys.DOWN)) {
                // Navigate Clawkin selection down
                battleHud.moveSelectionDown();
            } else if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                // Confirm Clawkin switch
                if (battleHud.canSwitchToHighlighted()) {
                    showSwitchConfirmation();
                }
            }
            return;
        }

        if (!battleService.isBattleActive() && isInteractionPressed()) {
            battleService.closeBattleSession();
        }
    }

    // -----------------------------------------------------------------------
    // Transition helpers
    // -----------------------------------------------------------------------

    /**
     * Step 1 — called the frame a new battle session is detected.
     * Marks a transition as pending and starts the fade-to-black.
     */
    private void startTransition() {
        transitionPending = true;
        transition.start();
    }

    /**
     * Step 2 — called every frame while the transition overlay is playing.
     *
     * @param delta seconds since last frame
     */
    private void updateTransition(float delta) {
        transition.update(delta);
    }

    /**
     * Step 3 — called exactly once when the screen is fully black.
     * Shows the battle HUD behind the opaque overlay so it is ready when
     * the overlay fades out.
     */
    private void startBattle(BattleService battleService) {
        transitionPending = false;
        resetDialogueFlow();
        showBattleHud();
        
        // Set wild battle flag (for now, assume all battles are wild)
        // TODO: Determine from BattleContext if this is a trainer battle
        if (battleHud != null) {
            battleHud.setWildBattle(true);
        }
        
        if (battleService != null) {
            syncHudHpFromBattleState(battleService.getBattleStateMachine());
        }
    }

    private void syncHudHpFromBattleState(BattleStateMachine battle) {
        if (battleHud == null || battle == null) return;

        BattleContext ctx = battle.getContext();

        BattleUnit ally = battle.firstAlly();
        if (ally != null && playerBattleState != null) {
            Clawkin activeClawkin = playerBattleState.getActiveClawkin();
            int activeIndex = playerBattleState.getActiveClawkinIndex();
            battleHud.updateActiveClawkin(activeClawkin);

            // Use live battle-unit HP so the HUD always reflects damage immediately.
            battleHud.setPlayerHp(ally.getHp(), ally.getMaxHp());
            if (activeClawkin != null) {
                activeClawkin.setCurrentHp(ally.getHp());
            }
            
            // Update Clawkin container with party data
            List<Clawkin> party = playerBattleState.getParty();
            battleHud.updateClawkinContainer(party);

            // Use PlayerBattleState index as source of truth.
            if (activeIndex >= 0) {
                battleHud.setActiveClawkinIndex(activeIndex);
            }
        }

        BattleUnit enemy = battle.firstEnemy();
        if (enemy != null) {
            battleHud.setBossHp(enemy.getHp(), enemy.getMaxHp());
            if (ctx != null) {
                battleHud.updateEnemyCombatant(
                        ctx.getEncounterId(),
                        ctx.getEnemyDisplayName(),
                        ctx.getEnemyPortraitPath());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Rendering  (call AFTER engine.update)
    // -----------------------------------------------------------------------

    /**
     * Full render pass for one frame.
     *
     * <p>Draw order:
     * <ol>
     *   <li>{@link #renderBattleHUD()} — Stage (background + buttons) at the bottom.</li>
     *   <li>SpriteBatch text — HP / phase / log on top of the background.</li>
     *   <li>{@link #renderTransition(Batch)} — black fade overlay on top of everything.</li>
     * </ol>
     */
    public void render(Batch batch, BattleService battleService) {
        boolean hasSession  = battleService.hasBattleSession();
        boolean transitioning = transition.isTransitioning();

        if (!hasSession && !transitioning) return;

        // 1. Battle HUD (drawn only once the HUD has been initialised)
        if (inBattle) {
            renderBattleHUD();
        }

        // 2. Stat text — disabled; stats now shown via UI icons in BattleHud
        if (inBattle && hasSession && dialogueVisible) {
            renderDialogueBox(batch);
        }

        // 3. Transition overlay — always on top
        if (transitioning) {
            renderTransition(batch);
        }
    }



    /**
     * Draws the Scene2D Stage (battle background image + icon buttons).
     */
    private void renderBattleHUD() {
        if (battleHud != null) {
            battleHud.render();
        }
    }

    /**
     * Draws the full-screen black fade overlay.
     * Called every frame while {@link BattleTransition#isTransitioning()} is true.
     */
    private void renderTransition(Batch batch) {
        uiProjection.setToOrtho2D(0f, 0f,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.setProjectionMatrix(uiProjection);
        transition.render(batch);
    }

    private void renderDialogueBox(Batch batch) {
        dialogueBoxRenderer.renderBattleLog(
                batch,
                dialogueSpeakerName,
                dialogueFullText,
                dialogueSpans,
                (int) dialogueVisibleChars,
                Interactible.DialoguePosition.BOTTOM);
    }

    private void submitPlayerSkillAndOpenDialogue(BattleService battleService, int skillSlot) {
        if (battleService == null) {
            return;
        }

        BattleStateMachine machine = battleService.getBattleStateMachine();
        if (machine == null || !machine.canAcceptPlayerAction() || dialogueVisible) {
            return;
        }

        battleService.submitPlayerSkill(skillSlot);
        if (battleActionSfxHandler != null) {
            battleActionSfxHandler.playForPlayerAction(skillSlot, machine.getLastLogSpans());
        }
        openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.PLAYER_RESULT);
    }

    private void handleDialogueAdvance(BattleService battleService) {
        if (!isDialogueFullyRevealed()) {
            dialogueVisibleChars = dialogueFullText.length();
            return;
        }

        BattleStateMachine machine = battleService.getBattleStateMachine();
        if (machine == null) {
            resetDialogueFlow();
            return;
        }

        // Handle run confirmation
        if (dialogueFlowPhase == DialogueFlowPhase.RUN_CONFIRMATION) {
            // Check for Yes (Z/Space/Enter) or No (X/Escape)
            if (isInteractionPressed()) {
                // Confirmed - attempt to run
                battleService.submitEscapeAction();
                if (battleActionSfxHandler != null) {
                    battleActionSfxHandler.playEscapeAction();
                }
                openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.PLAYER_RESULT);
            } else if (Gdx.input.isKeyJustPressed(Keys.X) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                // Cancelled - return to battle
                resetDialogueFlow();
            }
            return;
        }

        // Handle switch confirmation
        if (dialogueFlowPhase == DialogueFlowPhase.SWITCH_CONFIRMATION) {
            // Check for Yes (Z/Space/Enter) or No (X/Escape)
            if (isInteractionPressed()) {
                // Confirmed - switch Clawkin
                performClawkinSwitch(battleService);
                resetDialogueFlow();
            } else if (Gdx.input.isKeyJustPressed(Keys.X) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                // Cancelled - return to battle
                resetDialogueFlow();
            }
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.PLAYER_RESULT) {
            if (machine.canExecuteEnemyAction()) {
                battleService.resolveEnemyTurn();
                if (battleActionSfxHandler != null) {
                    battleActionSfxHandler.playForEnemyActionResult(machine.getLastLogSpans());
                }
                openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.ENEMY_RESULT);
                return;
            }
            resetDialogueFlow();
            if (!battleService.isBattleActive()) {
                battleService.closeBattleSession();
            }
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.ENEMY_RESULT) {
            resetDialogueFlow();
            if (!battleService.isBattleActive()) {
                battleService.closeBattleSession();
            }
            return;
        }

        resetDialogueFlow();
        if (!battleService.isBattleActive()) {
            battleService.closeBattleSession();
        }
    }

    private void openDialogue(String speakerName, String text, List<BattleTextSpan> spans, DialogueFlowPhase phase) {
        dialogueFlowPhase = phase;
        dialogueVisible = true;
        dialogueSpeakerName = speakerName == null ? "" : speakerName;
        dialogueFullText = text == null ? "" : text;
        dialogueSpans = spans == null ? List.of() : List.copyOf(spans);
        dialogueVisibleChars = 0f;
        dialogueSoundManager.stop();
    }

    private void updateTypewriter(float delta) {
        if (!dialogueVisible || dialogueFullText.isEmpty()) {
            dialogueSoundManager.stop();
            return;
        }
        int previousVisible = (int) dialogueVisibleChars;
        dialogueVisibleChars = Math.min(
                dialogueFullText.length(),
                dialogueVisibleChars + (DIALOGUE_TYPEWRITER_CHARS_PER_SECOND * delta)
        );

        int currentVisible = (int) dialogueVisibleChars;
        if (currentVisible > previousVisible) {
            playDialogueSounds(previousVisible, currentVisible);
        }
    }

    private boolean isDialogueFullyRevealed() {
        return dialogueVisibleChars >= dialogueFullText.length();
    }

    private void resetDialogueFlow() {
        dialogueFlowPhase = DialogueFlowPhase.NONE;
        dialogueVisible = false;
        dialogueSpeakerName = "";
        dialogueFullText = "";
        dialogueSpans = List.of();
        dialogueVisibleChars = 0f;
        dialogueSoundManager.stop();
    }

    private void playDialogueSounds(int startIndex, int endIndex) {
        if (dialogueFullText == null || dialogueFullText.isEmpty()) {
            return;
        }

        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(dialogueFullText.length(), endIndex);
        for (int i = safeStart; i < safeEnd; i++) {
            char c = dialogueFullText.charAt(i);
            dialogueSoundManager.onCharacterRevealed(c, i);
        }
    }

    // -----------------------------------------------------------------------
    // HUD show / hide
    // -----------------------------------------------------------------------

    private void showBattleHud() {
        inBattle = true;
        if (battleHud != null) battleHud.show();
    }

    private void hideBattleHud() {
        inBattle = false;
        if (battleHud != null) battleHud.hide();
    }

    // -----------------------------------------------------------------------
    // Inventory Screen Integration
    // -----------------------------------------------------------------------

    /**
     * Opens the inventory screen from battle.
     * Pauses battle input and switches to the inventory screen.
     * The inventory screen will automatically return to the game screen when closed.
     */
    private void openInventoryScreen() {
        if (game == null) {
            Gdx.app.log("BattleOverlay", "Cannot open inventory: game reference is null");
            return;
        }

        // Store the current input processor to restore it later
        // The inventory screen will handle its own input
        Gdx.app.log("BattleOverlay", "Opening inventory screen from battle");
        
        inventoryOpen = true;
        
        // Switch to inventory screen (uses cached screen from Main)
        game.setScreen(github.dluckycompany.clawkins.ui.InventoryScreen.class);
    }

    /**
     * Toggles the inventory screen open/closed.
     * Press E to open, press E again to close.
     */
    private void toggleInventory() {
        if (inventoryOpen) {
            // Close inventory - return to battle
            resumeFromInventory();
            inventoryOpen = false;
        } else {
            // Open inventory
            openInventoryScreen();
        }
    }

    /**
     * Shows a confirmation prompt before running from battle.
     * Player must confirm with Z/Space/Enter or cancel with X/Escape.
     */
    private void showRunConfirmation() {
        String confirmText = "Are you sure you want to run?\n[Z] Yes  [X] No";
        openDialogue(null, confirmText, List.of(), DialogueFlowPhase.RUN_CONFIRMATION);
    }

    /**
     * Shows a confirmation prompt before switching Clawkins.
     * Switching ends the player's turn.
     * Player must confirm with Z/Space/Enter or cancel with X/Escape.
     */
    private void showSwitchConfirmation() {
        if (battleHud == null) return;
        
        String clawkinName = battleHud.getHighlightedClawkinName();
        if (clawkinName == null) {
            clawkinName = "this Clawkin";
        }
        
        String confirmText = "Switch to " + clawkinName + "?\nThis will end your turn!\n\n[Z] Yes  [X] No";
        openDialogue(null, confirmText, List.of(), DialogueFlowPhase.SWITCH_CONFIRMATION);
    }

    /**
     * Performs the actual Clawkin switch after confirmation.
     * Updates the active Clawkin in the player battle state.
     * Switching ends the player's turn.
     */
    private void performClawkinSwitch(BattleService battleService) {
        if (battleHud == null || playerBattleState == null) return;
        
        int newIndex = battleHud.getHighlightedClawkinIndex();
        Clawkin newClawkin = battleHud.getClawkinAtSlot(newIndex);
        
        if (newClawkin == null) {
            Gdx.app.log("BattleOverlay", "Cannot switch: Clawkin not found at slot " + newIndex);
            return;
        }

        if (battleService == null || !battleService.switchActiveClawkin(newIndex)) {
            if (game != null && game.getAudioService() != null) {
                game.getAudioService().playSound(SoundEffect.UI_ERROR);
            }
            return;
        }

        if (battleActionSfxHandler != null) {
            battleActionSfxHandler.playSwitchAction();
        }

        // Update HUD to reflect new active clawkin immediately.
        battleHud.setActiveClawkinIndex(newIndex);
        battleHud.updateActiveClawkin(newClawkin);

        // Sync HP display after switching.
        BattleStateMachine machine = battleService.getBattleStateMachine();
        if (machine != null) {
            syncHudHpFromBattleState(machine);
        }
        
        Gdx.app.log("BattleOverlay", "Switched to " + newClawkin.getName());
        
        // Switching Clawkin ends the player's turn - trigger enemy action
        if (machine != null && machine.canExecuteEnemyAction()) {
            battleService.resolveEnemyTurn();
            if (battleActionSfxHandler != null) {
                battleActionSfxHandler.playForEnemyActionResult(machine.getLastLogSpans());
            }
            openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.ENEMY_RESULT);
        }
    }

    /**
     * Called when returning from inventory screen to battle.
     * Restores battle HUD input processor.
     */
    public void resumeFromInventory() {
        if (inBattle && battleHud != null) {
            // Restore battle HUD input
            battleHud.show();
            Gdx.app.log("BattleOverlay", "Resumed battle from inventory");
        }
    }

    /**
     * Returns true if currently in an active battle.
     */
    public boolean isInBattle() {
        return inBattle;
    }

    // -----------------------------------------------------------------------
    // Disposable
    // -----------------------------------------------------------------------

    @Override
    public void dispose() {
        skinFont.dispose();
        transition.dispose();
        if (battleHud != null) battleHud.dispose();
        if (skin != null) skin.dispose();
        dialogueSoundManager.dispose();
    }

    // -----------------------------------------------------------------------
    // Static helpers
    // -----------------------------------------------------------------------

    private static boolean isInteractionPressed() {
        return Gdx.input.isKeyJustPressed(Keys.Z)
                || Gdx.input.isKeyJustPressed(Keys.SPACE);
    }

}
