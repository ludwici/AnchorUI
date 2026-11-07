package com.ludwici.anchorui.api;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class LoreComponent {
    public String descSource;
    public boolean hidden;
    public List<LorePiece> pieces = new ArrayList<>();

    public LoreComponent() {
        this("", false);
    }

    public LoreComponent(String desc, boolean hidden) {
        this.descSource = desc;
        this.pieces = LoreParser.parse(desc);
        this.hidden = hidden;
    }

    public LoreComponent(JsonObject root) {
        this(
                GsonHelper.getAsString(root, "desc", ""),
                GsonHelper.getAsBoolean(root, "hidden", false)
        );
    }
}
