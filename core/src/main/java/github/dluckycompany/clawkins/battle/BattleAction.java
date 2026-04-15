package github.dluckycompany.clawkins.battle;

public class BattleAction {
    private final BattleActionType type;
    private final String actorId;
    private final String targetId;
    private final int skillSlot;
    private final String itemId;
    private final int targetClawkinIndex;

    public BattleAction(BattleActionType type, String actorId, String targetId) {
        this(type, actorId, targetId, 1, null, -1);
    }

    public BattleAction(BattleActionType type, String actorId, String targetId, int skillSlot) {
        this(type, actorId, targetId, skillSlot, null, -1);
    }

    public BattleAction(BattleActionType type, String actorId, String targetId, String itemId, int targetClawkinIndex) {
        this(type, actorId, targetId, 1, itemId, targetClawkinIndex);
    }

    private BattleAction(BattleActionType type, String actorId, String targetId, int skillSlot, String itemId, int targetClawkinIndex) {
        this.type = type;
        this.actorId = actorId;
        this.targetId = targetId;
        this.skillSlot = skillSlot;
        this.itemId = itemId;
        this.targetClawkinIndex = targetClawkinIndex;
    }

    public BattleActionType getType() {
        return type;
    }

    public String getActorId() {
        return actorId;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getSkillSlot() {
        return skillSlot;
    }

    public String getItemId() {
        return itemId;
    }

    public int getTargetClawkinIndex() {
        return targetClawkinIndex;
    }

    public boolean isItemAction() {
        return itemId != null;
    }
}
