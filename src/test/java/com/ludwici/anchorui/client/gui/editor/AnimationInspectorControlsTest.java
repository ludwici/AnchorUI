package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnimationInspectorControlsTest {
    private static AppearanceInspectorControls.Control field(JsonArray tracks, String name) {
        return AnimationInspectorControls.describe(tracks).stream().filter(control -> control.field().path().equals(name)).findFirst().orElseThrow();
    }

    @Test
    public void addsCopiesReordersAndRemovesUnlimitedNamedTracks() {
        JsonArray tracks = new JsonArray();
        for (int index = 0; index < 12; index++) {
            tracks = AnimationInspectorControls.create(tracks);
        }
        assertEquals(12, tracks.size());
        assertEquals("anim12", tracks.get(11).getAsJsonObject().get("id").getAsString());
        tracks = AnimationInspectorControls.duplicate(tracks, 2);
        assertEquals(13, tracks.size());
        assertEquals("anim13", tracks.get(3).getAsJsonObject().get("id").getAsString());
        tracks = AnimationInspectorControls.move(tracks, 3, -1);
        assertEquals("anim13", tracks.get(2).getAsJsonObject().get("id").getAsString());
        tracks = AnimationInspectorControls.delete(tracks, 2);
        assertEquals(12, tracks.size());
        assertEquals(12, UiAnimationSpec.parse(document(tracks)).tracks().size());
    }

    @Test
    public void editsDelayAndDurationAndCanResetIndividualValues() {
        JsonArray tracks = AnimationInspectorControls.create(new JsonArray());
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.delay"), "1500");
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.duration"), "900");
        assertEquals(1500, UiAnimationSpec.parse(document(tracks)).tracks().getFirst().delayMillis());
        assertEquals(900, UiAnimationSpec.parse(document(tracks)).tracks().getFirst().durationMillis());
        JsonArray invalid = tracks;
        assertThrows(IllegalArgumentException.class, () -> AnimationInspectorControls.set(invalid, field(invalid, "animations.t0.delay"), "-1"));
        tracks = AnimationInspectorControls.reset(tracks, field(tracks, "animations.t0.delay"));
        assertEquals(0, UiAnimationSpec.parse(document(tracks)).tracks().getFirst().delayMillis());
    }

    @Test
    public void supportsCustomFramesAndRenameWithoutCollidingIds() {
        JsonArray tracks = AnimationInspectorControls.create(new JsonArray());
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.id"), "intro_fade");
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.preset"), "custom");
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.from.opacity"), "0");
        assertEquals(0.0, UiAnimationSpec.parse(document(tracks)).tracks().getFirst().from().opacity(), 0.00001);
        tracks = AnimationInspectorControls.create(tracks);
        JsonArray original = tracks;
        assertThrows(IllegalArgumentException.class, () -> AnimationInspectorControls.set(original, field(original, "animations.t1.id"), "intro_fade"));
    }

    @Test
    public void changeTriggerRequiresWatchBeforeEnabling() {
        JsonArray tracks = AnimationInspectorControls.create(new JsonArray());
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.trigger"), "change");
        assertFalse(UiAnimationSpec.parse(document(tracks)).tracks().getFirst().enabled());
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.watch"), "{\"bind\":\"anchorui:ui.state\",\"path\":\"visible\",\"fallback\":false}");
        tracks = AnimationInspectorControls.set(tracks, field(tracks, "animations.t0.enabled"), "true");
        assertTrue(UiAnimationSpec.parse(document(tracks)).tracks().getFirst().enabled());
    }

    private static com.google.gson.JsonObject document(JsonArray tracks) {
        var json = new com.google.gson.JsonObject();
        json.add("animations", tracks);
        return json;
    }
}
