package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LayoutInspectorControls {
    public record Control(String section, String field, String label, String value, boolean modified) {
        public String key() {
            return section + "." + field;
        }
    }

    private LayoutInspectorControls() {
    }

    public static List<Control> describe(String id, JsonObject source, HudLayoutOffsets overrides) {
        return describe(id, source, overrides, false);
    }

    public static List<Control> describe(String id, JsonObject source, HudLayoutOffsets overrides, boolean gridChild) {
        return describe(id, source, overrides, gridChild, gridChild);
    }

    public static List<Control> describe(String id, JsonObject source, HudLayoutOffsets overrides,
                                         boolean structuralChild, boolean gridChild) {
        if (source == null) {
            return List.of();
        }
        JsonObject effective = overrides.effectiveSettings(id, source);
        List<Control> controls = new ArrayList<>();
        JsonElement type = effective.get("type");
        boolean container = type != null && type.isJsonPrimitive()
                && WidgetContainers.supportsChildren(type.getAsString());
        if (container) {
            String layout = value(effective, "layout", "type", "none");
            controls.add(control(overrides, id, "layout", "type", "Layout", layout));
            if ("row".equals(layout) || "column".equals(layout)
                    || "anchorui:row".equals(layout) || "anchorui:column".equals(layout)) {
                controls.add(control(overrides, id, "layout", "gap", "Gap", value(effective, "layout", "gap", "0")));
                controls.add(control(overrides, id, "layout", "align", "Align", value(effective, "layout", "align", "start")));
                controls.add(control(overrides, id, "layout", "justify", "Justify", value(effective, "layout", "justify", "start")));
                controls.add(control(overrides, id, "layout", "wrap", "Wrap", value(effective, "layout", "wrap", "false")));
                controls.add(control(overrides, id, "layout", "lineGap", "Line gap", value(effective, "layout", "lineGap",
                        value(effective, "layout", "gap", "0"))));
                controls.add(control(overrides, id, "layout", "collapseHidden", "Collapse", value(effective, "layout", "collapseHidden", "false")));
            } else if ("grid".equals(layout) || "anchorui:grid".equals(layout)) {
                controls.add(control(overrides, id, "layout", "columns", "Columns", value(effective, "layout", "columns", "1")));
                controls.add(control(overrides, id, "layout", "gap", "Gap", value(effective, "layout", "gap", "0")));
                controls.add(control(overrides, id, "layout", "columnGap", "Col gap", value(effective, "layout", "columnGap",
                        value(effective, "layout", "gap", "0"))));
                controls.add(control(overrides, id, "layout", "rowGap", "Row gap", value(effective, "layout", "rowGap",
                        value(effective, "layout", "gap", "0"))));
                controls.add(control(overrides, id, "layout", "cellWidth", "Cell W", value(effective, "layout", "cellWidth", "0")));
                controls.add(control(overrides, id, "layout", "cellHeight", "Cell H", value(effective, "layout", "cellHeight", "0")));
                controls.add(control(overrides, id, "layout", "justifyItems", "Cell X", value(effective, "layout", "justifyItems", "start")));
                controls.add(control(overrides, id, "layout", "alignItems", "Cell Y", value(effective, "layout", "alignItems", "start")));
                controls.add(control(overrides, id, "layout", "collapseHidden", "Collapse", value(effective, "layout", "collapseHidden", "false")));
            }
        }
        if (structuralChild || gridChild) {
            String enabled = value(effective, "layoutItem", "enabled", "true");
            if (!gridChild) {
                controls.add(control(overrides, id, "layoutItem", "enabled", "Auto item", enabled));
            }
            if (gridChild || Boolean.parseBoolean(enabled)) {
                if (!gridChild) {
                    controls.add(control(overrides, id, "layoutItem", "grow", "Grow", value(effective, "layoutItem", "grow", "0")));
                    controls.add(control(overrides, id, "layoutItem", "shrink", "Shrink", value(effective, "layoutItem", "shrink", "0")));
                }
                controls.add(control(overrides, id, "layoutItem", "minWidth", "Min W", value(effective, "layoutItem", "minWidth", "0")));
                controls.add(control(overrides, id, "layoutItem", "maxWidth", "Max W", value(effective, "layoutItem", "maxWidth", "16384")));
                controls.add(control(overrides, id, "layoutItem", "minHeight", "Min H", value(effective, "layoutItem", "minHeight", "0")));
                controls.add(control(overrides, id, "layoutItem", "maxHeight", "Max H", value(effective, "layoutItem", "maxHeight", "16384")));
            }
        }
        return List.copyOf(controls);
    }

    private static Control control(HudLayoutOffsets overrides, String id, String section, String field,
                                   String label, String value) {
        return new Control(section, field, label, value, overrides.settingOverride(id, section, field) != null);
    }

    private static String value(JsonObject settings, String section, String field, String fallback) {
        JsonElement raw = settings.get(section);
        if (raw == null || raw.isJsonNull()) {
            return fallback;
        }
        if (raw.isJsonObject()) {
            JsonElement property = raw.getAsJsonObject().get(field);
            return property == null || property.isJsonNull() ? fallback : property.getAsString();
        }
        if ("layout".equals(section) && "type".equals(field) && raw.getAsJsonPrimitive().isString()) {
            return raw.getAsString();
        }
        if ("layoutItem".equals(section) && "enabled".equals(field) && raw.getAsJsonPrimitive().isBoolean()) {
            return raw.getAsString();
        }
        return fallback;
    }

    public static JsonElement stepped(Control control, int direction) {
        Objects.requireNonNull(control, "Control must not be null");
        if (direction == 0) {
            throw new IllegalArgumentException("Editor adjustment direction cannot be zero");
        }
        String field = control.field();
        String section = control.section();
        if ("type".equals(field) && "layout".equals(section)) {
            return new JsonPrimitive(cycle(control.value(), direction, "none", "row", "column", "grid"));
        }
        if ("align".equals(field) || "justifyItems".equals(field) || "alignItems".equals(field)) {
            return new JsonPrimitive(cycle(control.value(), direction, "start", "center", "end", "stretch"));
        }
        if ("justify".equals(field)) {
            return new JsonPrimitive(cycle(control.value(), direction, "start", "center", "end", "space_between"));
        }
        if ("wrap".equals(field) || "collapseHidden".equals(field) || "enabled".equals(field)) {
            return new JsonPrimitive(!Boolean.parseBoolean(control.value()));
        }
        double previous = Double.parseDouble(control.value());
        double step = switch (field) {
            case "grow", "shrink", "columns" -> 1;
            case "gap", "lineGap", "columnGap", "rowGap" -> 2;
            default -> 10;
        };
        double min = switch (field) {
            case "columns" -> 1;
            default -> 0;
        };
        double max = switch (field) {
            case "grow", "shrink" -> 1000;
            case "columns" -> 256;
            case "gap", "lineGap", "columnGap", "rowGap" -> 4096;
            default -> 16384;
        };
        double updated = Math.max(min, Math.min(max, previous + (direction > 0 ? step : -step)));
        if ("grow".equals(field) || "shrink".equals(field)) {
            return new JsonPrimitive(updated);
        }
        return new JsonPrimitive((int) updated);
    }

    private static String cycle(String value, int direction, String... options) {
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value) || ("anchorui:" + options[i]).equals(value)) {
                index = i;
                break;
            }
        }
        return options[Math.floorMod(index + (direction > 0 ? 1 : -1), options.length)];
    }
}
