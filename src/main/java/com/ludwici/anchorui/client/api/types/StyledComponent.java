package com.ludwici.anchorui.client.api.types;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.api.types.elements.BackgroundElement;
import com.ludwici.anchorui.client.api.types.elements.PaddingElement;
import com.ludwici.anchorui.client.api.types.elements.PositionElement;
import com.ludwici.anchorui.client.api.types.elements.SizeElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public abstract class StyledComponent {
    public String id;
    public PositionElement position;
    public PaddingElement padding;
    public SizeElement size;
    public BackgroundElement background;
    public boolean hidden;
    public String stylePath;

    public StyledComponent(JsonObject root) {
        id = GsonHelper.getAsString(root, "id", "");
        stylePath = GsonHelper.getAsString(root, "style", "anchorui:style/default");

        var pos = GsonHelper.getAsJsonObject(root, "position", new JsonObject());
        position = new PositionElement(pos);
        padding = new PaddingElement();
        size = new SizeElement();
        background = new BackgroundElement();

        hidden = GsonHelper.getAsBoolean(root, "hidden", false);
        updateStyle();
        overrideStyle(root);
    }

    protected void overrideStyle(JsonObject root) {
        var padd = GsonHelper.getAsJsonObject(root, "padding", null);
        if (padd != null) {
            padding = new PaddingElement(padd);
        }
    }

    public abstract String getType();

    protected void updateStyle(JsonObject root){
    }

    public void updateStyle() {
        var styles = LoreLoader.INSTANCE.getStyles();

        for (var s : styles.entrySet()) {
            var res = ResourceLocation.tryParse(stylePath);
            if (res == null) {
                continue;
            }
            if (s.getKey().compareTo(res) == 0) {
                var json = GsonHelper.getAsJsonObject(s.getValue(), getType());
                padding = new PaddingElement(GsonHelper.getAsJsonObject(json, "padding", new JsonObject()));
                size = new SizeElement(GsonHelper.getAsJsonObject(json, "size", new JsonObject()));
                background = new BackgroundElement(GsonHelper.getAsJsonObject(json, "background", new JsonObject()));
                updateStyle(json);
                break;
            }
        }
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    public int getPaddingLeft() {
        return padding.left;
    }

    public int getPaddingTop() {
        return padding.top;
    }

    public int getPaddingRight() {
        return padding.right;
    }

    public int getPaddingBottom() {
        return padding.bottom;
    }

}
