package github.kinuseka.testproject.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.maps.MapObject;

/**
 * Stores a reference back to the original Tiled map object that spawned this
 * entity.
 * Useful for:
 * - Debugging (knowing which map object an entity came from)
 * - Reading additional custom properties at runtime
 * - Future features like map transitions or object respawning
 */
public class Tiled implements Component {
    public static final ComponentMapper<Tiled> MAPPER = ComponentMapper.getFor(Tiled.class);

    private final int id;
    private final MapObject mapObjectRef;

    public Tiled(MapObject mapObjectRef) {
        this.id = mapObjectRef.getProperties().get("id", -1, Integer.class);
        this.mapObjectRef = mapObjectRef;
    }

    public int getId() {
        return id;
    }

    public MapObject getMapObjectRef() {
        return mapObjectRef;
    }
}
