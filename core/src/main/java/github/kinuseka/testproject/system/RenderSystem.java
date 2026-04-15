package github.kinuseka.testproject.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import github.kinuseka.testproject.Main;
import github.kinuseka.testproject.component.Graphic;
import github.kinuseka.testproject.component.Transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the tiled map and all entities that have [Transform + Graphic].
 *
 * Render order per frame:
 * 1. Apply viewport
 * 2. Draw tiled map background layers (everything before the "objects" layer)
 * 3. Draw entities sorted by Transform (z → y → x)
 * 4. Draw tiled map foreground layers (everything after the "objects" layer)
 *
 * This system OWNS the OrthogonalTiledMapRenderer and is Disposable.
 */
public class RenderSystem extends SortedIteratingSystem implements Disposable {
    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final OrthogonalTiledMapRenderer tiledRenderer;
    private final List<MapLayer> bgdLayers;
    private final List<MapLayer> fgdLayers;

    public RenderSystem(Batch batch, Viewport viewport, OrthographicCamera camera) {
        super(
                Family.all(Transform.class, Graphic.class).get(),
                Comparator.comparing(Transform.MAPPER::get));

        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
        this.tiledRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, batch);
        this.bgdLayers = new ArrayList<>();
        this.fgdLayers = new ArrayList<>();
    }

    @Override
    public void update(float deltaTime) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        viewport.apply();

        batch.begin();
        batch.setColor(Color.WHITE);
        tiledRenderer.setView(camera);

        // Draw background map layers
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        // Draw entities (sorted by Transform)
        forceSort();
        super.update(deltaTime);

        // Draw foreground map layers
        batch.setColor(Color.WHITE);
        fgdLayers.forEach(tiledRenderer::renderMapLayer);
        batch.end();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        Graphic graphic = Graphic.MAPPER.get(entity);
        if (graphic.getRegion() == null) {
            return;
        }

        Vector2 position = transform.getPosition();
        Vector2 scaling = transform.getScaling();
        Vector2 size = transform.getSize();

        batch.setColor(graphic.getColor());
        batch.draw(
                graphic.getRegion(),
                position.x - (1f - scaling.x) * size.x * 0.5f,
                position.y - (1f - scaling.y) * size.y * 0.5f,
                size.x * 0.5f, size.y * 0.5f,
                size.x, size.y,
                scaling.x, scaling.y,
                transform.getRotationDeg());
    }

    /**
     * Sets the tiled map and sorts its layers into background vs foreground.
     * Everything before the "objects" layer is background; everything after is
     * foreground.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);

        bgdLayers.clear();
        fgdLayers.clear();
        List<MapLayer> currentLayers = bgdLayers;
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                currentLayers = fgdLayers;
                continue;
            }
            if (layer.getClass().equals(MapLayer.class)) {
                continue;
            }
            currentLayers.add(layer);
        }
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
    }
}
