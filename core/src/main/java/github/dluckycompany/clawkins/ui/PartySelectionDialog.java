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
import com.badlogic.gdx.utils.Align;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;
import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.input.InputConventions;
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
    private final AudioService audioService;
    private final boolean battleContext;
    
    // Post-transaction callback to refresh inventory UI
    private Runnable onItemApplied;
    private Runnable onClosed;
    private Dialog activeStatusDialog;
    private Runnable activeStatusOnClose;

    private final List<Table> rowActors = new java.util.ArrayList<>();
    private final List<Clawkin> rowTargets = new java.util.ArrayList<>();
    private int selectedRowIndex = -1;

    private static final Color ORANGE = new Color(1.0f, 0.64f, 0.0f, 1.0f);
    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color DIALOG_BG = Color.valueOf("#2D241B");
    private static final Color TITLE_BG = Color.valueOf("#3A2E22");
    private static final Color INFO_BG = Color.valueOf("#3F3226");
    private static final Color ROW_BG = Color.valueOf("#221B14");
    private static final Color ROW_SELECTED_BG = Color.valueOf("#E0BF4A");
    private static final Color CANCEL_BG = Color.valueOf("#B7833A");
    private static final Color STATUS_BG = Color.valueOf("#2B2219");

    /**
     * Constructor
     *
     * @param party The list of party members (Swee'pea, Ginger, Dart)
     * @param item The item being used
     * @param inventory The inventory to decrement from (for transaction)
     * @param skin The Scene2D Skin for styling
     * @param font The BitmapFont for text
     */
    public PartySelectionDialog(
            List<Clawkin> party,
            Item item,
            Inventory inventory,
            Skin skin,
            BitmapFont font,
            AudioService audioService,
            boolean battleContext) {
        super("Use " + item.getName(), skin);
        this.party = party;
        this.item = item;
        this.inventory = inventory;
        this.font = font;
        this.audioService = audioService;
        this.battleContext = battleContext;

        setModal(true);      // Prevent input bleed-through to inventory
        setMovable(false);   // Fixed position in viewport center
        setResizable(false); // Fixed size

        buildContent();
    }

    /**
     * Build the dialog content with data-bound party member selection rows
     */
    private void buildContent() {
        getContentTable().setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(DIALOG_BG, 12, 2));
        getContentTable().pad(14f);

        // Title row
        Table titleTable = new Table();
        titleTable.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(TITLE_BG, 8, 1));
        titleTable.pad(8f, 10f, 8f, 10f);
        Label titleLabel = new Label("Select a target for " + item.getName(), new Label.LabelStyle(font, ORANGE));
        titleLabel.setFontScale(1.6f);
        titleTable.add(titleLabel).expandX().fillX();
        getContentTable().add(titleTable).expandX().fillX().row();

        // Item effect description
        Table infoTable = new Table();
        infoTable.setBackground(RoundedPanelDrawable.createRoundedPanelWithStroke(INFO_BG, 8, 1));
        infoTable.pad(8f, 10f, 8f, 10f);
        Label effectLabel = new Label("Effect: " + item.getEffectDescription(), new Label.LabelStyle(font, WHITE));
        effectLabel.setFontScale(1.2f);
        infoTable.add(effectLabel).expandX().fillX();
        getContentTable().add(infoTable).padTop(8f).expandX().fillX().row();

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
            RoundedPanelDrawable.createRoundedPanelWithStroke(CANCEL_BG, 8, 1),
            RoundedPanelDrawable.createRoundedPanelWithStroke(ORANGE, 8, 1),
            RoundedPanelDrawable.createRoundedPanelWithStroke(BLACK, 8, 1),
            font
        ));
        cancelBtn.getLabel().setColor(Color.valueOf("#1F1A13"));
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
        memberRow.setBackground(createRowBackground(false));
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

        if (!isUseAllowedInCurrentContext()) {
            if (audioService != null) {
                audioService.playSound(SoundEffect.FAILURE_1);
            }
            String detail;
            if (PlayerBattleState.isEntirePartyFelled(party)) {
                detail = "While your whole team is down, only revive items can be used.";
            } else if (battleContext) {
                detail = item.getName() + " cannot be used in combat.";
            } else {
                detail = item.getName() + " cannot be used outside combat.";
            }
            showStatusDialog("Use Not Allowed", detail);
            return;
        }

        try {
            // ============ PHASE 1: VALIDATION ============
            // Verify the item can be used on this target
            // Example: Potion only heals if HP < Max HP
            boolean canUseOnTarget = canUseItemOn(target);
            
            if (!canUseOnTarget) {
                Gdx.app.log("PartySelectionDialog", target.getName() + " doesn't need " + item.getName());
                if (audioService != null) {
                    audioService.playSound(SoundEffect.FAILURE_1);
                }
                String msg;
                if (item.getType() == Item.ItemType.REVIVE) {
                    msg = target.getName() + " is not fainted.";
                } else if (item.getType() == Item.ItemType.POTION && !target.isAlive()) {
                    msg = "Healing items can't be used on fainted Clawkins. Use a revive item.";
                } else {
                    msg = target.getName() + " is already at full HP.";
                }
                showStatusDialog("Use Not Allowed", msg);
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
                showStatusDialog(
                    "Use Not Allowed",
                    item.getName() + " had no effect on " + target.getName() + "."
                );
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
            if (audioService != null) {
                audioService.playSound(SoundEffect.BATTLE_HEAL);
            }

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

        // Healing items: living targets only, and not at full HP. Revive: KO only. Stat boost: when allowed by context.
        return switch (item.getType()) {
            case POTION -> target.isAlive() && target.getCurrentHp() < target.getMaxHp();
            case REVIVE -> target.getCurrentHp() == 0;
            case STAT_BOOSTER -> true;
        };
    }

    private boolean isUseAllowedInCurrentContext() {
        return PlayerBattleState.isInventoryItemUseAllowed(item, battleContext, party);
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
        if (activeStatusDialog != null) {
            if (isInteractKey(keycode) || isCancelKey(keycode)) {
                closeActiveStatusDialog();
            }
            return true;
        }

        if (rowTargets.isEmpty()) {
            if (keycode == Input.Keys.ESCAPE
                    || keycode == Input.Keys.X
                    || keycode == Input.Keys.BACKSPACE
                    || keycode == Input.Keys.BUTTON_B) {
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
            case Input.Keys.NUMPAD_ENTER:
            case Input.Keys.Z:
            case Input.Keys.SPACE:
            case Input.Keys.BUTTON_A:
                if (selectedRowIndex >= 0 && selectedRowIndex < rowTargets.size()) {
                    onClawkinSelected(rowTargets.get(selectedRowIndex));
                }
                return true;

            case Input.Keys.ESCAPE:
            case Input.Keys.X:
            case Input.Keys.BACKSPACE:
            case Input.Keys.BUTTON_B:
                closeDialog();
                return true;

            default:
                return false;
        }
    }

    private void refreshRowHighlight() {
        for (int i = 0; i < rowActors.size(); i++) {
            Table row = rowActors.get(i);
            row.setBackground(createRowBackground(i == selectedRowIndex));
        }
    }

    private com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable createRowBackground(boolean selected) {
        if (selected) {
            return RoundedPanelDrawable.createRoundedPanelWithStroke(ROW_SELECTED_BG, 8, 2);
        }
        return RoundedPanelDrawable.createRoundedPanelWithStroke(ROW_BG, 8, 1);
    }

    private void closeDialog() {
        hide(null);
        if (onClosed != null) {
            onClosed.run();
        }
    }

    private void showStatusDialog(String title, String message) {
        showStatusDialog(title, message, null);
    }

    private void showStatusDialog(String title, String message, Runnable onClose) {
        Dialog statusDialog = new Dialog(title, getSkin()) {
            @Override
            public void hide() {
                hide(null);
            }

            @Override
            protected void result(Object object) {
                Runnable callback = activeStatusOnClose;
                activeStatusDialog = null;
                activeStatusOnClose = null;
                if (callback != null) callback.run();
            }
        };
        statusDialog.setModal(true);
        statusDialog.setMovable(false);
        statusDialog.getTitleLabel().setColor(ORANGE);
        statusDialog.getTitleLabel().setFontScale(1.05f);
        statusDialog.getContentTable().setBackground(
            RoundedPanelDrawable.createRoundedPanelWithStroke(STATUS_BG, 10, 2)
        );
        statusDialog.getContentTable().pad(8f);

        Label messageLabel = new Label(message, new Label.LabelStyle(font, WHITE));
        messageLabel.setWrap(true);
        messageLabel.setFontScale(0.95f);
        messageLabel.setAlignment(Align.center);
        statusDialog.getContentTable().add(messageLabel).width(360f).pad(14f);
        TextButton okButton = new TextButton("OK", new TextButton.TextButtonStyle(
            RoundedPanelDrawable.createRoundedPanelWithStroke(CANCEL_BG, 8, 2),
            RoundedPanelDrawable.createRoundedPanelWithStroke(ORANGE, 8, 2),
            RoundedPanelDrawable.createRoundedPanelWithStroke(BLACK, 8, 2),
            font
        ));
        okButton.getLabel().setColor(BLACK);
        okButton.getLabel().setFontScale(1.0f);
        statusDialog.button(okButton, true);
        statusDialog.getButtonTable().pad(6f, 12f, 10f, 12f);
        activeStatusDialog = statusDialog;
        activeStatusOnClose = onClose;
        statusDialog.show(getStage(), null);
    }

    private void closeActiveStatusDialog() {
        if (activeStatusDialog == null) {
            return;
        }
        Dialog dialog = activeStatusDialog;
        Runnable callback = activeStatusOnClose;
        activeStatusDialog = null;
        activeStatusOnClose = null;
        dialog.hide(null);
        if (callback != null) {
            callback.run();
        }
    }

    private boolean isInteractKey(int keycode) {
        return InputConventions.isInteractKey(keycode);
    }

    private boolean isCancelKey(int keycode) {
        return InputConventions.isCancelKey(keycode);
    }
}
