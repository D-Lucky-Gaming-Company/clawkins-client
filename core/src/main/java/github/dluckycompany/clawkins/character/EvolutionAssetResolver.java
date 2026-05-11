package github.dluckycompany.clawkins.character;

import java.util.Map;

/**
 * Resolves evolved Clawkin image assets based on level thresholds.
 * At Level 20, each Clawkin transforms and receives a new portrait.
 *
 * This is the single source of truth for evolution asset mappings.
 */
public final class EvolutionAssetResolver {

    /** The level at which Clawkins evolve and receive new art. */
    public static final int EVOLUTION_LEVEL = 20;

    /** Mapping of Clawkin ID → evolved portrait path (Level 20+). */
    private static final Map<String, String> EVOLVED_IMAGE_PATHS = Map.of(
            "clawkin_ginger", "entities/clawkins/Clawkin_10_Ginger_Blur.png",
            "clawkin_sweepea", "entities/clawkins/Clawkin_11_Sleepea'.png",
            "clawkin_dart", "entities/clawkins/Clawkin_12_Dark.png"
    );

    private EvolutionAssetResolver() {
        // Utility class — no instantiation
    }

    /**
     * Returns the evolved image path for a Clawkin if it has reached the evolution level.
     *
     * @param clawkinId the Clawkin's ID (e.g. "clawkin_ginger")
     * @param level     the Clawkin's current level
     * @return the evolved image path, or {@code null} if no evolution applies
     */
    public static String resolveEvolvedImagePath(String clawkinId, int level) {
        if (level < EVOLUTION_LEVEL) {
            return null;
        }
        if (clawkinId == null) {
            return null;
        }
        String normalized = clawkinId.toLowerCase().trim();
        return EVOLVED_IMAGE_PATHS.get(normalized);
    }

    /**
     * Returns true if the given Clawkin has an evolution defined.
     */
    public static boolean hasEvolution(String clawkinId) {
        if (clawkinId == null) {
            return false;
        }
        return EVOLVED_IMAGE_PATHS.containsKey(clawkinId.toLowerCase().trim());
    }
}
