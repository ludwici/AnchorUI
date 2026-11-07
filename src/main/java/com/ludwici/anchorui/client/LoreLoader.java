package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.widget.WidgetDefinitionRegistry;
import com.ludwici.anchorui.api.widget.WidgetDocumentResolver;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

import static com.ludwici.anchorui.AnchorUI.GSON;

public class LoreLoader extends SimpleJsonResourceReloadListener {
    public static final LoreLoader INSTANCE = new LoreLoader();
    private Map<ResourceLocation, LoreEntity> data = Map.of();
    private Map<ResourceLocation, LoreStyle> styles = Map.of();
    private WidgetDefinitionRegistry definitions = new WidgetDefinitionRegistry(Map.of());
    private volatile Map<ResourceLocation, HudDocument> hudDocuments = Map.of();
    private volatile long revision;

    public record HudDocument(LoreEntity entity, boolean testOnly) {
    }

    private LoreLoader() {
        super(GSON, "lore");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> rawDocuments = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawDefinitions = new LinkedHashMap<>();
        Map<ResourceLocation, JsonObject> rawHuds = new LinkedHashMap<>();
        Map<ResourceLocation, LoreStyle> loadedStyles = new HashMap<>();
        for (var entry : objects.entrySet()) {
            ResourceLocation resource = entry.getKey();
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
            } else if (resource.getPath().startsWith("hud/")) {
                rawHuds.put(resource, json);
            } else {
                rawDocuments.put(resource, json);
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
                loadedHuds.put(resource, new HudDocument(new LoreEntity(expanded, resource.getNamespace()), GsonHelper.getAsBoolean(entry.getValue(), "testOnly", false)));
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
            style = styles.get(ResourceLocation.fromNamespaceAndPath("anchorui", "style/default"));
        }
        return style == null ? new LoreStyle(new com.google.gson.JsonObject()) : style;
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
