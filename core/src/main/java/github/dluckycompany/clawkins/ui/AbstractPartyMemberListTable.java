package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Base party roster table with shared index-based selection and HP refresh logic.
 */
public abstract class AbstractPartyMemberListTable extends AbstractUiTable implements PartyMemberListView {

    protected final List<ClawkinHpDisplay> cards = new ArrayList<>();
    protected final List<Clawkin> party;
    protected final BitmapFont font;
    protected ClawkinHpDisplay selectedCard;
    protected int selectedIndex;

    protected AbstractPartyMemberListTable(List<Clawkin> party, BitmapFont font) {
        this.party = party;
        this.font = font;
        composeLayout();
    }

    @Override
    protected void buildLayout() {
        cards.clear();
        for (Clawkin clawkin : party) {
            ClawkinHpDisplay card = createCard(clawkin);
            cards.add(card);
            addCardRow(card);
        }
        if (!cards.isEmpty()) {
            setSelected(0);
        }
    }

    protected abstract ClawkinHpDisplay createCard(Clawkin clawkin);

    protected abstract void addCardRow(ClawkinHpDisplay card);

    protected abstract void highlightSelected();

    @Override
    public void setSelected(int index) {
        if (index >= 0 && index < cards.size()) {
            selectedIndex = index;
            selectedCard = cards.get(index);
            highlightSelected();
        }
    }

    @Override
    public Clawkin getSelected() {
        return selectedCard != null ? selectedCard.getClawkin() : null;
    }

    @Override
    public int getSelectedIndex() {
        return selectedCard != null ? selectedIndex : -1;
    }

    @Override
    public List<? extends ClawkinHpDisplay> getCards() {
        return cards;
    }

    @Override
    public void selectNext() {
        int nextIndex = (selectedIndex + 1) % cards.size();
        setSelected(nextIndex);
    }

    @Override
    public void selectPrevious() {
        int prevIndex = (selectedIndex - 1 + cards.size()) % cards.size();
        setSelected(prevIndex);
    }

    @Override
    public void updateAll() {
        for (ClawkinHpDisplay card : cards) {
            card.updateHp();
        }
    }
}
