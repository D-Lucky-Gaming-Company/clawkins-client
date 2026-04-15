package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Player-facing profile metadata used by UI systems.
 */
public class PlayerProfile implements Component {
    public static final ComponentMapper<PlayerProfile> MAPPER = ComponentMapper.getFor(PlayerProfile.class);

    private final String playerName;

    public PlayerProfile(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
