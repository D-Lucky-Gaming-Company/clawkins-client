package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Utility class for managing viewport coordinate transformations and scissor stack operations.
 * 
 * Handles the complex transformations needed to ensure UI elements remain properly positioned
 * and clipped across fullscreen transitions when using FitViewport.
 * 
 * Key responsibility: Convert between virtual world coordinates (800x600) and physical
 * screen pixel coordinates during scissor stack operations for ScrollPane clipping.
 */
public class ViewportCoordinateHelper {

    /**
     * Calculate scissor coordinates for a ScrollPane or clipped area within a Stage.
     * Converts from virtual world coordinates to physical screen pixel coordinates,
     * accounting for the viewport's centering and scaling.
     *
     * This is critical for ScrollPane clipping during fullscreen transitions:
     * - Virtual coordinates (800x600) stay constant
     * - Physical coordinates change with screen resolution
     * - Scissor test expects physical screen coordinates
     *
     * @param stage the UI stage containing the clipping area
     * @param worldCoords rectangle in virtual world coordinates (x, y, width, height)
     * @return rectangle in physical screen pixel coordinates suitable for glScissor()
     */
    public static Rectangle getScissorBounds(Stage stage, Rectangle worldCoords) {
        // Get viewport's physical dimensions and scaling
        Viewport viewport = stage.getViewport();
        
        // Get the viewport's screen coordinates (where it's positioned on the physical screen)
        int screenX = viewport.getScreenX();
        int screenY = viewport.getScreenY();
        int screenWidth = viewport.getScreenWidth();
        int screenHeight = viewport.getScreenHeight();
        
        // Get virtual world dimensions
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        // Calculate scale factors
        float scaleX = screenWidth / worldWidth;
        float scaleY = screenHeight / worldHeight;
        
        // Transform virtual coordinates to screen pixel coordinates
        float pixelX = screenX + (worldCoords.x * scaleX);
        float pixelY = screenY + (screenHeight - ((worldCoords.y + worldCoords.height) * scaleY));
        float pixelWidth = worldCoords.width * scaleX;
        float pixelHeight = worldCoords.height * scaleY;
        
        // Handle HDPI scaling (important for high-DPI displays)
        pixelX = HdpiUtils.toLogicalX((int) pixelX);
        pixelY = HdpiUtils.toLogicalY((int) pixelY);
        pixelWidth = HdpiUtils.toLogicalX((int) pixelWidth);
        pixelHeight = HdpiUtils.toLogicalY((int) pixelHeight);
        
        return new Rectangle(pixelX, pixelY, pixelWidth, pixelHeight);
    }

    /**
     * Convert virtual world coordinates to physical screen pixel coordinates.
     * Useful for custom drawing or clipping operations that work in pixel space.
     *
     * @param stage the UI stage
     * @param virtualX X-coordinate in virtual world space
     * @param virtualY Y-coordinate in virtual world space
     * @return Vector2 containing pixel coordinates on physical screen
     */
    public static Vector2 virtualToScreen(Stage stage, float virtualX, float virtualY) {
        Viewport viewport = stage.getViewport();
        
        int screenX = viewport.getScreenX();
        int screenY = viewport.getScreenY();
        int screenWidth = viewport.getScreenWidth();
        int screenHeight = viewport.getScreenHeight();
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        float scaleX = screenWidth / worldWidth;
        float scaleY = screenHeight / worldHeight;
        
        float pixelX = screenX + (virtualX * scaleX);
        float pixelY = screenY + (screenHeight - (virtualY * scaleY));
        
        return new Vector2(pixelX, pixelY);
    }

    /**
     * Convert physical screen pixel coordinates to virtual world coordinates.
     * Inverse transformation of virtualToScreen().
     *
     * @param stage the UI stage
     * @param screenX X-coordinate in physical screen pixels
     * @param screenY Y-coordinate in physical screen pixels
     * @return Vector2 containing virtual world coordinates
     */
    public static Vector2 screenToVirtual(Stage stage, float screenX, float screenY) {
        Viewport viewport = stage.getViewport();
        
        int vpScreenX = viewport.getScreenX();
        int vpScreenY = viewport.getScreenY();
        int vpScreenWidth = viewport.getScreenWidth();
        int vpScreenHeight = viewport.getScreenHeight();
        
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        
        float scaleX = worldWidth / vpScreenWidth;
        float scaleY = worldHeight / vpScreenHeight;
        
        float virtualX = (screenX - vpScreenX) * scaleX;
        float virtualY = (vpScreenHeight - (screenY - vpScreenY)) * scaleY;
        
        return new Vector2(virtualX, virtualY);
    }

    /**
     * Check if a point in virtual world coordinates is within the viewport's safe area.
     * Useful for determining if UI elements are clipped at screen edges.
     *
     * @param stage the UI stage
     * @param worldX X-coordinate in virtual world space
     * @param worldY Y-coordinate in virtual world space
     * @param padding safety margin in virtual units
     * @return true if point is within safe bounds
     */
    public static boolean isInSafeArea(Stage stage, float worldX, float worldY, float padding) {
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        
        return worldX >= padding && 
               worldX <= worldWidth - padding &&
               worldY >= padding && 
               worldY <= worldHeight - padding;
    }

    /**
     * Get the aspect ratio of the physical screen.
     * Useful for responsive design decisions.
     *
     * @param stage the UI stage
     * @return width / height ratio of the physical screen
     */
    public static float getScreenAspectRatio(Stage stage) {
        Viewport viewport = stage.getViewport();
        return (float) viewport.getScreenWidth() / viewport.getScreenHeight();
    }

    /**
     * Get the virtual world aspect ratio (typically constant at 4:3 for 800x600)
     *
     * @param stage the UI stage
     * @return width / height ratio of the virtual world
     */
    public static float getVirtualAspectRatio(Stage stage) {
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        return worldWidth / worldHeight;
    }
}
