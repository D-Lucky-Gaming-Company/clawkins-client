package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

import github.dluckycompany.clawkins.GameScreen;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.model.Gender;
import github.dluckycompany.clawkins.model.PlayerProfile;

/**
 * Character setup screen for creating a new character.
 * 
 * <h3>Features</h3>
 * <ul>
 *   <li>Two-step character creation flow: gender selection and name input</li>
 *   <li>Gender selection with Male and Female options</li>
 *   <li>Name input with validation</li>
 *   <li>Dark fantasy themed UI matching MainMenuScreen</li>
 *   <li>Keyboard shortcuts (Enter to confirm, Escape to go back)</li>
 * </ul>
 * 
 * <p>This screen is displayed after clicking "NEW GAME" on the main menu
 * and before transitioning to the game screen.
 */
public class CharacterSetupScreen implements Screen {

    // -----------------------------------------------------------------------
    // Setup Step Enum
    // -----------------------------------------------------------------------

    /**
     * Represents the current step in the character setup flow.
     */
    private enum SetupStep {
        /** Gender selection step (first step) */
        GENDER_SELECTION,
        /** Name input step (second step) */
        NAME_INPUT
    }

    // -----------------------------------------------------------------------
    // Game References
    // -----------------------------------------------------------------------

    private final Main game;
    private final Batch batch;

    // -----------------------------------------------------------------------
    // UI Components
    // -----------------------------------------------------------------------

    private final Stage stage;
    private Table rootTable;
    private Table contentTable;
    private TextField nameField;
    private Label errorLabel;

    // -----------------------------------------------------------------------
    // Fonts
    // -----------------------------------------------------------------------

    private BitmapFont titleFont;
    private BitmapFont buttonFont;
    private BitmapFont labelFont;
    private BitmapFont welcomeFont;

    // -----------------------------------------------------------------------
    // Background Textures
    // -----------------------------------------------------------------------

    private Texture backgroundTexture;
    private Texture menuBackgroundTexture;

    // -----------------------------------------------------------------------
    // State Fields
    // -----------------------------------------------------------------------

    private SetupStep currentStep;
    private Gender selectedGender;
    private String playerName;
    private PlayerProfile profile;
    private boolean isTransitioning;
    private float inputGuardTimer;
    private boolean waitForConfirmRelease;
    
    // Hover debounce tracking
    private TextButton lastHoveredButton;
    private final List<TextButton> currentStepButtons = new ArrayList<>();
    private final List<Runnable> currentStepActions = new ArrayList<>();
    private int selectedButtonIndex = 0;
    
    // Virtual UI resolution (fixed, independent of physical screen)
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;
    private static final float SCREEN_ENTRY_INPUT_GUARD_SECONDS = 0.15f;
    private static final float STEP_TRANSITION_INPUT_GUARD_SECONDS = 0.15f;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates the character setup screen.
     *
     * @param game the Main game instance for screen transitions
     * @param batch the shared SpriteBatch from the main Game class
     */
    public CharacterSetupScreen(Main game, Batch batch) {
        this.game = game;
        this.batch = batch;
        this.stage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        this.isTransitioning = false;
        
        // Load fonts on construction
        loadFonts();
    }

    // -----------------------------------------------------------------------
    // Screen Lifecycle Methods
    // -----------------------------------------------------------------------

    @Override
    public void show() {
        // Set input processor to stage for UI interaction
        Gdx.input.setInputProcessor(stage);
        
        // Reset state to GENDER_SELECTION (first step)
        currentStep = SetupStep.GENDER_SELECTION;
        
        // Clear previous selections
        selectedGender = null;
        playerName = null;
        isTransitioning = false;
        inputGuardTimer = SCREEN_ENTRY_INPUT_GUARD_SECONDS;
        waitForConfirmRelease = true;
        
        // Build gender selection UI
        buildGenderSelectionUI();
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.129f, 0.129f, 0.137f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background using stage viewport for proper scaling
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();
        drawBackground();
        batch.end();

        // Draw Stage UI
        stage.act(delta);
        stage.draw();

        if (inputGuardTimer > 0f) {
            inputGuardTimer = Math.max(0f, inputGuardTimer - delta);
        }
        if (waitForConfirmRelease && !isMenuConfirmHeld()) {
            waitForConfirmRelease = false;
        }
        
        // Handle keyboard shortcuts only if not transitioning
        if (!isTransitioning) {
            handleKeyboardShortcuts();
        }
    }
    
    /**
     * Handles keyboard shortcuts for the character setup screen.
     */
    private void handleKeyboardShortcuts() {
        if (isInputGuardActive()) {
            return;
        }

        if (currentStep == SetupStep.GENDER_SELECTION) {
            handleStepButtonInput();
        } else if (currentStep == SetupStep.NAME_INPUT) {
            handleNameInputKeyboard();
        }

        if (currentStep == SetupStep.GENDER_SELECTION && Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            game.setScreen(MainMenuScreen.class);
        }
    }

    private void handleNameInputKeyboard() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            AudioService audio = game.getAudioService();
            if (audio != null) {
                audio.playSound(SoundEffect.UI_BACK);
            }
            transitionToGenderSelection();
            return;
        }

        // On name-entry screen, avoid WASD so typing letters doesn't move selection.
        if (isNameInputNavigateBackwardPressed()) {
            moveSelection(-1);
        } else if (isNameInputNavigateForwardPressed()) {
            moveSelection(1);
        }

        // Enter confirms currently selected action (default = Start).
        if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER)) {
            if (!currentStepButtons.isEmpty()) {
                triggerSelectedButton();
            } else {
                completeSetup();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // No-op for desktop
    }

    @Override
    public void resume() {
        // No-op for desktop
    }

    @Override
    public void hide() {
        // No-op
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (titleFont != null) titleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (labelFont != null) labelFont.dispose();
        if (welcomeFont != null) welcomeFont.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (menuBackgroundTexture != null) menuBackgroundTexture.dispose();
    }

    // -----------------------------------------------------------------------
    // UI Building Methods
    // -----------------------------------------------------------------------

    /**
     * Loads fonts using FreeTypeFontGenerator.
     * Matches the font style from MainMenuScreen (earthbound-dialogue-gold.otf).
     */
    private void loadFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("font/earthbound-dialogue-gold.otf"));

        // Title font — size 48
        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 48;
        titleParam.borderWidth = 2.0f;
        titleParam.borderColor = Color.BLACK;
        titleFont = generator.generateFont(titleParam);

        // Button font — size 32
        FreeTypeFontGenerator.FreeTypeFontParameter buttonParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonParam.size = 32;
        buttonParam.borderWidth = 1.0f;
        buttonParam.borderColor = Color.BLACK;
        buttonFont = generator.generateFont(buttonParam);

        // Label font — size 24
        FreeTypeFontGenerator.FreeTypeFontParameter labelParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        labelParam.size = 24;
        labelParam.borderWidth = 1.0f;
        labelParam.borderColor = Color.BLACK;
        labelFont = generator.generateFont(labelParam);
        
        // Welcome font — size 36 for welcome message
        FreeTypeFontGenerator.FreeTypeFontParameter welcomeParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        welcomeParam.size = 36;
        welcomeParam.borderWidth = 1.5f;
        welcomeParam.borderColor = Color.BLACK;
        welcomeFont = generator.generateFont(welcomeParam);

        generator.dispose();
    }

    /**
     * Builds the gender selection UI (first step).
     * Displays "Male" and "Female" buttons with a back button.
     */
    private void buildGenderSelectionUI() {
        // Clear any existing UI elements
        stage.clear();
        
        // Create root table with proper structure
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        
        // Create title label
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.valueOf("#D4A035"));
        Label titleLabel = new Label("CHARACTER CREATION", titleStyle);
        rootTable.add(titleLabel).padTop(220).padBottom(20).row();
        
        // Create content table for gender selection
        contentTable = new Table();
        
        // Create instruction label
        Label.LabelStyle labelStyle = new Label.LabelStyle(labelFont, Color.valueOf("#D6CBB8"));
        Label instructionLabel = new Label("Select Your Gender", labelStyle);
        contentTable.add(instructionLabel).padBottom(20).row();
        
        // Create button style
        TextButtonStyle buttonStyle = createButtonStyle();
        
        // Create Male button
        TextButton maleButton = new TextButton("Male", buttonStyle);
        addButtonSoundEffects(maleButton, () -> {
            if (isTransitioning) return;
            AudioService audio = game.getAudioService();
            if (audio != null) audio.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("CharacterSetup", "Male selected");
            selectedGender = Gender.MALE;
            transitionToNameInput();
        }, () -> setSelectedButtonByReference(maleButton));
        
        // Create Female button
        TextButton femaleButton = new TextButton("Female", buttonStyle);
        addButtonSoundEffects(femaleButton, () -> {
            if (isTransitioning) return;
            AudioService audio = game.getAudioService();
            if (audio != null) audio.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("CharacterSetup", "Female selected");
            selectedGender = Gender.FEMALE;
            transitionToNameInput();
        }, () -> setSelectedButtonByReference(femaleButton));
        
        // Create Back button
        TextButton backButton = new TextButton("Back", buttonStyle);
        addButtonSoundEffects(backButton, () -> {
            if (isTransitioning) return;
            AudioService audio = game.getAudioService();
            if (audio != null) audio.playSound(SoundEffect.UI_BACK);
            Gdx.app.log("CharacterSetup", "Back to main menu");
            game.setScreen(MainMenuScreen.class);
        }, () -> setSelectedButtonByReference(backButton));
        
        // Fixed button sizing for consistent layout
        float btnWidth = 250f;
        float btnHeight = 60f;
        float btnSpacing = 15f;
        
        // Layout buttons in content table
        contentTable.add(maleButton).width(btnWidth).height(btnHeight).padTop(btnSpacing).row();
        contentTable.add(femaleButton).width(btnWidth).height(btnHeight).padTop(btnSpacing).row();
        contentTable.add(backButton).width(btnWidth).height(btnHeight).padTop(btnSpacing * 2).row();
        
        // Add content table to root
        rootTable.add(contentTable).padBottom(25).row();
        
        // Add root table to stage
        stage.addActor(rootTable);

        registerCurrentStepButtons(
            List.of(maleButton, femaleButton, backButton),
            List.of(
                () -> {
                    AudioService audio = game.getAudioService();
                    if (audio != null) audio.playSound(SoundEffect.UI_SELECT);
                    selectedGender = Gender.MALE;
                    transitionToNameInput();
                },
                () -> {
                    AudioService audio = game.getAudioService();
                    if (audio != null) audio.playSound(SoundEffect.UI_SELECT);
                    selectedGender = Gender.FEMALE;
                    transitionToNameInput();
                },
                () -> {
                    AudioService audio = game.getAudioService();
                    if (audio != null) audio.playSound(SoundEffect.UI_BACK);
                    game.setScreen(MainMenuScreen.class);
                }
            )
        );
    }

    /**
     * Builds the name input UI (second step).
     * Displays a text field for name entry with validation.
     */
    private void buildNameInputUI() {
        // Clear any existing UI elements
        stage.clear();
        
        // Create root table with proper structure
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        
        // Create title label
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, Color.valueOf("#D4A035"));
        Label titleLabel = new Label("CHARACTER CREATION", titleStyle);
        rootTable.add(titleLabel).padTop(150).padBottom(40).row();
        
        // Create content table for name input
        contentTable = new Table();
        
        // Create instruction label
        Label.LabelStyle labelStyle = new Label.LabelStyle(labelFont, Color.valueOf("#D6CBB8"));
        Label instructionLabel = new Label("Enter Your Name", labelStyle);
        contentTable.add(instructionLabel).padBottom(20).row();
        
        // Create TextField style matching CheatConsoleOverlay pattern
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = labelFont;
        textFieldStyle.fontColor = Color.valueOf("#1F1A13");  // Dark brown text
        textFieldStyle.background = new NinePatchDrawable(createTextFieldBackground(Color.WHITE));
        textFieldStyle.focusedBackground = new NinePatchDrawable(createTextFieldBackground(Color.valueOf("#F0F0F0")));
        textFieldStyle.cursor = new NinePatchDrawable(createTextFieldCursor());
        textFieldStyle.selection = new NinePatchDrawable(createTextFieldSelection());
        
        // Create TextField for name input
        nameField = new TextField("", textFieldStyle);
        nameField.setMessageText("Your character name...");
        nameField.setMaxLength(20);  // Reasonable name length limit
        // Keep caret movement stable during editing (center-aligned TextField can jump cursor).
        nameField.setAlignment(Align.left);
        String defaultName = getDefaultNameForSelectedGender();
        if (!defaultName.isEmpty()) {
            nameField.setText(defaultName);
            nameField.setCursorPosition(defaultName.length());
        }
        
        // Add listener to clear errors when text changes
        nameField.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                clearError();
            }
        });
        
        // Create error label (initially empty, red color)
        Label.LabelStyle errorStyle = new Label.LabelStyle(labelFont, Color.valueOf("#FF4444"));
        errorLabel = new Label("", errorStyle);
        errorLabel.setVisible(false);
        
        // Create button style
        TextButtonStyle buttonStyle = createButtonStyle();

        // Create Back button
        TextButton backButton = new TextButton("Back", buttonStyle);
        addButtonSoundEffects(backButton, () -> {
            if (isTransitioning || isInputGuardActive()) return;
            AudioService audio = game.getAudioService();
            if (audio != null) audio.playSound(SoundEffect.UI_BACK);
            Gdx.app.log("CharacterSetup", "Back to gender selection");
            transitionToGenderSelection();
        }, () -> setSelectedButtonByReference(backButton));

        // Create Start button
        TextButton startButton = new TextButton("Start", buttonStyle);
        addButtonSoundEffects(startButton, () -> {
            if (isTransitioning || isInputGuardActive()) return;
            Gdx.app.log("CharacterSetup", "Start button clicked");
            completeSetup();
        }, () -> setSelectedButtonByReference(startButton));

        float btnWidth = 200f;
        float btnHeight = 60f;
        float btnSpacing = 15f;
        float textFieldWidth = 400f;
        float textFieldHeight = 40f;
        
        // Layout the content table
        contentTable.add(nameField).width(textFieldWidth).height(textFieldHeight).padTop(btnSpacing).row();
        contentTable.add(errorLabel).padTop(btnSpacing * 0.5f).row();
        
        Table buttonRow = new Table();
        buttonRow.add(backButton).width(btnWidth).height(btnHeight).padRight(btnSpacing);
        buttonRow.add(startButton).width(btnWidth).height(btnHeight).padLeft(btnSpacing);
        contentTable.add(buttonRow).padTop(btnSpacing * 2).row();

        Label controlsLabel = new Label("Enter: Confirm    Esc: Back    Arrow Keys: Select", labelStyle);
        contentTable.add(controlsLabel).padTop(btnSpacing).row();
        
        // Add content table to root
        rootTable.add(contentTable).padBottom(25).row();
        
        // Add root table to stage
        stage.addActor(rootTable);
        
        // Set keyboard focus to TextField
        stage.setKeyboardFocus(nameField);
        registerCurrentStepButtons(
            List.of(backButton, startButton),
            List.of(
                () -> {
                    AudioService audio = game.getAudioService();
                    if (audio != null) audio.playSound(SoundEffect.UI_BACK);
                    transitionToGenderSelection();
                },
                this::completeSetup
            )
        );
        setSelectedButtonByReference(startButton);
    }
    
    /**
     * Creates a NinePatch background for TextField.
     */
    private NinePatch createTextFieldBackground(Color color) {
        Pixmap pixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, 20, 20);
        
        // Border
        pixmap.setColor(Color.valueOf("#1F1A13"));
        pixmap.drawRectangle(0, 0, 20, 20);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return new NinePatch(texture, 5, 5, 5, 5);
    }
    
    /**
     * Creates a cursor drawable for TextField.
     */
    private NinePatch createTextFieldCursor() {
        Pixmap pixmap = new Pixmap(2, 20, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.valueOf("#1F1A13"));
        pixmap.fill();
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return new NinePatch(texture, 0, 0, 0, 0);
    }
    
    /**
     * Creates a selection drawable for TextField.
     */
    private NinePatch createTextFieldSelection() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.valueOf("#C19253"));
        pixmap.fill();
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return new NinePatch(texture, 0, 0, 0, 0);
    }

    /**
     * Draws the background matching MainMenuScreen style.
     * Uses the same MenuUI_Background.png with a dark tint overlay.
     * Updated to work with FitViewport virtual coordinates.
     */
    private void drawBackground() {
        // Use virtual UI dimensions instead of physical screen dimensions
        float w = VIRTUAL_UI_WIDTH;
        float h = VIRTUAL_UI_HEIGHT;

        if (menuBackgroundTexture == null) {
            menuBackgroundTexture = new Texture(Gdx.files.internal("ui/MenuUI_Background.png"));
        }

        // Keep menu background visible at full opacity.
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(menuBackgroundTexture, 0, 0, w, h);

        // Light tint keeps button text readable without hiding the image.
        batch.setColor(0.10f, 0.10f, 0.12f, 0.22f);
        batch.draw(getWhitePixel(), 0, 0, w, h);

        // Reset color to white for other batch operations
        batch.setColor(Color.WHITE);
    }

    /**
     * Returns a 1×1 white texture for background fills.
     */
    private Texture getWhitePixel() {
        if (backgroundTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            backgroundTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        return backgroundTexture;
    }

    /**
     * Creates a gold/orange styled button matching MainMenuScreen.
     * Uses rounded appearance with hover effects.
     *
     * @return TextButtonStyle with gold/orange theme
     */
    private TextButtonStyle createButtonStyle() {
        // Create button background textures (up, over, down, checked)
        Pixmap upPixmap = createRoundedRectangle(200, 50, Color.valueOf("#C58A2B"));
        Pixmap overPixmap = createRoundedRectangle(200, 50, Color.valueOf("#D4A035"));
        Pixmap downPixmap = createRoundedRectangle(200, 50, Color.valueOf("#A66F1F"));
        Pixmap checkedPixmap = createRoundedRectangle(200, 50, Color.valueOf("#E0BF4A"));
        Pixmap checkedOverPixmap = createRoundedRectangle(200, 50, Color.valueOf("#ECCD61"));

        Texture upTexture = new Texture(upPixmap);
        Texture overTexture = new Texture(overPixmap);
        Texture downTexture = new Texture(downPixmap);
        Texture checkedTexture = new Texture(checkedPixmap);
        Texture checkedOverTexture = new Texture(checkedOverPixmap);

        upPixmap.dispose();
        overPixmap.dispose();
        downPixmap.dispose();
        checkedPixmap.dispose();
        checkedOverPixmap.dispose();

        // Create 9-patch for stretching
        NinePatch upPatch = new NinePatch(upTexture, 10, 10, 10, 10);
        NinePatch overPatch = new NinePatch(overTexture, 10, 10, 10, 10);
        NinePatch downPatch = new NinePatch(downTexture, 10, 10, 10, 10);
        NinePatch checkedPatch = new NinePatch(checkedTexture, 10, 10, 10, 10);
        NinePatch checkedOverPatch = new NinePatch(checkedOverTexture, 10, 10, 10, 10);

        TextButtonStyle style = new TextButtonStyle();
        style.up = new NinePatchDrawable(upPatch);
        style.over = new NinePatchDrawable(overPatch);
        style.down = new NinePatchDrawable(downPatch);
        style.checked = new NinePatchDrawable(checkedPatch);
        style.checkedOver = new NinePatchDrawable(checkedOverPatch);
        style.checkedDown = new NinePatchDrawable(checkedPatch);
        style.font = buttonFont;
        style.fontColor = Color.BLACK;
        style.downFontColor = Color.valueOf("#2B2B2B");
        style.overFontColor = Color.BLACK;
        style.checkedFontColor = Color.valueOf("#1F1A13");

        return style;
    }

    /**
     * Creates a rounded rectangle pixmap (solid color with border).
     *
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param color the fill color
     * @return Pixmap with rounded rectangle
     */
    private Pixmap createRoundedRectangle(int width, int height, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, width, height);

        // Border
        pixmap.setColor(Color.BLACK);
        pixmap.drawRectangle(0, 0, width, height);
        pixmap.drawRectangle(1, 1, width - 2, height - 2);

        pixmap.setColor(Color.valueOf("#F4A460"));  // Light border highlight
        pixmap.drawLine(2, 2, width - 3, 2);
        pixmap.drawLine(2, 2, 2, height - 3);

        return pixmap;
    }

    // -----------------------------------------------------------------------
    // State Transition Methods
    // -----------------------------------------------------------------------

    /**
     * Transitions from gender selection to name input step.
     */
    private void transitionToNameInput() {
        // Clear stage
        stage.clear();

        inputGuardTimer = STEP_TRANSITION_INPUT_GUARD_SECONDS;
        waitForConfirmRelease = true;
        
        // Set current step to NAME_INPUT
        currentStep = SetupStep.NAME_INPUT;
        
        // Build name input UI
        buildNameInputUI();
    }

    /**
     * Transitions from name input back to gender selection step.
     */
    private void transitionToGenderSelection() {
        // Clear stage
        stage.clear();
        
        // Set current step to GENDER_SELECTION
        currentStep = SetupStep.GENDER_SELECTION;
        
        // Clear errorLabel if it exists
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
        
        // Build gender selection UI
        buildGenderSelectionUI();
    }

    // -----------------------------------------------------------------------
    // Validation Methods
    // -----------------------------------------------------------------------

    /**
     * Validates the player name.
     *
     * @param name the name to validate
     * @return true if the name is valid, false otherwise
     */
    private boolean validateName(String name) {
        // Check if name is null
        if (name == null) {
            showError("Name cannot be empty");
            return false;
        }
        
        // Trim the name
        String trimmed = name.trim();
        
        // Check if trimmed name is empty
        if (trimmed.isEmpty()) {
            showError("Name cannot be empty");
            return false;
        }
        
        // Validation passes
        return true;
    }

    /**
     * Displays an error message to the player.
     *
     * @param message the error message to display
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setColor(Color.valueOf("#FF4444"));
            errorLabel.setVisible(true);
        }
    }

    /**
     * Clears the error message.
     */
    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }

    // -----------------------------------------------------------------------
    // Completion Methods
    // -----------------------------------------------------------------------

    /**
     * Completes the character setup and transitions to the game screen.
     */
    private void completeSetup() {
        // Get text from nameField
        String enteredName = nameField.getText();
        
        // Validate name
        if (!validateName(enteredName)) {
            // Validation failed, error already shown, play error sound
            AudioService audio = game.getAudioService();
            if (audio != null) audio.playSound(SoundEffect.UI_ERROR);
            return;
        }
        
        // Play success sound
        AudioService audio = game.getAudioService();
        if (audio != null) audio.playSound(SoundEffect.UI_SELECT);
        
        // Trim the name and store in playerName field
        playerName = enteredName.trim();
        
        // Create new PlayerProfile with playerName and selectedGender
        profile = new PlayerProfile(playerName, selectedGender);
        handleIntroSequenceEvent(profile);
        
        // Log for debugging
        Gdx.app.log("CharacterSetup", "Character created: " + playerName + " (" + selectedGender + ")");
        
        // Start welcome transition sequence
        showWelcomeTransition();
    }

    private String getDefaultNameForSelectedGender() {
        if (selectedGender == Gender.MALE) {
            return "Mark";
        }
        if (selectedGender == Gender.FEMALE) {
            return "Alice";
        }
        return "";
    }

    private void handleIntroSequenceEvent(PlayerProfile createdProfile) {
        // Placeholder hook for intro sequence logic (to be implemented later).
        if (createdProfile != null) {
            Gdx.app.log("CharacterSetup", "Intro sequence handler placeholder invoked.");
        }
    }
    
    /**
     * Shows a polished welcome transition with the player's name before starting the game.
     * Sequence: Fade out controls → Show welcome message → Hold → Fade to black → Start game
     */
    private void showWelcomeTransition() {
        // Set transitioning flag to disable input
        isTransitioning = true;
        
        // Create welcome message label
        Label.LabelStyle welcomeStyle = new Label.LabelStyle(welcomeFont, Color.valueOf("#D4A035"));
        Label welcomeLabel = new Label("Welcome, " + playerName + ",\nto the world of Clawkins!", welcomeStyle);
        welcomeLabel.setAlignment(Align.center);
        welcomeLabel.setWrap(true);
        
        // Create welcome table centered on screen
        Table welcomeTable = new Table();
        welcomeTable.setFillParent(true);
        welcomeTable.center();
        welcomeTable.add(welcomeLabel).width(VIRTUAL_UI_WIDTH * 0.8f).center();
        
        // Start invisible
        welcomeTable.getColor().a = 0f;
        
        // Create fade overlay for final black screen transition
        Label.LabelStyle overlayStyle = new Label.LabelStyle(labelFont, Color.BLACK);
        Label fadeOverlay = new Label("", overlayStyle);
        fadeOverlay.setFillParent(true);
        fadeOverlay.setAlignment(Align.center);
        
        // Create black background for overlay
        Pixmap overlayPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        overlayPixmap.setColor(Color.BLACK);
        overlayPixmap.fill();
        Texture overlayTexture = new Texture(overlayPixmap);
        overlayPixmap.dispose();
        
        fadeOverlay.getStyle().background = new NinePatchDrawable(new NinePatch(overlayTexture, 0, 0, 0, 0));
        fadeOverlay.getColor().a = 0f;
        
        // Add welcome table and overlay to stage
        stage.addActor(welcomeTable);
        stage.addActor(fadeOverlay);
        
        // Create action sequence
        Action transitionSequence = Actions.sequence(
            // Step 1: Fade out current content (0.5s)
            Actions.run(() -> {
                if (contentTable != null) {
                    contentTable.addAction(Actions.fadeOut(0.5f));
                }
            }),
            Actions.delay(0.5f),
            
            // Step 2: Fade in welcome message (0.5s)
            Actions.run(() -> welcomeTable.addAction(Actions.fadeIn(0.5f))),
            Actions.delay(0.5f),
            
            // Step 3: Hold welcome message (1.75s)
            Actions.delay(1.75f),
            
            // Step 4: Fade to black (1.0s)
            Actions.run(() -> fadeOverlay.addAction(Actions.fadeIn(1.0f))),
            Actions.delay(1.0f),
            
            // Step 5: Start game
            Actions.run(() -> {
                Gdx.app.log("CharacterSetup", "Transition complete, starting game");
                game.setScreen(GameScreen.class);
            })
        );
        
        // Execute the sequence on the stage
        stage.addAction(transitionSequence);
    }

    /**
     * Returns the created player profile.
     *
     * @return the player profile, or null if not yet created
     */
    public PlayerProfile getPlayerProfile() {
        return profile;
    }
    
    // -----------------------------------------------------------------------
    // Sound Effects Helper
    // -----------------------------------------------------------------------

    /**
     * Adds sound effects to a button with hover debouncing.
     * Plays hover sound once when mouse enters, and click sound on click.
     *
     * @param button the button to add sound effects to
     * @param onClickAction the action to run when clicked (should include sound)
     */
    private void addButtonSoundEffects(TextButton button, Runnable onClickAction, Runnable onHoverAction) {
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClickAction.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                // Only play hover sound if this is a different button than last hovered
                if (lastHoveredButton != button && !button.isDisabled()) {
                    lastHoveredButton = button;
                    AudioService audio = game.getAudioService();
                    if (audio != null) {
                        audio.playSound(SoundEffect.UI_HOVER);
                    }
                }
                if (onHoverAction != null) {
                    onHoverAction.run();
                }
            }
        });
    }

    private void registerCurrentStepButtons(List<TextButton> buttons, List<Runnable> actions) {
        clearCurrentStepButtons();
        if (buttons != null) {
            currentStepButtons.addAll(buttons);
        }
        if (actions != null) {
            currentStepActions.addAll(actions);
        }
        selectedButtonIndex = findFirstEnabledButtonIndex();
        updateSelectedButtonVisuals();
    }

    private void clearCurrentStepButtons() {
        currentStepButtons.clear();
        currentStepActions.clear();
        selectedButtonIndex = 0;
    }

    private void handleStepButtonInput() {
        if (currentStepButtons.isEmpty()) {
            return;
        }

        if (isMenuUpPressed()) {
            moveSelection(-1);
        } else if (isMenuDownPressed()) {
            moveSelection(1);
        }

        if (isMenuConfirmPressed()) {
            triggerSelectedButton();
        }
    }

    private boolean isMenuUpPressed() {
        return Gdx.input.isKeyJustPressed(Keys.W)
                || Gdx.input.isKeyJustPressed(Keys.UP)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_UP);
    }

    private boolean isMenuDownPressed() {
        return Gdx.input.isKeyJustPressed(Keys.S)
                || Gdx.input.isKeyJustPressed(Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_DOWN);
    }

    private boolean isMenuLeftPressed() {
        return Gdx.input.isKeyJustPressed(Keys.A)
                || Gdx.input.isKeyJustPressed(Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_LEFT);
    }

    private boolean isMenuRightPressed() {
        return Gdx.input.isKeyJustPressed(Keys.D)
                || Gdx.input.isKeyJustPressed(Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_RIGHT);
    }

    private boolean isMenuConfirmPressed() {
        return Gdx.input.isKeyJustPressed(Keys.Z)
                || Gdx.input.isKeyJustPressed(Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Keys.BUTTON_A);
    }

    private boolean isNameInputNavigateBackwardPressed() {
        return Gdx.input.isKeyJustPressed(Keys.UP)
                || Gdx.input.isKeyJustPressed(Keys.LEFT)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_UP)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_LEFT);
    }

    private boolean isNameInputNavigateForwardPressed() {
        return Gdx.input.isKeyJustPressed(Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Keys.RIGHT)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_DOWN)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_RIGHT);
    }

    private boolean isMenuConfirmHeld() {
        return Gdx.input.isKeyPressed(Keys.Z)
                || Gdx.input.isKeyPressed(Keys.SPACE)
                || Gdx.input.isKeyPressed(Keys.ENTER)
                || Gdx.input.isKeyPressed(Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyPressed(Keys.BUTTON_A);
    }

    private boolean isInputGuardActive() {
        return inputGuardTimer > 0f || waitForConfirmRelease;
    }

    private void setSelectedButtonByReference(TextButton button) {
        if (button == null || button.isDisabled()) {
            return;
        }
        int index = currentStepButtons.indexOf(button);
        if (index >= 0) {
            selectedButtonIndex = index;
            updateSelectedButtonVisuals();
        }
    }

    private void moveSelection(int direction) {
        int size = currentStepButtons.size();
        int next = selectedButtonIndex;
        for (int i = 0; i < size; i++) {
            next = (next + direction + size) % size;
            TextButton candidate = currentStepButtons.get(next);
            if (candidate != null && !candidate.isDisabled()) {
                if (selectedButtonIndex != next) {
                    selectedButtonIndex = next;
                    updateSelectedButtonVisuals();
                    AudioService audio = game.getAudioService();
                    if (audio != null) {
                        audio.playSound(SoundEffect.UI_HOVER);
                    }
                }
                return;
            }
        }
    }

    private void triggerSelectedButton() {
        if (selectedButtonIndex < 0 || selectedButtonIndex >= currentStepButtons.size()) {
            return;
        }
        TextButton selectedButton = currentStepButtons.get(selectedButtonIndex);
        if (selectedButton == null || selectedButton.isDisabled()) {
            AudioService audio = game.getAudioService();
            if (audio != null) {
                audio.playSound(SoundEffect.UI_ERROR);
            }
            return;
        }
        if (selectedButtonIndex < currentStepActions.size()) {
            Runnable action = currentStepActions.get(selectedButtonIndex);
            if (action != null) {
                action.run();
            }
        }
    }

    private void updateSelectedButtonVisuals() {
        for (int i = 0; i < currentStepButtons.size(); i++) {
            TextButton button = currentStepButtons.get(i);
            if (button != null) {
                button.setChecked(i == selectedButtonIndex);
            }
        }
    }

    private int findFirstEnabledButtonIndex() {
        for (int i = 0; i < currentStepButtons.size(); i++) {
            TextButton button = currentStepButtons.get(i);
            if (button != null && !button.isDisabled()) {
                return i;
            }
        }
        return 0;
    }
}
