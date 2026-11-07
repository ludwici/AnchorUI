# Anchor UI

JSON-driven, anchor-based user interface framework for Minecraft 1.21.1 (NeoForge).

The current implementation provides the reusable `AnchorLayout` resolver, plus item-lore screens as a working consumer of the layout engine. This is an early framework prototype, not yet a complete public widget API.

## WoW-style positioning

Each UI frame may declare a unique `id` and a `position`. `point` is the attachment point *on the frame itself*, `anchor` is the target point *on the referenced frame*, and `relativeTo` identifies that target frame. The reserved identifier `screen` represents the entire GUI.

```json
{
  "type": "text",
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

`point` and `anchor` accept `TOPLEFT`, `TOP`, `TOPRIGHT`, `LEFT`, `CENTER`, `RIGHT`, `BOTTOMLEFT`, `BOTTOM`, and `BOTTOMRIGHT`. If either point is omitted, it defaults to the other. `relativeTo` defaults to `screen`. Positive X offsets move right and positive Y offsets move down. The engine resolves references independently of JSON order, recalculates coordinates when the screen is initialized or resized, and rejects duplicate ids, invalid references, and circular dependencies.

## Resource packs

The bundled demo uses `assets/anchorui/lore/test/block.json` and `assets/anchorui/lore/test/anchors.json`. Layout styles live at `assets/<namespace>/lore/style/<name>.json` and are selected through `"style": "namespace:style/name"` (or `namespace:name`). Styles provide per-type `position`, `size`, `padding`, `text.color` and `background.color`. An element can override these settings, including individual fields nested inside objects.

Two input formats are supported:

- The 1.21.1 format defines `title`, `item`, `scale`, `lore` and optional `style`. Built-in `title`, `model`, and `lore` frames use that style, and can refer to one another by id.
- The 1.20.1 format defines `item` and `entities`, an array of elements with `type` (`text`, `model`, `list`), optional `id`, and layout/style properties. A `list` can contain its own `lore` entries. Both formats can use the 1.21.1 rich-text parser for lore text.

`size.width`/`height` of zero means auto-sizing for text and lore; models default to 128 by 128. JSON dimensions and offsets are scaled GUI pixels. A background color accepts `AARRGGBB`, `RRGGBB`, or an RGBA object. An image field is parsed for future texture rendering but is not yet drawn.

## Programmatic layout

`AnchorLayout.resolve(screenWidth, screenHeight, frames)` accepts frame descriptors and produces immutable `Bounds` for each frame. It does not require a `Screen`, Minecraft client state, or renderer, so it can be reused in other UI systems. The item-lore screen is its first consumer.

## Development

Java 21 and NeoForge 21.1 are required. Run `./gradlew build`; GitHub Actions checks builds and unit tests on the `1.21.1` branch.


## Interactive test scenes

Use `/anchorui test list` or `/anchorui test scene1` in-game to open the built-in client-only test harness. Nine scenes exercise anchors, relative chains, dimensions, styling, rich text, item model input, resizing, hidden frames, invalid references, and vanilla StringWidget text alignment and ellipsis. Navigate with the on-screen arrows; press **G** to toggle anchor outlines and **Esc** to close.

See [docs/TEST_SCENES.md](docs/TEST_SCENES.md) for each command and expected result. The scenes are JSON resources under `assets/anchorui/lore/test/scenes`, so they use the same rendering path as ordinary lore screens.

## Text widgets

`AnchoredTextWidget` delegates text rendering to Minecraft's `StringWidget`. Anchor UI keeps the outer element's anchor, background, padding, dimensions and visibility. JSON `align` for `type: "text"` accepts `left`, `center` and `right` (default `left`). Built-in text is vertically centered and clipped with an ellipsis when too wide. Java callers can pass `Component` rather than a raw string.
