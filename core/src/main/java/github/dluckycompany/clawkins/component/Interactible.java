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
    private final String objectText;
    private final String objectTextInteracted;
    private final boolean hasCollision;
    private final DialoguePosition dialoguePosition;
    private final boolean isMerchant;
    private boolean interacted;

    public Interactible(
            String objectName,
            String objectId,
            String objectText,
            String objectTextInteracted,
            boolean hasCollision,
            DialoguePosition dialoguePosition) {
        this(objectName, objectId, objectText, objectTextInteracted, hasCollision, dialoguePosition, false);
    }

    public Interactible(
            String objectName,
            String objectId,
            String objectText,
            String objectTextInteracted,
            boolean hasCollision,
            DialoguePosition dialoguePosition,
            boolean isMerchant) {
        this.objectName = objectName;
        this.objectId = objectId;
        this.objectText = objectText;
        this.objectTextInteracted = objectTextInteracted;
        this.hasCollision = hasCollision;
        this.dialoguePosition = dialoguePosition;
        this.isMerchant = isMerchant;
        this.interacted = false;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectText() {
        return objectText;
    }

    public String getObjectTextInteracted() {
        return objectTextInteracted;
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

    public boolean isInteracted() {
        return interacted;
    }

    public void setInteracted(boolean interacted) {
        this.interacted = interacted;
    }
}
