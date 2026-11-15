package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PanelParentLayoutTest {
    private static AnchorLayout.Frame frame(String id, String target, int x, int y, int w, int h) {
        return new AnchorLayout.Frame(id,
                new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, target, x, y), w, h);
    }

    @Test
    public void parentPanelAppliesItsPaddingToRowChildrenWithoutContentIds() {
        String panel = "test:panel";
        var frames = List.of(frame(panel, "screen", 10, 12, 220, 80),
                frame("test:a", panel, 0, 0, 40, 20),
                frame("test:b", panel, 0, 0, 50, 20));
        var arrangement = ContainerAutoLayout.parse(JsonParser.parseString("""
                {"layout":{"type":"row","gap":5}}
                """).getAsJsonObject());
        var result = ContainerLayout.resolve(600, 300, frames,
                Map.of(panel, new PaddingElement(8, 6)),
                Map.of(), Map.of("test:a", panel, "test:b", panel),
                Map.of(panel, arrangement), Map.of(), Set.of(), Set.of(), ignored -> {});
        assertEquals(new AnchorLayout.Bounds(18, 18, 40, 20), result.bounds().get("test:a"));
        assertEquals(new AnchorLayout.Bounds(63, 18, 50, 20), result.bounds().get("test:b"));
        assertTrue(result.bounds().containsKey(panel));
    }

    @Test
    public void plainPanelIsAlreadyAContainerAndPositionsChildrenWithinPadding() {
        String panel = "test:panel";
        var frames = List.of(frame(panel, "screen", 20, 30, 160, 100),
                frame("test:new", panel, 2, 3, 40, 20),
                frame("test:legacy", WidgetContainers.contentId(panel), 2, 3, 40, 20),
                frame("test:external", "screen", 200, 50, 40, 20));
        var result = ContainerLayout.resolve(600, 300, frames,
                Map.of(panel, new PaddingElement(10, 8)),
                Map.of(), Map.of("test:new", panel, "test:legacy", panel, "test:external", panel),
                ignored -> {});
        assertEquals(new AnchorLayout.Bounds(32, 41, 40, 20), result.bounds().get("test:new"));
        assertEquals(result.bounds().get("test:new"), result.bounds().get("test:legacy"));
        assertEquals(new AnchorLayout.Bounds(200, 50, 40, 20), result.bounds().get("test:external"));
    }

    @Test
    public void gridOwnsDirectChildrenEvenWhenTheyKeepOldScreenAnchors() {
        String panel = "test:grid";
        var frames = List.of(frame(panel, "screen", 20, 30, 160, 100),
                frame("test:first", "screen", 500, 500, 40, 20),
                frame("test:second", panel, 20, 30, 40, 20));
        var layout = ContainerAutoLayout.parse(JsonParser.parseString("""
                {"layout":{"type":"grid","columns":2,"columnGap":8}}
                """).getAsJsonObject());
        var result = ContainerLayout.resolve(600, 300, frames,
                Map.of(panel, new PaddingElement(10, 8)), Map.of(),
                Map.of("test:first", panel, "test:second", panel),
                Map.of(panel, layout), Map.of(), Set.of(), Set.of(), ignored -> {});
        assertEquals(new AnchorLayout.Bounds(30, 38, 40, 20), result.bounds().get("test:first"));
        assertEquals(new AnchorLayout.Bounds(104, 38, 40, 20), result.bounds().get("test:second"));
    }
}
