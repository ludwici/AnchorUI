# Local UI State (NeoForge 1.21.1)

AnchorUI screens can declare lightweight client-only state in JSON and update that state using pre-registered UI actions. This is intended for tabs, selecting a category, showing details, local page numbers, and other **presentation state**. It does not change server data or make game actions authoritative.

## Declare state on a screen

A `state` object at the root of a screen JSON defines the available keys and their initial types:

```json
{
  "state": {
    "tab": "skills",
    "details": false,
    "page": 1
  },
  "entities": [
    {
      "type": "anchorui:button",
      "id": "mymod:inventory_tab",
      "text": "Inventory",
      "onClick": {
        "type": "anchorui:set_state",
        "key": "tab",
        "value": "inventory"
      },
      "size": {
        "width": 120,
        "height": 20
      }
    },
    {
      "type": "anchorui:button",
      "id": "mymod:details_toggle",
      "text": "Show details",
      "onClick": {
        "type": "anchorui:toggle_state",
        "key": "details"
      },
      "size": {
        "width": 120,
        "height": 20
      }
    },
    {
      "type": "anchorui:text",
      "id": "mymod:selected_tab",
      "text": "Current tab: {{anchorui:ui.state#tab}}",
      "size": {
        "width": 220,
        "height": 20
      }
    },
    {
      "type": "anchorui:text",
      "id": "mymod:details_text",
      "text": "Details are expanded",
      "visibleWhen": {
        "bind": "anchorui:ui.state",
        "path": "details",
        "fallback": false
      },
      "size": {
        "width": 200,
        "height": 20
      }
    }
  ]
}
```

State updates from `anchorui:set_state` and `anchorui:toggle_state` are reflected by Conditions and text bindings on the next screen render, without rebuilding unrelated widgets. These actions work on normal AnchorUI **screens**, not display-only HUDs.

Ready-made widgets can change UI state directly: `anchorui:checkbox` and `anchorui:switch` toggle boolean state; `anchorui:select` and `anchorui:tabs` set a selected string/number value. They all emit `onChange` events. See [Built-in Interactive Controls](UI_CONTROLS.md).\n\n## JSON state actions

- `anchorui:set_state`: requires a declared `key` and `value` of the same JSON type as the initial value. Use it for selected tabs, page numbers, and structured values.
- `anchorui:toggle_state`: requires a declared boolean `key`; flips it between `true` and `false`.

State key names use ASCII letters, digits, and underscores, starting with a letter or underscore. Each key needs a non-null initial value. Undeclared keys, type changes and invalid actions fail configuration validation. JSON primitives preserve their kind (string/number/boolean), and arrays or objects must stay arrays or objects. State values are cloned when stored or retrieved, preventing bypasses through mutating a returned `JsonElement`.

The built-in object provider `anchorui:ui.state` exposes a snapshot of the *current screen's* state to any text or Condition binding:

```json
{
  "visibleWhen": {
    "left": {
      "bind": "anchorui:ui.state",
      "path": "tab",
      "fallback": ""
    },
    "op": "eq",
    "right": "skills"
  }
}
```

It can also be referenced as `{{anchorui:ui.state#tab}}` in live text.

## Scope and lifetime

Each newly opened AnchorUI screen starts with independent state. Existing instances keep their state through `init()`, resize, Template List rebuilds and a return via `close_screen`. A **new instance** starts with its declared defaults unless an explicit `open_screen.stateHandoff` transfers selected values. Nothing is persisted to disk and nothing is sent to the server automatically.

The `anchorui:ui.state` provider returns an empty object outside state-aware screens. Conditions should use suitable fallbacks.

## Stage 4 — optional navigation state handoff

The `anchorui:open_screen` action accepts `stateHandoff`, a mapping of **destination state keys to source keys**:

```json
{
  "onClick": {
    "type": "anchorui:open_screen",
    "screen": "mymod:details",
    "stateHandoff": {
      "selectedTab": "tab",
      "details": "details"
    }
  }
}
```

Both screens must declare the corresponding keys in their root `state` objects. Every copied value must match the destination's declared JSON type. Object/array values are deep-copied. Other destination keys remain at their defaults. Without `stateHandoff`, opening a screen behaves exactly as before.

Malformed mappings and undeclared source keys are rejected when compiling the action. Missing or type-incompatible destination keys reject **the entire transition**, without partially applying values or closing the current screen. Missing target documents also leave the source screen open. Runtime navigation errors are logged and shown in the local chat.

On normal navigation, the old screen emits `onClose` and the new one emits `onOpen`. Returning with `close_screen` reopens the previous *instance* (preserving its state), emits `onClose` for the closing screen and `onOpen` for the returned screen. F8 editor transitions suppress those lifecycle events. When a screen is reopened or its widgets are rebuilt, focus is restored by the focused widget's namespaced ID if that widget still exists, is visible and enabled.

### In-game navigation QA

```text
/anchorui screen open anchorui:lifecycle_source_demo
```

Choose a tab and change **Show details**, then click **Transfer state**. The destination should show the selected values while retaining its target-only default. Return with **Back to source**: the original values should remain. **Open with defaults** starts the destination at its defaults. **Try missing screen** leaves the current screen unchanged and reports an error. The two screens use the demo `onOpen`/`onClose` event handler so you can check normal transitions and F8 behavior in chat.

## Java access

A third-party mod can read or update local state using the `UiStateHost` interface:

```java
if (context.screen() instanceof UiStateHost host) {
    UiLocalState state = host.uiState();
    String selectedTab = state.get("tab").getAsString();
    state.set("tab", new JsonPrimitive("inventory"));
    state.toggle("details");
}
```

`state.snapshot()` produces a defensive JSON copy; `get`, `set`, `toggle`, `validateSet`, and `validateToggle` are provided. Access the state on the client thread while interacting with the screen. To modify actual quests, inventories, currency, or permissions, send a validated request to your own server handler; UI state is display-only.

## Manual test

```text
/anchorui screen open anchorui:ui_state_demo
```

Switch **Skills** and **Inventory**, verify the bound tab label updates immediately, toggle **Details** to show/hide the panel, and resize the GUI. Reopen the screen to confirm the original state is restored.

## Resource reload of an open screen

After Minecraft's **F3+T** resource reload, an open AnchorUI screen with a resource document ID detects the new document revision and rebuilds from the refreshed resource pack **without closing and reopening the screen**. Compatible declared Local UI State keys keep their current values; newly declared keys start from the new JSON defaults, and keys whose JSON type changed reset to the new default. The reload does not replay `onOpen`/`onClose` solely because the document changed.

If the new document is missing, the previously loaded UI remains open. If the new layout is rejected, AnchorUI restores the previous document/state and logs the reason. Invalid individual widget settings may still produce skipped-widget diagnostics. Test these behaviors with [Stage 3 QA](QA_STAGE_3.md).
