package github.dluckycompany.clawkins.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;

/**
 * Utility class for adding consistent sound effects to UI buttons.
 * Provides hover debouncing and click sounds using the existing AudioService.
 */
public class UiSoundHelper {
    
    private Actor lastHoveredActor;
    private final AudioService audioService;
    
    public UiSoundHelper(AudioService audioService) {
        this.audioService = audioService;
    }
    
    /**
     * Adds sound effects to a button with hover debouncing.
     * Plays UI_SELECT on click and UI_HOVER on hover (debounced).
     *
     * @param button the button to add sound effects to
     * @param onClickAction the action to run when clicked
     */
    public void addButtonSounds(TextButton button, Runnable onClickAction) {
        addButtonSounds(button, onClickAction, SoundEffect.UI_SELECT);
    }
    
    /**
     * Adds sound effects to a button with hover debouncing and custom click sound.
     *
     * @param button the button to add sound effects to
     * @param onClickAction the action to run when clicked
     * @param clickSound the sound to play on click
     */
    public void addButtonSounds(TextButton button, Runnable onClickAction, SoundEffect clickSound) {
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (audioService != null && clickSound != null) {
                    audioService.playSound(clickSound);
                }
                if (onClickAction != null) {
                    onClickAction.run();
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                // Only play hover sound if this is a different actor than last hovered
                if (lastHoveredActor != button && !button.isDisabled()) {
                    lastHoveredActor = button;
                    if (audioService != null) {
                        audioService.playSound(SoundEffect.UI_HOVER);
                    }
                }
            }
        });
    }
    
    /**
     * Plays a UI sound effect.
     *
     * @param effect the sound effect to play
     */
    public void playSound(SoundEffect effect) {
        if (audioService != null && effect != null) {
            audioService.playSound(effect);
        }
    }
    
    /**
     * Resets the hover tracking. Call this when changing screens or rebuilding UI.
     */
    public void resetHoverTracking() {
        lastHoveredActor = null;
    }
}
