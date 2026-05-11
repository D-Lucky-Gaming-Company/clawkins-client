package github.dluckycompany.clawkins.battle;

import java.util.ArrayList;
import java.util.List;

import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.item.Inventory;
import github.dluckycompany.clawkins.item.Item;
import github.dluckycompany.clawkins.item.Wallet;

/**
 * Persistent player combat state shared across battles.
 *
 * Current behavior: HP persists between encounters for the lifetime of the game
 * session.
 */
public class PlayerBattleState {
    private static final String DEFAULT_SKILL_1_NAME = "Slash";
    private static final int DEFAULT_SKILL_1_POWER = 12;
    private static final String DEFAULT_SKILL_2_NAME = "Heavy Strike";
    private static final int DEFAULT_SKILL_2_POWER = 16;
    private static final String DEFAULT_SKILL_3_NAME = "Quick Jab";
    private static final int DEFAULT_SKILL_3_POWER = 9;

    private boolean initialized;
    private int maxHp;
    private int currentHp;
    private int attack;
    private int defense;
    private int speed;
    private final List<BattleSkill> playerSkills;
    private final List<Clawkin> party;
    private final Inventory inventory;
    private final Wallet wallet;
    private int activeClawkinIndex = -1;

    public PlayerBattleState() {
        this.playerSkills = new ArrayList<>();
        this.party = new ArrayList<>();
        this.inventory = new Inventory();
        this.wallet = new Wallet();
    }

    public void initializeIfUnset(int maxHp, int attack, int defense, int speed) {
        initializeIfUnset(maxHp, attack, defense, speed, defaultSkills());
    }

    public void initializeIfUnset(int maxHp, int attack, int defense, int speed, List<BattleSkill> skills) {
        if (initialized) {
            return;
        }
        this.maxHp = Math.max(1, maxHp);
        this.currentHp = this.maxHp;
        this.attack = Math.max(1, attack);
        this.defense = Math.max(0, defense);
        this.speed = Math.max(1, speed);
        playerSkills.clear();
        playerSkills.addAll(sanitizeSkills(skills));
        this.initialized = true;
    }

    public void ensureInitialized(int defaultMaxHp, int defaultAttack, int defaultDefense, int defaultSpeed) {
        initializeIfUnset(defaultMaxHp, defaultAttack, defaultDefense, defaultSpeed);
    }

    public BattleUnit createBattleUnit() {
        return new BattleUnit("player", currentHp, attack, defense, speed);
    }

    public List<BattleSkill> createPlayerSkills() {
        return new ArrayList<>(playerSkills);
    }

    public void applyBattleResult(BattleUnit playerUnit) {
        if (!initialized || playerUnit == null) {
            return;
        }
        currentHp = Math.max(0, Math.min(maxHp, playerUnit.getHp()));
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getAttack() {
        return attack;
    }

    public int getDefense() {
        return defense;
    }

    public int getSpeed() {
        return speed;
    }

    private static List<BattleSkill> sanitizeSkills(List<BattleSkill> rawSkills) {
        if (rawSkills == null || rawSkills.isEmpty()) {
            return defaultSkills();
        }

        List<BattleSkill> sanitized = new ArrayList<>();
        for (BattleSkill skill : rawSkills) {
            if (skill == null) {
                continue;
            }
            String name = skill.getName() == null || skill.getName().isBlank() ? "Skill" : skill.getName();
            int power = Math.max(1, skill.getPower());
            sanitized.add(new BattleSkill(name, power));
        }

        if (sanitized.isEmpty()) {
            return defaultSkills();
        }
        return sanitized;
    }

    private static List<BattleSkill> defaultSkills() {
        return List.of(
                new BattleSkill(DEFAULT_SKILL_1_NAME, DEFAULT_SKILL_1_POWER),
                new BattleSkill(DEFAULT_SKILL_2_NAME, DEFAULT_SKILL_2_POWER),
                new BattleSkill(DEFAULT_SKILL_3_NAME, DEFAULT_SKILL_3_POWER)
        );
    }

    // ============ Party Management ============

    public List<Clawkin> getParty() {
        return party;
    }

    public void addClawkinToParty(Clawkin clawkin) {
        if (clawkin != null && party.size() < 3) {
            party.add(clawkin);
        }
    }

    public int getPartySize() {
        return party.size();
    }

    public Clawkin getClawkin(int index) {
        if (index >= 0 && index < party.size()) {
            return party.get(index);
        }
        return null;
    }

    public List<Clawkin> getAlivePartyMembers() {
        List<Clawkin> alive = new ArrayList<>();
        for (Clawkin clawkin : party) {
            if (clawkin.isAlive()) {
                alive.add(clawkin);
            }
        }
        return alive;
    }

    public boolean hasAnyAliveClawkin() {
        return !getAlivePartyMembers().isEmpty();
    }

    /**
     * True when the party has at least one member and every member is KO'd (0 HP).
     */
    public static boolean isEntirePartyFelled(List<Clawkin> party) {
        if (party == null || party.isEmpty()) {
            return false;
        }
        for (Clawkin c : party) {
            if (c != null && c.isAlive()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEntirePartyFelled() {
        return isEntirePartyFelled(party);
    }

    /**
     * Inventory / party-target item use when the whole party is down: only revive items,
     * still respecting per-item battle usability when {@code battleContext} is true.
     */
    public static boolean isInventoryItemUseAllowed(Item item, boolean battleContext, List<Clawkin> party) {
        if (item == null) {
            return false;
        }
        if (isEntirePartyFelled(party)) {
            if (item.getType() != Item.ItemType.REVIVE) {
                return false;
            }
            return !battleContext || item.isUsableInBattle();
        }
        if (battleContext) {
            return item.isUsableInBattle();
        }
        return item.getType() == Item.ItemType.POTION;
    }

    // ============ Active Clawkin ============

    public void setActiveClawkinIndex(int index) {
        if (index < 0 || index >= party.size()) {
            this.activeClawkinIndex = -1;
            return;
        }
        this.activeClawkinIndex = index;
    }

    public int getActiveClawkinIndex() {
        if (activeClawkinIndex >= 0 && activeClawkinIndex < party.size()) {
            return activeClawkinIndex;
        }
        // If no clawkin has been explicitly selected yet, default to first slot.
        return party.isEmpty() ? -1 : 0;
    }

    public Clawkin getActiveClawkin() {
        return getClawkin(getActiveClawkinIndex());
    }

    /**
     * Returns the index of the next alive clawkin, excluding {@code fromIndex}.
     * Searches forward first (fromIndex+1 … end), then wraps around (0 … fromIndex-1).
     * Returns -1 if every other slot is also fainted or empty.
     */
    public int findNextAliveClawkinIndex(int fromIndex) {
        // Forward pass
        for (int i = fromIndex + 1; i < party.size(); i++) {
            Clawkin c = party.get(i);
            if (c != null && c.isAlive()) {
                return i;
            }
        }
        // Wrap-around pass
        for (int i = 0; i < fromIndex; i++) {
            Clawkin c = party.get(i);
            if (c != null && c.isAlive()) {
                return i;
            }
        }
        return -1;
    }

    public void applyClawkinBattleResult(int clawkinIndex, BattleUnit unit) {
        Clawkin clawkin = getClawkin(clawkinIndex);
        if (clawkin == null || unit == null) return;
        clawkin.setCurrentHp(unit.getHp());
    }

    // ============ Inventory & Wallet ============

    public Inventory getInventory() {
        return inventory;
    }

    public Wallet getWallet() {
        return wallet;
    }
}