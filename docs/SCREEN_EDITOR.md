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

A **draggable Widgets panel** on the left lists *only the widgets belonging to the currently open Anchor UI screen*. It does not include HUDs from other screens or the global vanilla HUD reference catalog. The entries are sorted: `minecraft:*` first if present in the document, then IDs whose namespace matches the open screen's resource namespace, then all other namespaces alphabetically. If the screen comes from `catelot:screens/profile`, for example, `catelot:*` entries appear ahead of other mods' widgets. Click an entry to select it in the editor and inspect its bounds; hidden widgets can be selected here too. Scroll the list with the mouse wheel, and drag its header to move it separately from the inspector.

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

The file uses the same override schema as `hud-layout.json`, including the namespaced widget ID, `x`/`y` deltas, `relativeTo`, `point`, `anchor`, and optional `hidden`. Neither the original resource pack JSON nor the server's data is modified. Unchanged fields continue to use resource defaults, and the saved deltas are reapplied on the next screen opening or resize.

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

