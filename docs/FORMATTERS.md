# Data Formatters (NeoForge 1.21.1)

Anchor UI supports reusable data formatters registered by Java mods and referenced from resource-pack JSON. Formatters transform resolved binding values into display values without creating a custom widget.

## Built-in formatters

| Identifier | Input | Example |
| --- | --- | --- |
| `anchorui:integer` | Finite number | `11.6` → `"12"` |
| `anchorui:percent` | Fraction (0–1 typical) | `0.256` → `"26%"` |
| `anchorui:compact` | Finite number | `12500` → `"12.5K"` |
| `anchorui:uppercase` | String | `"Hello"` → `"HELLO"` |

Percent multiplies input by 100 and rounds to an integer. Compact notation uses locale-independent K, M, B suffixes.

## Register from Java

Register the formatter on the client during `FMLClientSetupEvent.enqueueWork`, before HUD or screen JSON is constructed.

```java
DataFormatterRegistry.register(
    ResourceLocation.fromNamespaceAndPath("rpg", "mana"),
    (value, context) -> new JsonPrimitive(value.getAsInt() + " MP")
);
```

`value` is the resolved Gson `JsonElement`, `context` is `WidgetContext`. The result is another `JsonElement`. Return a JSON string for text widgets, or a numeric primitive for a numeric target. Formatter callbacks should be inexpensive, deterministic, read-only, and should handle unexpected input types.

The ID must be namespaced (for example, `rpg:mana`). Duplicate registrations throw an exception.

## Use in JSON

**Formatted text:** convert a numeric source to display text with a string fallback:

```json
{
  "type": "anchorui:text",
  "id": "rpg:mana_label",
  "text": {
    "bind": "rpg:player.mana",
    "format": "rpg:mana",
    "fallback": "No mana"
  },
  "size": {
    "width": 160,
    "height": 20
  }
}
```

Evaluation order: data provider → optional `path` → formatter → target-type validation. The fallback type must match the **formatted result type**, not necessarily the source type. Missing input skips the formatter and uses the fallback. Exceptions, null outputs and wrong output types also use the fallback.

**Inline text:** append a formatter after a pipe (`|`):

```json
{
  "type": "anchorui:text",
  "id": "rpg:status",
  "text": "Mana: {{rpg:player.mana|rpg:mana}} | XP: {{anchorui:player.experience|anchorui:percent}}",
  "size": {
    "width": 300,
    "height": 20
  }
}
```

Combine an optional nested path with a formatter:

```text
{{rpg:player.stats#mana.current|rpg:mana}}
```

Syntax: `{{namespace:source#optional.path|optional_namespace:formatter}}`. Inline placeholders use `?` as their fallback, and substituted text is escaped to prevent injection of rich-text tags.

**List entries:** the optional `itemFormat` applies after `itemPath`:

```json
{
  "type": "anchorui:data_list",
  "id": "rpg:effects",
  "items": {
    "bind": "anchorui:player.effects",
    "fallback": []
  },
  "itemPath": "name",
  "itemFormat": "anchorui:uppercase",
  "maxEntries": 8,
  "size": {
    "width": 200,
    "height": 80
  }
}
```

If an item formatter fails, the individual entry displays as empty text; the rest of the list continues normally. JSON `format` is also available for any custom widget using `BoundData.parse(...)`. A progress-bar `value` or `max` remains numeric: a formatter returning text causes that numeric binding to fall back. Use a separate text widget for formatted labels.

## Typed formatter options

For configurable formatting, register a Codec-validated handler:

```java
DataFormatterRegistry.register(
    ResourceLocation.fromNamespaceAndPath("rpg", "suffix"),
    Codec.STRING.fieldOf("suffix").codec(),
    (value, context, suffix) -> new JsonPrimitive(value.getAsString() + suffix)
);
```

```json
{
  "type": "anchorui:text",
  "id": "rpg:gold",
  "text": {
    "bind": "rpg:player.gold",
    "format": {
      "type": "rpg:suffix",
      "suffix": " gold"
    },
    "fallback": "Unknown gold"
  }
}
```

Configuration is decoded **once when the widget is constructed**, not during each render. For advanced cases, `DataFormatterRegistry.registerFactory(id, factory)` accepts formatter JSON once and returns the compiled handler.

## Error handling and testing

A missing/invalid formatter ID or an invalid Codec configuration rejects just the affected widget during creation through the existing UI error-isolation system. Runtime formatter errors use the binding fallback instead of crashing a screen. Registered Java callbacks are trusted code; resource-pack JSON can only reference installed formatters and cannot execute arbitrary Java.

To see built-ins in game, open:

```text
/anchorui screen open anchorui:bindings
```

The test screen uppercases the player name and status effect names, formats the experience fraction as a percentage and the experience level as an integer.

See [Typed Data Binding](DATA_BINDINGS.md) for providers, nested binding paths and fallback semantics.
