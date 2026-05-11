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
        if (t.isEmpty() || "default".equals(t)) {
            return true;
        }
        return hasDifficultyToken(t);
    }

    public static EncounterDifficultyTier tierForWildTable(String encounterTableId) {
        String t = normalize(encounterTableId);
        if (t.startsWith("hard_enemy") || t.contains("hard")) {
            return EncounterDifficultyTier.HARD;
        }
        if (t.startsWith("intermediate_enemy")
                || t.contains("intermediate")
                || t.contains("middle")
                || t.contains("normal")) {
            return EncounterDifficultyTier.INTERMEDIATE;
        }
        return EncounterDifficultyTier.EASY;
    }

    private static boolean hasDifficultyToken(String normalizedTableId) {
        return normalizedTableId.contains("easy")
                || normalizedTableId.contains("intermediate")
                || normalizedTableId.contains("middle")
                || normalizedTableId.contains("normal")
                || normalizedTableId.contains("hard");
    }
}
