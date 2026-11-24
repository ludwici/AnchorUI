# In-game HUDs

Anchor UI supports mod-contributed HUD elements in Minecraft **1.21.1 (NeoForge and Fabric)**, sharing widget types, style resolution, namespaced IDs, `ref` templates and the `AnchorLayout` engine with JSON screens.

## Where to put HUD files

Provide one or more files under:

```text
src/main/resources/assets/mymod/anchorui/hud/status.json
src/main/resources/assets/mymod/anchorui/hud/progress.json
```

Each HUD file uses the same `entities` and `imports` structure as a Screen. Active HUDs render together in the game, allowing cross-file and cross-mod anchors, but **F8 edits only the selected HUD**; individual HUD layouts are saved separately.

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

## Vanilla Minecraft HUD anchors (read-only)

Anchor UI now exposes all 25 NeoForge 1.21.1 vanilla HUD layer IDs as **read-only anchor references**, such as `minecraft:hotbar`, `minecraft:crosshair`, `minecraft:player_health`, and `minecraft:boss_overlay`. Custom resource-pack HUD widgets can use them in `position.relativeTo` just like any other element. The vanilla graphics are not moved or otherwise modified.

F8 previews their logical bounds in purple (active-only by default). The `IDs` button switches between active / all / hidden reference rectangles. While picking a custom widget's parent, the inspector lists **every** vanilla ID for direct selection, including contextual HUD layers not currently drawn. Boundaries for variable-size vanilla elements are approximate; see the full catalog and caveats in [Vanilla HUD Anchors](VANILLA_HUD_ANCHORS.md).

Try `/anchorui test vanilla on` to display bundled example widgets attached to the hotbar and crosshair. Disable with `/anchorui test vanilla off`.

## Cross-mod HUD anchors

Suppose Mod A defines `moda:health` in `assets/moda/anchorui/hud/health.json`. Mod B can place a widget below it in `assets/modb/anchorui/hud/mana.json`:

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

An exported `assets/<namespace>/anchorui/widgets/<name>.json` definition can also be included in a HUD using `imports` or instantiated using `ref` and local overrides. Reusable definitions are only instantiated where explicitly referenced; other mods do not populate your HUD automatically.

## Lifecycle and input behavior

NeoForge registers a layer after vanilla chat using `RegisterGuiLayersEvent`; Fabric uses `HudRenderCallback`. The exact layer order can differ between loaders. The manager gathers registered descriptions, creates widgets with `AnchorWidgetRegistry` and `WidgetContext`, calculates shared anchor bounds, then uses `AbstractWidget.render` on each frame.

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

The `hud1` test is independent of the RPG bars. Turn on the RPG demo separately with `/anchorui test rpg on` and disable it using `/anchorui test rpg off`. Turning off `hud1` never turns off `rpg`, and vice versa. Each test-only HUD document can declare a `"testGroup"` alongside `"testOnly": true`; `hud1` remains the fallback group for older test JSON.

The built-in preview contains:

- An imported `anchorui_test:hud_header` text widget (exported from a different namespace).
- `anchorui:hud_body`, an Anchor UI rich-text widget anchored beneath it.
- `anchorui:hud_item`, a Minecraft diamond item widget.
- `anchorui_test:hud_extra`, a cross-document widget anchored beside the diamond.
- `anchorui_test:hud_copy`, a `ref` of the exported header with its own text and coordinates.
- `anchorui_test:hud_corner`, anchored to the bottom-right corner to test resizing and GUI Scale.

Test resources are located at:

```text
assets/anchorui/anchorui/hud/hud1.json
assets/anchorui_test/anchorui/hud/hud1_extra.json
assets/anchorui_test/anchorui/widgets/hud_header.json
```

Preview state is a local session setting, not a saved player preference. Resource packs can customize or replace the test JSON files during development.

## Future work

The HUD core covers JSON composition, read-only display and an optional player-facing editor. Future work includes live data bindings, conditional visibility, animations and precise render-layer selection.

## Creating HUD widgets in F8

Use **+ Add widget** in the HUD inspector to create Text, Image, Panel, Container, Button, or Progress directly in the live preview. Each new widget receives a unique namespaced ID and starts near the screen center. If a Panel or Container is selected, the new widget becomes its child (including automatic Grid placement). Drag and customize the new element just like an existing resource-authored widget.

The **Delete** action is enabled only for editor-created widgets; bundled/resource-pack widgets cannot be deleted this way. **Save**, **Cancel**, and the unsaved-changes exit dialog also account for added and deleted widgets. The full `createdWidgets` definition is saved alongside positions and appearance overrides in `config/anchorui/hud-layout.json`; it is local to this client, not a modification to resource packs.

The list of creatable types is extensible from Java with `EditorWidgetCreation.register(type, defaultsSupplier)`. New types must supply valid widget settings so the editor can construct them safely; registration of a rendering factory alone does not imply the type can be created from an empty definition.

## Compact inspector tabs

The HUD editor now uses a short, three-row action area and five tabs: **Place**, **Layout**, **Style**, **Anims** and **More**. Place shows position and anchors, Layout shows auto-layout options, Style contains visual/content settings, Anims handles animation tracks, and More shows read-only source JSON and diagnostics. Changed values use colored text rather than side stripes; square controls center their symbols. Nothing is shown in the inspector until a widget is selected. F8 still remembers the selected HUD, and all edit actions keep their existing Save/Discard behavior.

## HUD editor

The F8 inspector has a collapsed **DIAGNOSTICS** category when HUD widget construction or layout resolution reports problems. Expand it to see the current error and up to 24 unique warnings, with the full diagnostic trail retained in `latest.log`. This includes invalid widget configurations and unresolved anchor references detected during HUD rebuilds. Rejected editor anchor/layout choices display the specific validation reason when available, without applying invalid changes. Diagnostics clear when HUD state is reset and are refreshed on rebuild. The section also displays errors and warnings from HUD resources and shared templates/styles/definitions after a reload, excluding unrelated screen documents. See [Resource reload and diagnostics](RESOURCE_RELOAD.md) for details and a manual F3+T test checklist.

The F8 HUD editor opens an unsaved-changes confirmation on **Esc**, **F8** or **Cancel** when preview settings differ from the last saved configuration. Choose **Discard & Exit** (restore saved state), **Save & Exit**, or **Cancel** (stay in the editor). **Save** and **Ctrl+S** remain explicit save actions. Returning layout, appearance, animation or anchor values to their resource defaults automatically removes redundant overrides, so a fully reverted edit does not trigger the prompt. The confirmation renders over the existing preview, without replacing the HUD editor screen.

When a custom widget is selected, the expanded **ANCHORS (N)** category lists every effective anchor constraint in a compact collapsible row. Expand an individual anchor to choose **Point** and **Anchor** from dropdowns, choose the **Relative to** target (including vanilla HUD references), and adjust X/Y by typing an integer or using +/- (Shift for 10-pixel steps). **[+] Add anchor** duplicates an existing constraint; **[-] Delete this anchor** removes it (at least one must remain). **Reset anchors** restores the original resource definition. Changes are validated before the HUD preview is updated. The old Point/Anchor toolbar controls have been removed. HUD and screen layout settings are saved to pretty-printed, indented JSON with complete anchor arrays; existing compact saved files continue to load.

The editor includes a **separate, draggable Widgets panel** listing every loaded HUD widget, including inactive vanilla reference IDs. Click any entry to select it in the inspector and highlight its on-screen bounds. Hidden custom widgets remain in the list and can be selected without clicking an invisible rectangle. Scroll the list independently of the inspector; drag its title bar to move it.

List order is **vanilla `minecraft:*` IDs first**, then `anchorui:*` (the current HUD editor's mod ID), then other namespaces. Within a namespace group, container children appear **nested below their structural parent** with simple type markers (`[P]` panel/container, `[B]` button, `[T]` text, `[=]` progress, `[I]` image). Click a container's disclosure arrow to fold or unfold its descendants; click its name to select it. Hover a row to see the full namespaced ID and widget type. Cross-namespace children remain in their respective namespace group. Purple vanilla items are read-only and can be inspected but cannot be moved or modified. While **Pick parent** is active, clicking an entry sets the selected custom widget's parent to that ID. The panel also lists vanilla references even if they are currently inactive.

Pressing **Tab** temporarily hides both editor panels while keeping on-screen widget outlines visible.


The HUD editor opens **above the world** and previews only the HUD chosen in the header dropdown. Choose a bundled HUD, a user-created HUD or the legacy unassigned view. Other HUDs are still available as anchors and remain visible in ordinary gameplay. Unlike the normal HUD layer, the editor accepts mouse and keyboard input **for editing placement only**. The world remains fully sharp in the editor: vanilla screen background blurring and fullscreen dimming are intentionally disabled. Only the editor's own toolbar strips and widget-selection overlays are drawn.

Open it with either:

```text
/anchorui hud edit
```

or the **F8** key binding, configurable under Controls → Key Binds → Anchor UI. F8 opens the HUD editor only when no screen is open; while an Anchor UI JSON screen is open, the same shortcut opens its [screen editor](SCREEN_EDITOR.md). Press F8 again inside an editor to save and close it.

To edit widget placement:

1. Click a widget's highlighted rectangle to select it, then **drag** it to the desired position.
2. Use the **arrow keys** to move the selected widget by one GUI pixel, or **Shift + arrows** to move by 10 pixels.
3. Use the **Point ▼** and **Anchor ▼** dropdowns to pick one of the nine attachment points directly. The selected value applies immediately and keeps existing offsets. The same dropdown widget is used in the component inspector for layout mode, alignment, distribution, and custom Java `CHOICE` fields. Numeric fields still use **- / +**, while Boolean layout and appearance settings use checkboxes.
4. Select a widget, then press **Change parent**. Click another highlighted widget to choose it as the new parent, or click an empty part of the world to use **screen** as the parent. The parent changes **only after the click**. Circular relationships and attempts to parent a widget to itself are rejected. Press **Esc** or **Cancel parent** to leave selection mode without changing anything.
5. Use **Hide / Show** to control whether a selected widget is displayed in gameplay. Hidden widgets keep their layout bounds by default, so other widgets can remain attached. In a parent with automatic row/column/grid layout, `layout.collapseHidden: true` instead lets visible siblings close the gap and excludes hidden children from scroll overflow.
6. Press **R** to reset the selected widget's **position offset only**. Use **Reset** to restore the selected widget's overrides (position, anchors, visibility and component layout values).
7. Controls now appear in a **narrow, vertical panel at the right edge**. Drag the panel by its title bar to move it anywhere on-screen. Its default placement leaves the bottom-right HUD widget uncovered. Press **Tab** to hide the panel temporarily, then press Tab again to restore it.
8. **Show demo / Hide demo** toggles the bundled HUD test resources for easier testing.
9. Choose **Save** (or Ctrl+S) to persist the current settings and close, or **Cancel / Esc** to discard all unsaved changes.

The editor saves the player's client-side layout overrides at:

```text
.minecraft/config/anchorui/hud-layouts/<namespace>/<hud-name>.json
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



### Magnetic alignment when dragging

In the HUD editor (F8 or `/anchorui hud edit`), widgets now snap to nearby **visible** widgets while being dragged:

- Snap threshold: **6 GUI pixels** independently for X and Y (based on the unsnapped cursor position).
- Match left/top edges, right/bottom edges, centers, and opposite edges (to place two widgets immediately next to one another).
- Cyan alignment guidelines appear only while snapping. Hold **Alt** while dragging for unrestricted pixel-precise placement.
- Descendants anchored to the widget being dragged are ignored as snap targets, because they move together with their parent.
- The magnet changes only the dragged widget's user offset; it does **not** silently change its `relativeTo`, `point`, or `anchor`.
- Arrow-key nudging (1 px, or 10 px with Shift) stays precise and does not snap. Save/Cancel remain unchanged; selected HUDs save to individual files under `hud-layouts/`, while legacy unassigned widgets keep `hud-layout.json`.

### Movable, scrollable property inspector

The editor's right-side panel has a narrow width (218 GUI pixels where space permits) and a smaller font for inspector text (83% scale). Drag the **panel's title bar** to reposition it. The panel's default height leaves the bottom-right corner of the game available for HUD selection. Panel position is a temporary UI state; widget layout changes are saved separately.

Select any widget to see its computed layout properties and original JSON configuration. The scrollable inspector displays the widget's full namespaced ID, actual bounds, current parent, point, anchor, saved offset and visibility, followed by **all fields from the original JSON**, including custom nested objects and arrays supplied by other mods. Long values wrap to additional lines. Use the **mouse wheel** over the inspector or drag its scrollbar to see the remaining fields.

The editor draws ID labels for the currently selected or hovered widget, avoiding overlapping labels from nearby widgets. Ordinary HUD widgets still do not receive clicks outside the editor.

### Reset individual properties

The scrollable property inspector displays a small **R** button on the right of each editable field: `parent`, `point`, `anchor`, `user offset` and `visible`. When a field differs from its original JSON configuration, its reset button is enabled. Click it to reset **only that field**, leaving unrelated user changes in place.

- Resetting **user offset** sets both X and Y deltas to `0, 0`. The source JSON's own `position.x` and `position.y` remain unchanged.
- Resetting **parent**, **point** or **anchor** restores the respective value from the source JSON while preserving the other two attachment settings. A reset that would create a cyclic layout is rejected.
- Resetting **visible** removes the user's hidden override and returns to the widget's original visibility.

Compact inspector controls (−, +, E, ▼, R, color swatches and checkboxes) all use 11×11 square visuals with a small gap between rows. The inspector buttons follow their lines when scrolling. A grey reset button means the field is already at its default. The regular **Reset** button still restores every editable field of the selected widget, and there is no global Reset all action. Saving remains an explicit action.

### Component-aware inspector (row, column, grid, flex)

Both the HUD editor and the F8 **screen editor** now show live **COMPONENT / LAYOUT** fields when a selected widget is an AnchorUI panel/container or an element structurally nested inside one.

- Select a **panel or container** to change its `layout` mode (`none`, `row`, `column`, `grid`), gaps, alignment/distribution, wrapping, line gap, column count, per-cell sizing and `collapseHidden`. Only fields applicable to the active layout appear. The collapsed-space policy and manual QA are described in [Stage 1/2 QA](QA_STAGE_1_2.md).
- Select a **row/column child** to edit `layoutItem.enabled`, `grow`, `shrink` and min/max dimensions. A **Grid child** is always managed by its Grid; its inspector exposes only optional min/max sizing rather than a misleading Auto item toggle.
- Use **▼** for enum choices, a checkbox for Boolean values, or **- / +** for numeric fields; use the small **R** to remove just that field's override. Modified values are highlighted only when they differ from the original, compiled resource/style defaults. The inspector retains those defaults independently of rebuilt preview widgets; modified fields retain active per-field R reset buttons, and restoring the default clears redundant overrides. Invalid settings (for instance, a minimum larger than a maximum or a cyclic layout) are rejected, leaving the current preview intact.
- Changes reflow the existing widget instances immediately, including the bounds shown by F8, container scroll limits and anchored descendants. The parent/point/anchor editor controls and magnetic dragging still work.
- **Save** persists these settings as client-side overrides, not as changes to the mod/resource-pack JSON. **Cancel** discards them. Legacy layout files without these fields continue to load.

For example, editing a panel's layout and one child may produce:

```json
{
  "version": 1,
  "offsets": {
    "mypack:toolbar": {
      "layout": {
        "type": "row",
        "gap": 8,
        "wrap": true
      }
    },
    "mypack:toolbar/child_1": {
      "layoutItem": {
        "enabled": true,
        "grow": 2,
        "minWidth": 40
      }
    }
  }
}
```

The overrides are merged with resource-defined layout settings. A field reset reverts to the inherited resource/style value; it does **not** erase other fields. The original `config/anchorui/hud-layout.json` schema stays at version 1, and the screen editor uses its existing per-screen file location. No arbitrary widget types, textures or gameplay configuration are added by the editor.

### Move a widget into a container

In the F8 HUD editor, select a widget, press **Pick parent**, then click a panel/container (or its entry in Widgets). It is **immediately moved into that container**. No secondary menu is needed.

- For a **Grid**, every direct child automatically occupies a cell according to the Grid's `columns`, gaps, cell sizes and alignment. No extra `relativeTo`, `/__content` or `layoutItem.enabled` is necessary. Explicit source child positions are ignored by the Grid.
- For a **row/column**, the editor makes the Panel the anchor target and the runtime applies its padding automatically.
- Click empty world in parent-selection mode to detach the widget back to the screen root. Clicking a regular widget or vanilla HUD reference still allows an advanced anchor-target relationship.

The inspector displays the **Panel parent** as a single field. F8 Parent selection prioritizes Panels even when their children overlap them. Structural moves persist locally and are reversible through Cancel, Parent reset ; circular relationships are rejected.

The **SOURCE JSON [+]** group starts collapsed. The renderer no longer injects `__anchorui_container_parent` into nested widget settings and automatically binds children to their Panel, including its padding. Legacy `/__content` references are not supported. Point `relativeTo` at the parent Panel ID instead.

### Visual and content properties in the HUD editor

The same F8 inspector now exposes a **VISUAL / CONTENT** section for built-in widgets and registered custom Java widgets. It can adjust text labels, text and fill/border/accent colors, image/panel texture resource IDs, state-control background skins, progress bar value labels, widths/heights and relevant panel border/radius fields. A mod may expose additional fields with `WidgetEditorRegistry` (see [Custom widgets](CUSTOM_WIDGETS.md)).

- **- / +** changes numeric, Boolean and enumerated options.
- **E** opens a free-form editor for `Kind.TEXT`, or a **texture browser** for `Kind.TEXTURE`. The browser lists active client resource packs, namespaces, expandable folders and PNG or JPG/JPEG images only. PNG uses Minecraft's native texture loader; JPG/JPEG is converted into a dynamic texture for preview and ordinary widget rendering. Other image formats are deliberately excluded; merely having a file in a resource pack does not make it a valid texture. **Hovering** shows a thumbnail with transparency and the image's original resolution; selection does not change the widget until **Use texture** is pressed. The native decoder validates each file before it can be used, including Java-registered texture fields; nonexistent or unreadable resource IDs cannot be committed. Colored swatches open the graphical Color Picker for every `Kind.COLOR` field. Its hue/saturation palette, opacity slider (0–100%) and editable Alpha field (0–255) use the same `anchorui:color_picker` implementation. Alpha changes preserve RGB and serialize partially transparent colors as `#AARRGGBB` (`#RRGGBB` when fully opaque). The checkerboard preview makes transparency visible. Click **Apply** to commit a color override or **Cancel** / **Esc** to discard it. A normal HUD Save is still required to persist the editor session.
- **R** resets just the selected field. The regular **Reset** button clears a selected widget's overrides.
- Invalid values are rejected before or during widget reconstruction, and the previous preview is restored. Edits rebuild the HUD widget instances **on change**, not on every frame.
- Changes persist locally under `appearance` in the selected HUD's `config/anchorui/hud-layouts/` file; source resource packs and mod-provided Java code are unchanged.

For a basic check, enable `/anchorui test rpg on`, open **F8**, select a health progress bar, edit its `Fill` color and `Width`, save and reopen. Use the image/panel demo to browse the available packs and choose another PNG texture. Verify that nonexistent, non-PNG/non-JPEG or unreadable files cannot be selected, and that a resource removed before applying is rejected.

### Demo HUD spacing

The built-in HUD preview now has **zero gap** between its connected elements: the header/body, body/diamond, diamond/extra label and extra label/copied header use `x: 0` or `y: 0` wherever their edges are attached. The bottom-right corner widget keeps its intentional `x: -16, y: -16` screen padding.

HUD-specific overrides in `config/anchorui/hud-layouts/` and legacy unassigned overrides in `config/anchorui/hud-layout.json` are applied in addition to resource-defined offsets. If you previously dragged a demo element, click **Reset** on that widget on the affected widgets to restore edge-to-edge placement; changing the demo JSON does not automatically erase existing saved user preferences.

## Regression checklist

- Enter a world and enable the preview via `/anchorui test hud1 on`.
- Open chat with **T**: the HUD should remain visible. Close chat, open inventory: the HUD should be hidden there.
- Press **F1**: the HUD should disappear and reappear along with the vanilla UI.
- Open `/anchorui hud edit` or **F8**, drag the `anchorui_test:hud_header` widget and verify its children move with it.
- Change the **Point** and **Anchor** of `anchorui_test:hud_extra`: it must move immediately to the corresponding attachment position while retaining its manual offsets. Press **Change parent** and click another widget; the parent changes only after your click. Try to select itself or create a cycle, which must be rejected. Click empty space to choose `screen`, or press Esc to cancel parent selection.
- The bottom-right `anchorui_test:hud_corner` must be directly clickable and draggable with the right-side inspector open. Drag the panel by its title bar, scroll the widget's raw JSON, and confirm that nested properties are visible. Press Tab if the panel overlaps a widget. Test **Hide / Show**, then **Cancel** to restore the previous layout.
- Reopen, drag a widget, choose **Save & close**, and verify the same position persists after reopening and restarting the game.
- Try **Reset** and **Ctrl+S**; the saved file should contain only modified positions, anchors, visibility flags and explicit component layout changes.
- Resize the game or change GUI Scale while HUD is visible to check anchor reflow.

- In the editor, move a widget vertically to create `user offset: 0, 63`, scroll to that row and press its right-hand **R**. Verify the offset becomes `0, 0` without resetting its parent, attachment points or visibility. Reset parent, point, anchor and visibility individually and verify the other user overrides remain intact.

## First dynamic HUD widget: `anchorui:progress`

A resource pack author can now display a live health bar, hunger bar or other numeric meter without writing Java code. The built-in `anchorui:progress` uses the same size, padding, background, color and anchor fields as other widgets. The filled segment is set by `value / max` and clamped to the range 0–100%.

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health",
  "value": {"bind": "anchorui:player.health", "fallback": 0},
  "max": {"bind": "anchorui:player.max_health", "fallback": 20},
  "fillColor": "#D95565",
  "showText": true,
  "size": {"width": 180, "height": 16},
  "background": {"color": "AA231E27"}
}
```

Numeric values can also be literals, for example `"max": 20`. `fallback` is optional. The built-in data IDs are `anchorui:player.health`, `anchorui:player.max_health`, `anchorui:player.food`, `anchorui:player.experience` (0.0 to 1.0) and `anchorui:player.level`. Other mods can supply their own numeric providers.

A bound progress bar reads the data when rendering but **does not recreate the widget or reparse the JSON every frame**. The bar is display-only on the gameplay HUD. For a visual test, use `/anchorui test rpg on` (and `/anchorui test rpg off` to disable it), or enable the standalone sample resource pack from `examples/resourcepack/AnchorUI-Starter`.

## RPG bar consistency

The bundled RPG health and food progress bars now inherit identical padding, track background and overlaid one-pixel border from `anchorui:style/rpg_progress`; each bar supplies only its own fill color and data binding. This prevents mismatched second-bar frames when the shared skin changes. The standalone starter resource pack still supplies matching per-widget styling. Enable the RPG test separately with `/anchorui test rpg on` and use F8 to inspect or override each bar.

### Collapsible editor panels

Use **[-] / [+]** in the Widgets header to collapse or reopen its panel. Click a namespace category heading to collapse or expand the entries in that category without removing or modifying widgets. The properties **Inspector** panel has a separate **[-] / [+]** toggle. Both are draggable, and **Tab** hides both panels. At narrow GUI scales, the inspector automatically starts collapsed if both panels would overlap; opening either panel collapses the conflicting one.

The editor panels are drawn in a foreground render layer above the in-game HUD and its widget labels. This prevents HUD text and outlines from drawing over the editor's panel backgrounds.


### Default list state and center magnet

The Widgets panel initially appears as a collapsed **Widgets [+]** header. Click it to expand; use category headers to collapse individual namespaces. The properties inspector opens independently and remains fully usable while the list is collapsed.

When dragging a custom HUD widget, the magnetic aligner also detects the **screen's horizontal and vertical centerlines** (the center of the widget snaps to these lines). Center snapping has the same 6 GUI-pixel threshold as widget-to-widget snapping and does not activate while holding Alt. The editor draws a guide at the corresponding screen midpoint.

### Shared scroll controls

The Inspector, Widgets list and JSON scrollable containers use the same loader-independent `NativeContainerScrollPanel` controller on both Fabric and NeoForge. Each instance maintains its own scroll offset, wheel input and draggable scrollbar. The controller draws only the scrollbar, leaving widget content, clipping and layout to AnchorUI. Scrollbar colors and widths are configured through its shared style options.

The F8 Widgets panel automatically reduces its expanded height when dragged close to the screen bottom, and grows when moved back up. The list remains scrollable within the available space.


### Collapsible inspector categories

In F8, click the section title to expand or collapse **CURRENT WIDGET**,
**COMPONENT / LAYOUT**, **VISUAL / CONTENT**, **ANIMATION**, or
**SOURCE JSON**. The header shows `[+]` while collapsed and `[-]`
while expanded. Collapsed fields do not occupy rows, and the inspector
scrollbar automatically reflects the new content height. ANIMATION and
SOURCE JSON initially start collapsed to keep the inspector compact.
Section expansion remains consistent when selecting another widget
within the same editor session.

See [Declarative animations](ANIMATIONS.md) for the implemented dynamic
list of named tracks, independent triggers and millisecond delays.
Each track also has its own collapsible settings section.

### Smooth RPG health and food bar fill

The RPG HUD demo now interpolates the **visible filled width** of both
progress bars with `sizeTransition` rather than smoothing the entire
widget's transform. The configured background, frame, padding and
current value text are unchanged. Textured fills are clipped instead
of being stretched. For custom designs, a separate `anchorui:image`
or panel can have its visible width/height bound through the **same**
`sizeTransition` mechanism. See [Animation API](ANIMATIONS.md).
