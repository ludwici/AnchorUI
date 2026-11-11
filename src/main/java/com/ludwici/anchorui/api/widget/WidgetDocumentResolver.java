package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.layout.WidgetContainers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class WidgetDocumentResolver {
    private WidgetDocumentResolver() {
    }

    public static JsonObject resolve(JsonObject document, String namespace, Map<String, JsonObject> templates) {
        return resolve(document, namespace, templates, null);
    }

    public static JsonObject resolve(JsonObject document, String namespace, Map<String, JsonObject> templates, Consumer<String> warningSink) {
        JsonObject resolved = document.deepCopy();
        LinkedHashMap<String, JsonObject> elements = new LinkedHashMap<>();
        JsonArray imports = array(document, "imports");
        for (JsonElement value : imports) {
            String id = WidgetIds.id(value.getAsString(), namespace);
            try {
                addNested(elements, template(id, templates, new ArrayList<>()), namespace, templates, warningSink);
            } catch (UnknownWidgetDefinitionException exception) {
                if (warningSink == null) {
                    throw exception;
                }
                warningSink.accept(exception.getMessage());
            }
        }
        JsonArray inline = array(document, "entities");
        int index = 0;
        for (JsonElement raw : inline) {
            JsonObject element = raw.getAsJsonObject().deepCopy();
            JsonObject output;
            if (element.has("ref")) {
                String referenced = WidgetIds.id(element.get("ref").getAsString(), namespace);
                element.remove("ref");
                normalizePosition(element, namespace);
                try {
                    output = merge(template(referenced, templates, new ArrayList<>()), element);
                } catch (UnknownWidgetDefinitionException exception) {
                    if (warningSink == null) {
                        throw exception;
                    }
                    warningSink.accept(exception.getMessage());
                    index++;
                    continue;
                }
                if (!element.has("id") || element.get("id").getAsString().isBlank()) {
                    output.addProperty("id", referenced);
                }
            } else {
                normalizePosition(element, namespace);
                output = element;
                if (!output.has("id") || output.get("id").getAsString().isBlank()) {
                    String type = output.get("type").getAsString();
                    String name = type.substring(type.lastIndexOf(':') + 1).replace('/', '_') + "_" + index;
                    output.addProperty("id", name);
                }
            }
            output.addProperty("id", WidgetIds.id(output.get("id").getAsString(), namespace));
            addNested(elements, output, namespace, templates, warningSink);
            index++;
        }
        JsonArray array = new JsonArray();
        for (JsonObject element : elements.values()) {
            array.add(element);
        }
        if (document.has("entities") || document.has("imports")) {
            resolved.add("entities", array);
        }
        resolved.remove("imports");
        return resolved;
    }

    private static void addNested(Map<String, JsonObject> elements, JsonObject original, String namespace, Map<String, JsonObject> templates, Consumer<String> warningSink) {
        JsonArray children = array(original, "children");
        JsonObject parent = original.deepCopy();
        parent.remove("children");
        String id = parent.get("id").getAsString();
        if (!children.isEmpty() && !WidgetContainers.supportsChildren(WidgetTypeIds.parse(parent.get("type").getAsString()))) {
            throw new IllegalArgumentException("Widget '" + id + "' cannot contain children; use anchorui:panel or anchorui:container");
        }
        add(elements, parent);

        int index = 0;
        for (JsonElement raw : children) {
            JsonObject child = raw.getAsJsonObject().deepCopy();
            String childNamespace = WidgetIds.namespace(id);
            boolean ownId = child.has("id") && !child.get("id").getAsString().isBlank();
            if (child.has("ref")) {
                String templateId = WidgetIds.id(child.get("ref").getAsString(), childNamespace);
                child.remove("ref");
                normalizePosition(child, childNamespace);
                try {
                    child = merge(template(templateId, templates, new ArrayList<>()), child);
                } catch (UnknownWidgetDefinitionException exception) {
                    if (warningSink == null) {
                        throw exception;
                    }
                    warningSink.accept(exception.getMessage());
                    index++;
                    continue;
                }
            }
            if (!ownId) {
                child.addProperty("id", id.substring(id.indexOf(':') + 1) + "/child_" + index);
            }
            child.addProperty("id", WidgetIds.id(child.get("id").getAsString(), childNamespace));
            normalizePosition(child, childNamespace);
            JsonObject position = child.has("position") && child.get("position").isJsonObject() ? child.getAsJsonObject("position") : new JsonObject();
            if (!position.has("relativeTo")) {
                position.addProperty("relativeTo", WidgetContainers.contentId(id));
            }
            child.add("position", position);
            child.addProperty(WidgetContainers.PARENT_FIELD, id);
            addNested(elements, child, childNamespace, templates, warningSink);
            index++;
        }
    }

    public static final class UnknownWidgetDefinitionException extends IllegalArgumentException {
        public UnknownWidgetDefinitionException(String id) {
            super("Unknown widget definition: " + id);
        }
    }

    private static JsonObject template(String id, Map<String, JsonObject> templates, List<String> path) {
        JsonObject source = templates.get(id);
        if (source == null) {
            throw new UnknownWidgetDefinitionException(id);
        }
        if (path.contains(id)) {
            path.add(id);
            throw new IllegalArgumentException("Circular widget definitions: " + String.join(" -> ", path));
        }
        path.add(id);
        JsonObject result = source.deepCopy();
        if (result.has("ref")) {
            String parent = WidgetIds.id(result.get("ref").getAsString(), WidgetIds.namespace(id));
            result.remove("ref");
            normalizePosition(result, WidgetIds.namespace(id));
            result = merge(template(parent, templates, path), result);
        } else {
            normalizePosition(result, WidgetIds.namespace(id));
        }
        result.addProperty("id", id);
        path.remove(path.size() - 1);
        return result;
    }

    private static void normalizePosition(JsonObject entry, String namespace) {
        if (!entry.has("position") || !entry.get("position").isJsonObject()) {
            return;
        }
        JsonObject position = entry.getAsJsonObject("position");
        if (position.has("relativeTo")) {
            position.addProperty("relativeTo", WidgetIds.reference(position.get("relativeTo").getAsString(), namespace));
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (!root.has(key)) {
            return new JsonArray();
        }
        if (!root.get(key).isJsonArray()) {
            throw new IllegalArgumentException("Expected array for '" + key + "'");
        }
        return root.getAsJsonArray(key);
    }

    private static JsonObject merge(JsonObject base, JsonObject overrides) {
        JsonObject result = base.deepCopy();
        for (var entry : overrides.entrySet()) {
            JsonElement previous = result.get(entry.getKey());
            JsonElement value = entry.getValue();
            if (previous != null && previous.isJsonObject() && value.isJsonObject()) {
                result.add(entry.getKey(), merge(previous.getAsJsonObject(), value.getAsJsonObject()));
            } else {
                result.add(entry.getKey(), value.deepCopy());
            }
        }
        return result;
    }

    private static void add(Map<String, JsonObject> map, JsonObject entry) {
        String id = entry.get("id").getAsString();
        if (map.putIfAbsent(id, entry) != null) {
            throw new IllegalArgumentException("Duplicate widget id: " + id);
        }
    }
}
