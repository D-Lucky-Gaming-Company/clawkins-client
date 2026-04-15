package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Simple solid color drawable for Scene2D widgets.
 * Creates a 1x1 white texture and draws it tinted to the specified color.
 */
public class ColorDrawable implements Drawable {
    private static final TextureRegion whiteRegion;
    private final Color color;
    private float minWidth = 0;
    private float minHeight = 0;

    static {
        // Create a 1x1 white texture (done once for all instances)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        whiteRegion = new TextureRegion(whiteTexture, 0, 0, 1, 1);
    }

    /**
     * Create a drawable with the specified color.
     *
     * @param color the color to render
     */
    public ColorDrawable(Color color) {
        this.color = new Color(color);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        Color prevColor = batch.getColor();
        batch.setColor(color);
        batch.draw(whiteRegion, x, y, width, height);
        batch.setColor(prevColor);
    }

    @Override
    public float getLeftWidth() {
        return 0;
    }

    @Override
    public void setLeftWidth(float leftWidth) {
    }

    @Override
    public float getRightWidth() {
        return 0;
    }

    @Override
    public void setRightWidth(float rightWidth) {
    }

    @Override
    public float getTopHeight() {
        return 0;
    }

    @Override
    public void setTopHeight(float topHeight) {
    }

    @Override
    public float getBottomHeight() {
        return 0;
    }

    @Override
    public void setBottomHeight(float bottomHeight) {
    }

    @Override
    public float getMinWidth() {
        return minWidth;
    }

    @Override
    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
    }

    @Override
    public float getMinHeight() {
        return minHeight;
    }

    @Override
    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
    }
}

