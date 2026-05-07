package github.dluckycompany.clawkins.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum MapAsset implements Asset<TiledMap>{
    NURSE_INTERIOR("nurse_interior.tmx"),
    NURSE_INTERIOR_2("nurse_interior 2.tmx"),
    NURSE_INTERIOR_3("nurse_interior 3.tmx"),
    NURSE_INTERIOR_4("nurse_interior 4.tmx"),
    COTTAGE_SAMPLE("cottage_sample.tmx"),
    SHOP_INTERIOR("shop_interior.tmx"),
    SHOP_INTERIOR_2("shop_interior 2.tmx"),
    SHOP_INTERIOR_3("shop_interior 3.tmx"),
    MOUNTAIN_1("mountain_1.tmx"),
    MOUNTAIN_2("mountain_2.tmx"),
    MOUNTAIN_3("mountain_3.tmx"),
    MOUNTAIN_4("mountain_4.tmx"),
    MOUNTAIN_5("mountain_5.tmx"),
    CAVE_ENTRANCE("cave_entrance.tmx"),
    CAVE_1("cave_1.tmx"),
    CAVE_2("cave_2.tmx"),
    CAVE_3("cave_3.tmx"),
    FIELD("field.tmx"),
    FIELD_2("field_2.tmx"),
    FIELD_3("field_3.tmx"),
    FIELD_4("field_4.tmx"),
    FIELD_5("field_5.tmx"),
    FIELD_SECRET("field_secret.tmx"),
    MANSION_MAZE("mansion_maze.tmx"),
    MANSION_GARDEN("mansion_garden.tmx"),
    MANSION_EXIT("custom tilesets/mansion_exit.tmx"),
    BACKALLEY_1("backalley/backalley_1.tmx"),
    BACKALLEY_2("backalley/backalley_2.tmx"),
    BACKALLEY_3("backalley/backalley_3.tmx"),
    BACKALLEY_4("backalley/backalley_4.tmx"),
    BACKALLEY_EXIT("backalley/backalley_exit.tmx"),
    BACKALLEY_SECRET("backalley/backalley_secret.tmx"),
    TEST_WORLD("test_world.tmx");

    private final AssetDescriptor<TiledMap> descriptor;
    private final String mapPath;
    private final String fileName;
    private static final Map<String, MapAsset> LOOKUP = new HashMap<>();

    static {
        for (MapAsset asset : values()) {
            LOOKUP.put(normalize(asset.name()), asset);
            LOOKUP.put(normalize(asset.mapPath), asset);
            LOOKUP.put(normalize(asset.fileName), asset);

            String withoutExt = asset.fileName.endsWith(".tmx")
                ? asset.fileName.substring(0, asset.fileName.length() - 4)
                : asset.fileName;
            LOOKUP.put(normalize(withoutExt), asset);
        }

        // Legacy aliases retained so existing saves/transitions still resolve.
        LOOKUP.put(normalize("nurse_interior_1"), NURSE_INTERIOR);
        LOOKUP.put(normalize("shop"), SHOP_INTERIOR);
        LOOKUP.put(normalize("shop_1"), SHOP_INTERIOR);
        LOOKUP.put(normalize("shop_2"), SHOP_INTERIOR_2);
        LOOKUP.put(normalize("shop_3"), SHOP_INTERIOR_3);
        LOOKUP.put(normalize("mountain"), MOUNTAIN_1);
        LOOKUP.put(normalize("cave"), CAVE_ENTRANCE);
        LOOKUP.put(normalize("cottage"), COTTAGE_SAMPLE);
        LOOKUP.put(normalize("backalley"), BACKALLEY_1);
    }

    MapAsset(String mapName) {
        String normalizedMapPath = mapName;
        if (mapName.startsWith("mountain_") || mapName.startsWith("cave_")) {
            normalizedMapPath = "mountain + cave/" + mapName;
        }
        this.mapPath = normalizedMapPath;
        int slash = this.mapPath.lastIndexOf('/');
        this.fileName = slash >= 0 ? this.mapPath.substring(slash + 1) : this.mapPath;

        TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
        parameters.projectFilePath = "maps/test.tiled-project";
        this.descriptor = new AssetDescriptor<>("maps/" + this.mapPath, TiledMap.class, parameters);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor() {
        return this.descriptor;
    }

    /**
     * Resolves map keys from enum names, world file names, or TMX paths.
     * Accepted examples: FIELD_2, field_2, field_2.tmx, custom tilesets/mansion_exit.tmx.
     */
    public static MapAsset fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return LOOKUP.get(normalize(key));
    }

    private static String normalize(String value) {
        return value.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
    }

}
