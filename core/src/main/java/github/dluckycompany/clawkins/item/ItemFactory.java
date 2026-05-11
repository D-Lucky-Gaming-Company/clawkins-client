package github.dluckycompany.clawkins.item;

/**
 * Factory for creating predefined common items.
 * Items are singletons initialized once and reused throughout the game.
 */
public class ItemFactory {
    // Potion items
    public static final Item BASIC_POTION = new Item(
        "basic_potion",
        "Potion",
        "Restores a small amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(30),
        50,
        true,
        "05_apple_pie"  // Apple pie for healing visuals
    );

    public static final Item FULL_HEAL = new Item(
        "full_heal",
        "Elixir",
        "Fully restores HP.",
        Item.ItemType.POTION,
        new HealEffect(999),
        200,
        true,
        "04_bowl"  // Bowl for restorative visuals
    );

    // Revive items
    public static final Item REVIVE = new Item(
        "revive",
        "Revive",
        "Revives a fainted clawkin with 50 HP.",
        Item.ItemType.REVIVE,
        new ReviveEffect(50),
        150,
        true,
        "50_giantgummybear"  // Giant gummy bear for magical item
    );

    public static final Item FULL_REVIVE = new Item(
        "full_revive",
        "Full Revive",
        "Revives a fainted clawkin with full HP.",
        Item.ItemType.REVIVE,
        new ReviveEffect(999),
        300,
        true,
        "51_giantgummybear_dish"  // Giant gummy bear on dish for premium revive
    );

    // Stat booster items
    public static final Item ATTACK_BOOST = new Item(
        "attack_boost",
        "Attack Boost",
        "Boosts attack by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 5, 3),
        100,
        true,
        "13_bacon"  // Bacon for hearty strength boost
    );

    public static final Item DEFENSE_BOOST = new Item(
        "defense_boost",
        "Defense Boost",
        "Boosts defense by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 5, 3),
        100,
        true,
        "87_ramen"  // Ramen for nourishing defense boost
    );

    public static final Item SPEED_BOOST = new Item(
        "speed_boost",
        "Speed Boost",
        "Boosts speed by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.SPEED, 5, 3),
        100,
        true,
        "57_icecream"  // Ice cream for quick energy/speed
    );

    // Special items
    public static final Item MACARAMBONI = new Item(
        "macaramboni",
        "Macaramboni",
        "A delicious pasta dish that boosts a Clawkin's level by 1.",
        Item.ItemType.SPECIAL,
        new LevelBoostEffect(1),
        500,
        true,
        "68_macncheese_dish"  // Mac and cheese dish for level boost
    );

    private ItemFactory() {
        // Utility class, no instantiation
    }

    /**
     * Get an item by its ID.
     *
     * @param itemId the item ID
     * @return the Item, or null if not found
     */
    public static Item getItemById(String itemId) {
        return switch (itemId) {
            case "basic_potion" -> BASIC_POTION;
            case "full_heal" -> FULL_HEAL;
            case "revive" -> REVIVE;
            case "full_revive" -> FULL_REVIVE;
            case "attack_boost" -> ATTACK_BOOST;
            case "defense_boost" -> DEFENSE_BOOST;
            case "speed_boost" -> SPEED_BOOST;
            case "macaramboni" -> MACARAMBONI;
            default -> null;
        };
    }
}
