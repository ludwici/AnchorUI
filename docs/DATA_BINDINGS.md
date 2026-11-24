# Typed Data Binding

Anchor UI lets modpack authors display changing Minecraft values in HUDs and screens using **JSON resource packs**, without writing a Java mod. All UI documents belong under `assets/<namespace>/anchorui/`.

Bindings are registered by Java mods under namespaced IDs. Resource-pack authors refer to those IDs using `bind`. A missing or invalid value uses the optional `fallback`. If omitted, the property uses its own typed default (for example an empty string for a text binding, or the property's numeric default for a number). The provider ID must still be registered.

## Available value types

| Type | Supported uses |
| --- | --- |
| Number | `anchorui:progress.value`, `max`; third-party widgets via `BoundData` |
| String | `anchorui:text.text`, including interpolation templates |
| Boolean | `visibleWhen` and `enabledWhen` for any HUD or screen widget, including composite [Conditions](CONDITIONS.md) |
| Object | Structured source with `path`, e.g. `health` on `player.profile` |
| List | `anchorui:data_list.items` for text, or `anchorui:template_list.items` for interactive row templates |

For buttons and nested widgets generated from a list binding, see [Template List and Item Context](TEMPLATE_LIST.md). Item fields can drive per-row text, Conditions and action arguments.\n\nThe previously published `NumericDataRegistry` API remains supported and shares its registrations with the new typed registry.

### Plain numeric values and nested fields

```json
{
  "type": "anchorui:progress",
  "id": "mypack:health",
  "value": {
    "bind": "anchorui:player.profile",
    "path": "health",
    "fallback": 0
  },
  "max": {
    "bind": "anchorui:player.profile",
    "path": "maxHealth",
    "fallback": 20
  },
  "fillColor": "#CB5A65",
  "showText": true,
  "position": {"relativeTo": "screen", "point": "TOPLEFT", "anchor": "TOPLEFT", "x": 20, "y": 30},
  "size": {"width": 200, "height": 16}
}
```

`path` may access nested object fields and array positions, e.g. `"stats.mana"` or `"quests[0].title"`. Paths only read JSON values; they are not Java expressions or script code. If the field does not exist or has the wrong type, the binding uses the specified `fallback` or the property default when omitted.

Existing numeric binds, such as `{"bind":"anchorui:player.health"}`, continue to work.

### Live text

A single string-valued field:

```json
{
  "type": "anchorui:text",
  "id": "mypack:player_name",
  "text": {
    "bind": "anchorui:player.name",
    "fallback": "Unknown player"
  },
  "size": {"width": 180, "height": 20}
}
```

Or an inline template with multiple data providers:

```json
{
  "type": "anchorui:text",
  "id": "mypack:welcome",
  "text": "<b>Hello, {{anchorui:player.name}}!</b> Level {{anchorui:player.profile#level}}",
  "size": {"width": 280, "height": 24}
}
```

Template substitutions use `{{namespace:source}}` or `{{namespace:source#object.path}}`. The placeholders are compiled when the widget is created, not reparsed every frame. Displayed content is updated only when the resolved text changes. Rich-text markup in the fixed template remains available; substituted values cannot inject formatting tags. For dynamically sized labels, set `size.width` and `size.height` explicitly so layout remains stable as data changes.

### Formatted binding values

Registered Java formatters can transform resolved values before their target binding type is checked. Use `"format": "namespace:formatter"` inside a binding object, or `{{anchorui:player.experience|anchorui:percent}}` in text templates. Lists also support `itemFormat` for each selected entry. Built-in options include `anchorui:integer`, `anchorui:percent`, `anchorui:compact` and `anchorui:uppercase`.

See [Data Formatters](FORMATTERS.md) for Java registration, typed options and error handling.

### Boolean visibility

```json
{
  "type": "anchorui:text",
  "id": "mypack:sneak_hint",
  "text": "Sneaking",
  "visibleWhen": {
    "bind": "anchorui:player.sneaking",
    "fallback": false
  },
  "size": {"width": 100, "height": 18}
}
```

`visibleWhen` and `enabledWhen` work with any widget in HUDs and screens. When visibility becomes false, the widget stops rendering but **keeps its layout bounds**, so anchored siblings do not jump around. When `enabledWhen` becomes false, the widget remains visible but cannot be clicked; buttons show their disabled appearance. Existing static `hidden` settings and player-controlled HUD visibility continue to apply. Conditions also support `all`, `any`, `not`, numeric/string/list comparisons and missing-value checks: see [Conditions](CONDITIONS.md).

### Lists and structured entries

```json
{
  "type": "anchorui:data_list",
  "id": "mypack:effects",
  "items": {
    "bind": "anchorui:player.effects",
    "fallback": []
  },
  "itemPath": "name",
  "emptyText": "No active effects",
  "maxEntries": 8,
  "size": {"width": 210, "height": 78}
}
```

`anchorui:data_list` renders one text entry per list element, with optional `itemPath` selecting an object field from each entry. Lists may also contain literal strings or numbers. Elements are capped by `maxEntries` (1–100), overflow is clipped to the widget bounds, and text is wrapped as needed. The displayed lines are cached until the data or widget width changes.

## Built-in Minecraft sources

| ID | Type | Content |
| --- | --- | --- |
| `anchorui:player.health` | Number | Current health |
| `anchorui:player.max_health` | Number | Maximum health |
| `anchorui:player.food` | Number | Hunger value |
| `anchorui:player.experience` | Number | Experience bar fraction (0–1) |
| `anchorui:player.level` | Number | Experience level |
| `anchorui:player.name` | String | Local player's display name |
| `anchorui:world.dimension` | String | Dimension ID |
| `anchorui:player.alive` | Boolean | Local player is alive |
| `anchorui:player.sneaking` | Boolean | Local player is sneaking |
| `anchorui:player.creative` | Boolean | Creative-type build permission |
| `anchorui:player.profile` | Object | `name`, `health`, `maxHealth`, `level`, `food`, `alive` |
| `anchorui:player.effects` | List of objects | Each entry has `name`, `amplifier` and `durationTicks` |

## Java integration API

Register providers during client setup, before UI construction, e.g. within `FMLClientSetupEvent.enqueueWork`:

```java
DataValueRegistry.registerString(
    ResourceLocation.fromNamespaceAndPath("mymod", "guild"),
    context -> guildNameFor(context.minecraft().player)
);

DataValueRegistry.registerBoolean(
    ResourceLocation.fromNamespaceAndPath("mymod", "can_quest"),
    context -> canStartQuest(context.minecraft().player)
);

DataValueRegistry.registerNumber(
    ResourceLocation.fromNamespaceAndPath("mymod", "player.mana"),
    context -> manaFor(context.minecraft().player)
);

DataValueRegistry.registerObject(
    ResourceLocation.fromNamespaceAndPath("mymod", "player.stats"),
    context -> serializeStatsToJson(context.minecraft().player)
);

DataValueRegistry.registerList(
    ResourceLocation.fromNamespaceAndPath("mymod", "active_quests"),
    context -> serializeQuestsToJsonArray(context.minecraft().player)
);
```

The object/list example helper methods above are placeholders supplied by your own mod, not Anchor UI methods. For third-party widgets, `BoundData.parse(json, "field", defaultJsonElement, DataBindingSpec.Kind.OBJECT)` can resolve typed inputs and nested `path` selections.

### Event-driven / mutable values

A provider can store its **most recently published value** instead of computing data every frame:

```java
DataValueRegistry.MutableValue balance = DataValueRegistry.registerMutable(
    ResourceLocation.fromNamespaceAndPath("mymod", "player.balance"),
    new JsonPrimitive(0)
);

// In your own client-side packet or state-change callback:
balance.set(new JsonPrimitive(newBalance));
```

A JSON widget reads the latest value through `{"bind":"mymod:player.balance","fallback":0}`. Mutable providers support any JSON value (number, string, boolean, object or list). Values are copied when published to prevent accidental modification after an update.

This **push-style publication** does not schedule a tick, perform network synchronization or create an event bus on its own. Your mod is responsible for publishing client-safe updates and for synchronizing server-authoritative information. The UI reads published values through a revision-aware per-binding snapshot, so unchanged data is not cloned on every render. Choose a polling provider for inexpensive live fields, and a mutable provider for values that are costly to calculate or arrive asynchronously.

### Revision-aware data sources (opt-in)

For an expensive provider that can cheaply tell whether its data has changed, register it with a revision counter. The source supplies **two independent functions**: a cheap, context-aware revision and a more expensive read.

```java
AtomicLong manaRevision = new AtomicLong();

DataValueRegistry.registerRevisioned(
    ResourceLocation.fromNamespaceAndPath("mymod", "player.mana_stats"),
    context -> manaRevision.get(),
    context -> makeManaStatsJson(context.minecraft().player)
);
```

In your own client-side event handler, call `manaRevision.incrementAndGet()` whenever the stats used by that provider change. `makeManaStatsJson` is a helper supplied by your mod, not by AnchorUI.

A binding that references `mymod:player.mana_stats` checks the revision at each evaluation, but calls the expensive read only when the revision differs or the widget context changes. For example:

```json
{
  "text": {
    "bind": "mymod:player.mana_stats",
    "path": "mana",
    "fallback": 0
  }
}
```

You can also implement `DataValueRegistry.RevisionedProvider` directly and use `DataValueRegistry.registerRevisioned(id, provider)`.

`registerMutable(id, initial)` is **automatically revisioned**: `set(updated)` publishes a defensive copy and advances the revision only when the JSON value actually differs. This is the simplest option when your data is already available in client-side events or packets.

**Correctness rules:**

- Revisions must change whenever the provider's visible JSON content changes for an unchanged `WidgetContext`. If you cannot guarantee that, use an ordinary live provider.
- The revision callback must be cheap and free from side effects. It is still checked on each binding evaluation. Do not put expensive JSON serialization in the revision function.
- Each compiled binding/condition owns its own snapshot. This is **not** one global cache across all UI widgets or Minecraft worlds. Source snapshots are discarded when the widget/condition is rebuilt.
- Cached JSON values are defensively copied so formatters and consumers cannot modify the saved snapshot. A formatter still executes when a bound value is requested, preserving its normal contextual behavior.
- If a provider read fails or the revision changes while a read is underway, no out-of-date cache entry is committed. Normal fallback handling continues.
- If your provider is associated with a particular player, world or item context, advance the revision whenever its data changes, or rebuild the widget when the relevant context changes. The cache also checks the identity of the `WidgetContext` instance.

**No JSON syntax change is required.** Existing bindings, template expressions, conditions, lists and progress values use the same `bind` and `fallback` structure. A live provider without revision support continues to be invoked on each read.

Data is display-only. JSON bindings cannot run arbitrary Java, issue commands, modify player inventory or award quests.

## Manual tests

```text
/anchorui test binding on
/anchorui test binding off
/anchorui screen open anchorui:bindings
```

The independent `binding` HUD test shows a player-name template, a world-dimension string, a health bar reading the structured `player.profile` object, a sneaking-only label and a dynamically updated list of status effects. This does not enable the older `hud1` or `rpg` demos. The Screen test displays similar live values.

An additional resource-pack Screen example is included at `examples/resourcepack/AnchorUI-Starter/assets/mypack/anchorui/screens/bindings.json`; when the starter pack is enabled, open it with `/anchorui screen open mypack:bindings`.
