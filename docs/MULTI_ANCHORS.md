# Multiple anchors and dynamic widget dimensions

AnchorUI 1.21.1 can constrain a widget to **several anchor points at once**. Two different horizontal points determine its width; two different vertical points determine its height. These dimensions update when the GUI, target widgets or layout changes. Use this feature in both JSON screens and HUD documents.

## Stretch horizontally

`position.anchors` is an array of full anchor definitions. Each definition uses the existing `point` (on the current widget), `anchor` (on the target), `relativeTo`, `x` and `y` fields.

```json
{
  "type": "anchorui:progress",
  "id": "mymod:responsive_bar",
  "position": {
    "anchors": [
      {
        "point": "LEFT",
        "anchor": "LEFT",
        "relativeTo": "screen",
        "x": 16
      },
      {
        "point": "RIGHT",
        "anchor": "RIGHT",
        "relativeTo": "screen",
        "x": -16
      }
    ]
  },
  "size": {
    "width": 160,
    "height": 14
  },
  "value": 70,
  "max": 100
}
```

The left edge is 16 GUI pixels from the screen's left edge; the right edge is 16 GUI pixels from its right edge. The rendered width is **screen width minus 32**, regardless of `size.width`. The bar's height remains 14 because the constraints both use the widget's vertical center (`LEFT` / `RIGHT`).

To stretch vertically, pair `TOP` with `BOTTOM`. To stretch in both dimensions, use `TOPLEFT` and `BOTTOMRIGHT` or supply all four corners:

```json
{
  "position": {
    "anchors": [
      {
        "point": "TOPLEFT",
        "anchor": "TOPLEFT",
        "x": 20,
        "y": 30
      },
      {
        "point": "BOTTOMRIGHT",
        "anchor": "BOTTOMRIGHT",
        "x": -20,
        "y": -30
      }
    ]
  },
  "size": {
    "width": 20,
    "height": 20
  }
}
```

Both `size.width` and `size.height` are ignored in this case. They are still valid as fallbacks for axes with only one distinct anchored position.

## Anchor between two other widgets

Constraints can use **different target widgets**.

```json
{
  "type": "anchorui:panel",
  "id": "mymod:middle",
  "position": {
    "anchors": [
      {
        "point": "LEFT",
        "anchor": "RIGHT",
        "relativeTo": "mymod:left_card",
        "x": 8
      },
      {
        "point": "RIGHT",
        "anchor": "LEFT",
        "relativeTo": "mymod:right_card",
        "x": -8
      }
    ]
  },
  "size": {
    "width": 10,
    "height": 22
  }
}
```

Here the middle panel automatically fills the gap between the two cards, leaving 8 pixels on each side. Its width changes as either card moves. Both target cards must have a compatible vertical center, since the `LEFT` and `RIGHT` points also constrain the panel's Y coordinate.

Short target IDs resolve in the widget/document namespace. `screen` is reserved. References in any order are supported; unknown targets warn and fall back to the screen, while circular dependencies raise errors.

## Arbitrarily many anchors

The array may contain any number of constraints. They must **agree geometrically**:

- Two distinct horizontal point coordinates (`LEFT`, `CENTER`, `RIGHT`) determine width; two distinct vertical coordinates (`TOP`, `CENTER`, `BOTTOM`) determine height.
- Additional constraints with the same point coordinate must resolve to the same screen coordinate.
- Three horizontal or vertical coordinates are accepted when the middle position matches the derived center. The engine uses Minecraft-compatible integer GUI-pixel rounding.
- Negative computed sizes, incompatible coordinates and anchor cycles raise contextual layout errors instead of silently choosing a size.

For example, adding a `CENTER` constraint can verify a width already derived from `LEFT` and `RIGHT`. It does **not** override those edges.

### Parsing and overriding positions

`position.anchors` replaces the legacy single-anchor form:

```json
{
  "position": {
    "point": "TOPLEFT",
    "anchor": "TOPLEFT",
    "relativeTo": "screen",
    "x": 12,
    "y": 20
  }
}
```

Both formats work, but do not mix `anchors` with `point`, `anchor`, `relativeTo`, `x` or `y` at the **same** `position` level. Each array item has its own values. If a style or `ref` template defines the other format, an explicit `position` override replaces the previous position rather than creating an invalid hybrid.

## Containers, Grid and template lists

Nested widget constraints default to the structural parent panel. When they reference that parent, the layout applies its content padding: a LEFT constraint uses the inner left edge, RIGHT uses the inner right edge, TOP/BOTTOM likewise. Use `position.anchors` on children to stretch them across the padded area without declaring any internal `__content` references.

- A panel stretched between anchors passes its **calculated size** into its row/column/grid layout; `autoSize.width` or `autoSize.height` cannot replace the corresponding explicitly anchored dimension.
- A direct child with multiple explicit anchors remains **manually positioned**, including inside a Grid. Other grid children are still managed by their configured tracks. This prevents the Grid from overwriting authored constraints.
- `template_list` preserves and remaps secondary anchor references for nested repeated widgets. For a repeated **root**, the template-list cell offset is applied to every anchor, and root anchors are targeted at the list container. Explicit stretched sizes take priority over a static generated cell width.

## F8 editor and test screen

Open the bundled demo:

```text
/anchorui screen open anchorui:multi_anchor_demo
```

Change the Minecraft window or GUI Scale and observe the top panel, its text and progress bar stretch horizontally. The lower progress bar spans the space between two separate panels. Open **F8** to inspect the calculated widget bounds, drag the widgets and save/cancel local offsets.

The F8 editor currently displays and edits the **first (primary) anchor** using its existing Point/Anchor/Parent controls. It preserves the secondary constraints and translates **all** anchors together when dragging or nudging. Add, remove, or change the other constraints directly in resource-pack JSON for now; the editor does not yet have a multi-anchor list editor.

**Automated tests:** `MultiAnchorLayoutTest`, `MultiAnchorContainerLayoutTest`, `TemplateListMultiAnchorTest` and `MultiAnchorStyleMergeTest` cover resizing, multi-target invalidation, nested padding, automatic layouts, references, inheritance and error cases. In-client rendering and interaction across GUI scales still need manual verification.
