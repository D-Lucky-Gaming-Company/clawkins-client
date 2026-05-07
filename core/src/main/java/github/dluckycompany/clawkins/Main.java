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
import github.dluckycompany.clawkins.ui.MainMenuScreen;

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
        // Placeholder registrations. Add real files later (safe no-op if files don't exist).
        audioService.registerMusic(MusicTrack.EXPLORATION, "audio/music/exploration.mp3");
        audioService.registerMusic(MusicTrack.EXPLORATION_2, "audio/music/exploration2.mp3");
        audioService.registerMusic(MusicTrack.FLINTLOCK, "audio/music/Flintlock.mp3");
        audioService.registerMusic(MusicTrack.BATTLE, "audio/music/battle.mp3");
        audioService.registerMusic(MusicTrack.MENU, "audio/music/menu.mp3");
        audioService.registerMusic(MusicTrack.VICTORY, "audio/music/victory.mp3");
        audioService.registerMusic(MusicTrack.DEFEAT, "audio/music/defeat.mp3");
        audioService.registerSound(SoundEffect.CONFIRM, "audio/sfx/confirm.wav");
        audioService.registerSound(SoundEffect.CANCEL, "audio/sfx/cancel.wav");
        audioService.registerSound(SoundEffect.HIT, "audio/sfx/hit.wav");
        audioService.registerSound(SoundEffect.ENCOUNTER, "audio/sfx/encounter.wav");
        audioService.registerSound(SoundEffect.AREA_NAME_DISPLAY, "sfx/misc/area_name.mp3");
        this.glProfiler = new GLProfiler(Gdx.graphics);
        this.glProfiler.enable();

        // Add screens to cache
        addScreen(new MainMenuScreen(batch,
            () -> startNewGame(),
            () -> continueGame(),
            () -> exitGame()
        ));
        addScreen(new GameScreen(this));

        // Show main menu first
        setScreen(MainMenuScreen.class);
    }

    /**
     * Callback when "NEW GAME" button is clicked.
     */
    private void startNewGame() {
        Gdx.app.log("Main", "Start New Game");
        setScreen(GameScreen.class);
    }

    /**
     * Callback when "CONTINUE" button is clicked.
     */
    private void continueGame() {
        Gdx.app.log("Main", "Continue Game");
        // TODO: load save file and transition to GameScreen
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

    @Override
    public void render() {
        glProfiler.reset();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        super.render();

        Gdx.graphics.setTitle("Test Game - Draw Calls " + glProfiler.getDrawCalls());
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
}
