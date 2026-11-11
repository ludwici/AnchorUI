package com.ludwici.anchorui.client.gui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UiHoverTrackerTest {
    @Test
    public void emitsOnlyHoverTransitionsAndLeavesBeforeEntering() {
        UiHoverTracker tracker = new UiHoverTracker();
        List<String> events = new ArrayList<>();
        tracker.update(null, (id, entered) -> events.add(id + ":" + entered));
        tracker.update("a", (id, entered) -> events.add(id + ":" + entered));
        tracker.update("a", (id, entered) -> events.add(id + ":" + entered));
        tracker.update("b", (id, entered) -> events.add(id + ":" + entered));
        tracker.update(null, (id, entered) -> events.add(id + ":" + entered));
        tracker.update(null, (id, entered) -> events.add(id + ":" + entered));
        assertEquals(List.of("a:true", "a:false", "b:true", "b:false"), events);
    }

    @Test
    public void resetDoesNotEmitActionsForInactivePreview() {
        UiHoverTracker tracker = new UiHoverTracker();
        List<String> events = new ArrayList<>();
        tracker.update("a", (id, entered) -> events.add(id + ":" + entered));
        tracker.reset();
        tracker.update("a", (id, entered) -> events.add(id + ":" + entered));
        assertEquals(List.of("a:true", "a:true"), events);
    }
}
