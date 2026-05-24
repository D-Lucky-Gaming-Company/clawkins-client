package github.dluckycompany.clawkins.leaderboard;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import java.util.List;

/**
 * HUD widget that displays the leaderboard in the top-right corner.
 * Styled to match the retro pixel aesthetic of the game.
 * Text is right-aligned so entries expand leftward.
 */
public class LeaderboardHud extends Table {

    private static final float PAD = 8f;
    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle entryStyle;

    public LeaderboardHud(BitmapFont font) {
        this.titleStyle = new Label.LabelStyle(font, Color.GOLD);
        this.entryStyle = new Label.LabelStyle(font, Color.WHITE);
        pad(PAD);
        align(Align.topRight);
    }

    /**
     * Rebuilds the leaderboard display with the given entries.
     */
    public void refresh(List<LeaderboardEntry> entries) {
        clearChildren();

        Label title = new Label("Leaderboard", titleStyle);
        add(title).right().padBottom(4f).row();

        if (entries == null || entries.isEmpty()) {
            Label empty = new Label("No records yet", entryStyle);
            add(empty).right().row();
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            String text = (i + 1) + ". " + entry.getName() + " - " + entry.getFormattedTime();
            Label label = new Label(text, entryStyle);
            add(label).right().padBottom(2f).row();
        }

        pack();
    }
}
