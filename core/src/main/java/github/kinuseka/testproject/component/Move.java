package github.kinuseka.testproject.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

/**
 * Stores movement data: maximum speed and current direction.
 * The MoveSystem reads direction and maxSpeed to update Transform.position each
 * frame.
 */
public class Move implements Component {
    public static final ComponentMapper<Move> MAPPER = ComponentMapper.getFor(Move.class);

    private float maxSpeed;
    private final Vector2 direction;

    public Move(float maxSpeed) {
        this.maxSpeed = maxSpeed;
        this.direction = new Vector2();
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Vector2 getDirection() {
        return direction;
    }
}
