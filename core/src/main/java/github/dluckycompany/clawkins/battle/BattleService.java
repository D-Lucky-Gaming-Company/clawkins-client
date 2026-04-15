package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;
import github.dluckycompany.clawkins.character.Clawkin;
import github.dluckycompany.clawkins.encounter.EncounterEvent;
import github.dluckycompany.clawkins.encounter.EncounterEventBus;
import github.dluckycompany.clawkins.encounter.EncounterEventType;

import java.util.List;

/**
 * Bridges world encounter events to battle state startup.
 */
public class BattleService {
    private static final String TAG = BattleService.class.getSimpleName();
    private static final int DEFAULT_PLAYER_HP = 100;
    private static final int DEFAULT_PLAYER_ATTACK = 12;
    private static final int DEFAULT_PLAYER_DEFENSE = 8;
    private static final int DEFAULT_PLAYER_SPEED = 10;

    private final EncounterEventBus encounterEventBus;
    private final BattleStateMachine battleStateMachine;
    private final PlayerBattleState playerBattleState;

    public BattleService(EncounterEventBus encounterEventBus, PlayerBattleState playerBattleState) {
        this.encounterEventBus = encounterEventBus;
        this.battleStateMachine = new BattleStateMachine();
        this.playerBattleState = playerBattleState;
    }

    public void update(float delta) {
        if (!battleStateMachine.isActive()) {
            while (encounterEventBus.hasEvents()) {
                EncounterEvent event = encounterEventBus.poll();
                if (event == null || event.getType() != EncounterEventType.START_ENCOUNTER) {
                    continue;
                }
                startBattle(event);
                break;
            }
        }

        if (battleStateMachine.isActive() && battleStateMachine.getPhase() == BattlePhase.CLAWKIN_FAINTED) {
            handleClawkinFainted();
        }
    }

    private void startBattle(EncounterEvent event) {
        playerBattleState.ensureInitialized(
                DEFAULT_PLAYER_HP,
                DEFAULT_PLAYER_ATTACK,
                DEFAULT_PLAYER_DEFENSE,
                DEFAULT_PLAYER_SPEED
        );

        String enemyId = event.getEncounterId();
        String enemyLabel = event.getEnemyName();
        if (enemyLabel == null || enemyLabel.isBlank()) {
            enemyLabel = enemyId;
        }
        BattleUnit activeUnit = buildActiveClawkinUnit();
        List<BattleSkill> activeSkills = createActiveClawkinSkills();
        Clawkin activeForLabel = playerBattleState.getActiveClawkin();
        String allyLabel = clawkinHudName(activeForLabel, activeUnit);
        BattleContext context = new BattleContext(
                event.getEncounterId(),
                event.getEncounterTableId(),
            List.of(activeUnit),
                List.of(new BattleUnit(enemyId, event.getEnemyHp(), event.getEnemyAttack(), event.getEnemyDefense(), event.getEnemySpeed())),
            activeSkills,
            event.getEnemySkills(),
                enemyLabel,
                event.getEnemyImagePath(),
                allyLabel
        );

        Clawkin activeClawkin = playerBattleState.getActiveClawkin();
        if (activeClawkin != null) {
            Gdx.app.log(
                    TAG,
                    "Battle start active clawkin -> id=" + activeClawkin.getId()
                            + ", name=" + activeClawkin.getName()
                            + ", hp=" + activeClawkin.getCurrentHp() + "/" + activeClawkin.getMaxHp()
                            + ", atk=" + activeClawkin.getBaseAttack()
                            + ", def=" + activeClawkin.getBaseDefense()
                            + ", spd=" + activeClawkin.getBaseSpeed()
                            + ", skillsUsed=" + activeSkills.size()
            );
            for (int i = 0; i < activeSkills.size(); i++) {
                BattleSkill skill = activeSkills.get(i);
                Gdx.app.log(
                        TAG,
                        "  skill" + (i + 1)
                                + " -> name=" + skill.getName()
                                + ", type=" + skill.getEffectType()
                        + ", base=" + skill.getEffectBaseStat()
                        + ", scale=" + skill.getEffectStatScale()
                                + ", duration=" + skill.getEffectDurationTurns()
                );
            }
        } else {
            Gdx.app.log(TAG, "Battle start active clawkin -> none (fallback skills may apply)");
        }

        battleStateMachine.begin(context);

        if (!playerBattleState.hasAnyAliveClawkin()) {
            battleStateMachine.finishAsDefeat();
        }
    }

    /**
     * Builds the first BattleUnit for the player's side at battle start.
     *
     * Priority:
     *   1. Use the clawkin the player selected in the team viewer (activeClawkinIndex),
     *      provided it is alive.
     *   2. Fall back to the first alive clawkin in party order.
     *   3. If the party is entirely fainted/empty, return a dummy 0-HP unit so the
     *      post-begin hasAnyAliveClawkin() check triggers an immediate defeat.
     */
    private BattleUnit buildActiveClawkinUnit() {
        // 1 — respect team-viewer selection
        int preSelected = playerBattleState.getActiveClawkinIndex();
        if (preSelected >= 0) {
            Clawkin c = playerBattleState.getClawkin(preSelected);
            if (c != null && c.isAlive()) {
                return new BattleUnit(c.getId(), c.getCurrentHp(), c.getBaseAttack(), c.getBaseDefense(), c.getBaseSpeed());
            }
        }

        // 2 — fall back to first alive slot
        for (int i = 0; i < playerBattleState.getPartySize(); i++) {
            Clawkin c = playerBattleState.getClawkin(i);
            if (c != null && c.isAlive()) {
                playerBattleState.setActiveClawkinIndex(i);
                return new BattleUnit(c.getId(), c.getCurrentHp(), c.getBaseAttack(), c.getBaseDefense(), c.getBaseSpeed());
            }
        }

        // 3 — no alive clawkin
        playerBattleState.setActiveClawkinIndex(-1);
        return new BattleUnit("none", 0, 1, 0, 1);
    }

    private void handleClawkinFainted() {
        int faintedIndex = playerBattleState.getActiveClawkinIndex();
        Clawkin fainted = playerBattleState.getClawkin(faintedIndex);
        if (fainted != null) {
            fainted.setCurrentHp(0);
        }

        int nextIndex = playerBattleState.findNextAliveClawkinIndex(faintedIndex);
        if (nextIndex == -1) {
            battleStateMachine.finishAsDefeat();
            return;
        }

        playerBattleState.setActiveClawkinIndex(nextIndex);
        Clawkin next = playerBattleState.getActiveClawkin();
        battleStateMachine.replaceAlly(
                new BattleUnit(next.getId(), next.getCurrentHp(), next.getBaseAttack(), next.getBaseDefense(), next.getBaseSpeed()),
                clawkinHudName(next, null)
        );
        battleStateMachine.replacePlayerSkills(resolveSkillsForClawkin(next));
        battleStateMachine.advanceFromFainted();
    }

    private List<BattleSkill> createActiveClawkinSkills() {
        Clawkin active = playerBattleState.getActiveClawkin();
        return resolveSkillsForClawkin(active);
    }

    private List<BattleSkill> resolveSkillsForClawkin(Clawkin clawkin) {
        if (clawkin == null) {
            Gdx.app.log(TAG, "resolveSkillsForClawkin: no active clawkin, using fallback player skills.");
            return playerBattleState.createPlayerSkills();
        }

        List<BattleSkill> skills = clawkin.getSkills();
        if (skills == null || skills.isEmpty()) {
            Gdx.app.log(TAG, "resolveSkillsForClawkin: clawkin has no parsed skills, using fallback player skills. clawkinId=" + clawkin.getId());
            return playerBattleState.createPlayerSkills();
        }
        Gdx.app.log(TAG, "resolveSkillsForClawkin: using parsed clawkin skills. clawkinId=" + clawkin.getId() + ", count=" + skills.size());
        return skills;
    }

    public BattleStateMachine getBattleStateMachine() {
        return battleStateMachine;
    }

    public boolean isBattleActive() {
        return battleStateMachine.isActive();
    }

    public boolean hasBattleSession() {
        return battleStateMachine.hasSession();
    }

    public void submitPlayerSkill(int skillSlot) {
        battleStateMachine.submitPlayerAction(new BattleAction(BattleActionType.ATTACK, "player", "enemy", skillSlot));
    }

    public void submitEscapeAction() {
        battleStateMachine.submitPlayerAction(new BattleAction(BattleActionType.ESCAPE, "player", "enemy"));
    }

    public void resolveEnemyTurn() {
        if (!battleStateMachine.canExecuteEnemyAction()) {
            return;
        }

        battleStateMachine.executeEnemyTurn();

        if (battleStateMachine.getPhase() == BattlePhase.CLAWKIN_FAINTED) {
            handleClawkinFainted();
        }
    }

    public void closeBattleSession() {
        if (battleStateMachine.hasSession()) {
            playerBattleState.applyBattleResult(battleStateMachine.firstAlly());
            playerBattleState.applyClawkinBattleResult(
                    playerBattleState.getActiveClawkinIndex(),
                    battleStateMachine.firstAlly()
            );
        }

        // Preserve the player's SWITCH selection across encounters.
        // Only auto-correct if the selected slot became invalid or the clawkin fainted.
        int activeIndex = playerBattleState.getActiveClawkinIndex();
        Clawkin active = playerBattleState.getClawkin(activeIndex);
        if (active == null || !active.isAlive()) {
            int replacement = -1;
            for (int i = 0; i < playerBattleState.getPartySize(); i++) {
                Clawkin candidate = playerBattleState.getClawkin(i);
                if (candidate != null && candidate.isAlive()) {
                    replacement = i;
                    break;
                }
            }
            playerBattleState.setActiveClawkinIndex(replacement);
        }

        battleStateMachine.reset();
    }

    public PlayerBattleState getPlayerBattleState() {
        return playerBattleState;
    }

    /** Same resolution as the player HP bar label in BattleHud (name, else id, else unit id). */
    private static String clawkinHudName(Clawkin c, BattleUnit fallbackUnit) {
        if (c != null) {
            String name = c.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
            String id = c.getId();
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        if (fallbackUnit != null) {
            String id = fallbackUnit.getId();
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        return "";
    }
}
