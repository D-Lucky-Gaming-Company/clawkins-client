package github.dluckycompany.clawkins.inventory;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Item;

/**
 * Command object representing an item use action.
 * Encapsulates the three-phase workflow: Selection -> Validation -> Application
 * 
 * Phase A (Selection): System enters TARGET_SELECTION state when item's Use button is clicked.
 * Phase B (Validation): Party members are presented to the player for selection.
 * Phase C (Application): Upon target selection, item effect is applied, and item is consumed if effect succeeds.
 */
public class ItemUseAction {
    private final Item item;
    private Clawkin selectedTarget;
    private boolean completed = false;

    public ItemUseAction(Item item) {
        this.item = item;
        this.selectedTarget = null;
    }

    /**
     * Check if this action has been completed.
     *
     * @return true if item was used and consumed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Set the target clawkin for this use action (Phase B/C boundary).
     */
    public void setTarget(Clawkin target) {
        this.selectedTarget = target;
    }

    /**
     * Get the currently selected target.
     */
    public Clawkin getTarget() {
        return selectedTarget;
    }

    /**
     * Execute the use action on the selected target (Phase C).
     * Validates target state and applies item effect.
     * Returns true only if the item was successfully used.
     *
     * @return true if item was successfully used, false if validation failed
     */
    public boolean execute() {
        if (selectedTarget == null || item == null) {
            System.out.println("[ItemUseAction] Cannot execute: target or item is null");
            return false;
        }

        // Validate: Check if item can be used on this target
        if (!validateUseOnTarget()) {
            System.out.println("[ItemUseAction] Validation failed: item cannot be used on " + selectedTarget.getName());
            return false;
        }

        // Apply the item effect to target
        int hpBefore = selectedTarget.getCurrentHp();
        item.applyEffectTo(selectedTarget);
        int hpAfter = selectedTarget.getCurrentHp();

        // Log only if HP actually changed
        if (hpAfter != hpBefore) {
            System.out.println("[ItemUseAction] Item used successfully on " + selectedTarget.getName() + 
                             ": HP " + hpBefore + " → " + hpAfter);
            completed = true;
            return true;
        }

        System.out.println("[ItemUseAction] Item had no effect on " + selectedTarget.getName());
        return false;
    }

    /**
     * Validate if this item can be used on the target.
     * For healing items: target must not be at full HP.
     * For revive items: target must be fainted (0 HP).
     */
    private boolean validateUseOnTarget() {
        if (item == null || selectedTarget == null) {
            return false;
        }

        // For healing items: check if target is not at full HP
        if (item.getType() == github.dluckycompany.clawkins.item.Item.ItemType.POTION) {
            return selectedTarget.getCurrentHp() < selectedTarget.getMaxHp();
        }

        // For revive items: check if target is fainted
        if (item.getType() == github.dluckycompany.clawkins.item.Item.ItemType.REVIVE) {
            return selectedTarget.getCurrentHp() <= 0;
        }

        // Other item types can always be used
        return true;
    }

    /**
     * Get the item being used.
     */
    public Item getItem() {
        return item;
    }
}
