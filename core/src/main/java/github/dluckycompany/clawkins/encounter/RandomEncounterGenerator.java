package github.dluckycompany.clawkins.encounter;

import com.badlogic.gdx.math.MathUtils;
import github.dluckycompany.clawkins.battle.BattleSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds {@link EncounterEvent}s for random battles using {@link RandomEncounterCatalog}
 * for names and base stats. Skill layout follows "The Dark Rider" on {@code field.tmx}.
 */
public final class RandomEncounterGenerator {

    public RandomEncounterGenerator() {
    }

    /**
     * Publishes a random encounter to the bus when the probability roll succeeds upstream.
     */
    public void randomEncounter(EncounterDifficultyTier tier, EncounterEventBus bus) {
        randomEncounter(tier, 1, bus);
    }

    public void randomEncounter(EncounterDifficultyTier tier, int playerLevel, EncounterEventBus bus) {
        if (tier == null || tier == EncounterDifficultyTier.NONE || bus == null) {
            return;
        }
        EncounterEvent event = composeEncounter(tier, playerLevel);
        bus.publish(event);
    }

    EncounterEvent composeEncounter(EncounterDifficultyTier tier) {
        return composeEncounter(tier, 1);
    }

    EncounterEvent composeEncounter(EncounterDifficultyTier tier, int playerLevel) {
        RandomEncounterCatalog.Definition def = RandomEncounterCatalog.pickRandom(tier);
        if (def == null) {
            def = RandomEncounterCatalog.fallbackDefinition();
        }
        int enemyLevel = WildEnemyBalance.clampEnemyLevelToFairRange(
                rollEnemyLevelForTier(tier, playerLevel),
                playerLevel,
                tier
        );
        float tierScale = RandomEncounterCatalog.statScaleFor(tier);
        float levelScale = levelScaleMultiplier(enemyLevel);
        float totalScale = tierScale * levelScale;
        int hp = scaleStat(def.baseHp() + jitter(4), totalScale);
        int atk = scaleStat(def.baseAttack() + jitter(2), totalScale);
        int defStat = scaleStat(def.baseDefense() + jitter(2), totalScale);
        int spd = scaleStat(def.baseSpeed() + jitter(2), totalScale);

        hp = Math.max(12, hp);
        atk = Math.max(4, atk);
        defStat = Math.max(1, defStat);
        spd = Math.max(2, spd);

        WildEncounterBalanceContext balanceContext =
                WildEncounterBalanceContext.forDefault(playerLevel, enemyLevel, defStat, spd);
        hp = WildEnemyBalance.capHpForEarlyLevels(hp, balanceContext, ThreadLocalRandom.current());
        atk = WildEnemyBalance.capAttackForEarlyLevels(atk, balanceContext);

        String displayName = def.displayName();
        String encounterId = "random_" + tier.name().toLowerCase() + "_lv" + enemyLevel + "_" + MathUtils.random(1, 999_999);
        String tableId = "random_" + tier.name().toLowerCase();
        List<BattleSkill> skills = skillsForTier(tier, balanceContext);

        return new EncounterEvent(
                EncounterEventType.START_ENCOUNTER,
                encounterId,
                tableId,
                enemyLevel,
                hp,
                atk,
                defStat,
                spd,
                skills,
                displayName,
                ""
        );
    }

    private static int scaleStat(int value, float tierScale) {
        return Math.max(1, Math.round(value * tierScale));
    }

    private static int jitter(int range) {
        if (range <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(-range, range + 1);
    }

    /**
     * Public for {@link WildClawkinEncounterGenerator} and tests: same skill layout as random field encounters.
     */
    public static List<BattleSkill> skillsForTier(EncounterDifficultyTier tier, int enemyLevel) {
        return skillsForTier(
                tier,
                WildEncounterBalanceContext.forDefault(enemyLevel, enemyLevel, 10, 6)
        );
    }

    public static List<BattleSkill> skillsForTier(EncounterDifficultyTier tier, WildEncounterBalanceContext context) {
        int enemyLevel = context.enemyLevel();
        // Primary offense is base + attack[self]; keep bases high enough that similar-level
        // player defense (~25–40) still yields ~5–15 chip after armor pen, not endless 1s.
        int tierB = skillBonus(tier);
        int s1 = scaleSkillBaseForLevel(context, 18 + ThreadLocalRandom.current().nextInt(0, 9) + tierB * 2);
        int s2Base = scaleSkillBaseForLevel(context, 22 + ThreadLocalRandom.current().nextInt(0, 9) + tierB * 3);
        int s3 = scaleSkillBaseForLevel(context, 14 + ThreadLocalRandom.current().nextInt(0, 6) + tierB * 2);
        int s4 = scaleSkillBaseForLevel(context, 20 + ThreadLocalRandom.current().nextInt(0, 9) + tierB * 3);
        BattleSkill slot2 =
                tier == EncounterDifficultyTier.EASY
                        ? new BattleSkill("Scratch", BattleSkill.EffectType.ATTACK, s2Base, "", 0, 0)
                        : new BattleSkill(
                                "Mend",
                                BattleSkill.EffectType.HEAL,
                                s2Base,
                                "attack[self] * 0.25",
                                0,
                                0);
        List<BattleSkill> skills = new ArrayList<>();
        skills.add(new BattleSkill("Lunge", BattleSkill.EffectType.ATTACK, s1, "", 0, 0));
        if (enemyLevel >= 3) {
            skills.add(slot2);
        }
        if (enemyLevel >= 6) {
            skills.add(new BattleSkill("Brace", BattleSkill.EffectType.DEFENSE, s3, "", 2, 0));
        }
        if (enemyLevel >= 8) {
            skills.add(new BattleSkill("Ravage", BattleSkill.EffectType.DAMAGE, s4, "attack[self] * 0.65", 0, 2));
        }
        return skills;
    }

    private static int scaleSkillBaseForLevel(WildEncounterBalanceContext context, int base) {
        return WildEnemyBalance.scaleSkillBaseForEarlyLevels(context, base);
    }

    /** Small additive bump shared with wild clawkin encounters (attack pressure). */
    public static int tierPotencyBonus(EncounterDifficultyTier tier) {
        return skillBonus(tier);
    }

    private static int skillBonus(EncounterDifficultyTier tier) {
        return switch (tier) {
            case EASY -> 0;
            case MIDDLE -> 1;
            case NORMAL -> 2;
            case INTERMEDIATE -> 3;
            case HARD -> 4;
            case NONE -> 0;
        };
    }

    /** Level roll used for both catalog random encounters and wild clawkin map encounters. */
    public static int rollEnemyLevelForTier(EncounterDifficultyTier tier, int playerLevel) {
        if (tier == null || tier == EncounterDifficultyTier.NONE) {
            return 1;
        }
        int safePlayerLevel = Math.max(1, playerLevel);
        return switch (tier) {
            case EASY -> randomEasyEnemyLevel(safePlayerLevel);
            case MIDDLE -> randomMiddleEnemyLevel(safePlayerLevel); // legacy tier support
            case INTERMEDIATE -> randomIntermediateEnemyLevel(safePlayerLevel);
            case NORMAL -> randomNormalEnemyLevel(safePlayerLevel); // legacy tier support
            case HARD -> randomHardEnemyLevel(safePlayerLevel);
            case NONE -> 1;
        };
    }

    private static int randomEasyEnemyLevel(int playerLevel) {
        float roll = ThreadLocalRandom.current().nextFloat();
        int anchor = Math.max(1, playerLevel - 1);
        if (roll < 0.70f) {
            return randomBetweenInclusive(Math.max(1, anchor - 2), Math.max(1, anchor + 1));
        }
        if (roll < 0.90f) {
            return randomBetweenInclusive(Math.max(1, anchor - 4), Math.max(1, anchor - 1));
        }
        return randomBetweenInclusive(Math.max(1, anchor + 1), Math.max(1, anchor + 2));
    }

    private static int randomMiddleEnemyLevel(int playerLevel) {
        float roll = ThreadLocalRandom.current().nextFloat();
        if (roll < 0.70f) {
            return randomBetweenInclusive(Math.max(1, playerLevel - 1), Math.max(1, playerLevel + 1));
        }
        if (roll < 0.90f) {
            return randomBetweenInclusive(Math.max(1, playerLevel - 3), Math.max(1, playerLevel - 1));
        }
        return randomBetweenInclusive(Math.max(1, playerLevel + 1), Math.max(1, playerLevel + 3));
    }

    private static int randomIntermediateEnemyLevel(int playerLevel) {
        float roll = ThreadLocalRandom.current().nextFloat();
        int anchor = playerLevel + 1;
        if (roll < 0.70f) {
            return randomBetweenInclusive(Math.max(1, playerLevel - 1), Math.max(1, playerLevel + 1));
        }
        if (roll < 0.90f) {
            return randomBetweenInclusive(Math.max(1, playerLevel - 2), Math.max(1, playerLevel));
        }
        return randomBetweenInclusive(Math.max(1, playerLevel + 1), Math.max(1, playerLevel + 2));
    }

    private static int randomNormalEnemyLevel(int playerLevel) {
        float roll = ThreadLocalRandom.current().nextFloat();
        if (roll < 0.70f) {
            return randomBetweenInclusive(Math.max(1, playerLevel), Math.max(1, playerLevel + 2));
        }
        if (roll < 0.90f) {
            return randomBetweenInclusive(Math.max(1, playerLevel - 1), Math.max(1, playerLevel + 1));
        }
        return randomBetweenInclusive(Math.max(1, playerLevel + 1), Math.max(1, playerLevel + 2));
    }

    private static int randomHardEnemyLevel(int playerLevel) {
        if (ThreadLocalRandom.current().nextFloat() < 0.85f) {
            int commonMin = Math.max(1, playerLevel - 1);
            int commonMax = Math.max(commonMin, playerLevel + 1);
            return randomBetweenInclusive(commonMin, commonMax);
        }
        int rareMin = Math.max(1, playerLevel + 1);
        int rareMax = Math.max(rareMin, playerLevel + 3);
        return randomBetweenInclusive(rareMin, rareMax);
    }

    private static int randomBetweenInclusive(int minInclusive, int maxInclusive) {
        int min = Math.max(1, minInclusive);
        int max = Math.max(min, maxInclusive);
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Multiplier applied to catalog stats by enemy level; exposed for wild clawkin generation. */
    public static float levelScaleMultiplier(int level) {
        int safeLevel = Math.max(1, level);
        if (safeLevel <= 5) {
            // Keep Lv5 enemies around parity with Lv5 Clawkins.
            return 0.90f + (safeLevel - 1) * 0.025f;
        }
        if (safeLevel <= 15) {
            // Gentler mid-game ramp — Lv15 ≈ 1.30x instead of 1.45x.
            return 1.00f + (safeLevel - 5) * 0.030f;
        }
        // Lv16+: taper so high-level catalog defs don't spike past party stats.
        float at15 = 1.00f + 10 * 0.030f;
        return at15 + (safeLevel - 15) * 0.012f;
    }
}
