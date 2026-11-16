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

Here the middle panel automatically fills the gap between the two cards, leaving 8 pixels on each side. Its width changes as either card moves. **The target cards may have different vertical positions**: the first `LEFT` attachment determines the bar's vertical center, while the `RIGHT` attachment supplies its second horizontal edge. Moving the right card vertically no longer blocks the layout or forces the left card to follow it. To make the bar follow the right card's Y position instead, put the right attachment first. Move both anchors together using the F8 editor offset to reposition the bar without changing its width.

Short target IDs resolve in the widget/document namespace. `screen` is reserved. References in any order are supported; unknown targets warn and fall back to the screen, while circular dependencies raise errors.

## Arbitrarily many anchors

The array may contain any number of constraints. Each coordinate is resolved independently using the widget points on that axis:

- Two distinct horizontal points (`LEFT`, `CENTER`, `RIGHT`) can determine width; two distinct vertical points (`TOP`, `CENTER`, `BOTTOM`) can determine height. The corresponding `size` field is then ignored.
- If several attachments provide the **same point on one axis**, the **first** attachment determines that coordinate. Another point may still contribute to the other axis. For example, the first `LEFT` attachment controls the Y center, while the later `RIGHT` attachment provides the opposite X edge, without overriding Y.
- When both outside edges (`LEFT` and `RIGHT`, or `TOP` and `BOTTOM`) are set, they determine the corresponding size. Additional `CENTER` coordinates do not over-constrain the result; outside edges take precedence on that axis.
- Two different points including `CENTER` can also determine the size. Negative computed sizes, out-of-range positions and circular references remain errors.

This is AnchorUI's deterministic, **WoW-inspired** resolution rule, not a claim that every undocumented conflict or protected-frame restriction in Blizzard's UI engine behaves identically. The JSON format is unchanged; no `axis` field or other per-constraint switch is required.

To control vertical placement independently of `LEFT` + `RIGHT` width, change the first anchor's `y` offset, move the widget in F8, or add `TOP` / `BOTTOM` attachments. Those vertical endpoints may define the height without the horizontal attachments forcing their Y positions.

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

Both F8 inspectors now have an expanded-by-default, collapsible **ANCHORS (N)** section showing **every effective anchor** of the selected widget. Each numbered entry displays the widget's `point`, target `anchor`, `relativeTo` ID and X/Y offsets. The section also identifies whether width and height are fixed or determined by anchors. It includes the current editor preview offsets and any primary-anchor override, not only the original resource-pack values.

The **ANCHORS (N)** section is an editor in both HUD and screen F8. It displays a compact header for each constraint, with optional independent expansion. **[+] Add anchor** duplicates the last anchor, preserving current geometry until you edit the new one. Expand a constraint to change its widget **Point**, target **Anchor**, **Relative to** (choose screen or any available widget; HUD also offers vanilla HUD references) and X/Y offsets. Point, Anchor and Relative to use dropdowns. Click an offset to type an integer, or use - / + (hold Shift for 10-pixel steps). **[-] Delete this anchor** removes a constraint; the last remaining anchor cannot be deleted. **Reset anchors to resource defaults** clears all local anchor changes without clearing unrelated component overrides. The legacy Point/Anchor toolbar buttons and duplicate inspector properties were removed.

Anchor-list edits are immediately validated against the live layout. Invalid spans or dependency cycles leave the previous preview intact and show an error; **Save** persists the full anchor array into the pretty-printed HUD or per-screen config, while **Cancel** discards it. The inspector shows *effective* coordinates (including the independent user-offset delta); editing an X/Y value does not double-apply that delta. Dragging/nudging still moves **every** anchor together. Neither action changes the original resource-pack JSON.

**Automated tests:** `MultiAnchorLayoutTest`, `MultiAnchorContainerLayoutTest`, `TemplateListMultiAnchorTest`, `MultiAnchorStyleMergeTest` and `AnchorInspectorRowsTest` cover resizing, multi-target invalidation, nested padding, automatic layouts, references, inheritance and error cases. In-client rendering and interaction across GUI scales still need manual verification.
