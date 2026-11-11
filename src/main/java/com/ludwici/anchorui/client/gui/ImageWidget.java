package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class ImageWidget extends AnchoredWidget {
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int u;
    private final int v;
    private final int regionWidth;
    private final int regionHeight;
    private final int tint;

    public ImageWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 64, style.size.height > 0 ? style.size.height : 64, style);
        String path = GsonHelper.getAsString(settings, "texture", "");
        if (path.isBlank()) {
            throw new IllegalArgumentException("anchorui:image requires texture");
        }
        String qualified = path.contains(":") ? path : context.entity().namespace + ":" + path;
        texture = ResourceLocation.tryParse(qualified);
        if (texture == null) {
            throw new IllegalArgumentException("Invalid image texture: " + path);
        }
        textureWidth = GsonHelper.getAsInt(settings, "textureWidth", 256);
        textureHeight = GsonHelper.getAsInt(settings, "textureHeight", 256);
        u = GsonHelper.getAsInt(settings, "u", 0);
        v = GsonHelper.getAsInt(settings, "v", 0);
        regionWidth = GsonHelper.getAsInt(settings, "regionWidth", textureWidth - u);
        regionHeight = GsonHelper.getAsInt(settings, "regionHeight", textureHeight - v);
        if (textureWidth <= 0 || textureHeight <= 0 || u < 0 || v < 0 || regionWidth <= 0 || regionHeight <= 0
                || (long) u + regionWidth > textureWidth || (long) v + regionHeight > textureHeight) {
            throw new IllegalArgumentException("Invalid anchorui:image UV region");
        }
        tint = WidgetColors.parse(GsonHelper.getAsString(settings, "tint", "#FFFFFFFF"));
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int width = getWidth() - style.padding.left - style.padding.right;
        int height = getHeight() - style.padding.top - style.padding.bottom;
        if (width <= 0 || height <= 0) {
            return;
        }
        float alpha = (tint >>> 24 & 0xFF) / 255f;
        float red = (tint >>> 16 & 0xFF) / 255f;
        float green = (tint >>> 8 & 0xFF) / 255f;
        float blue = (tint & 0xFF) / 255f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        try {
            graphics.blit(texture, getX() + style.padding.left, getY() + style.padding.top,
                    width, height, (float) u, (float) v, regionWidth, regionHeight, textureWidth, textureHeight);
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
