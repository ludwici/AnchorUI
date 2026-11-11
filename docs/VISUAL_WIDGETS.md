# Visual widgets and nested containers

Available on the 1.21.1 NeoForge branch: `anchorui:image`, `anchorui:panel`, and `anchorui:container`.

The same widgets work in JSON screens and in HUD documents. Flat `entities` documents continue to work as before.

## Textures

```json
{
  "type": "anchorui:image",
  "id": "mypack:banner",
  "texture": "mypack:textures/gui/banner.png",
  "textureWidth": 512,
  "textureHeight": 256,
  "u": 0,
  "v": 0,
  "regionWidth": 256,
  "regionHeight": 128,
  "tint": "#FFFFFFFF",
  "size": {"width": 200, "height": 100}
}
```

- `texture` resolves from `assets/<namespace>/...`; without a namespace, the screen's namespace is used.
- `textureWidth` and `textureHeight` are source texture (atlas) dimensions, both defaulting to 256. If no UV region is given, the whole texture atlas is used.
- `u`, `v`, `regionWidth`, and `regionHeight` select a source region. Invalid regions reject only that widget.
- `size` scales the image; `padding` insets it without changing its external anchor.
- `tint` accepts `#RRGGBB` or `#AARRGGBB` (white by default).

## Panels

```json
{
  "type": "anchorui:panel",
  "id": "mypack:card",
  "size": {"width": 240, "height": 100},
  "fillColor": "#D71D283B",
  "borderColor": "#FFE1BE80",
  "borderWidth": 2,
  "radius": 8
}
```

Panels support optional fill, borders, and rounded corners. If `fillColor` is omitted, the usual style `background.color` is used. A panel may contain children. A `container` draws with the same optional appearance, but primarily serves as a logical layout parent.

## Nested children

```json
{
  "entities": [
    {
      "type": "anchorui:container",
      "id": "mypack:window",
      "size": {"width": 260, "height": 140},
      "padding": {"left": 12, "top": 10, "right": 12, "bottom": 10},
      "fillColor": "#CC162738",
      "position": {"point": "CENTER", "anchor": "CENTER", "relativeTo": "screen"},
      "children": [
        {
          "type": "anchorui:text",
          "id": "mypack:title",
          "text": "<b>Title</b>",
          "size": {"width": 200, "height": 20}
        },
        {
          "type": "anchorui:progress",
          "id": "mypack:health",
          "position": {"y": 30},
          "size": {"width": 190, "height": 14},
          "value": {"bind": "anchorui:player.health", "fallback": 0},
          "max": {"bind": "anchorui:player.max_health", "fallback": 20}
        }
      ]
    }
  ]
}
```

- A child with no explicit `relativeTo` is placed at the top-left of its parent's **padded content area**, with the same anchor/point semantics as any other widget. Do not manually add the parent's padding to the child's offset.
- A child can be any registered widget. Nested `panel` and `container` elements may have their own `children`. Existing `ref` templates can be used for children too.
- Descendants are rendered after the parent and hidden whenever their parent is hidden. **Clipping is disabled by default.** Set `"clipChildren": true` on a panel/container to clip its descendants to its padded content area. Multiple clipping ancestors intersect; parents without clipping do not constrain their descendants. Mouse clicks outside opted-in clip areas are ignored.
- Children have normal namespaced IDs; an omitted ID is generated as `<parent path>/child_0`, etc. The internal content frame uses `<parent id>/__content`, which should not be declared as a widget ID.
- Children can still explicitly specify a different `position.relativeTo` target; clipping applies only when an ancestor sets `clipChildren: true`.
- The HUD editor treats containers as ordinary movable frames: dragging a parent automatically moves children anchored to its content frame.

These are **fixed-size layout containers**, not flex/grid/auto-sizing containers. Advanced layout, scroll containers, 9-slice textures, and animated transitions are separate future improvements.

## Smoke tests

Open `/anchorui screen open anchorui:visual_demo` for a bundled example with nested panels, cropped source texture, padding and a bound progress bar. Check resizing, F3+T resource reload, and in-game HUD previews with F8.
