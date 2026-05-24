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

    public static final Color PLAYER_NAME_COLOR = Color.YELLOW;

    private static final float PAD = 8f;
    private final BitmapFont font;
    private final Label.LabelStyle titleStyle;
    private final Label.LabelStyle entryStyle;

    public LeaderboardHud(BitmapFont font) {
        this.font = font;
        this.titleStyle = new Label.LabelStyle(font, Color.GOLD);
        this.entryStyle = new Label.LabelStyle(font, Color.WHITE);
        pad(PAD);
        align(Align.topRight);
    }

    /**
     * Rebuilds the leaderboard display with the given entries.
     */
    public void refresh(List<LeaderboardEntry> entries) {
        refresh(entries, null);
    }

    /**
     * Rebuilds the leaderboard display, highlighting the given player's name in yellow.
     */
    public void refresh(List<LeaderboardEntry> entries, String highlightPlayerName) {
        clearChildren();

        Label title = new Label("Leaderboard", titleStyle);
        add(title).right().padBottom(4f).row();

        if (entries == null || entries.isEmpty()) {
            Label empty = new Label("No records yet", entryStyle);
            add(empty).right().row();
            pack();
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            boolean isPlayer = isHighlightedPlayer(entry.getName(), highlightPlayerName);

            Label rankLabel = new Label((i + 1) + ". ", entryStyle);
            Label nameLabel = new Label(entry.getName(), styleForName(isPlayer));
            Label timeLabel = new Label(" - " + entry.getFormattedTime(), entryStyle);

            Table row = new Table();
            row.add(rankLabel);
            row.add(nameLabel);
            row.add(timeLabel);
            add(row).right().padBottom(2f).row();
        }

        pack();
    }

    static boolean isHighlightedPlayer(String entryName, String highlightPlayerName) {
        if (entryName == null || highlightPlayerName == null) {
            return false;
        }
        return entryName.trim().equalsIgnoreCase(highlightPlayerName.trim());
    }

    private Label.LabelStyle styleForName(boolean isPlayer) {
        return isPlayer ? new Label.LabelStyle(font, PLAYER_NAME_COLOR) : entryStyle;
    }
}
