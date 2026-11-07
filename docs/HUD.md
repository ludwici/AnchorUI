# In-game HUDs

Anchor UI supports mod-contributed HUD elements in Minecraft **1.21.1 NeoForge**, sharing widget types, style resolution, namespaced IDs, `ref` templates and the `AnchorLayout` engine with JSON screens.

## Where to put HUD files

Provide one or more files under:

```text
src/main/resources/assets/mymod/lore/hud/status.json
src/main/resources/assets/mymod/lore/hud/progress.json
```

Each JSON document has the same `entities` and `imports` structure as a screen. Unlike item-lore screens, **all regular HUD documents from all mods are composed into one shared in-game HUD**, allowing cross-file and cross-mod anchors.

A simple example:

```json
{
  "style": "anchorui:style/default",
  "entities": [
    {
      "type": "anchorui:text",
      "id": "mymod:status",
      "text": "My status",
      "position": {
        "point": "TOPLEFT",
        "anchor": "TOPLEFT",
        "relativeTo": "screen",
        "x": 12,
        "y": 45
      },
      "size": {
        "width": 150,
        "height": 24
      },
      "background": {
        "color": "AA223344"
      }
    }
  ]
}
```

The `mymod:status` widget becomes part of the HUD automatically when the resource is loaded. HUD instances are recreated only when needed, such as after a resource reload, world change or GUI resize.

## Cross-mod HUD anchors

Suppose Mod A defines `moda:health` in `assets/moda/lore/hud/health.json`. Mod B can place a widget below it in `assets/modb/lore/hud/mana.json`:

```json
{
  "entities": [
    {
      "type": "anchorui:text",
      "id": "modb:mana",
      "text": "Mana",
      "position": {
        "point": "TOPLEFT",
        "anchor": "BOTTOMLEFT",
        "relativeTo": "moda:health",
        "x": 0,
        "y": 5
      }
    }
  ]
}
```

The layout solver resolves both widgets together regardless of which HUD document was loaded first. If `moda:health` is unavailable, the anchor falls back to `screen` with a warning, as it does in ordinary screens. Fully qualified widget IDs must be unique across the assembled HUD.

An exported `assets/<namespace>/lore/widgets/<name>.json` definition can also be included in a HUD using `imports` or instantiated using `ref` and local overrides. Reusable definitions are only instantiated where explicitly referenced; other mods do not populate your HUD automatically.

## Lifecycle and input behavior

Anchor UI registers one layer after the vanilla chat GUI layer through NeoForge's `RegisterGuiLayersEvent`. The manager gathers registered descriptions, creates widgets with `AnchorWidgetRegistry` and `WidgetContext`, calculates shared anchor bounds, then uses `AbstractWidget.render` on each frame.

- The HUD is shown during normal gameplay when a player and world exist.
- **F1** hides the custom HUD along with the vanilla GUI.
- Opening an inventory, pause menu or other `Screen` hides the custom HUD until normal gameplay resumes.
- GUI size changes, resource reloads and switching worlds rebuild the HUD instances and bounds.
- Rendering uses cached widgets. There are no server tick handlers or per-frame JSON parsing.
- HUD widget factories receive `context.screen() == null`. Their default `context.item()` is `ItemStack.EMPTY`.
- The initial implementation is **display-only**. Mouse clicks and keyboard focus are intentionally not routed into regular game HUD widgets. An interactive HUD editor is a future feature.
- A malformed or unknown widget factory is logged and skipped. Cyclic anchor references reject the shared layout, matching `AnchorLayout` behavior in normal screens.

## Test-only HUD documents

When creating bundled visual demos, use `"testOnly": true` on the HUD root. Such documents are loaded and validated as normal but remain hidden until explicitly enabled.

```text
/anchorui test hud1
/anchorui test hud1 on
/anchorui test hud1 off
```

The built-in preview contains:

- An imported `anchorui_test:hud_header` text widget (exported from a different namespace).
- `anchorui:hud_body`, an Anchor UI rich-text widget anchored beneath it.
- `anchorui:hud_item`, a Minecraft diamond item widget.
- `anchorui_test:hud_extra`, a cross-document widget anchored beside the diamond.
- `anchorui_test:hud_copy`, a `ref` of the exported header with its own text and coordinates.
- `anchorui_test:hud_corner`, anchored to the bottom-right corner to test resizing and GUI Scale.

Test resources are located at:

```text
assets/anchorui/lore/hud/hud1.json
assets/anchorui_test/lore/hud/hud1_extra.json
assets/anchorui_test/lore/widgets/hud_header.json
```

Preview state is a local session setting, not a saved player preference. Resource packs can customize or replace the test JSON files during development.

## Future work

The HUD core covers JSON composition and read-only display. Later extensions will add live data bindings, conditional visibility, animations, precise render-layer selection and player-editable anchors/positioning, without requiring changes to the current widget type registry.
