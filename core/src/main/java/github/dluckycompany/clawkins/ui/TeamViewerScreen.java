package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;
import github.dluckycompany.clawkins.input.InputConventions;

/**
 * TeamViewerScreen - Full-Screen Modal Party Member Viewer
 * 
 * ARCHITECTURE: Pure Table-based vertical layout (no Stack) with proper viewport scaling
 * - Root table fills entire stage using fillParent(true)
 * - Content area expands vertically with cards centered horizontally
 * - Footer docked at bottom with beige background + back button
 * 
 * Layout (Vertical Table Structure):
 * ┌───────────────────────────────────────┐
 * │ [Warm Tan Background - Full Screen]   │
 * │                                       │
 * │   ┌─────────────────────────────┐    │
 * │   │  [ClawkinCard 0] Selected   │    │
 * │   ├─────────────────────────────┤    │
 * │   │  [ClawkinCard 1]            │    │
 * │   ├─────────────────────────────┤    │
 * │   │  [ClawkinCard 2]            │    │
 * │   └─────────────────────────────┘    │
 * │                                       │
 * ├───────────────────────────────────────┤
 * │ ← BACK  "Selected: [Name]" [Beige]  │
 * └───────────────────────────────────────┘
 */
public class TeamViewerScreen implements InputProcessor {
    
    // ============ Color Palette ============
    private static final Color WARM_TAN = Color.valueOf("#C19253");
    private static final Color WARM_BEIGE = Color.valueOf("#E7DAC7");
    private static final Color DARK_BROWN = Color.valueOf("#1F1A13");
    
    private static final int MAX_PARTY_SIZE = 3;
    private static final float CARD_HEIGHT = 140f;
    private static final float FOOTER_HEIGHT = 70f;
    private static final float CARD_SPACING = 8f;
    
    // Stage and UI components
    private final Stage stage;
    private final BitmapFont font;
    private final List<Clawkin> party;
    private final AudioService audioService;
    
    // Root layout hierarchy (Vertical Table - no Stack)
    private final Table rootTable;          // Full-screen tan background + vertical layout
    private final Table cardsContainer;     // 3-card vertical layout (centered)
    private final Table footerTable;        // Bottom beige bar
    
    // Card storage
    private final List<ClawkinCardView> cards = new ArrayList<>();
    private final List<Table> cardWrappers = new ArrayList<>();
    
    // State management
    private int currentSelectedIndex = 0;
    private int lastSelectedIndex = -1;  // For debouncing navigation SFX
    private int activeFighterIndex = -1;   // index of the clawkin currently set as active fighter (-1 = none)
    private Label footerMessageLabel;
    private Label cancelOptionLabel;
    private Label switchOptionLabel;
    private Label summaryOptionLabel;
    private TextureRegionDrawable backDrawable;
    private boolean actionMenuOpen = false;
    private int selectedActionIndex = 1; // default to SWITCH
    private int lastActionIndex = -1;  // For debouncing action menu navigation SFX
    private boolean cancelKeyLatched;
    private int sharedTotalExperience = -1;
    
    // Previous input processor (for restoration)
    private final InputProcessor previousInputProcessor;
    private InputMultiplexer inputMultiplexer;  // Store multiplexer so parent can't override
    
    // Callbacks
    private Runnable onBackPressed;
    private Runnable onSummaryRequested;
    private CardSelectionCallback cardSelectionCallback;
    private Consumer<Integer> onActiveFighterSet;   // fired when player confirms a new active fighter

    private enum ActionOption {
        CANCEL,
        SWITCH,
        SUMMARY
    }
    
    /**
     * Constructor - Initialize full-screen team viewer modal
     * 
     * @param stage The LibGDX Stage to render into (must use FitViewport with proper dimensions)
     * @param party List of Clawkin party members (0-3 members)
     * @param font BitmapFont for text rendering
     * @param audioService AudioService for playing UI sounds
     */
    public TeamViewerScreen(Stage stage, List<Clawkin> party, BitmapFont font, AudioService audioService) {
        this.stage = stage;
        this.font = font;
        this.party = new ArrayList<>(party != null ? party : new ArrayList<>());
        this.audioService = audioService;
        
        // Initialize layout tables
        this.rootTable = new Table();
        this.cardsContainer = new Table();
        this.footerTable = new Table();
        
        // Store previous input processor for later restoration
        this.previousInputProcessor = Gdx.input.getInputProcessor();
        
        // Load assets
        loadBackButton();
        
        // Build the complete GUI hierarchy
        buildLayout();
        
        // Populate with party members and setup input
        populateTeam(this.party);
        
        // Set this as the input processor for keyboard/mouse events
        setupInputProcessor();
    }
    
    /**
     * Load back button asset from ui/buttons/
     */
    private void loadBackButton() {
        try {
            backDrawable = new TextureRegionDrawable(
                new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/buttons/back.png"))
            );
        } catch (Exception e) {
            Gdx.app.log("TeamViewerScreen", "Failed to load back button: " + e.getMessage());
        }
    }
    
    /**
     * BUILD LAYOUT - Proper Vertical Table Hierarchy (No Stack)
     * 
     * Structure:
     * rootTable (fillParent=true, vertical layout, dark brown background for full occlusion)
     * ├─ Row 1: cardsContainer (expand & fill) - centered cards with tan background
     * └─ Row 2: footerTable (fixed height) - docked at bottom
     */
    private void buildLayout() {
        // ============ ROOT TABLE ============
        // Full-screen vertical layout with DARK BROWN background
        // This creates complete occlusion - prevents world map from showing at edges
        rootTable.clear();
        rootTable.setFillParent(true);          // CRITICAL: Fill entire stage/viewport
        rootTable.setBackground(new ColorDrawable(Color.valueOf("#1F1A13")));  // Dark Brown
        rootTable.top();                        // Align top for row-based layout
        
        // Configure for vertical layout (rows)
        rootTable.columnDefaults(0).expandX().fillX();
        
        // ============ CARDS CONTAINER ============
        // Vertical layout of 3 ClawkinCard instances (centered horizontally)
        // This container has a tan background and is padded inset from dark edges
        cardsContainer.clear();
        cardsContainer.center();               // CENTER HORIZONTALLY & VERTICALLY
        cardsContainer.setBackground(new ColorDrawable(WARM_TAN));  // Tan interior
        cardsContainer.defaults().width(400).height(CARD_HEIGHT).padBottom(CARD_SPACING);
        cardsContainer.columnDefaults(0).center();
        
        // Add cards container to root with padding (creates visible dark border)
        rootTable.add(cardsContainer).expand().fill().pad(20).row();
        
        // Build and add footer
        buildFooter();
        rootTable.add(footerTable).expandX().fillX().height(FOOTER_HEIGHT)
            .padLeft(20).padRight(20).padBottom(20);
        
        // Add root to stage
        stage.addActor(rootTable);
        
        Gdx.app.log("TeamViewerScreen", "Layout: Dark brown occlusion + tan card area");
    }
    
    /**
     * Populate the 3 card slots with party members
     * 
     * @param partyMembers List of Clawkin to display (0-3 members)
     */
    private void populateTeam(List<Clawkin> partyMembers) {
        // Clear existing cards
        cards.clear();
        cardsContainer.clearChildren();
        cardWrappers.clear();
        
        // Create 3 card slots
        for (int slotIndex = 0; slotIndex < MAX_PARTY_SIZE; slotIndex++) {
            Clawkin clawkin = (slotIndex < partyMembers.size()) ? partyMembers.get(slotIndex) : null;
            ClawkinCard card = new ClawkinCard(clawkin, font, sharedTotalExperience);
            cards.add(card);
            
            // Create wrapper for margin/padding and event handling
            final int finalSlotIndex = slotIndex;
            Table cardWrapper = new Table();
            cardWrapper.add(card).expand().fill();
            
            // Mouse interaction: hover to select, click to confirm
            cardWrapper.addListener(new ClickListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    // Hover: visually indicate this card can be selected
                    // Play hover sound only when entering a different card
                    if (currentSelectedIndex != finalSlotIndex && audioService != null) {
                        audioService.playSound(SoundEffect.UI_HOVER);
                    }
                    selectSlot(finalSlotIndex);
                }
                
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // Click: select slot and open action menu for this clawkin.
                    // Play select sound
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_SELECT);
                    }
                    selectSlot(finalSlotIndex);
                    openActionMenuForCurrentSelection();
                }
            });
            
            cardsContainer.add(cardWrapper).row();
            cardWrappers.add(cardWrapper);
        }
        
        // Set initial selection
        selectSlot(0);
        
        Gdx.app.log("TeamViewerScreen", "Populated " + cards.size() + " cards");
    }
    
    /**
     * Build the footer table with back button, message, and beige background
     */
    private void buildFooter() {
        footerTable.clear();
        footerTable.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(WARM_BEIGE, 12, 2));
        footerTable.pad(12);
        
        // Back button (left side)
        if (backDrawable != null) {
            ImageButton.ImageButtonStyle backStyle = new ImageButton.ImageButtonStyle();
            backStyle.up = backDrawable;
            backStyle.down = backDrawable;
            backStyle.checked = backDrawable;
            ImageButton backBtn = new ImageButton(backStyle);
            backBtn.setSize(40, 40);
            
            backBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("TeamViewerScreen", "[INPUT] Back button clicked");
                    // Play back sound
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_BACK);
                    }
                    // CRITICAL FIX: Call exitTeamViewer() immediately
                    // Do NOT defer via animation callback - it never executes when delta=0 (paused game)
                    exitTeamViewer();
                }
            });
            
            footerTable.add(backBtn).width(40).height(40).padRight(15);
        } else {
            Label backLabel = new Label("← BACK", new Label.LabelStyle(font, DARK_BROWN));
            backLabel.setFontScale(1.2f);
            footerTable.add(backLabel).padRight(15).left();
        }
        
        // Selection message (center)
        footerMessageLabel = new Label("Choose a Clawkin", new Label.LabelStyle(font, DARK_BROWN));
        footerMessageLabel.setFontScale(1.1f);
        Table centerContent = new Table();
        centerContent.defaults().pad(2);
        centerContent.add(footerMessageLabel).row();

        Table optionsRow = new Table();
        Label.LabelStyle optionStyle = new Label.LabelStyle(font, DARK_BROWN);
        cancelOptionLabel = new Label("CANCEL", optionStyle);
        switchOptionLabel = new Label("SWITCH", optionStyle);
        summaryOptionLabel = new Label("SUMMARY", optionStyle);

        cancelOptionLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!actionMenuOpen) return;
                // Play cancel sound
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_BACK);
                }
                selectedActionIndex = ActionOption.CANCEL.ordinal();
                executeSelectedAction();
            }
        });
        switchOptionLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!actionMenuOpen) return;
                // Play select sound
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_SELECT);
                }
                selectedActionIndex = ActionOption.SWITCH.ordinal();
                executeSelectedAction();
            }
        });
        summaryOptionLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!actionMenuOpen) return;
                // Play select sound
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_SELECT);
                }
                selectedActionIndex = ActionOption.SUMMARY.ordinal();
                executeSelectedAction();
            }
        });

        optionsRow.add(cancelOptionLabel).padRight(18);
        optionsRow.add(switchOptionLabel).padRight(18);
        optionsRow.add(summaryOptionLabel);
        centerContent.add(optionsRow);

        footerTable.add(centerContent).expand().center();
        
        // Right spacer for visual balance
        footerTable.add(new Label("", new Label.LabelStyle(font, DARK_BROWN))).padRight(15).width(50);
        updateActionOptionVisuals();
    }
    
    /**
     * SELECT SLOT - Update visual feedback and footer with animation
     * 
     * @param index The card slot to select (0-2)
     */
    public void selectSlot(int index) {
        if (index < 0 || index >= MAX_PARTY_SIZE) {
            return;
        }
        
        // Only animate if selection actually changed
        if (index != currentSelectedIndex) {
            currentSelectedIndex = index;
            
            // Update visual feedback for all cards with animation
            for (int i = 0; i < cards.size(); i++) {
                ClawkinCardView cardView = cards.get(i);
                boolean isNowSelected = (i == currentSelectedIndex);
                cardView.setSelected(isNowSelected);

                // Animation: selected card scales to 1.05, others scale back to 1.0
                Actor cardActor = (Actor) cardView;
                if (isNowSelected) {
                    cardActor.clearActions();
                    cardActor.addAction(Actions.scaleTo(1.05f, 1.05f, 0.1f));
                } else {
                    cardActor.clearActions();
                    cardActor.addAction(Actions.scaleTo(1.0f, 1.0f, 0.1f));
                }
            }
            
            // Update footer message
            updateFooterMessage();
        }
    }
    
    /**
     * Update the footer message based on currently hovered + active fighter cards.
     */
    private void updateFooterMessage() {
        if (footerMessageLabel == null) return;
        if (currentSelectedIndex >= 0 && currentSelectedIndex < cards.size()) {
            ClawkinCardView hoveredCard = cards.get(currentSelectedIndex);
            if (!hoveredCard.isEmpty() && hoveredCard.getClawkin() != null) {
                Clawkin clawkin = hoveredCard.getClawkin();
                boolean isActiveFighter = (currentSelectedIndex == activeFighterIndex);
                if (actionMenuOpen) {
                    footerMessageLabel.setText(clawkin.getName() + " - Choose: CANCEL / SWITCH / SUMMARY");
                } else if (isActiveFighter) {
                    footerMessageLabel.setText(clawkin.getName() + " [Active Fighter] - Press Enter to select action");
                } else {
                    footerMessageLabel.setText(clawkin.getName() + " (" + buildLevelExpText(clawkin) + ") - Press Enter to select action");
                }
            } else {
                footerMessageLabel.setText("Empty Slot");
            }
        }
        updateActionOptionVisuals();
    }

    private String buildLevelExpText(Clawkin clawkin) {
        if (clawkin == null) {
            return "Lv.1";
        }
        if (sharedTotalExperience < 0) {
            return "Lv." + clawkin.getLevel();
        }

        int level = LevelSystem.calculateLevelFromExp(sharedTotalExperience);
        if (level >= LevelSystem.MAX_LEVEL) {
            return "Lv." + level + ", XP MAX";
        }

        int levelExpFloor = LevelSystem.getExpRequiredForLevel(level);
        int expForNextLevel = LevelSystem.getExpForNextLevel(level);
        int expIntoLevel = Math.max(0, sharedTotalExperience - levelExpFloor);
        int clampedExpIntoLevel = Math.min(expIntoLevel, Math.max(0, expForNextLevel));
        return "Lv." + level + ", XP " + clampedExpIntoLevel + "/" + expForNextLevel;
    }

    public void setSharedExperience(int totalExp) {
        sharedTotalExperience = Math.max(0, totalExp);
        for (ClawkinCardView card : cards) {
            if (card != null) {
                card.setSharedExperience(sharedTotalExperience);
            }
        }
        updateFooterMessage();
    }

    private void updateActionOptionVisuals() {
        if (cancelOptionLabel == null || switchOptionLabel == null || summaryOptionLabel == null) {
            return;
        }

        Color dim = new Color(DARK_BROWN.r, DARK_BROWN.g, DARK_BROWN.b, actionMenuOpen ? 0.55f : 0.35f);
        cancelOptionLabel.setColor(dim);
        switchOptionLabel.setColor(dim);
        summaryOptionLabel.setColor(dim);

        if (!actionMenuOpen) {
            return;
        }

        Color selected = new Color(0.95f, 0.77f, 0.31f, 1f);
        switch (selectedActionIndex) {
            case 0 -> cancelOptionLabel.setColor(selected);
            case 1 -> switchOptionLabel.setColor(selected);
            case 2 -> summaryOptionLabel.setColor(selected);
            default -> {
            }
        }
    }

    private void openActionMenuForCurrentSelection() {
        if (currentSelectedIndex < 0 || currentSelectedIndex >= cards.size()) {
            return;
        }
        ClawkinCardView card = cards.get(currentSelectedIndex);
        if (card.isEmpty() || card.getClawkin() == null) {
            return;
        }
        actionMenuOpen = true;
        selectedActionIndex = ActionOption.SWITCH.ordinal();
        updateFooterMessage();
    }

    private void closeActionMenu() {
        actionMenuOpen = false;
        selectedActionIndex = ActionOption.SWITCH.ordinal();
        updateFooterMessage();
    }

    private void executeSelectedAction() {
        ActionOption option = ActionOption.values()[selectedActionIndex];
        switch (option) {
            case CANCEL -> closeActionMenu();
            case SWITCH -> {
                confirmActiveFighter();
                closeActionMenu();
            }
            case SUMMARY -> {
                closeActionMenu();
                if (onSummaryRequested != null) {
                    onSummaryRequested.run();
                }
            }
        }
    }

    /**
     * Set which card slot is currently the active battle fighter.
     * Visually marks the card with a gold highlight and updates the footer.
     *
     * @param index Party slot index (0-2), or -1 for none
     */
    public void setActiveFighterIndex(int index) {
        // Clear old active-fighter mark
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setActiveFighter(false);
        }
        activeFighterIndex = index;
        if (index >= 0 && index < cards.size()) {
            cards.get(index).setActiveFighter(true);
        }
        updateFooterMessage();
    }

    /**
     * Confirm the currently hovered card as the active battle fighter.
     * Only has effect when the hovered card holds a living Clawkin.
     */
    private void confirmActiveFighter() {
        if (currentSelectedIndex < 0 || currentSelectedIndex >= cards.size()) return;
        ClawkinCardView card = cards.get(currentSelectedIndex);
        if (card.isEmpty() || card.getClawkin() == null) return;

        setActiveFighterIndex(currentSelectedIndex);

        if (onActiveFighterSet != null) {
            onActiveFighterSet.accept(activeFighterIndex);
        }
        if (cardSelectionCallback != null) {
            cardSelectionCallback.onCardSelected(currentSelectedIndex, card.getClawkin());
        }
        Gdx.app.log("TeamViewerScreen", "[INPUT] Active fighter set to slot " + activeFighterIndex
            + " (" + card.getClawkin().getName() + ")");
    }

    /**
     * Register a callback that fires whenever the player confirms a new active fighter.
     *
     * @param callback Consumer receiving the newly confirmed slot index (0-2)
     */
    public void setOnActiveFighterSet(Consumer<Integer> callback) {
        this.onActiveFighterSet = callback;
    }

    /**
     * Register a callback invoked when SUMMARY is selected and confirmed.
     */
    public void setOnSummaryRequested(Runnable callback) {
        this.onSummaryRequested = callback;
    }
    
    /**
     * Apply "squish and fade" button animation effect (Professional Game Dev Standard)
     * 
     * Animation Sequence:
     * 1. Parallel: Scale to 0.9f + Darken to GRAY (100ms, bounceOut interpolation)
     * 2. Parallel: Scale back to 1.0f + Restore WHITE color (100ms)
     * 3. Execute callback (screen transition, etc.)
     * 
     * This provides immediate tactile feedback before the screen change.
     * Uses bounceOut interpolation for a natural "snap" feel.
     * 
     * @param button The button widget to animate
     * @param onComplete Optional Runnable to execute after animation (can be null)
     */
    
    /**
     * Update HP display for the currently selected Clawkin
     */
    public void updateSelectedHp() {
        if (currentSelectedIndex >= 0 && currentSelectedIndex < cards.size()) {
            ClawkinCardView card = cards.get(currentSelectedIndex);
            if (!card.isEmpty()) {
                card.updateHp();
            }
        }
    }
    
    /**
     * Exit the Team Viewer - Shared method for both Escape key and Back button
     * 
     * This is the SINGLE SOURCE OF TRUTH for all exit logic.
     * Both keyboard (ESCAPE) and UI (back button) routes call this method.
     * 
     * CRITICAL: This method executes IMMEDIATELY, not deferred behind animation callbacks.
     * The previous code called this via Actions.run() which never executes when delta=0 (paused game).
     * 
     * Restoration Flow:
     * 1. Restore previous input processor (critical for input chain integrity)
     * 2. Trigger onBackPressed callback (GameScreen.toggleTeamViewer)
     */
    private void exitTeamViewer() {
        Gdx.app.log("TeamViewerScreen", "[CLOSE] exitTeamViewer() called IMMEDIATELY");
        
        // CRITICAL: Restore the previous input processor to restore input focus
        // This maintains the input chain: GameScreen → inventoryStage
        if (previousInputProcessor != null) {
            Gdx.input.setInputProcessor(previousInputProcessor);
            Gdx.app.log("TeamViewerScreen", "[CLOSE] Restored previous input processor");
        } else {
            Gdx.input.setInputProcessor(null);
            Gdx.app.log("TeamViewerScreen", "[CLOSE] Set input processor to null");
        }
        
        // Trigger the parent callback (typically toggleTeamViewer in GameScreen)
        // This removes the TeamViewer from the stage and cleans up state
        if (onBackPressed != null) {
            Gdx.app.log("TeamViewerScreen", "[CLOSE] onBackPressed.run() invoked - GameScreen.toggleTeamViewer() should execute now");
            onBackPressed.run();
        } else {
            Gdx.app.log("TeamViewerScreen", "[ERROR] onBackPressed callback is NULL - TeamViewer will NOT close!");
        }
    }
    

    
    /**
     * Set the callback for back button presses
     * 
     * @param callback Runnable to execute when back is pressed
     */
    public void setOnBackPressed(Runnable callback) {
        this.onBackPressed = callback;
    }
    
    /**
     * Set the callback for card selection events
     * 
     * @param callback CardSelectionCallback to execute on card click
     */
    public void setCardSelectionCallback(CardSelectionCallback callback) {
        this.cardSelectionCallback = callback;
    }
    
    /**
     * Get the root table actor for adding to parent stage
     * 
     * @return The root Table actor
     */
    public Table getRootTable() {
        return rootTable;
    }
    
    /**
     * Get the currently selected Clawkin
     * 
     * @return The selected Clawkin, or null if the slot is empty
     */
    public Clawkin getSelectedClawkin() {
        if (currentSelectedIndex >= 0 && currentSelectedIndex < cards.size()) {
            ClawkinCardView card = cards.get(currentSelectedIndex);
            return card.isEmpty() ? null : card.getClawkin();
        }
        return null;
    }
    
    /**
     * Get the currently selected slot index
     * 
     * @return Current selected slot (0-2)
     */
    public int getSelectedIndex() {
        return currentSelectedIndex;
    }
    
    // ============================================================================
    // INPUT PROCESSOR IMPLEMENTATION - Bidirectional Keyboard + Mouse Handling
    // ============================================================================
    
    /**
     * Setup input handling using InputMultiplexer
     * 
     * CRITICAL ARCHITECTURE:
     * The Stage MUST process events first (for Scene2D UI like button clicks).
     * Then TeamViewerScreen handles keyboard-only events (Escape key).
     * 
     * Event Flow:
     * 1. Touch/Click → Stage → Button ClickListener → exitTeamViewer()
     * 2. ESCAPE key → Stage (ignores) → TeamViewerScreen.keyDown() → exitTeamViewer()
     * 
     * InputMultiplexer ensures no events are lost and proper delegation happens.
     */
    private void setupInputProcessor() {
        // Create multiplexer: Stage first (UI priority), then keyboard handler
        inputMultiplexer = new InputMultiplexer();
        
        // Stage processes touch/click events first (highest priority for UI)
        inputMultiplexer.addProcessor(stage);
        
        // TeamViewerScreen handles keyboard events (secondary priority)
        inputMultiplexer.addProcessor(this);
        
        // Set multiplexer as the active input processor
        Gdx.input.setInputProcessor(inputMultiplexer);
        Gdx.app.log("TeamViewerScreen", "[INPUT] Multiplexer set: [Stage, TeamViewerScreen]");
    }
    
    /**
     * Get the input multiplexer for this TeamViewer
     * Parent (GameScreen) must use this multiplexer to avoid breaking the input chain
     */
    public InputMultiplexer getInputMultiplexer() {
        return inputMultiplexer;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        // Log all key events for debugging
        Gdx.app.log("TeamViewerScreen", "[INPUT] keyDown received: " + keycode);

        if (actionMenuOpen) {
            if (keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
                int newIndex = (selectedActionIndex + ActionOption.values().length - 1) % ActionOption.values().length;
                // Play navigation sound only when action changes
                if (newIndex != lastActionIndex && audioService != null) {
                    audioService.playSound(SoundEffect.UI_HOVER);
                    lastActionIndex = newIndex;
                }
                selectedActionIndex = newIndex;
                updateActionOptionVisuals();
                return true;
            }

            if (keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
                int newIndex = (selectedActionIndex + 1) % ActionOption.values().length;
                // Play navigation sound only when action changes
                if (newIndex != lastActionIndex && audioService != null) {
                    audioService.playSound(SoundEffect.UI_HOVER);
                    lastActionIndex = newIndex;
                }
                selectedActionIndex = newIndex;
                updateActionOptionVisuals();
                return true;
            }

            if (InputConventions.isInteractKey(keycode)) {
                // Play appropriate sound based on selected action
                if (audioService != null) {
                    if (selectedActionIndex == ActionOption.CANCEL.ordinal()) {
                        audioService.playSound(SoundEffect.UI_BACK);
                    } else {
                        audioService.playSound(SoundEffect.UI_SELECT);
                    }
                }
                executeSelectedAction();
                return true;
            }

            if (InputConventions.isCancelKey(keycode)) {
                if (cancelKeyLatched) {
                    return true;
                }
                cancelKeyLatched = true;
                // Play cancel sound
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_BACK);
                }
                closeActionMenu();
                return true;
            }

            return true;
        }

        if (keycode == Input.Keys.W || keycode == Input.Keys.UP || keycode == Input.Keys.A || keycode == Input.Keys.LEFT) {
            // Navigate previous card (wrap around)
            int newIndex = (currentSelectedIndex - 1 + MAX_PARTY_SIZE) % MAX_PARTY_SIZE;
            // Play navigation sound only when selection changes
            if (newIndex != lastSelectedIndex && audioService != null) {
                audioService.playSound(SoundEffect.UI_HOVER);
                lastSelectedIndex = newIndex;
            }
            selectSlot(newIndex);
            return true;
        }

        if (keycode == Input.Keys.S || keycode == Input.Keys.DOWN || keycode == Input.Keys.D || keycode == Input.Keys.RIGHT) {
            // Navigate next card (wrap around)
            int newIndex = (currentSelectedIndex + 1) % MAX_PARTY_SIZE;
            // Play navigation sound only when selection changes
            if (newIndex != lastSelectedIndex && audioService != null) {
                audioService.playSound(SoundEffect.UI_HOVER);
                lastSelectedIndex = newIndex;
            }
            selectSlot(newIndex);
            return true;
        }

        if (InputConventions.isInteractKey(keycode)) {
            // Play select sound
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_SELECT);
            }
            openActionMenuForCurrentSelection();
            return true;
        }

        if (InputConventions.isCancelKey(keycode)) {
            if (cancelKeyLatched) {
                return true;
            }
            cancelKeyLatched = true;
            Gdx.app.log("TeamViewerScreen", "[INPUT] Cancel key pressed");
            // Play back sound
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_BACK);
            }
            // CRITICAL FIX: Call exitTeamViewer() immediately
            // Do NOT defer via animation callback - it never executes when delta=0 (paused game)
            exitTeamViewer();
            return true;
        }

        return false;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        if (InputConventions.isCancelKey(keycode)) {
            cancelKeyLatched = false;
        }
        // Log key up events for debugging
        if (keycode == Input.Keys.ESCAPE) {
            Gdx.app.log("TeamViewerScreen", "[INPUT] ESCAPE key released");
        }
        return false;  // Stage will handle key up events
    }
    
    @Override
    public boolean keyTyped(char character) {
        return false;  // No character input needed
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Log touch events for debugging
        Gdx.app.log("TeamViewerScreen", "[INPUT] touchDown at (" + screenX + "," + screenY + ")");
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Not needed - Stage is first in multiplexer and handles this
        return false;
    }
    
    /**
     * Dispose of resources and cleanup input processor
     * 
     * Call this when the Team Viewer is completely removed from the UI.
     * Ensures any remaining action states are cleared.
     */
    public void dispose() {
        if (rootTable != null) {
            rootTable.remove();
        }
        
        // Clear any pending actions (animations) to prevent lingering effects
        rootTable.clearActions();
    }
}
