package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

/**
 * Component for cosmetic map props that can still be referenced by id.
 */
public class Prop implements Component {
    public static final ComponentMapper<Prop> MAPPER = ComponentMapper.getFor(Prop.class);

    private final String objectId;

    public Prop(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectId() {
        return objectId;
    }
}
