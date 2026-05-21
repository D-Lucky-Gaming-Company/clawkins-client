package github.dluckycompany.clawkins.battle;

import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Minimal state machine scaffold for turn-based combat.
 */
public class BattleStateMachine {
    private static final String TAG = BattleStateMachine.class.getSimpleName();

    /**
     * Max fraction of defender DEF ignored by the attacker's speed (armor penetration cap).
     */
    private static final double ARMOR_PENETRATION_P_MAX = 0.25d;
    /**
     * Scale constant for penetration curve: {@code p = min(p_max, S / (S + K))}.
     */
    private static final double ARMOR_PENETRATION_K = 50d;

    private enum EnemyAiTier {
        MINOR,
        BOSS_1,
        BOSS_2,
        BOSS_3,
        BOSS_4
    }

    private BattlePhase phase = BattlePhase.INIT;
    private BattleContext context;
    private String lastLog = "";
    private List<BattleTextSpan> lastLogSpans = List.of();
    /** Set when the player tried to use a skill that is on cooldown (for UI/audio feedback). */
    private boolean lastPlayerSkillCooldownReject = false;
    
    // Round tracking for EXP rewards
    private int currentRound = 0;
    private int roundExpAccumulated = 0;

    public void begin(BattleContext context) {
        this.context = context;
        this.currentRound = 0;
        this.roundExpAccumulated = 0;

        BattleUnit ally = firstAlly();
        BattleUnit enemy = firstEnemy();
        if (ally != null && enemy != null && ally.getHp() > 0 && enemy.getHp() > 0) {
            boolean allyFirst = allyActsBeforeEnemy(ally, enemy);
            this.phase = allyFirst ? BattlePhase.PLAYER_COMMAND : BattlePhase.ENEMY_COMMAND;
            String allyName = allyLogName(ally);
            String enemyName = enemyLogName(enemy);
            String who = allyFirst ? allyName : enemyName;
            setLastLogPlain("Encounter started.\n" + who + " moves first.");
        } else {
            this.phase = BattlePhase.PLAYER_COMMAND;
            setLastLogPlain("Encounter started.");
        }
    }

    public BattlePhase getPhase() {
        return phase;
    }

    public BattleContext getContext() {
        return context;
    }

    public boolean isActive() {
        return context != null
                && phase != BattlePhase.VICTORY
                && phase != BattlePhase.DEFEAT
                && phase != BattlePhase.ESCAPE;
    }

    public boolean hasSession() {
        return context != null;
    }

    public boolean canAcceptPlayerAction() {
        return phase == BattlePhase.PLAYER_COMMAND;
    }

    public boolean canExecuteEnemyAction() {
        return phase == BattlePhase.ENEMY_COMMAND;
    }

    public void submitPlayerAction(BattleAction action) {
        if (!canAcceptPlayerAction()) {
            return;
        }
        lastPlayerSkillCooldownReject = false;
        if (action.getType() == BattleActionType.ESCAPE) {
            finishAsEscape();
            setLastLogPlain("You escaped.");
            return;
        }
        if (action.getType() == BattleActionType.ITEM) {
            handleItemAction(action);
            // Item usage consumes the turn - proceed to enemy command
            phase = BattlePhase.ENEMY_COMMAND;
            return;
        }
        if (action.getType() != BattleActionType.ATTACK) {
            setLastLogPlain("Only ATTACK, ITEM, and ESCAPE are wired in this prototype.");
            return;
        }

        BattleUnit player = firstAlly();
        BattleUnit enemy = firstEnemy();
        if (player == null || enemy == null) {
            finishAsDefeat();
            return;
        }

        BattleSkill skill = playerSkill(action.getSkillSlot());
        String skillName = skill != null ? skill.getName() : "Attack";
        String playerName = allyLogName(player);
        String enemyName = enemyLogName(enemy);
        Gdx.app.log(TAG, "Player action -> slot=" + action.getSkillSlot() + ", resolvedSkill=" + describeSkill(skill));

        // Validate skill unlock state using SkillManager
        SkillManager skillManager = context != null ? context.getSkillManager() : null;
        if (skillManager != null) {
            int slotIndex = action.getSkillSlot() - 1; // Convert 1-based to 0-based
            if (!skillManager.canUseSkill(slotIndex, player)) {
                String validationMsg = skillManager.getSkillValidationMessage(slotIndex, player);
                setLastLogPlain(validationMsg);
                if (skillManager.isSkillBlockedOnlyByCooldown(slotIndex, player)) {
                    lastPlayerSkillCooldownReject = true;
                }
                return; // Don't consume turn if skill cannot be used
            }
        }

        // Check cooldown (fallback if SkillManager not available)
        if (skill != null && player.isSkillOnCooldown(skillName)) {
            int cooldownRemaining = player.getSkillCooldown(skillName);
            setLastLogPlain(skillName + " is on cooldown (" + cooldownRemaining + " turns remaining).");
            lastPlayerSkillCooldownReject = true;
            return; // Don't consume turn if skill is on cooldown
        }

        if (skill != null && skill.getEffectType() == BattleSkill.EffectType.HEAL) {
            int heal = resolveMagnitude(player, enemy, skill, "maxhp[self] * 0.15");
            player.setHp(Math.min(player.getMaxHp(), player.getHp() + heal));
            
            // Stretch & Nap also grants +1 DEF for 2 turns
            if ("Stretch & Nap".equals(skillName)) {
                player.addTemporaryBoost(BattleUnit.StatType.DEFENSE, 1, 2);
            }
            
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(" and recovered ")
                    .appendHeal(heal)
                    .appendPlain(".");
            
            if ("Stretch & Nap".equals(skillName)) {
                lb.appendPlain(" Defense UP for 2 turns!");
            }
            
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player heal applied -> amount=" + heal + ", hpNow=" + player.getHp() + "/" + player.getMaxHp());
            
        } else if (skill != null && skill.getEffectType() == BattleSkill.EffectType.BLEED) {
            // Claw & Chomp: deals damage and inflicts Bleed
            int damage = skill.getEffectBaseStat(); // Flat 25 damage
            enemy.setHp(Math.max(0, enemy.getHp() - damage));
            
            // Apply Bleed: 5% of enemy's Max HP per turn for 2 turns
            int bleedDamage = Math.max(1, (int) Math.round(enemy.getMaxHp() * 0.05));
            enemy.applyBleed(bleedDamage, skill.getEffectDurationTurns());
            
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(" and deals ")
                    .appendDamage(damage)
                    .appendPlain(" to ")
                    .appendName(enemyName)
                    .appendPlain(". ")
                    .appendName(enemyName)
                    .appendPlain(" is bleeding!");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player Bleed applied -> damage=" + damage + ", bleedDamage=" + bleedDamage + "/turn for " + skill.getEffectDurationTurns() + " turns");
            
        } else if (skill != null && skill.getEffectType() == BattleSkill.EffectType.ATTACK) {
            if (skill.getEffectDurationTurns() > 0) {
                int boost = Math.max(1, resolveMagnitude(player, enemy, skill, "attack[self]") / 4);
                int turns = skill.getEffectDurationTurns();
                player.addTemporaryBoost(BattleUnit.StatType.ATTACK, boost, turns);
                LogBuilder lb = new LogBuilder()
                        .appendName(playerName)
                        .appendPlain(" uses ")
                        .appendName(skillName)
                        .appendPlain(", attack UP.");
                setLastLog(lb.text(), lb.spans());
                Gdx.app.log(TAG, "Player attack buff applied -> boost=" + boost + ", turns=" + turns + ", attackNow=" + player.getAttack());
            } else {
                int offense = resolveMagnitude(player, enemy, skill, "attack[self]");
                int damage = physicalDamageFromHit(offense, enemy, player);
                enemy.setHp(Math.max(0, enemy.getHp() - damage));
                LogBuilder lb = new LogBuilder()
                        .appendName(playerName)
                        .appendPlain(" uses ")
                        .appendName(skillName)
                        .appendPlain(" and deals ")
                        .appendDamage(damage)
                        .appendPlain(" to ")
                        .appendName(enemyName)
                        .appendPlain(".");
                setLastLog(lb.text(), lb.spans());
                Gdx.app.log(TAG, "Player attack-as-damage applied -> offense=" + offense + ", enemyDefense=" + enemy.getDefense()
                        + ", effDefense=" + effectiveDefenseVsAttacker(enemy, player) + ", damage=" + damage);
            }
        } else if (skill != null && skill.getEffectType() == BattleSkill.EffectType.DEFENSE) {
            int boost = Math.max(1, resolveMagnitude(player, enemy, skill, "defense[self]") / 4);
            int turns = Math.max(1, skill.getEffectDurationTurns());
            player.addTemporaryBoost(BattleUnit.StatType.DEFENSE, boost, turns);
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(", ")
                    .appendDefenseUpWords()
                    .appendPlain(".");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player defense buff applied -> boost=" + boost + ", turns=" + turns + ", defenseNow=" + player.getDefense());
        } else if (skill != null && skill.getEffectType() == BattleSkill.EffectType.PARRY) {
            int turns = Math.max(1, skill.getEffectDurationTurns());
            player.activateParry(skillName, turns);
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(" and enters Parry Stance.");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player parry stance applied -> turns=" + turns + ", skill=" + skillName);
        } else {
            // Heavy Paw: flat 15 damage
            int damage = skill != null ? skill.getEffectBaseStat() : 10;
            enemy.setHp(Math.max(0, enemy.getHp() - damage));
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(" and deals ")
                    .appendDamage(damage)
                    .appendPlain(" to ")
                    .appendName(enemyName)
                    .appendPlain(".");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player damage applied -> damage=" + damage);
        }

        if (skill != null && skill.getTurnCooldown() > 0) {
            player.setSkillCooldown(skillName, skill.getTurnCooldown());
        }

        // Tick enemy cooldowns and boosts
        enemy.tickTemporaryBoosts();
        enemy.tickCooldowns();
        
        // Apply Bleed damage to enemy
        int bleedDamage = enemy.tickBleed();
        if (bleedDamage > 0) {
            enemy.setHp(Math.max(0, enemy.getHp() - bleedDamage));
            String bleedMsg = "\n" + enemyName + " takes " + bleedDamage + " bleed damage.";
            int off = lastLog.length();
            lastLog = lastLog + bleedMsg;
            ArrayList<BattleTextSpan> ext = new ArrayList<>(lastLogSpans);
            int nameStart = off + 1;
            ext.add(new BattleTextSpan(nameStart, nameStart + enemyName.length(), BattleTextRole.NAME));
            int dmgStart = lastLog.indexOf(String.valueOf(bleedDamage), off);
            if (dmgStart >= 0) {
                ext.add(new BattleTextSpan(dmgStart, dmgStart + String.valueOf(bleedDamage).length(), BattleTextRole.DAMAGE));
            }
            lastLogSpans = List.copyOf(ext);
            Gdx.app.log(TAG, "Enemy bleed damage -> amount=" + bleedDamage + ", hpNow=" + enemy.getHp());
        }

        if (enemy.getHp() <= 0) {
            finishAsVictory();
            LogBuilder lb = new LogBuilder().appendPlain("Victory! ").appendName(enemyLogName(enemy)).appendPlain(" was defeated.");
            setLastLog(lb.text(), lb.spans());
            return;
        }

        phase = BattlePhase.ENEMY_COMMAND;
    }

    public void tick(float delta) {
        if (canExecuteEnemyAction()) {
            executeEnemyTurn();
        }
    }

    public void executeEnemyTurn() {
        if (!canExecuteEnemyAction()) {
            return;
        }

            BattleUnit player = firstAlly();
            BattleUnit enemy = firstEnemy();
            if (player == null || enemy == null) {
                finishAsDefeat();
                return;
            }

            BattleSkill enemySkill = chooseEnemySkill(enemy, player);
            String enemySkillName = enemySkill != null ? enemySkill.getName() : "Attack";
            String enemyName = enemyLogName(enemy);
            String playerName = allyLogName(player);
            int playerDefenseBeforeAction = player.getDefense();
            Gdx.app.log(TAG, "Enemy action -> resolvedSkill=" + describeSkill(enemySkill));

            if (enemySkill != null && enemySkill.getEffectType() == BattleSkill.EffectType.HEAL) {
                int heal = resolveMagnitude(enemy, player, enemySkill, "defense[self]");
                enemy.setHp(Math.min(enemy.getMaxHp(), enemy.getHp() + heal));
                LogBuilder lb = new LogBuilder()
                        .appendName(enemyName)
                        .appendPlain(" uses ")
                        .appendName(enemySkillName)
                        .appendPlain(" and recovered ")
                        .appendHeal(heal)
                        .appendPlain(".");
                setLastLog(lb.text(), lb.spans());
                Gdx.app.log(TAG, "Enemy heal applied -> amount=" + heal + ", hpNow=" + enemy.getHp() + "/" + enemy.getMaxHp());
            } else if (enemySkill != null && enemySkill.getEffectType() == BattleSkill.EffectType.ATTACK) {
                if (enemySkill.getEffectDurationTurns() > 0) {
                    int boost = Math.max(1, resolveMagnitude(enemy, player, enemySkill, "attack[self]") / 4);
                    int turns = enemySkill.getEffectDurationTurns();
                    enemy.addTemporaryBoost(BattleUnit.StatType.ATTACK, boost, turns);
                    LogBuilder lb = new LogBuilder()
                            .appendName(enemyName)
                            .appendPlain(" uses ")
                            .appendName(enemySkillName)
                            .appendPlain(", attack UP.");
                    setLastLog(lb.text(), lb.spans());
                    Gdx.app.log(TAG, "Enemy attack buff applied -> boost=" + boost + ", turns=" + turns + ", attackNow=" + enemy.getAttack());
                } else {
                    int offense = resolveMagnitude(enemy, player, enemySkill, "attack[self]");
                    int damage = physicalDamageFromHit(offense, player, enemy);
                    if (!tryResolveDartParry(player, enemy, enemyName, enemySkillName, offense, damage)) {
                        player.setHp(Math.max(0, player.getHp() - damage));
                        LogBuilder lb = new LogBuilder()
                                .appendName(enemyName)
                                .appendPlain(" uses ")
                                .appendName(enemySkillName)
                                .appendPlain(" and deals ")
                                .appendDamage(damage)
                                .appendPlain(" to you.");
                        setLastLog(lb.text(), lb.spans());
                        Gdx.app.log(TAG, "Enemy attack-as-damage applied -> offense=" + offense + ", playerDefense=" + player.getDefense()
                                + ", effDefense=" + effectiveDefenseVsAttacker(player, enemy) + ", damage=" + damage);
                    }
                }
            } else if (enemySkill != null && enemySkill.getEffectType() == BattleSkill.EffectType.DEFENSE) {
                int boost = Math.max(1, resolveMagnitude(enemy, player, enemySkill, "defense[self]") / 4);
                int turns = Math.max(1, enemySkill.getEffectDurationTurns());
                enemy.addTemporaryBoost(BattleUnit.StatType.DEFENSE, boost, turns);
                LogBuilder lb = new LogBuilder()
                        .appendName(enemyName)
                        .appendPlain(" uses ")
                        .appendName(enemySkillName)
                        .appendPlain(", ")
                        .appendDefenseUpWords()
                        .appendPlain(".");
                setLastLog(lb.text(), lb.spans());
                Gdx.app.log(TAG, "Enemy defense buff applied -> boost=" + boost + ", turns=" + turns + ", defenseNow=" + enemy.getDefense());
            } else {
                int offense = resolveMagnitude(enemy, player, enemySkill, "attack[self]");
                int damage = physicalDamageFromHit(offense, player, enemy);
                if (!tryResolveDartParry(player, enemy, enemyName, enemySkillName, offense, damage)) {
                    player.setHp(Math.max(0, player.getHp() - damage));
                    LogBuilder lb = new LogBuilder()
                            .appendName(enemyName)
                            .appendPlain(" uses ")
                            .appendName(enemySkillName)
                            .appendPlain(" and deals ")
                            .appendDamage(damage)
                            .appendPlain(" to you.");
                    setLastLog(lb.text(), lb.spans());
                    Gdx.app.log(TAG, "Enemy damage applied -> offense=" + offense + ", playerDefense=" + player.getDefense()
                            + ", effDefense=" + effectiveDefenseVsAttacker(player, enemy) + ", damage=" + damage);
                }
            }

            if (enemy.getHp() <= 0) {
                finishAsVictory();
                lastLog = lastLog + "\nVictory! " + enemyName + " was defeated.";
                return;
            }

            if (player.hasActiveParry()) {
                player.consumeParryTurn();
            }

            player.tickTemporaryBoosts();
            player.tickCooldowns();
            
            // Apply Bleed damage to player
            int bleedDamage = player.tickBleed();
            if (bleedDamage > 0) {
                player.setHp(Math.max(0, player.getHp() - bleedDamage));
                String bleedMsg = "\nYou take " + bleedDamage + " bleed damage.";
                int off = lastLog.length();
                lastLog = lastLog + bleedMsg;
                ArrayList<BattleTextSpan> ext = new ArrayList<>(lastLogSpans);
                int dmgStart = lastLog.indexOf(String.valueOf(bleedDamage), off);
                if (dmgStart >= 0) {
                    ext.add(new BattleTextSpan(dmgStart, dmgStart + String.valueOf(bleedDamage).length(), BattleTextRole.DAMAGE));
                }
                lastLogSpans = List.copyOf(ext);
                Gdx.app.log(TAG, "Player bleed damage -> amount=" + bleedDamage + ", hpNow=" + player.getHp());
            }
            
            int playerDefenseAfterTick = player.getDefense();
            if (playerDefenseAfterTick < playerDefenseBeforeAction) {
                String extra = "\n" + playerName + "'s defense is now back to normal.";
                int off = lastLog.length();
                lastLog = lastLog + extra;
                ArrayList<BattleTextSpan> ext = new ArrayList<>(lastLogSpans);
                int nameStart = off + 1;
                ext.add(new BattleTextSpan(nameStart, nameStart + playerName.length(), BattleTextRole.NAME));
                lastLogSpans = List.copyOf(ext);
            }
            if (player.getHp() <= 0) {
                phase = BattlePhase.CLAWKIN_FAINTED;
                LogBuilder fl = new LogBuilder().appendName(allyLogName(player)).appendPlain(" fainted!");
                setLastLog(fl.text(), fl.spans());
                return;
            }
            
            // Grant round EXP at the end of each complete round
            currentRound++;
            int roundExp = github.dluckycompany.clawkins.character.LevelSystem.calculateRoundExpReward();
            roundExpAccumulated += roundExp;
            Gdx.app.log(TAG, "Round " + currentRound + " complete -> granted " + roundExp + " EXP (total accumulated: " + roundExpAccumulated + ")");
            
            phase = BattlePhase.PLAYER_COMMAND;
    }

    public void finishAsVictory() {
        phase = BattlePhase.VICTORY;
    }

    public void finishAsDefeat() {
        phase = BattlePhase.DEFEAT;
    }

    public void finishAsEscape() {
        phase = BattlePhase.ESCAPE;
    }

    /** Replaces the active ally with a new unit (used on clawkin switch). */
    public void replaceAlly(BattleUnit unit, String allyDisplayName) {
        if (context != null) {
            context.replaceFirstAlly(unit);
            context.setAllyDisplayName(allyDisplayName);
        }
    }

    public void replacePlayerSkills(List<BattleSkill> skills) {
        if (context != null) {
            context.replacePlayerSkills(skills);
        }
    }

    /** Called after a successful clawkin switch to resume battle. */
    public void advanceFromFainted() {
        if (phase == BattlePhase.CLAWKIN_FAINTED) {
            phase = BattlePhase.PLAYER_COMMAND;
        }
    }

    /**
     * Consumes the player's turn after manually switching clawkins.
     * This moves battle flow to enemy command phase.
     */
    public void consumeTurnAfterSwitch(String allyDisplayName) {
        if (phase != BattlePhase.PLAYER_COMMAND) {
            return;
        }
        String display = (allyDisplayName == null || allyDisplayName.isBlank()) ? "Your clawkin" : allyDisplayName;
        setLastLogPlain(display + " entered battle.");
        phase = BattlePhase.ENEMY_COMMAND;
    }

    public void reset() {
        phase = BattlePhase.INIT;
        context = null;
        lastLog = "";
        lastLogSpans = List.of();
        lastPlayerSkillCooldownReject = false;
        currentRound = 0;
        roundExpAccumulated = 0;
    }
    
    /**
     * Gets the total EXP accumulated from completed rounds.
     * This EXP should be awarded even if the battle is lost.
     * 
     * @return Total round EXP accumulated
     */
    public int getRoundExpAccumulated() {
        return roundExpAccumulated;
    }
    
    /**
     * Gets the current round number.
     * 
     * @return Current round (0 before first round completes)
     */
    public int getCurrentRound() {
        return currentRound;
    }

    public String getLastLog() {
        return lastLog;
    }

    public List<BattleTextSpan> getLastLogSpans() {
        return List.copyOf(lastLogSpans);
    }

    /**
     * Whether the last {@link #submitPlayerAction} rejected an attack due to skill cooldown, and clears the flag.
     */
    public boolean consumeLastPlayerSkillCooldownReject() {
        boolean v = lastPlayerSkillCooldownReject;
        lastPlayerSkillCooldownReject = false;
        return v;
    }

    private void setLastLogPlain(String text) {
        lastLog = text == null ? "" : text;
        lastLogSpans = List.of();
    }

    private void setLastLog(String text, List<BattleTextSpan> spans) {
        lastLog = text == null ? "" : text;
        lastLogSpans = spans == null ? List.of() : List.copyOf(spans);
    }

    /** Builds {@link #lastLog} with parallel {@link BattleTextSpan} ranges for colored battle dialogue. */
    private static final class LogBuilder {
        private final StringBuilder sb = new StringBuilder();
        private final ArrayList<BattleTextSpan> spans = new ArrayList<>();

        LogBuilder appendPlain(String t) {
            if (t != null) {
                sb.append(t);
            }
            return this;
        }

        LogBuilder appendName(String name) {
            int s = sb.length();
            sb.append(name);
            spans.add(new BattleTextSpan(s, sb.length(), BattleTextRole.NAME));
            return this;
        }

        LogBuilder appendDamage(int v) {
            String t = String.valueOf(v);
            int s = sb.length();
            sb.append(t);
            spans.add(new BattleTextSpan(s, sb.length(), BattleTextRole.DAMAGE));
            return this;
        }

        LogBuilder appendHeal(int v) {
            String t = String.valueOf(v);
            int s = sb.length();
            sb.append(t);
            spans.add(new BattleTextSpan(s, sb.length(), BattleTextRole.HEAL));
            return this;
        }

        LogBuilder appendDefenseUpWords() {
            String t = "defense UP";
            int s = sb.length();
            sb.append(t);
            spans.add(new BattleTextSpan(s, sb.length(), BattleTextRole.DEFENSE_UP));
            return this;
        }

        String text() {
            return sb.toString();
        }

        List<BattleTextSpan> spans() {
            return List.copyOf(spans);
        }
    }

    public BattleUnit firstAlly() {
        if (context == null || context.getAllies().isEmpty()) {
            return null;
        }
        return context.getAllies().getFirst();
    }

    public BattleUnit firstEnemy() {
        if (context == null || context.getEnemies().isEmpty()) {
            return null;
        }
        return context.getEnemies().getFirst();
    }

    public BattleSkill playerSkill(int slot) {
        if (context == null || context.getPlayerSkills().isEmpty()) {
            return null;
        }
        int size = context.getPlayerSkills().size();
        int idx = Math.max(1, Math.min(size, slot)) - 1;
        return context.getPlayerSkills().get(idx);
    }

    private String allyLogName(BattleUnit player) {
        if (context != null) {
            String n = context.getAllyDisplayName();
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        return displayUnitName(player, "clawkin");
    }

    private String enemyLogName(BattleUnit enemy) {
        if (context != null) {
            String n = context.getEnemyDisplayName();
            if (n != null && !n.isBlank()) {
                return n;
            }
        }
        return displayUnitName(enemy, "enemy");
    }

    private static String displayUnitName(BattleUnit unit, String fallback) {
        if (unit == null) {
            return fallback;
        }
        String id = unit.getId();
        if (id == null || id.isBlank()) {
            return fallback;
        }
        return id;
    }

    private BattleSkill chooseEnemySkill(BattleUnit enemy, BattleUnit player) {
        if (context == null) {
            return null;
        }
        List<BattleSkill> enemySkills = context.getEnemySkills();
        if (enemySkills.isEmpty()) {
            return null;
        }
        if (enemySkills.size() == 1 || enemy == null || player == null) {
            return enemySkills.getFirst();
        }

        EnemyAiTier tier = resolveEnemyAiTier();

        // Lower tiers stay less consistent, higher tiers act intentionally more often.
        if (ThreadLocalRandom.current().nextDouble() > smartDecisionChance(tier)) {
            return randomEnemySkill();
        }

        double[] scores = new double[enemySkills.size()];
        double maxScore = Double.NEGATIVE_INFINITY;
        int bestIndex = 0;
        for (int i = 0; i < enemySkills.size(); i++) {
            BattleSkill skill = enemySkills.get(i);
            double score = scoreEnemySkill(skill, enemy, player, tier);
            scores[i] = score;
            if (score > maxScore) {
                maxScore = score;
                bestIndex = i;
            }
        }

        if (maxScore <= 0.01d) {
            return randomEnemySkill();
        }

        // Keep unpredictability using weighted sampling; boss tiers sample sharper toward the best option.
        if (ThreadLocalRandom.current().nextDouble() < explorationChance(tier)) {
            return weightedSampleByScore(enemySkills, scores, sharpness(tier));
        }

        return enemySkills.get(bestIndex);
    }

    private BattleSkill weightedSampleByScore(List<BattleSkill> skills, double[] scores, double sharpness) {
        double sum = 0d;
        double[] weights = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double normalized = Math.max(0.01d, scores[i]);
            double w = Math.pow(normalized, Math.max(1d, sharpness));
            weights[i] = w;
            sum += w;
        }

        if (sum <= 0d) {
            return randomEnemySkill();
        }

        double roll = ThreadLocalRandom.current().nextDouble(sum);
        double running = 0d;
        for (int i = 0; i < weights.length; i++) {
            running += weights[i];
            if (roll <= running) {
                return skills.get(i);
            }
        }
        return skills.getLast();
    }

    private double scoreEnemySkill(BattleSkill skill, BattleUnit enemy, BattleUnit player, EnemyAiTier tier) {
        if (skill == null || enemy == null || player == null) {
            return 0d;
        }

        BattleSkill.EffectType type = skill.getEffectType();
        boolean healSkill = type == BattleSkill.EffectType.HEAL;
        boolean attackBuffSkill = type == BattleSkill.EffectType.ATTACK && skill.getEffectDurationTurns() > 0;
        boolean defenseBuffSkill = type == BattleSkill.EffectType.DEFENSE;
        boolean directDamageSkill = !healSkill && !attackBuffSkill && !defenseBuffSkill;

        double playerHpRatio = ratio(player.getHp(), player.getMaxHp());
        double enemyHpRatio = ratio(enemy.getHp(), enemy.getMaxHp());
        boolean playerLowHp = playerHpRatio <= 0.40d;
        boolean playerCriticalHp = playerHpRatio <= 0.22d;
        boolean enemyLowHp = enemyHpRatio <= 0.35d;
        boolean enemyCriticalHp = enemyHpRatio <= 0.20d;

        double playerThreat = player.getAttack() / (double) Math.max(1, enemy.getDefense());
        boolean playerHasHighAttackPressure = playerThreat >= 1.55d;

        int estimatedDamage = estimateEnemyDamage(skill, enemy, player);
        int estimatedHeal = estimateEnemyHeal(skill, enemy, player);
        int enemyMissingHp = Math.max(0, enemy.getMaxHp() - enemy.getHp());

        double score = 0.4d;

        if (directDamageSkill) {
            double hpChunk = estimatedDamage / (double) Math.max(1, player.getMaxHp());
            score += 3.0d + hpChunk * 8.0d;
            if (playerLowHp) {
                score += 2.0d + finishBias(tier);
            }
            if (estimatedDamage >= player.getHp()) {
                score += 3.5d + finishBias(tier) * 1.8d;
            }
            if (player.getDefense() >= enemy.getAttack() && estimatedDamage < Math.max(2, player.getMaxHp() / 8)) {
                score -= 1.0d;
            }
            if (enemyLowHp && !playerCriticalHp) {
                score -= cautionBias(tier);
            }
        }

        if (healSkill) {
            if (enemyMissingHp <= 0) {
                score -= 1.5d;
            } else {
                double healCoverage = Math.min(1.0d, estimatedHeal / (double) enemyMissingHp);
                score += 1.5d + healCoverage * 4.5d;
            }
            if (enemyLowHp) {
                score += 1.0d + cautionBias(tier) * 1.5d;
            }
            if (enemyCriticalHp) {
                score += 1.5d + cautionBias(tier) * 2.0d;
            }
            if (playerCriticalHp && !enemyLowHp) {
                score -= 1.2d;
            }
        }

        if (defenseBuffSkill) {
            score += 1.2d;
            if (playerHasHighAttackPressure) {
                score += 1.8d + cautionBias(tier);
            }
            if (enemyLowHp) {
                score += 0.8d + cautionBias(tier);
            }
            if (playerCriticalHp) {
                score -= 0.6d;
            }
        }

        if (attackBuffSkill) {
            score += 1.0d;
            if (enemyHpRatio >= 0.55d) {
                score += 0.8d;
            }
            if (player.getDefense() > enemy.getAttack()) {
                score += 1.2d;
            }
            if (enemyLowHp) {
                score -= 1.0d;
            }
        }

        return Math.max(0.05d, score);
    }

    private int estimateEnemyDamage(BattleSkill skill, BattleUnit enemy, BattleUnit player) {
        int offense = resolveMagnitude(enemy, player, skill, "attack[self]");
        return physicalDamageFromHit(offense, player, enemy);
    }

    private int estimateEnemyHeal(BattleSkill skill, BattleUnit enemy, BattleUnit player) {
        return resolveMagnitude(enemy, player, skill, "defense[self]");
    }

    private EnemyAiTier resolveEnemyAiTier() {
        if (context == null) {
            return EnemyAiTier.MINOR;
        }
        String encounterId = safeLower(context.getEncounterId());
        String encounterTableId = safeLower(context.getEncounterTableId());
        String enemyName = safeLower(context.getEnemyDisplayName());
        String merged = encounterId + " " + encounterTableId + " " + enemyName;

        // New naming convention: boss_0_encounter (Bert Jr.), then boss_1_encounter, boss_2_encounter...
        if (containsAny(merged, "boss_3_encounter", "boss_4", "boss4", "boss04", "final_boss", "finalboss")) {
            return EnemyAiTier.BOSS_4;
        }
        if (containsAny(merged, "boss_2_encounter", "boss_3", "boss3", "boss03")) {
            return EnemyAiTier.BOSS_3;
        }
        if (containsAny(merged, "boss_1_encounter", "boss_2", "boss2", "boss02")) {
            return EnemyAiTier.BOSS_2;
        }
        if (containsAny(merged, "boss_0_encounter", "boss_1", "boss1", "boss01", "bert_jr")) {
            return EnemyAiTier.BOSS_1;
        }
        if (merged.contains("boss")) {
            return EnemyAiTier.BOSS_2;
        }
        return EnemyAiTier.MINOR;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private static boolean containsAny(String source, String... needles) {
        if (source == null || source.isBlank() || needles == null || needles.length == 0) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static double ratio(int value, int max) {
        if (max <= 0) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, value / (double) max));
    }

    private static double smartDecisionChance(EnemyAiTier tier) {
        return switch (tier) {
            case MINOR -> 0.28d;
            case BOSS_1 -> 0.58d;
            case BOSS_2 -> 0.72d;
            case BOSS_3 -> 0.84d;
            case BOSS_4 -> 0.93d;
        };
    }

    private static double explorationChance(EnemyAiTier tier) {
        return switch (tier) {
            case MINOR -> 0.72d;
            case BOSS_1 -> 0.45d;
            case BOSS_2 -> 0.33d;
            case BOSS_3 -> 0.22d;
            case BOSS_4 -> 0.14d;
        };
    }

    private static double sharpness(EnemyAiTier tier) {
        return switch (tier) {
            case MINOR -> 1.0d;
            case BOSS_1 -> 1.2d;
            case BOSS_2 -> 1.45d;
            case BOSS_3 -> 1.75d;
            case BOSS_4 -> 2.1d;
        };
    }

    private static double cautionBias(EnemyAiTier tier) {
        return switch (tier) {
            case MINOR -> 0.4d;
            case BOSS_1 -> 0.9d;
            case BOSS_2 -> 1.2d;
            case BOSS_3 -> 1.5d;
            case BOSS_4 -> 1.8d;
        };
    }

    private static double finishBias(EnemyAiTier tier) {
        return switch (tier) {
            case MINOR -> 0.3d;
            case BOSS_1 -> 0.7d;
            case BOSS_2 -> 1.1d;
            case BOSS_3 -> 1.4d;
            case BOSS_4 -> 1.8d;
        };
    }

    private BattleSkill randomEnemySkill() {
        if (context == null) {
            return null;
        }
        List<BattleSkill> enemySkills = context.getEnemySkills();
        if (enemySkills.isEmpty()) {
            return null;
        }
        int randomIndex = ThreadLocalRandom.current().nextInt(enemySkills.size());
        return enemySkills.get(randomIndex);
    }

    /**
     * Whether the ally should act before the enemy on the first turn of battle.
     * Higher speed wins; ties break on higher attack, then higher defense, then ally (player) first.
     */
    private static boolean allyActsBeforeEnemy(BattleUnit ally, BattleUnit enemy) {
        if (enemy == null) {
            return true;
        }
        if (ally == null) {
            return false;
        }
        int c = Integer.compare(enemy.getSpeed(), ally.getSpeed());
        if (c != 0) {
            return c < 0;
        }
        c = Integer.compare(enemy.getAttack(), ally.getAttack());
        if (c != 0) {
            return c < 0;
        }
        c = Integer.compare(enemy.getDefense(), ally.getDefense());
        if (c != 0) {
            return c < 0;
        }
        return true;
    }

    /**
     * Defender's defense after the attacker's speed applies capped percent armor penetration:
     * {@code D_eff = floor(D * (1 - min(p_max, S / (S + K))))}.
     */
    private static int effectiveDefenseVsAttacker(BattleUnit defender, BattleUnit attacker) {
        if (defender == null) {
            return 0;
        }
        int raw = defender.getDefense();
        if (attacker == null) {
            return Math.max(0, raw);
        }
        int s = Math.max(1, attacker.getSpeed());
        double p = Math.min(ARMOR_PENETRATION_P_MAX, s / (s + ARMOR_PENETRATION_K));
        return Math.max(0, (int) Math.floor(raw * (1.0d - p)));
    }

    private static int physicalDamageFromHit(int offense, BattleUnit defender, BattleUnit attacker) {
        int def = effectiveDefenseVsAttacker(defender, attacker);
        return Math.max(1, offense - def);
    }

    private static int resolveMagnitude(BattleUnit self, BattleUnit enemy, BattleSkill skill, String fallbackScale) {
        if (self == null) {
            return 1;
        }
        if (skill == null) {
            double fallback = evaluateScaleExpression(fallbackScale, self, enemy);
            return Math.max(1, (int) Math.round(fallback));
        }

        String scaleExpr = skill.getEffectStatScale();
        if (scaleExpr == null || scaleExpr.isBlank()) {
            scaleExpr = fallbackScale;
        }

        double scaled = evaluateScaleExpression(scaleExpr, self, enemy);
        double total = skill.getEffectBaseStat() + scaled;
        return Math.max(1, (int) Math.round(total));
    }

    private boolean tryResolveDartParry(
            BattleUnit player,
            BattleUnit enemy,
            String enemyName,
            String enemySkillName,
            int incomingOffense,
            int incomingDamage
    ) {
        if (!isDartWithParryActive(player) || enemy == null) {
            return false;
        }

        double successChance = dartParrySuccessChance(player);
        boolean parrySucceeded = ThreadLocalRandom.current().nextDouble() < successChance;

        if (!parrySucceeded) {
            int chipBase = Math.round(incomingOffense * 0.25f);
            int chipDefense = effectiveDefenseVsAttacker(player, enemy);
            int chipDamage = Math.max(1, chipBase - chipDefense);
            player.setHp(Math.max(0, player.getHp() - chipDamage));
            String parrySkillName = player.getActiveParrySkillName();
            if (parrySkillName != null && !parrySkillName.isBlank()) {
                player.setSkillCooldown(parrySkillName, 1);
            }
            LogBuilder lb = new LogBuilder()
                    .appendName("Dart")
                    .appendPlain("'s parry slips against ")
                    .appendName(enemyName)
                    .appendPlain("'s ")
                    .appendName(enemySkillName)
                    .appendPlain(" and he takes ")
                    .appendDamage(chipDamage)
                    .appendPlain(" chip damage.");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Parry failed -> chipDamage=" + chipDamage + ", successChance=" + successChance);
        } else {
            int reflectedDamage = Math.max(1, incomingDamage + player.getDefense());
            enemy.setHp(Math.max(0, enemy.getHp() - reflectedDamage));
            LogBuilder lb = new LogBuilder()
                    .appendName("Dart")
                    .appendPlain("'s parry deflects ")
                    .appendName(enemyName)
                    .appendPlain("'s ")
                    .appendName(enemySkillName)
                    .appendPlain(" for ")
                    .appendDamage(reflectedDamage)
                    .appendPlain(" damage!");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Parry succeeded -> reflectedDamage=" + reflectedDamage + ", successChance=" + successChance);
        }

        player.consumeParryTurn();
        return true;
    }

    private static boolean isDartWithParryActive(BattleUnit player) {
        if (player == null || !player.hasActiveParry()) {
            return false;
        }
        String id = player.getId();
        return id != null && id.toLowerCase().contains("dart");
    }

    private static double dartParrySuccessChance(BattleUnit player) {
        int speed = player == null ? 0 : player.getSpeed();
        // Requested rule: 50% base + (speed / 10)% bonus.
        double successChance = 0.50d + (speed / 1000.0d);
        return Math.max(0.05d, Math.min(0.95d, successChance));
    }

    private static String describeSkill(BattleSkill skill) {
        if (skill == null) {
            return "<null>";
        }
        return "name=" + skill.getName()
                + ", type=" + skill.getEffectType()
                + ", base=" + skill.getEffectBaseStat()
                + ", scale=" + skill.getEffectStatScale()
                + ", duration=" + skill.getEffectDurationTurns()
                + ", powerCompat=" + skill.getPower();
    }

    private static double evaluateScaleExpression(String expression, BattleUnit self, BattleUnit enemy) {
        if (expression == null || expression.isBlank()) {
            return 0d;
        }

        try {
            return new SkillExpressionParser(expression, self, enemy).parse();
        } catch (RuntimeException ex) {
            Gdx.app.error(TAG, "Failed to parse skill stat scale expression: '" + expression + "'", ex);
            return 0d;
        }
    }

    private static final class SkillExpressionParser {
        private final String src;
        private final BattleUnit self;
        private final BattleUnit enemy;
        private int index;

        SkillExpressionParser(String src, BattleUnit self, BattleUnit enemy) {
            this.src = src;
            this.self = self;
            this.enemy = enemy;
            this.index = 0;
        }

        double parse() {
            double value = parseExpression();
            skipWhitespace();
            if (index < src.length()) {
                throw new IllegalArgumentException("Unexpected token at position " + index);
            }
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseFactor();
                } else if (match('/')) {
                    double divisor = parseFactor();
                    if (Math.abs(divisor) < 1e-6d) {
                        value = 0d;
                    } else {
                        value /= divisor;
                    }
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }

            char c = peek();
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (Character.isLetter(c)) {
                return resolveIdentifier(parseIdentifier());
            }
            throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + index);
        }

        private double parseNumber() {
            int start = index;
            while (index < src.length()) {
                char ch = src.charAt(index);
                if (Character.isDigit(ch) || ch == '.') {
                    index++;
                } else {
                    break;
                }
            }
            return Double.parseDouble(src.substring(start, index));
        }

        private String parseIdentifier() {
            int start = index;
            while (index < src.length()) {
                char ch = src.charAt(index);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '[' || ch == ']') {
                    index++;
                } else {
                    break;
                }
            }
            return src.substring(start, index);
        }

        private double resolveIdentifier(String token) {
            String normalized = token.trim().toLowerCase();
            if (normalized.isBlank()) {
                return 0d;
            }

            String statName = normalized;
            String side = "self";
            int bracketOpen = normalized.indexOf('[');
            int bracketClose = normalized.indexOf(']');
            if (bracketOpen >= 0 && bracketClose > bracketOpen) {
                statName = normalized.substring(0, bracketOpen);
                side = normalized.substring(bracketOpen + 1, bracketClose);
            }

            BattleUnit actor = "enemy".equals(side) ? enemy : self;
            if (actor == null) {
                return 0d;
            }

            return switch (statName) {
                case "attack", "atk" -> actor.getAttack();
                case "defense", "def" -> actor.getDefense();
                case "speed", "spd" -> actor.getSpeed();
                case "maxhp", "max_hp", "hp" -> actor.getMaxHp();
                default -> 0d;
            };
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!match(expected)) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + index);
            }
        }

        private char peek() {
            if (index >= src.length()) {
                return '\0';
            }
            return src.charAt(index);
        }

        private boolean match(char expected) {
            if (index < src.length() && src.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < src.length() && Character.isWhitespace(src.charAt(index))) {
                index++;
            }
        }
    }

    /**
     * Handle an ITEM action by applying the item effect to a target clawkin.
     * Shows item usage message and consumes the player's turn.
     * Assumes the PlayerBattleState contains the item references and target clawkin.
     *
     * @param action the item action with itemId and targetClawkinIndex
     */
    private void handleItemAction(BattleAction action) {
        if (!action.isItemAction() || context == null) {
            setLastLogPlain("Invalid item action.");
            return;
        }

        BattleUnit player = firstAlly();
        if (player == null) {
            setLastLogPlain("No active Clawkin to use item.");
            return;
        }

        String playerName = allyLogName(player);
        String itemId = action.getItemId();
        
        // Format item name for display (capitalize and remove underscores)
        String itemName = formatItemName(itemId);
        
        // Build item usage message
        LogBuilder lb = new LogBuilder()
                .appendName(playerName)
                .appendPlain(" used ")
                .appendName(itemName)
                .appendPlain("!");
        
        setLastLog(lb.text(), lb.spans());
        Gdx.app.log(TAG, "Item used -> player=" + playerName + ", item=" + itemName);
        
        // Note: Actual item effects (healing, stat boosts, etc.) are applied by the caller
        // (BattleOverlay/BattleHud) before this method is called.
        // This method only handles the battle message display.
    }
    
    /**
     * Formats an item ID into a display name.
     * Example: "potion_small" -> "Potion Small"
     * 
     * @param itemId The item ID
     * @return Formatted display name
     */
    private String formatItemName(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "Item";
        }
        
        // Replace underscores with spaces and capitalize each word
        String[] words = itemId.split("_");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                formatted.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }
            }
        }
        return formatted.toString();
    }
}
