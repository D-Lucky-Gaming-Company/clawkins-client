package github.dluckycompany.clawkins.ui;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Callback for team-viewer card selection events.
 */
@FunctionalInterface
public interface CardSelectionCallback {

    /**
     * Invoked when a card is clicked.
     *
     * @param slotIndex the slot index (0-2)
     * @param clawkin   the Clawkin in that slot
     */
    void onCardSelected(int slotIndex, Clawkin clawkin);
}
