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
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.input.InputConventions;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.ItemTextureManager;
import github.dluckycompany.clawkins.item.MerchantInventory;
import github.dluckycompany.clawkins.item.Wallet;

/**
 * MerchantShopUI - Warm, High-Fidelity Two-Panel Layout (Merchant Shop)
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
public class MerchantShopUI {

    private final Stage stage;
    private final BitmapFont font;
    private final Inventory playerInventory;  // Player's inventory (for selling)
    private final MerchantInventory merchantInventory;  // Merchant's inventory (for buying)
    private final List<Clawkin> party;
    private final Skin skin;
    private final Wallet wallet;
    private final AudioService audioService;
    private final boolean battleContext;
    private final String merchantName;
    
    // Shop mode: BUY or SELL
    private enum ShopMode { BUY, SELL }
    private ShopMode currentMode = ShopMode.BUY;  // Default to buying from merchant

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
    private int lastSelectedIndex = -1;  // For debouncing navigation SFX

    // Keyboard action mode: false=list navigation, true=USE/DROP selection.
    private boolean actionMode = false;
    private int actionIndex = 0; // 0=USE, 1=DROP
    private TextButton useBtnRef;
    private TextButton dropBtnRef;
    private TextButton buyTabRef;  // Store BUY tab reference for highlighting
    private TextButton sellTabRef;  // Store SELL tab reference for highlighting
    private PartySelectionDialog activePartyDialog;
    private DropQuantityDialog activeDropDialog;
    private BuyQuantityDialog activeBuyDialog;
    private Dialog activeModalDialog;
    private Runnable activeModalOnClose;
    
    // Wallet label reference for immediate updates
    private Label walletLabel;

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
    private static final Color TEXT_MUTED = Color.valueOf("#5A4B39");        // Muted dark brown for helper text
    private static final Color BUTTON_BG = Color.valueOf("#C19253");         // Tan (button background)
    private static final Color BUTTON_TEXT = Color.valueOf("#1F1A13");       // Dark Brown (button text)
    private static final Color ROW_SELECTED_BG = Color.valueOf("#ECCD61");   // Warm yellow selected state
    private static final Color ROW_HOVER_BG = Color.valueOf("#CFA66B");      // Slightly lighter tan hover
    private static final Color ACTION_SELECTED_TINT = Color.valueOf("#ECCD61");
    private static final Color HEALTH_ITEM_TINT = new Color(0.34f, 0.78f, 0.45f, 0.22f);
    private static final Color STAT_BOOST_ITEM_TINT = new Color(0.86f, 0.34f, 0.34f, 0.20f);
    private static final Color DEFENSE_ITEM_TINT = new Color(0.33f, 0.53f, 0.92f, 0.24f);
    private static final Color MODAL_BG = Color.valueOf("#E7DAC7");

    // UI dimensions (relative to viewport)
    private static final float PADDING = 20f;
    private static final float TITLE_HEIGHT = 80f;
    private static final float DIVIDER_WIDTH = 2f;                 // 2px vertical divider

    /**
     * Constructor
     */
    public MerchantShopUI(Stage stage, BitmapFont font, Inventory playerInventory, MerchantInventory merchantInventory, List<Clawkin> party, Skin skin, Wallet wallet, AudioService audioService, String merchantName, boolean battleContext) {
        this.stage = stage;
        this.font = font;
        this.playerInventory = playerInventory;
        this.merchantInventory = merchantInventory;
        this.party = party;
        this.skin = skin;
        this.wallet = wallet;
        this.audioService = audioService;
        this.merchantName = merchantName != null ? merchantName : "Merchant";
        this.battleContext = battleContext;
        
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
                // Play back/cancel sound
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_BACK);
                }
                if (onBackPressed != null) {
                    onBackPressed.run();
                }
            }
        });
        headerTable.add(backBtn).width(40).height(40).padRight(20f);
        
        // Title
        Label titleLabel = new Label(merchantName.toUpperCase() + "'S SHOP", new Label.LabelStyle(font, TEXT_HIGHLIGHT));
        titleLabel.setFontScale(2.5f);
        headerTable.add(titleLabel).expandX();
        
        // Money display (right side of header)
        walletLabel = null;
        if (wallet != null) {
            walletLabel = new Label("Money: " + wallet.getMoney(), new Label.LabelStyle(font, TEXT_HIGHLIGHT));
            walletLabel.setFontScale(1.8f);
            headerTable.add(walletLabel).padRight(20f).right();
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
        detailPanel.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(PANEL_LEFT_BG, 12, 1));
        detailPanel.pad(PADDING + 6);  // Extra padding for rounded corner clearance
        detailPanel.top().left();
        
        showSelectionPrompt();
        
        // Add left panel to content table (35% width)
        contentTable.add(detailPanel).width(280).expandY().fillY();
        
        // ============ VERTICAL DIVIDER (2px dark brown line) ============
        Table divider = new Table();
        divider.setBackground(new ColorDrawable(PANEL_DIVIDER));
        contentTable.add(divider).width(DIVIDER_WIDTH).expandY().fillY();
        
        // ============ RIGHT COLUMN (65%): Item List - TAN with Rounded Corners ============
        Table rightColumn = new Table();
        // Apply rounded corner background (12px radius)
        rightColumn.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(PANEL_RIGHT_BG, 12, 1));
        rightColumn.top().left();
        rightColumn.pad(16f);  // Extra padding for rounded corner clearance
        
        // BUY/SELL tabs
        Table tabsTable = new Table();
        
        buyTabRef = new TextButton("BUY", new TextButton.TextButtonStyle(
            RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(BUTTON_BG, 8, 1, 0.85f),
            null, null, font
        ));
        buyTabRef.getLabel().setColor(BUTTON_TEXT);
        buyTabRef.getLabel().setFontScale(1.4f);
        buyTabRef.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentMode != ShopMode.BUY) {
                    currentMode = ShopMode.BUY;
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_SELECT);
                    }
                    updateTabHighlights();
                    populateItemList();
                }
            }
        });
        
        sellTabRef = new TextButton("SELL", new TextButton.TextButtonStyle(
            RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(BUTTON_BG, 8, 1, 0.85f),
            null, null, font
        ));
        sellTabRef.getLabel().setColor(BUTTON_TEXT);
        sellTabRef.getLabel().setFontScale(1.4f);
        sellTabRef.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentMode != ShopMode.SELL) {
                    currentMode = ShopMode.SELL;
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_SELECT);
                    }
                    updateTabHighlights();
                    populateItemList();
                }
            }
        });
        
        // Set initial highlight
        updateTabHighlights();
        
        tabsTable.add(buyTabRef).width(120).height(40).padRight(10);
        tabsTable.add(sellTabRef).width(120).height(40);
        rightColumn.add(tabsTable).expandX().padBottom(15f).row();
        
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
     * Populate item list based on current shop mode (BUY or SELL)
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

        List<Item> itemsToDisplay;
        if (currentMode == ShopMode.BUY) {
            // Show merchant's items for buying
            itemsToDisplay = merchantInventory != null ? merchantInventory.getAllItems() : new ArrayList<>();
        } else {
            // Show player's items for selling
            itemsToDisplay = playerInventory != null ? playerInventory.getAllItems() : new ArrayList<>();
        }

        if (itemsToDisplay.isEmpty()) {
            String emptyMessage = currentMode == ShopMode.BUY 
                ? "This merchant has nothing to sell." 
                : "No items in your bag.";
            Label emptyLabel = new Label(emptyMessage, new Label.LabelStyle(font, TEXT_PRIMARY));
            emptyLabel.setFontScale(1.5f);
            itemListContainer.add(emptyLabel).padTop(24f).row();
            
            if (currentMode == ShopMode.SELL) {
                Label emptyHint = new Label("Find, buy, or loot items to fill this list.", new Label.LabelStyle(font, TEXT_MUTED));
                emptyHint.setFontScale(1.0f);
                itemListContainer.add(emptyHint).padTop(6f).row();
            }
        } else {
            // Add each item as a clickable row with rounded background
            for (int i = 0; i < itemsToDisplay.size(); i++) {
                Item item = itemsToDisplay.get(i);
                int quantity = currentMode == ShopMode.BUY 
                    ? merchantInventory.getQuantity(item) 
                    : playerInventory.getQuantity(item);
                createClickableItemRow(item, quantity, i);
            }

            if (!navigableItems.isEmpty()) {
                selectedIndex = 0;
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
        itemRow.setBackground(createItemRowDefaultBackground());
        itemRow.pad(8f);  // Increased padding for rounded corner clearance
        itemRow.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        // Create a container table for image with rounded corners
        Table imageContainer = new Table();
        imageContainer.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(getItemTintColor(item), 6, 1));
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
        // Only show quantity in SELL mode (player's inventory)
        // In BUY mode, quantity is selected during purchase prompt
        if (currentMode == ShopMode.SELL) {
            String quantityText = "x" + quantity;
            Label quantityLabel = new Label(quantityText, new Label.LabelStyle(font, BUTTON_TEXT));
            quantityLabel.setFontScale(1.4f);
            textContainer.add(quantityLabel).right().padLeft(10f);
        }
        
        itemRow.add(textContainer).expandX().fillX().height(60f);

        // Add click listener to select item
        itemRow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedIndex = index;
                selectItem(item, itemRow);
                actionMode = true;
                actionIndex = 0;
                updateActionButtonHighlight();
                // Play selection sound on click
                if (audioService != null) {
                    audioService.playSound(SoundEffect.UI_SELECT);
                }
            }
            
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                // Play hover sound only when entering a different item
                if (selectedIndex != index && audioService != null) {
                    audioService.playSound(SoundEffect.UI_HOVER);
                }
                if (selectedItemRow != itemRow) {
                    itemRow.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(ROW_HOVER_BG, 8, 1));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (selectedItemRow != itemRow) {
                    itemRow.setBackground(createItemRowDefaultBackground());
                }
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
            selectedItemRow.setBackground(createItemRowDefaultBackground());
        }

        // Set new selection (highlight with rounded beige)
        selectedItem = item;
        selectedItemRow = itemRow;
        selectedItemRow.setBackground(createItemRowSelectedBackground());

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
        if (activeDropDialog != null) {
            return activeDropDialog.handleNavigationKey(keycode);
        }
        if (activeBuyDialog != null) {
            return activeBuyDialog.handleNavigationKey(keycode);
        }
        if (activeModalDialog != null) {
            if (isInteractKey(keycode) || isCancelKey(keycode)) {
                closeActiveModalDialog();
            }
            return true;
        }

        switch (keycode) {
            case Input.Keys.W, Input.Keys.UP -> {
                if (actionMode) {
                    actionIndex = 0;
                    updateActionButtonHighlight();
                } else {
                    navigateSelection(-1);
                }
                return true;
            }
            case Input.Keys.S, Input.Keys.DOWN -> {
                if (actionMode) {
                    actionIndex = 1;
                    updateActionButtonHighlight();
                } else {
                    navigateSelection(1);
                }
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
            case Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER, Input.Keys.Z, Input.Keys.SPACE, Input.Keys.BUTTON_A -> {
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
                    triggerBuyAction();
                } else {
                    triggerSellAction();
                }
                return true;
            }
            case Input.Keys.X, Input.Keys.ESCAPE, Input.Keys.BACKSPACE, Input.Keys.BUTTON_B -> {
                handleCancelInput();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void handleCancelInput() {
        // Step back from action button focus to item selection.
        if (actionMode) {
            actionMode = false;
            updateActionButtonHighlight();
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_BACK);
            }
            return;
        }

        // Deselect current item and return to neutral details state.
        if (selectedItem != null || selectedItemRow != null) {
            deselectCurrentItem();
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_BACK);
            }
            return;
        }

        // No active selection left: close inventory.
        if (onBackPressed != null) {
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_BACK);
            }
            onBackPressed.run();
        }
    }

    private void deselectCurrentItem() {
        if (selectedItemRow != null) {
            selectedItemRow.setBackground(createItemRowDefaultBackground());
        }
        selectedItem = null;
        selectedItemRow = null;
        actionMode = false;
        actionIndex = 0;
        useBtnRef = null;
        dropBtnRef = null;
        showSelectionPrompt();
    }

    private void showSelectionPrompt() {
        detailPanel.clearChildren();
        detailPanel.top().left();
        Label selectPrompt = new Label("Select an item to view details",
            new Label.LabelStyle(font, TEXT_PRIMARY));
        selectPrompt.setFontScale(1.4f);
        selectPrompt.setWrap(true);
        Label selectHint = new Label("Use W/S or Arrow Keys. Press Z/Enter to choose, X to back.",
            new Label.LabelStyle(font, TEXT_MUTED));
        selectHint.setFontScale(0.95f);
        selectHint.setWrap(true);
        detailPanel.add(selectPrompt).expandX().fillX().padBottom(8f).row();
        detailPanel.add(selectHint).expandX().fillX().top();
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

        // Play navigation sound only when selection actually changes
        if (selectedIndex != lastSelectedIndex && audioService != null) {
            audioService.playSound(SoundEffect.UI_HOVER);
            lastSelectedIndex = selectedIndex;
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
        Table imageContainer = new Table();
        imageContainer.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(PANEL_DIVIDER, 8, 1));
        imageContainer.pad(10f);
        imageContainer.center();
        
        ItemTextureManager textureManager = ItemTextureManager.getInstance();
        Image itemImage = new Image(textureManager.getItemTexture(item.getImageName()));
        imageContainer.add(itemImage).width(80f).height(80f);
        
        detailPanel.add(imageContainer).expandX().fillX().height(100f).padBottom(15f).row();

        // Item name
        Label nameLabel = new Label(item.getName(), new Label.LabelStyle(font, TEXT_PRIMARY));
        nameLabel.setFontScale(2.2f);
        detailPanel.add(nameLabel).expandX().fillX().padBottom(15f).row();

        // Item description
        Label descLabel = new Label(item.getDescription(), new Label.LabelStyle(font, TEXT_PRIMARY));
        descLabel.setFontScale(1.3f);
        descLabel.setWrap(true);
        detailPanel.add(descLabel).expandX().fillX().padBottom(15f).row();

        // Item type
        Label typeLabel = new Label("Type: " + item.getType(), new Label.LabelStyle(font, TEXT_PRIMARY));
        typeLabel.setFontScale(1.2f);
        detailPanel.add(typeLabel).expandX().fillX().padBottom(10f).row();

        // Price display
        if (currentMode == ShopMode.BUY) {
            int buyPrice = merchantInventory.getBuyPrice(item);
            Label priceLabel = new Label("Price: " + buyPrice + " gold", new Label.LabelStyle(font, TEXT_PRIMARY));
            priceLabel.setFontScale(1.3f);
            priceLabel.setColor(Color.GOLD);
            detailPanel.add(priceLabel).expandX().fillX().padBottom(20f).row();
        } else {
            int sellPrice = item.getSellPrice();
            Label priceLabel = new Label("Sell for: " + sellPrice + " gold", new Label.LabelStyle(font, TEXT_PRIMARY));
            priceLabel.setFontScale(1.3f);
            priceLabel.setColor(Color.GOLD);
            detailPanel.add(priceLabel).expandX().fillX().padBottom(20f).row();
        }

        // Spacer to push button to bottom
        detailPanel.add().expand().row();

        // Show only the relevant button based on mode
        if (currentMode == ShopMode.BUY) {
            // BUY button
            TextButton buyBtn = new TextButton("BUY", 
                new TextButton.TextButtonStyle(
                    RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(BUTTON_BG, 8, 1, 0.85f),
                    null, null, font
                )
            );
            buyBtn.getLabel().setColor(BUTTON_TEXT);
            buyBtn.getLabel().setFontScale(1.4f);
            
            Color darkTan = Color.valueOf("#A07A46");
            applyFadeEffect(buyBtn, darkTan);
            
            buyBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    triggerBuyAction();
                }
                
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_HOVER);
                    }
                }
            });
            detailPanel.add(buyBtn).expandX().fillX().height(50).row();
            useBtnRef = buyBtn;
            dropBtnRef = null;
        } else {
            // SELL button
            TextButton sellBtn = new TextButton("SELL",
                new TextButton.TextButtonStyle(
                    RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(BUTTON_BG, 8, 1, 0.85f),
                    null, null, font
                )
            );
            sellBtn.getLabel().setColor(BUTTON_TEXT);
            sellBtn.getLabel().setFontScale(1.4f);
            
            Color darkTan = Color.valueOf("#A07A46");
            applyFadeEffect(sellBtn, darkTan);
            
            sellBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    triggerSellAction();
                }
                
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_HOVER);
                    }
                }
            });
            detailPanel.add(sellBtn).expandX().fillX().height(50).row();
            useBtnRef = null;
            dropBtnRef = sellBtn;
        }

        updateActionButtonHighlight();
    }

    private void triggerBuyAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.UI_SELECT);
        }
        if (selectedItem == null) {
            return;
        }

        int buyPrice = merchantInventory.getBuyPrice(selectedItem);
        
        // Check if player has enough money for at least 1 item
        if (wallet.getMoney() < buyPrice) {
            showSimpleDialog("Not Enough Money", 
                "You need " + buyPrice + " gold to buy this item.\nYou have " + wallet.getMoney() + " gold.");
            return;
        }
        
        // Check if merchant has stock
        if (!merchantInventory.hasStock(selectedItem, 1)) {
            showSimpleDialog("Out of Stock", 
                "This merchant doesn't have any more of this item.");
            return;
        }
        
        // Calculate max affordable quantity
        int maxAffordable = (int) (wallet.getMoney() / buyPrice);
        int maxStock = merchantInventory.getQuantity(selectedItem);
        
        // Handle infinite stock (-1) - use only affordability limit
        if (maxStock == -1) {
            maxStock = Integer.MAX_VALUE;  // Effectively infinite for dialog purposes
        }
        
        // Show quantity selection dialog (like SELL)
        BuyQuantityDialog dialog = new BuyQuantityDialog(
            selectedItem, maxAffordable, maxStock, buyPrice, skin,
            minusDrawable, plusDrawable, exitDrawable
        );
        dialog.pad(20f);
        dialog.show(stage);
        
        dialog.setConfirmCallback((buyAmount) -> {
            // Play confirm sound
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_SELECT);
            }
            
            int totalPrice = buyPrice * buyAmount;
            
            // Double-check affordability and stock
            if (wallet.getMoney() < totalPrice) {
                showSimpleDialog("Not Enough Money", 
                    "You need " + totalPrice + " gold but only have " + wallet.getMoney() + " gold.");
                return;
            }
            
            if (!merchantInventory.hasStock(selectedItem, buyAmount)) {
                showSimpleDialog("Out of Stock", 
                    "Merchant only has " + merchantInventory.getQuantity(selectedItem) + " in stock.");
                return;
            }
            
            // Perform the transaction
            wallet.removeMoney(totalPrice);
            playerInventory.addItem(selectedItem, buyAmount);
            merchantInventory.removeStock(selectedItem, buyAmount);
            
            // CRITICAL: Update wallet display immediately
            refreshWalletDisplay();
            
            Gdx.app.log("MerchantShopUI",
                "[Item Bought] Bought " + buyAmount + "x " + selectedItem.getName() + 
                " for " + totalPrice + " gold");
            
            // Show success message with proper layout
            showSuccessDialog("Purchase Complete", 
                "Bought " + buyAmount + "x " + selectedItem.getName() + "\nfor " + totalPrice + " gold!");
            
            // Refresh the list if merchant is out of stock
            if (!merchantInventory.hasStock(selectedItem, 1) && !merchantInventory.isInfiniteStock()) {
                populateItemList();
            }
        });
        
        dialog.setCancelCallback(() -> {
            // Play cancel sound
            if (audioService != null) {
                audioService.playSound(SoundEffect.UI_BACK);
            }
        });
        
        activeBuyDialog = dialog;
    }

    private void openPartySelectionDialog() {
        if (selectedItem == null || party == null || party.isEmpty()) {
            return;
        }

        PartySelectionDialog dialog = new PartySelectionDialog(
            party,
            selectedItem,
            playerInventory,
            skin,
            font,
            audioService,
            battleContext
        );
        dialog.pad(20f);

        dialog.setOnItemApplied(() -> {
            refreshItemList();
            selectedItem = null;
            selectedItemRow = null;
        });

        dialog.setOnClosed(() -> activePartyDialog = null);
        activePartyDialog = dialog;
        dialog.show(stage, null);
    }

    private void triggerSellAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.UI_SELECT);
        }
        
        if (selectedItem != null) {
            int quantity = playerInventory.getQuantity(selectedItem);
            if (quantity > 0) {
                int sellPrice = selectedItem.getSellPrice();
                
                // Show quantity selection dialog
                SellQuantityDialog dialog = new SellQuantityDialog(
                    selectedItem, quantity, sellPrice, skin,
                    minusDrawable, plusDrawable, exitDrawable
                );
                dialog.pad(20f);
                dialog.show(stage);
                
                dialog.setConfirmCallback((sellAmount) -> {
                    // Play confirm sound
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_SELECT);
                    }
                    
                    int totalPrice = sellPrice * sellAmount;
                    boolean removed = playerInventory.removeItem(selectedItem, sellAmount);
                    if (removed) {
                        wallet.addMoney(totalPrice);
                        
                        // CRITICAL: Update wallet display immediately
                        refreshWalletDisplay();
                        
                        Gdx.app.log("MerchantShopUI",
                            "[Item Sold] Sold " + sellAmount + "x " + selectedItem.getName() + 
                            " for " + totalPrice + " gold");
                        
                        showSuccessDialog("Sold", 
                            "Sold " + sellAmount + "x " + selectedItem.getName() + 
                            "\nfor " + totalPrice + " gold!");
                        
                        // Refresh item list
                        populateItemList();
                        selectedItem = null;
                        selectedItemRow = null;
                    } else {
                        showSimpleDialog("Error", "Failed to sell item.");
                    }
                });
                
                dialog.setCancelCallback(() -> {
                    // Play cancel sound
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_BACK);
                    }
                });
                
                activeDropDialog = null;  // Not using DropQuantityDialog for sell, using SellQuantityDialog
            }
        }
    }
    
    private void showSimpleDialog(String title, String message) {
        Dialog dialog = new Dialog(title, skin);
        dialog.text(message);
        dialog.button("OK", true);
        dialog.show(stage);
        activeModalDialog = dialog;
    }
    
    /**
     * Show a success dialog with proper padding and layout
     * Fixes cramped popup issue with proper spacing
     */
    private void showSuccessDialog(String title, String message) {
        Dialog dialog = new Dialog(title, skin) {
            @Override
            protected void result(Object object) {
                // Dialog closes automatically
            }
        };
        
        // CRITICAL: Proper padding and spacing for readable layout
        dialog.getContentTable().pad(30);  // 30px padding around content
        dialog.getButtonTable().pad(20);   // 20px padding around buttons
        dialog.getButtonTable().padTop(15); // Extra space above buttons
        
        // Add message with proper wrapping and centering
        Label messageLabel = new Label(message, skin);
        messageLabel.setWrap(true);
        messageLabel.setAlignment(Align.center);
        dialog.getContentTable().add(messageLabel).width(300).center().space(10);
        
        // Add OK button with proper styling
        dialog.button("OK", true);
        
        dialog.show(stage);
        activeModalDialog = dialog;
    }
    
    /**
     * Refresh the wallet display immediately after transaction
     * CRITICAL: This ensures money updates are visible without reopening menu
     */
    private void refreshWalletDisplay() {
        if (walletLabel != null && wallet != null) {
            walletLabel.setText("Money: " + wallet.getMoney());
            Gdx.app.log("MerchantShopUI", "Wallet display refreshed: " + wallet.getMoney() + " gold");
        }
    }
    
    /**
     * Update BUY/SELL tab highlights based on current mode
     * CRITICAL: This ensures the active tab is visually highlighted
     */
    private void updateTabHighlights() {
        if (buyTabRef != null && sellTabRef != null) {
            // Update BUY tab
            buyTabRef.getStyle().up = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
                currentMode == ShopMode.BUY ? ROW_SELECTED_BG : BUTTON_BG, 8, 1, 0.85f);
            
            // Update SELL tab
            sellTabRef.getStyle().up = RoundedPanelDrawable.createRoundedPanelWithStrokeAndGrain(
                currentMode == ShopMode.SELL ? ROW_SELECTED_BG : BUTTON_BG, 8, 1, 0.85f);
            
            Gdx.app.log("MerchantShopUI", "Tab highlights updated - Mode: " + currentMode);
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

        useBtnRef.setColor(actionIndex == 0 ? ACTION_SELECTED_TINT : Color.WHITE);
        dropBtnRef.setColor(actionIndex == 1 ? ACTION_SELECTED_TINT : Color.WHITE);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable createItemRowDefaultBackground() {
        return RoundedPanelDrawable.createRoundedPanelWithStroke(BUTTON_BG, 8, 1);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable createItemRowSelectedBackground() {
        return RoundedPanelDrawable.createRoundedPanelWithStroke(ROW_SELECTED_BG, 8, 1);
    }

    private boolean isUseAllowedInCurrentContext(Item item) {
        return PlayerBattleState.isInventoryItemUseAllowed(item, battleContext, party);
    }

    private String buildUseDeniedMessage(Item item) {
        if (item == null) {
            return "This item cannot be used right now.";
        }
        if (PlayerBattleState.isEntirePartyFelled(party)) {
            return "While your whole team is down, only revive items can be used.";
        }
        if (battleContext) {
            return item.getName() + " cannot be used in combat.";
        }
        return item.getName() + " can only be used in combat.";
    }

    private Color getItemTintColor(Item item) {
        if (item == null) {
            return PANEL_LEFT_BG;
        }
        String itemId = item.getId() == null ? "" : item.getId().toLowerCase();
        if (itemId.contains("defense")) {
            return DEFENSE_ITEM_TINT;
        }
        return switch (item.getType()) {
            case POTION -> HEALTH_ITEM_TINT;
            case STAT_BOOSTER -> STAT_BOOST_ITEM_TINT;
            case REVIVE -> STAT_BOOST_ITEM_TINT;
            case SPECIAL -> PANEL_LEFT_BG;
        };
    }

    private void showUsePermissionDialog(String title, String message) {
        showUsePermissionDialog(title, message, null);
    }

    private void showUsePermissionDialog(String title, String message, Runnable onClose) {
        Dialog dialog = new Dialog(title, skin) {
            @Override
            public void hide() {
                hide(null);
            }

            @Override
            protected void result(Object object) {
                Runnable callback = activeModalOnClose;
                activeModalDialog = null;
                activeModalOnClose = null;
                if (callback != null) callback.run();
            }
        };
        dialog.setModal(true);
        dialog.setMovable(false);
        dialog.getTitleLabel().setColor(TEXT_PRIMARY);
        dialog.getTitleLabel().setFontScale(1.05f);
        dialog.getContentTable().setBackground(
            RoundedPanelDrawable.createRoundedPanelWithStroke(MODAL_BG, 10, 2)
        );
        dialog.getContentTable().pad(8f);

        Label body = new Label(message, new Label.LabelStyle(font, TEXT_PRIMARY));
        body.setWrap(true);
        body.setFontScale(1.0f);
        body.setAlignment(Align.center);

        dialog.getContentTable().add(body).width(360f).pad(14f);
        TextButton okButton = new TextButton(
            "OK",
            new TextButton.TextButtonStyle(
                RoundedPanelDrawable.createRoundedPanelWithStroke(BUTTON_BG, 8, 2),
                RoundedPanelDrawable.createRoundedPanelWithStroke(ROW_HOVER_BG, 8, 2),
                RoundedPanelDrawable.createRoundedPanelWithStroke(PANEL_DIVIDER, 8, 2),
                font
            )
        );
        okButton.getLabel().setColor(BUTTON_TEXT);
        okButton.getLabel().setFontScale(1.0f);
        dialog.button(okButton, true);
        dialog.getButtonTable().pad(6f, 12f, 10f, 12f);
        activeModalDialog = dialog;
        activeModalOnClose = onClose;
        dialog.show(stage, null);
    }

    private void showDropResultDialog(String message) {
        Dialog dialog = new Dialog("Drop Confirmed", skin) {
            @Override
            protected void result(Object object) {
                activeModalDialog = null;
                activeModalOnClose = null;
            }
        };
        dialog.setModal(true);
        dialog.setMovable(false);

        Label body = new Label(message, new Label.LabelStyle(font, TEXT_PRIMARY));
        body.setWrap(true);
        body.setFontScale(1.1f);

        dialog.getContentTable().add(body).width(430f).pad(16f);
        dialog.button("OK");
        activeModalDialog = dialog;
        activeModalOnClose = null;
        dialog.show(stage);
    }

    private void closeActiveModalDialog() {
        if (activeModalDialog == null) {
            return;
        }
        Dialog dialog = activeModalDialog;
        Runnable callback = activeModalOnClose;
        activeModalDialog = null;
        activeModalOnClose = null;
        dialog.hide(null);
        if (callback != null) {
            callback.run();
        }
    }

    private boolean isInteractKey(int keycode) {
        return InputConventions.isInteractKey(keycode);
    }

    private boolean isCancelKey(int keycode) {
        return InputConventions.isCancelKey(keycode);
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
        Gdx.app.log("MerchantShopUI", "[Refresh] Item list repopulated with " + 
            (playerInventory != null ? playerInventory.getAllItems().size() : 0) + " items");
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
