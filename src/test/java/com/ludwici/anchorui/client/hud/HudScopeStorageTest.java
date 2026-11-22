package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudScopeStorageTest {
    @TempDir
    Path config;

    @Test
    public void twoHudsKeepIndependentWidgetLayouts() throws IOException {
        ResourceLocation firstId = ResourceLocation.parse("example:hud/left");
        ResourceLocation secondId = ResourceLocation.parse("example:hud/right");
        HudLayoutOffsets combined = new HudLayoutOffsets();
        combined.set("example:label", 11, 22);
        combined.set("example:badge", -8, 14);
        combined.setHidden("example:badge", true);

        HudScopeStorage.save(config, firstId, combined.onlyIds(Set.of("example:label")));
        HudScopeStorage.save(config, secondId, combined.onlyIds(Set.of("example:badge")));

        Map<ResourceLocation, HudLayoutOffsets> loaded = HudScopeStorage.loadAll(config);
        assertEquals(2, loaded.size());
        assertEquals(new HudLayoutOffsets.Offset(11, 22), loaded.get(firstId).get("example:label"));
        assertEquals(HudLayoutOffsets.Offset.ZERO, loaded.get(firstId).get("example:badge"));
        assertEquals(new HudLayoutOffsets.Offset(-8, 14), loaded.get(secondId).get("example:badge"));
        assertTrue(loaded.get(secondId).isHidden("example:badge"));

        HudLayoutOffsets merged = loaded.get(firstId).copy();
        merged.mergeFrom(loaded.get(secondId));
        assertEquals(combined.get("example:label"), merged.get("example:label"));
        assertEquals(combined.get("example:badge"), merged.get("example:badge"));
        assertTrue(merged.isHidden("example:badge"));
    }

    @Test
    public void emptyHudLayoutDeletesOnlyItsOwnFile() throws IOException {
        ResourceLocation firstId = ResourceLocation.parse("example:hud/first");
        ResourceLocation secondId = ResourceLocation.parse("example:hud/second");
        HudLayoutOffsets first = new HudLayoutOffsets();
        first.set("example:one", 10, 10);
        HudLayoutOffsets second = new HudLayoutOffsets();
        second.set("example:two", 20, 20);
        HudScopeStorage.save(config, firstId, first);
        HudScopeStorage.save(config, secondId, second);

        HudScopeStorage.save(config, firstId, new HudLayoutOffsets());
        assertFalse(Files.exists(HudScopeStorage.path(config, firstId)));
        assertTrue(Files.isRegularFile(HudScopeStorage.path(config, secondId)));
    }

    @Test
    public void createdWidgetsStayWithTheirOwner() throws IOException {
        JsonObject widget = new JsonObject();
        widget.addProperty("type", "anchorui:text");
        widget.addProperty("text", "One");
        HudLayoutOffsets all = new HudLayoutOffsets();
        all.addCreatedWidget("example:one", widget);
        all.addCreatedWidget("example:two", widget);

        HudLayoutOffsets first = all.onlyIds(Set.of("example:one"));
        HudLayoutOffsets second = all.onlyIds(Set.of("example:two"));
        assertTrue(first.isCreatedWidget("example:one"));
        assertFalse(first.isCreatedWidget("example:two"));
        assertTrue(second.isCreatedWidget("example:two"));
        assertFalse(second.isCreatedWidget("example:one"));

        ResourceLocation id = ResourceLocation.parse("example:hud/only_one");
        HudScopeStorage.save(config, id, first);
        assertEquals(1, HudScopeStorage.loadAll(config).get(id).createdWidgets().size());
    }
}
