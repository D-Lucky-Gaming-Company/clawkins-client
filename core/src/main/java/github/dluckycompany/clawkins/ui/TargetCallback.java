package github.dluckycompany.clawkins.ui;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Callback interface for decoupled target selection.
 * Allows Party Selection UI to be completely independent of Inventory logic.
 * 
 * Usage Pattern: PartySelectionOverlay selector = new PartySelectionOverlay(party, skin, font);
 * Set callback to handle target selection and cancellation events.
 */
public interface TargetCallback {
    /**
     * Invoked when a player clicks on a party member in the selection UI.
     * 
     * @param target the Clawkin that was selected
     */
    void onTargetSelected(Clawkin target);

    /**
     * Invoked when the player cancels the selection (e.g., presses Cancel button).
     */
    void onCancel();
}
