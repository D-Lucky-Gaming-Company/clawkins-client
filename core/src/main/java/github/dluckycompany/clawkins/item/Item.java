package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Represents an item in the inventory.
 * Items hold their own effect logic via the ItemEffect interface.
 */
public class Item {
    public enum ItemType {
        POTION,      // Healing item
        REVIVE,      // Revive item
        STAT_BOOSTER, // Stat boosting item
        SPECIAL      // Special items (e.g., level boost, rare items)
    }

    private final String id;
    private final String name;
    private final String description;
    private final ItemType type;
    private final ItemEffect effect;
    private final int sellPrice;
    private final boolean usableInBattle;
    private final String imageName;  // Asset filename (without .png) for dynamic texture loading

    public Item(String id, String name, String description, ItemType type, ItemEffect effect, int sellPrice, boolean usableInBattle, String imageName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.effect = effect;
        this.sellPrice = Math.max(0, sellPrice);
        this.usableInBattle = usableInBattle;
        this.imageName = imageName;
    }

    // ============ Properties ============
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getType() {
        return type;
    }

    public int getSellPrice() {
        return sellPrice;
    }

    public boolean isUsableInBattle() {
        return usableInBattle;
    }

    public String getImageName() {
        return imageName;
    }

    /**
     * Get the item's effect instance.
     * Used for checking effect types (e.g., instanceof LevelBoostEffect).
     * 
     * @return the ItemEffect instance, or null if no effect
     */
    public ItemEffect getEffect() {
        return effect;
    }

    // ============ Effect Application ============
    
    /**
     * Apply this item's effect to the target clawkin.
     * Item logic lives here - all effects are self-contained.
     * 
     * Returns the result from the ItemEffect, which indicates whether
     * the effect was actually needed and applied (e.g., true if healing
     * was applied, false if target was already at max HP).
     *
     * @param target the clawkin to apply the effect to
     * @return true if the effect was applied, false if not applicable
     */
    public boolean applyEffectTo(Clawkin target) {
        if (effect != null && target != null) {
            return effect.apply(target);
        }
        return false;
    }

    public String getEffectDescription() {
        return effect != null ? effect.getDescription() : "No effect";
    }

    // ============ Equality ============
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Item)) return false;
        Item other = (Item) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
