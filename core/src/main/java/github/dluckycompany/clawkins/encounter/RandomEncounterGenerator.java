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
        float tierScale = RandomEncounterCatalog.statScaleFor(tier);
        int hp = scaleStat(def.baseHp() + jitter(4), tierScale);
        int atk = scaleStat(def.baseAttack() + jitter(2), tierScale);
        int defStat = scaleStat(def.baseDefense() + jitter(2), tierScale);
        int spd = scaleStat(def.baseSpeed() + jitter(2), tierScale);

        hp = Math.max(12, hp);
        atk = Math.max(4, atk);
        defStat = Math.max(1, defStat);
        spd = Math.max(2, spd);

        String displayName = def.displayName();
        String encounterId = "random_" + tier.name().toLowerCase() + "_" + MathUtils.random(1, 999_999);
        String tableId = "random_" + tier.name().toLowerCase();
        List<BattleSkill> skills = skillsForTier(tier);

        return new EncounterEvent(
                EncounterEventType.START_ENCOUNTER,
                encounterId,
                tableId,
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
     * Skill layout follows the Dark Rider example: attack / heal-with-attack scaling / defense buff.
     * Potencies get a small tier bump for higher difficulties.
     */
    private static List<BattleSkill> skillsForTier(EncounterDifficultyTier tier) {
        int s1 = 8 + ThreadLocalRandom.current().nextInt(0, 5) + skillBonus(tier);
        int s2Base = 16 + ThreadLocalRandom.current().nextInt(0, 7) + skillBonus(tier) * 2;
        int s3 = 8 + ThreadLocalRandom.current().nextInt(0, 5) + skillBonus(tier);
        return new ArrayList<>(List.of(
                new BattleSkill("Lunge", BattleSkill.EffectType.ATTACK, s1, "", 0, 0),
                new BattleSkill(
                        "Mend",
                        BattleSkill.EffectType.HEAL,
                        s2Base,
                        "attack[self] * 0.25",
                        0,
                        0
                ),
                new BattleSkill("Brace", BattleSkill.EffectType.DEFENSE, s3, "", 2, 0)
        ));
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
}
