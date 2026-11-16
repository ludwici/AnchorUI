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
| `onClose` | Screen document root | Normal screen closure or navigation/replacement, not an F8 editor transition | JSON null |

These events are supported on resource-pack **screens**. Regular HUD overlays remain visual and do not capture mouse input. F8 preview rendering does not fire widget hover or change events; entering or leaving the F8 editor does **not** fire `onClose`/`onOpen`. Normal navigation fires `onClose` on the outgoing screen and `onOpen` on the incoming one, including returning to a previous screen instance.

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

The `UiEvent` record exposes `name()`, `targetId()`, `value()`, and `item()`. Widget IDs are resolved, namespaced IDs from the AnchorUI widget registry; screen events use the document ID (or `namespace:screen` for programmatically constructed screens). The `value()` accessor returns a defensive copy.

Both the old and new actions compile once at screen construction. Duplicate registrations fail immediately. Unknown IDs and malformed event actions are rejected at screen/widget creation. Compilation errors identify the offending JSON event field (for example `Invalid UI event 'onClick': Unknown UI action type: mymod:missing_action`); the screen/widget build log identifies the document or widget. Runtime action failures are logged once per event binding and do not crash the screen.

## Template List event context

Events from a repeated template row carry its current item object. For `onClick` and hover events, `event.value()` is that item object; `event.item()` exposes the row object for **all** event types, including `onChange`. For `onChange`, `event.value()` still represents the actual changed value, such as scrollbar offsets. Non-row widgets have `event.item()` equal to JSON null. Java widget/action factories can also inspect `context.itemContext()` to get the row value, key and index.

See [Template List and Item Context](TEMPLATE_LIST.md) for the JSON syntax, live updates and an in-game quest demo.

## Short action syntax

For actions that need no arguments, the event value can be a **namespaced string**. These forms are equivalent:

```json
{
  "onClick": "mymod:accept_quest",
  "onHoverEnter": "mymod:show_hint"
}
```

```json
{
  "onClick": {
    "type": "mymod:accept_quest"
  },
  "onHoverEnter": {
    "type": "mymod:show_hint"
  }
}
```

Use the object form when the Java action needs configurable fields. This applies to all supported event fields, including screen-level `onOpen` and `onClose`. Existing JSON action objects and `UiActionRegistry.register` factories keep working unchanged.

## Action-time guards (`when`)

An action object may include an optional `when` Condition. It uses the same compiled syntax as `visibleWhen` and `enabledWhen`, but it is **evaluated when the event occurs**, not only when the widget is created:

```json
{
  "onClick": {
    "type": "anchorui:toggle_state",
    "key": "selected",
    "when": {
      "bind": "anchorui:ui.state",
      "path": "allowSelection",
      "fallback": false
    }
  }
}
```

This works on `onClick`, `onChange`, hover, and screen `onOpen`/`onClose` handlers, including actions inside repeated `template_list` items. The reserved `when` property is removed from the JSON before the registered Java action factory or its typed Codec receives its arguments. An omitted `when` preserves the previous unguarded behavior.

A false condition (including a missing source without a permissive fallback) skips the action. Invalid expressions or unknown data sources are rejected during binding compilation; provider errors fail closed and are logged. `when` never makes a button visually inactive: use `enabledWhen` for disabled appearance and input suppression. Client-side guards are UI behavior, **not server-side authorization**.

See [Conditions](CONDITIONS.md#action-level-conditions-when) for the full syntax.

## Java: declare typed action arguments with Codec

A mod defines **its own argument names and types** using a Java `record` plus a Minecraft `Codec<T>`. For example, this quest action accepts one required field (`quest`) and two optional fields (`amount`, `silent`):

```java
public record QuestArguments(String quest, int amount, boolean silent) {
    public static final Codec<QuestArguments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("quest").forGetter(QuestArguments::quest),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(QuestArguments::amount),
            Codec.BOOL.optionalFieldOf("silent", false).forGetter(QuestArguments::silent)
    ).apply(instance, QuestArguments::new));
}
```

Use the overload of the action registry that accepts the codec:

```java
UiActionRegistry.registerEvent(
    ResourceLocation.fromNamespaceAndPath("mymod", "accept_quest"),
    QuestArguments.CODEC,
    (context, arguments) -> event -> {
        if ("onClick".equals(event.name())) {
            MyModNetworking.requestQuestFromServer(arguments.quest(), arguments.amount(), arguments.silent());
        }
    }
);
```

Here `MyModNetworking.requestQuestFromServer` is an illustrative method **you implement in your mod**, not an AnchorUI method. It should send a request to your mod's server handler, where quest eligibility and rewards must be validated. Register the action on the client before JSON screens are loaded.

Then the resource pack can supply exactly those fields:

```json
{
  "onClick": {
    "type": "mymod:accept_quest",
    "quest": "mymod:first_quest",
    "amount": 2,
    "silent": true
  }
}
```

Java receives `arguments.quest()`, `arguments.amount()` and `arguments.silent()` as typed values. If `amount` and `silent` are omitted, they default to `1` and `false`. An absent `quest` or a wrong argument type causes a descriptive error when compiling the screen action. To support additional arguments, add fields to the record and its codec. Use `Codec` validation for ranges, allowed values and other constraints, or nested codecs for nested JSON configuration.

The framework removes the reserved `type` field before decoding arguments. Ordinary `Codec` object decoders ignore unrelated fields by default; if rejecting unknown argument names is important, implement explicit strict validation in your own configuration decoder. Do not use `type` as a custom argument name.

`UiActionRegistry.register(id, codec, (context, arguments) -> Runnable)` is also available for actions that do not need `UiEvent` metadata. Existing raw-JSON `register(id, Factory)` and `registerEvent(id, EventFactory)` remain supported when custom parsing is preferable. A string-only action supplies no custom arguments, so actions with required fields must use the object form.

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

`anchorui:open_screen` and `anchorui:close_screen` remain client-only registered actions. `open_screen` optionally accepts `stateHandoff` to copy declared state keys into the new screen; invalid target screens or incompatible transfers fail without closing the source. See [Local UI State](UI_STATE.md#stage-4--optional-navigation-state-handoff). A custom action may also receive the event metadata when registered through `registerEvent`.

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

The built-in `anchorui:checkbox`, `anchorui:switch`, `anchorui:select` and `anchorui:tabs` widgets already implement `UiChangeSource` and report their new state through `onChange`. See [Built-in Interactive Controls](UI_CONTROLS.md).\n\n## Java: emit changes from custom widgets

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
