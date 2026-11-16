package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetDefinitionRegistry;
import com.ludwici.anchorui.api.widget.WidgetDocumentResolver;
import com.ludwici.anchorui.api.widget.WidgetIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a complete, isolated resource snapshot before the reload listener publishes it.
 * A broken resource is reported without preventing unrelated screens and HUDs from loading.
 */
public final class LoreResourceCompiler {
    public enum Severity {
        WARNING, ERROR
    }

    public record Issue(Severity severity, ResourceLocation resource, String message, Throwable cause) {
        public String display() {
            return resource + ": " + message;
        }

        public boolean appliesToScreen(ResourceLocation screenId) {
            return resource.equals(screenId) || sharedResource();
        }

        public boolean appliesToHud() {
            return resource.getPath().startsWith("hud/") || sharedResource();
        }

        private boolean sharedResource() {
            String path = resource.getPath();
            return path.startsWith("style/") || path.startsWith("widgets/") || path.startsWith("templates/");
        }
    }

    public record Snapshot(Map<ResourceLocation, LoreEntity> screens, Map<ResourceLocation, LoreStyle> styles,
                           WidgetDefinitionRegistry definitions, Map<ResourceLocation, LoreLoader.HudDocument> huds,
                           List<Issue> issues) {
        public Snapshot {
            screens = Map.copyOf(screens);
            styles = Map.copyOf(styles);
            huds = Map.copyOf(huds);
            issues = List.copyOf(issues);
        }
    }

    private LoreResourceCompiler() {
    }

    public static Snapshot empty() {
        return new Snapshot(Map.of(), Map.of(), new WidgetDefinitionRegistry(Map.of()), Map.of(), List.of());
    }

    public static Snapshot compile(Map<ResourceLocation, JsonElement> objects) {
        Map<ResourceLocation, JsonObject> rawDocuments = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawDefinitions = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawTemplates = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawHuds = new LinkedHashMap<>();
        Map<ResourceLocation, LoreStyle> loadedStyles = new LinkedHashMap<>();
        List<Issue> issues = new ArrayList<>();

        for (var entry : objects.entrySet()) {
            ResourceLocation resource = entry.getKey();
            JsonElement raw = entry.getValue();
            if (raw == null || !raw.isJsonObject()) {
                issues.add(new Issue(Severity.ERROR, resource, "Expected a JSON object at the resource root", null));
                continue;
            }
            JsonObject json = raw.getAsJsonObject();
            try {
                String path = resource.getPath();
                if (path.startsWith("style/")) {
                    if (json.has("anchorui:option") && !json.get("anchorui:option").isJsonObject()) {
                        throw new IllegalArgumentException("Style 'anchorui:option' must be a JSON object");
                    }
                    loadedStyles.put(resource, new LoreStyle(json));
                } else if (path.startsWith("widgets/") || path.startsWith("templates/")) {
                    boolean template = path.startsWith("templates/");
                    String prefix = template ? "templates/" : "widgets/";
                    String definitionPath = path.substring(prefix.length());
                    if (definitionPath.isBlank()) {
                        throw new IllegalArgumentException("Empty widget " + (template ? "template" : "definition") + " path");
                    }
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), definitionPath);
                    validateDefinition(id, json);
                    (template ? rawTemplates : rawDefinitions).put(id, json);
                } else if (path.startsWith("hud/")) {
                    rawHuds.put(resource, json);
                } else {
                    rawDocuments.put(resource, json);
                }
            } catch (RuntimeException exception) {
                issues.add(new Issue(Severity.ERROR, resource, "Invalid resource: " + exception.getMessage(), exception));
            }
        }

        for (var entry : rawTemplates.entrySet()) {
            if (rawDefinitions.put(entry.getKey(), entry.getValue()) != null) {
                issues.add(new Issue(Severity.WARNING, entry.getKey(), "Template from templates/ overrides a definition from widgets/", null));
            }
        }
        WidgetDefinitionRegistry definitions = new WidgetDefinitionRegistry(rawDefinitions);
        Map<String, JsonObject> available = definitions.exportForResolution();
        Map<ResourceLocation, LoreEntity> screens = new LinkedHashMap<>();
        for (var entry : rawDocuments.entrySet()) {
            ResourceLocation resource = entry.getKey();
            try {
                JsonObject expanded = WidgetDocumentResolver.resolve(entry.getValue(), resource.getNamespace(), available, warning -> issues.add(new Issue(Severity.WARNING, resource, warning, null)));
                screens.put(resource, new LoreEntity(expanded, resource.getNamespace()));
            } catch (RuntimeException exception) {
                issues.add(new Issue(Severity.ERROR, resource, "Failed to load screen: " + exception.getMessage(), exception));
            }
        }
        Map<ResourceLocation, LoreLoader.HudDocument> huds = new LinkedHashMap<>();
        for (var entry : rawHuds.entrySet()) {
            ResourceLocation resource = entry.getKey();
            try {
                JsonObject expanded = WidgetDocumentResolver.resolve(entry.getValue(), resource.getNamespace(), available, warning -> issues.add(new Issue(Severity.WARNING, resource, warning, null)));
                boolean testOnly = GsonHelper.getAsBoolean(entry.getValue(), "testOnly", false);
                String testGroup = GsonHelper.getAsString(entry.getValue(), "testGroup", "hud1");
                huds.put(resource, new LoreLoader.HudDocument(new LoreEntity(expanded, resource.getNamespace()), testOnly, testGroup));
            } catch (RuntimeException exception) {
                issues.add(new Issue(Severity.ERROR, resource, "Failed to load HUD: " + exception.getMessage(), exception));
            }
        }
        return new Snapshot(screens, loadedStyles, definitions, huds, issues);
    }

    private static void validateDefinition(ResourceLocation id, JsonObject definition) {
        boolean hasType = definition.has("type");
        boolean hasReference = definition.has("ref");
        if (!hasType && !hasReference) {
            throw new IllegalArgumentException("Widget definition '" + id + "' requires 'type' or 'ref'");
        }
        if (hasType && (!definition.get("type").isJsonPrimitive() || !definition.get("type").getAsJsonPrimitive().isString() || definition.get("type").getAsString().isBlank())) {
            throw new IllegalArgumentException("Widget definition '" + id + "' requires a non-empty string 'type'");
        }
        if (hasReference) {
            JsonElement reference = definition.get("ref");
            if (!reference.isJsonPrimitive() || !reference.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Widget definition '" + id + "' requires a string 'ref'");
            }
            WidgetIds.id(reference.getAsString(), id.getNamespace());
        }
    }
}
