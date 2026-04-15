package github.kinuseka.testproject.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Stores the visual representation of an entity: a texture region and a tint
 * color.
 * The RenderSystem reads this to draw the entity.
 */
public class Graphic implements Component {
    public static final ComponentMapper<Graphic> MAPPER = ComponentMapper.getFor(Graphic.class);

    private TextureRegion region;
    private final Color color;

    public Graphic(TextureRegion region, Color color) {
        this.region = region;
        this.color = color;
    }

    public TextureRegion getRegion() {
        return region;
    }

    public void setRegion(TextureRegion region) {
        this.region = region;
    }

    public Color getColor() {
        return color;
    }
}
