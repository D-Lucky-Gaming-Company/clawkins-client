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
 *   <li>Blank line → vertical spacer</li>
 *   <li>{@code ---} → section divider (rendered as spacer)</li>
 *   <li>Table rows ({@code | … |}) → stripped to plain text</li>
 *   <li>Everything else → body text</li>
 * </ul>
 *
 * <p>Inline Markdown ({@code **bold**}, {@code _italic_}, backticks, links)
 * is stripped so only the plain text remains.
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
        List<CreditsLine> lines = new ArrayList<>();

        FileHandle file = Gdx.files.internal(internalPath);
        if (!file.exists()) {
            Gdx.app.error("CreditsParser", "Credits file not found: " + internalPath);
            lines.add(new CreditsLine(LineType.HEADING_1, "CLAWKIN"));
            lines.add(new CreditsLine(LineType.SPACER, ""));
            lines.add(new CreditsLine(LineType.BODY, "Credits file missing."));
            return lines;
        }

        String[] rawLines = file.readString("UTF-8").split("\r?\n", -1);

        for (String raw : rawLines) {
            String trimmed = raw.trim();

            // Blank line → spacer
            if (trimmed.isEmpty()) {
                lines.add(new CreditsLine(LineType.SPACER, ""));
                continue;
            }

            // Horizontal rule → spacer
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                lines.add(new CreditsLine(LineType.SPACER, ""));
                continue;
            }

            // Headings
            if (trimmed.startsWith("### ")) {
                lines.add(new CreditsLine(LineType.HEADING_3, stripInline(trimmed.substring(4))));
                continue;
            }
            if (trimmed.startsWith("## ")) {
                lines.add(new CreditsLine(LineType.HEADING_2, stripInline(trimmed.substring(3))));
                continue;
            }
            if (trimmed.startsWith("# ")) {
                lines.add(new CreditsLine(LineType.HEADING_1, stripInline(trimmed.substring(2))));
                continue;
            }

            // Bullet list
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                lines.add(new CreditsLine(LineType.BULLET, stripInline(trimmed.substring(2))));
                continue;
            }

            // Table header separator (|---|---|) → skip
            if (trimmed.startsWith("|") && trimmed.replace("-", "").replace("|", "").replace(" ", "").isEmpty()) {
                continue;
            }

            // Table row → extract cell text
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                String row = parseTableRow(trimmed);
                if (!row.isEmpty()) {
                    lines.add(new CreditsLine(LineType.BODY, row));
                }
                continue;
            }

            // Front-matter / metadata lines (e.g. "inclusion: manual") → skip
            if (trimmed.contains(":") && !trimmed.contains(" ")) {
                continue;
            }

            // Everything else → body
            String body = stripInline(trimmed);
            if (!body.isEmpty()) {
                lines.add(new CreditsLine(LineType.BODY, body));
            }
        }

        return lines;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Strip common inline Markdown: bold, italic, code, links, images.
     */
    private static String stripInline(String text) {
        if (text == null) return "";

        // Links: [text](url) → text
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
        // Images: ![alt](url) → alt
        text = text.replaceAll("!\\[([^\\]]*?)\\]\\([^)]*\\)", "$1");
        // Bold+italic: ***text*** or ___text___
        text = text.replaceAll("\\*{3}(.+?)\\*{3}", "$1");
        text = text.replaceAll("_{3}(.+?)_{3}", "$1");
        // Bold: **text** or __text__
        text = text.replaceAll("\\*{2}(.+?)\\*{2}", "$1");
        text = text.replaceAll("_{2}(.+?)_{2}", "$1");
        // Italic: *text* or _text_
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        text = text.replaceAll("_(.+?)_", "$1");
        // Inline code: `text`
        text = text.replaceAll("`([^`]+)`", "$1");
        // HTML entities
        text = text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                   .replace("&nbsp;", " ").replace("&#39;", "'").replace("&quot;", "\"");

        return text.trim();
    }

    /**
     * Parse a Markdown table row into a readable string.
     * {@code | Asset | Author | Notes |} → {@code Asset  |  Author  |  Notes}
     */
    private static String parseTableRow(String row) {
        // Remove leading/trailing pipes
        String inner = row.substring(1, row.length() - 1);
        String[] cells = inner.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String cell : cells) {
            String stripped = stripInline(cell.trim());
            if (stripped.isEmpty() || stripped.replace("-", "").replace(" ", "").isEmpty()) {
                continue; // skip separator cells
            }
            if (sb.length() > 0) sb.append("  |  ");
            sb.append(stripped);
        }
        return sb.toString();
    }
}
