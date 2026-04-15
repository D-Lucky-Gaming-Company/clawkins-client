package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

/**
 * ROUNDED PANEL UTILITY FOR LIBRARIAN-GRADE UI
 * 
 * Creates high-fidelity rounded corner drawables for LibGDX Scene2D.
 * Generates NinePatch textures programmatically with mathematically correct split values.
 * 
 * CRITICAL NinePatch Mathematics:
 * ================================
 * For a 12px radius rounded corner with proper scaling:
 * - Source texture size: (cornerRadius * 2 + 2) × (cornerRadius * 2 + 2) = 26×26
 * - Left split: 12px (protects left rounded corner)
 * - Right split: 12px (protects right rounded corner)
 * - Top split: 12px (protects top rounded corner)
 * - Bottom split: 12px (protects bottom rounded corner)
 * - Center stretchable area: 2×2px (wide enough to stretch, preserves corners)
 * 
 * This ensures LibGDX only stretches the 2px center, leaving the 12px corners untouched.
 * 
 * Standards: CIT-U Professional UI Design
 * - 12px radius rounded corners (industry standard for RPG UIs)
 * - Correct NinePatch splits to prevent distortion
 * - Clean edge rendering without scaling artifacts
 * - Color theme integration (#E7DAC7, #C19253, #1F1A13)
 */
public class RoundedPanelDrawable {

    /**
     * Create a NinePatchDrawable with rounded corners (12px radius)
     * Uses a 26×26px texture with proper NinePatch splits to prevent distortion
     * 
     * CRITICAL: The texture is designed as follows:
     * - Left 12px: Rounded corner (PROTECTED from horizontal stretch)
     * - Center 2px: Stretchable area
     * - Right 12px: Rounded corner (PROTECTED from horizontal stretch)
     * - Top 12px: Rounded corner (PROTECTED from vertical stretch)
     * - Center 2px: Stretchable area
     * - Bottom 12px: Rounded corner (PROTECTED from vertical stretch)
     * 
     * @param color The fill color for the panel (e.g., Color.valueOf("#E7DAC7"))
     * @param cornerRadius The radius of rounded corners in pixels (typically 12)
     * @return A NinePatchDrawable with mathematically correct splits
     */
    public static NinePatchDrawable createRoundedPanel(Color color, int cornerRadius) {
        // Create a texture specifically sized for NinePatch
        // Size: (cornerRadius * 2 + 2) to have cornerRadius on each side + 2px stretchable middle
        int textureSize = cornerRadius * 2 + 2;  // 26 for 12px corners
        Pixmap pixmap = createNinePatchPixmap(textureSize, textureSize, color, cornerRadius);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();  // Dispose pixmap after texture creation
        
        // Create TextureRegion and NinePatch with CORRECT split values
        TextureRegion region = new TextureRegion(texture);
        
        // Split values: ONLY the corners are protected, center stretches freely
        // This prevents distortion of the rounded corners and stroke
        NinePatch ninePatch = new NinePatch(region, cornerRadius, cornerRadius, cornerRadius, cornerRadius);
        
        return new NinePatchDrawable(ninePatch);
    }

    /**
     * Create a Pixmap optimized for NinePatch usage
     * Size is exactly (cornerRadius * 2 + 2) × (cornerRadius * 2 + 2) to ensure
     * the stretchable area is exactly 2 pixels wide/tall.
     * 
     * Layout (for 12px radius):
     * - Left 12px: Rounded corner with fill color
     * - Center 2px: Solid fill color (stretchable)
     * - Right 12px: Rounded corner with fill color
     * 
     * @param size Total pixel size (e.g., 26 for 12px corners)
     * @param color Fill color
     * @param cornerRadius Corner radius in pixels
     * @return A small Pixmap optimized for NinePatch stretching
     */
    private static Pixmap createNinePatchPixmap(int size, int size2, Color color, int cornerRadius) {
        Pixmap pixmap = new Pixmap(size, size2, Pixmap.Format.RGBA8888);
        
        // Set background to transparent
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        
        // Set color for drawing
        pixmap.setColor(color);
        
        // Draw the main rectangle (filled center)
        // This fills: cornerRadius to (size - cornerRadius) with color
        pixmap.fillRectangle(cornerRadius, 0, size - 2 * cornerRadius, size2);
        pixmap.fillRectangle(0, cornerRadius, size, size2 - 2 * cornerRadius);
        
        // Draw rounded corners (circles at each corner)
        drawCorner(pixmap, cornerRadius, cornerRadius, cornerRadius, color);  // Top-left
        drawCorner(pixmap, size - cornerRadius - 1, cornerRadius, cornerRadius, color);  // Top-right
        drawCorner(pixmap, cornerRadius, size2 - cornerRadius - 1, cornerRadius, color);  // Bottom-left
        drawCorner(pixmap, size - cornerRadius - 1, size2 - cornerRadius - 1, cornerRadius, color);  // Bottom-right
        
        return pixmap;
    }

    /**
     * Draw a filled circle at a corner position
     * Used to create smooth rounded corners
     */
    private static void drawCorner(Pixmap pixmap, int centerX, int centerY, int radius, Color color) {
        pixmap.setColor(color);
        
        // Bresenham's circle algorithm for smooth corners
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius) {
                    int px = centerX + x;
                    int py = centerY + y;
                    if (px >= 0 && px < pixmap.getWidth() && py >= 0 && py < pixmap.getHeight()) {
                        pixmap.drawPixel(px, py, Color.rgba8888(color));
                    }
                }
            }
        }
    }

    /**
     * Create a rounded panel with a black stroke outline
     * Combines rounded corners with a 2px black border
     * Uses correct NinePatch splits to prevent distortion
     * 
     * @param color The fill color for the panel (e.g., Color.valueOf("#E7DAC7"))
     * @param cornerRadius The radius of rounded corners in pixels (typically 12)
     * @param strokeWidth The width of the black stroke in pixels (typically 2)
     * @return A NinePatchDrawable with rounded corners and black stroke
     */
    public static NinePatchDrawable createRoundedPanelWithStroke(Color color, int cornerRadius, int strokeWidth) {
        // Create a texture specifically sized for NinePatch with stroke
        // Size accounts for stroke width too
        int textureSize = cornerRadius * 2 + 2 + (strokeWidth * 2);  // 30 for 12px corners + 2px stroke
        Pixmap pixmap = createNinePatchStrokedPixmap(textureSize, textureSize, color, cornerRadius, strokeWidth);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        TextureRegion region = new TextureRegion(texture);
        
        // Splits need to account for stroke width
        int splitValue = cornerRadius + strokeWidth;
        NinePatch ninePatch = new NinePatch(region, splitValue, splitValue, splitValue, splitValue);
        
        return new NinePatchDrawable(ninePatch);
    }

    /**
     * Create a Pixmap with rounded corners and a black stroke
     * Designed specifically for NinePatch to avoid distortion
     * 
     * @param size Width and height of the pixmap
     * @param color The fill color
     * @param cornerRadius The corner radius
     * @param strokeWidth The stroke width
     * @return A Pixmap with rounded corners and stroke
     */
    private static Pixmap createNinePatchStrokedPixmap(int size, int size2, Color color, int cornerRadius, int strokeWidth) {
        Pixmap pixmap = new Pixmap(size, size2, Pixmap.Format.RGBA8888);
        
        // Set background to transparent
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        
        // Draw the main rounded rectangle with fill color
        pixmap.setColor(color);
        
        int inset = strokeWidth;
        int innerRadius = cornerRadius - inset;
        if (innerRadius < 0) innerRadius = 1;
        
        // Draw filled center with rounded interior
        pixmap.fillRectangle(cornerRadius + inset, inset, size - 2 * (cornerRadius + inset), size2 - 2 * inset);
        pixmap.fillRectangle(inset, cornerRadius + inset, size - 2 * inset, size2 - 2 * (cornerRadius + inset));
        
        // Draw fill color corners
        drawCorner(pixmap, cornerRadius + inset, cornerRadius + inset, innerRadius, color);  // Top-left
        drawCorner(pixmap, size - cornerRadius - inset - 1, cornerRadius + inset, innerRadius, color);  // Top-right
        drawCorner(pixmap, cornerRadius + inset, size2 - cornerRadius - inset - 1, innerRadius, color);  // Bottom-left
        drawCorner(pixmap, size - cornerRadius - inset - 1, size2 - cornerRadius - inset - 1, innerRadius, color);  // Bottom-right
        
        // Draw black stroke around the rounded rectangle
        drawRoundedStroke(pixmap, cornerRadius, strokeWidth, Color.BLACK);
        
        return pixmap;
    }

    /**
     * Draw a rounded stroke outline for a rounded rectangle
     */
    private static void drawRoundedStroke(Pixmap pixmap, int cornerRadius, int strokeWidth, Color color) {
        pixmap.setColor(color);
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        
        // Draw horizontal stroke lines
        for (int i = 0; i < strokeWidth; i++) {
            for (int x = cornerRadius; x < width - cornerRadius; x++) {
                // Top stroke
                if (i < height) pixmap.drawPixel(x, i, Color.rgba8888(color));
                // Bottom stroke
                if (height - 1 - i >= 0) pixmap.drawPixel(x, height - 1 - i, Color.rgba8888(color));
            }
        }
        
        // Draw vertical stroke lines
        for (int i = 0; i < strokeWidth; i++) {
            for (int y = cornerRadius; y < height - cornerRadius; y++) {
                // Left stroke
                if (i < width) pixmap.drawPixel(i, y, Color.rgba8888(color));
                // Right stroke
                if (width - 1 - i >= 0) pixmap.drawPixel(width - 1 - i, y, Color.rgba8888(color));
            }
        }
        
        // Draw stroke around corners
        drawCornerStroke(pixmap, cornerRadius, cornerRadius, cornerRadius, strokeWidth, color);  // Top-left
        drawCornerStroke(pixmap, width - cornerRadius - 1, cornerRadius, cornerRadius, strokeWidth, color);  // Top-right
        drawCornerStroke(pixmap, cornerRadius, height - cornerRadius - 1, cornerRadius, strokeWidth, color);  // Bottom-left
        drawCornerStroke(pixmap, width - cornerRadius - 1, height - cornerRadius - 1, cornerRadius, strokeWidth, color);  // Bottom-right
    }

    /**
     * Draw a stroked corner
     */
    private static void drawCornerStroke(Pixmap pixmap, int centerX, int centerY, int radius, int strokeWidth, Color color) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                double dist = Math.sqrt(x * x + y * y);
                if (dist >= radius - strokeWidth && dist <= radius) {
                    int px = centerX + x;
                    int py = centerY + y;
                    if (px >= 0 && px < pixmap.getWidth() && py >= 0 && py < pixmap.getHeight()) {
                        pixmap.drawPixel(px, py, Color.rgba8888(color));
                    }
                }
            }
        }
    }

    /**
     * Create a stroked border drawable (for the modal border)
     * Creates a hollow rectangle with a specified stroke width and color
     * 
     * @param width Width of the drawable in pixels
     * @param height Height of the drawable in pixels
     * @param strokeWidth Stroke width in pixels (typically 2)
     * @param strokeColor Color of the stroke (e.g., Color.BLACK)
     * @param fillColor Fill color inside the stroke (typically transparent or background color)
     * @return A TextureRegion drawable for the stroked border
     */
    public static NinePatchDrawable createStrokedBorder(int width, int height, int strokeWidth, 
                                                        Color strokeColor, Color fillColor) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        
        // Fill with background color
        pixmap.setColor(fillColor);
        pixmap.fill();
        
        // Draw stroke outline
        pixmap.setColor(strokeColor);
        for (int i = 0; i < strokeWidth; i++) {
            pixmap.drawRectangle(i, i, width - 2 * i - 1, height - 2 * i - 1);
        }
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        TextureRegion region = new TextureRegion(texture);
        NinePatch ninePatch = new NinePatch(region, strokeWidth + 2, strokeWidth + 2, 
                                            strokeWidth + 2, strokeWidth + 2);
        
        return new NinePatchDrawable(ninePatch);
    }
}
