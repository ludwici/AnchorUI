package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetConfigDecoder;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Layout algorithms for the repeated root widgets of anchorui:template_list.
 * Register custom layouts during client initialization, before screens are opened.
 */
public final class TemplateListLayoutRegistry {
    public record Metrics(int rowHeight, int gap, int availableWidth) {
    }

    /**
     * A positive width overrides the generated row's width. A width of zero
     * retains the template's own width. Height must always be positive.
     */
    public record Cell(int x, int y, int width, int height) {
        public Cell {
            if (width < 0 || height < 1) {
                throw new IllegalArgumentException("Template cell width must be nonnegative and height must be positive");
            }
        }
    }

    @FunctionalInterface
    public interface Layout {
        Cell place(int index);
    }

    @FunctionalInterface
    public interface Factory {
        Layout create(JsonObject options, Metrics metrics);
    }

    @FunctionalInterface
    public interface ConfiguredFactory<T> {
        Layout create(T options, Metrics metrics);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new ConcurrentHashMap<>();
    private static final ResourceLocation VERTICAL = ResourceLocation.fromNamespaceAndPath("anchorui", "vertical");
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath("anchorui", "grid");

    static {
        register(VERTICAL, (options, metrics) -> index -> {
            long y = (long) index * (metrics.rowHeight() + (long) metrics.gap());
            return new Cell(0, checkedCoordinate(y), 0, metrics.rowHeight());
        });

        register(GRID, (options, metrics) -> {
            int columns = GsonHelper.getAsInt(options, "columns", 1);
            int columnGap = GsonHelper.getAsInt(options, "columnGap", metrics.gap());
            int rowGap = GsonHelper.getAsInt(options, "rowGap", metrics.gap());
            int cellHeight = GsonHelper.getAsInt(options, "cellHeight", metrics.rowHeight());
            int explicitWidth = GsonHelper.getAsInt(options, "cellWidth", 0);
            if (columns < 1 || columns > 256) {
                throw new IllegalArgumentException("Grid columns must be in the range 1..256");
            }
            if (columnGap < 0 || columnGap > 4096 || rowGap < 0 || rowGap > 4096) {
                throw new IllegalArgumentException("Grid gaps must be in the range 0..4096");
            }
            if (cellHeight < 1 || cellHeight > 4096) {
                throw new IllegalArgumentException("Grid cellHeight must be in the range 1..4096");
            }
            if (explicitWidth < 0 || explicitWidth > 16384) {
                throw new IllegalArgumentException("Grid cellWidth must be in the range 1..16384 when specified");
            }

            long totalGaps = (long) (columns - 1) * columnGap;
            long computedWidth = (metrics.availableWidth() - totalGaps) / columns;
            int cellWidth = explicitWidth > 0 ? explicitWidth : checkedWidth(computedWidth);
            return index -> {
                if (index < 0) {
                    throw new IllegalArgumentException("Template item index must not be negative");
                }
                long x = (long) (index % columns) * (cellWidth + (long) columnGap);
                long y = (long) (index / columns) * (cellHeight + (long) rowGap);
                return new Cell(checkedCoordinate(x), checkedCoordinate(y), cellWidth, cellHeight);
            };
        });
    }

    private TemplateListLayoutRegistry() {
    }

    public static void register(ResourceLocation id, Factory factory) {
        Objects.requireNonNull(id, "Template layout id must not be null");
        Objects.requireNonNull(factory, "Template layout factory must not be null");
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Template layout already registered: " + id);
        }
    }

    public static <T> void register(ResourceLocation id, Codec<T> codec, ConfiguredFactory<T> factory) {
        Objects.requireNonNull(codec, "Template layout codec must not be null");
        Objects.requireNonNull(factory, "Configured template layout factory must not be null");
        register(id, (options, metrics) -> {
            JsonObject arguments = options.deepCopy();
            arguments.remove("type");
            T config = WidgetConfigDecoder.decode(codec, arguments);
            return factory.create(config, metrics);
        });
    }

    public static Layout compile(JsonObject listSettings, int rowHeight, int gap) {
        Objects.requireNonNull(listSettings, "Template list settings must not be null");
        if (rowHeight < 1 || gap < 0) {
            throw new IllegalArgumentException("Invalid template list row metrics");
        }
        JsonObject options = new JsonObject();
        JsonElement raw = listSettings.get("layout");
        if (raw != null && !raw.isJsonNull()) {
            if (raw.isJsonObject()) {
                options = raw.getAsJsonObject().deepCopy();
            } else if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()) {
                options.addProperty("type", raw.getAsString());
            } else {
                throw new IllegalArgumentException("Template list layout must be a string or configuration object");
            }
        }
        String rawId = GsonHelper.getAsString(options, "type", "anchorui:vertical");
        if (!rawId.contains(":")) {
            rawId = "anchorui:" + rawId;
        }
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            throw new IllegalArgumentException("Invalid template layout id: " + rawId);
        }
        Factory factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown template layout: " + id);
        }

        JsonObject size = listSettings.has("size") && listSettings.get("size").isJsonObject()
                ? listSettings.getAsJsonObject("size") : new JsonObject();
        JsonObject padding = listSettings.has("padding") && listSettings.get("padding").isJsonObject()
                ? listSettings.getAsJsonObject("padding") : new JsonObject();
        int width = GsonHelper.getAsInt(size, "width", 0);
        int left = GsonHelper.getAsInt(padding, "left", 0);
        int right = GsonHelper.getAsInt(padding, "right", 0);
        int availableWidth = Math.max(0, width - left - right);
        Layout layout;
        try {
            layout = Objects.requireNonNull(factory.create(options, new Metrics(rowHeight, gap, availableWidth)),
                    "Template layout factory returned null: " + id);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid template layout '" + id + "': " + exception.getMessage(), exception);
        }
        return index -> {
            Cell cell;
            try {
                cell = Objects.requireNonNull(layout.place(index), "Template layout returned null cell");
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Template layout '" + id + "' failed for item " + index + ": " + exception.getMessage(), exception);
            }
            return cell;
        };
    }

    private static int checkedWidth(long value) {
        if (value < 1 || value > 16384) {
            throw new IllegalArgumentException("Grid needs a positive cellWidth or sufficient template list size.width");
        }
        return (int) value;
    }

    private static int checkedCoordinate(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Template layout coordinate exceeds integer bounds");
        }
        return (int) value;
    }
}
