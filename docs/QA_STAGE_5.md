# Stage 5 — Animation acceptance checks

Build and unit tests verify animation configuration and timing, not the
Minecraft renderer. The visual checks below must run in a real 1.21.1 client.

## Animated screen

```text
/anchorui screen open anchorui:stage5_animation_demo
```

- [ ] Reopen the screen: heading and buttons fade, slide and scale without
  disturbing their final anchors or sizes.
- [ ] Click **Toggle panel** repeatedly: panel hides with an exit effect,
  immediately loses mouse interaction, then animates back in.
- [ ] Click **Trigger pulse** repeatedly: bound change restarts from its
  current frame without an obvious jump.
- [ ] Click a button *during* its entrance: the click and hover regions must
  follow the visible position rather than the final anchor.
- [ ] Drag or scroll any animated interactive widget using a separate pack:
  drag distances and coordinates must follow its displayed scale, and
  clipping prevents hidden/clipped children from receiving mouse events.
- [ ] **Close with fade** finishes its transition before returning to the
  previous screen, and controls stop accepting input during closing.

## Animated HUD

```text
/anchorui test animation on
```

- [ ] The heading and two progress bars appear in the upper-right area and
  animate independently. They do not modify the existing RPG HUD.
- [ ] Cause player health to change; the health bar briefly scales and
  returns to normal. It should not pulse continuously while health is steady.
- [ ] Open the HUD F8 editor: widget bounds, list, inspector and dragging
  are stable even though runtime animations exist.
- [ ] Hide the HUD using the normal Minecraft Hide GUI setting: no UI
  shows behind a hidden HUD. Disable with `/anchorui test animation off`.

## Animation inspector

On either demo open **F8**, select a widget and scroll to **ANIMATION**.
Change Open effect to Fade/Zoom or Slide, choose duration and easing,
then Save. Reopen to verify the new transition. Reopen F8 and reset one field;
confirm the stored override is removed without resetting other fields.
Test Cancel to ensure unsaved changes do not persist. Try both screen and
HUD editors.

## Interaction and performance

- [ ] Test nested scrollable panels while a child uses Fade + Scale.
- [ ] Test dropdowns, tooltips, color picker and editor overlays, including
  animation in an ancestor container, at different GUI scales.
- [ ] Confirm opening a large screen with many nonanimated elements does
  not create a repeating full layout rebuild or global tick work.
- [ ] Ensure a malformed animation property identifies the widget in the
  log, rather than crashing the client or invalidating unrelated widgets.

F3+T is unchanged: close the GUI and reload resources from the Minecraft
world before reopening the screen.
