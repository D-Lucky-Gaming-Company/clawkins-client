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
    private Runnable onTeleportRequested;
    private float gameSpeedMultiplier = 1f;
    private String pendingTeleportMapKey = null;
    private github.dluckycompany.clawkins.progress.PlayerProgress playerProgress;
    private Runnable onSharedLevelChanged;
    
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
        
        // Handle teleport command with map parameter: "tp <map>"
        if (normalizedCode.startsWith("tp ")) {
            String mapKey = normalizedCode.substring(3).trim();
            return handleTeleport(mapKey);
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
     * Handle teleport cheat command.
     * Validates the map key and queues the teleport for GameScreen to process.
     */
    private CheatResult handleTeleport(String mapKey) {
        if (mapKey == null || mapKey.isEmpty()) {
            return CheatResult.failure("Usage: tp <map> (e.g., tp cottage)");
        }
        
        // Try to resolve the map key using MapAsset
        github.dluckycompany.clawkins.asset.MapAsset targetMap = 
            github.dluckycompany.clawkins.asset.MapAsset.fromKey(mapKey);
        
        if (targetMap == null) {
            Gdx.app.log("Cheat", "Unknown map: " + mapKey);
            return CheatResult.failure("Unknown map: " + mapKey + " (type 'maps' for list)");
        }
        
        // Store the pending teleport
        pendingTeleportMapKey = targetMap.name();
        
        // Notify GameScreen to process the teleport
        if (onTeleportRequested != null) {
            onTeleportRequested.run();
        }
        
        String displayName = github.dluckycompany.clawkins.asset.MapAssetName.fromAsset(targetMap);
        if (displayName == null) {
            displayName = targetMap.name();
        }
        
        Gdx.app.log("Cheat", "Teleporting to map: " + targetMap.name() + " (" + displayName + ")");
        return CheatResult.success("Teleporting to: " + displayName);
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
     * Set callback to be called when teleport is requested.
     * The callback should handle the actual map transition.
     */
    public void setOnTeleportRequested(Runnable callback) {
        this.onTeleportRequested = callback;
    }

    /**
     * Set the PlayerProgress reference so level-related cheats can update shared XP.
     */
    public void setPlayerProgress(github.dluckycompany.clawkins.progress.PlayerProgress progress) {
        this.playerProgress = progress;
    }

    /**
     * Set callback invoked after a level cheat changes the shared level.
     * GameScreen should wire this to applySharedLevelSet / applySharedLevelBoost.
     */
    public void setOnSharedLevelChanged(Runnable callback) {
        this.onSharedLevelChanged = callback;
    }
    
    /**
     * Get the current game speed multiplier.
     */
    public float getGameSpeedMultiplier() {
        return gameSpeedMultiplier;
    }
    
    /**
     * Get the pending teleport map key (if any).
     * Returns null if no teleport is pending.
     */
    public String getPendingTeleportMapKey() {
        return pendingTeleportMapKey;
    }
    
    /**
     * Clear the pending teleport map key after it has been processed.
     */
    public void clearPendingTeleport() {
        this.pendingTeleportMapKey = null;
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
        
        // Items cheat - adds a sampler of items across all categories
        registerCheat("items", () -> {
            try {
                // Potions — one of each tier
                playerBattleState.getInventory().addItem(ItemFactory.DONUT, 3);
                playerBattleState.getInventory().addItem(ItemFactory.BASIC_POTION, 5);
                playerBattleState.getInventory().addItem(ItemFactory.PANCAKE_STACK, 3);
                playerBattleState.getInventory().addItem(ItemFactory.SUPER_POTION, 3);
                playerBattleState.getInventory().addItem(ItemFactory.MEGA_POTION, 2);
                playerBattleState.getInventory().addItem(ItemFactory.FULL_HEAL, 2);
                // Revives
                playerBattleState.getInventory().addItem(ItemFactory.REVIVE, 3);
                playerBattleState.getInventory().addItem(ItemFactory.SUPER_REVIVE, 2);
                // Stat boosters — tier 1 per stat
                playerBattleState.getInventory().addItem(ItemFactory.ATTACK_BOOST, 3);
                playerBattleState.getInventory().addItem(ItemFactory.DEFENSE_BOOST, 3);
                playerBattleState.getInventory().addItem(ItemFactory.SPEED_BOOST, 3);

                Gdx.app.log("CheatCodeManager", "Items cheat: added sampler of items");
                return CheatResult.success("Added item sampler: potions, revives, and stat boosts!");
            } catch (Exception e) {
                Gdx.app.error("CheatCodeManager", "Items cheat failed", e);
                return CheatResult.failure("Failed to add items: " + e.getMessage());
            }
        });

        // Allitems cheat - adds 2 of every item in the game
        registerCheat("allitems", () -> {
            try {
                int count = 0;
                for (github.dluckycompany.clawkins.item.Item item : ItemFactory.getAllItems()) {
                    playerBattleState.getInventory().addItem(item, 2);
                    count++;
                }
                Gdx.app.log("CheatCodeManager", "Allitems cheat: added 2x of " + count + " items");
                return CheatResult.success("Added 2x of all " + count + " items!");
            } catch (Exception e) {
                Gdx.app.error("CheatCodeManager", "Allitems cheat failed", e);
                return CheatResult.failure("Failed to add all items: " + e.getMessage());
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
            sb.append("items - Add a sampler of items\n");
            sb.append("allitems - Add 2x of every item in the game\n");
            sb.append("maxlevel - Max out all Clawkin levels\n");
            sb.append("macaramboni - Add 5x Macaramboni (level boost items)\n");
            sb.append("whereami - Show current location\n");
            sb.append("speed - Toggle 2x game speed\n");
            sb.append("tp <map> - Teleport to map (e.g., tp cottage)\n");
            sb.append("maps - List all available maps\n");
            sb.append("end - Trigger ending credits\n");
            sb.append("help - Show this help");
            
            Gdx.app.log("CheatConsole", sb.toString());
            return CheatResult.success("Check console for cheat list");
        });
        
        // Speed cheat - toggles 2x game speed
        registerCheat("speed", () -> {
            if (gameSpeedMultiplier == 1f) {
                gameSpeedMultiplier = 2f;
                Gdx.app.log("Cheat", "2x speed enabled");
                return CheatResult.success("Game Speed: 2x");
            } else {
                gameSpeedMultiplier = 1f;
                Gdx.app.log("Cheat", "Speed returned to normal");
                return CheatResult.success("Game Speed: Normal");
            }
        });
        
        // Maps cheat - lists all available maps
        registerCheat("maps", () -> {
            StringBuilder sb = new StringBuilder("Available maps for teleport:\n");
            sb.append("Aliases (use with 'tp <name>'):\n");
            sb.append("  nursery, cottage, shop, mountain, cave\n");
            sb.append("  field, mansion, backalley, test\n");
            sb.append("\nSpecific maps:\n");
            sb.append("  nurse_interior, nurse_interior_2, nurse_interior_3, nurse_interior_4\n");
            sb.append("  cottage_sample\n");
            sb.append("  shop_interior, shop_interior_2, shop_interior_3\n");
            sb.append("  mountain_1, mountain_2, mountain_3, mountain_4, mountain_5\n");
            sb.append("  cave_entrance, cave_1, cave_2, cave_3\n");
            sb.append("  field, field_2, field_3, field_4, field_5, field_secret\n");
            sb.append("  mansion_maze, mansion_garden, mansion_exit\n");
            sb.append("  backalley_1, backalley_2, backalley_3, backalley_4\n");
            sb.append("  backalley_exit, backalley_secret\n");
            sb.append("  test_world\n");
            sb.append("\nExample: tp cottage");
            
            Gdx.app.log("CheatConsole", sb.toString());
            return CheatResult.success("Check console for map list");
        });
        
        // Maxlevel cheat - max out all Clawkin levels
        registerCheat("maxlevel", () -> {
            try {
                int maxLevel = github.dluckycompany.clawkins.character.LevelSystem.MAX_LEVEL;

                // Update shared XP to the EXP floor of max level
                if (playerProgress != null) {
                    int expForMaxLevel = github.dluckycompany.clawkins.character.LevelSystem.getExpRequiredForLevel(maxLevel);
                    playerProgress.setExperiencePoints(expForMaxLevel);
                }

                // Notify GameScreen to re-sync party stats and refresh all HUD displays
                if (onSharedLevelChanged != null) {
                    onSharedLevelChanged.run();
                } else {
                    // Fallback: sync Clawkins directly if callback not wired
                    for (Clawkin clawkin : playerBattleState.getParty()) {
                        clawkin.syncStatsToSharedExperienceLevel(maxLevel);
                    }
                }

                Gdx.app.log("CheatCodeManager", "Maxlevel cheat: shared XP set to level " + maxLevel);
                return CheatResult.success("All Clawkins maxed to level " + maxLevel + "!");
            } catch (Exception e) {
                Gdx.app.error("CheatCodeManager", "Maxlevel cheat failed", e);
                return CheatResult.failure("Failed to max levels: " + e.getMessage());
            }
        });
        
        // Macaramboni cheat - adds Macaramboni items
        registerCheat("macaramboni", () -> {
            try {
                // Add 5 Macaramboni items
                playerBattleState.getInventory().addItem(ItemFactory.MACARAMBONI, 5);
                
                int quantity = playerBattleState.getInventory().getQuantity(ItemFactory.MACARAMBONI);
                
                Gdx.app.log("CheatCodeManager", "Macaramboni cheat: Added 5 Macaramboni (total: " + quantity + ")");
                return CheatResult.success("Added 5x Macaramboni! Total: " + quantity);
            } catch (Exception e) {
                Gdx.app.error("CheatCodeManager", "Macaramboni cheat failed", e);
                return CheatResult.failure("Failed to add Macaramboni: " + e.getMessage());
            }
        });
        
        registerTeleportCheats();
    }
    
    /**
     * Register teleport cheat commands.
     * Supports both "tp <map>" format and direct map name shortcuts.
     */
    private void registerTeleportCheats() {
        // Register the main "tp" command that takes a map parameter
        // This will be handled specially in executeCheat
        
        // We'll handle "tp <map>" in executeCheat by parsing the command
        // For now, register common shortcuts
        
        // Note: The actual teleport logic will be in executeCheat
        // to handle the "tp <map>" syntax properly
    }
}