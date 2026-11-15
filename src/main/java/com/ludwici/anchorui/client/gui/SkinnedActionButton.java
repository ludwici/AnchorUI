package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import java.util.function.Consumer;

public final class SkinnedActionButton extends AbstractButton {
    private final WidgetContext context;
    private final Consumer<SkinnedActionButton> onPress;
    private final StateControlTextures textures;
    private final int fillColor;
    private final int borderColor;
    private final int hoverColor;
    private final int disabledColor;
    private final int focusColor;
    private final int textColor;

    public SkinnedActionButton(WidgetContext context, String label, LoreStyle.Element style,
                               JsonObject settings, Consumer<SkinnedActionButton> onPress) {
        super(0, 0, style.size.width > 0 ? style.size.width : 120, style.size.height > 0 ? style.size.height : 20, Component.literal(label));
        this.context = context;
        this.onPress = onPress;
        this.textures = new StateControlTextures(context, settings);
        this.fillColor = settings.has("fillColor")
                ? WidgetColors.parse(GsonHelper.getAsString(settings, "fillColor"))
                : (style.background.color >>> 24) != 0 ? style.background.color : 0xFF28384B;
        this.borderColor = WidgetColors.parse(GsonHelper.getAsString(settings, "borderColor", "#FF728CA3"));
        this.hoverColor = WidgetColors.parse(GsonHelper.getAsString(settings, "hoverColor", "#FF385E81"));
        this.disabledColor = WidgetColors.parse(GsonHelper.getAsString(settings, "disabledColor", "#FF353B43"));
        this.focusColor = WidgetColors.parse(GsonHelper.getAsString(settings, "focusColor", "#FFFFD16B"));
        this.textColor = style.textColor;
    }

    @Override
    public void onPress() {
        onPress.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean focused = isFocused();
        if (!textures.render(graphics, "background", x, y, width, height, ControlVisualStates.background(active, hovered, focused))) {
            int fill = !active ? disabledColor : hovered || focused ? hoverColor : fillColor;
            if ((borderColor >>> 24) != 0) {
                graphics.fill(x, y, x + width, y + height, borderColor);
                if (width > 2 && height > 2) {
                    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
                }
            } else if ((fill >>> 24) != 0) {
                graphics.fill(x, y, x + width, y + height, fill);
            }
        }
        int color = !active ? (textColor & 0x00FFFFFF) | 0x99000000 : textColor;
        graphics.drawCenteredString(context.font(), getMessage(), x + width / 2, y + (height - context.font().lineHeight) / 2, color);
        if (ControlVisualStates.needsFocusOutline(textures, active, hovered, focused)) {
            ControlVisualStates.renderFocusOutline(graphics, x, y, width, height, focusColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
