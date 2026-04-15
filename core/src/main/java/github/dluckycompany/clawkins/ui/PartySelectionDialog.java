package github.dluckycompany.clawkins.ui;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;

/**
 * PartySelectionDialog - Data-Bound Modal for Item Application
 * 
 * Implements the "Data-Bound Actor Pattern":
 * - Each party member row is a Table with Touchable.enabled
 * - The Clawkin instance is bound via setUserObject(clawkin)
 * - ClickListener retrieves the Clawkin and applies item effect
 * - Inventory is decremented and dialog is closed
 * 
 * ARCHITECTURE:
 * 1. Row Factory: Creates Table rows with data-binding
 * 2. Selection Callback: Validates item, applies it, decrements inventory
 * 3. State Reconciliation: Triggers inventory UI refresh after transaction
 * 4. Viewport Sync: Dialog centers in FitViewport(800, 600)
 */
public class PartySelectionDialog extends Dialog {

    private final List<Clawkin> party;
    private final Item item;
    private final Inventory inventory;
    private final BitmapFont font;
    
    // Post-transaction callback to refresh inventory UI
    private Runnable onItemApplied;
    private Runnable onClosed;

    private final List<Table> rowActors = new java.util.ArrayList<>();
    private final List<Clawkin> rowTargets = new java.util.ArrayList<>();
    private int selectedRowIndex = -1;

    private static final Color ORANGE = new Color(1.0f, 0.64f, 0.0f, 1.0f);
    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;

    /**
     * Constructor
     *
     * @param party The list of party members (Swee'pea, Ginger, Dart)
     * @param item The item being used
     * @param inventory The inventory to decrement from (for transaction)
     * @param skin The Scene2D Skin for styling
     * @param font The BitmapFont for text
     */
    public PartySelectionDialog(List<Clawkin> party, Item item, Inventory inventory, Skin skin, BitmapFont font) {
        super("Use " + item.getName(), skin);
        this.party = party;
        this.item = item;
        this.inventory = inventory;
        this.font = font;

        setModal(true);      // Prevent input bleed-through to inventory
        setMovable(false);   // Fixed position in viewport center
        setResizable(false); // Fixed size

        buildContent();
    }

    /**
     * Build the dialog content with data-bound party member selection rows
     */
    private void buildContent() {
        // Title row
        Table titleTable = new Table();
        Label titleLabel = new Label("Select a target for " + item.getName(), new Label.LabelStyle(font, ORANGE));
        titleLabel.setFontScale(1.6f);
        titleTable.add(titleLabel).pad(10f).expandX().fillX();
        getContentTable().add(titleTable).expandX().fillX().row();

        // Item effect description
        Label effectLabel = new Label("Effect: " + item.getEffectDescription(), new Label.LabelStyle(font, WHITE));
        effectLabel.setFontScale(1.2f);
        getContentTable().add(effectLabel).pad(10f).expandX().fillX().row();

        // Separator
        getContentTable().add().height(10f).row();

        // Party member selection rows (data-bound)
        if (party != null && !party.isEmpty()) {
            for (Clawkin clawkin : party) {
                createDataBoundPartyMemberRow(clawkin);
            }
            if (!rowActors.isEmpty()) {
                selectedRowIndex = 0;
                refreshRowHighlight();
            }
        } else {
            Label emptyLabel = new Label("No party members available", new Label.LabelStyle(font, WHITE));
            getContentTable().add(emptyLabel).pad(20f).row();
        }

        // Separator
        getContentTable().add().height(10f).row();

        // Cancel button
        TextButton cancelBtn = new TextButton("Cancel", new TextButton.TextButtonStyle(
            new ColorDrawable(ORANGE),
            new ColorDrawable(ORANGE),
            new ColorDrawable(BLACK),
            font
        ));
        cancelBtn.getLabel().setFontScale(1.2f);
        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeDialog();
            }
        });
        getContentTable().add(cancelBtn).expandX().fillX().height(50).row();
    }

    /**
     * Create a clickable data-bound row for a party member.
     * 
     * ROW PATTERN:
     * - Table with Touchable.enabled
     * - setUserObject(clawkin) binds the Clawkin instance
     * - ClickListener retrieves userObject and applies item transaction
     * 
     * @param clawkin The party member to add to selection list
     */
    private void createDataBoundPartyMemberRow(final Clawkin clawkin) {
        // Create the row Table
        Table memberRow = new Table();
        memberRow.setFillParent(false);
        memberRow.setBackground(new ColorDrawable(BLACK));
        memberRow.pad(10f);
        memberRow.setTouchable(Touchable.enabled);  // Enable touch input on this Table
        
        // CRITICAL: Bind the Clawkin data to the UI Actor
        // When the row is clicked, the ClickListener will retrieve this via getUserObject()
        memberRow.setUserObject(clawkin);

        // Member name (large, orange)
        Label nameLabel = new Label(clawkin.getName(), new Label.LabelStyle(font, ORANGE));
        nameLabel.setFontScale(1.8f);
        memberRow.add(nameLabel).width(150).left();

        // HP display (white text, shows current/max)
        int currentHp = clawkin.getCurrentHp();
        int maxHp = clawkin.getMaxHp();
        float hpPercent = (float) currentHp / maxHp;
        
        // Color HP text based on percentage: green (full), orange (medium), red (low)
        Color hpColor = hpPercent > 0.5f ? WHITE : (hpPercent > 0.25f ? ORANGE : Color.RED);
        String hpText = currentHp + "/" + maxHp + " HP";
        Label hpLabel = new Label(hpText, new Label.LabelStyle(font, hpColor));
        hpLabel.setFontScale(1.4f);
        memberRow.add(hpLabel).expandX().right().padLeft(20f);

        // ============================================================
        // CLICK LISTENER - Executes the item transaction
        // ============================================================
        memberRow.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Retrieve the data-bound Clawkin from userObject
                Object userObject = ((Table) event.getListenerActor()).getUserObject();
                if (userObject instanceof Clawkin selectedTarget) {
                    selectedRowIndex = rowTargets.indexOf(selectedTarget);
                    refreshRowHighlight();
                    onClawkinSelected(selectedTarget);
                }
            }
        });

        rowActors.add(memberRow);
        rowTargets.add(clawkin);

        // Add the row to the dialog with proper spacing
        getContentTable().add(memberRow).expandX().fillX().height(70).padBottom(5f).row();
    }

    /**
     * TRANSACTION HANDLER - Called when a party member row is clicked
     * 
     * This method implements the complete item use transaction:
     * 1. Validation: Check if item can be used on target
     * 2. Application: item.applyEffectTo(target) modifies target state
     * 3. Consumption: inventory.removeItem(item, 1) decrements count
     * 4. Feedback: Log the result and close dialog
     * 5. Reconciliation: Trigger inventory UI refresh
     * 
     * @param target The selected Clawkin to apply the item to
     */
    private void onClawkinSelected(Clawkin target) {
        if (target == null || item == null || inventory == null) {
            Gdx.app.error("PartySelectionDialog", "Invalid transaction state");
            return;
        }

        try {
            // ============ PHASE 1: VALIDATION ============
            // Verify the item can be used on this target
            // Example: Potion only heals if HP < Max HP
            boolean canUseOnTarget = canUseItemOn(target);
            
            if (!canUseOnTarget) {
                Gdx.app.log("PartySelectionDialog", target.getName() + " doesn't need " + item.getName());
                return;  // Don't close dialog, allow user to select someone else
            }

            // Record previous HP for feedback
            int previousHp = target.getCurrentHp();

            // ============ PHASE 2: APPLICATION ============
            // Apply the item's effect to the target
            // This modifies the target's state (e.g., restores HP)
            // Returns true if the effect was actually applied, false if not needed
            boolean effectApplied = item.applyEffectTo(target);

            if (!effectApplied) {
                Gdx.app.log("PartySelectionDialog", "Item effect was not applicable to " + target.getName());
                return;  // Don't close dialog, allow user to select someone else
            }

            // Record new HP after effect
            int newHp = target.getCurrentHp();
            int hpRestored = newHp - previousHp;

            // ============ PHASE 3: CONSUMPTION ============
            // Only decrement inventory if the effect was actually applied
            boolean removed = inventory.removeItem(item, 1);
            
            if (!removed) {
                Gdx.app.error("PartySelectionDialog", "Failed to decrement " + item.getName() + " from inventory");
                return;
            }

            // ============ PHASE 4: FEEDBACK ============
            // Log the successful transaction
            String feedback = String.format(
                "[Item Use] %s used %s on %s: %d → %d HP (+%d)",
                "Player",
                item.getName(),
                target.getName(),
                previousHp,
                newHp,
                hpRestored
            );
            Gdx.app.log("PartySelectionDialog", feedback);

            // ============ PHASE 5: STATE RECONCILIATION ============
            // Trigger the callback to refresh inventory UI
            // This removes the item from the list and updates display
            if (onItemApplied != null) {
                onItemApplied.run();
            }

            // Close the dialog
            closeDialog();

        } catch (Exception e) {
            Gdx.app.error("PartySelectionDialog", "Transaction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate whether the item can be used on this target.
     * 
     * Checks item-specific constraints:
     * - Healing items: Target must have HP < Max HP
     * - Status items: Target must not already have the status
     * 
     * @param target The target to validate
     * @return true if the item can be used, false otherwise
     */
    private boolean canUseItemOn(Clawkin target) {
        if (target == null) {
            return false;
        }

        // Example logic: Healing items can only be used if target is damaged
        return switch (item.getType()) {
            case POTION -> target.getCurrentHp() < target.getMaxHp();  // Can only heal if HP < Max HP
            case REVIVE -> target.getCurrentHp() == 0;                // Can only revive if target is KO'd
            case STAT_BOOSTER -> true;                                 // Always usable
        };
    }

    /**
     * Set the callback to trigger inventory UI refresh after item use.
     * 
     * This callback is invoked after the transaction completes successfully,
     * ensuring the inventory display is updated to reflect the new item count.
     * 
     * @param callback The refresh callback
     */
    public void setOnItemApplied(Runnable callback) {
        this.onItemApplied = callback;
    }

    public void setOnClosed(Runnable callback) {
        this.onClosed = callback;
    }

    public boolean handleNavigationKey(int keycode) {
        if (rowTargets.isEmpty()) {
            if (keycode == Input.Keys.ESCAPE) {
                closeDialog();
                return true;
            }
            return false;
        }

        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.UP:
                selectedRowIndex = (selectedRowIndex - 1 + rowTargets.size()) % rowTargets.size();
                refreshRowHighlight();
                return true;

            case Input.Keys.S:
            case Input.Keys.DOWN:
                selectedRowIndex = (selectedRowIndex + 1) % rowTargets.size();
                refreshRowHighlight();
                return true;

            case Input.Keys.ENTER:
                if (selectedRowIndex >= 0 && selectedRowIndex < rowTargets.size()) {
                    onClawkinSelected(rowTargets.get(selectedRowIndex));
                }
                return true;

            case Input.Keys.ESCAPE:
                closeDialog();
                return true;

            default:
                return false;
        }
    }

    private void refreshRowHighlight() {
        for (int i = 0; i < rowActors.size(); i++) {
            Table row = rowActors.get(i);
            row.setBackground(new ColorDrawable(i == selectedRowIndex ? ORANGE : BLACK));
        }
    }

    private void closeDialog() {
        hide();
        if (onClosed != null) {
            onClosed.run();
        }
    }
}
