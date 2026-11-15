package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorWidgetOrderTest {
    @Test
    public void vanillaFirstThenCurrentModThenOtherNamespacesAlphabetically() {
        List<String> ids = List.of("zmod:third", "catelot:teams", "minecraft:hotbar", "amod:alpha", "catelot:pokedex", "minecraft:crosshair", "zmod:first", "amod:beta");
        assertEquals(List.of("minecraft:crosshair", "minecraft:hotbar", "catelot:pokedex", "catelot:teams", "amod:alpha", "amod:beta", "zmod:first", "zmod:third"), EditorWidgetOrder.sorted(ids, "catelot"));
    }

    @Test
    public void hudPrioritizesAnchorUiAndEmptyListIsSupported() {
        assertEquals(List.of("minecraft:health", "anchorui:status", "anothermod:abc"), EditorWidgetOrder.sorted(List.of("anothermod:abc", "anchorui:status", "minecraft:health"), "anchorui"));
        assertTrue(EditorWidgetOrder.sorted(List.of(), "anchorui").isEmpty());
    }

    @Test
    public void screenListIncludesOnlyIdsSuppliedByThatScreen() {
        assertEquals(List.of("mypack:alpha", "mypack:beta", "xmod:component"), EditorWidgetOrder.sorted(List.of("xmod:component", "mypack:beta", "mypack:alpha"), "mypack"));
    }

    @Test
    public void currentNamespaceDoesNotOutrankMinecraft() {
        assertEquals(0, EditorWidgetOrder.group("minecraft:hotbar", "minecraft"));
        assertEquals(1, EditorWidgetOrder.group("example:status", "example"));
        assertEquals(2, EditorWidgetOrder.group("other:status", "example"));
    }

    @Test
    public void hierarchyKeepsChildrenBelowContainersAndRespectsNamespaceGroups() {
        List<String> ids = List.of("mypack:z_child", "othermod:foreign", "mypack:a_parent", "mypack:c_child", "mypack:nested", "mypack:root_other");
        Map<String, String> parents = Map.of("mypack:z_child", "mypack:a_parent", "mypack:c_child", "mypack:nested", "mypack:nested", "mypack:a_parent", "othermod:foreign", "mypack:a_parent");
        var nodes = EditorWidgetOrder.hierarchy(ids, "mypack", parents, Set.of());
        assertEquals(List.of("mypack:a_parent", "mypack:nested", "mypack:c_child", "mypack:z_child", "mypack:root_other", "othermod:foreign"), nodes.stream().map(EditorWidgetOrder.Node::id).toList());
        assertEquals(List.of(0, 1, 2, 1, 0, 0), nodes.stream().map(EditorWidgetOrder.Node::depth).toList());
        assertEquals(true, nodes.get(0).hasChildren());
        assertEquals(true, nodes.get(1).hasChildren());
        assertEquals(false, nodes.get(3).hasChildren());
    }

    @Test
    public void collapsedContainerHidesOnlyDescendantsAndCyclesDoNotLoop() {
        List<String> ids = List.of("mypack:root", "mypack:a", "mypack:b", "mypack:independent");
        Map<String, String> parents = Map.of("mypack:a", "mypack:root", "mypack:b", "mypack:a");
        var collapsed = EditorWidgetOrder.hierarchy(ids, "mypack", parents, Set.of("mypack:root"));
        assertEquals(List.of("mypack:independent", "mypack:root"), collapsed.stream().map(EditorWidgetOrder.Node::id).toList());

        var circular = EditorWidgetOrder.hierarchy(List.of("mypack:a", "mypack:b"), "mypack", Map.of("mypack:a", "mypack:b", "mypack:b", "mypack:a"), Set.of());
        assertEquals(2, circular.size());
        assertEquals(Set.of("mypack:a", "mypack:b"), Set.copyOf(circular.stream().map(EditorWidgetOrder.Node::id).toList()));
    }
}
