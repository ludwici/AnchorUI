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

While **Parent** is active, clicking a widget in the list chooses the target. For a panel or container, the editor opens a choice between **Anchor to container**, **Anchor to content area**, and **Move into container**. The last option changes the structural layout parent as well as the anchor. Choosing `screen` also offers **Move to screen (root)**. Self references and circular dependencies are rejected. Press **Tab** to temporarily hide both editor panels.


1. Click any outlined widget to select it. Drag it to reposition the widget. A 6 GUI-pixel magnet helps align it with other visible widgets along X/Y; hold **Alt** to bypass snapping.
2. Use arrow keys for one-pixel nudges (10 pixels with **Shift**).
3. **Point** and **Anchor** cycle through the same nine anchor locations as the HUD editor.
4. Use **Parent** and click another widget or its row in the Widgets list. Ordinary targets set an anchor directly. Selecting a **panel/container** opens three actions: anchor to its frame, anchor to its padded `/__content` frame, or **Move into container** to make the widget an actual child of its row/column/grid layout. Clicking empty world opens the screen/root options. Escape cancels the picker. Invalid/circular relationships are rejected.
5. **Hide/Show** toggles local widget visibility. **Reset** restores the selected widget's defaults; **Reset all** clears all overrides for the open screen.
6. **Save**, **F8** or **Ctrl+S** saves and returns to the original screen. **Cancel** or **Esc** discards edits and returns to the screen.
7. Drag the editor panel by its title bar. Press **Tab** to temporarily hide the panel while positioning widgets.

The editor uses the same widget factory, anchors, nesting, container clipping and scaling as normal screen rendering. Containers and their anchored children move together, and the editor's hitboxes and pointer calculations use the final visually scaled bounds.

The editor **does not forward gameplay clicks** to the underlying JSON buttons while active; mouse interaction is reserved for selection and moving widgets.

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

The right-side Inspector exposes the same detailed widget view as the HUD editor. Select a widget by clicking its outline or its entry in the Widgets list. The inspector distinguishes **anchor target** (the coordinate reference, such as `mypack:grid/__content`) from **container** (actual structural parent used for layout, clipping and inherited visibility). It also shows point, anchor, bounds, user offset, visibility and editable component fields. The **SOURCE JSON [+]** section is **collapsed by default**; click its heading to expand it and click again to collapse. Its values remain read-only.

For layout properties overridden from their authored defaults, an active **R** control appears beside the respective `parent`, `point`, `anchor`, `user offset`, or `visible` line. Click it to reset that property without discarding the other overrides. The existing Point, Anchor, Parent, Hide, Reset and Reset all buttons remain available. Scroll the inspector independently with the mouse wheel or drag its scrollbar. The properties and override markers update as widgets are moved.

The **Widgets** list starts collapsed (a short header at the top left); click **[+]** to open it, then **[-]** to collapse it again. Its namespace sections retain their individual expanded/collapsed state.

When dragging a widget, its **center** also magnetically aligns to the horizontal and vertical centerlines of the whole screen. Each axis snaps independently within the standard 6-GUI-pixel threshold. The same rule applies to the HUD editor; holding Alt disables all snapping. Alignment guides appear while the magnet is engaged.

### Editing the actual AnchorUI layout components

Selecting a `panel` or `container` now shows a **COMPONENT / LAYOUT** section above the read-only source JSON. It includes a `Layout` selector (`none`, `row`, `column`, `grid`) and the options appropriate to that layout: gaps, alignment, distribution, wrapping, column count, cell dimensions and hidden-child collapse.

Selecting a structural child of a container shows `Auto item` (participating in the automatic layout), flex `Grow` / `Shrink`, and the child's minimum/maximum width and height. These generic properties work for ordinary built-in and third-party widgets without replacing their Java factories. A manually anchored child can keep its explicit anchor, and `Auto item: false` excludes it from the automatic flow.

Click **-** or **+** on the right of a property to adjust it (or cycle through supported values), and **R** to reset that *one* field to the source/style default. Highlighted fields have saved local overrides. The layout is validated before it replaces the preview; invalid combinations are rejected instead of destroying the widget tree.

Changes affect the live layout and hitboxes immediately. **Save** persists layout overrides for this screen; **Cancel** discards them. Existing screen files and the `config/anchorui/hud-layout.json` HUD file are kept separate. You do not need to edit a mod's built-in JSON to try a new layout.

The editor treats unregistered Java `Codec` fields and gameplay handlers as read-only source JSON. Built-in text, color, image and texture properties and explicitly registered custom editor fields can be changed in VISUAL / CONTENT.

Try this with `/anchorui screen open anchorui:advanced_layout_demo`, then **F8**. Switch a panel from `row` to `grid`, alter its columns, try a child's `grow`, and confirm **Save** and **Cancel** behave independently. Test against `stack_layout_demo` and `conditional_content_demo` for hidden children and cases.

### Anchor reference vs. structural container parent

Selecting `mypack:grid` while picking a parent offers three distinct operations:

- **Anchor to container** changes only `relativeTo` to `mypack:grid`. The widget stays in its original structural layout group.
- **Anchor to content area** changes only `relativeTo` to `mypack:grid/__content`. Coordinates start inside the container's padding, but the widget is **not** automatically added to Grid.
- **Move into container** makes `mypack:grid` the widget's *structural parent*, anchors it to `mypack:grid/__content` with TOPLEFT/TOPLEFT, resets its local layout nudge and enables its automatic layout participation. It now receives the container's Grid/Row/Column placement, visibility inheritance, clipping and scrolling behavior. Use **Parent** then click empty space and choose **Move to screen (root)** to remove that structural membership.

A moved widget is represented by user-specific layout overrides, not by rewriting source `children` arrays:

```json
{
  "version": 1,
  "offsets": {
    "mypack:button": {
      "relativeTo": "mypack:grid/__content",
      "point": "TOPLEFT",
      "anchor": "TOPLEFT",
      "containerParent": "mypack:grid",
      "layoutItem": {
        "enabled": true
      }
    }
  }
}
```

The **Parent** reset control restores both the authored structural parent and the authored anchor target. Save/Cancel continue to work on the existing per-screen override file. The editor validates target containers and structural cycles before applying a move.

### Visual widget editing: text, colors, textures and sizes

The F8 inspector now also contains a **VISUAL / CONTENT** section for built-in widgets and for custom Java widgets that explicitly register editor fields. You can edit:

- `text` and text alignment/wrapping for text widgets; button labels and state-control labels.
- Text, fill, border and accent colors in hexadecimal format (`#RRGGBB` or `#AARRGGBB`); panel border width and corner radius.
- `texture` resource IDs for panels and images; resource-pack sprite variants such as `textures.background.normal`/`hover`/`disabled` for state controls.
- Width and height of applicable widgets, progress-bar `showText`, and other numeric/boolean/choice fields registered in Java.

Use **- / +** to adjust numeric, Boolean or enumerated options. Use **E** to open an editable input box for text, color or texture resource IDs, then press **Enter / Apply** or **Esc / Cancel**. Use **R** to reset an individual override. The original JSON is available in the collapsed **SOURCE JSON [+]** section below these controls.

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
