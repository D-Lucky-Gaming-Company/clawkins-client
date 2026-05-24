package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import github.dluckycompany.clawkins.character.Clawkin;

/**
 * Interactive Party Selection Overlay for target-based item usage.
 * 
 * Wraps each Clawkin in an interactive clickable component.
 * Implements Selection Callback Pattern for decoupled UI logic.
 * 
 * Features:
 * - ClickListeners on each party member card
 * - Touchable.enabled for FitViewport coordinate transform
 * - Visual feedback (highlight on hover/select)
 * - Modal dialog prevents input bleed-through to inventory
 * - FitViewport handles fullscreen resolution changes
 * 
 * Usage:
 *   PartySelectionOverlay overlay = new PartySelectionOverlay(party, skin, font);
 *   overlay.setTargetCallback((target) -> {
 *       // Execute item effect on target
 *       controller.executeUseAction(useAction);
 *   });
 *   stage.addActor(overlay);
 */
public class PartySelectionOverlay extends Dialog {
    private final List<Clawkin> party;
    private final Skin skin;
    private final BitmapFont font;
    private final List<SelectableClawkinEntry> selectableCards;
    
    private TargetCallback targetCallback;
    private Clawkin selectedClawkin;
    private int selectedIndex = -1;
    
    // Fixed virtual UI dimensions (must match parent stage)
    private static final float DIALOG_WIDTH = 300f;
    private static final float DIALOG_HEIGHT = 400f;
    private static final float CARD_WIDTH = 250f;
    private static final float CARD_HEIGHT = 70f;
    private static final float PADDING = 10f;

    public PartySelectionOverlay(List<Clawkin> party, Skin skin, BitmapFont font) {
        super("Select Target", skin);
        this.party = party;
        this.skin = skin;
        this.font = font;
        this.selectableCards = new ArrayList<>();
        
        buildUI();
        setModal(true);  // Prevent input bleed-through to inventory underneath
        setMovable(false);  // Fixed position
        setResizable(false);
    }

    private void buildUI() {
        Table contentTable = getContentTable();
        contentTable.clear();
        contentTable.pad(PADDING);
        
        // Create root container
        Table root = new Table();
        root.defaults().padBottom(PADDING).width(CARD_WIDTH).height(CARD_HEIGHT);
        
        // Build a selectable card for each party member
        for (Clawkin clawkin : party) {
            SelectableClawkinCard card = createSelectableCard(clawkin);
            selectableCards.add(card);
            root.add(card).row();
        }
        
        contentTable.add(root).expand().fill().row();
        
        // Cancel button at bottom
        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onCancel();
            }
        });
        contentTable.add(cancelButton)
            .width(CARD_WIDTH)
            .height(40)
            .padTop(PADDING);
        
        // Dialog sizing and positioning (center in 800x600 virtual space)
        setWidth(DIALOG_WIDTH);
        setHeight(DIALOG_HEIGHT);
        setPosition(400 - DIALOG_WIDTH / 2, 300 - DIALOG_HEIGHT / 2);
    }

    /**
     * Create a clickable, selectable wrapper around a Clawkin card.
     * 
     * Implementation Details:
     * 1. ClawkinCardUI provides the visual display
     * 2. SelectableClawkinCard extends Table to become a scene2d actor
     * 3. ClickListener captures input and passes selectedClawkin to callback
     * 4. Touchable.enabled ensures FitViewport coordinate transformation works
     */
    private SelectableClawkinCard createSelectableCard(Clawkin clawkin) {
        SelectableClawkinCard selectableCard = new SelectableClawkinCard(clawkin, font);
        selectableCard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClawkinSelected(clawkin, selectableCard);
            }
        });
        return selectableCard;
    }

    /**
     * Set the callback to invoke when a target is selected or cancelled.
     */
    public void setTargetCallback(TargetCallback callback) {
        this.targetCallback = callback;
    }

    /**
     * Internal handler for when a Clawkin card is clicked.
     * Updates selection state and executes callback.
     */
    private void onClawkinSelected(Clawkin clawkin, SelectableClawkinEntry card) {
        // Clear previous selection
        for (SelectableClawkinEntry c : selectableCards) {
            c.setSelected(false);
        }
        
        // Set new selection
        this.selectedClawkin = clawkin;
        this.selectedIndex = party.indexOf(clawkin);
        card.setSelected(true);
        
        System.out.println("[PartySelectionOverlay] Selected: " + clawkin.getName());
        
        // Execute callback with the selected target
        if (targetCallback != null) {
            targetCallback.onTargetSelected(clawkin);
        }
    }

    /**
     * Internal handler for Cancel button.
     */
    private void onCancel() {
        System.out.println("[PartySelectionOverlay] Cancelled");
        if (targetCallback != null) {
            targetCallback.onCancel();
        }
    }

    /**
     * Get the currently selected clawkin.
     */
    public Clawkin getSelectedClawkin() {
        return selectedClawkin;
    }

    /**
     * Get the index of the selected clawkin.
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Nested inner class: Wraps ClawkinCardUI in an interactive, selectable container.
     * 
     * This class bridges the gap between:
     * - ClawkinCardUI (display-only)
     * - Scene2D interaction model (touchable actors with click listeners)
     * 
     * Each SelectableClawkinCard is a Table that:
     * 1. Contains a ClawkinCardUI for rendering
     * 2. Has a SelectableClawkinCard is a Table that contains a ClawkinCardUI
     * 2. Can be clicked via ClickListener
     * 3. Maintains selection state (visual feedback)
     * 4. Is automatically managed by Stage input processor
     */
    public static class SelectableClawkinCard extends AbstractSelectableClawkinTable {
        private final BitmapFont font;
        private ClawkinCardUI card;

        public SelectableClawkinCard(Clawkin clawkin, BitmapFont font) {
            super(
                clawkin,
                createSolidDrawable(new Color(0.3f, 0.5f, 0.8f, 0.8f)),
                createSolidDrawable(new Color(0.2f, 0.2f, 0.2f, 0.6f))
            );
            this.font = font;
            composeLayout();
        }

        @Override
        protected void buildLayout() {
            card = (ClawkinCardUI) createInnerCard();
            mountInnerCard(card);
        }

        @Override
        protected ClawkinHpDisplay createInnerCard() {
            return new ClawkinCardUI(clawkin, font, 200f);
        }

        @Override
        protected void mountInnerCard(ClawkinHpDisplay card) {
            pad(PADDING);
            add((Table) card).expand().fill();
            setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        }

        @Override
        protected void onSelectionChanged(boolean selected) {
            if (selected) {
                System.out.println("[SelectableClawkinCard] " + clawkin.getName() + " is now selected");
            }
        }
    }
}
