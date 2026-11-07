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

    private static PositionElement adjusted(String id, PositionElement base, HudLayoutOffsets overrides) {
        HudLayoutOffsets.Offset offset = overrides.get(id);
        return new PositionElement(base.point, base.anchor, base.relativeTo, base.x + offset.x(), base.y + offset.y());
    }
}
