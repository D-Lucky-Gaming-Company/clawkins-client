package github.dluckycompany.clawkins.encounter;

import github.dluckycompany.clawkins.battle.BattleSkill;
import github.dluckycompany.clawkins.character.LevelSystem;
import github.dluckycompany.clawkins.character.StatGrowth;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * For map encounter tables like {@code easy_enemy} / {@code intermediate_enemy} / {@code hard_enemy},
 * replaces TMX-authored combat numbers with a fresh roll using real {@link StatGrowth} profiles
 * (Ginger, Dart, Swee'pea), wide per-stat noise, and build quirks so repeated fights stay varied.
 * <p>
 * Invoked from {@link github.dluckycompany.clawkins.battle.BattleService} at battle start; identity
 * fields (encounter id, display name, portrait, table id) are preserved from the map event.
 */
public final class WildClawkinEncounterGenerator {
    private static final RandomEncounterGenerator RANDOM_ENCOUNTER_GENERATOR = new RandomEncounterGenerator();

    private static final StatGrowth[] WILD_PROFILES = {
            StatGrowth.createGingerGrowth(),
            StatGrowth.createDartGrowth(),
            StatGrowth.createSweepeaGrowth(),
    };

    /** Stat silhouette multipliers {hp, atk, def, spd} — combined with profile + noise for variety. */
    private static final double[][] BUILD_ARCHETYPES = {
            {1.12, 1.10, 0.90, 1.00},
            {1.14, 0.92, 1.06, 0.92},
            {0.92, 1.12, 0.94, 1.10},
            {1.06, 0.94, 1.14, 0.94},
            {0.94, 1.14, 1.08, 0.90},
            {1.10, 1.06, 1.04, 0.84},
            {1.16, 0.88, 1.02, 0.96},
            {0.90, 1.08, 1.12, 0.96},
            {1.08, 1.14, 0.88, 0.94},
            {1.04, 0.90, 1.10, 1.02},
            {0.96, 1.06, 0.96, 1.08},
            {1.18, 0.94, 0.92, 0.92},
            {0.92, 0.96, 1.16, 1.02},
            {1.02, 1.18, 0.92, 0.94},
            {1.06, 1.02, 0.94, 1.04},
            {0.94, 1.04, 1.04, 1.02},
            {1.10, 0.96, 0.96, 1.06},
            {0.98, 1.12, 1.06, 0.90},
            {1.12, 0.90, 1.08, 0.94},
            {1.00, 1.00, 1.10, 0.94},
            {1.04, 1.08, 0.92, 1.00},
            {0.96, 0.98, 1.08, 1.04},
            {1.14, 1.02, 0.94, 0.92},
            {0.92, 1.16, 0.94, 1.02},
            {1.08, 0.92, 1.10, 0.94},
            {1.02, 1.14, 1.02, 0.86},
            {1.06, 0.88, 1.06, 1.04},
            {0.94, 1.06, 1.14, 0.90},
            {1.16, 0.96, 0.94, 0.96},
            {0.90, 1.04, 0.98, 1.12},
            {1.00, 1.18, 0.90, 0.96},
            {1.12, 0.94, 0.98, 1.00},
    };

    private WildClawkinEncounterGenerator() {
    }

    /**
     * If {@code authored} uses a wild table, returns a new event with rolled level/stats/skills;
     * otherwise returns {@code authored} unchanged.
     */
    public static EncounterEvent rollIfWildTable(EncounterEvent authored, int playerLevel) {
        if (authored == null || !WildEncounterTableIds.usesWildClawkinStats(authored.getEncounterTableId())) {
            return authored;
        }
        EncounterDifficultyTier tier = WildEncounterTableIds.tierForWildTable(authored.getEncounterTableId());
        EncounterEvent randomRoll = RANDOM_ENCOUNTER_GENERATOR.composeEncounter(tier, Math.max(1, playerLevel));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StatGrowth growth = WILD_PROFILES[r.nextInt(WILD_PROFILES.length)];

        int enemyLevel = Math.max(
                LevelSystem.MIN_LEVEL,
                Math.min(LevelSystem.MAX_LEVEL, randomRoll.getEnemyLevel()));

        int hp = growth.calculateHpAtLevel(enemyLevel);
        int atk = growth.calculateAttackAtLevel(enemyLevel);
        int def = growth.calculateDefenseAtLevel(enemyLevel);
        int spd = growth.calculateSpeedAtLevel(enemyLevel);

        hp = wideNoise(hp, r);
        atk = wideNoise(atk, r);
        def = wideNoise(def, r);
        spd = wideNoise(spd, r);

        int[] core = applyBuildQuirk(hp, atk, def, spd, r.nextInt(10_000), r);
        hp = core[0];
        atk = core[1];
        def = core[2];
        spd = core[3];

        hp = Math.max(12, hp);
        atk = Math.max(4, atk + RandomEncounterGenerator.tierPotencyBonus(tier) * 2);
        def = Math.max(1, def);
        spd = Math.max(2, spd);

        List<BattleSkill> skills = randomRoll.getEnemySkills();

        return new EncounterEvent(
                authored.getType(),
                authored.getEncounterId(),
                authored.getEncounterTableId(),
                enemyLevel,
                hp,
                atk,
                def,
                spd,
                skills,
                randomRoll.getEnemyName(),
                authored.getEnemyImagePath()
        );
    }

    private static int wideNoise(int stat, ThreadLocalRandom r) {
        double m = 0.78d + r.nextDouble() * 0.38d;
        return Math.max(1, (int) Math.round(stat * m));
    }

    /**
     * Shifts the rolled stat block toward distinct silhouettes (glass cannon, brick, skirmisher, …).
     */
    private static int[] applyBuildQuirk(int hp, int atk, int def, int spd, int pattern, ThreadLocalRandom r) {
        double[] row = BUILD_ARCHETYPES[Math.floorMod(pattern, BUILD_ARCHETYPES.length)];
        double fh = row[0];
        double fa = row[1];
        double fd = row[2];
        double fs = row[3];

        fh *= 0.97d + r.nextDouble() * 0.06d;
        fa *= 0.97d + r.nextDouble() * 0.06d;
        fd *= 0.97d + r.nextDouble() * 0.06d;
        fs *= 0.97d + r.nextDouble() * 0.06d;

        return new int[] {
                Math.max(1, (int) Math.round(hp * fh)),
                Math.max(1, (int) Math.round(atk * fa)),
                Math.max(0, (int) Math.round(def * fd)),
                Math.max(1, (int) Math.round(spd * fs))
        };
    }
}
