# Stage 3 — Appearance, input and rendering QA (NeoForge 1.21.1)

This checklist tests **in-game** behavior. Java unit tests check style merges, color parsing, layout and focus eligibility; GPU layering, texture reload and actual keyboard/mouse routing require the Minecraft client. Do not mark the remaining Stage 3 runtime QA complete from a successful Gradle build alone.

## 1. Compact themed screen and focus

```text
/anchorui screen open anchorui:stage3_compact_demo
```

1. Use Tab and Shift+Tab. Focus must move between the checkbox, select and button, not the static heading/panel or invisible elements. Enter/Space should activate the focused control as vanilla permits.
2. Click **Quality** repeatedly, choose a new option and open it again. Check that the dropdown remains clickable even when the original Select already has keyboard focus.
3. The panel uses `clipChildren: true`. Open the popup near its lower edge: the dropdown must render over the panel boundary, over the Close button and with options fully clickable. The Close button underneath the open option list **must not show its hover effect**, and scrolling the popup should not scroll the background panel.
4. Esc with Select open closes the popup first. A second Esc closes the screen. Changing focus with Tab closes the popup without leaving a ghost overlay.
5. Switch `Show selection`; the bound label should appear/disappear. The translucent heading demonstrates `#AARRGGBB` text alpha.
6. Repeat with at least two **GUI Scale** values (including a small scale) and a resized game window. Confirm hitboxes follow the displayed widgets and that clipping excludes offscreen Tab targets.

## 2. Shared skins and inspector

```text
/anchorui screen open anchorui:ui_controls_textured_demo
/anchorui screen open anchorui:editor_components_demo
/anchorui screen open anchorui:color_picker_demo
```

Check textured normal/hover/focused/disabled styles and the single-open dropdown rule. In F8 edit a background/border/foreground color; confirm the compact square color swatch opens the Color Picker, both HEX and RGB accept valid input, Apply commits, Cancel discards and Save persists the override. **While editing a color or a text/texture/numeric value, the editor's Save, Cancel, Reset and other panel buttons must remain visible behind the dialog, but must not respond to clicks or display hover effects until the dialog closes.** Check widget labels, texture fields, reset `R`, source-JSON inspector scroll, and keyboard Tab across controls and text fields. When swapping a texture path to an invalid ID, the editor should reject or report it without permanently damaging the saved screen.

Run the RPG HUD demo (test group `rpg`) and confirm the Health and Food bars have **identical border width, padding and frame geometry**, even though their fill colors and values differ. The automated `StageThreeVisualResourcesTest` enforces shared-resource parity; an actual render check is still necessary.

## 3. Resource-pack reload and broken assets

**Minecraft's default F3+T shortcut does not run while an ordinary GUI screen is open. AnchorUI must not intercept or override this vanilla behavior.** To test resource reload, close the screen first, press F3+T in the world, then reopen the screen. Because the reopen creates a **new screen instance**, Local UI State correctly starts from that document's defaults unless the navigation action uses `stateHandoff`.

1. Replace a screen JSON or shared style in a resource pack; close the screen, reload resources from the world, reopen and confirm the updated UI is used.
2. Temporarily remove a referenced PNG, reload from the world, and reopen a screen that uses it. `latest.log` should identify the namespaced texture and expected `assets/<namespace>/<path>`. Restore the PNG and reload again.
3. Reference a nonexistent style ID. The log must name the requested style and fallback; restore the style and reload.
4. Supply a malformed top-level JSON object or impossible texture region (dimensions/UV). Errors should identify the resource/widget without silently displaying the wrong skin. Restore valid files afterward.
5. Check saved editor overrides after reloading resources, ensuring that widget geometry and input remain usable. If a reload is initiated by another supported mechanism *while a screen instance is still open*, compatible Local UI State keys are preserved by the revision handler, but this is not an F3+T shortcut feature.

## 4. Overlay and tooltip order

Verify background blur remains **behind** all AnchorUI widgets, normal text/image widgets retain their intended stacking, Select menus float over ordinary widgets and outside clipped containers, and rich-text hover tooltips float above them. A tooltip on text visually obscured by an open Select popup should **not** appear. Check outlines, focus markers, scroll thumb overlays, nested scissors and cursor position at different GUI scales.

Custom Java-registered widgets with their own popups, narration or focus handling should also be checked: built-in routing is covered but custom widget UX remains integrator-dependent.

## 5. Regression tracking

Automated coverage: `ControlVisualStatesTest`, `StateControlTexturesTest`, `ColorValuesTest`, `UiFocusTraversalTest`, `UiLocalStateTest`, `SharedVisualStyleTest`, `StageThreeVisualResourcesTest` and component-editor tests. These do not use the real Minecraft GUI renderer or resource-pack refresh pipeline.

**Open QA after implementation:** screen-reader narration/accessibility, non-AnchorUI tooltip renderers, and GPU blur/overlay integration. Continue to Stage 4/5 only after these are accepted or explicitly deferred with a reason.
