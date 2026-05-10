package github.dluckycompany.clawkins.character;

/**
 * Core leveling system that manages EXP progression and level-ups.
 * Handles EXP thresholds, level calculations, and stat growth.
 */
public class LevelSystem {
    
    /** Maximum level a Clawkin can reach */
    public static final int MAX_LEVEL = 20;
    
    /** Minimum level a Clawkin can have */
    public static final int MIN_LEVEL = 1;

    private static final int MID_CURVE_START_LEVEL = 5;
    private static final int MID_CURVE_END_LEVEL = 8;
    
    /**
     * Calculates the total EXP required to reach a specific level.
     * Uses a staged curve so early progression is smooth while mid-level growth
     * (Lv5-Lv8) has stronger gates for short-session pacing.
     * 
     * @param level The target level (1-20)
     * @return Total EXP required to reach that level from level 1
     */
    public static int getExpRequiredForLevel(int level) {
        if (level <= MIN_LEVEL) {
            return 0;
        }
        if (level > MAX_LEVEL) {
            level = MAX_LEVEL;
        }
        int total = 0;
        for (int currentLevel = MIN_LEVEL; currentLevel < level; currentLevel++) {
            total += expNeededFromLevel(currentLevel);
        }
        return total;
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
     * Reward scales with the enemy's level requirement so progression remains
     * consistent throughout the run.
     * 
     * @param enemyLevel The level of the defeated enemy
     * @param isWildBattle True if wild encounter, false if trainer battle
     * @return EXP reward amount
     */
    public static int calculateExpReward(int enemyLevel, boolean isWildBattle) {
        int clampedLevel = Math.max(MIN_LEVEL, Math.min(enemyLevel, MAX_LEVEL));
        int expToNextForEnemy = getExpForNextLevel(clampedLevel);

        // Wild fights: about 3 wins per level. Trainer fights are faster.
        float rewardShare = isWildBattle ? 0.35f : 0.50f;
        int reward = Math.round(expToNextForEnemy * rewardShare);
        return Math.max(12, reward);
    }
    
    /**
     * Calculates EXP reward for completing a battle round.
     * Grants small EXP after each round to reward participation.
     * 
     * @return EXP reward per round
     */
    public static int calculateRoundExpReward() {
        // Small participation reward so long fights still make progress.
        return 6;
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

    private static int expNeededFromLevel(int currentLevel) {
        int level = Math.max(MIN_LEVEL, Math.min(currentLevel, MAX_LEVEL));

        // Example-aligned mid curve:
        // Lv5->6: 100, Lv6->7: 200, Lv7->8: 300
        if (level >= MID_CURVE_START_LEVEL && level <= MID_CURVE_END_LEVEL) {
            return (level - 4) * 100;
        }

        if (level < MID_CURVE_START_LEVEL) {
            return 40 + (level - 1) * 15;
        }

        // After Lv8, continue upward with gentler growth.
        return 400 + (level - MID_CURVE_END_LEVEL) * 45;
    }
}
