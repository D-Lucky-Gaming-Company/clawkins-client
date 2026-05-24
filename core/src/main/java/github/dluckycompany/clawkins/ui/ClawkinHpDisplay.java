package github.dluckycompany.clawkins.ui;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Contract for widgets that display a Clawkin's current HP.
 */
public interface ClawkinHpDisplay {

    void updateHp();

    Clawkin getClawkin();

    default boolean isEmpty() {
        return getClawkin() == null;
    }
}
