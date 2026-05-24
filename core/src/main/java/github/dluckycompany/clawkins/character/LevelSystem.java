package github.dluckycompany.clawkins.character;

/**
 * Core leveling system that manages EXP progression and level-ups.
 * Handles EXP thresholds, level calculations, and stat growth.
 *
 * XP Curve:
 *   Levels 1-8  : flat 50 XP each
 *   Level 9+    : 50 + (level - 8) * 45 XP  (grows by 45 per step)
 */
public class LevelSystem {
    
    /** Maximum level a Clawkin can reach */
    public static final int MAX_LEVEL = 20;
    
    /** Minimum level a Clawkin can have */
    public static final int MIN_LEVEL = 1;

    /** Levels 1-8 cost a flat amount of XP each */
    private static final int FLAT_XP_PER_LEVEL = 50;
    /** The level at which flat XP ends and scaling begins */
    private static final int SCALING_START_LEVEL = 8;
    /** Additional XP added per level step beyond the scaling start */
    private static final int SCALING_STEP_EXP = 45;
    
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
        return calculateExpReward(enemyLevel, isWildBattle, false);
    }

    /**
     * Calculates EXP reward for defeating an enemy.
     *
     * @param enemyLevel The level of the defeated enemy
     * @param isWildBattle True if wild/random clawkin encounter
     * @param roamingTrainer True if a roaming field trainer (harder than wild)
     * @return EXP reward amount
     */
    public static int calculateExpReward(int enemyLevel, boolean isWildBattle, boolean roamingTrainer) {
        int clampedLevel = Math.max(MIN_LEVEL, Math.min(enemyLevel, MAX_LEVEL));
        int expToNextForEnemy = getExpForNextLevel(clampedLevel);

        // Wild: ~3 wins per level. Story trainers: ~2 wins. Roaming trainers: ~1.5 wins.
        float rewardShare;
        if (roamingTrainer) {
            rewardShare = 0.65f;
        } else if (isWildBattle) {
            rewardShare = 0.35f;
        } else {
            rewardShare = 0.50f;
        }
        int reward = Math.round(expToNextForEnemy * rewardShare);
        return Math.max(12, reward);
    }

    /**
     * Calculates coin reward for defeating an enemy.
     *
     * Reward curve:
     * - Level 1 to 10 scales from 1 to 100 coins
     * - Level 11 to 20 scales from 100 to 500 coins
     * - Max level is capped at 500 coins
     *
     * @param enemyLevel The level of the defeated enemy
     * @return Coin reward amount
     */
    public static int calculateMoneyReward(int enemyLevel) {
        int clampedLevel = Math.max(MIN_LEVEL, Math.min(enemyLevel, MAX_LEVEL));
        if (clampedLevel <= 10) {
            float t = (clampedLevel - MIN_LEVEL) / 9f;
            return Math.round(1f + (99f * t));
        }

        float t = (clampedLevel - 10) / 10f;
        int reward = Math.round(100f + (400f * t));
        return Math.max(1, Math.min(500, reward));
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

        // Levels 1-8: flat 50 XP each.
        if (level <= SCALING_START_LEVEL) {
            return FLAT_XP_PER_LEVEL;
        }

        // Level 9+: scale upward by SCALING_STEP_EXP per level step.
        // Lv8->9 = 50+45 = 95, Lv9->10 = 140, Lv10->11 = 185, ...
        return FLAT_XP_PER_LEVEL + (level - SCALING_START_LEVEL) * SCALING_STEP_EXP;
    }
}
