package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorWidgetOrderTest {
    @Test
    public void vanillaFirstThenCurrentModThenOtherNamespacesAlphabetically() {
        List<String> ids = List.of(
                "zmod:third", "catelot:teams", "minecraft:hotbar",
                "amod:alpha", "catelot:pokedex", "minecraft:crosshair",
                "zmod:first", "amod:beta"
        );
        assertEquals(List.of(
                "minecraft:crosshair", "minecraft:hotbar",
                "catelot:pokedex", "catelot:teams",
                "amod:alpha", "amod:beta", "zmod:first", "zmod:third"
        ), EditorWidgetOrder.sorted(ids, "catelot"));
    }

    @Test
    public void hudPrioritizesAnchorUiAndEmptyListIsSupported() {
        assertEquals(List.of("minecraft:health", "anchorui:status", "anothermod:abc"),
                EditorWidgetOrder.sorted(List.of("anothermod:abc", "anchorui:status", "minecraft:health"), "anchorui"));
        assertTrue(EditorWidgetOrder.sorted(List.of(), "anchorui").isEmpty());
    }

    @Test
    public void screenListIncludesOnlyIdsSuppliedByThatScreen() {
        assertEquals(List.of("mypack:alpha", "mypack:beta", "xmod:component"),
                EditorWidgetOrder.sorted(List.of("xmod:component", "mypack:beta", "mypack:alpha"), "mypack"));
    }

    @Test
    public void currentNamespaceDoesNotOutrankMinecraft() {
        assertEquals(0, EditorWidgetOrder.group("minecraft:hotbar", "minecraft"));
        assertEquals(1, EditorWidgetOrder.group("example:status", "example"));
        assertEquals(2, EditorWidgetOrder.group("other:status", "example"));
    }
}
