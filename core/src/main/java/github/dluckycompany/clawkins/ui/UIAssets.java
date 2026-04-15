package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * UIAssets - Static asset manager for InventoryUI spritesheet
 * 
 * Loads and manages TextureRegions from InventoryUI_sheet.png with pixel-perfect coordinates.
 * Each drawable is cached to avoid redundant texture lookups.
 */
public class UIAssets {
    private static boolean isInitialized = false;
    private static Texture uiSheetTexture;
    
    // Drawables for Scene2D widgets
    private static TextureRegionDrawable mainBGDrawable;
    private static TextureRegionDrawable detailBGDrawable;
    private static Object buttonPillDrawable;  // Placeholder for NinePatchDrawable
    private static TextureRegionDrawable backBtnDrawable;

    /**
     * Initialize UIAssets by loading the spritesheet and creating all drawable references.
     * Safe to call multiple times (guards with isInitialized flag).
     */
    public static void initialize() {
        if (isInitialized) {
            return;
        }

        // TODO: Load texture from asset manager when available
        // For now, create a placeholder texture that can be populated later
        // uiSheetTexture = assetManager.get("assets/ui/inventory_ui/InventoryUI_sheet.png", Texture.class);
        
        System.out.println("[UIAssets] Asset loading placeholder - implement texture loading in production");
        
        // Mark as initialized even without texture to prevent repeated initialization attempts
        isInitialized = true;
    }

    /**
     * Returns whether UIAssets has been initialized.
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Get the main background drawable for the item list column (MainBG).
     * Used to set background on the right-side item list table.
     */
    public static TextureRegionDrawable getMainBGDrawable() {
        // Return null for now since texture loading is not yet implemented
        // When texture is loaded, this will return: new TextureRegionDrawable(mainBGRegion)
        return mainBGDrawable;
    }

    /**
     * Get the detail background drawable for the item details column (DetailBG).
     * Used to set background on the left-side detail table.
     */
    public static TextureRegionDrawable getDetailBGDrawable() {
        // Return null for now since texture loading is not yet implemented
        return detailBGDrawable;
    }

    /**
     * Get the button pill drawable for buttons.
     * Uses nine-patch scaling to support variable-size buttons.
     */
    public static Object getButtonPillDrawable() {
        // Return null for now since texture loading is not yet implemented
        return buttonPillDrawable;
    }

    /**
     * Get the back button drawable.
     */
    public static TextureRegionDrawable getBackBtnDrawable() {
        // Return null for now since texture loading is not yet implemented
        return backBtnDrawable;
    }

    /**
     * Get debug info about loaded assets.
     */
    public static String getDebugInfo() {
        return "UIAssets [initialized=" + isInitialized + 
               ", mainBG=" + (mainBGDrawable != null ? "loaded" : "null") +
               ", detailBG=" + (detailBGDrawable != null ? "loaded" : "null") + "]";
    }

    /**
     * Dispose all loaded assets when no longer needed.
     */
    public static void dispose() {
        if (uiSheetTexture != null) {
            uiSheetTexture.dispose();
        }
        isInitialized = false;
    }
}
