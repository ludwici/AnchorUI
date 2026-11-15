package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GridLayout {
    public enum Align {
        START, CENTER, END, STRETCH;

        private static Align parse(JsonObject properties, String key) {
            String raw = GsonHelper.getAsString(properties, key, "start");
            return switch (raw) {
                case "start" -> START;
                case "center" -> CENTER;
                case "end" -> END;
                case "stretch" -> STRETCH;
                default -> throw new IllegalArgumentException("Unknown grid " + key + ": " + raw);
            };
        }
    }

    public record Config(int columns, int columnGap, int rowGap, int cellWidth, int cellHeight,
                         Align justifyItems, Align alignItems, boolean collapseHidden) implements ContainerAutoLayout {
        public static Config parse(JsonObject settings) {
            JsonElement raw = settings.get("layout");
            if (raw == null) {
                return null;
            }
            JsonObject properties;
            if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()
                    && ("grid".equals(raw.getAsString()) || "anchorui:grid".equals(raw.getAsString()))) {
                properties = new JsonObject();
            } else if (raw.isJsonObject()) {
                properties = raw.getAsJsonObject();
                String type = GsonHelper.getAsString(properties, "type", "");
                if (!"grid".equals(type) && !"anchorui:grid".equals(type)) {
                    throw new IllegalArgumentException("Grid layout requires type 'grid'");
                }
            } else {
                throw new IllegalArgumentException("Grid layout requires a grid string or object");
            }
            int columns = GsonHelper.getAsInt(properties, "columns", 1);
            if (columns < 1 || columns > 256) {
                throw new IllegalArgumentException("Grid columns must be between 1 and 256");
            }
            int gap = dimension(properties, "gap", 0, 4096);
            int columnGap = dimension(properties, "columnGap", gap, 4096);
            int rowGap = dimension(properties, "rowGap", gap, 4096);
            int cellWidth = dimension(properties, "cellWidth", 0, 16384);
            int cellHeight = dimension(properties, "cellHeight", 0, 16384);
            return new Config(columns, columnGap, rowGap, cellWidth, cellHeight, Align.parse(properties, "justifyItems"), Align.parse(properties, "alignItems"), GsonHelper.getAsBoolean(properties, "collapseHidden", false));
        }

        private static int dimension(JsonObject object, String key, int defaultValue, int max) {
            int value = GsonHelper.getAsInt(object, key, defaultValue);
            if (value < 0 || value > max) {
                throw new IllegalArgumentException("Grid " + key + " must be between 0 and " + max);
            }
            return value;
        }

        @Override
        public ContainerAutoLayout.Arrangement arrange(int availableWidth, int availableHeight,
                                                       List<StackLayout.Item> items, boolean autoWidth, boolean autoHeight) {
            return GridLayout.layout(this, availableWidth, items, autoWidth);
        }
    }

    private GridLayout() {
    }

    private static ContainerAutoLayout.Arrangement layout(Config config, int availableWidth,
                                                           List<StackLayout.Item> items, boolean autoWidth) {
        if (items.isEmpty()) {
            return new ContainerAutoLayout.Arrangement(Map.of(), new ContainerLayout.Size(1, 1));
        }
        int columns = config.columns();
        int rows = (items.size() + columns - 1) / columns;
        int[] widths = new int[columns];
        int[] heights = new int[rows];
        if (config.cellWidth() > 0) {
            java.util.Arrays.fill(widths, config.cellWidth());
        } else if (!autoWidth) {
            int free = Math.max(0, availableWidth - config.columnGap() * (columns - 1));
            for (int column = 0; column < columns; column++) {
                widths[column] = free / columns + (column < free % columns ? 1 : 0);
            }
        } else {
            for (int index = 0; index < items.size(); index++) {
                StackLayout.Item item = items.get(index);
                widths[index % columns] = Math.max(widths[index % columns], item.rules().width(item.width()));
            }
        }
        if (config.cellHeight() > 0) {
            java.util.Arrays.fill(heights, config.cellHeight());
        } else {
            for (int index = 0; index < items.size(); index++) {
                StackLayout.Item item = items.get(index);
                heights[index / columns] = Math.max(heights[index / columns], item.rules().height(item.height()));
            }
        }

        long naturalWidth = (long) config.columnGap() * (columns - 1);
        long naturalHeight = (long) config.rowGap() * (rows - 1);
        for (int width : widths) {
            naturalWidth += width;
        }
        for (int height : heights) {
            naturalHeight += height;
        }
        if (naturalWidth > 16384 || naturalHeight > 16384) {
            throw new IllegalArgumentException("Grid content exceeds 16384 GUI pixels");
        }

        int[] x = new int[columns];
        int[] y = new int[rows];
        for (int column = 1; column < columns; column++) {
            x[column] = x[column - 1] + widths[column - 1] + config.columnGap();
        }
        for (int row = 1; row < rows; row++) {
            y[row] = y[row - 1] + heights[row - 1] + config.rowGap();
        }

        Map<String, StackLayout.Placement> placements = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            StackLayout.Item item = items.get(index);
            int column = index % columns;
            int row = index / columns;
            int cellWidth = widths[column];
            int cellHeight = heights[row];
            int width = config.justifyItems() == Align.STRETCH
                    ? item.rules().width(cellWidth) : item.rules().width(item.width());
            int height = config.alignItems() == Align.STRETCH
                    ? item.rules().height(cellHeight) : item.rules().height(item.height());
            int shiftX = switch (config.justifyItems()) {
                case START, STRETCH -> 0;
                case CENTER -> Math.max(0, cellWidth - width) / 2;
                case END -> Math.max(0, cellWidth - width);
            };
            int shiftY = switch (config.alignItems()) {
                case START, STRETCH -> 0;
                case CENTER -> Math.max(0, cellHeight - height) / 2;
                case END -> Math.max(0, cellHeight - height);
            };
            placements.put(item.id(), new StackLayout.Placement(x[column] + shiftX + item.offsetX(), y[row] + shiftY + item.offsetY(), width, height));
        }
        return new ContainerAutoLayout.Arrangement(Map.copyOf(placements), new ContainerLayout.Size((int) Math.max(1, naturalWidth), (int) Math.max(1, naturalHeight)));
    }
}
