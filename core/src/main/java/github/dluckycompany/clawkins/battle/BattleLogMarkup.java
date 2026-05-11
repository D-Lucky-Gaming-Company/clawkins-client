package github.dluckycompany.clawkins.battle;

/**
 * Converts plain battle log + spans to LibGDX BitmapFont markup and truncates by <em>plain</em> character
 * count so typewriter progress matches visible glyphs (tags do not consume plain length).
 */
public final class BattleLogMarkup {

    /** Saturated yellow — victory milestone headline. */
    private static final String MARKUP_MILESTONE = "[#ffff00ff]";
    /** Yellow — names / skill titles. */
    private static final String MARKUP_NAME = "[#ffeb3bff]";
    /** Light red — damage numbers. */
    private static final String MARKUP_DAMAGE = "[#ff8a80ff]";
    /** Green — heal numbers. */
    private static final String MARKUP_HEAL = "[#69f0aeff]";
    /** Blue — "defense UP" phrase. */
    private static final String MARKUP_DEFENSE_UP = "[#82b1ffff]";

    private static final String POP = "[]";

    private BattleLogMarkup() {
    }

    public static String openTag(BattleTextRole role) {
        return switch (role) {
            case MILESTONE -> MARKUP_MILESTONE;
            case NAME -> MARKUP_NAME;
            case DAMAGE -> MARKUP_DAMAGE;
            case HEAL -> MARKUP_HEAL;
            case DEFENSE_UP -> MARKUP_DEFENSE_UP;
        };
    }

    /** LibGDX markup uses {@code [[} for a literal bracket. */
    public static String escapeBrackets(String s) {
        return s.replace("[", "[[");
    }

    /**
     * Builds a full markup string for {@code plain}, applying {@code spans} (sorted by start).
     */
    public static String toMarkup(String plain, java.util.List<BattleTextSpan> spans) {
        if (plain == null || plain.isEmpty()) {
            return "";
        }
        if (spans == null || spans.isEmpty()) {
            return escapeBrackets(plain);
        }
        java.util.ArrayList<BattleTextSpan> sorted = new java.util.ArrayList<>(spans);
        sorted.sort(java.util.Comparator.comparingInt(BattleTextSpan::start));

        StringBuilder sb = new StringBuilder(plain.length() + sorted.size() * 16);
        int cursor = 0;
        int n = plain.length();
        for (BattleTextSpan sp : sorted) {
            int s = Math.min(Math.max(0, sp.start()), n);
            int e = Math.min(Math.max(s, sp.end()), n);
            if (s > cursor) {
                sb.append(escapeBrackets(plain.substring(cursor, s)));
            }
            if (e > s) {
                sb.append(openTag(sp.role()));
                sb.append(escapeBrackets(plain.substring(s, e)));
                sb.append(POP);
            }
            cursor = Math.max(cursor, e);
        }
        if (cursor < n) {
            sb.append(escapeBrackets(plain.substring(cursor)));
        }
        return sb.toString();
    }

    /**
     * Returns a markup prefix whose decoded <em>plain</em> text length is {@code maxPlainChars}
     * (excluding markup tags). Closes any open color with {@code []}. Safe for {@link com.badlogic.gdx.graphics.g2d.GlyphLayout}.
     */
    public static String truncateMarkupToPlainLength(String markup, int maxPlainChars) {
        if (maxPlainChars <= 0 || markup == null || markup.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.min(markup.length(), maxPlainChars + 32));
        int plain = 0;
        int i = 0;
        while (i < markup.length() && plain < maxPlainChars) {
            char c = markup.charAt(i);
            if (c == '[') {
                if (i + 1 < markup.length() && markup.charAt(i + 1) == '[') {
                    out.append("[[");
                    i += 2;
                    plain++;
                    continue;
                }
                int close = markup.indexOf(']', i);
                if (close < 0) {
                    break;
                }
                out.append(markup, i, close + 1);
                i = close + 1;
                continue;
            }
            out.append(c);
            plain++;
            i++;
        }
        out.append(POP);
        return out.toString();
    }
}
