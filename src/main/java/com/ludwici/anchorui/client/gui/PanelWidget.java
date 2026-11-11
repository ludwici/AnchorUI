package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.layout.RoundedPanelGeometry;

import java.util.List;
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
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private List<RoundedPanelGeometry.Band> cachedBands = List.of();

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
        if (outerRadius == 0) {
            renderRectangle(graphics, border, width, height);
        } else {
            renderRounded(graphics, border, width, height);
        }
        if (texture != null) {
            texture.render(graphics, getX(), getY(), width, height);
        }
    }

    private void renderRectangle(GuiGraphics graphics, int border, int width, int height) {
        int x = getX();
        int y = getY();
        if (border > 0 && (borderColor >>> 24) != 0) {
            graphics.fill(x, y, x + width, y + border, borderColor);
            graphics.fill(x, y + height - border, x + width, y + height, borderColor);
            if (height > 2 * border) {
                graphics.fill(x, y + border, x + border, y + height - border, borderColor);
                graphics.fill(x + width - border, y + border, x + width, y + height - border, borderColor);
            }
        }
        if ((fillColor >>> 24) != 0 && width > 2 * border && height > 2 * border) {
            graphics.fill(x + border, y + border, x + width - border, y + height - border, fillColor);
        }
    }

    private void renderRounded(GuiGraphics graphics, int border, int width, int height) {
        if (width != cachedWidth || height != cachedHeight) {
            cachedBands = RoundedPanelGeometry.bands(width, height, radius, borderWidth);
            cachedWidth = width;
            cachedHeight = height;
        }

        for (RoundedPanelGeometry.Band band : cachedBands) {
            int outerLeft = getX() + band.outerInset();
            int outerRight = getX() + width - band.outerInset();
            int top = getY() + band.top();
            int bottom = top + band.height();
            if (!band.hasInterior()) {
                draw(graphics, outerLeft, top, outerRight, bottom, border > 0 ? borderColor : fillColor);
                continue;
            }
            int innerLeft = getX() + border + band.innerInset();
            int innerRight = getX() + width - border - band.innerInset();
            if (border > 0) {
                draw(graphics, outerLeft, top, innerLeft, bottom, borderColor);
                draw(graphics, innerRight, top, outerRight, bottom, borderColor);
            }
            draw(graphics, innerLeft, top, innerRight, bottom, fillColor);
        }
    }

    private static void draw(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x2 > x1 && y2 > y1 && (color >>> 24) != 0) {
            graphics.fill(x1, y1, x2, y2, color);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
