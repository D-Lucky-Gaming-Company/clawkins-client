package github.dluckycompany.clawkins.battle;

/**
 * A half-open range {@code [start, end)} in {@link BattleStateMachine#getLastLog()} plain text with a
 * {@link BattleTextRole}. Gaps between spans render as default (white) body text.
 */
public record BattleTextSpan(int start, int end, BattleTextRole role) {
    public BattleTextSpan {
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid span: " + start + ".." + end);
        }
    }
}
