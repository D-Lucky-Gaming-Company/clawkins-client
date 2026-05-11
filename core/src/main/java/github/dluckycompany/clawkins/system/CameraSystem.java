package github.dluckycompany.clawkins.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import github.dluckycompany.clawkins.Main;
import github.dluckycompany.clawkins.component.CameraFollow;
import github.dluckycompany.clawkins.component.Transform;

/**
 * Smoothly follows the entity that has [CameraFollow + Transform].
 * Uses linear interpolation (lerp) so the camera doesn't snap instantly.
 * Clamps to map boundaries so we never show empty space outside the map.
 */
public class CameraSystem extends IteratingSystem {
    private static final float CAM_OFFSET_Y = 1f;

    private final Camera camera;
    private final float smoothingFactor;
    private final Vector2 targetPosition;
    private final Vector2 lastClampedFollowTarget = new Vector2();
    private float mapW;
    private float mapH;
    /** Multiplier on {@link #smoothingFactor} (1 = default follow speed). */
    private float smoothingSpeedMultiplier = 1f;
    /** Added to follow target X before map clamp (world units); negative nudges framing left. */
    private float followTargetBiasWorldX;

    public CameraSystem(Camera camera) {
        super(Family.all(CameraFollow.class, Transform.class).get());
        this.camera = camera;
        this.smoothingFactor = 4f;
        this.targetPosition = new Vector2();
    }

    /**
     * Scales how quickly the camera eases toward the follow target (e.g. {@code 0.25f} for quarter speed).
     */
    public void setSmoothingSpeedMultiplier(float multiplier) {
        this.smoothingSpeedMultiplier = Math.max(0.01f, multiplier);
    }

    public void setFollowTargetBiasWorldX(float biasWorldX) {
        this.followTargetBiasWorldX = biasWorldX;
    }

    /**
     * True when the camera is within {@code worldEpsilon} of the clamped follow target from the last update.
     */
    public boolean isCameraNearFollowTarget(float worldEpsilon) {
        float dx = camera.position.x - lastClampedFollowTarget.x;
        float dy = camera.position.y - lastClampedFollowTarget.y;
        float e = Math.max(0.0001f, worldEpsilon);
        return dx * dx + dy * dy <= e * e;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Transform transform = Transform.MAPPER.get(entity);
        calcTargetPosition(transform.getPosition());
        lastClampedFollowTarget.set(targetPosition);

        float progress = smoothingFactor * smoothingSpeedMultiplier * deltaTime;
        float smoothedX = MathUtils.lerp(camera.position.x, targetPosition.x, progress);
        float smoothedY = MathUtils.lerp(camera.position.y, targetPosition.y, progress);
        camera.position.set(smoothedX, smoothedY, camera.position.z);
    }

    /**
     * Immediately places the camera on the same clamped target used by follow smoothing.
     * Useful during map transitions to avoid one-frame camera correction.
     */
    public void snapTo(Transform transform) {
        if (transform == null) {
            return;
        }
        calcTargetPosition(transform.getPosition());
        camera.position.set(targetPosition.x, targetPosition.y, camera.position.z);
    }

    private void calcTargetPosition(Vector2 entityPosition) {
        float targetX = entityPosition.x + followTargetBiasWorldX;
        float camHalfW = camera.viewportWidth * 0.5f;
        if (mapW > camHalfW) {
            float min = Math.min(camHalfW, mapW - camHalfW);
            float max = Math.max(camHalfW, mapW - camHalfW);
            targetX = MathUtils.clamp(targetX, min, max);
        }

        float targetY = entityPosition.y + CAM_OFFSET_Y;
        float camHalfH = camera.viewportHeight * 0.5f;
        if (mapH > camHalfH) {
            float min = Math.min(camHalfH, mapH - camHalfH);
            float max = Math.max(camHalfH, mapH - camHalfH);
            targetY = MathUtils.clamp(targetY, min, max);
        }

        targetPosition.set(targetX, targetY);
    }

    /**
     * Call this when the map changes so the camera knows the map boundaries.
     */
    public void setMap(TiledMap tiledMap) {
        smoothingSpeedMultiplier = 1f;
        followTargetBiasWorldX = 0f;
        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        mapW = width * tileW * Main.UNIT_SCALE;
        mapH = height * tileH * Main.UNIT_SCALE;
    }
}
