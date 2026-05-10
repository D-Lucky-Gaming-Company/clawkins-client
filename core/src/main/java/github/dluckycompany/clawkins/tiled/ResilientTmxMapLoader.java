package github.dluckycompany.clawkins.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads TMX maps like {@link TmxMapLoader}, but when an external {@code .tsx} file is missing,
 * registers a temporary solid-red tileset for that {@code firstgid} range so the map still loads.
 */
public class ResilientTmxMapLoader extends TmxMapLoader {

    private static final String TAG = ResilientTmxMapLoader.class.getSimpleName();

    /** Lower 29 bits hold the tile gid; high bits are flip flags (Tiled / LibGDX convention). */
    private static final int TILED_GID_MASK = 0x1FFFFFFF;

    private static final Pattern CSV_DATA_BLOCK =
            Pattern.compile("<data[^>]*encoding=\"csv\"[^>]*>(.*?)</data>", Pattern.DOTALL);
    private static final Pattern GID_ATTRIBUTE = Pattern.compile("gid=\"(\\d+)\"");

    private static final int MAX_LAST_TILESET_SPAN = 8192;

    private final Map<String, TilesetPlaceholderContext> contexts = new ConcurrentHashMap<>();

    public ResilientTmxMapLoader(com.badlogic.gdx.assets.loaders.FileHandleResolver resolver) {
        super(resolver);
        MissingTileTexture.prepareFallback();
    }

    /**
     * AssetManager resolves textures before {@link #loadTiledMap}; the default loader parses every external {@code .tsx}
     * and crashes when the file is absent. Skip dependency discovery for missing TSX so loading can use placeholders.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Array getTileSetDependencyFileHandle(Array fileHandles, FileHandle tmxFile, Element tileset) {
        String source = tileset.getAttribute("source", null);
        if (source != null) {
            FileHandle tsxFile = getRelativeFileHandle(tmxFile, source);
            if (!tsxFile.exists()) {
                Gdx.app.error(TAG, "[missing tileset deps] skipping texture dependencies for absent TSX");
                Gdx.app.error(TAG, "[missing tileset deps] map file: " + tmxFile.path());
                Gdx.app.error(TAG, "[missing tileset deps] TMX source=: " + source);
                Gdx.app.error(TAG, "[missing tileset deps] resolved path (missing): " + tsxFile.path());
                return fileHandles;
            }
        }
        return super.getTileSetDependencyFileHandle(fileHandles, tmxFile, tileset);
    }

    @Override
    protected TiledMap loadTiledMap(FileHandle tmxFile, Parameters parameter, ImageResolver imageResolver) {
        TilesetPlaceholderContext ctx = TilesetPlaceholderContext.parse(tmxFile);
        contexts.put(tmxFile.path(), ctx);
        return super.loadTiledMap(tmxFile, parameter, imageResolver);
    }

    @Override
    public TiledMap loadSync(com.badlogic.gdx.assets.AssetManager manager,
            String fileName,
            FileHandle file,
            Parameters parameter) {
        TiledMap map = super.loadSync(manager, fileName, file, parameter);
        TilesetPlaceholderContext ctx = contexts.remove(file.path());
        if (ctx != null) {
            ctx.resolveDeferredRegions();
        }
        return map;
    }

    @Override
    protected void loadTileSet(Element element, FileHandle tmxFile, ImageResolver imageResolver) {
        if (!element.getName().equals("tileset")) {
            return;
        }

        int firstgid = element.getIntAttribute("firstgid", 1);
        String source = element.getAttribute("source", null);

        if (source != null) {
            FileHandle tsx = getRelativeFileHandle(tmxFile, source);
            if (!tsx.exists()) {
                TilesetPlaceholderContext ctx = contexts.get(tmxFile.path());
                int span =
                        ctx != null
                                ? ctx.tileSpanForTilesetIndex(ctx.indexOfFirstGid(firstgid))
                                : 1;
                int tw = mapTileWidth > 0 ? mapTileWidth : 16;
                int th = mapTileHeight > 0 ? mapTileHeight : 16;
                int lastGid = firstgid + Math.max(1, span) - 1;

                Gdx.app.error(TAG, "[missing tileset] map file: " + tmxFile.path());
                Gdx.app.error(TAG, "[missing tileset] TMX reference (source=): " + source);
                Gdx.app.error(TAG, "[missing tileset] resolved path (file missing): " + tsx.path());
                Gdx.app.error(
                        TAG,
                        "[missing tileset] placeholder tileset: firstgid=" + firstgid
                                + ", tileCount=" + span
                                + ", global gid range [" + firstgid + ".." + lastGid + "]"
                                + ", placeholder tile size " + tw + "x" + th
                                + " (solid red)");

                TiledMapTileSet tileSet = new TiledMapTileSet();
                tileSet.setName("(missing asset) " + source);
                tileSet.getProperties().put("firstgid", firstgid);

                TextureRegion region = ctx != null
                    ? ctx.requestDeferredRegion(tw, th)
                    : MissingTileTexture.fallbackRegion();
                int count = Math.max(1, span);
                for (int i = 0; i < count; i++) {
                    int gid = firstgid + i;
                    StaticTiledMapTile tile = new StaticTiledMapTile(region);
                    tile.setId(gid);
                    tileSet.putTile(gid, tile);
                }

                map.getTileSets().addTileSet(tileSet);
                return;
            }
        }

        super.loadTileSet(element, tmxFile, imageResolver);
    }

    private static final class TilesetPlaceholderContext {
        private final int[] firstGids;
        private final int maxReferencedGid;
        private final Array<DeferredRegion> deferredRegions = new Array<>();

        private TilesetPlaceholderContext(int[] firstGids, int maxReferencedGid) {
            this.firstGids = firstGids;
            this.maxReferencedGid = maxReferencedGid;
        }

        static TilesetPlaceholderContext parse(FileHandle tmxFile) {
            String raw = tmxFile.readString();
            Element mapRoot = new XmlReader().parse(raw);
            Array<Element> tilesetEls = mapRoot.getChildrenByName("tileset");
            int n = tilesetEls.size;
            int[] firstGids = new int[n];
            for (int i = 0; i < n; i++) {
                firstGids[i] = tilesetEls.get(i).getIntAttribute("firstgid", 1);
            }
            return new TilesetPlaceholderContext(firstGids, maxGidFromMap(raw));
        }

        int indexOfFirstGid(int firstGid) {
            for (int i = 0; i < firstGids.length; i++) {
                if (firstGids[i] == firstGid) {
                    return i;
                }
            }
            return 0;
        }

        int tileSpanForTilesetIndex(int tilesetIndex) {
            if (firstGids.length == 0) {
                return 1;
            }
            if (tilesetIndex + 1 < firstGids.length) {
                return Math.max(1, firstGids[tilesetIndex + 1] - firstGids[tilesetIndex]);
            }
            int first = firstGids[tilesetIndex];
            int span = maxReferencedGid >= first ? (maxReferencedGid - first + 1) : 1;
            return Math.min(MAX_LAST_TILESET_SPAN, Math.max(1, span));
        }

        TextureRegion requestDeferredRegion(int tileWidth, int tileHeight) {
            TextureRegion region = new TextureRegion();
            MissingTileTexture.applyFallback(region);
            deferredRegions.add(new DeferredRegion(tileWidth, tileHeight, region));
            return region;
        }

        void resolveDeferredRegions() {
            for (DeferredRegion deferred : deferredRegions) {
                MissingTileTexture.applyToRegion(deferred.region, deferred.tileWidth, deferred.tileHeight);
            }
            deferredRegions.clear();
        }
    }

    private static final class DeferredRegion {
        private final int tileWidth;
        private final int tileHeight;
        private final TextureRegion region;

        private DeferredRegion(int tileWidth, int tileHeight, TextureRegion region) {
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.region = region;
        }
    }

    private static int maxGidFromMap(String mapXml) {
        int max = 0;
        Matcher block = CSV_DATA_BLOCK.matcher(mapXml);
        while (block.find()) {
            String csv = block.group(1);
            for (String token : csv.split("[,\\s]+")) {
                if (token.isEmpty()) {
                    continue;
                }
                try {
                    long v = Long.parseLong(token.trim());
                    int gid = (int) (v & TILED_GID_MASK);
                    if (gid > max) {
                        max = gid;
                    }
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
        }
        Matcher gidMatcher = GID_ATTRIBUTE.matcher(mapXml);
        while (gidMatcher.find()) {
            try {
                long v = Long.parseLong(gidMatcher.group(1));
                int gid = (int) (v & TILED_GID_MASK);
                if (gid > max) {
                    max = gid;
                }
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return max;
    }

    /** One shared texture per tile size (bright red rectangle). */
    private static final class MissingTileTexture {
        private static final int FALLBACK_SIZE = 64;
        private static Texture fallbackTexture;
        private static final Map<Long, Texture> BY_SIZE = new HashMap<>();

        private MissingTileTexture() {
        }

        static void prepareFallback() {
            if (fallbackTexture != null) {
                return;
            }
            Pixmap pm = new Pixmap(FALLBACK_SIZE, FALLBACK_SIZE, Pixmap.Format.RGBA8888);
            pm.setColor(1f, 0f, 0f, 1f);
            pm.fill();
            pm.setColor(0.35f, 0f, 0f, 1f);
            for (int i = 0; i < FALLBACK_SIZE; i++) {
                pm.drawPixel(i, 0);
                pm.drawPixel(i, FALLBACK_SIZE - 1);
            }
            for (int j = 0; j < FALLBACK_SIZE; j++) {
                pm.drawPixel(0, j);
                pm.drawPixel(FALLBACK_SIZE - 1, j);
            }
            fallbackTexture = new Texture(pm);
            pm.dispose();
            fallbackTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }

        static TextureRegion fallbackRegion() {
            prepareFallback();
            return new TextureRegion(fallbackTexture);
        }

        static TextureRegion regionForTileSize(int tileWidth, int tileHeight) {
            int w = Math.max(1, tileWidth);
            int h = Math.max(1, tileHeight);
            long key = ((long) w << 32) | (h & 0xffffffffL);
            Texture tex = BY_SIZE.get(key);
            if (tex == null) {
                Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
                pm.setColor(1f, 0f, 0f, 1f);
                pm.fill();
                pm.setColor(0.35f, 0f, 0f, 1f);
                for (int i = 0; i < w; i++) {
                    pm.drawPixel(i, 0);
                    pm.drawPixel(i, h - 1);
                }
                for (int j = 0; j < h; j++) {
                    pm.drawPixel(0, j);
                    pm.drawPixel(w - 1, j);
                }
                tex = new Texture(pm);
                pm.dispose();
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                BY_SIZE.put(key, tex);
            }
            return new TextureRegion(tex);
        }

        static void applyToRegion(TextureRegion region, int tileWidth, int tileHeight) {
            int w = Math.max(1, tileWidth);
            int h = Math.max(1, tileHeight);
            TextureRegion prepared = regionForTileSize(w, h);
            region.setTexture(prepared.getTexture());
            region.setRegion(0, 0, w, h);
        }

        static void applyFallback(TextureRegion region) {
            prepareFallback();
            region.setTexture(fallbackTexture);
            region.setRegion(0, 0, FALLBACK_SIZE, FALLBACK_SIZE);
        }
    }
}
