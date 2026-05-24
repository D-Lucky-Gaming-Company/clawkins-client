package github.dluckycompany.clawkins.ui;

import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.inventory.InventoryController;
import github.dluckycompany.clawkins.inventory.ItemUseAction;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;

/**
 * Main inventory UI overlay for managing items and using them.
 * Displays item list, selected item details, and party member targets.
 * Uses fixed virtual coordinate system (800x600) - resolution-independent rendering.
 *
 * Usage:
 *   InventoryOverlay overlay = new InventoryOverlay(inventory, party, skin, font);
 *   stage.addActor(overlay);
 */
public class InventoryOverlay extends Dialog {
    private final Inventory inventory;
    private final InventoryController controller;
    private final List<Clawkin> party;
    private final Skin skin;
    private final BitmapFont font;
    private final com.badlogic.gdx.scenes.scene2d.Stage targetSelectionStage;  // Stage for party selection overlay
    private final com.badlogic.gdx.scenes.scene2d.Stage dialogStage;  // Stage for drop quantity dialog

    // UI components
    private Label selectedItemNameLabel;
    private Label selectedItemDescLabel;
    private Label selectedItemEffectLabel;
    private Label actionLogLabel;  // For HP change feedback
    private PartyMemberListView partyUI;
    private PartySelectionOverlay partySelectionOverlay;  // Interactive party selection
    private Item selectedItem;
    private Runnable onClose;
    private Consumer<ItemUseAction> onItemUsed;
    
    // Item list UI components (stored for reactive refresh)
    private Table itemTable;  // Internal table containing item buttons
    private ScrollPane itemListScrollPane;  // ScrollPane parent containing itemTable
    private Table itemListContainer;  // Container panel for the entire item list section
    
    // Three-phase item use action state
    private ItemUseAction currentUseAction;
    
    // UI state
    private boolean isSelectingTarget = false;
    private boolean isShowingDropDialog = false;
    private Table rightPanel;
    
    // Fixed virtual UI dimensions (constant, independent of physical resolution)
    private static final float DIALOG_WIDTH = 760f;   // Leave room for padding in 800x600 viewport
    private static final float DIALOG_HEIGHT = 500f;  // Leave room for padding
    
    // Column widths (fixed, deterministic sizing)
    private static final float ITEMS_COLUMN_WIDTH = 180f;
    private static final float DETAILS_COLUMN_WIDTH = 280f;
    private static final float ACTIONS_COLUMN_WIDTH = 180f;
    private static final float CELL_PADDING = 10f;
    private static final float ROW_HEIGHT = 35f;
    
    // Enumeration for layout debug
    private static final boolean DEBUG_LAYOUT = false;

    public InventoryOverlay(Inventory inventory, InventoryController controller, List<Clawkin> party, Skin skin, BitmapFont font) {
        super("Inventory", skin);
        this.inventory = inventory;
        this.controller = controller;
        this.party = party;
        this.skin = skin;
        this.font = font;
        
        // Create a dedicated Stage for party selection overlay
        // Uses FitViewport(800, 600) to handle fullscreen transitions
        this.targetSelectionStage = new com.badlogic.gdx.scenes.scene2d.Stage(
            new com.badlogic.gdx.utils.viewport.FitViewport(800f, 600f)
        );
        
        // Create a dedicated Stage for drop quantity dialogs
        // Uses FitViewport(800, 600) to handle fullscreen transitions
        this.dialogStage = new com.badlogic.gdx.scenes.scene2d.Stage(
            new com.badlogic.gdx.utils.viewport.FitViewport(800f, 600f)
        );
        
        // Create the interactive party selection overlay
        this.partySelectionOverlay = new PartySelectionOverlay(party, skin, font);
        this.partySelectionOverlay.setTargetCallback(new TargetCallback() {
            @Override
            public void onTargetSelected(Clawkin target) {
                // Execute the three-phase item use action
                executeItemUsePhaseC(target);
            }

            @Override
            public void onCancel() {
                // Return to inventory view
                exitTargetSelection();
            }
        });
        
        // Register inventory change callback with controller
        // This triggers UI refresh whenever items are used/dropped
        this.controller.setOnInventoryChanged(change -> {
            System.out.println("[InventoryOverlay] Inventory changed: " + change);
            logAction(change);
            System.out.println("[InventoryOverlay] Calling refreshItemList() from inventory change callback");
            refreshItemList();
        });
        
        buildUI();
    }

    private void buildUI() {
        // Get content table and clear it
        Table contentTable = getContentTable();
        contentTable.clear();
        
        // Create root container with padding to prevent edge clipping
        Table root = new Table();
        root.setFillParent(true);
        root.pad(CELL_PADDING);
        root.setDebug(DEBUG_LAYOUT);  // Enable debug visualization if needed
        
        // Create content table with 3-column layout
        Table content = new Table();
        content.setDebug(DEBUG_LAYOUT);
        
        // Define column widths explicitly - prevents compression and clipping
        content.defaults().pad(CELL_PADDING).top().left();
        
        // Column 1: Item list (fixed width)
        Table itemsColumn = buildItemListPanel();
        content.add(itemsColumn)
            .width(ITEMS_COLUMN_WIDTH)
            .fillY()
            .top()
            .left();
        
        // Column 2: Item details (fixed width, wider for text)
        Table detailsColumn = buildItemDetailsPanel();
        content.add(detailsColumn)
            .width(DETAILS_COLUMN_WIDTH)
            .fillY()
            .top()
            .left();
        
        // Column 3: Actions/Party (fixed width)
        this.rightPanel = buildRightPanelInitial();
        content.add(this.rightPanel)
            .width(ACTIONS_COLUMN_WIDTH)
            .fillY()
            .top()
            .left();
        
        // Add content table to root with fill expansion
        root.add(content).expand().fill().row();
        
        // Add button panel below content table
        Table buttonPanel = buildButtonPanel();
        root.add(buttonPanel)
            .width(ITEMS_COLUMN_WIDTH + DETAILS_COLUMN_WIDTH + ACTIONS_COLUMN_WIDTH + 2 * CELL_PADDING)
            .height(60)
            .bottom()
            .left()
            .padTop(CELL_PADDING);
        
        // Add root to contentTable
        contentTable.add(root).expand().fill();
        
        // Dialog sizing - fixed virtual dimensions
        setWidth(DIALOG_WIDTH);
        setHeight(DIALOG_HEIGHT);
        setPosition(400 - DIALOG_WIDTH / 2, 300 - DIALOG_HEIGHT / 2);  // Center in 800x600 virtual space
    }

    private Table buildRightPanelInitial() {
        Table panel = new Table();
        panel.top().left();
        panel.setDebug(DEBUG_LAYOUT);
        panel.defaults().left().padBottom(CELL_PADDING);
        
        Label titleLabel = new Label("Actions", skin);
        titleLabel.setColor(Color.GOLD);
        panel.add(titleLabel)
            .width(ACTIONS_COLUMN_WIDTH - 2 * CELL_PADDING)
            .height(30)
            .left()
            .row();
        
        Label infoLabel = new Label("Select an item\nand choose\nUse or Drop", skin);
        infoLabel.setAlignment(Align.left);
        infoLabel.setWrap(true);
        panel.add(infoLabel)
            .width(ACTIONS_COLUMN_WIDTH - 2 * CELL_PADDING)
            .expand()
            .fill()
            .left();
        
        return panel;
    }

    private Table buildItemListPanel() {
        Table panel = new Table();
        panel.top().left();
        panel.setDebug(DEBUG_LAYOUT);
        panel.defaults().left().padBottom(CELL_PADDING);
        
        // Store reference to this panel BEFORE calling rebuildItemTable()
        this.itemListContainer = panel;
        
        Label titleLabel = new Label("Items", skin);
        titleLabel.setColor(Color.GOLD);
        panel.add(titleLabel)
            .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING)
            .height(30)
            .left()
            .row();

        // Create the scrollable item list - rebuildItemTable() will create itemTable and itemListScrollPane
        rebuildItemTable();
        
        // Add scrollPane to panel (will be refreshed later on inventory changes)
        if (itemListScrollPane != null) {
            panel.add(itemListScrollPane)
                .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING)
                .expand()
                .fill();
        }
        
        return panel;
    }
    
    /**
     * Rebuild the item list table from current inventory state.
     * Called during initial setup and on every inventory change (use/drop).
     * This ensures the item counts always reflect the backend state.
     * 
     * SAFE: This method performs no structural changes to existing actors.
     * It only creates a new itemTable and itemListScrollPane.
     */
    private void rebuildItemTable() {
        System.out.println("[InventoryOverlay] === REBUILDING ITEM TABLE ===");
        
        try {
            // Create fresh item table with constrained width
            itemTable = new Table();
            itemTable.top().left();
            itemTable.setDebug(DEBUG_LAYOUT);
            itemTable.defaults()
                .left()
                .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING - 15)  // Account for scrollbar
                .height(ROW_HEIGHT)
                .padBottom(5);
            
            // CRITICAL: Get the current inventory state from backend
            List<Item> items = inventory.getAllItems();
            System.out.println("[InventoryOverlay] Backend has " + items.size() + " total items");
            
            // NULL CHECK: If inventory is empty, show placeholder
            if (items.isEmpty()) {
                System.out.println("[InventoryOverlay] Inventory is empty, showing 'No items'");
                Label emptyLabel = new Label("No items", skin);
                emptyLabel.setAlignment(Align.left);
                itemTable.add(emptyLabel)
                    .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING - 15)
                    .left();
                System.out.println("[InventoryOverlay] Empty label added to table");
            } else {
                // Iterate through items and create UI buttons for each
                int itemCount = 0;
                for (Item item : items) {
                    if (item == null) {
                        System.out.println("[InventoryOverlay] WARNING: Null item in inventory list, skipping");
                        continue;
                    }
                    
                    int quantity = inventory.getQuantity(item);
                    System.out.println("[InventoryOverlay] Creating button for: " + item.getName() + " (qty=" + quantity + ")");
                    
                    Button itemButton = createItemButton(item, quantity);
                    if (itemButton != null) {
                        itemTable.add(itemButton)
                            .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING - 15)
                            .left()
                            .row();
                        itemCount++;
                    }
                }
                System.out.println("[InventoryOverlay] Added " + itemCount + " item buttons to table");
            }
            
            // Create new scroll pane with the fresh item table
            if (itemListScrollPane != null) {
                System.out.println("[InventoryOverlay] Disposing old scroll pane");
                itemListScrollPane.clear(); // Clear actor children safely
            }
            
            itemListScrollPane = new ScrollPane(itemTable, skin);
            if (itemListScrollPane != null) {
                itemListScrollPane.setFadeScrollBars(true);
                itemListScrollPane.setScrollingDisabled(true, false);
                System.out.println("[InventoryOverlay] New ScrollPane created and configured");
            } else {
                System.out.println("[InventoryOverlay] ERROR: Failed to create ScrollPane");
            }
            
            System.out.println("[InventoryOverlay] === ITEM TABLE REBUILD COMPLETE ===");
        } catch (Exception e) {
            System.err.println("[InventoryOverlay] CRITICAL ERROR in rebuildItemTable(): " + e.getMessage());
        }
    }

    private Button createItemButton(Item item, int quantity) {
        String buttonText = item.getName() + " ×" + quantity;
        TextButton button = new TextButton(buttonText, skin);
        final Item finalItem = item;
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectItem(finalItem);
            }
        });
        return button;
    }

    private Table buildItemDetailsPanel() {
        Table panel = new Table();
        panel.top().left();
        panel.setDebug(DEBUG_LAYOUT);
        panel.defaults().left().padBottom(CELL_PADDING);
        
        float columnWidth = DETAILS_COLUMN_WIDTH - 2 * CELL_PADDING;
        
        Label titleLabel = new Label("Details", skin);
        titleLabel.setColor(Color.GOLD);
        panel.add(titleLabel)
            .width(columnWidth)
            .height(30)
            .left()
            .row();

        selectedItemNameLabel = new Label("---", skin);
        selectedItemNameLabel.setColor(Color.GOLD);
        selectedItemNameLabel.setWrap(true);
        panel.add(selectedItemNameLabel)
            .width(columnWidth)
            .height(25)
            .padBottom(10)
            .left()
            .row();

        // Description section with wrapping and bounded width
        panel.add(new Label("Description:", skin))
            .width(columnWidth)
            .height(20)
            .padBottom(5)
            .left()
            .row();
        selectedItemDescLabel = new Label("", new Label.LabelStyle(font, Color.WHITE));
        selectedItemDescLabel.setWrap(true);
        selectedItemDescLabel.setAlignment(Align.left);
        panel.add(selectedItemDescLabel)
            .width(columnWidth)
            .expand()
            .left()
            .padBottom(10)
            .row();

        // Effect section with wrapping and bounded width
        panel.add(new Label("Effect:", skin))
            .width(columnWidth)
            .height(20)
            .padBottom(5)
            .left()
            .row();
        selectedItemEffectLabel = new Label("", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        selectedItemEffectLabel.setWrap(true);
        selectedItemEffectLabel.setAlignment(Align.left);
        panel.add(selectedItemEffectLabel)
            .width(columnWidth)
            .expand()
            .left()
            .padBottom(10)
            .row();

        // Action log section - displays HP change feedback during item use
        panel.add(new Label("Log:", skin))
            .width(columnWidth)
            .height(20)
            .padBottom(5)
            .left()
            .row();
        actionLogLabel = new Label("", new Label.LabelStyle(font, Color.YELLOW));
        actionLogLabel.setWrap(true);
        actionLogLabel.setAlignment(Align.left);
        panel.add(actionLogLabel)
            .width(columnWidth)
            .height(40)
            .left()
            .expand()
            .top();

        return panel;
    }

    private Table buildButtonPanel() {
        Table panel = new Table();
        panel.setDebug(DEBUG_LAYOUT);
        panel.defaults()
            .padRight(CELL_PADDING)
            .height(50)
            .expandX()
            .fillX()
            .left();

        // Calculate button width: (3 columns + padding) / 3 buttons
        float buttonWidth = (ITEMS_COLUMN_WIDTH + DETAILS_COLUMN_WIDTH + ACTIONS_COLUMN_WIDTH + 2 * CELL_PADDING) / 3;

        // DROP COMMAND: Show quantity selector for stackable items
        Button dropButton = new TextButton("Drop", skin);
        dropButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Prevent double-clicking while dialog is already open
                if (!isSelectingTarget && !isShowingDropDialog && selectedItem != null) {
                    int quantity = inventory.getQuantity(selectedItem);
                    if (quantity > 0) {
                        isShowingDropDialog = true;
                        showDropQuantityDialog(selectedItem, quantity);
                    }
                }
            }
        });
        panel.add(dropButton).width(buttonWidth).left();

        // USE COMMAND (Phase A, B, C workflow):
        // Phase A (Selection): Click Use -> create ItemUseAction
        // Phase B (Validation): Show interactive PartySelectionOverlay
        // Phase C (Application): TargetCallback triggers executeItemUsePhaseC()
        Button useButton = new TextButton("Use", skin);
        useButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedItem == null) {
                    logAction("Select an item first");
                    return;
                }
                
                if (!isSelectingTarget) {
                    // Phase A: Begin item use (creates use action)
                    currentUseAction = controller.beginUseItem(selectedItem);
                    if (currentUseAction != null) {
                        isSelectingTarget = true;
                        enterTargetSelection();  // Show interactive party selection overlay
                    }
                }
            }
        });
        panel.add(useButton).width(buttonWidth).left();
        
        Button cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (isSelectingTarget) {
                    exitTargetSelection();
                }
            }
        });
        panel.add(cancelButton).width(buttonWidth).left();

        return panel;
    }

    private void selectItem(Item item) {
        System.out.println("[InventoryOverlay] === SELECTING ITEM: " + (item != null ? item.getName() : "null") + " ===");
        
        selectedItem = item;
        
        // CRITICAL: Clear all stale references from labels to prevent overlap
        if (selectedItemNameLabel != null) {
            selectedItemNameLabel.setText(item != null ? item.getName() : "---");
        }
        if (selectedItemDescLabel != null) {
            selectedItemDescLabel.setText(item != null ? item.getDescription() : "");
        }
        if (selectedItemEffectLabel != null) {
            selectedItemEffectLabel.setText(item != null ? item.getEffectDescription() : "");
        }
        if (actionLogLabel != null) {
            actionLogLabel.setText("");
        }
        
        System.out.println("[InventoryOverlay] Item selection labels updated");
    }

    private void logAction(String message) {
        System.out.println("[InventoryUI] " + message);
        if (actionLogLabel != null) {
            actionLogLabel.setText(message);
        }
    }

    private void refreshItemList() {
        System.out.println("[InventoryOverlay] === STARTING ATOMIC INVENTORY REFRESH ===");
        
        if (itemListContainer == null) {
            System.out.println("[InventoryOverlay] ERROR: itemListContainer is null, cannot refresh");
            return;
        }
        
        try {
            // CRITICAL: Step 0 - COMPLETELY CLEAR the container before rebuilding
            // This prevents stale actors from overlapping with new ones
            System.out.println("[InventoryOverlay] Step 0: Clearing ALL children from itemListContainer");
            itemListContainer.clearChildren();
            System.out.println("[InventoryOverlay] Container cleared, all stale actors removed");
            
            // Step 1: Rebuild item table from backend inventory
            System.out.println("[InventoryOverlay] Step 1: Rebuilding item table from backend inventory");
            rebuildItemTable();
            System.out.println("[InventoryOverlay] Step 1 complete: Item table rebuilt");
            
            // Step 2: Recreate title label (which was removed by clearChildren())
            System.out.println("[InventoryOverlay] Step 2: Re-adding title label to container");
            Label titleLabel = new Label("Items", skin);
            titleLabel.setColor(Color.GOLD);
            itemListContainer.add(titleLabel)
                .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING)
                .height(30)
                .left()
                .row();
            System.out.println("[InventoryOverlay] Title label re-added");
            
            // Step 3: Add the fresh ScrollPane
            System.out.println("[InventoryOverlay] Step 3: Adding fresh ScrollPane to container");
            if (itemListScrollPane != null) {
                itemListContainer.add(itemListScrollPane)
                    .width(ITEMS_COLUMN_WIDTH - 2 * CELL_PADDING)
                    .expand()
                    .fill();
                System.out.println("[InventoryOverlay] ScrollPane added");
            } else {
                System.out.println("[InventoryOverlay] ERROR: itemListScrollPane is null!");
            }
            
            // Step 4: CRITICAL - Force Scene2D to recalculate layout bounds
            System.out.println("[InventoryOverlay] Step 4: Validating layout hierarchy");
            itemListContainer.invalidateHierarchy();
            itemListContainer.pack();
            System.out.println("[InventoryOverlay] Layout hierarchy invalidated and packed");
            
            System.out.println("[InventoryOverlay] === ATOMIC INVENTORY REFRESH COMPLETE ===");
        } catch (Exception e) {
            System.err.println("[InventoryOverlay] CRITICAL ERROR during refresh: " + e.getMessage());
        }
    }

    /**
     * Enter Target Selection Phase B
     * 
     * Shows the interactive PartySelectionOverlay as a modal dialog.
     * Input is routed to targetSelectionStage instead of the inventory stage.
     * FitViewport handles coordinate transformation for screen-space clicks.
     */
    private void enterTargetSelection() {
        logAction("Select a target for " + selectedItem.getName());
        
        // Clear party selection overlay and add fresh one
        targetSelectionStage.clear();
        partySelectionOverlay = new PartySelectionOverlay(party, skin, font);
        partySelectionOverlay.setTargetCallback(new TargetCallback() {
            @Override
            public void onTargetSelected(Clawkin target) {
                // Phase C: Execute the use action on selected target
                executeItemUsePhaseC(target);
            }

            @Override
            public void onCancel() {
                exitTargetSelection();
            }
        });
        targetSelectionStage.addActor(partySelectionOverlay);
        
        // Set targetSelectionStage as active InputProcessor
        // This ensures clicks go to the party selection overlay, not inventory
        com.badlogic.gdx.Gdx.input.setInputProcessor(targetSelectionStage);
        
        System.out.println("[InventoryOverlay] Entered TARGET_SELECTION phase");
    }

    /**
     * Exit Target Selection Phase B
     * 
     * Hides the PartySelectionOverlay and returns to inventory view.
     * Restores input processor to inventoryStage for continued inventory interaction.
     */
    private void exitTargetSelection() {
        isSelectingTarget = false;
        currentUseAction = null;
        targetSelectionStage.clear();
        logAction("Target selection cancelled");
        
        // Restore input processor to inventoryStage
        // Note: This assumes inventoryStage is the parent stage that contains this overlay
        com.badlogic.gdx.Gdx.input.setInputProcessor(getStage());
        
        System.out.println("[InventoryOverlay] Exited TARGET_SELECTION phase");
    }

    /**
     * Execute Item Use Phase C
     * 
     * Called by TargetCallback when a party member is clicked.
     * 1. Sets the target on the ItemUseAction
     * 2. Executes the use action via InventoryController
     * 3. Updates party HP display
     * 4. Exits target selection and returns to inventory
     */
    private void executeItemUsePhaseC(Clawkin target) {
        if (currentUseAction == null || selectedItem == null) {
            return;
        }

        // Phase C: Set target and execute
        currentUseAction.setTarget(target);
        boolean executed = controller.executeUseAction(currentUseAction);

        if (executed) {
            logAction("Used " + selectedItem.getName() + " on " + target.getName());
            System.out.println("[InventoryOverlay] Item use succeeded, triggering UI refresh");
            
            try {
                // Update party member HP display if partyUI exists
                if (partyUI != null) {
                    partyUI.updateAll();
                }

                // Refresh inventory list to remove consumed item from UI
                // This rebuilds the itemTable with updated quantities
                System.out.println("[InventoryOverlay] Calling refreshItemList() to update item counts");
                refreshItemList();

                // Fire callback if registered
                if (onItemUsed != null) {
                    onItemUsed.accept(currentUseAction);
                }
            } catch (Exception e) {
                System.err.println("[InventoryOverlay] ERROR during UI refresh after item use: " + e.getMessage());
                logAction("Error updating inventory display");
            }
        } else {
            logAction("Failed to use " + selectedItem.getName());
            System.out.println("[InventoryOverlay] Item use failed, no UI refresh needed");
        }

        // Clean up state and return to inventory
        isSelectingTarget = false;
        currentUseAction = null;
        targetSelectionStage.clear();
        
        // Restore input processor to inventoryStage
        com.badlogic.gdx.Gdx.input.setInputProcessor(getStage());
        
        System.out.println("[InventoryOverlay] Completed ITEM_USE phase");
    }

    /**
     * Set callback for when inventory closes.
     *
     * @param callback the callback to execute
     */
    public void setOnCloseCallback(Runnable callback) {
        this.onClose = callback;
    }

    /**
     * Set callback for when an item is used.
     *
     * @param callback receives ItemUseAction with item and target
     */
    public void setOnItemUsedCallback(Consumer<ItemUseAction> callback) {
        this.onItemUsed = callback;
    }

    @Override
    protected void result(Object object) {
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Show the Drop Quantity Dialog.
     * User can select how many items to drop.
     */
    private void showDropQuantityDialog(Item item, int currentQuantity) {
        System.out.println("[InventoryOverlay] === SHOWING DROP DIALOG FOR " + item.getName() + " ===");
        
        DropQuantityDialog dialog = new DropQuantityDialog(item, currentQuantity, skin, null, null, null);
        System.out.println("[InventoryOverlay] Created DropQuantityDialog instance");
        
        dialog.setConfirmCallback(quantityToDrop -> {
            System.out.println("[InventoryOverlay] Drop confirmed: " + quantityToDrop + "x " + item.getName());
            try {
                // Execute the transaction in the backend
                boolean dropped = controller.dropItemQuantity(item, quantityToDrop);
                if (dropped) {
                    logAction("Dropped " + quantityToDrop + "x " + item.getName());
                    System.out.println("[InventoryOverlay] Item successfully dropped from backend");
                    
                    // Clear selection if all items were dropped
                    int newQuantity = inventory.getQuantity(item);
                    if (newQuantity == 0) {
                        selectedItem = null;
                        selectedItemNameLabel.setText("---");
                        selectedItemDescLabel.setText("");
                        selectedItemEffectLabel.setText("");
                        actionLogLabel.setText("");
                        System.out.println("[InventoryOverlay] All instances dropped, cleared selection");
                    }
                    
                    // CRITICAL: Perform full atomic UI refresh with clearChildren()
                    System.out.println("[InventoryOverlay] Initiating atomic UI refresh");
                    refreshItemList();
                } else {
                    System.out.println("[InventoryOverlay] Drop failed at controller level (backend transaction rejected)");
                    logAction("Error: Cannot drop item");
                }
            } catch (Exception e) {
                System.err.println("[InventoryOverlay] EXCEPTION during drop transaction: " + e.getMessage());
                logAction("Error dropping item: " + e.getMessage());
            } finally {
                isShowingDropDialog = false;
                System.out.println("[InventoryOverlay] Drop dialog closed (confirm path)");
            }
        });
        
        dialog.setCancelCallback(() -> {
            System.out.println("[InventoryOverlay] Drop cancelled by user");
            logAction("Drop cancelled");
            isShowingDropDialog = false;
        });
        
        // CRITICAL: Save current input processor and switch to dialogStage
        // This ensures clicks route to dialog buttons instead of inventory stage beneath
        final InputProcessor previousProcessor = com.badlogic.gdx.Gdx.input.getInputProcessor();
        System.out.println("[InventoryOverlay] Saved input processor: " + previousProcessor.getClass().getSimpleName());
        
        // CRITICAL: Use Dialog.show(stage) to properly handle modality and input focus
        // This method automatically:
        // 1. Centers the dialog in the stage viewport
        // 2. Sets it to modal (blocks input to underlying actors)
        // 3. Manages the actor hierarchy and rendering order
        System.out.println("[InventoryOverlay] Calling dialog.show(dialogStage) for proper modal handling");
        dialog.show(dialogStage);
        
        // DIAGNOSTIC: Log dialog dimensions after show()
        System.out.println("[InventoryOverlay] After dialog.show() - Dialog Width: " + dialog.getWidth() + ", Height: " + dialog.getHeight());
        System.out.println("[InventoryOverlay] After dialog.show() - Dialog Position X: " + dialog.getX() + ", Y: " + dialog.getY());
        System.out.println("[InventoryOverlay] DialogStage actor count: " + dialogStage.getActors().size);
        
        // CRITICAL: Switch input processor to dialogStage so dialog buttons receive clicks
        com.badlogic.gdx.Gdx.input.setInputProcessor(dialogStage);
        System.out.println("[InventoryOverlay] Input processor switched to dialogStage");
        System.out.println("[InventoryOverlay] Drop dialog is now visible and modal");
    }

    /**
     * Get the target selection stage for rendering and viewport updates.
     * Called by GameScreen to render the PartySelectionOverlay.
     */
    public com.badlogic.gdx.scenes.scene2d.Stage getTargetSelectionStage() {
        return targetSelectionStage;
    }

    /**
     * Get the dialog stage for rendering drop quantity dialogs.
     * Called by GameScreen to render dialogs.
     */
    public com.badlogic.gdx.scenes.scene2d.Stage getDialogStage() {
        return dialogStage;
    }

    /**
     * Check if target selection is currently active.
     * Used by GameScreen to determine input processor and render order.
     */
    public boolean isSelectingTarget() {
        return isSelectingTarget;
    }
}
