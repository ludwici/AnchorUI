package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreParser;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class AnchoredTextWidget extends AnchoredWidget {
    private final Font font;
    private final Screen screen;
    private final Component message;
    private final List<FormattedCharSequence> lines;
    private final boolean multiline;
    private final int lineHeight;

    public AnchoredTextWidget(String text, Font font, LoreStyle.Element style) {
        this(LoreParser.parseComponent(text), font, style, null);
    }

    public AnchoredTextWidget(String text, Font font, LoreStyle.Element style, Screen screen) {
        this(LoreParser.parseComponent(text), font, style, screen);
    }

    public AnchoredTextWidget(Component message, Font font, LoreStyle.Element style) {
        this(message, font, style, null);
    }

    public AnchoredTextWidget(Component message, Font font, LoreStyle.Element style, Screen screen) {
        super(measureWidth(message, font, style), measureHeight(message, font, style), style);
        this.font = font;
        this.screen = screen;
        this.message = message;
        this.multiline = isMultiline(message, style);
        this.lineHeight = font.lineHeight + 2;
        setMessage(message);
        active = false;
        if (multiline) {
            this.lines = font.split(message, Math.max(1, getWidth() - style.padding.left - style.padding.right));
        } else {
            this.lines = List.of();
        }
    }

    private static boolean isMultiline(Component message, LoreStyle.Element style) {
        return style.textWrap || message.getString().contains("\n");
    }

    private static int measureWidth(Component message, Font font, LoreStyle.Element style) {
        if (style.size.width > 0) {
            return style.size.width;
        }
        if (style.textWrap) {
            return 160;
        }
        int max = 0;
        for (FormattedCharSequence line : font.split(message, Integer.MAX_VALUE)) {
            max = Math.max(max, font.width(line));
        }
        return max + style.padding.left + style.padding.right;
    }

    private static int measureHeight(Component message, Font font, LoreStyle.Element style) {
        if (style.size.height > 0) {
            return style.size.height;
        }
        if (!isMultiline(message, style)) {
            return font.lineHeight + style.padding.top + style.padding.bottom;
        }
        int contentWidth = Math.max(1, measureWidth(message, font, style) - style.padding.left - style.padding.right);
        int count = Math.max(1, font.split(message, contentWidth).size());
        return style.padding.top + style.padding.bottom + count * (font.lineHeight + 2) - 2;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int innerWidth = Math.max(0, getWidth() - style.padding.left - style.padding.right);
        int innerHeight = Math.max(0, getHeight() - style.padding.top - style.padding.bottom);
        if (innerWidth == 0 || innerHeight == 0) {
            return;
        }
        int x = getX() + style.padding.left;
        int y = getY() + style.padding.top;
        if (multiline) {
            renderLines(graphics, mouseX, mouseY, x, y, innerWidth, innerHeight);
        } else {
            renderSingleLine(graphics, mouseX, mouseY, x, y, innerWidth, innerHeight);
        }
    }

    private void renderLines(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        int contentHeight = lines.isEmpty() ? 0 : lines.size() * lineHeight - 2;
        int startY = y + Math.max(0, (height - contentHeight) / 2);
        Style hovered = null;
        graphics.enableScissor(x, y, x + width, y + height);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int lineWidth = font.width(line);
            int lineX = switch (style.textAlign) {
                case "center" -> x + (width - lineWidth) / 2;
                case "right" -> x + width - lineWidth;
                default -> x;
            };
            int lineY = startY + index * lineHeight;
            graphics.drawString(font, line, lineX, lineY, style.textColor, style.textShadow);
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
                    && mouseY >= lineY && mouseY < lineY + font.lineHeight) {
                hovered = RichTextHover.styleAt(font, line, mouseX - lineX);
            }
        }
        graphics.disableScissor();
        if (hovered != null && hovered.getHoverEvent() != null) {
            renderRichTooltip(graphics, hovered, mouseX, mouseY);
        }
    }

    private void renderSingleLine(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        FormattedCharSequence rendered = message.getVisualOrderText();
        if (font.width(message) > width) {
            int ellipsisWidth = font.width(CommonComponents.ELLIPSIS);
            if (width < ellipsisWidth) {
                return;
            }
            FormattedText clipped = font.substrByWidth(message, width - ellipsisWidth);
            rendered = Language.getInstance().getVisualOrder(FormattedText.composite(clipped, CommonComponents.ELLIPSIS));
        }
        int textWidth = font.width(rendered);
        int textX = switch (style.textAlign) {
            case "center" -> x + (width - textWidth) / 2;
            case "right" -> x + width - textWidth;
            default -> x;
        };
        int textY = y + Math.max(0, (height - font.lineHeight) / 2);
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            graphics.drawString(font, rendered, textX, textY, style.textColor, style.textShadow);
        } finally {
            graphics.disableScissor();
        }
        if (mouseX < textX || mouseX >= textX + textWidth || mouseY < textY || mouseY >= textY + font.lineHeight) {
            return;
        }
        Style hovered = RichTextHover.styleAt(font, rendered, mouseX - textX);
        if (hovered != null && hovered.getHoverEvent() != null) {
            renderRichTooltip(graphics, hovered, mouseX, mouseY);
        }
    }

    private void renderRichTooltip(GuiGraphics graphics, Style hovered, int mouseX, int mouseY) {
        if (screen instanceof ItemLoreScreen ui) {
            ui.queueRichTextTooltip(hovered);
        } else {
            graphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
