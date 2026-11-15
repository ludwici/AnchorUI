# AnchorUI — Roadmap and Decision Log

> Status reviewed against the `1.21.1` NeoForge branch on **2026-11-12**. This is the roadmap for the **UI framework**, not for games, quests, NPC conversations, or other systems built on top of it. Checked items describe functionality already present; unchecked items remain work or QA.

## 1. Purpose and boundaries

AnchorUI is a reusable, JSON-driven UI framework for Minecraft, with WoW-style anchors, HUDs and interactive screens. Mod and modpack authors should be able to assemble interfaces using regular Minecraft resource packs; Java integrations may register additional widgets, data providers, conditions, formatters, templates, layouts and actions.

Core decisions:

1. Authoring resources live under `assets/<namespace>/anchorui/` in mod assets or standard resource packs. **No External Lore Loader** or separate game-root content directory.
2. Keep test/anchor HUD examples separate from the RPG HUD example. Both are examples, not gameplay systems.
3. Preserve namespaced IDs, cross-mod reuse, Java extensibility, typed configuration and resource-pack overrides.
4. Prefer event-published state and selective invalidation over unnecessary tick polling or rebuilding widget trees every frame.
5. JSON is declarative, not a scripting environment. Gameplay-changing actions are controlled and validated by the integrating server-side mod.
6. **AnchorUI does not implement a dialogue system.** NPC conversations, branching stories, dialogue graphs, conversation progression, portraits and history belong in a separate mod built with AnchorUI. Existing example screens are general-purpose screen/action demonstrations, not a commitment to develop a dialogue engine.
7. Likewise, quest progression, rewards and quest persistence belong to external mods. AnchorUI supplies only generic UI primitives and integration points; quest-like example data may be used to exercise them.

## 2. Implemented foundation

### Anchors, composition and editing

- [x] Nine-point `AnchorLayout` with `point`, `anchor`, `relativeTo`, offsets, parent chains, resize support and diagnostics for bad references/cycles.
- [x] HUD JSON composition across mods and resource packs, separately toggled test and RPG HUD demos.
- [x] F8 HUD editor and context-sensitive screen editor: drag, parent/anchor selection, magnetic snapping, visibility overrides, reset, inspector and persistent local positioning.
- [x] Read-only vanilla HUD anchors for layout alignment; no modification of vanilla HUD elements.
- [x] Nested panels/containers, padding, opt-in clipping, basic group scaling, content-based `autoSize` and vertical/horizontal scrolling.
- [x] JSON visual primitives: images, texture regions, tint/alpha, nine-slice, borders and rounded panel geometry.

### Extensible UI model

- [x] Namespaced widget types/IDs, `imports`, reusable `widgets/*.json`, references and overrides.
- [x] Public Java widget factories and typed `Codec<T>` registration; reusable template definitions can come from resource packs or Java.
- [x] Typed Data Binding for numbers, strings, booleans, objects and lists; nested paths, fallbacks, rich/live text, numeric progress, data lists and namespaced formatters.
- [x] Java data providers, including `registerMutable` and publish-on-change caching.
- [x] Compiled `visibleWhen` and `enabledWhen` Conditions: Boolean logic, typed comparisons, string/array operations and custom Java-registered operators.
- [x] Screen events (`onOpen`, `onClose`, `onClick`, `onHoverEnter`, `onHoverLeave`, `onChange`) with typed, namespaced Java action registration.
- [x] Per-screen Local UI State with `set_state`, `toggle_state`, data bindings, validation and state preservation through same-screen reinitialization.
- [x] Built-in checkbox, switch, select and tabs controls, with state bindings, input events and resource-pack texture skins (variants, sprites/UV, nine-slice and shared styles).
- [x] `anchorui:template_list` with item context, stable `key`, nested reusable templates, grid layout, custom Java layout factories and selective keyed widget reconciliation. Reordered rows retain widget instances and their Item Context index is updated.
- [x] Resource reload-backed JSON loading, example resource pack, test scenes, unit tests and GitHub Actions build.

The completed functionality above does **not** imply finished visual consistency, a general flow layout system, condition-driven content selection, action guards or polished resource failure diagnostics.

## 3. Next work — ordered priorities

### Stage 1 — Generic conditional content and guarded actions

**Status: core delivered; follow-up layout behavior and in-client QA remain.** Extend the existing Conditions engine, not a domain-specific conversation or quest system.

- [x] Implement `anchorui:cases` for first-match widget/template selection, including a fallback and compiled Conditions.
- [x] Add `when` action-level guards evaluated at event time; invalid expressions fail validation and unavailable data fails closed.
- [x] Keep both features compatible with existing `visibleWhen` / `enabledWhen`, UI State, named templates and resource packs.
- [x] Add unit tests for nested widgets, template rows, first-match selection, missing/malformed conditions and live guard updates. In-game QA is still pending.
- [ ] Define an opt-in policy for whether hidden children keep or collapse their layout space; coordinate with Stage 2.

**Done when:** a JSON UI can select content and safely gate a generic action without implementing its own widget or scripting logic.

### Stage 2 — General-purpose automatic layouts

**Status: automatic row/column/grid layouts, flex sizing and wrapping implemented; in-game QA and advanced edge cases remain.** The older `template_list` layout engine is independent.

- [x] Add opt-in `row` and `column` layouts for normal panels/containers with `gap`, cross-axis `align`, main-axis `justify` and `space_between`.
- [x] Support `align: stretch` along a fixed cross axis, with preferred sizes retained for auto-sized cross axes.
- [x] Support opt-in `collapseHidden` with condition-triggered relayout, including local state and hidden `cases` alternatives.
- [x] Preserve manual anchors and F8 local offsets: children with external/non-top-left anchors or `layoutItem: false` opt out of the stack. Layout bounds integrate with scrolling, clipping and scaling.
- [x] Add automated tests for nested auto-sized containers, column/row alignment, distribution, padding, hidden rows and explicit anchors, plus a resource-pack demo.
- [x] Add explicit per-child `layoutItem.grow` / `shrink` weights and min/max width/height constraints, leaving weights at zero unless configured.
- [x] Add optional row/column `wrap` and `lineGap`, with per-line distribution and natural cross-axis auto-sizing.
- [x] Add independent panel/container `grid` with columns, configurable gaps/cell sizes, per-cell align/stretch and hidden-cell reflow.
- [x] Make every direct structural Grid child automatically managed by its parent settings, regardless of legacy child anchors or `layoutItem.enabled`; simplify F8 Parent to direct container insertion and hide generated internal context fields.
- [x] Treat `anchorui:panel` as a default parent without additional authored container/context nodes; keep nested hierarchy outside per-widget JSON, use Panel IDs for child anchors and apply padding in layout resolution. Preserve old `/__content` references for compatibility.
- [x] Preserve Screen Editor inspector scroll when changing values via `+`/`-` with SOURCE JSON collapsed; show one parent label instead of duplicated internal parent/anchor details.
- [x] Add focused geometry tests for constrained flex, wrapping in both axes, grid auto sizing, cell alignment and hidden cells.
- [ ] Consider later CSS-like features only when demanded by use cases: grid spans, named tracks, richer responsive constraints and fine-grained incremental layout. These are not part of the first generic Grid release.
- [x] Update HUD and screen F8 editors to inspect and edit row/column/grid/flex layout fields using persisted per-widget overrides, and browse nested widget structure in a collapsible hierarchy.
- [x] Distinguish coordinate anchor targets from structural container parents in both F8 editors; support container/content/move picker, move-to-root, cycle-safe saved overrides and default-collapsed read-only source JSON.
- [ ] Complete runtime QA for editor dragging, layout-field cycling, Save/Cancel, GUI resize and scale, nested scrolling, moving focus and resource reload.

**Acceptance:** normal panels and containers can arrange responsive rows, columns and cell grids using resource-pack JSON, with in-client verification of advanced focus/scale/scroll cases still outstanding.

### Stage 3 — Consistent appearance and input polish

**Priority: high; can proceed alongside Stage 2.**

- [x] Add a component-aware **VISUAL / CONTENT** inspector to HUD and screen F8 editors: editable text, basic colors, texture resource paths, sizes, basic widget styles and typed controls.
- [x] Register custom editable widget fields through an explicit Java `WidgetEditorRegistry`; persist appearance overrides separately from resource packs and validate edits.
- [x] Rebuild edited widget visuals on explicit changes, preserving screen Local UI State, with rollback for invalid widget configurations.
- [ ] In-game QA for appearance editing, resource reload, modal focus and custom Java widget codecs.
- [ ] Unify visual style inheritance and appearance across progress bars, buttons, panels and interactive controls, including hover/disabled/focused states where appropriate.
- [ ] Verify matching borders/frames on RPG HUD progress bars; keep the earlier second-bar regression in the QA suite.
- [ ] Audit rendering order for screen backgrounds, blur, outlines, overlays, tooltips and clipping.
- [ ] Improve missing-texture and malformed-style diagnostics; test F3+T reload and resource-pack replacement.
- [ ] Improve keyboard focus, navigation, tab order and accessibility for interactive screens.
- [ ] Extend demos with consistent texture/scale themes, including small GUI scales.

**Done when:** shared style resources give consistent results across built-in widgets, with stable rendering and input behavior.

### Stage 4 — State, lifecycle and screen navigation refinements

**Priority: medium.** Local UI State and basic screen actions are already implemented.

- [ ] Specify an optional, explicit state handoff/shared scope for screen navigation (if needed), without introducing a dialogue-specific state machine.
- [ ] Define lifecycle semantics for focus, reopening, navigation failures and resource reload.
- [ ] Clarify action guard and state validation errors for resource-pack authors.
- [ ] Test screen transitions, reopen/reset, invalid targets and nested interactive components.

**Done when:** independent screens can compose safely with predictable state lifetime and focus restoration.

### Stage 5 — Declarative animation primitives

**Priority: medium, after stable layout/visibility behavior.**

- [ ] Provide general-purpose fade, slide and interpolation primitives for widgets and screens.
- [ ] Define triggers on open/close, state changes and visibility/data changes, plus easing and duration configuration.
- [ ] Maintain correct anchors, clipping, transformed hitboxes, scroll and editor behavior during animations.
- [ ] Avoid global per-tick scans and per-frame tree recreation; add performance tests.

**Done when:** mod/resource-pack UI authors can add visual transitions without dedicated Java rendering code.

### Stage 6 — API, performance, documentation and release quality

**Priority: continuous.**

- [ ] Consolidate and document stable APIs for widgets, templates, actions, layout factories, data sources, formatters, conditions and resource reload.
- [ ] Explore dirty/epoch-based binding invalidation for expensive providers; retain existing live bindings and event-published providers.
- [ ] Investigate partial layout recalculation for large screens and keyed lists; instance retention is already implemented.
- [ ] Improve diagnostics for unknown types/refs, missing assets and invalid actions/conditions.
- [ ] Expand automated and in-client QA for F3+T, GUI resize/scale, multiple resource packs, keyboard focus, multiplayer and client-only behavior.
- [ ] Publish public English documentation (GitBook) with copyable examples and clear compatibility guarantees.
- [ ] Run CI for each supported version/loader branch once ports are in scope.

## 4. Existing QA / regressions

- [ ] Check visual parity of all progress bars, including equivalent borders and texture styling.
- [ ] Verify background blur, popups, outlines and tooltips render in the correct order.
- [ ] Verify texture replacement, broken resource paths, reloads and multiple GUI scales.
- [ ] Verify moved keyed list widgets keep correct focus, click handlers, index-sensitive custom widgets and scroll positions in-game. Unit tests and CI already cover the underlying reconciliation.
- [ ] In-game check: toggle cases using Local UI State, verify fallback and nested child clipping, and exercise an action guard with the bundled demo.
- [ ] In-game check: `stack_layout_demo` and `advanced_layout_demo` preserve F8 overrides, reflow on state changes, handle various GUI scales and keep scroll/clipping and focus correct.
- [x] Separate test/anchor and RPG HUD toggles.
- [x] Namespaced screen commands without showing the internal `screens/` path.
- [ ] 3D model `scene4` off-screen positioning: **deferred**, not a release blocker.
- [ ] Minor text vertical alignment observation: **deferred**, not a release blocker.

## 5. Explicitly outside the framework

- Built-in dialogue engine: NPC conversation trees, branching narrative rules, dialogue history, character portraits as a dedicated system, progression and persistence. Such a mod may **use** AnchorUI's generic controls and actions.
- Standalone quest engine: objective progression, rewards, quest storage and game rules. Quest-specific screens can be composed externally using AnchorUI widgets.
- External Lore Loader; arbitrary Java/scripts in JSON; unvalidated or client-authoritative gameplay changes.
- Full character/skill systems, game mechanics and specialized 3D scene editors. These may be separate integrations later.
- Existing small demo documents may remain as UI tests; they do not expand the scope of the core framework.

## 6. Recommended next sequence

1. **In-game verification of `cases` and guarded actions**, plus the remaining collapsed-space policy decision.
2. **In-game automatic-layout QA** for rows, columns, flex, wrap, grid, editor positioning, visibility changes and scroll/clipping.
3. **Visual consistency and interaction QA** (especially progress bars, textures and blur).
4. **Screen lifecycle polish and optional state handoff**.
5. **Animations**, alongside API stability, performance work, English documentation and additional test coverage.

This is a working backlog, not a release schedule. Update completion checkboxes only when code and tests actually land.
