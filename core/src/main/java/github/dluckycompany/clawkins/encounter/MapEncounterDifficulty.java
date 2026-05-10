package github.dluckycompany.clawkins.encounter;

import github.dluckycompany.clawkins.asset.MapAsset;

import java.util.EnumMap;
import java.util.Map;

/**
 * Assigns {@link EncounterDifficultyTier} to {@link MapAsset} for random encounter rolls.
 */
public final class MapEncounterDifficulty {
    private static final Map<MapAsset, EncounterDifficultyTier> BY_ASSET = new EnumMap<>(MapAsset.class);

    static {
        assignEasy(
                MapAsset.FIELD,
                MapAsset.FIELD_2,
                MapAsset.FIELD_3,
                MapAsset.FIELD_4,
                MapAsset.FIELD_5,
                MapAsset.FIELD_SECRET);
        assignIntermediate(
                MapAsset.MOUNTAIN_1,
                MapAsset.MOUNTAIN_2,
                MapAsset.MOUNTAIN_3,
                MapAsset.MOUNTAIN_4,
                MapAsset.MOUNTAIN_5);
        assignHard(
                MapAsset.CAVE_ENTRANCE,
                MapAsset.CAVE_1,
                MapAsset.CAVE_2,
                MapAsset.CAVE_3);
    }

    private MapEncounterDifficulty() {
    }

    private static void assignEasy(MapAsset... assets) {
        for (MapAsset a : assets) {
            BY_ASSET.put(a, EncounterDifficultyTier.EASY);
        }
    }

    private static void assignIntermediate(MapAsset... assets) {
        for (MapAsset a : assets) {
            BY_ASSET.put(a, EncounterDifficultyTier.INTERMEDIATE);
        }
    }

    private static void assignHard(MapAsset... assets) {
        for (MapAsset a : assets) {
            BY_ASSET.put(a, EncounterDifficultyTier.HARD);
        }
    }

    public static EncounterDifficultyTier tierFor(MapAsset mapAsset) {
        if (mapAsset == null) {
            return EncounterDifficultyTier.NONE;
        }
        return BY_ASSET.getOrDefault(mapAsset, EncounterDifficultyTier.NONE);
    }
}
