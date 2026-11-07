package com.ludwici.anchorui.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.client.api.LoreComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

import static com.ludwici.anchorui.AnchorUI.GSON;

public class LoreLoader extends SimpleJsonResourceReloadListener {

    private Map<ResourceLocation, LoreComponents> data = Map.of();
    private Map<ResourceLocation, JsonObject> style;

    public static final LoreLoader INSTANCE = new LoreLoader();

    private LoreLoader() {
        super(GSON, "lore");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, LoreComponents> data = new HashMap<>();
        Map<ResourceLocation, JsonObject> rawData = new HashMap<>();
        Map<ResourceLocation, JsonObject> style = new HashMap<>();
        for (var entry : pObject.entrySet()) {
            var key = entry.getKey();
            String path = entry.getKey().getPath();
            String[] parts = path.split("/", 2);
            if (parts.length > 1) {
                if (parts[0].equals("style")) {
                    System.out.println("Detect style file: " + parts[1]);
                    style.computeIfAbsent(key, e -> entry.getValue().getAsJsonObject());
                } else {
                    System.out.println(key.getNamespace() + ":lore/" + parts[1]);
                    rawData.computeIfAbsent(key, e -> entry.getValue().getAsJsonObject());
//                    data.computeIfAbsent(key, e -> new LoreComponents(entry.getValue().getAsJsonObject()));
                }
            }
        }

        this.style = style;
        rawData.forEach((r, j) -> {
            data.computeIfAbsent(r, e -> new LoreComponents(j));
        });

        this.data = data;
    }

    public Map<ResourceLocation, LoreComponents> getData() {
        return data;
    }

    public Map<ResourceLocation, JsonObject> getStyles() {
        return style;
    }
}
