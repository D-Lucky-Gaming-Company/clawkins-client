package github.dluckycompany.clawkins.ui;

/**
 * Full-featured party card used in the team viewer, including battle-fighter state.
 */
public interface ClawkinCardView extends ClawkinHpDisplay, SelectableUi {

    void refreshStats();

    void setSharedExperience(int totalExp);

    void setActiveFighter(boolean active);

    boolean isActiveFighter();
}
