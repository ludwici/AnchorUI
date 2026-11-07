package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudLayoutOffsetsTest {
    @TempDir
    Path directory;

    @Test
    public void storesNamespacedOffsetsAndRemovesZeroValues() {
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.set("moda:health", 12, -5);
        offsets.set("modb:mana", -17, 8);

        assertEquals(new HudLayoutOffsets.Offset(12, -5), offsets.get("moda:health"));
        assertEquals(new HudLayoutOffsets.Offset(-17, 8), offsets.get("modb:mana"));
        assertEquals(HudLayoutOffsets.Offset.ZERO, offsets.get("absent:widget"));

        offsets.set("moda:health", 0, 0);
        assertFalse(offsets.entries().containsKey("moda:health"));
        assertThrows(IllegalArgumentException.class, () -> offsets.set("Bad widget ID", 1, 2));
    }

    @Test
    public void savingAndReloadingPreservesOffsets() throws IOException {
        Path path = directory.resolve("anchorui/hud-layout.json");
        HudLayoutOffsets first = new HudLayoutOffsets();
        assertTrue(HudLayoutStorage.load(path).entries().isEmpty());
        first.set("moda:health", 20, 30);
        first.set("modb:mana", -8, 15);
        HudLayoutStorage.save(path, first);

        HudLayoutOffsets loaded = HudLayoutStorage.load(path);
        assertTrue(loaded.sameAs(first));
        assertEquals(1, loaded.toJson().get("version").getAsInt());
        assertTrue(loaded.toJson().getAsJsonObject("offsets").has("moda:health"));

        loaded.reset("moda:health");
        assertFalse(loaded.sameAs(first));
        assertTrue(first.entries().containsKey("moda:health"));
        loaded.resetAll();
        assertTrue(loaded.entries().isEmpty());
        assertFalse(first.entries().isEmpty());
    }

    @Test
    public void rejectsUnknownVersionsAndBadValues() {
        assertThrows(IllegalArgumentException.class, () -> HudLayoutOffsets.fromJson(JsonParser.parseString("{\"version\":2}").getAsJsonObject()));
        assertThrows(RuntimeException.class, () -> HudLayoutOffsets.fromJson(JsonParser.parseString("{\"version\":1,\"offsets\":{\"moda:health\":{\"x\":\"invalid\",\"y\":0}}}").getAsJsonObject()));
    }

    @Test
    public void visibilityAndAnchorsSurviveRestartAndReset() throws IOException {
        Path path = directory.resolve("anchorui/hud-layout.json");
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.set("moda:health", 12, -3);
        offsets.setHidden("moda:health", true);
        offsets.setAnchor("modb:mana", new HudLayoutOffsets.AnchorOverride("moda:health", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT));
        HudLayoutStorage.save(path, offsets);

        HudLayoutOffsets loaded = HudLayoutStorage.load(path);
        assertTrue(loaded.sameAs(offsets));
        assertTrue(loaded.isHidden("moda:health"));
        assertEquals(new HudLayoutOffsets.AnchorOverride("moda:health", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT), loaded.anchor("modb:mana"));
        assertEquals(HudLayoutOffsets.Offset.ZERO, loaded.get("modb:mana"));
        assertEquals("moda:health", loaded.toJson().getAsJsonObject("offsets").getAsJsonObject("modb:mana").get("relativeTo").getAsString());

        loaded.setHidden("moda:health", false);
        assertFalse(loaded.isHidden("moda:health"));
        loaded.reset("modb:mana");
        assertEquals(null, loaded.anchor("modb:mana"));
        loaded.resetAll();
        assertTrue(loaded.toJson().getAsJsonObject("offsets").entrySet().isEmpty());
    }

    @Test
    public void offsetResetDoesNotDiscardOtherWidgetOverrides() {
        String id = "moda:health";
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        overrides.set(id, 0, 63);
        overrides.setAnchor(id, new HudLayoutOffsets.AnchorOverride("othermod:frame", ElementAnchor.BOTTOMRIGHT, ElementAnchor.TOPLEFT));
        overrides.setHidden(id, true);

        overrides.set(id, 0, 0);

        assertEquals(HudLayoutOffsets.Offset.ZERO, overrides.get(id));
        assertFalse(overrides.entries().containsKey(id));
        assertEquals("othermod:frame", overrides.anchor(id).relativeTo());
        assertEquals(ElementAnchor.BOTTOMRIGHT, overrides.anchor(id).point());
        assertTrue(overrides.isHidden(id));

        var restoredPoint = new HudLayoutOffsets.AnchorOverride(overrides.anchor(id).relativeTo(), ElementAnchor.TOPLEFT, overrides.anchor(id).anchor());
        overrides.setAnchor(id, restoredPoint);
        assertEquals("othermod:frame", overrides.anchor(id).relativeTo());
        assertEquals(ElementAnchor.TOPLEFT, overrides.anchor(id).point());
        assertTrue(overrides.isHidden(id));

        overrides.setHidden(id, false);
        assertFalse(overrides.isHidden(id));
        assertEquals("othermod:frame", overrides.anchor(id).relativeTo());

        overrides.set(id, 8, 9);
        overrides.setHidden(id, true);
        overrides.clearAnchor(id);
        assertEquals(null, overrides.anchor(id));
        assertEquals(new HudLayoutOffsets.Offset(8, 9), overrides.get(id));
        assertTrue(overrides.isHidden(id));
    }

    @Test
    public void editorOffsetsKeepAnchoredChildrenAttached() {
        PositionElement root = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 15, 30);
        PositionElement child = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMLEFT, "moda:health", 0, 6);
        var original = AnchorLayout.resolve(640, 360, List.of(
                new AnchorLayout.Frame("moda:health", root, 180, 18),
                new AnchorLayout.Frame("modb:mana", child, 180, 12)
        ));

        HudLayoutOffsets overrides = new HudLayoutOffsets();
        overrides.set("moda:health", 34, -8);
        var moved = AnchorLayout.resolve(640, 360, List.of(
                new AnchorLayout.Frame("moda:health", adjusted("moda:health", root, overrides), 180, 18),
                new AnchorLayout.Frame("modb:mana", adjusted("modb:mana", child, overrides), 180, 12)
        ));
        assertEquals(original.get("moda:health").x() + 34, moved.get("moda:health").x());
        assertEquals(original.get("modb:mana").x() + 34, moved.get("modb:mana").x());
        assertEquals(original.get("modb:mana").y() - 8, moved.get("modb:mana").y());

        overrides.set("modb:mana", -9, 5);
        var movedChild = AnchorLayout.resolve(640, 360, List.of(
                new AnchorLayout.Frame("moda:health", adjusted("moda:health", root, overrides), 180, 18),
                new AnchorLayout.Frame("modb:mana", adjusted("modb:mana", child, overrides), 180, 12)
        ));
        assertEquals(moved.get("modb:mana").x() - 9, movedChild.get("modb:mana").x());
        assertEquals(moved.get("modb:mana").y() + 5, movedChild.get("modb:mana").y());
    }

    @Test
    public void changingPointAndAnchorMovesWidgetAccordingToNewGeometry() {
        PositionElement parent = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 20, 30);
        PositionElement child = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMLEFT, "moda:parent", 5, 6);
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        overrides.set("modb:child", 3, -2);
        var original = positioned(parent, child, overrides);

        overrides.setAnchor("modb:child", new HudLayoutOffsets.AnchorOverride("moda:parent", ElementAnchor.BOTTOMRIGHT, ElementAnchor.BOTTOMLEFT));
        var pointChanged = positioned(parent, child, overrides);
        assertEquals(original.get("modb:child").x() - 60, pointChanged.get("modb:child").x());
        assertEquals(original.get("modb:child").y() - 12, pointChanged.get("modb:child").y());
        assertEquals(new HudLayoutOffsets.Offset(3, -2), overrides.get("modb:child"));

        overrides.setAnchor("modb:child", new HudLayoutOffsets.AnchorOverride("moda:parent", ElementAnchor.BOTTOMRIGHT, ElementAnchor.TOPRIGHT));
        var anchorChanged = positioned(parent, child, overrides);
        assertEquals(pointChanged.get("modb:child").x() + 90, anchorChanged.get("modb:child").x());
        assertEquals(pointChanged.get("modb:child").y() - 18, anchorChanged.get("modb:child").y());
    }

    @Test
    public void reparentingMovesWidgetAndCircularParentsAreRejected() {
        PositionElement parent = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 20, 30);
        PositionElement child = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMLEFT, "moda:parent", 5, 6);
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        var original = positioned(parent, child, overrides);
        overrides.setAnchor("modb:child", new HudLayoutOffsets.AnchorOverride("screen", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMLEFT));
        var reparented = positioned(parent, child, overrides);
        assertEquals(5, reparented.get("modb:child").x());
        assertEquals(360 + 6, reparented.get("modb:child").y());
        assertFalse(original.get("modb:child").equals(reparented.get("modb:child")));

        overrides.setAnchor("modb:child", new HudLayoutOffsets.AnchorOverride("moda:parent", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMLEFT));
        overrides.setAnchor("moda:parent", new HudLayoutOffsets.AnchorOverride("modb:child", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT));
        assertThrows(IllegalArgumentException.class, () -> positioned(parent, child, overrides));
    }

    private static java.util.Map<String, AnchorLayout.Bounds> positioned(PositionElement parent, PositionElement child, HudLayoutOffsets overrides) {
        return AnchorLayout.resolve(640, 360, List.of(
                new AnchorLayout.Frame("moda:parent", adjusted("moda:parent", parent, overrides), 90, 18),
                new AnchorLayout.Frame("modb:child", adjusted("modb:child", child, overrides), 60, 12)
        ));
    }

    private static PositionElement adjusted(String id, PositionElement base, HudLayoutOffsets overrides) {
        HudLayoutOffsets.Offset offset = overrides.get(id);
        HudLayoutOffsets.AnchorOverride anchor = overrides.anchor(id);
        ElementAnchor point = anchor == null ? base.point : anchor.point();
        ElementAnchor parentAnchor = anchor == null ? base.anchor : anchor.anchor();
        String parent = anchor == null ? base.relativeTo : anchor.relativeTo();
        return new PositionElement(point, parentAnchor, parent, base.x + offset.x(), base.y + offset.y());
    }
}
