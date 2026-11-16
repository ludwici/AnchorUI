# Performance notes (NeoForge 1.21.1)

Anchor UI is designed for ordinary resource-pack screens, reactive client-side HUDs, and temporary editor overlays. The optimizations below reduce repeated work without changing JSON document syntax or turning data binding into a server-tick polling loop.

## Render and layout lifecycle

| Component | What is done per frame | What is cached or recalculated only when needed |
| --- | --- | --- |
| Vanilla HUD references | Collect a small snapshot of the current player HUD state | All 25 reference rectangles and their frames are generated only when the snapshot or GUI dimensions change. Vanilla visibility-only transitions do not force a layout pass. |
| HUD and screen hierarchy | Re-evaluate live `visibleWhen` conditions | Reuse temporary visibility lookups rather than allocating a new map per frame. Anchor layout, nested transforms and clipping are recalculated on layout/resource changes, not every normal render. |
| Rectangular panels | Draw a fill and up to four border strips | No row-by-row fill calls; respects semitransparent fill and border blending. |
| Rounded panels | Draw vertical bands containing neighboring rows of identical geometry | Rounded outline calculations and band grouping are cached until the panel size changes. |
| 9-slice textures | Draw up to nine pieces | UV tile descriptions are cached until the destination dimensions change. Texture image data itself remains managed by Minecraft. |
| Widget lists in F8 | Compare the current entry snapshot, draw only rows in the visible scroll viewport | Namespace sorting and section construction are reused until the list or visibility changes. Click-to-select uses constant-time row lookup. |
| Revision-aware bindings | Read a lightweight revision number when a revisioned provider is referenced | Compiled bindings and conditions reuse defensive JSON snapshots until the revision or context changes; unversioned providers remain live. |
| Automatic row/column/grid layout | No arrangement pass during normal rendering | Each screen reuses its last built-in arrangement per container when available width/height, child order, intrinsic sizes, rules, collapse state and layout configuration are unchanged. Custom Java layout implementations are always recalculated. |
| HUD and screen editors | Build one handle snapshot per editor frame | Reuse that snapshot across drawing, hit tests and list contents, instead of repeatedly assembling handles. |

The renderer **does not cache actual dynamic values indefinitely**. Text, progress values, collections and visibility bindings still read their providers in the normal rendering lifecycle so the UI responds to changed gameplay state. Existing bound text and list widgets already avoid rebuilding formatted lines while their resolved content and width remain equal.

## When layouts are recalculated

- A resource pack reload or a HUD test group toggle rebuilds its affected widget tree.
- GUI scale / window dimension changes change anchor and vanilla reference geometry.
- The screen/HUD editor applies layout changes when the user moves an element or updates its anchors or parent.
- Changes to the number of player-heart rows or the number of visible effect-icon rows update the vanilla positional references.
- The list inspector is transient and does not modify the in-game HUD update frequency.
- Screen relayout reuses unchanged built-in container arrangements **and** resolved anchor bounds. Each anchor entry is keyed by the frame definition and its resolved target bounds, so editing or moving a parent invalidates dependent widgets without invalidating unrelated frames. The complete anchor graph is still traversed to detect bad references and cycles. Transforms, clipping and scrolling retain their existing full-resolution path. Both caches are private to the screen instance, prune removed IDs, and need no global cache or tick handler.

## Profiling in the Minecraft client

A successful Gradle build verifies correctness of compilation and tests, **not real-world FPS improvements**. To assess performance, compare consistent in-game scenes and GUI scales before/after a release. Useful experiments:

1. Normal gameplay with the vanilla HUD, then gameplay with the custom RPG HUD enabled.
2. A screen containing many progress bars and dynamically bound text fields.
3. An image-heavy screen with dozens of 9-slice/rounded panels, testing both static and frequently resized elements.
4. F8 editor with hundreds of loaded widget IDs, scrolling through the list.
5. Changing GUI Scale, entering/leaving water and mounts, adding/removing status effects, and reloading resource packs.

Record per-frame CPU usage and allocation rate with a client profiler (for example Java Flight Recorder) and inspect graphics performance in the game. No measured FPS percentage is claimed here because no controlled Minecraft runtime benchmark has been performed.

## Future opportunities

- Further profiling of revision-aware providers with large, frequently updated objects and multi-widget usage. The implemented `RevisionedProvider` API is opt-in and caches **per binding**, not globally; expensive providers still need cheap revision checks and explicit invalidation from their owning mod.
- Dirty-rectangle / invalidation-aware screen rendering for very large mostly-static UI trees.
- Deeper incremental transform, clipping and scroll-limit resolution during editor dragging when many unrelated widgets exist. Container arrangements and anchor bounds are already cached, but dependency validation still traverses the frame graph.
- A client-side integration test fixture for scaled hit targets, nested clipping, resource reloads and UI render ordering.

The existing runtime contains no additional server tick loop for these optimizations.

## Shared scrolling implementation

Editor scrolling (HUD Inspector, Screen Inspector, Widgets list) and generic JSON vertical viewport scrolling share the same `NativeContainerScrollPanel` subclass of NeoForge's `ScrollPanel`. The implementation owns scroll-wheel delta handling, drag capture, scrollbar rendering and maximum scroll limits. The existing editors retain their custom content rendering, row virtualization, clipping and independent offsets; no duplicate thumb-position calculations or homemade drag formulas remain in those editors. Different visual scrollbar widths/colors are passed as style settings to the shared native adapter. The horizontal axis in a generic JSON `scroll: both` container remains a separate wheel-only path because NeoForge 1.21.1 `ScrollPanel` is vertical-only.
