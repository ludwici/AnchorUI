package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonElement;

import java.util.function.Consumer;

public interface UiChangeSource {
    void setChangeListener(Consumer<JsonElement> listener);
}
