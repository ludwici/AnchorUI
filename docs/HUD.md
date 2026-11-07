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
- Opening **chat** keeps the HUD visible; opening an inventory, pause menu or other screen hides it until normal gameplay resumes. The HUD editor draws its own interactive preview.
- GUI size changes, resource reloads and switching worlds rebuild the HUD instances and bounds.
- Rendering uses cached widgets. There are no server tick handlers or per-frame JSON parsing.
- HUD widget factories receive `context.screen() == null`. Their default `context.item()` is `ItemStack.EMPTY`.
- The normal HUD stays **display-only**. Mouse and keyboard input affect widget positioning only while the HUD editor is open; individual HUD widgets do not receive gameplay clicks.
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

The HUD core covers JSON composition, read-only display and an optional player-facing editor. Future work includes live data bindings, conditional visibility, animations and precise render-layer selection.

## HUD editor

The HUD editor opens **above the world** and previews all currently loaded HUD widgets, including elements contributed by other mods. Unlike the normal HUD layer, the editor accepts mouse and keyboard input **for editing placement only**. The world remains fully sharp in the editor: vanilla screen background blurring and fullscreen dimming are intentionally disabled. Only the editor's own toolbar strips and widget-selection overlays are drawn.

Open it with either:

```text
/anchorui hud edit
```

or the **F8** key binding, configurable under Controls → Key Binds → Anchor UI.

To edit widget placement:

1. Click a widget's highlighted rectangle to select it, then **drag** it to the desired position.
2. Use the **arrow keys** to move the selected widget by one GUI pixel, or **Shift + arrows** to move by 10 pixels.
3. Use **Point** and **Anchor** to cycle through the nine attachment points. **The widget immediately moves according to the newly selected points**; any previously configured manual offsets remain unchanged.
4. Select a widget, then press **Change parent**. Click another highlighted widget to choose it as the new parent, or click an empty part of the world to use **screen** as the parent. The parent changes **only after the click**. Circular relationships and attempts to parent a widget to itself are rejected. Press **Esc** or **Cancel parent** to leave selection mode without changing anything.
5. Use **Hide / Show** to control whether a selected widget is displayed in gameplay. Hidden widgets still occupy their layout bounds, so other widgets can remain attached.
6. Press **R** or **Reset** to restore the selected widget's default position, anchors and visibility. **Reset all** restores all HUD widget settings in the preview.
7. The controls are grouped in a compact panel **at the top center** of the editor, leaving the bottom-right corner unobstructed by editor buttons. Use **Tab** to hide this panel when a widget overlaps it, then press Tab again to show the controls.
8. **Show demo / Hide demo** toggles the bundled HUD test resources for easier testing.
9. Choose **Save & close** (or Ctrl+S) to persist the current settings, or **Cancel / Esc** to discard all unsaved changes.

The editor saves the player's client-side layout overrides at:

```text
.minecraft/config/anchorui/hud-layout.json
```

This is a regular JSON file, outside resource packs and the mod's built-in JSON resources. Each namespaced widget ID has optional `x` and `y` **deltas from its original anchor offsets**, a `hidden` flag and optional replacement `relativeTo`, `point` and `anchor` properties. Existing JSON defaults are preserved until explicitly edited. If you move the parent of an anchored chain, its child widgets follow naturally, including when the child belongs to a different mod. Child widgets can also receive independent offsets.

A generated override may look like this:

```json
{
  "version": 1,
  "offsets": {
    "moda:health": {
      "x": 16,
      "y": -8,
      "relativeTo": "screen",
      "point": "TOPLEFT",
      "anchor": "TOPLEFT"
    },
    "modb:mana": {
      "hidden": true
    }
  }
}
```

The full replacement anchor triple is stored only if the editor changes an anchor. This keeps source layouts reusable and avoids mutating another mod's resources.

The manager recalculates widget positions while dragging, but does not reconstruct widget instances for each mouse movement. Positions are calculated in GUI pixels and automatically reflow when GUI Scale or window dimensions change.

Point, anchor and parent changes recalculate the layout with the existing JSON offsets and saved manual deltas; the editor does not attempt to preserve the previous on-screen position when changing these attachments.

The editor modifies client layout preferences only. It does not write into another mod's resource files or the shared template registry, and no server-side installation is required for saved layout offsets.

**Important:** HUD widgets remain passive in ordinary gameplay. For example, a third-party `Button` may visually render as part of the HUD but does not receive gameplay mouse clicks. During editing, the editor captures clicks for selection and dragging, while still avoiding the widget's own click handler.

## Regression checklist

- Enter a world and enable the preview via `/anchorui test hud1 on`.
- Open chat with **T**: the HUD should remain visible. Close chat, open inventory: the HUD should be hidden there.
- Press **F1**: the HUD should disappear and reappear along with the vanilla UI.
- Open `/anchorui hud edit` or **F8**, drag the `anchorui_test:hud_header` widget and verify its children move with it.
- Change the **Point** and **Anchor** of `anchorui_test:hud_extra`: it must move immediately to the corresponding attachment position while retaining its manual offsets. Press **Change parent** and click another widget; the parent changes only after your click. Try to select itself or create a cycle, which must be rejected. Click empty space to choose `screen`, or press Esc to cancel parent selection.
- The bottom-right `anchorui_test:hud_corner` must be directly clickable and draggable with the editor panel open. Use Tab only if a widget overlaps the top-center panel. Test **Hide / Show**, then **Cancel** to restore the previous layout.
- Reopen, drag a widget, choose **Save & close**, and verify the same position persists after reopening and restarting the game.
- Try **Reset**, **Reset all** and **Ctrl+S**; the saved file should contain only modified widget positions, anchors and visibility flags.
- Resize the game or change GUI Scale while HUD is visible to check anchor reflow.
