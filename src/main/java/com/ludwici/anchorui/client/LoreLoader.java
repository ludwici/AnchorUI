package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.widget.WidgetDefinitionRegistry;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.client.gui.TextureRegion;
import com.ludwici.anchorui.client.gui.TextureImageFormats;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ludwici.anchorui.AnchorUI.GSON;

public class LoreLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    public static final LoreLoader INSTANCE = new LoreLoader();
    private volatile LoreResourceCompiler.Snapshot snapshot = LoreResourceCompiler.empty();
    private volatile long revision;
    private Map<ResourceLocation, JsonElement> resourceObjects = Map.of();
    private final Set<ResourceLocation> warnedMissingStyles = ConcurrentHashMap.newKeySet();

    public record HudDocument(LoreEntity entity, boolean testOnly, String testGroup) {
    }

    private LoreLoader() {
        super(GSON, "anchorui");
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath("anchorui", "resources");
    }

    @Override
    protected synchronized void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        resourceObjects = new java.util.LinkedHashMap<>(objects);
        publishDocuments();
    }

    /** Refresh locally created documents immediately, without forcing a full resource-pack reload. */
    public synchronized void refreshUserDocuments() {
        publishDocuments();
    }

    private void publishDocuments() {
        Map<ResourceLocation, JsonElement> merged = new java.util.LinkedHashMap<>(resourceObjects);
        for (var entry : UserUiStorage.load().entrySet()) {
            if (merged.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                AnchorUI.LOGGER.warn("Local Anchor UI document '{}' conflicts with a resource pack; resource-pack definition takes precedence", entry.getKey());
            }
        }
        LoreResourceCompiler.Snapshot updated = LoreResourceCompiler.compile(merged);
        for (LoreResourceCompiler.Issue issue : updated.issues()) {
            if (issue.severity() == LoreResourceCompiler.Severity.ERROR) {
                if (issue.cause() == null) {
                    AnchorUI.LOGGER.error("Anchor UI resource '{}': {}", issue.resource(), issue.message());
                } else {
                    AnchorUI.LOGGER.error("Anchor UI resource '{}': {}", issue.resource(), issue.message(), issue.cause());
                }
            } else {
                AnchorUI.LOGGER.warn("Anchor UI resource '{}': {}", issue.resource(), issue.message());
            }
        }
        warnedMissingStyles.clear();
        TextureRegion.clearMissingResourceWarnings();
        TextureImageFormats.clear();
        snapshot = updated;
        revision++;
    }

    public LoreStyle getStyle(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key != null && !key.getPath().startsWith("style/")) {
            key = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "style/" + key.getPath());
        }
        LoreStyle style = key == null ? null : snapshot.styles().get(key);
        if (style == null) {
            if (id != null && !id.isBlank() && key != null && warnedMissingStyles.add(key)) {
                AnchorUI.LOGGER.warn("Missing Anchor UI style '{}'; using anchorui:style/default. " + "Expected asset: assets/{}/anchorui/{}.json", key, key.getNamespace(), key.getPath());
            }
            style = snapshot.styles().get(ResourceLocation.fromNamespaceAndPath("anchorui", "style/default"));
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
        LoreStyle style = snapshot.styles().get(key);
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
        return snapshot.screens();
    }

    public WidgetDefinitionRegistry getDefinitions() {
        return snapshot.definitions();
    }

    public Map<ResourceLocation, HudDocument> getHudDocuments() {
        return snapshot.huds();
    }

    public long getRevision() {
        return revision;
    }

    public List<LoreResourceCompiler.Issue> getResourceIssues() {
        return snapshot.issues();
    }

    public Map<ResourceLocation, LoreStyle> getStyles() {
        return snapshot.styles();
    }
}
