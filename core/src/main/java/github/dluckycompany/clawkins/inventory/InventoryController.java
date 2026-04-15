package github.dluckycompany.clawkins.inventory;

import java.util.function.Consumer;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;

/**
 * Inventory Controller with Command Pattern
 * 
 * Decouples UI button clicks from actual inventory state modifications.
 * Manages Drop and Use commands as separate, reusable command objects.
 * 
 * Usage:
 *   InventoryController controller = new InventoryController(inventory);
 *   controller.setOnInventoryChanged(() -> refreshUI());
 *   
 *   // Drop command
 *   controller.dropItem(item);
 *   
 *   // Use command (three-phase flow)
 *   ItemUseAction useAction = controller.beginUseItem(item);
 *   // ... show party selection ...
 *   useAction.setTarget(selectedClawkin);
 *   controller.executeUseAction(useAction);
 */
public class InventoryController {
    private final Inventory inventory;
    private Consumer<String> onInventoryChanged;
    private Runnable onActionLog;

    public InventoryController(Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Register callback for inventory changes.
     * Called whenever an item is dropped or used.
     */
    public void setOnInventoryChanged(Consumer<String> callback) {
        this.onInventoryChanged = callback;
    }

    /**
     * Register callback for action logging.
     * Called when item use actions complete.
     */
    public void setOnActionLog(Runnable callback) {
        this.onActionLog = callback;
    }

    /**
     * Triggers the action log callback if registered.
     * Used for tracking when actions are logged (future extensibility).
     */
    protected void triggerActionLog() {
        if (this.onActionLog != null) {
            this.onActionLog.run();
        }
    }

    /**
     * Drop Command: Remove a specific quantity of an item from inventory.
     * Transactional: validates quantity before removing.
     * 
     * @param item the item to drop
     * @param quantity the amount to drop (must be between 1 and current quantity)
     * @return true if items were dropped, false if validation failed
     */
    public boolean dropItemQuantity(Item item, int quantity) {
        if (item == null || quantity <= 0) {
            System.out.println("[InventoryController] Invalid drop: item null or quantity " + quantity + " <= 0");
            return false;
        }

        int currentQuantity = inventory.getQuantity(item);
        if (currentQuantity < quantity) {
            System.out.println("[InventoryController] Cannot drop " + quantity + " of " + item.getName() + 
                             ": only " + currentQuantity + " in inventory");
            return false;
        }

        // Transactional: remove items all at once
        boolean removed = inventory.removeItem(item, quantity);

        if (removed) {
            String message;
            if (quantity == 1) {
                message = "Dropped 1 " + item.getName();
            } else if (currentQuantity == quantity) {
                message = "Dropped all " + item.getName() + " (" + quantity + ")";
            } else {
                message = "Dropped " + quantity + " " + item.getName();
            }

            if (onInventoryChanged != null) {
                onInventoryChanged.accept(message);
            }

            System.out.println("[InventoryController] " + message);
            return true;
        }

        return false;
    }

    /**
     * Legacy Drop Command: Remove all of an item from inventory.
     * Maintained for compatibility.
     *
     * @param item the item to drop
     * @return true if item was dropped, false if not in inventory
     */
    @Deprecated
    public boolean dropItem(Item item) {
        if (item == null) {
            return false;
        }

        int quantity = inventory.getQuantity(item);
        if (quantity <= 0) {
            return false;
        }

        return dropItemQuantity(item, quantity);
    }

    /**
     * Use Command (Phase A): Create a deferred use action.
     * Transitions UI to TARGET_SELECTION state.
     * 
     * @param item the item to use
     * @return ItemUseAction command object for Phase B/C
     */
    public ItemUseAction beginUseItem(Item item) {
        if (item == null || !inventory.hasItem(item, 1)) {
            return null;
        }

        System.out.println("[InventoryController] Phase A - Entering TARGET_SELECTION for " + item.getName());
        return new ItemUseAction(item);
    }

    /**
     * Use Command (Phase C): Execute the use action on selected target.
     * Validates target state, applies effect, and decrements inventory if successful.
     * This is a TRANSACTIONAL operation: either fully succeeds or fully fails.
     *
     * @param action the ItemUseAction with target already set
     * @return true if item was successfully used and consumed
     */
    public boolean executeUseAction(ItemUseAction action) {
        if (action == null) {
            return false;
        }

        Item item = action.getItem();
        Clawkin target = action.getTarget();

        if (item == null || target == null) {
            return false;
        }

        // Check inventory before attempting use
        if (!inventory.hasItem(item, 1)) {
            System.out.println("[InventoryController] Cannot use " + item.getName() + ": not in inventory");
            return false;
        }

        // TRANSACTION BEGINS: Execute the use action (Phase C)
        // This applies effect AND validates target state
        boolean useSucceeded = action.execute();

        if (!useSucceeded) {
            System.out.println("[InventoryController] Use action failed validation");
            return false;
        }

        // TRANSACTION CONTINUES: Decrement item from inventory after effect applied
        boolean removed = inventory.removeItem(item, 1);

        if (removed) {
            // TRANSACTION SUCCEEDS: Fire UI callback
            if (onInventoryChanged != null) {
                onInventoryChanged.accept("Used " + item.getName() + " on " + target.getName());
            }

            System.out.println("[InventoryController] Phase C - Successfully used " + item.getName() + " on " + target.getName());
            return true;
        }

        // TRANSACTION FAILED: Item wasn't removed for some reason
        System.out.println("[InventoryController] Failed to decrement item from inventory");
        return false;
    }

    /**
     * Get the current inventory.
     * Allows read-only access for UI display.
     */
    public Inventory getInventory() {
        return inventory;
    }
}
