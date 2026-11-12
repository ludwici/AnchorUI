package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StackLayoutTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static AnchorLayout.Frame frame(String id, String relativeTo, int x, int y, int w, int h) {
        return new AnchorLayout.Frame(id, new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT,
                relativeTo, x, y), w, h);
    }

    private static ContainerLayout.Result resolve(List<AnchorLayout.Frame> frames, Map<String, PaddingElement> padding,
                                                   Map<String, ContainerLayout.AutoSize> autoSizes,
                                                   Map<String, String> parents, Map<String, StackLayout.Config> stacks,
                                                   Set<String> hidden, Set<String> manual) {
        return ContainerLayout.resolve(500, 300, frames, padding, autoSizes, parents, stacks, hidden, manual,
                warning -> {
                    throw new AssertionError(warning);
                });
    }

    @Test
    public void parsesRowAndColumnOptionsAndRejectsInvalidValues() {
        assertNull(StackLayout.Config.parse(json("{}")));
        StackLayout.Config layout = StackLayout.Config.parse(json("""
                {"layout":{"type":"column","gap":8,"align":"center","justify":"space_between",
                 "collapseHidden":true}}
                """));
        assertEquals(StackLayout.Axis.COLUMN, layout.axis());
        assertEquals(8, layout.gap());
        assertEquals(StackLayout.Align.CENTER, layout.align());
        assertEquals(StackLayout.Justify.SPACE_BETWEEN, layout.justify());
        assertEquals(true, layout.collapseHidden());
        assertEquals(StackLayout.Axis.ROW, StackLayout.Config.parse(json("""
                {"layout":"row"}
                """)).axis());
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Config.parse(json("""
                {"layout":{"type":"grid"}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Config.parse(json("""
                {"layout":{"type":"row","gap":-1}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Config.parse(json("""
                {"layout":{"type":"row","align":"random"}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Config.parse(json("""
                {"layout":{"type":"column","justify":"random"}}
                """)));
    }

    @Test
    public void rowDistributesFreeSpaceAndCentersChildrenWithPadding() {
        String parent = "demo:row";
        var layout = StackLayout.Config.parse(json("""
                {"layout":{"type":"row","gap":10,"justify":"space_between","align":"center"}}
                """));
        var result = resolve(List.of(frame(parent, "screen", 10, 20, 220, 70),
                        frame("demo:a", parent + "/__content", 0, 0, 30, 12),
                        frame("demo:b", parent + "/__content", 0, 0, 50, 20)),
                Map.of(parent, new PaddingElement(10, 8, 10, 8)), Map.of(),
                Map.of("demo:a", parent, "demo:b", parent), Map.of(parent, layout), Set.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(20, 49, 30, 12), result.bounds().get("demo:a"));
        assertEquals(new AnchorLayout.Bounds(170, 45, 50, 20), result.bounds().get("demo:b"));
    }

    @Test
    public void columnCentersOrAlignsChildrenAtTheEnd() {
        String parent = "demo:column";
        var layout = StackLayout.Config.parse(json("""
                {"layout":{"type":"column","gap":5,"justify":"end","align":"end"}}
                """));
        var result = resolve(List.of(frame(parent, "screen", 0, 0, 100, 80),
                        frame("demo:a", parent + "/__content", 0, 0, 30, 10),
                        frame("demo:b", parent + "/__content", 0, 0, 40, 15)),
                Map.of(parent, new PaddingElement()), Map.of(),
                Map.of("demo:a", parent, "demo:b", parent), Map.of(parent, layout), Set.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(70, 50, 30, 10), result.bounds().get("demo:a"));
        assertEquals(new AnchorLayout.Bounds(60, 65, 40, 15), result.bounds().get("demo:b"));
    }

    @Test
    public void rowAndColumnAutoSizeMeasureIntrinsicContentInsteadOfOverflow() {
        String row = "demo:row";
        var rowLayout = StackLayout.Config.parse(json("""
                {"layout":{"type":"row","gap":8,"align":"center","justify":"center"}}
                """));
        var result = resolve(List.of(frame(row, "screen", 30, 40, 1, 1),
                        frame("demo:a", row + "/__content", 0, 0, 30, 12),
                        frame("demo:b", row + "/__content", 0, 0, 50, 20)),
                Map.of(row, new PaddingElement(4, 4, 4, 4)),
                Map.of(row, new ContainerLayout.AutoSize(true, true)),
                Map.of("demo:a", row, "demo:b", row), Map.of(row, rowLayout), Set.of(), Set.of());
        assertEquals(new ContainerLayout.Size(96, 28), result.sizes().get(row));
        assertEquals(new AnchorLayout.Bounds(34, 48, 30, 12), result.bounds().get("demo:a"));
        assertEquals(new AnchorLayout.Bounds(72, 44, 50, 20), result.bounds().get("demo:b"));
    }

    @Test
    public void hiddenChildrenCollapseOnlyWhenExplicitlyEnabled() {
        String parent = "demo:list";
        var frames = List.of(frame(parent, "screen", 0, 0, 100, 90),
                frame("demo:a", parent + "/__content", 0, 0, 30, 10),
                frame("demo:b", parent + "/__content", 0, 0, 40, 15),
                frame("demo:c", parent + "/__content", 0, 0, 20, 11));
        var parents = Map.of("demo:a", parent, "demo:b", parent, "demo:c", parent);
        var collapse = StackLayout.Config.parse(json("""
                {"layout":{"type":"column","gap":4,"collapseHidden":true}}
                """));
        var keepSpace = StackLayout.Config.parse(json("""
                {"layout":{"type":"column","gap":4}}
                """));
        var hidden = Set.of("demo:b");
        var first = resolve(frames, Map.of(parent, new PaddingElement()), Map.of(), parents,
                Map.of(parent, collapse), hidden, Set.of());
        var second = resolve(frames, Map.of(parent, new PaddingElement()), Map.of(), parents,
                Map.of(parent, keepSpace), hidden, Set.of());
        assertEquals(14, first.bounds().get("demo:c").y());
        assertEquals(33, second.bounds().get("demo:c").y());
        assertEquals(0, first.bounds().get("demo:b").y());
    }

    @Test
    public void collapsedAutoSizeOmitsHiddenChildSizes() {
        String parent = "demo:auto";
        var result = resolve(List.of(frame(parent, "screen", 0, 0, 1, 1),
                        frame("demo:small", parent + "/__content", 0, 0, 30, 10),
                        frame("demo:large", parent + "/__content", 0, 0, 250, 40)),
                Map.of(parent, new PaddingElement(2, 2)),
                Map.of(parent, new ContainerLayout.AutoSize(true, true)),
                Map.of("demo:small", parent, "demo:large", parent),
                Map.of(parent, StackLayout.Config.parse(json("""
                        {"layout":{"type":"column","gap":5,"collapseHidden":true}}
                        """))), Set.of("demo:large"), Set.of());
        assertEquals(new ContainerLayout.Size(34, 14), result.sizes().get(parent));
    }

    @Test
    public void stretchAndUnmanagedManualAnchorsRemainIndependent() {
        String parent = "demo:panel";
        var result = resolve(List.of(frame(parent, "screen", 10, 10, 160, 60),
                        frame("demo:a", parent + "/__content", 2, 3, 20, 10),
                        frame("demo:manual", parent + "/__content", 5, 7, 12, 11),
                        frame("demo:b", parent + "/__content", 0, 0, 40, 15)),
                Map.of(parent, new PaddingElement(0, 0)),
                Map.of(), Map.of("demo:a", parent, "demo:manual", parent, "demo:b", parent),
                Map.of(parent, StackLayout.Config.parse(json("""
                        {"layout":{"type":"row","gap":8,"align":"stretch"}}
                        """))), Set.of(), Set.of("demo:manual"));
        assertEquals(new AnchorLayout.Bounds(12, 13, 20, 60), result.bounds().get("demo:a"));
        assertEquals(new AnchorLayout.Bounds(15, 17, 12, 11), result.bounds().get("demo:manual"));
        assertEquals(new AnchorLayout.Bounds(38, 10, 40, 60), result.bounds().get("demo:b"));
    }

    @Test
    public void nestedAutoSizedRowsAndColumnsConverge() {
        String outer = "demo:outer";
        String inner = "demo:inner";
        var frames = List.of(frame(outer, "screen", 20, 20, 1, 1),
                frame(inner, outer + "/__content", 0, 0, 1, 1),
                frame("demo:text1", inner + "/__content", 0, 0, 40, 12),
                frame("demo:text2", inner + "/__content", 0, 0, 10, 15),
                frame("demo:button", outer + "/__content", 0, 0, 20, 10));
        var result = resolve(frames,
                Map.of(outer, new PaddingElement(1, 1), inner, new PaddingElement(2, 2)),
                Map.of(outer, new ContainerLayout.AutoSize(true, true),
                        inner, new ContainerLayout.AutoSize(true, true)),
                Map.of(inner, outer, "demo:text1", inner, "demo:text2", inner, "demo:button", outer),
                Map.of(outer, StackLayout.Config.parse(json("""
                        {"layout":{"type":"row","gap":6}}
                        """)),
                        inner, StackLayout.Config.parse(json("""
                        {"layout":{"type":"column","gap":4}}
                        """))), Set.of(), Set.of());
        assertEquals(new ContainerLayout.Size(44, 35), result.sizes().get(inner));
        assertEquals(new ContainerLayout.Size(72, 37), result.sizes().get(outer));
        assertEquals(new AnchorLayout.Bounds(71, 21, 20, 10), result.bounds().get("demo:button"));
        assertEquals(2, result.bounds().get("demo:text2").x() - result.bounds().get(inner).x());
    }

    @Test
    public void explicitNonContentAnchorIsNotManagedByStack() {
        String panel = "demo:panel";
        var explicit = new AnchorLayout.Frame("demo:external",
                new PositionElement(ElementAnchor.CENTER, ElementAnchor.CENTER, "screen", 0, 0), 30, 12);
        var result = resolve(List.of(frame(panel, "screen", 10, 10, 200, 70), explicit,
                        frame("demo:first", panel + "/__content", 0, 0, 20, 12)),
                Map.of(panel, new PaddingElement()), Map.of(),
                Map.of("demo:first", panel, "demo:external", panel),
                Map.of(panel, StackLayout.Config.parse(json("""
                        {"layout":{"type":"row","gap":10}}
                        """))), Set.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(235, 144, 30, 12), result.bounds().get("demo:external"));
        assertEquals(new AnchorLayout.Bounds(10, 10, 20, 12), result.bounds().get("demo:first"));
    }
}
