package com.ludwici.anchorui.client.gui;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class UiHoverTracker {
    private String hovered;

    public void update(String next, BiConsumer<String, Boolean> transition) {
        if (Objects.equals(hovered, next)) {
            return;
        }
        String previous = hovered;
        hovered = next;
        if (previous != null) {
            transition.accept(previous, false);
        }
        if (next != null) {
            transition.accept(next, true);
        }
    }

    public void reset() {
        hovered = null;
    }
}
