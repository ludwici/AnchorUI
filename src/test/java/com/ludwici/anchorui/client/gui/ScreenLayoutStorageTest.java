package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.ludwici.anchorui.client.hud.HudLayoutStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScreenLayoutStorageTest {
    @TempDir
    Path directory;

    @Test
    public void twoScreensKeepIndependentOverridesEvenWhenWidgetIdsMatch() throws IOException {
        Path firstPath = directory.resolve("screens/anchorui/screens/dialogue_intro.json");
        Path secondPath = directory.resolve("screens/anchorui/screens/visual_demo.json");
        Path hudPath = directory.resolve("hud-layout.json");

        HudLayoutOffsets first = new HudLayoutOffsets();
        first.set("anchorui:heading", 24, -12);
        first.setAnchor("anchorui:heading", new HudLayoutOffsets.AnchorOverride("screen", ElementAnchor.CENTER, ElementAnchor.CENTER));
        HudLayoutOffsets second = new HudLayoutOffsets();
        second.set("anchorui:heading", -7, 16);
        second.setHidden("anchorui:heading", true);

        HudLayoutStorage.save(firstPath, first);
        HudLayoutStorage.save(secondPath, second);

        HudLayoutOffsets reloadedFirst = HudLayoutStorage.load(firstPath);
        HudLayoutOffsets reloadedSecond = HudLayoutStorage.load(secondPath);

        assertTrue(first.sameAs(reloadedFirst));
        assertTrue(second.sameAs(reloadedSecond));
        assertFalse(reloadedFirst.sameAs(reloadedSecond));
        assertEquals(new HudLayoutOffsets.Offset(24, -12), reloadedFirst.get("anchorui:heading"));
        assertEquals(new HudLayoutOffsets.Offset(-7, 16), reloadedSecond.get("anchorui:heading"));
        assertFalse(reloadedFirst.isHidden("anchorui:heading"));
        assertTrue(reloadedSecond.isHidden("anchorui:heading"));
        assertEquals(ElementAnchor.CENTER, reloadedFirst.anchor("anchorui:heading").point());
        assertTrue(HudLayoutStorage.load(hudPath).entries().isEmpty());
    }

    @Test
    public void discardPreviewDoesNotModifySavedLayout() {
        HudLayoutOffsets saved = new HudLayoutOffsets();
        saved.set("anchorui:panel", 10, 20);

        HudLayoutOffsets preview = saved.copy();
        preview.set("anchorui:panel", 60, 90);
        preview.setHidden("anchorui:panel", true);

        assertEquals(new HudLayoutOffsets.Offset(10, 20), saved.get("anchorui:panel"));
        assertFalse(saved.isHidden("anchorui:panel"));
        assertFalse(saved.sameAs(preview));
    }
}
