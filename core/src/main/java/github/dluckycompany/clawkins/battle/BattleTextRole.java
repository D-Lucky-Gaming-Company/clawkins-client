package github.dluckycompany.clawkins.battle;

/**
 * Semantic styling for battle log fragments. Mapped to colors in {@link BattleLogMarkup}.
 */
public enum BattleTextRole {
    /** Post-victory "Milestone!" headline — saturated yellow. */
    MILESTONE,
    /** Unit names, skill names — yellow. */
    NAME,
    /** Numeric damage — light red. */
    DAMAGE,
    /** Heal amounts — green. */
    HEAL,
    /** The phrase {@code defense UP} — blue. */
    DEFENSE_UP
}
