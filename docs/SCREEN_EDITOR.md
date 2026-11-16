# Screen editor (NeoForge 1.21.1)

The F8 key binding now chooses its editor based on the active UI context:

- **No screen open:** edits the shared gameplay HUD, as before.
- **An Anchor UI JSON screen open:** edits that screen's widgets in-place with a live preview.
- **Other mods' or vanilla screens:** Anchor UI does not modify unregistered widgets in those screens. F8 is not intercepted there.

The key binding is still `key.anchorui.edit_hud` internally, so existing custom key assignments are preserved. It can be changed under Controls → Key Binds → Anchor UI.

Open an example JSON screen with:

```text
/anchorui screen open anchorui:visual_demo
```

Then press **F8**. The same editor also works with built-in test scenes (`/anchorui test scene1`), screens opened by Anchor UI actions and screens instantiated with the resource-aware Java constructor:

```java
new ItemLoreScreen(previousScreen, item, document, resourceId)
```

Third-party callers using the original three-argument constructor can still preview and edit widgets, but must supply the document resource ID to persist overrides safely.

## Working with widgets

A **draggable Widgets panel** on the left lists *only the widgets belonging to the currently open Anchor UI screen*. It does not include HUDs from other screens or the global vanilla HUD reference catalog. The entries are grouped: `minecraft:*` first if present, then the screen's namespace, then other namespaces. **Children now appear indented under their structural container** with type markers for panels, buttons, text, progress and image widgets. Click a container's disclosure arrow to collapse or expand its descendants, or click its name to select it. When the child belongs to another namespace it remains in that namespace's group. Hover any widget to see its full ID and type. Hidden widgets stay selectable from the list. Scroll the list with the mouse wheel and drag its header independently of the inspector.

While **Parent** is active, click a container in the Widgets list to **move the selected widget inside it immediately**. Selecting Grid makes it a Grid child automatically; no extra choices or content-area anchors are required. Click empty world to move the widget back to the screen root. Clicking a non-container widget can still change an advanced coordinate anchor. Self references and circular dependencies are rejected. Press **Tab** to temporarily hide both editor panels.


1. Click any outlined widget to select it. Drag it to reposition the widget. A 6 GUI-pixel magnet helps align it with other visible widgets along X/Y; hold **Alt** to bypass snapping.
2. Use arrow keys for one-pixel nudges (10 pixels with **Shift**).
3. **Point ▼** and **Anchor ▼** open a dropdown with all nine anchor locations. Pick a value directly instead of cycling. The menu opens above the toolbar when there is insufficient space below. For `position.anchors` arrays, these controls edit the **first** anchor only; secondary constraints are preserved and all anchors move together during dragging/nudging. Further anchor points are currently authored in JSON. See [Multiple anchors](MULTI_ANCHORS.md).
4. Use **Parent** and click a panel/container or select it in the Widgets list. The selected widget is immediately made its child. For a Grid parent, Grid automatically assigns its cell using the parent's layout settings; child position fields are not required. Click empty world to detach the widget to the screen root. Escape cancels the picker; invalid/circular references are rejected.
5. **Hide/Show** toggles local widget visibility. **Reset** restores the selected widget's defaults; **Reset all** clears all overrides for the open screen.
6. **Save**, **F8** or **Ctrl+S** saves and returns to the original screen. **Cancel** or **Esc** discards edits and returns to the screen.
7. Drag the editor panel by its title bar. Press **Tab** to temporarily hide the panel while positioning widgets.

The editor uses the same widget factory, anchors, nesting, container clipping and scaling as normal screen rendering. Containers and their anchored children move together, and the editor's hitboxes and pointer calculations use the final visually scaled bounds.

The editor **does not forward gameplay clicks** to the underlying JSON buttons while active; mouse interaction is reserved for selection and moving widgets.

## Diagnostics in the F8 inspector

If widget construction or layout resolution produces errors or warnings, the inspector shows a **DIAGNOSTICS (errors, warnings)** category above the widget-specific fields. It starts collapsed to keep the inspector compact; click the heading to expand messages. Warnings from missing widget types, invalid conditions/animations, and anchor/layout validation use the same snapshot that is reported in `latest.log`. Duplicate warnings are collapsed and only the first 24 entries are shown; the header counts every unique warning and the inspector explains when additional messages remain in the log. Long messages wrap inside the existing inspector scroll viewport.

The diagnostics also include resource reload failures for the current screen and shared styles/definitions/templates, with the resource ID and error severity. Unrelated screen or HUD resource errors are filtered out. See [Resource reload and diagnostics](RESOURCE_RELOAD.md) for the isolation policy and F3+T test checklist.

When an editor operation is rejected, the status message uses the underlying layout validator's reason where available (for example, a circular anchor relationship), rather than an unspecific `Invalid layout`. Valid screen state and saved overrides are retained after a rejected change. Source JSON remains read-only.

## Where are layouts stored?

For resource `anchorui:screens/visual_demo`, the editor writes:

```text
.minecraft/config/anchorui/screens/anchorui/screens/visual_demo.json
```

Another screen, such as `anchorui:screens/dialogue_intro`, has its **own** file and independent overrides. Test scenes use their corresponding `anchorui:test/scenes/...` paths.

The file uses the same override schema as `hud-layout.json`, including the namespaced widget ID, `x`/`y` deltas, `relativeTo`, `point`, `anchor`, optional `hidden`, and **optional `layout` / `layoutItem` property overrides**. Neither the original resource pack JSON nor the server's data is modified. Unchanged fields continue to use resource defaults, and the saved deltas are reapplied on the next screen opening or resize.

The HUD layout remains independent in `config/anchorui/hud-layout.json`.

### F8 live preview and lifecycle regression

The F8 editor draws the real screen's visible widgets **under** its selection outlines and inspector. Rendering the source screen through its ordinary `render()` entry point is not sufficient because that path only renders while the screen itself is active. The editor uses a dedicated **noninteractive preview** entry point, preserving normal fill/border/text/texture rendering, clipping, local UI State visibility and scroll indicators without dispatching widget clicks, hover events or screen actions.

Opening and exiting F8 is a temporary editor transition, not closing and reopening the authored screen. Its `onClose` and `onOpen` actions are not supposed to run just because F8 was toggled.

**Manual regression:**

1. Run `/anchorui screen open anchorui:advanced_layout_demo`. Verify the heading, buttons, switch, colored container panels, and grid are visible outside the editor.
2. Press **F8**. The **same actual elements** must remain visible behind the cyan/yellow selection outlines; the outlines alone are **not** a valid preview.
3. Collapse and expand the left Widgets list, select different elements, adjust a harmless layout property, and confirm their actual content stays rendered. An explicitly hidden child should disappear from the UI while its handle remains selectable from the list.
4. Save with F8, reopen the editor, and Cancel. The display and Local UI State should be preserved without unintended `onOpen`/`onClose` actions. In the normal screen toggle **Show optional grid item**, then reopen F8: visibility and grid compaction should agree with the latest state.
5. Repeat with `anchorui:editor_components_demo` to verify text, progress fill, checkbox colors, panel borders, and image textures. Check that opening an appearance edit dialog does not activate the underlying UI controls.

## Suggested manual checks

- Open the visual demo, F8, move a textured panel and its child, save, then reopen the screen: both should retain their relative placement.
- Reopen the editor, change layout and press Esc: the last saved layout should return.
- Toggle a hidden widget, reset it, then verify that clicks are not sent to an ordinary action button while editing.
- Try magnet snapping, hold Alt for free movement, and use arrow keys for fine positioning.
- Verify `scale`, `scaleOrigin`, `clipChildren`, and nested panels during movement.
- Test invalid/circular parent choices, and test resizing / GUI Scale changes.
- Open two different screens sharing a widget ID; their saved overrides must not affect each other.

### Editor panels and clear preview

The screen editor deliberately **does not blur the underlying world or interface**. The normal screen is rendered as a live preview, with the editor controls drawn above it at a separate render depth. This is editor-only behavior; ordinary UI screens keep their existing background treatment.

The left **Widgets** panel can be collapsed with **[-]** in its header and reopened with **[+]**. Each namespace section (VANILLA / CURRENT / OTHER MODS) can be collapsed independently by clicking its heading. The right **Inspector** panel has its own **[-] / [+]** toggle. Both panels remain draggable, and **Tab** hides the editor chrome entirely. On narrow GUI scales the inspector starts collapsed when the two expanded panels would overlap; expanding one panel collapses the other as needed.


### Widget properties and per-field reset

The right-side Inspector shows a single **parent** reference for each widget. For children nested inside a Panel, the parent is that Panel's ID — no `/__content` target or separate container line. It also shows point, anchor, bounds, user offset, visibility and editable component fields. The **SOURCE JSON [+]** section is **collapsed by default**; click its heading to expand it and again to collapse. Its values remain read-only. Using **- / +** on a property preserves the inspector's scroll position.

For layout properties overridden from their authored defaults, an active **R** control appears beside the respective `parent`, `point`, `anchor`, `user offset`, or `visible` line. Click it to reset that property without discarding the other overrides. The existing Point, Anchor, Parent, Hide, Reset and Reset all buttons remain available. Scroll the inspector independently with the mouse wheel or drag its scrollbar. The properties and override markers update as widgets are moved.

The **Widgets** list starts collapsed (a short header at the top left); click **[+]** to open it, then **[-]** to collapse it again. Drag the Widgets header freely toward the bottom of the screen: the expanded list automatically reduces its height to fit the remaining space, and grows back when moved upward. Its scrollbar adapts to the visible list height. Its namespace sections retain their individual expanded/collapsed state.

When dragging a widget, its **center** also magnetically aligns to the horizontal and vertical centerlines of the whole screen. Each axis snaps independently within the standard 6-GUI-pixel threshold. The same rule applies to the HUD editor; holding Alt disables all snapping. Alignment guides appear while the magnet is engaged.

### Editing the actual AnchorUI layout components

Selecting a `panel` or `container` now shows a **COMPONENT / LAYOUT** section above the read-only source JSON. It includes a `Layout` selector (`none`, `row`, `column`, `grid`) and the options appropriate to that layout: gaps, alignment, distribution, wrapping, column count, cell dimensions and hidden-child collapse.

Selecting a structural child of a container shows `Auto item` (participating in the automatic layout), flex `Grow` / `Shrink`, and the child's minimum/maximum width and height. These generic properties work for ordinary built-in and third-party widgets without replacing their Java factories. A manually anchored child can keep its explicit anchor, and `Auto item: false` excludes it from the automatic flow.

For enum properties such as **Layout**, **Align**, **Justify**, and **Cell X/Y**, use the **▼** dropdown to select a named value. Numeric settings retain **- / +**; Boolean fields such as **Wrap**, **Collapse**, **Auto item**, and **Show values** use a checkbox. **R** resets only that field to the source/style default. Highlighted fields have saved local overrides. The layout is validated before it replaces the preview; invalid combinations are rejected instead of destroying the widget tree.

Changes affect the live layout and hitboxes immediately. **Save** persists layout overrides for this screen; **Cancel** discards them. Existing screen files and the `config/anchorui/hud-layout.json` HUD file are kept separate. You do not need to edit a mod's built-in JSON to try a new layout.

Every compact F8 inspector control (−, +, E, ▼, R, color swatches and checkboxes) now uses the same 11×11 square visual size. Two-pixel vertical gaps keep consecutive controls visually separate; hitboxes remain generously sized.

The editor treats unregistered Java `Codec` fields and gameplay handlers as read-only source JSON. Built-in text, color, image and texture properties and explicitly registered custom editor fields can be changed in VISUAL / CONTENT.

Try this with `/anchorui screen open anchorui:advanced_layout_demo`, then **F8**. Switch a panel from `row` to `grid`, alter its columns, try a child's `grow`, and confirm **Save** and **Cancel** behave independently. Test against `stack_layout_demo` and `conditional_content_demo` for hidden children and cases. Use `hidden_space_policy_demo` to compare reserved vs collapsed layouts and their scroll limits side by side. The full client acceptance checklist is in [Stage 1/2 QA](QA_STAGE_1_2.md).

### Move widgets directly into Grid

With F8 open, select a widget, press **Parent**, and click your Grid panel (for example `anchorui:grid`) or select that panel from Widgets. **That's it:** the widget becomes a direct child of the Grid and receives a cell according to `columns`, gaps, cell sizing and alignment. Repeat for other widgets. The Grid's own settings control the arrangement.

When a widget belongs to a Grid, **no manual anchor selection, `__content` reference or `layoutItem.enabled` flag is needed**. Explicit old positions do not override Grid placement. Clicking empty space in Parent mode moves the selected widget back to the screen root. The editor saves the structural relationship locally:

```json
{
  "version": 1,
  "offsets": {
    "mypack:button": {
      "containerParent": "mypack:grid"
    }
  }
}
```

For **author-written UI JSON**, simply put widgets in the parent's `children` array; they are automatically Grid members. No `containerParent` or `__anchorui_*` field is needed in resource packs. The last example is an **editor save file**, not the format developers must author.

For row/column, the editor sets the Panel itself as the anchor and automatically applies its padding. Selecting an ordinary non-Panel widget remains an advanced anchor-target operation. When clicking on the screen in Parent mode, the editor prioritizes Panels over their overlapping children. Invalid cycles are rejected, and resetting Parent restores the authored hierarchy.

The **SOURCE JSON [+]** section is collapsed by default. The document resolver no longer injects `__anchorui_container_parent` into each child; the parent relation comes from `children` and is carried separately. Automatic parent-relative positions are hidden from the inspector; the removed `/__content` references are rejected during document resolution.

### Visual widget editing: text, colors, textures and sizes

The F8 inspector now also contains a **VISUAL / CONTENT** section for built-in widgets and for custom Java widgets that explicitly register editor fields. You can edit:

- `text` and text alignment/wrapping for text widgets; button labels and state-control labels.
- Text, fill, border and accent colors in hexadecimal format (`#RRGGBB` or `#AARRGGBB`); panel border width and corner radius.
- `texture` resource IDs for panels and images; resource-pack sprite variants such as `textures.background.normal`/`hover`/`disabled` for state controls.
- Width and height of applicable widgets, progress-bar `showText`, and other numeric/boolean/choice fields registered in Java.

Use **- / +** to adjust numeric values and click the checkbox for Boolean values. Java-registered `CHOICE` fields get a **▼** dropdown instead of requiring repeated clicks. Use **E** for text and texture resource IDs. For any color field (including Java-registered `Kind.COLOR` fields), the color swatch opens the same graphical Color Picker as `anchorui:color_picker`: choose a hue and saturation with the mouse or type HEX/RGB values. **Apply** commits the appearance override; **Cancel** or **Esc** discards the draft. The color dialog does not change the source JSON until the editor Save action. Use **R** to reset an individual override. The original JSON is available in the collapsed **SOURCE JSON [+]** section below these controls.

Changes refresh the edited widget's appearance immediately, while preserving the screen's Local UI State. This refresh is triggered on an explicit edit, **not** on each frame or mouse drag. Invalid values are rejected; for custom widgets with a `Codec`, invalid factory configurations cause the preview to roll back.

Appearance overrides are stored in the same per-screen override JSON under `appearance`, for example:

```json
{
  "version": 1,
  "offsets": {
    "anchorui:heading": {
      "appearance": {
        "text": "New heading",
        "color": "#FFEFEFEF",
        "size.width": 200
      }
    }
  }
}
```

Neither the resource pack nor server-owned gameplay data is modified. Developers can make their Java widget properties editable using `WidgetEditorRegistry.register(...)`; see [Custom widgets](CUSTOM_WIDGETS.md#10-поля-визуального-f8-редактора-java-api). Arbitrary event handlers, data bindings and Java-side behavior **are not** editable by this UI.

To test the bundled component editor showcase, run `/anchorui screen open anchorui:editor_components_demo` and press **F8**. It includes a heading, button, progress bar, checkbox, textured image and rounded panel; edit their labels, fill/border colors, sizes and texture paths, then Save/Cancel. Use `advanced_layout_demo` for automatic Layout and `ui_controls_textured_demo` for more state-control texture variants.

### Shared native scrollbar implementation

The right Inspector, the left Widgets list, and JSON scrollable containers now use the same `NativeContainerScrollPanel` adapter over NeoForge 1.21.1's `ScrollPanel`. Each instance has independent scroll state. Wheel motion, dragging the scrollbar thumb and clamping to the content range are delegated to the native component, not implemented separately for the editor. Widget-list grouping, inspector field resets, labels and clipping still use their existing drawing code. Scrolling a panel does not scroll the other editor panel; the editor's own controls take input precedence over the JSON content preview. The slim inspector/list scroll tracks keep their original approximate widths and colors.


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
