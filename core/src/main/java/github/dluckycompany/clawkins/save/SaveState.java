package github.dluckycompany.clawkins.save;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveState {
    private String fileName;
    private String displayName;
    private String createdAt;
    private String mapKey;
    private String playerName;
    private String playerGender;
    private float playerX;
    private float playerY;
    private long money;
    private int activeClawkinIndex = -1;
    private long sortEpoch;

    private final List<PartyEntry> party = new ArrayList<>();
    private final List<InventoryEntry> inventory = new ArrayList<>();
    private final Map<String, String> flags = new HashMap<>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerGender() {
        return playerGender;
    }

    public void setPlayerGender(String playerGender) {
        this.playerGender = playerGender;
    }

    public float getPlayerX() {
        return playerX;
    }

    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    public long getMoney() {
        return money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public int getActiveClawkinIndex() {
        return activeClawkinIndex;
    }

    public void setActiveClawkinIndex(int activeClawkinIndex) {
        this.activeClawkinIndex = activeClawkinIndex;
    }

    public List<PartyEntry> getParty() {
        return party;
    }

    public List<InventoryEntry> getInventory() {
        return inventory;
    }

    public Map<String, String> getFlags() {
        return flags;
    }

    public long getSortEpoch() {
        return sortEpoch;
    }

    public void setSortEpoch(long sortEpoch) {
        this.sortEpoch = sortEpoch;
    }

    public static class PartyEntry {
        private String id;
        private String name;
        private String imagePath;
        private String iconImagePath;
        private int level;
        private int maxHp;
        private int currentHp;
        private int attack;
        private int defense;
        private int speed;
        private SummaryEntry summary;
        private final List<SkillEntry> skills = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getIconImagePath() {
            return iconImagePath;
        }

        public void setIconImagePath(String iconImagePath) {
            this.iconImagePath = iconImagePath;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getMaxHp() {
            return maxHp;
        }

        public void setMaxHp(int maxHp) {
            this.maxHp = maxHp;
        }

        public int getCurrentHp() {
            return currentHp;
        }

        public void setCurrentHp(int currentHp) {
            this.currentHp = currentHp;
        }

        public int getAttack() {
            return attack;
        }

        public void setAttack(int attack) {
            this.attack = attack;
        }

        public int getDefense() {
            return defense;
        }

        public void setDefense(int defense) {
            this.defense = defense;
        }

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public SummaryEntry getSummary() {
            return summary;
        }

        public void setSummary(SummaryEntry summary) {
            this.summary = summary;
        }

        public List<SkillEntry> getSkills() {
            return skills;
        }
    }

    public static class SummaryEntry {
        private String species;
        private String role;
        private String title;
        private String overview;
        private int profileHp;
        private int profileAttack;
        private int profileDefense;
        private int profileSpeed;
        private String hpNote;
        private String attackNote;
        private String defenseNote;
        private String speedNote;

        public String getSpecies() {
            return species;
        }

        public void setSpecies(String species) {
            this.species = species;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOverview() {
            return overview;
        }

        public void setOverview(String overview) {
            this.overview = overview;
        }

        public int getProfileHp() {
            return profileHp;
        }

        public void setProfileHp(int profileHp) {
            this.profileHp = profileHp;
        }

        public int getProfileAttack() {
            return profileAttack;
        }

        public void setProfileAttack(int profileAttack) {
            this.profileAttack = profileAttack;
        }

        public int getProfileDefense() {
            return profileDefense;
        }

        public void setProfileDefense(int profileDefense) {
            this.profileDefense = profileDefense;
        }

        public int getProfileSpeed() {
            return profileSpeed;
        }

        public void setProfileSpeed(int profileSpeed) {
            this.profileSpeed = profileSpeed;
        }

        public String getHpNote() {
            return hpNote;
        }

        public void setHpNote(String hpNote) {
            this.hpNote = hpNote;
        }

        public String getAttackNote() {
            return attackNote;
        }

        public void setAttackNote(String attackNote) {
            this.attackNote = attackNote;
        }

        public String getDefenseNote() {
            return defenseNote;
        }

        public void setDefenseNote(String defenseNote) {
            this.defenseNote = defenseNote;
        }

        public String getSpeedNote() {
            return speedNote;
        }

        public void setSpeedNote(String speedNote) {
            this.speedNote = speedNote;
        }
    }

    public static class SkillEntry {
        private String name;
        private String effectType;
        private int effectBaseStat;
        private String effectStatScale;
        private int effectDurationTurns;
        private int turnCooldown;
        private String summaryDescription;
        private String summaryEffectText;
        private String summaryScalingText;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEffectType() {
            return effectType;
        }

        public void setEffectType(String effectType) {
            this.effectType = effectType;
        }

        public int getEffectBaseStat() {
            return effectBaseStat;
        }

        public void setEffectBaseStat(int effectBaseStat) {
            this.effectBaseStat = effectBaseStat;
        }

        public String getEffectStatScale() {
            return effectStatScale;
        }

        public void setEffectStatScale(String effectStatScale) {
            this.effectStatScale = effectStatScale;
        }

        public int getEffectDurationTurns() {
            return effectDurationTurns;
        }

        public void setEffectDurationTurns(int effectDurationTurns) {
            this.effectDurationTurns = effectDurationTurns;
        }

        public int getTurnCooldown() {
            return turnCooldown;
        }

        public void setTurnCooldown(int turnCooldown) {
            this.turnCooldown = turnCooldown;
        }

        public String getSummaryDescription() {
            return summaryDescription;
        }

        public void setSummaryDescription(String summaryDescription) {
            this.summaryDescription = summaryDescription;
        }

        public String getSummaryEffectText() {
            return summaryEffectText;
        }

        public void setSummaryEffectText(String summaryEffectText) {
            this.summaryEffectText = summaryEffectText;
        }

        public String getSummaryScalingText() {
            return summaryScalingText;
        }

        public void setSummaryScalingText(String summaryScalingText) {
            this.summaryScalingText = summaryScalingText;
        }
    }

    public static class InventoryEntry {
        private String itemId;
        private int quantity;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}
