package net.dawson.adorablehamsterpets.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownRenderer {
    // --- 1. Constants and Patterns ---
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.*?)]\\((.*?)\\)");
    public static final int LINE_SPACING = 2;
    public static final int HEADING_BOTTOM_MARGIN = 4;
    public static final int DIVIDER_HEIGHT = 13;
    public static final int LIST_INDENT = 10;
    public static final int SPACES_PER_INDENT_LEVEL = 4;

    // --- 2. Fields ---
    private final TextRenderer textRenderer;
    public final List<String> lines;
    private final int x;
    private final int startY;
    public final int width;
    private int totalHeight = 0;

    // --- 3. Constructor ---
    public MarkdownRenderer(String markdownContent, int x, int startY, int width) {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.lines = markdownContent == null ? List.of() : List.of(markdownContent.split("\n"));
        this.x = x;
        this.startY = startY;
        this.width = width;
        // Pass the content width to the height calculation
        this.calculateHeight(width);
    }

    // --- 4. Public Methods ---
    public void render(DrawContext context, int scrollY, @Nullable Style hoveredStyle) {
        int currentY = startY - scrollY;

        for (String originalLine : lines) {
            String trimmedLine = originalLine.trim();
            if (trimmedLine.isEmpty()) {
                currentY += textRenderer.fontHeight / 2;
                continue;
            }

            if (trimmedLine.startsWith("#")) {
                currentY = renderHeading(context, trimmedLine, currentY);
            } else if (trimmedLine.equals("---")) {
                currentY = renderDivider(context, currentY);
            } else if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ") || (trimmedLine.matches("^\\d+\\.\\s.*"))) {
                currentY = renderListItem(context, originalLine, currentY, hoveredStyle);
            } else {
                currentY = renderParagraph(context, trimmedLine, currentY, x, width, hoveredStyle);
            }
        }
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    // --- 5. Public Static Helpers ---
    public static int getIndentationLevel(String line) {
        int spaces = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                spaces++;
            } else {
                break;
            }
        }
        return spaces / SPACES_PER_INDENT_LEVEL;
    }

    // --- 6. Private Rendering Helpers ---
    private int renderHeading(DrawContext context, String line, int y) {
        int level = 0;
        while (level < line.length() && line.charAt(level) == '#') {
            level++;
        }
        String text = line.substring(level).trim();
        float scale = Math.max(1.0f, 2.0f - (level - 1) * 0.25f);
        int color = 0x323232;

        // Create a styled, bold text object for the heading
        MutableText styledText = Text.literal(text).setStyle(Style.EMPTY.withBold(true));

        // The available width for the text must be scaled down to account for the scaled-up rendering
        int scaledWidth = (int) (this.width / scale);
        List<OrderedText> wrappedLines = this.textRenderer.wrapLines(styledText, scaledWidth);

        MatrixStack matrices = context.getMatrices();
        for (OrderedText wrappedLine : wrappedLines) {
            matrices.push();
            matrices.translate(x, y, 0);
            matrices.scale(scale, scale, 1.0f);
            context.drawText(textRenderer, wrappedLine, 0, 0, color, false);
            matrices.pop();
            y += (int)(textRenderer.fontHeight * scale) + LINE_SPACING;
        }

        // Adjust Y position: remove the last line's spacing and add the final margin
        return y - LINE_SPACING + HEADING_BOTTOM_MARGIN;
    }

    private int renderDivider(DrawContext context, int y) {
        // Draw the divider 3 pixels down from the start, leaving 3px padding above.
        context.fill(x + 15, y + 3, x + width - 15, y + 4, 0xFFB3B3B3); // First two digits control the alpha.
        return y + DIVIDER_HEIGHT;
    }

    private int renderListItem(DrawContext context, String line, int y, @Nullable Style hoveredStyle) {
        int indentationLevel = getIndentationLevel(line);
        String trimmedLine = line.trim();

        String bullet;
        String content;

        if (trimmedLine.matches("^\\d+\\.\\s.*")) {
            int dotIndex = trimmedLine.indexOf('.');
            bullet = trimmedLine.substring(0, dotIndex + 1);
            content = trimmedLine.substring(dotIndex + 1).trim();
        } else {
            bullet = "•";
            content = trimmedLine.substring(1).trim();
        }

        int bulletX = x + (indentationLevel * LIST_INDENT);
        int contentX = bulletX + LIST_INDENT;
        int contentWidth = width - ((indentationLevel + 1) * LIST_INDENT);

        context.drawText(textRenderer, bullet, bulletX, y, 0x323232, false);
        return renderParagraph(context, content, y, contentX, contentWidth, hoveredStyle);
    }

    private int renderParagraph(DrawContext context, String line, int y, int startX, int lineWidth, @Nullable Style hoveredStyle) {
        int indentationLevel = getIndentationLevel(line);
        String content = line.substring(indentationLevel * SPACES_PER_INDENT_LEVEL);
        int finalStartX = startX + (indentationLevel * LIST_INDENT);
        int finalLineWidth = lineWidth - (indentationLevel * LIST_INDENT);

        MutableText styledText = parseLineToText(content, hoveredStyle);
        List<OrderedText> wrappedLines = textRenderer.wrapLines(styledText, finalLineWidth);

        for (OrderedText wrappedLine : wrappedLines) {
            context.drawText(textRenderer, wrappedLine, finalStartX, y, 0x323232, false);
            y += textRenderer.fontHeight + LINE_SPACING;
        }
        return y;
    }

    // --- 7. Parsing Logic ---
    public MutableText parseLineToText(String line, @Nullable Style hoveredStyle) {
        MutableText result = Text.empty();
        String remaining = line;

        while (!remaining.isEmpty()) {
            Matcher boldMatcher = BOLD_PATTERN.matcher(remaining);
            Matcher italicMatcher = ITALIC_PATTERN.matcher(remaining);
            Matcher codeMatcher = CODE_PATTERN.matcher(remaining);
            Matcher linkMatcher = LINK_PATTERN.matcher(remaining);

            int nextMatchPos = Integer.MAX_VALUE;
            Matcher nextMatcher = null;

            if (boldMatcher.find(0) && boldMatcher.start() < nextMatchPos) { nextMatchPos = boldMatcher.start(); nextMatcher = boldMatcher; }
            if (italicMatcher.find(0) && italicMatcher.start() < nextMatchPos) { nextMatchPos = italicMatcher.start(); nextMatcher = italicMatcher; }
            if (codeMatcher.find(0) && codeMatcher.start() < nextMatchPos) { nextMatchPos = codeMatcher.start(); nextMatcher = codeMatcher; }
            if (linkMatcher.find(0) && linkMatcher.start() < nextMatchPos) { nextMatchPos = linkMatcher.start(); nextMatcher = linkMatcher; }

            if (nextMatcher != null) {
                if (nextMatchPos > 0) {
                    result.append(Text.literal(remaining.substring(0, nextMatchPos)));
                }

                if (nextMatcher == boldMatcher) {
                    result.append(Text.literal(boldMatcher.group(1)).setStyle(Style.EMPTY.withBold(true)));
                } else if (nextMatcher == italicMatcher) {
                    result.append(Text.literal(italicMatcher.group(1)).setStyle(Style.EMPTY.withItalic(true)));
                } else if (nextMatcher == codeMatcher) {
                    result.append(Text.literal(codeMatcher.group(1)).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "uniform")).withColor(Formatting.BLACK)));
                } else if (nextMatcher == linkMatcher) {
                    String linkText = linkMatcher.group(1);
                    String url = linkMatcher.group(2);
                    ClickEvent clickEvent;

                    if (url.startsWith("ahp://copy ")) {
                        String command = url.substring("ahp://copy ".length());
                        clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, command);
                    } else {
                        // Default to OPEN_URL for https or any other scheme
                        clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
                    }

                    // Hover logic
                    Style linkStyle = Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true).withBold(true).withClickEvent(clickEvent);
                    if (hoveredStyle != null && hoveredStyle.getClickEvent() != null && hoveredStyle.getClickEvent().equals(clickEvent)) {
                        // Hover style: Gold, with no underline.
                        linkStyle = linkStyle.withColor(Formatting.GOLD).withUnderline(false).withBold(true);
                    }
                    result.append(Text.literal(linkText).setStyle(linkStyle));
                }
                remaining = remaining.substring(nextMatcher.end());
            } else {
                result.append(Text.literal(remaining));
                break;
            }
        }
        return result;
    }

    private void calculateHeight(int lineWidth) {
        int currentY = 0;
        for (String line : lines) {
            String trimmedLine = line.trim(); // Trim first to identify element type
            if (trimmedLine.isEmpty()) {
                currentY += textRenderer.fontHeight / 2;
                continue;
            }

            if (trimmedLine.startsWith("#")) {
                int level = 0;
                while (level < trimmedLine.length() && trimmedLine.charAt(level) == '#') level++;
                String text = trimmedLine.substring(level).trim();
                float scale = Math.max(1.0f, 2.0f - (level - 1) * 0.25f);

                int scaledWidth = (int) (lineWidth / scale);
                MutableText styledText = Text.literal(text).setStyle(Style.EMPTY.withBold(true));
                int wrappedLinesCount = this.textRenderer.wrapLines(styledText, scaledWidth).size();

                int heightOfLines = wrappedLinesCount * (int)(textRenderer.fontHeight * scale);
                int totalSpacing = Math.max(0, wrappedLinesCount - 1) * LINE_SPACING;
                currentY += heightOfLines + totalSpacing + HEADING_BOTTOM_MARGIN;
            } else if (trimmedLine.equals("---")) {
                currentY += DIVIDER_HEIGHT;
            } else {
                int indentationLevel = getIndentationLevel(line); // Use original line for indent
                String content = trimmedLine;
                int contentWidth;

                // Check if it's a list item and adjust content/width accordingly.
                if (content.startsWith("- ") || content.startsWith("* ") || content.matches("^\\d+\\.\\s.*")) {
                    contentWidth = lineWidth - ((indentationLevel + 1) * LIST_INDENT);
                    if (content.matches("^\\d+\\.\\s.*")) {
                        content = content.substring(content.indexOf('.') + 1).trim();
                    } else {
                        // Handles both "- " and "* "
                        content = content.substring(1).trim();
                    }
                } else {
                    // It's a paragraph, adjust width for its indentation level.
                    contentWidth = lineWidth - (indentationLevel * LIST_INDENT);
                }

                MutableText styledText = parseLineToText(content, null);
                int wrappedLinesCount = textRenderer.wrapLines(styledText, contentWidth).size();
                currentY += wrappedLinesCount * (textRenderer.fontHeight + LINE_SPACING);
            }
        }
        this.totalHeight = currentY;
    }
}