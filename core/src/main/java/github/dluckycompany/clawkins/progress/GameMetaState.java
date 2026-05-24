package github.dluckycompany.clawkins.progress;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import github.dluckycompany.clawkins.leaderboard.LeaderboardManager;

/**
 * Global meta-progress that persists outside individual save slots.
 * Tracks whether the player has beaten the game and their completion times.
 */
public class GameMetaState {

    private static final String TAG = "GameMetaState";
    private static final String META_FILE = "game_meta.txt";
    private static final String KEY_GAME_COMPLETED = "gameCompleted";
    private static final String KEY_BEST_COMPLETION_MILLIS = "bestCompletionMillis";
    private static final String KEY_BEST_COMPLETION_NAME = "bestCompletionName";
    private static final String KEY_LAST_COMPLETION_MILLIS = "lastCompletionMillis";
    private static final String KEY_LAST_COMPLETION_NAME = "lastCompletionName";
    private static final String KEY_LAST_RUN_PERSONAL_BEST = "lastRunWasPersonalBest";

    private boolean gameCompleted;
    private long bestCompletionMillis = -1L;
    private String bestCompletionName = "";
    private long lastCompletionMillis = -1L;
    private String lastCompletionName = "";
    private boolean lastRunWasPersonalBest;

    public GameMetaState() {
        load();
    }

    public boolean hasCompletedGame() {
        return gameCompleted;
    }

    public long getBestCompletionMillis() {
        return bestCompletionMillis;
    }

    public String getBestCompletionName() {
        return bestCompletionName;
    }

    public long getLastCompletionMillis() {
        return lastCompletionMillis;
    }

    public String getLastCompletionName() {
        return lastCompletionName;
    }

    public boolean isLastRunPersonalBest() {
        return lastRunWasPersonalBest;
    }

    public void markGameCompleted() {
        gameCompleted = true;
        save();
    }

    /**
     * Records a finished run. Always persists the latest time and keeps the player's fastest time seen.
     *
     * @return true when this run beat the player's previous personal best
     */
    public boolean recordCompletion(String playerName, long completionMillis) {
        if (playerName == null || playerName.isBlank() || completionMillis <= 0L) {
            lastRunWasPersonalBest = false;
            return false;
        }

        String name = playerName.trim();
        lastCompletionName = name;
        lastCompletionMillis = completionMillis;

        boolean samePlayerAsStoredBest = name.equalsIgnoreCase(bestCompletionName);
        long previousPersonalBest = samePlayerAsStoredBest ? bestCompletionMillis : -1L;
        lastRunWasPersonalBest = previousPersonalBest < 0L || completionMillis < previousPersonalBest;

        if (previousPersonalBest < 0L || samePlayerAsStoredBest) {
            bestCompletionName = name;
            if (previousPersonalBest < 0L || completionMillis < bestCompletionMillis) {
                bestCompletionMillis = completionMillis;
            }
        }

        gameCompleted = true;
        save();
        Gdx.app.log(TAG, "Recorded completion: " + name + " - "
                + LeaderboardManager.formatMillis(completionMillis)
                + (lastRunWasPersonalBest ? " (new personal best)" : ""));
        return lastRunWasPersonalBest;
    }

    public void load() {
        gameCompleted = false;
        bestCompletionMillis = -1L;
        bestCompletionName = "";
        lastCompletionMillis = -1L;
        lastCompletionName = "";
        lastRunWasPersonalBest = false;

        FileHandle file = Gdx.files.local(META_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            String[] lines = file.readString("UTF-8").split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                switch (key) {
                    case KEY_GAME_COMPLETED -> gameCompleted = parseBoolean(value);
                    case KEY_BEST_COMPLETION_MILLIS -> bestCompletionMillis = parseLong(value, -1L);
                    case KEY_BEST_COMPLETION_NAME -> bestCompletionName = value == null ? "" : value.trim();
                    case KEY_LAST_COMPLETION_MILLIS -> lastCompletionMillis = parseLong(value, -1L);
                    case KEY_LAST_COMPLETION_NAME -> lastCompletionName = value == null ? "" : value.trim();
                    case KEY_LAST_RUN_PERSONAL_BEST -> lastRunWasPersonalBest = parseBoolean(value);
                    default -> {
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to load meta state.", e);
        }
    }

    public void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(KEY_GAME_COMPLETED).append('=').append(gameCompleted).append('\n');
            if (bestCompletionMillis >= 0L) {
                sb.append(KEY_BEST_COMPLETION_MILLIS).append('=').append(bestCompletionMillis).append('\n');
                sb.append(KEY_BEST_COMPLETION_NAME).append('=').append(bestCompletionName).append('\n');
            }
            if (lastCompletionMillis >= 0L) {
                sb.append(KEY_LAST_COMPLETION_MILLIS).append('=').append(lastCompletionMillis).append('\n');
                sb.append(KEY_LAST_COMPLETION_NAME).append('=').append(lastCompletionName).append('\n');
                sb.append(KEY_LAST_RUN_PERSONAL_BEST).append('=').append(lastRunWasPersonalBest).append('\n');
            }

            FileHandle file = Gdx.files.local(META_FILE);
            file.writeString(sb.toString(), false, "UTF-8");
            Gdx.app.log(TAG, "Meta state saved (gameCompleted=" + gameCompleted + ").");
        } catch (Exception e) {
            Gdx.app.error(TAG, "Failed to save meta state.", e);
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return "true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized);
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
