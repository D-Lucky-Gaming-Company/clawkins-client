package github.dluckycompany.clawkins.encounter;

/**
 * Per-map encounter rate when rolling after every qualifying distance interval
 * ({@link github.dluckycompany.clawkins.system.RandomEncounterSystem}).
 */
public enum EncounterDifficultyTier {
    /** No random encounters on this map. */
    NONE(0f),
    /** Very infrequent — field routes and similar. */
    EASY(0.20f),
    /** Placeholder tier for future maps. */
    MIDDLE(0.25f),
    /** Placeholder tier for future maps. */
    NORMAL(0.25f),
    /** Placeholder tier for future maps. */
    INTERMEDIATE(0.30f),
    /** Placeholder tier for future maps. */
    HARD(0.35f);

    private final float encounterChance;

    EncounterDifficultyTier(float encounterChance) {
        this.encounterChance = encounterChance;
    }

    public float encounterChance() {
        return encounterChance;
    }
}
