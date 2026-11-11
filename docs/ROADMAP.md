# Anchor UI – Product direction

Anchor UI should let Minecraft modpack creators build useful HUDs and game screens through JSON resource packs without needing to write a separate Java mod. Custom Java widget factories remain available to extend the built-in capabilities.

## Current priorities

- **RPG-style HUDs:** typed data bindings (numbers, strings, booleans, objects and lists), namespaced data formatters, nested paths, live text, progress and data-list widgets are implemented. Compiled `visibleWhen` and `enabledWhen` Conditions support boolean logic and typed comparisons. Next: conditional content, richer screen actions and local UI State.
- **Dialogue and quest interfaces:** first JSON buttons and client-only `open_screen` / `close_screen` actions are implemented, together with a standalone two-page dialogue resource pack. Next: portraits, dialogue state, conditions and server-authorized quests/rewards.
- **JSON authoring with resource packs:** all built-in resources and the reload listener now use `assets/<namespace>/anchorui/`. An installable starter resource pack is included under `examples/resourcepack/`. Resource packs are the sole supported no-Java distribution method. No separate game-root loader is planned.

## Deferred, not ruled out

- Character/profile and skill-tree screens.
- More elaborate custom game systems using dedicated server-side features.

## Architecture guardrails

- Preserve compatibility with existing `assets/<namespace>/anchorui/` resources.
- Keep JSON declarative and avoid arbitrary script execution.
- Reuse the same widget registry, layout system and validation for HUD and screens.
- Prefer event-driven changes to dynamic values; avoid recreating every widget per frame.
- Keep game-changing actions server-authoritative, while visual UI remains client-side.
