# Visual widgets and nested containers

Available on the 1.21.1 NeoForge branch: `anchorui:image`, `anchorui:panel`, and `anchorui:container`.

The same widgets work in JSON screens and in HUD documents. Flat `entities` documents continue to work as before.

## Shared progress bar skins and frame layering

`anchorui:progress` now uses the same resource-pack texture region, tint and nine-slice syntax as the interactive controls. A progress bar can have three independent drawable parts: `textures.background` (full track), `textures.fill` (filled amount only), and `textures.frame` (full outline drawn **after** the fill). Each part accepts a texture path or a texture object, including a nine-slice object. If a part has no texture, the existing `background.color`, `fillColor`, and optional `borderColor` / `borderWidth` work as solid-color fallbacks.

A style file such as `assets/mypack/anchorui/style/status_bars.json` can define:

```json
{
  "anchorui:progress": {
    "padding": {
      "left": 1,
      "top": 1,
      "right": 1,
      "bottom": 1
    },
    "background": {
      "color": "#AA231E27"
    },
    "borderColor": "#FFB8AAB7",
    "borderWidth": 1,
    "textures": {
      "frame": {
        "texture": "mypack:textures/gui/progress_frame.png",
        "textureWidth": 48,
        "textureHeight": 48,
        "regionWidth": 48,
        "regionHeight": 48,
        "nineSlice": {
          "left": 8,
          "top": 8,
          "right": 8,
          "bottom": 8
        }
      },
      "fill": "mypack:textures/gui/progress_fill.png"
    }
  }
}
```

Choose `"style": "mypack:style/status_bars"` in the screen or HUD and set distinct `fillColor` / data bindings on each progress widget. When a texture is present for `fill`, the texture replaces the solid fill; for `frame`, it replaces the solid frame. The same resource can supply consistent width, padding, backgrounds and frames for multiple bars. Progress and panel visual properties now honor merged style values as well as per-widget overrides. The RPG HUD test fixture uses `anchorui:style/rpg_progress` to share its two bars' padding/background/frame.

The F8 VISUAL / CONTENT inspector exposes progress track color, frame color and width, and the three texture paths alongside its preexisting fill color and value text toggle. Existing progress documents without these optional appearance properties retain their former rendering.

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

For texture-based styling of interactive controls (`anchorui:checkbox`, `anchorui:switch`, `anchorui:select`, `anchorui:tabs`), see [Built-in Interactive Controls](UI_CONTROLS.md#textured-skins-from-resource-packs). Their skins reuse this same `TextureRegion` format, UV atlas settings, tint, and nine-slice support, with distinct textures for selected, hover, checked, and disabled states.

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

Panels, progress bars, images, text and other widgets can stretch to their parent or to multiple target widgets using `position.anchors`. Opposite LEFT/RIGHT points determine their width instead of `size.width`; TOP/BOTTOM points determine height. See [Multiple anchors](MULTI_ANCHORS.md).

**Every `anchorui:panel` is already a parent for its `children`**, with or without a `layout` setting. No extra container widget, context wrapper or parent ID is required. Panels support optional fill, borders and rounded corners. If `fillColor` is omitted, the usual style `background.color` is used. `anchorui:container` remains a compatibility alias for a panel primarily used for layout.

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
- Children have normal namespaced IDs; an omitted ID is generated as `<parent path>/child_0`, etc. The document loader derives the parent relationship directly from `children` and stores it separately from widget settings. **No `__anchorui_*`, `__content`, `containerParent` or additional context object belongs in author-written widget JSON.** The renderer handles padding, clipping and scroll geometry automatically. Old `/__content` references are rejected; update them to the actual Panel ID. The Panel owns its inner geometry.
- Children can still explicitly specify a different `position.relativeTo` target; clipping applies only when an ancestor sets `clipChildren: true`.
- Both F8 editors treat a Panel as the child's parent. Moving a Panel moves its descendants; moving a widget into a Panel updates its structural membership, with row/column/grid layout applied automatically when configured.
- To scale a panel/container **with its descendants as a single group**, set `"scale": 0.9` (range 0.1 to 4.0). `"scaleOrigin": "top_left"` is the default; `"scaleOrigin": "center"` scales around the panel's center, useful for a centered phone or dialogue window. Nested group scales multiply. Screen mouse hit-testing, dragging, scrolling, mouse-over and opt-in clips use the transformed coordinates. HUD editor hitboxes and mouse dragging account for group scaling, while JSON widget sizes remain in logical GUI pixels.

## Automatic container layouts: row, column and grid

A normal `anchorui:panel` or `anchorui:container` can arrange its direct children without manually setting every x/y coordinate. This is **opt-in**: documents without `layout` keep their existing anchor positions. Children retain normal namespaced IDs, style references, data bindings and events. These layouts work in HUDs and interactive screens.

### Rows and columns

```json
{
  "type": "anchorui:panel",
  "id": "mypack:toolbar",
  "position": {"point": "TOPLEFT", "relativeTo": "screen", "x": 20, "y": 20},
  "size": {"width": 300, "height": 50},
  "padding": {"left": 8, "top": 6, "right": 8, "bottom": 6},
  "layout": {
    "type": "row",
    "gap": 8,
    "align": "center",
    "justify": "start",
    "collapseHidden": true
  },
  "children": [
    {"type": "anchorui:button", "text": "Fixed", "size": {"width": 70, "height": 20}},
    {
      "type": "anchorui:button",
      "text": "Flexible",
      "size": {"width": 70, "height": 20},
      "layoutItem": {"grow": 1, "shrink": 1, "minWidth": 40}
    }
  ]
}
```

| Property | Meaning |
| --- | --- |
| `layout.type` | `row` (horizontal) or `column` (vertical); `"row"` and `"column"` also work as shorthand |
| `gap` | Distance between children along the main axis, 0–4096 GUI px |
| `align` | Cross-axis alignment: `start` (default), `center`, `end`, `stretch` |
| `justify` | Main-axis alignment: `start` (default), `center`, `end`, `space_between` |
| `wrap` | When true, move overflowing children into the next row (or column); default false |
| `lineGap` | Distance between wrapped rows/columns, defaulting to `gap` |
| `collapseHidden` | Remove invisible children from the flow and close their gaps; default false |

Children can opt into flex-like sizing through `layoutItem`:

```json
{
  "layoutItem": {
    "grow": 2,
    "shrink": 1,
    "minWidth": 40,
    "maxWidth": 180,
    "minHeight": 16,
    "maxHeight": 50
  }
}
```

- `grow` distributes *extra* room along the main axis by relative weight; `shrink` distributes a *deficit*, constrained by minimum size. `grow` and `shrink` both default to **0**, preserving the existing behavior unless explicitly requested.
- Values for `grow`/`shrink` must be finite and between 0–1000; the dimension constraints are GUI pixels in the 0–16384 range. A maximum cannot be smaller than its minimum.
- The main axis is width for `row` and height for `column`. The opposite axis uses `align`. When `align: stretch`, the cross axis fills the available line/container dimension subject to the child limits.
- With `wrap: true`, flex distribution happens separately within each wrapped line. Wrapping uses the preferred/clamped sizes; flex shrinking does not replace the wrap decision. **Wrapping is disabled if the parent is auto-sized along the main axis**, because no fixed wrapping limit exists.
- `autoSize` and padding continue to work. An auto-sized main axis uses intrinsic child sizes instead of distributing artificial free space; a fixed main axis and auto-sized cross axis can grow as lines wrap.
- `justify: space_between` distributes any remaining space between elements after flex sizing.

### Standalone grid for panels and containers

Grid is now available **outside** `anchorui:template_list`:

```json
{
  "type": "anchorui:panel",
  "size": {"width": 310, "height": 130},
  "padding": {"left": 8, "right": 8, "top": 6, "bottom": 6},
  "layout": {
    "type": "grid",
    "columns": 3,
    "columnGap": 6,
    "rowGap": 8,
    "justifyItems": "stretch",
    "alignItems": "center",
    "collapseHidden": true
  },
  "children": [
    {"type": "anchorui:button", "text": "A", "size": {"width": 70, "height": 22}},
    {"type": "anchorui:button", "text": "B", "size": {"width": 70, "height": 22}},
    {"type": "anchorui:button", "text": "C", "size": {"width": 70, "height": 22}}
  ]
}
```

- `columns`: 1–256, default 1. Elements fill each row left-to-right and then wrap to the next row. **The declared number of tracks stays fixed**, even if some cells are empty or their children become hidden.
- `columnGap` and `rowGap`: independent gaps (0–4096); `gap` provides both defaults.
- `cellWidth` and `cellHeight`: optional fixed sizes. Without `cellWidth`, a fixed-width container divides its padded width evenly between columns; an auto-width container sizes each column to its widest child. Without `cellHeight`, each row is as tall as its tallest child.
- `justifyItems`: horizontal placement *within* a cell, and `alignItems`: vertical placement. Both accept `start`, `center`, `end`, `stretch` and default to `start`.
- `collapseHidden`: a hidden child does not occupy a grid cell; later children fill the vacated position. The grid height can shrink with `autoSize`.
- Every direct child in the Grid's `children` array is placed automatically, in declaration order. No `position`, `relativeTo`, `__content` or `layoutItem.enabled` is needed. The Grid owns the child's cell even when a legacy anchor or `layoutItem: false` is present. Width/height constraints in `layoutItem` remain optional; flex `grow`/`shrink` apply only to row/column stacks.
- The `layout` shorthand `"grid"` is supported, defaulting to one column.

### Composition and compatibility

For **row/column**, a child joins automatic placement when it is nested in the Panel and uses its default top-left position relative to that Panel. `"layoutItem": false` or an explicit external/non-top-left anchor keeps a row/column child manually positioned; `position.x/y` are optional local nudges.

For **Grid**, *all direct structural children* always occupy Grid cells according to the parent's `columns`, `gap`, `cellWidth`/`cellHeight`, `alignItems`, and `justifyItems`. Grid ignores child `position` anchors and x/y for placement; optional size constraints can still be set per child. This also applies to widgets moved into Grid using F8 **Parent**. No internal anchor IDs should appear in authored children.

All variants reuse the existing AnchorUI scrolling, clipping, scale transforms, focus/hitboxes and F8 layout overrides. `visibleWhen` and `anchorui:cases` can control hidden children. Layout is recalculated when the participating hidden set changes, not on every ordinary frame.

**This is a general panel/container grid**, not the older `template_list.layout` grid, which retains its independent layout settings and Java extension API. Neither implementation currently provides a complete CSS Flexbox/Grid engine (e.g., grid spans, implicit named areas or arbitrary CSS sizing).

### Test screens

```text
/anchorui screen open anchorui:stack_layout_demo
/anchorui screen open anchorui:advanced_layout_demo
```

The first demonstrates basic row/column layouts and visibility collapse. The second demonstrates weighted flex sizing, multiple wrapped lines, and a three-column grid that reflows when you toggle a switch. Test different GUI scales and inspect the frames via F8.

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
