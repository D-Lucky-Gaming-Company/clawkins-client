package github.dluckycompany.clawkins.ui;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Contract for clickable party-member entries used in target selection flows.
 */
public interface SelectableClawkinEntry extends SelectableUi {

    Clawkin getClawkin();
}
