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

**Priority: high.** Grid placement for `template_list` already exists, but it is not a general layout engine.

- [ ] Add reusable row/column stack or flow layouts for ordinary container children, with `gap`, alignment and distribution.
- [ ] Investigate flexible size/grow, wrapping, min/max size and a general grid layout independent of `template_list`.
- [ ] Support optional collapsed layout space for hidden children without changing legacy anchored screens by default.
- [ ] Preserve explicit anchors, scroll, clipping, scaling, editor overrides and hit-testing when automatic layout is enabled.
- [ ] Add stress tests for nested layouts, resize/GUI scale, long text and mixed fixed/auto-sized children.

**Done when:** common menus, card groups and HUD sections can be assembled without manually assigning an x/y offset to every child.

### Stage 3 — Consistent appearance and input polish

**Priority: high; can proceed alongside Stage 2.**

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
2. **Row/column container layout**, with an opt-in collapsed-visibility policy.
3. **Visual consistency and interaction QA** (especially progress bars, textures and blur).
4. **Screen lifecycle polish and optional state handoff**.
5. **Animations**, alongside API stability, performance work, English documentation and additional test coverage.

This is a working backlog, not a release schedule. Update completion checkboxes only when code and tests actually land.
