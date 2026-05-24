package github.dluckycompany.clawkins.encounter;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;
import github.dluckycompany.clawkins.character.StatGrowth;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wild encounter tuning (HP + offense) for field and map clawkin battles.
 * <p>
 * Early levels (1–9) use conservative Swee'pea hit-budget curves; from level 10 onward,
 * stats are capped relative to the player's party so similar-level fights stay fair.
 */
public final class WildEnemyBalance {

    /** Last level where the original early-game hit-budget curves apply verbatim. */
    public static final int EARLY_LEVEL_BALANCE_MAX_LEVEL = 9;

    private static final double DAMAGE_CHIP_FLOOR_RATIO = 0.20d;
    private static final double ARMOR_PENETRATION_P_MAX = 0.25d;
    private static final double ARMOR_PENETRATION_K = 50d;

    private static final int TYPICAL_MAX_HIT_MULTIPLIER = 2;
    private static final int WORST_CASE_HIT_MULTIPLIER = 4;
    private static final float LONGER_FIGHT_ROLL_CHANCE = 0.30f;

    private WildEnemyBalance() {
    }

    public static int averagePartyDefense(List<Clawkin> party, int fallbackLevel) {
        int total = 0;
        int count = 0;
        if (party != null) {
            for (Clawkin clawkin : party) {
                if (clawkin == null) {
                    continue;
                }
                total += clawkin.getBaseDefense();
                count++;
            }
        }
        if (count > 0) {
            return total / count;
        }
        StatGrowth growth = StatGrowth.createSweepeaGrowth();
        int level = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, fallbackLevel));
        return growth.calculateDefenseAtLevel(level);
    }

    public static int averagePartyHp(List<Clawkin> party, int fallbackLevel) {
        int total = 0;
        int count = 0;
        if (party != null) {
            for (Clawkin clawkin : party) {
                if (clawkin == null) {
                    continue;
                }
                total += clawkin.getMaxHp();
                count++;
            }
        }
        if (count > 0) {
            return total / count;
        }
        StatGrowth growth = StatGrowth.createSweepeaGrowth();
        int level = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, fallbackLevel));
        return growth.calculateHpAtLevel(level);
    }

    public static int capHpForEarlyLevels(
            int rolledHp,
            WildEncounterBalanceContext context,
            ThreadLocalRandom random
    ) {
        if (context == null) {
            return rolledHp;
        }
        ThreadLocalRandom r = random != null ? random : ThreadLocalRandom.current();

        if (context.enemyLevel() <= EARLY_LEVEL_BALANCE_MAX_LEVEL) {
            return switch (context.mode()) {
                case DEFAULT -> capHpDefault(rolledHp, context, r);
                case WILD_PARTY_AVERAGE -> capHpDefault(rolledHp, context, r);
                case ROAMING_TRAINER -> capHpTrainer(rolledHp, context, r);
            };
        }
        return capHpMidGame(rolledHp, context, r);
    }

    public static int capAttackForEarlyLevels(int rolledAttack, WildEncounterBalanceContext context) {
        if (context == null) {
            return rolledAttack;
        }
        if (context.enemyLevel() <= EARLY_LEVEL_BALANCE_MAX_LEVEL) {
            return switch (context.mode()) {
                case DEFAULT -> capAttackDefault(rolledAttack, context.enemyLevel());
                case WILD_PARTY_AVERAGE -> capAttackForPartyAverage(rolledAttack, context);
                case ROAMING_TRAINER -> capAttackTrainer(rolledAttack, context);
            };
        }
        return capAttackMidGame(rolledAttack, context);
    }

    public static int scaleSkillBaseForEarlyLevels(WildEncounterBalanceContext context, int base) {
        if (context == null) {
            return base;
        }
        if (context.enemyLevel() <= EARLY_LEVEL_BALANCE_MAX_LEVEL) {
            if (context.mode() == EncounterBalanceMode.ROAMING_TRAINER) {
                return scaleSkillBaseTrainer(context.enemyLevel(), base);
            }
            return Math.max(4, Math.round(base * earlyOffensePotencyScale(context.enemyLevel())));
        }
        return scaleSkillBaseMidGame(context, base);
    }

    /**
     * Keeps rolled enemy levels near the player's level so Lv15 Clawkins aren't routinely
     * forced into Lv18+ stat blocks.
     */
    public static int clampEnemyLevelToFairRange(
            int enemyLevel,
            int playerLevel,
            EncounterDifficultyTier tier
    ) {
        int safePlayer = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, playerLevel));
        int safeEnemy = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, enemyLevel));
        int maxOverLevel = switch (tier == null ? EncounterDifficultyTier.NONE : tier) {
            case EASY -> 1;
            case MIDDLE, INTERMEDIATE, NORMAL -> 2;
            case HARD -> 3;
            case NONE -> 2;
        };
        int minLevel = Math.max(LevelSystem.MIN_LEVEL, safePlayer - 3);
        int maxLevel = Math.min(LevelSystem.MAX_LEVEL, safePlayer + maxOverLevel);
        return Math.max(minLevel, Math.min(safeEnemy, maxLevel));
    }

    public static int estimateConservativePlayerHit(int playerLevel, int enemyDefense) {
        int safeLevel = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, playerLevel));
        StatGrowth growth = StatGrowth.createSweepeaGrowth();
        int attack = growth.calculateAttackAtLevel(safeLevel);
        int skillBase = 6 + safeLevel * 2;
        int offense = attack + skillBase;
        int defense = Math.max(0, enemyDefense);

        int raw = Math.max(1, offense - defense);
        int chip = Math.max(1, (int) Math.round(offense * DAMAGE_CHIP_FLOOR_RATIO));
        return Math.max(raw, chip);
    }

    private static int capHpDefault(int rolledHp, WildEncounterBalanceContext context, ThreadLocalRandom r) {
        int hitBudget = estimateConservativePlayerHit(context.playerLevel(), context.enemyDefense());
        int budgetCap = conservativeHitBudgetCap(context.playerLevel());
        hitBudget = Math.min(hitBudget, budgetCap);

        int minHp = Math.max(8, hitBudget);
        int typicalMaxHp = Math.max(minHp, hitBudget * TYPICAL_MAX_HIT_MULTIPLIER);
        int worstMaxHp = Math.max(typicalMaxHp, hitBudget * WORST_CASE_HIT_MULTIPLIER);

        int targetHp;
        if (r.nextFloat() >= LONGER_FIGHT_ROLL_CHANCE) {
            targetHp = minHp + r.nextInt(typicalMaxHp - minHp + 1);
        } else {
            targetHp = typicalMaxHp + r.nextInt(worstMaxHp - typicalMaxHp + 1);
        }

        return Math.max(8, Math.min(rolledHp, targetHp));
    }

    /** Trainers absorb ~2–4 conservative hits ( tougher than wild ). */
    private static int capHpTrainer(int rolledHp, WildEncounterBalanceContext context, ThreadLocalRandom r) {
        int hitBudget = estimateConservativePlayerHit(context.playerLevel(), context.enemyDefense());
        int minHp = Math.max(12, hitBudget * 2);
        int maxHp = Math.max(minHp, hitBudget * 4);
        int targetHp = minHp + r.nextInt(maxHp - minHp + 1);
        return Math.max(12, Math.min(rolledHp, targetHp));
    }

    private static int capAttackDefault(int rolledAttack, int enemyLevel) {
        StatGrowth reference = StatGrowth.createSweepeaGrowth();
        int baseline = reference.calculateAttackAtLevel(enemyLevel);
        int maxAttack = Math.round(baseline * 1.10f) + 2;
        return Math.max(4, Math.min(rolledAttack, maxAttack));
    }

    /**
     * Limits enemy offense so a typical hit deals ~5–10% of average party HP against average party DEF.
     */
    private static int capAttackForPartyAverage(int rolledAttack, WildEncounterBalanceContext context) {
        int avgHp = Math.max(1, context.partyAverageHp());
        int avgDef = Math.max(0, context.partyAverageDefense());
        int levelOver = Math.max(0, context.enemyLevel() - context.playerLevel());
        float damageDivisor = 7.5f + levelOver * 0.75f;
        int targetDamage = Math.max(8, Math.round(avgHp / damageDivisor));
        int effectiveDef = estimateEffectiveDefense(avgDef, context.enemySpeed());
        int typicalSkill = estimateTypicalDirectSkillBase(context.enemyLevel());
        int maxOffense = targetDamage + effectiveDef;
        int maxAttack = Math.max(4, maxOffense - typicalSkill);
        return Math.max(4, Math.min(rolledAttack, maxAttack));
    }

    /** Trainer ATK keyed to the higher of player/enemy level (moderate pressure). */
    private static int capAttackTrainer(int rolledAttack, WildEncounterBalanceContext context) {
        int syncLevel = Math.max(context.playerLevel(), context.enemyLevel());
        StatGrowth reference = StatGrowth.createGingerGrowth();
        int baseline = reference.calculateAttackAtLevel(syncLevel);
        int maxAttack = Math.round(baseline * 0.85f) + 2;
        return Math.max(4, Math.min(rolledAttack, maxAttack));
    }

    private static int scaleSkillBaseTrainer(int enemyLevel, int base) {
        if (enemyLevel >= 10) {
            return base;
        }
        float scale = 0.60f + enemyLevel * 0.04f;
        return Math.max(4, Math.round(base * scale));
    }

    static float earlyOffensePotencyScale(int enemyLevel) {
        if (enemyLevel <= 4) {
            return 0.35f + enemyLevel * 0.13f;
        }
        return 0.52f + enemyLevel * 0.05f;
    }

    private static int estimateTypicalDirectSkillBase(int enemyLevel) {
        int raw = 20 + Math.max(0, enemyLevel - 3) * 2;
        if (enemyLevel <= EARLY_LEVEL_BALANCE_MAX_LEVEL) {
            return Math.max(4, Math.round(raw * earlyOffensePotencyScale(enemyLevel)));
        }
        return Math.max(4, Math.round(raw * (0.78f + Math.min(15, enemyLevel) * 0.012f)));
    }

    private static int estimateEffectiveDefense(int rawDefense, int attackerSpeed) {
        int speed = Math.max(1, attackerSpeed);
        double penetration = Math.min(ARMOR_PENETRATION_P_MAX, speed / (speed + ARMOR_PENETRATION_K));
        return Math.max(0, (int) Math.floor(rawDefense * (1.0d - penetration)));
    }

    private static int conservativeHitBudgetCap(int playerLevel) {
        int safeLevel = Math.max(LevelSystem.MIN_LEVEL, Math.min(LevelSystem.MAX_LEVEL, playerLevel));
        return 8 + safeLevel + safeLevel / 2;
    }

    /** Mid-game HP stays near party average even when the enemy outlevels the player slightly. */
    private static int capHpMidGame(int rolledHp, WildEncounterBalanceContext context, ThreadLocalRandom r) {
        if (context.mode() == EncounterBalanceMode.ROAMING_TRAINER) {
            return capHpTrainer(rolledHp, context, r);
        }

        int partyHp = context.partyAverageHp();
        if (partyHp <= 0) {
            partyHp = averagePartyHp(null, context.playerLevel());
        }

        int levelOver = Math.max(0, context.enemyLevel() - context.playerLevel());
        float overLevelFactor = 1.0f + levelOver * 0.06f;
        int minHp = Math.max(20, Math.round(partyHp * 0.80f));
        int maxHp = Math.max(minHp + 1, Math.round(partyHp * (1.20f + context.enemyLevel() * 0.015f) * overLevelFactor));
        int targetHp = minHp + r.nextInt(maxHp - minHp + 1);
        return Math.max(minHp, Math.min(rolledHp, targetHp));
    }

    private static int capAttackMidGame(int rolledAttack, WildEncounterBalanceContext context) {
        if (context.mode() == EncounterBalanceMode.ROAMING_TRAINER) {
            return capAttackTrainer(rolledAttack, context);
        }
        if (context.partyAverageHp() > 0 || context.partyAverageDefense() > 0) {
            return capAttackForPartyAverage(rolledAttack, context);
        }
        return capAttackForLevelSync(rolledAttack, context);
    }

    /**
     * Caps offense when no party snapshot exists (random catalog encounters).
     * Uses the lower of enemy level and player level + 2 so Lv18 rolls don't outscale Lv15 teams.
     */
    private static int capAttackForLevelSync(int rolledAttack, WildEncounterBalanceContext context) {
        int syncLevel = Math.min(context.enemyLevel(), context.playerLevel() + 2);
        StatGrowth reference = StatGrowth.createSweepeaGrowth();
        int baseline = reference.calculateAttackAtLevel(syncLevel);
        int maxAttack = Math.round(baseline * 1.08f) + 4;
        return Math.max(4, Math.min(rolledAttack, maxAttack));
    }

    private static int scaleSkillBaseMidGame(WildEncounterBalanceContext context, int base) {
        int levelOver = Math.max(0, context.enemyLevel() - context.playerLevel());
        float overLevelPenalty = Math.max(0.72f, 1.0f - levelOver * 0.08f);
        float playerPotency = 0.78f + Math.min(15, context.playerLevel()) * 0.012f;
        return Math.max(4, Math.round(base * overLevelPenalty * playerPotency));
    }
}
