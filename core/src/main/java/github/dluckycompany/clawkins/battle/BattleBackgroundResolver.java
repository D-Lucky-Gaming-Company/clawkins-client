package github.dluckycompany.clawkins.battle;

import java.util.Map;

/**
 * Data-driven resolver for battle background images based on encounter ID.
 * Maps boss encounters to their specific background assets.
 *
 * To add a new boss background:
 *   1. Add the encounter ID → asset path entry to BACKGROUNDS_BY_ENCOUNTER_ID.
 *   2. Place the PNG in the assets folder at the specified path.
 */
public final class BattleBackgroundResolver {

    /** Default background used when no boss-specific background is defined. */
    public static final String DEFAULT_BACKGROUND_PATH = "ui/battle_background.png";

    /** Boss encounter ID → background asset path. */
    private static final Map<String, String> BACKGROUNDS_BY_ENCOUNTER_ID = Map.ofEntries(
            Map.entry("boss_0_encounter", "ui/battle_ui/Backgrounds/cottage.png"),
            Map.entry("boss_1_encounter", "ui/battle_ui/Backgrounds/mansion.png"),
            Map.entry("boss_2_encounter", "ui/battle_ui/Backgrounds/cave.png")
    );

    private BattleBackgroundResolver() {
        // Utility class — no instantiation
    }

    /**
     * Resolves the background path for a given encounter.
     *
     * @param encounterId the encounter ID (e.g. "boss_0_encounter")
     * @return the background asset path, or {@link #DEFAULT_BACKGROUND_PATH} if no specific mapping exists
     */
    public static String resolve(String encounterId) {
        if (encounterId == null || encounterId.isBlank()) {
            return DEFAULT_BACKGROUND_PATH;
        }
        return BACKGROUNDS_BY_ENCOUNTER_ID.getOrDefault(encounterId.trim(), DEFAULT_BACKGROUND_PATH);
    }

    /**
     * Returns true if the encounter has a specific background defined.
     */
    public static boolean hasCustomBackground(String encounterId) {
        if (encounterId == null || encounterId.isBlank()) {
            return false;
        }
        return BACKGROUNDS_BY_ENCOUNTER_ID.containsKey(encounterId.trim());
    }
}
