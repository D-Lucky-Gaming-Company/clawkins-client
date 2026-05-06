package github.dluckycompany.clawkins.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;

public class SaveStateManager {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path saveDir;

    public SaveStateManager() {
        this.saveDir = Paths.get(System.getProperty("user.home"), "Documents", "Clawkins", "save_states");
    }

    public Path getSaveDirectory() {
        ensureSaveDirectory();
        return saveDir;
    }

    public boolean hasSaveStates() {
        return !listSaveStates().isEmpty();
    }

    public SaveState createSaveState(SaveState state) {
        if (state == null) {
            return null;
        }
        ensureSaveDirectory();
        String timestamp = LocalDateTime.now().format(FILE_FORMAT);
        String fileName = "save_" + timestamp + ".txt";
        state.setFileName(fileName);
        if (state.getCreatedAt() == null || state.getCreatedAt().isBlank()) {
            state.setCreatedAt(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        }
        if (state.getDisplayName() == null || state.getDisplayName().isBlank()) {
            state.setDisplayName("Save " + state.getCreatedAt());
        }
        if (!writeState(saveDir.resolve(fileName), state)) {
            return null;
        }
        return state;
    }

    public List<SaveState> listSaveStates() {
        ensureSaveDirectory();
        List<SaveState> saves = new ArrayList<>();
        try (var stream = Files.list(saveDir)) {
            stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".txt"))
                .forEach(path -> {
                    SaveState state = loadSaveStateFromPath(path);
                    if (state != null) {
                        saves.add(state);
                    }
                });
        } catch (IOException ex) {
            Gdx.app.error("SaveStateManager", "Failed to list save states: " + ex.getMessage());
        }

        saves.sort(Comparator.comparingLong(SaveState::getSortEpoch).reversed());
        return saves;
    }

    public SaveState loadSaveState(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        ensureSaveDirectory();
        return loadSaveStateFromPath(saveDir.resolve(fileName));
    }

    public boolean updateSaveState(String fileName, SaveState state) {
        if (fileName == null || fileName.isBlank() || state == null) {
            return false;
        }
        ensureSaveDirectory();
        state.setFileName(fileName);
        if (state.getCreatedAt() == null || state.getCreatedAt().isBlank()) {
            state.setCreatedAt(LocalDateTime.now().format(TIMESTAMP_FORMAT));
        }
        if (state.getDisplayName() == null || state.getDisplayName().isBlank()) {
            state.setDisplayName("Save " + state.getCreatedAt());
        }
        return writeState(saveDir.resolve(fileName), state);
    }

    public boolean deleteSaveState(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        ensureSaveDirectory();
        try {
            return Files.deleteIfExists(saveDir.resolve(fileName));
        } catch (IOException ex) {
            Gdx.app.error("SaveStateManager", "Failed to delete save state: " + fileName + " -> " + ex.getMessage());
            return false;
        }
    }

    private SaveState loadSaveStateFromPath(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            SaveState state = parse(content);
            if (state == null) {
                return null;
            }
            state.setFileName(path.getFileName().toString());
            state.setSortEpoch(resolveSortEpoch(state, path));
            return state;
        } catch (IOException ex) {
            Gdx.app.error("SaveStateManager", "Failed to read save state: " + path + " -> " + ex.getMessage());
            return null;
        }
    }

    private long resolveSortEpoch(SaveState state, Path path) {
        long epoch = parseCreatedAtEpoch(state.getCreatedAt());
        if (epoch > 0) {
            return epoch;
        }
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private long parseCreatedAtEpoch(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return 0L;
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(createdAt, TIMESTAMP_FORMAT);
            return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private void ensureSaveDirectory() {
        try {
            Files.createDirectories(saveDir);
        } catch (IOException ex) {
            Gdx.app.error("SaveStateManager", "Failed to create save directory: " + saveDir + " -> " + ex.getMessage());
        }
    }

    private boolean writeState(Path path, SaveState state) {
        try {
            Files.writeString(path, serialize(state), StandardCharsets.UTF_8);
            return true;
        } catch (IOException ex) {
            Gdx.app.error("SaveStateManager", "Failed to write save state: " + path + " -> " + ex.getMessage());
            return false;
        }
    }

    private String serialize(SaveState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Clawkins Save State\n");
        builder.append("version=1\n");
        writeField(builder, "displayName", state.getDisplayName());
        writeField(builder, "createdAt", state.getCreatedAt());
        writeField(builder, "mapKey", state.getMapKey());
        writeField(builder, "playerX", Float.toString(state.getPlayerX()));
        writeField(builder, "playerY", Float.toString(state.getPlayerY()));
        writeField(builder, "money", Long.toString(state.getMoney()));
        writeField(builder, "activeClawkinIndex", Integer.toString(state.getActiveClawkinIndex()));

        List<SaveState.PartyEntry> party = state.getParty();
        writeField(builder, "party.count", Integer.toString(party.size()));
        for (int i = 0; i < party.size(); i++) {
            SaveState.PartyEntry entry = party.get(i);
            writeField(builder, "party." + i + ".id", entry.getId());
            writeField(builder, "party." + i + ".name", entry.getName());
            writeField(builder, "party." + i + ".imagePath", entry.getImagePath());
            writeField(builder, "party." + i + ".iconImagePath", entry.getIconImagePath());
            writeField(builder, "party." + i + ".level", Integer.toString(entry.getLevel()));
            writeField(builder, "party." + i + ".maxHp", Integer.toString(entry.getMaxHp()));
            writeField(builder, "party." + i + ".currentHp", Integer.toString(entry.getCurrentHp()));
            writeField(builder, "party." + i + ".attack", Integer.toString(entry.getAttack()));
            writeField(builder, "party." + i + ".defense", Integer.toString(entry.getDefense()));
            writeField(builder, "party." + i + ".speed", Integer.toString(entry.getSpeed()));

            SaveState.SummaryEntry summary = entry.getSummary();
            if (summary != null) {
                writeField(builder, "party." + i + ".summary.species", summary.getSpecies());
                writeField(builder, "party." + i + ".summary.role", summary.getRole());
                writeField(builder, "party." + i + ".summary.title", summary.getTitle());
                writeField(builder, "party." + i + ".summary.overview", summary.getOverview());
                writeField(builder, "party." + i + ".summary.profileHp", Integer.toString(summary.getProfileHp()));
                writeField(builder, "party." + i + ".summary.profileAttack", Integer.toString(summary.getProfileAttack()));
                writeField(builder, "party." + i + ".summary.profileDefense", Integer.toString(summary.getProfileDefense()));
                writeField(builder, "party." + i + ".summary.profileSpeed", Integer.toString(summary.getProfileSpeed()));
                writeField(builder, "party." + i + ".summary.hpNote", summary.getHpNote());
                writeField(builder, "party." + i + ".summary.attackNote", summary.getAttackNote());
                writeField(builder, "party." + i + ".summary.defenseNote", summary.getDefenseNote());
                writeField(builder, "party." + i + ".summary.speedNote", summary.getSpeedNote());
            }

            List<SaveState.SkillEntry> skills = entry.getSkills();
            writeField(builder, "party." + i + ".skillCount", Integer.toString(skills.size()));
            for (int s = 0; s < skills.size(); s++) {
                SaveState.SkillEntry skill = skills.get(s);
                writeField(builder, "party." + i + ".skill." + s + ".name", skill.getName());
                writeField(builder, "party." + i + ".skill." + s + ".effectType", skill.getEffectType());
                writeField(builder, "party." + i + ".skill." + s + ".effectBaseStat", Integer.toString(skill.getEffectBaseStat()));
                writeField(builder, "party." + i + ".skill." + s + ".effectStatScale", skill.getEffectStatScale());
                writeField(builder, "party." + i + ".skill." + s + ".effectDurationTurns", Integer.toString(skill.getEffectDurationTurns()));
                writeField(builder, "party." + i + ".skill." + s + ".turnCooldown", Integer.toString(skill.getTurnCooldown()));
                writeField(builder, "party." + i + ".skill." + s + ".summaryDescription", skill.getSummaryDescription());
                writeField(builder, "party." + i + ".skill." + s + ".summaryEffectText", skill.getSummaryEffectText());
                writeField(builder, "party." + i + ".skill." + s + ".summaryScalingText", skill.getSummaryScalingText());
            }
        }

        List<SaveState.InventoryEntry> inventory = state.getInventory();
        writeField(builder, "inventory.count", Integer.toString(inventory.size()));
        for (int i = 0; i < inventory.size(); i++) {
            SaveState.InventoryEntry entry = inventory.get(i);
            writeField(builder, "inventory." + i + ".id", entry.getItemId());
            writeField(builder, "inventory." + i + ".qty", Integer.toString(entry.getQuantity()));
        }

        Map<String, String> flags = state.getFlags();
        writeField(builder, "flags.count", Integer.toString(flags.size()));
        int index = 0;
        for (var entry : flags.entrySet()) {
            writeField(builder, "flags." + index + ".key", entry.getKey());
            writeField(builder, "flags." + index + ".value", entry.getValue());
            index++;
        }

        return builder.toString();
    }

    private SaveState parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Map<String, String> values = new HashMap<>();
        String[] lines = content.split("\r?\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            values.put(key, value);
        }

        SaveState state = new SaveState();
        state.setDisplayName(values.getOrDefault("displayName", ""));
        state.setCreatedAt(values.getOrDefault("createdAt", ""));
        state.setMapKey(values.getOrDefault("mapKey", ""));
        state.setPlayerX(parseFloat(values.get("playerX"), 0f));
        state.setPlayerY(parseFloat(values.get("playerY"), 0f));
        state.setMoney(parseLong(values.get("money"), 0L));
        state.setActiveClawkinIndex(parseInt(values.get("activeClawkinIndex"), -1));

        int partyCount = parseInt(values.get("party.count"), 0);
        for (int i = 0; i < partyCount; i++) {
            SaveState.PartyEntry entry = new SaveState.PartyEntry();
            entry.setId(values.get("party." + i + ".id"));
            entry.setName(values.get("party." + i + ".name"));
            entry.setImagePath(values.get("party." + i + ".imagePath"));
            entry.setIconImagePath(values.get("party." + i + ".iconImagePath"));
            entry.setLevel(parseInt(values.get("party." + i + ".level"), 1));
            entry.setMaxHp(parseInt(values.get("party." + i + ".maxHp"), 1));
            entry.setCurrentHp(parseInt(values.get("party." + i + ".currentHp"), entry.getMaxHp()));
            entry.setAttack(parseInt(values.get("party." + i + ".attack"), 1));
            entry.setDefense(parseInt(values.get("party." + i + ".defense"), 0));
            entry.setSpeed(parseInt(values.get("party." + i + ".speed"), 1));

            SaveState.SummaryEntry summary = new SaveState.SummaryEntry();
            summary.setSpecies(values.get("party." + i + ".summary.species"));
            summary.setRole(values.get("party." + i + ".summary.role"));
            summary.setTitle(values.get("party." + i + ".summary.title"));
            summary.setOverview(values.get("party." + i + ".summary.overview"));
            summary.setProfileHp(parseInt(values.get("party." + i + ".summary.profileHp"), entry.getMaxHp()));
            summary.setProfileAttack(parseInt(values.get("party." + i + ".summary.profileAttack"), entry.getAttack()));
            summary.setProfileDefense(parseInt(values.get("party." + i + ".summary.profileDefense"), entry.getDefense()));
            summary.setProfileSpeed(parseInt(values.get("party." + i + ".summary.profileSpeed"), entry.getSpeed()));
            summary.setHpNote(values.get("party." + i + ".summary.hpNote"));
            summary.setAttackNote(values.get("party." + i + ".summary.attackNote"));
            summary.setDefenseNote(values.get("party." + i + ".summary.defenseNote"));
            summary.setSpeedNote(values.get("party." + i + ".summary.speedNote"));
            entry.setSummary(summary);

            int skillCount = parseInt(values.get("party." + i + ".skillCount"), 0);
            for (int s = 0; s < skillCount; s++) {
                SaveState.SkillEntry skill = new SaveState.SkillEntry();
                skill.setName(values.get("party." + i + ".skill." + s + ".name"));
                skill.setEffectType(values.get("party." + i + ".skill." + s + ".effectType"));
                skill.setEffectBaseStat(parseInt(values.get("party." + i + ".skill." + s + ".effectBaseStat"), 0));
                skill.setEffectStatScale(values.get("party." + i + ".skill." + s + ".effectStatScale"));
                skill.setEffectDurationTurns(parseInt(values.get("party." + i + ".skill." + s + ".effectDurationTurns"), 0));
                skill.setTurnCooldown(parseInt(values.get("party." + i + ".skill." + s + ".turnCooldown"), 0));
                skill.setSummaryDescription(values.get("party." + i + ".skill." + s + ".summaryDescription"));
                skill.setSummaryEffectText(values.get("party." + i + ".skill." + s + ".summaryEffectText"));
                skill.setSummaryScalingText(values.get("party." + i + ".skill." + s + ".summaryScalingText"));
                entry.getSkills().add(skill);
            }

            if ((entry.getId() == null || entry.getId().isBlank())
                && (entry.getName() == null || entry.getName().isBlank())) {
                continue;
            }
            state.getParty().add(entry);
        }

        int inventoryCount = parseInt(values.get("inventory.count"), 0);
        for (int i = 0; i < inventoryCount; i++) {
            SaveState.InventoryEntry entry = new SaveState.InventoryEntry();
            entry.setItemId(values.get("inventory." + i + ".id"));
            entry.setQuantity(parseInt(values.get("inventory." + i + ".qty"), 0));
            if (entry.getItemId() == null || entry.getItemId().isBlank()) {
                continue;
            }
            state.getInventory().add(entry);
        }

        int flagCount = parseInt(values.get("flags.count"), 0);
        for (int i = 0; i < flagCount; i++) {
            String key = values.get("flags." + i + ".key");
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = values.getOrDefault("flags." + i + ".value", "");
            state.getFlags().put(key, value);
        }

        return state;
    }

    private static void writeField(StringBuilder builder, String key, String value) {
        builder.append(key).append("=").append(value == null ? "" : value).append("\n");
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

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
