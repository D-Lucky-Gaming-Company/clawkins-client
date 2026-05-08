package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;

import github.dluckycompany.clawkins.asset.MapAsset;
import github.dluckycompany.clawkins.asset.MapAssetName;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
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
    private BitmapFont font;

    private Mode mode = Mode.LOAD;
    private SaveStateProvider saveStateProvider;
    private SaveStateConsumer loadConsumer;
    private Runnable onBack;

    private final List<SaveState> saveStates = new ArrayList<>();
    private final List<Label> entryLabels = new ArrayList<>();
    private int selectedIndex = 0;

    private Label statusLabel;
    private Table listTable;
    private TextButton primaryButton;
    private TextButton saveNewButton;
    private TextButton deleteButton;
    private TextButton backButton;
    private float inputGuardTimer;
    private boolean waitForConfirmRelease;

    private static final float ACTION_BUTTON_WIDTH = 190f;
    private static final float ACTION_BUTTON_HEIGHT = 56f;
    private static final float ACTION_BUTTON_SCALE = 1.1f;
    
    // Fixed virtual UI resolution matching InventoryScreen
    private static final float VIRTUAL_UI_WIDTH = 800f;
    private static final float VIRTUAL_UI_HEIGHT = 600f;
    private static final float PANEL_WIDTH = 700f;
    private static final float PANEL_HEIGHT = 500f;
    private static final float SCREEN_ENTRY_INPUT_GUARD_SECONDS = 0.15f;

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

        if (skin == null) {
            skin = buildSkin();
        }

        if (font == null) {
            font = new BitmapFont();
        }

        buildUI();
        refreshSaveList();

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
        }
        if (font != null) {
            font.dispose();
        }
        if (skin != null) {
            skin.dispose();
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
            new Label.LabelStyle(font, Color.valueOf("#E8E6E3")));
        title.setFontScale(1.4f);
        panel.add(title).padBottom(12f).row();
        
        // Save list container with proper sizing
        listTable = new Table();
        listTable.defaults().left().expandX().fillX();
        
        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).width(PANEL_WIDTH - 40f).height(PANEL_HEIGHT - 180f).padTop(8f).row();
        
        // Status label
        statusLabel = new Label("", new Label.LabelStyle(font, Color.valueOf("#C9C2B6")));
        panel.add(statusLabel).padTop(8f).row();
        
        // Action buttons
        Table actions = new Table();
        actions.defaults().width(ACTION_BUTTON_WIDTH).height(ACTION_BUTTON_HEIGHT).padRight(12f);

        primaryButton = new TextButton(mode == Mode.LOAD ? "Load" : "Overwrite", skin);
        primaryButton.getLabel().setFontScale(ACTION_BUTTON_SCALE);
        soundHelper.addButtonSounds(primaryButton, () -> handlePrimaryAction());

        if (mode == Mode.SAVE) {
            saveNewButton = new TextButton("Save New", skin);
            saveNewButton.getLabel().setFontScale(ACTION_BUTTON_SCALE);
            soundHelper.addButtonSounds(saveNewButton, () -> handleSaveNew());
        }

        deleteButton = new TextButton("Delete", skin);
        deleteButton.getLabel().setFontScale(ACTION_BUTTON_SCALE);
        soundHelper.addButtonSounds(deleteButton, () -> handleDelete());

        backButton = new TextButton("Back", skin);
        backButton.getLabel().setFontScale(ACTION_BUTTON_SCALE);
        soundHelper.addButtonSounds(backButton, () -> handleBack(), SoundEffect.UI_BACK);

        actions.add(primaryButton);
        if (saveNewButton != null) {
            actions.add(saveNewButton);
        }
        actions.add(deleteButton);
        actions.add(backButton);

        panel.add(actions).padTop(12f).row();
        
        // Add centered panel to root
        root.add(panel).width(PANEL_WIDTH).height(PANEL_HEIGHT).center();

        stage.addActor(root);
    }

    private void refreshSaveList() {
        saveStates.clear();
        saveStates.addAll(saveStateManager.listSaveStates());
        listTable.clear();
        entryLabels.clear();

        if (saveStates.isEmpty()) {
            Label empty = new Label("No save states found.", new Label.LabelStyle(font, Color.valueOf("#C9C2B6")));
            listTable.add(empty).left().padBottom(6f).row();
            selectedIndex = 0;
            updateButtonStates();
            return;
        }

        for (int i = 0; i < saveStates.size(); i++) {
            SaveState state = saveStates.get(i);
            String labelText = buildSaveEntryLabel(state);
            Label label = new Label(labelText, new Label.LabelStyle(font, Color.valueOf("#C9C2B6")));
            label.setFontScale(1.05f);
            int index = i;
            label.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    setSelectedIndex(index);
                }
            });
            entryLabels.add(label);
            listTable.add(label).left().padBottom(6f).row();
        }

        if (selectedIndex >= saveStates.size()) {
            selectedIndex = Math.max(0, saveStates.size() - 1);
        }
        updateSelectionVisuals();
        updateButtonStates();
    }

    private String buildSaveEntryLabel(SaveState state) {
        String base = state.getDisplayName() + " | " + state.getCreatedAt();

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
        String playerName = state.getPlayerName() == null ? "" : state.getPlayerName().trim();
        String locationName = resolveMapLocationName(state.getMapKey());

        if (!playerName.isEmpty() && !locationName.isEmpty()) {
            return playerName + " | " + locationName;
        }
        if (!playerName.isEmpty()) {
            return playerName;
        }
        return locationName;
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

        String levelText = playerEntry.getLevel() > 0 ? "Level " + playerEntry.getLevel() : "";
        String hpText = playerEntry.getMaxHp() > 0
                ? "HP " + Math.max(0, playerEntry.getCurrentHp()) + "/" + playerEntry.getMaxHp()
                : "";

        if (!levelText.isEmpty() && !hpText.isEmpty()) {
            return levelText + "  " + hpText;
        }
        if (!levelText.isEmpty()) {
            return levelText;
        }
        return hpText;
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
            entryLabels.get(i).setColor(selected ? Color.valueOf("#ECCD61") : Color.valueOf("#C9C2B6"));
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = !saveStates.isEmpty();
        primaryButton.setDisabled(!hasSelection);
        deleteButton.setDisabled(!hasSelection);
    }

    private void handleInput() {
        if (isInputGuardActive()) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            handleBack();
            return;
        }

        if (saveStates.isEmpty()) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Keys.W)
                || Gdx.input.isKeyJustPressed(Keys.UP)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_UP)) {
            setSelectedIndex(selectedIndex - 1);
        } else if (Gdx.input.isKeyJustPressed(Keys.S)
                || Gdx.input.isKeyJustPressed(Keys.DOWN)
                || Gdx.input.isKeyJustPressed(Keys.DPAD_DOWN)) {
            setSelectedIndex(selectedIndex + 1);
        } else if (Gdx.input.isKeyJustPressed(Keys.Z)
                || Gdx.input.isKeyJustPressed(Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Keys.NUMPAD_ENTER)
                || Gdx.input.isKeyJustPressed(Keys.BUTTON_A)) {
            handlePrimaryAction();
        } else if (Gdx.input.isKeyJustPressed(Keys.DEL) || Gdx.input.isKeyJustPressed(Keys.FORWARD_DEL)) {
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
        BitmapFont skinFont = new BitmapFont();
        skin.add("default-font", skinFont);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.valueOf("#C9A46D"));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        TextureRegionDrawable drawable = new TextureRegionDrawable(texture);
        TextButtonStyle buttonStyle = new TextButtonStyle(drawable, drawable, drawable, skinFont);
        buttonStyle.fontColor = Color.valueOf("#1E1912");
        skin.add("default", buttonStyle, TextButtonStyle.class);

        Pixmap knobPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(Color.valueOf("#8F6B3B"));
        knobPixmap.fill();
        Texture knobTexture = new Texture(knobPixmap);
        knobPixmap.dispose();

        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(knobTexture);
        ScrollPaneStyle scrollStyle = new ScrollPaneStyle();
        scrollStyle.background = drawable;
        scrollStyle.vScrollKnob = knobDrawable;
        scrollStyle.vScroll = drawable;
        skin.add("default", scrollStyle, ScrollPaneStyle.class);

        return skin;
    }
}
