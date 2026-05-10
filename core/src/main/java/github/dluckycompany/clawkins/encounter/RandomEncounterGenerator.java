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
        if (tier == null || tier == EncounterDifficultyTier.NONE || bus == null) {
            return;
        }
        EncounterEvent event = composeEncounter(tier);
        bus.publish(event);
    }

    EncounterEvent composeEncounter(EncounterDifficultyTier tier) {
        RandomEncounterCatalog.Definition def = RandomEncounterCatalog.pickRandom(tier);
        if (def == null) {
            def = RandomEncounterCatalog.fallbackDefinition();
        }
        int enemyLevel = randomEnemyLevelForTier(tier);
        float tierScale = RandomEncounterCatalog.statScaleFor(tier);
        float levelScale = levelScale(enemyLevel);
        float totalScale = tierScale * levelScale;
        int hp = scaleStat(def.baseHp() + jitter(4), totalScale);
        int atk = scaleStat(def.baseAttack() + jitter(2), totalScale);
        int defStat = scaleStat(def.baseDefense() + jitter(2), totalScale);
        int spd = scaleStat(def.baseSpeed() + jitter(2), totalScale);

        hp = Math.max(12, hp);
        atk = Math.max(4, atk);
        defStat = Math.max(1, defStat);
        spd = Math.max(2, spd);

        String displayName = def.displayName();
        String encounterId = "random_" + tier.name().toLowerCase() + "_lv" + enemyLevel + "_" + MathUtils.random(1, 999_999);
        String tableId = "random_" + tier.name().toLowerCase();
        List<BattleSkill> skills = skillsForTier(tier, enemyLevel);

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
     * Skill layout follows the Dark Rider example: attack / second offense or heal-with-attack / defense buff.
     * Easy-tier random foes never get {@link BattleSkill.EffectType#HEAL}. Potencies get a small tier bump
     * for higher difficulties.
     */
    private static List<BattleSkill> skillsForTier(EncounterDifficultyTier tier, int enemyLevel) {
        int s1 = 8 + ThreadLocalRandom.current().nextInt(0, 5) + skillBonus(tier);
        int s2Base = 16 + ThreadLocalRandom.current().nextInt(0, 7) + skillBonus(tier) * 2;
        int s3 = 8 + ThreadLocalRandom.current().nextInt(0, 5) + skillBonus(tier);
        int s4 = 14 + ThreadLocalRandom.current().nextInt(0, 7) + skillBonus(tier) * 2;
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
            skills.add(new BattleSkill("Ravage", BattleSkill.EffectType.DAMAGE, s4, "attack[self] * 0.40", 0, 2));
        }
        return skills;
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

    private static int randomEnemyLevelForTier(EncounterDifficultyTier tier) {
        if (tier == null || tier == EncounterDifficultyTier.NONE) {
            return 1;
        }
        return switch (tier) {
            case EASY -> ThreadLocalRandom.current().nextInt(1, 9); // 1-8 inclusive
            case MIDDLE -> ThreadLocalRandom.current().nextInt(6, 13);
            case NORMAL -> ThreadLocalRandom.current().nextInt(10, 17);
            case INTERMEDIATE -> ThreadLocalRandom.current().nextInt(14, 21);
            case HARD -> ThreadLocalRandom.current().nextInt(18, 21);
            case NONE -> 1;
        };
    }

    private static float levelScale(int level) {
        int safeLevel = Math.max(1, level);
        if (safeLevel <= 5) {
            // Keep Lv5 enemies around parity with Lv5 Clawkins.
            return 0.90f + (safeLevel - 1) * 0.025f;
        }
        // Past Lv5, ramp steadily so Lv9 can pressure underleveled teams.
        return 1.00f + (safeLevel - 5) * 0.045f;
    }
}
