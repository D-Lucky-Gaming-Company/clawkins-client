package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Barebones UI component for displaying a party member.
 * Shows name, HP bar, and status. Used in inventory and party UIs.
 */
public class ClawkinCardUI extends AbstractClawkinHpTable {
    private final BitmapFont font;
    private final float hpBarWidth;
    private Label nameLabel;
    private Label hpLabel;
    private ProgressBar hpBar;
    private final Color hpBarColor = Color.GREEN.cpy();

    public ClawkinCardUI(Clawkin clawkin, BitmapFont font, float hpBarWidth) {
        super(clawkin);
        this.font = font;
        this.hpBarWidth = hpBarWidth;
        composeLayout();
    }

    @Override
    protected void buildLayout() {
        nameLabel = new Label(clawkin.getName(), new Label.LabelStyle(font, Color.WHITE));
        hpLabel = new Label(clawkin.getCurrentHp() + "/" + clawkin.getMaxHp(),
            new Label.LabelStyle(font, Color.WHITE));

        ProgressBar.ProgressBarStyle barStyle = createProgressBarStyle();
        hpBar = new ProgressBar(0, clawkin.getMaxHp(), 1, false, barStyle);
        hpBar.setValue(clawkin.getCurrentHp());
        hpBar.setWidth(hpBarWidth);

        add(nameLabel).left().padLeft(5).row();
        add(hpBar).width(hpBarWidth).row();
        add(hpLabel).left().padLeft(5);
    }

    @Override
    public void updateHp() {
        hpBar.setValue(clawkin.getCurrentHp());
        hpLabel.setText(clawkin.getCurrentHp() + "/" + clawkin.getMaxHp());

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

    private ProgressBar.ProgressBarStyle createProgressBarStyle() {
        ProgressBar.ProgressBarStyle style = new ProgressBar.ProgressBarStyle();
        style.background = createSolidDrawable(Color.DARK_GRAY, 100, 20);
        style.knobBefore = createSolidDrawable(Color.GREEN, 100, 20);
        return style;
    }
}
