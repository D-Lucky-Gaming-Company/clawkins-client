package github.kinuseka.testproject.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import github.kinuseka.testproject.component.Move;
import github.kinuseka.testproject.component.Transform;

/**
 * Applies movement to entities each frame.
 * For every entity with [Move + Transform], adds direction * maxSpeed *
 * deltaTime to position.
 *
 * This is a simplified move system (no Box2D physics). If you add physics
 * later,
 * you'd replace this with a PhysicMoveSystem that sets body velocity instead.
 */
public class MoveSystem extends IteratingSystem {

    public MoveSystem() {
        super(Family.all(Move.class, Transform.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Move move = Move.MAPPER.get(entity);
        Transform transform = Transform.MAPPER.get(entity);

        Vector2 direction = move.getDirection();
        if (direction.isZero()) {
            return;
        }

        float speed = move.getMaxSpeed();
        Vector2 position = transform.getPosition();
        position.x += direction.x * speed * deltaTime;
        position.y += direction.y * speed * deltaTime;
    }
}
