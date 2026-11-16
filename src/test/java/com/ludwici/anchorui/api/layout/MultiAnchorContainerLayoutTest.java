package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiAnchorContainerLayoutTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static AnchorLayout.Frame frame(String id, int width, int height, String position) {
        return new AnchorLayout.Frame(id, new PositionElement(json(position)), width, height);
    }

    @Test
    public void stretchedPanelUsesResolvedWidthForRowFlexAndIgnoresAutoWidth() {
        AnchorLayout.Frame panel = frame("demo:panel", 25, 40, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPLEFT","x":20,"y":10},
                  {"point":"TOPRIGHT","anchor":"TOPRIGHT","x":-20,"y":10}
                ]}
                """);
        AnchorLayout.Frame child = frame("demo:child", 10, 16, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","relativeTo":"demo:panel"}
                """);
        StackLayout.Rules grow = StackLayout.Rules.parse(json("""
                {"layoutItem":{"grow":1}}
                """));
        var result = ContainerLayout.resolve(400, 200, List.of(panel, child), Map.of(panel.id(), new PaddingElement()), Map.of(panel.id(), new ContainerLayout.AutoSize(true, false)), Map.of(child.id(), panel.id()), Map.of(panel.id(), ContainerAutoLayout.parse(json("""
                {"layout":"row"}
                """))), Map.of(child.id(), grow), Set.of(), Set.of(), warning -> {});
        assertEquals(new AnchorLayout.Bounds(20, 10, 360, 40), result.bounds().get(panel.id()));
        assertEquals(new AnchorLayout.Bounds(20, 10, 360, 16), result.bounds().get(child.id()));
        assertEquals(new ContainerLayout.Size(360, 40), result.sizes().get(panel.id()));
    }

    @Test
    public void nestedLeftRightAnchorsStretchWithinParentContentPadding() {
        AnchorLayout.Frame panel = frame("demo:panel", 300, 100, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":20}
                """);
        AnchorLayout.Frame child = frame("demo:child", 12, 20, """
                {"anchors":[
                  {"point":"LEFT","anchor":"LEFT","relativeTo":"demo:panel"},
                  {"point":"RIGHT","anchor":"RIGHT","relativeTo":"demo:panel"}
                ]}
                """);
        var result = ContainerLayout.resolve(400, 250, List.of(panel, child), Map.of(panel.id(), new PaddingElement(10, 6, 12, 8)), Map.of(), Map.of(child.id(), panel.id()), warning -> {});
        assertEquals(new AnchorLayout.Bounds(20, 59, 278, 20), result.bounds().get(child.id()));
    }

    @Test
    public void explicitMultiAnchorsAreNotOverwrittenByGridCellPlacement() {
        AnchorLayout.Frame grid = frame("demo:grid", 300, 100, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":20}
                """);
        AnchorLayout.Frame child = frame("demo:child", 10, 18, """
                {"anchors":[
                  {"point":"TOPLEFT","anchor":"TOPLEFT","relativeTo":"demo:grid","x":8,"y":6},
                  {"point":"TOPRIGHT","anchor":"TOPRIGHT","relativeTo":"demo:grid","x":-8,"y":6}
                ]}
                """);
        AnchorLayout.Frame other = frame("demo:other", 30, 18, """
                {"point":"TOPLEFT","anchor":"TOPLEFT","relativeTo":"demo:grid"}
                """);
        var result = ContainerLayout.resolve(500, 300, List.of(grid, child, other), Map.of(grid.id(), new PaddingElement()), Map.of(), Map.of(child.id(), grid.id(), other.id(), grid.id()), Map.of(grid.id(), ContainerAutoLayout.parse(json("""
                {"layout":{"type":"grid","columns":2,"columnGap":10,"justifyItems":"stretch"}}
                """))), Map.of(), Set.of(), Set.of(), warning -> {});
        assertEquals(new AnchorLayout.Bounds(18, 26, 284, 18), result.bounds().get(child.id()));
        assertEquals(new AnchorLayout.Bounds(10, 20, 145, 18), result.bounds().get(other.id()));
    }
}
