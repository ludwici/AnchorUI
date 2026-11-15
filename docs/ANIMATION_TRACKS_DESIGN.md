# Animation tracks v2 — design proposal

**Status:** proposed architecture, **not yet implemented**. The current
`animation.open/show/hide/change/close` format and its F8 presets remain
supported and unchanged until migration code and tests are in place.

## Problem

The current `UiAnimationSpec` has a map from `Trigger` to one `Track`.
Consequently a widget can only have one definition per trigger. The
current F8 inspector exposes fixed Open/Show/Hide/Close slots, making it
awkward to combine two entrance effects or configure several independent
data-change animations.

**Goal:** any number of named animation tracks per widget, including
multiple tracks with the same trigger, without introducing
`open2`, `open3`, or a hard-coded maximum.

## Proposed resource-pack JSON

Keep existing `animation` as a legacy declaration. Introduce an
`animations` **array**. Each entry has a stable, unique `id`, a trigger
and an effect. Multiple entries may listen to the same trigger:

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health_bar",
  "animations": [
    {
      "id": "intro_fade",
      "trigger": "open",
      "duration": 400,
      "easing": "easeOut",
      "from": {
        "opacity": 0
      },
      "to": {
        "opacity": 1
      }
    },
    {
      "id": "intro_slide",
      "trigger": "open",
      "duration": 600,
      "easing": "outCubic",
      "from": {
        "x": -36
      },
      "to": {
        "x": 0
      }
    },
    {
      "id": "health_pulse",
      "trigger": "change",
      "watch": {
        "bind": "anchorui:player.health",
        "fallback": 20
      },
      "duration": 240,
      "easing": "easeOut",
      "from": {
        "scale": 1.15
      },
      "to": {
        "scale": 1
      }
    }
  ]
}
```

The list has no arbitrary item-count limit. Runtime resource limits
can still guard unreasonable duration, deep nesting, duplicate IDs,
invalid bindings and memory use. The array's order is stable so the
editor can reorder tracks deterministically.

### Initial schema

- `id`: unique within this widget, required, stable across editing and
  widget rebuilds. Not an `id` of a child widget.
- `trigger`: initially `open`, `close`, `show`, `hide`, `change`.
  Each track independently has its own trigger and timing.
- `watch`: binding required for `change`. It is per track, not per widget.
- `preset`: optional `fade`, `slideLeft`, `slideRight`, `slideUp`,
  `zoom`; alternatively use explicit `from`/`to`.
- `duration` and `easing`: same rules and names as v1.
- `delay`: optional nonnegative milliseconds before the effect starts.
  This allows a staggered sequence without a separate animation DSL.
- `from` and `to`: optional properties `opacity`, `x`, `y`, `scale`.
  Any absent property uses the neutral transform (1, 0, 0, 1).
- `enabled`: optional boolean; disabling a track preserves its ID and
  editor configuration, avoiding destructive removal.

For the first version, avoid introducing new properties such as
rotation or color until the rendering system supports them reliably.
A track with no effect on any supported property should be rejected.

### Composition and timing

Each track has its own clock and state. Multiple tracks may run
simultaneously for the same trigger. At a given frame the widget's
combined render transform is composed deterministically:

- X/Y translation: **sum** of active track offsets.
- Scale: **product** of active track scale multipliers.
- Opacity: **product** of active track alpha multipliers (clamped 0–1).

Tracks that are inactive contribute the neutral value, **not their
start or end keyframes**. This prevents an inactive fade from making
the widget permanently invisible.

If the same track is retriggered before completing, it starts at its
current interpolated value, not at its original `from` value. A new
trigger should not cancel *unrelated* tracks.

A `hide` trigger makes the underlying widget non-interactive
immediately while its visual exit may continue. A screen `close`
waits for the latest **finite** close track, with a bounded maximum
duration. Looping close tracks must not block navigation.

Loop/ping-pong, sequences and explicit Java-triggered animation events
are candidate follow-ups, not mandatory for the first v2 change.
For sequences in the first implementation, `delay` is sufficient
to create simple staggered groups.

### Layout, interaction and performance

Animation composition happens after Layout. The widget ID, anchor,
layout size and persisted F8 offsets remain unchanged. The same
composed transform must be used for render, hit-testing, drag delta,
scroll hitboxes and popups, respecting ancestor transforms and clipping.

Compile definitions **once** on screen/HUD creation or config changes.
Keep a map of track instances keyed by **widget ID + animation ID**,
plus a small list of active tracks and watched bindings. Rendering
should not scan unrelated widgets or rebuild UI trees each frame.
F8 editing previews stable geometry; replay can be an explicit
editor action later.

### Resource inheritance and compatibility

Document/root defaults can provide animation tracks, while widget
tracks merge **by animation ID**, preserving predictable inheritance
and allowing a child to override/disable one inherited track without
replacing all others.

Do not break older packs. Interpret legacy
`animation.open/show/hide/change/close` as generated internal tracks
with reserved IDs such as `legacy:open`. The public JSON stays intact;
no forced resource-pack migration.

Both syntaxes should work on screens and HUD. Mixed old+new syntax must
be defined clearly and tested. A duplicate user-defined ID must be a
configuration error with the widget ID and animation ID in the log.

## F8 editor: dynamic track list

Replace the fixed Open/Show/Hide/Close slot grid **after** v2 runtime
support is implemented. The top-level **ANIMATION [+]/[-]** section
remains collapsible. Inside it:

1. **Add animation** creates a new unique ID (and immediately shows
   trigger, preset, duration and easing).
2. Each track gets a collapsible row with its name, trigger, enabled
   state and individual settings.
3. Support **duplicate**, **delete**, **move up/down** and **reset**.
   Reordering changes deterministically how tracks are listed, but
   not the established composition rules.
4. A `change` track exposes a per-track watch binding and accepts
   developer-defined bindings, not just a fixed `quest` key.
5. Save/Cancel use the existing draft editor state. Changes remain
   in a client override file unless the developer edits resource JSON.

An arbitrary JSON array cannot be represented by today's fixed
`WidgetEditorRegistry.Field` paths alone. Add a dedicated typed
animation editor model and persist a **complete, validated animation
track list** as a widget override, instead of inventing numbered
primitive fields or overloading `appearance`. Existing preset fields
remain compatible with older saved editor configurations.

## Implementation phases

1. Add a v2 parser, validation, v1 adapter, immutable track model and
   unit tests for multiple same-trigger tracks.
2. Add parallel/composed runtime with separate per-ID clocks and
   tests for overlaps, rapid retriggers and visibility/close lifecycle.
3. Wire common screen+HUD rendering, clip/input transformations,
   per-track bindings and performance counters.
4. Introduce the dynamic F8 list editor and migration of legacy
   saved overrides without losing previously configured animations.
5. Add demos with multiple same-trigger tracks, nesting and close
   behavior; run actual client QA before marking v2 complete.

This is the proposed direction, not a promise of runtime support
before these phases are implemented and verified.
