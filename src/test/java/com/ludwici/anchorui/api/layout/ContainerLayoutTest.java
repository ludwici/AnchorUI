package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContainerLayoutTest {
    private static AnchorLayout.Frame frame(String id, String parent, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id, new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, parent, x, y), width, height);
    }

    @Test
    public void measuresChildrenIncludingPaddingAndPreservesFixedDimensions() {
        var frames = List.of(frame("demo:panel", "screen", 50, 60, 120, 70),
                frame("demo:label", "demo:panel/__content", 3, 4, 40, 12));
        var result = ContainerLayout.resolve(400, 300, frames,
                Map.of("demo:panel", new PaddingElement(5, 6, 7, 8)),
                Map.of("demo:panel", new ContainerLayout.AutoSize(true, true)),
                Map.of("demo:label", "demo:panel"), ignored -> {});
        assertEquals(new ContainerLayout.Size(55, 30), result.sizes().get("demo:panel"));
        assertEquals(new AnchorLayout.Bounds(58, 70, 40, 12), result.bounds().get("demo:label"));
    }

    @Test
    public void nestedAutoContainersConvergeAndCanBeAnchoredToScreenCenter() {
        var outer = new AnchorLayout.Frame("demo:outer",
                new PositionElement(ElementAnchor.CENTER, ElementAnchor.CENTER, "screen", 0, 0), 120, 90);
        var frames = List.of(outer, frame("demo:inner", "demo:outer/__content", 3, 4, 1, 1),
                frame("demo:text", "demo:inner/__content", 0, 0, 60, 14));
        var result = ContainerLayout.resolve(400, 300, frames,
                Map.of("demo:outer", new PaddingElement(6, 5, 6, 5),
                        "demo:inner", new PaddingElement(2, 3, 2, 3)),
                Map.of("demo:outer", new ContainerLayout.AutoSize(true, true),
                        "demo:inner", new ContainerLayout.AutoSize(true, true)),
                Map.of("demo:inner", "demo:outer", "demo:text", "demo:inner"), ignored -> {});
        assertEquals(new ContainerLayout.Size(64, 20), result.sizes().get("demo:inner"));
        assertEquals(new ContainerLayout.Size(85, 34), result.sizes().get("demo:outer"));
        assertEquals(200, result.bounds().get("demo:outer").x() + result.bounds().get("demo:outer").width() / 2);
    }

    @Test
    public void fixedSizeContainersAreUnaffected() {
        var frames = List.of(frame("demo:fixed", "screen", 10, 15, 210, 95),
                frame("demo:child", "demo:fixed/__content", 0, 0, 300, 130));
        var result = ContainerLayout.resolve(400, 300, frames,
                Map.of("demo:fixed", new PaddingElement(10, 10, 10, 10)),
                Map.of(), Map.of("demo:child", "demo:fixed"), ignored -> {});
        assertEquals(new ContainerLayout.Size(210, 95), result.sizes().get("demo:fixed"));
    }

    @Test
    public void parsesExplicitAutoSizeSettings() {
        assertEquals(new ContainerLayout.AutoSize(true, true),
                ContainerLayout.AutoSize.parse(JsonParser.parseString("{\"autoSize\": true}").getAsJsonObject()));
        assertEquals(new ContainerLayout.AutoSize(false, true),
                ContainerLayout.AutoSize.parse(JsonParser.parseString("{\"autoSize\": \"height\"}").getAsJsonObject()));
        assertEquals(ContainerLayout.AutoSize.NONE,
                ContainerLayout.AutoSize.parse(JsonParser.parseString("{}").getAsJsonObject()));
    }

    @Test
    public void rejectsInvalidStructuralContainer() {
        assertThrows(IllegalArgumentException.class, () -> ContainerLayout.resolve(100, 100,
                List.of(frame("demo:child", "screen", 0, 0, 10, 10)),
                Map.of(), Map.of(), Map.of("demo:child", "demo:missing"), ignored -> {}));
    }
}
