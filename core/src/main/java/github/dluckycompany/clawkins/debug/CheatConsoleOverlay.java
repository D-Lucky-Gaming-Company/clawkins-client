package github.dluckycompany.clawkins.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Disposable;

import github.dluckycompany.clawkins.debug.CheatCodeManager.CheatResult;
import github.dluckycompany.clawkins.ui.ColorDrawable;

/**
 * Cheat console overlay UI for developer debugging.
 * 
 * Features:
 * - F12 to toggle console
 * - Text input field with automatic focus
 * - Enter to execute cheat
 * - Escape to close console
 * - Success/error feedback messages
 * - Compact, non-intrusive design
 */
public class CheatConsoleOverlay implements Disposable {
    private static final float CONSOLE_WIDTH = 400f;
    private static final float CONSOLE_HEIGHT = 120f;
    private static final float FADE_DURATION = 0.15f;
    
    // UI Colors (matching game's color scheme)
    private static final Color CONSOLE_BG = Color.valueOf("#E7DAC7CC"); // Warm beige with transparency
    private static final Color BORDER_COLOR = Color.valueOf("#1F1A13");  // Dark brown
    private static final Color TEXT_COLOR = Color.valueOf("#1F1A13");    // Dark brown
    private static final Color SUCCESS_COLOR = Color.valueOf("#2D5016");  // Dark green
    private static final Color ERROR_COLOR = Color.valueOf("#8B0000");    // Dark red
    
    private final Stage stage;
    private final CheatCodeManager cheatManager;
    private final BitmapFont font;
    private final Skin skin;
    
    private Table consoleTable;
    private TextField inputField;
    private Label titleLabel;
    private Label hintLabel;
    private Label feedbackLabel;
    
    private boolean isVisible = false;
    private boolean isAnimating = false;
    private InputProcessor previousInputProcessor;
    private float feedbackTimer = 0f;
    private static final float FEEDBACK_DURATION = 3f;
    
    public CheatConsoleOverlay(Stage stage, CheatCodeManager cheatManager, BitmapFont font, Skin skin) {
        this.stage = stage;
        this.cheatManager = cheatManager;
        this.font = font;
        this.skin = skin;
        
        Gdx.app.log("CheatConsoleOverlay", "Initializing cheat console overlay");
        createUI();
        Gdx.app.log("CheatConsoleOverlay", "Cheat console overlay initialized successfully");
    }
    
    private void createUI() {
        Gdx.app.log("CheatConsoleOverlay", "Creating UI components");
        
        // Create main console table
        consoleTable = new Table();
        consoleTable.setSize(CONSOLE_WIDTH, CONSOLE_HEIGHT);
        
        // Position at top-center of screen
        float screenWidth = stage.getViewport().getWorldWidth();
        float screenHeight = stage.getViewport().getWorldHeight();
        consoleTable.setPosition(
            (screenWidth - CONSOLE_WIDTH) / 2f,
            screenHeight - CONSOLE_HEIGHT - 50f
        );
        
        Gdx.app.log("CheatConsoleOverlay", "Console positioned at: " + consoleTable.getX() + ", " + consoleTable.getY());
        Gdx.app.log("CheatConsoleOverlay", "Stage viewport: " + screenWidth + "x" + screenHeight);
        
        // Background with border
        Drawable background = new ColorDrawable(CONSOLE_BG);
        consoleTable.setBackground(background);
        
        // Title label
        Label.LabelStyle titleStyle = new Label.LabelStyle(font, TEXT_COLOR);
        titleLabel = new Label("CHEAT CODE", titleStyle);
        titleLabel.setFontScale(1.2f);
        
        // Input field
        TextField.TextFieldStyle inputStyle = new TextField.TextFieldStyle();
        inputStyle.font = font;
        inputStyle.fontColor = TEXT_COLOR;
        inputStyle.background = new ColorDrawable(Color.WHITE);
        inputStyle.focusedBackground = new ColorDrawable(Color.valueOf("#F0F0F0"));
        inputStyle.cursor = new ColorDrawable(TEXT_COLOR);
        inputStyle.selection = new ColorDrawable(Color.valueOf("#C19253"));
        
        inputField = new TextField("", inputStyle);
        inputField.setMessageText("Enter cheat code...");
        
        // Hint label
        Label.LabelStyle hintStyle = new Label.LabelStyle(font, TEXT_COLOR);
        hintLabel = new Label("Press ENTER to run | ESC to close", hintStyle);
        hintLabel.setFontScale(0.8f);
        
        // Feedback label
        Label.LabelStyle feedbackStyle = new Label.LabelStyle(font, SUCCESS_COLOR);
        feedbackLabel = new Label("", feedbackStyle);
        feedbackLabel.setFontScale(0.9f);
        
        // Layout the console
        consoleTable.pad(10f);
        consoleTable.add(titleLabel).center().padBottom(5f).row();
        consoleTable.add(inputField).width(CONSOLE_WIDTH - 20f).height(25f).padBottom(5f).row();
        consoleTable.add(hintLabel).center().padBottom(5f).row();
        consoleTable.add(feedbackLabel).center();
        
        // Initially hidden
        consoleTable.setVisible(false);
        consoleTable.getColor().a = 0f;
        
        stage.addActor(consoleTable);
        
        Gdx.app.log("CheatConsoleOverlay", "Console table added to stage. Actors in stage: " + stage.getActors().size);
        
        // Set up input field listener
        inputField.setTextFieldListener(new TextField.TextFieldListener() {
            @Override
            public void keyTyped(TextField textField, char c) {
                if (c == '\r' || c == '\n') { // Enter key
                    executeCheat();
                }
            }
        });
    }
    
    /**
     * Toggle the cheat console visibility.
     */
    public void toggle() {
        if (isAnimating) return;
        
        if (isVisible) {
            close();
        } else {
            open();
        }
    }
    
    /**
     * Open the cheat console.
     */
    public void open() {
        if (isVisible || isAnimating) return;
        
        if (!cheatManager.areCheatsEnabled()) {
            showFeedback("Cheats are disabled", false);
            return;
        }
        
        isVisible = true;
        isAnimating = true;
        
        // Store previous input processor
        previousInputProcessor = Gdx.input.getInputProcessor();
        
        // Set stage as input processor
        Gdx.input.setInputProcessor(stage);
        
        // Clear input and feedback
        inputField.setText("");
        feedbackLabel.setText("");
        feedbackTimer = 0f;
        
        // Show and fade in
        consoleTable.setVisible(true);
        consoleTable.addAction(Actions.sequence(
            Actions.alpha(1f, FADE_DURATION),
            Actions.run(() -> {
                isAnimating = false;
                // Focus the input field
                stage.setKeyboardFocus(inputField);
            })
        ));
        
        Gdx.app.log("CheatConsole", "Console opened");
    }
    
    /**
     * Close the cheat console.
     */
    public void close() {
        if (!isVisible || isAnimating) return;
        
        isAnimating = true;
        
        // Fade out and hide
        consoleTable.addAction(Actions.sequence(
            Actions.alpha(0f, FADE_DURATION),
            Actions.run(() -> {
                consoleTable.setVisible(false);
                isVisible = false;
                isAnimating = false;
                
                // Restore previous input processor
                Gdx.input.setInputProcessor(previousInputProcessor);
                previousInputProcessor = null;
                
                // Clear keyboard focus
                stage.setKeyboardFocus(null);
            })
        ));
        
        Gdx.app.log("CheatConsole", "Console closed");
    }
    
    /**
     * Handle input when console is open.
     * Should be called from the main input handling loop.
     */
    public boolean handleInput() {
        if (!isVisible) return false;
        
        // Handle F12 to close
        if (Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            close();
            return true;
        }
        
        // Handle Escape to close
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            close();
            return true;
        }
        
        return true; // Consume input when console is open
    }
    
    /**
     * Update the console (call from render loop).
     */
    public void update(float deltaTime) {
        if (feedbackTimer > 0f) {
            feedbackTimer -= deltaTime;
            if (feedbackTimer <= 0f) {
                feedbackLabel.setText("");
            }
        }
    }
    
    /**
     * Render the console.
     */
    public void render(Batch batch) {
        if (isVisible || isAnimating) {
            // The stage will handle rendering the console table
            // This method is here for consistency with other overlay classes
        }
    }
    
    /**
     * Check if the console is currently visible.
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * Execute the cheat code from the input field.
     */
    private void executeCheat() {
        String cheatCode = inputField.getText();
        
        if (cheatCode == null || cheatCode.trim().isEmpty()) {
            showFeedback("Enter a cheat code first", false);
            return;
        }
        
        CheatResult result = cheatManager.executeCheat(cheatCode);
        showFeedback(result.message, result.success);
        
        // Clear input field after execution
        inputField.setText("");
        
        Gdx.app.log("CheatConsole", "Executed cheat: " + cheatCode + " | Result: " + result.message);
    }
    
    /**
     * Show feedback message with appropriate color.
     */
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setColor(success ? SUCCESS_COLOR : ERROR_COLOR);
        feedbackTimer = FEEDBACK_DURATION;
    }
    
    @Override
    public void dispose() {
        // Console table will be disposed with the stage
        // Nothing specific to dispose here
    }
}