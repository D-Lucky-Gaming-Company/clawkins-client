package github.dluckycompany.clawkins.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Interactible implements Component {
    public enum DialoguePosition {
        TOP,
        BOTTOM
    }

    public static final ComponentMapper<Interactible> MAPPER = ComponentMapper.getFor(Interactible.class);

    private final String objectName;
    private final String objectId;
    private final String groupId;
    private final String dialogueDirectory;
    private final boolean hasCollision;
    private final boolean isTrippable;
    private final DialoguePosition dialoguePosition;
    private final boolean isMerchant;
    private int interactionCount;

    public Interactible(
            String objectName,
            String objectId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition) {
        this(objectName, objectId, dialogueDirectory, hasCollision, dialoguePosition, false, false);
    }

    public Interactible(
            String objectName,
            String objectId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition,
            boolean isMerchant) {
        this(objectName, objectId, dialogueDirectory, hasCollision, dialoguePosition, isMerchant, false);
    }

    public Interactible(
            String objectName,
            String objectId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition,
            boolean isMerchant,
            boolean isTrippable) {
        this(objectName, objectId, null, dialogueDirectory, hasCollision, dialoguePosition, isMerchant, isTrippable);
    }

    public Interactible(
            String objectName,
            String objectId,
            String groupId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition,
            boolean isMerchant,
            boolean isTrippable) {
        this.objectName = objectName;
        this.objectId = objectId;
        this.groupId = groupId;
        this.dialogueDirectory = dialogueDirectory;
        this.hasCollision = hasCollision;
        this.isTrippable = isTrippable;
        this.dialoguePosition = dialoguePosition;
        this.isMerchant = isMerchant;
        this.interactionCount = 0;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getDialogueDirectory() {
        return dialogueDirectory;
    }

    public boolean hasCollision() {
        return hasCollision;
    }

    public boolean isTrippable() {
        return isTrippable;
    }

    public DialoguePosition getDialoguePosition() {
        return dialoguePosition;
    }

    public boolean isMerchant() {
        return isMerchant;
    }

    public int getInteractionCount() {
        return interactionCount;
    }

    public void setInteractionCount(int interactionCount) {
        this.interactionCount = Math.max(0, interactionCount);
    }

    public void incrementInteractionCount() {
        interactionCount++;
    }
}
