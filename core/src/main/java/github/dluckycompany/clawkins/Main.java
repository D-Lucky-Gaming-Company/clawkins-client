package github.dluckycompany.clawkins;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.MusicTrack;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.model.PlayerProfile;
import github.dluckycompany.clawkins.save.SaveStateManager;
import github.dluckycompany.clawkins.ui.CharacterSetupScreen;
import github.dluckycompany.clawkins.ui.EndingCreditsScreen;
import github.dluckycompany.clawkins.ui.LeaderboardScreen;
import github.dluckycompany.clawkins.ui.MainMenuScreen;
import github.dluckycompany.clawkins.ui.SaveStateScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all
 * platforms.
 */
public class Main extends Game {
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;
    public static final float UNIT_SCALE = 1f / 16f;

    private Batch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private AssetService assetService;
    private AudioService audioService;
    private GLProfiler glProfiler;
    private SaveStateManager saveStateManager;
    private SaveStateScreen saveStateScreen;
    private PlayerProfile playerProfile;  // Stores player name and gender from character setup

    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    @Override
    public void create() {
        // Not Over Yet 16:30 of the video:
        // https://www.youtube.com/watch?v=VFdyYJuZr4k&list=PLTKHCDn5RKK8us8DL7OGqgp4rQQByiX0C&index=3
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.assetService = new AssetService(new InternalFileHandleResolver());
        this.audioService = new AudioService();
        this.saveStateManager = new SaveStateManager();
        // Placeholder registrations. Add real files later (safe no-op if files don't exist).
        audioService.registerMusic(MusicTrack.EXPLORATION, "audio/music/exploration.mp3");
        audioService.registerMusic(MusicTrack.EXPLORATION_2, "audio/music/exploration2.mp3");
        audioService.registerMusic(MusicTrack.FLINTLOCK, "audio/music/Flintlock.mp3");
        audioService.registerMusic(MusicTrack.BATTLE, "audio/music/battle.mp3");
        audioService.registerMusic(MusicTrack.MENU, "audio/music/menu.mp3");
        audioService.registerMusic(MusicTrack.VICTORY, "audio/music/victory.mp3");
        audioService.registerMusic(MusicTrack.DEFEAT, "audio/music/defeat.mp3");
        audioService.registerMusic(MusicTrack.COTTAGE, "audio/music/cottage.mp3");
        audioService.registerMusic(MusicTrack.FIELDS, "audio/music/fields.mp3");
        audioService.registerMusic(MusicTrack.MOUNTAIN_PATH, "audio/music/mountain_path.mp3");
        audioService.registerMusic(MusicTrack.NURSERY, "audio/music/nursery.mp3");
        audioService.registerMusic(MusicTrack.BACKALLEY_SHOP, "audio/music/backalley_shop.mp3");
        audioService.registerMusic(MusicTrack.TAMERGROUNDS, "audio/music/tamergrounds.mp3");
        audioService.registerMusic(MusicTrack.BOSS_CERBERUS, "audio/music/BOSS/boss_cerberus.mp3");
        audioService.registerMusic(MusicTrack.BOSS_CERBERUS_DIA, "audio/music/BOSS/boss_cerberus_dia/encounter.mp3");
        audioService.registerMusic(MusicTrack.BOSS_SPARTACUS, "audio/music/BOSS/boss_spartacus.mp3");
        audioService.registerMusic(MusicTrack.BOSS_SANTIRAL, "audio/music/BOSS/boss_santiral.mp3");
        audioService.registerMusic(MusicTrack.BOSS_BERTJR_DIA_FIRST_ENCOUNTER, "audio/music/BOSS/boss_bertjr_dia/first_encounter.mp3");
        audioService.registerMusic(MusicTrack.BOSS_DUKE_DIA_FIRST_ENCOUNTER, "audio/music/BOSS/boss_duke_dia/first_encounter.mp3");
        audioService.registerMusic(MusicTrack.CREDITS, "audio/music/ending.mp3");
        audioService.registerSound(SoundEffect.CONFIRM, "audio/sfx/confirm.wav");
        audioService.registerSound(SoundEffect.CANCEL, "audio/sfx/cancel.wav");
        audioService.registerSound(SoundEffect.HIT, "audio/sfx/hit.wav");
        audioService.registerSound(SoundEffect.BATTLE_ATTACK, "audio/soundEffects/SFX_RPGMaker2000/Attack1.wav");
        audioService.registerSound(SoundEffect.BATTLE_DEFEND, "audio/soundEffects/SFX_RPGMaker2000/Barrier.wav");
        audioService.registerSound(SoundEffect.BATTLE_HEAL, "audio/soundEffects/SFX_RPGMAKER2000/Recovery3.wav");
        audioService.registerSound(SoundEffect.BATTLE_SPECIAL, "audio/soundEffects/SFX_RPGMaker2000/Teleport1.wav");
        audioService.registerSound(SoundEffect.BATTLE_SWITCH, "audio/soundEffects/SFX_RPGMaker2000/Teleport2.wav");
        audioService.registerSound(SoundEffect.BATTLE_ESCAPE, "audio/soundEffects/SFX_RPGMaker2000/Escape.wav");
        audioService.registerSound(SoundEffect.BATTLE_ENEMY_ACTION, "audio/soundEffects/SFX_RPGMaker2000/Attack2.wav");
        audioService.registerSound(SoundEffect.ENCOUNTER, "audio/sfx/encounter.wav");
        audioService.registerSound(SoundEffect.ENEMY_ALERT, "audio/soundEffects/SFX_MayGenko/square channel SFX/ba-da 1.ogg");
        audioService.registerSound(SoundEffect.AREA_NAME_DISPLAY, "audio/soundEffects/mapTransition/area_name.mp3");
        // UI/Menu sounds
        audioService.registerSound(SoundEffect.UI_HOVER, "audio/soundEffects/SFX_MayGenko/square channel SFX/menu beep 1.ogg");
        audioService.registerSound(SoundEffect.UI_SELECT, "audio/soundEffects/SFX_MayGenko/square channel SFX/menu select beep 1.ogg");
        audioService.registerSound(SoundEffect.UI_BACK, "audio/soundEffects/SFX_MayGenko/square channel SFX/menu back 1.ogg");
        audioService.registerSound(SoundEffect.UI_ERROR, "audio/soundEffects/SFX_MayGenko/square channel SFX/menu error 1.ogg");
        audioService.registerSound(SoundEffect.FAILURE_1, "audio/soundEffects/SFX_Others/Failure1.mp3");
        audioService.registerSound(SoundEffect.LEVEL_UP, "audio/soundEffects/SFX_Others/levelUp.mp3");
        audioService.registerSound(SoundEffect.MILESTONE, "audio/soundEffects/SFX_Others/milestone.mp3");
        audioService.registerSound(SoundEffect.PARRY_WIN, "audio/soundEffects/SFX_Others/parryWin.mp3");
        audioService.registerSound(SoundEffect.PARRY_FAIL, "audio/soundEffects/SFX_Others/parryFail.mp3");
        audioService.registerSound(SoundEffect.FALLEN, "audio/soundEffects/SFX_Others/fallen.mp3");
        this.glProfiler = new GLProfiler(Gdx.graphics);
        this.glProfiler.enable();

        // Add screens to cache
        addScreen(new MainMenuScreen(batch,
            () -> startNewGame(),
            () -> continueGame(),
            () -> exitGame(),
            saveStateManager,
            audioService
        ));
        addScreen(new CharacterSetupScreen(this, batch));
        addScreen(new GameScreen(this, null));  // Initial GameScreen with no profile
        addScreen(new EndingCreditsScreen(this));
        addScreen(new LeaderboardScreen(this));
        this.saveStateScreen = new SaveStateScreen(batch, saveStateManager, audioService);
        addScreen(saveStateScreen);

        // Show main menu first
        setScreen(MainMenuScreen.class);
    }

    /**
     * Callback when "NEW GAME" button is clicked.
     */
    private void startNewGame() {
        Gdx.app.log("Main", "Start New Game");
        rebuildGameScreenForFreshSession();
        setScreen(CharacterSetupScreen.class);
    }

    public void rebuildGameScreenForFreshSession() {
        Screen existingGameScreen = screenCache.remove(GameScreen.class);
        if (existingGameScreen != null) {
            existingGameScreen.dispose();
        }
        // Create new GameScreen with current player profile (will be set after character setup)
        addScreen(new GameScreen(this, playerProfile));
    }

    /**
     * Callback when "CONTINUE" button is clicked.
     */
    private void continueGame() {
        Gdx.app.log("Main", "Continue Game");
        openSaveStateScreenForLoad();
    }

    private void openSaveStateScreenForLoad() {
        SaveStateScreen screen = getScreen(SaveStateScreen.class);
        screen.configure(
            SaveStateScreen.Mode.LOAD,
            null,
            saveState -> {
                if (saveState == null) {
                    return;
                }
                GameScreen gameScreen = getScreen(GameScreen.class);
                gameScreen.queueSaveStateLoad(saveState);
                setScreen(GameScreen.class);
            },
            () -> setScreen(MainMenuScreen.class)
        );
        setScreen(SaveStateScreen.class);
    }

    /**
     * Callback when "EXIT GAME" button is clicked.
     */
    private void exitGame() {
        Gdx.app.log("Main", "Exit Game");
        Gdx.app.exit();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    public void addScreen(Screen screen) {
        screenCache.put(screen.getClass(), screen);
    }

    public void setScreen(Class<? extends Screen> screenClass) {
        Screen cachedScreen = screenCache.get(screenClass);
        if (cachedScreen == null) {
            throw new GdxRuntimeException("No screen with class " + screenClass + " found in the screen cache");
        }

        super.setScreen(cachedScreen);
    }

    public <T extends Screen> T getScreen(Class<T> screenClass) {
        Screen cached = screenCache.get(screenClass);
        if (cached == null) {
            throw new GdxRuntimeException("No screen with class " + screenClass + " found in the screen cache");
        }
        return screenClass.cast(cached);
    }

    @Override
    public void render() {
        glProfiler.reset();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render();

        // glProfiler.getDrawCalls() still tracked internally for logic — title removed
        // fpsLogger.log();
    }

    @Override
    public void dispose() {
        screenCache.values().forEach(Screen::dispose);
        screenCache.clear();

        this.batch.dispose();
        this.audioService.dispose();
        this.assetService.debugDiagnostics();
        this.assetService.dispose();
    }

    public Batch getBatch() {
        return batch;
    }

    public AssetService getAssetService() {
        return assetService;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public AudioService getAudioService() {
        return audioService;
    }

    public SaveStateManager getSaveStateManager() {
        return saveStateManager;
    }

    /**
     * Gets the current player profile (name and gender from character setup).
     *
     * @return the player profile, or null if not yet created
     */
    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    /**
     * Sets the player profile (called by CharacterSetupScreen after character creation).
     *
     * @param profile the player profile to store
     */
    public void setPlayerProfile(PlayerProfile profile) {
        this.playerProfile = profile;
        Gdx.app.log("Main", "Player profile set: " + 
            (profile != null ? profile.getName() + " (" + profile.getGender() + ")" : "null"));
    }
}
