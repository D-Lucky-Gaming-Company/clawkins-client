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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import github.dluckycompany.clawkins.GameScreen;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.DialogueSoundManager;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;
import github.dluckycompany.clawkins.character.SkillUnlockSystem;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.input.InputConventions;
import github.dluckycompany.clawkins.progress.PlayerProgress;
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
        VICTORY_REWARD,
        VICTORY_LEVEL_UP,
        VICTORY_MILESTONE,
        RUN_CONFIRMATION,
        SWITCH_CONFIRMATION,
        SKILL_CONFIRMATION,
        SKILL_STATS
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
    /** Cached service reference for syncing inventory-applied HP updates. */
    private BattleService battleService;

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
    private int skillConfirmationOptionIndex = 0; // 0=Yes, 1=No, 2=Stats
    private int pendingLevelUps = 0;
    private int pendingLevelUpTargetLevel = LevelSystem.MIN_LEVEL;
    private int pendingVictoryCoinReward = 0;
    private boolean pendingVictoryMilestone = false;
    private String pendingVictoryMilestoneText = "";
    /** Prevents double-applying {@link GameScreen#DEFAULT_BATTLE_XP_REWARD} if victory dialogue is reopened. */
    private boolean victoryXpGrantedThisSession = false;
    private static final int[] LEVEL_MILESTONE_THRESHOLDS = {10, 15, 20};
    /**
     * Per-boss coin overrides applied on victory.
     *
     * <p>How to add a new boss reward:<br>
     * 1. Uncomment (or add) the entry below using the encounter ID string.<br>
     * 2. Set the coin value to match the intended boss difficulty/prestige.<br>
     * 3. Add the matching XP entry in {@link github.dluckycompany.clawkins.GameScreen#BOSS_XP_REWARDS_BY_ENCOUNTER_ID}.
     *
     * <p>Current values:<br>
     * - boss_0 (Bert Jr.)  : 500 coins<br>
     * - boss_1 (Spartacus) : 650 coins<br>
     * - boss_2 (Cerberus)  : 750 coins
     */
    private static final Map<String, Integer> BOSS_COIN_REWARDS_BY_ENCOUNTER_ID = Map.ofEntries(
            Map.entry("boss_0_encounter", 500),
            Map.entry("boss_1_encounter", 650),
            Map.entry("boss_2_encounter", 750)
    );
    /** True when inventory is open from battle. */
    private boolean inventoryOpen = false;
    /** Small input guard after returning from inventory to avoid key bleed-through. */
    private int postInventoryInputBlockFrames = 0;

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
        this.battleService = battleService;
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

        // Button4 -> Reserved evolved skill slot (not implemented yet)
        battleHud.setOnItem(this::showEvolvedSkillUnavailable);

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
        
        // Clawkin icon clicked -> Show switch confirmation
        battleHud.setOnClawkinSelected(() -> {
            if (battleHud.canSwitchToHighlighted()) {
                showSwitchConfirmation();
            }
        });

        battleHud.setOnSkillSelectionChanged(() -> {
            if (game != null && game.getAudioService() != null) {
                game.getAudioService().playSound(SoundEffect.UI_SELECT);
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
                resetVictoryDialogueState();
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
            if (dialogueFlowPhase == DialogueFlowPhase.SKILL_CONFIRMATION || dialogueFlowPhase == DialogueFlowPhase.SKILL_STATS) {
                handleSkillOverlayInput(battleService);
                return;
            }
            updateTypewriter(delta);
            // Check for keyboard interaction OR cancel
            // For mouse clicks, only advance if NOT clicking on a UI element
            boolean keyboardInteraction = Gdx.input.isKeyJustPressed(Keys.Z) || Gdx.input.isKeyJustPressed(Keys.SPACE);
            boolean cancelPressed = Gdx.input.isKeyJustPressed(Keys.X) || Gdx.input.isKeyJustPressed(Keys.ESCAPE);
            boolean mouseClickedOutsideUI = Gdx.input.justTouched() && !isClickOnUIElement();
            
            if (keyboardInteraction || cancelPressed || mouseClickedOutsideUI) {
                handleDialogueAdvance(battleService);
            }
            return;
        }

        if (battle.canAcceptPlayerAction()) {
            if (postInventoryInputBlockFrames > 0) {
                postInventoryInputBlockFrames--;
                return;
            }
            // Keyboard input for battle actions
            if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
                if (battleHud.isSkillSlotEnabled(0)) {
                    battleHud.setSelectedSkillIndex(0, true);
                    battleHud.triggerAttack();
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
                if (battleHud.isSkillSlotEnabled(1)) {
                    battleHud.setSelectedSkillIndex(1, true);
                    battleHud.triggerDefend();
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
                if (battleHud.isSkillSlotEnabled(2)) {
                    battleHud.setSelectedSkillIndex(2, true);
                    battleHud.triggerSpecial();
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
                if (battleHud.isSkillSlotEnabled(3)) {
                    battleHud.setSelectedSkillIndex(3, true);
                    battleHud.triggerItem();
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                // Confirm Clawkin switch
                if (battleHud.canSwitchToHighlighted()) {
                    showSwitchConfirmation();
                }
            } else if (isBattleSkillSelectLeftPressed()) {
                battleHud.moveSelectedSkill(-1);
            } else if (isBattleSkillSelectRightPressed()) {
                battleHud.moveSelectedSkill(1);
            } else if (isBattleSkillConfirmPressed()) {
                showSkillConfirmation(battleService);
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
        resetVictoryDialogueState();
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

            // Sync Clawkin HP from BattleUnit (source of truth during battle)
            if (activeClawkin != null) {
                activeClawkin.setCurrentHp(ally.getHp());
                
                // Use Clawkin's maxHp (not BattleUnit's) for correct health bar display
                // BattleUnit's maxHp is fixed at construction, but Clawkin's maxHp is the real stat
                float currentHp = ally.getHp();
                float maxHp = activeClawkin.getMaxHp();
                battleHud.setPlayerHp(currentHp, maxHp);
                
                // Update EXP/Level display
                int level = activeClawkin.getLevel();
                PlayerProgress progress = resolvePlayerProgress();
                if (progress != null) {
                    battleHud.updateExpFromTotalExp(progress.getExperiencePoints());
                } else {
                    battleHud.updateExpFromLevel(level);
                }

                // Update skill button labels with actual skill names
                if (ctx != null && ctx.getSkillManager() != null) {
                    battleHud.updateSkillLabels(ctx.getSkillManager(), ally);
                }
            } else {
                // Fallback if no active Clawkin
                battleHud.setPlayerHp(ally.getHp(), ally.getMaxHp());
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
                        ctx.getEncounterTableId(),
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
        if (battleHud == null || battleHud.getStage() == null) {
            return;
        }
        if (dialogueFlowPhase == DialogueFlowPhase.SKILL_STATS) {
            dialogueBoxRenderer.renderPromptMarkupLarge(
                    batch,
                    battleHud.getStage().getViewport(),
                    dialogueFullText,
                    Interactible.DialoguePosition.BOTTOM);
            return;
        }
        if (dialogueFlowPhase == DialogueFlowPhase.SKILL_CONFIRMATION) {
            dialogueBoxRenderer.renderPromptMarkup(
                    batch,
                    battleHud.getStage().getViewport(),
                    dialogueFullText,
                    Interactible.DialoguePosition.BOTTOM);
            return;
        }
        dialogueBoxRenderer.renderBattleLog(
                batch,
                battleHud.getStage().getViewport(),
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
        String lastLog = machine.getLastLog();
        boolean cooldownRejected = machine.consumeLastPlayerSkillCooldownReject();
        if (cooldownRejected) {
            if (game != null && game.getAudioService() != null) {
                game.getAudioService().playSound(SoundEffect.FAILURE_1);
            }
        } else if (battleActionSfxHandler != null) {
            battleActionSfxHandler.playForPlayerAction(skillSlot, machine.getLastLogSpans());
        }
        openDialogue(null, lastLog, machine.getLastLogSpans(), DialogueFlowPhase.PLAYER_RESULT);
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

        if (dialogueFlowPhase == DialogueFlowPhase.VICTORY_REWARD) {
            if (pendingLevelUps > 0) {
                String levelUpText = buildLevelUpDialogueText(pendingLevelUps, pendingLevelUpTargetLevel);
                openDialogue(
                        null,
                        levelUpText,
                        buildLevelUpDialogueSpans(
                                levelUpText,
                                pendingLevelUps,
                                pendingLevelUpTargetLevel
                        ),
                        DialogueFlowPhase.VICTORY_LEVEL_UP
                );
                playLevelUpSound();
                pendingLevelUps = 0;
                return;
            }
            if (tryOpenVictoryMilestoneDialogue()) {
                return;
            }
            resetDialogueFlow();
            battleService.closeBattleSession();
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.VICTORY_LEVEL_UP) {
            if (tryOpenVictoryMilestoneDialogue()) {
                return;
            }
            resetDialogueFlow();
            battleService.closeBattleSession();
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.VICTORY_MILESTONE) {
            resetDialogueFlow();
            battleService.closeBattleSession();
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
                // Keep any follow-up dialogue (e.g. enemy action) opened by the switch flow.
                if (dialogueFlowPhase == DialogueFlowPhase.SWITCH_CONFIRMATION) {
                    resetDialogueFlow();
                }
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
                    battleActionSfxHandler.playForEnemyActionResult(machine.getLastLogSpans(), machine.getLastLog());
                }
                openDialogue(null, machine.getLastLog(), machine.getLastLogSpans(), DialogueFlowPhase.ENEMY_RESULT);
                return;
            }
            if (tryOpenVictoryDialogue(battleService, machine)) {
                return;
            }
            resetDialogueFlow();
            if (!battleService.isBattleActive()) {
                battleService.closeBattleSession();
            }
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.ENEMY_RESULT) {
            // Grant round EXP after each complete round
            grantRoundExp(battleService);

            if (tryOpenVictoryDialogue(battleService, machine)) {
                return;
            }
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

    private void openDialogueInstant(String speakerName, String text, List<BattleTextSpan> spans, DialogueFlowPhase phase) {
        openDialogue(speakerName, text, spans, phase);
        dialogueVisibleChars = dialogueFullText.length();
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

    private void resetVictoryDialogueState() {
        pendingLevelUps = 0;
        pendingLevelUpTargetLevel = LevelSystem.MIN_LEVEL;
        pendingVictoryCoinReward = 0;
        pendingVictoryMilestone = false;
        pendingVictoryMilestoneText = "";
        victoryXpGrantedThisSession = false;
    }

    private boolean tryOpenVictoryDialogue(BattleService battleService, BattleStateMachine machine) {
        if (battleService == null || machine == null) {
            return false;
        }
        if (battleService.isBattleActive() || machine.getPhase() != BattlePhase.VICTORY) {
            return false;
        }

        PlayerProgress progress = resolvePlayerProgress();
        GameScreen gameScreen = resolveGameScreen();
        int xpBeforeReward = progress != null ? progress.getExperiencePoints() : 0;
        BattleContext context = machine.getContext();
        String encounterId = context != null ? context.getEncounterId() : null;
        int xpAwarded = GameScreen.DEFAULT_BATTLE_XP_REWARD;
        if (gameScreen != null && progress != null && !victoryXpGrantedThisSession) {
            xpAwarded = gameScreen.applyVictoryExperienceReward(encounterId);
            victoryXpGrantedThisSession = true;
        }
        int currentXp = progress != null ? progress.getExperiencePoints() : xpBeforeReward;
        int beforeLevel = LevelSystem.calculateLevelFromExp(xpBeforeReward);
        int afterLevel = LevelSystem.calculateLevelFromExp(currentXp);
        pendingLevelUps = Math.max(0, afterLevel - beforeLevel);
        pendingLevelUpTargetLevel = afterLevel;
        int milestoneThreshold = highestCrossedLevelMilestoneThreshold(beforeLevel, afterLevel);
        boolean gainedSkill = partyGainedSkillsFromLevelUps(beforeLevel, afterLevel);
        pendingVictoryMilestone = milestoneThreshold >= 0 || gainedSkill;
        pendingVictoryMilestoneText =
                pendingVictoryMilestone
                        ? buildVictoryMilestoneDialogueText(milestoneThreshold, gainedSkill)
                        : "";
        int enemyLevel = LevelSystem.MIN_LEVEL;
        if (context != null) {
            enemyLevel = context.getEnemyLevel();
        }
        pendingVictoryCoinReward = resolveVictoryCoinReward(context, enemyLevel);
        if (playerBattleState != null && playerBattleState.getWallet() != null) {
            playerBattleState.getWallet().addMoney(pendingVictoryCoinReward);
        }
        if (gameScreen != null) {
            gameScreen.refreshHudWallet();
        }

        String rewardText = "Victory Rewards\n"
                + "XP Gained: +" + xpAwarded + "\n"
                + "Coins Gained: +" + pendingVictoryCoinReward + "\n"
                + "Items Earned: None";
        openDialogue(null, rewardText, List.of(), DialogueFlowPhase.VICTORY_REWARD);
        return true;
    }

    private GameScreen resolveGameScreen() {
        if (game == null) {
            return null;
        }
        try {
            return game.getScreen(GameScreen.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private PlayerProgress resolvePlayerProgress() {
        GameScreen screen = resolveGameScreen();
        return screen != null ? screen.getPlayerProgress() : null;
    }

    private int resolveVictoryCoinReward(BattleContext context, int enemyLevel) {
        if (context != null) {
            Integer bossReward = BOSS_COIN_REWARDS_BY_ENCOUNTER_ID.get(context.getEncounterId());
            if (bossReward != null) {
                return Math.max(0, bossReward);
            }
        }
        int baseReward = LevelSystem.calculateMoneyReward(enemyLevel);
        if (enemyLevel >= LevelSystem.MAX_LEVEL) {
            return rollMaxLevelReward(baseReward);
        }
        return baseReward;
    }

    private int rollMaxLevelReward(int baseReward) {
        if (baseReward <= 0) {
            return 0;
        }
        double factor = ThreadLocalRandom.current().nextDouble(0.25d, 1.0000001d);
        return Math.max(1, (int) Math.round(baseReward * factor));
    }

    private String buildLevelUpDialogueText(int levelsGained, int targetLevel) {
        if (levelsGained <= 1) {
            return "Level Up!\nYou reached Level " + targetLevel + "!";
        }
        return "Level Up!\nYou gained " + levelsGained + " levels!\nNow at Level " + targetLevel + "!";
    }

    private List<BattleTextSpan> buildLevelUpDialogueSpans(String text, int levelsGained, int targetLevel) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        ArrayList<BattleTextSpan> spans = new ArrayList<>();

        int headlineStart = text.indexOf("Level Up!");
        if (headlineStart >= 0) {
            spans.add(new BattleTextSpan(headlineStart, headlineStart + "Level Up!".length(), BattleTextRole.NAME));
        }

        if (levelsGained > 1) {
            String gainedToken = Integer.toString(levelsGained);
            int gainedStart = text.indexOf(gainedToken + " levels");
            if (gainedStart >= 0) {
                spans.add(new BattleTextSpan(gainedStart, gainedStart + gainedToken.length(), BattleTextRole.HEAL));
            }
        }

        String targetToken = "Level " + targetLevel;
        int targetStart = text.lastIndexOf(targetToken);
        if (targetStart >= 0) {
            spans.add(new BattleTextSpan(targetStart, targetStart + targetToken.length(), BattleTextRole.HEAL));
        }

        return List.copyOf(spans);
    }

    private void playLevelUpSound() {
        if (game != null && game.getAudioService() != null) {
            game.getAudioService().playSound(SoundEffect.LEVEL_UP);
        }
    }

    /**
     * Opens the post-victory milestone dialogue if one was queued. Returns true when the flow was continued.
     */
    private boolean tryOpenVictoryMilestoneDialogue() {
        if (!pendingVictoryMilestone || pendingVictoryMilestoneText == null || pendingVictoryMilestoneText.isEmpty()) {
            return false;
        }
        openDialogue(null, pendingVictoryMilestoneText, List.of(), DialogueFlowPhase.VICTORY_MILESTONE);
        playMilestoneSound();
        pendingVictoryMilestone = false;
        pendingVictoryMilestoneText = "";
        return true;
    }

    private void playMilestoneSound() {
        if (game != null && game.getAudioService() != null) {
            game.getAudioService().playSound(SoundEffect.MILESTONE);
        }
    }

    private static int highestCrossedLevelMilestoneThreshold(int beforeLevel, int afterLevel) {
        int highest = -1;
        if (afterLevel <= beforeLevel) {
            return -1;
        }
        for (int threshold : LEVEL_MILESTONE_THRESHOLDS) {
            if (beforeLevel < threshold && afterLevel >= threshold) {
                highest = threshold;
            }
        }
        return highest;
    }

    private boolean partyGainedSkillsFromLevelUps(int beforeLevel, int afterLevel) {
        if (playerBattleState == null || afterLevel <= beforeLevel) {
            return false;
        }
        List<Clawkin> party = playerBattleState.getParty();
        if (party == null || party.isEmpty()) {
            return false;
        }
        for (Clawkin clawkin : party) {
            if (clawkin == null || clawkin.getId() == null || clawkin.getId().isBlank()) {
                continue;
            }
            for (int lv = beforeLevel + 1; lv <= afterLevel; lv++) {
                if (!SkillUnlockSystem.getSkillsUnlockedAtLevel(clawkin.getId(), lv).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String buildVictoryMilestoneDialogueText(int milestoneThreshold, boolean gainedSkill) {
        boolean levelMilestone = milestoneThreshold >= 0;
        if (levelMilestone && gainedSkill) {
            return "Milestone!\nYou reached Level " + milestoneThreshold + " and learned a new skill!";
        }
        if (levelMilestone) {
            return "Milestone!\nYou reached Level " + milestoneThreshold + "!";
        }
        if (gainedSkill) {
            return "Milestone!\nYou learned a new skill!";
        }
        return "";
    }
    
    /**
     * Grants round EXP to the active Clawkin after each complete round.
     * Updates the BattleHUD EXP bar display.
     * 
     * Note: Full EXP/level-up integration requires ClawkinData persistence.
     * For now, this updates the visual display only.
     * 
     * @param battleService The battle service
     */
    private void grantRoundExp(BattleService battleService) {
        if (battleService == null || playerBattleState == null || battleHud == null) {
            return;
        }
        
        BattleStateMachine machine = battleService.getBattleStateMachine();
        if (machine == null) {
            return;
        }
        
        // Get round EXP from battle state machine
        int roundExp = github.dluckycompany.clawkins.character.LevelSystem.calculateRoundExpReward();
        if (roundExp <= 0) {
            return;
        }
        
        // Get active Clawkin
        github.dluckycompany.clawkins.character.Clawkin activeClawkin = playerBattleState.getActiveClawkin();
        if (activeClawkin == null) {
            return;
        }
        
        // Log round EXP grant
        Gdx.app.log("BattleOverlay", "Granted " + roundExp + " round EXP to " + activeClawkin.getName() + 
                " (Round " + machine.getCurrentRound() + ")");
        
        // TODO: Integrate with ClawkinData persistence system when available
        // For now, the round EXP is tracked in BattleStateMachine.roundExpAccumulated
        // and will be awarded at battle end along with victory EXP
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

    private void showSkillConfirmation(BattleService battleService) {
        if (battleHud != null && battleHud.getSelectedSkillIndex() == 3) {
            showEvolvedSkillUnavailable();
            return;
        }
        skillConfirmationOptionIndex = 0;
        String text = buildSkillConfirmationText(battleService);
        openDialogueInstant(null, text, List.of(), DialogueFlowPhase.SKILL_CONFIRMATION);
    }

    private void showEvolvedSkillUnavailable() {
        openDialogueInstant(
                null,
                "Evolved skill is not implemented yet.\n\n[Z/Space/X] Close",
                List.of(),
                DialogueFlowPhase.NONE
        );
    }

    private void showSkillStats(BattleService battleService) {
        String text = buildSkillStatsText(battleService);
        openDialogueInstant(null, text, List.of(), DialogueFlowPhase.SKILL_STATS);
    }

    private void handleSkillOverlayInput(BattleService battleService) {
        if (dialogueFlowPhase == DialogueFlowPhase.SKILL_CONFIRMATION) {
            if (InputConventions.isMenuLeftJustPressed()) {
                moveSkillConfirmationSelection(-1);
                return;
            }
            if (InputConventions.isMenuRightJustPressed()) {
                moveSkillConfirmationSelection(1);
                return;
            }
            if (isBattleSkillConfirmPressed()) {
                applySkillConfirmationSelection(battleService);
                return;
            }
            if (InputConventions.isCancelJustPressed()) {
                resetDialogueFlow();
            }
            return;
        }

        if (dialogueFlowPhase == DialogueFlowPhase.SKILL_STATS) {
            if (InputConventions.isCancelJustPressed()) {
                // Close current child overlay only, return to parent skill confirmation.
                showSkillConfirmation(battleService);
            }
        }
    }

    private void moveSkillConfirmationSelection(int delta) {
        int next = (skillConfirmationOptionIndex + delta + 3) % 3;
        if (next == skillConfirmationOptionIndex) {
            return;
        }
        skillConfirmationOptionIndex = next;
        if (game != null && game.getAudioService() != null) {
            game.getAudioService().playSound(SoundEffect.UI_HOVER);
        }
        dialogueFullText = buildSkillConfirmationText(null);
        dialogueVisibleChars = dialogueFullText.length();
    }

    private void applySkillConfirmationSelection(BattleService battleService) {
        switch (skillConfirmationOptionIndex) {
            case 0 -> submitSelectedSkillFromOverlay(battleService);
            case 1 -> resetDialogueFlow();
            case 2 -> showSkillStats(battleService);
            default -> resetDialogueFlow();
        }
    }

    private void submitSelectedSkillFromOverlay(BattleService battleService) {
        if (battleService == null || battleHud == null) {
            resetDialogueFlow();
            return;
        }
        int slot = battleHud.getSelectedSkillIndex() + 1;
        // Close confirmation layer first so skill submit path is not blocked by dialogueVisible guard.
        resetDialogueFlow();
        submitPlayerSkillAndOpenDialogue(battleService, slot);
    }

    private String buildSkillConfirmationText(BattleService battleService) {
        BattleSkill skill = resolveSelectedSkill(battleService);
        String skillName = skill == null || skill.getName() == null || skill.getName().isBlank()
                ? "Skill"
                : skill.getName();
        String skillDescription = skill == null || skill.getSummaryDescription().isBlank()
                ? "Use this skill?"
                : skill.getSummaryDescription();

        String yes = skillConfirmationOptionIndex == 0 ? "[#F4D175]YES[]" : "[#8A8479]YES[]";
        String no = skillConfirmationOptionIndex == 1 ? "[#F4D175]NO[]" : "[#8A8479]NO[]";
        String stats = skillConfirmationOptionIndex == 2 ? "[#F4D175]STATS[]" : "[#8A8479]STATS[]";

        return "[#ECCD61]" + skillName + "[]\n"
                + "[#D6CBB8]" + skillDescription + "[]\n\n"
                + yes + "   " + no + "   " + stats
                + "\n[#8A8479][Left/Right] Select  [Z/Space] Confirm  [X] Back[]";
    }

    private String buildSkillStatsText(BattleService battleService) {
        BattleSkill skill = resolveSelectedSkill(battleService);
        Clawkin clawkin = resolveActiveClawkin();

        if (skill == null) {
            return "No skill selected.\n\n[X] Back";
        }

        String skillName = skill.getName() == null || skill.getName().isBlank() ? "Skill" : skill.getName();
        String scaleExpr = skill.getEffectStatScale() == null || skill.getEffectStatScale().isBlank()
                ? "-"
                : skill.getEffectStatScale();
        String darkTone = toneForSkill(skill, true);
        String brightTone = toneForSkill(skill, false);

        if (clawkin == null) {
            return "[#F4D175]" + skillName + " — Stats[]\n"
                    + "[#D6CBB8]Type " + skill.getEffectType() + "  Scale " + scaleExpr + "[]\n"
                    + "[" + brightTone + "]Power +" + skill.getEffectBaseStat() + "[]\n"
                    + "[" + darkTone + "]Dur " + skill.getEffectDurationTurns() + "T  CD " + skill.getTurnCooldown() + "T[]\n\n"
                    + "[#8A8479][X] Back[]";
        }

        String clawkinName = clawkin.getName() == null || clawkin.getName().isBlank() ? "Clawkin" : clawkin.getName();
        int scaleContribution = resolveSkillScaleValue(skill, clawkin);
        int totalEstimate = Math.max(1, skill.getEffectBaseStat() + scaleContribution);

        String scaleLineColor = "#D6CBB8";
        if (skill.getEffectType() == BattleSkill.EffectType.ATTACK) {
            scaleLineColor = "#82B1FF";
        } else if (skill.getEffectType() == BattleSkill.EffectType.DEFENSE) {
            scaleLineColor = "#82B1FF";
        } else if (skill.getEffectType() == BattleSkill.EffectType.PARRY) {
            scaleLineColor = "#82B1FF";
        } else if (skill.getEffectType() == BattleSkill.EffectType.HEAL) {
            scaleLineColor = "#7CE7A1";
        }

        return "[#F4D175]" + skillName + " — Stats[]\n"
                + "[#D6CBB8][#ECCD61]" + clawkinName + "[] [#C0B6A6]Lv " + clawkin.getLevel() + "[]\n"
                + "[#8FA7C4]ATK " + clawkin.getBaseAttack() + "  DEF " + clawkin.getBaseDefense() + "  SPD " + clawkin.getBaseSpeed() + "[]\n"
                + "[" + darkTone + "]Base " + scaleContribution + " + Skill +" + skill.getEffectBaseStat() + "[]\n"
                + "[" + brightTone + "]Output " + totalEstimate + "[]\n"
                + "[" + scaleLineColor + "]Type " + skill.getEffectType() + "  Scale " + scaleExpr + "[]\n"
                + "[" + darkTone + "]Dur " + skill.getEffectDurationTurns() + "T  CD " + skill.getTurnCooldown() + "T[]\n"
                + "\n"
                + "[#8A8479][X] Back[]";
    }

    private BattleSkill resolveSelectedSkill(BattleService battleService) {
        if (battleHud == null) {
            return null;
        }
        int slotIndex = battleHud.getSelectedSkillIndex();
        if (battleService != null) {
            BattleStateMachine machine = battleService.getBattleStateMachine();
            if (machine != null) {
                return machine.playerSkill(slotIndex + 1);
            }
        }
        Clawkin active = resolveActiveClawkin();
        if (active == null) {
            return null;
        }
        List<BattleSkill> skills = active.getSkills();
        if (slotIndex < 0 || slotIndex >= skills.size()) {
            return skills.isEmpty() ? null : skills.getFirst();
        }
        return skills.get(slotIndex);
    }

    private Clawkin resolveActiveClawkin() {
        if (playerBattleState == null) {
            return null;
        }
        return playerBattleState.getActiveClawkin();
    }

    private int resolveSkillScaleValue(BattleSkill skill, Clawkin clawkin) {
        if (skill == null || clawkin == null) {
            return 0;
        }
        String scaleExpr = skill.getEffectStatScale();
        if (scaleExpr == null || scaleExpr.isBlank()) {
            return 0;
        }
        String expr = scaleExpr.toLowerCase();
        if ("attack[self]".equals(expr)) {
            return clawkin.getBaseAttack();
        }
        if ("defense[self]".equals(expr)) {
            return clawkin.getBaseDefense();
        }
        if ("speed[self]".equals(expr)) {
            return clawkin.getBaseSpeed();
        }
        return 0;
    }

    private String toneForSkill(BattleSkill skill, boolean darker) {
        if (skill == null || skill.getEffectType() == null) {
            return darker ? "#9E9E9E" : "#E0E0E0";
        }
        return switch (skill.getEffectType()) {
            case DAMAGE, ATTACK -> darker ? "#8B1E1E" : "#FF4D4D";
            case DEFENSE, PARRY -> darker ? "#1E3A8A" : "#4DA3FF";
            case HEAL -> darker ? "#1E7A39" : "#57F28E";
            case BLEED -> darker ? "#8B1E1E" : "#FF4D4D"; // Same as DAMAGE (red tone)
        };
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
                battleActionSfxHandler.playForEnemyActionResult(machine.getLastLogSpans(), machine.getLastLog());
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
            syncActiveClawkinHpIntoBattleState();
            // Restore battle HUD input
            battleHud.show();
            if (battleService != null && battleService.getBattleStateMachine() != null) {
                syncHudHpFromBattleState(battleService.getBattleStateMachine());
            }
            inventoryOpen = false;
            // Block 1-2 frames so the close key used in inventory does not trigger run prompt in battle.
            postInventoryInputBlockFrames = 2;
            Gdx.app.log("BattleOverlay", "Resumed battle from inventory");
        }
    }

    /**
     * Inventory applies item effects on party clawkin instances.
     * During battle, BattleStateMachine's active ally is the source of truth per frame,
     * so we must copy active clawkin HP back into the active ally before HUD sync.
     */
    private void syncActiveClawkinHpIntoBattleState() {
        if (battleService == null || playerBattleState == null) {
            return;
        }
        BattleStateMachine machine = battleService.getBattleStateMachine();
        if (machine == null) {
            return;
        }

        BattleUnit activeAlly = machine.firstAlly();
        Clawkin activeClawkin = playerBattleState.getActiveClawkin();
        if (activeAlly == null || activeClawkin == null) {
            return;
        }

        activeAlly.setHp(activeClawkin.getCurrentHp());
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
        return InputConventions.isInteractJustPressed();
    }

    private static boolean isBattleSkillConfirmPressed() {
        return Gdx.input.isKeyJustPressed(Keys.Z) || Gdx.input.isKeyJustPressed(Keys.SPACE);
    }

    private static boolean isBattleSkillSelectLeftPressed() {
        return Gdx.input.isKeyJustPressed(Keys.LEFT) || Gdx.input.isKeyJustPressed(Keys.DPAD_LEFT);
    }

    private static boolean isBattleSkillSelectRightPressed() {
        return Gdx.input.isKeyJustPressed(Keys.RIGHT) || Gdx.input.isKeyJustPressed(Keys.DPAD_RIGHT);
    }
    
    /**
     * Checks if the current mouse position is over a UI element.
     * Used to prevent dialog advancement when clicking buttons.
     */
    private boolean isClickOnUIElement() {
        if (battleHud == null || !battleHud.isVisible()) {
            return false;
        }
        
        // Get mouse coordinates
        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        
        // Check if the Stage hit any actor at this position
        // The Stage's hit() method will return an actor if one was hit
        com.badlogic.gdx.math.Vector2 stageCoords = battleHud.getStage().screenToStageCoordinates(
            new com.badlogic.gdx.math.Vector2(screenX, screenY)
        );
        
        com.badlogic.gdx.scenes.scene2d.Actor hitActor = battleHud.getStage().hit(stageCoords.x, stageCoords.y, true);
        
        // If we hit an actor (button, icon, etc.), return true
        return hitActor != null;
    }

}
