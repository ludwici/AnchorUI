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

### Styling individual dropdown entries

Each `anchorui:select` option may have its own `style`. A style reference resolves from `assets/<namespace>/anchorui/style/<name>.json`; entries without a style keep the widget's shared `textures.option`, `textColor`, `fillColor` and `accentColor` appearance. Only the fields supplied by the option override the corresponding shared behavior.

```json
{
  "type": "anchorui:select",
  "stateKey": "rarity",
  "options": [
    {
      "value": "common",
      "label": "Common"
    },
    {
      "value": "rare",
      "label": "Rare",
      "style": "mypack:rare_option"
    },
    {
      "value": "legendary",
      "label": "Legendary",
      "style": {
        "ref": "mypack:rare_option",
        "textColor": "#FFFFD05A",
        "selected": "#FF9C7428"
      }
    }
  ]
}
```

For `mypack:rare_option`, put this in `assets/mypack/anchorui/style/rare_option.json`:

```json
{
  "anchorui:option": {
    "textColor": "#FFE3C3FF",
    "background": "#554A325F",
    "hover": "#BB6A4B91",
    "selected": "#FF704996",
    "selectedHover": "#FF9572B9",
    "selectedTextColor": "#FFFFFFFF"
  }
}
```

`background`, `hover`, `selected`, and `selectedHover` accept colors or the same texture objects as `textures.option` (including `u`, `v`, `tint`, and `nineSlice`). The `textures` property can also explicitly define option variants in the usual `{"option": {"normal": ..., "hover": ..., "selected": ...}}` shape. Unspecified states fall back to the list's regular rendering; option text can additionally use `hoverTextColor` and `selectedTextColor`.

Use an inline `style` object to avoid a separate file. Inline `{"ref": "mypack:rare_option", ...}` deep-merges overrides with a referenced style. These per-option styles affect **only the dropdown rows**, not the widget's closed field or other choices. The textured controls demo includes both a referenced and an overridden option:

```text
/anchorui screen open anchorui:ui_controls_textured_demo
```

## Action button appearance (opt-in)

`anchorui:button` continues to use the **vanilla Minecraft button** unless you explicitly configure a custom visual style. To opt in, provide a fill/border/hover/disabled color or a `textures` object. The click event behavior and Action Guards are unchanged.

```json
{
  "type": "anchorui:button",
  "id": "mypack:confirm",
  "text": "Confirm",
  "size": {"width": 130, "height": 22},
  "fillColor": "#FF284C67",
  "borderColor": "#FFC6D5E3",
  "hoverColor": "#FF3B7593",
  "disabledColor": "#FF343D48",
  "textures": {
    "background": {
      "normal": "mypack:textures/gui/button_normal.png",
      "hover": "mypack:textures/gui/button_hover.png",
      "disabled": "mypack:textures/gui/button_disabled.png"
    }
  },
  "onClick": {"type": "mypack:confirm_action"}
}
```

The sprite fields accept the same resource-pack texture syntax and resource locations as other skinned AnchorUI controls. A missing state uses the `normal` sprite if supplied. When no background sprite is configured, the optional `fillColor`, `borderColor`, `hoverColor` and `disabledColor` settings draw a simple colored button. Text color uses the existing widget `color`/style property.

Both the HUD and screen F8 editors can edit these fields using **VISUAL / CONTENT**. The `normal`, `hover` and `disabled` paths can be entered with **E** and are saved as local appearance overrides. Custom appearance applies on a deliberate edit; the ordinary vanilla-button path stays unchanged until an explicit style is configured.

## Appearance

All four controls use `size.width` / `size.height` and the existing widget text color. Their fill can inherit the widget style's `background.color`. You can override colors directly in JSON:

```json
{
  "type": "anchorui:switch",
  "text": "Notifications",
  "stateKey": "notifications",
  "fillColor": "#CC1B2534",
  "borderColor": "#99B1C2",
  "accentColor": "#377EA2",
  "size": {
    "width": 210,
    "height": 22
  }
}
```

`fillColor`, `borderColor` and `accentColor` accept `#RRGGBB` or `#AARRGGBB`. The same settings apply to checkbox, select and tabs.

## Textured skins from resource packs

Each built-in control accepts a `textures` object. You can skin the entire control **and** its individual parts; only the parts you specify use textures, while unspecified parts retain the existing color-based rendering.

A texture can use a direct resource location such as `"mymod:textures/gui/toggle.png"` or an object with the same image-region properties as `anchorui:image`: `texture`, `textureWidth`, `textureHeight`, `u`, `v`, `regionWidth`, `regionHeight`, `tint`, and `nineSlice`. Source files live under `assets/<namespace>/textures/` in the mod or resource pack. A single image atlas may provide different appearances by assigning different `u` / `v` regions to each state.

### Checkbox and switch skins

```json
{
  "type": "anchorui:checkbox",
  "text": "Enable effects",
  "stateKey": "details",
  "indicatorSize": 16,
  "textures": {
    "background": {
      "normal": {
        "texture": "mymod:textures/gui/frame.png",
        "textureWidth": 48,
        "textureHeight": 48,
        "nineSlice": {
          "left": 12,
          "top": 12,
          "right": 12,
          "bottom": 12
        }
      },
      "hover": "mymod:textures/gui/frame_hover.png",
      "disabled": "mymod:textures/gui/frame_disabled.png"
    },
    "indicator": {
      "off": "mymod:textures/gui/unchecked.png",
      "on": "mymod:textures/gui/checked.png",
      "offHover": "mymod:textures/gui/unchecked_hover.png",
      "onHover": "mymod:textures/gui/checked_hover.png",
      "offDisabled": "mymod:textures/gui/unchecked_disabled.png",
      "onDisabled": "mymod:textures/gui/checked_disabled.png"
    }
  },
  "size": {
    "width": 180,
    "height": 24
  }
}
```

The checkbox uses `background` across its whole widget and `indicator` for the checked/unchecked box. A switch uses the same `background`, but defines `track` and `thumb` instead of `indicator`. Its dimensions can be customized with `trackWidth`, `trackHeight`, `thumbWidth`, and `thumbHeight`, while the thumb still moves between off/on positions.

For every toggle part (`indicator`, `track`, `thumb`), available variants are `off`, `on`, `offHover`, `onHover`, `offDisabled`, `onDisabled`, and optional `normal`. If an exact hovered/disabled sprite is missing, the corresponding `off`/`on` sprite is used. Without a matching sprite, the existing color renderer remains available.

### Dropdown and tab skins

```json
{
  "type": "anchorui:select",
  "text": "Difficulty",
  "stateKey": "difficulty",
  "options": ["easy", "normal", "hard"],
  "popupRowHeight": 24,
  "textures": {
    "background": {
      "normal": "mymod:textures/gui/selector.png",
      "hover": "mymod:textures/gui/selector_hover.png"
    },
    "arrow": {
      "closed": "mymod:textures/gui/arrow_down.png",
      "open": "mymod:textures/gui/arrow_up.png"
    },
    "popup": "mymod:textures/gui/dropdown_panel.png",
    "option": {
      "normal": "mymod:textures/gui/option.png",
      "hover": "mymod:textures/gui/option_hover.png",
      "selected": "mymod:textures/gui/option_selected.png",
      "selectedHover": "mymod:textures/gui/option_selected_hover.png"
    }
  },
  "size": {
    "width": 180,
    "height": 24
  }
}
```

Select supports `background`, `arrow` (`closed`, `open`, `disabled`), `popup` (`normal`), and `option` (`normal`, `hover`, `selected`, `selectedHover`, `disabled`).

Tabs support `background` plus a `tab` texture for **each tab cell**, with variants `normal`, `hover`, `selected`, `selectedHover`, and `disabled`. Missing state variants fall back to their base state or `normal`. Text remains on top of its textured part, so tab labels and selected values continue to use the existing font/colors.

The popup row height defaults to 20 GUI pixels; the dropdown arrow size defaults to 12. Override with `popupRowHeight` (12–128) and `arrowSize` (4–128). `indicatorSize` defaults to 14 (4–128); switch track and thumb sizes are also bounded to prevent invalid sizes.

### Reuse one skin in a style file

You do **not** need to repeat `textures` in each control. Styles already support reusable per-widget configurations; put skins in `assets/mymod/anchorui/style/rpg_controls.json`:

```json
{
  "anchorui:checkbox": {
    "textures": {
      "indicator": {
        "off": "mymod:textures/gui/off.png",
        "on": "mymod:textures/gui/on.png"
      }
    }
  },
  "anchorui:tabs": {
    "textures": {
      "tab": {
        "normal": "mymod:textures/gui/tab.png",
        "selected": "mymod:textures/gui/tab_selected.png"
      }
    }
  }
}
```

Use `"style": "mymod:style/rpg_controls"` on the screen. Individual widgets can override some states in their own `textures`; the style and widget objects are **deep-merged**, so other states stay inherited. Existing `fillColor`, `borderColor` and `accentColor` remain usable when a texture is not supplied.

### Try the textured demo

```text
/anchorui screen open anchorui:ui_controls_textured_demo
```

This screen shares the interactive controls and Local UI State from `ui_controls_demo`, but uses `anchorui:style/textured_controls`. The bundled example uses the existing `anchorui:textures/gui/nine_slice_panel.png` tinted differently for normal, hover and selected states. Replace it with your own PNGs, or point variants at different regions of a single texture atlas; no Java code changes are required. Use **F3+T** after editing resource-pack textures or JSON styles.

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
