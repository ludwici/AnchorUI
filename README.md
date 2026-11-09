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

## Resource packs and optional external directory

**Modpack authors do not need to write a Java mod or create a separate folder in the game root.** They can distribute Anchor UI documents as a standard Minecraft resource pack under `resourcepacks/`:

```text
resourcepacks/
└── MyPack/
    ├── pack.mcmeta
    └── assets/
        └── mypack/
            └── lore/
                ├── hud/
                │   └── status.json
                ├── widgets/
                └── style/
```

Enable the pack in Minecraft's Resource Packs menu. Anchor UI already reads JSON from `assets/<namespace>/lore/` through the client resource reload listener, including resources provided by other mods or enabled resource packs. Resource packs also carry textures and can be reloaded with F3+T.

**Root directory naming:** the formerly reserved top-level `lore/` directory is now named `anchorui/` in the code. It is reserved for a possible standalone external JSON loader, but the current implementation does not load JSON from that folder. Anchor UI no longer creates the empty directory automatically on startup. No migration, rename check or old-directory scan is performed.

Do not confuse the future optional game-root `anchorui/` directory with the existing `assets/<namespace>/lore/` path inside resource packs. The latter remains unchanged for compatibility with existing screens, HUD documents, templates, tests, and styles.

## Resource packs

The bundled demo uses `assets/anchorui/lore/test/block.json` and `assets/anchorui/lore/test/anchors.json`. Layout styles live at `assets/<namespace>/lore/style/<name>.json` and are selected through `"style": "namespace:style/name"` (or `namespace:name`). Styles provide per-type `position`, `size`, `padding`, `text.color` and `background.color`. An element can override these settings, including individual fields nested inside objects.

Two input formats are supported:

- The 1.21.1 format defines `title`, `item`, `scale`, `lore` and optional `style`. Built-in `title`, `model`, and `lore` frames use that style, and can refer to one another by id.
- The 1.20.1 format defines `item` and `entities`, an array of elements with `type` (`anchorui:text`, `anchorui:model`, `anchorui:list`), optional `id`, and layout/style properties. A `list` can contain its own `lore` entries. Both formats can use the 1.21.1 rich-text parser for lore text.

`size.width`/`height` of zero means auto-sizing for text and lore; models default to 128 by 128. JSON dimensions and offsets are scaled GUI pixels. A background color accepts `AARRGGBB`, `RRGGBB`, or an RGBA object. An image field is parsed for future texture rendering but is not yet drawn.

## Programmatic layout

`AnchorLayout.resolve(screenWidth, screenHeight, frames)` accepts frame descriptors and produces immutable `Bounds` for each frame. It does not require a `Screen`, Minecraft client state, or renderer, so it can be reused in other UI systems. The item-lore screen is its first consumer.

## Development

Java 21 and NeoForge 21.1 are required. Run `./gradlew build`; GitHub Actions checks builds and unit tests on the `1.21.1` branch.


## Interactive test scenes

Use `/anchorui test list` or `/anchorui test scene1` in-game to open the built-in client-only test harness. Fourteen scenes exercise anchors, relative chains, dimensions, styling, rich text, item model input, resizing, hidden frames, invalid references, and vanilla StringWidget text alignment and ellipsis. Navigate with the on-screen arrows; press **G** to toggle anchor outlines and **Esc** to close.

See [docs/TEST_SCENES.md](docs/TEST_SCENES.md) for each command and expected result. The scenes are JSON resources under `assets/anchorui/lore/test/scenes`, so they use the same rendering path as ordinary lore screens.

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

See **[Custom Widgets API](docs/CUSTOM_WIDGETS.md)** for complete Java and JSON examples, guarantees, and current limitations.

## Typed widget configuration using records

A third-party mod can declare its custom fields as a Java `record` with a Mojang `Codec<T>` (typically built with `RecordCodecBuilder`) and register it using `AnchorWidgetRegistry.register(type, codec, (context, element, style, config) -> widget)`. Anchor UI parses the full JSON element using `JsonOps.INSTANCE`, ignores unrelated layout fields, and supplies a validated, typed configuration object. Defaults can be declared with `optionalFieldOf` and invariants with `Codec.validate`.

Bad configuration is reported as a `WARN` with the widget type and ID; the problematic widget is skipped without affecting its neighbors. The existing three-parameter `WidgetFactory` overload and raw `element.settings` remain available. The test counter now uses `CounterConfig` and `scene13` demonstrates defaults and validation failures. See [docs/CUSTOM_WIDGETS.md](docs/CUSTOM_WIDGETS.md).

## Namespaced widget IDs and cross-mod references

Widget `id` and `position.relativeTo` now accept `namespace:path`. Unqualified IDs are scoped to the **namespace of the screen resource** (for example, `"id": "header"` inside `assets/my_mod/lore/...` resolves to `my_mod:header`). `"relativeTo": "screen"` remains a special built-in target.

Anchor UI loads reusable widget definitions from **`assets/<namespace>/lore/widgets/<name>.json`** into a reload-safe catalog before it expands screen documents. The definition file's path determines its ID: `assets/librarymod/lore/widgets/header.json` exports `librarymod:header`. Other mods may opt into this definition using a screen's `"imports": ["librarymod:header"]` or `{"ref": "librarymod:header", "id": "consumer:copied_header"}` inside `entities`. `ref` supports deep-merging local JSON overrides.

Within a screen, Anchor UI first declares all widget IDs in a per-screen `WidgetInstanceRegistry`, then creates them using the registered factories, applies styles and dimensions, and finally resolves all anchor relationships. Factories can inspect `context.widgets().definition(qualifiedId)`, and completed instances are exposed by `context.widgets().widget(qualifiedId)`. The registry is scoped to the currently open screen; importing is explicit, so unrelated widgets from other screens are not instantiated automatically.

See [Widget Namespaces and Imports](docs/WIDGET_REFERENCES.md) and test `/anchorui test scene14` for a working cross-namespace demonstration.

## In-game HUD support (NeoForge 1.21.1)

Anchor UI also renders **in-game HUD documents** via a single NeoForge `RegisterGuiLayersEvent` layer ordered above vanilla chat. Add JSON files to `assets/<namespace>/lore/hud/<name>.json`. All non-test HUD documents are collected from loaded resource packs and mods, and their widgets are positioned in one shared `AnchorLayout`—including cross-mod `relativeTo` references, `imports` and `ref` templates. Widgets use the same registered factories and styles as regular screens.

The HUD is rendered without an open `Screen`. A HUD factory receives `WidgetContext.screen() == null` and should not depend on screen-only functions. HUD widgets are currently **display-only**: mouse/key interactions (including vanilla `Button` clicks) are reserved for future HUD editor work. F1 hides the HUD, and HUD rendering is suppressed when a normal game screen is open. Layout and widget instances are rebuilt when the GUI size, world or loaded resources change; they are not recreated every frame.

Run `/anchorui test hud1` to toggle a built-in HUD preview (or use `/anchorui test hud1 on` / `off`). It combines `anchorui:text`, `anchorui:model` and widgets defined in `anchorui_test`, including cross-file anchors and a `ref` override. The preview resource files have `"testOnly": true` and are disabled by default.

See [HUD Documentation](docs/HUD.md) for complete JSON examples and current limitations.

## Player HUD editor

Use **F8** or `/anchorui hud edit` in a world to open the HUD layout editor. Select and drag a widget to reposition it, nudge with arrow keys (Shift for 10-pixel increments), cycle attachment points (which immediately update positions), pick a new parent by clicking another widget, hide/show widgets, reset individual or all overrides, and choose **Save & close**. The editor uses a compact, movable vertical panel at the right edge, with an 83%-scale, scrollable inspector for every widget's current layout values and complete original JSON (including custom fields). The default placement keeps bottom-right anchored widgets accessible. **Tab** hides the panel when needed. **Esc / Cancel** discards the preview. The optional demo HUD can be toggled from the editor.

The editor persists client-specific position, anchor and visibility overrides by namespaced widget ID in `config/anchorui/hud-layout.json`. Position offsets are applied **in addition to the original JSON anchor settings**; changing anchors moves widgets according to the new geometry, while click-to-select parent changes reject cycles. The normal HUD remains display-only; HUD elements continue rendering while the chat screen is open and still hide for F1 and inventory/menu screens.

See [HUD editor and persistence](docs/HUD.md#hud-editor).

The bundled HUD preview uses zero offsets between adjacent widgets, so their boundaries touch. Previously saved per-widget offsets can be cleared with **Reset** or **Reset all** in the editor.
