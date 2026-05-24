package github.dluckycompany.clawkins.ui;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Barebones UI for displaying and selecting party members.
 * Shows all 3 clawkins with HP bars and allows selection.
 * Can be reused in inventory overlays, pause menus, and other screens.
 */
public class PartyMemberListUI extends AbstractPartyMemberListTable {

    public PartyMemberListUI(List<Clawkin> party, BitmapFont font) {
        super(party, font);
    }

    @Override
    protected ClawkinHpDisplay createCard(Clawkin clawkin) {
        return new ClawkinCardUI(clawkin, font, 150f);
    }

    @Override
    protected void addCardRow(ClawkinHpDisplay card) {
        add((Table) card).padTop(10).padBottom(10).padLeft(10).padRight(10).row();
    }

    @Override
    protected void highlightSelected() {
        for (int i = 0; i < cards.size(); i++) {
            Table card = (Table) cards.get(i);
            if (i == selectedIndex) {
                card.setBackground(createSolidDrawable(new Color(0.3f, 0.3f, 0.5f, 1f), 200, 100));
            } else {
                card.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
        }
    }
}
