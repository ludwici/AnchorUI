package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StackLayout {
    public enum Axis {
        ROW, COLUMN
    }

    public enum Align {
        START, CENTER, END, STRETCH
    }

    public enum Justify {
        START, CENTER, END, SPACE_BETWEEN
    }

    public record Config(Axis axis, int gap, Align align, Justify justify, boolean collapseHidden) {
        public static Config parse(JsonObject settings) {
            if (!settings.has("layout")) {
                return null;
            }
            JsonElement raw = settings.get("layout");
            JsonObject properties;
            String type;
            if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()) {
                type = raw.getAsString();
                properties = new JsonObject();
            } else if (raw.isJsonObject()) {
                properties = raw.getAsJsonObject();
                type = GsonHelper.getAsString(properties, "type", "column");
            } else {
                throw new IllegalArgumentException("Container layout must be 'row', 'column' or an object");
            }
            Axis axis = switch (type) {
                case "row", "anchorui:row" -> Axis.ROW;
                case "column", "anchorui:column" -> Axis.COLUMN;
                default -> throw new IllegalArgumentException("Unknown container layout: " + type);
            };
            int gap = GsonHelper.getAsInt(properties, "gap", 0);
            if (gap < 0 || gap > 4096) {
                throw new IllegalArgumentException("Container layout gap must be between 0 and 4096");
            }
            String rawAlign = GsonHelper.getAsString(properties, "align", "start");
            Align align = switch (rawAlign) {
                case "start" -> Align.START;
                case "center" -> Align.CENTER;
                case "end" -> Align.END;
                case "stretch" -> Align.STRETCH;
                default -> throw new IllegalArgumentException("Unknown container layout align: " + rawAlign);
            };
            String rawJustify = GsonHelper.getAsString(properties, "justify", "start");
            Justify justify = switch (rawJustify) {
                case "start" -> Justify.START;
                case "center" -> Justify.CENTER;
                case "end" -> Justify.END;
                case "space_between" -> Justify.SPACE_BETWEEN;
                default -> throw new IllegalArgumentException("Unknown container layout justify: " + rawJustify);
            };
            boolean collapseHidden = GsonHelper.getAsBoolean(properties, "collapseHidden", false);
            return new Config(axis, gap, align, justify, collapseHidden);
        }
    }

    public record Item(String id, int width, int height, int offsetX, int offsetY) {
    }

    public record Placement(int x, int y, int width, int height) {
    }

    private StackLayout() {
    }

    public static Map<String, Placement> arrange(Config config, int width, int height,
                                                  List<Item> items, boolean stretchCross) {
        if (items.isEmpty()) {
            return Map.of();
        }
        boolean row = config.axis() == Axis.ROW;
        int mainAvailable = Math.max(0, row ? width : height);
        int crossAvailable = Math.max(0, row ? height : width);
        long occupied = (long) config.gap() * (items.size() - 1);
        for (Item item : items) {
            occupied += row ? item.width() : item.height();
        }
        int free = (int) Math.max(0, Math.min(Integer.MAX_VALUE, mainAvailable - occupied));
        int start = switch (config.justify()) {
            case START, SPACE_BETWEEN -> 0;
            case CENTER -> free / 2;
            case END -> free;
        };
        int cursor = start;
        Map<String, Placement> placed = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            int itemWidth = item.width();
            int itemHeight = item.height();
            if (config.align() == Align.STRETCH && stretchCross) {
                if (row) {
                    itemHeight = crossAvailable;
                } else {
                    itemWidth = crossAvailable;
                }
            }
            int itemCross = row ? itemHeight : itemWidth;
            int cross = switch (config.align()) {
                case START, STRETCH -> 0;
                case CENTER -> Math.max(0, crossAvailable - itemCross) / 2;
                case END -> Math.max(0, crossAvailable - itemCross);
            };
            int x = (row ? cursor : cross) + item.offsetX();
            int y = (row ? cross : cursor) + item.offsetY();
            placed.put(item.id(), new Placement(x, y, itemWidth, itemHeight));
            cursor += (row ? item.width() : item.height()) + config.gap();
            if (config.justify() == Justify.SPACE_BETWEEN && items.size() > 1 && index < items.size() - 1) {
                cursor += (int) ((long) (index + 1) * free / (items.size() - 1))
                        - (int) ((long) index * free / (items.size() - 1));
            }
        }
        return Map.copyOf(placed);
    }

    public static ContainerLayout.Size intrinsic(Config config, List<Item> items) {
        if (items.isEmpty()) {
            return new ContainerLayout.Size(1, 1);
        }
        long main = (long) config.gap() * (items.size() - 1);
        int cross = 1;
        for (Item item : items) {
            main += config.axis() == Axis.ROW ? item.width() : item.height();
            cross = Math.max(cross, config.axis() == Axis.ROW ? item.height() : item.width());
        }
        if (main > 16384) {
            throw new IllegalArgumentException("Auto-sized stack content exceeds 16384 GUI pixels");
        }
        return config.axis() == Axis.ROW
                ? new ContainerLayout.Size((int) Math.max(1, main), cross)
                : new ContainerLayout.Size(cross, (int) Math.max(1, main));
    }
}
