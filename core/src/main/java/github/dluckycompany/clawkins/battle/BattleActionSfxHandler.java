package github.dluckycompany.clawkins.battle;

import java.util.List;

import github.dluckycompany.clawkins.audio.AudioService;
import github.dluckycompany.clawkins.audio.SoundEffect;

/**
 * Small mapper for battle-cycle action SFX.
 */
public class BattleActionSfxHandler {
    private final AudioService audioService;

    public BattleActionSfxHandler(AudioService audioService) {
        this.audioService = audioService;
    }

    public void playForActionResult(List<BattleTextSpan> spans) {
        if (audioService == null) {
            return;
        }
        if (containsRole(spans, BattleTextRole.DAMAGE)) {
            audioService.playSound(SoundEffect.HIT);
            return;
        }
        if (containsRole(spans, BattleTextRole.HEAL)) {
            audioService.playSound(SoundEffect.CONFIRM);
            return;
        }
        if (containsRole(spans, BattleTextRole.DEFENSE_UP)) {
            audioService.playSound(SoundEffect.UI_HOVER);
            return;
        }
        audioService.playSound(SoundEffect.UI_SELECT);
    }

    public void playSwitchAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.UI_SELECT);
        }
    }

    public void playEscapeAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.UI_BACK);
        }
    }

    private static boolean containsRole(List<BattleTextSpan> spans, BattleTextRole role) {
        if (spans == null || spans.isEmpty()) {
            return false;
        }
        for (BattleTextSpan span : spans) {
            if (span != null && span.role() == role) {
                return true;
            }
        }
        return false;
    }
}
