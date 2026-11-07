package com.ludwici.anchorui.api.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class BackgroundElement {
    public final int color;
    public final ResourceLocation image;

    public BackgroundElement(JsonObject root) {
        JsonElement value = root.get("color");
        if (value != null && value.isJsonObject()) {
            JsonObject rgba = value.getAsJsonObject();
            color = ((GsonHelper.getAsInt(rgba, "a", 0) & 255) << 24)
                    | ((GsonHelper.getAsInt(rgba, "r", 0) & 255) << 16)
                    | ((GsonHelper.getAsInt(rgba, "g", 0) & 255) << 8)
                    | (GsonHelper.getAsInt(rgba, "b", 0) & 255);
        } else if (value != null && value.isJsonPrimitive()) {
            String hex = value.getAsString().replace("#", "");
            color = hex.length() == 6 ? (int) (0xFF000000L | Long.parseLong(hex, 16)) : (int) Long.parseLong(hex, 16);
        } else {
            color = 0;
        }
        image = ResourceLocation.tryParse(GsonHelper.getAsString(root, "image", ""));
    }
}
