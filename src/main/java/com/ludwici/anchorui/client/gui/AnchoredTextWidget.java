package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class AnchoredTextWidget extends AnchoredWidget {
    private final StringWidget content;

    public AnchoredTextWidget(String text, Font font, LoreStyle.Element style) {
        this(Component.literal(text), font, style);
    }

    public AnchoredTextWidget(Component message, Font font, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : font.width(message) + style.padding.left + style.padding.right,
                style.size.height > 0 ? style.size.height : font.lineHeight + style.padding.top + style.padding.bottom, style);
        setMessage(message);
        active = false;
        content = new StringWidget(Math.max(0, getWidth() - style.padding.left - style.padding.right),
                Math.max(0, getHeight() - style.padding.top - style.padding.bottom), message, font).setColor(style.textColor);
        switch (style.textAlign) {
            case "center" -> content.alignCenter();
            case "right" -> content.alignRight();
            default -> content.alignLeft();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int contentWidth = Math.max(0, getWidth() - style.padding.left - style.padding.right);
        int contentHeight = Math.max(0, getHeight() - style.padding.top - style.padding.bottom);
        if (contentWidth == 0 || contentHeight == 0) {
            return;
        }
        content.setWidth(contentWidth);
        content.setHeight(contentHeight);
        content.setPosition(getX() + style.padding.left, getY() + style.padding.top);
        content.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
