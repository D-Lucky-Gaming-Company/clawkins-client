package github.dluckycompany.clawkins.item;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Interface for item effects that can be applied to clawkins.
 * Each item contains its own effect logic via this interface.
 * 
 * The apply() method returns a boolean to indicate whether the effect
 * was actually needed and applied. Example: healing item returns false
 * if the target is already at max HP.
 */
public interface ItemEffect {
    /**
     * Apply this effect to the target clawkin.
     * 
     * Returns true if the effect was actually applied (e.g., healing was needed).
     * Returns false if the effect was not appropriate for the target state
     * (e.g., healing a full-HP target).
     *
     * @param target the clawkin to apply the effect to
     * @return true if the effect was applied, false if not applicable
     */
    boolean apply(Clawkin target);

    /**
     * Get a human-readable description of this effect.
     *
     * @return effect description (e.g., "Heals 30 HP")
     */
    String getDescription();
}
