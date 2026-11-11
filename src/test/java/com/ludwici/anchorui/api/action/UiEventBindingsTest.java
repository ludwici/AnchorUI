package com.ludwici.anchorui.api.action;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiEventBindingsTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ui_events_test", path);
    }

    private static JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    @Test
    public void preservesLegacyRunnableActionsForClickAndOtherEvents() {
        AtomicInteger count = new AtomicInteger();
        UiActionRegistry.register(id("legacy"), (context, settings) -> count::incrementAndGet);

        UiActionRegistry.compile(null, json("""
                {
                  "type": "ui_events_test:legacy"
                }
                """)).run();

        UiEventBindings events = UiEventBindings.compile(null, json("""
                {
                  "onOpen": {
                    "type": "ui_events_test:legacy"
                  },
                  "onHoverEnter": {
                    "type": "ui_events_test:legacy"
                  }
                }
                """), "onOpen", "onHoverEnter", "onHoverLeave");
        assertTrue(events.has("onOpen"));
        assertFalse(events.has("onHoverLeave"));
        events.fire("onOpen", "ui_events_test:screen", JsonNull.INSTANCE);
        events.fire("onHoverEnter", "ui_events_test:widget", JsonNull.INSTANCE);
        events.fire("onHoverLeave", "ui_events_test:widget", JsonNull.INSTANCE);
        assertEquals(3, count.get());
    }

    @Test
    public void eventAwareFactoriesReceiveNameTargetAndDefensiveValue() {
        AtomicReference<UiEvent> received = new AtomicReference<>();
        UiActionRegistry.registerEvent(id("typed"), (context, settings) -> received::set);
        UiEventBindings events = UiEventBindings.compile(null, json("""
                {
                  "onChange": {
                    "type": "ui_events_test:typed"
                  }
                }
                """), "onChange");

        JsonObject value = json("""
                {
                  "x": 2,
                  "y": 8
                }
                """);
        events.fire("onChange", "ui_events_test:scroll", value);
        value.addProperty("x", 999);
        assertEquals("onChange", received.get().name());
        assertEquals("ui_events_test:scroll", received.get().targetId());
        assertEquals(2, received.get().value().getAsJsonObject().get("x").getAsInt());
        JsonObject extracted = received.get().value().getAsJsonObject();
        assertNotSame(extracted, received.get().value());
        extracted.addProperty("y", -1);
        assertEquals(8, received.get().value().getAsJsonObject().get("y").getAsInt());
    }

    @Test
    public void rejectsUnknownActionsAndMalformedEventDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> UiEventBindings.compile(null, json("""
                {
                  "onOpen": {
                    "type": "ui_events_test:not_registered"
                  }
                }
                """), "onOpen"));
        assertThrows(IllegalArgumentException.class, () -> UiEventBindings.compile(null, json("""
                {
                  "onHoverEnter": "ui_events_test:not_an_object"
                }
                """), "onHoverEnter"));
        assertThrows(IllegalArgumentException.class, () -> UiEventBindings.compile(null, json("""
                {
                  "onChange": {
                    "type": "not_namespaced"
                  }
                }
                """), "onChange"));
    }

    @Test
    public void onlyCompilesHandlersRequestedByTheCaller() {
        AtomicReference<String> lastEvent = new AtomicReference<>();
        UiActionRegistry.registerEvent(id("limited"), (context, settings) -> event -> lastEvent.set(event.name()));
        UiEventBindings events = UiEventBindings.compile(null, json("""
                {
                  "onHoverEnter": {
                    "type": "ui_events_test:limited"
                  },
                  "onClick": "invalid but outside this binding group"
                }
                """), "onHoverEnter", "onHoverLeave");
        events.fire("onHoverEnter", "ui_events_test:text", new JsonPrimitive("hello"));
        assertEquals("onHoverEnter", lastEvent.get());
        assertFalse(events.has("onClick"));
    }

    @Test
    public void duplicateRegistrationIsRejected() {
        UiActionRegistry.registerEvent(id("duplicate"), (context, settings) -> event -> {});
        assertThrows(IllegalStateException.class, () -> UiActionRegistry.register(id("duplicate"), (context, settings) -> () -> {}));
    }
}
