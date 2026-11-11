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
- To keep corners and frame edges from stretching, add a `nineSlice` object with `left`, `top`, `right` and `bottom` source-pixel margins. The image is then drawn as nine regions: fixed corners, stretched edges and stretched center. Margins are relative to the selected UV rectangle, not the full atlas. They must leave at least one source pixel in the center. Small target sizes shrink the corner margins proportionally.

```json
{
  "type": "anchorui:image",
  "texture": "mypack:textures/gui/frame.png",
  "textureWidth": 48,
  "textureHeight": 48,
  "size": {"width": 270, "height": 160},
  "nineSlice": {"left": 12, "top": 12, "right": 12, "bottom": 12}
}
```

`anchorui:panel` and `anchorui:container` may also specify `texture`, UV attributes and `nineSlice`. The image covers the panel body; tint and alpha apply to the texture, while `fillColor` and `borderColor` draw underneath it.

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
- To scale a panel/container **with its descendants as a single group**, set `"scale": 0.9` (range 0.1 to 4.0). `"scaleOrigin": "top_left"` is the default; `"scaleOrigin": "center"` scales around the panel's center, useful for a centered phone or dialogue window. Nested group scales multiply. Screen mouse hit-testing, dragging, scrolling, mouse-over and opt-in clips use the transformed coordinates. HUD editor hitboxes and mouse dragging account for group scaling, while JSON widget sizes remain in logical GUI pixels.

## Automatic container sizing

A `panel` or `container` can calculate its outer size from its **direct children** (including nested auto-sized containers) and its padding. The default remains fixed sizing, for compatibility with existing resource packs.

```json
{
  "type": "anchorui:container",
  "id": "mypack:auto_card",
  "autoSize": "both",
  "padding": {"left": 12, "right": 12, "top": 8, "bottom": 8},
  "position": {"point": "CENTER", "anchor": "CENTER", "relativeTo": "screen"},
  "children": [
    {
      "type": "anchorui:text",
      "text": "Content sets the container width",
      "size": {"width": 210, "height": 18}
    },
    {
      "type": "anchorui:text",
      "text": "The second row expands the height",
      "position": {"y": 26},
      "size": {"width": 210, "height": 18}
    }
  ]
}
```

`autoSize` accepts `"width"`, `"height"`, `"both"`, `"none"`, `true`/`false`, or `{"width": true, "height": false}`. With one automatic axis, the other stays at the declared `size` (or the widget default). Nested auto-sized panels resolve from inside out, and normal anchors work on the resulting frame.

Auto sizing measures the **declared widget bounds**, not the pixels occupied by dynamically rendered text or textures; for wrapped text, provide appropriate widget sizes. Children with positions tied to a parent's right, center or bottom edge **on an auto-sized axis** can cause circular sizing and are rejected if the layout cannot converge. This is content measurement, not flex/grid flow.

## Scrollable container viewports

A fixed-size `panel` or `container` can opt in to scrolling when its children extend beyond its content viewport:

```json
{
  "type": "anchorui:panel",
  "id": "mypack:scroll_area",
  "size": {"width": 200, "height": 110},
  "padding": {"left": 8, "right": 8, "top": 8, "bottom": 8},
  "scroll": {"axis": "vertical", "step": 18},
  "children": [
    {"type": "anchorui:text", "text": "First", "size": {"width": 150, "height": 16}},
    {"type": "anchorui:text", "text": "Last", "position": {"y": 180}, "size": {"width": 150, "height": 16}}
  ]
}
```

`scroll` defaults to off and may be `"vertical"`, `"horizontal"`, `"both"` or `{"axis":"vertical","step":18}` (step is 1–1024 GUI pixels). Wheel input is routed to the viewport under the pointer; scrolling stops at its computed content bounds. The selected scroll offsets move *descendants only*. Nested scaling, anchoring, scissor clipping, interaction hitboxes and scrollable descendants use the same transform hierarchy.

**A scrolling container always clips its descendants** even when `clipChildren` is omitted, so off-screen elements do not render or receive clicks. Other containers still default to no clipping. **Vertical overflow uses NeoForge's native `ScrollPanel` scrollbar.** Its handle is interactive: click the scrollbar and drag the handle, or scroll the mouse wheel while the pointer is over the viewport. The native component owns handle size, scroll limits, and mouse capture; Anchor UI adapts its scroll offset to the existing JSON child transforms. Scrollbars are interactive in ordinary JSON screens and in F8 screen/HUD editor previews. During normal gameplay, HUD overlays do not capture the mouse wheel or clicks.

NeoForge 1.21.1's `ScrollPanel` is vertical-only. `"scroll": "horizontal"` and the horizontal axis of `"both"` can still be moved using horizontal wheel input (or vertical wheel input when configured as horizontal-only), but **there is no horizontal draggable bar yet**. No custom non-interactive horizontal scrollbar is displayed. The native vertical scrollbar is not rendered in viewports too small for its handle.

Minecraft and NeoForge already provide `AbstractSelectionList`, `AbstractScrollWidget` and `ScrollPanel` for conventional lists/text scroll areas. Those remain the preferred components for specialized interactive list widgets. Anchor UI reuses the **native NeoForge `ScrollPanel` implementation for its vertical scrollbar, wheel handling and dragging**. Its small adapter only synchronizes native scroll position with arbitrary anchored, nested and scaled JSON widget hierarchies; rendering those children remains the responsibility of Anchor UI's established renderer.

For a predictable viewport, prefer **fixed size on the scrollable axis**: if `autoSize: "height"` expands the viewport to fit its children, there is normally nothing left to scroll. Automatic sizing is not a replacement for a responsive flex/grid layout.

Group scaling affects visuals and interactive hitboxes, but not the logical size used by the outer anchor layout. Full grid/flex auto layout, animations and transitions remain future work.

## Smoke tests

Open `/anchorui screen open anchorui:visual_demo` for the existing textured/nested UI example. Open `/anchorui screen open anchorui:auto_scroll_demo` for automatic sizing (including a nested panel) and a scrollable list of JSON text widgets. Try the mouse wheel over the right-hand viewport, hover or click its content, change GUI Scale and open F8 to inspect the widget positions. Check F3+T resource reload and in-game HUD previews with F8.
