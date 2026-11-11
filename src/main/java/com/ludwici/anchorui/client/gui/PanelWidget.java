package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.util.GsonHelper;

public final class PanelWidget extends AnchoredWidget {
    private final int fillColor;
    private final int borderColor;
    private final int borderWidth;
    private final int radius;
    private final TextureRegion texture;

    public PanelWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 120, style.size.height > 0 ? style.size.height : 70, style);
        fillColor = settings.has("fillColor") ? WidgetColors.parse(GsonHelper.getAsString(settings, "fillColor")) : style.background.color;
        borderColor = WidgetColors.parse(GsonHelper.getAsString(settings, "borderColor", "#00000000"));
        borderWidth = GsonHelper.getAsInt(settings, "borderWidth", 0);
        radius = GsonHelper.getAsInt(settings, "radius", 0);
        if (borderWidth < 0 || radius < 0) {
            throw new IllegalArgumentException("Panel borderWidth and radius must not be negative");
        }
        texture = settings.has("texture") ? new TextureRegion(context, settings) : null;
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        int border = Math.min(borderWidth, Math.min(width, height) / 2);
        int outerRadius = Math.min(radius, Math.min(width, height) / 2);
        int innerWidth = width - border * 2;
        int innerHeight = height - border * 2;
        int innerRadius = Math.max(0, outerRadius - border);
        if (outerRadius == 0) {
            renderRectangle(graphics, border, width, height);
        } else {
            renderRounded(graphics, border, outerRadius, innerWidth, innerHeight, innerRadius, width, height);
        }
        if (texture != null) {
            texture.render(graphics, getX(), getY(), width, height);
        }
    }

    private void renderRectangle(GuiGraphics graphics, int border, int width, int height) {
        int x = getX();
        int y = getY();
        if (border > 0 && (borderColor >>> 24) != 0) {
            graphics.fill(x, y, x + width, y + height, borderColor);
        }
        if ((fillColor >>> 24) != 0) {
            graphics.fill(x + border, y + border, x + width - border, y + height - border, fillColor);
        }
    }

    private void renderRounded(GuiGraphics graphics, int border, int outerRadius, int innerWidth,
                               int innerHeight, int innerRadius, int width, int height) {
        for (int row = 0; row < height; row++) {
            int outside = inset(row, height, outerRadius);
            int outerLeft = getX() + outside;
            int outerRight = getX() + width - outside;
            int innerRow = row - border;
            boolean inside = innerWidth > 0 && innerHeight > 0 && innerRow >= 0 && innerRow < innerHeight;
            if (!inside) {
                draw(graphics, outerLeft, getY() + row, outerRight, border > 0 ? borderColor : fillColor);
                continue;
            }
            int insideInset = inset(innerRow, innerHeight, Math.min(innerRadius, Math.min(innerWidth, innerHeight) / 2));
            int innerLeft = getX() + border + insideInset;
            int innerRight = getX() + width - border - insideInset;
            if (border > 0) {
                draw(graphics, outerLeft, getY() + row, innerLeft, borderColor);
                draw(graphics, innerRight, getY() + row, outerRight, borderColor);
            }
            draw(graphics, innerLeft, getY() + row, innerRight, fillColor);
        }
    }

    private static int inset(int row, int height, int radius) {
        if (radius <= 0) {
            return 0;
        }
        double distance = Math.max(radius - row - 0.5, row + 0.5 - (height - radius));
        if (distance <= 0) {
            return 0;
        }
        return (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
    }

    private static void draw(GuiGraphics graphics, int x1, int y, int x2, int color) {
        if (x2 > x1 && (color >>> 24) != 0) {
            graphics.fill(x1, y, x2, y + 1, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
