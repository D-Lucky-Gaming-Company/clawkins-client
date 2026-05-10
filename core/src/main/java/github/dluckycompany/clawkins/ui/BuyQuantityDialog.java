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
 * HIGH-FIDELITY MODAL BUY DIALOG
 * 
 * A modern, theme-consistent "Buy Quantity" dialog featuring:
 * - Custom antique color palette (#E7DAC7, #1F1A13, #C19253)
 * - Rounded corners with NinePatch (12px radius)
 * - Mathematically centered quantity controls
 * - Professional RPG aesthetic
 * - Modal behavior with proper input handling
 * - Price calculation display with affordability checking
 * 
 * Uses Dialog.show(stage) for proper modality and input handling.
 */
public class BuyQuantityDialog extends Dialog {
    // ============================================================
    // CUSTOM COLOR PALETTE (HEX VALUES)
    // ============================================================
    private static final Color DIALOG_BG = Color.valueOf("#E7DAC7");        // Warm Beige / Antique Paper
    private static final Color TEXT_PRIMARY = Color.valueOf("#1F1A13");     // Dark Coffee / Deep Brown
    
    private final Item item;
    private final int maxAffordableQuantity;
    private final int maxStockQuantity;
    private final int buyPricePerItem;
    private final BitmapFont font;
    private int selectedQuantity = 1;
    private Label quantityLabel;
    private Label totalPriceLabel;
    private Label infoLabel;
    private Consumer<Integer> confirmCallback;
    private Runnable cancelCallback;
    
    // Button drawables for graphical buttons
    private final TextureRegionDrawable minusDrawable;
    private final TextureRegionDrawable plusDrawable;
    
    // Dialog result constants
    private static final Integer DIALOG_CONFIRMED = 1;
    private static final Integer DIALOG_CANCELLED = 0;

    public BuyQuantityDialog(Item item, int maxAffordableQuantity, int maxStockQuantity, int buyPricePerItem, Skin skin, 
                            TextureRegionDrawable minusDrawable,
                            TextureRegionDrawable plusDrawable,
                            TextureRegionDrawable exitDrawable) {
        super("Buy " + item.getName(), createWindowStyle(skin));
        this.item = item;
        this.maxAffordableQuantity = Math.max(1, maxAffordableQuantity);
        this.maxStockQuantity = Math.max(1, maxStockQuantity);
        this.buyPricePerItem = buyPricePerItem;
        this.minusDrawable = minusDrawable;
        this.plusDrawable = plusDrawable;
        
        // CRITICAL: Use safe font access to prevent crashes
        BitmapFont retrievedFont = UISkinManager.getSafeFont(skin, "default");
        if (retrievedFont == null) {
            Gdx.app.error("BuyQuantityDialog", "FATAL: Font is null after safe access, creating fallback");
            retrievedFont = new BitmapFont();  // Last-resort fallback
        }
        this.font = retrievedFont;
        
        setModal(true);
        setMovable(false);
        setResizable(false);
        
        buildUI();
    }

    /**
     * Create a custom WindowStyle with the antique color palette
     * SAFE: Uses UISkinManager for font access
     */
    private static Window.WindowStyle createWindowStyle(Skin skin) {
        Window.WindowStyle style = new Window.WindowStyle();
        
        // Apply rounded corner background with black stroke
        style.background = RoundedPanelDrawable.createRoundedPanelWithStroke(DIALOG_BG, 12, 2);
        
        // CRITICAL: Safe font access
        BitmapFont titleFont = UISkinManager.getSafeFont(skin, "default");
        if (titleFont != null) {
            style.titleFont = titleFont;
        } else {
            Gdx.app.error("BuyQuantityDialog", "WARNING: titleFont is null, using fallback");
            style.titleFont = new BitmapFont();
        }
        
        style.titleFontColor = TEXT_PRIMARY;
        
        return style;
    }

    /**
     * Apply a smooth color fade effect to an ImageButton on interaction
     */
    private void applyImageButtonFade(ImageButton imageButton) {
        imageButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                imageButton.setColor(0.7f, 0.7f, 0.7f, 1f);
                imageButton.addAction(Actions.scaleTo(0.95f, 0.95f, 0.05f));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                imageButton.addAction(Actions.sequence(
                    Actions.color(Color.WHITE, 0.15f, Interpolation.fade)
                ));
                imageButton.addAction(Actions.scaleTo(1f, 1f, 0.1f));
            }
        });
    }

    /**
     * Create a high-fidelity rounded button style
     */
    private TextButton.TextButtonStyle getRoundedButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = RoundedPanelDrawable.createRoundedPanel(Color.valueOf("#E7DAC7"), 8);
        style.over = RoundedPanelDrawable.createRoundedPanel(Color.valueOf("#C19253"), 8);
        style.down = style.over;
        style.checked = style.over;
        style.fontColor = Color.valueOf("#1F1A13");
        style.font = font;
        return style;
    }

    /**
     * Apply a smooth color fade effect to a TextButton
     */
    private void applyTextButtonFade(TextButton textButton) {
        textButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                textButton.addAction(Actions.scaleTo(0.95f, 0.95f, 0.05f));
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                textButton.addAction(Actions.scaleTo(1f, 1f, 0.1f, Interpolation.pow2Out));
            }
        });
    }

    private void buildUI() {
        Table contentTable = getContentTable();
        contentTable.clear();
        contentTable.pad(20);  // Proper padding to prevent text touching edges
        
        // ============================================================
        // QUANTITY SELECTION CONTROLS
        // [Minus Button] | [Centered Quantity Label] | [Plus Button]
        // ============================================================
        Table quantityTable = new Table();
        quantityTable.pad(15);
        
        // MINUS button
        ImageButton minusButton;
        if (minusDrawable != null) {
            ImageButton.ImageButtonStyle minusStyle = new ImageButton.ImageButtonStyle();
            minusStyle.up = minusDrawable;
            minusStyle.down = minusDrawable;
            minusStyle.checked = minusDrawable;
            minusButton = new ImageButton(minusStyle);
        } else {
            minusButton = new ImageButton(new ImageButton.ImageButtonStyle());
            Gdx.app.log("BuyQuantityDialog", "Minus button texture failed to load");
        }
        minusButton.setSize(40, 40);
        applyImageButtonFade(minusButton);
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
        
        // CENTERED QUANTITY LABEL
        quantityLabel = new Label("1", new Label.LabelStyle(font, TEXT_PRIMARY));
        quantityLabel.setFontScale(2.0f);
        quantityLabel.setAlignment(Align.center);
        quantityTable.add(quantityLabel).expandX().fillX().padRight(15);
        
        // PLUS button
        ImageButton plusButton;
        if (plusDrawable != null) {
            ImageButton.ImageButtonStyle plusStyle = new ImageButton.ImageButtonStyle();
            plusStyle.up = plusDrawable;
            plusStyle.down = plusDrawable;
            plusStyle.checked = plusDrawable;
            plusButton = new ImageButton(plusStyle);
        } else {
            plusButton = new ImageButton(new ImageButton.ImageButtonStyle());
            Gdx.app.log("BuyQuantityDialog", "Plus button texture failed to load");
        }
        plusButton.setSize(40, 40);
        applyImageButtonFade(plusButton);
        plusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                int maxQuantity = Math.min(maxAffordableQuantity, maxStockQuantity);
                if (selectedQuantity < maxQuantity) {
                    selectedQuantity++;
                    updateQuantityLabel();
                }
            }
        });
        quantityTable.add(plusButton).width(40).height(40);
        
        contentTable.add(quantityTable).expandX().fillX().height(70).padBottom(25).row();
        
        // ============================================================
        // INFO LABEL (Shows buy summary with price)
        // ============================================================
        infoLabel = new Label("Buy: 1x " + item.getName(), new Label.LabelStyle(font, TEXT_PRIMARY));
        infoLabel.setFontScale(1.3f);
        infoLabel.setWrap(true);
        infoLabel.setAlignment(Align.center);
        contentTable.add(infoLabel).expandX().fillX().padBottom(10).row();
        
        // Total price label
        totalPriceLabel = new Label("Total: " + buyPricePerItem + " gold", new Label.LabelStyle(font, TEXT_PRIMARY));
        totalPriceLabel.setFontScale(1.5f);
        totalPriceLabel.setColor(Color.GOLD);
        totalPriceLabel.setAlignment(Align.center);
        contentTable.add(totalPriceLabel).expandX().fillX().padBottom(20).row();
        
        // ============================================================
        // DIALOG BUTTONS (Confirm / Cancel)
        // ============================================================
        TextButton.TextButtonStyle roundedButtonStyle = getRoundedButtonStyle();
        
        TextButton confirmButton = new TextButton("Confirm", roundedButtonStyle);
        confirmButton.getLabel().setFontScale(1.4f);
        applyTextButtonFade(confirmButton);
        
        TextButton cancelButton = new TextButton("Cancel", roundedButtonStyle);
        cancelButton.getLabel().setFontScale(1.4f);
        applyTextButtonFade(cancelButton);
        
        button(confirmButton, DIALOG_CONFIRMED).padRight(10);
        button(cancelButton, DIALOG_CANCELLED);
        
        // Update title styling
        getTitleLabel().setColor(TEXT_PRIMARY);
        getTitleLabel().setAlignment(Align.center);
        
        // Apply padding to button table
        getButtonTable().pad(20);
        getButtonTable().padTop(10);
        
        // Pack the dialog to calculate proper size
        pack();
        
        Gdx.app.log("BuyQuantityDialog", 
            "Dialog created - Size: " + getWidth() + "x" + getHeight() + 
            " - Max affordable: " + maxAffordableQuantity + 
            " - Max stock: " + maxStockQuantity);
    }

    private void updateQuantityLabel() {
        Gdx.app.log("BuyQuantityDialog", "Quantity changed to: " + selectedQuantity);
        quantityLabel.setText(String.valueOf(selectedQuantity));
        if (infoLabel != null) {
            infoLabel.setText("Buy: " + selectedQuantity + "x " + item.getName());
        }
        if (totalPriceLabel != null) {
            int totalPrice = selectedQuantity * buyPricePerItem;
            totalPriceLabel.setText("Total: " + totalPrice + " gold");
        }
    }

    public void setConfirmCallback(Consumer<Integer> callback) {
        this.confirmCallback = callback;
    }

    public void setCancelCallback(Runnable callback) {
        this.cancelCallback = callback;
    }

    public Item getItem() {
        return item;
    }

    public int getMaxAffordableQuantity() {
        return maxAffordableQuantity;
    }
    
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
        int maxQuantity = Math.min(maxAffordableQuantity, maxStockQuantity);
        int next = Math.max(1, Math.min(maxQuantity, selectedQuantity + delta));
        if (next != selectedQuantity) {
            selectedQuantity = next;
            updateQuantityLabel();
        }
    }

    @Override
    protected void result(Object object) {
        Gdx.app.log("BuyQuantityDialog", "result() called with: " + object);
        
        try {
            if (object == DIALOG_CONFIRMED) {
                Gdx.app.log("BuyQuantityDialog", "CONFIRMED - buying " + selectedQuantity + "x " + item.getName());
                if (confirmCallback != null) {
                    try {
                        confirmCallback.accept(selectedQuantity);
                        Gdx.app.log("BuyQuantityDialog", "Confirm callback executed successfully");
                    } catch (Exception e) {
                        Gdx.app.error("BuyQuantityDialog", "ERROR in confirm callback: " + e.getMessage(), e);
                    }
                }
            } else if (object == DIALOG_CANCELLED) {
                Gdx.app.log("BuyQuantityDialog", "CANCELLED by user");
                if (cancelCallback != null) {
                    try {
                        cancelCallback.run();
                        Gdx.app.log("BuyQuantityDialog", "Cancel callback executed successfully");
                    } catch (Exception e) {
                        Gdx.app.error("BuyQuantityDialog", "ERROR in cancel callback: " + e.getMessage(), e);
                    }
                }
            } else {
                Gdx.app.log("BuyQuantityDialog", "Unknown result: " + object);
            }
        } catch (Exception e) {
            Gdx.app.error("BuyQuantityDialog", "FATAL ERROR in result(): " + e.getMessage(), e);
        }
    }
}
