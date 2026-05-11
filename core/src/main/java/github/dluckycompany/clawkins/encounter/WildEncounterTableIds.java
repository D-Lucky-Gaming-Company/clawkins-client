package github.dluckycompany.clawkins.encounter;

import java.util.Locale;

/**
 * Encounter table ids on map enemies that use procedurally rolled stats (Clawkin growth curves)
 * instead of {@code enemyHp} / {@code enemyAttack} numbers authored in TMX.
 * <p>
 * {@code default} (Tiled fallback) is treated like {@code easy_enemy}.
 */
public final class WildEncounterTableIds {

    private WildEncounterTableIds() {
    }

    public static String normalize(String encounterTableId) {
        if (encounterTableId == null) {
            return "";
        }
        return encounterTableId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean usesWildClawkinStats(String encounterTableId) {
        String t = normalize(encounterTableId);
        return switch (t) {
            case "", "default", "easy_enemy", "intermediate_enemy", "hard_enemy" -> true;
            default -> false;
        };
    }

    public static EncounterDifficultyTier tierForWildTable(String encounterTableId) {
        String t = normalize(encounterTableId);
        return switch (t) {
            case "hard_enemy" -> EncounterDifficultyTier.HARD;
            case "intermediate_enemy" -> EncounterDifficultyTier.INTERMEDIATE;
            case "", "default", "easy_enemy" -> EncounterDifficultyTier.EASY;
            default -> EncounterDifficultyTier.EASY;
        };
    }
}
