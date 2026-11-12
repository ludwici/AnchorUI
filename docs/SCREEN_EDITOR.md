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

While **Parent** is active, clicking an ID in the list chooses that widget as the anchor target rather than selecting a different widget. Self references and circular dependencies remain rejected. Press **Tab** to temporarily hide both editor panels.


1. Click any outlined widget to select it. Drag it to reposition the widget. A 6 GUI-pixel magnet helps align it with other visible widgets along X/Y; hold **Alt** to bypass snapping.
2. Use arrow keys for one-pixel nudges (10 pixels with **Shift**).
3. **Point** and **Anchor** cycle through the same nine anchor locations as the HUD editor.
4. Use **Parent** and click another widget to change its anchor target. Click empty space to anchor it to `screen`. Invalid/circular relationships are rejected. Escape while choosing a parent cancels the selection.
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

The right-side Inspector now exposes the same detailed widget view as the HUD editor. Select a widget by clicking its outline or its entry in the Widgets list. The inspector shows its namespaced ID, current parent, point, anchor, screen bounds, user offset and visibility, followed by a scrollable **SOURCE JSON** section. Source JSON values remain read-only; changing authored resource-pack definitions is not part of the editor.

For layout properties overridden from their authored defaults, an active **R** control appears beside the respective `parent`, `point`, `anchor`, `user offset`, or `visible` line. Click it to reset that property without discarding the other overrides. The existing Point, Anchor, Parent, Hide, Reset and Reset all buttons remain available. Scroll the inspector independently with the mouse wheel or drag its scrollbar. The properties and override markers update as widgets are moved.

The **Widgets** list starts collapsed (a short header at the top left); click **[+]** to open it, then **[-]** to collapse it again. Its namespace sections retain their individual expanded/collapsed state.

When dragging a widget, its **center** also magnetically aligns to the horizontal and vertical centerlines of the whole screen. Each axis snaps independently within the standard 6-GUI-pixel threshold. The same rule applies to the HUD editor; holding Alt disables all snapping. Alignment guides appear while the magnet is engaged.

### Editing the actual AnchorUI layout components

Selecting a `panel` or `container` now shows a **COMPONENT / LAYOUT** section above the read-only source JSON. It includes a `Layout` selector (`none`, `row`, `column`, `grid`) and the options appropriate to that layout: gaps, alignment, distribution, wrapping, column count, cell dimensions and hidden-child collapse.

Selecting a structural child of a container shows `Auto item` (participating in the automatic layout), flex `Grow` / `Shrink`, and the child's minimum/maximum width and height. These generic properties work for ordinary built-in and third-party widgets without replacing their Java factories. A manually anchored child can keep its explicit anchor, and `Auto item: false` excludes it from the automatic flow.

Click **-** or **+** on the right of a property to adjust it (or cycle through supported values), and **R** to reset that *one* field to the source/style default. Highlighted fields have saved local overrides. The layout is validated before it replaces the preview; invalid combinations are rejected instead of destroying the widget tree.

Changes affect the live layout and hitboxes immediately. **Save** persists layout overrides for this screen; **Cancel** discards them. Existing screen files and the `config/anchorui/hud-layout.json` HUD file are kept separate. You do not need to edit a mod's built-in JSON to try a new layout.

The editor still treats arbitrary widget-specific configuration (text, textures, Java `Codec` fields, and gameplay handlers) as **read-only source JSON**; it does not yet provide a generic authoring IDE for every component.

Try this with `/anchorui screen open anchorui:advanced_layout_demo`, then **F8**. Switch a panel from `row` to `grid`, alter its columns, try a child's `grow`, and confirm **Save** and **Cancel** behave independently. Test against `stack_layout_demo` and `conditional_content_demo` for hidden children and cases.

### Shared native scrollbar implementation

The right Inspector, the left Widgets list, and JSON scrollable containers now use the same `NativeContainerScrollPanel` adapter over NeoForge 1.21.1's `ScrollPanel`. Each instance has independent scroll state. Wheel motion, dragging the scrollbar thumb and clamping to the content range are delegated to the native component, not implemented separately for the editor. Widget-list grouping, inspector field resets, labels and clipping still use their existing drawing code. Scrolling a panel does not scroll the other editor panel; the editor's own controls take input precedence over the JSON content preview. The slim inspector/list scroll tracks keep their original approximate widths and colors.
