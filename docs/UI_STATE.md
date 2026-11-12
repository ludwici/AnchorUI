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

## JSON state actions

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

Each newly opened AnchorUI screen starts with its own independent state. Values survive that screen instance's `init()` calls, including a GUI resize or a Template List rebuild, but are reset when the screen is closed and opened as a **new instance**. Switching to a different screen starts a separate state. Nothing is persisted to disk and nothing is sent to the server automatically.

The `anchorui:ui.state` provider returns an empty object outside state-aware screens. Conditions should use suitable fallbacks.

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
