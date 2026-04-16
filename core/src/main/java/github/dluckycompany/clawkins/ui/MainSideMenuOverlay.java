package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import github.dluckycompany.clawkins.audio.AudioService;

/**
 * Main side-menu + settings submenu controller rendered on a shared UI stage.
 * GameScreen owns world/screen transitions; this class owns menu visuals/state/input.
 */
public class MainSideMenuOverlay {
    public enum Action {
        NONE,
        OPEN_CLAWKINS,
        OPEN_INVENTORY,
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

    private boolean sideMenuVisible;
    private boolean sideMenuAnimating;
    private int selectedIndex;
    private Submenu activeSubmenu = Submenu.NONE;

    private Table sideMenuRoot;
    private Label[] optionLabels;

    private Table settingsPanel;
    private Slider musicSlider;
    private Slider sfxSlider;
    private TextButton muteButton;

    public MainSideMenuOverlay(Stage stage, Skin skin, BitmapFont font, AudioService audioService) {
        this.stage = stage;
        this.skin = skin;
        this.font = font;
        this.audioService = audioService;
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
            return Action.NONE;
        }

        if (isMenuDownPressed()) {
            selectedIndex = (selectedIndex + 1) % MENU_OPTIONS.length;
            updateSelectionVisuals();
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
                closeSidebar(false);
                activeSubmenu = Submenu.CLAWKINS;
                yield Action.OPEN_CLAWKINS;
            }
            case 1 -> {
                closeSidebar(false);
                activeSubmenu = Submenu.INVENTORY;
                yield Action.OPEN_INVENTORY;
            }
            case 2 -> {
                showSettingsSubmenu();
                yield Action.NONE;
            }
            case 3 -> {
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
        settingsPanel.setSize(460f, 320f);
        settingsPanel.setPosition((VIRTUAL_UI_WIDTH - 460f) * 0.5f, (VIRTUAL_UI_HEIGHT - 320f) * 0.5f);
        settingsPanel.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
            Color.valueOf("#ECE8DF"), 10, 1));
        settingsPanel.pad(16);
        settingsPanel.top().left();
        settingsPanel.defaults().left().padBottom(10);

        Label title = new Label("SETTINGS", new Label.LabelStyle(font, Color.valueOf("#252018")));
        title.setFontScale(SETTINGS_TITLE_SCALE);
        settingsPanel.add(title).left().row();

        Label musicLabel = new Label("Music Volume", new Label.LabelStyle(font, Color.valueOf("#3B342A")));
        musicLabel.setFontScale(SETTINGS_LABEL_SCALE);
        settingsPanel.add(musicLabel).left().row();
        musicSlider = new Slider(0f, 1f, 0.01f, false, skin);
        musicSlider.addListener(event -> {
            audioService.setMusicVolume(musicSlider.getValue());
            return false;
        });
        settingsPanel.add(musicSlider).width(400f).height(28f).left().row();

        Label sfxLabel = new Label("SFX Volume", new Label.LabelStyle(font, Color.valueOf("#3B342A")));
        sfxLabel.setFontScale(SETTINGS_LABEL_SCALE);
        settingsPanel.add(sfxLabel).left().row();
        sfxSlider = new Slider(0f, 1f, 0.01f, false, skin);
        sfxSlider.addListener(event -> {
            audioService.setSoundVolume(sfxSlider.getValue());
            return false;
        });
        settingsPanel.add(sfxSlider).width(400f).height(28f).left().row();

        Table actions = new Table();
        actions.left();

        muteButton = new TextButton("Mute: OFF", skin);
        muteButton.getLabel().setFontScale(SETTINGS_BUTTON_SCALE);
        muteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                audioService.setMuted(!audioService.isMuted());
                refreshSettingsValues();
            }
        });

        TextButton backButton = new TextButton("Back", skin);
        backButton.getLabel().setFontScale(SETTINGS_BUTTON_SCALE);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                returnToSidebarFromSubmenu();
            }
        });

        actions.add(muteButton).height(44f).padRight(10);
        actions.add(backButton).height(44f);
        settingsPanel.add(actions).left().padTop(12).row();
    }

    private void refreshSettingsValues() {
        if (musicSlider != null) {
            musicSlider.setValue(audioService.getMusicVolume());
        }
        if (sfxSlider != null) {
            sfxSlider.setValue(audioService.getSoundVolume());
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
}
