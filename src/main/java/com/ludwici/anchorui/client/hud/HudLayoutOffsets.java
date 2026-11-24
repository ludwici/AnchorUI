package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import java.util.List;
import com.ludwici.anchorui.api.widget.WidgetIds;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.gui.editor.AppearanceInspectorControls;
import com.ludwici.anchorui.api.WidgetTypeIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class HudLayoutOffsets {
    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    public record AnchorOverride(String relativeTo, ElementAnchor point, ElementAnchor anchor) {
        public AnchorOverride {
            relativeTo = WidgetIds.reference(relativeTo, "anchorui");
            if (point == null || anchor == null) {
                throw new IllegalArgumentException("HUD anchor points must not be null");
            }
        }
    }

    private final Map<String, Offset> offsets = new LinkedHashMap<>();
    private final Map<String, AnchorOverride> anchors = new LinkedHashMap<>();
    private final Map<String, List<PositionElement.Constraint>> anchorLists = new LinkedHashMap<>();
    private final Map<String, String> containerParents = new LinkedHashMap<>();
    private final Set<String> hidden = new TreeSet<>();
    private final Map<String, JsonObject> componentSettings = new LinkedHashMap<>();
    private final Map<String, JsonObject> createdWidgets = new LinkedHashMap<>();

    public Map<String, JsonObject> createdWidgets() {
        Map<String, JsonObject> copy = new LinkedHashMap<>();
        for (var entry : createdWidgets.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(copy);
    }

    public boolean isCreatedWidget(String id) {
        return createdWidgets.containsKey(WidgetIds.id(id, "anchorui"));
    }

    public void addCreatedWidget(String id, JsonObject definition) {
        String qualified = WidgetIds.id(id, "anchorui");
        JsonObject value = definition.deepCopy();
        if (value.has("id") && !qualified.equals(WidgetIds.id(value.get("id").getAsString(), "anchorui"))) {
            throw new IllegalArgumentException("Created widget ID differs from its stored definition: " + qualified);
        }
        value.addProperty("id", qualified);
        new com.ludwici.anchorui.api.LoreEntity.UiElement(value);
        if (createdWidgets.putIfAbsent(qualified, value) != null) {
            throw new IllegalArgumentException("Duplicate created widget: " + qualified);
        }
    }

    public boolean removeCreatedWidget(String id) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (createdWidgets.remove(qualified) == null) {
            return false;
        }
        reset(qualified);
        return true;
    }

    public Offset get(String id) {
        return offsets.getOrDefault(id, Offset.ZERO);
    }

    public AnchorOverride anchor(String id) {
        return anchors.get(id);
    }

    public List<PositionElement.Constraint> anchorConstraints(String id) {
        return anchorLists.get(id);
    }

    public PositionElement effectivePosition(String id, PositionElement original) {
        List<PositionElement.Constraint> list = anchorLists.get(id);
        if (list != null) {
            return new PositionElement(list);
        }
        AnchorOverride override = anchors.get(id);
        return override == null ? original : original.withPrimary(override.point(), override.anchor(), override.relativeTo());
    }

    public void setAnchorConstraints(String id, List<PositionElement.Constraint> constraints) {
        String qualified = WidgetIds.id(id, "anchorui");
        PositionElement validated = new PositionElement(constraints);
        anchorLists.put(qualified, validated.anchors);
        anchors.remove(qualified);
    }

    public void setAnchorConstraints(String id, List<PositionElement.Constraint> constraints, PositionElement source) {
        List<PositionElement.Constraint> validated = new PositionElement(constraints).anchors;
        if (validated.equals(source.anchors)) {
            clearAnchor(id);
        } else {
            setAnchorConstraints(id, validated);
        }
    }

    public void clearAnchorConstraints(String id) {
        anchorLists.remove(WidgetIds.id(id, "anchorui"));
    }

    public boolean isHidden(String id) {
        return hidden.contains(id);
    }

    public String containerParent(String id, String sourceParent) {
        String override = containerParents.get(id);
        return override == null ? sourceParent : "screen".equals(override) ? null : override;
    }

    public void setContainerParent(String id, String parent) {
        String qualified = WidgetIds.id(id, "anchorui");
        String target = "screen".equals(parent) ? "screen" : WidgetIds.id(parent, "anchorui");
        if (qualified.equals(target)) {
            throw new IllegalArgumentException("Widget cannot contain itself");
        }
        containerParents.put(qualified, target);
    }

    public void clearContainerParent(String id) {
        containerParents.remove(WidgetIds.id(id, "anchorui"));
    }

    public boolean hasContainerParentOverride(String id) {
        return containerParents.containsKey(id);
    }



    public JsonElement settingOverride(String id, String section, String property) {
        JsonObject overrides = componentSettings.get(id);
        if (overrides == null || !overrides.has(section) || !overrides.get(section).isJsonObject()) {
            return null;
        }
        JsonElement value = overrides.getAsJsonObject(section).get(property);
        return value == null ? null : value.deepCopy();
    }

    public void setSettingOverride(String id, String section, String property, JsonElement value) {
        if (!"layout".equals(section) && !"layoutItem".equals(section) && !"appearance".equals(section)) {
            throw new IllegalArgumentException("Unsupported editor override section: " + section);
        }
        boolean binding = "appearance".equals(section) && "text".equals(property) && value != null && value.isJsonObject() && value.getAsJsonObject().has("bind");
        if (value == null || value.isJsonNull() || (!value.isJsonPrimitive() && !binding)) {
            throw new IllegalArgumentException("Editor component overrides must be primitive values or text bindings");
        }
        if ("appearance".equals(section) && !property.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*){0,5}")) {
            throw new IllegalArgumentException("Invalid appearance property path");
        }
        String qualified = WidgetIds.id(id, "anchorui");
        JsonObject overrides = componentSettings.computeIfAbsent(qualified, ignored -> new JsonObject());
        JsonObject values = overrides.has(section) ? overrides.getAsJsonObject(section) : new JsonObject();
        values.add(property, value.deepCopy());
        overrides.add(section, values);
    }

    public void clearSettingOverride(String id, String section, String property) {
        JsonObject overrides = componentSettings.get(id);
        if (overrides == null || !overrides.has(section)) {
            return;
        }
        JsonObject values = overrides.getAsJsonObject(section);
        values.remove(property);
        if (values.isEmpty()) {
            overrides.remove(section);
        }
        if (overrides.isEmpty()) {
            componentSettings.remove(id);
        }
    }

    public boolean hasSettingOverrides(String id) {
        return componentSettings.containsKey(id);
    }

    public boolean hasAppearanceOverrides(String id) {
        JsonObject settings = componentSettings.get(id);
        return settings != null && (settings.has("appearance") || settings.has("animations"));
    }

    public JsonArray animationOverride(String id) {
        JsonObject settings = componentSettings.get(id);
        return settings != null && settings.has("animations") ? settings.getAsJsonArray("animations").deepCopy() : null;
    }

    public void setAnimationOverride(String id, JsonArray tracks) {
        JsonObject check = new JsonObject();
        check.add("animations", tracks);
        UiAnimationSpec.parse(check);
        String qualified = WidgetIds.id(id, "anchorui");
        JsonObject settings = componentSettings.computeIfAbsent(qualified, ignored -> new JsonObject());
        settings.add("animations", tracks.deepCopy());
    }

    public void clearAnimationOverride(String id) {
        JsonObject settings = componentSettings.get(id);
        if (settings != null) {
            settings.remove("animations");
            if (settings.isEmpty()) {
                componentSettings.remove(id);
            }
        }
    }

    public JsonObject widgetSettings(String id, JsonObject original) {
        JsonObject result = original.deepCopy();
        JsonObject settings = componentSettings.get(id);
        if (settings == null) {
            return result;
        }
        if (settings.has("animations")) {
            result.add("animations", settings.getAsJsonArray("animations").deepCopy());
        }
        if (!settings.has("appearance")) {
            return result;
        }
        var type = WidgetTypeIds.parse(result.get("type").getAsString());
        for (var entry : settings.getAsJsonObject("appearance").entrySet()) {
            WidgetEditorRegistry.Field field = WidgetEditorRegistry.find(type, entry.getKey());
            if (field == null) {
                continue;
            }
            JsonElement validated;
            try {
                if (entry.getValue().isJsonObject() && !WidgetTypeIds.TEXT.equals(type)) {
                    continue;
                }
                validated = AppearanceInspectorControls.parseEditorValue(field, AppearanceInspectorControls.displayValue(entry.getValue()));
            } catch (RuntimeException exception) {
                continue;
            }
            String[] segments = entry.getKey().split("\\.");
            JsonObject parent = result;
            for (int index = 0; index < segments.length - 1; index++) {
                String segment = segments[index];
                if (!parent.has(segment) || !parent.get(segment).isJsonObject()) {
                    parent.add(segment, new JsonObject());
                }
                parent = parent.getAsJsonObject(segment);
            }
            parent.add(segments[segments.length - 1], validated);
        }
        return result;
    }

    public JsonObject effectiveSettings(String id, JsonObject base) {
        JsonObject result = base.deepCopy();
        JsonObject overrides = componentSettings.get(id);
        if (overrides == null) {
            return result;
        }
        for (var entry : overrides.entrySet()) {
            if ("appearance".equals(entry.getKey()) || "animations".equals(entry.getKey())) {
                continue;
            }
            JsonObject section = new JsonObject();
            JsonElement existing = result.get(entry.getKey());
            if (existing != null && existing.isJsonObject()) {
                section = existing.getAsJsonObject().deepCopy();
            } else if (existing != null && existing.isJsonPrimitive()) {
                if ("layout".equals(entry.getKey()) && existing.getAsJsonPrimitive().isString()) {
                    section.add("type", existing.deepCopy());
                } else if ("layoutItem".equals(entry.getKey()) && existing.getAsJsonPrimitive().isBoolean()) {
                    section.add("enabled", existing.deepCopy());
                }
            }
            for (var property : entry.getValue().getAsJsonObject().entrySet()) {
                section.add(property.getKey(), property.getValue().deepCopy());
            }
            result.add(entry.getKey(), section);
        }
        return result;
    }


    public void set(String id, int x, int y) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (x == 0 && y == 0) {
            offsets.remove(qualified);
        } else {
            offsets.put(qualified, new Offset(x, y));
        }
    }

    public void setAnchor(String id, AnchorOverride anchor) {
        String qualified = WidgetIds.id(id, "anchorui");
        List<PositionElement.Constraint> list = anchorLists.get(qualified);
        if (list != null) {
            anchorLists.put(qualified, new PositionElement(list).withPrimary(anchor.point(), anchor.anchor(), anchor.relativeTo()).anchors);
        } else {
            anchors.put(qualified, anchor);
        }
    }

    public void clearAnchor(String id) {
        String qualified = WidgetIds.id(id, "anchorui");
        anchors.remove(qualified);
        anchorLists.remove(qualified);
    }

    public void setHidden(String id, boolean value) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (value) {
            hidden.add(qualified);
        } else {
            hidden.remove(qualified);
        }
    }

    public void reset(String id) {
        offsets.remove(id);
        anchors.remove(id);
        anchorLists.remove(id);
        containerParents.remove(id);
        hidden.remove(id);
        componentSettings.remove(id);
    }

    public void resetAll() {
        createdWidgets.clear();
        offsets.clear();
        anchors.clear();
        anchorLists.clear();
        containerParents.clear();
        hidden.clear();
        componentSettings.clear();
    }

    public Map<String, Offset> entries() {
        return Collections.unmodifiableMap(offsets);
    }

    public HudLayoutOffsets copy() {
        HudLayoutOffsets result = new HudLayoutOffsets();
        result.offsets.putAll(offsets);
        result.anchors.putAll(anchors);
        result.anchorLists.putAll(anchorLists);
        result.containerParents.putAll(containerParents);
        result.hidden.addAll(hidden);
        for (var entry : createdWidgets.entrySet()) {
            result.createdWidgets.put(entry.getKey(), entry.getValue().deepCopy());
        }
        for (var entry : componentSettings.entrySet()) {
            result.componentSettings.put(entry.getKey(), entry.getValue().deepCopy());
        }
        return result;
    }

    /** Keep only the state belonging to the specified HUD widget IDs. */
    public HudLayoutOffsets onlyIds(Set<String> ids) {
        HudLayoutOffsets result = new HudLayoutOffsets();
        for (String id : ids) {
            if (offsets.containsKey(id)) {
                result.offsets.put(id, offsets.get(id));
            }
            if (anchors.containsKey(id)) {
                result.anchors.put(id, anchors.get(id));
            }
            if (anchorLists.containsKey(id)) {
                result.anchorLists.put(id, List.copyOf(anchorLists.get(id)));
            }
            if (containerParents.containsKey(id)) {
                result.containerParents.put(id, containerParents.get(id));
            }
            if (hidden.contains(id)) {
                result.hidden.add(id);
            }
            if (componentSettings.containsKey(id)) {
                result.componentSettings.put(id, componentSettings.get(id).deepCopy());
            }
            if (createdWidgets.containsKey(id)) {
                result.createdWidgets.put(id, createdWidgets.get(id).deepCopy());
            }
        }
        return result;
    }

    /** Apply one HUD's saved state over the legacy shared layout, preserving other HUDs. */
    public void mergeFrom(HudLayoutOffsets other) {
        offsets.putAll(other.offsets);
        anchors.putAll(other.anchors);
        anchorLists.putAll(other.anchorLists);
        containerParents.putAll(other.containerParents);
        hidden.addAll(other.hidden);
        for (var entry : other.componentSettings.entrySet()) {
            componentSettings.put(entry.getKey(), entry.getValue().deepCopy());
        }
        for (var entry : other.createdWidgets.entrySet()) {
            createdWidgets.put(entry.getKey(), entry.getValue().deepCopy());
        }
    }

    public Set<String> allWidgetIds() {
        Set<String> ids = new TreeSet<>(offsets.keySet());
        ids.addAll(anchors.keySet());
        ids.addAll(anchorLists.keySet());
        ids.addAll(containerParents.keySet());
        ids.addAll(hidden);
        ids.addAll(componentSettings.keySet());
        ids.addAll(createdWidgets.keySet());
        return Collections.unmodifiableSet(ids);
    }

    public boolean isEmpty() {
        return allWidgetIds().isEmpty();
    }

    public boolean sameAs(HudLayoutOffsets other) {
        return offsets.equals(other.offsets) && anchors.equals(other.anchors) && anchorLists.equals(other.anchorLists)
                && containerParents.equals(other.containerParents) && hidden.equals(other.hidden)
                && componentSettings.equals(other.componentSettings) && createdWidgets.equals(other.createdWidgets);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject values = new JsonObject();
        Set<String> ids = new TreeSet<>();
        ids.addAll(offsets.keySet());
        ids.addAll(anchors.keySet());
        ids.addAll(anchorLists.keySet());
        ids.addAll(containerParents.keySet());
        ids.addAll(hidden);
        ids.addAll(componentSettings.keySet());
        for (String id : ids) {
            JsonObject value = new JsonObject();
            Offset offset = get(id);
            if (!Offset.ZERO.equals(offset)) {
                value.addProperty("x", offset.x());
                value.addProperty("y", offset.y());
            }
            List<PositionElement.Constraint> list = anchorLists.get(id);
            if (list != null) {
                JsonArray array = new JsonArray();
                for (PositionElement.Constraint entry : list) {
                    JsonObject json = new JsonObject();
                    json.addProperty("point", entry.point().name());
                    json.addProperty("anchor", entry.anchor().name());
                    json.addProperty("relativeTo", entry.relativeTo());
                    json.addProperty("x", entry.x());
                    json.addProperty("y", entry.y());
                    array.add(json);
                }
                value.add("anchors", array);
            } else {
                AnchorOverride anchor = anchors.get(id);
                if (anchor != null) {
                    value.addProperty("relativeTo", anchor.relativeTo());
                    value.addProperty("point", anchor.point().name());
                    value.addProperty("anchor", anchor.anchor().name());
                }
            }
            String containerParent = containerParents.get(id);
            if (containerParent != null) {
                value.addProperty("containerParent", containerParent);
            }
            if (hidden.contains(id)) {
                value.addProperty("hidden", true);
            }
            JsonObject component = componentSettings.get(id);
            if (component != null) {
                for (var override : component.entrySet()) {
                    value.add(override.getKey(), override.getValue().deepCopy());
                }
            }
            values.add(id, value);
        }
        root.add("offsets", values);
        if (!createdWidgets.isEmpty()) {
            JsonObject definitions = new JsonObject();
            for (String id : new TreeSet<>(createdWidgets.keySet())) {
                definitions.add(id, createdWidgets.get(id).deepCopy());
            }
            root.add("createdWidgets", definitions);
        }
        return root;
    }

    public static HudLayoutOffsets fromJson(JsonObject root) {
        if (root.has("version") && root.get("version").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported Anchor UI HUD layout version");
        }
        HudLayoutOffsets result = new HudLayoutOffsets();
        if (root.has("createdWidgets")) {
            if (!root.get("createdWidgets").isJsonObject()) {
                throw new IllegalArgumentException("Created widgets must be an object");
            }
            for (var entry : root.getAsJsonObject("createdWidgets").entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    throw new IllegalArgumentException("Created widget definition must be an object: " + entry.getKey());
                }
                result.addCreatedWidget(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }
        if (!root.has("offsets")) {
            return result;
        }
        JsonObject positions = root.getAsJsonObject("offsets");
        for (var entry : positions.entrySet()) {
            String id = WidgetIds.id(entry.getKey(), "anchorui");
            JsonObject value = entry.getValue().getAsJsonObject();
            int x = value.has("x") ? value.get("x").getAsInt() : 0;
            int y = value.has("y") ? value.get("y").getAsInt() : 0;
            result.set(id, x, y);
            if (value.has("anchors")) {
                if (!value.get("anchors").isJsonArray()) {
                    throw new IllegalArgumentException("Saved anchors must be an array");
                }
                JsonObject position = new JsonObject();
                position.add("anchors", value.getAsJsonArray("anchors"));
                result.setAnchorConstraints(id, new PositionElement(position).anchors);
            } else if (value.has("relativeTo")) {
                String target = value.get("relativeTo").getAsString();
                ElementAnchor point = ElementAnchor.fromString(value.get("point").getAsString());
                ElementAnchor anchor = ElementAnchor.fromString(value.get("anchor").getAsString());
                result.setAnchor(id, new AnchorOverride(target, point, anchor));
            }
            if (value.has("containerParent")) {
                result.setContainerParent(id, value.get("containerParent").getAsString());
            }
            if (value.has("hidden")) {
                result.setHidden(id, value.get("hidden").getAsBoolean());
            }
            if (value.has("animations")) {
                if (!value.get("animations").isJsonArray()) {
                    throw new IllegalArgumentException("Saved animation override must be an array");
                }
                result.setAnimationOverride(id, value.getAsJsonArray("animations"));
            }
            for (String section : new String[]{"layout", "layoutItem", "appearance"}) {
                if (value.has(section)) {
                    if (!value.get(section).isJsonObject()) {
                        throw new IllegalArgumentException("Editor override '" + section + "' must be an object");
                    }
                    for (var property : value.getAsJsonObject(section).entrySet()) {
                        result.setSettingOverride(id, section, property.getKey(), property.getValue());
                    }
                }
            }
        }
        return result;
    }
}
