package github.dluckycompany.clawkins.debug;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;

import github.dluckycompany.clawkins.battle.PlayerBattleState;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.ItemFactory;

/**
 * Manages cheat code execution and validation.
 * Provides a clean interface for registering and executing debug commands.
 */
public class CheatCodeManager {
    private static final boolean ENABLE_CHEATS = true;
    
    private final Map<String, CheatCommand> cheatCommands = new HashMap<>();
    private final PlayerBattleState playerBattleState;
    private String currentMapKey = "unknown";
    private float playerX = 0f;
    private float playerY = 0f;
    private Runnable onMoneyChanged;
    
    public interface CheatCommand {
        CheatResult execute();
    }
    
    public static class CheatResult {
        public final boolean success;
        public final String message;
        
        public CheatResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static CheatResult success(String message) {
            return new CheatResult(true, message);
        }
        
        public static CheatResult failure(String message) {
            return new CheatResult(false, message);
        }
    }
    
    public CheatCodeManager(PlayerBattleState playerBattleState) {
        this.playerBattleState = playerBattleState;
        registerDefaultCheats();
    }
    
    /**
     * Execute a cheat code by name.
     * 
     * @param code The cheat code to execute (case-insensitive, whitespace trimmed)
     * @return CheatResult indicating success/failure and feedback message
     */
    public CheatResult executeCheat(String code) {
        if (!ENABLE_CHEATS) {
            return CheatResult.failure("Cheats are disabled");
        }
        
        if (code == null) {
            return CheatResult.failure("Invalid cheat code");
        }
        
        // Normalize input: trim whitespace and convert to lowercase
        String normalizedCode = code.trim().toLowerCase();
        
        if (normalizedCode.isEmpty()) {
            return CheatResult.failure("Empty cheat code");
        }
        
        CheatCommand command = cheatCommands.get(normalizedCode);
        if (command == null) {
            return CheatResult.failure("Unknown cheat: " + normalizedCode);
        }
        
        try {
            return command.execute();
        } catch (Exception e) {
            Gdx.app.error("CheatCodeManager", "Error executing cheat '" + normalizedCode + "'", e);
            return CheatResult.failure("Cheat execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Register a new cheat command.
     * 
     * @param code The cheat code (will be normalized to lowercase)
     * @param command The command to execute
     */
    public void registerCheat(String code, CheatCommand command) {
        if (code != null && command != null) {
            cheatCommands.put(code.toLowerCase().trim(), command);
        }
    }
    
    /**
     * Update player position for whereami cheat.
     */
    public void updatePlayerPosition(String mapKey, float x, float y) {
        this.currentMapKey = mapKey != null ? mapKey : "unknown";
        this.playerX = x;
        this.playerY = y;
    }
    
    /**
     * Set callback to be called when money changes.
     */
    public void setOnMoneyChanged(Runnable callback) {
        this.onMoneyChanged = callback;
    }
    
    /**
     * Notify that money has changed (for HUD updates).
     */
    private void notifyMoneyChanged() {
        if (onMoneyChanged != null) {
            onMoneyChanged.run();
        }
    }
    
    /**
     * Check if cheats are enabled.
     */
    public boolean areCheatsEnabled() {
        return ENABLE_CHEATS;
    }
    
    /**
     * Get all registered cheat codes (for help/debugging).
     */
    public String[] getRegisteredCheats() {
        return cheatCommands.keySet().toArray(new String[0]);
    }
    
    private void registerDefaultCheats() {
        // Money cheat - adds 1000 coins
        registerCheat("money", () -> {
            long amount = 1000L;
            long beforeMoney = playerBattleState.getWallet().getMoney();
            playerBattleState.getWallet().addMoney(amount);
            long afterMoney = playerBattleState.getWallet().getMoney();
            
            notifyMoneyChanged();
            
            Gdx.app.log("CheatCodeManager", "Money cheat: " + beforeMoney + " -> " + afterMoney);
            return CheatResult.success("Added " + amount + " coins! Total: " + afterMoney);
        });
        
        // Heal cheat - fully heals all party members
        registerCheat("heal", () -> {
            int healedCount = 0;
            StringBuilder healLog = new StringBuilder("Heal cheat results:\n");
            
            for (Clawkin clawkin : playerBattleState.getParty()) {
                int beforeHp = clawkin.getCurrentHp();
                int maxHp = clawkin.getMaxHp();
                
                if (beforeHp < maxHp) {
                    clawkin.setCurrentHp(maxHp);
                    healedCount++;
                    healLog.append(String.format("  %s: %d -> %d HP\n", clawkin.getName(), beforeHp, maxHp));
                } else {
                    healLog.append(String.format("  %s: Already at full HP (%d)\n", clawkin.getName(), maxHp));
                }
            }
            
            Gdx.app.log("CheatCodeManager", healLog.toString());
            
            if (healedCount > 0) {
                return CheatResult.success("Healed " + healedCount + " party member(s) to full HP!");
            } else {
                return CheatResult.success("All party members already at full HP!");
            }
        });
        
        // Items cheat - adds basic test items
        registerCheat("items", () -> {
            try {
                int beforePotions = playerBattleState.getInventory().getQuantity(ItemFactory.BASIC_POTION);
                int beforeElixirs = playerBattleState.getInventory().getQuantity(ItemFactory.FULL_HEAL);
                int beforeAttackBoosts = playerBattleState.getInventory().getQuantity(ItemFactory.ATTACK_BOOST);
                int beforeDefenseBoosts = playerBattleState.getInventory().getQuantity(ItemFactory.DEFENSE_BOOST);
                
                // Add some basic items using ItemFactory
                playerBattleState.getInventory().addItem(ItemFactory.BASIC_POTION, 5);
                playerBattleState.getInventory().addItem(ItemFactory.FULL_HEAL, 2);
                playerBattleState.getInventory().addItem(ItemFactory.ATTACK_BOOST, 3);
                playerBattleState.getInventory().addItem(ItemFactory.DEFENSE_BOOST, 3);
                
                int afterPotions = playerBattleState.getInventory().getQuantity(ItemFactory.BASIC_POTION);
                int afterElixirs = playerBattleState.getInventory().getQuantity(ItemFactory.FULL_HEAL);
                int afterAttackBoosts = playerBattleState.getInventory().getQuantity(ItemFactory.ATTACK_BOOST);
                int afterDefenseBoosts = playerBattleState.getInventory().getQuantity(ItemFactory.DEFENSE_BOOST);
                
                Gdx.app.log("CheatCodeManager", String.format("Items cheat:\n  Potions: %d -> %d\n  Elixirs: %d -> %d\n  Attack Boosts: %d -> %d\n  Defense Boosts: %d -> %d",
                    beforePotions, afterPotions, beforeElixirs, afterElixirs, beforeAttackBoosts, afterAttackBoosts, beforeDefenseBoosts, afterDefenseBoosts));
                
                return CheatResult.success("Added test items: 5x Potion, 2x Elixir, 3x Attack Boost, 3x Defense Boost");
            } catch (Exception e) {
                Gdx.app.error("CheatCodeManager", "Items cheat failed", e);
                return CheatResult.failure("Failed to add items: " + e.getMessage());
            }
        });
        
        // Where am I cheat - displays current location
        registerCheat("whereami", () -> {
            String message = String.format("Map: %s | Position: (%.1f, %.1f)", 
                currentMapKey, playerX, playerY);
            Gdx.app.log("CheatConsole", "Player location: " + message);
            return CheatResult.success(message);
        });
        
        // Rich cheat - adds a lot of money
        registerCheat("rich", () -> {
            long amount = 10000L;
            long beforeMoney = playerBattleState.getWallet().getMoney();
            playerBattleState.getWallet().addMoney(amount);
            long afterMoney = playerBattleState.getWallet().getMoney();
            
            notifyMoneyChanged();
            
            Gdx.app.log("CheatCodeManager", "Rich cheat: " + beforeMoney + " -> " + afterMoney);
            return CheatResult.success("Added " + amount + " coins! You're rich! Total: " + afterMoney);
        });
        
        // Poor cheat - removes all money
        registerCheat("poor", () -> {
            long currentMoney = playerBattleState.getWallet().getMoney();
            playerBattleState.getWallet().removeMoney(currentMoney);
            long afterMoney = playerBattleState.getWallet().getMoney();
            
            notifyMoneyChanged();
            
            Gdx.app.log("CheatCodeManager", "Poor cheat: " + currentMoney + " -> " + afterMoney);
            return CheatResult.success("Removed all money. Back to being poor!");
        });
        
        // Help cheat - lists available cheats
        registerCheat("help", () -> {
            StringBuilder sb = new StringBuilder("Available cheats:\n");
            sb.append("money - Add 1000 coins\n");
            sb.append("rich - Add 10000 coins\n");
            sb.append("poor - Remove all money\n");
            sb.append("heal - Heal all party members\n");
            sb.append("items - Add test items\n");
            sb.append("whereami - Show current location\n");
            sb.append("end - Trigger ending credits\n");
            sb.append("help - Show this help");
            
            Gdx.app.log("CheatConsole", sb.toString());
            return CheatResult.success("Check console for cheat list");
        });
    }
}