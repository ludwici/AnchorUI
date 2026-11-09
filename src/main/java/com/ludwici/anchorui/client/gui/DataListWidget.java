package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.binding.BoundData;
import com.ludwici.anchorui.api.binding.DataBindingSpec;
import com.ludwici.anchorui.api.binding.DataValuePaths;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class DataListWidget extends AnchoredWidget {
    private final WidgetContext context;
    private final BoundData items;
    private final List<String> itemPath;
    private final String emptyText;
    private final int maxEntries;
    private final Font font;
    private JsonArray lastItems;
    private int lastWidth = -1;
    private List<FormattedCharSequence> lines = List.of();

    public DataListWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 180, style.size.height > 0 ? style.size.height : 72, style);
        this.context = context;
        this.font = context.font();
        this.items = BoundData.parse(settings, "items", new JsonArray(), DataBindingSpec.Kind.LIST);
        this.itemPath = DataValuePaths.tokens(GsonHelper.getAsString(settings, "itemPath", ""));
        this.emptyText = GsonHelper.getAsString(settings, "emptyText", "");
        this.maxEntries = GsonHelper.getAsInt(settings, "maxEntries", 12);
        if (maxEntries < 1 || maxEntries > 100) {
            throw new IllegalArgumentException("maxEntries must be between 1 and 100");
        }
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
        JsonArray current = items.get(context).getAsJsonArray();
        if (!current.equals(lastItems) || lastWidth != innerWidth) {
            lastItems = current;
            lastWidth = innerWidth;
            rebuildLines(current, innerWidth);
        }

        int x = getX() + style.padding.left;
        int y = getY() + style.padding.top;
        int lineHeight = font.lineHeight + 2;
        graphics.enableScissor(x, y, x + innerWidth, y + innerHeight);
        for (int index = 0; index < lines.size(); index++) {
            int lineY = y + index * lineHeight;
            if (lineY + font.lineHeight > y + innerHeight) {
                break;
            }
            FormattedCharSequence line = lines.get(index);
            int lineX = switch (style.textAlign) {
                case "center" -> x + (innerWidth - font.width(line)) / 2;
                case "right" -> x + innerWidth - font.width(line);
                default -> x;
            };
            graphics.drawString(font, line, lineX, lineY, style.textColor, false);
        }
        graphics.disableScissor();
    }

    private void rebuildLines(JsonArray items, int width) {
        List<FormattedCharSequence> content = new ArrayList<>();
        int count = Math.min(items.size(), maxEntries);
        if (count == 0 && !emptyText.isEmpty()) {
            content.addAll(font.split(Component.literal(emptyText), width));
        }
        for (int index = 0; index < count; index++) {
            JsonElement value = DataValuePaths.resolve(items.get(index), itemPath);
            String line = value == null || value.isJsonNull() ? "" : value.isJsonPrimitive() ? value.getAsString() : value.toString();
            content.addAll(font.split(Component.literal(line), width));
        }
        lines = List.copyOf(content);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
