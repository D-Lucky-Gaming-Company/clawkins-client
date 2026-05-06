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
    private final String dialogueDirectory;
    private final boolean hasCollision;
    private final DialoguePosition dialoguePosition;
    private final boolean isMerchant;
    private int interactionCount;

    public Interactible(
            String objectName,
            String objectId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition) {
        this(objectName, objectId, dialogueDirectory, hasCollision, dialoguePosition, false);
    }

    public Interactible(
            String objectName,
            String objectId,
            String dialogueDirectory,
            boolean hasCollision,
            DialoguePosition dialoguePosition,
            boolean isMerchant) {
        this.objectName = objectName;
        this.objectId = objectId;
        this.dialogueDirectory = dialogueDirectory;
        this.hasCollision = hasCollision;
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

    public String getDialogueDirectory() {
        return dialogueDirectory;
    }

    public boolean hasCollision() {
        return hasCollision;
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

    public void incrementInteractionCount() {
        interactionCount++;
    }
}
