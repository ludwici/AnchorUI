# AnchorUI — Roadmap and Decision Log

> Status reviewed against the `1.21.1` NeoForge branch on **2026-11-15**. This is the roadmap for the **UI framework**, not for games, quests, NPC conversations, or other systems built on top of it. Checked items describe functionality already present; unchecked items remain work or QA.

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
- [x] Support any number of constraints per widget with `position.anchors`; opposing points derive dynamic width/height and override `size.width/height` and corresponding `autoSize` axes. Resolve multiple targets, validate overconstraints, retain dependency-aware caching, preserve F8 offsets and integrate nested padding, row/column/grid and template lists. See [Multiple anchors](docs/MULTI_ANCHORS.md).
- [x] Expose every effective anchor constraint in a collapsible `ANCHORS (N)` section of both F8 inspectors, with primary marker, target IDs and offsets. Keep secondary anchors intact when editing the primary or dragging a widget.
- [x] Make `ANCHORS` a full editor in HUD and screen F8: add/duplicate and delete constraints, independently edit Point/Anchor/Relative to and X/Y via dropdowns or numeric inputs, preserve drag offsets, reject invalid intermediate geometry, and save the complete list to per-widget JSON overrides. Remove old Point/Anchor toolbar buttons.
- [x] Persist HUD and screen F8 overrides as formatted multi-line JSON with two-space indentation and round-trip compatibility for compact legacy files; preserve the existing atomic-save behavior.
- [x] Normalize visual/layout/animation/anchor edits back to resource values, clear redundant overrides and restore correct modified highlights and unsaved-state detection in both F8 inspectors.
- [x] Confirm unsaved F8 editor exits from Esc, F8 and Cancel with three actions: discard and exit, save and exit, or stay editing. Keep the preview live beneath the modal; Save/Ctrl+S remain explicit save shortcuts.
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

**Status: all Stage 1 framework features and the hidden-space policy are implemented and covered by automated tests; in-client checks remain under QA.** Extend the existing Conditions engine, not a domain-specific conversation or quest system.

- [x] Implement `anchorui:cases` for first-match widget/template selection, including a fallback and compiled Conditions.
- [x] Add `when` action-level guards evaluated at event time; invalid expressions fail validation and unavailable data fails closed.
- [x] Keep both features compatible with existing `visibleWhen` / `enabledWhen`, UI State, named templates and resource packs.
- [x] Add unit tests for nested widgets, template rows, first-match selection, missing/malformed conditions and live guard updates. In-game QA is still pending.
- [x] Define and test the opt-in hidden-space policy: hidden direct children reserve their layout slot by default, while `layout.collapseHidden: true` removes them from automatic row/column/grid flow, auto-sized content measurements and scroll overflow. Existing manual/anchored layouts keep their geometry; local UI State, `visibleWhen`, `cases` and editor hide/show trigger the same policy.

**Done when:** a JSON UI can select content and safely gate a generic action without implementing its own widget or scripting logic.

### Stage 2 — General-purpose automatic layouts

**Status: the planned row/column/grid, flex, wrapping and hidden-child policies are implemented. Additional integration tests cover responsive geometry, saved F8 layout changes and collapsed-child scroll limits; actual in-client QA still remains.** The older `template_list` layout engine is independent.

- [x] Add opt-in `row` and `column` layouts for normal panels/containers with `gap`, cross-axis `align`, main-axis `justify` and `space_between`.
- [x] Support `align: stretch` along a fixed cross axis, with preferred sizes retained for auto-sized cross axes.
- [x] Support opt-in `collapseHidden` with condition-triggered relayout, including local state and hidden `cases` alternatives.
- [x] Preserve manual anchors and F8 local offsets: children with external/non-top-left anchors or `layoutItem: false` opt out of the stack. Layout bounds integrate with scrolling, clipping and scaling.
- [x] Add automated tests for nested auto-sized containers, column/row alignment, distribution, padding, hidden rows and explicit anchors, plus a resource-pack demo.
- [x] Add explicit per-child `layoutItem.grow` / `shrink` weights and min/max width/height constraints, leaving weights at zero unless configured.
- [x] Add optional row/column `wrap` and `lineGap`, with per-line distribution and natural cross-axis auto-sizing.
- [x] Add independent panel/container `grid` with columns, configurable gaps/cell sizes, per-cell align/stretch and hidden-cell reflow.
- [x] Make every direct structural Grid child automatically managed by its parent settings, regardless of legacy single child anchors or `layoutItem.enabled`; simplify F8 Parent to direct container insertion and hide generated internal context fields. Explicit **multiple** anchors now opt a child out of Grid management so its authored constraints can stretch it instead.
- [x] Treat `anchorui:panel` as a default parent without additional authored container/context nodes; keep nested hierarchy outside per-widget JSON, use Panel IDs for child anchors and apply padding in layout resolution. Do not accept removed `/__content` anchors; Panel padding and viewport geometry are computed directly.
- [x] Preserve Screen Editor inspector scroll when changing values via `+`/`-` with SOURCE JSON collapsed; show one parent label instead of duplicated internal parent/anchor details.
- [x] Add focused geometry tests for constrained flex, wrapping in both axes, grid auto sizing, cell alignment and hidden cells.
- [x] Scope decision: CSS-like features (grid spans, named tracks and fine-grained incremental layout) remain **deferred extensions**. Multi-anchor responsive sizing was subsequently implemented for the concrete stretch use case; it does not introduce CSS Grid track/span syntax.
- [x] Update HUD and screen F8 editors to inspect and edit row/column/grid/flex layout fields using persisted per-widget overrides, and browse nested widget structure in a collapsible hierarchy.
- [x] Distinguish coordinate anchor targets from structural container parents in both F8 editors; support container/content/move picker, move-to-root, cycle-safe saved overrides and default-collapsed read-only source JSON.
- [x] Expand automated Stage 1/2 regression coverage for reserve-vs-collapse semantics, row/grid conditional reflow, autoSize with hidden manual children, F8 override Save/Cancel, widget ID retention, resize/wrapping and scroll clamping; add a dedicated visual comparison demo.
- [ ] Complete **in-client** runtime QA for editor dragging, layout-field cycling, Save/Cancel, GUI resize and scale, nested scrolling, moving focus and resource reload. See [Stage 1/2 QA](docs/QA_STAGE_1_2.md). This requires launching Minecraft and cannot be certified by Gradle tests alone.

**Acceptance:** row/column/grid and hidden-space behavior have automated coverage and a repeatable in-game acceptance checklist. In-client focus, scale, editor and reload verification remains outstanding, so Stage 2 is not yet fully QA-signed-off.

### Stage 3 — Consistent appearance and input polish

**Status: main keyboard, rendering, color/style diagnostics and resource-reload fixes implemented; in-game verification and remaining accessibility/blur QA are still required.**

- [x] Add a component-aware **VISUAL / CONTENT** inspector to HUD and screen F8 editors: editable text, basic colors, texture resource paths, sizes, basic widget styles and typed controls.
- [x] Register custom editable widget fields through an explicit Java `WidgetEditorRegistry`; persist appearance overrides separately from resource packs and validate edits.
- [x] Rebuild edited widget visuals on explicit changes, preserving screen Local UI State, with rollback for invalid widget configurations.
- [x] Add per-choice Select styles with color/texture state precedence over shared row skins.
- [x] Add `progress` track/fill/frame texture parts and per-widget text/color/frame overrides, and make Panel and Progress visual settings inherit from shared style resources.
- [x] Share both RPG HUD bar backgrounds, padding and border styling through `anchorui:style/rpg_progress`, with focused geometry/style tests (in-game rendering QA remains).
- [x] Replace Boolean `-/+` inspector steppers with real checkbox controls in both F8 editors (including Java-registered Boolean fields).
- [x] Add explicit `focused` / `hoverFocused` button/control texture variants, visible keyboard-focus fallback outlines, and one-open-dropdown popup layering.
- [x] Share appearance inheritance between progress bars, panels, skinned buttons and state controls; use one HEX parser for background/text/skin colors, respecting `#AARRGGBB` text alpha. Style merging and variant priorities are covered by unit tests.
- [x] Validate equal RPG progress bar dimensions, frame, padding and border inheritance in a dedicated regression test; in-game rendering parity still needs confirmation.
- [x] Render built-in rich text Tooltip after floating Select popups and outside child scissors; suppress tooltips from text hidden underneath an open popup. The popup remains above ordinary widgets and scroll indicators.
- [x] Warn once per reload about missing texture files and missing style references, with expected resource paths; include widget ID/type in configuration errors and skip malformed root documents instead of crashing reload.
- [x] Detect resource-pack revisions triggered while a screen instance remains open and rebase compatible Local UI State keys; never override vanilla F3+T handling in open GUI screens. Reopen a screen after reloading resources in the world to inspect updated resource-pack content.
- [x] Implement Tab/Shift+Tab traversal that skips hidden, inactive, clipped and offscreen widgets, while allowing nested controls to consume Tab internally; preserve previously implemented focused/hoverFocused textures and focus outlines.
- [x] Add `stage3_compact_demo`, an input/skin/pop-up small-scale test screen, and explicit RPG progress parity resource tests.
- [ ] In-game QA for appearance editor, scaled/clipped keyboard and mouse hitboxes, Select popup hover occlusion, Color Picker/text modal focus, resource reload from the world, nested tooltips and custom Java widget codecs. See [Stage 3 QA](docs/QA_STAGE_3.md).
- [ ] Verify the complete screen background/blur/outline/tooltip order in Minecraft (including modded overlays and small GUI scales); unit tests cannot certify GPU rendering order.
- [ ] Finish accessibility/narration and custom widget tab-order review after in-client checks; current focus traversal covers built-in and registered active widgets, but screen reader behavior is not fully verified.

**Done when:** shared style resources give consistent results across built-in widgets, with stable rendering and input behavior.

### Stage 4 — State, lifecycle and screen navigation refinements

**Status: completed and confirmed in Minecraft.** Local UI State, focus restoration and screen navigation are implemented and tested. Vanilla F3+T remains unchanged; resource-pack reload regression checks are tracked separately.

- [x] Add explicit, optional `open_screen.stateHandoff` mapping selected source UI State keys into typed destination keys; preserve existing default behavior without a mapping and avoid global shared-state storage.
- [x] Preserve keyboard focus by namespaced widget ID through screen re-entry and widget rebuilding, including F8 and keyed lists; reject missing navigation targets and invalid transfers without replacing the current screen.
- [x] Document normal navigation vs F8 lifecycle events and Local UI State lifetime; display navigation errors to the player and log them.
- [x] Add two-screen navigation demo plus unit/resource tests for selective transfer, defaults, invalid mappings and missing targets.
- [x] In-game verification of Stage 4 screen navigation, state handoff/defaults, focus and F8 round-trips confirmed by the user. Resource reload is tested from the world using vanilla F3+T and remains a separate cross-stage regression check.
- [x] Scope decision: additional shared/persistent State Scopes are deferred until a concrete cross-screen use case justifies expanding the public API.

**Done when:** independent screens can compose safely with predictable state lifetime and focus restoration.

### Stage 5 — Declarative animation primitives

**Status: implementation available for screens and HUD; final in-client QA remains.**

- [x] Implement generic fade, slide, opacity and scale interpolation on screen and HUD widgets without changing anchored layout coordinates.
- [x] Support any number of named concurrent animation tracks (including multiple of the same trigger), independent change watches, Java-registered namespaced custom triggers, per-track duration, **delay (ms)** and easing.
- [x] Add generic bound visual width/height transitions for textured or colored widgets without per-frame relayout; apply the same interpolator to RPG health/food fill widths while keeping bar frames/text fixed.
- [x] Map pointer hitboxes, hover, click, release, drag and wheel input through animated transforms; preserve clipping and stable static F8 preview.
- [x] Provide dynamic collapsible track editing in both F8 inspectors, including add, duplicate, reorder, delete, trigger, preset, custom frames and delay. Persist complete track arrays with Save/Cancel.
- [x] Avoid global tick handlers and per-frame widget-tree recreation. Update only configured animated screen widgets; for HUD widgets piggyback on existing HUD rendering.
- [ ] Finish in-game testing of nested scrolling, clipped animated children, popups, GUI scales, editor overrides and high-widget-count performance; see [Stage 5 QA](docs/QA_STAGE_5.md).

**Done when:** resource-pack authors can create general visual transitions without dedicated Java rendering code, and in-client animation regression checks have passed.

### Stage 6 — API, performance, documentation and release quality

**Priority: continuous.**

- [x] Audit the existing Java registries and implement first API consistency fixes: shared namespaced ID validation, typed formatter argument normalization, and fault-isolated custom animation triggers with regression tests. See [Java extension API](docs/JAVA_EXTENSION_API.md).
- [ ] Consolidate and document stable APIs for widgets, templates, actions, layout factories, data sources, formatters, conditions and resource reload. Initial registry consistency pass now aligns animation-trigger and F8 editor duplicate/null exception contracts with the other Java registries; public ABI stability is not yet promised.
- [x] Add opt-in revision-aware source caching for expensive providers in `BoundData` and `BoundCondition`, including atomic `registerMutable` revisions, context invalidation, live-provider compatibility, and regression tests. See [Data bindings](docs/DATA_BINDINGS.md).
- [ ] Profile revision-aware providers in large screens, nested lists and multi-widget HUDs; consider shared invalidation only if measured results justify it.
- [x] Cache independent built-in row/column/grid arrangements per screen, reusing unchanged child order, sizes, layout rules and available space across editor, condition and keyed-list relayouts. Custom Java layouts remain live; automated cache invalidation tests cover these boundaries.
- [x] Add dependency-aware per-screen anchor snapshots: unchanged frame definitions and target bounds reuse their resolved geometry, while resize, reparenting, removed frames and cycles are handled correctly. Covered by independent unit tests.
- [ ] Investigate partial transform/clipping/scroll resolution for very large screens and keyed lists; anchor dependency checks still traverse the graph, while the retained full path preserves correct validation and diagnostics.
- [x] Improve early JSON diagnostics for missing template imports/references (including resource paths and nested/case locations), malformed entity/child entries and missing widget types; action compilation now identifies the failing event field. Add regression tests.
- [x] Surface runtime screen/HUD build and layout diagnostics in a collapsible F8 inspector section with deduplicated warnings, bounded display, counts and preserved details in the log. Show specific rejected anchor/layout reasons instead of generic errors; cover diagnostics formatting in tests.
- [x] Isolate malformed resource-pack entries with a validated snapshot compiler, preserve valid widgets when an invalid template would override them, report file-specific load failures, and publish screen/HUD/style/template maps atomically on F3+T. Show relevant reload issues in both F8 inspectors. See [Resource reload and diagnostics](docs/RESOURCE_RELOAD.md).
- [ ] Continue diagnostics review for runtime missing textures/styles and condition errors; validate F3+T behavior in a live Minecraft client. Automated resource compiler and inspector regression tests do not replace client integration QA.
- [x] Add automated isolation tests for invalid resource roots, styles, templates, HUDs, screens, missing optional imports and repeated snapshots; validate scoped F8 reload issue display and run them in CI.
- [ ] Complete in-client QA for F3+T, GUI resize/scale, multiple resource packs, keyboard focus, multiplayer and client-only behavior. Use the [resource reload checklist](docs/RESOURCE_RELOAD.md).
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
4. **Screen lifecycle and optional state handoff** — completed and confirmed in Minecraft; optional wider state scopes deferred.
5. **Animations**, alongside API stability, performance work, English documentation and additional test coverage.

This is a working backlog, not a release schedule. Update completion checkboxes only when code and tests actually land.
