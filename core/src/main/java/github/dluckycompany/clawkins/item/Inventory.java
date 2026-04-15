package github.dluckycompany.clawkins.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inventory management system.
 * Items are tracked by type with quantities (no slot system).
 */
public class Inventory {
    // Item -> Quantity mapping
    private final Map<Item, Integer> items = new HashMap<>();

    // ============ Add/Remove Items ============
    
    /**
     * Add item(s) to inventory.
     *
     * @param item the item to add
     * @param quantity number of items to add (must be positive)
     */
    public void addItem(Item item, int quantity) {
        if (item == null || quantity <= 0) {
            return;
        }
        items.put(item, items.getOrDefault(item, 0) + quantity);
    }

    /**
     * Remove item(s) from inventory.
     *
     * @param item the item to remove
     * @param quantity number of items to remove
     * @return true if successfully removed, false if not enough items
     */
    public boolean removeItem(Item item, int quantity) {
        if (item == null || quantity <= 0) {
            return false;
        }
        
        int current = items.getOrDefault(item, 0);
        if (current < quantity) {
            return false;
        }
        
        if (current == quantity) {
            items.remove(item);
        } else {
            items.put(item, current - quantity);
        }
        return true;
    }

    /**
     * Drop all of an item from inventory.
     *
     * @param item the item to drop
     */
    public void dropAllOfItem(Item item) {
        items.remove(item);
    }

    /**
     * Clear entire inventory.
     */
    public void clear() {
        items.clear();
    }

    // ============ Query Items ============
    
    /**
     * Get quantity of an item in inventory.
     *
     * @param item the item to check
     * @return quantity (0 if not present)
     */
    public int getQuantity(Item item) {
        return items.getOrDefault(item, 0);
    }

    /**
     * Check if inventory has enough of an item.
     *
     * @param item the item to check
     * @param quantity the minimum quantity needed
     * @return true if inventory has at least that quantity
     */
    public boolean hasItem(Item item, int quantity) {
        return getQuantity(item) >= quantity;
    }

    /**
     * Check if inventory contains an item (any quantity).
     *
     * @param item the item to check
     * @return true if inventory has this item
     */
    public boolean contains(Item item) {
        return items.containsKey(item);
    }

    /**
     * Get all items in inventory.
     *
     * @return list of unique items (quantities available via getQuantity())
     */
    public List<Item> getAllItems() {
        return new ArrayList<>(items.keySet());
    }

    /**
     * Get total number of items (counting quantities).
     *
     * @return total item count
     */
    public int getTotalItemCount() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Check if inventory is empty.
     *
     * @return true if no items present
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Get count of unique item types.
     *
     * @return number of different items
     */
    public int getUniqueItemCount() {
        return items.size();
    }
}
