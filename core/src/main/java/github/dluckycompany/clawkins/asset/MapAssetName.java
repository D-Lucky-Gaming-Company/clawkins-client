package github.dluckycompany.clawkins.asset;

import java.util.EnumMap;
import java.util.Map;

public enum MapAssetName {
    NURSE_INTERIOR(MapAsset.NURSE_INTERIOR, "Clinic"),
    COTTAGE(MapAsset.COTTAGE, "Cottage"),
    FIELD(MapAsset.FIELD, "Field"),
    FIELD_2(MapAsset.FIELD_2, "Field"),
    FIELD_3(MapAsset.FIELD_3, "Field"),
    FIELD_4(MapAsset.FIELD_4, "Field"),
    FIELD_5(MapAsset.FIELD_5, "Field"),
    FIELD_SECRET(MapAsset.FIELD_SECRET, "Field"),
    MANSION_MAZE(MapAsset.MANSION_MAZE, "Mansion Maze"),
    MANSION_GARDEN(MapAsset.MANSION_GARDEN, "Mansion Garden"),
    MANSION_EXIT(MapAsset.MANSION_EXIT, "Mansion Exit"),
    BACKALLEY(MapAsset.BACKALLEY, "Back Alley"),
    BACKALLEY_2(MapAsset.BACKALLEY_2, "Back Alley"),
    BACKALLEY_3(MapAsset.BACKALLEY_3, "Back Alley"),
    BACKALLEY_4(MapAsset.BACKALLEY_4, "Back Alley"),
    BACKALLEY_EXIT(MapAsset.BACKALLEY_EXIT, "Back Alley"),
    BACKALLEY_SECRET(MapAsset.BACKALLEY_SECRET, "Back Alley Secret");

    private static final Map<MapAsset, MapAssetName> BY_ASSET = new EnumMap<>(MapAsset.class);

    static {
        for (MapAssetName value : values()) {
            BY_ASSET.put(value.asset, value);
        }
    }

    private final MapAsset asset;
    private final String displayName;

    MapAssetName(MapAsset asset, String displayName) {
        this.asset = asset;
        this.displayName = displayName;
    }

    public MapAsset asset() {
        return asset;
    }

    public String displayName() {
        return displayName;
    }

    public static String fromAsset(MapAsset asset) {
        MapAssetName mapAssetName = BY_ASSET.get(asset);
        return mapAssetName == null ? null : mapAssetName.displayName();
    }
}
