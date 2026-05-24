package github.dluckycompany.clawkins.leaderboard;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the leaderboard: loading, saving, sorting, and updating entries.
 * Persists data to a .txt file using LibGDX file handling.
 * Keeps only the top 5 fastest completion times.
 */
public class LeaderboardManager {

    private static final String TAG = "LeaderboardManager";
    private static final String LEADERBOARD_FILE = "leaderboard.txt";
    private static final int MAX_ENTRIES = 5;

    private final List<LeaderboardEntry> entries;

    public LeaderboardManager() {
        this.entries = new ArrayList<>();
        load();
    }

    /**
     * Loads leaderboard from file. Creates default entries if file doesn't exist.
     */
    public void load() {
        entries.clear();

        FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
        if (!file.exists()) {
            Gdx.app.log(TAG, "Leaderboard file not found, creating defaults.");
            createDefaults();
            save();
            return;
        }

        try {
            String content = file.readString("UTF-8");
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split(",", 2);
                if (parts.length != 2) {
                    Gdx.app.error(TAG, "Skipping malformed line: " + trimmed);
                    continue;
                }
                String name = parts[0].trim();
                String timeStr = parts[1].trim();
                long millis = parseTimeToMillis(timeStr);
                if (millis >= 0 && !name.isEmpty()) {
                    entries.add(new LeaderboardEntry(name, millis));
                }
            }
            sortAndTrim();
            Gdx.app.log(TAG, "Loaded " + entries.size() + " leaderboard entries.");
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load leaderboard, using defaults.", e);
            entries.clear();
            createDefaults();
            save();
        }
    }

    /**
     * Saves the current leaderboard to file.
     */
    public void save() {
        StringBuilder sb = new StringBuilder();
        for (LeaderboardEntry entry : entries) {
            sb.append(entry.getName()).append(",").append(formatMillis(entry.getTimeMillis())).append("\n");
        }

        try {
            FileHandle file = Gdx.files.local(LEADERBOARD_FILE);
            file.writeString(sb.toString(), false, "UTF-8");
            Gdx.app.log(TAG, "Leaderboard saved (" + entries.size() + " entries).");
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to save leaderboard.", e);
        }
    }

    /**
     * Submits a new completion time. If it qualifies for top 5, it's inserted and saved.
     *
     * @param playerName     the player's name
     * @param completionMillis the completion time in milliseconds
     * @return true if the entry made it into the top 5
     */
    public boolean submit(String playerName, long completionMillis) {
        if (playerName == null || playerName.isBlank() || completionMillis <= 0) {
            return false;
        }

        entries.add(new LeaderboardEntry(playerName.trim(), completionMillis));
        sortAndTrim();
        save();

        // Check if the submitted entry is still in the list
        for (LeaderboardEntry entry : entries) {
            if (entry.getName().equals(playerName.trim()) && entry.getTimeMillis() == completionMillis) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the current top entries.
     */
    public List<LeaderboardEntry> getEntries() {
        return List.copyOf(entries);
    }

    private void sortAndTrim() {
        entries.sort(Comparator.comparingLong(LeaderboardEntry::getTimeMillis));
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    private void createDefaults() {
        entries.add(new LeaderboardEntry("Ralph", parseTimeToMillis("00:18:42")));
        entries.add(new LeaderboardEntry("Gaving", parseTimeToMillis("00:20:15")));
        entries.add(new LeaderboardEntry("Milbert", parseTimeToMillis("00:23:09")));
        entries.add(new LeaderboardEntry("John", parseTimeToMillis("00:26:31")));
        entries.add(new LeaderboardEntry("Alexandra", parseTimeToMillis("00:30:02")));
    }

    /**
     * Parses "HH:MM:SS" format to milliseconds.
     */
    public static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return -1;
        }
        String[] parts = timeStr.trim().split(":");
        if (parts.length != 3) {
            return -1;
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            return ((long) hours * 3600 + (long) minutes * 60 + seconds) * 1000L;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Formats milliseconds to "HH:MM:SS" display string.
     */
    public static String formatMillis(long millis) {
        if (millis < 0) {
            return "00:00:00";
        }
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
