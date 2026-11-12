package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
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

    public record Config(Axis axis, int gap, Align align, Justify justify,
                         boolean collapseHidden, boolean wrap, int lineGap) implements ContainerAutoLayout {
        public Config(Axis axis, int gap, Align align, Justify justify, boolean collapseHidden) {
            this(axis, gap, align, justify, collapseHidden, false, gap);
        }

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
                default -> throw new IllegalArgumentException("Unknown stack layout: " + type);
            };
            int gap = gap(properties, "gap", 0);
            int lineGap = gap(properties, "lineGap", gap);
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
            boolean wrap = GsonHelper.getAsBoolean(properties, "wrap", false);
            return new Config(axis, gap, align, justify, collapseHidden, wrap, lineGap);
        }

        private static int gap(JsonObject object, String key, int fallback) {
            int result = GsonHelper.getAsInt(object, key, fallback);
            if (result < 0 || result > 4096) {
                throw new IllegalArgumentException("Container layout " + key + " must be between 0 and 4096");
            }
            return result;
        }

        @Override
        public ContainerAutoLayout.Arrangement arrange(int availableWidth, int availableHeight, List<Item> items,
                                                       boolean autoWidth, boolean autoHeight) {
            return StackLayout.layout(this, availableWidth, availableHeight, items, autoWidth, autoHeight);
        }
    }

    public record Rules(boolean managed, double grow, double shrink, int minWidth, int maxWidth,
                        int minHeight, int maxHeight) {
        public static final Rules DEFAULT = new Rules(true, 0, 0, 0, 16384, 0, 16384);

        public static Rules parse(JsonObject settings) {
            JsonElement raw = settings.get("layoutItem");
            if (raw == null || raw.isJsonNull()) {
                return DEFAULT;
            }
            if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isBoolean()) {
                return raw.getAsBoolean() ? DEFAULT : new Rules(false, 0, 0, 0, 16384, 0, 16384);
            }
            if (!raw.isJsonObject()) {
                throw new IllegalArgumentException("layoutItem must be a boolean or an object");
            }
            JsonObject object = raw.getAsJsonObject();
            double grow = number(object, "grow");
            double shrink = number(object, "shrink");
            int minWidth = dimension(object, "minWidth", 0);
            int maxWidth = dimension(object, "maxWidth", 16384);
            int minHeight = dimension(object, "minHeight", 0);
            int maxHeight = dimension(object, "maxHeight", 16384);
            if (maxWidth < minWidth || maxHeight < minHeight) {
                throw new IllegalArgumentException("layoutItem maximum size must not be smaller than its minimum");
            }
            return new Rules(GsonHelper.getAsBoolean(object, "enabled", true), grow, shrink, minWidth, maxWidth, minHeight, maxHeight);
        }

        private static double number(JsonObject object, String key) {
            double result = GsonHelper.getAsDouble(object, key, 0);
            if (!Double.isFinite(result) || result < 0 || result > 1000) {
                throw new IllegalArgumentException("layoutItem " + key + " must be between 0 and 1000");
            }
            return result;
        }

        private static int dimension(JsonObject object, String key, int fallback) {
            int result = GsonHelper.getAsInt(object, key, fallback);
            if (result < 0 || result > 16384) {
                throw new IllegalArgumentException("layoutItem " + key + " must be between 0 and 16384");
            }
            return result;
        }

        public int width(int requested) {
            return Math.max(minWidth, Math.min(maxWidth, requested));
        }

        public int height(int requested) {
            return Math.max(minHeight, Math.min(maxHeight, requested));
        }
    }

    public record Item(String id, int width, int height, int offsetX, int offsetY, Rules rules) {
        public Item(String id, int width, int height, int offsetX, int offsetY) {
            this(id, width, height, offsetX, offsetY, Rules.DEFAULT);
        }
    }

    public record Placement(int x, int y, int width, int height) {
    }

    private record Line(List<Integer> indices, int mainSize, int crossSize) {
    }

    private StackLayout() {
    }

    public static Map<String, Placement> arrange(Config config, int width, int height,
                                                  List<Item> items, boolean stretchCross) {
        boolean row = config.axis() == Axis.ROW;
        return layout(config, width, height, items, row ? false : !stretchCross,
                row ? !stretchCross : false).placements();
    }

    public static ContainerLayout.Size intrinsic(Config config, List<Item> items) {
        return layout(config, 0, 0, items, true, true).intrinsic();
    }

    private static ContainerAutoLayout.Arrangement layout(Config config, int width, int height, List<Item> items,
                                                           boolean autoWidth, boolean autoHeight) {
        if (items.isEmpty()) {
            return new ContainerAutoLayout.Arrangement(Map.of(), new ContainerLayout.Size(1, 1));
        }
        boolean row = config.axis() == Axis.ROW;
        int mainAvailable = Math.max(0, row ? width : height);
        int crossAvailable = Math.max(0, row ? height : width);
        boolean autoMain = row ? autoWidth : autoHeight;
        boolean autoCross = row ? autoHeight : autoWidth;

        int count = items.size();
        int[] mains = new int[count];
        int[] crosses = new int[count];
        for (int index = 0; index < count; index++) {
            Item item = items.get(index);
            mains[index] = row ? item.rules().width(item.width()) : item.rules().height(item.height());
            crosses[index] = row ? item.rules().height(item.height()) : item.rules().width(item.width());
        }

        List<Line> lines = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int currentMain = 0;
        int currentCross = 0;
        boolean wrapping = config.wrap() && !autoMain && mainAvailable > 0;
        for (int index = 0; index < count; index++) {
            int next = currentMain + (current.isEmpty() ? 0 : config.gap()) + mains[index];
            if (wrapping && !current.isEmpty() && next > mainAvailable) {
                lines.add(new Line(List.copyOf(current), currentMain, currentCross));
                current.clear();
                currentMain = 0;
                currentCross = 0;
            }
            currentMain += (current.isEmpty() ? 0 : config.gap()) + mains[index];
            currentCross = Math.max(currentCross, crosses[index]);
            current.add(index);
        }
        lines.add(new Line(List.copyOf(current), currentMain, currentCross));

        int naturalMain = 1;
        long naturalCross = (long) config.lineGap() * (lines.size() - 1);
        for (Line line : lines) {
            naturalMain = Math.max(naturalMain, line.mainSize());
            naturalCross += line.crossSize();
        }
        if (naturalMain > 16384 || naturalCross > 16384) {
            throw new IllegalArgumentException("Auto-sized stack content exceeds 16384 GUI pixels");
        }
        ContainerLayout.Size intrinsic = row
                ? new ContainerLayout.Size(naturalMain, (int) Math.max(1, naturalCross))
                : new ContainerLayout.Size((int) Math.max(1, naturalCross), naturalMain);

        int crossCursor = 0;
        Map<String, Placement> placements = new LinkedHashMap<>();
        for (Line line : lines) {
            int[] resized = mains.clone();
            if (!autoMain) {
                distribute(items, line.indices(), resized, mainAvailable - line.mainSize()
                        , row, config.gap());
            }
            int occupied = config.gap() * Math.max(0, line.indices().size() - 1);
            for (int index : line.indices()) {
                occupied += resized[index];
            }
            int free = autoMain ? 0 : Math.max(0, mainAvailable - occupied);
            int cursor = switch (config.justify()) {
                case START, SPACE_BETWEEN -> 0;
                case CENTER -> free / 2;
                case END -> free;
            };
            int lineCross = lines.size() == 1 && !autoCross ? Math.max(line.crossSize(), crossAvailable) : line.crossSize();
            for (int local = 0; local < line.indices().size(); local++) {
                int index = line.indices().get(local);
                Item item = items.get(index);
                int mainSize = resized[index];
                int crossSize = crosses[index];
                if (config.align() == Align.STRETCH) {
                    int target = lineCross;
                    crossSize = row ? item.rules().height(target) : item.rules().width(target);
                }
                int crossOffset = switch (config.align()) {
                    case START, STRETCH -> 0;
                    case CENTER -> Math.max(0, lineCross - crossSize) / 2;
                    case END -> Math.max(0, lineCross - crossSize);
                };
                int mainPosition = cursor;
                int crossPosition = crossCursor + crossOffset;
                placements.put(item.id(), row
                        ? new Placement(mainPosition + item.offsetX(), crossPosition + item.offsetY(), mainSize, crossSize)
                        : new Placement(crossPosition + item.offsetX(), mainPosition + item.offsetY(), crossSize, mainSize));
                cursor += mainSize + config.gap();
                if (config.justify() == Justify.SPACE_BETWEEN && line.indices().size() > 1
                        && local < line.indices().size() - 1) {
                    cursor += (int) ((long) (local + 1) * free / (line.indices().size() - 1))
                            - (int) ((long) local * free / (line.indices().size() - 1));
                }
            }
            crossCursor += lineCross + config.lineGap();
        }
        return new ContainerAutoLayout.Arrangement(Map.copyOf(placements), intrinsic);
    }

    private static void distribute(List<Item> items, List<Integer> indices, int[] sizes, int initialDelta,
                                   boolean row, int gap) {
        int delta = initialDelta;
        if (delta == 0) {
            return;
        }
        boolean growing = delta > 0;
        int remaining = Math.abs(delta);
        for (int pass = 0; pass < indices.size() + 1 && remaining > 0; pass++) {
            double totalWeight = 0;
            for (int index : indices) {
                Rules rules = items.get(index).rules();
                int limit = growing
                        ? (row ? rules.maxWidth() : rules.maxHeight()) - sizes[index]
                        : sizes[index] - (row ? rules.minWidth() : rules.minHeight());
                if (limit > 0) {
                    totalWeight += growing ? rules.grow() : rules.shrink() * Math.max(1, sizes[index]);
                }
            }
            if (totalWeight <= 0) {
                break;
            }
            int allocated = 0;
            int largestIndex = -1;
            double largestFraction = -1;
            for (int index : indices) {
                Rules rules = items.get(index).rules();
                int limit = growing
                        ? (row ? rules.maxWidth() : rules.maxHeight()) - sizes[index]
                        : sizes[index] - (row ? rules.minWidth() : rules.minHeight());
                double weight = growing ? rules.grow() : rules.shrink() * Math.max(1, sizes[index]);
                if (limit <= 0 || weight <= 0) {
                    continue;
                }
                double exact = (double) remaining * weight / totalWeight;
                int portion = Math.min(limit, (int) Math.floor(exact));
                if (portion > 0) {
                    sizes[index] += growing ? portion : -portion;
                    allocated += portion;
                }
                double fractional = exact - Math.floor(exact);
                if (limit > portion && fractional > largestFraction) {
                    largestFraction = fractional;
                    largestIndex = index;
                }
            }
            remaining -= allocated;
            if (allocated == 0 && largestIndex >= 0) {
                sizes[largestIndex] += growing ? 1 : -1;
                remaining--;
            }
        }
    }
}
