package github.dluckycompany.clawkins.leaderboard;

/**
 * A single leaderboard entry: player name + completion time.
 */
public class LeaderboardEntry {
    private final String name;
    private final long timeMillis;

    public LeaderboardEntry(String name, long timeMillis) {
        this.name = name == null ? "" : name.trim();
        this.timeMillis = Math.max(0, timeMillis);
    }

    public String getName() {
        return name;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public String getFormattedTime() {
        return LeaderboardManager.formatMillis(timeMillis);
    }
}
