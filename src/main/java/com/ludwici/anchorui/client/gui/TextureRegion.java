package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.layout.NineSlice;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TextureRegion {
    private static final Set<ResourceLocation> MISSING_WARNED = ConcurrentHashMap.newKeySet();
    private final ResourceLocation texture;
    private final int textureWidth;
    private final int textureHeight;
    private final int u;
    private final int v;
    private final int sourceWidth;
    private final int sourceHeight;
    private final int tint;
    private final NineSlice.Insets nineSlice;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private List<NineSlice.Tile> cachedTiles = List.of();

    public TextureRegion(WidgetContext context, JsonObject settings) {
        String path = GsonHelper.getAsString(settings, "texture", "");
        if (path.isBlank()) {
            throw new IllegalArgumentException("A textured widget requires texture");
        }
        String qualified = path.contains(":") ? path : context.entity().namespace + ":" + path;
        texture = ResourceLocation.tryParse(qualified);
        if (texture == null) {
            throw new IllegalArgumentException("Invalid texture resource identifier '" + path + "'; expected namespace:path");
        }
        if (context != null && context.minecraft() != null && context.minecraft().getResourceManager() != null
                && context.minecraft().getResourceManager().getResource(texture).isEmpty()
                && MISSING_WARNED.add(texture)) {
            AnchorUI.LOGGER.warn("AnchorUI missing texture resource '{}'. Expected asset path assets/{}/{}. " + "Check the active resource packs and reload with F3+T after fixing the asset.", texture, texture.getNamespace(), texture.getPath());
        }
        textureWidth = GsonHelper.getAsInt(settings, "textureWidth", 256);
        textureHeight = GsonHelper.getAsInt(settings, "textureHeight", 256);
        u = GsonHelper.getAsInt(settings, "u", 0);
        v = GsonHelper.getAsInt(settings, "v", 0);
        sourceWidth = GsonHelper.getAsInt(settings, "regionWidth", textureWidth - u);
        sourceHeight = GsonHelper.getAsInt(settings, "regionHeight", textureHeight - v);
        if (textureWidth <= 0 || textureHeight <= 0 || u < 0 || v < 0 || sourceWidth <= 0 || sourceHeight <= 0
                || (long) u + sourceWidth > textureWidth || (long) v + sourceHeight > textureHeight) {
            throw new IllegalArgumentException("Invalid texture region for '" + texture + "' (textureWidth=" + textureWidth + ", textureHeight=" + textureHeight + ", u=" + u + ", v=" + v + ", regionWidth=" + sourceWidth + ", regionHeight=" + sourceHeight + ")");
        }
        tint = WidgetColors.parse(GsonHelper.getAsString(settings, "tint", "#FFFFFFFF"));
        if (settings.has("nineSlice")) {
            JsonObject slice = GsonHelper.getAsJsonObject(settings, "nineSlice");
            nineSlice = new NineSlice.Insets(GsonHelper.getAsInt(slice, "left", 0), GsonHelper.getAsInt(slice, "top", 0), GsonHelper.getAsInt(slice, "right", 0), GsonHelper.getAsInt(slice, "bottom", 0));
            NineSlice.tiles(1, 1, sourceWidth, sourceHeight, nineSlice);
        } else {
            nineSlice = null;
        }
    }

    public static void clearMissingResourceWarnings() {
        MISSING_WARNED.clear();
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
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
            if (nineSlice == null) {
                graphics.blit(texture, x, y, width, height, (float) u, (float) v, sourceWidth, sourceHeight, textureWidth, textureHeight);
            } else {
                for (NineSlice.Tile tile : tiles(width, height)) {
                    graphics.blit(texture, x + tile.x(), y + tile.y(), tile.width(), tile.height(), (float) (u + tile.u()), (float) (v + tile.v()), tile.sourceWidth(), tile.sourceHeight(), textureWidth, textureHeight);
                }
            }
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }
    private List<NineSlice.Tile> tiles(int width, int height) {
        if (cachedWidth != width || cachedHeight != height) {
            cachedTiles = NineSlice.tiles(width, height, sourceWidth, sourceHeight, nineSlice);
            cachedWidth = width;
            cachedHeight = height;
        }
        return cachedTiles;
    }
}
