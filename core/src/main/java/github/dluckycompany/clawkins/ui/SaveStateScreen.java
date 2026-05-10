package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import github.dluckycompany.clawkins.asset.MapAsset;
import github.dluckycompany.clawkins.asset.MapAssetName;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.input.InputConventions;
import github.dluckycompany.clawkins.save.SaveState;
import github.dluckycompany.clawkins.save.SaveStateManager;

public class SaveStateScreen implements Screen {
    public enum Mode {
        LOAD,
        SAVE
    }

    public interface SaveStateProvider {
        SaveState buildSaveState();
    }

    public interface SaveStateConsumer {
        void onLoad(SaveState state);
    }

    private final Batch batch;
    private final SaveStateManager saveStateManager;
    private final AudioService audioService;
    private UiSoundHelper soundHelper;

    private Stage stage;
    private Skin skin;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private BitmapFont buttonFont;

    private Mode mode = Mode.LOAD;
    private SaveStateProvider saveStateProvider;
    private SaveStateConsumer loadConsumer;
    private Runnable onBack;

    private final List<SaveState> saveStates = new ArrayList<>();
    private final List<Label> entryLabels = new ArrayList<>();
    private final List<Table> entryRows = new ArrayList<>();
    private final List<String> entryTexts = new ArrayList<>();
    private final List<TextButton> actionButtons = new ArrayList<>();
    private final List<Runnable> actionButtonActions = new ArrayList<>();
    private int selectedIndex = 0;
    private int selectedActionButtonIndex = 0;

    private Label statusLabel;
    private Label controlsHintLabel;
    private Table listTable;
    private TextButton primaryButton;
    private TextButton saveNewButton;
    private TextButton deleteButton;
    private TextButton backButton;
    private float inputGuardTimer;
    private boolean waitForConfirmRelease;

    private static final float ACTION_BUTTON_WIDTH = 190f;
    private static final float ACTION_BUTTON_HEIGHT = 56f;
    private static final float TITLE_SCALE = 1.0f;
    private static final float LIST_ENTRY_SCALE = 1.2f;
    private static final float STATUS_SCALE = 1.05f;
    private static final float CONTROL_HINT_SCALE = 0.95f;
    
    // Fixed virtual UI resolution matching InventoryScreen
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;
    private static final float PANEL_WIDTH = 700f;
    private static final float PANEL_HEIGHT = 500f;
    private static final float SCREEN_ENTRY_INPUT_GUARD_SECONDS = 0.15f;
    private static final DateTimeFormatter SAVE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SAVE_TIMESTAMP_SHORT_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    public SaveStateScreen(Batch batch, SaveStateManager saveStateManager, AudioService audioService) {
        this.batch = batch;
        this.saveStateManager = saveStateManager;
        this.audioService = audioService;
        this.soundHelper = new UiSoundHelper(audioService);
    }

    public void configure(Mode mode, SaveStateProvider saveStateProvider, SaveStateConsumer loadConsumer, Runnable onBack) {
        this.mode = mode == null ? Mode.LOAD : mode;
        this.saveStateProvider = saveStateProvider;
        this.loadConsumer = loadConsumer;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        if (stage == null) {
            // Use FitViewport with fixed virtual resolution (same as InventoryScreen)
            stage = new Stage(new FitViewport(VIRTUAL_UI_WIDTH, VIRTUAL_UI_HEIGHT));
        } else {
            stage.clear();
        }

        if (titleFont == null || bodyFont == null || buttonFont == null) {
            loadFonts();
        }
        bodyFont.getData().markupEnabled = true;

        if (skin == null) {
            skin = buildSkin();
        }

        buildUI();
        refreshSaveList();
        soundHelper.resetHoverTracking();

        inputGuardTimer = SCREEN_ENTRY_INPUT_GUARD_SECONDS;
        waitForConfirmRelease = true;
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        updateInputGuard(delta);
        handleInput();

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(stage.getCamera().combined);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            // Update viewport with centering (true) to maintain fixed virtual size
            // This ensures the UI remains centered with black bars on sides in fullscreen
            stage.getViewport().update(width, height, true);
        }
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
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (titleFont != null) {
            titleFont.dispose();
            titleFont = null;
        }
        if (bodyFont != null) {
            bodyFont.dispose();
            bodyFont = null;
        }
        if (buttonFont != null) {
            buttonFont.dispose();
            buttonFont = null;
        }
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
    }

    private void buildUI() {
        // Root table fills the entire virtual viewport with dark background
        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(new ColorDrawable(new Color(0f, 0f, 0f, 0.85f)));
        
        // Inner panel with fixed size (centered, similar to InventoryScreen)
        Table panel = new Table();
        panel.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
            Color.valueOf("#2A2A2E"), 12, 2));
        panel.pad(20f);
        panel.defaults().left().expandX().fillX();
        
        // Title
        Label title = new Label(mode == Mode.LOAD ? "LOAD SAVE STATE" : "SAVE STATE", 
            new Label.LabelStyle(titleFont, Color.valueOf("#E8E6E3")));
        title.setFontScale(TITLE_SCALE);
        panel.add(title).padBottom(12f).row();
        
        // Save list container with proper sizing
        listTable = new Table();
        listTable.defaults().left().expandX().fillX();
        
        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).width(PANEL_WIDTH - 40f).height(PANEL_HEIGHT - 180f).padTop(8f).row();
        
        // Status label
        statusLabel = new Label("", new Label.LabelStyle(bodyFont, Color.valueOf("#C9C2B6")));
        statusLabel.setFontScale(STATUS_SCALE);
        panel.add(statusLabel).padTop(8f).row();
        
        // Action buttons
        Table actions = new Table();
        actions.defaults().width(ACTION_BUTTON_WIDTH).height(ACTION_BUTTON_HEIGHT).padRight(12f);

        primaryButton = soundHelper.createButton(mode == Mode.LOAD ? "LOAD" : "OVERWRITE", skin, this::handlePrimaryAction);

        if (mode == Mode.SAVE) {
            saveNewButton = soundHelper.createButton("SAVE NEW", skin, this::handleSaveNew);
        }

        deleteButton = soundHelper.createButton("DELETE", skin, this::handleDelete);

        backButton = soundHelper.createButton("BACK", skin, this::handleBack, SoundEffect.UI_BACK);

        actions.add(primaryButton);
        if (saveNewButton != null) {
            actions.add(saveNewButton);
        }
        actions.add(deleteButton);
        actions.add(backButton);

        panel.add(actions).padTop(12f).row();

        controlsHintLabel = new Label("", new Label.LabelStyle(bodyFont, Color.valueOf("#B8B0A2")));
        controlsHintLabel.setFontScale(CONTROL_HINT_SCALE);
        panel.add(controlsHintLabel).padTop(8f).row();
        
        // Add centered panel to root
        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT).center();

        stage.addActor(root);
        registerActionButtons();
        setSelectedActionButton(0, false);
    }

    private void refreshSaveList() {
        saveStates.clear();
        saveStates.addAll(saveStateManager.listSaveStates());
        listTable.clear();
        entryLabels.clear();
        entryRows.clear();
        entryTexts.clear();

        if (saveStates.isEmpty()) {
            Label empty = new Label("No save states found.", new Label.LabelStyle(bodyFont, Color.valueOf("#C9C2B6")));
            empty.setFontScale(1.15f);
            listTable.add(empty).left().padBottom(6f).row();
            selectedIndex = 0;
            updateButtonStates();
            updateControlsHint();
            return;
        }

        for (int i = 0; i < saveStates.size(); i++) {
            SaveState state = saveStates.get(i);
            String entryText = buildSaveEntryLabel(state);
            entryTexts.add(entryText);
            Label label = new Label("", new Label.LabelStyle(bodyFont, Color.valueOf("#C9C2B6")));
            label.setFontScale(LIST_ENTRY_SCALE);
            label.setWrap(true);
            int index = i;
            label.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    setSelectedIndex(index);
                }
            });
            entryLabels.add(label);
            Table row = new Table();
            row.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
                    Color.valueOf("#35353B"), 8, 1));
            row.pad(10f, 12f, 10f, 12f);
            row.add(label).left().width(PANEL_WIDTH - 84f).expandX().fillX();
            listTable.add(row).left().expandX().fillX().padBottom(8f).row();
            entryRows.add(row);
        }

        if (selectedIndex >= saveStates.size()) {
            selectedIndex = Math.max(0, saveStates.size() - 1);
        }
        updateSelectionVisuals();
        updateButtonStates();
        updateControlsHint();
    }

    private String buildSaveEntryLabel(SaveState state) {
        String saveName = toMarkupSafe((state.getDisplayName() == null || state.getDisplayName().isBlank())
                ? "Save"
                : state.getDisplayName());
        String shortDate = toMarkupSafe(formatShortCreatedAt(state.getCreatedAt()));
        String base = "[#D4A035]" + saveName + "[]  [#B8B0A2]" + shortDate + "[]";

        if (mode != Mode.LOAD) {
            return base;
        }

        String playerNameLine = buildPlayerIdentityLine(state);
        String playerStatsLine = buildPlayerStatsLine(state);

        if (playerNameLine.isEmpty() && playerStatsLine.isEmpty()) {
            return base;
        }
        if (playerNameLine.isEmpty()) {
            return base + "\n" + playerStatsLine;
        }
        if (playerStatsLine.isEmpty()) {
            return base + "\n" + playerNameLine;
        }
        return base + "\n" + playerNameLine + "\n" + playerStatsLine;
    }

    private static String buildPlayerIdentityLine(SaveState state) {
        if (state == null) {
            return "";
        }
        String playerName = toMarkupSafe(state.getPlayerName() == null ? "" : state.getPlayerName().trim());
        String locationName = toMarkupSafe(resolveMapLocationName(state.getMapKey()));

        if (!playerName.isEmpty() && !locationName.isEmpty()) {
            return "[#F4D175]" + playerName + "[]  [#7CC8FF]" + locationName + "[]";
        }
        if (!playerName.isEmpty()) {
            return "[#F4D175]" + playerName + "[]";
        }
        return "[#7CC8FF]" + locationName + "[]";
    }

    private static String resolveMapLocationName(String mapKey) {
        if (mapKey == null || mapKey.isBlank()) {
            return "";
        }
        MapAsset mapAsset = MapAsset.fromKey(mapKey);
        if (mapAsset == null) {
            return "";
        }
        String label = MapAssetName.fromAsset(mapAsset);
        return label == null ? "" : label;
    }

    private static String buildPlayerStatsLine(SaveState state) {
        if (state == null || state.getParty().isEmpty()) {
            return "";
        }

        SaveState.PartyEntry playerEntry = resolvePrimaryPlayerEntry(state);
        if (playerEntry == null) {
            return "";
        }

        String clawkinName = toMarkupSafe(playerEntry.getName() == null || playerEntry.getName().isBlank()
                ? "Clawkin"
                : playerEntry.getName());
        String levelText = playerEntry.getLevel() > 0 ? "Level " + playerEntry.getLevel() : "";
        String hpText = playerEntry.getMaxHp() > 0
                ? "HP " + Math.max(0, playerEntry.getCurrentHp()) + "/" + playerEntry.getMaxHp()
                : "";

        if (!levelText.isEmpty() && !hpText.isEmpty()) {
            return "[#C9C2B6]" + clawkinName + "[]  [#ECCD61]" + levelText + "[]  [#82D89B]" + hpText + "[]";
        }
        if (!levelText.isEmpty()) {
            return "[#C9C2B6]" + clawkinName + "[]  [#ECCD61]" + levelText + "[]";
        }
        return "[#C9C2B6]" + clawkinName + "[]  [#82D89B]" + hpText + "[]";
    }

    private static SaveState.PartyEntry resolvePrimaryPlayerEntry(SaveState state) {
        if (state == null || state.getParty().isEmpty()) {
            return null;
        }
        int activeIndex = state.getActiveClawkinIndex();
        if (activeIndex >= 0 && activeIndex < state.getParty().size()) {
            return state.getParty().get(activeIndex);
        }
        return state.getParty().get(0);
    }

    private void setSelectedIndex(int index) {
        if (saveStates.isEmpty()) {
            return;
        }
        selectedIndex = Math.max(0, Math.min(index, saveStates.size() - 1));
        updateSelectionVisuals();
    }

    private void updateSelectionVisuals() {
        for (int i = 0; i < entryLabels.size(); i++) {
            boolean selected = i == selectedIndex;
            String text = i < entryTexts.size() ? entryTexts.get(i) : "";
            entryLabels.get(i).setText(text);
            entryLabels.get(i).setColor(Color.WHITE);
            if (i < entryRows.size()) {
                entryRows.get(i).setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(
                        selected ? Color.valueOf("#3F3725") : Color.valueOf("#35353B"),
                        8,
                        selected ? 2 : 1));
            }
        }
    }

    private void moveSelection(int step) {
        if (saveStates.isEmpty()) {
            return;
        }
        int size = saveStates.size();
        selectedIndex = (selectedIndex + step + size) % size;
        updateSelectionVisuals();
        soundHelper.playSound(SoundEffect.UI_HOVER);
    }

    private boolean isMenuCancelPressed() {
        return InputConventions.isCancelJustPressed();
    }

    private boolean isMenuUpPressed() {
        return InputConventions.isMenuUpJustPressed();
    }

    private boolean isMenuDownPressed() {
        return InputConventions.isMenuDownJustPressed();
    }

    private boolean isMenuLeftPressed() {
        return InputConventions.isMenuLeftJustPressed();
    }

    private boolean isMenuRightPressed() {
        return InputConventions.isMenuRightJustPressed();
    }

    private boolean isMenuConfirmPressed() {
        return InputConventions.isInteractJustPressed();
    }

    private boolean isDeletePressed() {
        return Gdx.input.isKeyJustPressed(Keys.DEL) || Gdx.input.isKeyJustPressed(Keys.FORWARD_DEL);
    }

    private void loadFonts() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("font/earthbound-dialogue-gold.otf"));

        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 38;
        titleParam.borderWidth = 2.0f;
        titleParam.borderColor = Color.BLACK;
        titleFont = generator.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter bodyParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        bodyParam.size = 23;
        bodyParam.borderWidth = 1.0f;
        bodyParam.borderColor = Color.BLACK;
        bodyFont = generator.generateFont(bodyParam);

        FreeTypeFontGenerator.FreeTypeFontParameter buttonParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        buttonParam.size = 26;
        buttonParam.borderWidth = 1.25f;
        buttonParam.borderColor = Color.BLACK;
        buttonFont = generator.generateFont(buttonParam);

        generator.dispose();
    }

    private void updateButtonStates() {
        boolean hasSelection = !saveStates.isEmpty();
        primaryButton.setDisabled(!hasSelection);
        deleteButton.setDisabled(!hasSelection);
        selectedActionButtonIndex = findFirstEnabledActionButton();
        updateActionButtonSelectionVisuals();
    }

    private int findFirstEnabledActionButton() {
        for (int i = 0; i < actionButtons.size(); i++) {
            TextButton button = actionButtons.get(i);
            if (button != null && !button.isDisabled()) {
                return i;
            }
        }
        return 0;
    }

    private void handleInput() {
        if (isInputGuardActive()) {
            return;
        }

        if (isMenuCancelPressed()) {
            handleBack();
            return;
        }

        if (saveStates.size() > 1 && isMenuUpPressed()) {
            moveSelection(-1);
            return;
        }
        if (saveStates.size() > 1 && isMenuDownPressed()) {
            moveSelection(1);
            return;
        }

        if (isMenuLeftPressed()) {
            moveActionSelection(-1);
        } else if (isMenuRightPressed()) {
            moveActionSelection(1);
        } else if (isMenuConfirmPressed()) {
            triggerSelectedActionButton();
        } else if (isDeletePressed() && !saveStates.isEmpty()) {
            handleDelete();
        }
    }

    private void handlePrimaryAction() {
        if (saveStates.isEmpty()) {
            return;
        }
        if (mode == Mode.LOAD) {
            SaveState selected = saveStates.get(selectedIndex);
            SaveState loaded = saveStateManager.loadSaveState(selected.getFileName());
            if (loaded == null) {
                setStatus("Failed to load save state.");
                return;
            }
            if (loadConsumer != null) {
                loadConsumer.onLoad(loaded);
            }
            return;
        }

        handleOverwrite();
    }

    private void handleSaveNew() {
        if (saveStateProvider == null) {
            setStatus("No save provider available.");
            return;
        }
        SaveState newState = saveStateProvider.buildSaveState();
        if (newState == null) {
            setStatus("Save failed.");
            return;
        }
        SaveState created = saveStateManager.createSaveState(newState);
        if (created == null) {
            setStatus("Save failed.");
            return;
        }
        setStatus("Game saved.");
        refreshSaveList();
    }

    private void handleOverwrite() {
        if (saveStateProvider == null || saveStates.isEmpty()) {
            return;
        }
        SaveState selected = saveStates.get(selectedIndex);
        SaveState newState = saveStateProvider.buildSaveState();
        if (newState == null) {
            setStatus("Save failed.");
            return;
        }
        newState.setDisplayName(selected.getDisplayName());
        newState.setCreatedAt(selected.getCreatedAt());
        if (!saveStateManager.updateSaveState(selected.getFileName(), newState)) {
            setStatus("Overwrite failed.");
            return;
        }
        setStatus("Game saved.");
        refreshSaveList();
    }

    private void handleDelete() {
        if (saveStates.isEmpty()) {
            return;
        }
        SaveState selected = saveStates.get(selectedIndex);
        if (!saveStateManager.deleteSaveState(selected.getFileName())) {
            setStatus("Delete failed.");
            return;
        }
        setStatus("Save deleted.");
        refreshSaveList();
    }

    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    private void updateInputGuard(float delta) {
        if (inputGuardTimer > 0f) {
            inputGuardTimer = Math.max(0f, inputGuardTimer - delta);
        }
        if (waitForConfirmRelease && !isMenuConfirmHeld()) {
            waitForConfirmRelease = false;
        }
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

    private Skin buildSkin() {
        Skin skin = new Skin();
        BitmapFont skinFont = buttonFont;
        skin.add("default-font", skinFont);

        TextButtonStyle buttonStyle = new TextButtonStyle(
                RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(Color.valueOf("#C19253"), 8, 1, 0.75f),
                RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(Color.valueOf("#8F6232"), 8, 1, 0.70f),
                RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(Color.valueOf("#D7AE63"), 8, 1, 0.85f),
                skinFont);
        buttonStyle.checked = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
                Color.valueOf("#ECCD61"), 8, 1, 0.90f);
        buttonStyle.checkedOver = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
                Color.valueOf("#F4DA83"), 8, 1, 0.92f);
        buttonStyle.checkedDown = buttonStyle.checked;
        buttonStyle.disabled = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
                Color.valueOf("#73624B"), 8, 1, 0.65f);
        buttonStyle.fontColor = Color.valueOf("#1E1912");
        buttonStyle.overFontColor = Color.valueOf("#1E1912");
        buttonStyle.downFontColor = Color.valueOf("#1E1912");
        buttonStyle.checkedFontColor = Color.valueOf("#1E1912");
        buttonStyle.disabledFontColor = Color.valueOf("#C7B89E");
        skin.add("default", buttonStyle, TextButtonStyle.class);

        ScrollPaneStyle scrollStyle = new ScrollPaneStyle();
        scrollStyle.background = RoundedPanelDrawable.createRoundedPanelWithStroke(
                Color.valueOf("#222228"), 8, 1);
        scrollStyle.vScroll = new ColorDrawable(Color.valueOf("#4A4338"));
        scrollStyle.vScrollKnob = new ColorDrawable(Color.valueOf("#C19253"));
        skin.add("default", scrollStyle, ScrollPaneStyle.class);

        return skin;
    }

    private void registerActionButtons() {
        actionButtons.clear();
        actionButtonActions.clear();
        addActionButton(primaryButton, this::handlePrimaryAction);
        if (saveNewButton != null) {
            addActionButton(saveNewButton, this::handleSaveNew);
        }
        addActionButton(deleteButton, this::handleDelete);
        addActionButton(backButton, this::handleBack);
    }

    private void addActionButton(TextButton button, Runnable action) {
        if (button == null) {
            return;
        }
        actionButtons.add(button);
        actionButtonActions.add(action);
        button.addListener(new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                int idx = actionButtons.indexOf(button);
                if (idx >= 0) {
                    setSelectedActionButton(idx, false);
                }
            }
        });
    }

    private void setSelectedActionButton(int index, boolean playHoverSound) {
        if (actionButtons.isEmpty()) {
            return;
        }
        if (index < 0 || index >= actionButtons.size()) {
            return;
        }
        TextButton target = actionButtons.get(index);
        if (target == null || target.isDisabled()) {
            return;
        }
        if (selectedActionButtonIndex == index) {
            return;
        }
        selectedActionButtonIndex = index;
        updateActionButtonSelectionVisuals();
        if (playHoverSound) {
            soundHelper.playSound(SoundEffect.UI_HOVER);
        }
    }

    private void moveActionSelection(int step) {
        if (actionButtons.isEmpty()) {
            return;
        }
        int size = actionButtons.size();
        int nextIndex = selectedActionButtonIndex;
        for (int i = 0; i < size; i++) {
            nextIndex = (nextIndex + step + size) % size;
            TextButton button = actionButtons.get(nextIndex);
            if (button != null && !button.isDisabled()) {
                setSelectedActionButton(nextIndex, true);
                return;
            }
        }
    }

    private void updateActionButtonSelectionVisuals() {
        for (int i = 0; i < actionButtons.size(); i++) {
            TextButton button = actionButtons.get(i);
            if (button != null) {
                button.setChecked(i == selectedActionButtonIndex);
            }
        }
    }

    private void triggerSelectedActionButton() {
        if (actionButtons.isEmpty()) {
            return;
        }
        if (selectedActionButtonIndex < 0 || selectedActionButtonIndex >= actionButtons.size()) {
            return;
        }
        TextButton selectedButton = actionButtons.get(selectedActionButtonIndex);
        if (selectedButton == null || selectedButton.isDisabled()) {
            soundHelper.playSound(SoundEffect.UI_ERROR);
            return;
        }
        if (selectedActionButtonIndex < actionButtonActions.size()) {
            Runnable action = actionButtonActions.get(selectedActionButtonIndex);
            if (action != null) {
                soundHelper.playSound(SoundEffect.UI_SELECT);
                action.run();
            }
        }
    }

    private void updateControlsHint() {
        if (controlsHintLabel == null) {
            return;
        }
        if (saveStates.size() <= 1) {
            controlsHintLabel.setText("Left/Right: Select Button    Enter/Z: Confirm    Esc/X: Back    Del: Delete");
            return;
        }
        controlsHintLabel.setText("Up/Down: Save Slot    Left/Right: Select Button    Enter/Z: Confirm    Esc/X: Back");
    }

    private static String formatShortCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return "Unknown time";
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(createdAt, SAVE_TIMESTAMP_FORMAT);
            return parsed.format(SAVE_TIMESTAMP_SHORT_FORMAT);
        } catch (Exception ignored) {
            return createdAt;
        }
    }

    private static String toMarkupSafe(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("[", "(").replace("]", ")");
    }
}
