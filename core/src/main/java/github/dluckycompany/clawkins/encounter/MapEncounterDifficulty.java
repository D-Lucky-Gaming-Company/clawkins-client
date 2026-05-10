package github.dluckycompany.clawkins.encounter;

import github.dluckycompany.clawkins.asset.MapAsset;

import java.util.EnumMap;
import java.util.Map;

/**
 * Assigns {@link EncounterDifficultyTier} to {@link MapAsset} for random encounter rolls.
 * Only {@link EncounterDifficultyTier#EASY} maps are configured; other tiers are reserved
 * until maps are assigned.
 */
public final class MapEncounterDifficulty {
    private static final Map<MapAsset, EncounterDifficultyTier> BY_ASSET = new EnumMap<>(MapAsset.class);

    static {
        assignEasy(MapAsset.FIELD, MapAsset.FIELD_2, MapAsset.FIELD_4);
        // Placeholder: add MapAsset.MOUNTAIN_1 -> MIDDLE, etc., when ready.
    }

    private MapEncounterDifficulty() {
    }

    private static void assignEasy(MapAsset... assets) {
        for (MapAsset a : assets) {
            BY_ASSET.put(a, EncounterDifficultyTier.EASY);
        }
    }

    public static EncounterDifficultyTier tierFor(MapAsset mapAsset) {
        if (mapAsset == null) {
            return EncounterDifficultyTier.NONE;
        }
        return BY_ASSET.getOrDefault(mapAsset, EncounterDifficultyTier.NONE);
    }
}
