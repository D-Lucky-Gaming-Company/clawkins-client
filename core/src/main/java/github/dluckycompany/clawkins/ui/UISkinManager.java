package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

/**
 * SKIN INITIALIZATION & FONT REGISTRATION UTILITY
 * 
 * Manages proper initialization of LibGDX Skin resources to prevent font crashes.
 * All fonts must be registered BEFORE Dialog instantiation.
 * 
 * Usage:
 *   Skin skin = new Skin();
 *   UISkinManager.initializeSkin(skin, bitmapFont);
 *   // ... now safe to create dialogs
 */
public class UISkinManager {
    
    // ============================================================
    // CUSTOM COLOR PALETTE (HEX VALUES)
    // ============================================================
    private static final Color DIALOG_BG = Color.valueOf("#E7DAC7");        // Warm Beige / Antique Paper
    private static final Color BORDER_COLOR = Color.valueOf("#1F1A13");     // Dark Coffee / Deep Brown
    private static final Color HIGHLIGHT_COLOR = Color.valueOf("#C19253");  // Warm Tan / Wood
    private static final Color TEXT_PRIMARY = Color.valueOf("#1F1A13");     // Dark Coffee / Deep Brown
    private static final Color BUTTON_BG = Color.valueOf("#C19253");        // Tan (button background)
    private static final Color BUTTON_TEXT = Color.valueOf("#1F1A13");      // Dark Brown (button text)
    
    /**
     * CRITICAL: Initialize Skin with all required fonts and styles.
     * MUST be called before any Dialog creation.
     * 
     * This method:
     * 1. Registers the default BitmapFont
     * 2. Creates and registers all required styles (Window, Label, TextButton)
     * 3. Ensures all font references are safe
     * 
     * @param skin The Skin instance to initialize
     * @param font The BitmapFont to register (usually the game's default font)
     */
    public static void initializeSkin(Skin skin, BitmapFont font) {
        if (skin == null || font == null) {
            Gdx.app.error("UISkinManager", "FATAL: Skin or font is null during initialization");
            return;
        }
        
        // ============================================================
        // STEP 1: Register the default font
        // ============================================================
        // This MUST be done first before creating any styles that reference it
        skin.add("default", font);
        Gdx.app.log("UISkinManager", "Registered font: 'default'");
        
        // ============================================================
        // STEP 2: Create and register Label.LabelStyle
        // ============================================================
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = TEXT_PRIMARY;
        skin.add("default", labelStyle, Label.LabelStyle.class);
        Gdx.app.log("UISkinManager", "Registered LabelStyle: 'default'");
        
        // ============================================================
        // STEP 3: Create and register TextButton.TextButtonStyle
        // ============================================================
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = BUTTON_TEXT;
        buttonStyle.up = new ColorDrawable(BUTTON_BG);           // Normal state
        buttonStyle.down = new ColorDrawable(BORDER_COLOR);      // Pressed state
        buttonStyle.over = new ColorDrawable(HIGHLIGHT_COLOR);   // Hover state
        skin.add("default", buttonStyle, TextButton.TextButtonStyle.class);
        Gdx.app.log("UISkinManager", "Registered TextButtonStyle: 'default'");
        
        // ============================================================
        // STEP 4: Create and register Window.WindowStyle
        // ============================================================
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;          // CRITICAL: Title font must match
        windowStyle.titleFontColor = TEXT_PRIMARY;
        windowStyle.background = new ColorDrawable(DIALOG_BG);   // Window background
        skin.add("default", windowStyle, Window.WindowStyle.class);
        Gdx.app.log("UISkinManager", "Registered WindowStyle: 'default'");
        
        Gdx.app.log("UISkinManager", "✓ SKIN INITIALIZATION COMPLETE - All fonts and styles registered");
    }
    
    /**
     * SAFE FONT ACCESS
     * 
     * Retrieves a font from the Skin with fallback protection.
     * Returns a guaranteed non-null BitmapFont (fallback if needed).
     * 
     * @param skin The Skin instance
     * @param fontKey The key to look up (e.g., "default")
     * @return BitmapFont (guaranteed non-null)
     */
    public static BitmapFont getSafeFont(Skin skin, String fontKey) {
        if (skin == null) {
            Gdx.app.error("UISkinManager", "Cannot get font: Skin is null");
            return createFallbackFont();
        }
        
        try {
            BitmapFont font = skin.getFont(fontKey);
            if (font != null) {
                return font;
            }
        } catch (Exception e) {
            Gdx.app.error("UISkinManager", "Font not found in Skin: " + fontKey + " - " + e.getMessage());
        }
        
        Gdx.app.log("UISkinManager", "Using fallback font for key: " + fontKey);
        return createFallbackFont();
    }
    
    /**
     * Create a temporary fallback font if the registered font is missing.
     * This prevents crashes during development/testing.
     */
    private static BitmapFont createFallbackFont() {
        Gdx.app.log("UISkinManager", "Creating fallback BitmapFont...");
        BitmapFont fallback = new BitmapFont();
        Gdx.app.log("UISkinManager", "WARNING: Using fallback font - this should only happen during development");
        return fallback;
    }
    
    /**
     * Dispose all Skin resources.
     * MUST be called in Screen.dispose() to prevent memory leaks.
     * 
     * @param skin The Skin to dispose
     */
    public static void disposeSkin(Skin skin) {
        if (skin != null) {
            try {
                // Dispose all fonts registered in the Skin
                for (String fontName : skin.getAll(BitmapFont.class).keys()) {
                    BitmapFont font = skin.getFont(fontName);
                    if (font != null) {
                        font.dispose();
                    }
                }
                
                // Dispose the Skin itself
                skin.dispose();
                Gdx.app.log("UISkinManager", "✓ Skin disposed successfully");
            } catch (Exception e) {
                Gdx.app.error("UISkinManager", "Error disposing Skin: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verify that a Skin is properly initialized with required fonts.
     * Useful for debugging font registration issues.
     * 
     * @param skin The Skin to verify
     * @return true if all required fonts are present
     */
    public static boolean verifySkinFonts(Skin skin) {
        if (skin == null) {
            Gdx.app.error("UISkinManager", "✗ Skin verification failed: Skin is null");
            return false;
        }
        
        boolean hasDefault = false;
        try {
            BitmapFont defaultFont = skin.getFont("default");
            if (defaultFont != null) {
                hasDefault = true;
                Gdx.app.log("UISkinManager", "✓ Font 'default' verified");
            }
        } catch (Exception e) {
            Gdx.app.error("UISkinManager", "✗ Font 'default' not found");
        }
        
        if (hasDefault) {
            Gdx.app.log("UISkinManager", "✓ SKIN VERIFICATION PASSED");
            return true;
        } else {
            Gdx.app.error("UISkinManager", "✗ SKIN VERIFICATION FAILED - Missing required fonts");
            return false;
        }
    }
}
