# Java extension API audit — AnchorUI 1.21.1

**Status:** development API, before the first release. This page describes
the common Java interfaces in `1.21.1` (NeoForge) and `1.21.1-fabric` (Fabric); it is not a promise of
binary compatibility. The framework remains a client UI library. Gameplay
state changes must be validated by the server in the integrating mod.

For the integration workflow, defaults and built-in feature index, start with [Integration reference](INTEGRATION_GUIDE.md).

## Registration lifecycle

Register Java extensions **once on the client during mod startup**, ideally
inside `FMLClientSetupEvent.enqueueWork` on NeoForge or `ClientModInitializer.onInitializeClient` on Fabric, before any screen or HUD using
their IDs is constructed. Do not register again on F3+T/resource reload,
world join or screen open: duplicate registrations are rejected.

Use a full `ResourceLocation` with your mod namespace, such as
`mymod:mana`. JSON references to actions, data sources, formatters,
condition operators and custom animation triggers require explicit
namespaces. `UiIdentifiers.requireNamespaced(value, description)` validates
this when integrations need to parse user-supplied strings.

| Extension | Java API | JSON integration | Runtime contract |
| --- | --- | --- | --- |
| Widget type | `AnchorWidgetRegistry.register(id, codec, factory)` | `type: "mymod:badge"` | A factory creates an `AbstractWidget`. Configuration is decoded at build time |
| F8 fields | `WidgetEditorRegistry.register(type, fields)` | Saved client appearance overrides | Explicit allowlist of editable paths; call separately from widget factory registration |
| Template | `WidgetTemplateRegistry.register(id, definitionOrSupplier)` | `ref`, `imports` or `template_list` | Supplier returns a validated JSON template; compiled/reused when needed, not every rendered frame |
| Template list layout | `TemplateListLayoutRegistry.register(id, codec, factory)` | `layout.type` in `anchorui:template_list` | A factory returns a cell-placement function; errors include the layout ID and item index |
| Action/event | `UiActionRegistry.registerEvent(id, codec, factory)` | `onClick`, `onOpen`, `onChange`, etc. | Compile an event handler once per UI instance; event payload available in `UiEvent` |
| Data source | `DataValueRegistry.registerNumber/String/Boolean/Object/List` | `{"bind":"mymod:mana"}` | Read client-side information when the consuming widget evaluates it |
| Published value | `DataValueRegistry.registerMutable` | Same as above | Push updated snapshots from client packet/state callbacks; advances revision only when published JSON changes |
| Revision-aware data source | `DataValueRegistry.registerRevisioned(id, revision, provider)` | Same `bind` syntax | Cache expensive source data in each compiled binding/condition until context or revision changes; ordinary providers stay live |
| Formatter | `DataFormatterRegistry.register(id, codec, formatter)` | `format: {"type":"mymod:formatter",...}` | Decode typed options once; reserved `type` is removed before the codec sees options |
| Condition operation | `ConditionOperationRegistry.register/ registerUnary` | `op: "mymod:has_rank"` | Pure boolean client predicate over already resolved JSON operands |
| Animation trigger | `UiAnimationTriggerRegistry.register/ registerCondition` | `trigger: "mymod:quest_completed"` | Poll the registered revision/condition only for widgets with that custom track |
| Visual transition | Built-in `animations` and `sizeTransition` | Widget JSON and F8 | No Java registration necessary; composed after layout calculation |

Further reading: [Custom widgets](CUSTOM_WIDGETS.md),
[Events and actions](UI_EVENTS.md), [Data bindings](DATA_BINDINGS.md),
[Formatters](FORMATTERS.md), [Conditions](CONDITIONS.md),
[Template lists](TEMPLATE_LIST.md) and [Animations](ANIMATIONS.md).

## Typed registration examples

### Widget plus editor fields

```java
ResourceLocation type = ResourceLocation.fromNamespaceAndPath("mymod", "badge");
AnchorWidgetRegistry.register(type, BadgeConfig.CODEC, (context, element, style, config) -> new MyBadgeWidget(context.font(), config));
WidgetEditorRegistry.register(type, List.of(WidgetEditorRegistry.Field.text("caption", "Caption", "Badge"), WidgetEditorRegistry.Field.color("fillColor", "Fill", "#FF334455")));
```

The widget configuration `Codec` validates widget-specific properties.
The F8 fields define a separate explicit allowlist. They do **not** register
a codec or implicitly make all JSON paths editable.

`WidgetContext` provides `minecraft()`, `widgets()`, `font()`,
`itemContext()`, and more. For HUD construction, `screen()` is **null**;
do not assume a regular `Screen` always exists.

### Configured action

```java
UiActionRegistry.registerEvent(ResourceLocation.fromNamespaceAndPath("mymod", "show_badge"), BadgeActionConfig.CODEC, (context, config) -> event -> MyClientUi.openBadge(config.badgeId(), event.targetId()));
```

Example resource JSON:

```json
{
  "onClick": {
    "type": "mymod:show_badge",
    "badgeId": "mymod:trainer"
  }
}
```

`Codec<T>` receives only action arguments, **not** the reserved `type` or
`when` fields. Register with `registerEvent` to use the `UiEvent` payload,
or `register` for a plain `Runnable`.

### Published data instead of per-frame calculation

```java
DataValueRegistry.MutableValue mana = DataValueRegistry.registerMutable(ResourceLocation.fromNamespaceAndPath("mymod", "player.mana"), new JsonPrimitive(0));
```

When your own client packet updates mana, call `mana.set(new JsonPrimitive(newMana))`.
The mutable source is revisioned: each compiled binding checks a cheap
revision and reads the published JSON snapshot only when it changes.
No server tick loop or global widget-polling timer is added by this API.

For expensive calculated sources, register separate revision and read
functions:

```java
AtomicLong statsEpoch = new AtomicLong();
DataValueRegistry.registerRevisioned(ResourceLocation.fromNamespaceAndPath("mymod", "player.stats"), context -> statsEpoch.get(), context -> buildStatsJson(context.minecraft().player));
```

The integrating mod increments `statsEpoch` when the corresponding data
changes. Ordinary providers with no revision remain live, and
`DataValueRegistry.RevisionedProvider` can be implemented directly.
See [Data bindings](DATA_BINDINGS.md) for the cache's context, fallback
and thread-safety contracts.

`NumericDataRegistry` remains a legacy numeric adapter over
`DataValueRegistry`; for new integrations use the typed registry directly.

### Java-owned custom animation trigger

```java
UiAnimationTriggerRegistry.register(ResourceLocation.fromNamespaceAndPath("mymod", "quest_completed"), context -> MyClientEvents.completionCounter());
```

JSON:

```json
{
  "animations": [
    {
      "id": "quest_pulse",
      "trigger": "mymod:quest_completed",
      "preset": "zoom",
      "duration": 300,
      "delay": 100
    }
  ]
}
```

Increasing the revision retriggers the track. Alternatively use
`registerCondition(id, context -> boolean)` to trigger on false-to-true
transitions. The initial observed value is a baseline, not an automatic
event. A missing registration is reported and can be supplied later;
an exception from another mod's trigger is reported once and isolated
from the rest of the UI.

## Resource reload snapshots

`LoreLoader.INSTANCE.getRevision()` increments when a new client resource snapshot is published. The related read-only accessors `getData()`, `getStyles()`, `getDefinitions()` and `getHudDocuments()` expose the currently loaded documents. The new `getResourceIssues()` accessor returns immutable issue records with severity, resource ID, message and optional cause. These can be used by custom development tools; built-in F8 inspectors already filter them by screen/HUD relevance.

Resource-pack JSON definitions belong to the reload lifecycle, but the global Java extension registries **do not**: register factories once during client startup, never inside reload callbacks. Valid `templates/` definitions override corresponding `widgets/` definitions, while malformed templates cannot replace working widget definitions. See [Resource reload and diagnostics](RESOURCE_RELOAD.md) for failure boundaries, in-client F3+T tests and limitations of the Minecraft JSON parser.

## Validations and error boundaries

- Every Java registry rejects duplicate IDs with `IllegalStateException`. Registering twice on resource reload is a mod integration error. The animation-trigger and F8 editor-field registries now follow the same duplicate contract as widget types, templates, data sources, formatters, actions and layout factories.
- Null required registration IDs, factories, callbacks, fields and similar inputs raise `NullPointerException` at registration time. Validation of malformed JSON configuration still raises `IllegalArgumentException`; runtime provider/factory errors are handled at their documented isolation boundary.
- A malformed or unknown namespaced action/formatter/operator is rejected
  during JSON compilation; codecs receive their typed argument data,
  never arbitrary code from JSON.
- Widget factory failures or invalid codec settings are isolated to the
  affected widget. A failed action event is logged without crashing the
  entire event dispatcher.
- Runtime data-binding and formatter failures use the binding fallback
  value. Animation trigger callbacks are also isolated so one third-party
  exception does not stop the HUD/screen render.
- F8 screen/HUD inspectors expose widget/layout build diagnostics in a collapsible **DIAGNOSTICS** category. The full details are also logged; the inspector displays a bounded set of deduplicated warnings, and editor layout failures report the validator's reason without saving an invalid change.
- Resource packs are **not trusted server commands**. UI visibility,
  conditions and client-side actions must not grant items, permissions or
  affect authoritative gameplay state without server validation.

## Remaining Stage 6 work

This is the **initial API audit**, not the final stability review. Before
a release we still need to assess high-frequency provider caching/epoch
invalidation, profile large nested layouts, complete in-client resource-pack reload QA, verify GUI scale/focus/popup layering in game and publish
the full English GitBook reference. No ABI stability guarantee has been
issued for `1.21.1`.
