package github.dluckycompany.clawkins.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.item.StatBoostEffect;

/**
 * Represents a clawkin - a party member in the player's team.
 * Clawkins persist across battles and team interactions.
 */
public class Clawkin {
    private final String id;
    private final String name;
    private final String imagePath;
    private final String iconImagePath;
    private int level;

    private int maxHp;
    private int currentHp;
    private int baseAttack;
    private int baseDefense;
    private int baseSpeed;
    private final List<BattleSkill> skills;
    private final SummaryProfile summaryProfile;
    
    // Temporary stat boosts: stat type -> (boost amount, turns remaining)
    private final Map<StatBoostEffect.StatType, StatBoost> statBoosts;

    public Clawkin(String id, String name, String imagePath, int level, int maxHp, int attack, int defense, int speed, List<BattleSkill> skills) {
        this(
            id,
            name,
            imagePath,
            "",
            level,
            maxHp,
            attack,
            defense,
            speed,
            skills,
            SummaryProfile.fromCoreStats(name, maxHp, attack, defense, speed)
        );
    }

    public Clawkin(
        String id,
        String name,
        String imagePath,
        String iconImagePath,
        int level,
        int maxHp,
        int attack,
        int defense,
        int speed,
        List<BattleSkill> skills,
        SummaryProfile summaryProfile
    ) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath == null ? "" : imagePath.trim();
        this.iconImagePath = iconImagePath == null ? "" : iconImagePath.trim();
        this.level = Math.max(1, level);
        this.maxHp = Math.max(1, maxHp);
        this.currentHp = this.maxHp;
        this.baseAttack = Math.max(1, attack);
        this.baseDefense = Math.max(0, defense);
        this.baseSpeed = Math.max(1, speed);
        this.skills = new ArrayList<>(skills == null ? List.of() : skills);
        this.summaryProfile = summaryProfile == null
            ? SummaryProfile.fromCoreStats(name, this.maxHp, this.baseAttack, this.baseDefense, this.baseSpeed)
            : summaryProfile.withFallbacks(name, this.maxHp, this.baseAttack, this.baseDefense, this.baseSpeed);
        this.statBoosts = new HashMap<>();
    }

    // ============ Core Stats ============
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getIconImagePath() {
        return iconImagePath;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    /**
     * Applies {@link LevelSystem} clamped {@code sharedLevel} and recomputes HP / ATK / DEF / SPD from
     * {@link StatGrowth} for this clawkin's id (same curves as {@link ClawkinData}).
     * <p>
     * When max HP increases, current HP rises by the same delta so wounded clawkins keep their relative
     * buffer after leveling (mirrors {@link ClawkinData#performLevelUp()}).
     */
    public void syncStatsToSharedExperienceLevel(int sharedLevel) {
        int clamped = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, sharedLevel));
        StatGrowth growth = StatGrowth.getGrowthForClawkin(id);

        int oldMaxHp = maxHp;
        int newMaxHp = Math.max(1, growth.calculateHpAtLevel(clamped));
        int newAtk = Math.max(1, growth.calculateAttackAtLevel(clamped));
        int newDef = Math.max(0, growth.calculateDefenseAtLevel(clamped));
        int newSpd = Math.max(1, growth.calculateSpeedAtLevel(clamped));

        int hpGain = newMaxHp - oldMaxHp;
        maxHp = newMaxHp;
        baseAttack = newAtk;
        baseDefense = newDef;
        baseSpeed = newSpd;
        level = clamped;

        int adjustedHp = currentHp + hpGain;
        currentHp = Math.max(0, Math.min(maxHp, adjustedHp));

        // Keep battle skill list aligned with level (SkillManager already uses SkillUnlockSystem + level).
        skills.clear();
        skills.addAll(SkillUnlockSystem.getAllSkillsUpToLevel(id, clamped));
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public int getBaseSpeed() {
        return baseSpeed;
    }

    public List<BattleSkill> getSkills() {
        return new ArrayList<>(skills);
    }

    public SummaryProfile getSummaryProfile() {
        return summaryProfile;
    }

    // ============ Effective Stats (with boosts) ============
    
    public int getEffectiveAttack() {
        int boost = getStatBoost(StatBoostEffect.StatType.ATTACK);
        return Math.max(1, baseAttack + boost);
    }

    public int getEffectiveDefense() {
        int boost = getStatBoost(StatBoostEffect.StatType.DEFENSE);
        return Math.max(0, baseDefense + boost);
    }

    public int getEffectiveSpeed() {
        int boost = getStatBoost(StatBoostEffect.StatType.SPEED);
        return Math.max(1, baseSpeed + boost);
    }

    // ============ HP Management ============
    
    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage);
        currentHp = Math.max(0, currentHp - actualDamage);
    }

    public void heal(int amount) {
        int actualAmount = Math.max(0, amount);
        currentHp = Math.min(maxHp, currentHp + actualAmount);
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public void restoreFullHealth() {
        currentHp = maxHp;
    }

    public void setCurrentHp(int hp) {
        currentHp = Math.max(0, Math.min(maxHp, hp));
    }

    // ============ Stat Boosts ============
    
    public void addStatBoost(StatBoostEffect.StatType stat, int amount, int durationTurns) {
        if (amount <= 0 || durationTurns <= 0) {
            return;
        }
        statBoosts.put(stat, new StatBoost(amount, durationTurns));
    }

    public void decrementStatBoostTimers() {
        statBoosts.values().removeIf(boost -> {
            boost.decrementTurns();
            return boost.turnsRemaining <= 0;
        });
    }

    private int getStatBoost(StatBoostEffect.StatType stat) {
        StatBoost boost = statBoosts.get(stat);
        return boost != null ? boost.amount : 0;
    }

    public boolean hasStatBoost(StatBoostEffect.StatType stat) {
        return statBoosts.containsKey(stat);
    }

    // ============ Helper Class for Stat Boosts ============
    
    private static class StatBoost {
        int amount;
        int turnsRemaining;

        StatBoost(int amount, int turnsRemaining) {
            this.amount = amount;
            this.turnsRemaining = turnsRemaining;
        }

        void decrementTurns() {
            turnsRemaining--;
        }
    }

    public static class SummaryProfile {
        private final String species;
        private final String role;
        private final String title;
        private final String overview;
        private final int profileHp;
        private final int profileAttack;
        private final int profileDefense;
        private final int profileSpeed;
        private final String hpNote;
        private final String attackNote;
        private final String defenseNote;
        private final String speedNote;

        public SummaryProfile(
            String species,
            String role,
            String title,
            String overview,
            int profileHp,
            int profileAttack,
            int profileDefense,
            int profileSpeed,
            String hpNote,
            String attackNote,
            String defenseNote,
            String speedNote
        ) {
            this.species = trimOrEmpty(species);
            this.role = trimOrEmpty(role);
            this.title = trimOrEmpty(title);
            this.overview = trimOrEmpty(overview);
            this.profileHp = Math.max(1, profileHp);
            this.profileAttack = Math.max(1, profileAttack);
            this.profileDefense = Math.max(0, profileDefense);
            this.profileSpeed = Math.max(1, profileSpeed);
            this.hpNote = trimOrEmpty(hpNote);
            this.attackNote = trimOrEmpty(attackNote);
            this.defenseNote = trimOrEmpty(defenseNote);
            this.speedNote = trimOrEmpty(speedNote);
        }

        public static SummaryProfile fromCoreStats(String name, int hp, int atk, int def, int spd) {
            String safeName = (name == null || name.isBlank()) ? "Clawkin" : name;
            return new SummaryProfile(
                "",
                "",
                "",
                safeName + " is a dependable team member with a flexible battle profile.",
                hp,
                atk,
                def,
                spd,
                "",
                "",
                "",
                ""
            );
        }

        private SummaryProfile withFallbacks(String name, int hp, int atk, int def, int spd) {
            SummaryProfile fallback = fromCoreStats(name, hp, atk, def, spd);
            return new SummaryProfile(
                species.isBlank() ? fallback.species : species,
                role.isBlank() ? fallback.role : role,
                title.isBlank() ? fallback.title : title,
                overview.isBlank() ? fallback.overview : overview,
                profileHp > 0 ? profileHp : fallback.profileHp,
                profileAttack > 0 ? profileAttack : fallback.profileAttack,
                profileDefense >= 0 ? profileDefense : fallback.profileDefense,
                profileSpeed > 0 ? profileSpeed : fallback.profileSpeed,
                hpNote,
                attackNote,
                defenseNote,
                speedNote
            );
        }

        private static String trimOrEmpty(String value) {
            return value == null ? "" : value.trim();
        }

        public String getSpecies() {
            return species;
        }

        public String getRole() {
            return role;
        }

        public String getTitle() {
            return title;
        }

        public String getOverview() {
            return overview;
        }

        public int getProfileHp() {
            return profileHp;
        }

        public int getProfileAttack() {
            return profileAttack;
        }

        public int getProfileDefense() {
            return profileDefense;
        }

        public int getProfileSpeed() {
            return profileSpeed;
        }

        public String getHpNote() {
            return hpNote;
        }

        public String getAttackNote() {
            return attackNote;
        }

        public String getDefenseNote() {
            return defenseNote;
        }

        public String getSpeedNote() {
            return speedNote;
        }
    }
}
