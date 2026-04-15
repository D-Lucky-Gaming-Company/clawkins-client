package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.ItemTextureManager;
import github.dluckycompany.clawkins.item.Wallet;

/**
 * InventoryUI - Warm, High-Fidelity Two-Panel Layout
 * 
 * Color Palette (Custom HEX):
 * - Left Panel Background: #E7DAC7 (Warm Beige / Antique Paper)
 * - Right Panel Background: #C19253 (Warm Tan / Wood)
 * - Borders & Divider: #1F1A13 (Dark Coffee / Deep Brown)
 * - Text: #1F1A13 (Dark Coffee / Deep Brown)
 * 
 * Layout Structure:
 * ΓöîΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÉ
 * Γöé ΓåÉ BACK                             INVENTORY            Γöé
 * Γö£ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓö¼ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöñ
 * Γöé DETAILS [BEIGE]  Γöé ITEMS [TAN]                         Γöé
 * Γöé ΓÇó Item Name      Γöé ΓÇó Potion x3                         Γöé
 * Γöé ΓÇó Description    Γöé ΓÇó Full Heal x1                      Γöé
 * Γöé ΓÇó Type: Potion   Γöé ΓÇó Revive x2                         Γöé
 * Γöé ΓÇó [USE] [DROP]   Γöé (scrollable)                        Γöé
 * ΓööΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓö┤ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÿ
 * 
 * FitViewport Compatible: All positioning uses relative sizing
 */
public class InventoryUI {

    private final Stage stage;
    private final BitmapFont font;
    private final Inventory inventory;
    private final List<Clawkin> party;
    private final Skin skin;
    private final Wallet wallet;

    // Main layout table
    private Table rootTable;
    private Table itemListContainer;
    private Table detailPanel;
    private ScrollPane itemScrollPane;
    
    // Selected item tracking
    private Item selectedItem;
    private Table selectedItemRow;
    private final List<Item> navigableItems = new ArrayList<>();
    private final List<Table> navigableRows = new ArrayList<>();
    private int selectedIndex = -1;

    // Keyboard action mode: false=list navigation, true=USE/DROP selection.
    private boolean actionMode = false;
    private int actionIndex = 0; // 0=USE, 1=DROP
    private TextButton useBtnRef;
    private TextButton dropBtnRef;
    private PartySelectionDialog activePartyDialog;

    // Callbacks
    private Runnable onItemDropped;
    private Runnable onBackPressed;
    
    // Button drawables (loaded from assets/ui/buttons/)
    private TextureRegionDrawable backDrawable;
    private TextureRegionDrawable minusDrawable;
    private TextureRegionDrawable plusDrawable;
    private TextureRegionDrawable exitDrawable;

    // ============================================================
    // CUSTOM COLOR PALETTE (HEX VALUES)
    // ============================================================
    private static final Color PANEL_LEFT_BG = Color.valueOf("#E7DAC7");     // Warm Beige / Antique Paper
    private static final Color PANEL_RIGHT_BG = Color.valueOf("#C19253");    // Warm Tan / Wood
    private static final Color PANEL_DIVIDER = Color.valueOf("#1F1A13");     // Dark Coffee / Deep Brown
    private static final Color TEXT_PRIMARY = Color.valueOf("#1F1A13");      // Dark Coffee / Deep Brown
    private static final Color TEXT_HIGHLIGHT = Color.valueOf("#E7DAC7");    // Beige (for highlights)
    private static final Color BUTTON_BG = Color.valueOf("#C19253");         // Tan (button background)
    private static final Color BUTTON_TEXT = Color.valueOf("#1F1A13");       // Dark Brown (button text)

    // UI dimensions (relative to viewport)
    private static final float PADDING = 20f;
    private static final float TITLE_HEIGHT = 80f;
    private static final float DIVIDER_WIDTH = 2f;                 // 2px vertical divider

    /**
     * Constructor
     */
    public InventoryUI(Stage stage, BitmapFont font, Inventory inventory, List<Clawkin> party, Skin skin, Wallet wallet) {
        this.stage = stage;
        this.font = font;
        this.inventory = inventory;
        this.party = party;
        this.skin = skin;
        this.wallet = wallet;
        
        // Load button assets from PNG files
        loadButtonAssets();
    }

    /**
     * Load button textures from assets/ui/buttons/ and create drawables
     * Supports: back.png, exit.png, minus.png, plus.png
     * Uses TextureRegionDrawable for Scene2D integration
     */
    private void loadButtonAssets() {
        try {
            // Load back button (circular arrow for returning to world)
            backDrawable = new TextureRegionDrawable(
                new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/buttons/back.png"))
            );
            if (backDrawable == null) {
                Gdx.app.error("InventoryUI", "Failed to load back.png button texture");
            } else {
                Gdx.app.log("InventoryUI", "Successfully loaded back.png button");
            }
        } catch (Exception e) {
            Gdx.app.error("InventoryUI", "Error loading back button: " + e.getMessage());
        }

        try {
            // Load exit button (X for closing dialogs)
            exitDrawable = new TextureRegionDrawable(
                new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/buttons/exit.png"))
            );
            if (exitDrawable == null) {
                Gdx.app.error("InventoryUI", "Failed to load exit.png button texture");
            } else {
                Gdx.app.log("InventoryUI", "Successfully loaded exit.png button");
            }
        } catch (Exception e) {
            Gdx.app.error("InventoryUI", "Error loading exit button: " + e.getMessage());
        }

        try {
            // Load minus button (decrement quantity)
            minusDrawable = new TextureRegionDrawable(
                new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/buttons/minus.png"))
            );
            if (minusDrawable == null) {
                Gdx.app.error("InventoryUI", "Failed to load minus.png button texture");
            } else {
                Gdx.app.log("InventoryUI", "Successfully loaded minus.png button");
            }
        } catch (Exception e) {
            Gdx.app.error("InventoryUI", "Error loading minus button: " + e.getMessage());
        }

        try {
            // Load plus button (increment quantity)
            plusDrawable = new TextureRegionDrawable(
                new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("ui/buttons/plus.png"))
            );
            if (plusDrawable == null) {
                Gdx.app.error("InventoryUI", "Failed to load plus.png button texture");
            } else {
                Gdx.app.log("InventoryUI", "Successfully loaded plus.png button");
            }
        } catch (Exception e) {
            Gdx.app.error("InventoryUI", "Error loading plus button: " + e.getMessage());
        }
    }

    /**
     * Apply a smooth color fade effect to a button on interaction
     * On touchDown: Instantly darken to a shade of the theme tan color (#A07A46)
     * On touchUp: Fade smoothly back to original white/tan over 0.2 seconds
     * 
     * @param targetButton The TextButton to apply the fade effect to
     * @param fadeColor The color to fade to on press (e.g., #A07A46 darker tan)
     */
    private void applyFadeEffect(TextButton targetButton, Color fadeColor) {
        targetButton.setProgrammaticChangeEvents(false);  // Prevent internal state loops
        
        targetButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Instantly darken on touch
                targetButton.setColor(fadeColor);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Fade back to white smoothly over 0.2 seconds
                targetButton.addAction(Actions.sequence(
                    Actions.color(Color.WHITE, 0.2f, Interpolation.pow2Out)
                ));
            }
        });
    }

    /**
     * Apply a smooth color fade effect to an ImageButton on interaction
     * On touchDown: Quick gray tint (0.7f, 0.7f, 0.7f, 1f)
     * On touchUp: Fade smoothly back to white over 0.15 seconds with optional scale effect
     * 
     * @param imageButton The ImageButton to apply the fade effect to
     * @param includeScale Whether to add squish scaling effect (0.95f ΓåÆ 1f)
     */
    private void applyFadeEffectImageButton(ImageButton imageButton, boolean includeScale) {
        imageButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Quick gray tint on touch
                imageButton.setColor(0.7f, 0.7f, 0.7f, 1f);
                
                // Optional: Squish effect on press
                if (includeScale) {
                    imageButton.addAction(Actions.scaleTo(0.95f, 0.95f, 0.05f));
                }
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                // Fade back to white smoothly
                imageButton.addAction(Actions.sequence(
                    Actions.color(Color.WHITE, 0.15f, Interpolation.fade)
                ));
                
                // Optional: Scale back to normal
                if (includeScale) {
                    imageButton.addAction(Actions.scaleTo(1f, 1f, 0.1f));
                }
            }
        });
    }


    /**
     * Build the complete full-screen UI layout
     * Two-column design: LEFT (35% - beige background), RIGHT (65% - tan background)
     * 
     * MODAL LAYERING ARCHITECTURE:
     * ================================
     * This UI uses LibGDX's built-in Dialog class for modal overlays.
     * When a user clicks "USE" or "DROP":
     * 
     * 1. Dialog is instantiated and dialog.show(stage) is called
     * 2. Dialog framework automatically:
     *    a) Raises dialog above all existing actors (z-order)
     *    b) Dims the background (if stageBackground is set)
     *    c) Blocks input to background actors (setModal(true))
     *    d) Centers the dialog on screen
     * 3. Background inventory table REMAINS VISIBLE behind the dialog
     * 4. When dialog closes (Confirm/Cancel), callback fires
     * 5. refreshItemList() updates item counts without re-rendering
     * 
     * KEY PRINCIPLE: Never call itemListContainer.clearChildren()
     * before showing a dialog. The Dialog framework handles z-ordering.
     * 
     * UNIVERSAL ROUNDED CORNERS:
     * ================================
     * All UI sub-elements use RoundedPanelDrawable with NinePatch:
     * - Main panels: 12px radius (detail + item list)
     * - Item rows: 8px radius (each clickable item)
     * - Item icons: Wrapped in rounded containers
     * - Dialog background: 12px radius + 2px black stroke
     * - Selection highlight: 8px radius beige overlay
     */
    public void buildLayout() {
        // Create root table (fills entire stage)
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.setBackground(new ColorDrawable(PANEL_DIVIDER));  // Dark brown background
        rootTable.top();

        // ============================================================
        // HEADER ROW: Back Button + Title
        // ============================================================
        Table headerTable = new Table();
        headerTable.setBackground(new ColorDrawable(PANEL_DIVIDER));  // Dark brown header
        headerTable.pad(10f);
        headerTable.top().left();
        
        // Back button (using graphical asset from ui/buttons/back.png)
        ImageButton backBtn;
        if (backDrawable != null) {
            // Create ImageButton with graphical asset
            ImageButton.ImageButtonStyle backStyle = new ImageButton.ImageButtonStyle();
            backStyle.up = backDrawable;
            backStyle.down = backDrawable;  // Use same drawable - fade effect handles tinting
            backStyle.checked = backDrawable;
            backBtn = new ImageButton(backStyle);
        } else {
            // Fallback to text button if asset loading failed
            backBtn = new ImageButton(new ImageButton.ImageButtonStyle());
            Gdx.app.log("InventoryUI", "Back button texture failed to load, using fallback");
        }
        
        backBtn.setSize(40, 40);  // Match original PNG aspect ratio
        
        // Apply fade effect for smooth color transition
        applyFadeEffectImageButton(backBtn, true);  // Include scale effect
        
        // Add the action callback
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (onBackPressed != null) {
                    onBackPressed.run();
                }
            }
        });
        headerTable.add(backBtn).width(40).height(40).padRight(20f);
        
        // Title
        Label titleLabel = new Label("INVENTORY", new Label.LabelStyle(font, TEXT_HIGHLIGHT));
        titleLabel.setFontScale(2.5f);
        headerTable.add(titleLabel).expandX();
        
        // Money display (right side of header)
        if (wallet != null) {
            Label moneyLabel = new Label("Money: " + wallet.getMoney(), new Label.LabelStyle(font, TEXT_HIGHLIGHT));
            moneyLabel.setFontScale(1.8f);
            headerTable.add(moneyLabel).padRight(20f).right();
        }
        
        rootTable.add(headerTable).expandX().fillX().height(TITLE_HEIGHT).row();

        // ============================================================
        // MAIN CONTENT: Two-column layout with divider
        // ============================================================
        Table contentTable = new Table();
        contentTable.setBackground(new ColorDrawable(PANEL_DIVIDER));  // Dark divider background
        
        // ============ LEFT COLUMN (35%): Item Details - BEIGE with Rounded Corners ============
        detailPanel = new Table();
        // Apply rounded corner background (12px radius)
        detailPanel.setBackground(RoundedPanelDrawable.createRoundedPanel(PANEL_LEFT_BG, 12));
        detailPanel.pad(PADDING + 6);  // Extra padding for rounded corner clearance
        detailPanel.top().left();
        
        Label selectPrompt = new Label("Select an item to view details", 
            new Label.LabelStyle(font, TEXT_PRIMARY));
        selectPrompt.setFontScale(1.4f);
        selectPrompt.setWrap(true);
        detailPanel.add(selectPrompt).expand().fill();
        
        // Add left panel to content table (35% width)
        contentTable.add(detailPanel).width(280).expandY().fillY();
        
        // ============ VERTICAL DIVIDER (2px dark brown line) ============
        Table divider = new Table();
        divider.setBackground(new ColorDrawable(PANEL_DIVIDER));
        contentTable.add(divider).width(DIVIDER_WIDTH).expandY().fillY();
        
        // ============ RIGHT COLUMN (65%): Item List - TAN with Rounded Corners ============
        Table rightColumn = new Table();
        // Apply rounded corner background (12px radius)
        rightColumn.setBackground(RoundedPanelDrawable.createRoundedPanel(PANEL_RIGHT_BG, 12));
        rightColumn.top().left();
        rightColumn.pad(16f);  // Extra padding for rounded corner clearance
        
        // Items title
        Label itemsTitle = new Label("ITEMS", new Label.LabelStyle(font, TEXT_PRIMARY));
        itemsTitle.setFontScale(2.0f);
        rightColumn.add(itemsTitle).expandX().fillX().padBottom(15f).row();
        
        // Item list container with scroll pane
        itemListContainer = new Table();
        itemListContainer.top().left();

        itemScrollPane = new ScrollPane(itemListContainer);
        itemScrollPane.setStyle(createScrollPaneStyle());
        itemScrollPane.setScrollingDisabled(true, false);
        
        rightColumn.add(itemScrollPane).expand().fill();
        
        // Add right panel to content table (65% width)
        contentTable.add(rightColumn).expand().fill();
        
        rootTable.add(contentTable).expand().fill().row();

        // Populate item list
        populateItemList();

        // Add root table to stage
        stage.addActor(rootTable);
    }

    /**
     * Create custom ScrollPane style for the item list
     * Transparent background to show the tan panel underneath
     */
    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        // Transparent background - let tan panel show through
        style.background = null;
        style.hScroll = null;  // Hide horizontal scrollbar
        style.vScroll = null;  // Hide vertical scrollbar
        return style;
    }

    /**
     * Populate item list from inventory with clickable items
     * CRITICAL: This must clear old items AND repopulate with new ones atomically
     */
    private void populateItemList() {
        itemListContainer.clearChildren();
        navigableItems.clear();
        navigableRows.clear();
        selectedIndex = -1;
        selectedItem = null;
        selectedItemRow = null;
        actionMode = false;
        actionIndex = 0;
        useBtnRef = null;
        dropBtnRef = null;

        if (inventory == null || inventory.getAllItems().isEmpty()) {
            Label emptyLabel = new Label("No items", new Label.LabelStyle(font, TEXT_PRIMARY));
            emptyLabel.setFontScale(1.8f);
            itemListContainer.add(emptyLabel).pad(20f).row();
        } else {
            // Add each item as a clickable row with rounded background
            for (int i = 0; i < inventory.getAllItems().size(); i++) {
                Item item = inventory.getAllItems().get(i);
                int quantity = inventory.getQuantity(item);
                createClickableItemRow(item, quantity, i);
            }

            if (!navigableItems.isEmpty()) {
                selectedIndex = 0;
                selectItem(navigableItems.get(0), navigableRows.get(0));
            }
        }
        
        // CRITICAL: Force layout recalculation to ensure items appear
        itemListContainer.invalidateHierarchy();
    }

    /**
     * Create a clickable item row with rounded corners
     * Styled with tan rounded background and dark brown text
     * Layout: Image (left) | Name + Quantity (right)
     */
    private void createClickableItemRow(Item item, int quantity, int index) {
        final Table itemRow = new Table();
        // Apply rounded corner background instead of sharp rectangle (8px radius)
        itemRow.setBackground(RoundedPanelDrawable.createRoundedPanel(BUTTON_BG, 8));
        itemRow.pad(8f);  // Increased padding for rounded corner clearance
        itemRow.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        // Create a container table for image with rounded corners
        Table imageContainer = new Table();
        imageContainer.setBackground(RoundedPanelDrawable.createRoundedPanel(PANEL_LEFT_BG, 6));  // 6px radius beige rounded
        imageContainer.pad(5f);
        
        // Load and display item image
        ItemTextureManager textureManager = ItemTextureManager.getInstance();
        Image itemImage = new Image(textureManager.getItemTexture(item.getImageName()));
        imageContainer.add(itemImage).width(50f).height(50f).center();
        
        itemRow.add(imageContainer).width(60f).height(60f).padRight(10f);

        // Create container for text (name + quantity)
        Table textContainer = new Table();
        textContainer.left().center();
        
        // Item name (left-aligned)
        Label itemNameLabel = new Label(item.getName(), new Label.LabelStyle(font, BUTTON_TEXT));
        itemNameLabel.setFontScale(1.5f);
        textContainer.add(itemNameLabel).expandX().left();

        // Quantity (right-aligned, formatted as xN)
        String quantityText = "x" + quantity;
        Label quantityLabel = new Label(quantityText, new Label.LabelStyle(font, BUTTON_TEXT));
        quantityLabel.setFontScale(1.4f);
        textContainer.add(quantityLabel).right().padLeft(10f);
        
        itemRow.add(textContainer).expandX().fillX().height(60f);

        // Add click listener to select item
        itemRow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedIndex = index;
                selectItem(item, itemRow);
            }
        });

        navigableItems.add(item);
        navigableRows.add(itemRow);
        itemListContainer.add(itemRow).expandX().fillX().height(70f).padBottom(5f).row();
    }

    /**
     * Select an item and update the detail panel
     * Selected items highlight with beige rounded background
     */
    private void selectItem(Item item, Table itemRow) {
        // Clear previous selection highlight (back to rounded tan)
        if (selectedItemRow != null) {
            selectedItemRow.setBackground(RoundedPanelDrawable.createRoundedPanel(BUTTON_BG, 8));
        }

        // Set new selection (highlight with rounded beige)
        selectedItem = item;
        selectedItemRow = itemRow;
        selectedItemRow.setBackground(RoundedPanelDrawable.createRoundedPanel(TEXT_HIGHLIGHT, 8));  // Beige rounded highlight

        if (itemScrollPane != null && selectedItemRow != null) {
            itemScrollPane.scrollTo(
                selectedItemRow.getX(),
                selectedItemRow.getY(),
                selectedItemRow.getWidth(),
                selectedItemRow.getHeight(),
                false,
                true
            );
        }

        // Update detail panel
        updateDetailPanel(item);
    }

    /**
     * Handle keyboard navigation in the inventory list.
     * Supports both WASD and arrow keys.
     */
    public boolean handleNavigationKey(int keycode) {
        if (activePartyDialog != null) {
            return activePartyDialog.handleNavigationKey(keycode);
        }

        switch (keycode) {
            case Input.Keys.W, Input.Keys.UP -> {
                actionMode = false;
                updateActionButtonHighlight();
                navigateSelection(-1);
                return true;
            }
            case Input.Keys.S, Input.Keys.DOWN -> {
                actionMode = false;
                updateActionButtonHighlight();
                navigateSelection(1);
                return true;
            }
            case Input.Keys.A, Input.Keys.LEFT -> {
                if (actionMode) {
                    actionIndex = 0;
                    updateActionButtonHighlight();
                } else {
                    navigateSelection(-1);
                }
                return true;
            }
            case Input.Keys.D, Input.Keys.RIGHT -> {
                if (actionMode) {
                    actionIndex = 1;
                    updateActionButtonHighlight();
                } else {
                    navigateSelection(1);
                }
                return true;
            }
            case Input.Keys.ENTER -> {
                // Enter should mirror click flow: select item, then choose USE/DROP, then confirm.
                if (navigableItems.isEmpty()) {
                    return true;
                }

                if (selectedIndex < 0 || selectedIndex >= navigableItems.size()) {
                    selectedIndex = 0;
                }

                if (!actionMode) {
                    selectItem(navigableItems.get(selectedIndex), navigableRows.get(selectedIndex));
                    actionMode = true;
                    actionIndex = 0;
                    updateActionButtonHighlight();
                    return true;
                }

                if (actionIndex == 0) {
                    triggerUseAction();
                } else {
                    triggerDropAction();
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void navigateSelection(int delta) {
        if (navigableItems.isEmpty()) {
            return;
        }

        if (selectedIndex < 0 || selectedIndex >= navigableItems.size()) {
            selectedIndex = 0;
        } else {
            selectedIndex = (selectedIndex + delta + navigableItems.size()) % navigableItems.size();
        }

        selectItem(navigableItems.get(selectedIndex), navigableRows.get(selectedIndex));
    }

    /**
     * Update the left detail panel with selected item information
     * Uses beige background with dark brown text
     * Displays item image prominently at the top
     */
    private void updateDetailPanel(Item item) {
        detailPanel.clearChildren();
        detailPanel.top().left();

        // Item image container (centered with rounded corners)
        // Wraps the item icon in a rounded rectangular background for visual hierarchy
        Table imageContainer = new Table();
        imageContainer.setBackground(RoundedPanelDrawable.createRoundedPanel(PANEL_DIVIDER, 8));  // 8px radius dark brown rounded
        imageContainer.pad(10f);
        imageContainer.center();
        
        ItemTextureManager textureManager = ItemTextureManager.getInstance();
        Image itemImage = new Image(textureManager.getItemTexture(item.getImageName()));
        imageContainer.add(itemImage).width(80f).height(80f);
        
        detailPanel.add(imageContainer).expandX().fillX().height(100f).padBottom(15f).row();

        // Item name (large, dark brown, bold-ish)
        Label nameLabel = new Label(item.getName(), new Label.LabelStyle(font, TEXT_PRIMARY));
        nameLabel.setFontScale(2.2f);
        detailPanel.add(nameLabel).expandX().fillX().padBottom(15f).row();

        // Item description (wrapped text)
        Label descLabel = new Label(item.getDescription(), new Label.LabelStyle(font, TEXT_PRIMARY));
        descLabel.setFontScale(1.3f);
        descLabel.setWrap(true);
        detailPanel.add(descLabel).expandX().fillX().padBottom(15f).row();

        // Item type
        Label typeLabel = new Label("Type: " + item.getType(), new Label.LabelStyle(font, TEXT_PRIMARY));
        typeLabel.setFontScale(1.2f);
        detailPanel.add(typeLabel).expandX().fillX().padBottom(20f).row();

        // Spacer to push buttons to bottom
        detailPanel.add().expand().row();

        // USE button (tan background with rounded corners, dark brown text)
        TextButton useBtn = new TextButton("USE", 
            new TextButton.TextButtonStyle(
                RoundedPanelDrawable.createRoundedPanel(BUTTON_BG, 8),
                null,  // No down drawable - fade animation handles highlighting
                null,  // No checked drawable
                font
            )
        );
        useBtn.getLabel().setColor(BUTTON_TEXT);
        useBtn.getLabel().setFontScale(1.4f);
        
        // Apply fade effect for smooth color transition
        Color darkTan = Color.valueOf("#A07A46");  // Darker tan for fade effect
        applyFadeEffect(useBtn, darkTan);
        
        // Add the action callback
        useBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                triggerUseAction();
            }
        });
        detailPanel.add(useBtn).expandX().fillX().height(50).padBottom(5f).row();

        // DROP button (tan background with rounded corners, dark brown text)
        TextButton dropBtn = new TextButton("DROP",
            new TextButton.TextButtonStyle(
                RoundedPanelDrawable.createRoundedPanel(BUTTON_BG, 8),
                null,  // No down drawable - fade animation handles highlighting
                null,  // No checked drawable
                font
            )
        );
        dropBtn.getLabel().setColor(BUTTON_TEXT);
        dropBtn.getLabel().setFontScale(1.4f);
        
        // Apply fade effect for smooth color transition
        applyFadeEffect(dropBtn, darkTan);
        
        // Add the action callback
        dropBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                triggerDropAction();
            }
        });
        detailPanel.add(dropBtn).expandX().fillX().height(50).row();

        useBtnRef = useBtn;
        dropBtnRef = dropBtn;
        updateActionButtonHighlight();
    }

    private void triggerUseAction() {
        if (selectedItem != null && party != null && !party.isEmpty()) {
            PartySelectionDialog dialog = new PartySelectionDialog(
                party,
                selectedItem,
                inventory,
                skin,
                font
            );
            dialog.pad(20f);

            dialog.setOnItemApplied(() -> {
                refreshItemList();
                selectedItem = null;
                selectedItemRow = null;
            });

            dialog.setOnClosed(() -> activePartyDialog = null);
            activePartyDialog = dialog;
            dialog.show(stage);
        }
    }

    private void triggerDropAction() {
        if (selectedItem != null) {
            int quantity = inventory.getQuantity(selectedItem);
            if (quantity > 0) {
                DropQuantityDialog dialog = new DropQuantityDialog(selectedItem, quantity, skin,
                    minusDrawable, plusDrawable, exitDrawable);
                dialog.pad(20f);
                dialog.show(stage);

                dialog.setConfirmCallback((dropAmount) -> {
                    boolean removed = inventory.removeItem(selectedItem, dropAmount);
                    if (removed) {
                        com.badlogic.gdx.Gdx.app.log("InventoryUI",
                            "[Item Drop] Dropped " + dropAmount + "x " + selectedItem.getName());

                        refreshItemList();
                        selectedItem = null;
                        selectedItemRow = null;

                        if (onItemDropped != null) {
                            onItemDropped.run();
                        }
                    } else {
                        com.badlogic.gdx.Gdx.app.error("InventoryUI",
                            "Failed to drop " + dropAmount + "x " + selectedItem.getName());
                    }
                });

                dialog.setCancelCallback(() -> {
                    Gdx.app.log("InventoryUI", "[Item Drop] Cancelled by user");
                    refreshItemList();
                });
            }
        }
    }

    private void updateActionButtonHighlight() {
        if (useBtnRef == null || dropBtnRef == null) {
            return;
        }

        if (!actionMode) {
            useBtnRef.setColor(Color.WHITE);
            dropBtnRef.setColor(Color.WHITE);
            return;
        }

        useBtnRef.setColor(actionIndex == 0 ? Color.valueOf("#A07A46") : Color.WHITE);
        dropBtnRef.setColor(actionIndex == 1 ? Color.valueOf("#A07A46") : Color.WHITE);
    }

    /**
     * Get the minus button drawable for use in dialogs
     */
    public TextureRegionDrawable getMinusDrawable() {
        return minusDrawable;
    }

    /**
     * Get the plus button drawable for use in dialogs
     */
    public TextureRegionDrawable getPlusDrawable() {
        return plusDrawable;
    }

    /**
     * Get the exit button drawable for use in dialogs
     */
    public TextureRegionDrawable getExitDrawable() {
        return exitDrawable;
    }

    /**
     * Get the currently selected item
     */
    public Item getSelectedItem() {
        return selectedItem;
    }

    /**
     * Set callback for back button press
     */
    public void setOnBackPressed(Runnable callback) {
        this.onBackPressed = callback;
    }

    /**
     * Set callback for item drop (optional)
     */
    public void setOnItemDropped(Runnable callback) {
        this.onItemDropped = callback;
    }

    /**
     * Refresh the item list with proper hierarchy invalidation
     * CRITICAL: Must be called after inventory changes to ensure items reappear
     * Call this method after:
     * - Dialog closures (USE, DROP)
     * - Item consumption/removal
     * - Any inventory modification
     */
    public void refreshItemList() {
        populateItemList();  // Clear and repopulate atomically
        itemListContainer.invalidateHierarchy();  // Force layout recalculation
        
        // Log for debugging
        Gdx.app.log("InventoryUI", "[Refresh] Item list repopulated with " + 
            (inventory != null ? inventory.getAllItems().size() : 0) + " items");
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        if (rootTable != null) {
            rootTable.remove();
        }
    }
}
