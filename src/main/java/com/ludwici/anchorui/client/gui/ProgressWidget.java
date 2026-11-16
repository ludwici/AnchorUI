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
    private final StateControlTextures textures;
    private final int fillColor;
    private final int borderColor;
    private final int borderWidth;
    private final boolean showText;
    private final WidgetSizeTransition fillTransition;

    public ProgressWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 160, style.size.height > 0 ? style.size.height : 14, style);
        this.context = context;
        font = context.font();
        value = BoundNumber.parse(settings, "value", 0.0);
        maximum = BoundNumber.parse(settings, "max", 100.0);
        JsonObject appearance = style.resolvedSettings;
        fillColor = WidgetColors.parse(GsonHelper.getAsString(appearance, "fillColor", "#E85B60"));
        borderColor = WidgetColors.parse(GsonHelper.getAsString(appearance, "borderColor", "#00000000"));
        borderWidth = GsonHelper.getAsInt(appearance, "borderWidth", 0);
        if (borderWidth < 0 || borderWidth > 256) {
            throw new IllegalArgumentException("Progress borderWidth must be between 0 and 256");
        }
        textures = new StateControlTextures(context, appearance);
        fillTransition = WidgetSizeTransition.parse(appearance, context, true);
        showText = GsonHelper.getAsBoolean(appearance, "showText", false);
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!textures.render(graphics, "background", getX(), getY(), getWidth(), getHeight(), "normal")) {
            renderBackground(graphics);
        }
        int left = getX() + style.padding.left;
        int top = getY() + style.padding.top;
        int width = Math.max(0, getWidth() - style.padding.left - style.padding.right);
        int height = Math.max(0, getHeight() - style.padding.top - style.padding.bottom);
        if (width == 0 || height == 0) {
            renderFrame(graphics);
            return;
        }
        double current = value.get(context);
        double max = maximum.get(context);
        double fraction = ProgressMath.fraction(current, max);
        int filled = (int) Math.round(width * (fillTransition == null ? fraction : fillTransition.widthFraction(fraction, System.nanoTime())));
        if (filled > 0 && !textures.renderClippedWidth(graphics, "fill", left, top, width, height, filled, "normal")) {
            graphics.fill(left, top, left + filled, top + height, fillColor);
        }
        renderFrame(graphics);
        if (showText) {
            String label = Math.round(current) + " / " + Math.round(max);
            int textY = top + (height - font.lineHeight) / 2;
            graphics.drawCenteredString(font, label, left + width / 2, textY, style.textColor);
        }
    }

    private void renderFrame(GuiGraphics graphics) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        if (textures.render(graphics, "frame", x, y, width, height, "normal")
                || borderWidth == 0 || (borderColor >>> 24) == 0 || width <= 0 || height <= 0) {
            return;
        }
        int border = Math.min(borderWidth, Math.min(width, height) / 2);
        graphics.fill(x, y, x + width, y + border, borderColor);
        graphics.fill(x, y + height - border, x + width, y + height, borderColor);
        if (height > border * 2) {
            graphics.fill(x, y + border, x + border, y + height - border, borderColor);
            graphics.fill(x + width - border, y + border, x + width, y + height - border, borderColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
