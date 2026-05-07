package github.dluckycompany.clawkins.asset;

import java.util.EnumMap;
import java.util.Map;

public enum MapAssetName {
    NURSE_INTERIOR(MapAsset.NURSE_INTERIOR, "Nursery"),
    NURSE_INTERIOR_1(MapAsset.NURSE_INTERIOR_1, "Nursery"),
    NURSE_INTERIOR_2(MapAsset.NURSE_INTERIOR_2, "Nursery"),
    NURSE_INTERIOR_3(MapAsset.NURSE_INTERIOR_3, "Nursery"),
    COTTAGE(MapAsset.COTTAGE, "Cottage"),
    SHOP(MapAsset.SHOP, "Shop"),
    SHOP_1(MapAsset.SHOP_1, "Shop"),
    SHOP_2(MapAsset.SHOP_2, "Shop"),
    SHOP_3(MapAsset.SHOP_3, "Shop"),
    MOUNTAIN(MapAsset.MOUNTAIN, "Mountain"),
    MOUNTAIN_1(MapAsset.MOUNTAIN_1, "Mountain"),
    MOUNTAIN_2(MapAsset.MOUNTAIN_2, "Mountain"),
    MOUNTAIN_3(MapAsset.MOUNTAIN_3, "Mountain"),
    CAVE(MapAsset.CAVE, "Cave"),
    CAVE_1(MapAsset.CAVE_1, "Cave"),
    CAVE_2(MapAsset.CAVE_2, "Cave"),
    CAVE_3(MapAsset.CAVE_3, "Cave"),
    FIELD(MapAsset.FIELD, "Field"),
    FIELD_2(MapAsset.FIELD_2, "Field"),
    FIELD_3(MapAsset.FIELD_3, "Field"),
    FIELD_4(MapAsset.FIELD_4, "Field"),
    FIELD_5(MapAsset.FIELD_5, "Field"),
    FIELD_SECRET(MapAsset.FIELD_SECRET, "Field Secret"),
    MANSION_MAZE(MapAsset.MANSION_MAZE, "Mansion", "Maze"),
    MANSION_GARDEN(MapAsset.MANSION_GARDEN, "Mansion", "Garden"),
    MANSION_EXIT(MapAsset.MANSION_EXIT, "Mansion", "Exit"),
    BACKALLEY(MapAsset.BACKALLEY, "Back Alley"),
    BACKALLEY_2(MapAsset.BACKALLEY_2, "Back Alley"),
    BACKALLEY_3(MapAsset.BACKALLEY_3, "Back Alley"),
    BACKALLEY_4(MapAsset.BACKALLEY_4, "Back Alley"),
    BACKALLEY_EXIT(MapAsset.BACKALLEY_EXIT, "Back Alley"),
    BACKALLEY_SECRET(MapAsset.BACKALLEY_SECRET, "Back Alley", "Secret");

    private static final Map<MapAsset, MapAssetName> BY_ASSET = new EnumMap<>(MapAsset.class);

    static {
        for (MapAssetName value : values()) {
            BY_ASSET.put(value.asset, value);
        }
    }

    private final MapAsset asset;
    private final String areaName;
    private final String variationName;

    MapAssetName(MapAsset asset, String areaName) {
        this(asset, areaName, null);
    }

    MapAssetName(MapAsset asset, String areaName, String variationName) {
        this.asset = asset;
        this.areaName = areaName;
        this.variationName = variationName;
    }

    public MapAsset asset() {
        return asset;
    }

    public String areaName() {
        return areaName;
    }

    public String variationName() {
        return variationName;
    }

    public String displayName() {
        return variationName == null || variationName.isBlank()
            ? areaName
            : areaName + " " + variationName;
    }

    public static String fromAsset(MapAsset asset) {
        MapAssetName mapAssetName = BY_ASSET.get(asset);
        return mapAssetName == null ? null : mapAssetName.displayName();
    }

    public static MapAssetName fromAssetEntry(MapAsset asset) {
        return BY_ASSET.get(asset);
    }
}
