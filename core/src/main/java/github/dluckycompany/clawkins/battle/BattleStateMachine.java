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
    private BattlePhase phase = BattlePhase.INIT;
    private BattleContext context;
    private String lastLog = "";
    private List<BattleTextSpan> lastLogSpans = List.of();

    public void begin(BattleContext context) {
        this.context = context;
        this.phase = BattlePhase.PLAYER_COMMAND;
        setLastLogPlain("Encounter started.");
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
        if (action.getType() == BattleActionType.ESCAPE) {
            finishAsEscape();
            setLastLogPlain("You escaped.");
            return;
        }
        if (action.getType() == BattleActionType.ITEM) {
            handleItemAction(action);
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

        if (skill != null && skill.getEffectType() == BattleSkill.EffectType.HEAL) {
            int heal = resolveMagnitude(player, enemy, skill, "defense[self]");
            player.setHp(Math.min(player.getMaxHp(), player.getHp() + heal));
            LogBuilder lb = new LogBuilder()
                    .appendName(playerName)
                    .appendPlain(" uses ")
                    .appendName(skillName)
                    .appendPlain(" and recovered ")
                    .appendHeal(heal)
                    .appendPlain(".");
            setLastLog(lb.text(), lb.spans());
            Gdx.app.log(TAG, "Player heal applied -> amount=" + heal + ", hpNow=" + player.getHp() + "/" + player.getMaxHp());
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
                int damage = Math.max(1, offense - enemy.getDefense());
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
                Gdx.app.log(TAG, "Player attack-as-damage applied -> offense=" + offense + ", enemyDefense=" + enemy.getDefense() + ", damage=" + damage);
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
        } else {
            int offense = resolveMagnitude(player, enemy, skill, "attack[self]");
            int damage = Math.max(1, offense - enemy.getDefense());
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
            Gdx.app.log(TAG, "Player damage applied -> offense=" + offense + ", enemyDefense=" + enemy.getDefense() + ", damage=" + damage);
        }

        enemy.tickTemporaryBoosts();

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

            BattleSkill enemySkill = randomEnemySkill();
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
                    int damage = Math.max(1, offense - player.getDefense());
                    player.setHp(Math.max(0, player.getHp() - damage));
                    LogBuilder lb = new LogBuilder()
                            .appendName(enemyName)
                            .appendPlain(" uses ")
                            .appendName(enemySkillName)
                            .appendPlain(" and deals ")
                            .appendDamage(damage)
                            .appendPlain(" to you.");
                    setLastLog(lb.text(), lb.spans());
                    Gdx.app.log(TAG, "Enemy attack-as-damage applied -> offense=" + offense + ", playerDefense=" + player.getDefense() + ", damage=" + damage);
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
                int damage = Math.max(1, offense - player.getDefense());
                player.setHp(Math.max(0, player.getHp() - damage));
                LogBuilder lb = new LogBuilder()
                        .appendName(enemyName)
                        .appendPlain(" uses ")
                        .appendName(enemySkillName)
                        .appendPlain(" and deals ")
                        .appendDamage(damage)
                        .appendPlain(" to you.");
                setLastLog(lb.text(), lb.spans());
                Gdx.app.log(TAG, "Enemy damage applied -> offense=" + offense + ", playerDefense=" + player.getDefense() + ", damage=" + damage);
            }

            player.tickTemporaryBoosts();
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

    public void reset() {
        phase = BattlePhase.INIT;
        context = null;
        lastLog = "";
        lastLogSpans = List.of();
    }

    public String getLastLog() {
        return lastLog;
    }

    public List<BattleTextSpan> getLastLogSpans() {
        return List.copyOf(lastLogSpans);
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
        int idx = Math.max(1, Math.min(3, slot)) - 1;
        if (idx >= context.getPlayerSkills().size()) {
            return context.getPlayerSkills().getFirst();
        }
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
     * Assumes the PlayerBattleState contains the item references and target clawkin.
     *
     * @param action the item action with itemId and targetClawkinIndex
     */
    private void handleItemAction(BattleAction action) {
        if (!action.isItemAction() || context == null) {
            setLastLogPlain("Invalid item action.");
            return;
        }

        // This method is called from BattleHud when an item is used.
        // The actual item removal from inventory should be done by the caller (BattleHud/UI).
        // Here we just log success.
        setLastLogPlain("Used item on party member " + action.getTargetClawkinIndex() + ".");
    }
}
