package github.kinuseka.testproject.tiled;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Vector2;
import github.kinuseka.testproject.Main;
import github.kinuseka.testproject.component.CameraFollow;
import github.kinuseka.testproject.component.Graphic;
import github.kinuseka.testproject.component.Move;
import github.kinuseka.testproject.component.Player;
import github.kinuseka.testproject.component.Tiled;
import github.kinuseka.testproject.component.Transform;

/**
 * The "entity factory" — turns Tiled map objects into Ashley ECS entities.
 *
 * For each TiledMapTileMapObject it:
 * 1. Creates an Entity
 * 2. Reads position, size, texture from the map object → adds Transform +
 * Graphic
 * 3. Checks the object's name to decide additional components:
 * - "Player" → adds Player, CameraFollow, Move
 * - (future: "NPC", "Chest", etc.)
 * 4. Adds a Tiled component (stores the original map object reference)
 * 5. Adds the entity to the Engine
 *
 * To add support for new object types in the future, add more cases in
 * onLoadObject().
 */
public class TiledObjectConfigurator {
    private static final String TAG = TiledObjectConfigurator.class.getSimpleName();

    private final Engine engine;

    public TiledObjectConfigurator(Engine engine) {
        this.engine = engine;
    }

    /**
     * Called by TiledService for each object in the "objects" layer.
     * Creates and configures an entity from the map object's properties.
     */
    public void onLoadObject(TiledMapTileMapObject tileMapObject) {
        Entity entity = engine.createEntity();
        TiledMapTile tile = tileMapObject.getTile();
        TextureRegion textureRegion = tile.getTextureRegion();

        // --- Transform ---
        // Tiled stores positions in pixels; we convert to world units via UNIT_SCALE
        addTransform(tileMapObject, textureRegion, entity);

        // --- Graphic ---
        entity.add(new Graphic(textureRegion, Color.WHITE.cpy()));

        // --- Tiled reference ---
        entity.add(new Tiled(tileMapObject));

        // --- Object-specific components ---
        configureByName(tileMapObject, entity);

        engine.addEntity(entity);
        Gdx.app.debug(TAG, "Spawned entity: name=" + tileMapObject.getName()
                + " pos=" + Transform.MAPPER.get(entity).getPosition());
    }

    /**
     * Adds a Transform component based on the map object's pixel position and size.
     */
    private void addTransform(TiledMapTileMapObject tileMapObject, TextureRegion region, Entity entity) {
        // Convert pixel coordinates to world coordinates
        float x = tileMapObject.getX() * Main.UNIT_SCALE;
        float y = tileMapObject.getY() * Main.UNIT_SCALE;
        float w = region.getRegionWidth() * Main.UNIT_SCALE;
        float h = region.getRegionHeight() * Main.UNIT_SCALE;

        // Tiled tile objects have their origin at bottom-left, but y points to the
        // bottom
        // of the graphic. We need to shift down by the height so the entity stands on
        // its feet.
        y -= h;

        Vector2 position = new Vector2(x, y);
        Vector2 size = new Vector2(w, h);
        Vector2 scaling = new Vector2(tileMapObject.getScaleX(), tileMapObject.getScaleY());
        float rotation = tileMapObject.getRotation();

        entity.add(new Transform(position, 1, size, scaling, rotation));
    }

    /**
     * Configures object-type-specific components based on the object's name.
     * Extend this method when you add new object types.
     */
    private void configureByName(TiledMapTileMapObject tileMapObject, Entity entity) {
        String name = tileMapObject.getName();
        if (name == null || name.isBlank()) {
            return;
        }

        switch (name) {
            case "Player" -> {
                entity.add(new Player());
                entity.add(new CameraFollow());
                entity.add(new Move(3f)); // 3 tiles/sec
                Gdx.app.debug(TAG, "Configured as Player with CameraFollow and Move");
            }
            // Future object types go here:
            // case "NPC" -> { ... }
            // case "Chest" -> { ... }
            default ->
                Gdx.app.debug(TAG, "Unknown object name: " + name + " — spawned with default components only");
        }
    }
}
