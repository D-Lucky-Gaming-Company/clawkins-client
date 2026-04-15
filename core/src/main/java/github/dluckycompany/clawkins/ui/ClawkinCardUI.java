package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Barebones UI component for displaying a party member.
 * Shows name, HP bar, and status. Used in inventory and party UIs.
 */
public class ClawkinCardUI extends Table {
    private final Clawkin clawkin;
    private final Label nameLabel;
    private final Label hpLabel;
    private final ProgressBar hpBar;
    private final Color hpBarColor;

    public ClawkinCardUI(Clawkin clawkin, BitmapFont font, float hpBarWidth) {
        this.clawkin = clawkin;
        
        // Name label
        nameLabel = new Label(clawkin.getName(), new Label.LabelStyle(font, Color.WHITE));
        
        // HP label
        hpLabel = new Label(clawkin.getCurrentHp() + "/" + clawkin.getMaxHp(), 
            new Label.LabelStyle(font, Color.WHITE));
        
        // HP bar
        ProgressBar.ProgressBarStyle barStyle = createProgressBarStyle();
        hpBar = new ProgressBar(0, clawkin.getMaxHp(), 1, false, barStyle);
        hpBar.setValue(clawkin.getCurrentHp());
        hpBar.setWidth(hpBarWidth);
        this.hpBarColor = Color.GREEN.cpy();
        
        // Build UI
        add(nameLabel).left().padLeft(5).row();
        add(hpBar).width(hpBarWidth).row();
        add(hpLabel).left().padLeft(5);
    }

    /**
     * Update the displayed HP.
     */
    public void updateHp() {
        hpBar.setValue(clawkin.getCurrentHp());
        hpLabel.setText(clawkin.getCurrentHp() + "/" + clawkin.getMaxHp());
        
        // Color code: green (healthy), yellow (caution), red (critical)
        if (clawkin.getCurrentHp() <= 0) {
            hpBarColor.set(Color.GRAY);
        } else if (clawkin.getCurrentHp() < clawkin.getMaxHp() * 0.25f) {
            hpBarColor.set(Color.RED);
        } else if (clawkin.getCurrentHp() < clawkin.getMaxHp() * 0.5f) {
            hpBarColor.set(Color.YELLOW);
        } else {
            hpBarColor.set(Color.GREEN);
        }
    }

    public Clawkin getClawkin() {
        return clawkin;
    }

    private ProgressBar.ProgressBarStyle createProgressBarStyle() {
        // Create a minimal progress bar style with colored backgrounds
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        style.background = createColorDrawable(Color.DARK_GRAY, 100, 20);
        style.knobBefore = createColorDrawable(Color.GREEN, 100, 20);
        return style;
    }

    /**
     * Helper to create a colored drawable (using internal rendering).
     * In a real game, you'd use texture assets.
     */
    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createColorDrawable(Color color, float width, float height) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap((int) width, (int) height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture));
    }
}
