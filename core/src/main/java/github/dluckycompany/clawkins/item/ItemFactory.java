package github.dluckycompany.clawkins.item;

/**
 * Factory for creating predefined common items.
 * Items are singletons initialized once and reused throughout the game.
 *
 * HEAL TIERS:
 *   Donut (40) → Potion (30) → Pancake (60) → Super Potion (80) →
 *   Mega Potion (150) → Pizza (120) → Lemon Pie (200) → Steak Dinner (250) →
 *   Strawberry Cake (350) → Roast Feast (400) → Elixir (999)
 *
 * STAT BOOST TIERS (per stat):
 *   Tier 1 (+5, 3 turns) → Tier 2 (+5, 5 turns) → Tier 3 (+10, 4 turns) → Tier 4 (+15, 5 turns)
 */
public class ItemFactory {

    // =========================================================================
    // POTIONS — Healing items
    // =========================================================================

    public static final Item DONUT = new Item(
        "donut",
        "Donut",
        "A sweet treat that restores a little HP.",
        Item.ItemType.POTION,
        new HealEffect(40),
        30,
        true,
        "35_donut_dish"
    );

    public static final Item BASIC_POTION = new Item(
        "basic_potion",
        "Potion",
        "Restores a small amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(30),
        50,
        true,
        "05_apple_pie"
    );

    public static final Item PANCAKE_STACK = new Item(
        "pancake_stack",
        "Pancake Stack",
        "A fluffy stack that restores a decent amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(60),
        80,
        true,
        "80_pancakes_dish"
    );

    public static final Item SUPER_POTION = new Item(
        "super_potion",
        "Super Potion",
        "Restores a good amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(80),
        100,
        true,
        "08_bread_dish"
    );

    public static final Item PIZZA_SLICE = new Item(
        "pizza_slice",
        "Pizza Slice",
        "A hearty slice that restores a solid amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(120),
        150,
        true,
        "82_pizza_dish"
    );

    public static final Item MEGA_POTION = new Item(
        "mega_potion",
        "Mega Potion",
        "Restores a large amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(150),
        180,
        true,
        "33_curry_dish"
    );

    public static final Item LEMON_PIE = new Item(
        "lemon_pie",
        "Lemon Pie",
        "A zesty pie that restores a significant amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(200),
        250,
        true,
        "64_lemonpie_dish"
    );

    public static final Item STEAK_DINNER = new Item(
        "steak_dinner",
        "Steak Dinner",
        "A filling meal that restores a large amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(250),
        300,
        true,
        "96_steak_dish"
    );

    public static final Item STRAWBERRY_CAKE = new Item(
        "strawberry_cake",
        "Strawberry Cake",
        "A rich cake that restores a great amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(350),
        400,
        true,
        "91_strawberrycake_dish"
    );

    public static final Item ROAST_FEAST = new Item(
        "roast_feast",
        "Roast Feast",
        "A grand roasted meal that restores a massive amount of HP.",
        Item.ItemType.POTION,
        new HealEffect(400),
        450,
        true,
        "86_roastedchicken_dish"
    );

    public static final Item FULL_HEAL = new Item(
        "full_heal",
        "Elixir",
        "Fully restores HP.",
        Item.ItemType.POTION,
        new HealEffect(999),
        200,
        true,
        "04_bowl"
    );

    // =========================================================================
    // REVIVE items
    // =========================================================================

    public static final Item REVIVE = new Item(
        "revive",
        "Revive",
        "Revives a fainted Clawkin with 50 HP.",
        Item.ItemType.REVIVE,
        new ReviveEffect(50),
        150,
        true,
        "50_giantgummybear"
    );

    public static final Item SUPER_REVIVE = new Item(
        "super_revive",
        "Super Revive",
        "Revives a fainted Clawkin with 150 HP.",
        Item.ItemType.REVIVE,
        new ReviveEffect(150),
        250,
        true,
        "53_gingerbreadman_dish"
    );

    public static final Item FULL_REVIVE = new Item(
        "full_revive",
        "Full Revive",
        "Revives a fainted Clawkin with full HP.",
        Item.ItemType.REVIVE,
        new ReviveEffect(999),
        300,
        true,
        "51_giantgummybear_dish"
    );

    // =========================================================================
    // STAT BOOSTERS — Attack
    // =========================================================================

    /** Tier 1 ATK: +5 for 3 turns */
    public static final Item ATTACK_BOOST = new Item(
        "attack_boost",
        "Attack Boost",
        "Boosts attack by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 5, 3),
        100,
        true,
        "13_bacon"
    );

    /** Tier 2 ATK: +5 for 5 turns */
    public static final Item NACHO_PLATTER = new Item(
        "nacho_platter",
        "Nacho Platter",
        "Crunchy nachos that boost attack by 5 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 5, 5),
        120,
        true,
        "72_nacho_dish"
    );

    /** Tier 3 ATK: +10 for 4 turns */
    public static final Item POWER_STEAK = new Item(
        "power_steak",
        "Power Steak",
        "A raw steak that boosts attack by 10 for 4 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 10, 4),
        180,
        true,
        "95_steak"
    );

    /** Tier 4 ATK: +15 for 5 turns */
    public static final Item BATTLE_BURGER = new Item(
        "battle_burger",
        "Battle Burger",
        "A massive burger that boosts attack by 15 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 15, 5),
        300,
        true,
        "16_burger_dish"
    );

    // =========================================================================
    // STAT BOOSTERS — Defense
    // =========================================================================

    /** Tier 1 DEF: +5 for 3 turns */
    public static final Item DEFENSE_BOOST = new Item(
        "defense_boost",
        "Defense Boost",
        "Boosts defense by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 5, 3),
        100,
        true,
        "87_ramen"
    );

    /** Tier 2 DEF: +5 for 5 turns */
    public static final Item EGG_SALAD = new Item(
        "egg_salad",
        "Egg Salad",
        "A nourishing salad that boosts defense by 5 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 5, 5),
        120,
        true,
        "41_eggsalad_bowl"
    );

    /** Tier 3 DEF: +10 for 4 turns */
    public static final Item IRON_SALMON = new Item(
        "iron_salmon",
        "Iron Salmon",
        "A fortifying fish that boosts defense by 10 for 4 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 10, 4),
        180,
        true,
        "88_salmon"
    );

    /** Tier 4 DEF: +15 for 5 turns */
    public static final Item FORTRESS_CAKE = new Item(
        "fortress_cake",
        "Fortress Cake",
        "A dense chocolate cake that boosts defense by 15 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 15, 5),
        300,
        true,
        "31_chocolatecake_dish"
    );

    // =========================================================================
    // STAT BOOSTERS — Speed
    // =========================================================================

    /** Tier 1 SPD: +5 for 3 turns */
    public static final Item SPEED_BOOST = new Item(
        "speed_boost",
        "Speed Boost",
        "Boosts speed by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.SPEED, 5, 3),
        100,
        true,
        "57_icecream"
    );

    /** Tier 2 SPD: +5 for 5 turns */
    public static final Item WAFFLE = new Item(
        "waffle",
        "Waffle",
        "A light waffle that boosts speed by 5 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.SPEED, 5, 5),
        120,
        true,
        "102_waffle_dish"
    );

    /** Tier 3 SPD: +10 for 4 turns */
    public static final Item TURBO_FRIES = new Item(
        "turbo_fries",
        "Turbo Fries",
        "Crispy fries that boost speed by 10 for 4 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.SPEED, 10, 4),
        180,
        true,
        "45_frenchfries_dish"
    );

    /** Tier 4 SPD: +15 for 5 turns */
    public static final Item SUSHI_SPRINT = new Item(
        "sushi_sprint",
        "Sushi Sprint",
        "Fresh sushi that boosts speed by 15 for 5 turns.",
        Item.ItemType.STAT_BOOSTER,
        new StatBoostEffect(StatBoostEffect.StatType.SPEED, 15, 5),
        300,
        true,
        "98_sushi_dish"
    );

    // =========================================================================
    // COMBO items — multiple effects at once
    // =========================================================================

    /** ATK +8 AND DEF +8 for 3 turns */
    public static final Item WARRIORS_BENTO = new Item(
        "warriors_bento",
        "Warrior's Bento",
        "A balanced bento that boosts attack and defense by 8 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new ComboEffect(
            new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 8, 3),
            new StatBoostEffect(StatBoostEffect.StatType.DEFENSE, 8, 3)
        ),
        250,
        true,
        "37_dumplings_dish"
    );

    /** Heal 100 AND SPD +5 for 3 turns */
    public static final Item SPAGHETTI_SPRINT = new Item(
        "spaghetti_sprint",
        "Spaghetti Sprint",
        "Restores 100 HP and boosts speed by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new ComboEffect(
            new HealEffect(100),
            new StatBoostEffect(StatBoostEffect.StatType.SPEED, 5, 3)
        ),
        220,
        true,
        "94_spaghetti"
    );

    /** Heal 80 AND ATK +5 for 3 turns */
    public static final Item MEATBALL_MIGHT = new Item(
        "meatball_might",
        "Meatball Might",
        "Restores 80 HP and boosts attack by 5 for 3 turns.",
        Item.ItemType.STAT_BOOSTER,
        new ComboEffect(
            new HealEffect(80),
            new StatBoostEffect(StatBoostEffect.StatType.ATTACK, 5, 3)
        ),
        200,
        true,
        "70_meatball_dish"
    );

    // =========================================================================
    // SPECIAL items
    // =========================================================================

    public static final Item MACARAMBONI = new Item(
        "macaramboni",
        "Macaramboni",
        "A delicious pasta dish that boosts a Clawkin's level by 1.",
        Item.ItemType.SPECIAL,
        new LevelBoostEffect(1),
        500,
        true,
        "68_macncheese_dish"
    );

    // =========================================================================

    private ItemFactory() {}

    /**
     * Get an item by its ID.
     *
     * @param itemId the item ID
     * @return the Item, or null if not found
     */
    public static Item getItemById(String itemId) {
        if (itemId == null) return null;
        return switch (itemId) {
            // Potions
            case "donut"           -> DONUT;
            case "basic_potion"    -> BASIC_POTION;
            case "pancake_stack"   -> PANCAKE_STACK;
            case "super_potion"    -> SUPER_POTION;
            case "pizza_slice"     -> PIZZA_SLICE;
            case "mega_potion"     -> MEGA_POTION;
            case "lemon_pie"       -> LEMON_PIE;
            case "steak_dinner"    -> STEAK_DINNER;
            case "strawberry_cake" -> STRAWBERRY_CAKE;
            case "roast_feast"     -> ROAST_FEAST;
            case "full_heal"       -> FULL_HEAL;
            // Revives
            case "revive"          -> REVIVE;
            case "super_revive"    -> SUPER_REVIVE;
            case "full_revive"     -> FULL_REVIVE;
            // ATK boosters
            case "attack_boost"    -> ATTACK_BOOST;
            case "nacho_platter"   -> NACHO_PLATTER;
            case "power_steak"     -> POWER_STEAK;
            case "battle_burger"   -> BATTLE_BURGER;
            // DEF boosters
            case "defense_boost"   -> DEFENSE_BOOST;
            case "egg_salad"       -> EGG_SALAD;
            case "iron_salmon"     -> IRON_SALMON;
            case "fortress_cake"   -> FORTRESS_CAKE;
            // SPD boosters
            case "speed_boost"     -> SPEED_BOOST;
            case "waffle"          -> WAFFLE;
            case "turbo_fries"     -> TURBO_FRIES;
            case "sushi_sprint"    -> SUSHI_SPRINT;
            // Combos
            case "warriors_bento"  -> WARRIORS_BENTO;
            case "spaghetti_sprint"-> SPAGHETTI_SPRINT;
            case "meatball_might"  -> MEATBALL_MIGHT;
            // Special
            case "macaramboni"     -> MACARAMBONI;
            default                -> null;
        };
    }

    /**
     * Returns all defined items. Useful for cheat codes and testing.
     */
    public static Item[] getAllItems() {
        return new Item[]{
            DONUT, BASIC_POTION, PANCAKE_STACK, SUPER_POTION, PIZZA_SLICE,
            MEGA_POTION, LEMON_PIE, STEAK_DINNER, STRAWBERRY_CAKE, ROAST_FEAST, FULL_HEAL,
            REVIVE, SUPER_REVIVE, FULL_REVIVE,
            ATTACK_BOOST, NACHO_PLATTER, POWER_STEAK, BATTLE_BURGER,
            DEFENSE_BOOST, EGG_SALAD, IRON_SALMON, FORTRESS_CAKE,
            SPEED_BOOST, WAFFLE, TURBO_FRIES, SUSHI_SPRINT,
            WARRIORS_BENTO, SPAGHETTI_SPRINT, MEATBALL_MIGHT,
            MACARAMBONI
        };
    }
}
