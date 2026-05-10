package github.dluckycompany.clawkins.character;

import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all progression data for a Clawkin.
 * This class manages level, EXP, stats, and skills.
 * Designed to be serializable for save/load support.
 */
public class ClawkinData {
    
    private final String id;
    private final String name;
    private final String imagePath;
    private final String iconImagePath;
    
    // Leveling data
    private int level;
    private int currentExp;
    
    // Stats (calculated from level and growth)
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int speed;
    
    // Skills
    private final List<BattleSkill> unlockedSkills;
    
    // Growth profile
    private final StatGrowth statGrowth;
    
    // Summary profile (for UI display)
    private final Clawkin.SummaryProfile summaryProfile;
    
    /**
     * Creates a new ClawkinData with initial stats.
     * 
     * @param id Unique identifier
     * @param name Display name
     * @param imagePath Path to portrait image
     * @param iconImagePath Path to icon image
     * @param startingLevel Initial level
     * @param summaryProfile Summary profile for UI
     */
    public ClawkinData(
        String id,
        String name,
        String imagePath,
        String iconImagePath,
        int startingLevel,
        Clawkin.SummaryProfile summaryProfile
    ) {
        this.id = id;
        this.name = name;
        this.imagePath = imagePath == null ? "" : imagePath.trim();
        this.iconImagePath = iconImagePath == null ? "" : iconImagePath.trim();
        this.summaryProfile = summaryProfile;
        
        // Get growth profile for this Clawkin
        this.statGrowth = StatGrowth.getGrowthForClawkin(id);
        
        // Initialize at starting level
        this.level = Math.max(LevelSystem.MIN_LEVEL, Math.min(startingLevel, LevelSystem.MAX_LEVEL));
        this.currentExp = LevelSystem.getExpRequiredForLevel(this.level);
        
        // Calculate stats based on level
        this.maxHp = statGrowth.calculateHpAtLevel(this.level);
        this.currentHp = this.maxHp;
        this.attack = statGrowth.calculateAttackAtLevel(this.level);
        this.defense = statGrowth.calculateDefenseAtLevel(this.level);
        this.speed = statGrowth.calculateSpeedAtLevel(this.level);
        
        // Unlock skills up to current level
        this.unlockedSkills = new ArrayList<>(SkillUnlockSystem.getAllSkillsUpToLevel(id, this.level));
    }
    
    /**
     * Grants EXP and processes level-ups.
     * 
     * @param expAmount Amount of EXP to grant
     * @return List of level-up results (empty if no level-up occurred)
     */
    public List<LevelUpResult> grantExp(int expAmount) {
        List<LevelUpResult> levelUpResults = new ArrayList<>();
        
        if (expAmount <= 0 || level >= LevelSystem.MAX_LEVEL) {
            return levelUpResults;
        }
        
        // Add EXP
        currentExp += expAmount;
        
        // Check for level-ups (can level up multiple times)
        while (level < LevelSystem.MAX_LEVEL) {
            int expForNextLevel = LevelSystem.getExpRequiredForLevel(level + 1);
            
            if (currentExp < expForNextLevel) {
                break; // Not enough EXP for next level
            }
            
            // Level up!
            LevelUpResult result = performLevelUp();
            levelUpResults.add(result);
        }
        
        return levelUpResults;
    }
    
    /**
     * Performs a single level-up.
     * 
     * @return LevelUpResult containing stat increases and new skills
     */
    private LevelUpResult performLevelUp() {
        // Store old values
        int oldLevel = level;
        int oldMaxHp = maxHp;
        int oldAttack = attack;
        int oldDefense = defense;
        int oldSpeed = speed;
        
        // Increase level
        level++;
        
        // Calculate new stats
        int newMaxHp = statGrowth.calculateHpAtLevel(level);
        int newAttack = statGrowth.calculateAttackAtLevel(level);
        int newDefense = statGrowth.calculateDefenseAtLevel(level);
        int newSpeed = statGrowth.calculateSpeedAtLevel(level);
        
        // Calculate HP increase and apply it to current HP
        int hpIncrease = newMaxHp - oldMaxHp;
        currentHp += hpIncrease; // Increase current HP by the same amount
        
        // Update stats
        maxHp = newMaxHp;
        attack = newAttack;
        defense = newDefense;
        speed = newSpeed;
        
        // Check for new skills
        List<BattleSkill> newSkills = SkillUnlockSystem.getSkillsUnlockedAtLevel(id, level);
        unlockedSkills.addAll(newSkills);
        
        // Create result
        return new LevelUpResult(
            oldLevel,
            level,
            oldMaxHp,
            newMaxHp,
            oldAttack,
            newAttack,
            oldDefense,
            newDefense,
            oldSpeed,
            newSpeed,
            newSkills
        );
    }
    
    /**
     * Gets the current progress toward the next level.
     * 
     * @return Percentage (0.0 to 1.0) of progress to next level
     */
    public float getExpProgressToNextLevel() {
        if (level >= LevelSystem.MAX_LEVEL) {
            return 1.0f;
        }
        
        int currentLevelExp = LevelSystem.getExpRequiredForLevel(level);
        int nextLevelExp = LevelSystem.getExpRequiredForLevel(level + 1);
        int expInCurrentLevel = currentExp - currentLevelExp;
        int expNeededForLevel = nextLevelExp - currentLevelExp;
        
        return (float) expInCurrentLevel / (float) expNeededForLevel;
    }
    
    /**
     * Gets EXP remaining until next level.
     * 
     * @return EXP needed for next level
     */
    public int getExpToNextLevel() {
        if (level >= LevelSystem.MAX_LEVEL) {
            return 0;
        }
        
        int nextLevelExp = LevelSystem.getExpRequiredForLevel(level + 1);
        return nextLevelExp - currentExp;
    }
    
    // ============ Getters ============
    
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
    
    public int getCurrentExp() {
        return currentExp;
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
    
    public List<BattleSkill> getUnlockedSkills() {
        return new ArrayList<>(unlockedSkills);
    }
    
    public Clawkin.SummaryProfile getSummaryProfile() {
        return summaryProfile;
    }
    
    public StatGrowth getStatGrowth() {
        return statGrowth;
    }
    
    // ============ HP Management ============
    
    public void setCurrentHp(int hp) {
        currentHp = Math.max(0, Math.min(maxHp, hp));
    }
    
    public void takeDamage(int damage) {
        int actualDamage = Math.max(1, damage);
        currentHp = Math.max(0, currentHp - actualDamage);
    }
    
    public void heal(int amount) {
        int actualAmount = Math.max(0, amount);
        currentHp = Math.min(maxHp, currentHp + actualAmount);
    }
    
    public void restoreFullHealth() {
        currentHp = maxHp;
    }
    
    public boolean isDead() {
        return currentHp <= 0;
    }
    
    public boolean isAlive() {
        return currentHp > 0;
    }
    
    /**
     * Converts this ClawkinData to a Clawkin instance.
     * Used for battle system integration.
     * 
     * @return Clawkin instance with current stats
     */
    public Clawkin toClawkin() {
        return new Clawkin(
            id,
            name,
            imagePath,
            iconImagePath,
            level,
            maxHp,
            attack,
            defense,
            speed,
            new ArrayList<>(unlockedSkills),
            summaryProfile
        );
    }
    
    /**
     * Updates this ClawkinData from a Clawkin instance.
     * Used to sync state after battles.
     * 
     * @param clawkin The Clawkin to sync from
     */
    public void syncFromClawkin(Clawkin clawkin) {
        if (clawkin == null) {
            return;
        }
        
        // Only sync HP (other stats are managed by leveling system)
        this.currentHp = clawkin.getCurrentHp();
    }
}
