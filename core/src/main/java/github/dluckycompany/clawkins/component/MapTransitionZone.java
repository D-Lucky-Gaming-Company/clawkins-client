package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Rectangle;

/**
 * Marks an entity as a map transition zone.
 * Stores the world-space trigger bounds and the destination map/anchor info.
 * These entities are created from rectangle objects in Tiled trigger layers.
 */
public class MapTransitionZone implements Component {
    public static final ComponentMapper<MapTransitionZone> MAPPER =
        ComponentMapper.getFor(MapTransitionZone.class);

    private final Rectangle worldBounds;
    private final String transitionId;
    private final String targetMap;
    private final String targetTransitionId;

    public MapTransitionZone(Rectangle worldBounds, String transitionId,
                             String targetMap, String targetTransitionId) {
        this.worldBounds = worldBounds;
        this.transitionId = transitionId;
        this.targetMap = targetMap;
        this.targetTransitionId = targetTransitionId;
    }

    public Rectangle getWorldBounds() {
        return worldBounds;
    }

    public String getTransitionId() {
        return transitionId;
    }

    public String getTargetMap() {
        return targetMap;
    }

    public String getTargetTransitionId() {
        return targetTransitionId;
    }
}
