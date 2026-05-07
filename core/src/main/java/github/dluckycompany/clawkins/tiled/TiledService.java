package github.dluckycompany.clawkins.tiled;

import java.util.function.Consumer;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;

import github.dluckycompany.clawkins.asset.AssetService;
import github.dluckycompany.clawkins.asset.MapAsset;

/**
 * Parses a TiledMap's layers and dispatches game objects to registered
 * consumers.
 *
 * How it works:
 * 1. You register a consumer via setLoadObjectConsumer()
 * 2. You call setMap(tiledMap)
 * 3. TiledService iterates through the map's layers
 * 4. For supported object layers, it calls your consumer for each map object
 *
 * This is where the map data becomes game entities — the consumer
 * (TiledObjectConfigurator)
 * turns each map object into an ECS entity with the right components.
 */
public class TiledService {
    private final AssetService assetService;

    private TiledMap currentMap;
    private Consumer<TiledMap> mapChangeConsumer;
    private Consumer<MapObject> loadObjectConsumer;

    public TiledService(AssetService assetService) {
        this.assetService = assetService;
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
        // Store map reference for potential future tracking needs
        // though current implementation doesn't reference it directly
        this.currentMap = tiledMap;
        loadMapObjects(tiledMap);

        if (this.mapChangeConsumer != null) {
            this.mapChangeConsumer.accept(tiledMap);
        }
    }

    /**
     * Gets the currently active map, if any.
     */
    public TiledMap getCurrentMap() {
        return this.currentMap;
    }

    /**
     * Register a consumer that gets called whenever the map changes.
     * Typically used to hand the map to RenderSystem and CameraSystem.
     */
    public void setMapChangeConsumer(Consumer<TiledMap> mapChangeConsumer) {
        this.mapChangeConsumer = mapChangeConsumer;
    }

    /**
     * Register a consumer for each game object found in supported object layers.
     * Typically this is TiledObjectConfigurator::onLoadObject.
     */
    public void setLoadObjectConsumer(Consumer<MapObject> loadObjectConsumer) {
        this.loadObjectConsumer = loadObjectConsumer;
    }

    /**
     * Iterates through all map layers, dispatching objects from supported object
     * layers.
     */
    private void loadMapObjects(TiledMap tiledMap) {
        for (MapLayer layer : tiledMap.getLayers()) {
            if (isSupportedObjectLayer(layer)) {
                loadObjectLayer(layer);
            }
            // Other layer types (ground, background, foreground) are handled by
            // RenderSystem
        }
    }

    /**
     * Processes each object in a map object layer.
     */
    private void loadObjectLayer(MapLayer objectLayer) {
        if (loadObjectConsumer == null)
            return;

        for (MapObject mapObject : objectLayer.getObjects()) {
            loadObjectConsumer.accept(mapObject);
        }
    }

    private static boolean isSupportedObjectLayer(MapLayer layer) {
        if (layer == null || layer.getObjects() == null || layer.getObjects().getCount() == 0) {
            return false;
        }
        String name = layer.getName();
        return "objects".equals(name) || "barrier".equals(name);
    }
}
