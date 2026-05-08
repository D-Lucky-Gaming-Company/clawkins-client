package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;

/**
 * Main side-menu + settings submenu controller rendered on a shared UI stage.
 * GameScreen owns world/screen transitions; this class owns menu visuals/state/input.
 */
public class MainSideMenuOverlay {
    public enum Action {
        NONE,
        OPEN_CLAWKINS,
        OPEN_INVENTORY,
        OPEN_SAVE_STATE,
        OPEN_LOAD_STATE,
        EXIT_GAME,
        RETURN_TO_SIDEBAR
    }

    private enum Submenu {
        NONE,
        CLAWKINS,
        INVENTORY,
        SETTINGS
    }

    private static final String[] MENU_OPTIONS = {
        "CLAWKINS",
        "INVENTORY",
        "SETTINGS",
        "SAVE STATE",
        "LOAD STATE",
        "EXIT GAME"
    };

    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;
    private static final float MENU_PANEL_WIDTH = 250f;
    private static final float MENU_PANEL_HEIGHT = 520f;
    private static final float MENU_PANEL_MARGIN = 12f;
    private static final float MENU_SLIDE_DURATION = 0.18f;
    private static final float SIDEBAR_TEXT_SCALE = 1.35f;
    private static final float SETTINGS_TITLE_SCALE = 1.3f;
    private static final float SETTINGS_LABEL_SCALE = 1.15f;
    private static final float SETTINGS_BUTTON_SCALE = 1.1f;

    private final Stage stage;
    private final Skin skin;
    private final BitmapFont font;
    private final AudioService audioService;
    private final UiSoundHelper soundHelper;

    private boolean sideMenuVisible;
    private boolean sideMenuAnimating;
    private int selectedIndex;
    private Submenu activeSubmenu = Submenu.NONE;

    private Table sideMenuRoot;
    private Label[] optionLabels;

    private Table settingsPanel;
    private Slider masterVolumeSlider;
    private TextButton muteButton;

    public MainSideMenuOverlay(Stage stage, Skin skin, BitmapFont font, AudioService audioService) {
        this.stage = stage;
        this.skin = skin;
        this.font = font;
        this.audioService = audioService;
        this.soundHelper = new UiSoundHelper(audioService);
    }

    public Action handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (activeSubmenu != Submenu.NONE) {
                return Action.NONE;
            }
            if (sideMenuVisible) {
                closeSidebar(true);
            } else {
                openSidebar();
            }
            return Action.NONE;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (activeSubmenu != Submenu.NONE) {
                return Action.RETURN_TO_SIDEBAR;
            }
            if (sideMenuVisible) {
                closeSidebar(true);
            }
            return Action.NONE;
        }

        if (!sideMenuVisible || activeSubmenu != Submenu.NONE || sideMenuAnimating) {
            return Action.NONE;
        }

        if (isMenuUpPressed()) {
            selectedIndex = (selectedIndex + MENU_OPTIONS.length - 1) % MENU_OPTIONS.length;
            updateSelectionVisuals();
            soundHelper.playSound(SoundEffect.UI_HOVER);
            return Action.NONE;
        }

        if (isMenuDownPressed()) {
            selectedIndex = (selectedIndex + 1) % MENU_OPTIONS.length;
            updateSelectionVisuals();
            soundHelper.playSound(SoundEffect.UI_HOVER);
            return Action.NONE;
        }

        if (!isMenuSelectPressed()) {
            return Action.NONE;
        }

        return activateSelectedOption();
    }

    public void returnToSidebarFromSubmenu() {
        switch (activeSubmenu) {
            case CLAWKINS:
            case INVENTORY:
            case SETTINGS:
                activeSubmenu = Submenu.NONE;
                openSidebar();
                break;
            case NONE:
            default:
                break;
        }
    }

    public void closeAll() {
        activeSubmenu = Submenu.NONE;
        sideMenuVisible = false;
        sideMenuAnimating = false;
        stage.clear();
        Gdx.input.setInputProcessor(null);
    }

    public void resetAfterScreenReturn() {
        activeSubmenu = Submenu.NONE;
        sideMenuVisible = false;
        sideMenuAnimating = false;
        stage.clear();
    }

    public void restoreSidebarAfterExternalScreenReturn() {
        if (activeSubmenu == Submenu.NONE) {
            resetAfterScreenReturn();
            return;
        }

        activeSubmenu = Submenu.NONE;
        openSidebar();
    }

    public boolean isBlockingGameplay() {
        return sideMenuVisible || activeSubmenu != Submenu.NONE;
    }

    public boolean isSidebarVisible() {
        return sideMenuVisible;
    }

    public boolean isSettingsVisible() {
        return activeSubmenu == Submenu.SETTINGS;
    }

    private boolean isMenuUpPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.W)
            || Gdx.input.isKeyJustPressed(Input.Keys.UP)
            || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_UP);
    }

    private boolean isMenuDownPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.S)
            || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
            || Gdx.input.isKeyJustPressed(Input.Keys.DPAD_DOWN);
    }

    private boolean isMenuSelectPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.BUTTON_A);
    }

    private Action activateSelectedOption() {
        return switch (selectedIndex) {
            case 0 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                closeSidebar(false);
                activeSubmenu = Submenu.CLAWKINS;
                yield Action.OPEN_CLAWKINS;
            }
            case 1 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                closeSidebar(false);
                activeSubmenu = Submenu.INVENTORY;
                yield Action.OPEN_INVENTORY;
            }
            case 2 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                showSettingsSubmenu();
                yield Action.NONE;
            }
            case 3 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                closeSidebar(false);
                activeSubmenu = Submenu.NONE;
                yield Action.OPEN_SAVE_STATE;
            }
            case 4 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                closeSidebar(false);
                activeSubmenu = Submenu.NONE;
                yield Action.OPEN_LOAD_STATE;
            }
            case 5 -> {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                closeSidebar(false);
                activeSubmenu = Submenu.NONE;
                yield Action.EXIT_GAME;
            }
            default -> Action.NONE;
        };
    }

    private void openSidebar() {
        ensureSidebarBuilt();
        if (sideMenuVisible) {
            return;
        }

        stage.clear();
        selectedIndex = 0;
        updateSelectionVisuals();

        sideMenuRoot.clearActions();
        sideMenuRoot.setPosition(VIRTUAL_UI_WIDTH + MENU_PANEL_WIDTH, getSidebarY());
        stage.addActor(sideMenuRoot);

        sideMenuAnimating = true;
        sideMenuRoot.addAction(Actions.sequence(
            Actions.moveTo(getSidebarOpenX(), getSidebarY(), MENU_SLIDE_DURATION),
            Actions.run(() -> sideMenuAnimating = false)
        ));

        sideMenuVisible = true;
        Gdx.input.setInputProcessor(stage);
    }

    private void closeSidebar(boolean smooth) {
        if (!sideMenuVisible || sideMenuRoot == null) {
            return;
        }

        sideMenuRoot.clearActions();
        sideMenuAnimating = smooth;

        if (smooth) {
            sideMenuRoot.addAction(Actions.sequence(
                Actions.moveTo(VIRTUAL_UI_WIDTH + MENU_PANEL_WIDTH, getSidebarY(), MENU_SLIDE_DURATION),
                Actions.run(() -> {
                    stage.clear();
                    sideMenuVisible = false;
                    sideMenuAnimating = false;
                    Gdx.input.setInputProcessor(null);
                })
            ));
            return;
        }

        stage.clear();
        sideMenuVisible = false;
        sideMenuAnimating = false;
        Gdx.input.setInputProcessor(null);
    }

    private void showSettingsSubmenu() {
        closeSidebar(false);
        activeSubmenu = Submenu.SETTINGS;
        ensureSettingsBuilt();
        refreshSettingsValues();
        stage.clear();
        stage.addActor(settingsPanel);
        Gdx.input.setInputProcessor(stage);
    }

    private void ensureSidebarBuilt() {
        if (sideMenuRoot != null) {
            return;
        }

        optionLabels = new Label[MENU_OPTIONS.length];

        Table shadow = new Table();
        shadow.setBackground(new ColorDrawable(new Color(0f, 0f, 0f, 0.28f)));
        shadow.setSize(MENU_PANEL_WIDTH, MENU_PANEL_HEIGHT);
        shadow.setPosition(4f, -4f);

        Table panel = new Table();
        panel.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
            Color.valueOf("#F2F1EE"), 10, 2));
        panel.setSize(MENU_PANEL_WIDTH, MENU_PANEL_HEIGHT);
        panel.pad(18);
        panel.top().left();

        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            Label label = new Label("", new Label.LabelStyle(font, Color.valueOf("#4A4338")));
            label.setFontScale(SIDEBAR_TEXT_SCALE);
            optionLabels[i] = label;
            panel.add(label).left().padBottom(20).row();
        }

        sideMenuRoot = new Table();
        sideMenuRoot.setSize(MENU_PANEL_WIDTH, MENU_PANEL_HEIGHT);
        sideMenuRoot.addActor(shadow);
        sideMenuRoot.addActor(panel);
    }

    private void ensureSettingsBuilt() {
        if (settingsPanel != null) {
            return;
        }

        settingsPanel = new Table();
        settingsPanel.setSize(460f, 360f);
        settingsPanel.setPosition((VIRTUAL_UI_WIDTH - 460f) * 0.5f, (VIRTUAL_UI_HEIGHT - 360f) * 0.5f);
        settingsPanel.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
            Color.valueOf("#ECE8DF"), 10, 1));
        settingsPanel.pad(16);
        settingsPanel.top().left();
        settingsPanel.defaults().left().padBottom(10);

        Label title = new Label("SETTINGS", new Label.LabelStyle(font, Color.valueOf("#252018")));
        title.setFontScale(SETTINGS_TITLE_SCALE);
        settingsPanel.add(title).left().row();

        // Volume label with current value display
        final Label audioLabel = new Label("Audio Volume: 100%", new Label.LabelStyle(font, Color.valueOf("#3B342A")));
        audioLabel.setFontScale(SETTINGS_LABEL_SCALE);
        settingsPanel.add(audioLabel).left().padTop(10).row();
        
        // Create custom slider style with visible knob and track
        masterVolumeSlider = new Slider(0f, 1f, 0.01f, false, createSliderStyle());
        
        // Track last slider value to debounce sound
        final float[] lastSliderValue = {masterVolumeSlider.getValue()};
        
        masterVolumeSlider.addListener(event -> {
            float currentValue = masterVolumeSlider.getValue();
            audioService.setMasterVolume(currentValue);
            
            // Update label with percentage
            int percentage = Math.round(currentValue * 100);
            audioLabel.setText("Audio Volume: " + percentage + "%");
            
            // Play sound only when value changes significantly (debounce)
            if (Math.abs(currentValue - lastSliderValue[0]) > 0.05f) {
                soundHelper.playSound(SoundEffect.UI_HOVER);
                lastSliderValue[0] = currentValue;
            }
            
            return false;
        });
        settingsPanel.add(masterVolumeSlider).width(410f).height(40f).left().padBottom(15).row();

        Table actions = new Table();
        actions.left();

        muteButton = new TextButton("Mute: OFF", skin);
        muteButton.getLabel().setFontScale(SETTINGS_BUTTON_SCALE);
        soundHelper.addButtonSounds(muteButton, () -> {
            audioService.setMuted(!audioService.isMuted());
            refreshSettingsValues();
        });

        TextButton backButton = new TextButton("Back", skin);
        backButton.getLabel().setFontScale(SETTINGS_BUTTON_SCALE);
        soundHelper.addButtonSounds(backButton, () -> {
            returnToSidebarFromSubmenu();
        }, SoundEffect.UI_BACK);

        actions.add(muteButton).height(44f).padRight(10);
        actions.add(backButton).height(44f);
        settingsPanel.add(actions).left().padTop(12).row();
    }

    private void refreshSettingsValues() {
        if (masterVolumeSlider != null) {
            masterVolumeSlider.setValue(audioService.getMasterVolume());
        }
        if (muteButton != null) {
            muteButton.setText(audioService.isMuted() ? "Mute: ON" : "Mute: OFF");
        }
    }

    private void updateSelectionVisuals() {
        if (optionLabels == null) {
            return;
        }

        for (int i = 0; i < optionLabels.length; i++) {
            boolean selected = i == selectedIndex;
            optionLabels[i].setText((selected ? "> " : "  ") + MENU_OPTIONS[i]);
            optionLabels[i].setColor(selected ? Color.valueOf("#1E1912") : Color.valueOf("#4A4338"));
        }
    }

    private float getSidebarOpenX() {
        return VIRTUAL_UI_WIDTH - MENU_PANEL_WIDTH - MENU_PANEL_MARGIN;
    }

    private float getSidebarY() {
        return (VIRTUAL_UI_HEIGHT - MENU_PANEL_HEIGHT) * 0.5f;
    }
    
    /**
     * Create a custom slider style with visible knob and track.
     * Uses pixel-art JRPG aesthetic with clear visual feedback.
     */
    private Slider.SliderStyle createSliderStyle() {
        Slider.SliderStyle style = new Slider.SliderStyle();
        
        // Track background (unfilled portion) - light gray
        style.background = createDrawable(Color.valueOf("#9B8B7E"), 8f, 410f);
        
        // Track foreground (filled portion) - warm tan to show progress
        style.knobBefore = createDrawable(Color.valueOf("#C19253"), 8f, 410f);
        
        // Knob (draggable thumb) - dark brown circle
        style.knob = createDrawable(Color.valueOf("#4A4338"), 24f, 24f);
        
        return style;
    }
    
    /**
     * Create a simple colored drawable for slider components.
     */
    private ColorDrawable createDrawable(Color color, float height, float width) {
        // Note: ColorDrawable doesn't use height/width parameters, but they're kept for API clarity
        return new ColorDrawable(color);
    }
}
