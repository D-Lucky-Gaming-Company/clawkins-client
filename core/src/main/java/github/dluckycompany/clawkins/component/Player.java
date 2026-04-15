package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Tag component — marks an entity as the player.
 * Contains no data; its mere presence on an entity is the signal.
 */
public class Player implements Component {
    public static final ComponentMapper<Player> MAPPER = ComponentMapper.getFor(Player.class);
}
