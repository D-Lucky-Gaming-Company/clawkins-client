package github.dluckycompany.clawkins.ui;

import java.util.List;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Contract for party rosters that support index-based selection and HP refresh.
 */
public interface PartyMemberListView {

    void setSelected(int index);

    Clawkin getSelected();

    int getSelectedIndex();

    List<? extends ClawkinHpDisplay> getCards();

    void selectNext();

    void selectPrevious();

    void updateAll();
}
