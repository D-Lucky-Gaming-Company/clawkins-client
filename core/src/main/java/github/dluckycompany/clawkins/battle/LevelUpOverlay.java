package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Matrix4;

import github.dluckycompany.clawkins.character.LevelUpResult;
import github.dluckycompany.clawkins.character.ExpManager;

import java.util.List;

/**
 * Displays level-up information after battle victory.
 * Shows stat increases and newly unlocked skills.
 */
public class LevelUpOverlay {
    
    private static final String TAG = "LevelUpOverlay";
    
    private final BitmapFont font;
    private final Matrix4 uiProjection;
    
    private boolean visible = false;
    private String levelUpMessage = "";
    private String clawkinName = "";
    
    public LevelUpOverlay(BitmapFont font) {
        this.font = font;
        this.uiProjection = new Matrix4();
    }
    
    /**
     * Shows the level-up overlay with results.
     * 
     * @param clawkinName Name of the Clawkin that leveled up
     * @param results List of level-up results
     */
    public void show(String clawkinName, List<LevelUpResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        
        this.clawkinName = clawkinName;
        this.levelUpMessage = ExpManager.formatLevelUpMessage(clawkinName, results);
        this.visible = true;
        
        Gdx.app.log(TAG, "Showing level-up overlay for " + clawkinName);
    }
    
    /**
     * Hides the level-up overlay.
     */
    public void hide() {
        visible = false;
        levelUpMessage = "";
        clawkinName = "";
    }
    
    /**
     * Checks if the overlay is currently visible.
     * 
     * @return True if visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Updates the overlay (checks for input to dismiss).
     * 
     * @return True if the overlay was dismissed this frame
     */
    public boolean update() {
        if (!visible) {
            return false;
        }
        
        // Check for any interaction key to dismiss
        if (Gdx.input.isKeyJustPressed(Keys.Z) || 
            Gdx.input.isKeyJustPressed(Keys.SPACE) || 
            Gdx.input.isKeyJustPressed(Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Keys.X) ||
            Gdx.input.isKeyJustPressed(Keys.ESCAPE) ||
            Gdx.input.justTouched()) {
            hide();
            return true;
        }
        
        return false;
    }
    
    /**
     * Renders the level-up overlay.
     * 
     * @param batch The sprite batch to render with
     */
    public void render(Batch batch) {
        if (!visible || levelUpMessage.isEmpty()) {
            return;
        }
        
        // Set up UI projection
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        uiProjection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        batch.setProjectionMatrix(uiProjection);
        
        batch.begin();
        
        // Draw semi-transparent background
        // (Note: This would ideally use a filled rectangle, but we'll use text for simplicity)
        
        // Calculate text position (centered)
        float textX = 50f;
        float textY = screenHeight - 100f;
        
        // Draw level-up message
        font.setColor(Color.YELLOW);
        font.draw(batch, levelUpMessage, textX, textY);
        
        // Draw prompt to continue
        font.setColor(Color.WHITE);
        String prompt = "\n\nPress any key to continue...";
        font.draw(batch, prompt, textX, textY - 300f);
        
        batch.end();
    }
}
