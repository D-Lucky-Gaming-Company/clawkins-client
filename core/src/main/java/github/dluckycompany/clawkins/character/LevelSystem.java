package github.dluckycompany.clawkins.character;

/**
 * Core leveling system that manages EXP progression and level-ups.
 * Handles EXP thresholds, level calculations, and stat growth.
 */
public class LevelSystem {
    
    /** Maximum level a Clawkin can reach */
    public static final int MAX_LEVEL = 30;
    
    /** Minimum level a Clawkin can have */
    public static final int MIN_LEVEL = 1;
    
    /**
     * Calculates the total EXP required to reach a specific level.
     * Uses a flat 50 EXP per level for consistent, fast progression.
     * 
     * Formula: EXP = 50 * (level - 1)
     * 
     * @param level The target level (1-30)
     * @return Total EXP required to reach that level from level 1
     */
    public static int getExpRequiredForLevel(int level) {
        if (level <= MIN_LEVEL) {
            return 0;
        }
        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        
        // Flat 50 EXP per level for fast, consistent progression
        int expPerLevel = 50;
        int levelDiff = level - 1;
        
        return expPerLevel * levelDiff;
    }
    
    /**
     * Calculates EXP needed to advance from current level to next level.
     * 
     * @param currentLevel The current level
     * @return EXP needed for next level
     */
    public static int getExpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return 0; // Already at max level
        }
        
        int currentLevelExp = getExpRequiredForLevel(currentLevel);
        int nextLevelExp = getExpRequiredForLevel(currentLevel + 1);
        
        return nextLevelExp - currentLevelExp;
    }
    
    /**
     * Calculates what level a Clawkin should be based on total EXP.
     * 
     * @param totalExp Total accumulated EXP
     * @return The level corresponding to that EXP amount
     */
    public static int calculateLevelFromExp(int totalExp) {
        if (totalExp <= 0) {
            return MIN_LEVEL;
        }
        
        // Binary search for efficiency
        for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
            int expForThisLevel = getExpRequiredForLevel(level);
            int expForNextLevel = getExpRequiredForLevel(level + 1);
            
            if (totalExp >= expForThisLevel && totalExp < expForNextLevel) {
                return level;
            }
        }
        
        return MAX_LEVEL;
    }
    
    /**
     * Calculates EXP reward for defeating an enemy.
     * Balanced to grant approximately 2 levels per victory (100 EXP total).
     * 
     * @param enemyLevel The level of the defeated enemy
     * @param isWildBattle True if wild encounter, false if trainer battle
     * @return EXP reward amount
     */
    public static int calculateExpReward(int enemyLevel, boolean isWildBattle) {
        // Base EXP: ~100 EXP per victory = 2 levels (50 EXP per level)
        int baseExp = 100;
        
        // Trainer battles give slightly more EXP
        if (!isWildBattle) {
            baseExp = (int) (baseExp * 1.2f); // 120 EXP = ~2.4 levels
        }
        
        return Math.max(50, baseExp);
    }
    
    /**
     * Calculates EXP reward for completing a battle round.
     * Grants small EXP after each round to reward participation.
     * 
     * @return EXP reward per round
     */
    public static int calculateRoundExpReward() {
        // Grant 5 EXP per round (10% of a level)
        // This ensures progression even in difficult/lost battles
        return 5;
    }
    
    /**
     * Checks if a level is valid.
     * 
     * @param level The level to check
     * @return True if level is within valid range
     */
    public static boolean isValidLevel(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }
}
