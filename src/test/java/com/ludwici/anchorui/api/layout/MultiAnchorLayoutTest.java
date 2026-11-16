package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiAnchorLayoutTest {
    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static AnchorLayout.Frame frame(String id, int width, int height, String json) {
        return new AnchorLayout.Frame(id, new PositionElement(json(json)), width, height);
    }

    @Test
    public void opposingLeftAndRightOverridePreferredWidthOnEveryResize() {
        AnchorLayout.Frame bar = frame("demo:bar", 15, 24, """
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT","relativeTo":"screen","x":12},
                  {"point":"RIGHT","anchor":"RIGHT","relativeTo":"screen","x":-20}
                ]}
                """);
        assertEquals(new AnchorLayout.Bounds(12, 88, 288, 24), AnchorLayout.resolve(320, 200, List.of(bar)).get(bar.id()));
        assertEquals(new AnchorLayout.Bounds(12, 88, 468, 24), AnchorLayout.resolve(500, 200, List.of(bar)).get(bar.id()));
        assertTrue(bar.position().dynamicWidth());
        assertEquals(false, bar.position().dynamicHeight());
    }

    @Test
    public void topBottomStretchHeightAndFourCornersStretchBothAxes() {
        AnchorLayout.Frame column = frame("demo:column", 45, 3, """
                {"anchors":[
                  {"point":"TOP","anchor":"TOP","y":8},
                  {"point":"BOTTOM","anchor":"BOTTOM","y":-16}
                ]}
                """);
        assertEquals(new AnchorLayout.Bounds(127, 8, 45, 176), AnchorLayout.resolve(300, 200, List.of(column)).get(column.id()));

        AnchorLayout.Frame rectangle = frame("demo:rectangle", 7, 9, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":20},
                  {"point":"TOPRIGHT","anchor":"TOPRIGHT","x":-12,"y":20},
                  {"point":"BOTTOMLEFT","anchor":"BOTTOMLEFT","x":10,"y":-30},
                  {"point":"BOTTOMRIGHT","anchor":"BOTTOMRIGHT","x":-12,"y":-30}
                ]}
                """);
        assertEquals(new AnchorLayout.Bounds(10, 20, 278, 150), AnchorLayout.resolve(300, 200, List.of(rectangle)).get(rectangle.id()));
    }

    @Test
    public void unlimitedRedundantAnchorsMustAgreeWithDerivedGeometry() {
        AnchorLayout.Frame frame = frame("demo:many", 1, 1, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":20},
                  {"point":"TOPRIGHT","anchor":"TOPRIGHT","x":-10,"y":20},
                  {"point":"BOTTOMLEFT","anchor":"BOTTOMLEFT","x":10,"y":-20},
                  {"point":"BOTTOMRIGHT","anchor":"BOTTOMRIGHT","x":-10,"y":-20},
                  {"point":"CENTER","anchor":"CENTER"},
                  {"point":"LEFT","anchor":"LEFT","x":10},
                  {"point":"RIGHT","anchor":"RIGHT","x":-10},
                  {"point":"TOP","anchor":"TOP","y":20},
                  {"point":"BOTTOM","anchor":"BOTTOM","y":-20}
                ]}
                """);
        assertEquals(new AnchorLayout.Bounds(10, 20, 280, 160), AnchorLayout.resolve(300, 200, List.of(frame)).get(frame.id()));
    }

    @Test
    public void separateTargetFramesContributeToOneDynamicSpanAndCacheBoth() {
        AnchorLayout.Frame bar = frame("demo:bar", 20, 20, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPRIGHT","relativeTo":"demo:start","x":5},
                  {"point":"TOPRIGHT","anchor":"TOPLEFT","relativeTo":"demo:end","x":-5}
                ]}
                """);
        AnchorLayout.Frame start = frame("demo:start", 20, 10, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":40}
                """);
        AnchorLayout.Frame end = frame("demo:end", 20, 10, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":210,"y":40}
                """);
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        List<AnchorLayout.Frame> before = List.of(bar, end, start);
        Map<String, AnchorLayout.Bounds> first = AnchorLayout.resolve(400, 200, before, cache, warning -> {});
        assertEquals(new AnchorLayout.Bounds(35, 40, 170, 20), first.get("demo:bar"));
        Map<String, AnchorLayout.Bounds> repeated = AnchorLayout.resolve(400, 200, before, cache, warning -> {});
        assertSame(first.get("demo:bar"), repeated.get("demo:bar"));

        AnchorLayout.Frame moved = frame("demo:end", 20, 10, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":270,"y":40}
                """);
        Map<String, AnchorLayout.Bounds> changed = AnchorLayout.resolve(400, 200, List.of(bar, moved, start), cache, warning -> {});
        assertEquals(new AnchorLayout.Bounds(35, 40, 230, 20), changed.get("demo:bar"));
        assertNotSame(first.get("demo:bar"), changed.get("demo:bar"));
        assertSame(first.get("demo:start"), changed.get("demo:start"));
    }

    @Test
    public void rejectsConflictingRedundantOrNegativeAnchorsAndCrossAnchorCycles() {
        AnchorLayout.Frame conflicting = frame("demo:conflict", 30, 20, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPLEFT","x":10},
                  {"point":"BOTTOMLEFT","anchor":"BOTTOMLEFT","x":15}
                ]}
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(300, 200, List.of(conflicting))).getMessage().contains("Conflicting horizontal"));

        AnchorLayout.Frame negative = frame("demo:negative", 30, 20, """
                {"anchors":[
                  {"point":"LEFT","anchor":"RIGHT","x":5},
                  {"point":"RIGHT","anchor":"LEFT","x":-5}
                ]}
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(300, 200, List.of(negative))).getMessage().contains("Invalid horizontal span"));

        AnchorLayout.Frame overconstrained = frame("demo:over", 1, 1, """
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT","x":10},
                  {"point":"RIGHT","anchor":"RIGHT","x":-10},
                  {"point":"CENTER","anchor":"CENTER","x":5}
                ]}
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(300, 200, List.of(overconstrained))).getMessage().contains("Over-constrained"));

        AnchorLayout.Frame cycle = frame("demo:cycle", 30, 20, """
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT"},
                  {"point":"RIGHT","anchor":"RIGHT","relativeTo":"demo:dependent"}
                ]}
                """);
        AnchorLayout.Frame dependent = frame("demo:dependent", 30, 20, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","relativeTo":"demo:cycle"}
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(300, 200, List.of(cycle, dependent))).getMessage().contains("Circular"));
    }

    @Test
    public void missingSecondaryAnchorStillWarnsEveryTimeWhenCached() {
        AnchorLayout.Frame bar = frame("demo:bar", 50, 20, """
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT","relativeTo":"screen","x":10},
                  {"point":"RIGHT","anchor":"RIGHT","relativeTo":"demo:missing","x":-10}
                ]}
                """);
        List<String> warnings = new ArrayList<>();
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        AnchorLayout.resolve(300, 200, List.of(bar), cache, warnings::add);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("anchor #1"));
        warnings.clear();
        AnchorLayout.resolve(300, 200, List.of(bar), cache, warnings::add);
        assertEquals(1, warnings.size());
    }

    @Test
    public void invalidJsonAndEditorTransformsCannotLoseSecondaryConstraints() {
        PositionElement source = new PositionElement(json("""
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT","relativeTo":"panel"},
                  {"point":"RIGHT","anchor":"RIGHT","relativeTo":"panel"}
                ]}
                """));
        PositionElement updated = source.withReferences(value -> value.equals("panel") ? "demo:panel" : value)
                .withPrimary(source.point, source.anchor, "demo:alternate")
                .translated(8, -4);
        assertEquals(2, updated.anchors.size());
        assertEquals("demo:alternate", updated.anchors.getFirst().relativeTo());
        assertEquals("demo:panel", updated.anchors.get(1).relativeTo());
        assertEquals(8, updated.anchors.getFirst().x());
        assertEquals(8, updated.anchors.get(1).x());
        assertEquals(-4, updated.anchors.get(1).y());
        assertEquals(2, source.anchors.size());
        assertEquals(0, source.anchors.getFirst().x());

        for (String raw : List.of("""
                {"anchors":[]}
                """, """
                {"anchors":{}}
                """, """
                {"anchors":[42]}
                """, """
                {"anchors":[{"point":"INVALID"}]}
                """, """
                {"anchors":[{"point":"LEFT"}],"x":2}
                """)) {
            assertThrows(IllegalArgumentException.class, () -> new PositionElement(json(raw)));
        }
    }
}
