# Built-in Interactive Controls (NeoForge 1.21.1)

AnchorUI provides four ready-to-use screen widgets that read and update **Local UI State** without any custom Java widget:

| Widget type | Behavior | State type |
| --- | --- | --- |
| `anchorui:checkbox` | Checked/unchecked checkbox with a label | Boolean |
| `anchorui:switch` | On/off switch with a label | Boolean |
| `anchorui:select` | Dropdown selection list with keyboard and mouse input | String or number |
| `anchorui:tabs` | Horizontal row of selectable tabs | String or number |

These controls require a standard AnchorUI **Screen**, not a display-only HUD. Define every `stateKey` at the root of the screen's `state` object. Each control reads the latest state on render, and updating it immediately makes the new value available to text Data Bindings and Conditions.

## Screen example

```json
{
  "state": {
    "tab": "skills",
    "details": true,
    "notifications": false,
    "difficulty": "normal"
  },
  "entities": [
    {
      "type": "anchorui:tabs",
      "id": "mypack:main_tabs",
      "stateKey": "tab",
      "options": [
        {"value": "skills", "label": "Skills"},
        {"value": "inventory", "label": "Inventory"},
        {"value": "quests", "label": "Quests"}
      ],
      "size": {"width": 270, "height": 24}
    },
    {
      "type": "anchorui:checkbox",
      "id": "mypack:details",
      "text": "Show details",
      "stateKey": "details",
      "size": {"width": 220, "height": 22}
    },
    {
      "type": "anchorui:switch",
      "id": "mypack:notifications",
      "text": "Notifications",
      "stateKey": "notifications",
      "size": {"width": 220, "height": 22}
    },
    {
      "type": "anchorui:select",
      "id": "mypack:difficulty",
      "text": "Difficulty",
      "stateKey": "difficulty",
      "options": [
        {"value": "easy", "label": "Easy"},
        {"value": "normal", "label": "Normal"},
        {"value": "hard", "label": "Hard"}
      ],
      "size": {"width": 220, "height": 22}
    }
  ]
}
```

The snippets above show control configuration. For real screens, give each widget a `position` or place it in anchored containers so the widgets do not overlap.

To display the selected tab's content:

```json
{
  "type": "anchorui:panel",
  "id": "mypack:skills_panel",
  "visibleWhen": {
    "left": {"bind": "anchorui:ui.state", "path": "tab", "fallback": ""},
    "op": "eq",
    "right": "skills"
  },
  "size": {"width": 240, "height": 120}
}
```

Or display a chosen value with `"text": "Difficulty: {{anchorui:ui.state#difficulty}}"`.

## Options and validation

`select` and `tabs` accept 1–64 unique options. Either plain values or objects with `value` and a user-facing `label` are supported:

```json
{
  "options": [
    "skills",
    {"value": "inventory", "label": "Inventory"},
    {"value": "quests", "label": "Quest journal"}
  ]
}
```

String and numeric options are allowed (including numeric page IDs). The corresponding state key must already contain a matching value of the same JSON type. The selected value must appear among the options at construction time. Duplicate values, unsupported types, absent keys and mismatched state types cause a descriptive configuration error. `checkbox` and `switch` require a boolean state key. All four widgets require `stateKey`.

No arbitrary values or scripts are evaluated from JSON. State changes are client-side presentation behavior only.

## onChange and onClick

These widgets implement `UiChangeSource`, so they work with existing namespaced UI events:

```json
{
  "type": "anchorui:select",
  "id": "mypack:sorting",
  "stateKey": "sort",
  "options": ["name", "level", "recent"],
  "onChange": "mymod:on_sort_changed",
  "size": {"width": 180, "height": 22}
}
```

The Java action receives the **new selected value** in `event.value()`. Boolean widgets emit a JSON boolean; select/tabs emit the selected option's original JSON primitive (string or number). `onChange` fires only when a value actually changes. The normal `onClick` event still fires for successful user interactions (including clicks that reopen or close the dropdown), so prefer `onChange` for state changes.

The select menu opens outside the widget's normal bounds, above other screen widgets. It shows up to eight options at once and supports scrolling the menu with the wheel, clicking an option, and keyboard Up/Down plus Enter/Space. Escape closes the open menu. Tabs can be selected with the mouse, or stepped with the Left/Right arrow keys while focused.

## Screen lifecycle and Java access

Values are stored in the screen's `UiLocalState` and survive reinitialization/resize or Template List rebuilding within that screen instance. Newly opened screens start at their declared defaults. No state is written to disk or sent to the server.

Java integrations can access this state via `UiStateHost`, and events can be registered through `UiActionRegistry` as with any other AnchorUI widget. The controls' behavior does not depend on custom Java actions being registered.

See [Local UI State](UI_STATE.md) and [UI Events](UI_EVENTS.md) for the complete data and event APIs.

## In-game test

```text
/anchorui screen open anchorui:ui_controls_demo
```

Switch Skills / Inventory / Quests and confirm the conditional label changes. Toggle Show details to hide/reveal the difficulty label. Toggle Notifications. Open the Difficulty dropdown, change selection and confirm the bound text updates. Each change should also be reported in chat through the registered demo `onChange` action. Close and reopen the screen to confirm defaults are restored.
