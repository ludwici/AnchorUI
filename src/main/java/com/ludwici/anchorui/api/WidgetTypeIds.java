package com.ludwici.anchorui.api;

import com.ludwici.anchorui.AnchorUI;
import net.minecraft.resources.ResourceLocation;

public final class WidgetTypeIds {
    public static final ResourceLocation TEXT = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "text");
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "model");
    public static final ResourceLocation LIST = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "list");
    public static final ResourceLocation PROGRESS = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "progress");
    public static final ResourceLocation BUTTON = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "button");
    public static final ResourceLocation CHECKBOX = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "checkbox");
    public static final ResourceLocation SWITCH = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "switch");
    public static final ResourceLocation SELECT = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "select");
    public static final ResourceLocation TABS = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "tabs");
    public static final ResourceLocation COLOR_PICKER = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "color_picker");
    public static final ResourceLocation DATA_LIST = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "data_list");
    public static final ResourceLocation TEMPLATE_LIST = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "template_list");
    public static final ResourceLocation IMAGE = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "image");
    public static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "panel");
    public static final ResourceLocation CONTAINER = ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "container");

    private WidgetTypeIds() {
    }

    public static ResourceLocation parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UI element type must not be empty");
        }
        String qualified = value.indexOf(':') < 0 ? AnchorUI.MODID + ":" + value : value;
        ResourceLocation id = ResourceLocation.tryParse(qualified);
        if (id == null) {
            throw new IllegalArgumentException("Invalid UI element type: " + value);
        }
        return id;
    }
}
