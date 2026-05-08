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

    public void playForPlayerAction(int skillSlot, List<BattleTextSpan> spans) {
        if (audioService == null) {
            return;
        }
        if (skillSlot == 1) {
            audioService.playSound(SoundEffect.BATTLE_ATTACK);
            return;
        }
        if (skillSlot == 2) {
            audioService.playSound(SoundEffect.BATTLE_DEFEND);
            return;
        }
        if (containsRole(spans, BattleTextRole.HEAL)) {
            audioService.playSound(SoundEffect.BATTLE_HEAL);
            return;
        }
        audioService.playSound(SoundEffect.BATTLE_SPECIAL);
    }

    public void playForEnemyActionResult(List<BattleTextSpan> spans) {
        if (audioService == null) {
            return;
        }
        if (containsRole(spans, BattleTextRole.DAMAGE)) {
            audioService.playSound(SoundEffect.BATTLE_ENEMY_ACTION);
            return;
        }
        if (containsRole(spans, BattleTextRole.HEAL)) {
            audioService.playSound(SoundEffect.BATTLE_HEAL);
            return;
        }
        if (containsRole(spans, BattleTextRole.DEFENSE_UP)) {
            audioService.playSound(SoundEffect.BATTLE_DEFEND);
            return;
        }
        audioService.playSound(SoundEffect.BATTLE_ENEMY_ACTION);
    }

    public void playSwitchAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.BATTLE_SWITCH);
        }
    }

    public void playEscapeAction() {
        if (audioService != null) {
            audioService.playSound(SoundEffect.BATTLE_ESCAPE);
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
