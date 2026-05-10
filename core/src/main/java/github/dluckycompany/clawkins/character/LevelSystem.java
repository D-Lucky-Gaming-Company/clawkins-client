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
     * Uses a quadratic formula for smooth progression.
     * 
     * Formula: EXP = baseEXP * (level - 1)^2 + linearEXP * (level - 1)
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
        
        // Quadratic growth: starts slow, accelerates at higher levels
        int baseEXP = 50;  // Base EXP per level
        int linearEXP = 25; // Linear component
        int levelDiff = level - 1;
        
        return (baseEXP * levelDiff * levelDiff) + (linearEXP * levelDiff);
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
     * Scales based on enemy level and difficulty.
     * 
     * @param enemyLevel The level of the defeated enemy
     * @param isWildBattle True if wild encounter, false if trainer battle
     * @return EXP reward amount
     */
    public static int calculateExpReward(int enemyLevel, boolean isWildBattle) {
        // Base EXP formula
        int baseExp = 30 + (enemyLevel * 10);
        
        // Trainer battles give more EXP
        if (!isWildBattle) {
            baseExp = (int) (baseExp * 1.5f);
        }
        
        return Math.max(10, baseExp);
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
