package github.dluckycompany.clawkins.progress;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;

/**
 * Tracks player progression flags and per-event stats.
 *
 * Save/load can use {@link #writeToFlags(Map)} and {@link #loadFromFlags(Map)}.
 */
public class PlayerProgress {
    private static final String FLAG_PREFIX = "progress.";
    public static final String PROTOCOL_FLAG_KEY = "progress.protocol.v1";

    private final Set<String> accomplishedEvents = new LinkedHashSet<>();
    private final Map<String, EventStats> eventStatsById = new HashMap<>();
    private final Map<String, Integer> inventoryQuantitiesByItemId = new HashMap<>();
    private final Map<String, ClawkinStats> clawkinStatsById = new HashMap<>();
    private int enemiesDefeated;
    private int experiencePoints;

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

    public int getEnemiesDefeated() {
        return enemiesDefeated;
    }

    public void incrementEnemiesDefeated() {
        enemiesDefeated++;
    }

    public int getExperiencePoints() {
        return experiencePoints;
    }

    public void addExperiencePoints(int amount) {
        if (amount <= 0) {
            return;
        }
        experiencePoints += amount;
    }

    public void setExperiencePoints(int amount) {
        experiencePoints = Math.max(0, amount);
    }

    public void captureInventory(Inventory inventory) {
        inventoryQuantitiesByItemId.clear();
        if (inventory == null) {
            return;
        }

        for (Item item : inventory.getAllItems()) {
            if (item == null || item.getId() == null || item.getId().isBlank()) {
                continue;
            }
            int quantity = inventory.getQuantity(item);
            if (quantity > 0) {
                inventoryQuantitiesByItemId.put(item.getId(), quantity);
            }
        }
    }

    public Map<String, Integer> snapshotInventoryQuantities() {
        return Map.copyOf(inventoryQuantitiesByItemId);
    }

    public void capturePartyStats(List<Clawkin> party) {
        clawkinStatsById.clear();
        if (party == null) {
            return;
        }

        for (Clawkin clawkin : party) {
            if (clawkin == null || clawkin.getId() == null || clawkin.getId().isBlank()) {
                continue;
            }
            ClawkinStats stats = new ClawkinStats();
            stats.level = clawkin.getLevel();
            stats.maxHp = clawkin.getMaxHp();
            stats.currentHp = clawkin.getCurrentHp();
            stats.attack = clawkin.getBaseAttack();
            stats.defense = clawkin.getBaseDefense();
            stats.speed = clawkin.getBaseSpeed();
            clawkinStatsById.put(clawkin.getId(), stats);
        }
    }

    public Map<String, ClawkinStats> snapshotClawkinStats() {
        Map<String, ClawkinStats> copy = new HashMap<>();
        for (Map.Entry<String, ClawkinStats> entry : clawkinStatsById.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return Map.copyOf(copy);
    }

    public void writeToFlags(Map<String, String> flags) {
        if (flags == null) {
            return;
        }
        flags.put(FLAG_PREFIX + "version", "1");
        flags.put(FLAG_PREFIX + "xp", Integer.toString(experiencePoints));
        flags.put(FLAG_PREFIX + "enemiesDefeated", Integer.toString(enemiesDefeated));

        flags.put(FLAG_PREFIX + "events.accomplished.count", Integer.toString(accomplishedEvents.size()));
        int accomplishedIndex = 0;
        for (String eventId : accomplishedEvents) {
            flags.put(FLAG_PREFIX + "events.accomplished." + accomplishedIndex + ".id", eventId);
            accomplishedIndex++;
        }

        flags.put(FLAG_PREFIX + "events.stats.count", Integer.toString(eventStatsById.size()));
        int eventStatsIndex = 0;
        for (Map.Entry<String, EventStats> entry : eventStatsById.entrySet()) {
            String key = FLAG_PREFIX + "events.stats." + eventStatsIndex;
            EventStats stats = entry.getValue();
            flags.put(key + ".id", entry.getKey());
            flags.put(key + ".attempts", Integer.toString(stats.attemptCount));
            flags.put(key + ".accepted", Integer.toString(stats.acceptedCount));
            flags.put(key + ".declined", Integer.toString(stats.declinedCount));
            flags.put(key + ".wins", Integer.toString(stats.winCount));
            flags.put(key + ".losses", Integer.toString(stats.lossCount));
            flags.put(key + ".completed", Integer.toString(stats.completedCount));
            eventStatsIndex++;
        }

        flags.put(FLAG_PREFIX + "inventory.count", Integer.toString(inventoryQuantitiesByItemId.size()));
        int inventoryIndex = 0;
        for (Map.Entry<String, Integer> entry : inventoryQuantitiesByItemId.entrySet()) {
            String key = FLAG_PREFIX + "inventory." + inventoryIndex;
            flags.put(key + ".id", entry.getKey());
            flags.put(key + ".qty", Integer.toString(entry.getValue()));
            inventoryIndex++;
        }

        flags.put(FLAG_PREFIX + "clawkin.count", Integer.toString(clawkinStatsById.size()));
        int clawkinIndex = 0;
        for (Map.Entry<String, ClawkinStats> entry : clawkinStatsById.entrySet()) {
            String key = FLAG_PREFIX + "clawkin." + clawkinIndex;
            ClawkinStats stats = entry.getValue();
            flags.put(key + ".id", entry.getKey());
            flags.put(key + ".level", Integer.toString(stats.level));
            flags.put(key + ".maxHp", Integer.toString(stats.maxHp));
            flags.put(key + ".currentHp", Integer.toString(stats.currentHp));
            flags.put(key + ".attack", Integer.toString(stats.attack));
            flags.put(key + ".defense", Integer.toString(stats.defense));
            flags.put(key + ".speed", Integer.toString(stats.speed));
            clawkinIndex++;
        }
    }

    /**
     * Serializes progress flags to a compact single-line Base64 payload suitable for
     * text save files with simple key=value parsing.
     */
    public String toProtocolPayload() {
        Map<String, String> flags = new TreeMap<>();
        writeToFlags(flags);
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, String> entry : flags.entrySet()) {
            raw.append(escape(entry.getKey()))
                    .append('=')
                    .append(escape(entry.getValue()))
                    .append('\n');
        }
        return Base64.getEncoder().encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Loads progress state from {@link #toProtocolPayload()}.
     * Falls back safely to empty state when payload is invalid.
     */
    public void loadFromProtocolPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            loadFromFlags(Map.of());
            return;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(payload.trim());
            String raw = new String(decoded, StandardCharsets.UTF_8);
            Map<String, String> flags = new HashMap<>();
            String[] lines = raw.split("\r?\n");
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = unescape(line.substring(0, idx));
                String value = unescape(line.substring(idx + 1));
                if (!key.isBlank()) {
                    flags.put(key, value);
                }
            }
            loadFromFlags(flags);
        } catch (IllegalArgumentException ex) {
            loadFromFlags(Map.of());
        }
    }

    public void loadFromFlags(Map<String, String> flags) {
        accomplishedEvents.clear();
        eventStatsById.clear();
        inventoryQuantitiesByItemId.clear();
        clawkinStatsById.clear();
        enemiesDefeated = 0;
        experiencePoints = 0;

        if (flags == null || flags.isEmpty()) {
            return;
        }

        experiencePoints = parseInt(flags.get(FLAG_PREFIX + "xp"), 0);
        enemiesDefeated = parseInt(flags.get(FLAG_PREFIX + "enemiesDefeated"), 0);

        int accomplishedCount = parseInt(flags.get(FLAG_PREFIX + "events.accomplished.count"), 0);
        for (int i = 0; i < accomplishedCount; i++) {
            String key = normalize(flags.get(FLAG_PREFIX + "events.accomplished." + i + ".id"));
            if (!key.isEmpty()) {
                accomplishedEvents.add(key);
            }
        }

        int eventStatsCount = parseInt(flags.get(FLAG_PREFIX + "events.stats.count"), 0);
        for (int i = 0; i < eventStatsCount; i++) {
            String id = normalize(flags.get(FLAG_PREFIX + "events.stats." + i + ".id"));
            if (id.isEmpty()) {
                continue;
            }
            EventStats stats = new EventStats();
            String baseKey = FLAG_PREFIX + "events.stats." + i;
            stats.attemptCount = parseInt(flags.get(baseKey + ".attempts"), 0);
            stats.acceptedCount = parseInt(flags.get(baseKey + ".accepted"), 0);
            stats.declinedCount = parseInt(flags.get(baseKey + ".declined"), 0);
            stats.winCount = parseInt(flags.get(baseKey + ".wins"), 0);
            stats.lossCount = parseInt(flags.get(baseKey + ".losses"), 0);
            stats.completedCount = parseInt(flags.get(baseKey + ".completed"), 0);
            eventStatsById.put(id, stats);
        }

        int inventoryCount = parseInt(flags.get(FLAG_PREFIX + "inventory.count"), 0);
        for (int i = 0; i < inventoryCount; i++) {
            String id = flags.get(FLAG_PREFIX + "inventory." + i + ".id");
            if (id == null || id.isBlank()) {
                continue;
            }
            int quantity = parseInt(flags.get(FLAG_PREFIX + "inventory." + i + ".qty"), 0);
            if (quantity > 0) {
                inventoryQuantitiesByItemId.put(id, quantity);
            }
        }

        int clawkinCount = parseInt(flags.get(FLAG_PREFIX + "clawkin.count"), 0);
        for (int i = 0; i < clawkinCount; i++) {
            String id = flags.get(FLAG_PREFIX + "clawkin." + i + ".id");
            if (id == null || id.isBlank()) {
                continue;
            }
            String baseKey = FLAG_PREFIX + "clawkin." + i;
            ClawkinStats stats = new ClawkinStats();
            stats.level = parseInt(flags.get(baseKey + ".level"), 1);
            stats.maxHp = parseInt(flags.get(baseKey + ".maxHp"), 1);
            stats.currentHp = parseInt(flags.get(baseKey + ".currentHp"), stats.maxHp);
            stats.attack = parseInt(flags.get(baseKey + ".attack"), 1);
            stats.defense = parseInt(flags.get(baseKey + ".defense"), 0);
            stats.speed = parseInt(flags.get(baseKey + ".speed"), 1);
            clawkinStatsById.put(id, stats);
        }
    }

    private EventStats eventStats(String eventId) {
        return eventStatsById.computeIfAbsent(eventId, key -> new EventStats());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("=", "\\=");
    }

    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case '=' -> out.append('=');
                    case '\\' -> out.append('\\');
                    default -> {
                        out.append('\\');
                        out.append(c);
                    }
                }
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            out.append(c);
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }

    public static final class ClawkinStats {
        private int level;
        private int maxHp;
        private int currentHp;
        private int attack;
        private int defense;
        private int speed;

        private ClawkinStats copy() {
            ClawkinStats copy = new ClawkinStats();
            copy.level = level;
            copy.maxHp = maxHp;
            copy.currentHp = currentHp;
            copy.attack = attack;
            copy.defense = defense;
            copy.speed = speed;
            return copy;
        }

        public int getLevel() {
            return level;
        }

        public int getMaxHp() {
            return maxHp;
        }

        public int getCurrentHp() {
            return currentHp;
        }

        public int getAttack() {
            return attack;
        }

        public int getDefense() {
            return defense;
        }

        public int getSpeed() {
            return speed;
        }
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
