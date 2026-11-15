package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.condition.ConditionExpression;

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
        LinkedHashMap<String, String> widgetParents = new LinkedHashMap<>();
        JsonArray imports = array(document, "imports");
        for (JsonElement value : imports) {
            String id = WidgetIds.id(value.getAsString(), namespace);
            try {
                addNested(elements, widgetParents, template(id, templates, new ArrayList<>()), namespace, templates, warningSink);
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
            addNested(elements, widgetParents, output, namespace, templates, warningSink);
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
        if (!widgetParents.isEmpty()) {
            JsonObject hierarchy = new JsonObject();
            for (var entry : widgetParents.entrySet()) {
                hierarchy.addProperty(entry.getKey(), entry.getValue());
            }
            resolved.add("widgetParents", hierarchy);
        }
        return resolved;
    }

    private static void addNested(Map<String, JsonObject> elements, Map<String, String> widgetParents,
                                  JsonObject original, String namespace, Map<String, JsonObject> templates,
                                  Consumer<String> warningSink) {
        addNested(elements, widgetParents, original, namespace, templates, warningSink, 0, null);
    }

    private static void addNested(Map<String, JsonObject> elements, Map<String, String> widgetParents,
                                  JsonObject original, String namespace, Map<String, JsonObject> templates,
                                  Consumer<String> warningSink, int caseDepth, String parentId) {
        if ("anchorui:cases".equals(original.get("type").getAsString())) {
            expandCases(elements, widgetParents, original, templates, warningSink, caseDepth, parentId);
            return;
        }
        JsonArray children = array(original, "children");
        JsonObject parent = original.deepCopy();
        parent.remove("children");
        String id = parent.get("id").getAsString();
        if (!children.isEmpty() && !WidgetContainers.supportsChildren(parent.get("type").getAsString())) {
            throw new IllegalArgumentException("Widget '" + id + "' cannot contain children; use anchorui:panel");
        }
        add(elements, parent);
        if (parentId != null) {
            widgetParents.put(id, parentId);
        }

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
                position.addProperty("relativeTo", id);
            }
            child.add("position", position);
            addNested(elements, widgetParents, child, childNamespace, templates, warningSink, caseDepth, id);
            index++;
        }
    }

    private static void expandCases(Map<String, JsonObject> elements, Map<String, String> widgetParents,
                                    JsonObject original, Map<String, JsonObject> templates,
                                    Consumer<String> warningSink, int caseDepth, String parentId) {
        String id = original.get("id").getAsString();
        if (caseDepth >= 8) {
            throw new IllegalArgumentException("Conditional widget nesting exceeds 8 at " + id);
        }
        if (original.has("children")) {
            throw new IllegalArgumentException("Conditional widget '" + id + "' must put children inside a case widget");
        }
        JsonArray choices = array(original, "cases");
        if (choices.isEmpty() || choices.size() > 16) {
            throw new IllegalArgumentException("Conditional widget '" + id + "' requires 1..16 cases");
        }
        JsonObject base = original.deepCopy();
        base.remove("type");
        base.remove("cases");
        base.remove("otherwise");
        List<JsonElement> prior = new ArrayList<>();
        for (int index = 0; index < choices.size(); index++) {
            JsonElement choiceElement = choices.get(index);
            if (!choiceElement.isJsonObject()) {
                throw new IllegalArgumentException("Case " + index + " of '" + id + "' must be an object");
            }
            JsonObject choice = choiceElement.getAsJsonObject();
            if (!choice.has("when") || choice.get("when").isJsonNull()) {
                throw new IllegalArgumentException("Case " + index + " of '" + id + "' requires a condition in 'when'");
            }
            if (!choice.has("widget") || !choice.get("widget").isJsonObject()) {
                throw new IllegalArgumentException("Case " + index + " of '" + id + "' requires a widget object");
            }
            JsonElement when = choice.get("when").deepCopy();
            ConditionExpression.parse(when, false, false);
            JsonElement selector = exclusiveCase(prior, when);
            addCaseBranch(elements, widgetParents, base, choice.getAsJsonObject("widget"), selector,
                    id + "/case_" + index, templates, warningSink, caseDepth + 1, parentId);
            prior.add(when);
        }
        if (original.has("otherwise")) {
            JsonElement fallback = original.get("otherwise");
            if (!fallback.isJsonObject()) {
                throw new IllegalArgumentException("Conditional widget '" + id + "' requires an object in 'otherwise'");
            }
            addCaseBranch(elements, widgetParents, base, fallback.getAsJsonObject(), excludePrior(prior),
                    id + "/otherwise", templates, warningSink, caseDepth + 1, parentId);
        }
    }

    private static void addCaseBranch(Map<String, JsonObject> elements, Map<String, String> widgetParents,
                                      JsonObject base, JsonObject rawWidget, JsonElement selector,
                                      String id, Map<String, JsonObject> templates,
                                      Consumer<String> warningSink, int caseDepth, String parentId) {
        String namespace = WidgetIds.namespace(id);
        JsonObject templateWidget = new JsonObject();
        JsonObject widget = rawWidget.deepCopy();
        if (widget.has("ref")) {
            String reference = WidgetIds.id(widget.get("ref").getAsString(), namespace);
            widget.remove("ref");
            try {
                templateWidget = template(reference, templates, new ArrayList<>());
            } catch (UnknownWidgetDefinitionException exception) {
                if (warningSink == null) {
                    throw exception;
                }
                warningSink.accept(exception.getMessage());
                return;
            }
        }
        JsonObject output = merge(merge(templateWidget, base), widget);
        output.addProperty("id", id);
        output.remove("ref");
        output.add("visibleWhen", conjunction(base.get("visibleWhen"), templateWidget.get("visibleWhen"),
                widget.get("visibleWhen"), selector));
        if (base.has("enabledWhen") && (templateWidget.has("enabledWhen") || widget.has("enabledWhen"))) {
            output.add("enabledWhen", conjunction(base.get("enabledWhen"), templateWidget.get("enabledWhen"),
                    widget.get("enabledWhen")));
        }
        if (!output.has("type") || !output.get("type").isJsonPrimitive()) {
            throw new IllegalArgumentException("Conditional widget branch '" + id + "' requires 'type' or 'ref'");
        }
        normalizePosition(output, namespace);
        addNested(elements, widgetParents, output, namespace, templates, warningSink, caseDepth, parentId);
    }

    private static JsonElement exclusiveCase(List<JsonElement> prior, JsonElement condition) {
        if (prior.isEmpty()) {
            return condition.deepCopy();
        }
        return conjunction(excludePrior(prior), condition);
    }

    private static JsonElement excludePrior(List<JsonElement> prior) {
        JsonObject not = new JsonObject();
        if (prior.size() == 1) {
            not.add("not", prior.getFirst().deepCopy());
        } else {
            JsonArray any = new JsonArray();
            for (JsonElement condition : prior) {
                any.add(condition.deepCopy());
            }
            JsonObject group = new JsonObject();
            group.add("any", any);
            not.add("not", group);
        }
        return not;
    }

    private static JsonElement conjunction(JsonElement... values) {
        JsonArray all = new JsonArray();
        for (JsonElement value : values) {
            if (value != null && !value.isJsonNull()) {
                all.add(value.deepCopy());
            }
        }
        if (all.size() == 1) {
            return all.get(0).deepCopy();
        }
        JsonObject group = new JsonObject();
        group.add("all", all);
        return group;
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
