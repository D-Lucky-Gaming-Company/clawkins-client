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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayList;
import java.util.List;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.save.SaveStateManager;

/**
 * Dark fantasy-themed main menu screen.
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Centered title "CLAWKINS" with subtitle "- DAWN OF THE PRIMAL -"</li>
 *   <li>Three vertically stacked buttons: NEW GAME, CONTINUE, EXIT GAME</li>
 *   <li>Gold/orange styled buttons with hover effects</li>
 *   <li>Dark gradient background with vignette</li>
 *   <li>Fade-in animation on load</li>
 * </ul>
 *
 * <p>Call {@link #show()} when transitioning to this screen, and ensure
 * {@link #dispose()} is called when exiting.
 */
public class MainMenuScreen implements Screen {

    // -----------------------------------------------------------------------
    // Graphics
    // -----------------------------------------------------------------------

    private final Batch batch;
    private final Stage stage;
    
    // Virtual UI resolution (fixed, independent of physical screen) - MATCHES CharacterSetupScreen
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;

    // -----------------------------------------------------------------------
    // Fonts
    // -----------------------------------------------------------------------

    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont buttonFont;

    // -----------------------------------------------------------------------
    // Background & Title Image
    // -----------------------------------------------------------------------

    private Texture backgroundTexture;
    private Texture menuBackgroundTexture;

    // -----------------------------------------------------------------------
    // Callbacks
    // -----------------------------------------------------------------------

    private final Runnable onNewGame;
    private final Runnable onContinue;
    private final Runnable onExit;
    private final SaveStateManager saveStateManager;
    private final AudioService audioService;
    
    // Hover debounce tracking
    private TextButton lastHoveredButton;
    private final List<TextButton> menuButtons = new ArrayList<>();
    private final List<Runnable> menuActions = new ArrayList<>();
    private int selectedButtonIndex = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Creates the main menu screen.
     *
     * @param batch shared SpriteBatch from the main Game class
     * @param onNewGame called when "NEW GAME" button is clicked
     * @param onContinue called when "CONTINUE" button is clicked
     * @param onExit called when "EXIT GAME" button is clicked
     * @param saveStateManager save state manager for checking available saves
     * @param audioService audio service for playing menu sounds
     */
    public MainMenuScreen(Batch batch, Runnable onNewGame, Runnable onContinue, Runnable onExit, SaveStateManager saveStateManager, AudioService audioService) {
        this.batch = batch;
        this.stage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));

        this.onNewGame = onNewGame;
        this.onContinue = onContinue;
        this.onExit = onExit;
        this.saveStateManager = saveStateManager;
        this.audioService = audioService;

        Gdx.input.setInputProcessor(stage);
    }

    // -----------------------------------------------------------------------
    // Screen lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void show() {
        buildUI();
        
        // Set input processor to stage when screen is shown
        // This ensures buttons work when returning from other screens
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        // Clear screen
        Gdx.gl.glClearColor(0.129f, 0.129f, 0.137f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background using stage viewport for proper scaling - MATCHES CharacterSetupScreen
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);
        batch.begin();
        drawBackground();
        batch.end();

        // Draw Stage UI
        stage.act(delta);
        stage.draw();

        handleMenuInput();

        // Handle keyboard shortcut
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            onExit.run();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (titleFont != null) titleFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (menuBackgroundTexture != null) menuBackgroundTexture.dispose();
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
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_HOVER);
                    }
                }
                if (onHoverAction != null) {
                    onHoverAction.run();
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // UI building
    // -----------------------------------------------------------------------

    /**
     * Builds the main menu UI table and adds it to the Stage.
     * Uses responsive Table layout that scales to any screen size.
     */
    private void buildUI() {
        // Clear any existing UI elements from previous show() calls
        stage.clear();
        
        // Load fonts
        loadFonts();

        // Root table — fills the entire viewport
        Table root = new Table();
        root.setFillParent(true);
        // root.debug();  // Uncomment to visualize table layout

        // ── Buttons section ──────────────────────────
        Table buttonTable = new Table();

        TextButtonStyle buttonStyle = createButtonStyle();

        TextButton newGameBtn = new TextButton("NEW GAME", buttonStyle);
        addButtonSoundEffects(newGameBtn, () -> {
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "NEW GAME clicked");
            onNewGame.run();
        }, () -> setSelectedButtonByReference(newGameBtn));

        boolean hasSaves = saveStateManager != null && saveStateManager.hasSaveStates();
        TextButton continueBtn = new TextButton("CONTINUE", buttonStyle);
        continueBtn.setDisabled(!hasSaves);
        addButtonSoundEffects(continueBtn, () -> {
            if (continueBtn.isDisabled()) {
                if (audioService != null) audioService.playSound(SoundEffect.UI_ERROR);
                return;
            }
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "CONTINUE clicked");
            onContinue.run();
        }, () -> setSelectedButtonByReference(continueBtn));

        TextButton exitBtn = new TextButton("EXIT GAME", buttonStyle);
        addButtonSoundEffects(exitBtn, () -> {
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "EXIT GAME clicked");
            onExit.run();
        }, () -> setSelectedButtonByReference(exitBtn));

        menuButtons.clear();
        menuActions.clear();
        menuButtons.add(newGameBtn);
        menuButtons.add(continueBtn);
        menuButtons.add(exitBtn);
        menuActions.add(() -> {
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "NEW GAME clicked");
            onNewGame.run();
        });
        menuActions.add(() -> {
            if (continueBtn.isDisabled()) {
                if (audioService != null) audioService.playSound(SoundEffect.UI_ERROR);
                return;
            }
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "CONTINUE clicked");
            onContinue.run();
        });
        menuActions.add(() -> {
            if (audioService != null) audioService.playSound(SoundEffect.UI_SELECT);
            Gdx.app.log("MainMenu", "EXIT GAME clicked");
            onExit.run();
        });
        selectedButtonIndex = findFirstEnabledButtonIndex();
        updateSelectedButtonVisuals();

        // Fixed button sizing using virtual dimensions - consistent with CharacterSetupScreen approach
        float btnWidth = 300f;
        float btnHeight = 65f;
        float btnSpacing = 18f;

        buttonTable.add(newGameBtn).width(btnWidth).height(btnHeight).row();
        buttonTable.add(continueBtn).width(btnWidth).height(btnHeight).padTop(btnSpacing).row();
        buttonTable.add(exitBtn).width(btnWidth).height(btnHeight).padTop(btnSpacing).row();

        Label statusLabel = new Label(
            hasSaves ? "" : "No save states found.",
            new Label.LabelStyle(buttonFont, Color.valueOf("#D6CBB8"))
        );
        statusLabel.setFontScale(0.9f);
        buttonTable.add(statusLabel).padTop(btnSpacing * 0.6f);

        // Center button table in root with top padding to position below logo
        root.add(buttonTable).center().padTop(240f);

        stage.addActor(root);
    }

    /**
     * Creates a gold/orange styled button with rounded appearance.
     */
    private TextButtonStyle createButtonStyle() {
        // Create button background textures (up, over, down, checked/selected)
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

    private void handleMenuInput() {
        if (menuButtons.isEmpty()) {
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

    private boolean isMenuConfirmPressed() {
        return Gdx.input.isKeyJustPressed(Keys.Z)
                || Gdx.input.isKeyJustPressed(Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Keys.BUTTON_A);
    }

    private void moveSelection(int direction) {
        if (menuButtons.isEmpty()) {
            return;
        }

        int size = menuButtons.size();
        int nextIndex = selectedButtonIndex;
        for (int i = 0; i < size; i++) {
            nextIndex = (nextIndex + direction + size) % size;
            TextButton candidate = menuButtons.get(nextIndex);
            if (candidate != null && !candidate.isDisabled()) {
                setSelectedButton(nextIndex, true);
                return;
            }
        }
    }

    private void triggerSelectedButton() {
        if (selectedButtonIndex < 0 || selectedButtonIndex >= menuButtons.size()) {
            return;
        }

        TextButton selectedButton = menuButtons.get(selectedButtonIndex);
        if (selectedButton == null) {
            return;
        }

        if (selectedButton.isDisabled()) {
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_ERROR);
            }
            return;
        }

        if (selectedButtonIndex < menuActions.size()) {
            Runnable action = menuActions.get(selectedButtonIndex);
            if (action != null) {
                action.run();
            }
        }
    }

    private void setSelectedButtonByReference(TextButton button) {
        if (button == null || button.isDisabled()) {
            return;
        }
        int index = menuButtons.indexOf(button);
        if (index >= 0) {
            setSelectedButton(index, false);
        }
    }

    private void setSelectedButton(int index, boolean playHoverSound) {
        if (menuButtons.isEmpty()) {
            return;
        }
        if (index < 0 || index >= menuButtons.size()) {
            return;
        }
        if (selectedButtonIndex == index) {
            return;
        }

        selectedButtonIndex = index;
        updateSelectedButtonVisuals();

        if (playHoverSound && audioService != null) {
            audioService.playSound(SoundEffect.UI_HOVER);
        }
    }

    private void updateSelectedButtonVisuals() {
        for (int i = 0; i < menuButtons.size(); i++) {
            TextButton button = menuButtons.get(i);
            if (button != null) {
                button.setChecked(i == selectedButtonIndex);
            }
        }
    }

    private int findFirstEnabledButtonIndex() {
        for (int i = 0; i < menuButtons.size(); i++) {
            TextButton button = menuButtons.get(i);
            if (button != null && !button.isDisabled()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Creates a rounded rectangle pixmap (solid color with border).
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

    /**
     * Loads all fonts used on the menu.
     */
    private void loadFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("font/earthbound-dialogue-gold.otf"));

        // Title font — large
        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 72;
        titleParam.borderWidth = 2.0f;
        titleParam.borderColor = Color.BLACK;
        titleFont = generator.generateFont(titleParam);

        // Subtitle font — medium
        FreeTypeFontGenerator.FreeTypeFontParameter subtitleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        subtitleParam.size = 24;
        subtitleParam.borderWidth = 1.0f;
        subtitleParam.borderColor = Color.BLACK;
        subtitleFont = generator.generateFont(subtitleParam);

        // Button font — medium
        FreeTypeFontGenerator.FreeTypeFontParameter buttonParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonParam.size = 28;
        buttonParam.borderWidth = 1.0f;
        buttonParam.borderColor = Color.BLACK;
        buttonFont = generator.generateFont(buttonParam);

        generator.dispose();
    }

    /**
     * Draws the background using virtual coordinates - MATCHES CharacterSetupScreen.
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
}
