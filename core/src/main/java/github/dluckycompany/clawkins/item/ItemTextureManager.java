package github.dluckycompany.clawkins.item;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Manages dynamic loading and caching of item textures from the assets/items directory.
 * Prevents texture leaks and frame drops by reusing loaded textures.
 * 
 * Implements a singleton pattern to ensure a single cache across the application.
 */
public class ItemTextureManager {
    private static ItemTextureManager instance;
    
    private final Map<String, TextureRegion> textureCache;
    private TextureRegion fallbackTexture;
    
    // Base directory for item assets
    private static final String ITEMS_DIRECTORY = "items/";
    private static final String TEXTURE_EXTENSION = ".png";
    private static final String FALLBACK_IMAGE_NAME = "missing_image";
    
    private ItemTextureManager() {
        this.textureCache = new HashMap<>();
        this.fallbackTexture = null;
    }
    
    /**
     * Get the singleton instance of ItemTextureManager
     */
    public static ItemTextureManager getInstance() {
        if (instance == null) {
            instance = new ItemTextureManager();
        }
        return instance;
    }
    
    /**
     * Load or retrieve a cached TextureRegion for the given image name
     * 
     * @param imageName The filename without extension (e.g., "05_apple_pie")
     * @return TextureRegion for the item, or fallback texture if not found
     */
    public TextureRegion getItemTexture(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) {
            Gdx.app.error("ItemTextureManager", "Image name is null or empty, returning fallback");
            return getFallbackTexture();
        }
        
        // Check cache first
        if (textureCache.containsKey(imageName)) {
            return textureCache.get(imageName);
        }
        
        // Construct full path: items/imageName.png
        String filePath = ITEMS_DIRECTORY + imageName + TEXTURE_EXTENSION;
        
        try {
            // Check if file exists
            if (!Gdx.files.internal(filePath).exists()) {
                Gdx.app.error("ItemTextureManager", 
                    "Image file not found: " + filePath + " - using fallback");
                return getFallbackTexture();
            }
            
            // Load texture
            Texture texture = new Texture(Gdx.files.internal(filePath));
            TextureRegion region = new TextureRegion(texture);
            
            // Cache it
            textureCache.put(imageName, region);
            
            Gdx.app.log("ItemTextureManager", "Loaded item texture: " + filePath);
            return region;
            
        } catch (Exception e) {
            Gdx.app.error("ItemTextureManager", 
                "Failed to load texture: " + filePath + " - " + e.getMessage());
            return getFallbackTexture();
        }
    }
    
    /**
     * Get the fallback texture (used when an image is missing)
     * Creates a simple placeholder if not already loaded
     */
    private TextureRegion getFallbackTexture() {
        if (fallbackTexture == null) {
            try {
                String fallbackPath = ITEMS_DIRECTORY + FALLBACK_IMAGE_NAME + TEXTURE_EXTENSION;
                
                if (Gdx.files.internal(fallbackPath).exists()) {
                    Texture fallbackTex = new Texture(Gdx.files.internal(fallbackPath));
                    fallbackTexture = new TextureRegion(fallbackTex);
                    Gdx.app.log("ItemTextureManager", "Loaded fallback texture: " + fallbackPath);
                } else {
                    // Create a 64x64 white placeholder texture
                    Texture placeholderTex = new Texture(64, 64, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                    com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(64, 64, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                    pixmap.setColor(0.8f, 0.8f, 0.8f, 1f);  // Light gray
                    pixmap.fill();
                    placeholderTex.draw(pixmap, 0, 0);
                    pixmap.dispose();
                    
                    fallbackTexture = new TextureRegion(placeholderTex);
                    Gdx.app.log("ItemTextureManager", "Created fallback placeholder texture (64x64 gray)");
                }
            } catch (Exception e) {
                Gdx.app.error("ItemTextureManager", "Failed to load fallback texture: " + e.getMessage());
            }
        }
        return fallbackTexture;
    }
    
    /**
     * Preload multiple item textures to avoid stutter during gameplay
     * 
     * @param items Array of items to preload
     */
    public void preloadTextures(Item[] items) {
        if (items == null) return;
        
        Gdx.app.log("ItemTextureManager", "Preloading " + items.length + " item textures...");
        
        int loaded = 0;
        for (Item item : items) {
            if (item != null && item.getImageName() != null) {
                getItemTexture(item.getImageName());
                loaded++;
            }
        }
        
        Gdx.app.log("ItemTextureManager", "Preloaded " + loaded + " textures");
    }
    
    /**
     * Clear the texture cache and dispose all loaded textures
     * Should be called when disposing the application or when memory is needed
     */
    public void dispose() {
        Gdx.app.log("ItemTextureManager", "Disposing all cached textures (" + textureCache.size() + " textures)");
        
        for (TextureRegion region : textureCache.values()) {
            if (region != null && region.getTexture() != null) {
                region.getTexture().dispose();
            }
        }
        
        textureCache.clear();
        
        if (fallbackTexture != null && fallbackTexture.getTexture() != null) {
            fallbackTexture.getTexture().dispose();
            fallbackTexture = null;
        }
        
        Gdx.app.log("ItemTextureManager", "Texture disposal complete");
    }
    
    /**
     * Get the current cache size (for debugging)
     */
    public int getCacheSize() {
        return textureCache.size();
    }
    
    /**
     * Check if a texture is cached
     */
    public boolean isCached(String imageName) {
        return textureCache.containsKey(imageName);
    }
}
