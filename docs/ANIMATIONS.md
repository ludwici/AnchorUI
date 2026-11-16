# Declarative animations — named tracks

AnchorUI lets resource-pack authors attach **any number of independent
animation tracks** to one widget. The same `animations` array works on
screens and on HUD widgets, including custom widget types registered in Java.
Rendering applies the animations *after* anchored layout calculations, so
animations do not change the saved editor offset or the widget's layout.

## The `animations` array

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health_bar",
  "animations": [
    {
      "id": "fade_in",
      "trigger": "open",
      "preset": "fade",
      "duration": 400,
      "delay": 0,
      "easing": "easeOut"
    },
    {
      "id": "slide_in",
      "trigger": "open",
      "preset": "slideLeft",
      "duration": 600,
      "delay": 200,
      "easing": "outCubic"
    },
    {
      "id": "health_pulse",
      "trigger": "change",
      "watch": {
        "bind": "anchorui:player.health",
        "fallback": 20
      },
      "duration": 240,
      "delay": 0,
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

There is **no fixed number of tracks or one-track-per-trigger restriction**.
Every track needs a unique `id` within its widget. IDs start with a letter
and can include letters, digits, `_` or `-`. All supported controls share
the same syntax.

The old single-object `animation` format was removed *before the first
release* and is intentionally rejected. Update unpublished resource packs
to use `animations`. There is no automatic migration layer.

## Track properties

| Property | Purpose |
|---|---|
| `id` | Unique stable animation ID within a widget |
| `trigger` | `open`, `show`, `hide`, `close`, `change`, or a Java-registered namespaced ID such as `mymod:quest_completed` |
| `preset` | `fade`, `slideLeft`, `slideRight`, `slideUp`, `zoom`, or `custom` |
| `duration` | Playback length in milliseconds, 1–10000; default 300 |
| `delay` | Wait before playback, in milliseconds, 0–60000; default 0 |
| `easing` | `linear`, `easeIn`, `easeOut`, `easeInOut`, `outCubic` |
| `from`, `to` | Optional keyframe objects overriding preset start/end values |
| `watch` | Bound-data object required for enabled `change` animations |
| `enabled` | Set `false` to keep a disabled track in the array |

Keyframes can specify `opacity` (0–1), `x`/`y` (-4096..4096 GUI pixels)
and `scale` (>0..4). Unspecified properties have neutral values: opacity 1,
translation 0 and scale 1, or the corresponding preset's start/end value.

### Delay behavior

A track starts its delay timer when its trigger fires. For `open` and
`show` animations, **the starting keyframe is held during the delay**,
so a delayed fade-in remains invisible until it begins and a delayed
slide-in waits at its starting offset. For `change`, `hide` and `close`,
the track contributes a neutral transform until the delay expires, then
interpolates from `from` to `to`. Multiple tracks remain independent.

A `hide` transition keeps the widget visually rendered through its delay and
duration but makes it non-interactive immediately. For a `close` transition,
the screen waits for the **largest delay + duration** among enabled
`close` tracks before navigating away.

### Simultaneous animations

All tracks with the same trigger start together (each with its own delay).
Other triggers can run independent tracks as their conditions change.
At each frame:

- Opacity = product of active opacity values.
- Scale = product of active scale values.
- X/Y = sum of active translation offsets.

Values are bounded before sending them to rendering. A delayed entry track holds its starting pose; a delayed change or exit track
makes no visual contribution until it starts. A triggered track retains its final
frame until the runtime clears it on a conflicting visibility/close trigger;
authors should normally use neutral `to` values for entrance/change tracks.
When the **same** track is triggered again during playback, it continues
from its currently interpolated frame to avoid snapping backwards.

A `change` track's `watch` is independent from every other track and can
bind to any registered data provider. Multiple health/energy/state watches
on the same widget will not overwrite one another. Disabled tracks are not
evaluated.

## Root-level defaults

An `animations` array can also be placed on a screen/HUD document root.
A widget without its own `animations` uses that array. If a widget explicitly
defines `animations`, including an empty array, its array **replaces** the
document defaults for that widget. This also makes F8 deletion and clearing
predictable.

## F8 editor

Open an authored screen or HUD and press **F8**. Select a custom widget,
expand **ANIMATION [+]**, then use:

- **Add animation**: creates a new uniquely named track.
- **Click a track header**: expand its individual settings.
- **Animation ID, Trigger, Effect, Duration, Delay, Easing and Enabled**:
  edit using the existing inspector controls.
- **Custom**: edit independent `from` and `to` opacity/X/Y/scale values.
- **Watch binding JSON**: for `change`, enter a valid bound-data object.
  When you first choose `change` without a watch the track is disabled;
  specify the watch first, then enable it.
- **Duplicate, Move up, Move down, Delete**: modify the track list.
  Duplicating generates a fresh unique ID.

The editor uses the existing **Save / Cancel** draft lifecycle. It persists
complete per-widget animation arrays in the client layout override, **not**
back into the resource pack. F8 preview uses stable nonanimated geometry.
There is no arbitrary maximum number of entries in the inspector.

## In-game tests

```text
/anchorui screen open anchorui:stage5_animation_demo
/anchorui test animation on
/anchorui test animation off
```

The screen demo includes two simultaneous entrance effects, delayed motion,
show/hide, data changes and close. The HUD demo has a delayed entrance
combined with an independent health-change effect.

See [Stage 5 QA](QA_STAGE_5.md). Minecraft vanilla F3+T behavior is
unchanged: reload resource packs from the world after closing the GUI.

## Java-defined namespaced triggers

You can register your own trigger logic, then reference its ID as
`trigger` in any animation array:

```json
{
  "animations": [
    {
      "id": "quest_glow",
      "trigger": "mymod:quest_completed",
      "duration": 500,
      "delay": 100,
      "preset": "zoom",
      "easing": "outCubic"
    }
  ]
}
```

Register the trigger **once**, during your mod's client initialization:

```java
UiAnimationTriggerRegistry.registerCondition(ResourceLocation.fromNamespaceAndPath("mymod", "quest_completed"), context -> MyQuestClientState.justCompletedQuest());
```

A condition triggers only on a **false → true** transition. It must become
false before another true value can start it again. For an event which may
occur several times, register a monotonically increasing revision:

```java
UiAnimationTriggerRegistry.register(ResourceLocation.fromNamespaceAndPath("mymod", "quest_completed"), context -> MyQuestClientEvents.getCompletionRevision());
```

Increase the revision in your mod's Java event handler when a quest is
completed. AnchorUI samples that counter when the associated animated
widget renders and starts the animation if the revision increased. The
initial value is remembered rather than played immediately. This API does
not install a global tick listener, does not need an AnchorUI-specific
event bus, and does not implement quest progression itself.

The F8 editor's **Trigger ID** field accepts the standard trigger names
or custom namespaced IDs. Keep IDs registered before any screen/HUD that
uses them is displayed. Use a *client-side* data source: the condition
and revision callbacks are evaluated while rendering on the client.

## General-purpose visual size transitions

`sizeTransition` smoothly interpolates the **visible width and/or height**
of a widget as numeric data changes. It is a reusable visual-size primitive,
not a progress-bar-only `smoothFill` option. Its cropping happens after
layout and leaves the original anchor and size unchanged, so nearby widgets
do not jump. Textured widgets render at their original size and are **cropped**,
rather than having their artwork squeezed into a smaller rectangle.

For example, put a textured `anchorui:image` on top of a separate background
panel, then crop the image to the player's health percentage:

```json
{
  "type": "anchorui:image",
  "id": "mymod:health_fill",
  "texture": "mymod:textures/gui/health_fill.png",
  "size": {
    "width": 180,
    "height": 16
  },
  "sizeTransition": {
    "duration": 420,
    "easing": "easeOut",
    "width": {
      "value": {
        "bind": "anchorui:player.health",
        "fallback": 0
      },
      "max": {
        "bind": "anchorui:player.max_health",
        "fallback": 20
      }
    }
  }
}
```

The same `sizeTransition` property can be used on ordinary panels and
other authored visual widgets. A `height` object works analogously to
`width`; both can be present. Each computes its own fraction
`value / max` (clamped to 0..1) and animates independently. The duration
is 1–10000 milliseconds (default: 400 ms), and easing supports the same
values as named animation tracks (default: `easeOut`). On first display
the interpolator starts at the *actual* fraction; subsequent changes
animate from the currently displayed fraction, even when changes happen
rapidly.

### Built-in RPG progress bars

An `anchorui:progress` already knows its `value` and `max`, so it can
use just the transition settings:

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health",
  "size": {
    "width": 180,
    "height": 16
  },
  "value": {
    "bind": "anchorui:player.health",
    "fallback": 0
  },
  "max": {
    "bind": "anchorui:player.max_health",
    "fallback": 20
  },
  "sizeTransition": {
    "duration": 420,
    "easing": "easeOut"
  }
}
```

Here only the **inner filled width** animates. The outer bar border and
text remain fixed, while numeric health text updates immediately. Textures
specified as `textures.fill` are cropped to the filled width, not stretched.
The bundled `rpg_status.json` enables this for health and food. Test via
`/anchorui test rpg on`, then take damage or consume food to see the change.

For a wholly custom bar look, use separate background and fill widgets
with `sizeTransition` on the fill element. No built-in progress artwork
is required. An independent `animations` array can still fade/slide/scale
the widget as usual.
