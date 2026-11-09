# Anchor UI – Product direction

Anchor UI should let Minecraft modpack creators build useful HUDs and game screens through JSON resource packs without needing to write a separate Java mod. Custom Java widget factories remain available to extend the built-in capabilities.

## Current priorities

- **RPG-style HUDs:** first numeric data bindings and the `anchorui:progress` widget are implemented, including live health, hunger and experience sources. Next: richer status effects, conditional visibility, images, containers and additional data types.
- **Dialogue and quest interfaces:** first JSON buttons and client-only `open_screen` / `close_screen` actions are implemented, together with a standalone two-page dialogue resource pack. Next: portraits, dialogue state, conditions and server-authorized quests/rewards.
- **JSON authoring with resource packs:** all built-in resources and the reload listener now use `assets/<namespace>/anchorui/`. An installable starter resource pack is included under `examples/resourcepack/`. The optional external `anchorui/` root directory remains reserved and is not automatically created or loaded.

## Deferred, not ruled out

- Character/profile and skill-tree screens.
- More elaborate custom game systems using dedicated server-side features.

## Architecture guardrails

- Preserve compatibility with existing `assets/<namespace>/anchorui/` resources.
- Keep JSON declarative and avoid arbitrary script execution.
- Reuse the same widget registry, layout system and validation for HUD and screens.
- Prefer event-driven changes to dynamic values; avoid recreating every widget per frame.
- Keep game-changing actions server-authoritative, while visual UI remains client-side.
