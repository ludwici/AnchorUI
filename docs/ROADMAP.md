# Anchor UI – Product direction

Anchor UI should let Minecraft modpack creators build useful HUDs and game screens through JSON resource packs without needing to write a separate Java mod. Custom Java widget factories remain available to extend the built-in capabilities.

## Current priorities

- **RPG-style HUDs:** typed data bindings (numbers, strings, booleans, objects and lists), namespaced data formatters, nested paths, live text, progress and data-list widgets are implemented. Compiled `visibleWhen` and `enabledWhen` Conditions support boolean logic and typed comparisons. Screen UI Events (`onOpen`, `onClose`, `onHoverEnter`, `onHoverLeave`, `onClick`, `onChange`) are implemented with registered client actions. Interactive JSON `anchorui:template_list` widgets repeat rows from list bindings with scoped Item Context and action payloads, and can reference reusable templates by ID from resource-pack `templates/` files or Java `WidgetTemplateRegistry` registrations. Grid placement for Template Lists (including custom Java layout algorithms) and per-screen Local UI State with set/toggle actions and data bindings are implemented. Built-in screen controls (checkbox, switch, select and tabs) now bind directly to Local UI State and emit onChange events. Next: richer conditional content and more granular row reconciliation.
- **Dialogue and quest interfaces:** JSON buttons, client-only `open_screen` / `close_screen` actions and screen/widget UI Events are implemented, together with a standalone two-page dialogue resource pack. Next: portraits, dialogue state and server-authorized quests/rewards.
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
