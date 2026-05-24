package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Base {@link Table} with shared Scene2D drawable helpers for UI widgets.
 */
public abstract class AbstractUiTable extends Table {

    /**
     * Builds this widget's Scene2D child layout. Subclasses call {@link #composeLayout()}
     * from their constructor once all required fields are initialized.
     */
    protected abstract void buildLayout();

    protected final void composeLayout() {
        buildLayout();
    }

    protected static Drawable createSolidDrawable(Color color, int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    protected static Drawable createSolidDrawable(Color color) {
        return createSolidDrawable(color, 256, 256);
    }
}
