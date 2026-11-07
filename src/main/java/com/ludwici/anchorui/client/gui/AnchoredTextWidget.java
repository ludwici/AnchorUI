package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreParser;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
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
    private final Component message;
    private final StringWidget singleLine;
    private final List<FormattedCharSequence> lines;
    private final boolean multiline;
    private final int lineHeight;

    public AnchoredTextWidget(String text, Font font, LoreStyle.Element style) {
        this(LoreParser.parseComponent(text), font, style);
    }

    public AnchoredTextWidget(Component message, Font font, LoreStyle.Element style) {
        super(measureWidth(message, font, style), measureHeight(message, font, style), style);
        this.font = font;
        this.message = message;
        this.multiline = isMultiline(message, style);
        this.lineHeight = font.lineHeight + 2;
        setMessage(message);
        active = false;
        if (multiline) {
            this.lines = font.split(message, Math.max(1, getWidth() - style.padding.left - style.padding.right));
            this.singleLine = null;
        } else {
            this.lines = List.of();
            this.singleLine = new StringWidget(Math.max(0, getWidth() - style.padding.left - style.padding.right),
                    Math.max(0, getHeight() - style.padding.top - style.padding.bottom), message, font).setColor(style.textColor);
            switch (style.textAlign) {
                case "center" -> singleLine.alignCenter();
                case "right" -> singleLine.alignRight();
                default -> singleLine.alignLeft();
            }
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
            singleLine.setWidth(innerWidth);
            singleLine.setHeight(innerHeight);
            singleLine.setPosition(x, y);
            singleLine.render(graphics, mouseX, mouseY, partialTick);
            renderSingleLineHover(graphics, mouseX, mouseY, x, y, innerWidth, innerHeight);
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
            graphics.drawString(font, line, lineX, lineY, style.textColor);
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
                    && mouseY >= lineY && mouseY < lineY + font.lineHeight) {
                hovered = RichTextHover.styleAt(font, line, mouseX - lineX);
            }
        }
        graphics.disableScissor();
        if (hovered != null && hovered.getHoverEvent() != null) {
            graphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
        }
    }

    private void renderSingleLineHover(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        int textWidth = font.width(message);
        int textX = x + Math.round(switch (style.textAlign) {
            case "center" -> 0.5F;
            case "right" -> 1.0F;
            default -> 0.0F;
        } * (width - textWidth));
        int textY = y + (height - 9) / 2;
        if (mouseX < x || mouseX >= x + width || mouseY < textY || mouseY >= textY + font.lineHeight) {
            return;
        }
        FormattedCharSequence rendered = message.getVisualOrderText();
        if (textWidth > width) {
            FormattedText clipped = font.substrByWidth(message, width - font.width(CommonComponents.ELLIPSIS));
            rendered = Language.getInstance().getVisualOrder(FormattedText.composite(clipped, CommonComponents.ELLIPSIS));
        }
        Style hovered = RichTextHover.styleAt(font, rendered, mouseX - textX);
        if (hovered != null && hovered.getHoverEvent() != null) {
            graphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
