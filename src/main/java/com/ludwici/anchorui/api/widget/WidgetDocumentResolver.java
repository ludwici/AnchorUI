package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
                add(elements, template(id, templates, new ArrayList<>()));
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
            add(elements, output);
            index++;
        }
        JsonArray array = new JsonArray();
        for (JsonObject element : elements.values()) {
            array.add(element);
        }
        resolved.add("entities", array);
        resolved.remove("imports");
        return resolved;
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
