package github.dluckycompany.clawkins.item;

/**
 * Wallet system for managing player money.
 */
public class Wallet {
    private long currentMoney = 0;

    /**
     * Add money to the wallet.
     *
     * @param amount the amount to add (must be non-negative)
     */
    public void addMoney(long amount) {
        if (amount > 0) {
            currentMoney += amount;
        }
    }

    /**
     * Remove money from the wallet.
     *
     * @param amount the amount to remove
     * @return true if successful, false if insufficient funds
     */
    public boolean removeMoney(long amount) {
        if (amount < 0 || currentMoney < amount) {
            return false;
        }
        currentMoney -= amount;
        return true;
    }

    /**
     * Check if wallet can afford an amount.
     *
     * @param amount the amount to check
     * @return true if wallet has enough money
     */
    public boolean canAfford(long amount) {
        return currentMoney >= amount && amount >= 0;
    }

    /**
     * Get current money balance.
     *
     * @return current balance
     */
    public long getMoney() {
        return currentMoney;
    }

    /**
     * Set money to a specific amount.
     * Useful for initialization or cheating.
     *
     * @param amount the amount to set
     */
    public void setMoney(long amount) {
        currentMoney = Math.max(0, amount);
    }

    /**
     * Empty the wallet.
     */
    public void clear() {
        currentMoney = 0;
    }
}
