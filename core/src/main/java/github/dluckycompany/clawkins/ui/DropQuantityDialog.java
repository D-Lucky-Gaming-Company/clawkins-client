package github.dluckycompany.clawkins.ui;

import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import github.dluckycompany.clawkins.item.Item;

/**
 * HIGH-FIDELITY MODAL DROP DIALOG
 * 
 * A modern, theme-consistent "Drop Quantity" dialog featuring:
 * - Custom antique color palette (#E7DAC7, #1F1A13, #C19253)
 * - Rounded corners with NinePatch (12px radius)
 * - Mathematically centered quantity controls
 * - Professional RPG aesthetic
 * - Modal behavior with proper input handling
 * 
 * Uses Dialog.show(stage) for proper modality and input handling.
 * Buttons are handled through Dialog.button() which properly
 * triggers hide(result) and calls result(Object) override.
 */
public class DropQuantityDialog extends Dialog {
    // ============================================================
    // CUSTOM COLOR PALETTE (HEX VALUES)
    // ============================================================
    private static final Color DIALOG_BG = Color.valueOf("#E7DAC7");        // Warm Beige / Antique Paper
    private static final Color TEXT_PRIMARY = Color.valueOf("#1F1A13");     // Dark Coffee / Deep Brown
    
    private final Item item;
    private final int maxQuantity;
    private final BitmapFont font;
    private int selectedQuantity = 1;
    private Label quantityLabel;
    private Label infoLabel;
    private Consumer<Integer> confirmCallback;
    private Runnable cancelCallback;
    
    // Button drawables for graphical buttons
    private final TextureRegionDrawable minusDrawable;
    private final TextureRegionDrawable plusDrawable;
    
    // Dialog result constants
    private static final Integer DIALOG_CONFIRMED = 1;
    private static final Integer DIALOG_CANCELLED = 0;

    public DropQuantityDialog(Item item, int currentQuantity, Skin skin, 
                            TextureRegionDrawable minusDrawable,
                            TextureRegionDrawable plusDrawable,
                            TextureRegionDrawable exitDrawable) {
        super("Drop " + item.getName(), createWindowStyle(skin));
        this.item = item;
        this.maxQuantity = Math.max(1, currentQuantity);
        this.minusDrawable = minusDrawable;
        this.plusDrawable = plusDrawable;
        
        // CRITICAL: Use safe font access to prevent crashes
        // If skin.getFont("default") fails, UISkinManager provides fallback
        BitmapFont retrievedFont = UISkinManager.getSafeFont(skin, "default");
        if (retrievedFont == null) {
            Gdx.app.error("DropQuantityDialog", "FATAL: Font is null after safe access, creating fallback");
            retrievedFont = new BitmapFont();  // Last-resort fallback
        }
        this.font = retrievedFont;
        
        setModal(true);
        setMovable(false);
        setResizable(false);
        
        buildUI();
    }

    /**
     * Convenience constructor for backward compatibility (no graphical buttons)
     * @deprecated Use the full constructor with drawables instead
     */
    @Deprecated
    public DropQuantityDialog(Item item, int currentQuantity, Skin skin) {
        this(item, currentQuantity, skin, null, null, null);
    }

    /**
     * Create a custom WindowStyle with the antique color palette
     * SAFE: Uses UISkinManager for font access
     * 
     * Features:
     * - Rounded corner background (12px radius) with black stroke (2px)
     * - Theme-consistent #E7DAC7 beige color
     * - Professional modal styling for CIT-U standards
     */
    private static Window.WindowStyle createWindowStyle(Skin skin) {
        Window.WindowStyle style = new Window.WindowStyle();
        
        // Apply rounded corner background with black stroke instead of sharp rectangle
        style.background = RoundedPanelDrawable.createRoundedPanelWithStroke(DIALOG_BG, 12, 2);
        
        // CRITICAL: Safe font access instead of direct skin.getFont()
        BitmapFont titleFont = UISkinManager.getSafeFont(skin, "default");
        if (titleFont != null) {
            style.titleFont = titleFont;
        } else {
            Gdx.app.error("DropQuantityDialog", "WARNING: titleFont is null, using fallback");
            style.titleFont = new BitmapFont();
        }
        
        style.titleFontColor = TEXT_PRIMARY;
        
        return style;
    }

    /**
     * Apply a smooth color fade effect to an ImageButton on interaction
     * On touchDown: Quick gray tint (0.7f, 0.7f, 0.7f, 1f) with optional squish scale
     * On touchUp: Fade smoothly back to white over 0.15 seconds
     */
    private void applyImageButtonFade(ImageButton imageButton) {
        imageButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Quick gray tint on touch
                imageButton.setColor(0.7f, 0.7f, 0.7f, 1f);
                // Squish effect on press
                imageButton.addAction(Actions.scaleTo(0.95f, 0.95f, 0.05f));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Fade back to white smoothly
                imageButton.addAction(Actions.sequence(
                    Actions.color(Color.WHITE, 0.15f, Interpolation.fade)
                ));
                // Scale back to normal
                imageButton.addAction(Actions.scaleTo(1f, 1f, 0.1f));
            }
        });
    }

    /**
     * Create a high-fidelity rounded button style matching the "Paper-on-Wood" theme
     * 
     * Style Definition:
     * - Up state: Rounded beige (#E7DAC7) background
     * - Over state: Rounded tan (#C19253) background (hover highlight)
     * - Text: Dark brown (#1F1A13)
     * - NinePatch: 8px rounded corners for consistency with inventory buttons
     * 
     * @return A TextButtonStyle with rounded NinePatch drawables
     */
    private TextButton.TextButtonStyle getRoundedButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        
        // Up state: Beige rounded background
        style.up = RoundedPanelDrawable.createRoundedPanel(Color.valueOf("#E7DAC7"), 8);
        
        // Over state: Tan rounded background (hover highlight)
        style.over = RoundedPanelDrawable.createRoundedPanel(Color.valueOf("#C19253"), 8);
        
        // Down state: Use over state for press feedback
        style.down = style.over;
        
        // Checked state: Same as over
        style.checked = style.over;
        
        // Button text color
        style.fontColor = Color.valueOf("#1F1A13");  // Dark Brown
        
        // Font
        style.font = font;
        
        return style;
    }

    /**
     * Apply a smooth color fade effect to a TextButton on interaction
     * On touchDown: Scale down slightly (0.95x) for press feedback
     * On touchUp: Scale back up over 0.1 seconds with smooth interpolation
     */
    private void applyTextButtonFade(TextButton textButton) {
        textButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Squish effect on press for tactile feedback
                textButton.addAction(Actions.scaleTo(0.95f, 0.95f, 0.05f));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Scale back to normal smoothly
                textButton.addAction(Actions.scaleTo(1f, 1f, 0.1f, Interpolation.pow2Out));
            }
        });
    }

    private void buildUI() {
        Table contentTable = getContentTable();
        contentTable.clear();
        contentTable.pad(20);  // 20px padding to prevent text touching rounded edges
        
        // REMOVED: "Available: X" label (now redundant - header shows "DROP: [Item Name]")
        // This simplifies the modal and reduces clutter
        
        // ============================================================
        // QUANTITY SELECTION CONTROLS
        // [Minus Button] | [Centered Quantity Label] | [Plus Button]
        // ============================================================
        Table quantityTable = new Table();
        quantityTable.pad(15);  // Increased internal padding for visual breathing room
        
        // MINUS button (graphical asset from ui/buttons/minus.png)
        ImageButton minusButton;
        if (minusDrawable != null) {
            ImageButton.ImageButtonStyle minusStyle = new ImageButton.ImageButtonStyle();
            minusStyle.up = minusDrawable;
            minusStyle.down = minusDrawable;  // Use same drawable - fade effect handles tinting
            minusStyle.checked = minusDrawable;
            minusButton = new ImageButton(minusStyle);
        } else {
            minusButton = new ImageButton(new ImageButton.ImageButtonStyle());
            Gdx.app.log("DropQuantityDialog", "Minus button texture failed to load");
        }
        minusButton.setSize(40, 40);
        
        // Apply fade effect
        applyImageButtonFade(minusButton);
        
        // Add click listener for functionality
        minusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedQuantity > 1) {
                    selectedQuantity--;
                    updateQuantityLabel();
                }
            }
        });
        quantityTable.add(minusButton).width(40).height(40).padRight(15);
        
        // CENTERED QUANTITY LABEL - CRITICAL for centering!
        quantityLabel = new Label("1", new Label.LabelStyle(font, TEXT_PRIMARY));
        quantityLabel.setFontScale(2.0f);
        quantityLabel.setAlignment(Align.center);  // Align text to center
        quantityTable.add(quantityLabel).expandX().fillX().padRight(15);  // expandX() makes it grow to fill space
        
        // PLUS button (graphical asset from ui/buttons/plus.png)
        ImageButton plusButton;
        if (plusDrawable != null) {
            ImageButton.ImageButtonStyle plusStyle = new ImageButton.ImageButtonStyle();
            plusStyle.up = plusDrawable;
            plusStyle.down = plusDrawable;  // Use same drawable - fade effect handles tinting
            plusStyle.checked = plusDrawable;
            plusButton = new ImageButton(plusStyle);
        } else {
            plusButton = new ImageButton(new ImageButton.ImageButtonStyle());
            Gdx.app.log("DropQuantityDialog", "Plus button texture failed to load");
        }
        plusButton.setSize(40, 40);
        
        // Apply fade effect
        applyImageButtonFade(plusButton);
        
        // Add click listener for functionality
        plusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedQuantity < maxQuantity) {
                    selectedQuantity++;
                    updateQuantityLabel();
                }
            }
        });
        quantityTable.add(plusButton).width(40).height(40);
        
        contentTable.add(quantityTable).expandX().fillX().height(70).padBottom(25).row();
        
        // ============================================================
        // INFO LABEL (Shows drop summary with proper padding)
        // ============================================================
        // Use standard ASCII characters (no Unicode) to avoid glyph rendering issues
        infoLabel = new Label("Drop: 1x " + item.getName(), new Label.LabelStyle(font, TEXT_PRIMARY));
        infoLabel.setFontScale(1.3f);
        infoLabel.setWrap(true);
        infoLabel.setAlignment(Align.center);  // Center the summary text
        contentTable.add(infoLabel).expandX().fillX().padBottom(30).row();  // Increased padding for breathing room
        
        // ============================================================
        // DIALOG BUTTONS (Confirm / Cancel) with Rounded Style
        // ============================================================
        // Apply high-fidelity rounded button styles matching the "Paper-on-Wood" theme
        TextButton.TextButtonStyle roundedButtonStyle = getRoundedButtonStyle();
        
        TextButton confirmButton = new TextButton("Confirm", roundedButtonStyle);
        confirmButton.getLabel().setFontScale(1.4f);
        
        // Apply fade effect for smooth interaction feedback
        applyTextButtonFade(confirmButton);
        
        TextButton cancelButton = new TextButton("Cancel", roundedButtonStyle);
        cancelButton.getLabel().setFontScale(1.4f);
        
        // Apply fade effect for smooth interaction feedback
        applyTextButtonFade(cancelButton);
        
        // Add buttons to dialog with proper spacing
        button(confirmButton, DIALOG_CONFIRMED).padRight(10);
        button(cancelButton, DIALOG_CANCELLED);
        
        // Update title color to match palette
        // CRITICAL: Do NOT set title table background (causes black bar)
        // Leave background unset (defaults to transparent)
        getTitleLabel().setColor(TEXT_PRIMARY);
        getTitleLabel().setAlignment(Align.center);  // Center the title text
        
        // Apply padding to button table to prevent buttons from touching borders
        getButtonTable().pad(20);
        getButtonTable().padTop(10);  // Extra spacing above buttons
        
        // CRITICAL: Pack the dialog to calculate proper size from content
        // This ensures the dialog is sized to fit all content, not just a fixed 280x200
        pack();
        
        // Black stroke border is now included in the window background (createRoundedPanelWithStroke)
        // No need for separate addModalBorder() overlay
        
        Gdx.app.log("DropQuantityDialog", 
            "Dialog created - Size: " + getWidth() + "x" + getHeight() + 
            " - Position: (" + getX() + ", " + getY() + ")");
    }

    private void updateQuantityLabel() {
        Gdx.app.log("DropQuantityDialog", "Quantity changed to: " + selectedQuantity);
        quantityLabel.setText(String.valueOf(selectedQuantity));
        if (infoLabel != null) {
            // Use standard ASCII characters (no Unicode 'x' symbol) for compatibility
            infoLabel.setText("Drop: " + selectedQuantity + "x " + item.getName());
        }
    }

    /**
     * Set callback for when user confirms drop.
     * Receives the quantity to be dropped.
     */
    public void setConfirmCallback(Consumer<Integer> callback) {
        this.confirmCallback = callback;
    }

    /**
     * Set callback for when user cancels the drop.
     */
    public void setCancelCallback(Runnable callback) {
        this.cancelCallback = callback;
    }

    /**
     * Get the item being dropped.
     */
    public Item getItem() {
        return item;
    }

    /**
     * Get the maximum quantity available to drop.
     */
    public int getMaxQuantity() {
        return maxQuantity;
    }
    
    /**
     * Get the currently selected quantity.
     */
    public int getSelectedQuantity() {
        return selectedQuantity;
    }

    public boolean handleNavigationKey(int keycode) {
        switch (keycode) {
            case Input.Keys.A:
            case Input.Keys.LEFT:
                adjustQuantity(-1);
                return true;
            case Input.Keys.D:
            case Input.Keys.RIGHT:
                adjustQuantity(1);
                return true;
            case Input.Keys.W:
            case Input.Keys.UP:
            case Input.Keys.S:
            case Input.Keys.DOWN:
            case Input.Keys.ENTER:
            case Input.Keys.NUMPAD_ENTER:
            case Input.Keys.Z:
            case Input.Keys.SPACE:
            case Input.Keys.BUTTON_A:
                result(DIALOG_CONFIRMED);
                hide(null);
                return true;
            case Input.Keys.X:
            case Input.Keys.ESCAPE:
            case Input.Keys.BACKSPACE:
            case Input.Keys.BUTTON_B:
                result(DIALOG_CANCELLED);
                hide(null);
                return true;
            default:
                return false;
        }
    }

    private void adjustQuantity(int delta) {
        int next = Math.max(1, Math.min(maxQuantity, selectedQuantity + delta));
        if (next != selectedQuantity) {
            selectedQuantity = next;
            updateQuantityLabel();
        }
    }

    /**
     * Override Dialog.result() to handle callback execution.
     * This is called automatically when Dialog.button() action is triggered.
     * 
     * The Dialog framework automatically:
     * 1. Calls this result() method
     * 2. Calls hide() to dismiss the dialog
     * 3. Removes the dialog from stage
     */
    @Override
    protected void result(Object object) {
        Gdx.app.log("DropQuantityDialog", "result() called with: " + object);
        
        try {
            if (object == DIALOG_CONFIRMED) {
                Gdx.app.log("DropQuantityDialog", "CONFIRMED - dropping " + selectedQuantity + "x " + item.getName());
                if (confirmCallback != null) {
                    try {
                        confirmCallback.accept(selectedQuantity);
                        Gdx.app.log("DropQuantityDialog", "Confirm callback executed successfully");
                    } catch (Exception e) {
                        Gdx.app.error("DropQuantityDialog", "ERROR in confirm callback: " + e.getMessage(), e);
                    }
                }
            } else if (object == DIALOG_CANCELLED) {
                Gdx.app.log("DropQuantityDialog", "CANCELLED by user");
                if (cancelCallback != null) {
                    try {
                        cancelCallback.run();
                        Gdx.app.log("DropQuantityDialog", "Cancel callback executed successfully");
                    } catch (Exception e) {
                        Gdx.app.error("DropQuantityDialog", "ERROR in cancel callback: " + e.getMessage(), e);
                    }
                }
            } else {
                Gdx.app.log("DropQuantityDialog", "Unknown result: " + object);
            }
        } catch (Exception e) {
            Gdx.app.error("DropQuantityDialog", "FATAL ERROR in result(): " + e.getMessage(), e);
        }
    }
}
