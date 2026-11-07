package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;

public class LoreStyle {

    public abstract static class SizedElement {
        public PositionElement position;
        public PaddingElement padding;

        public SizedElement(JsonObject root) {
            position = new PositionElement(root);
            padding = new PaddingElement(root);
        }
    }

    public static class LoreEntity extends SizedElement {
        public LoreEntity(JsonObject root) {
            super(root);
        }
    }

    public static class TitleEntity extends SizedElement {
        public TitleEntity(JsonObject root) {
            super(root);
        }
    }

    public static class ModelEntity extends SizedElement {
        public ModelEntity(JsonObject root) {
            super(root);
        }
    }

    public LoreEntity lore;
    public TitleEntity title;
    public ModelEntity model;

    public LoreStyle(JsonObject root) {
        lore = new LoreEntity(root);
        title = new TitleEntity(root);
        model = new ModelEntity(root);
    }


}
