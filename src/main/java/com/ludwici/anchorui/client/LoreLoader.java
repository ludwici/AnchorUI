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

    private Map<ResourceLocation, LoreEntity> data = Map.of();
    private Map<ResourceLocation, LoreStyle> styles;

    public static final LoreLoader INSTANCE = new LoreLoader();

    private LoreLoader() {
        super(GSON, "lore");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, LoreEntity> data = new HashMap<>();
        Map<ResourceLocation, LoreStyle> styles = new HashMap<>();
        for (var entry: object.entrySet()) {
            var key = entry.getKey();
            String path = entry.getKey().getPath();
            String[] parts = path.split("/", 2);
            if (parts.length > 1) {
                if (parts[0].equals("style")) {
                    System.out.println("Detect style file: " + parts[1]);
                    styles.computeIfAbsent(key, s -> new LoreStyle(entry.getValue().getAsJsonObject()));
                } else {
                    System.out.println(key.getNamespace() + ":lore/" + path);
                    data.computeIfAbsent(key, e -> new LoreEntity(entry.getValue().getAsJsonObject()));
                }
            }

        }

        this.data = data;
        this.styles = styles;
    }

    public Map<ResourceLocation, LoreEntity> getData() {
        return data;
    }

    public Map<ResourceLocation, LoreStyle> getStyles() {
        return styles;
    }
}
