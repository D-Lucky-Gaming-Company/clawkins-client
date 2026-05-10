package github.dluckycompany.clawkins.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MerchantInventory - Manages items available for purchase from a merchant
 * 
 * Features:
 * - Stock tracking (limited quantities per item)
 * - Custom pricing per merchant
 * - Infinite stock mode (for basic merchants)
 */
public class MerchantInventory {
    private final Map<Item, Integer> stock;  // Item → quantity available (-1 = infinite)
    private final Map<Item, Integer> buyPrices;  // Item → buy price (what player pays)
    private final boolean infiniteStock;
    
    /**
     * Create a merchant inventory with infinite stock
     */
    public MerchantInventory() {
        this.stock = new HashMap<>();
        this.buyPrices = new HashMap<>();
        this.infiniteStock = true;
    }
    
    /**
     * Create a merchant inventory with limited stock
     */
    public MerchantInventory(boolean infiniteStock) {
        this.stock = new HashMap<>();
        this.buyPrices = new HashMap<>();
        this.infiniteStock = infiniteStock;
    }
    
    /**
     * Add an item to the merchant's inventory
     * 
     * @param item The item to add
     * @param quantity The quantity available (-1 for infinite)
     * @param buyPrice The price the player pays to buy this item
     */
    public void addItem(Item item, int quantity, int buyPrice) {
        if (item == null) {
            return;
        }
        stock.put(item, infiniteStock ? -1 : quantity);
        buyPrices.put(item, buyPrice);
    }
    
    /**
     * Add an item with default buy price (2x sell price)
     */
    public void addItem(Item item, int quantity) {
        addItem(item, quantity, item.getSellPrice() * 2);
    }
    
    /**
     * Get all items available for purchase
     */
    public List<Item> getAllItems() {
        return new ArrayList<>(stock.keySet());
    }
    
    /**
     * Check if merchant has an item in stock
     */
    public boolean hasItem(Item item) {
        return stock.containsKey(item) && getQuantity(item) != 0;
    }
    
    /**
     * Check if merchant has enough quantity of an item
     */
    public boolean hasStock(Item item, int quantity) {
        if (!stock.containsKey(item)) {
            return false;
        }
        int available = stock.get(item);
        return available == -1 || available >= quantity;  // -1 = infinite stock
    }
    
    /**
     * Get quantity available for an item
     * 
     * @return quantity available, or -1 for infinite stock
     */
    public int getQuantity(Item item) {
        return stock.getOrDefault(item, 0);
    }
    
    /**
     * Get the buy price for an item (what player pays)
     */
    public int getBuyPrice(Item item) {
        return buyPrices.getOrDefault(item, item.getSellPrice() * 2);
    }
    
    /**
     * Remove stock when player buys an item
     * 
     * @return true if stock was removed, false if not enough stock
     */
    public boolean removeStock(Item item, int quantity) {
        if (!hasStock(item, quantity)) {
            return false;
        }
        
        int currentStock = stock.get(item);
        if (currentStock == -1) {
            // Infinite stock - don't decrease
            return true;
        }
        
        stock.put(item, currentStock - quantity);
        return true;
    }
    
    /**
     * Restock an item (add more quantity)
     */
    public void restock(Item item, int quantity) {
        if (!stock.containsKey(item)) {
            return;
        }
        
        int currentStock = stock.get(item);
        if (currentStock == -1) {
            // Already infinite stock
            return;
        }
        
        stock.put(item, currentStock + quantity);
    }
    
    /**
     * Check if this merchant has infinite stock
     */
    public boolean isInfiniteStock() {
        return infiniteStock;
    }
    
    /**
     * Create a default merchant inventory with common items
     */
    public static MerchantInventory createDefaultInventory() {
        MerchantInventory inventory = new MerchantInventory(true);  // Infinite stock
        
        // Add common items (using ItemFactory static fields)
        // Potions
        inventory.addItem(ItemFactory.BASIC_POTION, -1, 50);
        inventory.addItem(ItemFactory.FULL_HEAL, -1, 200);
        
        // Stat boosters
        inventory.addItem(ItemFactory.ATTACK_BOOST, -1, 100);
        inventory.addItem(ItemFactory.DEFENSE_BOOST, -1, 100);
        
        // Revive
        inventory.addItem(ItemFactory.REVIVE, -1, 300);
        
        return inventory;
    }
}
