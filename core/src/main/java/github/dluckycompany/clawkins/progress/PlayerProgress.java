package github.dluckycompany.clawkins.progress;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks player progression flags and per-event stats.
 *
 * This is intentionally runtime-only for now. Save/load wiring can serialize
 * {@link #getAccomplishedEvents()} and {@link #snapshotEventStats()} later.
 */
public class PlayerProgress {
    private final Set<String> accomplishedEvents = new LinkedHashSet<>();
    private final Map<String, EventStats> eventStatsById = new HashMap<>();

    public boolean isEventAccomplished(String eventId) {
        String key = normalize(eventId);
        return !key.isEmpty() && accomplishedEvents.contains(key);
    }

    public boolean markEventAccomplished(String eventId) {
        String key = normalize(eventId);
        if (key.isEmpty()) {
            return false;
        }
        eventStats(key).completedCount++;
        return accomplishedEvents.add(key);
    }

    public Set<String> getAccomplishedEvents() {
        return Collections.unmodifiableSet(accomplishedEvents);
    }

    public void incrementAttempts(String eventId) {
        String key = normalize(eventId);
        if (!key.isEmpty()) {
            eventStats(key).attemptCount++;
        }
    }

    public void incrementAccepted(String eventId) {
        String key = normalize(eventId);
        if (!key.isEmpty()) {
            eventStats(key).acceptedCount++;
        }
    }

    public void incrementDeclined(String eventId) {
        String key = normalize(eventId);
        if (!key.isEmpty()) {
            eventStats(key).declinedCount++;
        }
    }

    public void incrementWins(String eventId) {
        String key = normalize(eventId);
        if (!key.isEmpty()) {
            eventStats(key).winCount++;
        }
    }

    public void incrementLosses(String eventId) {
        String key = normalize(eventId);
        if (!key.isEmpty()) {
            eventStats(key).lossCount++;
        }
    }

    public EventStats getEventStats(String eventId) {
        String key = normalize(eventId);
        if (key.isEmpty()) {
            return EventStats.empty();
        }
        EventStats stats = eventStatsById.get(key);
        return stats == null ? EventStats.empty() : stats.copy();
    }

    public Map<String, EventStats> snapshotEventStats() {
        Map<String, EventStats> copy = new HashMap<>();
        for (Map.Entry<String, EventStats> entry : eventStatsById.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Map.copyOf(copy);
    }

    private EventStats eventStats(String eventId) {
        return eventStatsById.computeIfAbsent(eventId, key -> new EventStats());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public static final class EventStats {
        private int attemptCount;
        private int acceptedCount;
        private int declinedCount;
        private int winCount;
        private int lossCount;
        private int completedCount;

        private static EventStats empty() {
            return new EventStats();
        }

        private EventStats copy() {
            EventStats copy = new EventStats();
            copy.attemptCount = attemptCount;
            copy.acceptedCount = acceptedCount;
            copy.declinedCount = declinedCount;
            copy.winCount = winCount;
            copy.lossCount = lossCount;
            copy.completedCount = completedCount;
            return copy;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public int getAcceptedCount() {
            return acceptedCount;
        }

        public int getDeclinedCount() {
            return declinedCount;
        }

        public int getWinCount() {
            return winCount;
        }

        public int getLossCount() {
            return lossCount;
        }

        public int getCompletedCount() {
            return completedCount;
        }
    }
}
