package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

import static com.ludwici.anchorui.AnchorUI.GSON;

public class LoreLoader extends SimpleJsonResourceReloadListener {
    public static final LoreLoader INSTANCE = new LoreLoader();
    private Map<ResourceLocation, LoreEntity> data = Map.of();
    private Map<ResourceLocation, LoreStyle> styles = Map.of();

    private LoreLoader() {
        super(GSON, "lore");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, LoreEntity> loaded = new HashMap<>();
        Map<ResourceLocation, LoreStyle> loadedStyles = new HashMap<>();
        for (var entry : objects.entrySet()) {
            ResourceLocation key = entry.getKey();
            if (key.getPath().startsWith("style/")) {
                loadedStyles.put(key, new LoreStyle(entry.getValue().getAsJsonObject()));
            } else {
                loaded.put(key, new LoreEntity(entry.getValue().getAsJsonObject()));
            }
        }
        data = Map.copyOf(loaded);
        styles = Map.copyOf(loadedStyles);
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

    public Map<ResourceLocation, LoreStyle> getStyles() {
        return styles;
    }
}
