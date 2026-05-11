package github.dluckycompany.clawkins.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Parses a Markdown file into a flat list of {@link CreditsLine} entries
 * suitable for display in the ending credits scroller.
 *
 * <p>Supported Markdown constructs:
 * <ul>
 *   <li>{@code # Heading} → large heading</li>
 *   <li>{@code ## Sub-heading} → medium heading</li>
 *   <li>{@code ### Sub-sub-heading} → small heading</li>
 *   <li>{@code - item} or {@code * item} → bullet entry</li>
 *   <li>Blank line / {@code ---} → vertical spacer (deduplicated)</li>
 *   <li>Table rows ({@code | … |}) → cleaned, humanized body lines</li>
 *   <li>Everything else → body text</li>
 * </ul>
 *
 * <p>All inline Markdown, raw URLs, email addresses, and .mp3 filenames
 * are cleaned so only human-readable text appears in the credits.
 */
public class CreditsParser {

    public enum LineType {
        HEADING_1,
        HEADING_2,
        HEADING_3,
        BODY,
        BULLET,
        SPACER
    }

    public static final class CreditsLine {
        public final LineType type;
        public final String text;

        public CreditsLine(LineType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    /**
     * Load and parse the credits file at the given internal path.
     *
     * @param internalPath path relative to the assets root, e.g. {@code "docs/CREDITS.md"}
     * @return ordered list of parsed lines; never null, may be empty on error
     */
    public static List<CreditsLine> parse(String internalPath) {
        List<CreditsLine> rawLines = new ArrayList<>();

        FileHandle file = Gdx.files.internal(internalPath);
        if (!file.exists()) {
            Gdx.app.error("CreditsParser", "Credits file not found: " + internalPath);
            rawLines.add(new CreditsLine(LineType.HEADING_1, "CLAWKIN"));
            rawLines.add(new CreditsLine(LineType.SPACER, ""));
            rawLines.add(new CreditsLine(LineType.BODY, "Credits file missing."));
            return rawLines;
        }

        String[] sourceLines = file.readString("UTF-8").split("\r?\n", -1);

        for (String raw : sourceLines) {
            String trimmed = raw.trim();

            // Blank line → spacer
            if (trimmed.isEmpty()) {
                rawLines.add(new CreditsLine(LineType.SPACER, ""));
                continue;
            }

            // Horizontal rule → spacer
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")
                    || trimmed.matches("-{3,}") || trimmed.matches("\\*{3,}")) {
                rawLines.add(new CreditsLine(LineType.SPACER, ""));
                continue;
            }

            // Blockquote (> text) — strip the > prefix and treat as body
            if (trimmed.startsWith("> ")) {
                String body = stripInline(trimmed.substring(2));
                if (!body.isEmpty()) {
                    rawLines.add(new CreditsLine(LineType.BODY, body));
                }
                continue;
            }

            // Headings
            if (trimmed.startsWith("### ")) {
                rawLines.add(new CreditsLine(LineType.HEADING_3, stripInline(trimmed.substring(4))));
                continue;
            }
            if (trimmed.startsWith("## ")) {
                rawLines.add(new CreditsLine(LineType.HEADING_2, stripInline(trimmed.substring(3))));
                continue;
            }
            if (trimmed.startsWith("# ")) {
                rawLines.add(new CreditsLine(LineType.HEADING_1, stripInline(trimmed.substring(2))));
                continue;
            }

            // Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String bullet = stripInline(trimmed.substring(2));
                if (!bullet.isEmpty()) {
                    rawLines.add(new CreditsLine(LineType.BULLET, bullet));
                }
                continue;
            }

            // Table header separator (|---|---|) → skip entirely
            if (trimmed.startsWith("|") && trimmed.replace("-", "").replace("|", "").replace(" ", "").isEmpty()) {
                continue;
            }

            // Table row → extract and humanize cell text
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                List<String> rows = parseTableRow(trimmed);
                for (String row : rows) {
                    if (!row.isEmpty()) {
                        rawLines.add(new CreditsLine(LineType.BODY, row));
                        Gdx.app.log("CreditsParser", "Table row: " + row);
                    }
                }
                continue;
            }

            // Front-matter / metadata lines (e.g. "inclusion:manual") → skip
            if (trimmed.contains(":") && !trimmed.contains(" ")) {
                continue;
            }

            // Everything else → body
            String body = stripInline(trimmed);
            if (!body.isEmpty()) {
                rawLines.add(new CreditsLine(LineType.BODY, body));
                Gdx.app.log("CreditsParser", "Body: " + body);
            }
        }

        // Deduplicate consecutive spacers so the credits don't have giant gaps
        return deduplicateSpacers(rawLines);
    }

    // -----------------------------------------------------------------------
    // Spacer deduplication
    // -----------------------------------------------------------------------

    /**
     * Collapse runs of consecutive SPACER entries into a single spacer.
     * Also removes leading/trailing spacers from the list.
     */
    private static List<CreditsLine> deduplicateSpacers(List<CreditsLine> input) {
        List<CreditsLine> out = new ArrayList<>();
        boolean lastWasSpacer = true; // treat start as spacer to strip leading spacers
        for (CreditsLine line : input) {
            if (line.type == LineType.SPACER) {
                if (!lastWasSpacer) {
                    out.add(line);
                    lastWasSpacer = true;
                }
            } else {
                out.add(line);
                lastWasSpacer = false;
            }
        }
        // Strip trailing spacer
        if (!out.isEmpty() && out.get(out.size() - 1).type == LineType.SPACER) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Inline stripping
    // -----------------------------------------------------------------------

    /**
     * Strip all inline Markdown from a string, leaving only plain readable text.
     *
     * Order matters:
     * 1. Email addresses in angle brackets
     * 2. Nested bold+link: **[text](url)**
     * 3. Plain links: [text](url)
     * 4. Bare URLs (http/https)
     * 5. Images
     * 6. Bold+italic, bold, italic
     * 7. Inline code (backticks) — also humanizes .mp3 filenames
     * 8. HTML entities
     * 9. Stray punctuation cleanup
     */
    private static String stripInline(String text) {
        if (text == null) return "";

        // 1. Email addresses <user@domain> → strip entirely (not useful in credits)
        text = text.replaceAll("<[^>]+@[^>]+>", "");

        // 2. Bold+italic links: ***[text](url)*** → text
        text = text.replaceAll("\\*{3}\\[([^\\]]+)\\]\\([^)]*\\)\\*{3}", "$1");
        text = text.replaceAll("_{3}\\[([^\\]]+)\\]\\([^)]*\\)_{3}", "$1");

        // 3. Bold links: **[text](url)** → text
        text = text.replaceAll("\\*{2}\\[([^\\]]+)\\]\\([^)]*\\)\\*{2}", "$1");
        text = text.replaceAll("_{2}\\[([^\\]]+)\\]\\([^)]*\\)_{2}", "$1");

        // 4. Italic links: *[text](url)* or _[text](url)_ → text
        text = text.replaceAll("\\*\\[([^\\]]+)\\]\\([^)]*\\)\\*", "$1");
        text = text.replaceAll("_\\[([^\\]]+)\\]\\([^)]*\\)_", "$1");

        // 5. Plain links: [text](url) → text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");

        // 6. Bare URLs (http/https) → strip entirely
        text = text.replaceAll("https?://\\S+", "");

        // 7. Images: ![alt](url) → alt
        text = text.replaceAll("!\\[([^\\]]*?)\\]\\([^)]*\\)", "$1");

        // 8. Bold+italic: ***text*** or ___text___
        text = text.replaceAll("\\*{3}(.+?)\\*{3}", "$1");
        text = text.replaceAll("_{3}(.+?)_{3}", "$1");

        // 9. Bold: **text** or __text__
        text = text.replaceAll("\\*{2}(.+?)\\*{2}", "$1");
        text = text.replaceAll("_{2}(.+?)_{2}", "$1");

        // 10. Italic: *text* or _text_  (single char, non-greedy)
        text = text.replaceAll("\\*([^*]+?)\\*", "$1");
        text = text.replaceAll("_([^_]+?)_", "$1");

        // 11. Inline code: `filename.mp3` → humanize the filename
        java.util.regex.Matcher codeMatcher = java.util.regex.Pattern.compile("`([^`]+)`").matcher(text);
        StringBuffer codeResult = new StringBuffer();
        while (codeMatcher.find()) {
            codeMatcher.appendReplacement(codeResult,
                java.util.regex.Matcher.quoteReplacement(humanizeFilename(codeMatcher.group(1))));
        }
        codeMatcher.appendTail(codeResult);
        text = codeResult.toString();

        // 12. HTML entities
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&nbsp;", " ")
                   .replace("&#39;", "'")
                   .replace("&quot;", "\"");

        // 13. Em-dash placeholder cleanup — lone em-dash means "no source", skip it
        text = text.replaceAll("^—$", "").replaceAll("^\\s*—\\s*$", "");

        // 14. Stray angle brackets (leftover from email stripping)
        text = text.replaceAll("[<>]", "");

        // 15. Collapse multiple spaces/tabs into one
        text = text.replaceAll("[ \\t]{2,}", " ");

        return text.trim();
    }

    // -----------------------------------------------------------------------
    // Table parsing
    // -----------------------------------------------------------------------

    /**
     * Parse a Markdown table row into one or more display strings.
     *
     * Rules:
     * - Single non-empty cell → return as-is (e.g. SFX filename lists)
     * - Two cells → "Cell1 — Cell2"
     * - Three+ cells → "Cell1 — Cell2" (first two meaningful cells only;
     *   third column is usually "Notes" or a URL source — include if meaningful)
     * - Cells that are just "—" (em-dash placeholder) are treated as empty
     * - Rows where ALL cells are empty after cleaning → skip
     */
    private static List<String> parseTableRow(String row) {
        List<String> results = new ArrayList<>();

        // Remove leading/trailing pipes and split
        String inner = row.substring(1, row.length() - 1);
        String[] rawCells = inner.split("\\|");

        // Clean each cell
        List<String> cells = new ArrayList<>();
        for (String cell : rawCells) {
            String cleaned = stripInline(cell.trim());
            // Treat lone em-dash or empty as absent
            if (cleaned.isEmpty() || cleaned.equals("—") || cleaned.equals("-")) {
                cells.add("");
            } else {
                cells.add(cleaned);
            }
        }

        // Collect non-empty cells
        List<String> meaningful = new ArrayList<>();
        for (String c : cells) {
            if (!c.isEmpty()) meaningful.add(c);
        }

        if (meaningful.isEmpty()) {
            return results; // nothing to show
        }

        if (meaningful.size() == 1) {
            // Single-column table (e.g. SFX filename list) — humanize and show
            String display = humanizeFilename(meaningful.get(0));
            if (!display.isEmpty()) {
                results.add(display);
            }
        } else {
            // Multi-column: join first two meaningful cells with em-dash
            String col1 = humanizeFilename(meaningful.get(0));
            String col2 = meaningful.get(1);
            if (col1.isEmpty()) {
                results.add(col2);
            } else if (col2.isEmpty()) {
                results.add(col1);
            } else {
                results.add(col1 + " - " + col2);
            }
            // If there's a third meaningful cell (e.g. Notes), add it as a sub-line
            if (meaningful.size() >= 3) {
                String col3 = meaningful.get(2);
                if (!col3.isEmpty() && !col3.equals("—")) {
                    results.add("  " + col3);
                }
            }
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Filename humanization
    // -----------------------------------------------------------------------

    /**
     * Convert a raw filename like {@code exploration.mp3} or
     * {@code BOSS/boss_cerberus.mp3} into a readable display name like
     * {@code Exploration Theme} or {@code Boss: Cerberus}.
     *
     * If the input doesn't look like a filename, returns it unchanged.
     */
    static String humanizeFilename(String input) {
        if (input == null || input.isEmpty()) return "";

        // Only process if it looks like a filename (contains a dot with extension)
        if (!input.matches(".*\\.[a-zA-Z0-9]{2,5}$")) {
            return input; // not a filename, return as-is
        }

        // Strip directory prefix (e.g. BOSS/, mapTransition/, SFX_Others/)
        String name = input;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        // Strip extension
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            name = name.substring(0, dotIdx);
        }

        // Replace underscores and hyphens with spaces
        name = name.replace('_', ' ').replace('-', ' ');

        // Title-case each word
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
        }

        return sb.toString();
    }
}
