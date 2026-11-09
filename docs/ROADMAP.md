# Anchor UI – Product direction

Anchor UI should let Minecraft modpack creators build useful HUDs and game screens through JSON resource packs without needing to write a separate Java mod. Custom Java widget factories remain available to extend the built-in capabilities.

## Current priorities

- **RPG-style HUDs:** health and other resource indicators, status effects, progress, conditional visibility and custom visual composition. Complete the dynamic data/binding and built-in progress, image and container widgets incrementally.
- **Dialogue and quest interfaces:** declarative text, portraits, choices, steps and rewards, backed by an approved action/data interface. Server-side authority is required for anything that changes gameplay.
- **JSON authoring with resource packs:** use Minecraft's client resource pipeline as the default distribution method. Support the same namespaced widget IDs, anchors, refs and styles. The optional external `anchorui/` root directory is reserved but is not required by modpack creators or actively loaded.

## Deferred, not ruled out

- Character/profile and skill-tree screens.
- More elaborate custom game systems using dedicated server-side features.

## Architecture guardrails

- Preserve compatibility with existing `assets/<namespace>/lore/` resources.
- Keep JSON declarative and avoid arbitrary script execution.
- Reuse the same widget registry, layout system and validation for HUD and screens.
- Prefer event-driven changes to dynamic values; avoid recreating every widget per frame.
- Keep game-changing actions server-authoritative, while visual UI remains client-side.
