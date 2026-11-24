package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreParser;
import com.ludwici.anchorui.api.binding.BoundText;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class BoundTextWidget extends AnchoredWidget {
    private final WidgetContext context;
    private final BoundText binding;
    private final Font font;
    private List<FormattedCharSequence> lines = List.of();
    private String lastText;
    private int lastWidth = -1;

    public BoundTextWidget(WidgetContext context, BoundText binding, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 160, style.size.height > 0 ? style.size.height : (style.textWrap ? 38 : 16), style);
        this.context = context;
        this.binding = binding;
        this.font = context.font();
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int innerWidth = Math.max(0, getWidth() - style.padding.left - style.padding.right);
        int innerHeight = Math.max(0, getHeight() - style.padding.top - style.padding.bottom);
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        String text = binding.get(context);
        if (!text.equals(lastText) || innerWidth != lastWidth) {
            Component component = LoreParser.parseComponent(text);
            lines = font.split(component, style.textWrap || text.contains("\n") ? innerWidth : Integer.MAX_VALUE);
            lastText = text;
            lastWidth = innerWidth;
            setMessage(component);
        }

        int x = getX() + style.padding.left;
        int y = getY() + style.padding.top;
        int lineHeight = font.lineHeight + 2;
        int contentHeight = lines.isEmpty() ? 0 : lines.size() * lineHeight - 2;
        int startY = y + Math.max(0, (innerHeight - contentHeight) / 2);
        TextClip.enable(graphics, x, y, x + innerWidth, y + innerHeight);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            int lineX = switch (style.textAlign) {
                case "center" -> x + (innerWidth - font.width(line)) / 2;
                case "right" -> x + innerWidth - font.width(line);
                default -> x;
            };
            int lineY = startY + index * lineHeight;
            if (lineY + font.lineHeight <= y || lineY >= y + innerHeight) {
                continue;
            }
            graphics.drawString(font, line, lineX, lineY, style.textColor, style.textShadow);
            if (mouseX >= lineX && mouseX < lineX + font.width(line) && mouseY >= lineY && mouseY < lineY + font.lineHeight) {
                var hovered = RichTextHover.styleAt(font, line, mouseX - lineX);
                if (hovered != null && hovered.getHoverEvent() != null) {
                    if (context.screen() instanceof ItemLoreScreen ui) {
                        ui.queueRichTextTooltip(hovered);
                    } else {
                        graphics.disableScissor();
                        graphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
                        TextClip.enable(graphics, x, y, x + innerWidth, y + innerHeight);
                    }
                }
            }
        }
        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
