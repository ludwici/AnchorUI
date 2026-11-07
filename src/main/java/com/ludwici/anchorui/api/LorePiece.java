package com.ludwici.anchorui.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.LinkedHashMap;
import java.util.Map;

public class LorePiece {
    public String text;
    public Map<String, Object> tags = new LinkedHashMap<>();
    private Style style;

    public LorePiece(String text, Map<String, Object> tags) {
        this.text = text;
        this.tags.putAll(tags);
        applyStyle();
    }

    public void applyStyle() {
        this.style = Component.literal("").withStyle(style -> {
            style = style.withBold(tags.containsKey("bold"));
            style = style.withItalic(tags.containsKey("italic"));
            style = style.withUnderlined(tags.containsKey("underlined"));
            if (tags.containsKey("t")) {
                if (tags.get("t") instanceof Map<?,?> t && t.get("id") instanceof String tooltip) {
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tooltip)));
                }
            }
            return style;
        }).getStyle();
    }

    public Style getStyle() {
        return style;
    }

    @Override
    public String toString() {
        return "LorePiece{text='" + text + "', tags=" + tags + "}";
    }
}
