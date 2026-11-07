package com.ludwici.anchorui.client.api.types;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class LoreText extends StyledComponent {
    public String text;
    public int color;

    public LoreText(JsonObject root, String text) {
        super(root);
        this.text = text;
    }

    public LoreText(JsonObject root) {
        this(
                root,
                GsonHelper.getAsString(root, "text", "")
//                GsonHelper.getAsString(root, "color", "")
        );
    }

    @Override
    protected void overrideStyle(JsonObject root) {
        super.overrideStyle(root);
        String color = GsonHelper.getAsString(root, "color", null);
        if (color != null) {
            this.color = Integer.parseInt(color, 16);
        }
    }

    @Override
    protected void updateStyle(JsonObject root) {
        this.color = Integer.parseInt(GsonHelper.getAsString(root, "color"), 16);
    }

    @Override
    public String getType() {
        return "text";
    }
}
