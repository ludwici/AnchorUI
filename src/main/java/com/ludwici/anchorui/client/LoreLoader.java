package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.widget.WidgetDefinitionRegistry;
import com.ludwici.anchorui.api.widget.WidgetDocumentResolver;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.client.gui.TextureRegion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ludwici.anchorui.AnchorUI.GSON;

public class LoreLoader extends SimpleJsonResourceReloadListener {
    public static final LoreLoader INSTANCE = new LoreLoader();
    private Map<ResourceLocation, LoreEntity> data = Map.of();
    private Map<ResourceLocation, LoreStyle> styles = Map.of();
    private WidgetDefinitionRegistry definitions = new WidgetDefinitionRegistry(Map.of());
    private volatile Map<ResourceLocation, HudDocument> hudDocuments = Map.of();
    private volatile long revision;
    private final Set<ResourceLocation> warnedMissingStyles = ConcurrentHashMap.newKeySet();

    public record HudDocument(LoreEntity entity, boolean testOnly, String testGroup) {
    }

    private LoreLoader() {
        super(GSON, "anchorui");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        warnedMissingStyles.clear();
        TextureRegion.clearMissingResourceWarnings();
        Map<ResourceLocation, JsonObject> rawDocuments = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawDefinitions = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawTemplates = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawHuds = new LinkedHashMap<>();
        Map<ResourceLocation, LoreStyle> loadedStyles = new HashMap<>();
        for (var entry : objects.entrySet()) {
            ResourceLocation resource = entry.getKey();
            if (!entry.getValue().isJsonObject()) {
                AnchorUI.LOGGER.error("Invalid Anchor UI resource '{}': expected a JSON object, got {}",
                        resource, entry.getValue());
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            if (resource.getPath().startsWith("style/")) {
                loadedStyles.put(resource, new LoreStyle(json));
            } else if (resource.getPath().startsWith("widgets/")) {
                String path = resource.getPath().substring("widgets/".length());
                if (path.isBlank()) {
                    AnchorUI.LOGGER.warn("Empty widget definition path: {}", resource);
                    continue;
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), path);
                rawDefinitions.put(id, json);
            } else if (resource.getPath().startsWith("templates/")) {
                String path = resource.getPath().substring("templates/".length());
                if (path.isBlank()) {
                    AnchorUI.LOGGER.warn("Empty widget template path: {}", resource);
                    continue;
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), path);
                rawTemplates.put(id, json);
            } else if (resource.getPath().startsWith("hud/")) {
                rawHuds.put(resource, json);
            } else {
                rawDocuments.put(resource, json);
            }
        }
        for (var entry : rawTemplates.entrySet()) {
            if (rawDefinitions.put(entry.getKey(), entry.getValue()) != null) {
                AnchorUI.LOGGER.warn("Widget template '{}' from templates/ overrides a definition from widgets/", entry.getKey());
            }
        }
        WidgetDefinitionRegistry loadedDefinitions = new WidgetDefinitionRegistry(rawDefinitions);
        Map<String, JsonObject> source = loadedDefinitions.exportForResolution();
        Map<ResourceLocation, LoreEntity> loaded = new LinkedHashMap<>();
        for (var entry : rawDocuments.entrySet()) {
            ResourceLocation resource = entry.getKey();
            try {
                JsonObject expanded = WidgetDocumentResolver.resolve(entry.getValue(), resource.getNamespace(), source, warning -> AnchorUI.LOGGER.warn("Anchor UI document '{}': {}", resource, warning));
                loaded.put(resource, new LoreEntity(expanded, resource.getNamespace()));
            } catch (RuntimeException exception) {
                AnchorUI.LOGGER.error("Failed to load Anchor UI document '{}'", resource, exception);
            }
        }
        Map<ResourceLocation, HudDocument> loadedHuds = new LinkedHashMap<>();
        for (var entry : rawHuds.entrySet()) {
            ResourceLocation resource = entry.getKey();
            try {
                JsonObject expanded = WidgetDocumentResolver.resolve(entry.getValue(), resource.getNamespace(), source, warning -> AnchorUI.LOGGER.warn("Anchor UI HUD '{}': {}", resource, warning));
                loadedHuds.put(resource, new HudDocument(new LoreEntity(expanded, resource.getNamespace()), GsonHelper.getAsBoolean(entry.getValue(), "testOnly", false), GsonHelper.getAsString(entry.getValue(), "testGroup", "hud1")));
            } catch (RuntimeException exception) {
                AnchorUI.LOGGER.error("Failed to load Anchor UI HUD '{}'", resource, exception);
            }
        }
        definitions = loadedDefinitions;
        styles = Map.copyOf(loadedStyles);
        data = Map.copyOf(loaded);
        hudDocuments = Map.copyOf(loadedHuds);
        revision++;
    }

    public LoreStyle getStyle(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key != null && !key.getPath().startsWith("style/")) {
            key = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "style/" + key.getPath());
        }
        LoreStyle style = key == null ? null : styles.get(key);
        if (style == null) {
            if (id != null && !id.isBlank() && key != null && warnedMissingStyles.add(key)) {
                AnchorUI.LOGGER.warn("Missing Anchor UI style '{}'; using anchorui:style/default. "
                        + "Expected asset: assets/{}/anchorui/{}.json",
                        key, key.getNamespace(), key.getPath());
            }
            style = styles.get(ResourceLocation.fromNamespaceAndPath("anchorui", "style/default"));
        }
        return style == null ? new LoreStyle(new com.google.gson.JsonObject()) : style;
    }

    public JsonObject getOptionStyle(String id, String defaultNamespace) {
        String qualified = id.contains(":") ? id : defaultNamespace + ":" + id;
        ResourceLocation key = ResourceLocation.tryParse(qualified);
        if (key == null) {
            throw new IllegalArgumentException("Invalid select option style id: " + id);
        }
        if (!key.getPath().startsWith("style/")) {
            key = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "style/" + key.getPath());
        }
        LoreStyle style = styles.get(key);
        if (style == null) {
            throw new IllegalArgumentException("Missing select option style: " + key);
        }
        JsonObject root = style.root();
        if (root.has("anchorui:option")) {
            if (!root.get("anchorui:option").isJsonObject()) {
                throw new IllegalArgumentException("Select option style 'anchorui:option' must be an object: " + key);
            }
            return root.getAsJsonObject("anchorui:option").deepCopy();
        }
        return root;
    }

    public Map<ResourceLocation, LoreEntity> getData() {
        return data;
    }

    public WidgetDefinitionRegistry getDefinitions() {
        return definitions;
    }

    public Map<ResourceLocation, HudDocument> getHudDocuments() {
        return hudDocuments;
    }

    public long getRevision() {
        return revision;
    }

    public Map<ResourceLocation, LoreStyle> getStyles() {
        return styles;
    }
}
