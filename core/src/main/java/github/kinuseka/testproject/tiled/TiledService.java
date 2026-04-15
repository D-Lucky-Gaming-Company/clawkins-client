package github.kinuseka.testproject.tiled;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.utils.GdxRuntimeException;
import github.kinuseka.testproject.asset.AssetService;
import github.kinuseka.testproject.asset.MapAsset;

import java.util.function.Consumer;

/**
 * Parses a TiledMap's layers and dispatches game objects to registered
 * consumers.
 *
 * How it works:
 * 1. You register a consumer via setLoadObjectConsumer()
 * 2. You call setMap(tiledMap)
 * 3. TiledService iterates through the map's layers
 * 4. For the "objects" layer, it calls your consumer for each
 * TiledMapTileMapObject
 *
 * This is where the map data becomes game entities — the consumer
 * (TiledObjectConfigurator)
 * turns each map object into an ECS entity with the right components.
 */
public class TiledService {
    private final AssetService assetService;

    private TiledMap currentMap;
    private Consumer<TiledMap> mapChangeConsumer;
    private Consumer<TiledMapTileMapObject> loadObjectConsumer;

    public TiledService(AssetService assetService) {
        this.assetService = assetService;
        this.currentMap = null;
        this.mapChangeConsumer = null;
        this.loadObjectConsumer = null;
    }

    /**
     * Loads a map asset and tags it with the MapAsset enum for future reference.
     */
    public TiledMap loadMap(MapAsset mapAsset) {
        TiledMap tiledMap = this.assetService.load(mapAsset);
        tiledMap.getProperties().put("mapAsset", mapAsset);
        return tiledMap;
    }

    /**
     * Sets the active map: parses its objects and notifies the map-change consumer.
     */
    public void setMap(TiledMap tiledMap) {
        this.currentMap = tiledMap;
        loadMapObjects(tiledMap);

        if (this.mapChangeConsumer != null) {
            this.mapChangeConsumer.accept(tiledMap);
        }
    }

    /**
     * Register a consumer that gets called whenever the map changes.
     * Typically used to hand the map to RenderSystem and CameraSystem.
     */
    public void setMapChangeConsumer(Consumer<TiledMap> mapChangeConsumer) {
        this.mapChangeConsumer = mapChangeConsumer;
    }

    /**
     * Register a consumer for each game object found in the "objects" layer.
     * Typically this is TiledObjectConfigurator::onLoadObject.
     */
    public void setLoadObjectConsumer(Consumer<TiledMapTileMapObject> loadObjectConsumer) {
        this.loadObjectConsumer = loadObjectConsumer;
    }

    /**
     * Iterates through all map layers, dispatching objects from the "objects"
     * layer.
     */
    private void loadMapObjects(TiledMap tiledMap) {
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                loadObjectLayer(layer);
            }
            // Other layer types (ground, background, foreground) are handled by
            // RenderSystem
        }
    }

    /**
     * Processes each object in the "objects" layer.
     * Only TiledMapTileMapObject is supported (objects placed from a tileset).
     */
    private void loadObjectLayer(MapLayer objectLayer) {
        if (loadObjectConsumer == null)
            return;

        for (MapObject mapObject : objectLayer.getObjects()) {
            if (mapObject instanceof TiledMapTileMapObject tileMapObject) {
                loadObjectConsumer.accept(tileMapObject);
            } else {
                throw new GdxRuntimeException(
                        "Unsupported object type in 'objects' layer: " + mapObject.getClass().getSimpleName()
                                + ". Only tile objects (placed from a tileset) are supported.");
            }
        }
    }
}
