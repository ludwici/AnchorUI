package com.ludwici.anchorui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnchorUI {
    public static final String MODID = "anchorui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();

    private AnchorUI() {
    }
}
