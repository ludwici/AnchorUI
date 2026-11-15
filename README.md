# Anchor UI

JSON-driven, anchor-based user interface framework for Minecraft 1.21.1 (NeoForge).

The current implementation provides the reusable `AnchorLayout` resolver, plus item-lore screens as a working consumer of the layout engine. The layout and JSON renderer are still evolving; a public widget-factory API is available for third-party integrations.

## WoW-style positioning

Each UI frame may declare a unique `id` and a `position`. `point` is the attachment point *on the frame itself*, `anchor` is the target point *on the referenced frame*, and `relativeTo` identifies that target frame. The reserved identifier `screen` represents the entire GUI.

```json
{
  "type": "anchorui:text",
  "id": "description",
  "text": "Relative to another frame",
  "position": {
    "point": "TOPLEFT",
    "anchor": "BOTTOMLEFT",
    "relativeTo": "header",
    "x": 0,
    "y": 8
  }
}
```

`point` and `anchor` accept `TOPLEFT`, `TOP`, `TOPRIGHT`, `LEFT`, `CENTER`, `RIGHT`, `BOTTOMLEFT`, `BOTTOM`, and `BOTTOMRIGHT`. If either point is omitted, it defaults to the other. `relativeTo` defaults to `screen`. Positive X offsets move right and positive Y offsets move down. The engine resolves references independently of JSON order, recalculates coordinates when the screen is initialized or resized, and rejects duplicate ids and circular dependencies; unknown references fall back to the screen with a warning.

## Resource packs

**Modpack authors do not need to write a Java mod or create a separate folder in the game root.** They can distribute Anchor UI documents as a standard Minecraft resource pack under `resourcepacks/`:

```text
resourcepacks/
└── MyPack/
    ├── pack.mcmeta
    └── assets/
        └── mypack/
            └── anchorui/
                ├── hud/
                │   └── status.json
                ├── widgets/
                └── style/
```

Enable the pack in Minecraft's Resource Packs menu. Anchor UI already reads JSON from `assets/<namespace>/anchorui/` through the client resource reload listener, including resources provided by other mods or enabled resource packs. Resource packs also carry textures and can be reloaded with F3+T.

## JSON layouts and styles

The bundled demo uses `assets/anchorui/anchorui/test/block.json` and `assets/anchorui/anchorui/test/anchors.json`. Layout styles live at `assets/<namespace>/anchorui/style/<name>.json` and are selected through `"style": "namespace:style/name"` (or `namespace:name`). Styles provide per-type `position`, `size`, `padding`, `text.color` and `background.color`. An element can override these settings, including individual fields nested inside objects.

Two input formats are supported:

- The 1.21.1 format defines `title`, `item`, `scale`, `lore` and optional `style`. Built-in `title`, `model`, and `lore` frames use that style, and can refer to one another by id.
- The 1.20.1 format defines `item` and `entities`, an array of elements with `type` (`anchorui:text`, `anchorui:model`, `anchorui:list`), optional `id`, and layout/style properties. A `list` can contain its own `lore` entries. Both formats can use the 1.21.1 rich-text parser for lore text.

`size.width`/`height` of zero means auto-sizing for text and lore; models default to 128 by 128. JSON dimensions and offsets are scaled GUI pixels. A background color accepts `AARRGGBB`, `RRGGBB`, or an RGBA object. Images can be rendered using `anchorui:image`; panels, nested containers, optional clipping and nine-slice textures are also supported. See [Visual Widgets](docs/VISUAL_WIDGETS.md).

## Programmatic layout

`AnchorLayout.resolve(screenWidth, screenHeight, frames)` accepts frame descriptors and produces immutable `Bounds` for each frame. It does not require a `Screen`, Minecraft client state, or renderer, so it can be reused in other UI systems. The item-lore screen is its first consumer.

## Development

Java 21 and NeoForge 21.1 are required. Run `./gradlew build`; GitHub Actions checks builds and unit tests on the `1.21.1` branch.


## Interactive test scenes

Use `/anchorui test list` or `/anchorui test scene1` in-game to open the built-in client-only test harness. Fourteen scenes exercise anchors, relative chains, dimensions, styling, rich text, item model input, resizing, hidden frames, invalid references, and vanilla StringWidget text alignment and ellipsis. Navigate with the on-screen arrows; press **G** to toggle anchor outlines and **Esc** to close.

See [docs/TEST_SCENES.md](docs/TEST_SCENES.md) for each command and expected result. The scenes are JSON resources under `assets/anchorui/anchorui/test/scenes`, so they use the same rendering path as ordinary lore screens.

## Text widgets

`AnchoredTextWidget` delegates text rendering to Minecraft's `StringWidget`. Anchor UI keeps the outer element's anchor, background, padding, dimensions and visibility. JSON `align` for `type: "text"` accepts `left`, `center` and `right` (default `left`). Built-in text is vertically centered and clipped with an ellipsis when too wide. Java callers can pass `Component` rather than a raw string.

## Rich text and multiline text

Text elements accept the same lore tags: `<b>`, `<i>`, `<u>` and `<t id="tooltip text">`. The existing lore parser converts these tags to styled Minecraft Components.
Single-line text continues using vanilla `StringWidget`. Set `"wrap": true`, or include `\n` in text, for multiline wrapping through `Font.split`, preserving formatting and hover tooltips.
Multiline text respects `align`, `padding`, `size`, and uses auto-height when `"size": {"height": 0}`. Run `/anchorui test scene10` to check both modes.
## Namespaced widget types

Every built-in type is now namespaced: `anchorui:text`, `anchorui:model`, and `anchorui:list`. JSON elements should declare fully-qualified identifiers, for example `"type": "anchorui:text"`.

For compatibility, unqualified names like `"text"` are interpreted as `anchorui:text` (never `minecraft:text`). Explicit namespaces are preserved: `othermod:text` is a distinct type, **not** an alias for `anchorui:text`.

In Java, `LoreEntity.UiElement.type` is a Minecraft `ResourceLocation`. Use `WidgetTypeIds.TEXT`, `WidgetTypeIds.MODEL`, and `WidgetTypeIds.LIST` for built-in identifiers. Type-specific style sections may use namespaced keys (such as `"anchorui:text"`). For built-in types only, legacy short keys (such as `"text"`) remain supported; a fully qualified style key takes precedence. A custom namespace never inherits styling intended for `anchorui:text`.

Third-party mods can register namespaced widget factories through `AnchorWidgetRegistry.register`. Unknown widget types are logged and skipped; see [docs/CUSTOM_WIDGETS.md](docs/CUSTOM_WIDGETS.md) for the registration API and an example.

## Extending Anchor UI with your own widgets

External mods register a `WidgetFactory` under a namespaced `ResourceLocation` using `AnchorWidgetRegistry.register` during NeoForge client setup. A factory receives `WidgetContext`, `LoreEntity.UiElement` (including arbitrary JSON settings), and `LoreStyle.Element`; it returns any vanilla `AbstractWidget`. Anchor UI handles layout, configured dimensions, visibility, focus and mouse routing through the normal Minecraft screen.

The demo type `anchorui_test:counter` uses a vanilla `Button` and can be tested with `/anchorui test scene11`. `/anchorui test scene12` checks unknown types and exceptions from factories without breaking the other widgets.

See **[Custom Widgets API](docs/CUSTOM_WIDGETS.md)** for complete Java and JSON examples, guarantees, and current limitations. Mods can also register reusable formatters for JSON bindings and text templates; see **[Data Formatters](docs/FORMATTERS.md)**. JSON screens also support lifecycle, hover, click and change handlers using registered Java actions; see **[UI Events](docs/UI_EVENTS.md)**. Repeated quest/dialogue buttons can be authored using **[Template List and Item Context](docs/TEMPLATE_LIST.md)**, with per-row event data, dynamic updates, Grid Layout and custom Java layout algorithms. JSON screens support stateful tabs, checkboxes, switches and dropdowns through **[Built-in Interactive Controls](docs/UI_CONTROLS.md)** and **[Local UI State](docs/UI_STATE.md)**. Controls can use resource-pack texture skins with normal, hovered, selected and disabled variants; skins support atlases, UV regions, tint, nine-slice and shared reusable style definitions.

## Component-aware F8 editors

Both F8 editors now display **nested components** in the Widgets list (with type hints and collapsible container branches), rather than treating the entire layout as an alphabetic list of unrelated IDs. The inspector can edit generic `layout` and `layoutItem` properties directly: switch `row`/`column`/`grid`, adjust gaps, wrap, alignment, columns and cell dimensions, toggle participation in auto layout, and tune `grow`/`shrink` and size limits.

**Parent selection now distinguishes anchoring to a container frame, anchoring to its padded `__content` area, and actually moving a widget into its row/column/grid structure.** Moves are stored as structural `containerParent` overrides and can be undone through Save/Cancel or parent reset. The inspector shows both the coordinate anchor and the structural parent. Click **SOURCE JSON [+]** to expand the original read-only definition; this group starts collapsed in both editors.

The **- / +** controls apply changes to the current preview and validate them; **R** clears a single overridden field. The **VISUAL / CONTENT** section now also edits built-in text, colors, texture resource paths and common size/style settings. Press **E** to edit text/color/texture fields, then Enter to apply or Esc to cancel. Java widget authors can register additional editable, typed fields using `WidgetEditorRegistry.register(...)`. All changes use local `config/anchorui/` overrides; resource packs and server gameplay logic are not modified. Custom fields must be explicitly registered and event handlers remain read-only.

See [HUD editor](docs/HUD.md#component-aware-inspector-row-column-grid-flex) and [Screen editor](docs/SCREEN_EDITOR.md#editing-the-actual-anchorui-layout-components).

## Automatic layouts for panels and containers

Ordinary `panel` and `container` widgets support opt-in `row`, `column` and **standalone `grid`** layouts. They use the same anchors, scrolling, padding, clipping, transform and editor systems as manually positioned children.

```json
{
  "type": "anchorui:panel",
  "layout": {"type": "row", "gap": 8, "align": "center", "wrap": true},
  "size": {"width": 260, "height": 65},
  "children": [
    {"type": "anchorui:button", "text": "Fixed", "size": {"width": 70, "height": 20}},
    {
      "type": "anchorui:button",
      "text": "Flex",
      "size": {"width": 70, "height": 20},
      "layoutItem": {"grow": 1, "shrink": 1, "minWidth": 40}
    }
  ]
}
```

Stack children can use weighted `layoutItem.grow` / `shrink` and min/max sizes; these weights default to zero for backward compatibility. Stacks support `wrap`, `lineGap`, `justify` and `align`. A standalone grid uses `layout: {"type":"grid","columns":3,"columnGap":6,"rowGap":8}` plus optional cell sizing and per-cell alignment. `collapseHidden` remains opt-in and compacts both stacks and grids when Conditions hide children. Explicit anchors and `layoutItem: false` still opt out of automatic placement.

The existing `template_list.layout` grid/custom layouts remain separate and unchanged. See [Visual widgets and automatic container layouts](docs/VISUAL_WIDGETS.md#automatic-container-layouts-row-column-and-grid) for syntax and limits.

Try:

```text
/anchorui screen open anchorui:stack_layout_demo
/anchorui screen open anchorui:advanced_layout_demo
```

## Conditional content and guarded actions

Resource packs can select the first matching widget variant through `anchorui:cases`, using the existing Conditions grammar and optional `otherwise` fallback. Variant widgets are expanded during document loading and switch visibility without recreating the screen. Cases support ordinary nested widgets, named templates, and repeated `template_list` entries.

Registered action objects can also declare a `when` condition, evaluated **at event time**. A false result skips the action without changing the widget's visible/disabled appearance. Conditions and action guards are client-side UI behavior, not server authorization.

Try both features with:

```text
/anchorui screen open anchorui:conditional_content_demo
```

See [Conditions](docs/CONDITIONS.md#conditional-widget-content-anchoruicases) and [UI Events](docs/UI_EVENTS.md#action-time-guards-when) for the JSON schemas, examples and safety notes. A standalone dialogue/quest engine is intentionally outside AnchorUI's framework scope.

## Typed widget configuration using records

A third-party mod can declare its custom fields as a Java `record` with a Mojang `Codec<T>` (typically built with `RecordCodecBuilder`) and register it using `AnchorWidgetRegistry.register(type, codec, (context, element, style, config) -> widget)`. Anchor UI parses the full JSON element using `JsonOps.INSTANCE`, ignores unrelated layout fields, and supplies a validated, typed configuration object. Defaults can be declared with `optionalFieldOf` and invariants with `Codec.validate`.

Bad configuration is reported as a `WARN` with the widget type and ID; the problematic widget is skipped without affecting its neighbors. The existing three-parameter `WidgetFactory` overload and raw `element.settings` remain available. The test counter now uses `CounterConfig` and `scene13` demonstrates defaults and validation failures. See [docs/CUSTOM_WIDGETS.md](docs/CUSTOM_WIDGETS.md).

## Namespaced widget IDs and cross-mod references

Widget `id` and `position.relativeTo` now accept `namespace:path`. Unqualified IDs are scoped to the **namespace of the screen resource** (for example, `"id": "header"` inside `assets/my_mod/anchorui/...` resolves to `my_mod:header`). `"relativeTo": "screen"` remains a special built-in target.

Anchor UI loads reusable widget definitions from **`assets/<namespace>/anchorui/widgets/<name>.json`** into a reload-safe catalog before it expands screen documents. The definition file's path determines its ID: `assets/librarymod/anchorui/widgets/header.json` exports `librarymod:header`. Other mods may opt into this definition using a screen's `"imports": ["librarymod:header"]` or `{"ref": "librarymod:header", "id": "consumer:copied_header"}` inside `entities`. `ref` supports deep-merging local JSON overrides.

Within a screen, Anchor UI first declares all widget IDs in a per-screen `WidgetInstanceRegistry`, then creates them using the registered factories, applies styles and dimensions, and finally resolves all anchor relationships. Factories can inspect `context.widgets().definition(qualifiedId)`, and completed instances are exposed by `context.widgets().widget(qualifiedId)`. The registry is scoped to the currently open screen; importing is explicit, so unrelated widgets from other screens are not instantiated automatically.

See [Widget Namespaces and Imports](docs/WIDGET_REFERENCES.md) and test `/anchorui test scene14` for a working cross-namespace demonstration.

## F8: HUD and screen layout editors (NeoForge 1.21.1)

Both editors now have a dedicated scrollable, draggable **Widgets** list alongside the properties inspector. HUD mode lists all custom HUD widgets and 25 vanilla read-only references; screen mode shows only the current screen's widgets. Entries are grouped in this order: vanilla `minecraft:*`, current editor namespace, then other namespaces alphabetically. Click an entry to select it or to choose an anchor parent. See [HUD editor](docs/HUD.md) and [Screen editor](docs/SCREEN_EDITOR.md).


Press **F8** with no screen open to edit the HUD. Press **F8** while an Anchor UI screen is open to edit that screen's own widgets. Both editors support dragging, magnetic alignment, changing anchors and parents, hiding, resetting, and saving/cancelling local layouts. Screen overrides live separately per resource ID under `config/anchorui/screens/`; the HUD keeps using `config/anchorui/hud-layout.json`. The editors do not modify source resource pack JSON.

See [Screen Editor](docs/SCREEN_EDITOR.md) for details, controls and the resource-aware `ItemLoreScreen` Java constructor.

## Read-only vanilla HUD references

Custom HUD widgets can now use the **25 vanilla HUD IDs** (the NeoForge 1.21.1 layer names) as anchor targets, for example `"relativeTo": "minecraft:hotbar"`. Their bounds are visible in the F8 HUD editor, and a list in the parent-picker includes inactive / temporary HUD anchors. These are purely positional references: vanilla HUDs remain unchanged. See [Vanilla HUD Anchors](docs/VANILLA_HUD_ANCHORS.md) for the full catalog and known geometry limitations. Test with `/anchorui test vanilla on`.

## In-game HUD support (NeoForge 1.21.1)

Anchor UI also renders **in-game HUD documents** via a single NeoForge `RegisterGuiLayersEvent` layer ordered above vanilla chat. Add JSON files to `assets/<namespace>/anchorui/hud/<name>.json`. All non-test HUD documents are collected from loaded resource packs and mods, and their widgets are positioned in one shared `AnchorLayout`—including cross-mod `relativeTo` references, `imports` and `ref` templates. Widgets use the same registered factories and styles as regular screens.

The HUD is rendered without an open `Screen`. A HUD factory receives `WidgetContext.screen() == null` and should not depend on screen-only functions. HUD widgets are currently **display-only**: mouse/key interactions (including vanilla `Button` clicks) are reserved for future HUD editor work. F1 hides the HUD, and HUD rendering is suppressed when a normal game screen is open. Layout and widget instances are rebuilt when the GUI size, world or loaded resources change; they are not recreated every frame.

Run `/anchorui test hud1` to toggle the anchoring HUD preview (or use `/anchorui test hud1 on` / `off`). It combines `anchorui:text`, `anchorui:model` and widgets defined in `anchorui_test`, including cross-file anchors and a `ref` override. The preview resource files have `"testOnly": true` and are disabled by default.

See [HUD Documentation](docs/HUD.md) for complete JSON examples and current limitations.

## Player HUD editor

Use **F8** or `/anchorui hud edit` in a world to open the HUD layout editor. Select and drag a widget to reposition it, nudge with arrow keys (Shift for 10-pixel increments), cycle attachment points (which immediately update positions), pick a new parent by clicking another widget, hide/show widgets, reset individual or all overrides, and choose **Save & close**. The editor uses a compact, movable vertical panel at the right edge, with an 83%-scale, scrollable inspector for every widget's current layout values and complete original JSON (including custom fields). The default placement keeps bottom-right anchored widgets accessible. **Tab** hides the panel when needed. **Esc / Cancel** discards the preview. The optional demo HUD can be toggled from the editor.

The editor persists client-specific position, anchor and visibility overrides by namespaced widget ID in `config/anchorui/hud-layout.json`. Position offsets are applied **in addition to the original JSON anchor settings**; changing anchors moves widgets according to the new geometry, while click-to-select parent changes reject cycles. The normal HUD remains display-only; HUD elements continue rendering while the chat screen is open and still hide for F1 and inventory/menu screens.

See [HUD editor and persistence](docs/HUD.md#hud-editor).

The bundled HUD preview uses zero offsets between adjacent widgets, so their boundaries touch. Previously saved per-widget offsets can be cleared with **Reset** or **Reset all** in the editor.

## Modpack authoring: dynamic data and dialogue screens

**No Java mod required for UI configuration.** Create and enable a Minecraft 1.21.1 resource pack with `pack.mcmeta` and JSON under `assets/<namespace>/anchorui/`. The standard resource pack format for Minecraft 1.21.1 is `34`.

The new built-in `anchorui:progress` widget supports numeric data bindings. A value may be a literal number or a `{"bind": "anchorui:player.health", "fallback": 0}` reference to a registered provider. Built-in client sources currently include `anchorui:player.health`, `anchorui:player.max_health`, `anchorui:player.food`, `anchorui:player.experience` and `anchorui:player.level`. Other mods can register numeric providers using `NumericDataRegistry.register`.

For dialogue-style screens, `anchorui:button` accepts `onClick` actions. Currently allowed bundled actions are `anchorui:open_screen` and `anchorui:close_screen`. Open a resource-pack screen directly using `/anchorui screen open namespace:name`. HUD widgets remain display-only; clickable buttons only work in normal `Screen` interfaces.

The repo includes a **ready-to-copy resource pack** at [examples/resourcepack/AnchorUI-Starter](examples/resourcepack/AnchorUI-Starter). It contains a live RPG HUD and a two-page dialogue. See [Modpack Authoring Guide](docs/MODPACK_AUTHORING.md) for paths, JSON examples, installation and current safety limits. Quest actions and rewards will be connected through server-authorized data/actions in a later milestone.

### Separate HUD demos and short screen IDs

Use `/anchorui test hud1 on` for anchoring/reference tests and `/anchorui test rpg on` for the live health/food RPG demo. They are independently toggled and default to off. For resource-pack screens, use `/anchorui screen open mypack:traveler`; the loader looks under `assets/mypack/anchorui/screens/traveler.json` automatically. Unquoted namespaced IDs (including `:`) work in the command. Screen button actions may likewise reference `"screen": "mypack:traveler_next"`.

## Conditions: visibility and enabled states

Anchor UI supports **compiled JSON Conditions** that extend the old boolean `visibleWhen` and add `enabledWhen` to any HUD or screen widget. Combine checks using `all`, `any`, `not`, compare bound numbers/strings and inspect lists via the existing namespaced data registry. Conditions compile when widgets load, use short-circuit evaluation, and do not create tick loops. Missing data cannot automatically enable an `enabledWhen`-guarded widget. Try `/anchorui screen open anchorui:conditions` or `/anchorui test conditions on`. Other mods can register binary or unary custom operators through `ConditionOperationRegistry.register(ResourceLocation, ...)` or `registerUnary(ResourceLocation, ...)`, then reference namespaced values like `"op": "mymod:custom_op"` in JSON. See [Conditions](docs/CONDITIONS.md) for examples, registration details, fallback behavior and security notes.

## Typed Data Binding for modpack authors

Anchor UI now supports **number, string, boolean, object and list bindings** through a shared namespaced registry. Built-in `anchorui:text` can display dynamic strings and `{{namespace:value}}` templates, `visibleWhen` conditionally shows any screen/HUD widget, `anchorui:data_list` renders arrays, and `anchorui:progress` can read numbers from nested objects with `path`. Existing `NumericDataRegistry` registrations remain compatible.

Run `/anchorui test binding on` to view a live data HUD, and `/anchorui screen open anchorui:bindings` to check the matching JSON Screen. The starter resource pack includes `/anchorui screen open mypack:bindings`.

External mods can publish values through `DataValueRegistry.registerNumber`, `registerString`, `registerBoolean`, `registerObject` and `registerList`. `registerMutable` provides a publish-on-event cache without a tick loop.

See [Typed Data Binding](docs/DATA_BINDINGS.md) for supported JSON syntax, data types, providers, paths, fallbacks and Java examples. Only resource packs are supported for standalone authoring; no direct game-root JSON loader is planned.

## Textures, panels and nested containers

New `anchorui:image`, `anchorui:panel` and `anchorui:container` types support resource-pack textures, framed backgrounds, and nested `children` using the existing anchoring system. Containers provide a padded content area and clip descendants to their available space. The same format works in HUD documents and regular screens. Open `/anchorui screen open anchorui:visual_demo` for a demo, and see [Visual Widgets](docs/VISUAL_WIDGETS.md) for JSON examples.

## Automatic container sizing and scrolling

JSON `anchorui:panel` / `anchorui:container` support opt-in `autoSize` (width, height, both) and scrollable fixed-size content viewports (`scroll: "vertical"`, `"horizontal"` or `"both"`). Legacy fixed layouts remain unchanged. See [Visual Widgets](docs/VISUAL_WIDGETS.md) and try `/anchorui screen open anchorui:auto_scroll_demo` for a nested auto-size/scrolling demo. Scrollable viewports clip descendants and use the existing anchor/scale transforms; specialized item lists should still use native Minecraft/NeoForge list and scroll widgets.

## Performance

HUD anchor geometry is now recomputed only when needed, panel/9-slice rendering reuses geometry, and the F8 widget lists cache sorting and render only their visible rows. Bound values remain responsive to gameplay updates. See [Performance notes](docs/PERFORMANCE.md) for details, profiling guidance and remaining opportunities.
