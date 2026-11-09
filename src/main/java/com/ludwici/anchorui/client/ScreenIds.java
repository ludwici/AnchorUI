package com.ludwici.anchorui.client;

import net.minecraft.resources.ResourceLocation;

public final class ScreenIds {
    private static final String PREFIX = "screens/";

    private ScreenIds() {
    }

    public static ResourceLocation resolve(String userId) {
        if (userId == null || !userId.contains(":")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(userId);
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith(PREFIX)) {
            path = path.substring(PREFIX.length());
        }
        if (path.isBlank() || path.startsWith("/")) {
            return null;
        }
        return ResourceLocation.tryParse(id.getNamespace() + ":" + PREFIX + path);
    }

    public static String display(ResourceLocation resource) {
        String path = resource.getPath();
        if (!path.startsWith(PREFIX)) {
            return resource.toString();
        }
        return resource.getNamespace() + ":" + path.substring(PREFIX.length());
    }
}
