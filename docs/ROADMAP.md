# AnchorUI — Product Direction (NeoForge 1.21.1)

See [the canonical roadmap](../ROADMAP.md) for completed items, detailed priorities and acceptance criteria. Status last reviewed **2026-11-12**.

## Framework mission

AnchorUI provides reusable, JSON-driven tools for assembling Minecraft HUDs and interactive screens. Resource packs supply UI documents, templates, styles and textures; Java mods register custom widgets, data, actions, conditions and layouts. It remains a **UI framework**, not a gameplay or narrative engine.

**Not part of AnchorUI's roadmap:** a dialogue system (NPC conversations, branching dialogue graphs, dialogue progression/history and narrative rules) or a quest engine (objectives, rewards and persistence). These should be separate mods built on AnchorUI. Existing quest-like list data and simple two-page screen demos remain tests of generic UI primitives, not features to turn into a dialogue or quest subsystem.

## Already implemented

- [x] Nine-point anchors, nested panels, padding, clipping, auto sizing, scrolling, images, textures, borders, rounded corners and nine-slice.
- [x] HUD and screen editors with saving, snapping, parent selection, reset and read-only vanilla HUD anchors.
- [x] Typed Data Bindings, formatted text, nested paths, Java `DataValueRegistry` and mutable/push-style providers.
- [x] Compiled `visibleWhen`/`enabledWhen` Conditions with typed operations and Java-registered operators.
- [x] Screen events, Java action registration and typed action arguments.
- [x] Per-screen Local UI State, `set_state`/`toggle_state`, data bindings, checkbox, switch, select and tabs controls.
- [x] Textured control skins with per-state sprites, shared styles, UV selection and nine-slice.
- [x] `anchorui:template_list` with per-row Item Context, resource-pack/Java named templates, grids, custom Java layouts, stable keyed IDs and selective widget retention on reorder.
- [x] Standard resource-pack content under `assets/<namespace>/anchorui/`, test scenes and CI.

## Next priorities

1. **Generic conditional content and action guards:** reusable `cases`/variant selection; evaluate action predicates at interaction time; validate errors and integration with Local UI State and Template Lists.
2. **Automatic layouts:** general row/column/flow layout for containers, spacing/alignment, optional wrap and flexible sizing; opt-in layout-space collapse for hidden elements.
3. **Style and input fidelity:** consistent progress-bar/widget frames, texture/style inheritance, blur/render order, missing-asset errors, resource reload and keyboard/focus behavior.
4. **Lifecycle and navigation:** optional general-purpose screen-state handoff, predictable focus and reopen behavior; no dialogue-specific state machine.
5. **Declarative animations:** generic fade/slide/transition effects that respect transforms, clipping and input.
6. **Ongoing quality:** API stabilization, dirty/invalidation-aware data and layout performance, automated/client QA, GitBook documentation and future supported-branch CI.

## Guardrails

- Resource packs are the only no-Java content distribution method; **no External Lore Loader**.
- JSON remains declarative; Java integrations may extend registries but JSON cannot execute arbitrary Java or scripts.
- Reuse a common layout/registry/validation model for HUDs and screens.
- Do not rebuild unchanged widget instances unnecessarily or add server tick loops merely to watch UI data.
- The integrating mod/server validates any gameplay-affecting action.
- Keep backwards compatibility for existing authored UI where feasible and document intentional changes.
