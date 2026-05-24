package github.dluckycompany.clawkins.ui;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.character.LevelSystem;

/**
 * ClawkinCard - Reusable party member slot widget
 * 
 * Displays a single Clawkin party member as a card with portrait, metadata,
 * and health bar. Handles empty slots with a ghost appearance.
 * 
 * Layout: [Portrait 100x100] | [Metadata] | [HP Bar]
 * 
 * Color Palette:
 * - Portrait background: #E7DAC7 (beige)
 * - Text: #1F1A13 (dark brown)
 * - HP Bar: Green (#638A52) >50%, Yellow (#C19253) <=50%, Red (#A13D3D) <=20%
 * - Border: 2px black stroke (via RoundedPanelDrawable)
 */
public class ClawkinCard extends AbstractClawkinHpTable implements ClawkinCardView {
    
    // ============ Color Palette ============
    private static final Color PORTRAIT_BG = Color.valueOf("#E7DAC7");
    private static final Color ACTIVE_FIGHTER_BG = Color.valueOf("#F5DFA0");   // warm gold tint
    private static final Color TEXT_COLOR = Color.valueOf("#1F1A13");
    private static final Color HP_GREEN = Color.valueOf("#638A52");
    private static final Color HP_YELLOW = Color.valueOf("#C19253");
    private static final Color HP_RED = Color.valueOf("#A13D3D");
    private static final Color HP_BACKGROUND = Color.valueOf("#444444");

    // Keep portrait textures cached across card instances.
    private static final Map<String, Texture> PORTRAIT_CACHE = new HashMap<>();
    
    private final BitmapFont font;
    private Table hpBarContainer;       // Container for left-aligned HP bar
    private Image hpBarForeground;      // Foreground bar that grows left-to-right
    private Cell<Image> hpBarForegroundCell; // Layout cell used to control dynamic width
    private Label hpLabel;
    private Label levelLabel;
    private Label expLabel;
    private Table portraitContainer;
    private boolean isSelected = false;
    private boolean isActiveFighter = false;
    private int sharedTotalExperience = -1;
    
    // HP bar constants
    private static final float HP_BAR_WIDTH = 130f;    // Total width of the bar track
    private static final float HP_BAR_HEIGHT = 22f;    // Height of the bar
    
    /**
     * Create a ClawkinCard widget
     * 
     * @param clawkin The Clawkin to display, or null for empty slot
     * @param font The BitmapFont for text rendering
     */
    public ClawkinCard(Clawkin clawkin, BitmapFont font) {
        this(clawkin, font, -1);
    }

    public ClawkinCard(Clawkin clawkin, BitmapFont font, int sharedTotalExperience) {
        super(clawkin);
        this.font = font;
        this.sharedTotalExperience = sharedTotalExperience;
        composeLayout();
    }

    @Override
    protected void buildLayout() {
        pad(8);
        defaults().padRight(12).expandY().fillY();

        if (clawkin == null) {
            buildGhostSlot();
        } else {
            buildClawkinSlot();
        }

        updateBorderStyle();
    }
    
    /**
     * Build the layout for an empty clawkin slot
     */
    private void buildGhostSlot() {
        // Create container with dashed border appearance
        Table ghostContent = new Table();
        ghostContent.setBackground(RoundedPanelDrawable.createRoundedPanel(PORTRAIT_BG, 12));
        
        // "EMPTY" label
        Label emptyLabel = new Label("EMPTY", new Label.LabelStyle(font, TEXT_COLOR));
        emptyLabel.setFontScale(1.2f);
        ghostContent.add(emptyLabel).center().pad(40);
        
        // Add to main table
        this.add(ghostContent).expand().fill();
    }
    
    /**
     * Build the layout for a non-null clawkin
     */
    private void buildClawkinSlot() {
        // ============ Portrait Cell ============
        portraitContainer = new Table();
        portraitContainer.setBackground(RoundedPanelDrawable.createRoundedPanel(PORTRAIT_BG, 12));
        portraitContainer.pad(4);

        Image portraitImage = buildPortraitImage();
        if (portraitImage != null) {
            portraitImage.setScaling(Scaling.fit);
            portraitContainer.add(portraitImage).size(100, 100);
        } else {
            // Fallback placeholder if no portrait texture is available.
            Table portraitPlaceholder = new Table();
            portraitPlaceholder.setBackground(RoundedPanelDrawable.createRoundedPanel(Color.valueOf("#D4A574"), 6));
            portraitContainer.add(portraitPlaceholder).size(100, 100);
        }
        
        this.add(portraitContainer).size(108, 108);
        
        // ============ Metadata Cell ============
        Table metadataCell = new Table();
        metadataCell.left();
        metadataCell.columnDefaults(0).left().expandX().fillX().padRight(8);
        
        // Name label (large, bold)
        Label nameLabel = new Label(clawkin.getName(), new Label.LabelStyle(font, TEXT_COLOR));
        nameLabel.setFontScale(1.3f);
        metadataCell.add(nameLabel).left().row();
        
        // Species/Role label (smaller text)
        Label speciesLabel = new Label("Type: " + (clawkin.getId().length() > 0 ? clawkin.getId() : "Unknown"), 
            new Label.LabelStyle(font, TEXT_COLOR));
        speciesLabel.setFontScale(0.8f);
        metadataCell.add(speciesLabel).left().row();
        
        // Spacer
        metadataCell.add().expand().row();
        
        // Level label (bottom-left positioning)
        levelLabel = new Label("", new Label.LabelStyle(font, TEXT_COLOR));
        levelLabel.setFontScale(0.9f);
        metadataCell.add(levelLabel).left().row();

        expLabel = new Label("", new Label.LabelStyle(font, TEXT_COLOR));
        expLabel.setFontScale(0.75f);
        metadataCell.add(expLabel).left().bottom();
        
        this.add(metadataCell).expandX().fillX();
        
        // ============ Vitality (HP Bar) Cell ============
        buildHpBar();
        this.add(hpBarContainer).width(140).fillY().padRight(8);
    }
    
    /**
     * Build the HP bar with left-aligned, directional fill (left-to-right)
     * 
     * Structure:
     * hpBarContainer (vertical layout)
     * ├─ hpLabel (top, centered)
     * └─ hpStack (horizontal, left-aligned)
     *    ├─ Background bar (empty track - gray, full width)
     *    └─ Foreground bar (health fill - colored, dynamic width)
     */
    private void buildHpBar() {
        // ============ HP CONTAINER ============
        // Vertical layout: label above bar
        hpBarContainer = new Table();
        hpBarContainer.center();
        hpBarContainer.columnDefaults(0).expandX().fillX();
        
        // HP Text Label (above bar)
        hpLabel = new Label(
            clawkin.getCurrentHp() + "/" + clawkin.getMaxHp(),
            new Label.LabelStyle(font, TEXT_COLOR)
        );
        hpLabel.setFontScale(0.7f);
        hpLabel.setAlignment(Align.center);
        hpBarContainer.add(hpLabel).expandX().fillX().padBottom(4).row();
        
        // ============ HP BAR STACK (Background + Foreground) ============
        Stack hpBarStack = new Stack();
        
        // Background bar (gray rounded rectangle - full width, static)
        Drawable hpBgDrawable = RoundedPanelDrawable.createRoundedPanel(HP_BACKGROUND, 6);
        Image hpBackgroundBar = new Image(hpBgDrawable);
        hpBarStack.add(hpBackgroundBar);
        
        // Foreground bar (colored health - dynamic width, left-aligned)
        // Create a table for left alignment of the foreground bar
        Table hpForegroundContainer = new Table();
        hpForegroundContainer.left();  // CRITICAL: Align to left
        
        hpBarForeground = new Image(RoundedPanelDrawable.createRoundedPanel(HP_GREEN, 6));
        hpBarForegroundCell = hpForegroundContainer.add(hpBarForeground).left();  // Left-aligned in container
        
        hpBarStack.add(hpForegroundContainer);
        
        // Add HP bar stack to container
        hpBarContainer.add(hpBarStack).expandX().fillX().height(HP_BAR_HEIGHT);
        
        // Set initial HP bar display
        refreshStats();
        refreshProgressText();
    }
    
    /**
     * Update the HP bar display with new health value
     * 
     * Implements left-to-right directional fill by calculating
     * the exact pixel width based on HP percentage.
     */
    @Override
    public void updateHp() {
        refreshStats();
    }
    
    /**
     * Refresh the card's stats display - recalculates HP bar width
     * based on current HP/maxHP ratio. Uses relative percentage calculation.
     * 
     * Formula: barWidth = HP_BAR_WIDTH * (currentHP / maxHP)
     * This ensures proportional display (25/50 = exactly half-width).
     * 
     * Applies color thresholds:
     * - Green: health ≥ 50%
     * - Yellow: health ≤ 50%
     * - Red: health ≤ 20% (critical)
     */
    public void refreshStats() {
        if (clawkin == null || hpLabel == null || hpBarForeground == null || hpBarForegroundCell == null) {
            return;
        }
        
        // Update label
        hpLabel.setText(clawkin.getCurrentHp() + "/" + clawkin.getMaxHp());
        
        // Calculate health percentage (clamped 0.0 to 1.0)
        float hpPercent = Math.max(0, Math.min(1, (float) clawkin.getCurrentHp() / clawkin.getMaxHp()));
        
        // Calculate the actual width of the foreground bar (left-to-right fill)
        float barWidth = HP_BAR_WIDTH * hpPercent;
        
        // Update the cell width so Scene2D Table layout keeps true proportional fill.
        hpBarForegroundCell.width(barWidth).height(HP_BAR_HEIGHT);
        hpBarContainer.invalidateHierarchy();
        
        // Determine color based on health thresholds
        Color barColor = HP_GREEN;      // >=50%: green
        if (hpPercent <= 0.2f) {
            barColor = HP_RED;          // <=20%: red (critical)
        } else if (hpPercent <= 0.5f) {
            barColor = HP_YELLOW;       // <=50%: yellow (medium)
        }
        
        // Update foreground bar color
        Drawable barDrawable = RoundedPanelDrawable.createRoundedPanel(barColor, 6);
        hpBarForeground.setDrawable(barDrawable);
        refreshProgressText();
    }

    private void refreshProgressText() {
        if (clawkin == null || levelLabel == null || expLabel == null) {
            return;
        }
        if (sharedTotalExperience < 0) {
            levelLabel.setText("Lv. " + clawkin.getLevel());
            expLabel.setText("");
            return;
        }

        int level = LevelSystem.calculateLevelFromExp(sharedTotalExperience);
        levelLabel.setText("Lv. " + level);
        if (level >= LevelSystem.MAX_LEVEL) {
            expLabel.setText("XP: MAX");
            return;
        }

        int levelFloor = LevelSystem.getExpRequiredForLevel(level);
        int nextLevelExp = LevelSystem.getExpForNextLevel(level);
        int expIntoLevel = Math.max(0, sharedTotalExperience - levelFloor);
        int clampedExp = Math.min(expIntoLevel, Math.max(0, nextLevelExp));
        expLabel.setText("XP: " + clampedExp + " / " + nextLevelExp);
    }

    public void setSharedExperience(int totalExp) {
        sharedTotalExperience = Math.max(0, totalExp);
        refreshProgressText();
    }
    
    /**
     * Set selection state for this card
     * 
     * @param selected True to highlight as selected
     */
    @Override
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateBorderStyle();
    }
    
    // Update the card border/background to reflect both hover and active-fighter state.
    private void updateBorderStyle() {
        Color bg = isActiveFighter ? ACTIVE_FIGHTER_BG : PORTRAIT_BG;
        if (isSelected) {
            this.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(bg, 12, 4));
        } else if (isActiveFighter) {
            this.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(bg, 12, 2));
        } else {
            this.setBackground(RoundedPanelDrawable.createRoundedPanel(PORTRAIT_BG, 12));
        }
    }

    /**
     * Mark this card as the currently active battle fighter.
     *
     * @param active True to show the gold "active fighter" highlight
     */
    public void setActiveFighter(boolean active) {
        this.isActiveFighter = active;
        updateBorderStyle();
    }

    /**
     * Check whether this card is the active battle fighter.
     */
    public boolean isActiveFighter() {
        return isActiveFighter;
    }

    @Override
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Builds a portrait image using authored image path first, then fallbacks.
     */
    private Image buildPortraitImage() {
        String[] candidatePaths = resolvePortraitPaths();

        for (String path : candidatePaths) {
            Texture texture = getOrLoadTexture(path);
            if (texture != null) {
                return new Image(new TextureRegionDrawable(new TextureRegion(texture)));
            }
        }

        return null;
    }

    private String[] resolvePortraitPaths() {
        String configured = clawkin.getImagePath();
        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.trim();
            if (trimmed.startsWith("/")) {
                trimmed = trimmed.substring(1);
            }
            return new String[] { trimmed };
        }

        // Legacy fallback mapping when no explicit image path is authored.
        String name = clawkin.getName() == null ? "" : clawkin.getName().toLowerCase();
        String id = clawkin.getId() == null ? "" : clawkin.getId().toLowerCase();

        if (name.contains("ginger") || id.contains("ginger")) {
            return new String[] {
                
                "entities/clawkins/Clawkin_01.png",
              
            };
        }

        // Handle both "Swee'pea" and common misspellings like "Sweapee".
        if (name.contains("swee") || name.contains("swea") || id.contains("swee") || id.contains("swea")) {
            return new String[] {
                "entities/clawkins/Clawkin_02.png",
            };
        }

        if (name.contains("dart") || id.contains("dart")) {
            return new String[] {
                
                "entities/clawkins/Clawkin_03.png",
                
            };
        }

        return new String[] {
            
            "entities/clawkins/Clawkin_01.png",
            
        };
    }

    private Texture getOrLoadTexture(String path) {
        Texture cached = PORTRAIT_CACHE.get(path);
        if (cached != null) {
            return cached;
        }

        try {
            if (!Gdx.files.internal(path).exists()) {
                return null;
            }
            Texture loaded = new Texture(Gdx.files.internal(path));
            PORTRAIT_CACHE.put(path, loaded);
            return loaded;
        } catch (Exception e) {
            Gdx.app.log("ClawkinCard", "Failed to load portrait " + path + ": " + e.getMessage());
            return null;
        }
    }
}
