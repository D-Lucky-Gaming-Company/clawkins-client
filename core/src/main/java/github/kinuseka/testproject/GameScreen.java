package github.kinuseka.testproject;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import github.kinuseka.testproject.asset.AssetService;
import github.kinuseka.testproject.asset.MapAsset;
import github.kinuseka.testproject.system.CameraSystem;
import github.kinuseka.testproject.system.MoveSystem;
import github.kinuseka.testproject.system.RenderSystem;
import github.kinuseka.testproject.tiled.TiledObjectConfigurator;
import github.kinuseka.testproject.tiled.TiledService;

import java.util.function.Consumer;

public class GameScreen extends ScreenAdapter {
    private final Main game;
    private final Engine engine;
    private final TiledService tiledService;
    private final TiledObjectConfigurator tiledObjectConfigurator;

    public GameScreen(Main game) {
        this.game = game;
        AssetService assetService = game.getAssetService();
        Viewport viewport = game.getViewport();
        OrthographicCamera camera = game.getCamera();
        Batch batch = game.getBatch();

        // Create and configure the Ashley ECS Engine
        this.engine = new Engine();

        // Register systems in processing order
        this.engine.addSystem(new MoveSystem());
        this.engine.addSystem(new CameraSystem(camera));
        this.engine.addSystem(new RenderSystem(batch, viewport, camera));

        // Tiled map services
        this.tiledService = new TiledService(assetService);
        this.tiledObjectConfigurator = new TiledObjectConfigurator(engine);

        // Wire up callbacks:
        // - When a map object is found → TiledObjectConfigurator creates an entity
        // - When the map changes → hand it to RenderSystem and CameraSystem
        this.tiledService.setLoadObjectConsumer(tiledObjectConfigurator::onLoadObject);

        Consumer<TiledMap> renderConsumer = engine.getSystem(RenderSystem.class)::setMap;
        Consumer<TiledMap> cameraConsumer = engine.getSystem(CameraSystem.class)::setMap;
        this.tiledService.setMapChangeConsumer(renderConsumer.andThen(cameraConsumer));
    }

    @Override
    public void show() {
        // Load the map and hand it to TiledService.
        // This triggers: object parsing → entity spawning → map change notification to
        // systems.
        TiledMap startMap = this.tiledService.loadMap(MapAsset.MAIN);
        this.tiledService.setMap(startMap);
    }

    @Override
    public void render(float delta) {
        // Cap delta to avoid spiral-of-death on lag spikes
        delta = Math.min(1 / 30f, delta);

        // This single call updates ALL systems in order:
        // MoveSystem → CameraSystem → RenderSystem
        engine.update(delta);
    }

    @Override
    public void hide() {
        engine.removeAllEntities();
    }

    @Override
    public void dispose() {
        // Dispose any systems that implement Disposable (e.g. RenderSystem)
        for (EntitySystem system : engine.getSystems()) {
            if (system instanceof Disposable disposable) {
                disposable.dispose();
            }
        }
        engine.removeAllEntities();
    }
}
