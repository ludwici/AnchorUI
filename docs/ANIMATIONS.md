# Declarative widget animations (Stage 5)

AnchorUI animates **rendering**, not the underlying anchor or layout coordinate.
This works on authored screens, structural children, and HUD widgets using the
same `animation` property. The render transform combines with existing
container transforms, clipping and scrolling. No custom Java rendering code
or global per-tick loop is required.

## Triggers

- `open` — first display of an animated widget (screen or HUD).
- `show` — a widget becomes visible again (such as `visibleWhen`).
- `hide` — a visible widget becomes hidden. Its visual exit may finish, but
  it immediately loses mouse interaction.
- `close` — when an authored screen is closed with `close_screen`; the
  screen stays open for the longest configured close duration, suppressing
  further interaction. HUD widgets have no screen-close lifecycle.
- `change` — a bound value changes, configured with `animation.watch`.
  For example, a progress bar can react to player health changes.

## Explicit keyframes

Each trigger supports `duration` (integer milliseconds, 1–10000), optional
`easing`, and `opacity`, `x`, `y`, `scale` arrays of **two numbers**
(`[from, to]`). `x` and `y` are visual pixel offsets from the fixed anchor;
`scale` is centered on the widget and must be positive. `opacity` is 0–1.
Omitted values use identity. Existing layout geometry and saved F8 offsets
are not changed.

```json
{
  "type": "anchorui:panel",
  "id": "mypack:menu_panel",
  "size": {
    "width": 180,
    "height": 90
  },
  "animation": {
    "open": {
      "duration": 450,
      "easing": "easeOut",
      "opacity": [0, 1],
      "y": [16, 0],
      "scale": [0.9, 1]
    },
    "hide": {
      "duration": 250,
      "opacity": [1, 0]
    }
  }
}
```

Available easing values: `linear`, `easeIn`, `easeOut`, `easeInOut`,
`outCubic`. Each animation is sampled during rendering; it does not
rebuild the UI tree every frame.

## Presets in F8

Select an authored widget in the screen or HUD F8 editor. Open the **ANIMATION**
section below Visual / Content. For each supported trigger — Open, Show, Hide,
Close — choose an effect, duration and easing. Available effect choices are:

- `none`: disable a local effect, including an inherited screen default.
- `fade`: opacity 0 → 1 on entry, 1 → 0 on exit.
- `slideLeft`, `slideRight`, `slideUp`: move visually without changing
  the widget's anchor coordinates.
- `zoom`: scale 0.82 → 1 on entry, 1 → 0.82 on exit.
- `custom`: retain hand-authored explicit keyframe arrays.

Preset duration defaults to 300 ms; the editor's duration and easing fields
can override it. F8 overrides are saved in the existing screen/HUD layout
configuration, not back into the resource pack. Reset (`R`) removes the
individual override. During F8 editing the preview stays still, so elements
can be selected and dragged reliably. Open/Show/Hide/Close presets can be
configured without editing JSON. Advanced `change` watches remain JSON-only.

An authored track may also use a `preset`:

```json
{
  "animation": {
    "open": {
      "preset": "slideLeft",
      "duration": 400,
      "easing": "outCubic"
    },
    "close": {
      "preset": "fade",
      "duration": 220
    }
  }
}
```

The `animation` object can also appear at a **screen/HUD document root** as
defaults for its widgets; a widget-level trigger replaces the corresponding
root trigger. `preset: "none"` disables that trigger on a specific widget.

## Bound change trigger

```json
{
  "animation": {
    "watch": {
      "bind": "anchorui:player.health",
      "fallback": 20
    },
    "change": {
      "duration": 350,
      "easing": "easeOut",
      "scale": [1.15, 1]
    }
  }
}
```

`change` requires `watch` and will run when the bound value changes.
Changes that overlap an ongoing animation continue from the current
interpolated transform to avoid a visible jump. Watch sources are evaluated
only for configured animated widgets.

## In-game test commands

```text
/anchorui screen open anchorui:stage5_animation_demo
/anchorui test animation on
/anchorui test animation off
```

The screen demo showcases open, show, hide, change and close. The separate
HUD demo shows a sliding heading, health/fill effects and a bound health
change animation. The HUD normally draws only while the world HUD is visible;
F8 HUD editor preview deliberately holds widgets still.

See [Stage 5 QA](QA_STAGE_5.md). The client QA is needed to verify real
rendered opacity, nested clips and drag/scroll hitboxes under scale and
resource-pack overrides.
