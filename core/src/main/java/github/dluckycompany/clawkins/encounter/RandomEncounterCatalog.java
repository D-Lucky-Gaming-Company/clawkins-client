package github.dluckycompany.clawkins.encounter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Authorable pools of random encounter identities: display name plus base combat stats,
 * grouped by {@link EncounterDifficultyTier}. Stats are multiplied by
 * {@link #statScaleFor(EncounterDifficultyTier)} so the same curve can be tuned in one place.
 */
public final class RandomEncounterCatalog {

    /**
     * One row in the random encounter table (before global tier scaling and generator jitter).
     */
    public record Definition(
            String displayName,
            int baseHp,
            int baseAttack,
            int baseDefense,
            int baseSpeed) {
    }

    private static final List<Definition> EASY = List.of(
            new Definition("The Dark Rider", 40, 14, 10, 6),
            new Definition("Mist Walker", 38, 15, 9, 7),
            new Definition("Fen Lurker", 42, 13, 11, 5),
            new Definition("Dusk Scout", 36, 16, 8, 8),
            new Definition("Hollow Drifter", 44, 14, 10, 5),
            new Definition("Rime Footpad", 39, 15, 10, 6)
    );

    private static final List<Definition> MIDDLE = List.of(
            new Definition("Ash Vanguard", 48, 17, 12, 7),
            new Definition("Gutter Jackal", 46, 19, 10, 8),
            new Definition("Ironbone Marauder", 52, 16, 14, 6),
            new Definition("Needle Stalker", 44, 18, 11, 9),
            new Definition("Saltmarsh Reaver", 50, 17, 13, 7)
    );

    private static final List<Definition> NORMAL = List.of(
            new Definition("Cinder Knight", 58, 20, 15, 8),
            new Definition("Blackroot Witch", 54, 22, 13, 9),
            new Definition("Stormline Raider", 62, 19, 16, 8),
            new Definition("Glassfang Hunter", 56, 21, 14, 10),
            new Definition("Depth Charter", 60, 20, 15, 9)
    );

    private static final List<Definition> INTERMEDIATE = List.of(
            new Definition("Pale Inquisitor", 70, 23, 18, 10),
            new Definition("Mire Executioner", 74, 22, 20, 9),
            new Definition("Thorn Abbess", 66, 25, 17, 11),
            new Definition("Red Ledger Enforcer", 72, 24, 19, 10),
            new Definition("Bell Hollow Elite", 68, 24, 18, 11)
    );

    private static final List<Definition> HARD = List.of(
            new Definition("Sable Crown Knight", 85, 28, 24, 11),
            new Definition("Grave Liturge", 80, 30, 22, 12),
            new Definition("World-Thread Stalker", 88, 27, 26, 11),
            new Definition("Drowned Ordinator", 92, 26, 28, 10),
            new Definition("Third Seal Warden", 86, 29, 25, 12)
    );

    private static final Map<EncounterDifficultyTier, List<Definition>> BY_TIER = buildTierMap();

    private RandomEncounterCatalog() {
    }

    private static Map<EncounterDifficultyTier, List<Definition>> buildTierMap() {
        Map<EncounterDifficultyTier, List<Definition>> m = new EnumMap<>(EncounterDifficultyTier.class);
        m.put(EncounterDifficultyTier.EASY, EASY);
        m.put(EncounterDifficultyTier.MIDDLE, MIDDLE);
        m.put(EncounterDifficultyTier.NORMAL, NORMAL);
        m.put(EncounterDifficultyTier.INTERMEDIATE, INTERMEDIATE);
        m.put(EncounterDifficultyTier.HARD, HARD);
        return Collections.unmodifiableMap(m);
    }

    /**
     * Multiplier applied to catalog base stats (after a definition is rolled). Per-tier lists are
     * authored at increasing power; this curve adds a small extra step per tier for global tuning.
     */
    public static float statScaleFor(EncounterDifficultyTier tier) {
        if (tier == null || tier == EncounterDifficultyTier.NONE) {
            return 1.0f;
        }
        return switch (tier) {
            case EASY -> 1.0f;
            case MIDDLE -> 1.04f;
            case NORMAL -> 1.08f;
            case INTERMEDIATE -> 1.12f;
            case HARD -> 1.16f;
            case NONE -> 1.0f;
        };
    }

    /** Safe default when no tier matches (should not happen for normal random rolls). */
    public static Definition fallbackDefinition() {
        return EASY.getFirst();
    }

    /**
     * Picks a uniform random definition for this tier. Falls back to {@link #EASY} if the tier
     * has no list yet. {@link EncounterDifficultyTier#NONE} yields {@code null}.
     */
    public static Definition pickRandom(EncounterDifficultyTier tier) {
        if (tier == null || tier == EncounterDifficultyTier.NONE) {
            return null;
        }
        List<Definition> list = BY_TIER.get(tier);
        if (list == null || list.isEmpty()) {
            list = EASY;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
