package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Disposable;
import github.dluckycompany.clawkins.component.Interactible;
import github.dluckycompany.clawkins.system.InteractionSystem;

public class DialogueOverlay implements Disposable {

    private final DialogueBoxRenderer dialogueBoxRenderer;
    private final boolean disposeRenderer;

    /**
     * @param dialogueBoxRenderer shared renderer (fonts + panel). If {@code disposeRenderer} is true,
     *                            this overlay disposes it in {@link #dispose()}.
     */
    public DialogueOverlay(DialogueBoxRenderer dialogueBoxRenderer, boolean disposeRenderer) {
        this.dialogueBoxRenderer = dialogueBoxRenderer;
        this.disposeRenderer = disposeRenderer;
    }

    public void render(Batch batch, InteractionSystem interactionSystem) {
        if (interactionSystem == null || !interactionSystem.isDialogueVisible()) {
            return;
        }

        dialogueBoxRenderer.render(
                batch,
                interactionSystem.getDialogueName(),
                interactionSystem.getDialogueText(),
                interactionSystem.getDialogueFullText(),
                interactionSystem.getDialoguePosition());
    }

    public void renderPrompt(Batch batch, String text, Interactible.DialoguePosition position) {
        dialogueBoxRenderer.render(batch, "", text, position);
    }

    @Override
    public void dispose() {
        if (disposeRenderer) {
            dialogueBoxRenderer.dispose();
        }
    }
}
