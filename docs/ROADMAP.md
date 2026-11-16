# AnchorUI — Product Direction (NeoForge 1.21.1)

See [the canonical roadmap](../ROADMAP.md) for completed items, detailed priorities and acceptance criteria. Status last reviewed **2026-11-15**.

## Framework mission

AnchorUI provides reusable, JSON-driven tools for assembling Minecraft HUDs and interactive screens. Resource packs supply UI documents, templates, styles and textures; Java mods register custom widgets, data, actions, conditions and layouts. It remains a **UI framework**, not a gameplay or narrative engine.

**Not part of AnchorUI's roadmap:** a dialogue system (NPC conversations, branching dialogue graphs, dialogue progression/history and narrative rules) or a quest engine (objectives, rewards and persistence). These should be separate mods built on AnchorUI. Existing quest-like list data and simple two-page screen demos remain tests of generic UI primitives, not features to turn into a dialogue or quest subsystem.

## Already implemented

- [x] Nine-point anchors, nested panels, padding, clipping, auto sizing, scrolling, images, textures, borders, rounded corners and nine-slice.
- [x] Opt-in row, column and general grid layouts on ordinary containers, including flex grow/shrink, wrapping, alignment, gaps, min/max dimensions, reactive hidden-child collapse and stable preferred widget sizes.
- [x] HUD and screen editors with saving, snapping, parent selection, reset and read-only vanilla HUD anchors.
- [x] Component-aware visual F8 editing for text, colors, button/controls skin paths and sizing, with safe Java-registered editor fields, live previews and local override persistence.
- [x] F8 parent picker now separates outer frame anchors, padded content anchors, and saved structural moves into row/column/grid (including root detach); source JSON sections are collapsed by default.
- [x] Typed Data Bindings, formatted text, nested paths, Java `DataValueRegistry` and mutable/push-style providers.
- [x] Compiled `visibleWhen`/`enabledWhen` Conditions with typed operations and Java-registered operators.
- [x] Screen events, Java action registration and typed action arguments.
- [x] Per-screen Local UI State, `set_state`/`toggle_state`, data bindings, checkbox, switch, select and tabs controls.
- [x] Textured control skins with per-state sprites, shared styles, UV selection and nine-slice.
- [x] `anchorui:template_list` with per-row Item Context, resource-pack/Java named templates, grids, custom Java layouts, stable keyed IDs and selective widget retention on reorder.
- [x] Standard resource-pack content under `assets/<namespace>/anchorui/`, test scenes and CI.

## Next priorities

1. **Conditional content and action guards — implementation complete:** `anchorui:cases` and action-time `when` support Conditions, named templates and Template Lists. The default reserve-space vs opt-in `collapseHidden` policy is now documented, tested, and consistent with scroll limits. In-client QA remains.
2. **Automatic layouts — planned functionality and regression tests delivered:** row/column/grid, flex, wrapping, reflow, scroll clamping and saved F8 changes have tests plus `hidden_space_policy_demo`. Next: the in-game [Stage 1/2 QA](QA_STAGE_1_2.md), covering GUI scales, scrolling, focus and editor overrides. Grid spans/named tracks are deferred, not release blockers.
3. **Style and input fidelity:** shared Progress/Panel/Button skins, accurate text alpha, keyboard Tab/Shift+Tab focus traversal, floating popups, deferred rich-text Tooltip, missing-style/texture diagnostics and screen revision refresh are implemented (without overriding vanilla F3+T behavior). Added compact GUI demo and RPG bar parity tests. Remaining: [in-client QA](QA_STAGE_3.md), blur/overlay order and accessibility/narration review.
4. **Lifecycle and navigation — completed and in-game confirmed:** typed `open_screen.stateHandoff` copies selected Local UI State keys into new screens; invalid transitions retain the source screen, and widget IDs restore focus after return/rebuild. The user confirmed the Stage 4 interactions work. Vanilla F3+T remains unchanged, with resource reload regression QA tracked separately. Broader State Scopes are deferred pending a real use case; no dialogue-specific state machine.
5. **Declarative animations — named tracks implemented; in-client QA pending:** the `animations` JSON array supports unlimited independently named tracks with simultaneous transforms, separate change watches and millisecond delay. Both F8 editors provide dynamic add/duplicate/reorder/delete and keyframe controls. See [Animation API](ANIMATIONS.md) and [Stage 5 QA](QA_STAGE_5.md).
6. **Ongoing quality:** API stabilization, dirty/invalidation-aware data and layout performance, automated/client QA, GitBook documentation and future supported-branch CI.

## Guardrails

- Resource packs are the only no-Java content distribution method; **no External Lore Loader**.
- JSON remains declarative; Java integrations may extend registries but JSON cannot execute arbitrary Java or scripts.
- Reuse a common layout/registry/validation model for HUDs and screens.
- Do not rebuild unchanged widget instances unnecessarily or add server tick loops merely to watch UI data.
- The integrating mod/server validates any gameplay-affecting action.
- Keep backwards compatibility for existing authored UI where feasible and document intentional changes.
