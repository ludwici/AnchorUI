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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdvancedContainerLayoutTest {
    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static AnchorLayout.Frame frame(String id, String target, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id,
                new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, target, x, y),
                width, height);
    }

    private static ContainerLayout.Result layout(List<AnchorLayout.Frame> frames,
                                                   Map<String, PaddingElement> pads,
                                                   Map<String, ContainerLayout.AutoSize> auto,
                                                   Map<String, String> parents,
                                                   Map<String, ? extends ContainerAutoLayout> layouts,
                                                   Map<String, StackLayout.Rules> rules,
                                                   Set<String> hidden) {
        return ContainerLayout.resolve(500, 320, frames, pads, auto, parents, layouts, rules, hidden, Set.of(),
                warning -> {
                    throw new AssertionError(warning);
                });
    }

    @Test
    public void validatesFlexRulesAndGridConfiguration() {
        var rules = StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":2,"shrink":1,"minWidth":24,"maxWidth":120}}
                """));
        assertEquals(2, rules.grow());
        assertEquals(1, rules.shrink());
        assertEquals(24, rules.minWidth());
        assertEquals(120, rules.maxWidth());
        assertEquals(false, StackLayout.Rules.parse(json("""
                {"layoutItem":false}
                """)).managed());
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":-1}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Rules.parse(json("""
                {"layoutItem":{"shrink":10000}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> StackLayout.Rules.parse(json("""
                {"layoutItem":{"minHeight":80,"maxHeight":20}}
                """)));
        assertInstanceOf(GridLayout.Config.class, ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":3}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":0}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columnGap":-1}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","justifyItems":"invalid"}}
                """)));
    }

    @Test
    public void rowGrowAllocatesExtraSpaceAndPreservesOrder() {
        var frames = List.of(frame("x:panel", "screen", 0, 0, 200, 50),
                frame("x:a", "x:panel/__content", 0, 0, 40, 20),
                frame("x:b", "x:panel/__content", 0, 0, 50, 20));
        var grow = StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":1}}
                """));
        var result = layout(frames, Map.of("x:panel", new PaddingElement()), Map.of(),
                Map.of("x:a", "x:panel", "x:b", "x:panel"),
                Map.of("x:panel", ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"row","gap":10}}
                        """))),
                Map.of("x:a", grow, "x:b", grow), Set.of());
        assertEquals(new AnchorLayout.Bounds(0, 0, 90, 20), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(100, 0, 100, 20), result.bounds().get("x:b"));
    }

    @Test
    public void rowShrinkRespectsMinimumSizesAndRestoresWhenSpaceReturns() {
        var frames = List.of(frame("x:panel", "screen", 0, 0, 100, 50),
                frame("x:a", "x:panel/__content", 0, 0, 80, 20),
                frame("x:b", "x:panel/__content", 0, 0, 80, 20));
        var first = StackLayout.Rules.parse(json("""
                {"layoutItem":{"shrink":1,"minWidth":60}}
                """));
        var second = StackLayout.Rules.parse(json("""
                {"layoutItem":{"shrink":1}}
                """));
        var parents = Map.of("x:a", "x:panel", "x:b", "x:panel");
        var config = Map.of("x:panel", ContainerAutoLayout.parse(json("""
                {"layout":"row"}
                """)));
        var result = layout(frames, Map.of("x:panel", new PaddingElement()), Map.of(), parents,
                config, Map.of("x:a", first, "x:b", second), Set.of());
        assertEquals(new AnchorLayout.Bounds(0, 0, 60, 20), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(60, 0, 40, 20), result.bounds().get("x:b"));

        var widened = List.of(frame("x:panel", "screen", 0, 0, 200, 50), frames.get(1), frames.get(2));
        var restored = layout(widened, Map.of("x:panel", new PaddingElement()), Map.of(), parents,
                config, Map.of("x:a", first, "x:b", second), Set.of());
        assertEquals(80, restored.bounds().get("x:a").width());
        assertEquals(80, restored.bounds().get("x:b").width());
    }

    @Test
    public void rowWrapAddsLinesAndAutoSizesCrossAxis() {
        String p = "x:panel";
        var frames = List.of(frame(p, "screen", 5, 6, 120, 1),
                frame("x:a", p + "/__content", 0, 0, 60, 10),
                frame("x:b", p + "/__content", 0, 0, 50, 20),
                frame("x:c", p + "/__content", 0, 0, 40, 12));
        var result = layout(frames, Map.of(p, new PaddingElement()),
                Map.of(p, new ContainerLayout.AutoSize(false, true)),
                Map.of("x:a", p, "x:b", p, "x:c", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"row","gap":10,"lineGap":7,"wrap":true}}
                        """))), Map.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(5, 6, 60, 10), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(75, 6, 50, 20), result.bounds().get("x:b"));
        assertEquals(new AnchorLayout.Bounds(5, 33, 40, 12), result.bounds().get("x:c"));
        assertEquals(new ContainerLayout.Size(120, 39), result.sizes().get(p));
    }

    @Test
    public void columnWrapCreatesAdditionalColumns() {
        String p = "x:panel";
        var frames = List.of(frame(p, "screen", 0, 0, 1, 50),
                frame("x:a", p + "/__content", 0, 0, 30, 20),
                frame("x:b", p + "/__content", 0, 0, 15, 20),
                frame("x:c", p + "/__content", 0, 0, 25, 20));
        var result = layout(frames, Map.of(p, new PaddingElement()),
                Map.of(p, new ContainerLayout.AutoSize(true, false)),
                Map.of("x:a", p, "x:b", p, "x:c", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"column","wrap":true,"gap":5,"lineGap":6}}
                        """))), Map.of(), Set.of());
        assertEquals(0, result.bounds().get("x:a").x());
        assertEquals(25, result.bounds().get("x:b").y());
        assertEquals(36, result.bounds().get("x:c").x());
        assertEquals(new ContainerLayout.Size(61, 50), result.sizes().get(p));
    }

    @Test
    public void fixedGridUsesEvenColumnsAndAlignsEachCell() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 0, 0, 320, 130),
                frame("x:a", p + "/__content", 0, 0, 50, 10),
                frame("x:b", p + "/__content", 0, 0, 70, 20),
                frame("x:c", p + "/__content", 0, 0, 40, 12),
                frame("x:d", p + "/__content", 0, 0, 30, 30));
        var result = layout(frames, Map.of(p, new PaddingElement(10, 10)),
                Map.of(), Map.of("x:a", p, "x:b", p, "x:c", p, "x:d", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"grid","columns":3,"columnGap":6,"rowGap":8,
                         "justifyItems":"center","alignItems":"center"}}
                        """))), Map.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(33, 15, 50, 10), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(125, 10, 70, 20), result.bounds().get("x:b"));
        assertEquals(new AnchorLayout.Bounds(242, 14, 40, 12), result.bounds().get("x:c"));
        assertEquals(new AnchorLayout.Bounds(43, 38, 30, 30), result.bounds().get("x:d"));
    }

    @Test
    public void gridAutoSizeMeasuresColumnsAndRows() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 0, 0, 1, 1),
                frame("x:a", p + "/__content", 0, 0, 30, 10),
                frame("x:b", p + "/__content", 0, 0, 50, 20),
                frame("x:c", p + "/__content", 0, 0, 80, 14));
        var result = layout(frames, Map.of(p, new PaddingElement(2, 2)),
                Map.of(p, new ContainerLayout.AutoSize(true, true)),
                Map.of("x:a", p, "x:b", p, "x:c", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"grid","columns":2,"columnGap":5,"rowGap":7}}
                        """))), Map.of(), Set.of());
        assertEquals(new ContainerLayout.Size(139, 45), result.sizes().get(p));
        assertEquals(2, result.bounds().get("x:a").x());
        assertEquals(29, result.bounds().get("x:c").y());
    }

    @Test
    public void collapsingGridHiddenCellsReflowsEveryStructuralChild() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 0, 0, 150, 100),
                frame("x:a", p + "/__content", 0, 0, 20, 10),
                frame("x:hidden", p + "/__content", 0, 0, 30, 12),
                frame("x:c", p + "/__content", 0, 0, 20, 10),
                frame("x:manual", p + "/__content", 7, 8, 40, 20));
        var parents = Map.of("x:a", p, "x:hidden", p, "x:c", p, "x:manual", p);
        var config = Map.of(p, ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":2,"columnGap":10,"rowGap":10,"collapseHidden":true}}
                """)));
        var result = layout(frames, Map.of(p, new PaddingElement()), Map.of(), parents,
                config, Map.of("x:manual", StackLayout.Rules.parse(json("""
                        {"layoutItem":false}
                        """))), Set.of("x:hidden"));
        assertEquals(0, result.bounds().get("x:a").y());
        assertEquals(0, result.bounds().get("x:c").y());
        assertEquals(80, result.bounds().get("x:c").x());
        assertEquals(new AnchorLayout.Bounds(0, 20, 40, 20), result.bounds().get("x:manual"));
    }
    @Test
    public void gridOwnsAllStructuralChildrenRegardlessOfTheirLegacyAnchorAndLayoutItem() {
        String p = "x:grid";
        var unrelated = new AnchorLayout.Frame("x:external",
                new PositionElement(ElementAnchor.CENTER, ElementAnchor.BOTTOMRIGHT, "screen", 90, 40), 50, 12);
        var frames = List.of(frame(p, "screen", 15, 25, 260, 150),
                frame("x:a", "screen", 100, 70, 40, 20),
                unrelated,
                frame("x:c", p, 200, 60, 60, 22));
        var parents = Map.of("x:a", p, "x:external", p, "x:c", p);
        var layout = Map.of(p, ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":2,"columnGap":8,"rowGap":6,
                  "justifyItems":"stretch","alignItems":"center"}}
                """)));
        var rules = Map.of("x:a", StackLayout.Rules.parse(json("""
                {"layoutItem":false}
                """)));
        var result = layout(frames, Map.of(p, new PaddingElement(10, 10)),
                Map.of(), parents, layout, rules, Set.of());
        assertEquals(new AnchorLayout.Bounds(25, 35, 116, 20), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(149, 39, 116, 12), result.bounds().get("x:external"));
        assertEquals(new AnchorLayout.Bounds(25, 61, 116, 22), result.bounds().get("x:c"));
    }

    @Test
    public void gridDoesNotAbsorbWidgetsThatAreNotItsChildren() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 10, 15, 180, 80),
                frame("x:a", "screen", 25, 29, 30, 14),
                frame("x:outside", "screen", 300, 100, 50, 20));
        var result = layout(frames, Map.of(p, new PaddingElement()), Map.of(),
                Map.of("x:a", p), Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"grid","columns":2,"gap":8}}
                        """))), Map.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(10, 15, 30, 14), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(300, 100, 50, 20), result.bounds().get("x:outside"));
    }

    @Test
    public void growClampsAtMaximumAndRedistributesUnusedSpace() {
        String p = "x:flex";
        var frames = List.of(frame(p, "screen", 0, 0, 180, 40),
                frame("x:a", p + "/__content", 0, 0, 40, 20),
                frame("x:b", p + "/__content", 0, 0, 40, 20));
        var first = StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":1,"maxWidth":60}}
                """));
        var second = StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":1,"maxWidth":160}}
                """));
        var result = layout(frames, Map.of(p, new PaddingElement()), Map.of(),
                Map.of("x:a", p, "x:b", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":"row"}
                        """))),
                Map.of("x:a", first, "x:b", second), Set.of());
        assertEquals(new AnchorLayout.Bounds(0, 0, 60, 20), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(60, 0, 120, 20), result.bounds().get("x:b"));
    }

    @Test
    public void explicitGridCellSizesAreStableWhenContainerIsLarger() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 0, 0, 300, 150),
                frame("x:a", p + "/__content", 0, 0, 50, 15),
                frame("x:b", p + "/__content", 0, 0, 50, 15),
                frame("x:c", p + "/__content", 0, 0, 50, 15));
        var result = layout(frames, Map.of(p, new PaddingElement()), Map.of(),
                Map.of("x:a", p, "x:b", p, "x:c", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"grid","columns":2,"cellWidth":90,"cellHeight":32,
                         "columnGap":8,"rowGap":6,"justifyItems":"end","alignItems":"end"}}
                        """))), Map.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(40, 17, 50, 15), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(138, 17, 50, 15), result.bounds().get("x:b"));
        assertEquals(new AnchorLayout.Bounds(40, 55, 50, 15), result.bounds().get("x:c"));
    }

    @Test
    public void wrappingFallsBackToSingleLineWhenMainAxisIsAutoSized() {
        String p = "x:row";
        var frames = List.of(frame(p, "screen", 0, 0, 1, 1),
                frame("x:a", p + "/__content", 0, 0, 60, 12),
                frame("x:b", p + "/__content", 0, 0, 60, 20));
        var result = layout(frames, Map.of(p, new PaddingElement(2, 2)),
                Map.of(p, new ContainerLayout.AutoSize(true, true)),
                Map.of("x:a", p, "x:b", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"row","wrap":true,"gap":5}}
                        """))), Map.of(), Set.of());
        assertEquals(new ContainerLayout.Size(129, 24), result.sizes().get(p));
        assertEquals(67, result.bounds().get("x:b").x());
        assertEquals(2, result.bounds().get("x:b").y());
    }


    @Test
    public void nestedGridUsesFlexSizedParentEvenWhenDefinitionsAreRegisteredChildFirst() {
        String outer = "x:outer";
        String inner = "x:inner";
        var frames = List.of(frame(outer, "screen", 0, 0, 200, 60),
                frame(inner, outer + "/__content", 0, 0, 50, 60),
                frame("x:side", outer + "/__content", 0, 0, 40, 20),
                frame("x:cell1", inner + "/__content", 0, 0, 30, 20),
                frame("x:cell2", inner + "/__content", 0, 0, 30, 20));
        var layouts = new java.util.LinkedHashMap<String, ContainerAutoLayout>();
        layouts.put(inner, ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":2,"columnGap":10}}
                """)));
        layouts.put(outer, ContainerAutoLayout.parse(json("""
                {"layout":{"type":"row","gap":10}}
                """)));
        var result = layout(frames, Map.of(outer, new PaddingElement(), inner, new PaddingElement()),
                Map.of(), Map.of(inner, outer, "x:side", outer, "x:cell1", inner, "x:cell2", inner),
                layouts, Map.of(inner, StackLayout.Rules.parse(json("""
                        {"layoutItem":{"grow":1}}
                        """))), Set.of());
        assertEquals(150, result.bounds().get(inner).width());
        assertEquals(160, result.bounds().get("x:side").x());
        assertEquals(80, result.bounds().get("x:cell2").x());
    }

    @Test
    public void gridKeepsThreeDeclaredTracksWithOnlyTwoCells() {
        String p = "x:grid";
        var frames = List.of(frame(p, "screen", 0, 0, 300, 50),
                frame("x:a", p + "/__content", 0, 0, 20, 20),
                frame("x:b", p + "/__content", 0, 0, 20, 20));
        var result = layout(frames, Map.of(p, new PaddingElement()), Map.of(),
                Map.of("x:a", p, "x:b", p),
                Map.of(p, ContainerAutoLayout.parse(json("""
                        {"layout":{"type":"grid","columns":3,"columnGap":6,"justifyItems":"stretch"}}
                        """))), Map.of(), Set.of());
        assertEquals(new AnchorLayout.Bounds(0, 0, 96, 20), result.bounds().get("x:a"));
        assertEquals(new AnchorLayout.Bounds(102, 0, 96, 20), result.bounds().get("x:b"));
    }
}
