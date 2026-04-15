package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.Graphic;
import github.dluckycompany.clawkins.component.Transform;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the tiled map and all entities that have [Transform + Graphic].
 *
 * Render order per frame:
 * 1. Apply viewport
 * 2. Draw tiled map background layers (everything before the "objects" layer,
 *    excluding the "elements" layer)
 * 3. Draw the "elements" layer interleaved with entities using Y-sorting so
 *    that entities appear in front of or behind elements tiles depending on
 *    their world Y position (RPG top-down convention).
 *    If the map has no "elements" layer, entities are drawn as a plain
 *    Y-sorted pass (original behaviour).
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

    // -- NEW: extracted "elements" layer for Y-sort interleaving --
    private TiledMapTileLayer elementsLayer;
    private final List<ElementTile> elementTiles = new ArrayList<>();

    private static class ElementTile implements Comparable<ElementTile> {
        float x, y;
        float sortY;
        TiledMapTile tile;
        boolean flipX, flipY;
        int rotation;

        @Override
        public int compareTo(ElementTile other) {
            return Float.compare(other.sortY, this.sortY);
        }
    }

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

        // Draw background map layers (unchanged)
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        forceSort();

        // -- CHANGED: if an "elements" layer exists, interleave it with entities;
        //    otherwise fall back to the original plain entity pass. --
        if (elementsLayer != null) {
            renderElementsWithEntityYSort(deltaTime);
        } else {
            super.update(deltaTime);
        }

        // Draw foreground map layers (unchanged)
        batch.setColor(Color.WHITE);
        fgdLayers.forEach(tiledRenderer::renderMapLayer);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Original processEntity — completely unchanged
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // NEW: Y-sort interleaved render for the "elements" layer
    // -------------------------------------------------------------------------

    private void renderElementsWithEntityYSort(float deltaTime) {
        float opacity = elementsLayer != null ? elementsLayer.getOpacity() : 1f;

        ImmutableArray<Entity> sorted = getEntities();
        int entityIdx = 0;
        int entityCount = sorted.size();
        int tileIdx = 0;
        int tileCount = elementTiles.size();

        while (entityIdx < entityCount || tileIdx < tileCount) {
            float entityY = entityIdx < entityCount 
                ? Transform.MAPPER.get(sorted.get(entityIdx)).getPosition().y 
                : -Float.MAX_VALUE;
            float tileY = tileIdx < tileCount 
                ? elementTiles.get(tileIdx).sortY 
                : -Float.MAX_VALUE;

            if (tileY >= entityY) { // tile is further away
                ElementTile t = elementTiles.get(tileIdx);
                batch.setColor(1f, 1f, 1f, opacity);
                
                TextureRegion region = t.tile.getTextureRegion();
                if (region != null) {
                    TextureRegion reg = new TextureRegion(region);
                    if (t.flipX) reg.flip(true, false);
                    if (t.flipY) reg.flip(false, true);

                    float regionWidth = region.getRegionWidth() * Main.UNIT_SCALE;
                    float regionHeight = region.getRegionHeight() * Main.UNIT_SCALE;

                    if (t.rotation == TiledMapTileLayer.Cell.ROTATE_90) {
                        reg.flip(false, true);
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 90f);
                    } else if (t.rotation == TiledMapTileLayer.Cell.ROTATE_180) {
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 180f);
                    } else if (t.rotation == TiledMapTileLayer.Cell.ROTATE_270) {
                        reg.flip(false, true);
                        batch.draw(reg, t.x, t.y, 0, 0, regionWidth, regionHeight, 1f, 1f, 270f);
                    } else {
                        batch.draw(reg, t.x, t.y, regionWidth, regionHeight);
                    }
                }
                tileIdx++;
            } else {
                Entity e = sorted.get(entityIdx);
                batch.setColor(Color.WHITE);
                processEntity(e, deltaTime);
                entityIdx++;
            }
        }
        batch.setColor(Color.WHITE);
    }

    // -------------------------------------------------------------------------
    // setMap — original logic + extraction of the "elements" layer
    // -------------------------------------------------------------------------

    /**
     * Sets the tiled map and sorts its layers into background vs foreground.
     * Everything before the "objects" layer is background; everything after is
     * foreground. The "elements" layer (if present) is extracted separately so
     * it can be Y-sorted with entities — it is NOT added to bgdLayers.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);

        bgdLayers.clear();
        fgdLayers.clear();
        elementsLayer = null;                  // reset each map load
        elementTiles.clear();

        List<MapLayer> currentLayers = bgdLayers;
        for (MapLayer layer : tiledMap.getLayers()) {
            if ("objects".equals(layer.getName())) {
                currentLayers = fgdLayers;
                continue;
            }
            if (layer.getClass().equals(MapLayer.class)) {
                continue;
            }
            // -- NEW: intercept the "elements" layer before it goes into bgdLayers --
            if ("elements".equalsIgnoreCase(layer.getName())
                    && layer instanceof TiledMapTileLayer) {
                elementsLayer = (TiledMapTileLayer) layer;
                continue;
            }
            currentLayers.add(layer);
        }

        if (elementsLayer != null) {
            int mapCols = elementsLayer.getWidth();
            int mapRows = elementsLayer.getHeight();
            float scaledTileW = elementsLayer.getTileWidth() * Main.UNIT_SCALE;
            float scaledTileH = elementsLayer.getTileHeight() * Main.UNIT_SCALE;
            float layerOffsetX = elementsLayer.getOffsetX() * Main.UNIT_SCALE;
            float layerOffsetY = elementsLayer.getOffsetY() * Main.UNIT_SCALE;

            for (int row = 0; row < mapRows; row++) {
                for (int col = 0; col < mapCols; col++) {
                    TiledMapTileLayer.Cell cell = elementsLayer.getCell(col, row);
                    if (cell == null) continue;
                    TiledMapTile tile = cell.getTile();
                    if (tile == null) continue;

                    ElementTile et = new ElementTile();
                    et.tile = tile;
                    et.x = col * scaledTileW + layerOffsetX + tile.getOffsetX() * Main.UNIT_SCALE;
                    et.y = row * scaledTileH + layerOffsetY + tile.getOffsetY() * Main.UNIT_SCALE;
                    et.flipX = cell.getFlipHorizontally();
                    et.flipY = cell.getFlipVertically();
                    et.rotation = cell.getRotation();

                    float anchorY = 0;
                    boolean hasCollision = false;
                    MapObjects objects = tile.getObjects();
                    if (objects != null) {
                        for (MapObject mapObject : objects) {
                            if (mapObject instanceof RectangleMapObject) {
                                Rectangle rect = ((RectangleMapObject) mapObject).getRectangle();
                                anchorY = rect.y * Main.UNIT_SCALE;
                                hasCollision = true;
                                break;
                            } else if (mapObject instanceof PolygonMapObject) {
                                Rectangle rect = ((PolygonMapObject) mapObject).getPolygon().getBoundingRectangle();
                                anchorY = rect.y * Main.UNIT_SCALE;
                                hasCollision = true;
                                break;
                            }
                        }
                    }
                    if (hasCollision) {
                        et.sortY = et.y + anchorY;
                    } else {
                        // Render above the player. Keep relative et.y so overlapping non-collision tiles sort correctly amongst themselves.
                        et.sortY = -1000000f + et.y;
                    }
                    elementTiles.add(et);
                }
            }
            java.util.Collections.sort(elementTiles);
        }
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
    }
}
