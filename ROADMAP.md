# Anchor UI — Roadmap and Decision Log

> Working plan recovered from the AnchorUI project discussion, cross-checked against the `1.21.1` branch on 2026-11-10. This document records **agreed direction**, **existing functionality**, **unfinished work**, and **deferred ideas**. A proposed implementation detail is not a finalized API contract.

## 1. Project direction

Anchor UI is a reusable, JSON-driven, WoW-style anchoring framework for Minecraft interfaces, initially targeting **Minecraft 1.21.1 / NeoForge**.

The immediate use cases are:

- RPG-style in-game HUDs;
- dialogue interfaces;
- quest-related screens (quest list, details, progress, dialogue choices);
- reusable UI elements contributed by different mods and resource packs.

Character/skill-tree screens and fully custom gameplay systems may become relevant **later**, but are **not current priorities**. Anchor UI should provide UI infrastructure rather than become a standalone quest or RPG gameplay engine.

**Non-negotiable decisions from the discussion:**

1. Modpack authors can define layouts through ordinary Minecraft **resource packs**, without writing Java.
2. Resource path: `assets/<namespace>/anchorui/` (not the old `lore/` path).
3. **Do not build an External Lore Loader.** No separate root-of-game JSON loader; standard resource packs and mod resources are sufficient. The player-specific HUD layout override file under `config/anchorui/` is a separate concern and is not a content loader.
4. Keep a distinct **test/anchor HUD** and **RPG HUD**. Test overlays should be independently toggled.
5. Cross-mod extensibility, namespaced widget types/IDs, references, styles, and Java integration are central requirements.
6. Prefer efficient, event-driven data publication where practical; avoid introducing server/client tick loops just to watch UI variables.
7. Maintain server authority for gameplay changes: a resource-pack JSON cannot directly grant rewards, execute arbitrary code, or bypass permission checks.

## 2. Existing foundation (as of 2026-11-10)

The following is documented in the current repository and/or supported by the latest commits. Treat it as an **implemented baseline**, not future work:

- [x] `AnchorLayout` with nine attachment points, `point`, `anchor`, `relativeTo`, offsets, parent chains, and resize handling.
- [x] Logging/fallback for unknown references, duplicate/circular anchor checks.
- [x] JSON layouts and styles, rich/multiline text, vanilla widgets, screen rendering.
- [x] Namespaced types, widget IDs, per-screen instance registry, reusable `widgets/*.json`, `imports` and `ref` with overrides.
- [x] Public widget registration, custom JSON fields, typed Java `record` + Mojang `Codec` configuration.
- [x] In-game HUD documents from resource packs/mods in a shared anchor layout.
- [x] HUD editor (F8 / `/anchorui hud edit`): drag, anchor/parent selection, hide, reset, inspect, and locally saved overrides.
- [x] Read-only vanilla HUD references: 25 NeoForge layer IDs, positional bounds, HUD editor outlines and parent selection, no vanilla manipulation.
- [x] Context-sensitive F8 screen editor for Anchor UI JSON screens: visual dragging with snap, parent/point/anchor changes, hide/reset, scaled hitboxes, save/cancel per-screen overrides.
- [x] Separate anchor-testing and RPG HUD demos: `/anchorui test hud1` and `/anchorui test rpg`.
- [x] `anchorui:progress` (numeric binding) and resource-pack dialogue demo with `anchorui:button`.
- [x] Resource-pack screen loading and `anchorui:open_screen` / `anchorui:close_screen` actions.
- [x] Typed Data Binding: number, string, boolean, object, list; nested `path` and fallbacks; dynamic text/templates; `anchorui:data_list`; boolean `visibleWhen`.
- [x] Java data registrations via `DataValueRegistry`, including cached `registerMutable` / publish-on-change providers and backward compatibility with numeric providers.
- [x] Binding test HUD/screen and starter resource pack examples.
- [x] JSON test scenes `scene1`–`scene14`, tests/CI build setup.

**Important distinctions:** `visibleWhen` is a single boolean-bound visibility rule, **not** a general conditional expression engine. Current dialogues demonstrate screen navigation, **not** complete branching conversation/quest state. Texture/image, panel, nested containers, opt-in clipping, nine-slice and basic group scaling are implemented. Content-based container auto sizing and opt-in generic JSON scrolling are implemented; full flex/grid layout and visual-style parity remain future work.

See `README.md`, `docs/HUD.md`, `docs/DATA_BINDINGS.md`, `docs/MODPACK_AUTHORING.md`, `docs/WIDGET_REFERENCES.md`, and `docs/CUSTOM_WIDGETS.md` for implemented APIs. If a later code change contradicts this snapshot, update the checkboxes.

## 3. Next development stages

### Stage A — Visual primitives and layout composition

**Priority: high.** RPG HUD and dialogue authors need more than text, item models and progress bars.

- [x] Implement a first-class **texture/image widget** with namespaced textures, scaling, UV cropping, tint, and alpha. Better missing-resource handling is still future work.
- [x] Implement initial **panel/container** widgets, nested children, padding, content bounds, anchors, clipping, and parent-first draw order. Advanced sizing and layout policies are future work.
- [ ] Unify widget visual styling: borders/frames, backgrounds, overlays and progress-bar style consistency. Specifically guard against the prior RPG HUD issue where the second bar did not receive the same border appearance as the first.
- [ ] Verify rendering order when screens use blur, backgrounds, outlines, tooltips and widget content; avoid elements unexpectedly appearing beneath the blur.
- [ ] Keep content and widgets reusable through `style`, `ref` and imports, rather than duplicating definitions.
- [ ] Extend tests/resource-pack demos for textures, resource reload, missing assets, panel nesting, different GUI scales and consistent progress-bar styling.

**Acceptance:** A resource pack can build an illustrated RPG HUD/dialogue panel from native Anchor UI widgets without requiring a custom Java widget just to render a frame/texture.

### Stage B — General Conditions

**Priority: high, after the necessary binding primitives.**

- [ ] Add a general condition model for visibility/enabled states and conditional content. Go beyond the existing `visibleWhen` boolean binding.
- [ ] Design safe comparison/composition rules: equality, numeric comparisons, negation, ALL/ANY logic and checks against structured bound data. Exact JSON syntax is **TBD**, not a committed schema.
- [ ] Decide whether conditions should also control screen transitions/choices and whether conditional branches reserve their layout space (current `visibleWhen` does).
- [ ] Respect missing-value fallbacks; detect malformed conditions with useful logs and isolate errors.
- [ ] Reevaluate expensive conditions only when required; no JSON reparsing every frame.

**Acceptance:** A dialogue choice or quest detail can be enabled/hidden based on safe, declared client-side state and a mod-provided value.

### Stage C — UI State and richer Screen Actions

**Priority: high for dialogues, after/alongside Conditions.**

- [ ] Provide **UI State** for local screen/dialogue variables (e.g., selected option, current page, temporary choice flags).
- [ ] Define state lifetime: per-widget, per-screen, and optionally across screen navigation. Specify reset/initialization behavior.
- [ ] Expand the explicit action registry beyond `open_screen` and `close_screen` to support safe, declarative state updates and dialogue flow. Names and JSON schema are **TBD**.
- [ ] Clarify focus, keyboard navigation, action enablement and errors when transitioning between screens.
- [ ] Keep actions from resource packs sandboxed. Any action that changes quests, inventories, currency, permissions or other gameplay state must go through a server-authorized integration supplied by another mod.
- [ ] Add tests for navigation loops, invalid actions, missing screens, state initialization/reset, and resource reloads.

**Acceptance:** A multipage dialogue can react to local choices without custom code for every page, while protected gameplay actions remain server controlled.

### Stage D — Dialogue and quest UI building blocks

**Priority: high product use case; build on A–C.**

- [ ] Turn the simple two-page demo into useful **branching dialogues** with conditional choices and reusable presentation templates.
- [ ] Supply quest-UI components/patterns: quest list, quest detail, objective progress, available/completed states, tracker and narrative descriptions.
- [ ] Bind those screens to structured data exposed by a quest/gameplay mod; add an example of integration with external quest providers.
- [ ] Define opt-in callbacks/actions for starting, advancing, accepting or turning in quests **only through server-validated integrations**.
- [ ] Cover empty lists, large lists, selection, scrolling, localization, tooltips and controller/keyboard focus where relevant.
- [ ] Keep this a **UI framework**: quest persistence, progression rules and reward logic are not automatically part of Anchor UI.

**Acceptance:** A modpack can present quest data and dialogues via resource-pack JSON while the actual quest engine remains independent.

### Stage E — Animations and polish

**Priority: medium; after stable state, conditions and visual primitives.**

- [ ] Add declarative animations/transitions for showing/hiding screens or widgets, e.g. fade, slide and interpolation where appropriate.
- [ ] Define when transitions trigger: state changes, screen opening/closing and data changes.
- [ ] Ensure animation does not destabilize anchors, hitboxes, clipping, focus or HUD editor overrides.
- [ ] Avoid per-tick global scans or allocating/rebuilding widget trees on every frame.
- [ ] Add animation examples and performance tests.

**Acceptance:** HUD/dialogue transitions feel polished without forcing each mod to write bespoke rendering logic.

### Stage F — API, performance, documentation, release quality

**Priority: ongoing.**

- [ ] Consolidate a documented, stable API for custom widgets, typed configuration, data sources, actions, resource reload and screen/HUD integration.
- [ ] Revisit Data Binding coverage so new built-in widgets can read typed values consistently. Existing structured bindings and mutable PUSH providers must remain supported.
- [ ] Keep heavy/externally synchronized values event-published when possible; make caching, invalidation and redraw behavior explicit. Rendering may read the latest cached value, but `registerMutable` does not run a tick listener by itself.
- [ ] Improve authoring diagnostics: unknown widget/type/ref, broken texture, malformed style/binding, invalid condition/action.
- [ ] Expand automated tests and test scenes, including independent demo toggles, GUI resize, F3+T resource reload, multiplayer/client-only behavior, and accessibility/keyboard handling.
- [ ] Prepare public **English** documentation (GitBook was discussed) and maintain ready-to-copy resource-pack examples.
- [ ] Check build/CI for every supported branch when ports begin; the current roadmap's implementation status concerns `1.21.1` only.

## 4. Regressions / known discussion notes to preserve

These were discussed during testing, and should remain in the QA backlog until explicitly verified:

- [ ] Verify that `anchorui:progress` bars share equivalent styling (including borders), not merely identical numeric dimensions.
- [ ] Verify blurred backgrounds, overlays, outlines and other layers render in the intended order.
- [x] Keep `/anchorui test hud1` independent of `/anchorui test rpg` (current documentation describes independent toggles).
- [x] Open namespaced screens via `/anchorui screen open namespace:name`; command parsing must accept the colon and hide internal `screens/` path details (documented in current README).
- [ ] Revisit the 3D model test from `scene4` only if that feature becomes relevant. The discussion explicitly **deferred** its off-screen/over-sensitive movement issue; do not treat it as a current blocker.
- [ ] Consider the earlier observation that text appeared slightly raised relative to expected alignment; **deferred**, not current priority.

## 5. Deferred / excluded scope

- **Excluded:** External Lore Loader, direct game-root content directory, arbitrary JSON-side Java/script execution, unapproved server mutations.
- **Deferred:** character profile and skill-tree screens, full RPG/unique gameplay systems, standalone quest engine, broad 3D widget controls.
- **Separate configuration:** player HUD positioning is persisted to `config/anchorui/hud-layout.json`, and screen-editor overrides are persisted per resource under `config/anchorui/screens/`; neither is an authored-content loader.
- **Compatibility policy:** preserve existing JSON and Java integration APIs when adding new features where feasible; document intentional breaking changes.

## 6. Suggested order of work

1. **Texture widget + style fidelity + container/panel composition** — unlocks production-quality RPG HUD and dialogue layouts.
2. **General Conditions** — turns static, bound screens into conditional interfaces.
3. **UI State + safe action expansion** — enables real dialogue choices and local UI flows.
4. **Dialogue/quest UI components** — practical end-user features on the stable primitives.
5. **Animations and polish**, alongside tests, API stabilization and English documentation.

This is a working backlog, not a promise of exact release versions or dates. Update completion state when code and tests actually land.
