package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import github.dluckycompany.clawkins.item.Wallet;

/**
 * Simple HUD widget displaying wallet balance.
 * Intended to be placed in a HUD stage and always visible.
 *
 * Usage:
 *   HudWallet hudWallet = new HudWallet(wallet, font);
 *   hudStage.addActor(hudWallet);
 *
 *   // Update position as needed
 *   hudWallet.setPosition(10, 10);
 */
public class HudWallet extends Label {
    private final Wallet wallet;

    public HudWallet(Wallet wallet, BitmapFont font) {
        super("", new LabelStyle(font, Color.GOLD));
        this.wallet = wallet;
        setText("Money: " + wallet.getMoney());
    }

    /**
     * Update the displayed money amount.
     * Call this after money changes.
     */
    public void updateDisplay() {
        setText("Money: " + wallet.getMoney());
        pack(); // Re-size based on new text
    }

    /**
     * Get the current wallet balance.
     *
     * @return the balance
     */
    public long getBalance() {
        return wallet.getMoney();
    }
}
