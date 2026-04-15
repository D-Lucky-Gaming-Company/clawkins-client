package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Barebones UI for displaying and selecting party members.
 * Shows all 3 clawkins with HP bars and allows selection.
 * Can be reused in inventory overlays, pause menus, and other screens.
 */
public class PartyMemberListUI extends Table {
    private final List<ClawkinCardUI> cards = new ArrayList<>();
    private final List<Clawkin> party;
    private final BitmapFont font;
    private ClawkinCardUI selectedCard;
    private int selectedIndex = 0;

    public PartyMemberListUI(List<Clawkin> party, BitmapFont font) {
        this.party = party;
        this.font = font;
        buildUI();
    }

    private void buildUI() {
        // Clear existing cards
        cards.clear();
        
        // Create a card for each party member
        for (Clawkin clawkin : party) {
            ClawkinCardUI card = new ClawkinCardUI(clawkin, font, 150f);
            cards.add(card);
            add(card).padTop(10).padBottom(10).padLeft(10).padRight(10).row();
        }
        
        // Select first party member by default
        if (!cards.isEmpty()) {
            setSelected(0);
        }
    }

    /**
     * Set the selected clawkin by index.
     *
     * @param index the party member index (0-2)
     */
    public void setSelected(int index) {
        if (index >= 0 && index < cards.size()) {
            selectedIndex = index;
            selectedCard = cards.get(index);
            highlightSelected();
        }
    }

    /**
     * Get the currently selected clawkin.
     *
     * @return the selected Clawkin, or null if none selected
     */
    public Clawkin getSelected() {
        return selectedCard != null ? selectedCard.getClawkin() : null;
    }

    /**
     * Get the index of the currently selected clawkin.
     *
     * @return the index (0-2), or -1 if none selected
     */
    public int getSelectedIndex() {
        return selectedCard != null ? selectedIndex : -1;
    }

    /**
     * Get all party member cards.
     *
     * @return list of ClawkinCardUI
     */
    public List<ClawkinCardUI> getCards() {
        return cards;
    }

    /**
     * Select next party member (cycles).
     */
    public void selectNext() {
        int nextIndex = (selectedIndex + 1) % cards.size();
        setSelected(nextIndex);
    }

    /**
     * Select previous party member (cycles).
     */
    public void selectPrevious() {
        int prevIndex = (selectedIndex - 1 + cards.size()) % cards.size();
        setSelected(prevIndex);
    }

    /**
     * Update all HP displays.
     * Call this when HP changes to refresh the UI.
     */
    public void updateAll() {
        for (ClawkinCardUI card : cards) {
            card.updateHp();
        }
    }

    private void highlightSelected() {
        for (int i = 0; i < cards.size(); i++) {
            ClawkinCardUI card = cards.get(i);
            // Simple highlight: change background color
            if (i == selectedIndex) {
                // Highlight selected card with a colored drawable
                com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(200, 100, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                pixmap.setColor(new com.badlogic.gdx.graphics.Color(0.3f, 0.3f, 0.5f, 1f));
                pixmap.fill();
                com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
                pixmap.dispose();
                card.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)));
            } else {
                card.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
            }
        }
    }
}
