# Stage 5 — Animation tracks QA

Run these checks in Minecraft 1.21.1, with a build containing the
`animations` array implementation. JUnit covers parsing and timing; a
client is required to check visual rendering and hitboxes.

## Screen and runtime

```text
/anchorui screen open anchorui:stage5_animation_demo
```

- [ ] Reopen: each element enters smoothly. The central panel executes
  **two simultaneous open tracks** (fade + delayed motion).
- [ ] The delayed movement begins after its configured wait without
  changing the element's final anchor or its layout position.
- [ ] Toggle panel repeatedly: exit becomes non-interactive immediately;
  entrance is restored after reappearing.
- [ ] Trigger pulse repeatedly: data-change animation is independent of
  the entrance tracks and continues from the current frame on retrigger.
- [ ] Clicking/dragging/wheeling an animated element follows its actual
  scaled visual hitbox; children respect clipping/parent scroll constraints.
- [ ] Close with fade waits for the longest `delay + duration` of its
  enabled closing tracks and doesn't accept input during the transition.

## HUD

```text
/anchorui test animation on
```

- [ ] Header, health and food bars enter; health starts with simultaneous
  effects including delayed motion.
- [ ] Taking damage triggers the health watch without re-running unrelated
  open effects.
- [ ] F8 editor previews use stationary geometry and remain selectable.
- [ ] Disable via `/anchorui test animation off`. Nothing remains rendered.

## Dynamic F8 editor (both screen and HUD)

- [ ] Open **ANIMATION [+]**, add three tracks, give each a unique ID.
- [ ] Expand an individual track header. Change its Trigger, Effect,
  Duration, **Delay (ms)**, Easing, Enabled.
- [ ] Duplicate a track, reorder it up/down, delete another, verify no
  duplicate IDs and no unexpected changes to other tracks.
- [ ] For Custom, edit both keyframes (opacity/X/Y/scale) and reopen.
- [ ] Change a trigger to `change`: it starts disabled. Enter the
  **Watch binding JSON**, then enable it. Invalid binding data reports
  an error without corrupting the saved configuration.
- [ ] Save, reopen and verify the complete list persists. Cancel leaves
  saved values unchanged; individual reset buttons affect only that field.
- [ ] Collapse/expand ANIMATION and the individual tracks; verify scroll
  height, hitboxes and inspector position update correctly.
- [ ] Try the editor at a narrow GUI scale and with nested scrollable
  children. Check Select popups and the Color Picker still layer correctly.

## JSON validation

- [ ] Invalid/duplicated animation ID, negative delay, invalid preset,
  invalid watch, invalid from/to frame or oversized duration should report
  the offending widget/animation in latest.log.
- [ ] The removed singular `animation` object must not silently work;
  use the `animations` array exclusively.
- [ ] Explicit widget `animations: []` overrides document defaults.

Vanilla F3+T is **not** intercepted in an open GUI. Close the screen first,
reload from the world, and reopen to inspect changed resource-pack JSON.
