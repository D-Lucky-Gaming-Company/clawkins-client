package github.dluckycompany.clawkins.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public enum MapAsset implements Asset<TiledMap>{
    NURSE_INTERIOR("nurse_interior.tmx"),
    NURSE_INTERIOR_1("nurse_interior_1.tmx"),
    NURSE_INTERIOR_2("nurse_interior_2.tmx"),
    NURSE_INTERIOR_3("nurse_interior_3.tmx"),
    COTTAGE("cottage_sample.tmx"),
    SHOP("shop.tmx"),
    SHOP_1("shop_1.tmx"),
    SHOP_2("shop_2.tmx"),
    SHOP_3("shop_3.tmx"),
    MOUNTAIN("mountain.tmx"),
    MOUNTAIN_1("mountain_1.tmx"),
    MOUNTAIN_2("mountain_2.tmx"),
    MOUNTAIN_3("mountain_3.tmx"),
    CAVE("cave.tmx"),
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
    BACKALLEY("backalley/backalley_1.tmx"),
    BACKALLEY_2("backalley/backalley_2.tmx"),
    BACKALLEY_3("backalley/backalley_3.tmx"),
    BACKALLEY_4("backalley/backalley_4.tmx"),
    BACKALLEY_EXIT("backalley/backalley_exit.tmx"),
    BACKALLEY_SECRET("backalley/backalley_secret.tmx");

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
    }

    MapAsset(String mapName) {
        this.mapPath = mapName;
        int slash = mapName.lastIndexOf('/');
        this.fileName = slash >= 0 ? mapName.substring(slash + 1) : mapName;

        TmxMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
        parameters.projectFilePath = "maps/test.tiled-project";
        this.descriptor = new AssetDescriptor<>("maps/" + mapPath, TiledMap.class, parameters);
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
