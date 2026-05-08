package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
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

import github.dluckycompany.clawkins.GameScreen;
import github.dluckycompany.clawkins.Main;
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
    
    // Virtual UI resolution (fixed, independent of physical screen)
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;

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
        
        // Handle keyboard shortcuts only if not transitioning
        if (!isTransitioning) {
            handleKeyboardShortcuts();
        }
    }
    
    /**
     * Handles keyboard shortcuts for the character setup screen.
     */
    private void handleKeyboardShortcuts() {
        // Check if Enter key pressed when currentStep is NAME_INPUT
        if (currentStep == SetupStep.NAME_INPUT) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
                completeSetup();
            }
        }
        
        // Check if Escape key pressed
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            if (currentStep == SetupStep.NAME_INPUT) {
                // If Escape pressed and currentStep is NAME_INPUT, call transitionToGenderSelection()
                transitionToGenderSelection();
            } else if (currentStep == SetupStep.GENDER_SELECTION) {
                // If Escape pressed and currentStep is GENDER_SELECTION, transition to MainMenuScreen
                game.setScreen(MainMenuScreen.class);
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
        maleButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isTransitioning) return;
                Gdx.app.log("CharacterSetup", "Male selected");
                selectedGender = Gender.MALE;
                transitionToNameInput();
            }
        });
        
        // Create Female button
        TextButton femaleButton = new TextButton("Female", buttonStyle);
        femaleButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isTransitioning) return;
                Gdx.app.log("CharacterSetup", "Female selected");
                selectedGender = Gender.FEMALE;
                transitionToNameInput();
            }
        });
        
        // Create Back button
        TextButton backButton = new TextButton("Back", buttonStyle);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isTransitioning) return;
                Gdx.app.log("CharacterSetup", "Back to main menu");
                game.setScreen(MainMenuScreen.class);
            }
        });
        
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
        nameField.setAlignment(Align.center);
        
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
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isTransitioning) return;
                Gdx.app.log("CharacterSetup", "Back to gender selection");
                transitionToGenderSelection();
            }
        });
        
        // Create Start button
        TextButton startButton = new TextButton("Start", buttonStyle);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isTransitioning) return;
                Gdx.app.log("CharacterSetup", "Start button clicked");
                completeSetup();
            }
        });
        
        // Fixed sizing for consistent layout
        float btnWidth = 200f;
        float btnHeight = 60f;
        float btnSpacing = 15f;
        float textFieldWidth = 400f;
        float textFieldHeight = 40f;
        
        // Layout the content table
        contentTable.add(nameField).width(textFieldWidth).height(textFieldHeight).padTop(btnSpacing).row();
        contentTable.add(errorLabel).padTop(btnSpacing * 0.5f).row();
        
        // Button row with both buttons side by side
        Table buttonRow = new Table();
        buttonRow.add(backButton).width(btnWidth).height(btnHeight).padRight(btnSpacing);
        buttonRow.add(startButton).width(btnWidth).height(btnHeight).padLeft(btnSpacing);
        
        contentTable.add(buttonRow).padTop(btnSpacing * 2).row();
        
        // Add content table to root
        rootTable.add(contentTable).padBottom(25).row();
        
        // Add root table to stage
        stage.addActor(rootTable);
        
        // Set keyboard focus to TextField
        stage.setKeyboardFocus(nameField);
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
        // Create button background textures (up, over, down)
        Pixmap upPixmap = createRoundedRectangle(200, 50, Color.valueOf("#C58A2B"));
        Pixmap overPixmap = createRoundedRectangle(200, 50, Color.valueOf("#D4A035"));
        Pixmap downPixmap = createRoundedRectangle(200, 50, Color.valueOf("#A66F1F"));

        Texture upTexture = new Texture(upPixmap);
        Texture overTexture = new Texture(overPixmap);
        Texture downTexture = new Texture(downPixmap);

        upPixmap.dispose();
        overPixmap.dispose();
        downPixmap.dispose();

        // Create 9-patch for stretching
        NinePatch upPatch = new NinePatch(upTexture, 10, 10, 10, 10);
        NinePatch overPatch = new NinePatch(overTexture, 10, 10, 10, 10);
        NinePatch downPatch = new NinePatch(downTexture, 10, 10, 10, 10);

        TextButtonStyle style = new TextButtonStyle();
        style.up = new NinePatchDrawable(upPatch);
        style.over = new NinePatchDrawable(overPatch);
        style.down = new NinePatchDrawable(downPatch);
        style.font = buttonFont;
        style.fontColor = Color.BLACK;
        style.downFontColor = Color.valueOf("#2B2B2B");
        style.overFontColor = Color.BLACK;

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
            // Validation failed, error already shown
            return;
        }
        
        // Trim the name and store in playerName field
        playerName = enteredName.trim();
        
        // Create new PlayerProfile with playerName and selectedGender
        profile = new PlayerProfile(playerName, selectedGender);
        
        // Log for debugging
        Gdx.app.log("CharacterSetup", "Character created: " + playerName + " (" + selectedGender + ")");
        
        // Start welcome transition sequence
        showWelcomeTransition();
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
}
