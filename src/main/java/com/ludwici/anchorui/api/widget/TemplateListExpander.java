package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.binding.DataValuePaths;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.layout.TemplateListLayoutRegistry;
import net.minecraft.util.GsonHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands a single declarative row template into ordinary AnchorUI widgets.
 * The expanded widgets use the normal registry, layout, conditions and events.
 */
public final class TemplateListExpander {
    private static final Pattern TEXT_ITEM = Pattern.compile("\\{\\{item(?:#([a-zA-Z0-9_.\\[\\]-]+))?\\}\\}");
    private static final int MAX_ITEMS = 256;

    public record Entry(JsonObject settings, ItemContext itemContext) {
        public Entry {
            settings = settings.deepCopy();
        }

        @Override
        public JsonObject settings() {
            return settings.deepCopy();
        }
    }

    private TemplateListExpander() {
    }

    public static List<Entry> expand(JsonObject config, JsonArray items, String listId, Map<String, JsonObject> templates) {
        JsonElement rawTemplate = config.get("template");
        JsonObject rowTemplate;
        if (rawTemplate != null && rawTemplate.isJsonObject()) {
            rowTemplate = rawTemplate.getAsJsonObject();
        } else if (rawTemplate != null && rawTemplate.isJsonPrimitive()
                && rawTemplate.getAsJsonPrimitive().isString()) {
            rowTemplate = new JsonObject();
            rowTemplate.addProperty("ref", rawTemplate.getAsString());
        } else {
            throw new IllegalArgumentException("template_list requires a widget object or namespaced template ID in 'template'");
        }

        Map<String, JsonObject> resolvedTemplates = new HashMap<>(WidgetTemplateRegistry.definitions());
        resolvedTemplates.putAll(templates);
        int rowHeight = GsonHelper.getAsInt(config, "rowHeight", 22);
        int gap = GsonHelper.getAsInt(config, "gap", 2);
        int maxEntries = GsonHelper.getAsInt(config, "maxEntries", 64);
        if (rowHeight < 1 || rowHeight > 4096 || gap < 0 || gap > 4096) {
            throw new IllegalArgumentException("template_list rowHeight must be 1..4096 and gap must be 0..4096");
        }
        if (maxEntries < 1 || maxEntries > MAX_ITEMS) {
            throw new IllegalArgumentException("template_list maxEntries must be 1.." + MAX_ITEMS);
        }
        TemplateListLayoutRegistry.Layout layout = TemplateListLayoutRegistry.compile(config, rowHeight, gap);

        String keyPath = GsonHelper.getAsString(config, "key", "");
        List<String> keyTokens = DataValuePaths.tokens(keyPath);
        int size = Math.min(maxEntries, items.size());
        String namespace = WidgetIds.namespace(listId);
        List<Entry> result = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>();

        for (int index = 0; index < size; index++) {
            JsonElement item = items.get(index);
            JsonElement keyValue = keyTokens.isEmpty() ? new JsonPrimitive(index) : DataValuePaths.resolve(item, keyTokens);
            if (keyValue == null || !keyValue.isJsonPrimitive() || keyValue.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("template_list key must resolve to a string or number for row " + index);
            }
            String key = keyValue.getAsString();
            if (!usedKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate template_list key: " + key);
            }

            ItemContext context = new ItemContext(item, key, index);
            String rowId = listId + "/entry_" + digest(key);
            JsonElement substituted = substitute(rowTemplate, context);
            JsonObject document = new JsonObject();
            JsonArray entities = new JsonArray();
            entities.add(substituted);
            document.add("entities", entities);
            JsonArray expanded = WidgetDocumentResolver.resolve(document, namespace, resolvedTemplates).getAsJsonArray("entities");
            if (expanded.isEmpty()) {
                continue;
            }
            String rootId = expanded.get(0).getAsJsonObject().get("id").getAsString();
            Map<String, String> ids = new LinkedHashMap<>();
            ids.put(rootId, rowId);
            for (int childIndex = 1; childIndex < expanded.size(); childIndex++) {
                JsonObject widget = expanded.get(childIndex).getAsJsonObject();
                String id = widget.get("id").getAsString();
                ids.put(id, rowId + "/" + id.substring(id.indexOf(':') + 1));
            }

            for (int widgetIndex = 0; widgetIndex < expanded.size(); widgetIndex++) {
                JsonObject widget = substitute(expanded.get(widgetIndex), context).getAsJsonObject();
                String id = widget.get("id").getAsString();
                widget.addProperty("id", ids.get(id));

                JsonObject position = widget.has("position") && widget.get("position").isJsonObject()
                        ? widget.getAsJsonObject("position") : new JsonObject();
                if (widgetIndex == 0) {
                    TemplateListLayoutRegistry.Cell cell = layout.place(index);
                    position.addProperty("relativeTo", WidgetContainers.contentId(listId));
                    int offsetX = GsonHelper.getAsInt(position, "x", 0);
                    int offsetY = GsonHelper.getAsInt(position, "y", 0);
                    position.addProperty("x", addCoordinate(cell.x(), offsetX));
                    position.addProperty("y", addCoordinate(cell.y(), offsetY));
                    if (!widget.has("size") || !widget.get("size").isJsonObject()) {
                        widget.add("size", new JsonObject());
                    }
                    JsonObject sizeObject = widget.getAsJsonObject("size");
                    if (cell.width() > 0) {
                        sizeObject.addProperty("width", cell.width());
                    }
                    if (!sizeObject.has("height") || cell.width() > 0) {
                        sizeObject.addProperty("height", cell.height());
                    }
                    widget.addProperty(WidgetContainers.PARENT_FIELD, listId);
                } else {
                    if (position.has("relativeTo")) {
                        String reference = position.get("relativeTo").getAsString();
                        position.addProperty("relativeTo", remap(reference, ids));
                    }
                    if (widget.has(WidgetContainers.PARENT_FIELD)) {
                        String parent = widget.get(WidgetContainers.PARENT_FIELD).getAsString();
                        widget.addProperty(WidgetContainers.PARENT_FIELD, ids.getOrDefault(parent, parent));
                    }
                }
                widget.add("position", position);
                result.add(new Entry(widget, context));
            }
        }
        return List.copyOf(result);
    }

    private static int addCoordinate(int coordinate, int offset) {
        long result = (long) coordinate + offset;
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Template list coordinate exceeds integer bounds");
        }
        return (int) result;
    }

    private static String remap(String reference, Map<String, String> ids) {
        String value = ids.get(reference);
        if (value != null) {
            return value;
        }
        String suffix = "/__content";
        if (reference.endsWith(suffix)) {
            String parent = reference.substring(0, reference.length() - suffix.length());
            String renamed = ids.get(parent);
            if (renamed != null) {
                return WidgetContainers.contentId(renamed);
            }
        }
        return reference;
    }

    private static JsonElement substitute(JsonElement original, ItemContext context) {
        if (original == null || original.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (original.isJsonObject()) {
            JsonObject object = original.getAsJsonObject();
            if (object.has("item") && (object.size() == 1 || (object.size() == 2 && object.has("fallback")))) {
                JsonElement path = object.get("item");
                if (!path.isJsonPrimitive() || !path.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Template item reference must be a path string");
                }
                JsonElement resolved = context.get(path.getAsString());
                return resolved.isJsonNull() && object.has("fallback")
                        ? object.get("fallback").deepCopy() : resolved;
            }
            JsonObject copy = new JsonObject();
            for (var entry : object.entrySet()) {
                copy.add(entry.getKey(), substitute(entry.getValue(), context));
            }
            return copy;
        }
        if (original.isJsonArray()) {
            JsonArray copy = new JsonArray();
            for (JsonElement value : original.getAsJsonArray()) {
                copy.add(substitute(value, context));
            }
            return copy;
        }
        if (!original.getAsJsonPrimitive().isString()) {
            return original.deepCopy();
        }
        String text = original.getAsString();
        Matcher matcher = TEXT_ITEM.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String path = matcher.group(1) == null ? "" : matcher.group(1);
            JsonElement value = context.get(path);
            String replacement = value.isJsonNull() ? "" : value.isJsonPrimitive() ? value.getAsString() : value.toString();
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement.replace("<", "‹").replace(">", "›")));
        }
        matcher.appendTail(result);
        return new JsonPrimitive(result.toString());
    }

    private static String digest(String key) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(24);
            for (int index = 0; index < 12; index++) {
                value.append(Character.forDigit((bytes[index] >>> 4) & 15, 16));
                value.append(Character.forDigit(bytes[index] & 15, 16));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
