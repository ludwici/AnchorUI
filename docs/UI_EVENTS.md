# UI Events (NeoForge 1.21.1)

AnchorUI supports declarative client-side UI event actions in resource-pack screen JSON. Java mods register action factories; resource packs only reference the registered IDs. Events never execute arbitrary Java from JSON or grant server-side permissions.

## Supported events

| JSON field | Place | Trigger | Event value |
| --- | --- | --- | --- |
| `onClick` | Any clickable screen widget | A successful widget click; built-in button also supports keyboard activation | JSON null |
| `onHoverEnter` | Any screen widget | Mouse enters the topmost visible widget's transformed/clipped bounds | JSON null |
| `onHoverLeave` | Any screen widget | Mouse leaves that widget, including when conditions hide it | JSON null |
| `onChange` | Scrollable panel/container | User changes scroll position with wheel or native scrollbar | `{"x": number, "y": number}` |
| `onChange` | Java widget implementing `UiChangeSource` | Widget explicitly notifies its listener of a user-made change | Any JSON value |
| `onOpen` | Screen document root | Screen is first shown, not each frame or resize | JSON null |
| `onClose` | Screen document root | Screen is removed, including navigation/replacement and entering the F8 editor | JSON null |

These events are supported on resource-pack **screens**. Regular HUD overlays remain visual and do not capture mouse input. F8 preview rendering does not fire widget hover or change events; genuine screen removal/return may fire `onClose`/`onOpen`.

Hover actions run only when the hovered widget changes, not on every render frame. Hidden widgets and widgets behind clipping boundaries do not receive hover transitions. An inactive/disabled widget can still be hovered (for example to show a tooltip), but it cannot fire an input change. `onChange` is not a general data-binding update notification: it describes user input.

## Java: register an event-aware action

Existing `UiActionRegistry.register(id, (context, json) -> Runnable)` is unchanged and works for every event. To receive event details, register an event-aware action instead:

```java
UiActionRegistry.registerEvent(
    ResourceLocation.fromNamespaceAndPath("mymod", "ui_event"),
    (context, settings) -> event -> {
        String name = event.name();
        String widgetId = event.targetId();
        JsonElement value = event.value();
        MyClientUiHandler.handle(name, widgetId, value);
    }
);
```

Register on the client during `FMLClientSetupEvent.enqueueWork`, before AnchorUI loads JSON screens. The code above assumes `MyClientUiHandler` is a helper supplied by your mod.

The `UiEvent` record exposes `name()`, `targetId()`, and `value()`. Widget IDs are resolved, namespaced IDs from the AnchorUI widget registry; screen events use the document ID (or `namespace:screen` for programmatically constructed screens). The `value()` accessor returns a defensive copy.

Both the old and new actions compile once at screen construction. Duplicate registrations fail immediately. Unknown IDs and malformed event actions are rejected at screen/widget creation. Runtime action failures are logged once per event binding and do not crash the screen.

## JSON screen and widget actions

```json
{
  "onOpen": {
    "type": "mymod:ui_event"
  },
  "onClose": {
    "type": "mymod:ui_event"
  },
  "entities": [
    {
      "type": "anchorui:text",
      "id": "mymod:hint",
      "text": "Point at this text",
      "onHoverEnter": {
        "type": "mymod:ui_event"
      },
      "onHoverLeave": {
        "type": "mymod:ui_event"
      },
      "size": {
        "width": 160,
        "height": 22
      }
    },
    {
      "type": "anchorui:button",
      "id": "mymod:next",
      "text": "Continue",
      "onClick": {
        "type": "anchorui:open_screen",
        "screen": "mymod:next"
      }
    }
  ]
}
```

`anchorui:open_screen` and `anchorui:close_screen` remain client-only registered actions. A custom action may also receive the event metadata when registered through `registerEvent`.

Scrollable panels can expose `onChange` in exactly the same way:

```json
{
  "type": "anchorui:panel",
  "id": "mymod:quest_list",
  "scroll": {
    "axis": "vertical",
    "step": 18
  },
  "onChange": {
    "type": "mymod:ui_event"
  },
  "size": {
    "width": 200,
    "height": 90
  }
}
```

When a player changes its scroll offset, the Java action receives e.g. `{"x": 0, "y": 36}`. Changes caused by layout recomputation do not emit this event.

## Java: emit changes from custom widgets

A custom registered `AbstractWidget` can implement `UiChangeSource`, which AnchorUI will connect to the widget's JSON `onChange` action automatically.

```java
private Consumer<JsonElement> changeListener = ignored -> {};

@Override
public void setChangeListener(Consumer<JsonElement> listener) {
    changeListener = Objects.requireNonNull(listener);
}

private void userChangedSelection(String selection) {
    changeListener.accept(new JsonPrimitive(selection));
}
```

The snippet is an excerpt from your custom widget class implementing `UiChangeSource`. Notify only on a genuine input change; do not invoke the listener every render or during programmatic setup. The screen suppresses notifications when it is not active or when the source widget is hidden/disabled.

## Testing

Open:

```text
/anchorui screen open anchorui:ui_events_demo
```

The bundled demo registers `anchorui_test:event_notice` as a client-only test action that writes event details to the in-game chat. Opening and closing the screen should show `onOpen`/`onClose`, hovering the highlighted label should show `onHoverEnter`/`onHoverLeave`, clicking the button should show `onClick`, and scrolling the list should show `onChange` with offsets. Repeated stationary hovering must not repeat a message. Close the screen with the bottom button and check it returns to the previous screen.

UI events are presentation/input hooks, not security rules. Use server-authorized networking and validation before applying quest completion, inventory changes, currency spending or other gameplay effects.
