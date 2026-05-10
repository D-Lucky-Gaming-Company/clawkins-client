package github.dluckycompany.clawkins.character;

/**
 * Defines stat growth patterns for Clawkins.
 * Each Clawkin has a unique growth profile that determines how their stats increase per level.
 */
public class StatGrowth {
    
    private final String clawkinId;
    private final GrowthRate hpGrowth;
    private final GrowthRate attackGrowth;
    private final GrowthRate defenseGrowth;
    private final GrowthRate speedGrowth;
    
    // Base stats at level 5 (starting level for Swee'pea)
    private final int baseHp;
    private final int baseAttack;
    private final int baseDefense;
    private final int baseSpeed;
    
    public StatGrowth(
        String clawkinId,
        int baseHp,
        int baseAttack,
        int baseDefense,
        int baseSpeed,
        GrowthRate hpGrowth,
        GrowthRate attackGrowth,
        GrowthRate defenseGrowth,
        GrowthRate speedGrowth
    ) {
        this.clawkinId = clawkinId;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseSpeed = baseSpeed;
        this.hpGrowth = hpGrowth;
        this.attackGrowth = attackGrowth;
        this.defenseGrowth = defenseGrowth;
        this.speedGrowth = speedGrowth;
    }
    
    /**
     * Calculates HP at a specific level.
     * 
     * @param level The target level
     * @return HP value at that level
     */
    public int calculateHpAtLevel(int level) {
        return calculateStat(baseHp, hpGrowth, level, 5);
    }
    
    /**
     * Calculates Attack at a specific level.
     * 
     * @param level The target level
     * @return Attack value at that level
     */
    public int calculateAttackAtLevel(int level) {
        return calculateStat(baseAttack, attackGrowth, level, 5);
    }
    
    /**
     * Calculates Defense at a specific level.
     * 
     * @param level The target level
     * @return Defense value at that level
     */
    public int calculateDefenseAtLevel(int level) {
        return calculateStat(baseDefense, defenseGrowth, level, 5);
    }
    
    /**
     * Calculates Speed at a specific level.
     * 
     * @param level The target level
     * @return Speed value at that level
     */
    public int calculateSpeedAtLevel(int level) {
        return calculateStat(baseSpeed, speedGrowth, level, 5);
    }
    
    /**
     * Generic stat calculation based on growth rate.
     * 
     * @param baseStat Base stat value at baseLevel
     * @param growthRate Growth rate enum
     * @param targetLevel Target level to calculate
     * @param baseLevel Base level (usually 5 for Swee'pea)
     * @return Calculated stat value
     */
    private int calculateStat(int baseStat, GrowthRate growthRate, int targetLevel, int baseLevel) {
        if (targetLevel <= baseLevel) {
            return baseStat;
        }
        
        int levelDiff = targetLevel - baseLevel;
        float growthPerLevel = growthRate.getGrowthPerLevel();
        
        // Linear growth with rounding
        int totalGrowth = Math.round(growthPerLevel * levelDiff);
        
        return baseStat + totalGrowth;
    }
    
    public String getClawkinId() {
        return clawkinId;
    }
    
    /**
     * Growth rate enum defining how quickly stats increase.
     */
    public enum GrowthRate {
        VERY_SLOW(1.5f),   // +1.5 per level (e.g., Speed for tanks)
        SLOW(2.0f),        // +2 per level
        MODERATE(3.0f),    // +3 per level
        FAST(4.0f),        // +4 per level
        VERY_FAST(5.0f),   // +5 per level (e.g., HP for tanks)
        EXTREME(7.0f);     // +7 per level (HP only)
        
        private final float growthPerLevel;
        
        GrowthRate(float growthPerLevel) {
            this.growthPerLevel = growthPerLevel;
        }
        
        public float getGrowthPerLevel() {
            return growthPerLevel;
        }
    }
    
    /**
     * Factory method to create Swee'pea's growth profile.
     * Swee'pea is a tanky bruiser with high HP and DEF growth.
     * 
     * Starting stats at Level 5:
     * - HP: 55
     * - ATK: 35
     * - DEF: 50
     * - SPEED: 20
     * 
     * @return StatGrowth for Swee'pea
     */
    public static StatGrowth createSweepeaGrowth() {
        return new StatGrowth(
            "clawkin_sweepea",
            55,   // Base HP at level 5
            35,   // Base ATK at level 5
            50,   // Base DEF at level 5
            20,   // Base SPEED at level 5
            GrowthRate.EXTREME,      // HP: +7 per level (very tanky)
            GrowthRate.MODERATE,     // ATK: +3 per level (moderate damage)
            GrowthRate.VERY_FAST,    // DEF: +5 per level (high defense)
            GrowthRate.VERY_SLOW     // SPEED: +1.5 per level (slow)
        );
    }
    
    /**
     * Factory method to create Ginger's growth profile.
     * Ginger is a fast attacker with high speed and offense.
     * 
     * Starting stats at Level 5:
     * - HP: 35
     * - ATK: 45
     * - DEF: 25
     * - SPEED: 60
     * 
     * @return StatGrowth for Ginger
     */
    public static StatGrowth createGingerGrowth() {
        return new StatGrowth(
            "clawkin_ginger",
            35,   // Base HP at level 5 (low durability)
            45,   // Base ATK at level 5 (high offense)
            25,   // Base DEF at level 5 (low defense)
            60,   // Base SPEED at level 5 (very fast)
            GrowthRate.MODERATE,     // HP: +3 per level (glass cannon)
            GrowthRate.VERY_FAST,    // ATK: +5 per level (strong offense)
            GrowthRate.SLOW,         // DEF: +2 per level (stays fragile)
            GrowthRate.FAST          // SPEED: +4 per level (maintains speed advantage)
        );
    }
    
    /**
     * Factory method to create Dart's growth profile.
     * Dart is a fast attacker with low defense.
     * 
     * @return StatGrowth for Dart
     */
    public static StatGrowth createDartGrowth() {
        return new StatGrowth(
            "clawkin_dart",
            45,   // Base HP at level 5
            45,   // Base ATK at level 5
            25,   // Base DEF at level 5
            40,   // Base SPEED at level 5
            GrowthRate.MODERATE,     // HP: +3 per level
            GrowthRate.VERY_FAST,    // ATK: +5 per level
            GrowthRate.SLOW,         // DEF: +2 per level
            GrowthRate.FAST          // SPEED: +4 per level
        );
    }
    
    /**
     * Gets the appropriate growth profile for a Clawkin by ID.
     * 
     * @param clawkinId The Clawkin's ID
     * @return StatGrowth profile, or Ginger's profile as default
     */
    public static StatGrowth getGrowthForClawkin(String clawkinId) {
        if (clawkinId == null) {
            return createGingerGrowth();
        }
        
        String normalized = clawkinId.toLowerCase().trim();
        
        if (normalized.contains("sweepea") || normalized.contains("swee")) {
            return createSweepeaGrowth();
        } else if (normalized.contains("dart")) {
            return createDartGrowth();
        } else if (normalized.contains("ginger")) {
            return createGingerGrowth();
        }
        
        // Default to Ginger's balanced growth
        return createGingerGrowth();
    }
}
