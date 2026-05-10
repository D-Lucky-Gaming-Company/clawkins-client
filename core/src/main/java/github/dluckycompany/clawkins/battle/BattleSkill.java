package github.dluckycompany.clawkins.battle;

public class BattleSkill {
    public enum EffectType {
        DAMAGE,
        HEAL,
        ATTACK,
        DEFENSE,
        BLEED  // Damage over time effect
    }

    private final String name;
    private final int effectBaseStat;
    private final EffectType effectType;
    private final String effectStatScale;
    private final int effectDurationTurns;
    private final int turnCooldown;
    private final String summaryDescription;
    private final String summaryEffectText;
    private final String summaryScalingText;

    public BattleSkill(String name, int effectBaseStat) {
        this(name, EffectType.DAMAGE, effectBaseStat, "attack[self]", 0, 0, "", "", "");
    }

    public BattleSkill(String name, EffectType effectType, String effectStatScale, int effectDurationTurns) {
        this(name, effectType, 0, effectStatScale, effectDurationTurns, 0, "", "", "");
    }

    public BattleSkill(String name, EffectType effectType, int effectBaseStat, String effectStatScale, int effectDurationTurns) {
        this(name, effectType, effectBaseStat, effectStatScale, effectDurationTurns, 0, "", "", "");
    }

    public BattleSkill(String name, EffectType effectType, int effectBaseStat, String effectStatScale, int effectDurationTurns, int turnCooldown) {
        this(name, effectType, effectBaseStat, effectStatScale, effectDurationTurns, turnCooldown, "", "", "");
    }

    public BattleSkill(
        String name,
        EffectType effectType,
        int effectBaseStat,
        String effectStatScale,
        int effectDurationTurns,
        int turnCooldown,
        String summaryDescription,
        String summaryEffectText,
        String summaryScalingText
    ) {
        this.name = name;
        this.effectBaseStat = Math.max(0, effectBaseStat);
        this.effectType = effectType == null ? EffectType.DAMAGE : effectType;
        this.effectStatScale = effectStatScale == null ? "" : effectStatScale.trim();
        this.effectDurationTurns = Math.max(0, effectDurationTurns);
        this.turnCooldown = Math.max(0, turnCooldown);
        this.summaryDescription = summaryDescription == null ? "" : summaryDescription.trim();
        this.summaryEffectText = summaryEffectText == null ? "" : summaryEffectText.trim();
        this.summaryScalingText = summaryScalingText == null ? "" : summaryScalingText.trim();
    }

    public String getName() {
        return name;
    }

    public int getEffectBaseStat() {
        return effectBaseStat;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    public String getEffectStatScale() {
        return effectStatScale;
    }

    public int getEffectDurationTurns() {
        return effectDurationTurns;
    }

    public int getTurnCooldown() {
        return turnCooldown;
    }

    public String getSummaryDescription() {
        return summaryDescription;
    }

    public String getSummaryEffectText() {
        return summaryEffectText;
    }

    public String getSummaryScalingText() {
        return summaryScalingText;
    }

    // Backward compatibility for existing call sites.
    public int getPower() {
        return getEffectBaseStat();
    }

    public String getEffectStat() {
        return getEffectStatScale();
    }
}
