package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.binding.BoundNumber;
import com.ludwici.anchorui.api.binding.ProgressMath;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.util.GsonHelper;


public final class ProgressWidget extends AnchoredWidget {
    private final WidgetContext context;
    private final Font font;
    private final BoundNumber value;
    private final BoundNumber maximum;
    private final int fillColor;
    private final boolean showText;

    public ProgressWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 160, style.size.height > 0 ? style.size.height : 14, style);
        this.context = context;
        this.font = context.font();
        this.value = BoundNumber.parse(settings, "value", 0.0);
        this.maximum = BoundNumber.parse(settings, "max", 100.0);
        this.fillColor = parseColor(GsonHelper.getAsString(settings, "fillColor", "#E85B60"));
        this.showText = GsonHelper.getAsBoolean(settings, "showText", false);
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = getX() + style.padding.left;
        int top = getY() + style.padding.top;
        int width = Math.max(0, getWidth() - style.padding.left - style.padding.right);
        int height = Math.max(0, getHeight() - style.padding.top - style.padding.bottom);
        if (width == 0 || height == 0) {
            return;
        }
        double current = value.get(context);
        double max = maximum.get(context);
        double ratio = ProgressMath.fraction(current, max);
        int filled = (int) Math.round(width * ratio);
        if (filled > 0) {
            graphics.fill(left, top, left + filled, top + height, fillColor);
        }
        if (showText) {
            String label = Math.round(current) + " / " + Math.round(max);
            int textY = top + (height - font.lineHeight) / 2;
            graphics.drawCenteredString(font, label, left + width / 2, textY, style.textColor);
        }
    }

    private static int parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("fillColor must be #RRGGBB or #AARRGGBB");
        }
        try {
            long parsed = Long.parseLong(hex, 16);
            return (int) (hex.length() == 6 ? parsed | 0xFF000000L : parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid fillColor: " + value, exception);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
