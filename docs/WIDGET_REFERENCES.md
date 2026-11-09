# Widget IDs, namespaces, and cross-mod imports

Anchor UI identifies a **widget factory** by `type` and an individual **widget instance** by `id`. Both support namespaced identifiers, but they have different roles.

```json
{
  "type": "anchorui:text",
  "id": "my_mod:header",
  "text": "Example"
}
```

`type` tells the registry which `WidgetFactory` to call. `id` determines the element's identity and how other widgets refer to it. `position.relativeTo` can point to any widget ID present in the **same assembled screen**, even if that widget was supplied by another mod.

## ID resolution

- `"id": "header"` in an `assets/my_mod/anchorui/...json` screen becomes `my_mod:header`.
- `"relativeTo": "footer"` in that screen refers to `my_mod:footer`.
- `"id": "othermod:header"` or `"relativeTo": "othermod:header"` keeps that namespace.
- `"relativeTo": "screen"` references the current screen and is always available.

All IDs are validated and duplicate fully qualified IDs in one screen are rejected. The anchor solver can resolve references in any element order, and still detects cycles. An anchor targeting an absent widget falls back to the screen with a warning.

## Export a reusable widget from another mod

Mod A exports a widget definition in its resource directory:

**`assets/moda/anchorui/widgets/header.json`**

```json
{
  "type": "anchorui:text",
  "text": "Provided by Mod A",
  "position": {
    "point": "CENTER",
    "anchor": "CENTER",
    "relativeTo": "screen",
    "x": 0,
    "y": -60
  },
  "size": {
    "width": 230,
    "height": 25
  }
}
```

Its globally registered definition ID is **`moda:header`**, derived from the resource path. The definition registry loads all `anchorui/widgets/` resources before processing individual screens; the order in which different mods provide them does not matter.

## Import that widget in another mod

Mod B creates a screen in **`assets/modb/anchorui/my_screen.json`**:

```json
{
  "imports": ["moda:header"],
  "entities": [
    {
      "type": "anchorui:text",
      "id": "modb:description",
      "text": "Attached below Mod A's widget",
      "position": {
        "point": "TOP",
        "anchor": "BOTTOM",
        "relativeTo": "moda:header",
        "x": 0,
        "y": 8
      },
      "size": {
        "width": 230,
        "height": 24
      }
    }
  ]
}
```

The resulting screen contains the imported `moda:header` and the local `modb:description`. Both participate in the same layout calculation. The resource namespace of the importing mod determines the default namespace for its own short IDs.

## Reuse a template with customized settings

A `ref` entry instantiates the exported definition and deep-merges an overlay of settings:

```json
{
  "ref": "moda:header",
  "id": "modb:custom_header",
  "text": "Customized title",
  "position": {
    "relativeTo": "modb:description",
    "y": 12
  }
}
```

Fields absent in the override come from `moda:header`. This enables reusable component presets with independently configured positions, texts, and custom fields. If `id` is omitted, the imported definition retains its original ID; adding the same ID twice to one screen is an error.

Templates may themselves reference another template using `"ref": "othermod:template"`. Circular template inheritance is reported as an error. Missing definitions raise a warning during resource loading; unavailable imports or references are skipped while the other widgets remain available. An anchor to the absent widget falls back to `screen` with its own warning.

## Two registries and lifecycle

`AnchorWidgetRegistry` remains the global registry of **factories by type**.

`WidgetDefinitionRegistry`, available at `LoreLoader.INSTANCE.getDefinitions()`, holds immutable snapshots of **exported JSON definitions by qualified ID**. It is rebuilt on resource reload; querying a definition returns a copy of its JSON.

`WidgetInstanceRegistry`, available from `WidgetContext.widgets()`, is a **per-screen registry**. First, every widget in the assembled screen is declared under its qualified ID. Then Anchor UI asks the type registry to create the widget instances, applies style/dimensions, attaches the instances, and finally positions all widgets using `AnchorLayout`.

This separation matters: a widget in one open screen cannot anchor to a widget visible only in some different screen. Importing a definition explicitly brings a new instance into the current screen. Factories can inspect declared descriptions early using `context.widgets().definition(id)`; `context.widgets().widget(id)` returns an instance once that factory has run.

## Test

Run `/anchorui test scene14`. The scene includes an exported template under `assets/anchorui_test/anchorui/widgets/`, a screen-local widget anchored to it, and a customized counter created with `ref`. Press **G** to inspect IDs and click the counter to check interaction.
