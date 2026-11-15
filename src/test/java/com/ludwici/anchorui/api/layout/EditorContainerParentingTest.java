package com.ludwici.anchorui.api.layout;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.ludwici.anchorui.client.hud.HudLayoutStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorContainerParentingTest {
    @TempDir
    Path directory;

    private static AnchorLayout.Frame frame(String id, String target, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id, new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, target, x, y), width, height);
    }

    @Test
    public void saveRestoreAndResetStructuralParentIndependentlyFromAnchorTarget() throws IOException {
        String child = "test:button";
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        assertNull(offsets.containerParent(child, null));
        assertEquals("test:left", offsets.containerParent(child, "test:left"));
        offsets.setAnchor(child, new HudLayoutOffsets.AnchorOverride("test:right", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT));
        assertEquals("test:left", offsets.containerParent(child, "test:left"));

        offsets.setContainerParent(child, "test:right");
        offsets.set(child, -20, -10);
        assertEquals("test:right", offsets.containerParent(child, "test:left"));
        assertEquals("test:right", offsets.anchor(child).relativeTo());

        Path path = directory.resolve("layout.json");
        HudLayoutStorage.save(path, offsets);
        HudLayoutOffsets loaded = HudLayoutStorage.load(path);
        assertTrue(loaded.sameAs(offsets));
        assertEquals("test:right", loaded.toJson().getAsJsonObject("offsets").getAsJsonObject(child).get("containerParent").getAsString());
        assertEquals("test:right", loaded.containerParent(child, "test:left"));

        HudLayoutOffsets copy = loaded.copy();
        copy.setContainerParent(child, "screen");
        assertNull(copy.containerParent(child, "test:left"));
        assertEquals("test:right", loaded.containerParent(child, "test:left"));
        copy.clearContainerParent(child);
        assertEquals("test:left", copy.containerParent(child, "test:left"));
        assertFalse(copy.sameAs(loaded));

        loaded.reset(child);
        assertEquals("test:left", loaded.containerParent(child, "test:left"));
        assertNull(loaded.anchor(child));
        assertThrows(IllegalArgumentException.class, () -> offsets.setContainerParent(child, child));
    }

    @Test
    public void structuralMoveMakesAnExistingWidgetParticipateInASecondGrid() {
        String left = "test:left";
        String right = "test:right";
        String child = "test:cell";
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.setContainerParent(child, right);
        offsets.setAnchor(child, new HudLayoutOffsets.AnchorOverride(right, ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT));
        offsets.set(child, -42, -7);

        var source = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 42, 7);
        var override = offsets.anchor(child);
        var offset = offsets.get(child);
        var attached = new PositionElement(override.point(), override.anchor(), override.relativeTo(), source.x + offset.x(), source.y + offset.y());
        var frames = List.of(frame(left, "screen", 0, 0, 100, 40), frame(right, "screen", 150, 20, 210, 80), frame("test:existing", right, 0, 0, 40, 15), new AnchorLayout.Frame(child, attached, 50, 18));
        var result = ContainerLayout.resolve(600, 300, frames,
                Map.of(left, new PaddingElement(), right, new PaddingElement(10, 10)),
                Map.of(), Map.of("test:existing", right, child, offsets.containerParent(child, left)),
                Map.of(right, new GridLayout.Config(2, 10, 10, 0, 0, GridLayout.Align.START, GridLayout.Align.START, true)),
                Set.of(), Set.of(), ignored -> {});
        assertEquals(160, result.bounds().get("test:existing").x());
        assertEquals(260, result.bounds().get(child).x());
        assertEquals(30, result.bounds().get(child).y());
    }

    @Test
    public void structuralCyclesAreRejectedEvenForStaticPanels() {
        assertThrows(IllegalArgumentException.class, () -> ContainerLayout.resolve(500, 300,
                List.of(frame("test:a", "screen", 0, 0, 100, 50), frame("test:b", "screen", 0, 0, 100, 50)),
                Map.of("test:a", new PaddingElement(), "test:b", new PaddingElement()),
                Map.of(), Map.of("test:a", "test:b", "test:b", "test:a"), ignored -> {}));
    }

    @Test
    public void visibilityTraversalPlacesNewParentBeforeMovedChildRegardlessOfJsonOrder() {
        List<String> original = List.of("test:child", "test:other", "test:container");
        var reordered = WidgetContainers.parentFirst(original, id -> id, Map.of("test:child", "test:container"));
        assertTrue(reordered.indexOf("test:container") < reordered.indexOf("test:child"));
        assertEquals(3, reordered.size());
        assertThrows(IllegalArgumentException.class, () -> WidgetContainers.parentFirst(original, id -> id, Map.of("test:child", "test:container", "test:container", "test:child")));
    }
}
